package com.github.radupana.featherweight.data.exercise

import android.content.Context
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
    val aliases: List<String> = emptyList()
)

@Serializable
data class ExercisesJson(
    val exercises: List<ExerciseData>
)

class ExerciseSeeder(
    private val exerciseDao: ExerciseDao,
    private val context: Context? = null,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    
    suspend fun seedExercises() = withContext(Dispatchers.IO) {
        // Check if already seeded
        val existingCount = exerciseDao.getAllExercisesWithDetails().size
        if (existingCount > 0) {
            println("Exercises already seeded (count: $existingCount), skipping")
            return@withContext
        }
        
        if (context == null) {
            println("❌ FATAL: Context not available for exercise seeding")
            throw IllegalStateException("Cannot initialize exercise database: Context is null. App cannot function without proper exercise data.")
        }
        
        try {
            // Load from bundled JSON
            val inputStream = context.assets.open("exercises.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val exercisesData = json.decodeFromString<ExercisesJson>(jsonString)
            
            println("Loading ${exercisesData.exercises.size} exercises from assets")
            
            // Convert and insert exercises
            val exercises = exercisesData.exercises.map { data ->
                Exercise(
                    id = 0, // Auto-generate
                    name = data.name,
                    category = ExerciseCategory.valueOf(data.category.uppercase().replace(" ", "_")),
                    equipment = parseEquipment(data.equipment),
                    muscleGroup = data.muscleGroup,
                    movementPattern = data.movementPattern,
                    type = ExerciseType.valueOf(data.type.uppercase()),
                    difficulty = ExerciseDifficulty.valueOf(data.difficulty.uppercase()),
                    requiresWeight = data.requiresWeight,
                    instructions = data.instructions,
                    usageCount = 0,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                )
            }
            
            // Insert exercises and aliases
            exercises.forEachIndexed { index, exercise ->
                val exerciseId = exerciseDao.insertExercise(exercise)
                
                // Insert aliases for fuzzy matching
                val data = exercisesData.exercises[index]
                if (data.aliases.isNotEmpty()) {
                    val aliases = data.aliases.map { alias ->
                        ExerciseAlias(
                            exerciseId = exerciseId,
                            alias = alias,
                            confidence = 1.0f,
                            exactMatchOnly = false,
                            source = "static"
                        )
                    }
                    exerciseDao.insertAliases(aliases)
                }
            }
            
            println("Successfully seeded ${exercises.size} exercises")
            
        } catch (e: Exception) {
            println("❌ FATAL: Failed to load exercise database: ${e.message}")
            e.printStackTrace()
            throw IllegalStateException("Exercise database loading failed. App cannot function without proper exercise data. Error: ${e.message}")
        }
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
}