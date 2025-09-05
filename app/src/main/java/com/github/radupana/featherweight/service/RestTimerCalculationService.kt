package com.github.radupana.featherweight.service

class RestTimerCalculationService {
    companion object {
        private const val DEFAULT_REST_DURATION = 90
    }

    fun calculateRestDuration(
        rpe: Float?,
        exerciseRestDuration: Int?,
    ): Int {
        rpe?.let { return rpeToRestDuration(it) }
        exerciseRestDuration?.let { return it }
        return DEFAULT_REST_DURATION
    }

    private fun rpeToRestDuration(rpe: Float): Int =
        when {
            rpe <= 5f -> 60
            rpe <= 7f -> 90
            rpe <= 9f -> 120
            else -> 180
        }
}
