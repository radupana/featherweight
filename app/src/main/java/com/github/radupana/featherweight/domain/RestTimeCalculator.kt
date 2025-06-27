package com.github.radupana.featherweight.domain

import com.github.radupana.featherweight.data.exercise.Exercise
import com.github.radupana.featherweight.data.exercise.ExerciseCategory
import com.github.radupana.featherweight.data.exercise.ExerciseType
import com.github.radupana.featherweight.data.exercise.MovementPattern
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Calculates smart rest time suggestions based on exercise characteristics
 */
object RestTimeCalculator {
    
    /**
     * Calculate suggested rest time for an exercise
     * @param exerciseName The name of the exercise
     * @param exercise Optional Exercise entity with detailed metadata
     * @param reps Number of reps performed (affects intensity)
     * @param weight Weight used (for intensity calculation)
     * @param oneRepMax Optional 1RM for intensity percentage calculation
     * @return Suggested rest duration with reasoning
     */
    fun calculateRestTime(
        exerciseName: String,
        exercise: Exercise? = null,
        reps: Int? = null,
        weight: Float? = null,
        oneRepMax: Float? = null
    ): RestSuggestion {
        // Use exercise metadata if available, otherwise categorize by name
        val restCategory = exercise?.let { 
            categorizeExercise(it) 
        } ?: categorizeByName(exerciseName)
        
        val baseRestTime = getBaseRestTime(restCategory)
        val intensity = calculateIntensity(reps, weight, oneRepMax)
        val adjustedRestTime = adjustForIntensity(baseRestTime, intensity)
        
        return RestSuggestion(
            duration = adjustedRestTime,
            category = restCategory,
            reasoning = buildReasoning(restCategory, intensity)
        )
    }
    
    private fun categorizeExercise(exercise: Exercise): RestCategory {
        return when {
            // Check for compound movements first
            isCompoundMovement(exercise) -> RestCategory.COMPOUND
            
            // Check exercise type
            exercise.type == ExerciseType.POWER -> RestCategory.COMPOUND
            exercise.type == ExerciseType.CARDIO -> RestCategory.CARDIO
            
            // Check by category
            exercise.category in listOf(
                ExerciseCategory.CHEST, 
                ExerciseCategory.BACK, 
                ExerciseCategory.LEGS
            ) && isMainMovement(exercise.name) -> RestCategory.COMPOUND
            
            exercise.category in listOf(
                ExerciseCategory.ARMS,
                ExerciseCategory.SHOULDERS
            ) -> RestCategory.ACCESSORY
            
            exercise.category == ExerciseCategory.CORE -> RestCategory.ISOLATION
            
            else -> RestCategory.ACCESSORY
        }
    }
    
    private fun categorizeByName(exerciseName: String): RestCategory {
        val name = exerciseName.lowercase()
        
        return when {
            // Compound movements - require longer rest
            name.contains("squat") ||
            name.contains("deadlift") ||
            name.contains("bench press") ||
            name.contains("overhead press") ||
            name.contains("press") && (name.contains("military") || name.contains("standing")) ||
            name.contains("row") && (name.contains("barbell") || name.contains("t-bar")) ||
            name.contains("pull-up") ||
            name.contains("chin-up") ||
            name.contains("dip") && !name.contains("tricep") ||
            name.contains("clean") ||
            name.contains("snatch") ||
            name.contains("thruster") -> RestCategory.COMPOUND
            
            // Accessory movements - moderate rest
            name.contains("row") ||
            name.contains("press") ||
            name.contains("pulldown") ||
            name.contains("fly") ||
            name.contains("raise") ||
            name.contains("shrug") ||
            name.contains("lunge") ||
            name.contains("step-up") ||
            name.contains("romanian deadlift") ||
            name.contains("stiff leg deadlift") -> RestCategory.ACCESSORY
            
            // Isolation movements - shorter rest
            name.contains("curl") ||
            name.contains("extension") ||
            name.contains("kickback") ||
            name.contains("calf") ||
            name.contains("tricep") ||
            name.contains("bicep") ||
            name.contains("lateral") ||
            name.contains("rear delt") ||
            name.contains("front raise") ||
            name.contains("crunch") ||
            name.contains("sit-up") ||
            name.contains("plank") -> RestCategory.ISOLATION
            
            // Cardio - very short rest
            name.contains("burpee") ||
            name.contains("jumping") ||
            name.contains("mountain climber") ||
            name.contains("sprint") -> RestCategory.CARDIO
            
            // Default to accessory if unsure
            else -> RestCategory.ACCESSORY
        }
    }
    
    private fun isCompoundMovement(exercise: Exercise): Boolean {
        // Check if exercise uses compound movement patterns
        return exercise.name.let { name ->
            val lowerName = name.lowercase()
            lowerName.contains("squat") ||
            lowerName.contains("deadlift") ||
            lowerName.contains("bench") ||
            lowerName.contains("press") && (lowerName.contains("overhead") || lowerName.contains("military")) ||
            lowerName.contains("pull-up") ||
            lowerName.contains("chin-up")
        }
    }
    
    private fun isMainMovement(exerciseName: String): Boolean {
        val mainMovements = listOf(
            "squat", "deadlift", "bench", "press", "row", "pull-up", "chin-up"
        )
        val lowerName = exerciseName.lowercase()
        return mainMovements.any { lowerName.contains(it) }
    }
    
    private fun getBaseRestTime(category: RestCategory): Duration {
        return when (category) {
            RestCategory.COMPOUND -> 4.minutes
            RestCategory.ACCESSORY -> 2.minutes
            RestCategory.ISOLATION -> 90.seconds
            RestCategory.CARDIO -> 60.seconds
        }
    }
    
    private fun calculateIntensity(reps: Int?, weight: Float?, oneRepMax: Float?): Intensity {
        // If we have weight and 1RM, calculate percentage
        if (weight != null && oneRepMax != null && oneRepMax > 0) {
            val percentage = (weight / oneRepMax) * 100
            return when {
                percentage >= 90 -> Intensity.VERY_HIGH
                percentage >= 80 -> Intensity.HIGH
                percentage >= 70 -> Intensity.MODERATE
                else -> Intensity.LOW
            }
        }
        
        // Fallback to rep-based intensity
        return when (reps) {
            in 1..3 -> Intensity.VERY_HIGH
            in 4..6 -> Intensity.HIGH
            in 7..12 -> Intensity.MODERATE
            else -> Intensity.LOW
        }
    }
    
    private fun adjustForIntensity(baseTime: Duration, intensity: Intensity): Duration {
        val multiplier = when (intensity) {
            Intensity.VERY_HIGH -> 1.3 // +30% for very heavy sets
            Intensity.HIGH -> 1.15 // +15% for heavy sets
            Intensity.MODERATE -> 1.0 // Base time for moderate intensity
            Intensity.LOW -> 0.8 // -20% for light/high-rep sets
        }
        
        return (baseTime * multiplier).coerceAtLeast(60.seconds).coerceAtMost(6.minutes)
    }
    
    private fun buildReasoning(category: RestCategory, intensity: Intensity): String {
        val baseReason = when (category) {
            RestCategory.COMPOUND -> "Compound"
            RestCategory.ACCESSORY -> "Accessory"
            RestCategory.ISOLATION -> "Isolation"
            RestCategory.CARDIO -> "Cardio"
        }
        
        val intensityModifier = when (intensity) {
            Intensity.VERY_HIGH -> " • Heavy"
            Intensity.HIGH -> " • Moderate-Heavy"
            Intensity.MODERATE -> ""
            Intensity.LOW -> " • Light"
        }
        
        return "$baseReason$intensityModifier"
    }
}

enum class RestCategory {
    COMPOUND,    // 3-5 minutes (Squat, Deadlift, Bench Press)
    ACCESSORY,   // 1.5-2.5 minutes (Rows, Overhead Press)
    ISOLATION,   // 1-1.5 minutes (Curls, Extensions)
    CARDIO       // 30-60 seconds (Burpees, Sprints)
}

enum class Intensity {
    VERY_HIGH,   // 90%+ 1RM or 1-3 reps
    HIGH,        // 80-90% 1RM or 4-6 reps
    MODERATE,    // 70-80% 1RM or 7-12 reps
    LOW          // <70% 1RM or 13+ reps
}

data class RestSuggestion(
    val duration: Duration,
    val category: RestCategory,
    val reasoning: String
)