package com.github.radupana.featherweight.data.exercise

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.github.radupana.featherweight.util.IdGenerator
import java.time.LocalDateTime

/**
 * Exercise entity representing both system and custom exercises.
 * This is the single source of truth for all exercises in the system.
 *
 * The type field explicitly indicates whether this is a SYSTEM or USER exercise.
 * userId is only populated for USER type exercises.
 */
@Entity(
    tableName = "exercises",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["type"]),
        Index(value = ["name"]),
        Index(value = ["category"]),
        Index(value = ["equipment"]),
        Index(value = ["userId", "name"], unique = true),
    ],
)
data class Exercise(
    @PrimaryKey
    val id: String = IdGenerator.generateId(),
    // Exercise type (SYSTEM or USER)
    val type: String = ExerciseType.SYSTEM.name,
    // User association (only for USER type exercises)
    val userId: String? = null,
    // Core exercise properties (formerly from ExerciseCore)
    val name: String,
    val category: String, // Was ExerciseCategory enum
    val movementPattern: String?, // Was MovementPattern enum
    val isCompound: Boolean = true,
    // Variation-specific properties
    val equipment: String, // Was Equipment enum
    val difficulty: String? = null, // Was ExerciseDifficulty enum
    val requiresWeight: Boolean = true,
    val rmScalingType: String? = null, // Was RMScalingType enum
    val restDurationSeconds: Int? = null,
    // Timestamps
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    // Soft delete flag for sync
    val isDeleted: Boolean = false,
)
