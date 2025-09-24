package com.github.radupana.featherweight

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.di.ServiceLocator
import com.github.radupana.featherweight.service.LocalDataMigrationService
import com.github.radupana.featherweight.ui.theme.FeatherweightTheme
import com.github.radupana.featherweight.util.MigrationStateManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class EmailVerificationActivity : ComponentActivity() {
    companion object {
        private const val TAG = "EmailVerificationActivity"
    }

    private val authManager by lazy { ServiceLocator.provideAuthenticationManager(this) }
    private val database by lazy { FeatherweightDatabase.getDatabase(this) }
    private val migrationService by lazy { LocalDataMigrationService(database) }
    private val migrationStateManager by lazy { MigrationStateManager(this) }
    private val syncManager by lazy { ServiceLocator.getSyncManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userEmail = FirebaseAuth.getInstance().currentUser?.email
        Log.i(TAG, "onCreate: Showing email verification screen for user: $userEmail")

        setContent {
            FeatherweightTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    EmailVerificationScreen(
                        onVerificationComplete = {
                            Log.i(TAG, "Email verification completed successfully")
                            handleVerificationComplete()
                        },
                        onSignOut = {
                            Log.i(TAG, "User signed out from email verification screen")
                            // Sign out and go back to welcome
                            FirebaseAuth.getInstance().signOut()
                            val authManager = ServiceLocator.provideAuthenticationManager(this)
                            authManager.clearUserData()

                            val intent = Intent(this, WelcomeActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        },
                    )
                }
            }
        }
    }

    private fun handleVerificationComplete() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            setupAuthenticatedUser(userId)

            // Navigate immediately to MainActivity
            navigateToMain()

            // Perform sync and migration in background after navigation
            lifecycleScope.launch {
                delay(100) // Small delay to ensure navigation completes
                performInitialSync(userId)
                handleDataMigration(userId)
            }
        } else {
            Log.e(TAG, "No user ID found after verification")
            navigateToMain()
        }
    }

    private fun setupAuthenticatedUser(userId: String) {
        authManager.setCurrentUserId(userId)
        authManager.setFirstLaunchComplete()
    }

    private suspend fun performInitialSync(userId: String) {
        try {
            Log.i(TAG, "Triggering initial sync for verified user: $userId")
            syncManager.syncAll()
            Log.i(TAG, "Initial sync completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Initial sync failed", e)
        }
    }

    private suspend fun handleDataMigration(userId: String) {
        if (!migrationStateManager.shouldAttemptMigration()) return

        Log.i(TAG, "Attempting local data migration for user: $userId")
        migrationStateManager.incrementMigrationAttempts()

        if (!migrationService.hasLocalData()) {
            Log.i(TAG, "No local data to migrate - skipping migration")
            return
        }

        Log.i(TAG, "Local data found, starting migration")
        val success = migrationService.migrateLocalDataToUser(userId)

        if (success) {
            handleSuccessfulMigration(userId)
        } else {
            handleFailedMigration()
        }
    }

    private suspend fun handleSuccessfulMigration(userId: String) {
        Log.i(TAG, "Migration successful")
        migrationStateManager.markMigrationCompleted(userId)
        migrationService.cleanupLocalData()

        Log.i(TAG, "Syncing migrated data to Firestore")
        try {
            syncManager.syncUserData(userId)
            Log.i(TAG, "Migration sync completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync migrated data", e)
        }
    }

    private fun handleFailedMigration() {
        Log.e(TAG, "Migration failed, will retry on next sign-in")
        if (migrationStateManager.getMigrationAttempts() >= MigrationStateManager.MAX_MIGRATION_ATTEMPTS) {
            Log.e(TAG, "Max migration attempts reached")
            Toast
                .makeText(
                    this@EmailVerificationActivity,
                    "Failed to migrate local data. Please contact support.",
                    Toast.LENGTH_LONG,
                ).show()
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this@EmailVerificationActivity, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

@Composable
fun EmailVerificationScreen(
    onVerificationComplete: () -> Unit,
    onSignOut: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isChecking by remember { mutableStateOf(false) }
    var isResending by remember { mutableStateOf(false) }

    val firebaseAuth = remember { ServiceLocator.provideFirebaseAuthService() }
    val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Verify Your Email",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "We've sent a verification email to:",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = userEmail,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Please check your email and click the verification link. Once verified, tap the button below to continue.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                scope.launch {
                    isChecking = true
                    Log.i("EmailVerificationActivity", "User clicked 'I've Verified My Email' button")
                    try {
                        // Reload the user to get the latest verification status
                        Log.d("EmailVerificationActivity", "Reloading user to check verification status")
                        firebaseAuth.reloadUser()

                        // Check if email is now verified
                        if (firebaseAuth.isEmailVerified()) {
                            Log.i("EmailVerificationActivity", "Email verification confirmed for user: $userEmail")
                            Toast
                                .makeText(
                                    context,
                                    "Email verified successfully!",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            onVerificationComplete()
                        } else {
                            Log.i("EmailVerificationActivity", "Email not yet verified for user: $userEmail")
                            Toast
                                .makeText(
                                    context,
                                    "Email not yet verified. Please check your inbox.",
                                    Toast.LENGTH_LONG,
                                ).show()
                        }
                    } catch (e: Exception) {
                        Log.e("EmailVerificationActivity", "Error checking verification status for user: $userEmail", e)
                        Toast
                            .makeText(
                                context,
                                "Error checking verification status",
                                Toast.LENGTH_SHORT,
                            ).show()
                    }
                    isChecking = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isChecking && !isResending,
        ) {
            if (isChecking) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text("I've Verified My Email")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = {
                scope.launch {
                    isResending = true
                    Log.i("EmailVerificationActivity", "User requested resend of verification email for: $userEmail")
                    firebaseAuth.sendEmailVerification().fold(
                        onSuccess = {
                            Log.i("EmailVerificationActivity", "Verification email successfully resent to: $userEmail")
                            Toast
                                .makeText(
                                    context,
                                    "Verification email sent!",
                                    Toast.LENGTH_LONG,
                                ).show()
                        },
                        onFailure = { e ->
                            Log.e("EmailVerificationActivity", "Failed to resend verification email to: $userEmail", e)
                            val errorMessage =
                                when {
                                    e.message?.contains("Too many requests") == true ->
                                        "Too many attempts. Please wait a few minutes before trying again."
                                    e.message?.contains("rate") == true ->
                                        "Rate limit exceeded. Please try again later."
                                    else -> "Failed to send email: ${e.message ?: "Please try again."}"
                                }
                            Toast
                                .makeText(
                                    context,
                                    errorMessage,
                                    Toast.LENGTH_LONG,
                                ).show()
                        },
                    )
                    isResending = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isChecking && !isResending,
        ) {
            if (isResending) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                )
            } else {
                Text("Resend Verification Email")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isChecking && !isResending,
        ) {
            Text("Sign Out")
        }
    }
}
