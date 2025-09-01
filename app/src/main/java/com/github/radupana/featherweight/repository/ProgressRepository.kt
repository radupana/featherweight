package com.github.radupana.featherweight.repository

import android.app.Application
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.GlobalExerciseProgress
import com.github.radupana.featherweight.data.PendingOneRMUpdate
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.SwapHistoryCount
import com.github.radupana.featherweight.data.TrainingAnalysis
import com.github.radupana.featherweight.data.exercise.ExerciseVariation
import com.github.radupana.featherweight.domain.ExerciseHistory
import com.github.radupana.featherweight.domain.ExerciseStats
import com.github.radupana.featherweight.service.GlobalProgressTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/**
 * Repository for managing progress tracking and analytics
 */
class ProgressRepository(application: Application) {
    private val db = FeatherweightDatabase.getDatabase(application)
    private val globalExerciseProgressDao = db.globalExerciseProgressDao()
    private val setLogDao = db.setLogDao()
    private val exerciseLogDao = db.exerciseLogDao()
    private val workoutDao = db.workoutDao()
    private val exerciseVariationDao = db.exerciseVariationDao()
    
    // Global progress management
    suspend fun updateProgressAfterWorkout(workoutId: Long): List<PendingOneRMUpdate> = 
        withContext(Dispatchers.IO) {
            emptyList()
        }
    
    suspend fun getGlobalExerciseProgress(
        exerciseVariationId: Long,
        lookbackDays: Int = 30
    ): GlobalExerciseProgress? = 
        withContext(Dispatchers.IO) {
            globalExerciseProgressDao.getProgressForExercise(exerciseVariationId)
        }
    
    suspend fun updateGlobalExerciseProgress(progress: GlobalExerciseProgress) = 
        withContext(Dispatchers.IO) {
            globalExerciseProgressDao.insertOrUpdate(progress)
        }
    
    suspend fun getAllGlobalProgress(): List<GlobalExerciseProgress> = 
        withContext(Dispatchers.IO) {
            // Get all unique exercise variation IDs
            val exerciseIds = exerciseLogDao.getAllUniqueExerciseVariationIds()
            exerciseIds.mapNotNull { id ->
                globalExerciseProgressDao.getProgressForExercise(id)
            }
        }
    
    // Exercise history and stats
    suspend fun getExerciseHistory(
        exerciseVariationId: Long,
        limit: Int = 10
    ): ExerciseHistory = 
        withContext(Dispatchers.IO) {
            // Get recent sets
            val recentSets = setLogDao.getSetLogsForExercise(exerciseVariationId)
                .sortedByDescending { it.completedAt }
                .take(limit)
            
            val lastWorkoutDate = recentSets.firstOrNull()?.completedAt?.let {
                LocalDateTime.parse(it)
            } ?: LocalDateTime.now()
            
            ExerciseHistory(
                exerciseVariationId = exerciseVariationId,
                lastWorkoutDate = lastWorkoutDate,
                sets = recentSets
            )
        }
    
    suspend fun getExerciseStats(exerciseVariationId: Long): ExerciseStats = 
        withContext(Dispatchers.IO) {
            val exercise = exerciseVariationDao.getExerciseVariationById(exerciseVariationId)
            val sets = setLogDao.getSetLogsForExercise(exerciseVariationId)
                .filter { it.isCompleted }
            
            if (sets.isEmpty()) {
                return@withContext ExerciseStats(
                    exerciseName = exercise?.name ?: "Unknown",
                    avgWeight = 0f,
                    avgReps = 0,
                    avgRpe = null,
                    maxWeight = 0f,
                    totalSets = 0
                )
            }
            
            ExerciseStats(
                exerciseName = exercise?.name ?: "Unknown",
                avgWeight = sets.map { it.actualWeight }.average().toFloat(),
                avgReps = sets.map { it.actualReps }.average().toInt(),
                avgRpe = sets.mapNotNull { it.actualRpe }.takeIf { it.isNotEmpty() }?.average()?.toFloat(),
                maxWeight = sets.maxOf { it.actualWeight },
                totalSets = sets.size
            )
        }
    
    // Training analysis
    suspend fun getTrainingAnalysis(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): TrainingAnalysis = 
        withContext(Dispatchers.IO) {
            val workouts = workoutDao.getWorkoutsInDateRange(startDate, endDate)
            val completedWorkouts = workouts.filter { it.status == com.github.radupana.featherweight.data.WorkoutStatus.COMPLETED }
            
            val totalWorkouts = completedWorkouts.size
            
            // Calculate total volume
            var totalVolume = 0f
            var totalSets = 0
            
            for (workout in completedWorkouts) {
                val exerciseLogs = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
                for (exerciseLog in exerciseLogs) {
                    val sets = setLogDao.getSetLogsForExercise(exerciseLog.id)
                    for (set in sets.filter { it.isCompleted }) {
                        totalVolume += (set.actualWeight * set.actualReps)
                        totalSets++
                    }
                }
            }
            
            TrainingAnalysis(
                analysisDate = LocalDateTime.now(),
                periodStart = startDate.toLocalDate(),
                periodEnd = endDate.toLocalDate(),
                overallAssessment = "Analysis of $totalWorkouts workouts",
                keyInsightsJson = "[]",
                recommendationsJson = "[]",
                warningsJson = "[]"
            )
        }
    
    // Swap history tracking
    suspend fun getSwapHistoryCounts(): List<SwapHistoryCount> = 
        withContext(Dispatchers.IO) {
            emptyList()
        }
    
    suspend fun incrementSwapCount(
        originalExerciseId: Long,
        swappedExerciseId: Long
    ) = withContext(Dispatchers.IO) {
    }
}