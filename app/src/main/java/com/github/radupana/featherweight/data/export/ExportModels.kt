package com.github.radupana.featherweight.data.export

data class ExportOptions(
    val includeBodyweight: Boolean = true,
    val includeOneRepMaxes: Boolean = true,
    val includeNotes: Boolean = true,
    val includeProfile: Boolean = true,
)

data class ExportedWorkoutHistory(
    val metadata: ExportMetadata,
    val userProfile: ExportedUserProfile? = null,
    val workouts: List<ExportedWorkout>,
)

data class ExportMetadata(
    val exportDate: String,
    val startDate: String,
    val endDate: String,
    val totalWorkouts: Int,
    val appVersion: String,
    val exportOptions: ExportOptionsMetadata,
)

data class ExportOptionsMetadata(
    val includeBodyweight: Boolean,
    val includeOneRepMaxes: Boolean,
    val includeNotes: Boolean,
)

data class ExportedUserProfile(
    val oneRepMaxHistory: List<OneRMRecord>? = null,
    val bodyweightHistory: List<BodyweightRecord>? = null,
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

data class ExportedWorkout(
    val id: Long,
    val date: String,
    val name: String?,
    val programmeInfo: ProgrammeInfo? = null,
    val duration: Long?,
    val status: String,
    val exercises: List<ExportedExercise>,
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

data class ExportResult(
    val success: Boolean,
    val filePath: String? = null,
    val fileSize: Long = 0,
    val workoutCount: Int = 0,
    val error: String? = null,
)
