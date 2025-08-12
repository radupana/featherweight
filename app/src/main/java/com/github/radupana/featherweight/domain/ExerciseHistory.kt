package com.github.radupana.featherweight.domain

import com.github.radupana.featherweight.data.SetLog
import java.time.LocalDateTime

data class ExerciseHistory(
    val exerciseVariationId: Long,
    val lastWorkoutDate: LocalDateTime,
    val sets: List<SetLog>,
)
