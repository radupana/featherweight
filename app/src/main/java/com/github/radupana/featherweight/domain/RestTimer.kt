package com.github.radupana.featherweight.domain

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class RestTimerState(
    val isActive: Boolean = false,
    val isPaused: Boolean = false,
    val remainingTime: Duration = Duration.ZERO,
    val totalTime: Duration = Duration.ZERO,
    val exerciseName: String? = null,
    val suggestion: String? = null
) {
    val progress: Float
        get() = if (totalTime.inWholeSeconds > 0) {
            1f - (remainingTime.inWholeSeconds.toFloat() / totalTime.inWholeSeconds.toFloat())
        } else 0f
    
    val isFinished: Boolean
        get() = isActive && remainingTime <= Duration.ZERO
}

class RestTimer {
    private val pauseState = MutableStateFlow(false)
    
    fun startTimer(duration: Duration, exerciseName: String? = null, suggestion: String? = null): Flow<RestTimerState> = flow {
        var remaining = duration
        val total = duration
        pauseState.value = false
        
        emit(RestTimerState(
            isActive = true,
            isPaused = false,
            remainingTime = remaining,
            totalTime = total,
            exerciseName = exerciseName,
            suggestion = suggestion
        ))
        
        while (remaining > Duration.ZERO) {
            if (!pauseState.value) {
                delay(1.seconds)
                remaining -= 1.seconds
            } else {
                // When paused, just wait
                delay(100) // Check pause state every 100ms
            }
            
            emit(RestTimerState(
                isActive = true,
                isPaused = pauseState.value,
                remainingTime = remaining.coerceAtLeast(Duration.ZERO),
                totalTime = total,
                exerciseName = exerciseName,
                suggestion = suggestion
            ))
        }
        
        // Timer finished
        emit(RestTimerState(
            isActive = true,
            isPaused = false,
            remainingTime = Duration.ZERO,
            totalTime = total,
            exerciseName = exerciseName,
            suggestion = suggestion
        ))
    }
    
    fun pause() {
        pauseState.value = true
    }
    
    fun resume() {
        pauseState.value = false
    }
}