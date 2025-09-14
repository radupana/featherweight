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

    fun signOut()

    fun isUserAuthenticated(): Boolean
}
