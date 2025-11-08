@file:Suppress("TooGenericExceptionCaught")

package com.github.radupana.featherweight.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.PendingOneRMUpdate
import com.github.radupana.featherweight.data.PersonalRecord
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.data.WorkoutMode
import com.github.radupana.featherweight.data.WorkoutStatus
import com.github.radupana.featherweight.data.exercise.Exercise
import com.github.radupana.featherweight.data.exercise.ExerciseWithDetails
import com.github.radupana.featherweight.data.exercise.RMScalingType
import com.github.radupana.featherweight.data.exercise.toRMScalingType
import com.github.radupana.featherweight.di.ServiceLocator
import com.github.radupana.featherweight.domain.ExerciseHistory
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.repository.NextProgrammeWorkoutInfo
import com.github.radupana.featherweight.service.BatchCompletionService
import com.github.radupana.featherweight.service.OneRMService
import com.github.radupana.featherweight.service.RestTimerCalculationService
import com.github.radupana.featherweight.service.RestTimerNotificationService
import com.github.radupana.featherweight.util.CloudLogger
import com.github.radupana.featherweight.util.ExceptionLogger
import com.github.radupana.featherweight.util.WeightFormatter
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class WorkoutState(
    val isActive: Boolean = false,
    val status: WorkoutStatus = WorkoutStatus.NOT_STARTED,
    val mode: WorkoutMode = WorkoutMode.ACTIVE,
    val workoutId: String? = null,
    val startTime: LocalDateTime? = null,
    val workoutName: String? = null,
    val isReadOnly: Boolean = false, // Deprecated - use mode instead
    val isInEditMode: Boolean = false,
    // Backup for rollback
    val originalWorkoutData: Triple<List<ExerciseLog>, List<SetLog>, String?>? = null,
    // Programme Integration
    val isProgrammeWorkout: Boolean = false,
    val programmeId: String? = null,
    val programmeName: String? = null,
    val weekNumber: Int? = null,
    val dayNumber: Int? = null,
    val programmeWorkoutName: String? = null,
    // Loading state to prevent UI from showing "No exercises" while loading
    val isLoadingExercises: Boolean = false,
    // PR Celebration
    val pendingPRs: List<PersonalRecord> = emptyList(),
    val shouldShowPRCelebration: Boolean = false,
    // Template Editing
    val templateWeekIndex: Int? = null,
    val templateWorkoutIndex: Int? = null,
)

data class InProgressWorkout(
    val id: String,
    val name: String?,
    val startDate: LocalDateTime,
    val exerciseCount: Int,
    val setCount: Int,
    val completedSets: Int,
    // Programme information
    val isProgrammeWorkout: Boolean = false,
    val programmeName: String? = null,
    val programmeWorkoutName: String? = null,
    val weekNumber: Int? = null,
    val dayNumber: Int? = null,
    val programmeId: String? = null,
)

class WorkoutViewModel(
    application: Application,
    val repository: FeatherweightRepository,
) : AndroidViewModel(application) {
    // Secondary constructor for Android ViewModelFactory
    constructor(application: Application) : this(
        application,
        FeatherweightRepository(application),
    )

    private val oneRMService = OneRMService()
    private val restTimerCalculationService = RestTimerCalculationService()
    private val restTimerNotificationService = RestTimerNotificationService(application)
    private val batchCompletionService = BatchCompletionService()

    companion object {
        private const val TAG = "WorkoutViewModel"
        private const val DEFAULT_REST_TIMER_SECONDS = 90
    }

    // Cache for 1RM estimates by exercise ID
    private val _oneRMEstimates = MutableStateFlow<Map<String, Float>>(emptyMap())
    val oneRMEstimates: StateFlow<Map<String, Float>> = _oneRMEstimates

    // Expose pending 1RM updates from repository
    val pendingOneRMUpdates = repository.pendingOneRMUpdates

    // Apply a pending 1RM update
    fun applyOneRMUpdate(update: PendingOneRMUpdate) {
        viewModelScope.launch {
            try {
                repository.applyOneRMUpdate(update)
                // Reload 1RM estimates after update
                loadOneRMEstimatesForCurrentExercises()
            } catch (e: android.database.sqlite.SQLiteException) {
                CloudLogger.error(TAG, "Failed to apply 1RM update", e)
                // Failed to apply 1RM update - operation will be retried
            } catch (e: IllegalStateException) {
                CloudLogger.error(TAG, "Invalid state when applying 1RM update", e)
                // Failed to apply 1RM update - operation will be retried
            }
        }
    }

    // Clear all pending updates
    fun clearPendingOneRMUpdates() {
        repository.clearPendingOneRMUpdates()
    }

    // Clear pending PRs after celebration
    fun clearPendingPRs() {
        _workoutState.value =
            _workoutState.value.copy(
                pendingPRs = emptyList(),
                shouldShowPRCelebration = false,
            )
    }

    // Workout state management
    private val _workoutState = MutableStateFlow(WorkoutState())
    val workoutState: StateFlow<WorkoutState> = _workoutState

    // Core state
    private val _currentWorkoutId = MutableStateFlow<String?>(null)
    val currentWorkoutId: StateFlow<String?> = _currentWorkoutId

    private val _selectedWorkoutExercises = MutableStateFlow<List<ExerciseLog>>(emptyList())
    val selectedWorkoutExercises: StateFlow<List<ExerciseLog>> = _selectedWorkoutExercises

    private val _selectedExerciseSets = MutableStateFlow<List<SetLog>>(emptyList())
    val selectedExerciseSets: StateFlow<List<SetLog>> = _selectedExerciseSets

    // Cache validation state for sets
    private val _setCompletionValidation = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val setCompletionValidation: StateFlow<Map<String, Boolean>> = _setCompletionValidation

    private val _exerciseHistory = MutableStateFlow<Map<String, ExerciseHistory>>(emptyMap())
    val exerciseHistory: StateFlow<Map<String, ExerciseHistory>> = _exerciseHistory

    private val _lastPerformance = MutableStateFlow<Map<String, SetLog>>(emptyMap())
    val lastPerformance: StateFlow<Map<String, SetLog>> = _lastPerformance

    private val _lastMaxPerformance = MutableStateFlow<Map<String, SetLog>>(emptyMap())
    val lastMaxPerformance: StateFlow<Map<String, SetLog>> = _lastMaxPerformance

    private val _weightPRs = MutableStateFlow<Map<String, PersonalRecord>>(emptyMap())
    val weightPRs: StateFlow<Map<String, PersonalRecord>> = _weightPRs

    // In-progress workouts for home screen
    private val _inProgressWorkouts = MutableStateFlow<List<InProgressWorkout>>(emptyList())
    val inProgressWorkouts: StateFlow<List<InProgressWorkout>> = _inProgressWorkouts

    // Exercise-related state
    private val exerciseDetailsMap = MutableStateFlow<Map<String, Exercise>>(emptyMap())

    // Reactive exercise name mapping
    // Use String keys to avoid ID collisions between system and custom exercises
    // Key format: "system_<id>" for system exercises, "custom_<id>" for custom exercises
    private val _exerciseNames = MutableStateFlow<Map<String, String>>(emptyMap())
    val exerciseNames: StateFlow<Map<String, String>> = _exerciseNames

    // Helper functions for exercise name key management
    private fun getExerciseNameKey(exerciseId: String): String = "exercise_$exerciseId"

    private fun getExerciseNameFromMap(exerciseId: String): String? = _exerciseNames.value[getExerciseNameKey(exerciseId)]

    // Exercise swap state
    private val _swappingExercise = MutableStateFlow<ExerciseLog?>(null)
    val swappingExercise: StateFlow<ExerciseLog?> = _swappingExercise

    // Rest timer state
    private val _restTimerSeconds = MutableStateFlow(0)
    val restTimerSeconds: StateFlow<Int> = _restTimerSeconds

    private val _restTimerInitialSeconds = MutableStateFlow(0)
    val restTimerInitialSeconds: StateFlow<Int> = _restTimerInitialSeconds

    private var restTimerJob: Job? = null
    private var restTimerEndTime: LocalDateTime? = null

    // Exercise card expansion state
    private val _expandedExerciseIds = MutableStateFlow<Set<String>>(emptySet())
    val expandedExerciseIds: StateFlow<Set<String>> = _expandedExerciseIds

    // Workout timer state
    private val _workoutTimerSeconds = MutableStateFlow(0)
    val workoutTimerSeconds: StateFlow<Int> = _workoutTimerSeconds

    private var workoutTimerJob: Job? = null
    private var workoutTimerStartTime: LocalDateTime? = null

    // Flag to prevent concurrent workout creation
    private var isCreatingWorkout = false

    init {
        loadInProgressWorkouts()
        checkForOngoingWorkout()
        loadExercises()
    }

    // ===== EXERCISE METHODS =====

    private fun loadExercises() {
        viewModelScope.launch {
            try {
                // Load all system exercises
                val systemExercises = repository.getAllExercises()

                // Load all custom exercises
                val customExercises = repository.getCustomExercises()

                // Convert custom exercises to Exercise for unified handling
                val customAsVariations =
                    customExercises

                // Populate exercise names (all exercises now in single table)
                val namesMap = mutableMapOf<String, String>()

                // Add all exercises with unified key format
                (systemExercises + customExercises).forEach { exercise ->
                    namesMap[getExerciseNameKey(exercise.id)] = exercise.name
                }

                _exerciseNames.value = namesMap

                // Combine all exercises for other uses
                val allExercises = systemExercises + customAsVariations

                // Create lookup map for exercise details
                val detailsMap = allExercises.associateBy { it.id }
                exerciseDetailsMap.value = detailsMap
            } catch (e: android.database.sqlite.SQLiteException) {
                CloudLogger.error(TAG, "Failed to load exercises", e)
                // Failed to load exercises - will retry on next navigation
            } catch (e: IllegalStateException) {
                CloudLogger.error(TAG, "Invalid state when loading exercises", e)
                // Failed to load exercises - will retry on next navigation
            }
        }
    }

    // Check if there's an ongoing workout when the app starts
    private fun checkForOngoingWorkout() {
        viewModelScope.launch {
            // Only check for ongoing workouts if we don't already have a workout loaded
            // This prevents overriding when navigating from history
            if (_currentWorkoutId.value == null) {
                val ongoingWorkout = repository.getOngoingWorkout()
                if (ongoingWorkout != null) {
                    resumeWorkout(ongoingWorkout.id)
                }
            }
        }
    }

    // Load all in-progress workouts for home screen
    fun loadInProgressWorkouts() {
        viewModelScope.launch {
            val workoutHistory = repository.getWorkoutHistory()
            val inProgress =
                workoutHistory
                    .filter { it.status != WorkoutStatus.COMPLETED }
                    .map { summary ->
                        // Calculate completed sets
                        val completedSets =
                            try {
                                val exercises = repository.getExercisesForWorkout(summary.id)
                                exercises.sumOf { exercise ->
                                    val sets = repository.getSetsForExercise(exercise.id)
                                    sets.count { it.isCompleted }
                                }
                            } catch (e: android.database.sqlite.SQLiteException) {
                                CloudLogger.error(TAG, "Failed to calculate completed sets for workout ${summary.id}", e)
                                0
                            } catch (e: IllegalStateException) {
                                CloudLogger.error(TAG, "Invalid state when calculating completed sets for workout ${summary.id}", e)
                                0
                            }

                        InProgressWorkout(
                            id = summary.id,
                            name = summary.name,
                            startDate = summary.date,
                            exerciseCount = summary.exerciseCount,
                            setCount = summary.setCount,
                            completedSets = completedSets,
                            // Include programme information
                            isProgrammeWorkout = summary.isProgrammeWorkout,
                            programmeName = summary.programmeName,
                            programmeWorkoutName = summary.programmeWorkoutName,
                            weekNumber = summary.weekNumber,
                            dayNumber = summary.dayNumber,
                            // Get programme ID from the summary
                            programmeId = summary.programmeId,
                        )
                    }
            _inProgressWorkouts.value = inProgress
        }
    }

    // Check if there are any in-progress workouts
    suspend fun hasInProgressWorkouts(): Boolean {
        val ongoingWorkout = repository.getOngoingWorkout()
        return ongoingWorkout != null
    }

    // Get the most recent in-progress workout
    suspend fun getMostRecentInProgressWorkout(): InProgressWorkout? {
        val ongoingWorkout = repository.getOngoingWorkout() ?: return null
        val exercises = repository.getExercisesForWorkout(ongoingWorkout.id)
        val allSets = mutableListOf<SetLog>()
        exercises.forEach { exercise ->
            allSets.addAll(repository.getSetsForExercise(exercise.id))
        }

        return InProgressWorkout(
            id = ongoingWorkout.id,
            name = ongoingWorkout.notes,
            startDate = ongoingWorkout.date,
            exerciseCount = exercises.size,
            setCount = allSets.size,
            completedSets = allSets.count { it.isCompleted },
        )
    }

    // View a completed workout (read-only, no timers, no state changes)
    fun viewCompletedWorkout(workoutId: String) {
        CloudLogger.info(TAG, "viewCompletedWorkout called with workoutId: $workoutId")
        viewModelScope.launch {
            repository.getWorkoutById(workoutId)?.let { workout ->
                CloudLogger.info(TAG, "Found workout: id=${workout.id}, status=${workout.status}")
                if (workout.status != WorkoutStatus.COMPLETED) {
                    CloudLogger.warn(TAG, "viewCompletedWorkout called on non-completed workout, status: ${workout.status}")
                    // viewCompletedWorkout called on non-completed workout
                    return@let
                }

                // Stop any running timers - we're just viewing
                stopWorkoutTimer()
                skipRestTimer()

                CloudLogger.info(TAG, "Setting _currentWorkoutId to: $workoutId")
                _currentWorkoutId.value = workoutId
                _workoutState.value =
                    WorkoutState(
                        isActive = false,
                        status = WorkoutStatus.COMPLETED,
                        workoutId = workoutId,
                        startTime = workout.date,
                        workoutName = workout.name,
                        isReadOnly = true,
                        isInEditMode = false,
                        originalWorkoutData = null,
                        isProgrammeWorkout = workout.isProgrammeWorkout,
                        programmeId = workout.programmeId,
                        programmeName =
                            workout.programmeId?.let {
                                repository.getProgrammeById(it)?.name
                            },
                        programmeWorkoutName = workout.programmeWorkoutName,
                        weekNumber = workout.weekNumber,
                        dayNumber = workout.dayNumber,
                    )

                // Set the timer to the final duration (static, not running)
                _workoutTimerSeconds.value = workout.durationSeconds?.toInt() ?: 0
                workoutTimerStartTime = null

                // Load exercises and sets
                loadExercisesForWorkout(workoutId, isInitialLoad = true)
                loadInProgressWorkouts()
            }
        }
    }

    // Resume an existing workout
    suspend fun resumeWorkout(workoutId: String) {
        val trace = safeNewTrace("workout_resume")
        trace?.start()

        val workout = repository.getWorkoutById(workoutId)
        if (workout != null) {
            CloudLogger.info(TAG, "Resuming workout - id: $workoutId, status: ${workout.status}, isProgramme: ${workout.isProgrammeWorkout}")
            val isCompleted = workout.status == WorkoutStatus.COMPLETED

            // Get programme information if this is a programme workout
            val programmeName =
                if (workout.isProgrammeWorkout && workout.programmeId != null) {
                    try {
                        repository.getProgrammeById(workout.programmeId)?.name
                    } catch (e: android.database.sqlite.SQLiteException) {
                        CloudLogger.error(TAG, "Failed to get programme name for ID ${workout.programmeId}", e)
                        null
                    } catch (e: IllegalStateException) {
                        CloudLogger.error(TAG, "Invalid state when getting programme name for ID ${workout.programmeId}", e)
                        null
                    }
                } else {
                    null
                }

            _currentWorkoutId.value = workoutId
            _workoutState.value =
                WorkoutState(
                    isActive = !isCompleted,
                    status = if (isCompleted) WorkoutStatus.COMPLETED else WorkoutStatus.IN_PROGRESS,
                    workoutId = workoutId,
                    startTime = workout.date,
                    workoutName = workout.name,
                    isReadOnly = isCompleted,
                    isInEditMode = false,
                    originalWorkoutData = null,
                    // Programme Integration
                    isProgrammeWorkout = workout.isProgrammeWorkout,
                    programmeId = workout.programmeId,
                    programmeName = programmeName,
                    weekNumber = workout.weekNumber,
                    dayNumber = workout.dayNumber,
                    programmeWorkoutName = workout.programmeWorkoutName,
                    // Clear any pending celebrations from previous sessions
                    pendingPRs = emptyList(),
                    shouldShowPRCelebration = false,
                )

            // Wait for exercises to load completely before UI renders
            loadExercisesForWorkout(workoutId, isInitialLoad = true)
            loadInProgressWorkouts()

            // Handle workout timer
            if (workout.status == WorkoutStatus.COMPLETED) {
                // For completed workouts, show the final duration (NEVER start a timer)
                stopWorkoutTimer() // Ensure any running timer is stopped
                _workoutTimerSeconds.value = workout.durationSeconds?.toInt() ?: 0
                workoutTimerStartTime = null
            } else if (workout.status == WorkoutStatus.IN_PROGRESS && workout.timerStartTime != null) {
                // For in-progress workouts, resume the running timer
                resumeWorkoutTimer(workout.timerStartTime)
            } else {
                // No timer data or not started
                stopWorkoutTimer()
                _workoutTimerSeconds.value = 0
                workoutTimerStartTime = null
            }

            trace?.stop()
        }
    }

    private fun safeNewTrace(name: String): Trace? =
        try {
            FirebasePerformance.getInstance().newTrace(name)
        } catch (e: IllegalStateException) {
            ExceptionLogger.logNonCritical(TAG, "Firebase Performance not available - likely in test environment", e)
            null
        } catch (e: ExceptionInInitializerError) {
            ExceptionLogger.logNonCritical(TAG, "Firebase Performance initialization failed", e)
            null
        } catch (e: NoClassDefFoundError) {
            ExceptionLogger.logNonCritical(TAG, "Firebase Performance class not found", e)
            null
        }

    // Start a completely new workout (force new)
    fun startNewWorkout(forceNew: Boolean = false) {
        CloudLogger.debug(TAG, "[WORKOUT_CREATE] startNewWorkout called - forceNew: $forceNew")

        // Prevent concurrent workout creation
        if (isCreatingWorkout) {
            CloudLogger.debug(TAG, "[WORKOUT_CREATE] Already creating workout, skipping duplicate call")
            return
        }

        viewModelScope.launch {
            isCreatingWorkout = true
            try {
                val ongoingWorkout = repository.getOngoingWorkout()
                CloudLogger.debug(TAG, "[WORKOUT_CREATE] Ongoing workout check - found: ${ongoingWorkout?.id}, status: ${ongoingWorkout?.status}")

                if (forceNew || ongoingWorkout == null) {
                    CloudLogger.debug(TAG, "[WORKOUT_CREATE] Creating NEW workout - reason: ${if (forceNew) "FORCED" else "NO_ONGOING"}")
                    // Clear any existing timers
                    stopWorkoutTimer()
                    _workoutTimerSeconds.value = 0
                    workoutTimerStartTime = null

                    // Clear validation cache
                    _setCompletionValidation.value = emptyMap()

                    val now = LocalDateTime.now()
                    val defaultName =
                        now.format(
                            DateTimeFormatter
                                .ofPattern("MMM d, yyyy 'at' HH:mm"),
                        )
                    val workout =
                        Workout(
                            date = now,
                            name = defaultName,
                            notes = null,
                        )
                    CloudLogger.debug(TAG, "[WORKOUT_CREATE] About to INSERT workout to database")
                    val workoutId = repository.insertWorkout(workout)
                    CloudLogger.debug(TAG, "[WORKOUT_CREATE] INSERTED workout - ID: $workoutId, timestamp: ${LocalDateTime.now()}")

                    CloudLogger.info(
                        TAG,
                        "Starting new workout - id: $workoutId, forceNew: $forceNew, " +
                            "timestamp: ${LocalDateTime.now()}",
                    )

                    _currentWorkoutId.value = workoutId
                    _workoutState.value =
                        WorkoutState(
                            isActive = true,
                            status = WorkoutStatus.IN_PROGRESS,
                            workoutId = workoutId,
                            startTime = LocalDateTime.now(),
                            workoutName = null,
                            isReadOnly = false,
                            isInEditMode = false,
                            originalWorkoutData = null,
                            // No programme context for regular workouts
                            isProgrammeWorkout = false,
                            programmeId = null,
                            programmeName = null,
                            weekNumber = null,
                            dayNumber = null,
                            programmeWorkoutName = null,
                            // Clear any pending celebrations from previous workouts
                            pendingPRs = emptyList(),
                            shouldShowPRCelebration = false,
                        )

                    loadExercisesForWorkout(workoutId, isInitialLoad = true)
                    loadInProgressWorkouts()
                } else {
                    CloudLogger.debug(TAG, "[WORKOUT_CREATE] NOT creating workout - resuming existing ID: ${ongoingWorkout.id}")
                    CloudLogger.info(TAG, "Resuming existing workout instead of starting new - workoutId: ${ongoingWorkout.id}")
                    resumeWorkout(ongoingWorkout.id)
                }
            } finally {
                isCreatingWorkout = false
            }
        }
    }

    // Complete the current workout (handles both regular and programme workouts)
    fun completeWorkout(
        onComplete: (() -> Unit)? = null,
        onProgrammeComplete: ((String) -> Unit)? = null,
    ) {
        val currentId = _currentWorkoutId.value ?: return

        viewModelScope.launch {
            val state = _workoutState.value

            val exerciseCount = selectedWorkoutExercises.value.size
            val completedSets = selectedExerciseSets.value.count { it.isCompleted }
            val totalSets = selectedExerciseSets.value.size
            val duration = _workoutTimerSeconds.value

            CloudLogger.info(
                TAG,
                "Completing workout - id: $currentId, duration: ${duration}s, " +
                    "exercises: $exerciseCount, completed sets: $completedSets/$totalSets, " +
                    "isProgramme: ${state.isProgrammeWorkout}, programme: ${state.programmeName ?: "none"}",
            )

            // Store programme ID before completion if this is a programme workout
            val programmeId =
                if (state.isProgrammeWorkout) {
                    repository.getWorkoutById(currentId)?.programmeId
                } else {
                    null
                }

            // Calculate final duration and complete the workout
            val finalDuration =
                if (workoutTimerStartTime != null) {
                    java.time.Duration
                        .between(workoutTimerStartTime, LocalDateTime.now())
                        .seconds
                } else {
                    _workoutTimerSeconds.value.toLong()
                }

            // Complete the workout (this will automatically update programme progress if applicable)
            repository.completeWorkout(currentId, finalDuration.toString())

            // Calculate and save deviations for programme workouts
            if (state.isProgrammeWorkout) {
                repository.calculateAndSaveDeviations(currentId)
            }

            val newStatus = WorkoutStatus.COMPLETED

            // Clear rest timer on workout completion
            skipRestTimer()

            // Stop and reset workout timer
            stopWorkoutTimer()
            _workoutTimerSeconds.value = 0
            workoutTimerStartTime = null

            _workoutState.value =
                state.copy(
                    isActive = false,
                    status = newStatus,
                    isReadOnly = true,
                    isInEditMode = false,
                    originalWorkoutData = null,
                )

            loadInProgressWorkouts()

            // Trigger debounced sync after workout completion
            triggerDebouncedSync()

            // Check if programme is complete
            if (programmeId != null) {
                val programme = repository.getProgrammeById(programmeId)
                if (programme?.status == com.github.radupana.featherweight.data.programme.ProgrammeStatus.COMPLETED) {
                    onProgrammeComplete?.invoke(programmeId)
                    return@launch
                }
            }

            // Callback for UI to handle post-completion actions
            onComplete?.invoke()
        }
    }

    // Update workout name (separate from notes)
    fun updateWorkoutName(name: String?) {
        val currentId = _currentWorkoutId.value ?: return
        viewModelScope.launch {
            repository.updateWorkoutName(currentId, name)
            _workoutState.value = _workoutState.value.copy(workoutName = name)
            loadInProgressWorkouts()
        }
    }

    // Start editing a workout template (for parsed programmes)
    fun startTemplateEdit(
        weekIndex: Int,
        workoutIndex: Int,
        parsedWorkout: com.github.radupana.featherweight.data.ParsedWorkout,
    ) {
        CloudLogger.debug("WorkoutViewModel", "startTemplateEdit called: week=$weekIndex, workout=$workoutIndex")
        CloudLogger.debug("WorkoutViewModel", "ParsedWorkout: ${parsedWorkout.exercises.size} exercises")

        viewModelScope.launch {
            // Clear any existing workout
            stopWorkoutTimer()
            _currentWorkoutId.value = null
            _selectedWorkoutExercises.value = emptyList()
            _selectedExerciseSets.value = emptyList()

            // Set template edit mode
            _workoutState.value =
                WorkoutState(
                    isActive = true,
                    mode = WorkoutMode.TEMPLATE_EDIT,
                    workoutName = parsedWorkout.name,
                    templateWeekIndex = weekIndex,
                    templateWorkoutIndex = workoutIndex,
                )

            // Load the parsed exercises into the workout
            val tempExercises = mutableListOf<ExerciseLog>()
            val tempSets = mutableListOf<SetLog>()
            val tempExerciseNames = mutableMapOf<String, String>()

            var setIdCounter = 1 // Counter for template sets

            parsedWorkout.exercises.forEachIndexed { exerciseIndex, parsedExercise ->
                // Use matchedExerciseId if available, otherwise try to find by name
                val variation =
                    if (parsedExercise.matchedExerciseId != null) {
                        repository.getExerciseById(parsedExercise.matchedExerciseId)
                    } else {
                        repository.getExerciseByName(parsedExercise.exerciseName)
                    }

                if (variation != null) {
                    // Create temporary exercise log with negative ID for template
                    val exerciseLog =
                        ExerciseLog(
                            id = "template_exercise_${exerciseIndex + 1}", // Template ID
                            workoutId = "template_workout", // Temporary ID for template
                            exerciseId = variation.id,
                            exerciseOrder = exerciseIndex,
                            notes = parsedExercise.notes,
                        )

                    tempExercises.add(exerciseLog)
                    // Use proper key based on whether it's a custom exercise
                    val key = getExerciseNameKey(exerciseLog.exerciseId)
                    tempExerciseNames[key] = variation.name

                    // Create sets for this exercise with unique negative IDs
                    parsedExercise.sets.forEachIndexed { setIndex, parsedSet ->
                        val setLog =
                            SetLog(
                                id = "template_set_${setIdCounter++}", // Template ID for each set
                                exerciseLogId = exerciseLog.id,
                                setOrder = setIndex + 1,
                                targetReps = parsedSet.reps,
                                targetWeight = parsedSet.weight?.let { WeightFormatter.roundToNearestQuarter(it) },
                                targetRpe = WeightFormatter.roundRPE(parsedSet.rpe),
                                actualReps = parsedSet.reps ?: 0,
                                actualWeight = parsedSet.weight?.let { WeightFormatter.roundToNearestQuarter(it) } ?: 0f,
                                actualRpe = WeightFormatter.roundRPE(parsedSet.rpe),
                                isCompleted = false,
                            )

                        CloudLogger.debug("WorkoutViewModel", "  SetLog created: targetReps=${setLog.targetReps}, targetWeight=${setLog.targetWeight}, targetRpe=${setLog.targetRpe}")
                        tempSets.add(setLog)
                    }
                } else {
                    CloudLogger.warn("WorkoutViewModel", "Exercise not found: ${parsedExercise.exerciseName} (ID: ${parsedExercise.matchedExerciseId})")
                }
            }

            // Update state with all exercises and sets
            _selectedWorkoutExercises.value = tempExercises
            _selectedExerciseSets.value = tempSets
            _exerciseNames.value = tempExerciseNames
        }
    }

    // Get display name for workout
    fun getWorkoutDisplayName(): String {
        val state = _workoutState.value
        return state.workoutName ?: state.startTime?.format(
            DateTimeFormatter.ofPattern("MMM d 'at' h:mm a"),
        ) ?: "Current Workout"
    }

    // Check if current workout can be edited
    fun canEditWorkout(): Boolean {
        val state = _workoutState.value
        // Completed workouts are always read-only, no exceptions
        return !state.isReadOnly
    }

    // Public synchronous function for UI
    fun canMarkSetComplete(set: SetLog): Boolean {
        // Use cached validation result if available
        val cachedResult = _setCompletionValidation.value[set.id]
        if (cachedResult != null) {
            return cachedResult
        }

        // If no cached result, trigger async validation and force UI update
        viewModelScope.launch {
            val validationResult = canMarkSetCompleteInternal(set)
            val validationMap = _setCompletionValidation.value.toMutableMap()
            validationMap[set.id] = validationResult
            _setCompletionValidation.value = validationMap

            // Force UI update by updating sets state to trigger recomposition
            _selectedExerciseSets.value = _selectedExerciseSets.value.toList()
        }
        return set.actualReps > 0 && set.actualWeight > 0
    }

    // Internal suspend function for validation logic
    private suspend fun canMarkSetCompleteInternal(set: SetLog): Boolean {
        // Find the exercise log for this set
        val exerciseLog = _selectedWorkoutExercises.value.find { it.id == set.exerciseLogId }

        // If exerciseLog has an exerciseId, check if the exercise requires weight
        val exerciseDetails = exerciseLog?.exerciseId?.let { repository.getExerciseById(it) }
        val requiresWeight = exerciseDetails?.requiresWeight ?: true

        val result =
            if (!requiresWeight) {
                // For exercises that don't require weight (like pull-ups), only check actualReps
                set.actualReps > 0
            } else {
                // For exercises that require weight, check both actualReps and actualWeight
                set.actualReps > 0 && set.actualWeight > 0
            }

        return result
    }

    private suspend fun loadExercisesForWorkout(
        workoutId: String,
        isInitialLoad: Boolean = false,
    ) {
        // Set loading state to true before starting
        _workoutState.value = _workoutState.value.copy(isLoadingExercises = true)

        try {
            _selectedWorkoutExercises.value = repository.getExercisesForWorkout(workoutId)

            // Load exercise names and last performance for all exercises
            val namesMap = mutableMapOf<String, String>()
            val performanceMap = mutableMapOf<String, SetLog>()
            val maxPerformanceMap = mutableMapOf<String, SetLog>()
            val prMap = mutableMapOf<String, PersonalRecord>()

            _selectedWorkoutExercises.value.forEach { exerciseLog ->
                // Get exercise name from the unified table
                val exerciseName = repository.getExerciseById(exerciseLog.exerciseId)?.name
                exerciseName?.let {
                    // Store with proper key to avoid ID collisions
                    val key = getExerciseNameKey(exerciseLog.exerciseId)
                    namesMap[key] = it
                }
                // Load last performance for this exercise variation
                val lastPerf = repository.getLastPerformanceForExercise(exerciseLog.exerciseId)
                lastPerf?.let {
                    performanceMap[exerciseLog.exerciseId] = it
                }

                val lastMaxPerf = repository.getLastMaxPerformanceForExercise(exerciseLog.exerciseId)
                lastMaxPerf?.let {
                    maxPerformanceMap[exerciseLog.exerciseId] = it
                }

                val weightPR = repository.getWeightPRForExercise(exerciseLog.exerciseId)
                weightPR?.let {
                    prMap[exerciseLog.exerciseId] = it
                }
            }
            _exerciseNames.value = namesMap
            _lastPerformance.value = performanceMap
            _lastMaxPerformance.value = maxPerformanceMap
            _weightPRs.value = prMap

            // Wait for sets to be loaded completely
            loadAllSetsForCurrentExercisesAndWait()

            if (isInitialLoad) {
                // Initial load: expand all exercises by default
                _expandedExerciseIds.value = _selectedWorkoutExercises.value.map { it.id }.toSet()
            } else {
                // Refresh: only clean up deleted exercise IDs, preserve user's expansion choices
                val currentExerciseIds = _selectedWorkoutExercises.value.map { it.id }.toSet()
                _expandedExerciseIds.value = _expandedExerciseIds.value.filter { it in currentExerciseIds }.toSet()
            }
        } finally {
            // Always set loading state to false when done
            _workoutState.value = _workoutState.value.copy(isLoadingExercises = false)
        }
    }

    private suspend fun updateSetCompletionValidation() {
        val validationMap = mutableMapOf<String, Boolean>()
        _selectedExerciseSets.value.forEach { set ->
            val isValid = canMarkSetCompleteInternal(set)
            validationMap[set.id] = isValid
        }
        _setCompletionValidation.value = validationMap

        // Force UI recomposition by updating the sets state
        // This ensures the UI reflects the updated validation cache
        _selectedExerciseSets.value = _selectedExerciseSets.value.toList()
    }

    private fun loadAllSetsForCurrentExercises() {
        // Skip database loading in TEMPLATE_EDIT mode
        if (_workoutState.value.mode == WorkoutMode.TEMPLATE_EDIT) {
            return
        }

        viewModelScope.launch {
            val allSets = mutableListOf<SetLog>()
            _selectedWorkoutExercises.value.forEach { exercise ->
                val sets = repository.getSetsForExercise(exercise.id)
                allSets.addAll(sets)
            }
            _selectedExerciseSets.value = allSets
            updateSetCompletionValidation()
            // Load 1RM estimates when sets are loaded
            loadOneRMEstimatesForCurrentExercises()
        }
    }

    private fun loadOneRMEstimatesForCurrentExercises() {
        viewModelScope.launch {
            val exerciseIds = _selectedWorkoutExercises.value.map { it.exerciseId }
            if (exerciseIds.isEmpty()) {
                _oneRMEstimates.value = emptyMap()
                return@launch
            }

            try {
                val maxes = repository.getCurrentMaxesForExercises(exerciseIds)
                _oneRMEstimates.value = maxes.filterValues { it > 0 }
            } catch (e: android.database.sqlite.SQLiteException) {
                CloudLogger.error(TAG, "Failed to load 1RM estimates", e)
                // Failed to load 1RM estimates - will show without estimates
            } catch (e: IllegalStateException) {
                CloudLogger.error(TAG, "Invalid state when loading 1RM estimates", e)
                // Failed to load 1RM estimates - will show without estimates
            }
        }
    }

    private suspend fun loadAllSetsForCurrentExercisesAndWait() {
        // Skip database loading in TEMPLATE_EDIT mode
        if (_workoutState.value.mode == WorkoutMode.TEMPLATE_EDIT) {
            return
        }

        val allSets = mutableListOf<SetLog>()
        _selectedWorkoutExercises.value.forEach { exercise ->
            val sets = repository.getSetsForExercise(exercise.id)
            allSets.addAll(sets)
        }
        _selectedExerciseSets.value = allSets
        updateSetCompletionValidation()
    }

    // ===== ENHANCED EXERCISE MANAGEMENT =====

    fun addExerciseToCurrentWorkout(exerciseWithDetails: ExerciseWithDetails) {
        if (!canEditWorkout()) return

        val exercise = exerciseWithDetails.variation

        // In TEMPLATE_EDIT mode, add locally only
        if (_workoutState.value.mode == WorkoutMode.TEMPLATE_EDIT) {
            val currentExercises = _selectedWorkoutExercises.value
            val newExerciseId = "temp_exercise_${System.currentTimeMillis()}_${currentExercises.size}"
            val newExercise =
                ExerciseLog(
                    id = newExerciseId,
                    workoutId = "temp_workout", // Template workout
                    exerciseId = exercise.id,
                    exerciseOrder = currentExercises.size,
                    notes = null,
                )

            // Add exercise to local state
            _selectedWorkoutExercises.value = currentExercises + newExercise

            // Add exercise name to cache
            // Add with proper key based on whether it's custom
            val key = getExerciseNameKey(exercise.id)
            _exerciseNames.value = _exerciseNames.value + (key to exercise.name)

            // Auto-add first empty set
            val currentSets = _selectedExerciseSets.value
            val firstSet =
                SetLog(
                    id = "temp_set_${System.currentTimeMillis()}_${currentSets.size}",
                    exerciseLogId = newExerciseId,
                    setOrder = 1,
                    targetReps = 0,
                    targetWeight = 0f,
                    actualReps = 0,
                    actualWeight = 0f,
                    isCompleted = false,
                )
            _selectedExerciseSets.value = currentSets + firstSet

            // Auto-expand the newly added exercise
            _expandedExerciseIds.value = _expandedExerciseIds.value + newExerciseId
            return
        }

        val currentId = _currentWorkoutId.value ?: return
        viewModelScope.launch {
            val exerciseLogId =
                repository.insertExerciseLogWithExerciseReference(
                    workoutId = currentId,
                    exercise = exercise,
                    exerciseOrder = selectedWorkoutExercises.value.size,
                    notes = null,
                )

            CloudLogger.info(TAG, "Exercise added to workout - exercise: ${exercise.name}, workoutId: $currentId, order: ${selectedWorkoutExercises.value.size}, exerciseLogId: $exerciseLogId")

            // Auto-add first empty set for better UX
            val firstSet =
                SetLog(
                    exerciseLogId = exerciseLogId,
                    setOrder = 1,
                    targetReps = null,
                    targetWeight = null,
                    actualReps = 0,
                    actualWeight = 0f,
                    isCompleted = false,
                )
            val setId = repository.insertSetLog(firstSet)
            CloudLogger.debug(TAG, "Auto-added first set - exerciseLogId: $exerciseLogId, setId: $setId")

            loadExercisesForWorkout(currentId)
            loadExerciseHistory(exercise.id)
            loadInProgressWorkouts()

            // Auto-expand the newly added exercise
            val currentExpanded = _expandedExerciseIds.value.toMutableSet()
            currentExpanded.add(exerciseLogId)
            _expandedExerciseIds.value = currentExpanded
        }
    }

    private fun loadExerciseHistory(exerciseId: String) {
        val currentId = _currentWorkoutId.value ?: return
        viewModelScope.launch {
            val history = repository.getExerciseHistory(exerciseId, currentId)
            if (history != null) {
                val currentHistory = _exerciseHistory.value.toMutableMap()
                currentHistory[exerciseId] = history
                _exerciseHistory.value = currentHistory
            }
        }
    }

    // ===== EXERCISE CARD EXPANSION MANAGEMENT =====

    fun toggleExerciseExpansion(exerciseId: String) {
        val current = _expandedExerciseIds.value.toMutableSet()
        if (current.contains(exerciseId)) {
            current.remove(exerciseId)
        } else {
            current.add(exerciseId)
        }
        _expandedExerciseIds.value = current
    }

    fun collapseAllExercises() {
        // Collapse all exercises - useful when starting to drag
        _expandedExerciseIds.value = emptySet()
    }

    // ===== EXERCISE REORDERING =====

    fun reorderExercises(
        fromIndex: Int,
        toIndex: Int,
    ) {
        if (!canEditWorkout() || fromIndex == toIndex) return

        val exercises = _selectedWorkoutExercises.value.toMutableList()

        // Validate indices
        val isFromIndexInvalid = fromIndex < 0 || fromIndex >= exercises.size
        val isToIndexInvalid = toIndex < 0 || toIndex >= exercises.size
        if (isFromIndexInvalid || isToIndexInvalid) return

        // Move the exercise
        val exerciseToMove = exercises.removeAt(fromIndex)
        exercises.add(toIndex, exerciseToMove)

        // Update UI state immediately for smooth visual feedback
        _selectedWorkoutExercises.value = exercises

        // Commit to database immediately
        commitExerciseReordering()
    }

    private fun commitExerciseReordering() {
        viewModelScope.launch {
            try {
                val exercises = _selectedWorkoutExercises.value

                // Update exercise orders in database
                exercises.forEachIndexed { index, exercise ->
                    repository.updateExerciseOrder(exercise.id, index)
                }
            } catch (e: android.database.sqlite.SQLiteException) {
                CloudLogger.error(TAG, "Error reordering exercises", e)
            } catch (e: IllegalStateException) {
                CloudLogger.error(TAG, "Invalid state when reordering exercises", e)
            }
        }
    }

    // ===== EXISTING SET MANAGEMENT METHODS =====

    // Set management - only allowed if workout can be edited
    fun addSetToExercise(
        exerciseLogId: String,
        targetReps: Int? = null,
        targetWeight: Float? = null,
        weight: Float = 0f,
        reps: Int = 0,
        rpe: Float? = null,
        onSetCreated: (String) -> Unit = {},
    ) {
        if (!canEditWorkout()) return

        viewModelScope.launch {
            val setOrder = repository.getSetsForExercise(exerciseLogId).size
            val isProgrammeWorkout = _workoutState.value.isProgrammeWorkout
            val setLog =
                SetLog(
                    exerciseLogId = exerciseLogId,
                    setOrder = setOrder,
                    targetReps = if (isProgrammeWorkout) targetReps else null,
                    targetWeight = if (isProgrammeWorkout) targetWeight?.let { WeightFormatter.roundToNearestQuarter(it) } else null,
                    actualReps = reps,
                    actualWeight = WeightFormatter.roundToNearestQuarter(weight),
                    actualRpe = WeightFormatter.roundRPE(rpe),
                    tag = null,
                    notes = null,
                    isCompleted = false,
                    completedAt = null,
                )
            val newSetId = repository.insertSetLog(setLog)

            // Immediately update validation cache for the new set with actual values
            val newSet = setLog.copy(id = newSetId)
            val validationResult = canMarkSetCompleteInternal(newSet)
            val validationMap = _setCompletionValidation.value.toMutableMap()
            validationMap[newSetId] = validationResult
            _setCompletionValidation.value = validationMap

            loadAllSetsForCurrentExercises()
            loadInProgressWorkouts()
            onSetCreated(newSetId)
        }
    }

    fun addSet(exerciseLogId: String) {
        // In TEMPLATE_EDIT mode, add sets locally only
        if (_workoutState.value.mode == WorkoutMode.TEMPLATE_EDIT) {
            val currentSets = _selectedExerciseSets.value
            val exerciseSets = currentSets.filter { it.exerciseLogId == exerciseLogId }
            val nextOrder = (exerciseSets.maxOfOrNull { it.setOrder } ?: 0) + 1
            // Generate a unique temporary ID for template sets
            val tempId = "temp_${System.currentTimeMillis()}_${exerciseLogId}_$nextOrder"
            val newSet =
                SetLog(
                    id = tempId,
                    exerciseLogId = exerciseLogId,
                    setOrder = nextOrder,
                    targetReps = 0,
                    targetWeight = 0f,
                    actualReps = 0,
                    actualWeight = 0f,
                    isCompleted = false,
                )
            _selectedExerciseSets.value = currentSets + newSet
        } else {
            addSetToExercise(exerciseLogId) { }
        }
    }

    fun copyLastSet(exerciseLogId: String) {
        if (!canEditWorkout()) return

        // In TEMPLATE_EDIT mode, copy from local state
        if (_workoutState.value.mode == WorkoutMode.TEMPLATE_EDIT) {
            val currentSets = _selectedExerciseSets.value
            val exerciseSets = currentSets.filter { it.exerciseLogId == exerciseLogId }
            val lastSet = exerciseSets.maxByOrNull { it.setOrder }
            if (lastSet != null) {
                val nextOrder = (exerciseSets.maxOfOrNull { it.setOrder } ?: 0) + 1
                // Generate a unique temporary ID for template sets
                val tempId = "temp_${System.currentTimeMillis()}_${exerciseLogId}_$nextOrder"
                val newSet =
                    lastSet.copy(
                        id = tempId,
                        setOrder = nextOrder,
                        isCompleted = false,
                    )
                _selectedExerciseSets.value = currentSets + newSet
            }
            return
        }

        viewModelScope.launch {
            val sets = repository.getSetsForExercise(exerciseLogId)
            val lastSet = sets.maxByOrNull { it.setOrder }

            if (lastSet != null && (lastSet.actualReps > 0 || lastSet.actualWeight > 0)) {
                val newSetOrder = (sets.maxOfOrNull { it.setOrder } ?: 0) + 1
                val isProgrammeWorkout = _workoutState.value.isProgrammeWorkout
                val newSet =
                    SetLog(
                        exerciseLogId = exerciseLogId,
                        setOrder = newSetOrder,
                        targetReps = if (isProgrammeWorkout) lastSet.targetReps else null,
                        targetWeight = if (isProgrammeWorkout) lastSet.targetWeight?.let { WeightFormatter.roundToNearestQuarter(it) } else null,
                        actualReps = lastSet.actualReps,
                        actualWeight = WeightFormatter.roundToNearestQuarter(lastSet.actualWeight),
                        actualRpe = WeightFormatter.roundRPE(lastSet.actualRpe),
                        isCompleted = false,
                    )

                val newSetId = repository.insertSetLog(newSet)

                // Update validation cache
                val validationResult = canMarkSetCompleteInternal(newSet.copy(id = newSetId))
                val validationMap = _setCompletionValidation.value.toMutableMap()
                validationMap[newSetId] = validationResult
                _setCompletionValidation.value = validationMap

                loadAllSetsForCurrentExercises()
                loadInProgressWorkouts()
            }
        }
    }

    fun updateSet(
        setId: String,
        reps: Int,
        weight: Float,
        rpe: Float?,
    ) {
        if (!canEditWorkout()) return

        val roundedWeight = WeightFormatter.roundToNearestQuarter(weight)
        val roundedRpe = WeightFormatter.roundRPE(rpe)

        CloudLogger.info(TAG, "Updating set - setId: $setId, weight: ${roundedWeight}kg, reps: $reps, rpe: $roundedRpe")

        viewModelScope.launch {
            val currentSets = _selectedExerciseSets.value
            val currentSet = currentSets.firstOrNull { it.id == setId }
            if (currentSet != null) {
                // In TEMPLATE_EDIT mode, update TARGET values (what the programme prescribes)
                // AND also update actual values for UI display
                // In normal workout mode, update ACTUAL values (what the user did)
                val updatedSet =
                    if (_workoutState.value.mode == WorkoutMode.TEMPLATE_EDIT) {
                        currentSet.copy(
                            targetReps = reps,
                            targetWeight = roundedWeight,
                            targetRpe = roundedRpe,
                            actualReps = reps,
                            actualWeight = roundedWeight,
                            actualRpe = roundedRpe,
                        )
                    } else {
                        currentSet.copy(
                            actualReps = reps,
                            actualWeight = roundedWeight,
                            actualRpe = roundedRpe,
                        )
                    }

                // Update the set in the local state immediately to prevent UI flicker
                val updatedSets =
                    currentSets.map { set ->
                        if (set.id == setId) updatedSet else set
                    }
                _selectedExerciseSets.value = updatedSets

                // Skip database operations in TEMPLATE_EDIT mode
                if (_workoutState.value.mode != WorkoutMode.TEMPLATE_EDIT) {
                    // Update validation cache for this set immediately within the same coroutine
                    val validationResult = canMarkSetCompleteInternal(updatedSet)
                    val validationMap = _setCompletionValidation.value.toMutableMap()
                    validationMap[setId] = validationResult
                    _setCompletionValidation.value = validationMap

                    // Then persist to database
                    repository.updateSetLog(updatedSet)

                    // Only reload in-progress workouts to update the card
                    loadInProgressWorkouts()
                }
            }
        }
    }

    fun deleteSet(setId: String) {
        if (!canEditWorkout()) return

        viewModelScope.launch {
            // Update local state immediately
            val updatedSets = _selectedExerciseSets.value.filter { it.id != setId }
            _selectedExerciseSets.value = updatedSets

            // Skip database operations in TEMPLATE_EDIT mode
            if (_workoutState.value.mode != WorkoutMode.TEMPLATE_EDIT) {
                // Then delete from database
                repository.deleteSetLog(setId)
                loadInProgressWorkouts()
            }
        }
    }

    fun markSetCompleted(
        setId: String,
        completed: Boolean,
    ) {
        // Skip in TEMPLATE_EDIT mode - templates can't be completed
        if (_workoutState.value.mode == WorkoutMode.TEMPLATE_EDIT) {
            return
        }

        viewModelScope.launch {
            completeSetInternal(setId, completed)
        }
    }

    private suspend fun completeSetInternal(
        setId: String,
        completed: Boolean,
    ) {
        // Validation
        if (!validateSetCompletion(setId, completed)) return

        // Update set state
        val timestamp = if (completed) LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) else null
        val updatedSets = updateSetState(setId, completed, timestamp)

        // Update workout status if needed
        updateWorkoutStatusIfNeeded(setId, completed)

        // Persist to database
        repository.markSetCompleted(setId, completed, timestamp)

        // Handle post-completion actions
        if (completed) {
            handleSetCompletion(setId, updatedSets)
        }

        loadInProgressWorkouts()
    }

    /**
     * Completes multiple sets in a batch, checking for PRs once at the end
     */
    private suspend fun completeSetsInBatch(
        setsToComplete: List<SetLog>,
    ) {
        if (setsToComplete.isEmpty()) return

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val completedSetIds = mutableListOf<String>()

        // Step 1: Update all sets in local state and database
        for (set in setsToComplete) {
            // Validate
            if (!validateSetCompletion(set.id, true)) continue

            // Update local state
            updateSetState(set.id, true, timestamp)

            // Persist to database
            repository.markSetCompleted(set.id, true, timestamp)

            completedSetIds.add(set.id)
        }

        if (completedSetIds.isEmpty()) return

        // Step 2: Update workout status once
        updateWorkoutStatusIfNeeded(completedSetIds.first(), true)

        // Step 3: Start timers once (use the last set's RPE for rest timer)
        val lastCompletedSet = setsToComplete.lastOrNull()
        if (lastCompletedSet != null) {
            val exerciseLog = _selectedWorkoutExercises.value.find { it.id == lastCompletedSet.exerciseLogId }
            val exerciseVariation = exerciseLog?.exerciseId?.let { repository.getExerciseById(it) }

            val restDuration =
                restTimerCalculationService.calculateRestDuration(
                    rpe = lastCompletedSet.actualRpe,
                    exerciseRestDuration = exerciseVariation?.restDurationSeconds,
                )

            CloudLogger.debug(TAG, "Rest timer calculated for batch: ${restDuration}s (RPE: ${lastCompletedSet.actualRpe})")
            startRestTimer(restDuration)

            if (workoutTimerStartTime == null) {
                startWorkoutTimer()
            }
        }

        // Step 4: Group sets by exercise for batch 1RM processing
        val completedSets =
            completedSetIds.mapNotNull { setId ->
                _selectedExerciseSets.value.find { it.id == setId }
            }

        // Group sets by exercise using the batch service
        val setsByExercise =
            batchCompletionService.groupSetsByExercise(completedSets) { set ->
                _selectedWorkoutExercises.value.find { it.id == set.exerciseLogId }?.exerciseId
            }

        // Step 5: Process 1RM updates - one per exercise with the best set
        for ((exerciseId, exerciseSets) in setsByExercise) {
            // Get exercise variation to determine scaling type for this exercise
            val exerciseVariation = repository.getExerciseById(exerciseId)
            val scalingType = exerciseVariation?.rmScalingType?.toRMScalingType() ?: RMScalingType.STANDARD

            // Find the best set for 1RM update
            val bestSet =
                batchCompletionService.findBestSetForOneRM(exerciseSets) { set ->
                    // Calculate estimated 1RM for this set
                    oneRMService.calculateEstimated1RM(set.actualWeight, set.actualReps, set.actualRpe, scalingType)
                }

            // Update 1RM with the best set only
            if (bestSet != null) {
                val currentEstimate = _oneRMEstimates.value[exerciseId]
                val newEstimate =
                    oneRMService.calculateEstimated1RM(
                        bestSet.actualWeight,
                        bestSet.actualReps,
                        bestSet.actualRpe,
                        scalingType,
                    )

                if (newEstimate != null && oneRMService.shouldUpdateOneRM(bestSet, currentEstimate, newEstimate)) {
                    CloudLogger.info(
                        TAG,
                        "Batch 1RM update - exercise: ${exerciseVariation?.name}, " +
                            "old: ${currentEstimate?.toInt()}kg, new: ${newEstimate.toInt()}kg, " +
                            "from: ${bestSet.actualWeight}kg x ${bestSet.actualReps} @ RPE ${bestSet.actualRpe}",
                    )
                    persistOneRMUpdate(exerciseId, bestSet, newEstimate, currentEstimate)
                }
            }
        }

        // Step 6: Check for PRs for each completed set
        val allPRs = mutableListOf<PersonalRecord>()
        val exerciseIds = mutableSetOf<String>()

        for (setId in completedSetIds) {
            val completedSet = _selectedExerciseSets.value.find { it.id == setId } ?: continue
            val exerciseLog = _selectedWorkoutExercises.value.find { it.id == completedSet.exerciseLogId } ?: continue

            exerciseIds.add(exerciseLog.exerciseId)

            // Collect PRs
            val setPRs = repository.checkForPR(completedSet, exerciseLog.exerciseId)
            allPRs.addAll(setPRs)
        }

        // Step 7: Show PR celebration once with all PRs
        if (allPRs.isNotEmpty()) {
            // Ensure exercise names are available
            for (exerciseId in exerciseIds) {
                val exerciseName = repository.getExerciseById(exerciseId)?.name
                if (exerciseName != null) {
                    val key = getExerciseNameKey(exerciseId)
                    CloudLogger.debug(TAG, "Adding exercise name for batch PR: $key -> $exerciseName")
                    _exerciseNames.value = _exerciseNames.value + (key to exerciseName)
                }
            }

            // Use the service to filter and keep only the best PR per exercise
            val bestPRsByExercise = batchCompletionService.filterBestPRsPerExercise(allPRs)

            CloudLogger.debug(TAG, "Batch completion found ${allPRs.size} PRs, showing ${bestPRsByExercise.size} best PRs")

            _workoutState.value =
                _workoutState.value.copy(
                    pendingPRs = _workoutState.value.pendingPRs + bestPRsByExercise,
                    shouldShowPRCelebration = true,
                )
        }

        // Step 6: Update 1RM estimates
        for (setId in completedSetIds) {
            val completedSet = _selectedExerciseSets.value.find { it.id == setId } ?: continue
            val exerciseLog = _selectedWorkoutExercises.value.find { it.id == completedSet.exerciseLogId } ?: continue

            val exerciseVariation = repository.getExerciseById(exerciseLog.exerciseId)
            val scalingType = exerciseVariation?.rmScalingType?.toRMScalingType() ?: RMScalingType.STANDARD

            val newEstimate =
                oneRMService.calculateEstimated1RM(
                    completedSet.actualWeight,
                    completedSet.actualReps,
                    completedSet.actualRpe,
                    scalingType,
                )

            if (newEstimate != null) {
                _oneRMEstimates.value = _oneRMEstimates.value + (exerciseLog.exerciseId to newEstimate)
            }
        }
    }

    private fun validateSetCompletion(
        setId: String,
        completed: Boolean,
    ): Boolean {
        if (!completed) return true
        val set = _selectedExerciseSets.value.find { it.id == setId }
        return set != null && canMarkSetComplete(set)
    }

    private fun updateSetState(
        setId: String,
        completed: Boolean,
        timestamp: String?,
    ): List<SetLog> {
        val updatedSets =
            _selectedExerciseSets.value.map { set ->
                if (set.id == setId) {
                    set.copy(isCompleted = completed, completedAt = timestamp)
                } else {
                    set
                }
            }
        _selectedExerciseSets.value = updatedSets
        return updatedSets
    }

    private suspend fun updateWorkoutStatusIfNeeded(
        setId: String,
        completed: Boolean,
    ) {
        val currentWorkoutId = _currentWorkoutId.value ?: return
        if (!completed) return

        val currentState = _workoutState.value
        if (!currentState.isActive || currentState.status == WorkoutStatus.COMPLETED) return

        val hasOtherCompletedSets = _selectedExerciseSets.value.any { it.isCompleted && it.id != setId }
        if (!hasOtherCompletedSets) {
            repository.updateWorkoutStatus(currentWorkoutId, WorkoutStatus.IN_PROGRESS)
        }
    }

    private suspend fun handleSetCompletion(
        setId: String,
        updatedSets: List<SetLog>,
    ) {
        val completedSet = _selectedExerciseSets.value.find { it.id == setId }

        val restDuration =
            if (completedSet != null) {
                val exerciseLog = _selectedWorkoutExercises.value.find { it.id == completedSet.exerciseLogId }
                val exerciseVariation =
                    exerciseLog?.exerciseId?.let {
                        repository.getExerciseById(it)
                    }

                val duration =
                    restTimerCalculationService.calculateRestDuration(
                        rpe = completedSet.actualRpe,
                        exerciseRestDuration = exerciseVariation?.restDurationSeconds,
                    )

                CloudLogger.debug(TAG, "Rest timer calculated: ${duration}s (RPE: ${completedSet.actualRpe}, Exercise default: ${exerciseVariation?.restDurationSeconds}s)")
                duration
            } else {
                DEFAULT_REST_TIMER_SECONDS
            }

        startRestTimer(restDuration)
        if (workoutTimerStartTime == null) {
            startWorkoutTimer()
        }

        // Check for PRs
        checkForPersonalRecords(setId, updatedSets)

        // Update 1RM estimates
        updateOneRMEstimate(setId, updatedSets)

        // Auto-collapse if all sets completed
        autoCollapseExerciseIfComplete(setId, updatedSets)
    }

    private suspend fun checkForPersonalRecords(
        setId: String,
        updatedSets: List<SetLog>,
    ) {
        try {
            val completedSet = updatedSets.find { it.id == setId } ?: return
            val exerciseLog = findExerciseLogForSet(setId, updatedSets) ?: return

            CloudLogger.debug(TAG, "Checking for PR: exerciseLog.id=${exerciseLog.id}, exerciseId=${exerciseLog.exerciseId}")
            val allPRs = repository.checkForPR(completedSet, exerciseLog.exerciseId)
            if (allPRs.isNotEmpty()) {
                // Ensure exercise name is available for PR dialog
                CloudLogger.debug(TAG, "PRs detected, looking up exercise name for dialog")
                // Get exercise name from the unified table
                val exerciseName = repository.getExerciseById(exerciseLog.exerciseId)?.name
                CloudLogger.debug(TAG, "Exercise name lookup for ID ${exerciseLog.exerciseId}: $exerciseName")
                if (exerciseName != null) {
                    val key = getExerciseNameKey(exerciseLog.exerciseId)
                    CloudLogger.debug(TAG, "Adding to exerciseNames map: $key -> $exerciseName (ID: ${exerciseLog.exerciseId})")
                    _exerciseNames.value = _exerciseNames.value + (key to exerciseName)
                } else {
                    CloudLogger.error(TAG, "Failed to get exercise name for PR dialog! ID: ${exerciseLog.exerciseId})")
                }

                _workoutState.value =
                    _workoutState.value.copy(
                        pendingPRs = _workoutState.value.pendingPRs + allPRs,
                        shouldShowPRCelebration = true,
                    )
            }
        } catch (e: android.database.sqlite.SQLiteException) {
            CloudLogger.error(TAG, "PR detection failed for set $setId", e)
        } catch (e: IllegalStateException) {
            CloudLogger.error(TAG, "Invalid state during PR detection for set $setId", e)
        }
    }

    private suspend fun updateOneRMEstimate(
        setId: String,
        updatedSets: List<SetLog>,
    ) {
        try {
            val completedSet = updatedSets.find { it.id == setId } ?: return
            val exerciseLog = findExerciseLogForSet(setId, updatedSets) ?: return

            CloudLogger.debug(
                TAG,
                "[1RM_FLOW] updateOneRMEstimate called for set: $setId, " +
                    "exercise: ${exerciseLog.exerciseId}, " +
                    "weight: ${completedSet.actualWeight}kg, reps: ${completedSet.actualReps}, RPE: ${completedSet.actualRpe}",
            )

            val exerciseVariation = repository.getExerciseById(exerciseLog.exerciseId)
            val scalingType = exerciseVariation?.rmScalingType?.toRMScalingType() ?: RMScalingType.STANDARD

            val currentEstimate = _oneRMEstimates.value[exerciseLog.exerciseId]
            val newEstimate =
                oneRMService.calculateEstimated1RM(
                    completedSet.actualWeight,
                    completedSet.actualReps,
                    completedSet.actualRpe,
                    scalingType,
                )

            CloudLogger.debug(
                TAG,
                "[1RM_FLOW] Calculated estimate: ${newEstimate?.toInt()}kg, " +
                    "current: ${currentEstimate?.toInt()}kg, " +
                    "shouldUpdate: ${newEstimate != null && oneRMService.shouldUpdateOneRM(completedSet, currentEstimate, newEstimate)}",
            )

            if (newEstimate != null && oneRMService.shouldUpdateOneRM(completedSet, currentEstimate, newEstimate)) {
                val exercise = repository.getExerciseById(exerciseLog.exerciseId)
                CloudLogger.info(
                    TAG,
                    "[1RM_FLOW] Single set 1RM update - exercise: ${exercise?.name}, " +
                        "old: ${currentEstimate?.toInt()}kg, new: ${newEstimate.toInt()}kg, " +
                        "from: ${completedSet.actualWeight}kg x ${completedSet.actualReps} @ RPE ${completedSet.actualRpe}",
                )
                persistOneRMUpdate(exerciseLog.exerciseId, completedSet, newEstimate, currentEstimate)
            } else {
                CloudLogger.debug(TAG, "[1RM_FLOW] 1RM not updated (estimate not better or shouldUpdate returned false)")
            }
        } catch (e: android.database.sqlite.SQLiteException) {
            CloudLogger.error(TAG, "Failed to update 1RM estimate", e)
        } catch (e: IllegalStateException) {
            CloudLogger.error(TAG, "Invalid state when updating 1RM estimate", e)
        }
    }

    private suspend fun persistOneRMUpdate(
        exerciseId: String?,
        completedSet: SetLog,
        newEstimate: Float,
        currentEstimate: Float?,
    ) {
        exerciseId ?: return

        CloudLogger.debug(
            TAG,
            "[1RM_FLOW] persistOneRMUpdate called - exerciseId: $exerciseId, " +
                "estimate: ${newEstimate.toInt()}kg, setId: ${completedSet.id}",
        )

        val percentOf1RM =
            if (currentEstimate != null && currentEstimate > 0) {
                completedSet.actualWeight / currentEstimate
            } else {
                1f
            }

        val confidence =
            oneRMService.calculateConfidence(
                completedSet.actualReps,
                completedSet.actualRpe,
                percentOf1RM,
            )

        val oneRMRecord =
            oneRMService.createOneRMRecord(
                exerciseId = exerciseId,
                set = completedSet,
                estimate = newEstimate,
                confidence = confidence,
            )

        CloudLogger.debug(
            TAG,
            "[1RM_FLOW] Created 1RM record - mostWeightLifted: ${oneRMRecord.mostWeightLifted}kg, " +
                "mostWeightReps: ${oneRMRecord.mostWeightReps}, " +
                "sourceSetId: ${oneRMRecord.sourceSetId}, " +
                "confidence: $confidence",
        )

        repository.updateOrInsertOneRM(oneRMRecord)
        CloudLogger.debug(TAG, "[1RM_FLOW] Called repository.updateOrInsertOneRM() - persistence complete")
        loadOneRMEstimatesForCurrentExercises()
    }

    private fun findExerciseLogForSet(
        setId: String,
        updatedSets: List<SetLog>,
    ): ExerciseLog? =
        _selectedWorkoutExercises.value.find { exerciseLog ->
            updatedSets.any { it.exerciseLogId == exerciseLog.id && it.id == setId }
        }

    private fun autoCollapseExerciseIfComplete(
        setId: String,
        updatedSets: List<SetLog>,
    ) {
        val exerciseLogId = updatedSets.find { it.id == setId }?.exerciseLogId ?: return
        val exerciseSets = updatedSets.filter { it.exerciseLogId == exerciseLogId }
        val allSetsCompleted = exerciseSets.isNotEmpty() && exerciseSets.all { it.isCompleted }

        if (allSetsCompleted) {
            val currentExpanded = _expandedExerciseIds.value.toMutableSet()
            currentExpanded.remove(exerciseLogId)
            _expandedExerciseIds.value = currentExpanded
        }
    }

    fun completeAllSetsInExercise(exerciseLogId: String) {
        viewModelScope.launch {
            val exerciseSets = _selectedExerciseSets.value.filter { it.exerciseLogId == exerciseLogId }
            val setsToComplete =
                exerciseSets.filter { set ->
                    canMarkSetCompleteInternal(set) && !set.isCompleted
                }

            if (setsToComplete.isEmpty()) return@launch

            // Batch complete all sets
            completeSetsInBatch(setsToComplete)

            // Auto-collapse the exercise after completing all sets
            val currentExpanded = _expandedExerciseIds.value.toMutableSet()
            currentExpanded.remove(exerciseLogId)
            _expandedExerciseIds.value = currentExpanded

            loadAllSetsForCurrentExercises()
            loadInProgressWorkouts()
        }
    }

    fun completeAllSetsInWorkout() {
        viewModelScope.launch {
            val allSets = _selectedExerciseSets.value
            val setsToComplete =
                allSets.filter { set ->
                    canMarkSetCompleteInternal(set) && !set.isCompleted
                }

            if (setsToComplete.isEmpty()) return@launch

            // Batch complete all sets
            completeSetsInBatch(setsToComplete)

            loadAllSetsForCurrentExercises()
            loadInProgressWorkouts()
        }
    }

    fun canCompleteAllSetsInWorkout(): Boolean {
        val allSets = _selectedExerciseSets.value
        val result = allSets.isNotEmpty() && allSets.any { canMarkSetComplete(it) && !it.isCompleted }

        return result
    }

    fun canCompleteAllSetsInExercise(exerciseLogId: String): Boolean {
        val exerciseSets = _selectedExerciseSets.value.filter { it.exerciseLogId == exerciseLogId }
        return exerciseSets.isNotEmpty() && exerciseSets.any { canMarkSetComplete(it) && !it.isCompleted }
    }

    fun deleteExercise(exerciseLogId: String) {
        if (!canEditWorkout()) return

        // In TEMPLATE_EDIT mode, delete locally only
        if (_workoutState.value.mode == WorkoutMode.TEMPLATE_EDIT) {
            // Remove exercise from local state
            _selectedWorkoutExercises.value = _selectedWorkoutExercises.value.filter { it.id != exerciseLogId }
            // Remove all sets for this exercise
            _selectedExerciseSets.value = _selectedExerciseSets.value.filter { it.exerciseLogId != exerciseLogId }
            // Remove from expanded exercises
            _expandedExerciseIds.value = _expandedExerciseIds.value - exerciseLogId
            return
        }

        viewModelScope.launch {
            repository.deleteExerciseLog(exerciseLogId)
            val currentId = _currentWorkoutId.value ?: return@launch
            loadExercisesForWorkout(currentId)
            loadInProgressWorkouts()
        }
    }

    // ===== EXERCISE SWAP METHODS =====

    fun initiateExerciseSwap(exerciseLogId: String) {
        if (!canEditWorkout()) return

        val exercise = _selectedWorkoutExercises.value.find { it.id == exerciseLogId }
        if (exercise != null) {
            _swappingExercise.value = exercise
        }
    }

    fun cancelExerciseSwap() {
        _swappingExercise.value = null
    }

    fun confirmExerciseSwap(newExerciseId: String) {
        if (!canEditWorkout()) return

        val swappingExercise = _swappingExercise.value ?: return

        viewModelScope.launch {
            try {
                // Get the new exercise details
                val newExercise = repository.getExerciseEntityById(newExerciseId)
                if (newExercise != null) {
                    // Check if we're in template edit mode (negative IDs)
                    if (_workoutState.value.mode == WorkoutMode.TEMPLATE_EDIT || swappingExercise.id.startsWith("temp_")) {
                        // For template edit, just update the in-memory state
                        val updatedExercises =
                            _selectedWorkoutExercises.value.map { exercise ->
                                if (exercise.id == swappingExercise.id) {
                                    exercise.copy(
                                        exerciseId = newExerciseId,
                                        originalExerciseId = swappingExercise.originalExerciseId ?: swappingExercise.exerciseId,
                                    )
                                } else {
                                    exercise
                                }
                            }
                        _selectedWorkoutExercises.value = updatedExercises

                        // Update exercise name in the map
                        // Remove old exercise name and add new one with proper keys
                        val oldKey = getExerciseNameKey(swappingExercise.exerciseId)
                        val newKey = getExerciseNameKey(newExerciseId)
                        _exerciseNames.value =
                            _exerciseNames.value.toMutableMap().apply {
                                remove(oldKey)
                                put(newKey, newExercise.name)
                            }

                        // Clear sets for this exercise to force re-creation with new exercise
                        _selectedExerciseSets.value =
                            _selectedExerciseSets.value.filter {
                                it.exerciseLogId != swappingExercise.id
                            }
                    } else {
                        // For regular workouts, use database operations
                        repository.swapExercise(
                            exerciseLogId = swappingExercise.id,
                            newExerciseId = newExerciseId,
                            originalExerciseId = swappingExercise.originalExerciseId ?: swappingExercise.exerciseId,
                        )

                        // Clear all sets for this exercise
                        repository.deleteSetsForExerciseLog(swappingExercise.id)

                        // Record swap history
                        repository.recordExerciseSwap(
                            originalExerciseId = swappingExercise.originalExerciseId ?: swappingExercise.exerciseId,
                            swappedToExerciseId = newExerciseId,
                            workoutId = _currentWorkoutId.value,
                            programmeId = _workoutState.value.programmeId,
                        )

                        // Reload exercises for the workout
                        val currentId = _currentWorkoutId.value ?: return@launch
                        loadExercisesForWorkout(currentId)
                    }

                    // Clear swap state
                    _swappingExercise.value = null
                }
            } catch (e: android.database.sqlite.SQLiteException) {
                CloudLogger.error(TAG, "Failed to swap exercise", e)
            } catch (e: IllegalStateException) {
                CloudLogger.error(TAG, "Invalid state when swapping exercise", e)
            }
        }
    }

    // Delete the current workout entirely
    fun deleteCurrentWorkout() {
        val currentId = _currentWorkoutId.value ?: return
        viewModelScope.launch {
            repository.deleteWorkout(currentId)

            // Reset state after deletion, including timers
            skipRestTimer()
            stopWorkoutTimer()
            _workoutTimerSeconds.value = 0
            workoutTimerStartTime = null
            _currentWorkoutId.value = null
            _workoutState.value = WorkoutState()
            _selectedWorkoutExercises.value = emptyList()
            _selectedExerciseSets.value = emptyList()
            loadInProgressWorkouts()
        }
    }

    // Start a workout from a template
    suspend fun startWorkoutFromTemplate(templateId: String) {
        CloudLogger.info(TAG, "startWorkoutFromTemplate called with templateId: $templateId")
        try {
            val workoutId = repository.startWorkoutFromTemplate(templateId)
            CloudLogger.info(TAG, "Created workout $workoutId from template $templateId, now resuming...")
            resumeWorkout(workoutId)
        } catch (e: IllegalArgumentException) {
            ExceptionLogger.logException(TAG, "Invalid template ID: $templateId", e)
        } catch (e: IllegalStateException) {
            ExceptionLogger.logException(TAG, "Invalid state for starting workout from template", e)
        } catch (e: android.database.sqlite.SQLiteException) {
            ExceptionLogger.logException(TAG, "Database error starting workout from template", e)
        }
    }

    // ===== PROGRAMME WORKOUT METHODS =====

    // Start a workout from a programme template
    suspend fun startProgrammeWorkout(
        programmeId: String,
        weekNumber: Int,
        dayNumber: Int,
        onReady: (() -> Unit)? = null,
    ) {
        try {
            // Set loading state immediately
            _workoutState.value = _workoutState.value.copy(isLoadingExercises = true)

            // Get programme information
            val programme = repository.getActiveProgramme()
            if (programme == null || programme.id != programmeId) {
                _workoutState.value = _workoutState.value.copy(isLoadingExercises = false)
                return
            }

            // Check if a workout already exists for this programme/week/day
            val existingWorkout =
                _inProgressWorkouts.value.find {
                    it.isProgrammeWorkout &&
                        it.programmeId == programmeId &&
                        it.weekNumber == weekNumber &&
                        it.dayNumber == dayNumber
                }

            if (existingWorkout != null) {
                resumeWorkout(existingWorkout.id)
                onReady?.invoke()
                return
            }

            // Create workout from programme template
            // Note: dayNumber here is the sequential day (1,2,3,4), not the template day
            val workoutId =
                repository.createWorkoutFromProgrammeTemplate(
                    programmeId = programmeId,
                    weekNumber = weekNumber,
                    dayNumber = dayNumber,
                )

            // Get the created workout to extract metadata
            val workout = repository.getWorkoutById(workoutId)
            if (workout == null) {
                _workoutState.value = _workoutState.value.copy(isLoadingExercises = false)
                return
            }

            _currentWorkoutId.value = workoutId
            _workoutState.value =
                WorkoutState(
                    isActive = true,
                    status = WorkoutStatus.IN_PROGRESS,
                    workoutId = workoutId,
                    startTime = workout.date,
                    workoutName = workout.programmeWorkoutName,
                    isReadOnly = false,
                    isInEditMode = false,
                    originalWorkoutData = null,
                    // Programme context
                    isProgrammeWorkout = true,
                    programmeId = programmeId,
                    programmeName = programme.name,
                    weekNumber = weekNumber,
                    dayNumber = dayNumber,
                    programmeWorkoutName = workout.programmeWorkoutName,
                    // Ensure loading state is set
                    isLoadingExercises = true,
                    // Clear any pending celebrations from previous workouts
                    pendingPRs = emptyList(),
                    shouldShowPRCelebration = false,
                )

            // IMPORTANT: Wait for exercises to load completely before allowing navigation
            loadExercisesForWorkout(workoutId, isInitialLoad = true)
            loadInProgressWorkouts()

            // Notify that workout is ready for navigation
            onReady?.invoke()
        } catch (e: android.database.sqlite.SQLiteException) {
            CloudLogger.error(TAG, "Error starting programme workout", e)
            _workoutState.value = _workoutState.value.copy(isLoadingExercises = false)
        } catch (e: IllegalStateException) {
            CloudLogger.error(TAG, "Invalid state when starting programme workout", e)
            _workoutState.value = _workoutState.value.copy(isLoadingExercises = false)
        }
    }

    // Get next programme workout for the active programme
    suspend fun getNextProgrammeWorkout(): NextProgrammeWorkoutInfo? {
        val activeProgramme = repository.getActiveProgramme() ?: return null
        return repository.getNextProgrammeWorkout(activeProgramme.id)
    }

    private fun startRestTimer(seconds: Int) {
        restTimerJob?.cancel()
        _restTimerSeconds.value = seconds
        _restTimerInitialSeconds.value = seconds

        // Store the end time for accurate tracking even when backgrounded
        restTimerEndTime = LocalDateTime.now().plusSeconds(seconds.toLong())
        val endTime = restTimerEndTime!!

        restTimerJob =
            viewModelScope.launch {
                while (coroutineContext.isActive) {
                    val now = LocalDateTime.now()
                    if (now.isAfter(endTime) || now.isEqual(endTime)) {
                        // Timer completed
                        _restTimerSeconds.value = 0
                        restTimerEndTime = null
                        CloudLogger.debug(TAG, "Rest timer reached zero - calling notification service")
                        restTimerNotificationService.notifyTimerCompleted()
                        break
                    }

                    // Calculate actual remaining seconds based on system time
                    val remaining =
                        java.time.Duration
                            .between(now, endTime)
                            .seconds
                    _restTimerSeconds.value = remaining.toInt().coerceAtLeast(0)

                    delay(100) // Check more frequently for better accuracy when resuming
                }
                // Timer completed - vibration will be handled by UI
            }
    }

    fun skipRestTimer() {
        restTimerJob?.cancel()
        restTimerEndTime = null
        _restTimerSeconds.value = 0
        _restTimerInitialSeconds.value = 0
    }

    fun adjustRestTimer(adjustment: Int) {
        val newValue = (_restTimerSeconds.value + adjustment).coerceAtLeast(0)
        if (newValue > 0) {
            // Update the end time when adjusting
            restTimerEndTime = LocalDateTime.now().plusSeconds(newValue.toLong())
            _restTimerSeconds.value = newValue
            // Only update initialSeconds when adding time (+15s), not when subtracting (-15s)
            if (adjustment > 0) {
                _restTimerInitialSeconds.value = newValue
            }

            // Restart the timer with the new end time
            val endTime = restTimerEndTime!!
            restTimerJob?.cancel()
            restTimerJob =
                viewModelScope.launch {
                    while (coroutineContext.isActive) {
                        val now = LocalDateTime.now()
                        if (now.isAfter(endTime) || now.isEqual(endTime)) {
                            // Timer completed
                            _restTimerSeconds.value = 0
                            restTimerEndTime = null
                            restTimerNotificationService.notifyTimerCompleted()
                            break
                        }

                        // Calculate actual remaining seconds based on system time
                        val remaining =
                            java.time.Duration
                                .between(now, endTime)
                                .seconds
                        _restTimerSeconds.value = remaining.toInt().coerceAtLeast(0)

                        delay(100) // Check more frequently for better accuracy when resuming
                    }
                }
        } else {
            skipRestTimer()
        }
    }

    fun selectRestTimerPreset(seconds: Int) {
        startRestTimer(seconds)
    }

    // Resume rest timer when app returns from background
    fun resumeRestTimerIfActive() {
        val endTime = restTimerEndTime ?: return

        val now = LocalDateTime.now()
        if (now.isAfter(endTime) || now.isEqual(endTime)) {
            // Timer already expired while in background
            CloudLogger.debug("WorkoutViewModel", "Rest timer expired while app was backgrounded")
            _restTimerSeconds.value = 0
            _restTimerInitialSeconds.value = 0
            restTimerEndTime = null
            restTimerJob?.cancel()
            return
        }

        // Calculate remaining time and resume
        val remaining =
            java.time.Duration
                .between(now, endTime)
                .seconds
        _restTimerSeconds.value = remaining.toInt().coerceAtLeast(0)
        CloudLogger.debug("WorkoutViewModel", "Resuming rest timer with $remaining seconds remaining")

        // Restart the timer coroutine
        restTimerJob?.cancel()
        restTimerJob =
            viewModelScope.launch {
                while (coroutineContext.isActive) {
                    val currentTime = LocalDateTime.now()
                    if (currentTime.isAfter(endTime) || currentTime.isEqual(endTime)) {
                        // Timer completed
                        _restTimerSeconds.value = 0
                        restTimerEndTime = null
                        restTimerNotificationService.notifyTimerCompleted()
                        break
                    }

                    // Calculate actual remaining seconds based on system time
                    val remainingSeconds =
                        java.time.Duration
                            .between(currentTime, endTime)
                            .seconds
                    _restTimerSeconds.value = remainingSeconds.toInt().coerceAtLeast(0)

                    delay(100) // Check more frequently for better accuracy
                }
            }
    }

    // Workout timer functions
    private fun startWorkoutTimer() {
        val currentWorkoutId = _currentWorkoutId.value ?: return

        viewModelScope.launch {
            // Save timer start time to database
            workoutTimerStartTime = LocalDateTime.now()
            repository.updateWorkoutTimerStart(currentWorkoutId, workoutTimerStartTime!!)

            // Capture the start time in a local variable to avoid race conditions
            val startTime = workoutTimerStartTime!!

            // Start the timer coroutine
            workoutTimerJob?.cancel()
            workoutTimerJob =
                viewModelScope.launch {
                    while (coroutineContext.isActive) {
                        val elapsed =
                            java.time.Duration
                                .between(startTime, LocalDateTime.now())
                                .seconds
                        _workoutTimerSeconds.value = elapsed.toInt()
                        delay(1000)
                    }
                }
        }
    }

    private fun stopWorkoutTimer() {
        workoutTimerJob?.cancel()
        workoutTimerJob = null
    }

    private fun resumeWorkoutTimer(startTime: LocalDateTime) {
        // CRITICAL: Stop any existing timer first to prevent multiple timers
        stopWorkoutTimer()

        workoutTimerStartTime = startTime

        // Calculate elapsed time
        val elapsed =
            java.time.Duration
                .between(startTime, LocalDateTime.now())
                .seconds
        _workoutTimerSeconds.value = elapsed.toInt()

        // Capture the start time in a local variable to avoid race conditions
        val capturedStartTime = startTime

        // Resume counting
        workoutTimerJob =
            viewModelScope.launch {
                while (coroutineContext.isActive) {
                    val newElapsed =
                        java.time.Duration
                            .between(capturedStartTime, LocalDateTime.now())
                            .seconds
                    _workoutTimerSeconds.value = newElapsed.toInt()
                    delay(1000)
                }
            }
    }

    fun saveTemplateChanges() {
        viewModelScope.launch {
            val state = _workoutState.value
            if (state.mode != WorkoutMode.TEMPLATE_EDIT) return@launch

            state.templateWeekIndex ?: return@launch
            state.templateWorkoutIndex ?: return@launch

            // Convert current workout state back to ParsedWorkout
            val exercises = _selectedWorkoutExercises.value
            val sets = _selectedExerciseSets.value

            val parsedExercises =
                exercises.map { exerciseLog ->
                    val exerciseSets = sets.filter { it.exerciseLogId == exerciseLog.id }
                    com.github.radupana.featherweight.data.ParsedExercise(
                        exerciseName =
                            getExerciseNameFromMap(exerciseLog.exerciseId)
                                ?: run {
                                    CloudLogger.warn(TAG, "Exercise name not found for template: ID ${exerciseLog.exerciseId})")
                                    "Unknown Exercise"
                                },
                        matchedExerciseId = exerciseLog.exerciseId,
                        sets =
                            exerciseSets.map { setLog ->
                                com.github.radupana.featherweight.data.ParsedSet(
                                    reps = setLog.targetReps,
                                    weight = setLog.targetWeight,
                                    rpe = setLog.targetRpe ?: setLog.actualRpe, // Prioritize targetRpe for template editing
                                )
                            },
                    )
                }

            val updatedWorkout =
                com.github.radupana.featherweight.data.ParsedWorkout(
                    dayOfWeek = null, // Use null for numbered days
                    name = state.workoutName ?: "",
                    exercises = parsedExercises,
                    estimatedDurationMinutes = exercises.size * 15,
                )

            // Updated workout will be sent back via the onTemplateSaved callback
            CloudLogger.debug("WorkoutViewModel", "Template changes saved: $updatedWorkout")
        }
    }

    override fun onCleared() {
        super.onCleared()
        restTimerJob?.cancel()
        workoutTimerJob?.cancel()
    }

    // Notes methods
    fun loadWorkoutNotes(
        workoutId: String,
        callback: (String?) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                val notes = repository.getWorkoutNotes(workoutId)
                callback(notes)
            } catch (e: android.database.sqlite.SQLiteException) {
                CloudLogger.error(TAG, "Failed to load workout notes", e)
                callback(null)
            } catch (e: IllegalStateException) {
                CloudLogger.error(TAG, "Invalid state when loading workout notes", e)
                callback(null)
            }
        }
    }

    fun saveWorkoutNotes(
        workoutId: String,
        notes: String,
    ) {
        viewModelScope.launch {
            try {
                repository.updateWorkoutNotes(workoutId, notes)
            } catch (e: android.database.sqlite.SQLiteException) {
                CloudLogger.error(TAG, "Failed to save workout notes", e)
            } catch (e: IllegalStateException) {
                CloudLogger.error(TAG, "Invalid state when saving workout notes", e)
            }
        }
    }

    private fun triggerDebouncedSync() {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val authManager = ServiceLocator.getAuthenticationManager(context)

                // Trigger debounced sync if user is authenticated
                if (authManager.getCurrentUserId() != null) {
                    // Get or create SyncViewModel to trigger debounced sync
                    val syncViewModel = ServiceLocator.getSyncViewModel(context)
                    syncViewModel.triggerDebouncedSync()
                }
            } catch (e: Exception) {
                CloudLogger.error(TAG, "Failed to trigger debounced sync", e)
            }
        }
    }
}
