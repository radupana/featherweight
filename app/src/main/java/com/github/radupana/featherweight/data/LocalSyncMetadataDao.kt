package com.github.radupana.featherweight.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.time.LocalDateTime

@Dao
interface LocalSyncMetadataDao {
    @Query(
        """
        SELECT * FROM local_sync_metadata
        WHERE user_id = :userId
        AND installation_id = :installationId
        AND data_type = :dataType
        """,
    )
    suspend fun getSyncMetadata(
        userId: String,
        installationId: String,
        dataType: String,
    ): LocalSyncMetadata?

    @Query(
        """
        SELECT last_sync_time FROM local_sync_metadata
        WHERE user_id = :userId
        AND installation_id = :installationId
        AND data_type = :dataType
        """,
    )
    suspend fun getLastSyncTime(
        userId: String,
        installationId: String,
        dataType: String,
    ): LocalDateTime?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(metadata: LocalSyncMetadata)

    @Query(
        """
        UPDATE local_sync_metadata
        SET last_sync_time = :syncTime,
            last_successful_sync = :syncTime,
            sync_error = NULL
        WHERE user_id = :userId
        AND installation_id = :installationId
        AND data_type = :dataType
        """,
    )
    suspend fun updateSuccessfulSync(
        userId: String,
        installationId: String,
        dataType: String,
        syncTime: LocalDateTime,
    )

    @Query(
        """
        UPDATE local_sync_metadata
        SET sync_error = :error
        WHERE user_id = :userId
        AND installation_id = :installationId
        AND data_type = :dataType
        """,
    )
    suspend fun updateSyncError(
        userId: String,
        installationId: String,
        dataType: String,
        error: String,
    )

    @Query(
        """
        SELECT COUNT(*) > 0 FROM local_sync_metadata
        WHERE user_id = :userId
        AND installation_id = :installationId
        AND last_successful_sync IS NOT NULL
        """,
    )
    suspend fun hasDeviceEverSynced(
        userId: String,
        installationId: String,
    ): Boolean

    @Query(
        """
        DELETE FROM local_sync_metadata
        WHERE user_id = :userId
        """,
    )
    suspend fun clearUserSyncMetadata(userId: String)

    @Query(
        """
        DELETE FROM local_sync_metadata
        WHERE user_id = :userId
        AND installation_id = :installationId
        """,
    )
    suspend fun clearDeviceSyncMetadata(
        userId: String,
        installationId: String,
    )
}
