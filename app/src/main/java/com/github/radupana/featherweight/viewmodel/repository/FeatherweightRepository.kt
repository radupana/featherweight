package com.github.radupana.featherweight.viewmodel.repository

import android.app.Application
import androidx.room.Room
import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.domain.ExerciseHistory
import com.github.radupana.featherweight.domain.ExerciseStats
import com.github.radupana.featherweight.domain.SmartSuggestions

class FeatherweightRepository(
    application: Application,
) {
    private val db =
        Room
            .databaseBuilder(
                application,
                FeatherweightDatabase::class.java,
                "featherweight-db",
            ).fallbackToDestructiveMigration(true) // TODO: remove once going to a real DB
            .build()

    private val workoutDao = db.workoutDao()
    private val exerciseLogDao = db.exerciseLogDao()
    private val setLogDao = db.setLogDao()

    // Basic CRUD operations
    suspend fun insertWorkout(workout: Workout): Long = workoutDao.insertWorkout(workout)

    suspend fun getExercisesForWorkout(workoutId: Long): List<ExerciseLog> =
        exerciseLogDao.getExerciseLogsForWorkout(workoutId)

    suspend fun getSetsForExercise(exerciseLogId: Long): List<SetLog> =
        setLogDao.getSetLogsForExercise(exerciseLogId)

    suspend fun markSetCompleted(setId: Long, completed: Boolean, completedAt: String?) =
        setLogDao.markSetCompleted(setId, completed, completedAt)

    suspend fun insertExerciseLog(exerciseLog: ExerciseLog): Long =
        exerciseLogDao.insertExerciseLog(exerciseLog)

    suspend fun insertSetLog(setLog: SetLog): Long = setLogDao.insertSetLog(setLog)

    suspend fun updateSetLog(setLog: SetLog) = setLogDao.updateSetLog(setLog)

    suspend fun deleteSetLog(setId: Long) = setLogDao.deleteSetLog(setId)

    // Smart suggestions functionality
    suspend fun getExerciseHistory(exerciseName: String, currentWorkoutId: Long): ExerciseHistory? {
        val allWorkouts = workoutDao.getAllWorkouts()

        for (workout in allWorkouts) {
            if (workout.id == currentWorkoutId) continue

            val exercises = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
            val matchingExercise = exercises.find { it.exerciseName == exerciseName }

            if (matchingExercise != null) {
                val sets = setLogDao.getSetLogsForExercise(matchingExercise.id)
                return ExerciseHistory(
                    exerciseName = exerciseName,
                    lastWorkoutDate = workout.date,
                    sets = sets
                )
            }
        }
        return null
    }

    suspend fun getExerciseStats(exerciseName: String): ExerciseStats? {
        val allWorkouts = workoutDao.getAllWorkouts()
        val allSetsForExercise = mutableListOf<SetLog>()

        for (workout in allWorkouts) {
            val exercises = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
            val matchingExercise = exercises.find { it.exerciseName == exerciseName }
            if (matchingExercise != null) {
                val sets = setLogDao.getSetLogsForExercise(matchingExercise.id)
                allSetsForExercise.addAll(sets)
            }
        }

        if (allSetsForExercise.isEmpty()) return null

        val completedSets = allSetsForExercise.filter {
            it.isCompleted && it.weight > 0 && it.reps > 0
        }
        if (completedSets.isEmpty()) return null

        return ExerciseStats(
            exerciseName = exerciseName,
            avgWeight = completedSets.map { it.weight }.average().toFloat(),
            avgReps = completedSets.map { it.reps }.average().toInt(),
            avgRpe = completedSets.mapNotNull { it.rpe }.average()
                .takeIf { !it.isNaN() }?.toFloat(),
            maxWeight = completedSets.maxOf { it.weight },
            totalSets = completedSets.size
        )
    }

    suspend fun getSmartSuggestions(
        exerciseName: String,
        currentWorkoutId: Long
    ): SmartSuggestions? {
        // Try history first
        val history = getExerciseHistory(exerciseName, currentWorkoutId)
        if (history != null && history.sets.isNotEmpty()) {
            val lastCompletedSets = history.sets.filter { it.isCompleted }
            if (lastCompletedSets.isNotEmpty()) {
                val mostCommonWeight = lastCompletedSets.groupBy { it.weight }
                    .maxByOrNull { it.value.size }?.key ?: 0f
                val mostCommonReps = lastCompletedSets.groupBy { it.reps }
                    .maxByOrNull { it.value.size }?.key ?: 0
                val avgRpe = lastCompletedSets.mapNotNull { it.rpe }.average()
                    .takeIf { !it.isNaN() }?.toFloat()

                return SmartSuggestions(
                    suggestedWeight = mostCommonWeight,
                    suggestedReps = mostCommonReps,
                    suggestedRpe = avgRpe,
                    lastWorkoutDate = history.lastWorkoutDate,
                    confidence = "Last workout"
                )
            }
        }

        // Fallback to overall stats
        val stats = getExerciseStats(exerciseName)
        if (stats != null) {
            return SmartSuggestions(
                suggestedWeight = stats.avgWeight,
                suggestedReps = stats.avgReps,
                suggestedRpe = stats.avgRpe,
                lastWorkoutDate = null,
                confidence = "Average from ${stats.totalSets} sets"
            )
        }

        return null
    }
}