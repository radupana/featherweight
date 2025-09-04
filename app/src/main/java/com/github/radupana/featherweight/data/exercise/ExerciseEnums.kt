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
}

enum class Equipment(
    val displayName: String,
) {
    // Free Weights
    BARBELL("Barbell"),
    DUMBBELL("Dumbbell"),
    KETTLEBELL("Kettlebell"),
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
    GHD_MACHINE("GHD Machine"),

    // Bodyweight/Basic
    BODYWEIGHT("Bodyweight"),
    PULL_UP_BAR("Pull-up Bar"),
    DIP_STATION("Dip Station"),
    RINGS("Gymnastic Rings"),
    AB_WHEEL("Ab Wheel"),

    // Accessories
    RESISTANCE_BAND("Resistance Band"),
    BAND("Resistance Band"), // Alias for RESISTANCE_BAND
    TRX("TRX"),
    MEDICINE_BALL("Medicine Ball"),
    STABILITY_BALL("Stability Ball"),

    // Cardio
    ASSAULT_BIKE("Assault Bike"),
    ROWER("Rower"),
    SKI_ERG("Ski Erg"),

    // Strongman Equipment
    ATLAS_STONE("Atlas Stone"),
    SANDBAG("Sandbag"),
    CAR_DEADLIFT("Car Deadlift Frame"),
    BATTLE_ROPES("Battle Ropes"),
    LOG("Log"), // Used in exercises.json
    YOKE("Yoke"), // Used in exercises.json
    SLED("Sled"), // Used in exercises.json
    TIRE("Tire"), // Used in exercises.json
    SLEDGEHAMMER("Sledgehammer"), // Used in exercises.json
    KEG("Keg"), // Used in exercises.json

    // Additional Equipment
    BENCH("Bench"), // Used in exercises.json
    BOX("Box"), // Used in exercises.json
    PARALLETTES("Parallettes"), // Used in exercises.json
    POLE("Pole"), // Used in exercises.json

    NONE("No Equipment"),
}

enum class MovementPattern {
    // Primary Patterns (based on functional movement)
    SQUAT,
    HINGE,
    PUSH,
    PULL,
    LUNGE,
    CARRY,
    ROTATION,

    // Exercise-specific patterns
    VERTICAL_PUSH,
    HORIZONTAL_PUSH,
    VERTICAL_PULL,
    HORIZONTAL_PULL,
    EXTENSION,
    PRESS, // Used in exercises.json
    ROW, // Used in exercises.json
    CURL, // Used in exercises.json
    FLY, // Used in exercises.json
    RAISE, // Used in exercises.json
    SHRUG, // Used in exercises.json

    // Core/Stability
    PLANK,
    ANTI_ROTATION,
    ISOMETRIC,
    CORE, // Used in exercises.json
    CRUNCH, // Used in exercises.json
    ROLLOUT, // Used in exercises.json
    HOLD, // Used in exercises.json
    PIKE, // Used in exercises.json
    TUCK, // Used in exercises.json
    ROLL, // Used in exercises.json

    // Cardio/Conditioning
    CONDITIONING,
    JUMP,
    SPRINT, // Used in exercises.json
    WALK, // Used in exercises.json
    STEP, // Used in exercises.json
    CYCLE, // Used in exercises.json
    CRAWL, // Used in exercises.json
    LOCOMOTION, // Used in exercises.json

    // Specialized movements
    ABDUCTION, // Used in exercises.json
    ADDUCTION, // Used in exercises.json
    CIRCLE, // Used in exercises.json
    COMPLEX, // Used in exercises.json
    EXPLOSIVE, // Used in exercises.json
    FLIP, // Used in exercises.json
    KICK, // Used in exercises.json
    LIFT, // Used in exercises.json
    OLYMPIC, // Used in exercises.json
    SLAM, // Used in exercises.json
    SWING, // Used in exercises.json
    WAVE, // Used in exercises.json
}

enum class ExerciseDifficulty(
    val displayName: String,
    val level: Int,
) {
    BEGINNER("Beginner", 1),
    INTERMEDIATE("Intermediate", 3),
    ADVANCED("Advanced", 4),
}

enum class InstructionType {
    EXECUTION,
}

enum class ExerciseRelationType {
    PROGRESSION,
    REGRESSION,
    ALTERNATIVE,
    SUPERSET_PARTNER,
    COMPOUND_PART,
}

enum class RMScalingType(
    val description: String,
) {
    STANDARD(
        "Most exercises - bench press, squat, rows, etc.",
    ),
    WEIGHTED_BODYWEIGHT(
        "Pull-ups, dips, muscle-ups where you add weight to your body",
    ),
    ISOLATION(
        "Single-joint movements like curls, extensions, lateral raises",
    ),
}
