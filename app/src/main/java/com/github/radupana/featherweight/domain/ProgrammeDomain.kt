package com.github.radupana.featherweight.domain

import com.github.radupana.featherweight.data.programme.ProgrammeDifficulty
import com.github.radupana.featherweight.data.programme.ProgrammeType
import java.time.LocalDateTime

data class ProgrammeSummary(
    val id: Long,
    val name: String,
    val startDate: LocalDateTime,
    val completionDate: LocalDateTime,
    val completedAt: LocalDateTime? = null,
    val durationWeeks: Int,
    val completedWorkouts: Int,
    val totalWorkouts: Int = 0,
    val programmeType: ProgrammeType = ProgrammeType.GENERAL_FITNESS,
    val difficulty: ProgrammeDifficulty = ProgrammeDifficulty.INTERMEDIATE,
    val completionNotes: String? = null,
)

data class ProgrammeHistoryDetails(
    val id: Long,
    val name: String,
    val programmeType: ProgrammeType,
    val difficulty: ProgrammeDifficulty,
    val durationWeeks: Int,
    val startedAt: LocalDateTime?,
    val completedAt: LocalDateTime?,
    val completedWorkouts: Int,
    val totalWorkouts: Int,
    val programDurationDays: Int,
    val workoutHistory: List<WorkoutHistoryEntry>,
    val completionNotes: String? = null,
)

data class ProgrammeWeekHistory(
    val weekNumber: Int,
    val weekName: String?,
    val workouts: List<WorkoutHistoryDetail>,
)

data class WorkoutHistoryDetail(
    val id: Long,
    val name: String,
    val date: LocalDateTime,
    val exerciseNames: List<String>,
    val totalExercises: Int,
    val totalVolume: Float,
    val duration: Long?,
)

data class WorkoutHistoryEntry(
    val workoutId: Long,
    val workoutName: String,
    val weekNumber: Int,
    val dayNumber: Int,
    val completed: Boolean,
    val completedAt: LocalDateTime?,
)

data class WorkoutDayInfo(
    val completedCount: Int,
    val inProgressCount: Int,
    val notStartedCount: Int,
)
