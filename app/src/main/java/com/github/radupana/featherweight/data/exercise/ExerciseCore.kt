package com.github.radupana.featherweight.data.exercise

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.github.radupana.featherweight.util.IdGenerator
import java.time.LocalDateTime

/**
 * ExerciseCore - Unified exercise grouping for both system and custom exercises.
 * userId = null for system exercises, non-null for custom exercises.
 */
@Entity(
    tableName = "exercise_cores",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["userId", "name"], unique = true),
    ],
)
data class ExerciseCore(
    @PrimaryKey
    val id: String = IdGenerator.generateId(),
    val userId: String? = null, // null = system exercise, non-null = custom exercise
    val name: String, // e.g., "Squat", "Deadlift", "Bench Press"
    val category: ExerciseCategory,
    val movementPattern: MovementPattern,
    val isCompound: Boolean,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)

/**
 * Exercise variation - unified for both system and custom exercises.
 * userId = null for system exercises, non-null for custom exercises.
 */
@Entity(
    tableName = "exercise_variations",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseCore::class,
            parentColumns = ["id"],
            childColumns = ["coreExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["coreExerciseId"]),
        Index(value = ["equipment"]),
        Index(value = ["userId"]),
        Index(value = ["userId", "name"], unique = true),
    ],
)
data class ExerciseVariation(
    @PrimaryKey
    val id: String = IdGenerator.generateId(),
    val userId: String? = null, // null = system exercise, non-null = custom exercise
    val coreExerciseId: String,
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

/**
 * Join table for variation muscle groups.
 */
@Entity(
    tableName = "variation_muscles",
    primaryKeys = ["variationId", "muscle"],
    foreignKeys = [
        ForeignKey(
            entity = ExerciseVariation::class,
            parentColumns = ["id"],
            childColumns = ["variationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["variationId"]),
        Index(value = ["muscle"]),
    ],
)
data class VariationMuscle(
    val variationId: String,
    val muscle: MuscleGroup,
    val isPrimary: Boolean,
    val emphasisModifier: Float = 1.0f, // How much this variation emphasizes this muscle
)

/**
 * Instructions for a specific variation.
 */
@Entity(
    tableName = "variation_instructions",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseVariation::class,
            parentColumns = ["id"],
            childColumns = ["variationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["variationId"]),
        Index(value = ["instructionType"]),
    ],
)
data class VariationInstruction(
    @PrimaryKey
    val id: String = IdGenerator.generateId(),
    val variationId: String, // Non-null, always references a variation
    val instructionType: InstructionType,
    val content: String,
    val orderIndex: Int = 0,
    val languageCode: String = "en",
)

/**
 * Aliases for a specific variation.
 */
@Entity(
    tableName = "variation_aliases",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseVariation::class,
            parentColumns = ["id"],
            childColumns = ["variationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["variationId"]),
        Index(value = ["alias"]),
        Index(value = ["languageCode"]),
    ],
)
data class VariationAlias(
    @PrimaryKey
    val id: String = IdGenerator.generateId(),
    val variationId: String, // Non-null, always references a variation
    val alias: String,
    val confidence: Float = 1.0f,
    val languageCode: String = "en",
    val source: String = "manual",
)
