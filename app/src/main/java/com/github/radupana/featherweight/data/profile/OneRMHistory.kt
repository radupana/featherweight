package com.github.radupana.featherweight.data.profile

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "one_rm_history",
    // Note: Removed foreign key since it can reference either system or custom exercises
    indices = [
        Index(value = ["exerciseVariationId", "recordedAt"]),
        Index("userId"),
    ],
)
data class OneRMHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String? = null,
    val exerciseVariationId: Long, // Can reference either system or custom exercise
    val isCustomExercise: Boolean = false, // true = custom exercise, false = system exercise
    val oneRMEstimate: Float,
    val context: String, // e.g., "140kg × 3 @ RPE 8"
    val recordedAt: LocalDateTime = LocalDateTime.now(),
)
