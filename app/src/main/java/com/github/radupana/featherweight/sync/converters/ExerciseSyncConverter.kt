package com.github.radupana.featherweight.sync.converters

import com.github.radupana.featherweight.data.exercise.CustomExerciseCore
import com.github.radupana.featherweight.data.exercise.CustomExerciseVariation
import com.github.radupana.featherweight.data.exercise.Equipment
import com.github.radupana.featherweight.data.exercise.ExerciseCategory
import com.github.radupana.featherweight.data.exercise.ExerciseCore
import com.github.radupana.featherweight.data.exercise.ExerciseDifficulty
import com.github.radupana.featherweight.data.exercise.ExerciseVariation
import com.github.radupana.featherweight.data.exercise.InstructionType
import com.github.radupana.featherweight.data.exercise.MovementPattern
import com.github.radupana.featherweight.data.exercise.MuscleGroup
import com.github.radupana.featherweight.data.exercise.RMScalingType
import com.github.radupana.featherweight.data.exercise.VariationAlias
import com.github.radupana.featherweight.data.exercise.VariationInstruction
import com.github.radupana.featherweight.data.exercise.VariationMuscle
import com.github.radupana.featherweight.sync.models.FirestoreExercise
import com.github.radupana.featherweight.sync.models.FirestoreInstruction
import com.github.radupana.featherweight.sync.models.FirestoreMuscle
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Converts between denormalized Firestore exercise models and normalized SQLite entities.
 * This is critical infrastructure that handles bidirectional data transformation.
 */
class ExerciseSyncConverter {
    companion object {
        /**
         * Converts a denormalized Firestore exercise to normalized SQLite entities.
         * Returns a data class containing all the related entities.
         */
        fun fromFirestore(
            firestoreExercise: FirestoreExercise,
            exerciseId: String,
        ): ExerciseEntityBundle {
            // Generate stable IDs based on exercise name
            val coreId = generateStableId("core_${firestoreExercise.coreName}")
            val variationId = generateStableId("var_${firestoreExercise.name}")

            // Create ExerciseCore
            val exerciseCore =
                ExerciseCore(
                    id = coreId,
                    name = firestoreExercise.coreName,
                    category = parseExerciseCategory(firestoreExercise.coreCategory),
                    movementPattern = parseMovementPattern(firestoreExercise.coreMovementPattern),
                    isCompound = firestoreExercise.coreIsCompound,
                    createdAt = parseTimestamp(firestoreExercise.createdAt),
                    updatedAt = parseTimestamp(firestoreExercise.updatedAt),
                )

            // Create ExerciseVariation
            val exerciseVariation =
                ExerciseVariation(
                    id = variationId,
                    coreExerciseId = coreId,
                    name = firestoreExercise.name,
                    equipment = parseEquipment(firestoreExercise.equipment),
                    difficulty = parseDifficulty(firestoreExercise.difficulty),
                    requiresWeight = firestoreExercise.requiresWeight,
                    recommendedRepRange = firestoreExercise.recommendedRepRange,
                    rmScalingType = parseRMScalingType(firestoreExercise.rmScalingType),
                    restDurationSeconds = firestoreExercise.restDurationSeconds,
                    createdAt = parseTimestamp(firestoreExercise.createdAt),
                    updatedAt = parseTimestamp(firestoreExercise.updatedAt),
                )

            // Create VariationMuscles
            val variationMuscles =
                firestoreExercise.muscles.map { muscle ->
                    VariationMuscle(
                        variationId = variationId,
                        muscle = parseMuscleGroup(muscle.muscle),
                        isPrimary = muscle.isPrimary,
                        emphasisModifier = muscle.emphasisModifier.toFloat(),
                    )
                }

            // Create VariationAliases
            val variationAliases =
                firestoreExercise.aliases.mapIndexed { index, alias ->
                    VariationAlias(
                        id = generateStableId("alias_${variationId}_$alias"),
                        variationId = variationId,
                        alias = alias,
                        confidence = 1.0f,
                        languageCode = "en",
                        source = "firestore",
                    )
                }

            // Create VariationInstructions
            val variationInstructions =
                firestoreExercise.instructions.map { instruction ->
                    VariationInstruction(
                        id = generateStableId("inst_${variationId}_${instruction.orderIndex}"),
                        variationId = variationId,
                        instructionType = parseInstructionType(instruction.type),
                        content = instruction.content,
                        orderIndex = instruction.orderIndex,
                        languageCode = instruction.languageCode,
                    )
                }

            return ExerciseEntityBundle(
                exerciseCore = exerciseCore,
                exerciseVariation = exerciseVariation,
                variationMuscles = variationMuscles,
                variationAliases = variationAliases,
                variationInstructions = variationInstructions,
                firestoreId = exerciseId,
            )
        }

        /**
         * Converts normalized SQLite entities to a denormalized Firestore exercise.
         * Used for syncing custom exercises to Firestore.
         */
        fun toFirestore(
            core: CustomExerciseCore,
            variation: CustomExerciseVariation,
            muscles: List<VariationMuscle>,
            aliases: List<VariationAlias>,
            instructions: List<VariationInstruction>,
        ): FirestoreExercise =
            FirestoreExercise(
                coreName = core.name,
                coreCategory = core.category.name,
                coreMovementPattern = core.movementPattern.name,
                coreIsCompound = core.isCompound,
                name = variation.name,
                equipment = variation.equipment.name,
                difficulty = variation.difficulty.name,
                requiresWeight = variation.requiresWeight,
                recommendedRepRange = variation.recommendedRepRange,
                rmScalingType = variation.rmScalingType.name,
                restDurationSeconds = variation.restDurationSeconds,
                muscles =
                    muscles.map { variationMuscle ->
                        FirestoreMuscle(
                            variationMuscle.muscle.name,
                            variationMuscle.isPrimary,
                            variationMuscle.emphasisModifier.toDouble(),
                        )
                    },
                aliases = aliases.map { it.alias },
                instructions =
                    instructions.map { instruction ->
                        FirestoreInstruction(
                            type = instruction.instructionType.name,
                            content = instruction.content,
                            orderIndex = instruction.orderIndex,
                            languageCode = instruction.languageCode,
                        )
                    },
                createdAt = formatTimestamp(core.createdAt),
                updatedAt = formatTimestamp(core.updatedAt),
            )

        /**
         * Generates a stable ID based on a seed string.
         * This ensures the same exercise always gets the same ID.
         */
        private fun generateStableId(seed: String): Long = seed.hashCode().toLong() and 0x7FFFFFFF

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
                ExerciseCategory.FULL_BODY
            }

        private fun parseMovementPattern(value: String): MovementPattern =
            try {
                MovementPattern.valueOf(value)
            } catch (e: IllegalArgumentException) {
                MovementPattern.PUSH
            }

        private fun parseEquipment(value: String): Equipment =
            try {
                Equipment.valueOf(value)
            } catch (e: IllegalArgumentException) {
                Equipment.NONE
            }

        private fun parseDifficulty(value: String): ExerciseDifficulty =
            try {
                ExerciseDifficulty.valueOf(value)
            } catch (e: IllegalArgumentException) {
                ExerciseDifficulty.INTERMEDIATE
            }

        private fun parseRMScalingType(value: String): RMScalingType =
            try {
                RMScalingType.valueOf(value)
            } catch (e: IllegalArgumentException) {
                RMScalingType.STANDARD
            }

        private fun parseMuscleGroup(value: String): MuscleGroup =
            try {
                MuscleGroup.valueOf(value)
            } catch (e: IllegalArgumentException) {
                MuscleGroup.FULL_BODY
            }

        private fun parseInstructionType(value: String): InstructionType =
            try {
                InstructionType.valueOf(value)
            } catch (e: IllegalArgumentException) {
                InstructionType.EXECUTION
            }
    }

    /**
     * Bundle containing all normalized entities for a single exercise.
     */
    data class ExerciseEntityBundle(
        val exerciseCore: ExerciseCore,
        val exerciseVariation: ExerciseVariation,
        val variationMuscles: List<VariationMuscle>,
        val variationAliases: List<VariationAlias>,
        val variationInstructions: List<VariationInstruction>,
        val firestoreId: String,
    )
}
