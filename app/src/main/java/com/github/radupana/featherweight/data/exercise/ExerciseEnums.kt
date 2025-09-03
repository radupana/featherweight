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

}

enum class MovementPattern {
    // Primary Patterns (based on functional movement)
    SQUAT,
    HINGE,
    PUSH,
    PULL,
    LUNGE,
    CARRY,
    ROTATE,
    ROTATION, // Added for JSON compatibility

    // Exercise-specific patterns
    VERTICAL_PUSH,
    HORIZONTAL_PUSH,
    VERTICAL_PULL,
    HORIZONTAL_PULL,
    PRESS, // Added for JSON compatibility
    ROW, // Added for JSON compatibility
    CURL, // Added for JSON compatibility
    EXTENSION, // Added for JSON compatibility
    FLY, // Added for JSON compatibility
    RAISE, // Added for JSON compatibility
    SHRUG, // Added for JSON compatibility

    // Core/Stability
    PLANK,
    ANTI_EXTENSION,
    ANTI_ROTATION,
    CORE, // Added for JSON compatibility
    CRUNCH, // Added for JSON compatibility
    ROLLOUT, // Added for JSON compatibility
    HOLD, // Added for JSON compatibility
    ISOMETRIC, // Added for JSON compatibility
    PIKE, // Added for JSON compatibility
    TUCK, // Added for JSON compatibility
    ROLL, // Added for JSON compatibility

    // Cardio/Conditioning
    GAIT,
    CONDITIONING,
    JUMP,
    SPRINT, // Added for JSON compatibility
    WALK, // Added for JSON compatibility
    STEP, // Added for JSON compatibility
    CYCLE, // Added for JSON compatibility
    CRAWL, // Added for JSON compatibility
    LOCOMOTION, // Added for JSON compatibility

    // Movement specific patterns from JSON
    ABDUCTION, // Added for JSON compatibility
    ADDUCTION, // Added for JSON compatibility
    CIRCLE, // Added for JSON compatibility
    COMPLEX, // Added for JSON compatibility
    EXPLOSIVE, // Added for JSON compatibility
    FLIP, // Added for JSON compatibility
    KICK, // Added for JSON compatibility
    LIFT, // Added for JSON compatibility
    OLYMPIC, // Added for JSON compatibility
    SLAM, // Added for JSON compatibility
    SWING, // Added for JSON compatibility
    WAVE, // Added for JSON compatibility
    ;

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
        fun fromLevel(level: Int): ExerciseDifficulty = entries.find { it.level == level } ?: BEGINNER
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
