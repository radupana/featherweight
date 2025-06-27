package com.github.radupana.featherweight.viewmodel

import android.content.Context
import android.media.MediaPlayer
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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

class RestTimerViewModel(
    private val context: Context? = null
) : ViewModel() {
    private val restTimer = RestTimer()
    private var timerJob: Job? = null
    private var lastTimerFinishedId: String? = null // Prevent duplicate notifications
    
    private val _timerState = MutableStateFlow(RestTimerState())
    val timerState: StateFlow<RestTimerState> = _timerState.asStateFlow()
    
    fun startTimer(duration: Duration, exerciseName: String? = null) {
        // Cancel any existing timer
        timerJob?.cancel()
        
        timerJob = viewModelScope.launch {
            restTimer.startTimer(duration, exerciseName).collect { state ->
                _timerState.value = state
                
                // Check if timer just finished
                if (state.isFinished && state.exerciseName != null) {
                    val timerId = "${state.exerciseName}-${state.totalTime.inWholeSeconds}"
                    if (lastTimerFinishedId != timerId) {
                        lastTimerFinishedId = timerId
                        triggerTimerCompletedFeedback()
                    }
                }
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
                
                // Check if timer just finished
                if (state.isFinished && state.exerciseName != null) {
                    val timerId = "${state.exerciseName}-${state.totalTime.inWholeSeconds}"
                    if (lastTimerFinishedId != timerId) {
                        lastTimerFinishedId = timerId
                        triggerTimerCompletedFeedback()
                    }
                }
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
                
                // Check if timer just finished
                if (state.isFinished && state.exerciseName != null) {
                    val timerId = "${state.exerciseName}-${state.totalTime.inWholeSeconds}"
                    if (lastTimerFinishedId != timerId) {
                        lastTimerFinishedId = timerId
                        triggerTimerCompletedFeedback()
                    }
                }
            }
        }
    }
    
    private fun triggerTimerCompletedFeedback() {
        context?.let { ctx ->
            // Haptic feedback
            val vibrator = ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.let { v ->
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    // Create a pulsing pattern for completed timer
                    val pattern = longArrayOf(0, 100, 50, 100, 50, 100)
                    val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                    v.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(longArrayOf(0, 100, 50, 100, 50, 100), -1)
                }
            }
            
            // TODO: Add sound notification (optional)
            // Could use system notification sound or custom sound
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

class RestTimerViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RestTimerViewModel::class.java)) {
            return RestTimerViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}