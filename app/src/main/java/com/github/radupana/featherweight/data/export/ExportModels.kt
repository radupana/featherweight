package com.github.radupana.featherweight.data.export

data class ExportOptions(
    val includeBodyweight: Boolean = true,
    val includeOneRepMaxes: Boolean = true,
    val includeNotes: Boolean = true,
    val includeProfile: Boolean = true,
)

data class OneRMRecord(
    val exerciseId: Long,
    val exerciseName: String,
    val weight: Float,
    val recordedDate: String,
)

data class BodyweightRecord(
    val weight: Float,
    val recordedDate: String,
)

data class ProgrammeInfo(
    val programmeName: String,
    val weekNumber: Int,
    val dayNumber: Int,
)

data class ExportedExercise(
    val exerciseId: Long?,
    val exerciseName: String,
    val order: Int,
    val supersetGroup: Int? = null,
    val notes: String? = null,
    val sets: List<ExportedSet>,
)

data class ExportedSet(
    val setNumber: Int,
    val targetReps: Int?,
    val targetWeight: Float?,
    val actualReps: Int,
    val actualWeight: Float,
    val rpe: Float?,
    val completed: Boolean,
)
