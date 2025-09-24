package com.github.radupana.featherweight.data.exercise

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Tracks user-specific exercise usage statistics and preferences.
 */
@Entity(
    tableName = "user_exercise_usage",
    indices = [
        Index(value = ["userId", "exerciseVariationId", "isCustomExercise"], unique = true),
        Index(value = ["userId", "usageCount"]),
        Index(value = ["userId", "lastUsedAt"]),
        Index(value = ["userId", "favorited"]),
    ],
)
data class UserExerciseUsage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val exerciseVariationId: Long,
    val isCustomExercise: Boolean = false,
    val usageCount: Int = 0,
    val favorited: Boolean = false,
    val lastUsedAt: LocalDateTime? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)
