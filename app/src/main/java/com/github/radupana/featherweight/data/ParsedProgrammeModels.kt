package com.github.radupana.featherweight.data

data class ParsedProgramme(
    val name: String,
    val description: String,
    val durationWeeks: Int,
    val programmeType: String,
    val difficulty: String,
    val weeks: List<ParsedWeek>,
    val rawText: String,
    val unmatchedExercises: List<String> = emptyList(),
)

data class ParsedWeek(
    val weekNumber: Int,
    val name: String,
    val description: String? = null,
    val focusAreas: String? = null,
    val intensityLevel: String? = null,
    val volumeLevel: String? = null,
    val isDeload: Boolean = false,
    val phase: String? = null,
    val workouts: List<ParsedWorkout>,
)

data class ParsedWorkout(
    val dayOfWeek: String?, // Nullable - null means use Day 1, Day 2, etc.
    val name: String,
    val estimatedDurationMinutes: Int = 60,
    val exercises: List<ParsedExercise>,
)

data class ParsedExercise(
    val exerciseName: String,
    val matchedExerciseId: String? = null,
    val sets: List<ParsedSet>,
    val notes: String? = null,
)

data class ParsedSet(
    val reps: Int? = null,
    val weight: Float? = null,
    val rpe: Float? = null,
)

data class TextParsingRequest(
    val rawText: String,
    val userMaxes: Map<String, Float> = emptyMap(),
)

data class TextParsingResult(
    val success: Boolean,
    val programme: ParsedProgramme? = null,
    val error: String? = null,
    val validationIssues: List<String> = emptyList(),
)
