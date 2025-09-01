package com.github.radupana.featherweight.repository

import android.app.Application
import android.util.Log
import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.data.WorkoutStatus
import com.github.radupana.featherweight.domain.WorkoutSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Repository for managing Workout-related data
 */
class WorkoutRepository(application: Application) {
    private val db = FeatherweightDatabase.getDatabase(application)
    private val workoutDao = db.workoutDao()
    private val exerciseLogDao = db.exerciseLogDao()
    private val setLogDao = db.setLogDao()
    private val programmeDao = db.programmeDao()
    
    companion object {
        private const val TAG = "WorkoutRepository"
    }
    
    // Basic Workout CRUD operations
    suspend fun createWorkout(workout: Workout): Long = 
        withContext(Dispatchers.IO) {
            workoutDao.insertWorkout(workout)
        }
    
    suspend fun getWorkoutById(workoutId: Long): Workout? = 
        withContext(Dispatchers.IO) {
            workoutDao.getWorkoutById(workoutId)
        }
    
    suspend fun updateWorkout(workout: Workout) = 
        withContext(Dispatchers.IO) {
            workoutDao.updateWorkout(workout)
        }
    
    suspend fun deleteWorkout(workout: Workout) = 
        withContext(Dispatchers.IO) {
            // Delete all exercise logs for this workout
            val exerciseLogs = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
            exerciseLogs.forEach { exerciseLog ->
                // Delete all set logs for this exercise
                val setLogs = setLogDao.getSetLogsForExercise(exerciseLog.id)
                setLogs.forEach { setLogDao.deleteSetLog(it.id) }
                // Delete the exercise log
                exerciseLogDao.deleteExerciseLog(exerciseLog.id)
            }
            // Finally delete the workout itself
            workoutDao.deleteWorkout(workout.id)
        }
    
    suspend fun deleteWorkoutById(workoutId: Long) = 
        withContext(Dispatchers.IO) {
            val workout = workoutDao.getWorkoutById(workoutId)
            if (workout != null) {
                deleteWorkout(workout)
            }
        }
    
    // Workout status management
    suspend fun updateWorkoutStatus(workoutId: Long, status: WorkoutStatus) = 
        withContext(Dispatchers.IO) {
            val workout = workoutDao.getWorkoutById(workoutId) ?: return@withContext
            workoutDao.updateWorkout(workout.copy(status = status))
        }
    
    suspend fun completeWorkout(workoutId: Long, duration: Long? = null) = 
        withContext(Dispatchers.IO) {
            val workout = workoutDao.getWorkoutById(workoutId) ?: return@withContext
            workoutDao.updateWorkout(
                workout.copy(
                    status = WorkoutStatus.COMPLETED,
                    durationSeconds = duration
                )
            )
        }
    
    suspend fun abandonWorkout(workoutId: Long) = 
        withContext(Dispatchers.IO) {
            val workout = workoutDao.getWorkoutById(workoutId) ?: return@withContext
            workoutDao.updateWorkout(
                workout.copy(
                    status = WorkoutStatus.NOT_STARTED
                )
            )
        }
    
    // Workout queries
    suspend fun getAllWorkouts(): List<Workout> = 
        withContext(Dispatchers.IO) {
            workoutDao.getAllWorkouts()
        }
    
    suspend fun getActiveWorkout(): Workout? = 
        withContext(Dispatchers.IO) {
            workoutDao.getAllWorkouts().firstOrNull { it.status == WorkoutStatus.IN_PROGRESS }
        }
    
    suspend fun getInProgressWorkouts(): List<Workout> = 
        withContext(Dispatchers.IO) {
            workoutDao.getAllWorkouts().filter { it.status == WorkoutStatus.IN_PROGRESS }
        }
    
    suspend fun getWorkoutsForDateRange(startDate: LocalDate, endDate: LocalDate): List<Workout> = 
        withContext(Dispatchers.IO) {
            val startDateTime = startDate.atStartOfDay()
            val endDateTime = endDate.plusDays(1).atStartOfDay()
            workoutDao.getWorkoutsInDateRange(startDateTime, endDateTime)
        }
    
    suspend fun getCompletedWorkoutsCount(): Int = 
        withContext(Dispatchers.IO) {
            workoutDao.getAllWorkouts().count { it.status == WorkoutStatus.COMPLETED }
        }
    
    suspend fun getLastWorkout(): Workout? = 
        withContext(Dispatchers.IO) {
            workoutDao.getAllWorkouts().maxByOrNull { it.date }
        }
    
    suspend fun getRecentWorkouts(limit: Int = 10): List<Workout> = 
        withContext(Dispatchers.IO) {
            workoutDao.getAllWorkouts().sortedByDescending { it.date }.take(limit)
        }
    
    // Exercise logs management
    suspend fun createExerciseLog(exerciseLog: ExerciseLog): Long = 
        withContext(Dispatchers.IO) {
            exerciseLogDao.insertExerciseLog(exerciseLog)
        }
    
    suspend fun updateExerciseLog(exerciseLog: ExerciseLog) = 
        withContext(Dispatchers.IO) {
            exerciseLogDao.update(exerciseLog)
        }
    
    suspend fun deleteExerciseLog(exerciseLog: ExerciseLog) = 
        withContext(Dispatchers.IO) {
            // Delete all set logs first
            val setLogs = setLogDao.getSetLogsForExercise(exerciseLog.id)
            setLogs.forEach { setLogDao.deleteSetLog(it.id) }
            // Then delete the exercise log
            exerciseLogDao.deleteExerciseLog(exerciseLog.id)
        }
    
    suspend fun getExerciseLogsForWorkout(workoutId: Long): List<ExerciseLog> = 
        withContext(Dispatchers.IO) {
            exerciseLogDao.getExerciseLogsForWorkout(workoutId)
        }
    
    // Set logs management
    suspend fun createSetLog(setLog: SetLog): Long = 
        withContext(Dispatchers.IO) {
            setLogDao.insertSetLog(setLog)
        }
    
    suspend fun updateSetLog(setLog: SetLog) = 
        withContext(Dispatchers.IO) {
            setLogDao.updateSetLog(setLog)
        }
    
    suspend fun deleteSetLog(setLog: SetLog) = 
        withContext(Dispatchers.IO) {
            setLogDao.deleteSetLog(setLog.id)
        }
    
    suspend fun getSetLogsForExercise(exerciseLogId: Long): List<SetLog> = 
        withContext(Dispatchers.IO) {
            setLogDao.getSetLogsForExercise(exerciseLogId)
        }
    
    suspend fun getCompletedSetsCount(exerciseLogId: Long): Int = 
        withContext(Dispatchers.IO) {
            setLogDao.getSetLogsForExercise(exerciseLogId).count { it.isCompleted }
        }
    
    // Workout volume calculations
    suspend fun calculateWorkoutVolume(workoutId: Long): Float = 
        withContext(Dispatchers.IO) {
            val exerciseLogs = exerciseLogDao.getExerciseLogsForWorkout(workoutId)
            var totalVolume = 0f
            
            for (exerciseLog in exerciseLogs) {
                val sets = setLogDao.getSetLogsForExercise(exerciseLog.id)
                for (set in sets.filter { it.isCompleted }) {
                    totalVolume += (set.actualWeight * set.actualReps)
                }
            }
            
            totalVolume
        }
    
    suspend fun getWorkoutExerciseCount(workoutId: Long): Int = 
        withContext(Dispatchers.IO) {
            exerciseLogDao.getExerciseLogsForWorkout(workoutId).size
        }
    
    suspend fun getWorkoutSetCount(workoutId: Long): Int = 
        withContext(Dispatchers.IO) {
            val exerciseLogs = exerciseLogDao.getExerciseLogsForWorkout(workoutId)
            var totalSets = 0
            for (exerciseLog in exerciseLogs) {
                totalSets += setLogDao.getSetLogsForExercise(exerciseLog.id).size
            }
            totalSets
        }
    
    // Get workout history with summaries
    suspend fun getWorkoutHistory(): List<WorkoutSummary> = 
        withContext(Dispatchers.IO) {
            val allWorkouts = workoutDao.getAllWorkouts()
            allWorkouts.mapNotNull { workout ->
                val exercises = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
                
                // Include completed workouts even if they have no exercises
                if (workout.status != WorkoutStatus.COMPLETED && exercises.isEmpty()) return@mapNotNull null
                
                val allSets = mutableListOf<SetLog>()
                exercises.forEach { exercise ->
                    allSets.addAll(setLogDao.getSetLogsForExercise(exercise.id))
                }
                
                val completedSets = allSets.filter { it.isCompleted }
                val totalWeight = completedSets.sumOf { (it.actualWeight * it.actualReps).toDouble() }.toFloat()
                
                // Get programme information if this is a programme workout
                val programmeName = if (workout.isProgrammeWorkout && workout.programmeId != null) {
                    try {
                        programmeDao.getProgrammeById(workout.programmeId)?.name
                    } catch (e: android.database.sqlite.SQLiteException) {
                        Log.e(TAG, "Failed to get programme name for programmeId: ${workout.programmeId}", e)
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
                    dayNumber = workout.dayNumber
                )
            }.sortedByDescending { it.date }
        }
}
