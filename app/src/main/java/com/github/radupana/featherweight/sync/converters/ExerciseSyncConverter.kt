package com.github.radupana.featherweight.sync.converters

import android.util.Log
import com.github.radupana.featherweight.data.exercise.Equipment
import com.github.radupana.featherweight.data.exercise.Exercise
import com.github.radupana.featherweight.data.exercise.ExerciseAlias
import com.github.radupana.featherweight.data.exercise.ExerciseCategory
import com.github.radupana.featherweight.data.exercise.ExerciseDifficulty
import com.github.radupana.featherweight.data.exercise.ExerciseInstruction
import com.github.radupana.featherweight.data.exercise.ExerciseMuscle
import com.github.radupana.featherweight.data.exercise.ExerciseType
import com.github.radupana.featherweight.data.exercise.InstructionType
import com.github.radupana.featherweight.data.exercise.MovementPattern
import com.github.radupana.featherweight.data.exercise.MuscleGroup
import com.github.radupana.featherweight.data.exercise.RMScalingType
import com.github.radupana.featherweight.sync.models.FirestoreExercise
import com.github.radupana.featherweight.sync.models.FirestoreInstruction
import com.github.radupana.featherweight.sync.models.FirestoreMuscle
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * Converts between denormalized Firestore exercise models and normalized SQLite entities.
 * This is critical infrastructure that handles bidirectional data transformation.
 */
class ExerciseSyncConverter {
    companion object {
        private const val TAG = "ExerciseSyncConverter"

        /**
         * Converts a denormalized Firestore exercise to SQLite entities.
         * Returns a data class containing all the related entities.
         */
        fun fromFirestore(
            firestoreExercise: FirestoreExercise,
            firestoreId: String,
        ): ExerciseEntityBundle {
            // Generate stable IDs based on exercise name
            val exerciseId = generateStableId("var_${firestoreExercise.name}")

            // Create Exercise with all fields - system exercises have type = SYSTEM and userId = null
            val exercise =
                Exercise(
                    id = exerciseId,
                    type = ExerciseType.SYSTEM.name,
                    userId = null,
                    name = firestoreExercise.name,
                    category = parseExerciseCategory(firestoreExercise.coreCategory).name,
                    movementPattern = parseMovementPattern(firestoreExercise.coreMovementPattern).name,
                    isCompound = firestoreExercise.coreIsCompound,
                    equipment = parseEquipment(firestoreExercise.equipment).name,
                    difficulty = parseDifficulty(firestoreExercise.difficulty)?.name,
                    requiresWeight = firestoreExercise.requiresWeight,
                    rmScalingType = parseRMScalingType(firestoreExercise.rmScalingType)?.name,
                    restDurationSeconds = firestoreExercise.restDurationSeconds,
                    createdAt = parseTimestamp(firestoreExercise.createdAt),
                    updatedAt = parseTimestamp(firestoreExercise.updatedAt),
                )

            // Create ExerciseMuscles
            val exerciseMuscles =
                firestoreExercise.muscles.map { muscle ->
                    ExerciseMuscle(
                        exerciseId = exerciseId,
                        muscle = parseMuscleGroup(muscle.muscle).name,
                        targetType = if (muscle.isPrimary) "primary" else "secondary",
                    )
                }

            // Create ExerciseAliases
            val exerciseAliases =
                firestoreExercise.aliases.mapIndexed { index, alias ->
                    ExerciseAlias(
                        id = generateStableId("alias_${exerciseId}_$alias"),
                        exerciseId = exerciseId,
                        alias = alias,
                    )
                }

            // Create ExerciseInstructions
            val exerciseInstructions =
                firestoreExercise.instructions.map { instruction ->
                    ExerciseInstruction(
                        id = generateStableId("inst_${exerciseId}_${instruction.orderIndex}"),
                        exerciseId = exerciseId,
                        instructionType = parseInstructionType(instruction.type).name,
                        instructionText = instruction.content,
                        orderIndex = instruction.orderIndex,
                    )
                }

            return ExerciseEntityBundle(
                exercise = exercise,
                exerciseMuscles = exerciseMuscles,
                exerciseAliases = exerciseAliases,
                exerciseInstructions = exerciseInstructions,
                firestoreId = firestoreId,
            )
        }

        /**
         * Converts SQLite entities to a denormalized Firestore exercise.
         * Used for syncing exercises to Firestore.
         */
        fun toFirestore(
            exercise: Exercise,
            muscles: List<ExerciseMuscle>,
            aliases: List<ExerciseAlias>,
            instructions: List<ExerciseInstruction>,
        ): FirestoreExercise =
            FirestoreExercise(
                coreName = exercise.name,
                coreCategory = exercise.category,
                coreMovementPattern = exercise.movementPattern ?: "PUSH",
                coreIsCompound = exercise.isCompound,
                name = exercise.name,
                equipment = exercise.equipment,
                difficulty = exercise.difficulty ?: "BEGINNER",
                requiresWeight = exercise.requiresWeight,
                recommendedRepRange = null, // This field doesn't exist anymore
                rmScalingType = exercise.rmScalingType ?: "STANDARD",
                restDurationSeconds = exercise.restDurationSeconds ?: 90,
                muscles =
                    muscles.map { exerciseMuscle ->
                        FirestoreMuscle(
                            exerciseMuscle.muscle,
                            isPrimary = exerciseMuscle.targetType == "primary",
                            emphasisModifier = 1.0, // Default value
                        )
                    },
                aliases = aliases.map { it.alias },
                instructions =
                    instructions.map { instruction ->
                        FirestoreInstruction(
                            type = instruction.instructionType,
                            content = instruction.instructionText,
                            orderIndex = instruction.orderIndex,
                            languageCode = "en", // Default value
                        )
                    },
                createdAt = formatTimestamp(exercise.createdAt),
                updatedAt = formatTimestamp(exercise.updatedAt),
            )

        /**
         * Generates a stable UUID based on a seed string.
         * This ensures the same exercise always gets the same UUID.
         * Uses UUID v5 (name-based) for deterministic generation.
         */
        private fun generateStableId(seed: String): String {
            // Use a namespace UUID for Featherweight exercises
            val namespace = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8")
            return UUID
                .nameUUIDFromBytes(
                    (namespace.toString() + seed).toByteArray(Charsets.UTF_8),
                ).toString()
        }

        private fun parseTimestamp(timestamp: String?): LocalDateTime =
            timestamp?.let {
                // Parse as Instant first (handles Z timezone), then convert to LocalDateTime
                val instant = java.time.Instant.parse(it)
                LocalDateTime.ofInstant(instant, ZoneOffset.UTC)
            } ?: LocalDateTime.now()

        private fun formatTimestamp(dateTime: LocalDateTime): String = dateTime.toInstant(ZoneOffset.UTC).toString()

        private fun parseExerciseCategory(value: String): ExerciseCategory =
            try {
                ExerciseCategory.valueOf(value)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Invalid ExerciseCategory value: $value, defaulting to OTHER", e)
                ExerciseCategory.OTHER
            }

        private fun parseMovementPattern(value: String): MovementPattern =
            try {
                // Handle legacy values from old schema and removed redundant values
                when (value) {
                    "COMPOUND" -> MovementPattern.PUSH
                    "ISOLATION" -> MovementPattern.EXTENSION
                    "POWER" -> MovementPattern.PUSH
                    "STABILIZATION" -> MovementPattern.ISOMETRIC
                    "FLEXION" -> MovementPattern.PULL
                    "PRESS" -> MovementPattern.PUSH
                    "ROW" -> MovementPattern.PULL
                    "CURL" -> MovementPattern.PULL
                    "FLY" -> MovementPattern.HORIZONTAL_PUSH
                    "RAISE" -> MovementPattern.PUSH
                    "SHRUG" -> MovementPattern.PULL
                    "CRUNCH" -> MovementPattern.CORE
                    "HOLD" -> MovementPattern.ISOMETRIC
                    "WALK", "STEP", "CYCLE", "CRAWL" -> MovementPattern.LOCOMOTION
                    "COMPLEX", "EXPLOSIVE", "LIFT" -> MovementPattern.OTHER
                    "PIKE", "TUCK", "ROLL" -> MovementPattern.CORE
                    "KICK", "FLIP", "CIRCLE", "WAVE" -> MovementPattern.OTHER
                    else -> MovementPattern.valueOf(value)
                }
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Invalid MovementPattern value: $value, defaulting to OTHER", e)
                MovementPattern.OTHER
            }

        private fun parseEquipment(value: String): Equipment =
            try {
                // Handle legacy values from old schema
                when (value) {
                    "RESISTANCE_BAND" -> Equipment.BAND
                    "CAR_DEADLIFT" -> Equipment.MACHINE
                    else -> Equipment.valueOf(value)
                }
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Invalid Equipment value: $value, defaulting to NONE", e)
                Equipment.NONE
            }

        private fun parseDifficulty(value: String?): ExerciseDifficulty? =
            value?.let {
                try {
                    ExerciseDifficulty.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    Log.w(TAG, "Invalid ExerciseDifficulty value: $it, defaulting to INTERMEDIATE", e)
                    ExerciseDifficulty.INTERMEDIATE
                }
            }

        private fun parseRMScalingType(value: String?): RMScalingType? =
            value?.let {
                try {
                    // Handle legacy values from old schema
                    when (it) {
                        "OLYMPIC" -> RMScalingType.STANDARD // Legacy Olympic lifts default to STANDARD
                        else -> RMScalingType.valueOf(it)
                    }
                } catch (e: IllegalArgumentException) {
                    Log.w(TAG, "Invalid RMScalingType value: $it, defaulting to STANDARD", e)
                    RMScalingType.STANDARD
                }
            }

        private fun parseMuscleGroup(value: String): MuscleGroup =
            try {
                // Handle legacy values from old schema
                when (value) {
                    "PECTORALS" -> MuscleGroup.CHEST
                    "ABS" -> MuscleGroup.CORE
                    else -> MuscleGroup.valueOf(value)
                }
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Invalid MuscleGroup value: $value, defaulting to OTHER", e)
                MuscleGroup.OTHER
            }

        private fun parseInstructionType(value: String): InstructionType =
            try {
                InstructionType.valueOf(value)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Invalid InstructionType value: $value, defaulting to EXECUTION", e)
                InstructionType.EXECUTION
            }
    }

    /**
     * Bundle containing all entities for a single exercise.
     */
    data class ExerciseEntityBundle(
        val exercise: Exercise,
        val exerciseMuscles: List<ExerciseMuscle>,
        val exerciseAliases: List<ExerciseAlias>,
        val exerciseInstructions: List<ExerciseInstruction>,
        val firestoreId: String,
    )
}
