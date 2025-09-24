package com.github.radupana.featherweight.service

import android.util.Log
import com.github.radupana.featherweight.util.ExceptionLogger
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class FirebaseAuthServiceImpl : FirebaseAuthService {
    companion object {
        private const val TAG = "FirebaseAuthService"
    }

    private val auth = FirebaseAuth.getInstance()

    override fun getCurrentUser(): FirebaseUser? = auth.currentUser

    override suspend fun signInWithEmailAndPassword(
        email: String,
        password: String,
    ): Result<FirebaseUser> =
        try {
            Log.i(TAG, "Attempting sign-in for email: $email")
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.let {
                Log.i(TAG, "Sign-in successful for user: ${it.uid}, email: $email")
                Result.success(it)
            } ?: Result.failure(Exception("Sign in failed: No user returned"))
        } catch (e: Exception) {
            ExceptionLogger.logNonCritical("FirebaseAuthService", "Sign in failed: ${e.message}", e)
            when (e) {
                is com.google.firebase.FirebaseTooManyRequestsException ->
                    Result.failure(Exception("Too many attempts. Please wait before trying again."))
                is FirebaseAuthException -> Result.failure(e)
                else -> Result.failure(e)
            }
        }

    override suspend fun createUserWithEmailAndPassword(
        email: String,
        password: String,
    ): Result<FirebaseUser> =
        try {
            Log.i(TAG, "Creating new account for email: $email")
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let {
                Log.i(TAG, "Account created successfully for user: ${it.uid}, email: $email")
                Result.success(it)
            } ?: Result.failure(Exception("Account creation failed: No user returned"))
        } catch (e: Exception) {
            ExceptionLogger.logNonCritical("FirebaseAuthService", "Account creation failed: ${e.message}", e)
            when (e) {
                is com.google.firebase.FirebaseTooManyRequestsException ->
                    Result.failure(Exception("Too many attempts. Please wait before trying again."))
                is FirebaseAuthException -> Result.failure(e)
                else -> Result.failure(e)
            }
        }

    override suspend fun signInWithCredential(credential: AuthCredential): Result<FirebaseUser> =
        try {
            val result = auth.signInWithCredential(credential).await()
            result.user?.let { Result.success(it) }
                ?: Result.failure(Exception("Sign in with credential failed: No user returned"))
        } catch (e: Exception) {
            ExceptionLogger.logNonCritical("FirebaseAuthService", "Sign in with credential failed: ${e.message}", e)
            when (e) {
                is com.google.firebase.FirebaseTooManyRequestsException ->
                    Result.failure(Exception("Too many attempts. Please wait before trying again."))
                is FirebaseAuthException -> Result.failure(e)
                else -> Result.failure(e)
            }
        }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> =
        try {
            Log.i(TAG, "Sending password reset email to: $email")
            auth.sendPasswordResetEmail(email).await()
            Log.i(TAG, "Password reset email sent successfully to: $email")
            Result.success(Unit)
        } catch (e: Exception) {
            ExceptionLogger.logNonCritical("FirebaseAuthService", "Password reset email failed: ${e.message}", e)
            when (e) {
                is com.google.firebase.FirebaseTooManyRequestsException ->
                    Result.failure(Exception("Too many attempts. Please wait before trying again."))
                is FirebaseAuthException -> Result.failure(e)
                else -> Result.failure(e)
            }
        }

    override suspend fun sendEmailVerification(): Result<Unit> =
        try {
            val user = auth.currentUser
            if (user != null) {
                Log.i(TAG, "Sending email verification to: ${user.email}")
                user.sendEmailVerification().await()
                Log.i(TAG, "Email verification sent successfully to: ${user.email}")
                Result.success(Unit)
            } else {
                Result.failure(Exception("No user logged in"))
            }
        } catch (e: Exception) {
            ExceptionLogger.logNonCritical("FirebaseAuthService", "Email verification failed: ${e.message}", e)
            // Handle rate limiting specifically
            when (e) {
                is com.google.firebase.FirebaseTooManyRequestsException ->
                    Result.failure(Exception("Too many requests. Please wait a few minutes before trying again."))
                else -> Result.failure(e)
            }
        }

    override suspend fun reloadUser(): Result<Unit> =
        try {
            val user = auth.currentUser
            if (user != null) {
                user.reload().await()
                Result.success(Unit)
            } else {
                Result.failure(Exception("No user logged in"))
            }
        } catch (e: Exception) {
            ExceptionLogger.logNonCritical("FirebaseAuthService", "User reload failed: ${e.message}", e)
            when (e) {
                is com.google.firebase.FirebaseTooManyRequestsException ->
                    Result.failure(Exception("Too many attempts. Please wait before trying again."))
                is FirebaseAuthException -> Result.failure(e)
                else -> Result.failure(e)
            }
        }

    override suspend fun updatePassword(newPassword: String): Result<Unit> =
        try {
            val user = auth.currentUser
            if (user != null) {
                user.updatePassword(newPassword).await()
                Result.success(Unit)
            } else {
                Result.failure(Exception("No user logged in"))
            }
        } catch (e: Exception) {
            ExceptionLogger.logNonCritical("FirebaseAuthService", "Password update failed: ${e.message}", e)
            when (e) {
                is com.google.firebase.FirebaseTooManyRequestsException ->
                    Result.failure(Exception("Too many attempts. Please wait before trying again."))
                is FirebaseAuthException -> Result.failure(e)
                else -> Result.failure(e)
            }
        }

    override suspend fun reauthenticateWithEmail(
        email: String,
        password: String,
    ): Result<Unit> =
        try {
            val credential = EmailAuthProvider.getCredential(email, password)
            val user = auth.currentUser
            if (user != null) {
                user.reauthenticate(credential).await()
                Result.success(Unit)
            } else {
                Result.failure(Exception("No user logged in"))
            }
        } catch (e: Exception) {
            ExceptionLogger.logNonCritical("FirebaseAuthService", "Reauthentication failed: ${e.message}", e)
            when (e) {
                is com.google.firebase.FirebaseTooManyRequestsException ->
                    Result.failure(Exception("Too many attempts. Please wait before trying again."))
                is FirebaseAuthException -> Result.failure(e)
                else -> Result.failure(e)
            }
        }

    override suspend fun deleteAccount(): Result<Unit> =
        try {
            val user = auth.currentUser
            if (user != null) {
                user.delete().await()
                Result.success(Unit)
            } else {
                Result.failure(Exception("No user logged in"))
            }
        } catch (e: Exception) {
            ExceptionLogger.logNonCritical("FirebaseAuthService", "Account deletion failed: ${e.message}", e)
            when (e) {
                is com.google.firebase.FirebaseTooManyRequestsException ->
                    Result.failure(Exception("Too many attempts. Please wait before trying again."))
                is FirebaseAuthException -> Result.failure(e)
                else -> Result.failure(e)
            }
        }

    override fun signOut() {
        val email = auth.currentUser?.email
        val uid = auth.currentUser?.uid
        Log.i(TAG, "User signing out - uid: $uid, email: $email")
        auth.signOut()
        Log.i(TAG, "User signed out successfully")
    }

    override fun isUserAuthenticated(): Boolean = auth.currentUser != null

    override fun isEmailVerified(): Boolean = auth.currentUser?.isEmailVerified ?: false

    override fun getUserEmail(): String? = auth.currentUser?.email

    override fun getAuthProvider(): String? =
        auth.currentUser
            ?.providerData
            ?.firstOrNull { it.providerId != "firebase" }
            ?.providerId

    override fun getAccountCreationTime(): Long? = auth.currentUser?.metadata?.creationTimestamp
}
