package com.github.radupana.featherweight.repository

import android.app.Application
import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.data.exercise.*
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
    private val db = FeatherweightDatabase.getDatabase(application)

    private val workoutDao = db.workoutDao()
    private val exerciseLogDao = db.exerciseLogDao()
    private val setLogDao = db.setLogDao()
    private val exerciseDao = db.exerciseDao()

    private val exerciseSeeder = ExerciseSeeder(exerciseDao)

    // Initialize with seed data for testing
    suspend fun seedDatabaseIfEmpty() {
        withContext(Dispatchers.IO) {
            val exerciseCount = exerciseDao.getAllExercisesWithDetails().size
            val workoutCount = workoutDao.getAllWorkouts().size

            // Always seed exercises if there are none
            if (exerciseCount == 0) {
                exerciseSeeder.seedMainLifts()
                println("Seeded ${exerciseDao.getAllExercisesWithDetails().size} exercises")
            }

            // Only seed workouts if database is completely empty
            if (workoutCount == 0) {
                seedRealistic531Data()
                println("Seeded realistic 531 workout data for past year")
            }
        }
    }

    private suspend fun clearAllWorkouts() {
        // Delete all workouts (will cascade delete exercises and sets)
        val allWorkouts = workoutDao.getAllWorkouts()
        allWorkouts.forEach { workout ->
            workoutDao.deleteWorkout(workout.id)
        }
    }

    // ===== EXERCISE METHODS =====

    suspend fun getAllExercises(): List<ExerciseWithDetails> =
        withContext(Dispatchers.IO) {
            exerciseDao.getAllExercisesWithDetails()
        }

    suspend fun getAllExercisesWithUsageStats(): List<Pair<ExerciseWithDetails, Int>> =
        withContext(Dispatchers.IO) {
            val exercises = exerciseDao.getAllExercisesWithDetails()

            // Get usage count for each exercise (count of exercise logs)
            exercises.map { exercise ->
                val usageCount = exerciseLogDao.getExerciseUsageCount(exercise.exercise.name)
                Pair(exercise, usageCount)
            }.sortedWith(
                compareByDescending<Pair<ExerciseWithDetails, Int>> { it.second }
                    .thenBy { it.first.exercise.name },
            )
        }

    suspend fun getExerciseById(id: Long): ExerciseWithDetails? =
        withContext(Dispatchers.IO) {
            exerciseDao.getExerciseWithDetails(id)
        }

    suspend fun searchExercises(query: String): List<Exercise> =
        withContext(Dispatchers.IO) {
            exerciseDao.searchExercises(query)
        }

    suspend fun getExercisesByCategory(category: ExerciseCategory): List<Exercise> =
        withContext(Dispatchers.IO) {
            exerciseDao.getExercisesByCategory(category)
        }

    suspend fun getExercisesByMuscleGroup(muscleGroup: MuscleGroup): List<ExerciseWithDetails> =
        withContext(Dispatchers.IO) {
            exerciseDao.getExercisesByMuscleGroup(muscleGroup)
        }

    suspend fun getExercisesByEquipment(equipment: List<Equipment>): List<ExerciseWithDetails> =
        withContext(Dispatchers.IO) {
            exerciseDao.getExercisesByEquipment(equipment)
        }

    suspend fun getFilteredExercises(
        category: ExerciseCategory? = null,
        muscleGroup: MuscleGroup? = null,
        equipment: Equipment? = null,
        availableEquipment: List<Equipment> = emptyList(),
        maxDifficulty: ExerciseDifficulty? = null,
        includeCustom: Boolean = true,
        searchQuery: String = "",
    ): List<ExerciseWithDetails> =
        withContext(Dispatchers.IO) {
            exerciseDao.getFilteredExercises(
                category,
                muscleGroup,
                equipment,
                availableEquipment,
                maxDifficulty,
                includeCustom,
                searchQuery,
            )
        }

    suspend fun createCustomExercise(
        name: String,
        category: ExerciseCategory,
        primaryMuscles: Set<MuscleGroup>,
        secondaryMuscles: Set<MuscleGroup> = emptySet(),
        requiredEquipment: Set<Equipment> = emptySet(),
        movementPatterns: Set<MovementPattern> = emptySet(),
        userId: String,
    ): Long =
        withContext(Dispatchers.IO) {
            try {
                val exercise =
                    Exercise(
                        name = name,
                        category = category,
                        type = ExerciseType.STRENGTH,
                        difficulty = ExerciseDifficulty.BEGINNER,
                        isCustom = true,
                        createdBy = userId,
                        isPublic = false,
                    )

                // Remove duplicates and ensure primary muscles take precedence over secondary
                val cleanSecondaryMuscles = secondaryMuscles - primaryMuscles

                val muscleGroups =
                    mutableListOf<ExerciseMuscleGroup>().apply {
                        // Add primary muscles
                        primaryMuscles.forEach { muscle ->
                            add(ExerciseMuscleGroup(0, muscle, isPrimary = true))
                        }
                        // Add secondary muscles (excluding any that are already primary)
                        cleanSecondaryMuscles.forEach { muscle ->
                            add(ExerciseMuscleGroup(0, muscle, isPrimary = false))
                        }
                    }

                val equipment =
                    requiredEquipment.map {
                        ExerciseEquipment(0, it, isRequired = true, isAlternative = false)
                    }

                val patterns =
                    movementPatterns.map {
                        ExerciseMovementPattern(0, it, isPrimary = true)
                    }

                exerciseDao.insertExerciseWithDetails(exercise, muscleGroups, equipment, patterns)
            } catch (e: Exception) {
                println("Error creating custom exercise '$name': ${e.message}")
                e.printStackTrace()
                throw Exception("Failed to create custom exercise: ${e.message}", e)
            }
        }

    // ===== EXISTING WORKOUT METHODS (Updated to work with new Exercise system) =====

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

    // Enhanced exercise log creation that links to Exercise entity
    suspend fun insertExerciseLogWithExerciseReference(
        workoutId: Long,
        exercise: ExerciseWithDetails,
        exerciseOrder: Int,
        notes: String? = null,
    ): Long {
        val exerciseLog =
            ExerciseLog(
                workoutId = workoutId,
                exerciseName = exercise.exercise.name,
                exerciseId = exercise.exercise.id,
                exerciseOrder = exerciseOrder,
                notes = notes,
            )
        return insertExerciseLog(exerciseLog)
    }

    // Get exercise details for an ExerciseLog
    suspend fun getExerciseDetailsForLog(exerciseLog: ExerciseLog): ExerciseWithDetails? =
        exerciseLog.exerciseId?.let { exerciseId ->
            getExerciseById(exerciseId)
        }

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

    // Smart suggestions functionality (enhanced with exercise data)
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

    // ===== ANALYTICS METHODS =====

    // Volume analytics
    suspend fun getWeeklyVolumeTotal(startDate: LocalDateTime): Float =
        withContext(Dispatchers.IO) {
            val endDate = startDate.plusDays(7)
            val workouts =
                workoutDao.getAllWorkouts().filter {
                    it.date >= startDate && it.date < endDate
                }

            var totalVolume = 0f
            workouts.forEach { workout ->
                val exercises = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
                exercises.forEach { exercise ->
                    val sets = setLogDao.getSetLogsForExercise(exercise.id)
                    sets.filter { it.isCompleted }.forEach { set ->
                        totalVolume += set.weight * set.reps
                    }
                }
            }
            totalVolume
        }

    suspend fun getMonthlyVolumeTotal(startDate: LocalDateTime): Float =
        withContext(Dispatchers.IO) {
            val endDate = startDate.plusMonths(1)
            val workouts =
                workoutDao.getAllWorkouts().filter {
                    it.date >= startDate && it.date < endDate
                }

            var totalVolume = 0f
            workouts.forEach { workout ->
                val exercises = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
                exercises.forEach { exercise ->
                    val sets = setLogDao.getSetLogsForExercise(exercise.id)
                    sets.filter { it.isCompleted }.forEach { set ->
                        totalVolume += set.weight * set.reps
                    }
                }
            }
            totalVolume
        }

    // Strength progression analytics
    suspend fun getPersonalRecords(exerciseName: String): List<Pair<Float, LocalDateTime>> =
        withContext(Dispatchers.IO) {
            val allWorkouts = workoutDao.getAllWorkouts()
            val records = mutableListOf<Pair<Float, LocalDateTime>>()
            var currentMaxWeight = 0f

            // Sort workouts by date to track progression over time
            allWorkouts.sortedBy { it.date }.forEach { workout ->
                val exercises = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
                val matchingExercise = exercises.find { it.exerciseName == exerciseName }

                if (matchingExercise != null) {
                    val sets = setLogDao.getSetLogsForExercise(matchingExercise.id)
                    val maxWeightInWorkout =
                        sets.filter { it.isCompleted && it.reps > 0 }
                            .maxOfOrNull { it.weight } ?: 0f

                    if (maxWeightInWorkout > currentMaxWeight) {
                        currentMaxWeight = maxWeightInWorkout
                        records.add(Pair(maxWeightInWorkout, workout.date))
                    }
                }
            }
            records
        }

    suspend fun getEstimated1RM(exerciseName: String): Float? =
        withContext(Dispatchers.IO) {
            val allWorkouts = workoutDao.getAllWorkouts()
            var bestEstimate = 0f

            allWorkouts.forEach { workout ->
                val exercises = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
                val matchingExercise = exercises.find { it.exerciseName == exerciseName }

                if (matchingExercise != null) {
                    val sets = setLogDao.getSetLogsForExercise(matchingExercise.id)
                    sets.filter { it.isCompleted && it.reps > 0 && it.weight > 0 }.forEach { set ->
                        // Brzycki formula: 1RM = weight / (1.0278 - 0.0278 * reps)
                        val estimated1RM = set.weight / (1.0278f - 0.0278f * set.reps)
                        if (estimated1RM > bestEstimate) {
                            bestEstimate = estimated1RM
                        }
                    }
                }
            }
            if (bestEstimate > 0) bestEstimate else null
        }

    // Performance insights
    suspend fun getTrainingFrequency(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): Int =
        withContext(Dispatchers.IO) {
            workoutDao.getAllWorkouts().count { workout ->
                workout.date >= startDate && workout.date < endDate &&
                    workout.notes?.contains("[COMPLETED]") == true
            }
        }

    suspend fun getAverageRPE(
        exerciseName: String? = null,
        daysSince: Int = 30,
    ): Float? =
        withContext(Dispatchers.IO) {
            val cutoffDate = LocalDateTime.now().minusDays(daysSince.toLong())
            val allWorkouts = workoutDao.getAllWorkouts().filter { it.date >= cutoffDate }
            val allRPEs = mutableListOf<Float>()

            allWorkouts.forEach { workout ->
                val exercises = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
                val targetExercises =
                    if (exerciseName != null) {
                        exercises.filter { it.exerciseName == exerciseName }
                    } else {
                        exercises
                    }

                targetExercises.forEach { exercise ->
                    val sets = setLogDao.getSetLogsForExercise(exercise.id)
                    sets.filter { it.isCompleted && it.rpe != null }.forEach { set ->
                        set.rpe?.let { allRPEs.add(it) }
                    }
                }
            }

            if (allRPEs.isNotEmpty()) allRPEs.average().toFloat() else null
        }

    suspend fun getTrainingStreak(): Int =
        withContext(Dispatchers.IO) {
            val allWorkouts =
                workoutDao.getAllWorkouts()
                    .filter { it.notes?.contains("[COMPLETED]") == true }
                    .sortedByDescending { it.date }

            if (allWorkouts.isEmpty()) return@withContext 0

            var streak = 0
            var currentDate = LocalDateTime.now().toLocalDate()
            var workoutIndex = 0

            while (workoutIndex < allWorkouts.size) {
                val workoutDate = allWorkouts[workoutIndex].date.toLocalDate()

                // Check if there's a workout on current date or the day before
                if (workoutDate == currentDate || workoutDate == currentDate.minusDays(1)) {
                    if (workoutDate == currentDate.minusDays(1)) {
                        currentDate = currentDate.minusDays(1)
                    }
                    streak++
                    workoutIndex++

                    // Skip any additional workouts on the same day
                    while (workoutIndex < allWorkouts.size &&
                        allWorkouts[workoutIndex].date.toLocalDate() == workoutDate
                    ) {
                        workoutIndex++
                    }

                    currentDate = currentDate.minusDays(1)
                } else {
                    break
                }
            }
            streak
        }

    // Quick stats for dashboard
    suspend fun getRecentPR(): Pair<String, Float>? =
        withContext(Dispatchers.IO) {
            val exerciseNames = listOf("Bench Press", "Back Squat", "Conventional Deadlift", "Overhead Press")
            var mostRecentPR: Triple<String, Float, LocalDateTime>? = null

            exerciseNames.forEach { exerciseName ->
                val records = getPersonalRecords(exerciseName)
                if (records.isNotEmpty()) {
                    val latestRecord = records.last()
                    if (mostRecentPR == null || latestRecord.second > mostRecentPR!!.third) {
                        mostRecentPR = Triple(exerciseName, latestRecord.first, latestRecord.second)
                    }
                }
            }

            mostRecentPR?.let { Pair(it.first, it.second) }
        }

    suspend fun getProgressPercentage(daysSince: Int = 30): Float? =
        withContext(Dispatchers.IO) {
            val cutoffDate = LocalDateTime.now().minusDays(daysSince.toLong())
            val exerciseNames = listOf("Bench Press", "Back Squat", "Conventional Deadlift", "Overhead Press")
            val improvements = mutableListOf<Float>()

            exerciseNames.forEach { exerciseName ->
                val records = getPersonalRecords(exerciseName)
                if (records.size >= 2) {
                    val recentRecords = records.filter { it.second >= cutoffDate }
                    if (recentRecords.isNotEmpty()) {
                        val oldMax = records.lastOrNull { it.second < cutoffDate }?.first
                        val newMax = recentRecords.maxOf { it.first }

                        if (oldMax != null && oldMax > 0) {
                            val improvement = ((newMax - oldMax) / oldMax) * 100
                            improvements.add(improvement)
                        }
                    }
                }
            }

            if (improvements.isNotEmpty()) improvements.average().toFloat() else null
        }

    // Historical volume data for charts
    suspend fun getWeeklyVolumeHistory(weeksBack: Int = 12): List<Pair<String, Float>> =
        withContext(Dispatchers.IO) {
            val now = LocalDateTime.now()
            val weeklyVolumes = mutableListOf<Pair<String, Float>>()

            repeat(weeksBack) { weekIndex ->
                val weekStart = now.minusDays((weekIndex + 1) * 7L)
                val volume = getWeeklyVolumeTotal(weekStart)
                val weekLabel =
                    if (weekIndex == 0) {
                        "This Week"
                    } else if (weekIndex == 1) {
                        "Last Week"
                    } else {
                        "${weekIndex + 1}w ago"
                    }
                weeklyVolumes.add(weekLabel to volume)
            }

            weeklyVolumes.reversed() // Chronological order
        }

    // ===== REALISTIC 531 SEED DATA =====
    private suspend fun seedRealistic531Data() {
        // 531 program structure: 4 days per week
        // Day 1: Squat + accessories
        // Day 2: Bench + accessories
        // Day 3: Deadlift + accessories
        // Day 4: Overhead Press + accessories

        val startDate = LocalDateTime.now().minusDays(365) // Past year
        val endDate = LocalDateTime.now()

        // Intermediate lifter starting maxes (kg)
        val initialMaxes =
            mapOf(
                "Back Squat" to 140f,
                "Bench Press" to 110f,
                "Conventional Deadlift" to 170f,
                "Overhead Press" to 75f,
            )

        // Progressive overload: 2.5kg squat/bench, 5kg deadlift per cycle (3 weeks)
        val progressionRates =
            mapOf(
                "Back Squat" to 2.5f,
                "Bench Press" to 2.5f,
                "Conventional Deadlift" to 5f,
                "Overhead Press" to 1.25f,
            )

        // 531 percentages for each week
        val week1Percentages = listOf(0.65f, 0.75f, 0.85f) // Week 1: 65%, 75%, 85%
        val week2Percentages = listOf(0.70f, 0.80f, 0.90f) // Week 2: 70%, 80%, 90%
        val week3Percentages = listOf(0.75f, 0.85f, 0.95f) // Week 3: 75%, 85%, 95%

        val weeklyPercentages = listOf(week1Percentages, week2Percentages, week3Percentages)

        var currentDate = startDate
        var cycleNumber = 0

        while (currentDate.isBefore(endDate)) {
            val weekInCycle = cycleNumber % 3
            val cycleCompleted = cycleNumber / 3

            // Update maxes every 3 weeks
            val currentMaxes =
                initialMaxes.mapValues { (exercise, initialMax) ->
                    initialMax + (progressionRates[exercise] ?: 0f) * cycleCompleted
                }

            // Day 1: Squat Day (Monday)
            if (currentDate.dayOfWeek.value == 1) {
                createSquatWorkout(currentDate, currentMaxes, weeklyPercentages[weekInCycle])
            }

            // Day 2: Bench Day (Wednesday)
            if (currentDate.dayOfWeek.value == 3) {
                createBenchWorkout(currentDate, currentMaxes, weeklyPercentages[weekInCycle])
            }

            // Day 3: Deadlift Day (Friday)
            if (currentDate.dayOfWeek.value == 5) {
                createDeadliftWorkout(currentDate, currentMaxes, weeklyPercentages[weekInCycle])
            }

            // Day 4: OHP Day (Saturday)
            if (currentDate.dayOfWeek.value == 6) {
                createOHPWorkout(currentDate, currentMaxes, weeklyPercentages[weekInCycle])
            }

            currentDate = currentDate.plusDays(1)

            // Increment cycle every 7 days
            if (currentDate.dayOfWeek.value == 1) {
                cycleNumber++
            }
        }
    }

    private suspend fun createSquatWorkout(
        date: LocalDateTime,
        maxes: Map<String, Float>,
        percentages: List<Float>,
    ) {
        val workoutName = "Lower Body - Squat Focus"
        val workout = Workout(date = date, notes = "$workoutName [COMPLETED]")
        val workoutId = workoutDao.insertWorkout(workout)

        // Main lift: Back Squat (531)
        val squatMax = maxes["Back Squat"] ?: 140f
        val squatExercise = searchExercises("Back Squat").firstOrNull()
        val squatLog =
            ExerciseLog(
                workoutId = workoutId,
                exerciseName = "Back Squat",
                exerciseId = squatExercise?.id,
                exerciseOrder = 0,
            )
        val squatLogId = exerciseLogDao.insertExerciseLog(squatLog)

        // 531 sets
        percentages.forEachIndexed { index, percentage ->
            val weight =
                (squatMax * percentage).let {
                    // Round to nearest 2.5kg
                    (it / 2.5f).toInt() * 2.5f
                }
            val reps =
                when (index) {
                    0 -> 5 // First set: 5 reps
                    1 -> 3 // Second set: 3 reps
                    2 -> (1..5).random() // AMRAP set
                    else -> 1
                }
            val rpe =
                when (index) {
                    0 -> (6.0f..7.0f).random()
                    1 -> (7.5f..8.5f).random()
                    2 -> (8.5f..9.5f).random()
                    else -> 8f
                }

            setLogDao.insertSetLog(
                SetLog(
                    exerciseLogId = squatLogId,
                    setOrder = index,
                    reps = reps,
                    weight = weight,
                    rpe = rpe,
                    isCompleted = true,
                    completedAt = date.toString(),
                ),
            )
        }

        // Accessories
        createAccessoryExercise(workoutId, "Romanian Deadlift", 1, date, (80f..100f), (8..12), 3)
        createAccessoryExercise(workoutId, "Bulgarian Split Squats", 2, date, (20f..30f), (8..12), 3)
        createAccessoryExercise(workoutId, "Pull-ups", 3, date, (0f..10f), (5..10), 3)
    }

    private suspend fun createBenchWorkout(
        date: LocalDateTime,
        maxes: Map<String, Float>,
        percentages: List<Float>,
    ) {
        val workoutName = "Upper Body - Bench Focus"
        val workout = Workout(date = date, notes = "$workoutName [COMPLETED]")
        val workoutId = workoutDao.insertWorkout(workout)

        // Main lift: Bench Press (531)
        val benchMax = maxes["Bench Press"] ?: 110f
        val benchExercise = searchExercises("Bench Press").firstOrNull()
        val benchLog =
            ExerciseLog(
                workoutId = workoutId,
                exerciseName = "Bench Press",
                exerciseId = benchExercise?.id,
                exerciseOrder = 0,
            )
        val benchLogId = exerciseLogDao.insertExerciseLog(benchLog)

        // 531 sets
        percentages.forEachIndexed { index, percentage ->
            val weight =
                (benchMax * percentage).let {
                    (it / 2.5f).toInt() * 2.5f
                }
            val reps =
                when (index) {
                    0 -> 5
                    1 -> 3
                    2 -> (1..8).random() // AMRAP
                    else -> 1
                }
            val rpe =
                when (index) {
                    0 -> (6.0f..7.0f).random()
                    1 -> (7.5f..8.5f).random()
                    2 -> (8.5f..9.5f).random()
                    else -> 8f
                }

            setLogDao.insertSetLog(
                SetLog(
                    exerciseLogId = benchLogId,
                    setOrder = index,
                    reps = reps,
                    weight = weight,
                    rpe = rpe,
                    isCompleted = true,
                    completedAt = date.toString(),
                ),
            )
        }

        // Accessories
        createAccessoryExercise(workoutId, "Incline Dumbbell Press", 1, date, (25f..35f), (8..12), 3)
        createAccessoryExercise(workoutId, "Bent-Over Barbell Row", 2, date, (60f..80f), (8..12), 3)
        createAccessoryExercise(workoutId, "Lateral Raises", 3, date, (8f..15f), (12..15), 4)
    }

    private suspend fun createDeadliftWorkout(
        date: LocalDateTime,
        maxes: Map<String, Float>,
        percentages: List<Float>,
    ) {
        val workoutName = "Lower Body - Deadlift Focus"
        val workout = Workout(date = date, notes = "$workoutName [COMPLETED]")
        val workoutId = workoutDao.insertWorkout(workout)

        // Main lift: Deadlift (531)
        val deadliftMax = maxes["Conventional Deadlift"] ?: 170f
        val deadliftExercise = searchExercises("Conventional Deadlift").firstOrNull()
        val deadliftLog =
            ExerciseLog(
                workoutId = workoutId,
                exerciseName = "Conventional Deadlift",
                exerciseId = deadliftExercise?.id,
                exerciseOrder = 0,
            )
        val deadliftLogId = exerciseLogDao.insertExerciseLog(deadliftLog)

        // 531 sets
        percentages.forEachIndexed { index, percentage ->
            val weight =
                (deadliftMax * percentage).let {
                    (it / 2.5f).toInt() * 2.5f
                }
            val reps =
                when (index) {
                    0 -> 5
                    1 -> 3
                    2 -> (1..3).random() // AMRAP (deadlifts are harder)
                    else -> 1
                }
            val rpe =
                when (index) {
                    0 -> (6.5f..7.5f).random()
                    1 -> (8.0f..8.5f).random()
                    2 -> (9.0f..9.5f).random()
                    else -> 8f
                }

            setLogDao.insertSetLog(
                SetLog(
                    exerciseLogId = deadliftLogId,
                    setOrder = index,
                    reps = reps,
                    weight = weight,
                    rpe = rpe,
                    isCompleted = true,
                    completedAt = date.toString(),
                ),
            )
        }

        // Accessories
        createAccessoryExercise(workoutId, "Front Squats", 1, date, (60f..80f), (6..8), 3)
        createAccessoryExercise(workoutId, "Barbell Curls", 2, date, (25f..35f), (8..12), 3)
        createAccessoryExercise(workoutId, "Hanging Leg Raises", 3, date, (0f..10f), (8..15), 3)
    }

    private suspend fun createOHPWorkout(
        date: LocalDateTime,
        maxes: Map<String, Float>,
        percentages: List<Float>,
    ) {
        val workoutName = "Upper Body - Press Focus"
        val workout = Workout(date = date, notes = "$workoutName [COMPLETED]")
        val workoutId = workoutDao.insertWorkout(workout)

        // Main lift: Overhead Press (531)
        val ohpMax = maxes["Overhead Press"] ?: 75f
        val ohpExercise = searchExercises("Overhead Press").firstOrNull()
        val ohpLog =
            ExerciseLog(
                workoutId = workoutId,
                exerciseName = "Overhead Press",
                exerciseId = ohpExercise?.id,
                exerciseOrder = 0,
            )
        val ohpLogId = exerciseLogDao.insertExerciseLog(ohpLog)

        // 531 sets
        percentages.forEachIndexed { index, percentage ->
            val weight =
                (ohpMax * percentage).let {
                    (it / 1.25f).toInt() * 1.25f // Smaller increments for OHP
                }
            val reps =
                when (index) {
                    0 -> 5
                    1 -> 3
                    2 -> (1..6).random() // AMRAP
                    else -> 1
                }
            val rpe =
                when (index) {
                    0 -> (6.5f..7.5f).random()
                    1 -> (8.0f..8.5f).random()
                    2 -> (9.0f..9.5f).random()
                    else -> 8f
                }

            setLogDao.insertSetLog(
                SetLog(
                    exerciseLogId = ohpLogId,
                    setOrder = index,
                    reps = reps,
                    weight = weight,
                    rpe = rpe,
                    isCompleted = true,
                    completedAt = date.toString(),
                ),
            )
        }

        // Accessories
        createAccessoryExercise(workoutId, "Incline Bench Press", 1, date, (60f..80f), (8..10), 3)
        createAccessoryExercise(workoutId, "Cable Flys", 2, date, (15f..25f), (10..15), 3)
        createAccessoryExercise(workoutId, "Dumbbell Curls", 3, date, (12f..20f), (10..15), 3)
    }

    private suspend fun createAccessoryExercise(
        workoutId: Long,
        exerciseName: String,
        order: Int,
        date: LocalDateTime,
        weightRange: ClosedFloatingPointRange<Float>,
        repRange: IntRange,
        numSets: Int,
    ) {
        // Try to find exercise, if not found just use the name
        val exercise =
            try {
                searchExercises(exerciseName).firstOrNull()
            } catch (e: Exception) {
                null
            }

        val exerciseLog =
            ExerciseLog(
                workoutId = workoutId,
                exerciseName = exerciseName,
                exerciseId = exercise?.id,
                exerciseOrder = order,
            )
        val exerciseLogId = exerciseLogDao.insertExerciseLog(exerciseLog)

        val baseWeight = weightRange.start + (weightRange.endInclusive - weightRange.start) * kotlin.random.Random.nextFloat()

        repeat(numSets) { setIndex ->
            val weight =
                if (exerciseName.contains("Pull-ups") || exerciseName.contains("Hanging")) {
                    // Bodyweight exercises - use added weight
                    listOf(0f, 2.5f, 5f, 7.5f, 10f).random()
                } else {
                    // Normal weight progression through sets
                    baseWeight + (setIndex * 2.5f)
                }

            val reps = repRange.random()
            val rpe = (6.5f..8.5f).random()

            setLogDao.insertSetLog(
                SetLog(
                    exerciseLogId = exerciseLogId,
                    setOrder = setIndex,
                    reps = reps,
                    weight = weight,
                    rpe = rpe,
                    isCompleted = true,
                    completedAt = date.toString(),
                ),
            )
        }
    }

    // Helper function for random float in range
    private fun ClosedFloatingPointRange<Float>.random(): Float {
        return start + (endInclusive - start) * kotlin.random.Random.nextFloat()
    }

    // ===== OLD SEED DATA (UNUSED) =====
    private suspend fun seedTestData() {
        // Create sample workouts from past dates
        val workout1 =
            Workout(
                date = LocalDateTime.now().minusDays(3),
                notes = "Upper Body Push",
            )
        val workout1Id = workoutDao.insertWorkout(workout1)

        // Get exercises from database instead of hardcoding
        val benchPressExercise = searchExercises("Bench Press").firstOrNull()
        val overheadPressExercise = searchExercises("Overhead Press").firstOrNull()

        // Add exercises to workout 1
        val benchPress =
            ExerciseLog(
                workoutId = workout1Id,
                exerciseName = "Bench Press",
                exerciseId = benchPressExercise?.id,
                exerciseOrder = 0,
            )
        val benchPressId = exerciseLogDao.insertExerciseLog(benchPress)

        val overheadPress =
            ExerciseLog(
                workoutId = workout1Id,
                exerciseName = "Overhead Press",
                exerciseId = overheadPressExercise?.id,
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

        val squatExercise = searchExercises("Back Squat").firstOrNull()
        val deadliftExercise = searchExercises("Conventional Deadlift").firstOrNull()

        val squat =
            ExerciseLog(
                workoutId = workout2Id,
                exerciseName = "Back Squat",
                exerciseId = squatExercise?.id,
                exerciseOrder = 0,
            )
        val squatId = exerciseLogDao.insertExerciseLog(squat)

        val deadlift =
            ExerciseLog(
                workoutId = workout2Id,
                exerciseName = "Conventional Deadlift",
                exerciseId = deadliftExercise?.id,
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
}
