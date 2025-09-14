package com.github.radupana.featherweight.service

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser

interface FirebaseAuthService {
    fun getCurrentUser(): FirebaseUser?

    suspend fun signInWithEmailAndPassword(
        email: String,
        password: String,
    ): Result<FirebaseUser>

    suspend fun createUserWithEmailAndPassword(
        email: String,
        password: String,
    ): Result<FirebaseUser>

    suspend fun signInWithCredential(credential: AuthCredential): Result<FirebaseUser>

    suspend fun sendPasswordResetEmail(email: String): Result<Unit>

    suspend fun sendEmailVerification(): Result<Unit>

    suspend fun reloadUser(): Result<Unit>

    suspend fun updatePassword(newPassword: String): Result<Unit>

    suspend fun reauthenticateWithEmail(
        email: String,
        password: String,
    ): Result<Unit>

    suspend fun deleteAccount(): Result<Unit>

    fun signOut()

    fun isUserAuthenticated(): Boolean

    fun isEmailVerified(): Boolean

    fun getUserEmail(): String?

    fun getAuthProvider(): String?

    fun getAccountCreationTime(): Long?
}
