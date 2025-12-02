package com.github.radupana.featherweight.data.voice

import com.github.radupana.featherweight.model.WeightUnit

data class ParsedVoiceWorkoutInput(
    val transcription: String,
    val exercises: List<ParsedExerciseData>,
    val confidence: Float,
    val warnings: List<String>,
)

data class ParsedExerciseData(
    val spokenName: String,
    val interpretedName: String,
    val matchedExerciseId: String?,
    val matchedExerciseName: String?,
    val sets: List<ParsedSetData>,
    val confidence: Float,
    val notes: String?,
) {
    val needsMapping: Boolean get() = matchedExerciseId == null
}

data class ParsedSetData(
    val setNumber: Int,
    val reps: Int,
    val weight: Float,
    val weightUnit: WeightUnit,
    val rpe: Float?,
    val isToFailure: Boolean,
    val notes: String?,
)
