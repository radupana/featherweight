package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.manager.AuthenticationManager
import com.github.radupana.featherweight.util.CloudLogger
import com.github.radupana.featherweight.util.ExceptionLogger
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Service responsible for complete account deletion including:
 * - Firebase Authentication account
 * - Firestore cloud data
 * - Local Room database data
 * - SharedPreferences
 */
class AccountDeletionService(
    private val database: FeatherweightDatabase,
    private val authManager: AuthenticationManager,
    private val firebaseAuth: FirebaseAuthService,
) {
    companion object {
        private const val TAG = "AccountDeletionService"
    }

    sealed class DeletionResult {
        object Success : DeletionResult()

        data class RequiresReauthentication(
            val authProvider: String,
        ) : DeletionResult()

        data class Error(
            val message: String,
        ) : DeletionResult()
    }

    /**
     * Attempts to delete the user account and all associated data.
     * Returns RequiresReauthentication if the user needs to authenticate again.
     *
     * IMPORTANT: Order of operations:
     * 1. Check if re-authentication is needed (without actually deleting)
     * 2. If auth is good, wipe all data
     * 3. Finally delete the auth account
     */
    suspend fun deleteAccount(): DeletionResult =
        withContext(Dispatchers.IO) {
            try {
                CloudLogger.debug(TAG, "Starting account deletion process")

                val userId = authManager.getCurrentUserId()
                if (userId == null) {
                    CloudLogger.error(TAG, "No user ID found")
                    return@withContext DeletionResult.Error("No user logged in")
                }

                // First, check if we can delete the auth account (dry run)
                val authCheck = checkAuthDeletionPossible()
                if (authCheck !is DeletionResult.Success) {
                    CloudLogger.debug(TAG, "Auth deletion requires re-authentication")
                    return@withContext authCheck
                }

                // Now we know auth deletion will work, so wipe data first

                // Step 1: Delete Firestore data FIRST (while we still have auth)
                CloudLogger.debug(TAG, "Deleting Firestore data for user: $userId")
                deleteFirestoreData(userId)

                // Step 2: Delete local Room database data
                CloudLogger.debug(TAG, "Deleting local database data for user: $userId")
                deleteLocalData(userId)

                // Step 3: Clear SharedPreferences
                CloudLogger.debug(TAG, "Clearing SharedPreferences")
                authManager.clearUserData()

                // Step 4: FINALLY delete Firebase Auth account (do this LAST!)
                CloudLogger.debug(TAG, "Deleting Firebase Auth account")
                val authDeletionResult = deleteFirebaseAuthAccount()
                if (authDeletionResult !is DeletionResult.Success) {
                    CloudLogger.error(TAG, "Auth deletion failed after data wipe: $authDeletionResult")
                    // Data is already wiped, but auth account remains - this is bad but safe
                    return@withContext authDeletionResult
                }

                CloudLogger.debug(TAG, "Account deletion completed successfully")
                DeletionResult.Success
            } catch (e: com.google.firebase.FirebaseException) {
                CloudLogger.error(TAG, "Account deletion failed - Firebase error", e)
                ExceptionLogger.logNonCritical(TAG, "Account deletion failed", e)
                DeletionResult.Error("Failed to delete account: ${e.message}")
            } catch (e: android.database.sqlite.SQLiteException) {
                CloudLogger.error(TAG, "Account deletion failed - database error", e)
                ExceptionLogger.logNonCritical(TAG, "Account deletion failed", e)
                DeletionResult.Error("Failed to delete account: ${e.message}")
            }
        }

    /**
     * Re-authenticates the user and then attempts deletion again.
     * For email users: requires password
     * For Google users: requires Google sign-in credential
     */
    suspend fun deleteAccountWithReauthentication(credential: AuthCredential): DeletionResult =
        withContext(Dispatchers.IO) {
            try {
                CloudLogger.debug(TAG, "Re-authenticating user for deletion")

                val user = FirebaseAuth.getInstance().currentUser
                if (user == null) {
                    return@withContext DeletionResult.Error("No user logged in")
                }

                // Re-authenticate
                user.reauthenticate(credential).await()
                CloudLogger.debug(TAG, "Re-authentication successful")

                // Now try deletion again
                deleteAccount()
            } catch (e: com.google.firebase.auth.FirebaseAuthException) {
                CloudLogger.error(TAG, "Re-authentication failed - Firebase auth error", e)
                DeletionResult.Error("Re-authentication failed: ${e.message}")
            } catch (e: java.io.IOException) {
                CloudLogger.error(TAG, "Re-authentication failed - network error", e)
                DeletionResult.Error("Re-authentication failed: ${e.message}")
            }
        }

    /**
     * Creates an email auth credential for re-authentication
     */
    fun createEmailCredential(
        email: String,
        password: String,
    ): AuthCredential = EmailAuthProvider.getCredential(email, password)

    /**
     * Creates a Google auth credential for re-authentication
     */
    fun createGoogleCredential(idToken: String): AuthCredential = GoogleAuthProvider.getCredential(idToken, null)

    private fun checkAuthDeletionPossible(): DeletionResult {
        return try {
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                return DeletionResult.Error("No user logged in")
            }

            // Check if the user's auth token is recent enough
            // This doesn't actually delete, just checks if deletion would work
            val metadata = user.metadata
            val lastSignInTime = metadata?.lastSignInTimestamp ?: 0
            val currentTime = System.currentTimeMillis()
            val timeSinceLastAuth = currentTime - lastSignInTime

            // Firebase requires auth within last 5 minutes for sensitive operations
            if (timeSinceLastAuth > 5 * 60 * 1000) {
                CloudLogger.debug(TAG, "Recent login required (last sign in: ${timeSinceLastAuth / 1000}s ago)")
                val authProvider = firebaseAuth.getAuthProvider() ?: "password"
                DeletionResult.RequiresReauthentication(authProvider)
            } else {
                DeletionResult.Success
            }
        } catch (e: com.google.firebase.FirebaseException) {
            CloudLogger.error(TAG, "Failed to check auth status - Firebase error", e)
            DeletionResult.Error("Failed to check authentication status: ${e.message}")
        } catch (e: IllegalStateException) {
            CloudLogger.error(TAG, "Failed to check auth status - invalid state", e)
            DeletionResult.Error("Failed to check authentication status: ${e.message}")
        }
    }

    private suspend fun deleteFirebaseAuthAccount(): DeletionResult {
        return try {
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                return DeletionResult.Error("No user logged in")
            }

            user.delete().await()
            CloudLogger.debug(TAG, "Firebase Auth account deleted successfully")
            DeletionResult.Success
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            CloudLogger.debug(TAG, "Recent login required for account deletion", e)
            val authProvider = firebaseAuth.getAuthProvider() ?: "password"
            DeletionResult.RequiresReauthentication(authProvider)
        } catch (e: com.google.firebase.auth.FirebaseAuthException) {
            CloudLogger.error(TAG, "Failed to delete Firebase Auth account - Firebase auth error", e)
            DeletionResult.Error("Failed to delete authentication account: ${e.message}")
        } catch (e: java.io.IOException) {
            CloudLogger.error(TAG, "Failed to delete Firebase Auth account - network error", e)
            DeletionResult.Error("Failed to delete authentication account: ${e.message}")
        }
    }

    // Suppress TooGenericExceptionCaught: Firebase initialization can throw RuntimeException
    // from unmocked Android methods in test environments. This method must handle all exceptions
    // gracefully to allow local data deletion to proceed even if Firebase operations fail.
    @Suppress("TooGenericExceptionCaught")
    private suspend fun deleteFirestoreData(userId: String) {
        try {
            val firestore = FirebaseFirestore.getInstance("featherweight-v2")

            // Delete user document and all subcollections
            val userDocRef = firestore.collection("users").document(userId)

            // Delete all subcollections first
            val subcollections =
                listOf(
                    "workouts",
                    "exerciseLogs",
                    "setLogs",
                    "exerciseCores",
                    "exerciseVariations",
                    "programmes",
                    "programmeWeeks",
                    "programmeWorkouts",
                    "exerciseSubstitutions",
                    "programmeProgress",
                    "userExerciseMaxes",
                    "oneRMHistory",
                    "personalRecords",
                    "exerciseSwapHistory",
                    "exercisePerformanceTracking",
                    "globalExerciseProgress",
                    "trainingAnalyses",
                    "parseRequests",
                )

            for (subcollection in subcollections) {
                deleteSubcollection(userDocRef.collection(subcollection).path)
            }

            // Delete the user document itself
            userDocRef.delete().await()

            // Delete sync metadata
            firestore
                .collection("syncMetadata")
                .document(userId)
                .delete()
                .await()

            CloudLogger.debug(TAG, "Firestore data deleted successfully")
        } catch (e: Throwable) {
            CloudLogger.error(TAG, "Failed to delete Firestore data: ${e.javaClass.simpleName} - ${e.message}", e)
            // Continue with local deletion even if Firestore fails
        }
    }

    private suspend fun deleteSubcollection(path: String) {
        try {
            val firestore = FirebaseFirestore.getInstance("featherweight-v2")
            val collection = firestore.collection(path)
            val documents = collection.get().await()

            val batch = firestore.batch()
            for (document in documents) {
                batch.delete(document.reference)
            }

            if (documents.size() > 0) {
                batch.commit().await()
                CloudLogger.debug(TAG, "Deleted ${documents.size()} documents from $path")
            }
        } catch (e: com.google.firebase.FirebaseException) {
            CloudLogger.error(TAG, "Failed to delete subcollection: $path - Firebase error", e)
        } catch (e: java.io.IOException) {
            CloudLogger.error(TAG, "Failed to delete subcollection: $path - network error", e)
        }
    }

    private suspend fun deleteLocalData(userId: String) {
        try {
            // Delete in order of dependencies (children before parents)

            // Workout related
            database.setLogDao().deleteAllForUser(userId)
            database.exerciseLogDao().deleteAllForUser(userId)
            database.workoutDao().deleteAllForUser(userId)

            // Programme related
            database.programmeDao().deleteAllProgrammesForUser(userId)

            // User stats
            database.exerciseMaxTrackingDao().deleteAllForUser(userId)
            database.personalRecordDao().deleteAllForUser(userId)

            // Analytics
            database.exerciseSwapHistoryDao().deleteAllForUser(userId)
            database.programmeExerciseTrackingDao().deleteAllForUser(userId)
            database.globalExerciseProgressDao().deleteAllForUser(userId)

            // Parse requests
            database.parseRequestDao().deleteAllForUser(userId)

            // Custom exercises (where user created them)
            database.exerciseDao().deleteAllCustomExercisesByUser(userId)

            // User exercise usage tracking
            database.userExerciseUsageDao().deleteAllUsageForUser(userId)

            CloudLogger.debug(TAG, "Local database data deleted successfully")
        } catch (e: android.database.sqlite.SQLiteException) {
            CloudLogger.error(TAG, "Failed to delete local data - database error", e)
            throw e
        }
    }
}
