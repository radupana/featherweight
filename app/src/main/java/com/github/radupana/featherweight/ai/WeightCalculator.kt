package com.github.radupana.featherweight.ai

import com.github.radupana.featherweight.data.profile.UserExerciseMax

enum class ProgrammeType {
    STRENGTH,
    HYPERTROPHY,
    ENDURANCE,
    GENERAL
}

data class WeightCalculation(
    val weight: Float,
    val percentageOf1RM: Float?,
    val source: String
)

class WeightCalculator {
    
    fun calculateWorkingWeight(
        exerciseName: String,
        repRange: IntRange,
        programmeType: ProgrammeType,
        user1RM: Float? = null,
        extractedWeight: Float? = null,
        aiSuggestedWeight: Float? = null
    ): WeightCalculation {
        
        // Priority hierarchy for weight calculation
        return when {
            // 1. Explicit weight from user input (highest priority)
            extractedWeight != null -> {
                WeightCalculation(
                    weight = extractedWeight,
                    percentageOf1RM = user1RM?.let { (extractedWeight / it) * 100f },
                    source = "user_input"
                )
            }
            
            // 2. Calculated from user's 1RM in database
            user1RM != null -> {
                val percentage = calculatePercentageForRepRange(repRange, programmeType)
                val weight = user1RM * (percentage / 100f)
                WeightCalculation(
                    weight = weight,
                    percentageOf1RM = percentage,
                    source = "user_1rm"
                )
            }
            
            // 3. AI suggested weight
            aiSuggestedWeight != null && aiSuggestedWeight > 0 -> {
                WeightCalculation(
                    weight = aiSuggestedWeight,
                    percentageOf1RM = null,
                    source = "ai_suggested"
                )
            }
            
            // 4. Smart exercise-specific defaults
            else -> {
                val defaultWeight = getSmartDefault(exerciseName, repRange)
                WeightCalculation(
                    weight = defaultWeight,
                    percentageOf1RM = null,
                    source = "smart_default"
                )
            }
        }
    }
    
    private fun calculatePercentageForRepRange(repRange: IntRange, programmeType: ProgrammeType): Float {
        val avgReps = (repRange.first + repRange.last) / 2f
        
        return when (programmeType) {
            ProgrammeType.STRENGTH -> when {
                avgReps <= 3 -> 85f
                avgReps <= 5 -> 80f
                avgReps <= 8 -> 75f
                else -> 70f
            }
            ProgrammeType.HYPERTROPHY -> when {
                avgReps <= 6 -> 75f
                avgReps <= 10 -> 70f
                avgReps <= 15 -> 65f
                else -> 60f
            }
            ProgrammeType.ENDURANCE -> when {
                avgReps <= 12 -> 65f
                avgReps <= 20 -> 55f
                else -> 50f
            }
            ProgrammeType.GENERAL -> when {
                avgReps <= 5 -> 75f
                avgReps <= 10 -> 70f
                avgReps <= 15 -> 65f
                else -> 60f
            }
        }
    }
    
    private fun getSmartDefault(exerciseName: String, repRange: IntRange): Float {
        val avgReps = (repRange.first + repRange.last) / 2f
        val exerciseCategory = categorizeExercise(exerciseName)
        
        return when (exerciseCategory) {
            ExerciseCategory.HEAVY_COMPOUND -> when {
                avgReps <= 5 -> 70f
                avgReps <= 10 -> 60f
                else -> 50f
            }
            ExerciseCategory.MEDIUM_COMPOUND -> when {
                avgReps <= 5 -> 50f
                avgReps <= 10 -> 40f
                else -> 35f
            }
            ExerciseCategory.LIGHT_COMPOUND -> when {
                avgReps <= 5 -> 35f
                avgReps <= 10 -> 30f
                else -> 25f
            }
            ExerciseCategory.ISOLATION -> when {
                avgReps <= 8 -> 25f
                avgReps <= 15 -> 20f
                else -> 15f
            }
            ExerciseCategory.BODYWEIGHT -> 0f
            ExerciseCategory.UNKNOWN -> 45f // Fallback
        }
    }
    
    private fun categorizeExercise(exerciseName: String): ExerciseCategory {
        val name = exerciseName.lowercase()
        
        return when {
            // Heavy compounds (typically heaviest lifts)
            name.contains("squat") ||
            name.contains("deadlift") -> ExerciseCategory.HEAVY_COMPOUND
            
            // Medium compounds
            name.contains("bench press") ||
            name.contains("overhead press") ||
            name.contains("row") ||
            name.contains("pull-up") && !name.contains("assisted") -> ExerciseCategory.MEDIUM_COMPOUND
            
            // Light compounds
            name.contains("lunge") ||
            name.contains("step up") ||
            name.contains("bulgarian split squat") ||
            name.contains("front squat") -> ExerciseCategory.LIGHT_COMPOUND
            
            // Isolation exercises
            name.contains("curl") ||
            name.contains("extension") ||
            name.contains("raise") ||
            name.contains("fly") ||
            name.contains("flye") -> ExerciseCategory.ISOLATION
            
            // Bodyweight exercises
            name.contains("push-up") ||
            name.contains("push up") ||
            name.contains("bodyweight") ||
            name.contains("plank") -> ExerciseCategory.BODYWEIGHT
            
            else -> ExerciseCategory.UNKNOWN
        }
    }
    
    fun determineProgrammeType(programmeDescription: String): ProgrammeType {
        val description = programmeDescription.lowercase()
        
        return when {
            description.contains("strength") ||
            description.contains("powerlifting") ||
            description.contains("max") ||
            description.contains("1rm") -> ProgrammeType.STRENGTH
            
            description.contains("hypertrophy") ||
            description.contains("muscle") ||
            description.contains("bodybuilding") ||
            description.contains("size") -> ProgrammeType.HYPERTROPHY
            
            description.contains("endurance") ||
            description.contains("cardio") ||
            description.contains("conditioning") -> ProgrammeType.ENDURANCE
            
            else -> ProgrammeType.GENERAL
        }
    }
    
    private enum class ExerciseCategory {
        HEAVY_COMPOUND,
        MEDIUM_COMPOUND,
        LIGHT_COMPOUND,
        ISOLATION,
        BODYWEIGHT,
        UNKNOWN
    }
}