package com.github.radupana.featherweight.repository

import android.app.Application
import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.data.WorkoutStatus
import com.github.radupana.featherweight.di.ServiceLocator
import com.github.radupana.featherweight.domain.WorkoutSummary
import com.github.radupana.featherweight.manager.AuthenticationManager
import com.github.radupana.featherweight.util.CloudLogger
import com.github.radupana.featherweight.util.ExceptionLogger
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for managing Workout-related data
 */
class WorkoutRepository(
    application: Application,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val db: FeatherweightDatabase = FeatherweightDatabase.getDatabase(application),
    private val authManager: AuthenticationManager = ServiceLocator.provideAuthenticationManager(application),
) {
    private val workoutDao = db.workoutDao()
    private val exerciseLogDao = db.exerciseLogDao()
    private val setLogDao = db.setLogDao()
    private val programmeDao = db.programmeDao()

    companion object {
        private const val TAG = "WorkoutRepository"
    }

    suspend fun createWorkout(workout: Workout): String =
        withContext(ioDispatcher) {
            val trace = safeNewTrace("workout_creation")
            trace?.start()
            // Ensure userId is set (use "local" for unauthenticated users)
            val workoutWithUserId = workout.copy(userId = authManager.getCurrentUserId() ?: "local")
            try {
                workoutDao.insertWorkout(workoutWithUserId)
                val id = workoutWithUserId.id
                CloudLogger.info(TAG, "Created workout - id: $id, name: ${workout.name}, status: ${workout.status}, programmeId: ${workout.programmeId}, userId: ${workoutWithUserId.userId}")
                trace?.stop()
                id
            } catch (e: android.database.sqlite.SQLiteException) {
                CloudLogger.error(TAG, "Failed to create workout - name: ${workout.name}", e)
                trace?.stop()
                throw e
            }
        }

    suspend fun getWorkoutById(workoutId: String): Workout? =
        withContext(ioDispatcher) {
            workoutDao.getWorkoutById(workoutId)
        }

    suspend fun deleteWorkout(workout: Workout) =
        withContext(ioDispatcher) {
            CloudLogger.info(TAG, "Deleting workout - id: ${workout.id}, name: ${workout.name}")
            try {
                val exerciseLogs = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
                exerciseLogs.forEach { exerciseLog ->
                    val setLogs = setLogDao.getSetLogsForExercise(exerciseLog.id)
                    setLogs.forEach { setLogDao.deleteSetLog(it.id) }
                    exerciseLogDao.deleteExerciseLog(exerciseLog.id)
                }
                workoutDao.deleteWorkout(workout.id)
                CloudLogger.info(TAG, "Workout deleted - id: ${workout.id}, had ${exerciseLogs.size} exercises")
            } catch (e: android.database.sqlite.SQLiteException) {
                CloudLogger.error(TAG, "Failed to delete workout - id: ${workout.id}, name: ${workout.name}", e)
                throw e
            }
        }

    suspend fun deleteWorkoutById(workoutId: String) =
        withContext(ioDispatcher) {
            val workout = workoutDao.getWorkoutById(workoutId)
            if (workout != null) {
                deleteWorkout(workout)
            }
        }

    suspend fun updateWorkoutStatus(
        workoutId: String,
        status: WorkoutStatus,
    ) = withContext(ioDispatcher) {
        val workout = workoutDao.getWorkoutById(workoutId) ?: return@withContext
        val oldStatus = workout.status
        workoutDao.updateWorkout(workout.copy(status = status))
        CloudLogger.info(TAG, "Updated workout status - id: $workoutId, from: $oldStatus to: $status")
    }

    suspend fun completeWorkout(
        workoutId: String,
        duration: Long? = null,
    ) = withContext(ioDispatcher) {
        val trace = safeNewTrace("workout_completion")
        trace?.start()

        val workout = workoutDao.getWorkoutById(workoutId) ?: return@withContext
        workoutDao.updateWorkout(
            workout.copy(
                status = WorkoutStatus.COMPLETED,
                durationSeconds = duration?.toString(),
            ),
        )

        val exerciseCount = exerciseLogDao.getExerciseLogsForWorkout(workoutId).size
        val setCount =
            exerciseLogDao.getExerciseLogsForWorkout(workoutId).sumOf {
                setLogDao.getSetLogsForExercise(it.id).size
            }

        trace?.putAttribute("exercise_count", exerciseCount.toString())
        trace?.putAttribute("set_count", setCount.toString())
        trace?.putMetric("duration_seconds", duration ?: 0)

        CloudLogger.info(
            TAG,
            "Workout completed - id: $workoutId, name: ${workout.name ?: "Unnamed"}, " +
                "duration: ${duration ?: 0}s, exercises: $exerciseCount, sets: $setCount, " +
                "programmeId: ${workout.programmeId ?: "none"}",
        )

        trace?.stop()
    }

    // Suppress TooGenericExceptionCaught: This is a safe wrapper that must handle ALL exceptions
    // from Firebase Performance initialization, including RuntimeException from unmocked Android
    // methods in test environments. The method is explicitly designed to never throw.
    @Suppress("TooGenericExceptionCaught")
    private fun safeNewTrace(name: String): Trace? =
        try {
            FirebasePerformance.getInstance().newTrace(name)
        } catch (e: Throwable) {
            ExceptionLogger.logNonCritical(TAG, "Firebase Performance not available: ${e.javaClass.simpleName}", e)
            null
        }

    suspend fun getExerciseLogsForWorkout(workoutId: String): List<ExerciseLog> =
        withContext(ioDispatcher) {
            exerciseLogDao.getExerciseLogsForWorkout(workoutId)
        }

    suspend fun getSetLogsForExercise(exerciseLogId: String): List<SetLog> =
        withContext(ioDispatcher) {
            setLogDao.getSetLogsForExercise(exerciseLogId)
        }

    suspend fun getWorkoutHistory(): List<WorkoutSummary> =
        withContext(ioDispatcher) {
            val startTime = System.currentTimeMillis()
            val userId = authManager.getCurrentUserId() ?: "local"
            val allWorkouts = workoutDao.getWorkoutsByUserId(userId)
            val result =
                allWorkouts
                    .mapNotNull { workout ->
                        val exercises = exerciseLogDao.getExerciseLogsForWorkout(workout.id)

                        if (workout.status != WorkoutStatus.COMPLETED && exercises.isEmpty()) return@mapNotNull null

                        val allSets = mutableListOf<SetLog>()
                        exercises.forEach { exercise ->
                            allSets.addAll(setLogDao.getSetLogsForExercise(exercise.id))
                        }

                        val completedSets = allSets.filter { it.isCompleted }
                        val totalWeight = completedSets.sumOf { (it.actualWeight * it.actualReps).toDouble() }.toFloat()

                        val programmeName =
                            if (workout.isProgrammeWorkout && workout.programmeId != null) {
                                try {
                                    programmeDao.getProgrammeById(workout.programmeId)?.name
                                } catch (e: android.database.sqlite.SQLiteException) {
                                    CloudLogger.error(TAG, "Failed to get programme name for programmeId: ${workout.programmeId}", e)
                                    null
                                }
                            } else {
                                null
                            }

                        WorkoutSummary(
                            id = workout.id,
                            date = workout.date,
                            name = workout.name ?: workout.programmeWorkoutName,
                            exerciseCount = exercises.size,
                            setCount = allSets.size,
                            totalWeight = totalWeight,
                            duration = workout.durationSeconds?.toLongOrNull(),
                            status = workout.status,
                            hasNotes = !workout.notes.isNullOrBlank(),
                            isProgrammeWorkout = workout.isProgrammeWorkout,
                            programmeId = workout.programmeId,
                            programmeName = programmeName,
                            programmeWorkoutName = workout.programmeWorkoutName,
                            weekNumber = workout.weekNumber,
                            dayNumber = workout.dayNumber,
                        )
                    }.sortedByDescending { it.date }

            CloudLogger.debug(
                TAG,
                "getWorkoutHistory took ${System.currentTimeMillis() - startTime}ms - " +
                    "total workouts: ${allWorkouts.size}, summaries: ${result.size}, " +
                    "completed: ${result.count { it.status == WorkoutStatus.COMPLETED }}",
            )
            result
        }
}
