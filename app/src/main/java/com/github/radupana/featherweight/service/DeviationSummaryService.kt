package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.programme.DeviationType
import com.github.radupana.featherweight.data.programme.ProgrammeWithDetailsRaw
import com.github.radupana.featherweight.data.programme.WorkoutDeviation
import kotlin.math.abs

data class DeviationSummary(
    val programmeName: String,
    val programmeType: String,
    val durationWeeks: Int,
    val workoutsCompleted: Int,
    val workoutsPrescribed: Int,
    val avgVolumeDeviationPercent: Float,
    val avgIntensityDeviationPercent: Float,
    val exerciseSwapCount: Int,
    val exerciseSkipCount: Int,
    val exerciseAddCount: Int,
    val keyDeviations: List<String>,
)

/**
 * Summarizer for a single deviation type.
 * Returns a human-readable summary string or null if no summary applies.
 */
private fun interface DeviationSummarizer {
    fun summarize(
        deviations: List<WorkoutDeviation>,
        completedWorkouts: Int,
    ): String?
}

class DeviationSummaryService {
    /**
     * Map of deviation types to their summarizers.
     * Each summarizer knows how to convert deviations of its type into a human-readable string.
     */
    private val deviationSummarizers: Map<DeviationType, DeviationSummarizer> =
        mapOf(
            DeviationType.VOLUME_DEVIATION to
                DeviationSummarizer { devs, _ ->
                    summarizeMagnitudeDeviation(devs, "Volume", "higher", "lower", "exercises")
                },
            DeviationType.INTENSITY_DEVIATION to
                DeviationSummarizer { devs, _ ->
                    summarizeMagnitudeDeviation(devs, "Weight", "heavier", "lighter", "exercises")
                },
            DeviationType.EXERCISE_SWAP to
                DeviationSummarizer { devs, completed ->
                    if (devs.isNotEmpty() && completed > 0) {
                        val uniqueWorkouts = devs.map { it.workoutId }.distinct().size
                        "Swapped exercises in $uniqueWorkouts of $completed workouts"
                    } else {
                        null
                    }
                },
            DeviationType.EXERCISE_SKIPPED to
                DeviationSummarizer { devs, _ ->
                    if (devs.isNotEmpty()) "Skipped ${devs.size} prescribed exercises" else null
                },
            DeviationType.EXERCISE_ADDED to
                DeviationSummarizer { devs, _ ->
                    if (devs.isNotEmpty()) "Added ${devs.size} extra exercises" else null
                },
            DeviationType.SET_COUNT_DEVIATION to
                DeviationSummarizer { devs, _ ->
                    summarizeMagnitudeDeviation(devs, "", "more", "fewer", "sets on average", prefix = true)
                },
            DeviationType.REP_DEVIATION to
                DeviationSummarizer { devs, _ ->
                    summarizeMagnitudeDeviation(devs, "", "more", "fewer", "reps on average", prefix = true)
                },
        )

    private fun summarizeMagnitudeDeviation(
        deviations: List<WorkoutDeviation>,
        label: String,
        positiveDirection: String,
        negativeDirection: String,
        suffix: String,
        prefix: Boolean = false,
    ): String? {
        if (deviations.isEmpty()) return null

        val avgMagnitude = deviations.map { it.deviationMagnitude }.average() * 100
        val direction = if (avgMagnitude >= 0) positiveDirection else negativeDirection
        val magnitude = abs(avgMagnitude).toInt()

        return if (prefix) {
            "$magnitude% $direction $suffix"
        } else {
            "$label $magnitude% $direction on average across ${deviations.size} $suffix"
        }
    }

    fun summarizeDeviations(
        deviations: List<WorkoutDeviation>,
        programmeDetails: ProgrammeWithDetailsRaw,
    ): DeviationSummary {
        val programme = programmeDetails.programme
        val totalPrescribedWorkouts = programmeDetails.progress?.totalWorkouts ?: 0
        val completedWorkouts = programmeDetails.progress?.completedWorkouts ?: 0

        if (deviations.isEmpty()) {
            return DeviationSummary(
                programmeName = programme.name,
                programmeType = programme.programmeType.name,
                durationWeeks = programme.durationWeeks,
                workoutsCompleted = completedWorkouts,
                workoutsPrescribed = totalPrescribedWorkouts,
                avgVolumeDeviationPercent = 0f,
                avgIntensityDeviationPercent = 0f,
                exerciseSwapCount = 0,
                exerciseSkipCount = 0,
                exerciseAddCount = 0,
                keyDeviations = emptyList(),
            )
        }

        val byType = deviations.groupBy { it.deviationType }

        val volumeDeviations = byType[DeviationType.VOLUME_DEVIATION] ?: emptyList()
        val intensityDeviations = byType[DeviationType.INTENSITY_DEVIATION] ?: emptyList()
        val swaps = byType[DeviationType.EXERCISE_SWAP] ?: emptyList()
        val skips = byType[DeviationType.EXERCISE_SKIPPED] ?: emptyList()
        val adds = byType[DeviationType.EXERCISE_ADDED] ?: emptyList()

        val avgVolumeDeviation = calculateAverageDeviation(volumeDeviations)
        val avgIntensityDeviation = calculateAverageDeviation(intensityDeviations)
        val keyDeviations = buildKeyDeviations(byType, completedWorkouts)

        return DeviationSummary(
            programmeName = programme.name,
            programmeType = programme.programmeType.name,
            durationWeeks = programme.durationWeeks,
            workoutsCompleted = completedWorkouts,
            workoutsPrescribed = totalPrescribedWorkouts,
            avgVolumeDeviationPercent = avgVolumeDeviation,
            avgIntensityDeviationPercent = avgIntensityDeviation,
            exerciseSwapCount = swaps.size,
            exerciseSkipCount = skips.size,
            exerciseAddCount = adds.size,
            keyDeviations = keyDeviations,
        )
    }

    private fun calculateAverageDeviation(deviations: List<WorkoutDeviation>): Float =
        if (deviations.isNotEmpty()) {
            deviations.map { it.deviationMagnitude }.average().toFloat() * 100
        } else {
            0f
        }

    private fun buildKeyDeviations(
        byType: Map<DeviationType, List<WorkoutDeviation>>,
        completedWorkouts: Int,
    ): List<String> =
        deviationSummarizers
            .mapNotNull { (type, summarizer) ->
                byType[type]?.let { deviations ->
                    summarizer.summarize(deviations, completedWorkouts)
                }
            }.take(MAX_KEY_DEVIATIONS)

    companion object {
        /**
         * Maximum number of key deviation summaries to include in the analysis payload.
         *
         * Set to 7 because:
         * - There are 7 deviation types tracked (volume, intensity, swap, skip, add, set count, rep)
         * - This ensures at most one summary per deviation type is included
         * - Keeps the AI prompt concise while covering all deviation categories
         * - Prevents token bloat in the AI request payload
         */
        private const val MAX_KEY_DEVIATIONS = 7
    }
}
