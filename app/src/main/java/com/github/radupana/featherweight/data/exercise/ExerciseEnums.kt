package com.github.radupana.featherweight.data.exercise

enum class MuscleGroup(
    val displayName: String,
) {
    // Upper Body
    CHEST("Chest"),
    PECTORALS("Pectorals"), // Added for JSON compatibility
    BACK("Back"),
    UPPER_BACK("Upper Back"),
    MIDDLE_BACK("Middle Back"),
    LATS("Lats"),
    LOWER_BACK("Lower Back"),
    TRAPS("Traps"),
    RHOMBOIDS("Rhomboids"),
    SHOULDERS("Shoulders"),
    ROTATOR_CUFF("Rotator Cuff"), // Added for JSON compatibility

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
    QUADRICEPS("Quadriceps"), // Added for JSON compatibility
    HAMSTRINGS("Hamstrings"),
    GLUTES("Glutes"),
    CALVES("Calves"),
    ADDUCTORS("Adductors"), // Added for JSON compatibility

    // Core
    CORE("Core"),
    ABS("Abs"),
    OBLIQUES("Obliques"),

    // Full Body
    FULL_BODY("Full Body"),
    ;

    companion object {
        fun getByCategory(category: ExerciseCategory): List<MuscleGroup> =
            when (category) {
                ExerciseCategory.CHEST -> listOf(CHEST, PECTORALS, FRONT_DELTS, TRICEPS)
                ExerciseCategory.BACK -> listOf(UPPER_BACK, LATS, LOWER_BACK, TRAPS, RHOMBOIDS, BICEPS)
                ExerciseCategory.SHOULDERS -> listOf(FRONT_DELTS, SIDE_DELTS, REAR_DELTS, ROTATOR_CUFF)
                ExerciseCategory.ARMS -> listOf(BICEPS, TRICEPS, FOREARMS)
                ExerciseCategory.LEGS -> listOf(QUADS, QUADRICEPS, HAMSTRINGS, GLUTES, CALVES, ADDUCTORS)
                ExerciseCategory.CORE -> listOf(ABS, OBLIQUES, CORE)
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
                muscles.any { it in listOf(MuscleGroup.CHEST, MuscleGroup.PECTORALS) } -> CHEST
                muscles.any { it in listOf(MuscleGroup.UPPER_BACK, MuscleGroup.LATS, MuscleGroup.LOWER_BACK) } -> BACK
                muscles.any { it in listOf(MuscleGroup.FRONT_DELTS, MuscleGroup.SIDE_DELTS, MuscleGroup.REAR_DELTS, MuscleGroup.ROTATOR_CUFF) } -> SHOULDERS
                muscles.any { it in listOf(MuscleGroup.BICEPS, MuscleGroup.TRICEPS, MuscleGroup.FOREARMS) } -> ARMS
                muscles.any { it in listOf(MuscleGroup.QUADS, MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES, MuscleGroup.ADDUCTORS) } -> LEGS
                muscles.any { it in listOf(MuscleGroup.ABS, MuscleGroup.OBLIQUES, MuscleGroup.CORE) } -> CORE
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
    TRAP_BAR("Trap Bar"),
    SAFETY_BAR("Safety Bar"),
    BUFFALO_BAR("Buffalo Bar"),
    SWISS_BAR("Swiss Bar"),
    CAMBERED_BAR("Cambered Bar"),
    AXLE_BAR("Axle Bar"),

    // Machines
    CABLE("Cable Machine"),
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
    BELT_SQUAT("Belt Squat Machine"),
    GHD_MACHINE("GHD Machine"),

    // Bodyweight/Basic
    BODYWEIGHT("Bodyweight"),
    PULL_UP_BAR("Pull-up Bar"),
    DIP_STATION("Dip Station"),
    PARALLEL_BARS("Parallel Bars"),
    PARALLETTES("Parallettes"),
    RINGS("Gymnastic Rings"),
    BOX("Plyo Box"),
    BENCH("Bench"),

    LEG_PRESS_MACHINE("Leg Press Machine"),
    PREACHER_BENCH("Preacher Bench"),
    AB_WHEEL("Ab Wheel"),

    // Accessories
    RESISTANCE_BAND("Resistance Band"),
    BAND("Resistance Band"), // Alias for RESISTANCE_BAND
    SUSPENSION_TRAINER("Suspension Trainer"),
    TRX("TRX"),
    MEDICINE_BALL("Medicine Ball"),
    BOSU_BALL("Bosu Ball"),
    FOAM_ROLLER("Foam Roller"),
    STABILITY_BALL("Stability Ball"),
    POLE("Pole"),

    // Cardio
    TREADMILL("Treadmill"),
    BIKE("Exercise Bike"),
    ASSAULT_BIKE("Assault Bike"),
    ROWING_MACHINE("Rowing Machine"),
    ROWER("Rower"),
    ELLIPTICAL("Elliptical"),
    SKI_ERG("Ski Erg"),

    // Strongman Equipment
    LOG("Log"),
    YOKE("Yoke"),
    SLED("Sled"),
    TIRE("Tire"),
    SLEDGEHAMMER("Sledgehammer"),
    ATLAS_STONE("Atlas Stone"),
    KEG("Keg"),
    SANDBAG("Sandbag"),
    CAR_DEADLIFT("Car Deadlift Frame"),
    BATTLE_ROPES("Battle Ropes"),

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
    ROTATION("Rotation"), // Added for JSON compatibility

    // Exercise-specific patterns
    VERTICAL_PUSH("Vertical Push"),
    HORIZONTAL_PUSH("Horizontal Push"),
    VERTICAL_PULL("Vertical Pull"),
    HORIZONTAL_PULL("Horizontal Pull"),
    PRESS("Press"), // Added for JSON compatibility
    ROW("Row"), // Added for JSON compatibility
    CURL("Curl"), // Added for JSON compatibility
    EXTENSION("Extension"), // Added for JSON compatibility
    FLY("Fly"), // Added for JSON compatibility
    RAISE("Raise"), // Added for JSON compatibility
    SHRUG("Shrug"), // Added for JSON compatibility

    // Core/Stability
    PLANK("Plank/Hold"),
    ANTI_EXTENSION("Anti-Extension"),
    ANTI_ROTATION("Anti-Rotation"),
    CORE("Core"), // Added for JSON compatibility
    CRUNCH("Crunch"), // Added for JSON compatibility
    ROLLOUT("Rollout"), // Added for JSON compatibility
    HOLD("Hold"), // Added for JSON compatibility
    ISOMETRIC("Isometric"), // Added for JSON compatibility
    PIKE("Pike"), // Added for JSON compatibility
    TUCK("Tuck"), // Added for JSON compatibility
    ROLL("Roll"), // Added for JSON compatibility

    // Cardio/Conditioning
    GAIT("Walking/Running"),
    CONDITIONING("Conditioning"),
    JUMP("Jump/Plyometric"),
    SPRINT("Sprint"), // Added for JSON compatibility
    WALK("Walk"), // Added for JSON compatibility
    STEP("Step"), // Added for JSON compatibility
    CYCLE("Cycle"), // Added for JSON compatibility
    CRAWL("Crawl"), // Added for JSON compatibility
    LOCOMOTION("Locomotion"), // Added for JSON compatibility

    // Movement specific patterns from JSON
    ABDUCTION("Abduction"), // Added for JSON compatibility
    ADDUCTION("Adduction"), // Added for JSON compatibility
    CIRCLE("Circle"), // Added for JSON compatibility
    COMPLEX("Complex"), // Added for JSON compatibility
    EXPLOSIVE("Explosive"), // Added for JSON compatibility
    FLIP("Flip"), // Added for JSON compatibility
    KICK("Kick"), // Added for JSON compatibility
    LIFT("Lift"), // Added for JSON compatibility
    OLYMPIC("Olympic"), // Added for JSON compatibility
    SLAM("Slam"), // Added for JSON compatibility
    SWING("Swing"), // Added for JSON compatibility
    WAVE("Wave"), // Added for JSON compatibility
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
    CONDITIONING("Conditioning"),
    SKILL("Skill Work"),
}

enum class InstructionType {
    SETUP,
    EXECUTION,
    COMMON_MISTAKES,
    SAFETY,
    BREATHING,
    CUES,
    VARIATIONS,
    TIPS,
}

enum class ExerciseRelationType {
    PROGRESSION,
    REGRESSION,
    ALTERNATIVE,
    SUPERSET_PARTNER,
    COMPOUND_PART,
    WARMUP_FOR,
    COOLDOWN_FOR,
    SIMILAR_PATTERN,
}

enum class RMScalingType(
    val displayName: String,
    val description: String,
) {
    STANDARD(
        "Standard",
        "Most exercises - bench press, squat, rows, etc.",
    ),
    WEIGHTED_BODYWEIGHT(
        "Weighted Bodyweight",
        "Pull-ups, dips, muscle-ups where you add weight to your body",
    ),
    ISOLATION(
        "Isolation",
        "Single-joint movements like curls, extensions, lateral raises",
    ),
}
