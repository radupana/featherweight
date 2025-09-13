package com.github.radupana.featherweight.model

enum class WeightUnit(
    val suffix: String,
    val increment: Float,
) {
    KG(
        suffix = "kg",
        increment = 0.25f,
    ),
    LBS(
        suffix = "lbs",
        increment = 0.5f,
    ),
}
