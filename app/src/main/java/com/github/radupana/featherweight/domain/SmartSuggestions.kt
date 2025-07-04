package com.github.radupana.featherweight.domain

import java.time.LocalDateTime

data class SmartSuggestions(
    val suggestedWeight: Float,
    val suggestedReps: Int,
    val suggestedRpe: Float? = null,
    val lastWorkoutDate: LocalDateTime? = null,
    val confidence: String,
    val reasoning: String = "", // NEW: Detailed explanation
    val alternativeSuggestions: List<AlternativeSuggestion> = emptyList() // NEW: Alternative options
)

data class AlternativeSuggestion(
    val reps: Int,
    val weight: Float,
    val rpe: Float? = null,
    val reasoning: String
)