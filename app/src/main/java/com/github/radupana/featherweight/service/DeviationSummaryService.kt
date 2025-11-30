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

class DeviationSummaryService {
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

        val volumeDeviations = deviations.filter { it.deviationType == DeviationType.VOLUME_DEVIATION }
        val intensityDeviations = deviations.filter { it.deviationType == DeviationType.INTENSITY_DEVIATION }
        val swaps = deviations.filter { it.deviationType == DeviationType.EXERCISE_SWAP }
        val skips = deviations.filter { it.deviationType == DeviationType.EXERCISE_SKIPPED }
        val adds = deviations.filter { it.deviationType == DeviationType.EXERCISE_ADDED }

        val avgVolumeDeviation =
            if (volumeDeviations.isNotEmpty()) {
                volumeDeviations.map { it.deviationMagnitude }.average().toFloat() * 100
            } else {
                0f
            }

        val avgIntensityDeviation =
            if (intensityDeviations.isNotEmpty()) {
                intensityDeviations.map { it.deviationMagnitude }.average().toFloat() * 100
            } else {
                0f
            }

        val keyDeviations = buildKeyDeviations(deviations, completedWorkouts)

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

    private fun buildKeyDeviations(
        deviations: List<WorkoutDeviation>,
        completedWorkouts: Int,
    ): List<String> {
        val summaries = mutableListOf<String>()

        val byType = deviations.groupBy { it.deviationType }

        byType[DeviationType.VOLUME_DEVIATION]?.let { volumeDevs ->
            if (volumeDevs.isNotEmpty()) {
                val avgMagnitude = volumeDevs.map { it.deviationMagnitude }.average() * 100
                val direction = if (avgMagnitude >= 0) "higher" else "lower"
                summaries.add(
                    "Volume ${abs(avgMagnitude).toInt()}% $direction on average across ${volumeDevs.size} exercises",
                )
            }
        }

        byType[DeviationType.INTENSITY_DEVIATION]?.let { intensityDevs ->
            if (intensityDevs.isNotEmpty()) {
                val avgMagnitude = intensityDevs.map { it.deviationMagnitude }.average() * 100
                val direction = if (avgMagnitude >= 0) "heavier" else "lighter"
                summaries.add(
                    "Weight ${abs(avgMagnitude).toInt()}% $direction on average across ${intensityDevs.size} exercises",
                )
            }
        }

        byType[DeviationType.EXERCISE_SWAP]?.let { swaps ->
            if (swaps.isNotEmpty()) {
                val uniqueWorkouts = swaps.map { it.workoutId }.distinct().size
                summaries.add("Swapped exercises in $uniqueWorkouts of $completedWorkouts workouts")
            }
        }

        byType[DeviationType.EXERCISE_SKIPPED]?.let { skips ->
            if (skips.isNotEmpty()) {
                summaries.add("Skipped ${skips.size} prescribed exercises")
            }
        }

        byType[DeviationType.EXERCISE_ADDED]?.let { adds ->
            if (adds.isNotEmpty()) {
                summaries.add("Added ${adds.size} extra exercises")
            }
        }

        byType[DeviationType.SET_COUNT_DEVIATION]?.let { setDevs ->
            if (setDevs.isNotEmpty()) {
                val avgMagnitude = setDevs.map { it.deviationMagnitude }.average() * 100
                val direction = if (avgMagnitude >= 0) "more" else "fewer"
                summaries.add("${abs(avgMagnitude).toInt()}% $direction sets on average")
            }
        }

        byType[DeviationType.REP_DEVIATION]?.let { repDevs ->
            if (repDevs.isNotEmpty()) {
                val avgMagnitude = repDevs.map { it.deviationMagnitude }.average() * 100
                val direction = if (avgMagnitude >= 0) "more" else "fewer"
                summaries.add("${abs(avgMagnitude).toInt()}% $direction reps on average")
            }
        }

        return summaries.take(MAX_KEY_DEVIATIONS)
    }

    companion object {
        private const val MAX_KEY_DEVIATIONS = 7
    }
}
