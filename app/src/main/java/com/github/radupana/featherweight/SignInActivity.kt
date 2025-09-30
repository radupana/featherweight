package com.github.radupana.featherweight

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.di.ServiceLocator
import com.github.radupana.featherweight.service.LocalDataMigrationService
import com.github.radupana.featherweight.ui.theme.FeatherweightTheme
import com.github.radupana.featherweight.util.MigrationStateManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

class SignInActivity : ComponentActivity() {
    companion object {
        private const val TAG = "SignInActivity"
    }

    private val authManager by lazy { ServiceLocator.provideAuthenticationManager(this) }
    private val firebaseAuth by lazy { ServiceLocator.provideFirebaseAuthService() }
    private val database by lazy { FeatherweightDatabase.getDatabase(this) }
    private val migrationService by lazy { LocalDataMigrationService(database) }
    private val migrationStateManager by lazy { MigrationStateManager(this) }
    private val syncManager by lazy { ServiceLocator.getSyncManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Log.i(TAG, "onCreate: Showing sign-in screen")

        setContent {
            FeatherweightTheme {
                SignInScreen(
                    authManager = authManager,
                    firebaseAuth = firebaseAuth,
                    onSignInSuccess = { handleSuccessfulSignIn() },
                )
            }
        }
    }

    private fun handleSuccessfulSignIn() {
        val userId = authManager.getCurrentUserId()
        if (userId != null && userId != "local") {
            // Check for local data migration in background coroutine
            lifecycleScope.launch {
                if (migrationStateManager.shouldAttemptMigration()) {
                    val hasLocalData = migrationService.hasLocalData()
                    if (hasLocalData) {
                        Log.i("SignInActivity", "Local data detected, will migrate in background")
                        migrationStateManager.incrementMigrationAttempts()
                        migrateLocalDataInBackground(userId)
                    }
                }
            }

            // Navigate immediately - don't wait for sync
            navigateToMain()

            // Start sync in background AFTER navigation
            lifecycleScope.launch {
                syncDataInBackground(userId)
            }
        } else {
            navigateToMain()
        }
    }

    private suspend fun syncDataInBackground(userId: String) {
        Log.i("SignInActivity", "Starting background sync for user: $userId")

        // Update sync state to show syncing in progress
        val syncViewModel = ServiceLocator.getSyncViewModel(this)
        syncViewModel.startBackgroundSync()

        try {
            syncManager.syncAll().fold(
                onSuccess = { syncState ->
                    Log.i("SignInActivity", "Background sync completed successfully: $syncState")
                    syncViewModel.onSyncCompleted(syncState)
                },
                onFailure = { error ->
                    Log.e("SignInActivity", "Background sync failed", error)
                    syncViewModel.onSyncFailed(error.message ?: "Sync failed")
                },
            )
        } catch (e: Exception) {
            Log.e("SignInActivity", "Background sync failed", e)
            syncViewModel.onSyncFailed(e.message ?: "Sync failed")
            // Sync failures are non-critical - user can still use the app with cached data
        }
    }

    private suspend fun migrateLocalDataInBackground(userId: String) {
        Log.i("SignInActivity", "Starting background migration for user: $userId")

        val migrationSuccess = migrationService.migrateLocalDataToUser(userId)

        if (migrationSuccess) {
            Log.i("SignInActivity", "Migration successful")
            migrationStateManager.markMigrationCompleted(userId)

            // Clean up local data after successful migration
            migrationService.cleanupLocalData()

            // Note: Sync is handled by syncDataInBackground() which calls syncAll()
            // We don't need a separate syncUserData() call here to avoid duplication
            Log.i("SignInActivity", "Migration completed, sync will be handled by syncAll()")
        } else {
            Log.e("SignInActivity", "Migration failed, will retry on next sign-in")
            if (migrationStateManager.getMigrationAttempts() >= MigrationStateManager.MAX_MIGRATION_ATTEMPTS) {
                Log.e("SignInActivity", "Max migration attempts reached")
                // Don't show toast from background - it could crash
                // User will see the issue when they check their data
            }
        }
    }

    private fun navigateToMain() {
        authManager.setFirstLaunchComplete()

        // Check if email verification is required
        val currentUser =
            com.google.firebase.auth.FirebaseAuth
                .getInstance()
                .currentUser
        if (currentUser != null &&
            !currentUser.isEmailVerified &&
            currentUser.providerData.any { it.providerId == "password" }
        ) {
            // User signed in with email/password but not verified
            startActivity(Intent(this, EmailVerificationActivity::class.java))
        } else {
            // User is verified or signed in with Google
            startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }
}

@Composable
fun SignInScreen(
    authManager: com.github.radupana.featherweight.manager.AuthenticationManager,
    firebaseAuth: com.github.radupana.featherweight.service.FirebaseAuthService,
    onSignInSuccess: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isSignUpMode by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account?.idToken?.let { token ->
                    val credential = GoogleAuthProvider.getCredential(token, null)
                    scope.launch {
                        isLoading = true
                        firebaseAuth.signInWithCredential(credential).fold(
                            onSuccess = { user ->
                                Log.i("SignInActivity", "Google sign-in successful, userId: ${user.uid}")
                                authManager.setCurrentUserId(user.uid)
                                Log.i("SignInActivity", "User ID saved, calling onSignInSuccess")
                                onSignInSuccess()
                            },
                            onFailure = { exception ->
                                Toast
                                    .makeText(
                                        context,
                                        "Google sign-in failed: ${exception.message}",
                                        Toast.LENGTH_LONG,
                                    ).show()
                            },
                        )
                        isLoading = false
                    }
                }
            } catch (e: ApiException) {
                Log.e("SignInActivity", "Google sign-in failed", e)
                Toast
                    .makeText(
                        context,
                        "Google sign-in failed",
                        Toast.LENGTH_SHORT,
                    ).show()
            }
        }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = if (isSignUpMode) "Create Account" else "Sign In",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading,
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation =
                    if (isPasswordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector =
                                if (isPasswordVisible) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                            contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                        )
                    }
                },
                enabled = !isLoading,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        val action = if (isSignUpMode) "sign-up" else "sign-in"
                        Log.i("SignInActivity", "User attempting $action with email: $email")

                        val result =
                            if (isSignUpMode) {
                                Log.i("SignInActivity", "Creating new account for email: $email")
                                firebaseAuth.createUserWithEmailAndPassword(email, password)
                            } else {
                                Log.i("SignInActivity", "Signing in with email: $email")
                                firebaseAuth.signInWithEmailAndPassword(email, password)
                            }

                        result.fold(
                            onSuccess = { user ->
                                Log.i("SignInActivity", "Authentication successful for user: ${user.uid}, email: $email")
                                if (isSignUpMode) {
                                    // For sign-up, send verification email and navigate directly to verification screen
                                    Log.i("SignInActivity", "New user sign-up, sending verification email to: $email")
                                    firebaseAuth.sendEmailVerification().fold(
                                        onSuccess = {
                                            Log.i("SignInActivity", "Verification email sent successfully to: $email")
                                            Toast
                                                .makeText(
                                                    context,
                                                    "Verification email sent to $email",
                                                    Toast.LENGTH_LONG,
                                                ).show()
                                        },
                                        onFailure = { e ->
                                            Log.e("SignInActivity", "Failed to send verification email to: $email", e)
                                        },
                                    )
                                    // Navigate directly to email verification without setting user ID or syncing
                                    Log.i("SignInActivity", "Navigating to email verification for unverified user: ${user.uid}")
                                    val intent = Intent(context, EmailVerificationActivity::class.java)
                                    context.startActivity(intent)
                                    (context as? ComponentActivity)?.finish()
                                } else {
                                    // For sign-in, set user ID and proceed with normal flow
                                    Log.i("SignInActivity", "Existing user sign-in, proceeding with normal flow")
                                    authManager.setCurrentUserId(user.uid)
                                    onSignInSuccess()
                                }
                            },
                            onFailure = { exception ->
                                Log.e("SignInActivity", "Authentication failed for email: $email - ${exception.message}", exception)
                                Toast
                                    .makeText(
                                        context,
                                        exception.message ?: "Authentication failed",
                                        Toast.LENGTH_LONG,
                                    ).show()
                            },
                        )
                        isLoading = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(if (isSignUpMode) "Create Account" else "Sign In")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            )

            GoogleSignInButton(
                onClick = {
                    Log.i("SignInActivity", "User initiated Google sign-in")
                    val gso =
                        GoogleSignInOptions
                            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(context.getString(R.string.default_web_client_id))
                            .requestEmail()
                            .build()
                    val googleSignInClient = GoogleSignIn.getClient(context, gso)
                    launcher.launch(googleSignInClient.signInIntent)
                },
                enabled = !isLoading,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text =
                        if (isSignUpMode) {
                            "Already have an account?"
                        } else {
                            "Don't have an account?"
                        },
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(
                    onClick = { isSignUpMode = !isSignUpMode },
                    enabled = !isLoading,
                ) {
                    Text(if (isSignUpMode) "Sign In" else "Sign Up")
                }
            }

            if (!isSignUpMode) {
                TextButton(
                    onClick = {
                        if (email.isNotBlank()) {
                            Log.i("SignInActivity", "User requested password reset for email: $email")
                            scope.launch {
                                isLoading = true
                                firebaseAuth.sendPasswordResetEmail(email).fold(
                                    onSuccess = {
                                        Log.i("SignInActivity", "Password reset email sent successfully to: $email")
                                        Toast
                                            .makeText(
                                                context,
                                                "Password reset email sent",
                                                Toast.LENGTH_LONG,
                                            ).show()
                                    },
                                    onFailure = { exception ->
                                        Log.e("SignInActivity", "Failed to send password reset email to: $email", exception)
                                        Toast
                                            .makeText(
                                                context,
                                                exception.message ?: "Failed to send reset email",
                                                Toast.LENGTH_LONG,
                                            ).show()
                                    },
                                )
                                isLoading = false
                            }
                        } else {
                            Log.i("SignInActivity", "Password reset requested but email field is empty")
                            Toast
                                .makeText(
                                    context,
                                    "Please enter your email first",
                                    Toast.LENGTH_SHORT,
                                ).show()
                        }
                    },
                    enabled = !isLoading,
                ) {
                    Text("Forgot Password?")
                }
            }
        }
    }
}

@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_dialog_info),
                contentDescription = "Google logo",
                modifier = Modifier.size(20.dp),
                tint = Color.Unspecified,
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text("Sign in with Google")
        }
    }
}
