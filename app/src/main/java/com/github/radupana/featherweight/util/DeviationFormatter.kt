package com.github.radupana.featherweight.util

import com.github.radupana.featherweight.data.programme.DeviationType
import com.github.radupana.featherweight.data.programme.WorkoutDeviation
import kotlin.math.abs

data class FormattedDeviations(
    val exerciseDeviations: Map<String, ExerciseDeviationInfo>,
    val addedExercises: Set<String>,
    val skippedExercises: List<SkippedExerciseInfo>,
)

data class ExerciseDeviationInfo(
    val exerciseLogId: String,
    val deviations: List<String>,
)

data class SkippedExerciseInfo(
    val exerciseName: String?,
)

object DeviationFormatter {
    fun formatDeviations(
        deviations: List<WorkoutDeviation>,
    ): FormattedDeviations {
        val exerciseDeviations = mutableMapOf<String, MutableList<String>>()
        val addedExercises = mutableSetOf<String>()
        val skippedExercises = mutableListOf<SkippedExerciseInfo>()

        deviations.forEach { deviation ->
            when (deviation.deviationType) {
                DeviationType.EXERCISE_SKIPPED -> {
                    skippedExercises.add(SkippedExerciseInfo(deviation.notes))
                }
                DeviationType.EXERCISE_ADDED -> {
                    deviation.exerciseLogId?.let { addedExercises.add(it) }
                }
                else -> {
                    deviation.exerciseLogId?.let { exerciseLogId ->
                        val formatted = formatSingleDeviation(deviation)
                        exerciseDeviations
                            .getOrPut(exerciseLogId) { mutableListOf() }
                            .add(formatted)
                    }
                }
            }
        }

        return FormattedDeviations(
            exerciseDeviations =
                exerciseDeviations.mapValues { (logId, deviationList) ->
                    ExerciseDeviationInfo(logId, deviationList)
                },
            addedExercises = addedExercises,
            skippedExercises = skippedExercises,
        )
    }

    private fun formatSingleDeviation(deviation: WorkoutDeviation): String {
        val magnitude = abs(deviation.deviationMagnitude)
        val direction = if (deviation.deviationMagnitude >= 0) "higher" else "lower"

        val severityLabel =
            when {
                magnitude < 0.15f -> "Slightly"
                magnitude < 0.30f -> "Moderately"
                magnitude < 0.50f -> "Significantly"
                else -> "Much"
            }

        val deviationName =
            when (deviation.deviationType) {
                DeviationType.VOLUME_DEVIATION -> "Volume"
                DeviationType.INTENSITY_DEVIATION -> "Intensity"
                DeviationType.SET_COUNT_DEVIATION -> "Sets"
                DeviationType.REP_DEVIATION -> "Reps"
                DeviationType.RPE_DEVIATION -> "RPE"
                DeviationType.EXERCISE_SWAP -> "Exercise swapped"
                else -> return ""
            }

        return if (deviation.deviationType == DeviationType.EXERCISE_SWAP) {
            deviationName
        } else {
            "$deviationName: $severityLabel $direction"
        }
    }
}
