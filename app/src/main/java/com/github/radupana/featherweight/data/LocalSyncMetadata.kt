package com.github.radupana.featherweight.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import java.time.LocalDateTime

/**
 * Local sync metadata tracking per user, installation, and data type.
 * This allows each device installation to maintain its own sync state.
 */
@Entity(
    tableName = "local_sync_metadata",
    primaryKeys = ["user_id", "installation_id", "data_type"],
)
data class LocalSyncMetadata(
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "installation_id")
    val installationId: String,
    @ColumnInfo(name = "data_type")
    val dataType: String, // e.g., "exercises", "workouts", "programmes", etc.
    @ColumnInfo(name = "last_sync_time")
    val lastSyncTime: LocalDateTime? = null,
    @ColumnInfo(name = "last_successful_sync")
    val lastSuccessfulSync: LocalDateTime? = null,
    @ColumnInfo(name = "sync_error")
    val syncError: String? = null,
)
