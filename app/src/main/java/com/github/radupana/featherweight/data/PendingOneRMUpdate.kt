package com.github.radupana.featherweight.data

data class PendingOneRMUpdate(
    val exerciseId: Long,
    val exerciseName: String,
    val currentMax: Float?,
    val suggestedMax: Float,
    val confidence: Float,
    val source: String // e.g., "3×100kg @ RPE 9"
)