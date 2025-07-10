package com.github.radupana.featherweight.data.programme

import kotlinx.serialization.Serializable

/**
 * Weight calculation rules for programme-based weight suggestions
 */
@Serializable
data class WeightCalculationRules(
    val baseOn: WeightBasis = WeightBasis.ONE_REP_MAX,
    val trainingMaxPercentage: Float = 0.9f, // e.g., 5/3/1 uses 90% of 1RM
    val roundingIncrement: Float = 2.5f, // Round to nearest 2.5kg
    val minimumBarWeight: Float = 20f, // Empty barbell weight
    val unit: WeightUnit = WeightUnit.KG,
)

@Serializable
enum class WeightBasis {
    ONE_REP_MAX, // Calculate from 1RM
    TRAINING_MAX, // Calculate from training max
    LAST_WORKOUT, // Use previous workout's weight
    FIXED_WEIGHT, // Programme specifies exact weight
    BODYWEIGHT, // For bodyweight exercises
    PERCENTAGE_BASED, // Use intensity percentages from programme
}

@Serializable
enum class WeightUnit {
    KG,
    LB,
}

/**
 * Progression rules for how weights should increase over time
 */
@Serializable
data class ProgressionRules(
    val type: ProgressionType = ProgressionType.LINEAR,
    val incrementRules: Map<String, Float> = defaultIncrements(), // Exercise name to increment amount
    val successCriteria: SuccessCriteria = SuccessCriteria(),
    val deloadRules: DeloadRules = DeloadRules(),
    val cycleLength: Int? = null, // For wave/block periodization
    val weeklyPercentages: List<List<Float>>? = null, // For wave progression: weeks -> sets -> percentages
    val autoProgressionEnabled: Boolean = true,
)

@Serializable
enum class ProgressionType {
    LINEAR, // Add fixed weight each session
    DOUBLE, // Increase weight when hitting rep target
    WAVE, // Cycle through intensities
    PERCENTAGE_BASED, // Based on 1RM percentages
    RPE_BASED, // Autoregulated based on RPE
    CUSTOM, // Programme-specific rules
}

@Serializable
data class SuccessCriteria(
    val requiredSets: Int? = null,
    val requiredReps: Int? = null,
    val allowedMissedReps: Int = 0, // Total missed reps across all sets
    val minRPE: Float? = null,
    val maxRPE: Float? = null,
    val techniqueRequirement: Boolean = true,
)

@Serializable
data class DeloadRules(
    val triggerAfterFailures: Int = 3,
    val deloadPercentage: Float = 0.85f, // Reduce to 85% of current weight
    val minimumWeight: Float = 20f, // Don't go below bar weight
    val autoDeload: Boolean = true,
)

/**
 * Suggested weight for an exercise with context
 */
data class SuggestedWeight(
    val weight: Float,
    val reps: IntRange,
    val rpe: Float? = null,
    val source: String, // e.g., "75% of 1RM", "Last week + 2.5kg"
    val confidence: Float, // 0-1, how confident we are in this suggestion
)

/**
 * Default increment values for common exercises
 */
fun defaultIncrements(): Map<String, Float> {
    return mapOf(
        // Lower body - larger increments
        "squat" to 5.0f,
        "deadlift" to 5.0f,
        "front squat" to 5.0f,
        "romanian deadlift" to 5.0f,
        "leg press" to 10.0f,
        // Upper body - smaller increments
        "bench press" to 2.5f,
        "overhead press" to 2.5f,
        "incline bench press" to 2.5f,
        "dumbbell press" to 2.5f,
        "barbell row" to 2.5f,
        "pull-up" to 2.5f,
        // Default for unspecified exercises
        "default" to 2.5f,
    )
}

/**
 * Common progression templates for popular programmes
 */
object ProgressionTemplates {
    val strongLifts5x5 =
        ProgressionRules(
            type = ProgressionType.LINEAR,
            incrementRules =
                mapOf(
                    "squat" to 5.0f,
                    "deadlift" to 5.0f,
                    "bench press" to 2.5f,
                    "overhead press" to 2.5f,
                    "barbell row" to 2.5f,
                ),
            successCriteria =
                SuccessCriteria(
                    requiredSets = 5,
                    requiredReps = 5,
                    allowedMissedReps = 2,
                ),
            deloadRules =
                DeloadRules(
                    triggerAfterFailures = 3,
                    deloadPercentage = 0.85f,
                ),
        )

    val startingStrength =
        ProgressionRules(
            type = ProgressionType.LINEAR,
            incrementRules =
                mapOf(
                    "squat" to 5.0f,
                    "deadlift" to 10.0f, // Faster progression for deadlift
                    "bench press" to 2.5f,
                    "overhead press" to 2.5f,
                    "power clean" to 2.5f,
                ),
            successCriteria =
                SuccessCriteria(
                    requiredSets = 3,
                    requiredReps = 5,
                    allowedMissedReps = 0,
                ),
        )

    val fiveThreeOne =
        ProgressionRules(
            type = ProgressionType.WAVE,
            cycleLength = 3,
            incrementRules =
                mapOf(
                    "squat" to 5.0f,
                    "deadlift" to 5.0f,
                    "bench press" to 2.5f,
                    "overhead press" to 2.5f,
                ),
            weeklyPercentages =
                listOf(
                    listOf(0.65f, 0.75f, 0.85f), // Week 1
                    listOf(0.70f, 0.80f, 0.90f), // Week 2
                    listOf(0.75f, 0.85f, 0.95f), // Week 3
                ),
            successCriteria =
                SuccessCriteria(
                    requiredSets = null, // Varies by week
                    requiredReps = null, // AMRAP sets
                ),
        )
}
