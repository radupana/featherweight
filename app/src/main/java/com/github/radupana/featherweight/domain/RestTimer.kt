package com.github.radupana.featherweight.domain

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class RestTimerState(
    val isActive: Boolean = false,
    val isPaused: Boolean = false,
    val remainingTime: Duration = Duration.ZERO,
    val totalTime: Duration = Duration.ZERO,
    val exerciseName: String? = null
) {
    val progress: Float
        get() = if (totalTime.inWholeSeconds > 0) {
            1f - (remainingTime.inWholeSeconds.toFloat() / totalTime.inWholeSeconds.toFloat())
        } else 0f
    
    val isFinished: Boolean
        get() = isActive && remainingTime <= Duration.ZERO
}

class RestTimer {
    fun startTimer(duration: Duration, exerciseName: String? = null): Flow<RestTimerState> = flow {
        var remaining = duration
        val total = duration
        
        emit(RestTimerState(
            isActive = true,
            isPaused = false,
            remainingTime = remaining,
            totalTime = total,
            exerciseName = exerciseName
        ))
        
        while (remaining > Duration.ZERO) {
            delay(1.seconds)
            remaining -= 1.seconds
            
            emit(RestTimerState(
                isActive = true,
                isPaused = false,
                remainingTime = remaining.coerceAtLeast(Duration.ZERO),
                totalTime = total,
                exerciseName = exerciseName
            ))
        }
        
        // Timer finished
        emit(RestTimerState(
            isActive = true,
            isPaused = false,
            remainingTime = Duration.ZERO,
            totalTime = total,
            exerciseName = exerciseName
        ))
    }
}