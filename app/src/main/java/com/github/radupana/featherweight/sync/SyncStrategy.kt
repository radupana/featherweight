package com.github.radupana.featherweight.sync

import com.google.firebase.Timestamp

/**
 * Strategy interface for syncing different types of data.
 * Each implementation handles a specific data type's sync logic.
 */
interface SyncStrategy {
    /**
     * Downloads and merges remote data with local database.
     * @param userId The user ID for user-specific data (null for system data)
     * @param lastSyncTime The last time this data was synced (null for full sync)
     * @return Result indicating success or failure
     */
    suspend fun downloadAndMerge(
        userId: String?,
        lastSyncTime: Timestamp?,
    ): Result<Unit>

    /**
     * Uploads local changes to remote storage.
     * @param userId The user ID for user-specific data (null for system data)
     * @param lastSyncTime The last time this data was synced
     * @return Result indicating success or failure
     */
    suspend fun uploadChanges(
        userId: String?,
        lastSyncTime: Timestamp?,
    ): Result<Unit>

    /**
     * Returns the type of data this strategy handles.
     */
    fun getDataType(): String
}

/**
 * Factory for creating sync strategies.
 */
interface SyncStrategyFactory {
    fun createSystemExerciseSyncStrategy(): SyncStrategy

    fun createUserDataSyncStrategy(): SyncStrategy

    fun createCustomExerciseSyncStrategy(): SyncStrategy
}
