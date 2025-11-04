package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.util.CloudLogger
import com.github.radupana.featherweight.util.ExceptionLogger
import com.github.radupana.featherweight.util.LogSanitizer
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
            CloudLogger.info(TAG, "Attempting sign-in for email: ${LogSanitizer.sanitizeEmail(email)}")
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.let {
                CloudLogger.info(TAG, "Sign-in successful for user: ${it.uid}")
                Result.success(it)
            } ?: Result.failure(Exception("Sign in failed: No user returned"))
        } catch (e: com.google.firebase.FirebaseTooManyRequestsException) {
            ExceptionLogger.logNonCritical("FirebaseAuthService", "Sign in failed: too many requests", e)
            Result.failure(Exception("Too many attempts. Please wait before trying again."))
        } catch (e: FirebaseAuthException) {
            ExceptionLogger.logNonCritical("FirebaseAuthService", "Sign in failed: ${e.message}", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            ExceptionLogger.logNonCritical("FirebaseAuthService", "Sign in failed: ${e.message}", e)
            Result.failure(e)
        }

    override suspend fun createUserWithEmailAndPassword(
        email: String,
        password: String,
    ): Result<FirebaseUser> =
        try {
            CloudLogger.info(TAG, "Creating new account for email: ${LogSanitizer.sanitizeEmail(email)}")
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let {
                CloudLogger.info(TAG, "Account created successfully for user: ${it.uid}")
                Result.success(it)
            } ?: Result.failure(Exception("Account creation failed: No user returned"))
        } catch (e: com.google.firebase.FirebaseTooManyRequestsException) {
            ExceptionLogger.logNonCritical("FirebaseAuthService", "Account creation failed: too many requests", e)
            Result.failure(Exception("Too many attempts. Please wait before trying again."))
        } catch (e: FirebaseAuthException) {
            ExceptionLogger.logNonCritical("FirebaseAuthService", "Account creation failed: ${e.message}", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            ExceptionLogger.logNonCritical("FirebaseAuthService", "Account creation failed: ${e.message}", e)
            Result.failure(e)
        }

    override suspend fun signInWithCredential(credential: AuthCredential): Result<FirebaseUser> =
        try {
            val result = auth.signInWithCredential(credential).await()
            result.user?.let { Result.success(it) }
                ?: Result.failure(Exception("Sign in with credential failed: No user returned"))
        } catch (e: com.google.firebase.FirebaseTooManyRequestsException) {
            ExceptionLogger.logNonCritical("FirebaseAuthService", "Sign in with credential failed: too many requests", e)
            Result.failure(Exception("Too many attempts. Please wait before trying again."))
        } catch (e: FirebaseAuthException) {
            ExceptionLogger.logNonCritical("FirebaseAuthService", "Sign in with credential failed: ${e.message}", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            ExceptionLogger.logNonCritical("FirebaseAuthService", "Sign in with credential failed: ${e.message}", e)
            Result.failure(e)
        }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> =
        try {
            CloudLogger.info(TAG, "Sending password reset email to: ${LogSanitizer.sanitizeEmail(email)}")
            auth.sendPasswordResetEmail(email).await()
            CloudLogger.info(TAG, "Password reset email sent successfully")
            Result.success(Unit)
        } catch (e: com.google.firebase.FirebaseTooManyRequestsException) {
            ExceptionLogger.logNonCritical("FirebaseAuthService", "Password reset email failed: too many requests", e)
            Result.failure(Exception("Too many attempts. Please wait before trying again."))
        } catch (e: FirebaseAuthException) {
            ExceptionLogger.logNonCritical("FirebaseAuthService", "Password reset email failed: ${e.message}", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            ExceptionLogger.logNonCritical("FirebaseAuthService", "Password reset email failed: ${e.message}", e)
            Result.failure(e)
        }

    override suspend fun sendEmailVerification(): Result<Unit> =
        try {
            val user = auth.currentUser
            if (user != null) {
                CloudLogger.info(TAG, "Sending email verification to: ${LogSanitizer.sanitizeEmail(user.email)}")
                user.sendEmailVerification().await()
                CloudLogger.info(TAG, "Email verification sent successfully")
                Result.success(Unit)
            } else {
                Result.failure(Exception("No user logged in"))
            }
        } catch (e: com.google.firebase.FirebaseTooManyRequestsException) {
            ExceptionLogger.logNonCritical("FirebaseAuthService", "Email verification failed: too many requests", e)
            Result.failure(Exception("Too many requests. Please wait a few minutes before trying again."))
        } catch (e: FirebaseAuthException) {
            ExceptionLogger.logNonCritical("FirebaseAuthService", "Email verification failed: ${e.message}", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            ExceptionLogger.logNonCritical("FirebaseAuthService", "Email verification failed: ${e.message}", e)
            Result.failure(e)
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
        } catch (e: com.google.firebase.FirebaseTooManyRequestsException) {
            ExceptionLogger.logNonCritical("FirebaseAuthService", "User reload failed: too many requests", e)
            Result.failure(Exception("Too many attempts. Please wait before trying again."))
        } catch (e: FirebaseAuthException) {
            ExceptionLogger.logNonCritical("FirebaseAuthService", "User reload failed: ${e.message}", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            ExceptionLogger.logNonCritical("FirebaseAuthService", "User reload failed: ${e.message}", e)
            Result.failure(e)
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
        } catch (e: com.google.firebase.FirebaseTooManyRequestsException) {
            ExceptionLogger.logNonCritical("FirebaseAuthService", "Password update failed: too many requests", e)
            Result.failure(Exception("Too many attempts. Please wait before trying again."))
        } catch (e: FirebaseAuthException) {
            ExceptionLogger.logNonCritical("FirebaseAuthService", "Password update failed: ${e.message}", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            ExceptionLogger.logNonCritical("FirebaseAuthService", "Password update failed: ${e.message}", e)
            Result.failure(e)
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
        } catch (e: com.google.firebase.FirebaseTooManyRequestsException) {
            ExceptionLogger.logNonCritical("FirebaseAuthService", "Reauthentication failed: too many requests", e)
            Result.failure(Exception("Too many attempts. Please wait before trying again."))
        } catch (e: FirebaseAuthException) {
            ExceptionLogger.logNonCritical("FirebaseAuthService", "Reauthentication failed: ${e.message}", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            ExceptionLogger.logNonCritical("FirebaseAuthService", "Reauthentication failed: ${e.message}", e)
            Result.failure(e)
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
        } catch (e: com.google.firebase.FirebaseTooManyRequestsException) {
            ExceptionLogger.logNonCritical("FirebaseAuthService", "Account deletion failed: too many requests", e)
            Result.failure(Exception("Too many attempts. Please wait before trying again."))
        } catch (e: FirebaseAuthException) {
            ExceptionLogger.logNonCritical("FirebaseAuthService", "Account deletion failed: ${e.message}", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            ExceptionLogger.logNonCritical("FirebaseAuthService", "Account deletion failed: ${e.message}", e)
            Result.failure(e)
        }

    override fun signOut() {
        val uid = auth.currentUser?.uid
        CloudLogger.info(TAG, "User signing out - uid: $uid")
        auth.signOut()
        CloudLogger.info(TAG, "User signed out successfully")
    }

    override fun isUserAuthenticated(): Boolean = auth.currentUser != null

    override fun isEmailVerified(): Boolean = auth.currentUser?.isEmailVerified ?: false

    override fun getUserEmail(): String? {
        val email = auth.currentUser?.email
        CloudLogger.debug("FirebaseAuthServiceImpl", "getUserEmail - currentUser: ${auth.currentUser?.uid}, has email: ${!email.isNullOrBlank()}")
        return email
    }

    override fun getAuthProvider(): String? =
        auth.currentUser
            ?.providerData
            ?.firstOrNull { it.providerId != "firebase" }
            ?.providerId

    override fun getAccountCreationTime(): Long? =
        auth.currentUser
            ?.metadata
            ?.creationTimestamp

    override fun isAnonymous(): Boolean = auth.currentUser?.isAnonymous ?: false
}
