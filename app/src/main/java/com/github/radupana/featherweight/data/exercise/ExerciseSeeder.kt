package com.github.radupana.featherweight.data.exercise

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDateTime

@Serializable
data class ExerciseData(
    val id: Long,
    val name: String,
    val category: String,
    val equipment: String,
    val muscleGroup: String,
    val movementPattern: String,
    val type: String = "STRENGTH",
    val difficulty: String = "BEGINNER",
    val requiresWeight: Boolean = true,
    val instructions: String? = null,
    val aliases: List<String> = emptyList(),
)

@Serializable
data class ExercisesJson(
    val exercises: List<ExerciseData>,
)

/**
 * Seeds exercise data into the new normalized database schema.
 * Creates ExerciseCore, ExerciseVariation, VariationMuscle, and VariationAlias entries.
 */
class ExerciseSeeder(
    private val exerciseDao: ExerciseDao,
    private val exerciseCoreDao: ExerciseCoreDao,
    private val exerciseVariationDao: ExerciseVariationDao,
    private val variationMuscleDao: VariationMuscleDao,
    private val variationAliasDao: VariationAliasDao,
    private val variationInstructionDao: VariationInstructionDao,
    private val context: Context? = null,
) {
    companion object {
        private const val TAG = "ExerciseSeeder"
    }
    private val json =
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

    suspend fun seedExercises() =
        withContext(Dispatchers.IO) {
            // Check if already seeded
            val existingCount = exerciseDao.getAllExercises().size
            if (existingCount > 0) {
                return@withContext
            }

            if (context == null) {
                error(
                    "Cannot initialize exercise database: Context is null. App cannot function without proper exercise data.",
                )
            }

            try {
                // Load from bundled JSON
                val inputStream = context.assets.open("exercises.json")
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                val exercisesData = json.decodeFromString<ExercisesJson>(jsonString)

                // Group exercises by base movement pattern to create cores
                val coreGroups =
                    exercisesData.exercises.groupBy { data ->
                        // Extract core exercise name (e.g., "Barbell Bench Press" -> "Bench Press")
                        extractCoreExerciseName(data.name)
                    }

                // Process each core group
                coreGroups.forEach { (coreName, variations) ->
                    if (coreName.isNotBlank()) {
                        // Get the first variation to extract core properties
                        val firstVariation = variations.first()

                        // Create the core exercise
                        val coreExercise =
                            ExerciseCore(
                                id = 0, // Auto-generate
                                name = coreName,
                                category = ExerciseCategory.valueOf(firstVariation.category.uppercase().replace(" ", "_")),
                                movementPattern = parseMovementPattern(firstVariation.movementPattern),
                                isCompound = isCompoundExercise(coreName),
                                createdAt = LocalDateTime.now(),
                                updatedAt = LocalDateTime.now(),
                            )

                        val coreId = exerciseCoreDao.insertExerciseCore(coreExercise)

                        // Create variations for this core
                        variations.forEach { data ->
                            val rmScalingType = determineRMScalingType(data.name)
                            
                            val variation =
                                ExerciseVariation(
                                    id = 0, // Auto-generate
                                    coreExerciseId = coreId,
                                    name = data.name,
                                    equipment = parseEquipment(data.equipment),
                                    difficulty = ExerciseDifficulty.valueOf(data.difficulty.uppercase()),
                                    requiresWeight = data.requiresWeight,
                                    recommendedRepRange = getRecommendedRepRange(data.difficulty),
                                    rmScalingType = rmScalingType,
                                    usageCount = 0,
                                    isCustom = false,
                                    createdBy = null,
                                    createdAt = LocalDateTime.now(),
                                    updatedAt = LocalDateTime.now(),
                                )

                            val variationId = exerciseVariationDao.insertExerciseVariation(variation)

                            // Add muscle mappings
                            val muscleGroup = parseMuscleGroup(data.muscleGroup)
                            val variationMuscle =
                                VariationMuscle(
                                    variationId = variationId,
                                    muscle = muscleGroup,
                                    isPrimary = true,
                                    emphasisModifier = 1.0f,
                                )
                            variationMuscleDao.insertVariationMuscle(variationMuscle)

                            // Add secondary muscles based on movement pattern
                            val secondaryMuscles = getSecondaryMuscles(firstVariation.movementPattern, muscleGroup)
                            secondaryMuscles.forEach { secondaryMuscle ->
                                val secondaryVariationMuscle =
                                    VariationMuscle(
                                        variationId = variationId,
                                        muscle = secondaryMuscle,
                                        isPrimary = false,
                                        emphasisModifier = 0.5f,
                                    )
                                variationMuscleDao.insertVariationMuscle(secondaryVariationMuscle)
                            }

                            // Add instructions if available
                            data.instructions?.let { instructionText ->
                                val instruction =
                                    VariationInstruction(
                                        id = 0,
                                        variationId = variationId,
                                        instructionType = InstructionType.EXECUTION,
                                        content = instructionText,
                                        orderIndex = 0,
                                        languageCode = "en",
                                    )
                                variationInstructionDao.insertInstruction(instruction)
                            }

                            // Add aliases
                            data.aliases.forEach { aliasText ->
                                val alias =
                                    VariationAlias(
                                        id = 0,
                                        variationId = variationId,
                                        alias = aliasText,
                                        confidence = 1.0f,
                                        languageCode = "en",
                                        source = "seed",
                                    )
                                variationAliasDao.insertAlias(alias)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                error(
                    "Exercise database loading failed. App cannot function without proper exercise data. Error: ${e.message}",
                )
            }
        }

    private fun extractCoreExerciseName(fullName: String): String {
        // Remove equipment prefixes to get core movement
        val withoutEquipment =
            fullName
                .replace("Barbell ", "")
                .replace("Dumbbell ", "")
                .replace("Cable ", "")
                .replace("Machine ", "")
                .replace("Smith Machine ", "")
                .replace("EZ Bar ", "")
                .replace("Kettlebell ", "")
                .replace("Resistance Band ", "")
                .replace("Bodyweight ", "")
                .replace("Weighted ", "")

        return withoutEquipment.trim()
    }

    private fun isCompoundExercise(coreName: String): Boolean {
        val compoundExercises =
            listOf(
                "Squat",
                "Deadlift",
                "Bench Press",
                "Press",
                "Row",
                "Pull Up",
                "Dip",
                "Lunge",
                "Clean",
                "Snatch",
                "Thruster",
            )
        return compoundExercises.any { coreName.contains(it, ignoreCase = true) }
    }

    private fun parseMovementPattern(value: String): MovementPattern =
        try {
            MovementPattern.valueOf(value.uppercase().replace(" ", "_"))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse movement pattern: $value, defaulting to PUSH", e)
            MovementPattern.PUSH
        }

    private fun parseEquipment(value: String): Equipment {
        // Special cases that don't follow simple transformation rules
        return when (value) {
            "Pull Up Bar" -> Equipment.PULL_UP_BAR
            "Ghd Machine" -> Equipment.GHD_MACHINE
            "Trx" -> Equipment.TRX
            else -> Equipment.valueOf(value.uppercase().replace(" ", "_"))
        }
    }

    private fun parseMuscleGroup(value: String): MuscleGroup =
        try {
            MuscleGroup.valueOf(value.uppercase().replace(" ", "_"))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse muscle group: $value, defaulting to CHEST", e)
            MuscleGroup.CHEST
        }

    private fun getRecommendedRepRange(difficulty: String): String =
        when (difficulty.uppercase()) {
            "BEGINNER" -> "8-12"
            "INTERMEDIATE" -> "6-10"
            "ADVANCED" -> "4-8"
            "ELITE" -> "1-5"
            else -> "8-12"
        }

    private fun determineRMScalingType(exerciseName: String): RMScalingType {
        val nameLower = exerciseName.lowercase()
        
        // Check for weighted bodyweight exercises
        if (isWeightedBodyweightExercise(nameLower)) {
            return RMScalingType.WEIGHTED_BODYWEIGHT
        }
        
        // Check for isolation exercises
        if (isIsolationExercise(nameLower)) {
            return RMScalingType.ISOLATION
        }
        
        // Default to standard for compound movements
        return RMScalingType.STANDARD
    }
    
    private fun isWeightedBodyweightExercise(nameLower: String): Boolean {
        if (!nameLower.contains("weighted")) return false
        
        val bodyweightExercises = listOf(
            "pull up", "pull-up", "chin up", "chin-up",
            "dip", "muscle up", "muscle-up"
        )
        
        return bodyweightExercises.any { nameLower.contains(it) }
    }
    
    private fun isIsolationExercise(nameLower: String): Boolean {
        val isolationKeywords = listOf(
            "curl", "extension", "fly", "flye", "raise", "shrug",
            "kickback", "pullover", "calf", "preacher",
            "concentration", "hammer"
        )
        
        // Check for standard isolation keywords
        if (isolationKeywords.any { nameLower.contains(it) }) {
            return true
        }
        
        // Special case for cable exercises
        if (nameLower.contains("cable")) {
            return nameLower.contains("crossover") || nameLower.contains("lateral")
        }
        
        return false
    }

    private fun getSecondaryMuscles(
        movementPattern: String,
        primaryMuscle: MuscleGroup,
    ): List<MuscleGroup> =
        when (movementPattern.uppercase()) {
            "PUSH" ->
                when (primaryMuscle) {
                    MuscleGroup.CHEST -> listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS)
                    MuscleGroup.SHOULDERS -> listOf(MuscleGroup.TRICEPS)
                    MuscleGroup.TRICEPS -> listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS)
                    else -> emptyList()
                }
            "PULL" ->
                when (primaryMuscle) {
                    MuscleGroup.BACK -> listOf(MuscleGroup.BICEPS, MuscleGroup.REAR_DELTS)
                    MuscleGroup.BICEPS -> listOf(MuscleGroup.BACK)
                    MuscleGroup.LATS -> listOf(MuscleGroup.BICEPS, MuscleGroup.MIDDLE_BACK)
                    else -> emptyList()
                }
            "SQUAT" -> listOf(MuscleGroup.GLUTES, MuscleGroup.CORE)
            "HINGE" -> listOf(MuscleGroup.GLUTES, MuscleGroup.CORE)
            else -> emptyList()
        }
}
