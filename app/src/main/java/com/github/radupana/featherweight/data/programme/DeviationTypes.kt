package com.github.radupana.featherweight.data.programme

/**
 * Types of deviations from prescribed programme workouts.
 */
enum class DeviationType {
    /** Exercise was substituted with a different exercise */
    EXERCISE_SWAP,

    /** Volume (sets × reps × weight) differed from target */
    VOLUME_DEVIATION,

    /** Intensity (weight) differed from target */
    INTENSITY_DEVIATION,

    /** RPE differed significantly from target */
    RPE_DEVIATION,

    /** Number of sets differed from target */
    SET_COUNT_DEVIATION,

    /** Number of reps differed from target */
    REP_DEVIATION,

    /** Exercise was completely skipped */
    EXERCISE_SKIPPED,

    /** Workout was done on different day than prescribed */
    SCHEDULE_DEVIATION,
}

// TODO: Add DeviationReason enum in future phase
// Plan: Post-workout quick tags (e.g., "Why did you swap? Fatigue | Equipment | Time")
// Options: FATIGUE, FORM_BREAKDOWN, INJURY_PREVENTION, EQUIPMENT_UNAVAILABLE,
//          TIME_CONSTRAINT, AUTO_REGULATION, GYM_BUSY, RECOVERY,
//          INTENTIONAL_MODIFICATION, OTHER
// See GH Issue #125 for full design
