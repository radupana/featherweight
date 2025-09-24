package com.github.radupana.featherweight.data.exercise

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Custom exercise core created by a user.
 * Stored separately from system exercises to maintain clean data boundaries.
 */
@Entity(
    tableName = "custom_exercise_cores",
    indices = [
        Index("userId"),
        Index("name"),
    ],
)
data class CustomExerciseCore(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val name: String,
    val category: ExerciseCategory,
    val movementPattern: MovementPattern,
    val isCompound: Boolean,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)

/**
 * Custom exercise variation created by a user.
 * References CustomExerciseCore instead of system ExerciseCore.
 */
@Entity(
    tableName = "custom_exercise_variations",
    foreignKeys = [
        ForeignKey(
            entity = CustomExerciseCore::class,
            parentColumns = ["id"],
            childColumns = ["customCoreExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("userId"),
        Index("customCoreExerciseId"),
        Index("name"),
        Index("equipment"),
    ],
)
data class CustomExerciseVariation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val customCoreExerciseId: Long,
    val name: String,
    val equipment: Equipment,
    val difficulty: ExerciseDifficulty,
    val requiresWeight: Boolean,
    val recommendedRepRange: String? = null,
    val rmScalingType: RMScalingType = RMScalingType.STANDARD,
    val restDurationSeconds: Int = 90,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)
