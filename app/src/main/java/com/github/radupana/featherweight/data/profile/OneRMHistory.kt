package com.github.radupana.featherweight.data.profile

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.github.radupana.featherweight.util.IdGenerator
import java.time.LocalDateTime

@Entity(
    tableName = "one_rm_history",
    indices = [
        Index(value = ["exerciseId", "recordedAt"]),
        Index("userId"),
        Index(value = ["userId", "exerciseId", "sourceSetId"], unique = true),
    ],
)
data class OneRMHistory(
    @PrimaryKey
    val id: String = IdGenerator.generateId(),
    val userId: String? = null,
    val exerciseId: String,
    val oneRMEstimate: Float,
    val context: String,
    val sourceSetId: String? = null,
    val recordedAt: LocalDateTime = LocalDateTime.now(),
)
