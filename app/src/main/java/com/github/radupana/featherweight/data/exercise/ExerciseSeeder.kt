// app/src/main/java/com/github/radupana/featherweight/data/exercise/ExerciseSeeder.kt
package com.github.radupana.featherweight.data.exercise

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExerciseSeeder(
    private val exerciseDao: ExerciseDao,
) {
    suspend fun seedMainLifts() =
        withContext(Dispatchers.IO) {
            // Seed all main lifts
            seedPowerliftingLifts()
            seedOlympicLifts()
            seedMainCompoundMovements()
            seedIsolationLifts()
            seedAccessoryMovements()
        }

    private suspend fun seedPowerliftingLifts() {
        // Squat variations
        createExercise(
            name = "Back Squat",
            category = ExerciseCategory.LEGS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.INTERMEDIATE,
            primaryMuscles = setOf(MuscleGroup.QUADS, MuscleGroup.GLUTES),
            secondaryMuscles =
                setOf(
                    MuscleGroup.HAMSTRINGS,
                    MuscleGroup.ABS,
                    MuscleGroup.LOWER_BACK,
                ),
            requiredEquipment = setOf(Equipment.BARBELL),
            movementPatterns = setOf(MovementPattern.SQUAT),
            instructions = "Stand with feet shoulder-width apart, bar on upper back. Descend by pushing hips back and bending knees until thighs are parallel to floor. Drive through heels to return to start.",
            tips = "Keep chest up, knees track over toes, maintain neutral spine throughout movement",
            commonMistakes = "Knees caving in, excessive forward lean, not reaching proper depth",
        )

        createExercise(
            name = "Front Squat",
            category = ExerciseCategory.LEGS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.ADVANCED,
            primaryMuscles = setOf(MuscleGroup.QUADS, MuscleGroup.GLUTES),
            secondaryMuscles = setOf(MuscleGroup.ABS, MuscleGroup.UPPER_BACK),
            requiredEquipment = setOf(Equipment.BARBELL),
            movementPatterns = setOf(MovementPattern.SQUAT),
            instructions = "Hold bar across front of shoulders in front rack position. Squat down while keeping torso upright. Drive up through heels.",
            tips = "Keep elbows high, maintain upright torso, core tight throughout",
            commonMistakes = "Dropping elbows, excessive forward lean, losing bar position",
        )

        createExercise(
            name = "Box Squat",
            category = ExerciseCategory.LEGS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.INTERMEDIATE,
            primaryMuscles = setOf(MuscleGroup.QUADS, MuscleGroup.GLUTES),
            secondaryMuscles = setOf(MuscleGroup.HAMSTRINGS, MuscleGroup.ABS),
            requiredEquipment = setOf(Equipment.BARBELL),
            optionalEquipment = setOf(Equipment.PLATES), // for box height
            movementPatterns = setOf(MovementPattern.SQUAT),
        )

        // Deadlift variations
        createExercise(
            name = "Conventional Deadlift",
            category = ExerciseCategory.BACK,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.INTERMEDIATE,
            primaryMuscles =
                setOf(
                    MuscleGroup.HAMSTRINGS,
                    MuscleGroup.GLUTES,
                    MuscleGroup.LOWER_BACK,
                ),
            secondaryMuscles =
                setOf(
                    MuscleGroup.UPPER_BACK,
                    MuscleGroup.TRAPS,
                    MuscleGroup.FOREARMS,
                    MuscleGroup.ABS,
                ),
            requiredEquipment = setOf(Equipment.BARBELL),
            movementPatterns = setOf(MovementPattern.HINGE),
            instructions = "Stand with feet hip-width apart, grip bar outside legs. Keep chest up, hinge at hips and drive through heels to stand up with bar.",
            tips = "Keep bar close to body, engage lats, drive hips forward at top",
            commonMistakes = "Bar drifting away from body, rounding lower back, hyperextending at top",
        )

        createExercise(
            name = "Sumo Deadlift",
            category = ExerciseCategory.BACK,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.INTERMEDIATE,
            primaryMuscles = setOf(MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.QUADS),
            secondaryMuscles =
                setOf(
                    MuscleGroup.LOWER_BACK,
                    MuscleGroup.UPPER_BACK,
                    MuscleGroup.TRAPS,
                ),
            requiredEquipment = setOf(Equipment.BARBELL),
            movementPatterns = setOf(MovementPattern.HINGE),
        )

        createExercise(
            name = "Romanian Deadlift",
            category = ExerciseCategory.LEGS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.INTERMEDIATE,
            primaryMuscles = setOf(MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES),
            secondaryMuscles = setOf(MuscleGroup.LOWER_BACK, MuscleGroup.UPPER_BACK),
            requiredEquipment = setOf(Equipment.BARBELL),
            alternativeEquipment = setOf(Equipment.DUMBBELL),
            movementPatterns = setOf(MovementPattern.HINGE),
        )

        // Bench Press variations
        createExercise(
            name = "Bench Press",
            category = ExerciseCategory.CHEST,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.INTERMEDIATE,
            primaryMuscles = setOf(MuscleGroup.CHEST),
            secondaryMuscles = setOf(MuscleGroup.FRONT_DELTS, MuscleGroup.TRICEPS),
            requiredEquipment = setOf(Equipment.BARBELL),
            movementPatterns = setOf(MovementPattern.HORIZONTAL_PUSH),
            instructions = "Lie on bench, grip bar slightly wider than shoulders. Lower bar to chest with control, press up explosively.",
            tips = "Retract shoulder blades, maintain arch, keep feet planted",
            commonMistakes = "Bouncing off chest, flaring elbows too wide, losing shoulder blade position",
        )

        createExercise(
            name = "Incline Bench Press",
            category = ExerciseCategory.CHEST,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.INTERMEDIATE,
            primaryMuscles = setOf(MuscleGroup.CHEST, MuscleGroup.FRONT_DELTS),
            secondaryMuscles = setOf(MuscleGroup.TRICEPS),
            requiredEquipment = setOf(Equipment.BARBELL),
            movementPatterns = setOf(MovementPattern.PUSH),
        )

        createExercise(
            name = "Close Grip Bench Press",
            category = ExerciseCategory.ARMS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.INTERMEDIATE,
            primaryMuscles = setOf(MuscleGroup.TRICEPS, MuscleGroup.CHEST),
            secondaryMuscles = setOf(MuscleGroup.FRONT_DELTS),
            requiredEquipment = setOf(Equipment.BARBELL),
            movementPatterns = setOf(MovementPattern.HORIZONTAL_PUSH),
        )
    }

    private suspend fun seedOlympicLifts() {
        createExercise(
            name = "Clean and Jerk",
            category = ExerciseCategory.FULL_BODY,
            type = ExerciseType.POWER,
            difficulty = ExerciseDifficulty.EXPERT,
            primaryMuscles = setOf(MuscleGroup.FULL_BODY),
            secondaryMuscles = setOf(),
            requiredEquipment = setOf(Equipment.BARBELL),
            movementPatterns = setOf(MovementPattern.PULL, MovementPattern.PUSH),
            instructions = "Complex movement: Clean bar to shoulders, then jerk overhead. Requires extensive technique work.",
            tips = "Learn from qualified coach, start with light weights, focus on mobility",
            commonMistakes = "Poor timing, inadequate mobility, rushing progression",
        )

        createExercise(
            name = "Snatch",
            category = ExerciseCategory.FULL_BODY,
            type = ExerciseType.POWER,
            difficulty = ExerciseDifficulty.EXPERT,
            primaryMuscles = setOf(MuscleGroup.FULL_BODY),
            secondaryMuscles = setOf(),
            requiredEquipment = setOf(Equipment.BARBELL),
            movementPatterns = setOf(MovementPattern.PULL),
        )

        createExercise(
            name = "Power Clean",
            category = ExerciseCategory.FULL_BODY,
            type = ExerciseType.POWER,
            difficulty = ExerciseDifficulty.ADVANCED,
            primaryMuscles = setOf(MuscleGroup.TRAPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES),
            secondaryMuscles = setOf(MuscleGroup.QUADS, MuscleGroup.UPPER_BACK, MuscleGroup.ABS),
            requiredEquipment = setOf(Equipment.BARBELL),
            movementPatterns = setOf(MovementPattern.PULL),
        )

        createExercise(
            name = "Hang Clean",
            category = ExerciseCategory.FULL_BODY,
            type = ExerciseType.POWER,
            difficulty = ExerciseDifficulty.ADVANCED,
            primaryMuscles = setOf(MuscleGroup.TRAPS, MuscleGroup.HAMSTRINGS),
            secondaryMuscles = setOf(MuscleGroup.UPPER_BACK, MuscleGroup.QUADS),
            requiredEquipment = setOf(Equipment.BARBELL),
            movementPatterns = setOf(MovementPattern.PULL),
        )
    }

    private suspend fun seedMainCompoundMovements() {
        // Upper Body Pulling
        createExercise(
            name = "Pull-ups",
            category = ExerciseCategory.BACK,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.INTERMEDIATE,
            primaryMuscles = setOf(MuscleGroup.LATS, MuscleGroup.UPPER_BACK),
            secondaryMuscles = setOf(MuscleGroup.BICEPS, MuscleGroup.REAR_DELTS),
            requiredEquipment = setOf(Equipment.PULL_UP_BAR),
            movementPatterns = setOf(MovementPattern.VERTICAL_PULL),
            requiresWeight = false,
        )

        createExercise(
            name = "Chin-ups",
            category = ExerciseCategory.BACK,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.INTERMEDIATE,
            primaryMuscles = setOf(MuscleGroup.LATS, MuscleGroup.BICEPS),
            secondaryMuscles = setOf(MuscleGroup.UPPER_BACK, MuscleGroup.REAR_DELTS),
            requiredEquipment = setOf(Equipment.PULL_UP_BAR),
            movementPatterns = setOf(MovementPattern.VERTICAL_PULL),
        )

        createExercise(
            name = "Barbell Row",
            category = ExerciseCategory.BACK,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.INTERMEDIATE,
            primaryMuscles = setOf(MuscleGroup.UPPER_BACK, MuscleGroup.LATS),
            secondaryMuscles =
                setOf(
                    MuscleGroup.BICEPS,
                    MuscleGroup.REAR_DELTS,
                    MuscleGroup.LOWER_BACK,
                ),
            requiredEquipment = setOf(Equipment.BARBELL),
            movementPatterns = setOf(MovementPattern.HORIZONTAL_PULL),
        )

        createExercise(
            name = "Pendlay Row",
            category = ExerciseCategory.BACK,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.ADVANCED,
            primaryMuscles = setOf(MuscleGroup.UPPER_BACK, MuscleGroup.LATS),
            secondaryMuscles = setOf(MuscleGroup.BICEPS, MuscleGroup.REAR_DELTS),
            requiredEquipment = setOf(Equipment.BARBELL),
            movementPatterns = setOf(MovementPattern.HORIZONTAL_PULL),
        )

        // Overhead Pressing
        createExercise(
            name = "Overhead Press",
            category = ExerciseCategory.SHOULDERS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.INTERMEDIATE,
            primaryMuscles = setOf(MuscleGroup.FRONT_DELTS, MuscleGroup.SIDE_DELTS),
            secondaryMuscles = setOf(MuscleGroup.TRICEPS, MuscleGroup.UPPER_BACK, MuscleGroup.ABS),
            requiredEquipment = setOf(Equipment.BARBELL),
            movementPatterns = setOf(MovementPattern.VERTICAL_PUSH),
            instructions = "Stand with feet hip-width, press bar from shoulders to overhead. Keep core tight.",
            tips = "Don't arch back excessively, press in straight line, squeeze glutes",
            commonMistakes = "Excessive back arch, pressing around head instead of through it",
        )

        createExercise(
            name = "Push Press",
            category = ExerciseCategory.SHOULDERS,
            type = ExerciseType.POWER,
            difficulty = ExerciseDifficulty.ADVANCED,
            primaryMuscles = setOf(MuscleGroup.FRONT_DELTS, MuscleGroup.SIDE_DELTS),
            secondaryMuscles = setOf(MuscleGroup.TRICEPS, MuscleGroup.QUADS, MuscleGroup.ABS),
            requiredEquipment = setOf(Equipment.BARBELL),
            movementPatterns = setOf(MovementPattern.VERTICAL_PUSH),
        )

        createExercise(
            name = "Dumbbell Shoulder Press",
            category = ExerciseCategory.SHOULDERS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.FRONT_DELTS, MuscleGroup.SIDE_DELTS),
            secondaryMuscles = setOf(MuscleGroup.TRICEPS),
            requiredEquipment = setOf(Equipment.DUMBBELL),
            movementPatterns = setOf(MovementPattern.VERTICAL_PUSH),
        )
    }

    private suspend fun seedIsolationLifts() {
        // Biceps
        createExercise(
            name = "Barbell Curls",
            category = ExerciseCategory.ARMS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.BICEPS),
            secondaryMuscles = setOf(MuscleGroup.FOREARMS),
            requiredEquipment = setOf(Equipment.BARBELL),
            movementPatterns = setOf(MovementPattern.PULL),
        )

        createExercise(
            name = "Dumbbell Bicep Curls",
            category = ExerciseCategory.ARMS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.BICEPS),
            secondaryMuscles = setOf(MuscleGroup.FOREARMS),
            requiredEquipment = setOf(Equipment.DUMBBELL),
            movementPatterns = setOf(MovementPattern.PULL),
        )

        createExercise(
            name = "Hammer Curls",
            category = ExerciseCategory.ARMS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.BICEPS, MuscleGroup.FOREARMS),
            secondaryMuscles = setOf(),
            requiredEquipment = setOf(Equipment.DUMBBELL),
            movementPatterns = setOf(MovementPattern.PULL),
        )

        // Triceps
        createExercise(
            name = "Dips",
            category = ExerciseCategory.ARMS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.INTERMEDIATE,
            primaryMuscles = setOf(MuscleGroup.TRICEPS, MuscleGroup.CHEST),
            secondaryMuscles = setOf(MuscleGroup.FRONT_DELTS),
            requiredEquipment = setOf(Equipment.DIP_STATION),
            movementPatterns = setOf(MovementPattern.PUSH),
        )

        createExercise(
            name = "Tricep Dips",
            category = ExerciseCategory.ARMS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.TRICEPS),
            secondaryMuscles = setOf(MuscleGroup.CHEST, MuscleGroup.FRONT_DELTS),
            requiredEquipment = setOf(Equipment.BODYWEIGHT),
            movementPatterns = setOf(MovementPattern.PUSH),
            requiresWeight = false,
        )

        createExercise(
            name = "Overhead Tricep Extension",
            category = ExerciseCategory.ARMS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.TRICEPS),
            secondaryMuscles = setOf(),
            requiredEquipment = setOf(Equipment.DUMBBELL),
            alternativeEquipment = setOf(Equipment.BARBELL),
            movementPatterns = setOf(MovementPattern.PUSH),
        )

        // Leg isolation
        createExercise(
            name = "Bulgarian Split Squat",
            category = ExerciseCategory.LEGS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.INTERMEDIATE,
            primaryMuscles = setOf(MuscleGroup.QUADS, MuscleGroup.GLUTES),
            secondaryMuscles = setOf(MuscleGroup.HAMSTRINGS, MuscleGroup.ABS),
            requiredEquipment = setOf(Equipment.BODYWEIGHT),
            optionalEquipment = setOf(Equipment.DUMBBELL),
            movementPatterns = setOf(MovementPattern.LUNGE),
            requiresWeight = false,
        )

        createExercise(
            name = "Walking Lunges",
            category = ExerciseCategory.LEGS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.QUADS, MuscleGroup.GLUTES),
            secondaryMuscles = setOf(MuscleGroup.HAMSTRINGS, MuscleGroup.ABS),
            requiredEquipment = setOf(Equipment.BODYWEIGHT),
            optionalEquipment = setOf(Equipment.DUMBBELL),
            movementPatterns = setOf(MovementPattern.LUNGE),
            requiresWeight = false,
        )

        createExercise(
            name = "Calf Raises",
            category = ExerciseCategory.LEGS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.CALVES),
            secondaryMuscles = setOf(),
            requiredEquipment = setOf(Equipment.BODYWEIGHT),
            optionalEquipment = setOf(Equipment.DUMBBELL, Equipment.CALF_RAISE),
            movementPatterns = setOf(MovementPattern.PUSH),
            requiresWeight = false,
        )
    }

    private suspend fun seedAccessoryMovements() {
        // Face pulls, lateral raises, etc.
        createExercise(
            name = "Face Pulls",
            category = ExerciseCategory.SHOULDERS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.REAR_DELTS),
            secondaryMuscles = setOf(MuscleGroup.UPPER_BACK, MuscleGroup.BICEPS),
            requiredEquipment = setOf(Equipment.CABLE_MACHINE),
            alternativeEquipment = setOf(Equipment.RESISTANCE_BAND),
            movementPatterns = setOf(MovementPattern.HORIZONTAL_PULL),
        )

        // Chest exercises
        createExercise(
            name = "Push-ups",
            category = ExerciseCategory.CHEST,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.CHEST),
            secondaryMuscles = setOf(MuscleGroup.TRICEPS, MuscleGroup.FRONT_DELTS),
            requiredEquipment = setOf(Equipment.BODYWEIGHT),
            movementPatterns = setOf(MovementPattern.HORIZONTAL_PUSH),
            requiresWeight = false,
        )

        createExercise(
            name = "Dumbbell Bench Press",
            category = ExerciseCategory.CHEST,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.CHEST),
            secondaryMuscles = setOf(MuscleGroup.TRICEPS, MuscleGroup.FRONT_DELTS),
            requiredEquipment = setOf(Equipment.DUMBBELL),
            movementPatterns = setOf(MovementPattern.HORIZONTAL_PUSH),
        )

        createExercise(
            name = "Incline Dumbbell Press",
            category = ExerciseCategory.CHEST,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.CHEST, MuscleGroup.FRONT_DELTS),
            secondaryMuscles = setOf(MuscleGroup.TRICEPS),
            requiredEquipment = setOf(Equipment.DUMBBELL),
            movementPatterns = setOf(MovementPattern.PUSH),
        )

        createExercise(
            name = "Decline Bench Press",
            category = ExerciseCategory.CHEST,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.INTERMEDIATE,
            primaryMuscles = setOf(MuscleGroup.CHEST),
            secondaryMuscles = setOf(MuscleGroup.TRICEPS, MuscleGroup.FRONT_DELTS),
            requiredEquipment = setOf(Equipment.BARBELL),
            movementPatterns = setOf(MovementPattern.HORIZONTAL_PUSH),
        )

        createExercise(
            name = "Chest Press Machine",
            category = ExerciseCategory.CHEST,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.CHEST),
            secondaryMuscles = setOf(MuscleGroup.TRICEPS, MuscleGroup.FRONT_DELTS),
            requiredEquipment = setOf(Equipment.MACHINE),
            movementPatterns = setOf(MovementPattern.HORIZONTAL_PUSH),
        )

        // Back exercises
        createExercise(
            name = "Dumbbell Row",
            category = ExerciseCategory.BACK,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.UPPER_BACK, MuscleGroup.LATS),
            secondaryMuscles = setOf(MuscleGroup.BICEPS, MuscleGroup.REAR_DELTS),
            requiredEquipment = setOf(Equipment.DUMBBELL),
            movementPatterns = setOf(MovementPattern.HORIZONTAL_PULL),
        )

        createExercise(
            name = "Cable Row Wide Grip",
            category = ExerciseCategory.BACK,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.UPPER_BACK, MuscleGroup.LATS),
            secondaryMuscles = setOf(MuscleGroup.BICEPS, MuscleGroup.REAR_DELTS),
            requiredEquipment = setOf(Equipment.CABLE_MACHINE),
            movementPatterns = setOf(MovementPattern.HORIZONTAL_PULL),
        )

        createExercise(
            name = "Machine Row",
            category = ExerciseCategory.BACK,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.UPPER_BACK, MuscleGroup.LATS),
            secondaryMuscles = setOf(MuscleGroup.BICEPS, MuscleGroup.REAR_DELTS),
            requiredEquipment = setOf(Equipment.MACHINE),
            movementPatterns = setOf(MovementPattern.HORIZONTAL_PULL),
        )

        createExercise(
            name = "Chest Supported Row",
            category = ExerciseCategory.BACK,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.UPPER_BACK, MuscleGroup.LATS),
            secondaryMuscles = setOf(MuscleGroup.BICEPS, MuscleGroup.REAR_DELTS),
            requiredEquipment = setOf(Equipment.DUMBBELL),
            movementPatterns = setOf(MovementPattern.HORIZONTAL_PULL),
        )

        createExercise(
            name = "Wide Grip Lat Pulldown",
            category = ExerciseCategory.BACK,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.LATS),
            secondaryMuscles = setOf(MuscleGroup.BICEPS, MuscleGroup.UPPER_BACK),
            requiredEquipment = setOf(Equipment.CABLE_MACHINE),
            movementPatterns = setOf(MovementPattern.VERTICAL_PULL),
        )

        createExercise(
            name = "Close Grip Lat Pulldown",
            category = ExerciseCategory.BACK,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.LATS),
            secondaryMuscles = setOf(MuscleGroup.BICEPS, MuscleGroup.UPPER_BACK),
            requiredEquipment = setOf(Equipment.CABLE_MACHINE),
            movementPatterns = setOf(MovementPattern.VERTICAL_PULL),
        )

        // Shoulder exercises
        createExercise(
            name = "Machine Shoulder Press",
            category = ExerciseCategory.SHOULDERS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.FRONT_DELTS, MuscleGroup.SIDE_DELTS),
            secondaryMuscles = setOf(MuscleGroup.TRICEPS),
            requiredEquipment = setOf(Equipment.MACHINE),
            movementPatterns = setOf(MovementPattern.VERTICAL_PUSH),
        )

        createExercise(
            name = "Cable Lateral Raise",
            category = ExerciseCategory.SHOULDERS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.SIDE_DELTS),
            secondaryMuscles = setOf(),
            requiredEquipment = setOf(Equipment.CABLE_MACHINE),
            movementPatterns = setOf(MovementPattern.PUSH),
        )

        createExercise(
            name = "Rear Delt Fly",
            category = ExerciseCategory.SHOULDERS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.REAR_DELTS),
            secondaryMuscles = setOf(MuscleGroup.UPPER_BACK),
            requiredEquipment = setOf(Equipment.DUMBBELL),
            alternativeEquipment = setOf(Equipment.CABLE_MACHINE),
            movementPatterns = setOf(MovementPattern.HORIZONTAL_PULL),
        )

        createExercise(
            name = "Cable Rear Delt Fly",
            category = ExerciseCategory.SHOULDERS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.REAR_DELTS),
            secondaryMuscles = setOf(MuscleGroup.UPPER_BACK),
            requiredEquipment = setOf(Equipment.CABLE_MACHINE),
            movementPatterns = setOf(MovementPattern.HORIZONTAL_PULL),
        )

        // Leg exercises
        createExercise(
            name = "Goblet Squat",
            category = ExerciseCategory.LEGS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.QUADS, MuscleGroup.GLUTES),
            secondaryMuscles = setOf(MuscleGroup.HAMSTRINGS, MuscleGroup.ABS),
            requiredEquipment = setOf(Equipment.DUMBBELL),
            movementPatterns = setOf(MovementPattern.SQUAT),
        )

        createExercise(
            name = "Dumbbell Lunges",
            category = ExerciseCategory.LEGS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.QUADS, MuscleGroup.GLUTES),
            secondaryMuscles = setOf(MuscleGroup.HAMSTRINGS, MuscleGroup.ABS),
            requiredEquipment = setOf(Equipment.DUMBBELL),
            movementPatterns = setOf(MovementPattern.LUNGE),
        )

        createExercise(
            name = "Stiff Leg Deadlift",
            category = ExerciseCategory.LEGS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.INTERMEDIATE,
            primaryMuscles = setOf(MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES),
            secondaryMuscles = setOf(MuscleGroup.LOWER_BACK),
            requiredEquipment = setOf(Equipment.DUMBBELL),
            alternativeEquipment = setOf(Equipment.BARBELL),
            movementPatterns = setOf(MovementPattern.HINGE),
        )

        createExercise(
            name = "Single Leg Romanian Deadlift",
            category = ExerciseCategory.LEGS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.INTERMEDIATE,
            primaryMuscles = setOf(MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES),
            secondaryMuscles = setOf(MuscleGroup.ABS, MuscleGroup.LOWER_BACK),
            requiredEquipment = setOf(Equipment.DUMBBELL),
            movementPatterns = setOf(MovementPattern.HINGE),
        )

        createExercise(
            name = "Hack Squat",
            category = ExerciseCategory.LEGS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.QUADS, MuscleGroup.GLUTES),
            secondaryMuscles = setOf(MuscleGroup.HAMSTRINGS),
            requiredEquipment = setOf(Equipment.MACHINE),
            movementPatterns = setOf(MovementPattern.SQUAT),
        )

        createExercise(
            name = "Lying Leg Curls",
            category = ExerciseCategory.LEGS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.HAMSTRINGS),
            secondaryMuscles = setOf(),
            requiredEquipment = setOf(Equipment.LEG_CURL),
            movementPatterns = setOf(MovementPattern.PULL),
        )

        createExercise(
            name = "Seated Leg Curls",
            category = ExerciseCategory.LEGS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.HAMSTRINGS),
            secondaryMuscles = setOf(),
            requiredEquipment = setOf(Equipment.LEG_CURL),
            movementPatterns = setOf(MovementPattern.PULL),
        )

        createExercise(
            name = "Seated Calf Raise",
            category = ExerciseCategory.LEGS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.CALVES),
            secondaryMuscles = setOf(),
            requiredEquipment = setOf(Equipment.CALF_RAISE),
            movementPatterns = setOf(MovementPattern.PUSH),
        )

        createExercise(
            name = "Standing Calf Raise",
            category = ExerciseCategory.LEGS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.CALVES),
            secondaryMuscles = setOf(),
            requiredEquipment = setOf(Equipment.CALF_RAISE),
            movementPatterns = setOf(MovementPattern.PUSH),
        )

        // Arms - more bicep and tricep variations
        createExercise(
            name = "EZ Bar Curls",
            category = ExerciseCategory.ARMS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.BICEPS),
            secondaryMuscles = setOf(MuscleGroup.FOREARMS),
            requiredEquipment = setOf(Equipment.BARBELL),
            movementPatterns = setOf(MovementPattern.PULL),
        )

        createExercise(
            name = "Concentration Curls",
            category = ExerciseCategory.ARMS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.BICEPS),
            secondaryMuscles = setOf(),
            requiredEquipment = setOf(Equipment.DUMBBELL),
            movementPatterns = setOf(MovementPattern.PULL),
        )

        createExercise(
            name = "Cable Hammer Curls",
            category = ExerciseCategory.ARMS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.BICEPS, MuscleGroup.FOREARMS),
            secondaryMuscles = setOf(),
            requiredEquipment = setOf(Equipment.CABLE_MACHINE),
            movementPatterns = setOf(MovementPattern.PULL),
        )

        createExercise(
            name = "Diamond Push-ups",
            category = ExerciseCategory.ARMS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.INTERMEDIATE,
            primaryMuscles = setOf(MuscleGroup.TRICEPS),
            secondaryMuscles = setOf(MuscleGroup.CHEST, MuscleGroup.FRONT_DELTS),
            requiredEquipment = setOf(Equipment.BODYWEIGHT),
            movementPatterns = setOf(MovementPattern.HORIZONTAL_PUSH),
            requiresWeight = false,
        )

        createExercise(
            name = "Cable Overhead Extension",
            category = ExerciseCategory.ARMS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.TRICEPS),
            secondaryMuscles = setOf(),
            requiredEquipment = setOf(Equipment.CABLE_MACHINE),
            movementPatterns = setOf(MovementPattern.PUSH),
        )

        createExercise(
            name = "French Press",
            category = ExerciseCategory.ARMS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.INTERMEDIATE,
            primaryMuscles = setOf(MuscleGroup.TRICEPS),
            secondaryMuscles = setOf(),
            requiredEquipment = setOf(Equipment.DUMBBELL),
            alternativeEquipment = setOf(Equipment.BARBELL),
            movementPatterns = setOf(MovementPattern.PUSH),
        )

        // Core and cardio exercises
        createExercise(
            name = "Mountain Climbers",
            category = ExerciseCategory.CARDIO,
            type = ExerciseType.CARDIO,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.ABS),
            secondaryMuscles = setOf(MuscleGroup.FULL_BODY),
            requiredEquipment = setOf(Equipment.BODYWEIGHT),
            movementPatterns = setOf(MovementPattern.PLANK),
            requiresWeight = false,
        )

        createExercise(
            name = "Burpees",
            category = ExerciseCategory.CARDIO,
            type = ExerciseType.CARDIO,
            difficulty = ExerciseDifficulty.INTERMEDIATE,
            primaryMuscles = setOf(MuscleGroup.FULL_BODY),
            secondaryMuscles = setOf(),
            requiredEquipment = setOf(Equipment.BODYWEIGHT),
            movementPatterns = setOf(MovementPattern.SQUAT, MovementPattern.HORIZONTAL_PUSH),
            requiresWeight = false,
        )

        createExercise(
            name = "Jumping Jacks",
            category = ExerciseCategory.CARDIO,
            type = ExerciseType.CARDIO,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.FULL_BODY),
            secondaryMuscles = setOf(),
            requiredEquipment = setOf(Equipment.BODYWEIGHT),
            movementPatterns = setOf(MovementPattern.JUMP),
            requiresWeight = false,
        )

        createExercise(
            name = "Dead Bug",
            category = ExerciseCategory.CORE,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.ABS),
            secondaryMuscles = setOf(MuscleGroup.LOWER_BACK),
            requiredEquipment = setOf(Equipment.BODYWEIGHT),
            movementPatterns = setOf(MovementPattern.PLANK),
            requiresWeight = false,
        )

        createExercise(
            name = "Bird Dog",
            category = ExerciseCategory.CORE,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.LOWER_BACK, MuscleGroup.ABS),
            secondaryMuscles = setOf(MuscleGroup.GLUTES),
            requiredEquipment = setOf(Equipment.BODYWEIGHT),
            movementPatterns = setOf(MovementPattern.PLANK),
            requiresWeight = false,
        )

        createExercise(
            name = "Side Plank",
            category = ExerciseCategory.CORE,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.INTERMEDIATE,
            primaryMuscles = setOf(MuscleGroup.OBLIQUES),
            secondaryMuscles = setOf(MuscleGroup.ABS, MuscleGroup.LOWER_BACK),
            requiredEquipment = setOf(Equipment.BODYWEIGHT),
            movementPatterns = setOf(MovementPattern.PLANK),
            requiresWeight = false,
        )

        createExercise(
            name = "Bicycle Crunches",
            category = ExerciseCategory.CORE,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.ABS, MuscleGroup.OBLIQUES),
            secondaryMuscles = setOf(),
            requiredEquipment = setOf(Equipment.BODYWEIGHT),
            movementPatterns = setOf(MovementPattern.ROTATE),
            requiresWeight = false,
        )

        createExercise(
            name = "Reverse Crunches",
            category = ExerciseCategory.CORE,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.ABS),
            secondaryMuscles = setOf(),
            requiredEquipment = setOf(Equipment.BODYWEIGHT),
            movementPatterns = setOf(MovementPattern.PULL),
            requiresWeight = false,
        )

        // Additional popular exercises
        createExercise(
            name = "Incline Push-ups",
            category = ExerciseCategory.CHEST,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.CHEST),
            secondaryMuscles = setOf(MuscleGroup.TRICEPS, MuscleGroup.FRONT_DELTS),
            requiredEquipment = setOf(Equipment.BODYWEIGHT),
            movementPatterns = setOf(MovementPattern.HORIZONTAL_PUSH),
            requiresWeight = false,
        )

        createExercise(
            name = "Decline Push-ups",
            category = ExerciseCategory.CHEST,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.INTERMEDIATE,
            primaryMuscles = setOf(MuscleGroup.CHEST),
            secondaryMuscles = setOf(MuscleGroup.TRICEPS, MuscleGroup.FRONT_DELTS),
            requiredEquipment = setOf(Equipment.BODYWEIGHT),
            movementPatterns = setOf(MovementPattern.HORIZONTAL_PUSH),
            requiresWeight = false,
        )

        createExercise(
            name = "Pike Push-ups",
            category = ExerciseCategory.SHOULDERS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.INTERMEDIATE,
            primaryMuscles = setOf(MuscleGroup.FRONT_DELTS, MuscleGroup.SIDE_DELTS),
            secondaryMuscles = setOf(MuscleGroup.TRICEPS),
            requiredEquipment = setOf(Equipment.BODYWEIGHT),
            movementPatterns = setOf(MovementPattern.VERTICAL_PUSH),
            requiresWeight = false,
        )

        createExercise(
            name = "Assisted Pull-ups",
            category = ExerciseCategory.BACK,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.LATS, MuscleGroup.UPPER_BACK),
            secondaryMuscles = setOf(MuscleGroup.BICEPS, MuscleGroup.REAR_DELTS),
            requiredEquipment = setOf(Equipment.PULL_UP_BAR),
            alternativeEquipment = setOf(Equipment.RESISTANCE_BAND),
            movementPatterns = setOf(MovementPattern.VERTICAL_PULL),
            requiresWeight = false,
        )

        createExercise(
            name = "Negative Pull-ups",
            category = ExerciseCategory.BACK,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.INTERMEDIATE,
            primaryMuscles = setOf(MuscleGroup.LATS, MuscleGroup.UPPER_BACK),
            secondaryMuscles = setOf(MuscleGroup.BICEPS, MuscleGroup.REAR_DELTS),
            requiredEquipment = setOf(Equipment.PULL_UP_BAR),
            movementPatterns = setOf(MovementPattern.VERTICAL_PULL),
            requiresWeight = false,
        )

        createExercise(
            name = "Step-ups",
            category = ExerciseCategory.LEGS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.QUADS, MuscleGroup.GLUTES),
            secondaryMuscles = setOf(MuscleGroup.HAMSTRINGS, MuscleGroup.CALVES),
            requiredEquipment = setOf(Equipment.BODYWEIGHT),
            optionalEquipment = setOf(Equipment.DUMBBELL),
            movementPatterns = setOf(MovementPattern.LUNGE),
        )

        createExercise(
            name = "Jump Squats",
            category = ExerciseCategory.LEGS,
            type = ExerciseType.POWER,
            difficulty = ExerciseDifficulty.INTERMEDIATE,
            primaryMuscles = setOf(MuscleGroup.QUADS, MuscleGroup.GLUTES),
            secondaryMuscles = setOf(MuscleGroup.HAMSTRINGS, MuscleGroup.CALVES),
            requiredEquipment = setOf(Equipment.BODYWEIGHT),
            movementPatterns = setOf(MovementPattern.SQUAT, MovementPattern.JUMP),
            requiresWeight = false,
        )

        createExercise(
            name = "Glute Bridges",
            category = ExerciseCategory.LEGS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.GLUTES),
            secondaryMuscles = setOf(MuscleGroup.HAMSTRINGS),
            requiredEquipment = setOf(Equipment.BODYWEIGHT),
            optionalEquipment = setOf(Equipment.DUMBBELL),
            movementPatterns = setOf(MovementPattern.HINGE),
            requiresWeight = false,
        )

        createExercise(
            name = "Single Leg Glute Bridge",
            category = ExerciseCategory.LEGS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.INTERMEDIATE,
            primaryMuscles = setOf(MuscleGroup.GLUTES),
            secondaryMuscles = setOf(MuscleGroup.HAMSTRINGS, MuscleGroup.ABS),
            requiredEquipment = setOf(Equipment.BODYWEIGHT),
            movementPatterns = setOf(MovementPattern.HINGE),
            requiresWeight = false,
        )

        createExercise(
            name = "Wall Sit",
            category = ExerciseCategory.LEGS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.QUADS, MuscleGroup.GLUTES),
            secondaryMuscles = setOf(),
            requiredEquipment = setOf(Equipment.BODYWEIGHT),
            movementPatterns = setOf(MovementPattern.SQUAT),
            requiresWeight = false,
        )

        createExercise(
            name = "Wrist Curls",
            category = ExerciseCategory.ARMS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.FOREARMS),
            secondaryMuscles = setOf(),
            requiredEquipment = setOf(Equipment.DUMBBELL),
            movementPatterns = setOf(MovementPattern.PULL),
        )

        createExercise(
            name = "Reverse Wrist Curls",
            category = ExerciseCategory.ARMS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.FOREARMS),
            secondaryMuscles = setOf(),
            requiredEquipment = setOf(Equipment.DUMBBELL),
            movementPatterns = setOf(MovementPattern.PULL),
        )

        createExercise(
            name = "Lateral Raises",
            category = ExerciseCategory.SHOULDERS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.SIDE_DELTS),
            secondaryMuscles = setOf(),
            requiredEquipment = setOf(Equipment.DUMBBELL),
            movementPatterns = setOf(MovementPattern.PUSH),
        )

        createExercise(
            name = "Plank",
            category = ExerciseCategory.CORE,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.ABS),
            secondaryMuscles = setOf(MuscleGroup.OBLIQUES, MuscleGroup.LOWER_BACK),
            requiredEquipment = setOf(Equipment.BODYWEIGHT),
            movementPatterns = setOf(MovementPattern.PLANK),
            requiresWeight = false,
        )

        createExercise(
            name = "Russian Twists",
            category = ExerciseCategory.CORE,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.OBLIQUES, MuscleGroup.ABS),
            secondaryMuscles = setOf(),
            requiredEquipment = setOf(Equipment.BODYWEIGHT),
            optionalEquipment = setOf(Equipment.MEDICINE_BALL),
            movementPatterns = setOf(MovementPattern.ROTATE),
            requiresWeight = false,
        )

        createExercise(
            name = "Leg Press",
            category = ExerciseCategory.LEGS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.QUADS, MuscleGroup.GLUTES),
            secondaryMuscles = setOf(MuscleGroup.HAMSTRINGS),
            requiredEquipment = setOf(Equipment.LEG_PRESS_MACHINE),
            movementPatterns = setOf(MovementPattern.PUSH),
        )

        createExercise(
            name = "Leg Curls",
            category = ExerciseCategory.LEGS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.HAMSTRINGS),
            secondaryMuscles = setOf(),
            requiredEquipment = setOf(Equipment.LEG_CURL),
            movementPatterns = setOf(MovementPattern.PULL),
        )

        createExercise(
            name = "Leg Extensions",
            category = ExerciseCategory.LEGS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.QUADS),
            secondaryMuscles = setOf(),
            requiredEquipment = setOf(Equipment.LEG_EXTENSION),
            movementPatterns = setOf(MovementPattern.PUSH),
        )

        createExercise(
            name = "Cable Crossover",
            category = ExerciseCategory.CHEST,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.INTERMEDIATE,
            primaryMuscles = setOf(MuscleGroup.CHEST),
            secondaryMuscles = setOf(MuscleGroup.FRONT_DELTS),
            requiredEquipment = setOf(Equipment.CABLE_MACHINE),
            movementPatterns = setOf(MovementPattern.PUSH),
        )

        createExercise(
            name = "Dumbbell Flyes",
            category = ExerciseCategory.CHEST,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.CHEST),
            secondaryMuscles = setOf(MuscleGroup.FRONT_DELTS),
            requiredEquipment = setOf(Equipment.DUMBBELL),
            movementPatterns = setOf(MovementPattern.PUSH),
        )

        createExercise(
            name = "T-Bar Row",
            category = ExerciseCategory.BACK,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.INTERMEDIATE,
            primaryMuscles = setOf(MuscleGroup.UPPER_BACK, MuscleGroup.LATS),
            secondaryMuscles = setOf(MuscleGroup.BICEPS, MuscleGroup.REAR_DELTS),
            requiredEquipment = setOf(Equipment.BARBELL),
            movementPatterns = setOf(MovementPattern.HORIZONTAL_PULL),
        )

        createExercise(
            name = "Seated Cable Row",
            category = ExerciseCategory.BACK,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.UPPER_BACK, MuscleGroup.LATS),
            secondaryMuscles = setOf(MuscleGroup.BICEPS, MuscleGroup.REAR_DELTS),
            requiredEquipment = setOf(Equipment.CABLE_MACHINE),
            movementPatterns = setOf(MovementPattern.HORIZONTAL_PULL),
        )

        createExercise(
            name = "Lat Pulldown",
            category = ExerciseCategory.BACK,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.LATS),
            secondaryMuscles = setOf(MuscleGroup.BICEPS, MuscleGroup.UPPER_BACK),
            requiredEquipment = setOf(Equipment.CABLE_MACHINE),
            movementPatterns = setOf(MovementPattern.VERTICAL_PULL),
        )

        createExercise(
            name = "Cable Bicep Curls",
            category = ExerciseCategory.ARMS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.BICEPS),
            secondaryMuscles = setOf(MuscleGroup.FOREARMS),
            requiredEquipment = setOf(Equipment.CABLE_MACHINE),
            movementPatterns = setOf(MovementPattern.PULL),
        )

        createExercise(
            name = "Preacher Curls",
            category = ExerciseCategory.ARMS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.INTERMEDIATE,
            primaryMuscles = setOf(MuscleGroup.BICEPS),
            secondaryMuscles = setOf(MuscleGroup.FOREARMS),
            requiredEquipment = setOf(Equipment.PREACHER_BENCH),
            alternativeEquipment = setOf(Equipment.DUMBBELL, Equipment.BARBELL),
            movementPatterns = setOf(MovementPattern.PULL),
        )

        createExercise(
            name = "Skull Crushers",
            category = ExerciseCategory.ARMS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.INTERMEDIATE,
            primaryMuscles = setOf(MuscleGroup.TRICEPS),
            secondaryMuscles = setOf(),
            requiredEquipment = setOf(Equipment.BARBELL),
            alternativeEquipment = setOf(Equipment.DUMBBELL),
            movementPatterns = setOf(MovementPattern.PUSH),
        )

        createExercise(
            name = "Cable Tricep Pushdown",
            category = ExerciseCategory.ARMS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.TRICEPS),
            secondaryMuscles = setOf(),
            requiredEquipment = setOf(Equipment.CABLE_MACHINE),
            movementPatterns = setOf(MovementPattern.PUSH),
        )

        createExercise(
            name = "Arnold Press",
            category = ExerciseCategory.SHOULDERS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.INTERMEDIATE,
            primaryMuscles = setOf(MuscleGroup.FRONT_DELTS, MuscleGroup.SIDE_DELTS),
            secondaryMuscles = setOf(MuscleGroup.TRICEPS),
            requiredEquipment = setOf(Equipment.DUMBBELL),
            movementPatterns = setOf(MovementPattern.VERTICAL_PUSH),
        )

        createExercise(
            name = "Upright Row",
            category = ExerciseCategory.SHOULDERS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.INTERMEDIATE,
            primaryMuscles = setOf(MuscleGroup.SIDE_DELTS, MuscleGroup.TRAPS),
            secondaryMuscles = setOf(MuscleGroup.BICEPS),
            requiredEquipment = setOf(Equipment.BARBELL),
            alternativeEquipment = setOf(Equipment.DUMBBELL),
            movementPatterns = setOf(MovementPattern.VERTICAL_PULL),
        )

        createExercise(
            name = "Shrugs",
            category = ExerciseCategory.SHOULDERS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.TRAPS),
            secondaryMuscles = setOf(),
            requiredEquipment = setOf(Equipment.DUMBBELL),
            alternativeEquipment = setOf(Equipment.BARBELL),
            movementPatterns = setOf(MovementPattern.PULL),
        )

        createExercise(
            name = "Good Mornings",
            category = ExerciseCategory.LEGS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.INTERMEDIATE,
            primaryMuscles = setOf(MuscleGroup.HAMSTRINGS, MuscleGroup.LOWER_BACK),
            secondaryMuscles = setOf(MuscleGroup.GLUTES),
            requiredEquipment = setOf(Equipment.BARBELL),
            movementPatterns = setOf(MovementPattern.HINGE),
        )

        createExercise(
            name = "Hip Thrust",
            category = ExerciseCategory.LEGS,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.INTERMEDIATE,
            primaryMuscles = setOf(MuscleGroup.GLUTES),
            secondaryMuscles = setOf(MuscleGroup.HAMSTRINGS),
            requiredEquipment = setOf(Equipment.BARBELL),
            alternativeEquipment = setOf(Equipment.DUMBBELL),
            movementPatterns = setOf(MovementPattern.HINGE),
        )

        createExercise(
            name = "Ab Wheel Rollout",
            category = ExerciseCategory.CORE,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.ADVANCED,
            primaryMuscles = setOf(MuscleGroup.ABS),
            secondaryMuscles = setOf(MuscleGroup.OBLIQUES, MuscleGroup.LOWER_BACK),
            requiredEquipment = setOf(Equipment.AB_WHEEL),
            movementPatterns = setOf(MovementPattern.PLANK),
            requiresWeight = false,
        )

        createExercise(
            name = "Hanging Leg Raises",
            category = ExerciseCategory.CORE,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.INTERMEDIATE,
            primaryMuscles = setOf(MuscleGroup.ABS),
            secondaryMuscles = setOf(MuscleGroup.OBLIQUES),
            requiredEquipment = setOf(Equipment.PULL_UP_BAR),
            movementPatterns = setOf(MovementPattern.PULL),
        )

        createExercise(
            name = "Cable Crunches",
            category = ExerciseCategory.CORE,
            type = ExerciseType.STRENGTH,
            difficulty = ExerciseDifficulty.BEGINNER,
            primaryMuscles = setOf(MuscleGroup.ABS),
            secondaryMuscles = setOf(),
            requiredEquipment = setOf(Equipment.CABLE_MACHINE),
            movementPatterns = setOf(MovementPattern.PULL),
        )
    }

    private suspend fun createExercise(
        name: String,
        category: ExerciseCategory,
        type: ExerciseType = ExerciseType.STRENGTH,
        difficulty: ExerciseDifficulty = ExerciseDifficulty.BEGINNER,
        primaryMuscles: Set<MuscleGroup>,
        secondaryMuscles: Set<MuscleGroup> = emptySet(),
        requiredEquipment: Set<Equipment>,
        optionalEquipment: Set<Equipment> = emptySet(),
        alternativeEquipment: Set<Equipment> = emptySet(),
        movementPatterns: Set<MovementPattern>,
        instructions: String? = null,
        tips: String? = null,
        commonMistakes: String? = null,
        requiresWeight: Boolean = true,
    ) {
        val exercise =
            Exercise(
                name = name,
                category = category,
                type = type,
                difficulty = difficulty,
                requiresWeight = requiresWeight,
                instructions = instructions,
                tips = tips,
                commonMistakes = commonMistakes,
            )

        val muscleGroups =
            primaryMuscles.map {
                ExerciseMuscleGroup(0, it, isPrimary = true)
            } +
                secondaryMuscles.map {
                    ExerciseMuscleGroup(0, it, isPrimary = false)
                }

        val equipment =
            requiredEquipment.map {
                ExerciseEquipment(0, it, isRequired = true, isAlternative = false)
            } +
                optionalEquipment.map {
                    ExerciseEquipment(0, it, isRequired = false, isAlternative = false)
                } +
                alternativeEquipment.map {
                    ExerciseEquipment(0, it, isRequired = false, isAlternative = true)
                }

        val patterns =
            movementPatterns.map {
                ExerciseMovementPattern(0, it, isPrimary = true)
            }

        exerciseDao.insertExerciseWithDetails(exercise, muscleGroups, equipment, patterns)
    }
}
