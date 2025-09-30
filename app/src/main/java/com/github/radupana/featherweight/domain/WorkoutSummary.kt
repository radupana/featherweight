package com.github.radupana.featherweight.domain

import com.github.radupana.featherweight.data.WorkoutStatus
import java.time.LocalDateTime

data class WorkoutSummary(
    val id: String,
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
    val programmeId: String? = null,
    val programmeName: String? = null,
    val programmeWorkoutName: String? = null,
    val weekNumber: Int? = null,
    val dayNumber: Int? = null,
)
