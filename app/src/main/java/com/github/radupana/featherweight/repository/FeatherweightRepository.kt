package com.github.radupana.featherweight.repository

import android.app.Application
import androidx.room.Room
import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.domain.ExerciseHistory
import com.github.radupana.featherweight.domain.ExerciseStats
import com.github.radupana.featherweight.domain.SmartSuggestions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

data class WorkoutSummary(
    val id: Long,
    val date: LocalDateTime,
    val name: String?,
    val exerciseCount: Int,
    val setCount: Int,
    val totalWeight: Float,
    val duration: Long?, // minutes
    val isCompleted: Boolean,
)

class FeatherweightRepository(
    application: Application,
) {
    private val db =
        Room
            .databaseBuilder(
                application,
                FeatherweightDatabase::class.java,
                "featherweight-db",
            ).fallbackToDestructiveMigration(true)
            .addCallback(
                object : androidx.room.RoomDatabase.Callback() {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Add seed data when database is created
                    }
                },
            ).build()

    private val workoutDao = db.workoutDao()
    private val exerciseLogDao = db.exerciseLogDao()
    private val setLogDao = db.setLogDao()

    // Initialize with seed data for testing
    suspend fun seedDatabaseIfEmpty() {
        withContext(Dispatchers.IO) {
            val workoutCount = workoutDao.getAllWorkouts().size
            if (workoutCount == 0) {
                seedTestData()
            }
        }
    }

    private suspend fun seedTestData() {
        // Create sample workouts from past dates
        val workout1 =
            Workout(
                date = LocalDateTime.now().minusDays(3),
                notes = "Upper Body Push",
            )
        val workout1Id = workoutDao.insertWorkout(workout1)

        // Add exercises to workout 1
        val benchPress =
            ExerciseLog(
                workoutId = workout1Id,
                exerciseName = "Bench Press",
                exerciseOrder = 0,
            )
        val benchPressId = exerciseLogDao.insertExerciseLog(benchPress)

        val overheadPress =
            ExerciseLog(
                workoutId = workout1Id,
                exerciseName = "Overhead Press",
                exerciseOrder = 1,
            )
        val overheadPressId = exerciseLogDao.insertExerciseLog(overheadPress)

        // Add sets for bench press
        setLogDao.insertSetLog(
            SetLog(
                exerciseLogId = benchPressId,
                setOrder = 0,
                reps = 8,
                weight = 80f,
                rpe = 7f,
                isCompleted = true,
                completedAt = LocalDateTime.now().minusDays(3).toString(),
            ),
        )
        setLogDao.insertSetLog(
            SetLog(
                exerciseLogId = benchPressId,
                setOrder = 1,
                reps = 8,
                weight = 82.5f,
                rpe = 8f,
                isCompleted = true,
                completedAt = LocalDateTime.now().minusDays(3).toString(),
            ),
        )
        setLogDao.insertSetLog(
            SetLog(
                exerciseLogId = benchPressId,
                setOrder = 2,
                reps = 6,
                weight = 85f,
                rpe = 9f,
                isCompleted = true,
                completedAt = LocalDateTime.now().minusDays(3).toString(),
            ),
        )

        // Add sets for overhead press
        setLogDao.insertSetLog(
            SetLog(
                exerciseLogId = overheadPressId,
                setOrder = 0,
                reps = 10,
                weight = 50f,
                rpe = 6f,
                isCompleted = true,
                completedAt = LocalDateTime.now().minusDays(3).toString(),
            ),
        )
        setLogDao.insertSetLog(
            SetLog(
                exerciseLogId = overheadPressId,
                setOrder = 1,
                reps = 8,
                weight = 52.5f,
                rpe = 7f,
                isCompleted = true,
                completedAt = LocalDateTime.now().minusDays(3).toString(),
            ),
        )

        // Mark workout 1 as completed
        completeWorkout(workout1Id)

        // Create workout 2
        val workout2 =
            Workout(
                date = LocalDateTime.now().minusDays(1),
                notes = "Lower Body",
            )
        val workout2Id = workoutDao.insertWorkout(workout2)

        val squat =
            ExerciseLog(
                workoutId = workout2Id,
                exerciseName = "Squat",
                exerciseOrder = 0,
            )
        val squatId = exerciseLogDao.insertExerciseLog(squat)

        val deadlift =
            ExerciseLog(
                workoutId = workout2Id,
                exerciseName = "Deadlift",
                exerciseOrder = 1,
            )
        val deadliftId = exerciseLogDao.insertExerciseLog(deadlift)

        // Add sets for squat
        setLogDao.insertSetLog(
            SetLog(
                exerciseLogId = squatId,
                setOrder = 0,
                reps = 5,
                weight = 100f,
                rpe = 7f,
                isCompleted = true,
                completedAt = LocalDateTime.now().minusDays(1).toString(),
            ),
        )
        setLogDao.insertSetLog(
            SetLog(
                exerciseLogId = squatId,
                setOrder = 1,
                reps = 5,
                weight = 105f,
                rpe = 8f,
                isCompleted = true,
                completedAt = LocalDateTime.now().minusDays(1).toString(),
            ),
        )

        // Add sets for deadlift
        setLogDao.insertSetLog(
            SetLog(
                exerciseLogId = deadliftId,
                setOrder = 0,
                reps = 3,
                weight = 120f,
                rpe = 8f,
                isCompleted = true,
                completedAt = LocalDateTime.now().minusDays(1).toString(),
            ),
        )
        setLogDao.insertSetLog(
            SetLog(
                exerciseLogId = deadliftId,
                setOrder = 1,
                reps = 3,
                weight = 125f,
                rpe = 9f,
                isCompleted = true,
                completedAt = LocalDateTime.now().minusDays(1).toString(),
            ),
        )

        // Mark workout 2 as completed
        completeWorkout(workout2Id)
    }

    // Basic CRUD operations
    suspend fun insertWorkout(workout: Workout): Long = workoutDao.insertWorkout(workout)

    suspend fun getExercisesForWorkout(workoutId: Long): List<ExerciseLog> = exerciseLogDao.getExerciseLogsForWorkout(workoutId)

    suspend fun getSetsForExercise(exerciseLogId: Long): List<SetLog> = setLogDao.getSetLogsForExercise(exerciseLogId)

    suspend fun markSetCompleted(
        setId: Long,
        completed: Boolean,
        completedAt: String?,
    ) = setLogDao.markSetCompleted(setId, completed, completedAt)

    suspend fun insertExerciseLog(exerciseLog: ExerciseLog): Long = exerciseLogDao.insertExerciseLog(exerciseLog)

    suspend fun insertSetLog(setLog: SetLog): Long = setLogDao.insertSetLog(setLog)

    suspend fun updateSetLog(setLog: SetLog) = setLogDao.updateSetLog(setLog)

    suspend fun deleteSetLog(setId: Long) = setLogDao.deleteSetLog(setId)

    // Workout state management
    suspend fun getOngoingWorkout(): Workout? {
        val allWorkouts = workoutDao.getAllWorkouts()
        return allWorkouts.find { workout ->
            // Check if workout has exercises but no completion marker
            val exercises = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
            val hasExercises = exercises.isNotEmpty()
            val isCompleted = workout.notes?.contains("[COMPLETED]") == true

            hasExercises && !isCompleted
        }
    }

    suspend fun completeWorkout(workoutId: Long) {
        val workout = workoutDao.getAllWorkouts().find { it.id == workoutId } ?: return
        val completedNotes =
            if (workout.notes != null) {
                "${workout.notes} [COMPLETED]"
            } else {
                "[COMPLETED]"
            }

        val updatedWorkout = workout.copy(notes = completedNotes)
        workoutDao.updateWorkout(updatedWorkout)
    }

    suspend fun updateWorkoutName(
        workoutId: Long,
        name: String?,
    ) {
        val workout = workoutDao.getAllWorkouts().find { it.id == workoutId } ?: return
        val isCompleted = workout.notes?.contains("[COMPLETED]") == true

        val newNotes =
            if (isCompleted) {
                if (name != null) "$name [COMPLETED]" else "[COMPLETED]"
            } else {
                name
            }

        val updatedWorkout = workout.copy(notes = newNotes)
        workoutDao.updateWorkout(updatedWorkout)
    }

    // History functionality
    suspend fun getWorkoutHistory(): List<WorkoutSummary> {
        val allWorkouts = workoutDao.getAllWorkouts()
        return allWorkouts
            .mapNotNull { workout ->
                val exercises = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
                if (exercises.isEmpty()) return@mapNotNull null

                val allSets = mutableListOf<SetLog>()
                exercises.forEach { exercise ->
                    allSets.addAll(setLogDao.getSetLogsForExercise(exercise.id))
                }

                val completedSets = allSets.filter { it.isCompleted }
                val totalWeight = completedSets.sumOf { (it.weight * it.reps).toDouble() }.toFloat()

                val isCompleted = workout.notes?.contains("[COMPLETED]") == true
                val displayName =
                    if (workout.notes != null && isCompleted) {
                        workout.notes!!
                            .replace(" [COMPLETED]", "")
                            .replace("[COMPLETED]", "")
                            .trim()
                            .takeIf { it.isNotBlank() }
                    } else {
                        workout.notes?.takeIf { it.isNotBlank() }
                    }

                WorkoutSummary(
                    id = workout.id,
                    date = workout.date,
                    name = displayName,
                    exerciseCount = exercises.size,
                    setCount = allSets.size,
                    totalWeight = totalWeight,
                    duration = null, // TODO: Calculate duration
                    isCompleted = isCompleted,
                )
            }.sortedByDescending { it.date }
    }

    // Smart suggestions functionality
    suspend fun getExerciseHistory(
        exerciseName: String,
        currentWorkoutId: Long,
    ): ExerciseHistory? {
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
                    sets = sets,
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

        val completedSets =
            allSetsForExercise.filter {
                it.isCompleted && it.weight > 0 && it.reps > 0
            }
        if (completedSets.isEmpty()) return null

        return ExerciseStats(
            exerciseName = exerciseName,
            avgWeight = completedSets.map { it.weight }.average().toFloat(),
            avgReps = completedSets.map { it.reps }.average().toInt(),
            avgRpe =
                completedSets
                    .mapNotNull { it.rpe }
                    .average()
                    .takeIf { !it.isNaN() }
                    ?.toFloat(),
            maxWeight = completedSets.maxOf { it.weight },
            totalSets = completedSets.size,
        )
    }

    suspend fun getSmartSuggestions(
        exerciseName: String,
        currentWorkoutId: Long,
    ): SmartSuggestions? {
        // Try history first
        val history = getExerciseHistory(exerciseName, currentWorkoutId)
        if (history != null && history.sets.isNotEmpty()) {
            val lastCompletedSets = history.sets.filter { it.isCompleted }
            if (lastCompletedSets.isNotEmpty()) {
                val mostCommonWeight =
                    lastCompletedSets
                        .groupBy { it.weight }
                        .maxByOrNull { it.value.size }
                        ?.key ?: 0f
                val mostCommonReps =
                    lastCompletedSets
                        .groupBy { it.reps }
                        .maxByOrNull { it.value.size }
                        ?.key ?: 0
                val avgRpe =
                    lastCompletedSets
                        .mapNotNull { it.rpe }
                        .average()
                        .takeIf { !it.isNaN() }
                        ?.toFloat()

                return SmartSuggestions(
                    suggestedWeight = mostCommonWeight,
                    suggestedReps = mostCommonReps,
                    suggestedRpe = avgRpe,
                    lastWorkoutDate = history.lastWorkoutDate,
                    confidence = "Last workout",
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
                confidence = "Average from ${stats.totalSets} sets",
            )
        }

        return null
    }

    suspend fun getWorkoutById(workoutId: Long): Workout? = workoutDao.getWorkoutById(workoutId)

    // Delete an exercise log (will cascade delete all its sets due to foreign key)
    suspend fun deleteExerciseLog(exerciseLogId: Long) = exerciseLogDao.deleteExerciseLog(exerciseLogId)

    // Delete an entire workout (will cascade delete all exercises and sets)
    suspend fun deleteWorkout(workoutId: Long) = workoutDao.deleteWorkout(workoutId)
}
