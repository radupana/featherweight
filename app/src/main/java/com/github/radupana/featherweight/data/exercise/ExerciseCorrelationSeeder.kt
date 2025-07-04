package com.github.radupana.featherweight.data.exercise

import com.github.radupana.featherweight.data.ExerciseCorrelation
import com.github.radupana.featherweight.data.ExerciseCorrelationDao
class ExerciseCorrelationSeeder(
    private val exerciseCorrelationDao: ExerciseCorrelationDao
) {
    suspend fun seedExerciseCorrelations() {
        // Check if already seeded
        if (exerciseCorrelationDao.getCount() > 0) {
            println("Exercise correlations already seeded")
            return
        }
        
        val correlations = mutableListOf<ExerciseCorrelation>()
        
        // Squat variations and related exercises
        correlations.addAll(listOf(
            ExerciseCorrelation(
                primaryExercise = "Barbell Back Squat",
                relatedExercise = "Barbell Front Squat",
                correlationStrength = 0.9f,
                movementPattern = MovementPattern.SQUAT,
                primaryMuscleGroup = MuscleGroup.QUADS,
                secondaryMuscleGroups = "[\"GLUTES\", \"HAMSTRINGS\"]",
                correlationType = "variation"
            ),
            ExerciseCorrelation(
                primaryExercise = "Barbell Back Squat",
                relatedExercise = "Leg Press",
                correlationStrength = 0.7f,
                movementPattern = MovementPattern.SQUAT,
                primaryMuscleGroup = MuscleGroup.QUADS,
                correlationType = "alternative"
            ),
            ExerciseCorrelation(
                primaryExercise = "Barbell Back Squat",
                relatedExercise = "Bulgarian Split Squat",
                correlationStrength = 0.6f,
                movementPattern = MovementPattern.SQUAT,
                primaryMuscleGroup = MuscleGroup.QUADS,
                correlationType = "accessory"
            )
        ))
        
        // Deadlift variations
        correlations.addAll(listOf(
            ExerciseCorrelation(
                primaryExercise = "Barbell Deadlift",
                relatedExercise = "Romanian Deadlift",
                correlationStrength = 0.8f,
                movementPattern = MovementPattern.HINGE,
                primaryMuscleGroup = MuscleGroup.HAMSTRINGS,
                secondaryMuscleGroups = "[\"GLUTES\", \"UPPER_BACK\"]",
                correlationType = "variation"
            ),
            ExerciseCorrelation(
                primaryExercise = "Barbell Deadlift",
                relatedExercise = "Trap Bar Deadlift",
                correlationStrength = 0.85f,
                movementPattern = MovementPattern.HINGE,
                primaryMuscleGroup = MuscleGroup.HAMSTRINGS,
                correlationType = "variation"
            ),
            ExerciseCorrelation(
                primaryExercise = "Barbell Deadlift",
                relatedExercise = "Rack Pull",
                correlationStrength = 0.7f,
                movementPattern = MovementPattern.HINGE,
                primaryMuscleGroup = MuscleGroup.UPPER_BACK,
                correlationType = "partial"
            )
        ))
        
        // Bench Press variations
        correlations.addAll(listOf(
            ExerciseCorrelation(
                primaryExercise = "Barbell Bench Press",
                relatedExercise = "Dumbbell Bench Press",
                correlationStrength = 0.9f,
                movementPattern = MovementPattern.PUSH,
                primaryMuscleGroup = MuscleGroup.CHEST,
                secondaryMuscleGroups = "[\"FRONT_DELTS\", \"TRICEPS\"]",
                correlationType = "variation"
            ),
            ExerciseCorrelation(
                primaryExercise = "Barbell Bench Press",
                relatedExercise = "Incline Barbell Press",
                correlationStrength = 0.8f,
                movementPattern = MovementPattern.PUSH,
                primaryMuscleGroup = MuscleGroup.CHEST,
                correlationType = "variation"
            ),
            ExerciseCorrelation(
                primaryExercise = "Barbell Bench Press",
                relatedExercise = "Close-Grip Bench Press",
                correlationStrength = 0.75f,
                movementPattern = MovementPattern.PUSH,
                primaryMuscleGroup = MuscleGroup.TRICEPS,
                secondaryMuscleGroups = "[\"CHEST\"]",
                correlationType = "variation"
            ),
            ExerciseCorrelation(
                primaryExercise = "Barbell Bench Press",
                relatedExercise = "Dips",
                correlationStrength = 0.6f,
                movementPattern = MovementPattern.PUSH,
                primaryMuscleGroup = MuscleGroup.CHEST,
                correlationType = "accessory"
            )
        ))
        
        // Overhead Press variations
        correlations.addAll(listOf(
            ExerciseCorrelation(
                primaryExercise = "Barbell Overhead Press",
                relatedExercise = "Dumbbell Shoulder Press",
                correlationStrength = 0.9f,
                movementPattern = MovementPattern.PUSH,
                primaryMuscleGroup = MuscleGroup.FRONT_DELTS,
                secondaryMuscleGroups = "[\"TRICEPS\"]",
                correlationType = "variation"
            ),
            ExerciseCorrelation(
                primaryExercise = "Barbell Overhead Press",
                relatedExercise = "Push Press",
                correlationStrength = 0.85f,
                movementPattern = MovementPattern.PUSH,
                primaryMuscleGroup = MuscleGroup.FRONT_DELTS,
                correlationType = "variation"
            ),
            ExerciseCorrelation(
                primaryExercise = "Barbell Overhead Press",
                relatedExercise = "Arnold Press",
                correlationStrength = 0.7f,
                movementPattern = MovementPattern.PUSH,
                primaryMuscleGroup = MuscleGroup.FRONT_DELTS,
                correlationType = "variation"
            )
        ))
        
        // Row variations
        correlations.addAll(listOf(
            ExerciseCorrelation(
                primaryExercise = "Barbell Bent Over Row",
                relatedExercise = "Dumbbell Row",
                correlationStrength = 0.85f,
                movementPattern = MovementPattern.PULL,
                primaryMuscleGroup = MuscleGroup.UPPER_BACK,
                secondaryMuscleGroups = "[\"BICEPS\"]",
                correlationType = "variation"
            ),
            ExerciseCorrelation(
                primaryExercise = "Barbell Bent Over Row",
                relatedExercise = "Cable Row",
                correlationStrength = 0.8f,
                movementPattern = MovementPattern.PULL,
                primaryMuscleGroup = MuscleGroup.UPPER_BACK,
                correlationType = "variation"
            ),
            ExerciseCorrelation(
                primaryExercise = "Barbell Bent Over Row",
                relatedExercise = "T-Bar Row",
                correlationStrength = 0.85f,
                movementPattern = MovementPattern.PULL,
                primaryMuscleGroup = MuscleGroup.UPPER_BACK,
                correlationType = "variation"
            )
        ))
        
        // Pull-up/Chin-up variations
        correlations.addAll(listOf(
            ExerciseCorrelation(
                primaryExercise = "Pull-ups",
                relatedExercise = "Chin-ups",
                correlationStrength = 0.95f,
                movementPattern = MovementPattern.PULL,
                primaryMuscleGroup = MuscleGroup.UPPER_BACK,
                secondaryMuscleGroups = "[\"BICEPS\"]",
                correlationType = "variation"
            ),
            ExerciseCorrelation(
                primaryExercise = "Pull-ups",
                relatedExercise = "Lat Pulldown",
                correlationStrength = 0.7f,
                movementPattern = MovementPattern.PULL,
                primaryMuscleGroup = MuscleGroup.LATS,
                correlationType = "alternative"
            ),
            ExerciseCorrelation(
                primaryExercise = "Pull-ups",
                relatedExercise = "Assisted Pull-ups",
                correlationStrength = 0.8f,
                movementPattern = MovementPattern.PULL,
                primaryMuscleGroup = MuscleGroup.UPPER_BACK,
                correlationType = "progression"
            )
        ))
        
        // Antagonist pairs
        correlations.addAll(listOf(
            ExerciseCorrelation(
                primaryExercise = "Barbell Bench Press",
                relatedExercise = "Barbell Bent Over Row",
                correlationStrength = 0.5f,
                movementPattern = MovementPattern.PUSH,
                primaryMuscleGroup = MuscleGroup.CHEST,
                correlationType = "antagonist"
            ),
            ExerciseCorrelation(
                primaryExercise = "Barbell Overhead Press",
                relatedExercise = "Pull-ups",
                correlationStrength = 0.5f,
                movementPattern = MovementPattern.PUSH,
                primaryMuscleGroup = MuscleGroup.FRONT_DELTS,
                correlationType = "antagonist"
            ),
            ExerciseCorrelation(
                primaryExercise = "Barbell Curl",
                relatedExercise = "Tricep Extension",
                correlationStrength = 0.5f,
                movementPattern = MovementPattern.PULL,
                primaryMuscleGroup = MuscleGroup.BICEPS,
                correlationType = "antagonist"
            )
        ))
        
        // Insert all correlations
        exerciseCorrelationDao.insertAll(correlations)
        println("Seeded ${correlations.size} exercise correlations")
    }
}