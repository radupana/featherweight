package com.github.radupana.featherweight.data.exercise

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * ExerciseCore - System-level exercise grouping mechanism for variations.
 * Contains only reference data shared across all users.
 */
@Entity(
    tableName = "exercise_cores",
)
data class ExerciseCore(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String, // e.g., "Squat", "Deadlift", "Bench Press"
    val category: ExerciseCategory,
    val movementPattern: MovementPattern,
    val isCompound: Boolean,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)

/**
 * System-level exercise variation that can be logged.
 * Contains only reference data shared across all users.
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
        Index(value = ["name"], unique = true),
        Index(value = ["equipment"]),
    ],
)
data class ExerciseVariation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val coreExerciseId: Long,
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
    val variationId: Long,
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
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val variationId: Long, // Non-null, always references a variation
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
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val variationId: Long, // Non-null, always references a variation
    val alias: String,
    val confidence: Float = 1.0f,
    val languageCode: String = "en",
    val source: String = "manual",
)

/**
 * Relationships between variations (progressions, regressions, alternatives, etc.).
 */
@Entity(
    tableName = "variation_relations",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseVariation::class,
            parentColumns = ["id"],
            childColumns = ["fromVariationId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseVariation::class,
            parentColumns = ["id"],
            childColumns = ["toVariationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["fromVariationId"]),
        Index(value = ["toVariationId"]),
        Index(value = ["relationType"]),
    ],
)
data class VariationRelation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fromVariationId: Long, // Non-null
    val toVariationId: Long, // Non-null
    val relationType: ExerciseRelationType,
    val strength: Float = 1.0f,
    val notes: String? = null,
)
