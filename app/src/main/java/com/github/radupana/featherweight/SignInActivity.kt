package com.github.radupana.featherweight

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.lifecycleScope
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.di.ServiceLocator
import com.github.radupana.featherweight.service.LocalDataMigrationService
import com.github.radupana.featherweight.ui.theme.FeatherweightTheme
import com.github.radupana.featherweight.util.CloudLogger
import com.github.radupana.featherweight.util.LogSanitizer
import com.github.radupana.featherweight.util.MigrationStateManager
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
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

        CloudLogger.info(TAG, "onCreate: Showing sign-in screen")

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
            lifecycleScope.launch {
                if (migrationStateManager.shouldAttemptMigration()) {
                    val hasLocalData = migrationService.hasLocalData()
                    if (hasLocalData) {
                        CloudLogger.info("SignInActivity", "Local data detected, will migrate in background")
                        migrationStateManager.incrementMigrationAttempts()
                        migrateLocalDataInBackground(userId)
                    }
                }
            }

            navigateToMain()

            lifecycleScope.launch {
                syncDataInBackground(userId)
            }
        } else {
            navigateToMain()
        }
    }

    private suspend fun syncDataInBackground(userId: String) {
        CloudLogger.info("SignInActivity", "Starting background sync for user: $userId")

        val syncViewModel = ServiceLocator.getSyncViewModel(this)
        syncViewModel.startBackgroundSync()

        try {
            syncManager.syncAll().fold(
                onSuccess = { syncState ->
                    CloudLogger.info("SignInActivity", "Background sync completed successfully: $syncState")
                    syncViewModel.onSyncCompleted(syncState)
                },
                onFailure = { error ->
                    CloudLogger.error("SignInActivity", "Background sync failed", error)
                    syncViewModel.onSyncFailed(error.message ?: "Sync failed")
                },
            )
        } catch (e: com.google.firebase.FirebaseException) {
            CloudLogger.error("SignInActivity", "Background sync failed", e)
            syncViewModel.onSyncFailed(e.message ?: "Sync failed")
        } catch (e: android.database.sqlite.SQLiteException) {
            CloudLogger.error("SignInActivity", "Background sync failed - database error", e)
            syncViewModel.onSyncFailed(e.message ?: "Sync failed")
        }
    }

    private suspend fun migrateLocalDataInBackground(userId: String) {
        CloudLogger.info("SignInActivity", "Starting background migration for user: $userId")

        val migrationSuccess = migrationService.migrateLocalDataToUser(userId)

        if (migrationSuccess) {
            CloudLogger.info("SignInActivity", "Migration successful")
            migrationStateManager.markMigrationCompleted(userId)
            migrationService.cleanupLocalData()
            CloudLogger.info("SignInActivity", "Migration completed, sync will be handled by syncAll()")
        } else {
            CloudLogger.error("SignInActivity", "Migration failed, will retry on next sign-in")
            if (migrationStateManager.getMigrationAttempts() >= MigrationStateManager.MAX_MIGRATION_ATTEMPTS) {
                CloudLogger.error("SignInActivity", "Max migration attempts reached")
            }
        }
    }

    private fun navigateToMain() {
        authManager.setFirstLaunchComplete()

        val currentUser =
            com.google.firebase.auth.FirebaseAuth
                .getInstance()
                .currentUser
        if (currentUser != null &&
            !currentUser.isEmailVerified &&
            currentUser.providerData.any { it.providerId == "password" }
        ) {
            startActivity(Intent(this, EmailVerificationActivity::class.java))
        } else {
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
    val credentialManager = remember { CredentialManager.create(context) }

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
                        CloudLogger.info("SignInActivity", "User attempting $action with email: ${LogSanitizer.sanitizeEmail(email)}")

                        val result =
                            if (isSignUpMode) {
                                CloudLogger.info("SignInActivity", "Creating new account")
                                firebaseAuth.createUserWithEmailAndPassword(email, password)
                            } else {
                                CloudLogger.info("SignInActivity", "Signing in with email")
                                firebaseAuth.signInWithEmailAndPassword(email, password)
                            }

                        result.fold(
                            onSuccess = { user ->
                                CloudLogger.info("SignInActivity", "Authentication successful for user: ${user.uid}")
                                if (isSignUpMode) {
                                    CloudLogger.info("SignInActivity", "New user sign-up, sending verification email")
                                    firebaseAuth.sendEmailVerification().fold(
                                        onSuccess = {
                                            CloudLogger.info("SignInActivity", "Verification email sent successfully")
                                            Toast
                                                .makeText(
                                                    context,
                                                    "Verification email sent to $email",
                                                    Toast.LENGTH_LONG,
                                                ).show()
                                        },
                                        onFailure = { e ->
                                            CloudLogger.error("SignInActivity", "Failed to send verification email", e)
                                        },
                                    )
                                    CloudLogger.info("SignInActivity", "Navigating to email verification for unverified user: ${user.uid}")
                                    val intent = Intent(context, EmailVerificationActivity::class.java)
                                    context.startActivity(intent)
                                    (context as? ComponentActivity)?.finish()
                                } else {
                                    val currentUserId = authManager.getCurrentUserId()
                                    if (currentUserId != null && currentUserId != user.uid) {
                                        CloudLogger.info(
                                            "SignInActivity",
                                            "Switching from user $currentUserId to ${user.uid}, clearing old data",
                                        )
                                        authManager.clearUserData()
                                    }
                                    CloudLogger.info("SignInActivity", "Existing user sign-in, proceeding with normal flow")
                                    authManager.setCurrentUserId(user.uid)
                                    onSignInSuccess()
                                }
                            },
                            onFailure = { exception ->
                                CloudLogger.error("SignInActivity", "Authentication failed - ${exception.message}", exception)
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
                    scope.launch {
                        isLoading = true
                        CloudLogger.info("SignInActivity", "User initiated Google sign-in")

                        try {
                            val googleIdOption =
                                GetGoogleIdOption
                                    .Builder()
                                    .setFilterByAuthorizedAccounts(false)
                                    .setServerClientId(context.getString(R.string.default_web_client_id))
                                    .build()

                            val request =
                                GetCredentialRequest
                                    .Builder()
                                    .addCredentialOption(googleIdOption)
                                    .build()

                            val result = credentialManager.getCredential(context, request)
                            val credential = result.credential

                            if (credential is CustomCredential &&
                                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                            ) {
                                val googleIdTokenCredential =
                                    GoogleIdTokenCredential.createFrom(credential.data)
                                val idToken = googleIdTokenCredential.idToken
                                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)

                                firebaseAuth.signInWithCredential(firebaseCredential).fold(
                                    onSuccess = { user ->
                                        CloudLogger.info("SignInActivity", "Google sign-in successful, userId: ${user.uid}")
                                        val currentUserId = authManager.getCurrentUserId()
                                        if (currentUserId != null && currentUserId != user.uid) {
                                            CloudLogger.info(
                                                "SignInActivity",
                                                "Switching from user $currentUserId to ${user.uid}, clearing old data",
                                            )
                                            authManager.clearUserData()
                                        }
                                        authManager.setCurrentUserId(user.uid)
                                        CloudLogger.info("SignInActivity", "User ID saved, calling onSignInSuccess")
                                        onSignInSuccess()
                                    },
                                    onFailure = { exception ->
                                        CloudLogger.error("SignInActivity", "Firebase sign-in failed", exception)
                                        Toast
                                            .makeText(
                                                context,
                                                "Google sign-in failed: ${exception.message}",
                                                Toast.LENGTH_LONG,
                                            ).show()
                                    },
                                )
                            } else {
                                CloudLogger.error("SignInActivity", "Unexpected credential type: ${credential.type}")
                                Toast
                                    .makeText(
                                        context,
                                        "Unexpected credential type",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                            }
                        } catch (e: GetCredentialCancellationException) {
                            CloudLogger.info("SignInActivity", "Google sign-in cancelled by user: ${e.message}")
                        } catch (e: NoCredentialException) {
                            CloudLogger.info("SignInActivity", "No Google account available: ${e.message}")
                            Toast
                                .makeText(
                                    context,
                                    "No Google account found on device",
                                    Toast.LENGTH_SHORT,
                                ).show()
                        } catch (e: GoogleIdTokenParsingException) {
                            CloudLogger.error("SignInActivity", "Invalid Google ID token", e)
                            Toast
                                .makeText(
                                    context,
                                    "Invalid Google credentials",
                                    Toast.LENGTH_SHORT,
                                ).show()
                        } catch (e: GetCredentialException) {
                            CloudLogger.error("SignInActivity", "Google sign-in failed", e)
                            Toast
                                .makeText(
                                    context,
                                    "Google sign-in failed",
                                    Toast.LENGTH_SHORT,
                                ).show()
                        }

                        isLoading = false
                    }
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
                            CloudLogger.info("SignInActivity", "User requested password reset")
                            scope.launch {
                                isLoading = true
                                firebaseAuth.sendPasswordResetEmail(email).fold(
                                    onSuccess = {
                                        CloudLogger.info("SignInActivity", "Password reset email sent successfully")
                                        Toast
                                            .makeText(
                                                context,
                                                "Password reset email sent",
                                                Toast.LENGTH_LONG,
                                            ).show()
                                    },
                                    onFailure = { exception ->
                                        CloudLogger.error("SignInActivity", "Failed to send password reset email", exception)
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
                            CloudLogger.info("SignInActivity", "Password reset requested but email field is empty")
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
