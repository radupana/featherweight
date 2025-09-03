package com.github.radupana.featherweight.repository

import android.app.Application
import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.data.WorkoutStatus
import com.github.radupana.featherweight.domain.WorkoutSummary
import com.github.radupana.featherweight.logging.BugfenderLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Repository for managing Workout-related data
 */
class WorkoutRepository(
    application: Application,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val db = FeatherweightDatabase.getDatabase(application)
    private val workoutDao = db.workoutDao()
    private val exerciseLogDao = db.exerciseLogDao()
    private val setLogDao = db.setLogDao()
    private val programmeDao = db.programmeDao()

    companion object {
        private const val TAG = "WorkoutRepository"
    }

    suspend fun createWorkout(workout: Workout): Long =
        withContext(ioDispatcher) {
            val id = workoutDao.insertWorkout(workout)
            BugfenderLogger.i(
                TAG,
                "Created workout - id: $id, name: ${workout.name}, status: ${workout.status}, programmeId: ${workout.programmeId}",
            )
            id
        }

    suspend fun getWorkoutById(workoutId: Long): Workout? =
        withContext(ioDispatcher) {
            workoutDao.getWorkoutById(workoutId)
        }

    suspend fun updateWorkout(workout: Workout) =
        withContext(ioDispatcher) {
            workoutDao.updateWorkout(workout)
        }

    suspend fun deleteWorkout(workout: Workout) =
        withContext(ioDispatcher) {
            BugfenderLogger.i(TAG, "Deleting workout - id: ${workout.id}, name: ${workout.name}")
            val exerciseLogs = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
            exerciseLogs.forEach { exerciseLog ->
                val setLogs = setLogDao.getSetLogsForExercise(exerciseLog.id)
                setLogs.forEach { setLogDao.deleteSetLog(it.id) }
                exerciseLogDao.deleteExerciseLog(exerciseLog.id)
            }
            workoutDao.deleteWorkout(workout.id)
            BugfenderLogger.i(TAG, "Workout deleted - id: ${workout.id}, had ${exerciseLogs.size} exercises")
        }

    suspend fun deleteWorkoutById(workoutId: Long) =
        withContext(ioDispatcher) {
            val workout = workoutDao.getWorkoutById(workoutId)
            if (workout != null) {
                deleteWorkout(workout)
            }
        }

    suspend fun updateWorkoutStatus(
        workoutId: Long,
        status: WorkoutStatus,
    ) = withContext(ioDispatcher) {
        val workout = workoutDao.getWorkoutById(workoutId) ?: return@withContext
        val oldStatus = workout.status
        workoutDao.updateWorkout(workout.copy(status = status))
        BugfenderLogger.i(TAG, "Updated workout status - id: $workoutId, from: $oldStatus to: $status")
    }

    suspend fun completeWorkout(
        workoutId: Long,
        duration: Long? = null,
    ) = withContext(ioDispatcher) {
        val workout = workoutDao.getWorkoutById(workoutId) ?: return@withContext
        workoutDao.updateWorkout(
            workout.copy(
                status = WorkoutStatus.COMPLETED,
                durationSeconds = duration,
            ),
        )

        val exerciseCount = exerciseLogDao.getExerciseLogsForWorkout(workoutId).size
        val setCount =
            exerciseLogDao.getExerciseLogsForWorkout(workoutId).sumOf {
                setLogDao.getSetLogsForExercise(it.id).size
            }

        BugfenderLogger.logUserAction(
            "workout_completed",
            mapOf(
                "workoutId" to workoutId,
                "name" to (workout.name ?: "Unnamed"),
                "duration_seconds" to (duration ?: 0),
                "exercises" to exerciseCount,
                "sets" to setCount,
                "programmeId" to (workout.programmeId ?: "none"),
            ),
        )
    }

    suspend fun abandonWorkout(workoutId: Long) =
        withContext(ioDispatcher) {
            val workout = workoutDao.getWorkoutById(workoutId) ?: return@withContext
            workoutDao.updateWorkout(
                workout.copy(
                    status = WorkoutStatus.NOT_STARTED,
                ),
            )
        }

    suspend fun getAllWorkouts(): List<Workout> =
        withContext(ioDispatcher) {
            workoutDao.getAllWorkouts()
        }

    suspend fun getActiveWorkout(): Workout? =
        withContext(ioDispatcher) {
            workoutDao.getAllWorkouts().firstOrNull { it.status == WorkoutStatus.IN_PROGRESS }
        }

    suspend fun getInProgressWorkouts(): List<Workout> =
        withContext(ioDispatcher) {
            workoutDao.getAllWorkouts().filter { it.status == WorkoutStatus.IN_PROGRESS }
        }

    suspend fun getWorkoutsForDateRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<Workout> =
        withContext(ioDispatcher) {
            val startTime = System.currentTimeMillis()
            val startDateTime = startDate.atStartOfDay()
            val endDateTime = endDate.plusDays(1).atStartOfDay()
            val workouts = workoutDao.getWorkoutsInDateRange(startDateTime, endDateTime)

            BugfenderLogger.logPerformance(
                TAG,
                "getWorkoutsForDateRange",
                System.currentTimeMillis() - startTime,
                mapOf(
                    "startDate" to startDate.toString(),
                    "endDate" to endDate.toString(),
                    "workoutsFound" to workouts.size,
                    "completedCount" to workouts.count { it.status == WorkoutStatus.COMPLETED },
                ),
            )
            workouts
        }

    suspend fun getExerciseLogsForWorkout(workoutId: Long): List<ExerciseLog> =
        withContext(ioDispatcher) {
            exerciseLogDao.getExerciseLogsForWorkout(workoutId)
        }

    suspend fun getSetLogsForExercise(exerciseLogId: Long): List<SetLog> =
        withContext(ioDispatcher) {
            setLogDao.getSetLogsForExercise(exerciseLogId)
        }

    suspend fun getWorkoutHistory(): List<WorkoutSummary> =
        withContext(ioDispatcher) {
            val startTime = System.currentTimeMillis()
            val allWorkouts = workoutDao.getAllWorkouts()
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
                                    BugfenderLogger.e(TAG, "Failed to get programme name for programmeId: ${workout.programmeId}", e)
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
                            duration = workout.durationSeconds,
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

            BugfenderLogger.logPerformance(
                TAG,
                "getWorkoutHistory",
                System.currentTimeMillis() - startTime,
                mapOf(
                    "totalWorkouts" to allWorkouts.size,
                    "summariesReturned" to result.size,
                    "completedCount" to result.count { it.status == WorkoutStatus.COMPLETED },
                ),
            )
            result
        }
}
