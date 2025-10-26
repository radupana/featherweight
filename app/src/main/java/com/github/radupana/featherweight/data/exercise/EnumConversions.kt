package com.github.radupana.featherweight.data.exercise

import com.github.radupana.featherweight.util.CloudLogger

/**
 * Extension functions to safely convert Strings to their corresponding enum types.
 * These functions are used when retrieving data from the database (String) and
 * converting it back to enums for use in business logic and UI.
 */

private const val TAG = "EnumConversions"

// Equipment conversions
fun String?.toEquipment(): Equipment =
    this?.let {
        try {
            Equipment.valueOf(it)
        } catch (e: IllegalArgumentException) {
            CloudLogger.debug(TAG, "Invalid Equipment value: '$it', using default NONE", e)
            Equipment.NONE
        }
    } ?: Equipment.NONE

fun String?.toEquipmentOrNull(): Equipment? =
    this?.let {
        try {
            Equipment.valueOf(it)
        } catch (e: IllegalArgumentException) {
            CloudLogger.debug(TAG, "Invalid Equipment value: '$it', returning null", e)
            null
        }
    }

// ExerciseCategory conversions
fun String?.toExerciseCategory(): ExerciseCategory =
    this?.let {
        try {
            ExerciseCategory.valueOf(it)
        } catch (e: IllegalArgumentException) {
            CloudLogger.debug(TAG, "Invalid ExerciseCategory value: '$it', using default OTHER", e)
            ExerciseCategory.OTHER
        }
    } ?: ExerciseCategory.OTHER

fun String?.toExerciseCategoryOrNull(): ExerciseCategory? =
    this?.let {
        try {
            ExerciseCategory.valueOf(it)
        } catch (e: IllegalArgumentException) {
            CloudLogger.debug(TAG, "Invalid ExerciseCategory value: '$it', returning null", e)
            null
        }
    }

// MovementPattern conversions
fun String?.toMovementPattern(): MovementPattern =
    this?.let {
        try {
            MovementPattern.valueOf(it)
        } catch (e: IllegalArgumentException) {
            CloudLogger.debug(TAG, "Invalid MovementPattern value: '$it', using default OTHER", e)
            MovementPattern.OTHER
        }
    } ?: MovementPattern.OTHER

fun String?.toMovementPatternOrNull(): MovementPattern? =
    this?.let {
        try {
            MovementPattern.valueOf(it)
        } catch (e: IllegalArgumentException) {
            CloudLogger.debug(TAG, "Invalid MovementPattern value: '$it', returning null", e)
            null
        }
    }

// ExerciseDifficulty conversions
fun String?.toExerciseDifficulty(): ExerciseDifficulty =
    this?.let {
        try {
            ExerciseDifficulty.valueOf(it)
        } catch (e: IllegalArgumentException) {
            CloudLogger.debug(TAG, "Invalid ExerciseDifficulty value: '$it', using default BEGINNER", e)
            ExerciseDifficulty.BEGINNER
        }
    } ?: ExerciseDifficulty.BEGINNER

fun String?.toExerciseDifficultyOrNull(): ExerciseDifficulty? =
    this?.let {
        try {
            ExerciseDifficulty.valueOf(it)
        } catch (e: IllegalArgumentException) {
            CloudLogger.debug(TAG, "Invalid ExerciseDifficulty value: '$it', returning null", e)
            null
        }
    }

// RMScalingType conversions
fun String?.toRMScalingType(): RMScalingType =
    this?.let {
        try {
            RMScalingType.valueOf(it)
        } catch (e: IllegalArgumentException) {
            CloudLogger.debug(TAG, "Invalid RMScalingType value: '$it', using default UNKNOWN", e)
            RMScalingType.UNKNOWN
        }
    } ?: RMScalingType.UNKNOWN

fun String?.toRMScalingTypeOrNull(): RMScalingType? =
    this?.let {
        try {
            RMScalingType.valueOf(it)
        } catch (e: IllegalArgumentException) {
            CloudLogger.debug(TAG, "Invalid RMScalingType value: '$it', returning null", e)
            null
        }
    }

// MuscleGroup conversions
fun String?.toMuscleGroup(): MuscleGroup =
    this?.let {
        try {
            MuscleGroup.valueOf(it)
        } catch (e: IllegalArgumentException) {
            CloudLogger.debug(TAG, "Invalid MuscleGroup value: '$it', using default OTHER", e)
            MuscleGroup.OTHER
        }
    } ?: MuscleGroup.OTHER

fun String?.toMuscleGroupOrNull(): MuscleGroup? =
    this?.let {
        try {
            MuscleGroup.valueOf(it)
        } catch (e: IllegalArgumentException) {
            CloudLogger.debug(TAG, "Invalid MuscleGroup value: '$it', returning null", e)
            null
        }
    }

// InstructionType conversions
fun String?.toInstructionType(): InstructionType =
    this?.let {
        try {
            InstructionType.valueOf(it)
        } catch (e: IllegalArgumentException) {
            CloudLogger.debug(TAG, "Invalid InstructionType value: '$it', using default EXECUTION", e)
            InstructionType.EXECUTION
        }
    } ?: InstructionType.EXECUTION

fun String?.toInstructionTypeOrNull(): InstructionType? =
    this?.let {
        try {
            InstructionType.valueOf(it)
        } catch (e: IllegalArgumentException) {
            CloudLogger.debug(TAG, "Invalid InstructionType value: '$it', returning null", e)
            null
        }
    }

// Extension to convert Exercise entity to domain model with enums
fun Exercise.toDomainModel(): ExerciseDomain =
    ExerciseDomain(
        id = this.id,
        userId = this.userId,
        name = this.name,
        category = this.category.toExerciseCategory(),
        movementPattern = this.movementPattern.toMovementPatternOrNull(),
        isCompound = this.isCompound,
        equipment = this.equipment.toEquipment(),
        difficulty = this.difficulty.toExerciseDifficultyOrNull(),
        requiresWeight = this.requiresWeight,
        rmScalingType = this.rmScalingType.toRMScalingTypeOrNull(),
        restDurationSeconds = this.restDurationSeconds,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
    )

// Domain model that uses enums (for ViewModels and UI)
data class ExerciseDomain(
    val id: String,
    val userId: String?,
    val name: String,
    val category: ExerciseCategory,
    val movementPattern: MovementPattern?,
    val isCompound: Boolean,
    val equipment: Equipment,
    val difficulty: ExerciseDifficulty?,
    val requiresWeight: Boolean,
    val rmScalingType: RMScalingType?,
    val restDurationSeconds: Int?,
    val createdAt: java.time.LocalDateTime,
    val updatedAt: java.time.LocalDateTime,
)
