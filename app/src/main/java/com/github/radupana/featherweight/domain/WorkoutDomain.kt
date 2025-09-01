package com.github.radupana.featherweight.domain

import com.github.radupana.featherweight.data.WorkoutStatus
import java.time.LocalDate
import java.time.LocalDateTime

data class WorkoutSummary(
    val id: Long,
    val date: LocalDateTime,
    val name: String?,
    val exerciseCount: Int,
    val setCount: Int,
    val totalWeight: Float,
    val duration: Long?, // seconds
    val status: WorkoutStatus,
    val hasNotes: Boolean = false,
    // Programme Integration Fields
    val isProgrammeWorkout: Boolean = false,
    val programmeId: Long? = null,
    val programmeName: String? = null,
    val programmeWorkoutName: String? = null,
    val weekNumber: Int? = null,
    val dayNumber: Int? = null,
)

data class WorkoutSummaryWithProgramme(
    val id: Long,
    val date: LocalDateTime,
    val name: String?,
    val programmeName: String?,
    val exerciseCount: Int,
    val setCount: Int,
    val totalWeight: Float,
    val duration: Long?,
    val status: WorkoutStatus,
    val hasNotes: Boolean = false,
)

data class WorkoutFilters(
    val dateRange: Pair<LocalDate, LocalDate>? = null,
    val exercises: List<String> = emptyList(),
    val muscleGroups: List<String> = emptyList(),
    val programmeId: Long? = null,
)
