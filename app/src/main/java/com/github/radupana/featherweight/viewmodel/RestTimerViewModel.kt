package com.github.radupana.featherweight.viewmodel

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.data.exercise.Exercise
import com.github.radupana.featherweight.domain.RestTimeCalculator
import com.github.radupana.featherweight.domain.RestTimer
import com.github.radupana.featherweight.domain.RestTimerState
import com.github.radupana.featherweight.utils.RestTimerNotificationManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class RestTimerViewModel(
    private val context: Context? = null,
) : ViewModel() {
    private val notificationManager = context?.let { RestTimerNotificationManager(it) }

    // Track multiple timers - one per workout
    private data class WorkoutTimer(
        val workoutId: Long,
        var job: Job,
        var state: RestTimerState,
        val restTimer: RestTimer = RestTimer(),
    )

    private val activeTimers = mutableMapOf<Long, WorkoutTimer>()
    private var currentlyViewedWorkoutId: Long? = null

    // The timer state that should be displayed (for the current workout)
    private val _timerState = MutableStateFlow(RestTimerState())
    val timerState: StateFlow<RestTimerState> = _timerState.asStateFlow()

    fun startTimer(
        duration: Duration,
        exerciseName: String? = null,
    ) {
        val workoutId = currentlyViewedWorkoutId ?: return

        // Cancel any existing timer for this workout
        activeTimers[workoutId]?.job?.cancel()

        val workoutTimer =
            WorkoutTimer(
                workoutId = workoutId,
                job = Job(),
                state = RestTimerState(),
                restTimer = RestTimer(),
            )

        workoutTimer.job =
            viewModelScope.launch {
                workoutTimer.restTimer.startTimer(duration, exerciseName).collect { state ->
                    // Update the timer state for this specific workout
                    workoutTimer.state = state

                    // If this is the currently viewed workout, update the displayed state
                    if (workoutId == currentlyViewedWorkoutId) {
                        _timerState.value = state
                    }

                    // Check if timer just finished
                    if (state.isFinished && state.exerciseName != null) {
                        triggerTimerCompletedFeedback()
                        notificationManager?.showRestCompleteNotification(state.exerciseName)

                        // Auto-dismiss timer after a short delay
                        viewModelScope.launch {
                            delay(2.seconds)
                            if (workoutId == currentlyViewedWorkoutId) {
                                stopTimer()
                            }
                        }
                    }
                }
            }

        activeTimers[workoutId] = workoutTimer
    }

    /**
     * Start timer with smart rest suggestion based on exercise
     */
    fun startSmartTimer(
        exerciseName: String,
        exercise: Exercise? = null,
        reps: Int? = null,
        weight: Float? = null,
        oneRepMax: Float? = null,
    ) {
        val suggestion =
            RestTimeCalculator.calculateRestTime(
                exerciseName = exerciseName,
                exercise = exercise,
                reps = reps,
                weight = weight,
                oneRepMax = oneRepMax,
            )

        startTimerWithState(suggestion.duration, exerciseName, suggestion.reasoning)
    }

    private fun startTimerWithState(
        duration: Duration,
        exerciseName: String?,
        suggestion: String?,
    ) {
        val workoutId = currentlyViewedWorkoutId ?: return

        // Cancel any existing timer for this workout
        activeTimers[workoutId]?.job?.cancel()

        val workoutTimer =
            WorkoutTimer(
                workoutId = workoutId,
                job = Job(),
                state = RestTimerState(),
                restTimer = RestTimer(),
            )

        workoutTimer.job =
            viewModelScope.launch {
                workoutTimer.restTimer.startTimer(duration, exerciseName, suggestion).collect { state ->
                    workoutTimer.state = state

                    if (workoutId == currentlyViewedWorkoutId) {
                        _timerState.value = state
                    }

                    if (state.isFinished && state.exerciseName != null) {
                        triggerTimerCompletedFeedback()
                        notificationManager?.showRestCompleteNotification(state.exerciseName)

                        viewModelScope.launch {
                            delay(2.seconds)
                            if (workoutId == currentlyViewedWorkoutId) {
                                stopTimer()
                            }
                        }
                    }
                }
            }

        activeTimers[workoutId] = workoutTimer
    }

    fun stopTimer() {
        val workoutId = currentlyViewedWorkoutId ?: return
        activeTimers[workoutId]?.job?.cancel()
        activeTimers.remove(workoutId)
        _timerState.value = RestTimerState()
        notificationManager?.cancelNotification()
    }

    // Workout lifecycle methods to properly scope timer
    fun bindToWorkout(workoutId: Long) {
        currentlyViewedWorkoutId = workoutId
        // Update displayed timer state to show this workout's timer (if any)
        val workoutTimer = activeTimers[workoutId]
        _timerState.value = workoutTimer?.state ?: RestTimerState()
    }

    fun unbindFromWorkout(workoutId: Long) {
        // Just clear the view association, don't stop the timer
        if (currentlyViewedWorkoutId == workoutId) {
            currentlyViewedWorkoutId = null
            _timerState.value = RestTimerState()
        }
    }

    fun onWorkoutCompleted() {
        val workoutId = currentlyViewedWorkoutId ?: return
        // Stop and remove timer for completed workout
        activeTimers[workoutId]?.job?.cancel()
        activeTimers.remove(workoutId)
        _timerState.value = RestTimerState()
        currentlyViewedWorkoutId = null
    }

    fun onWorkoutDeleted(workoutId: Long) {
        // Stop and remove timer for deleted workout
        activeTimers[workoutId]?.job?.cancel()
        activeTimers.remove(workoutId)

        // If this was the currently viewed workout, clear the display
        if (currentlyViewedWorkoutId == workoutId) {
            _timerState.value = RestTimerState()
            currentlyViewedWorkoutId = null
        }
    }

    fun addTime(additionalTime: Duration) {
        val workoutId = currentlyViewedWorkoutId ?: return
        val currentTimer = activeTimers[workoutId] ?: return
        val currentState = currentTimer.state

        if (currentState.isActive) {
            val newDuration = currentState.remainingTime + additionalTime
            val wasPaused = currentState.isPaused
            startTimerWithState(newDuration, currentState.exerciseName, currentState.suggestion)
            // Restore pause state if it was paused
            if (wasPaused) {
                pauseTimer()
            }
        }
    }

    fun subtractTime(timeToSubtract: Duration) {
        val workoutId = currentlyViewedWorkoutId ?: return
        val currentTimer = activeTimers[workoutId] ?: return
        val currentState = currentTimer.state

        if (currentState.isActive) {
            val newDuration = (currentState.remainingTime - timeToSubtract).coerceAtLeast(Duration.ZERO)
            if (newDuration == Duration.ZERO) {
                stopTimer()
            } else {
                val wasPaused = currentState.isPaused
                startTimerWithState(newDuration, currentState.exerciseName, currentState.suggestion)
                if (wasPaused) {
                    pauseTimer()
                }
            }
        }
    }

    fun pauseTimer() {
        val workoutId = currentlyViewedWorkoutId ?: return
        val workoutTimer = activeTimers[workoutId] ?: return

        if (workoutTimer.state.isActive && !workoutTimer.state.isPaused) {
            workoutTimer.job.cancel()
            val pausedState = workoutTimer.state.copy(isPaused = true)
            workoutTimer.state = pausedState
            activeTimers[workoutId] = workoutTimer
            _timerState.value = pausedState
        }
    }

    fun resumeTimer() {
        val workoutId = currentlyViewedWorkoutId ?: return
        val workoutTimer = activeTimers[workoutId] ?: return
        val currentState = workoutTimer.state

        if (currentState.isActive && currentState.isPaused) {
            startTimerWithState(currentState.remainingTime, currentState.exerciseName, currentState.suggestion)
        }
    }

    fun togglePause() {
        val workoutId = currentlyViewedWorkoutId ?: return
        val workoutTimer = activeTimers[workoutId] ?: return

        if (workoutTimer.state.isPaused) {
            resumeTimer()
        } else {
            pauseTimer()
        }
    }

    private fun triggerTimerCompletedFeedback() {
        // Haptic feedback
        val vibrator = context?.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        vibrator?.let {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(200)
            }
        }

        // Sound feedback temporarily disabled
        // TODO: Add timer complete sound when resource is available
    }

    override fun onCleared() {
        super.onCleared()
        // Cancel all active timers
        activeTimers.values.forEach { it.job.cancel() }
        activeTimers.clear()
        notificationManager?.cancelNotification()
    }
}

class RestTimerViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RestTimerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RestTimerViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
