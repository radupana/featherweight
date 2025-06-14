package com.github.radupana.featherweight.data.exercise

enum class MuscleGroup(
    val displayName: String,
) {
    // Upper Body
    CHEST("Chest"),
    UPPER_BACK("Upper Back"),
    LATS("Lats"),
    LOWER_BACK("Lower Back"),
    TRAPS("Traps"),
    RHOMBOIDS("Rhomboids"),

    // Shoulders
    FRONT_DELTS("Front Delts"),
    SIDE_DELTS("Side Delts"),
    REAR_DELTS("Rear Delts"),

    // Arms
    BICEPS("Biceps"),
    TRICEPS("Triceps"),
    FOREARMS("Forearms"),

    // Lower Body
    QUADS("Quadriceps"),
    HAMSTRINGS("Hamstrings"),
    GLUTES("Glutes"),
    CALVES("Calves"),

    // Core
    ABS("Abs"),
    OBLIQUES("Obliques"),

    // Full Body
    FULL_BODY("Full Body"),
    ;

    companion object {
        fun getByCategory(category: ExerciseCategory): List<MuscleGroup> =
            when (category) {
                ExerciseCategory.CHEST -> listOf(CHEST, FRONT_DELTS, TRICEPS)
                ExerciseCategory.BACK -> listOf(UPPER_BACK, LATS, LOWER_BACK, TRAPS, RHOMBOIDS, BICEPS)
                ExerciseCategory.SHOULDERS -> listOf(FRONT_DELTS, SIDE_DELTS, REAR_DELTS)
                ExerciseCategory.ARMS -> listOf(BICEPS, TRICEPS, FOREARMS)
                ExerciseCategory.LEGS -> listOf(QUADS, HAMSTRINGS, GLUTES, CALVES)
                ExerciseCategory.CORE -> listOf(ABS, OBLIQUES)
                ExerciseCategory.CARDIO -> listOf(FULL_BODY)
                ExerciseCategory.FULL_BODY -> values().toList()
            }

        fun fromCategory(category: ExerciseCategory): MuscleGroup =
            when (category) {
                ExerciseCategory.CHEST -> CHEST
                ExerciseCategory.BACK -> UPPER_BACK
                ExerciseCategory.SHOULDERS -> FRONT_DELTS
                ExerciseCategory.ARMS -> BICEPS
                ExerciseCategory.LEGS -> QUADS
                ExerciseCategory.CORE -> ABS
                ExerciseCategory.CARDIO -> FULL_BODY
                ExerciseCategory.FULL_BODY -> FULL_BODY
            }
    }
}

enum class ExerciseCategory(
    val displayName: String,
) {
    CHEST("Chest"),
    BACK("Back"),
    SHOULDERS("Shoulders"),
    ARMS("Arms"),
    LEGS("Legs"),
    CORE("Core"),
    CARDIO("Cardio"),
    FULL_BODY("Full Body"),
    ;

    companion object {
        fun fromMuscleGroups(muscles: Set<MuscleGroup>): ExerciseCategory =
            when {
                muscles.any { it in listOf(MuscleGroup.CHEST) } -> CHEST
                muscles.any { it in listOf(MuscleGroup.UPPER_BACK, MuscleGroup.LATS, MuscleGroup.LOWER_BACK) } -> BACK
                muscles.any { it in listOf(MuscleGroup.FRONT_DELTS, MuscleGroup.SIDE_DELTS, MuscleGroup.REAR_DELTS) } -> SHOULDERS
                muscles.any { it in listOf(MuscleGroup.BICEPS, MuscleGroup.TRICEPS, MuscleGroup.FOREARMS) } -> ARMS
                muscles.any { it in listOf(MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES) } -> LEGS
                muscles.any { it in listOf(MuscleGroup.ABS, MuscleGroup.OBLIQUES) } -> CORE
                muscles.contains(MuscleGroup.FULL_BODY) -> CARDIO
                else -> FULL_BODY
            }
    }
}

enum class Equipment(
    val displayName: String,
) {
    // Free Weights
    BARBELL("Barbell"),
    DUMBBELL("Dumbbell"),
    KETTLEBELL("Kettlebell"),
    PLATES("Weight Plates"),

    // Machines
    CABLE_MACHINE("Cable Machine"),
    MACHINE("Machine"),
    SMITH_MACHINE("Smith Machine"),
    LEG_PRESS("Leg Press"),
    LAT_PULLDOWN("Lat Pulldown"),
    SEATED_ROW("Seated Row"),
    CHEST_PRESS("Chest Press Machine"),
    SHOULDER_PRESS("Shoulder Press Machine"),
    LEG_CURL("Leg Curl Machine"),
    LEG_EXTENSION("Leg Extension Machine"),
    CALF_RAISE("Calf Raise Machine"),

    // Bodyweight/Basic
    BODYWEIGHT("Bodyweight"),
    PULL_UP_BAR("Pull-up Bar"),
    DIP_STATION("Dip Station"),
    PARALLEL_BARS("Parallel Bars"),

    LEG_PRESS_MACHINE("Leg Press Machine"),
    PREACHER_BENCH("Preacher Bench"),
    AB_WHEEL("Ab Wheel"),

    // Accessories
    RESISTANCE_BAND("Resistance Band"),
    SUSPENSION_TRAINER("Suspension Trainer"),
    MEDICINE_BALL("Medicine Ball"),
    BOSU_BALL("Bosu Ball"),
    FOAM_ROLLER("Foam Roller"),

    // Cardio
    TREADMILL("Treadmill"),
    BIKE("Exercise Bike"),
    ROWING_MACHINE("Rowing Machine"),
    ELLIPTICAL("Elliptical"),

    NONE("No Equipment"),
    ;

    companion object {
        fun getBasicEquipment(): List<Equipment> = listOf(BODYWEIGHT, BARBELL, DUMBBELL, PULL_UP_BAR, DIP_STATION)

        fun getGymEquipment(): List<Equipment> = values().filter { it != NONE }

        fun getHomeEquipment(): List<Equipment> = listOf(BODYWEIGHT, DUMBBELL, KETTLEBELL, RESISTANCE_BAND, PULL_UP_BAR)
    }
}

enum class MovementPattern(
    val displayName: String,
) {
    // Primary Patterns (based on functional movement)
    SQUAT("Squat"),
    HINGE("Hip Hinge"),
    PUSH("Push"),
    PULL("Pull"),
    LUNGE("Lunge"),
    CARRY("Carry"),
    ROTATE("Rotate"),

    // Exercise-specific patterns
    VERTICAL_PUSH("Vertical Push"),
    HORIZONTAL_PUSH("Horizontal Push"),
    VERTICAL_PULL("Vertical Pull"),
    HORIZONTAL_PULL("Horizontal Pull"),

    // Core/Stability
    PLANK("Plank/Hold"),
    ANTI_EXTENSION("Anti-Extension"),
    ANTI_ROTATION("Anti-Rotation"),

    // Cardio/Conditioning
    GAIT("Walking/Running"),
    CONDITIONING("Conditioning"),
    JUMP("Jump/Plyometric"),
    ;

    companion object {
        fun getCompoundMovements(): List<MovementPattern> = listOf(SQUAT, HINGE, PUSH, PULL, LUNGE)
    }
}

enum class ExerciseDifficulty(
    val displayName: String,
    val level: Int,
) {
    BEGINNER("Beginner", 1),
    NOVICE("Novice", 2),
    INTERMEDIATE("Intermediate", 3),
    ADVANCED("Advanced", 4),
    EXPERT("Expert", 5),
    ;

    companion object {
        fun fromLevel(level: Int): ExerciseDifficulty = values().find { it.level == level } ?: BEGINNER
    }
}

enum class ExerciseType(
    val displayName: String,
) {
    STRENGTH("Strength Training"),
    CARDIO("Cardiovascular"),
    FLEXIBILITY("Flexibility/Mobility"),
    POWER("Power/Explosive"),
    ENDURANCE("Muscular Endurance"),
    BALANCE("Balance/Stability"),
    REHABILITATION("Rehabilitation"),
    WARMUP("Warm-up"),
    COOLDOWN("Cool-down"),
}
