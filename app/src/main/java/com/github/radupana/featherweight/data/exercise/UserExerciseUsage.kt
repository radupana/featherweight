package com.github.radupana.featherweight.data.exercise

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.github.radupana.featherweight.util.IdGenerator
import java.time.LocalDateTime

/**
 * Tracks user-specific exercise usage statistics and preferences.
 */
@Entity(
    tableName = "user_exercise_usage",
    indices = [
        Index(value = ["userId", "exerciseId"], unique = true),
        Index(value = ["userId", "usageCount"]),
        Index(value = ["userId", "lastUsedAt"]),
    ],
)
data class UserExerciseUsage(
    @PrimaryKey
    val id: String = IdGenerator.generateId(),
    val userId: String,
    val exerciseId: String,
    val usageCount: Int = 0,
    val lastUsedAt: LocalDateTime? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)
