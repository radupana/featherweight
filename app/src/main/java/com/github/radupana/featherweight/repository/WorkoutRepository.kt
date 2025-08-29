package com.github.radupana.featherweight.repository

import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.data.WorkoutStatus
import com.github.radupana.featherweight.data.ExerciseLogDao
import com.github.radupana.featherweight.data.SetLogDao
import com.github.radupana.featherweight.data.WorkoutDao
import com.github.radupana.featherweight.domain.ExerciseHistory
import com.github.radupana.featherweight.service.GlobalProgressTracker
import com.github.radupana.featherweight.service.PRDetectionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Repository for managing workout-related database operations.
 * Handles workouts, their lifecycle, and related statistics.
 */
class WorkoutRepository(
    private val db: FeatherweightDatabase,
    private val workoutDao: WorkoutDao,
    private val exerciseLogDao: ExerciseLogDao,
    private val setLogDao: SetLogDao,
    private val prDetectionService: PRDetectionService,
    private val globalProgressTracker: GlobalProgressTracker,
) {
    /**
     * Creates a new workout
     */
    suspend fun insertWorkout(workout: Workout): Long = workoutDao.insertWorkout(workout)

    /**
     * Updates an existing workout
     */
    suspend fun updateWorkout(workout: Workout) = workoutDao.updateWorkout(workout)

    /**
     * Gets a workout by ID
     */
    suspend fun getWorkoutById(workoutId: Long): Workout? = workoutDao.getWorkoutById(workoutId)

    /**
     * Gets the currently ongoing workout (IN_PROGRESS status)
     */
    suspend fun getOngoingWorkout(): Workout? {
        val workouts = // Get workouts that are IN_PROGRESS
        workoutDao.getAllWorkouts().filter { it.status == WorkoutStatus.IN_PROGRESS }
        return workouts.firstOrNull()
    }

    /**
     * Deletes a workout and all associated data
     */
    suspend fun deleteWorkout(workoutId: Long) = workoutDao.deleteWorkout(workoutId)

    /**
     * Gets all exercises for a specific workout
     */
    suspend fun getExerciseLogsForWorkout(workoutId: Long): List<ExerciseLog> = 
        exerciseLogDao.getExerciseLogsForWorkout(workoutId)

    /**
     * Gets all sets for a specific workout
     */
    suspend fun getSetsForWorkout(workoutId: Long): List<SetLog> =
        withContext(Dispatchers.IO) {
            val exerciseLogs = exerciseLogDao.getExerciseLogsForWorkout(workoutId)
            exerciseLogs.flatMap { exerciseLog ->
                setLogDao.getSetLogsForExercise(exerciseLog.id)
            }
        }

    /**
     * Completes a workout, updating its status and calculating statistics
     */
    suspend fun completeWorkout(
        workoutId: Long,
        notes: String? = null,
        @Suppress("UNUSED_PARAMETER") isProgrammeWorkout: Boolean = false,
    ) {
        withContext(Dispatchers.IO) {
            val workout = workoutDao.getWorkoutById(workoutId) ?: return@withContext

            // Calculate workout duration
            val durationSeconds = workout.timerStartTime?.let {
                Duration.between(it, LocalDateTime.now()).seconds
            } ?: workout.timerElapsedSeconds.toLong()

            // Update workout status
            val updatedWorkout = workout.copy(
                status = WorkoutStatus.COMPLETED,
                notes = notes,
                notesUpdatedAt = notes?.let { LocalDateTime.now() },
                durationSeconds = durationSeconds,
            )
            workoutDao.updateWorkout(updatedWorkout)

            // Process personal records
            val exerciseLogs = exerciseLogDao.getExerciseLogsForWorkout(workoutId)
            for (exerciseLog in exerciseLogs) {
                val sets = setLogDao.getSetLogsForExercise(exerciseLog.id)
                    .filter { it.isCompleted }

                if (sets.isNotEmpty()) {
                    // Check for PRs
                    for (set in sets) {
                        prDetectionService.checkForPR(
                            setLog = set,
                            exerciseVariationId = exerciseLog.exerciseVariationId,
                        )
                    }
                }
            }

            // Update global progress after workout
            globalProgressTracker.updateProgressAfterWorkout(workoutId)
        }
    }

    /**
     * Gets workout history with summary statistics
     */
    suspend fun getWorkoutHistory(): List<WorkoutSummary> {
        return withContext(Dispatchers.IO) {
            val workouts = workoutDao.getAllWorkouts()
            workouts.map { workout ->
                val exerciseLogs = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
                val setCount = exerciseLogs.sumOf { exerciseLog ->
                    setLogDao.getSetLogsForExercise(exerciseLog.id).size
                }
                val totalWeight = exerciseLogs.sumOf { exerciseLog ->
                    setLogDao.getSetLogsForExercise(exerciseLog.id)
                        .filter { it.isCompleted }
                        .sumOf { set -> (set.actualWeight * set.actualReps).toDouble() }
                }.toFloat()

                WorkoutSummary(
                    id = workout.id,
                    date = workout.date,
                    name = workout.name ?: workout.programmeWorkoutName,
                    exerciseCount = exerciseLogs.size,
                    setCount = setCount,
                    totalWeight = totalWeight,
                    status = workout.status,
                    isProgrammeWorkout = workout.isProgrammeWorkout,
                    duration = workout.durationSeconds,
                )
            }
        }
    }

    /**
     * Gets exercise history for a specific exercise variation
     */
    suspend fun getExerciseHistory(
        exerciseVariationId: Long,
        currentWorkoutId: Long,
    ): ExerciseHistory? {
        return withContext(Dispatchers.IO) {
            val allWorkouts = workoutDao.getAllWorkouts()
                .filter { it.status == WorkoutStatus.COMPLETED }
                .sortedByDescending { it.date }

            for (workout in allWorkouts) {
                if (workout.id == currentWorkoutId) continue

                val exercises = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
                val matchingExercise = exercises.find { it.exerciseVariationId == exerciseVariationId }

                if (matchingExercise != null) {
                    val sets = setLogDao.getSetLogsForExercise(matchingExercise.id)
                        .filter { it.isCompleted }

                    if (sets.isNotEmpty()) {
                        return@withContext ExerciseHistory(
                            exerciseVariationId = exerciseVariationId,
                            lastWorkoutDate = workout.date,
                            sets = sets,
                        )
                    }
                }
            }
            null
        }
    }

    /**
     * Gets workouts filtered by various criteria
     */
    suspend fun getFilteredWorkouts(filters: WorkoutFilters): List<WorkoutSummary> {
        return withContext(Dispatchers.IO) {
            var workouts = workoutDao.getAllWorkouts()

            // Apply date range filter
            filters.dateRange?.let { (startDate, endDate) ->
                workouts = workouts.filter { workout ->
                    val workoutDate = workout.date.toLocalDate()
                    workoutDate >= startDate && workoutDate <= endDate
                }
            }

            // Apply programme filter
            filters.programmeId?.let { programmeId ->
                workouts = workouts.filter { it.programmeId == programmeId }
            }

            // Apply exercise and muscle group filters (requires loading exercise data)
            if (filters.exercises.isNotEmpty() || filters.muscleGroups.isNotEmpty()) {
                workouts = workouts.filter { workout ->
                    val exerciseLogs = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
                    
                    val hasExercise = if (filters.exercises.isNotEmpty()) {
                        exerciseLogs.any { log ->
                            val exercise = db.exerciseDao().getExerciseVariationById(log.exerciseVariationId)
                            exercise?.name in filters.exercises
                        }
                    } else true

                    val hasMuscleGroup = if (filters.muscleGroups.isNotEmpty()) {
                        exerciseLogs.any { log ->
                            val muscles = db.variationMuscleDao().getMusclesForVariation(log.exerciseVariationId)
                            muscles.any { it.muscle.displayName in filters.muscleGroups }
                        }
                    } else true

                    hasExercise && hasMuscleGroup
                }
            }

            // Convert to WorkoutSummary
            workouts.map { workout ->
                val exerciseLogs = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
                val setCount = exerciseLogs.sumOf { exerciseLog ->
                    setLogDao.getSetLogsForExercise(exerciseLog.id).size
                }
                val totalWeight = exerciseLogs.sumOf { exerciseLog ->
                    setLogDao.getSetLogsForExercise(exerciseLog.id)
                        .filter { it.isCompleted }
                        .sumOf { set -> (set.actualWeight * set.actualReps).toDouble() }
                }.toFloat()

                WorkoutSummary(
                    id = workout.id,
                    date = workout.date,
                    name = workout.name ?: workout.programmeWorkoutName,
                    exerciseCount = exerciseLogs.size,
                    setCount = setCount,
                    totalWeight = totalWeight,
                    status = workout.status,
                    isProgrammeWorkout = workout.isProgrammeWorkout,
                    duration = workout.durationSeconds,
                )
            }
        }
    }

    /**
     * Gets workout statistics for a specific date
     */
    suspend fun getWorkoutDayInfo(date: LocalDate): WorkoutDayInfo {
        return withContext(Dispatchers.IO) {
            val dayWorkouts = workoutDao.getWorkoutsInDateRange(
                startDate = date.atStartOfDay(),
                endDate = date.plusDays(1).atStartOfDay(),
            )

            WorkoutDayInfo(
                completedCount = dayWorkouts.count { it.status == WorkoutStatus.COMPLETED },
                inProgressCount = dayWorkouts.count { it.status == WorkoutStatus.IN_PROGRESS },
                notStartedCount = dayWorkouts.count { it.status == WorkoutStatus.NOT_STARTED },
            )
        }
    }

    /**
     * Gets recent workouts
     */
    // Note: This would need a proper Flow implementation in the DAO
    // For now, returning empty flow
    fun getRecentWorkoutsFlow(@Suppress("UNUSED_PARAMETER") limit: Int = 10): Flow<List<Workout>> = 
        kotlinx.coroutines.flow.flowOf(emptyList())

    /**
     * Gets total volume for a time period
     */
    suspend fun getWeeklyVolumeTotal(startDate: LocalDateTime): Float =
        withContext(Dispatchers.IO) {
            val endDate = startDate.plusWeeks(1)
            val workouts = workoutDao.getWorkoutsInDateRange(startDate, endDate)
            
            workouts.sumOf { workout ->
                val exerciseLogs = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
                exerciseLogs.sumOf { exerciseLog ->
                    setLogDao.getSetLogsForExercise(exerciseLog.id)
                        .filter { it.isCompleted }
                        .sumOf { set -> (set.actualWeight * set.actualReps).toDouble() }
                }
            }.toFloat()
        }

    /**
     * Gets total volume for a month
     */
    suspend fun getMonthlyVolumeTotal(startDate: LocalDateTime): Float =
        withContext(Dispatchers.IO) {
            val endDate = startDate.plusMonths(1)
            val workouts = workoutDao.getWorkoutsInDateRange(startDate, endDate)
            
            workouts.sumOf { workout ->
                val exerciseLogs = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
                exerciseLogs.sumOf { exerciseLog ->
                    setLogDao.getSetLogsForExercise(exerciseLog.id)
                        .filter { it.isCompleted }
                        .sumOf { set -> (set.actualWeight * set.actualReps).toDouble() }
                }
            }.toFloat()
        }

    /**
     * Gets training frequency for a period
     */
    suspend fun getTrainingFrequency(startDate: LocalDateTime, endDate: LocalDateTime): Int =
        withContext(Dispatchers.IO) {
            workoutDao.getWorkoutsInDateRange(startDate, endDate)
                .count { it.status == WorkoutStatus.COMPLETED }
        }

    /**
     * Updates workout timer information
     */
    suspend fun updateWorkoutTimer(workoutId: Long, elapsedSeconds: Int) {
        withContext(Dispatchers.IO) {
            val workout = workoutDao.getWorkoutById(workoutId) ?: return@withContext
            val updatedWorkout = workout.copy(
                timerElapsedSeconds = elapsedSeconds,
                timerStartTime = workout.timerStartTime ?: LocalDateTime.now(),
            )
            workoutDao.updateWorkout(updatedWorkout)
        }
    }

    /**
     * Starts a new workout session
     */
    suspend fun startWorkout(
        name: String? = null,
        programmeId: Long? = null,
        weekNumber: Int? = null,
        dayNumber: Int? = null,
        programmeWorkoutName: String? = null,
    ): Long {
        val workout = Workout(
            date = LocalDateTime.now(),
            name = name,
            programmeId = programmeId,
            weekNumber = weekNumber,
            dayNumber = dayNumber,
            programmeWorkoutName = programmeWorkoutName,
            isProgrammeWorkout = programmeId != null,
            status = WorkoutStatus.NOT_STARTED,
        )
        return insertWorkout(workout)
    }

    /**
     * Marks a workout as in progress
     */
    suspend fun markWorkoutInProgress(workoutId: Long) {
        withContext(Dispatchers.IO) {
            val workout = workoutDao.getWorkoutById(workoutId) ?: return@withContext
            if (workout.status == WorkoutStatus.NOT_STARTED) {
                val updatedWorkout = workout.copy(
                    status = WorkoutStatus.IN_PROGRESS,
                    timerStartTime = LocalDateTime.now(),
                )
                workoutDao.updateWorkout(updatedWorkout)
            }
        }
    }
}
