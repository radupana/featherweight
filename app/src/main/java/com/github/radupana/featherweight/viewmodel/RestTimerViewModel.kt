package com.github.radupana.featherweight.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.data.exercise.Exercise
import com.github.radupana.featherweight.domain.RestTimer
import com.github.radupana.featherweight.domain.RestTimerState
import com.github.radupana.featherweight.domain.RestTimeCalculator
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class RestTimerViewModel : ViewModel() {
    private val restTimer = RestTimer()
    private var timerJob: Job? = null
    
    private val _timerState = MutableStateFlow(RestTimerState())
    val timerState: StateFlow<RestTimerState> = _timerState.asStateFlow()
    
    fun startTimer(duration: Duration, exerciseName: String? = null) {
        // Cancel any existing timer
        timerJob?.cancel()
        
        timerJob = viewModelScope.launch {
            restTimer.startTimer(duration, exerciseName).collect { state ->
                _timerState.value = state
            }
        }
    }
    
    /**
     * Start timer with smart rest suggestion based on exercise
     */
    fun startSmartTimer(
        exerciseName: String,
        exercise: Exercise? = null,
        reps: Int? = null,
        weight: Float? = null,
        oneRepMax: Float? = null
    ) {
        // Cancel any existing timer
        timerJob?.cancel()
        
        val suggestion = RestTimeCalculator.calculateRestTime(
            exerciseName = exerciseName,
            exercise = exercise,
            reps = reps,
            weight = weight,
            oneRepMax = oneRepMax
        )
        
        timerJob = viewModelScope.launch {
            restTimer.startTimer(
                duration = suggestion.duration,
                exerciseName = exerciseName,
                suggestion = suggestion.reasoning
            ).collect { state ->
                _timerState.value = state
            }
        }
    }
    
    fun stopTimer() {
        timerJob?.cancel()
        _timerState.value = RestTimerState()
    }
    
    fun addTime(additionalTime: Duration) {
        val currentState = _timerState.value
        if (currentState.isActive && !currentState.isPaused) {
            val newDuration = currentState.remainingTime + additionalTime
            startTimerWithState(newDuration, currentState.exerciseName, currentState.suggestion)
        }
    }
    
    fun subtractTime(timeToSubtract: Duration) {
        val currentState = _timerState.value
        if (currentState.isActive && !currentState.isPaused) {
            val newDuration = (currentState.remainingTime - timeToSubtract).coerceAtLeast(Duration.ZERO)
            if (newDuration == Duration.ZERO) {
                stopTimer()
            } else {
                startTimerWithState(newDuration, currentState.exerciseName, currentState.suggestion)
            }
        }
    }
    
    private fun startTimerWithState(duration: Duration, exerciseName: String?, suggestion: String?) {
        // Cancel any existing timer
        timerJob?.cancel()
        
        timerJob = viewModelScope.launch {
            restTimer.startTimer(duration, exerciseName, suggestion).collect { state ->
                _timerState.value = state
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}