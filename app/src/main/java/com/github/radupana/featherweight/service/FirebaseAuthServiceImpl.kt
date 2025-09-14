package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.util.ExceptionLogger
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class FirebaseAuthServiceImpl : FirebaseAuthService {
    private val auth = FirebaseAuth.getInstance()

    override fun getCurrentUser(): FirebaseUser? = auth.currentUser

    override suspend fun signInWithEmailAndPassword(
        email: String,
        password: String,
    ): Result<FirebaseUser> =
        try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.let { Result.success(it) }
                ?: Result.failure(Exception("Sign in failed: No user returned"))
        } catch (e: FirebaseAuthException) {
            ExceptionLogger.logNonCritical("FirebaseAuthService", "Sign in failed", e)
            Result.failure(e)
        }

    override suspend fun createUserWithEmailAndPassword(
        email: String,
        password: String,
    ): Result<FirebaseUser> =
        try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let { Result.success(it) }
                ?: Result.failure(Exception("Account creation failed: No user returned"))
        } catch (e: FirebaseAuthException) {
            ExceptionLogger.logNonCritical("FirebaseAuthService", "Account creation failed", e)
            Result.failure(e)
        }

    override suspend fun signInWithCredential(credential: AuthCredential): Result<FirebaseUser> =
        try {
            val result = auth.signInWithCredential(credential).await()
            result.user?.let { Result.success(it) }
                ?: Result.failure(Exception("Sign in with credential failed: No user returned"))
        } catch (e: FirebaseAuthException) {
            ExceptionLogger.logNonCritical("FirebaseAuthService", "Sign in with credential failed", e)
            Result.failure(e)
        }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> =
        try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: FirebaseAuthException) {
            ExceptionLogger.logNonCritical("FirebaseAuthService", "Password reset email failed", e)
            Result.failure(e)
        }

    override fun signOut() {
        auth.signOut()
    }

    override fun isUserAuthenticated(): Boolean = auth.currentUser != null
}
