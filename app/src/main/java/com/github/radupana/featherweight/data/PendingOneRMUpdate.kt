package com.github.radupana.featherweight.data

import java.time.LocalDateTime

data class PendingOneRMUpdate(
    val exerciseVariationId: Long,
    val currentMax: Float?,
    val suggestedMax: Float,
    val confidence: Float,
    val source: String, // e.g., "3×100kg @ RPE 9"
    val workoutDate: LocalDateTime? = null,
)
