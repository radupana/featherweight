package com.github.radupana.featherweight.domain

import java.time.LocalDateTime

data class SmartSuggestions(
    val suggestedWeight: Float,
    val suggestedReps: Int,
    val suggestedRpe: Float?,
    val lastWorkoutDate: LocalDateTime?,
    val confidence: String
)