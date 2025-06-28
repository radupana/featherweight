package com.github.radupana.featherweight.repository

import android.app.Application
import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.data.exercise.Equipment
import com.github.radupana.featherweight.data.exercise.Exercise
import com.github.radupana.featherweight.data.exercise.ExerciseCategory
import com.github.radupana.featherweight.data.exercise.ExerciseDifficulty
import com.github.radupana.featherweight.data.exercise.ExerciseEquipment
import com.github.radupana.featherweight.data.exercise.ExerciseMovementPattern
import com.github.radupana.featherweight.data.exercise.ExerciseMuscleGroup
import com.github.radupana.featherweight.data.exercise.ExerciseSeeder
import com.github.radupana.featherweight.data.exercise.ExerciseType
import com.github.radupana.featherweight.data.exercise.ExerciseWithDetails
import com.github.radupana.featherweight.data.exercise.MovementPattern
import com.github.radupana.featherweight.data.exercise.MuscleGroup
import com.github.radupana.featherweight.domain.ExerciseHistory
import com.github.radupana.featherweight.domain.ExerciseStats
import com.github.radupana.featherweight.domain.SmartSuggestions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

data class NextProgrammeWorkoutInfo(
    val workoutStructure: com.github.radupana.featherweight.data.programme.WorkoutStructure,
    val actualWeekNumber: Int,
)

data class WorkoutSummary(
    val id: Long,
    val date: LocalDateTime,
    val name: String?,
    val exerciseCount: Int,
    val setCount: Int,
    val totalWeight: Float,
    // minutes
    val duration: Long?,
    val isCompleted: Boolean,
    // Programme Integration Fields
    val isProgrammeWorkout: Boolean = false,
    val programmeId: Long? = null,
    val programmeName: String? = null,
    val programmeWorkoutName: String? = null,
    val weekNumber: Int? = null,
    val dayNumber: Int? = null,
)

class FeatherweightRepository(
    application: Application,
) {
    private val db = FeatherweightDatabase.getDatabase(application)
    private val userPreferences = com.github.radupana.featherweight.data.UserPreferences(application)

    private val workoutDao = db.workoutDao()
    private val exerciseLogDao = db.exerciseLogDao()
    private val setLogDao = db.setLogDao()
    private val exerciseDao = db.exerciseDao()
    private val programmeDao = db.programmeDao()
    private val profileDao = db.profileDao()

    private val exerciseSeeder = ExerciseSeeder(exerciseDao)
    private val programmeSeeder = com.github.radupana.featherweight.data.programme.ProgrammeSeeder(programmeDao)

    fun getCurrentUserId(): Long = userPreferences.getCurrentUserId()

    // Initialize with seed data for testing
    suspend fun seedDatabaseIfEmpty() {
        withContext(Dispatchers.IO) {
            val exerciseCount = exerciseDao.getAllExercisesWithDetails().size
            val workoutCount = workoutDao.getAllWorkouts().size
            val programmeTemplateCount = programmeDao.getAllTemplates().size

            // Always seed exercises if there are none
            if (exerciseCount == 0) {
                exerciseSeeder.seedMainLifts()
                println("Seeded ${exerciseDao.getAllExercisesWithDetails().size} exercises")
            }

            // Seed programme templates if none exist
            if (programmeTemplateCount == 0) {
                programmeSeeder.seedPopularProgrammes()
                println("Seeded ${programmeDao.getAllTemplates().size} programme templates")
            }

            if (workoutCount < 10) {
                // Force seed more data if we have very few workouts for testing
                println("⚠️ Only $workoutCount workouts found - force seeding more data for pagination testing")
                seedRealistic531Data()
                println("Force-seeded additional workout data")
                
                // Calculate usage counts after seeding
                updateExerciseUsageCounts()
                println("Updated exercise usage counts")
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
    
    private suspend fun updateExerciseUsageCounts() {
        // Count usage for each exercise based on ExerciseLog entries
        val allExercises = exerciseDao.getAllExercisesWithDetails()
        val allWorkouts = workoutDao.getAllWorkouts()
        
        allExercises.forEach { exerciseWithDetails ->
            var count = 0
            allWorkouts.forEach { workout ->
                val exerciseLogs = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
                count += exerciseLogs.count { log ->
                    log.exerciseId == exerciseWithDetails.exercise.id || 
                    log.exerciseName == exerciseWithDetails.exercise.name
                }
            }
            
            // Update the exercise with the calculated count
            if (count > 0) {
                val updatedExercise = exerciseWithDetails.exercise.copy(usageCount = count)
                exerciseDao.updateExercise(updatedExercise)
            }
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
            exercises
                .map { exercise ->
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
                // Check for duplicate names
                val existingExercises = exerciseDao.getAllExercisesWithDetails()
                val duplicateExists =
                    existingExercises.any {
                        it.exercise.name.equals(name, ignoreCase = true)
                    }

                if (duplicateExists) {
                    throw Exception("An exercise with the name '$name' already exists")
                }
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

    suspend fun insertExerciseLog(exerciseLog: ExerciseLog): Long {
        val id = exerciseLogDao.insertExerciseLog(exerciseLog)
        // Increment usage count if the exercise has an ID reference
        exerciseLog.exerciseId?.let { exerciseId ->
            exerciseDao.incrementUsageCount(exerciseId)
        }
        return id
    }

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
        val id = exerciseLogDao.insertExerciseLog(exerciseLog)
        // Always increment usage count when adding exercise through this method
        exerciseDao.incrementUsageCount(exercise.exercise.id)
        return id
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

    suspend fun completeWorkout(workoutId: Long, durationSeconds: Long? = null) {
        println("🔄 completeWorkout called with workoutId: $workoutId")
        val workout = workoutDao.getAllWorkouts().find { it.id == workoutId } ?: return
        println(
            "📊 Workout found: isProgrammeWorkout=${workout.isProgrammeWorkout}, " +
                "programmeId=${workout.programmeId}, week=${workout.weekNumber}, day=${workout.dayNumber}",
        )

        // Check if already completed
        if (workout.notes?.contains("[COMPLETED]") == true) {
            println("⚠️ Workout already marked as completed, skipping")
            return
        }

        val completedNotes =
            if (workout.notes != null) {
                "${workout.notes} [COMPLETED]"
            } else {
                "[COMPLETED]"
            }

        val updatedWorkout = workout.copy(notes = completedNotes, durationSeconds = durationSeconds)
        workoutDao.updateWorkout(updatedWorkout)
        println("✅ Workout marked as completed in database")

        // Update programme progress if this is a programme workout
        if (workout.isProgrammeWorkout && workout.programmeId != null && workout.weekNumber != null && workout.dayNumber != null) {
            println("🔄 This is a programme workout, updating progress...")
            updateProgrammeProgressAfterWorkout(workout.programmeId, workout.weekNumber, workout.dayNumber)
        } else {
            println("⚠️ Not a programme workout or missing week/day info, skipping progress update")
        }
    }

    private suspend fun updateProgrammeProgressAfterWorkout(
        programmeId: Long,
        weekNumber: Int?,
        dayNumber: Int?,
    ) {
        try {
            println(
                "🔄 updateProgrammeProgressAfterWorkout called: programmeId=$programmeId, " +
                    "week=$weekNumber, day=$dayNumber",
            )

            // Get current progress before incrementing
            val progressBefore = programmeDao.getProgressForProgramme(programmeId)
            println("📊 Progress BEFORE increment: completedWorkouts=${progressBefore?.completedWorkouts}/${progressBefore?.totalWorkouts}")

            // Always increment completed workouts when a workout is completed
            programmeDao.incrementCompletedWorkouts(programmeId)
            println("✅ Called incrementCompletedWorkouts")

            // Verify the increment worked
            val progressAfterIncrement = programmeDao.getProgressForProgramme(programmeId)
            println("📊 Progress AFTER increment: completedWorkouts=${progressAfterIncrement?.completedWorkouts}/${progressAfterIncrement?.totalWorkouts}")

            // Add additional debugging for programme details
            val programme = programmeDao.getProgrammeById(programmeId)
            if (programme != null) {
                println("📋 Programme details: name='${programme.name}', isCustom=${programme.isCustom}, durationWeeks=${programme.durationWeeks}")
                if (programme.isCustom) {
                    val allWorkouts = programmeDao.getAllWorkoutsForProgramme(programmeId)
                    println("📋 Custom programme has ${allWorkouts.size} total workouts in database")
                }
            }

            // Now find the next workout and update current week/day to that
            val nextWorkoutInfo = getNextProgrammeWorkout(programmeId)
            if (nextWorkoutInfo != null) {
                println("🎯 Next workout found: Week ${nextWorkoutInfo.actualWeekNumber}, Day ${nextWorkoutInfo.workoutStructure.day}")
                // Update progress to point to the next workout
                programmeDao.updateProgress(
                    programmeId,
                    nextWorkoutInfo.actualWeekNumber,
                    nextWorkoutInfo.workoutStructure.day,
                    LocalDateTime.now().toString(),
                )
            } else {
                println("🎆 No more workouts - programme complete!")
                // Double-check the completion logic before marking complete
                val finalProgress = programmeDao.getProgressForProgramme(programmeId)
                if (finalProgress != null) {
                    println("🔍 Final completion check: ${finalProgress.completedWorkouts} completed out of ${finalProgress.totalWorkouts} total")
                    if (finalProgress.completedWorkouts >= finalProgress.totalWorkouts) {
                        println("✅ Programme truly complete - marking as finished")
                        completeProgramme(programmeId)
                    } else {
                        println("⚠️ WARNING: getNextProgrammeWorkout returned null but programme not complete!")
                        println("   This suggests a bug in the next workout logic")
                    }
                } else {
                    println("❌ ERROR: No progress record found for completion check")
                }
            }

            // Update adherence percentage
            val progress = programmeDao.getProgressForProgramme(programmeId)
            if (progress != null) {
                val adherence =
                    if (progress.totalWorkouts > 0) {
                        (progress.completedWorkouts.toFloat() / progress.totalWorkouts.toFloat()) * 100f
                    } else {
                        0f
                    }
                println("📊 Final progress: ${progress.completedWorkouts}/${progress.totalWorkouts} = $adherence%")

                val updatedProgress =
                    progress.copy(
                        adherencePercentage = adherence,
                        lastWorkoutDate = LocalDateTime.now(),
                    )
                programmeDao.insertOrUpdateProgress(updatedProgress)
                println("✅ Updated adherence percentage and last workout date")
            } else {
                println("⚠️ WARNING: No progress record found after update!")
            }

            println("✅ Programme progress update completed")
        } catch (e: Exception) {
            println("❌ Failed to update programme progress: ${e.message}")
            e.printStackTrace()
        }
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

                // Get programme information if this is a programme workout
                val programmeName =
                    if (workout.isProgrammeWorkout && workout.programmeId != null) {
                        try {
                            programmeDao.getProgrammeById(workout.programmeId)?.name
                        } catch (e: Exception) {
                            null
                        }
                    } else {
                        null
                    }

                WorkoutSummary(
                    id = workout.id,
                    date = workout.date,
                    name = displayName,
                    exerciseCount = exercises.size,
                    setCount = allSets.size,
                    totalWeight = totalWeight,
                    // TODO: Calculate duration
                    duration = null,
                    isCompleted = isCompleted,
                    isProgrammeWorkout = workout.isProgrammeWorkout,
                    programmeId = workout.programmeId,
                    programmeName = programmeName,
                    programmeWorkoutName = workout.programmeWorkoutName,
                    weekNumber = workout.weekNumber,
                    dayNumber = workout.dayNumber,
                )
            }.sortedByDescending { it.date }
    }

    // Paginated history functionality for better performance
    suspend fun getWorkoutHistoryPaged(
        page: Int,
        pageSize: Int = 20,
    ): List<WorkoutSummary> {
        val offset = page * pageSize
        val allWorkouts = workoutDao.getAllWorkouts().sortedByDescending { it.date }

        println("🔍 PAGINATION DEBUG:")
        println("  📊 Total workouts in DB: ${allWorkouts.size}")
        println("  📄 Requesting page: $page, pageSize: $pageSize, offset: $offset")

        // PRE-FILTER workouts that have exercises BEFORE pagination
        val validWorkouts =
            allWorkouts.filter { workout ->
                val exercises = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
                exercises.isNotEmpty()
            }

        println("  ✅ Valid workouts (with exercises): ${validWorkouts.size}")

        // NOW paginate the valid workouts
        val pagedWorkouts = validWorkouts.drop(offset).take(pageSize)
        println("  📋 Workouts after pagination: ${pagedWorkouts.size}")

        val results =
            pagedWorkouts.map { workout ->
                val exercises = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
                // We know exercises is not empty because we pre-filtered

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

                println("  ✅ Including workout ${workout.id}: ${displayName ?: "Unnamed"}")
                // Get programme information if this is a programme workout
                val programmeName =
                    if (workout.isProgrammeWorkout && workout.programmeId != null) {
                        try {
                            programmeDao.getProgrammeById(workout.programmeId)?.name
                        } catch (e: Exception) {
                            null
                        }
                    } else {
                        null
                    }

                WorkoutSummary(
                    id = workout.id,
                    date = workout.date,
                    name = displayName,
                    exerciseCount = exercises.size,
                    setCount = allSets.size,
                    totalWeight = totalWeight,
                    // TODO: Calculate duration
                    duration = null,
                    isCompleted = isCompleted,
                    isProgrammeWorkout = workout.isProgrammeWorkout,
                    programmeId = workout.programmeId,
                    programmeName = programmeName,
                    programmeWorkoutName = workout.programmeWorkoutName,
                    weekNumber = workout.weekNumber,
                    dayNumber = workout.dayNumber,
                )
            }

        println("  📦 Final results returned: ${results.size}")
        return results
    }

    suspend fun getTotalWorkoutCount(): Int {
        val allWorkouts = workoutDao.getAllWorkouts()
        println("🔍 DEBUG: Checking all workouts:")
        allWorkouts.forEach { workout ->
            val exercises = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
            println("  Workout ${workout.id}: ${workout.notes ?: "Unnamed"} - ${exercises.size} exercises")
        }

        val count =
            allWorkouts.count { workout ->
                val exercises = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
                exercises.isNotEmpty()
            }
        println("🔍 getTotalWorkoutCount: $count workouts with exercises out of ${allWorkouts.size} total")
        return count
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

    // Update exercise order
    suspend fun updateExerciseOrder(exerciseLogId: Long, newOrder: Int) = exerciseLogDao.updateExerciseOrder(exerciseLogId, newOrder)

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
                        sets
                            .filter { it.isCompleted && it.reps > 0 }
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
                workout.date >= startDate &&
                    workout.date < endDate &&
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
                workoutDao
                    .getAllWorkouts()
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

    suspend fun getAverageTrainingDaysPerWeek(): Float =
        withContext(Dispatchers.IO) {
            val allWorkouts =
                workoutDao
                    .getAllWorkouts()
                    .filter { it.notes?.contains("[COMPLETED]") == true }
                    .sortedBy { it.date }

            if (allWorkouts.isEmpty()) return@withContext 0f

            // Find earliest and latest workout dates
            val firstWorkoutDate = allWorkouts.first().date
            val lastWorkoutDate = allWorkouts.last().date

            // Calculate weeks between first and last workout
            val daysBetween =
                java.time.temporal.ChronoUnit.DAYS
                    .between(firstWorkoutDate, lastWorkoutDate)
                    .toFloat()
            val weeksBetween = (daysBetween / 7f).coerceAtLeast(1f) // At least 1 week

            // Count unique training days
            val uniqueTrainingDays = allWorkouts.map { it.date.toLocalDate() }.distinct().size

            // Calculate average days per week
            (uniqueTrainingDays / weeksBetween).coerceIn(0f, 7f)
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

    // ===== PROGRAMME-WORKOUT INTEGRATION METHODS =====

    // Create a workout from a programme template with full exercise structure
    suspend fun createWorkoutFromProgrammeTemplate(
        programmeId: Long,
        weekNumber: Int,
        dayNumber: Int,
        userMaxes: Map<String, Float> = emptyMap(),
    ): Long =
        withContext(Dispatchers.IO) {
            try {
                println("🔧 createWorkoutFromProgrammeTemplate called:")
                println("  - programmeId: $programmeId")
                println("  - weekNumber: $weekNumber")
                println("  - dayNumber: $dayNumber")

                val programme =
                    programmeDao.getProgrammeById(programmeId)
                        ?: throw IllegalArgumentException("Programme not found")

                // Check if this is a custom programme (AI-generated or user-created)
                if (programme.isCustom) {
                    println("🤖 Handling custom programme workout creation")
                    return@withContext createWorkoutFromCustomProgramme(
                        programmeId = programmeId,
                        weekNumber = weekNumber,
                        dayNumber = dayNumber,
                        userMaxes = userMaxes
                    )
                }

                // Find the template this programme was created from
                val template =
                    programmeDao.getAllTemplates().find { it.name == programme.name }
                        ?: throw IllegalArgumentException("Programme template not found")

                // Parse the JSON structure
                val structure =
                    com.github.radupana.featherweight.data.programme.ProgrammeWorkoutParser.parseStructure(template.jsonStructure)
                        ?: throw IllegalArgumentException("Invalid programme structure")

                println("📚 Template has ${structure.weeks.size} week(s) defined")

                // For programmes with single week templates that repeat (like Starting Strength)
                // Use modulo to find the template week
                val templateWeekNumber =
                    if (structure.weeks.size == 1) {
                        1 // Always use week 1 if only one week is defined
                    } else {
                        ((weekNumber - 1) % structure.weeks.size) + 1
                    }

                println("🔄 Using template week $templateWeekNumber for actual week $weekNumber")

                // Find the specific workout for this week and day
                // All programmes MUST have sequential days (1,2,3,4...)
                val workoutStructure =
                    com.github.radupana.featherweight.data.programme.ProgrammeWorkoutParser.getWorkoutForWeekAndDay(
                        structure,
                        templateWeekNumber,
                        dayNumber,
                    ) ?: throw IllegalArgumentException("Workout not found for template week $templateWeekNumber, day $dayNumber")

                println("🏋️ Found workout: ${workoutStructure.name} with ${workoutStructure.exercises.size} exercises")

                // Create the workout entry
                // Store actual programme week, not template week
                val workout =
                    Workout(
                        date = LocalDateTime.now(),
                        programmeId = programmeId,
                        weekNumber = weekNumber,
                        dayNumber = dayNumber,
                        programmeWorkoutName = workoutStructure.name,
                        isProgrammeWorkout = true,
                        notes = null,
                    )
                val workoutId = workoutDao.insertWorkout(workout)

                // Create exercises and sets from the structure
                workoutStructure.exercises.forEachIndexed { exerciseIndex, exerciseStructure ->
                    val exerciseLog =
                        createExerciseLogFromStructure(
                            workoutId = workoutId,
                            exerciseStructure = exerciseStructure,
                            exerciseOrder = exerciseIndex,
                            userMaxes = userMaxes,
                        )
                    val exerciseLogId = exerciseLogDao.insertExerciseLog(exerciseLog)

                    // Create sets for this exercise
                    createSetsFromStructure(exerciseLogId, exerciseStructure, userMaxes)
                }

                println("✅ Created programme workout: ${workoutStructure.name} (Actual Week $weekNumber, Day $dayNumber)")
                workoutId
            } catch (e: Exception) {
                println("❌ Error creating workout from programme: ${e.message}")
                e.printStackTrace()
                throw e
            }
        }

    private suspend fun createWorkoutFromCustomProgramme(
        programmeId: Long,
        weekNumber: Int,
        dayNumber: Int,
        userMaxes: Map<String, Float> = emptyMap(),
    ): Long {
        println("🔧 createWorkoutFromCustomProgramme called:")
        println("  - programmeId: $programmeId")
        println("  - weekNumber: $weekNumber")
        println("  - dayNumber: $dayNumber")
        
        // Get the progress to find which workout to use
        val progress = programmeDao.getProgressForProgramme(programmeId)
            ?: throw IllegalArgumentException("Programme progress not found")
        
        // Get all workouts for this programme
        val allWorkouts = programmeDao.getAllWorkoutsForProgramme(programmeId)
        if (allWorkouts.isEmpty()) {
            throw IllegalArgumentException("No workouts found for custom programme")
        }
        
        // Find the workout index based on completed workouts
        val workoutIndex = progress.completedWorkouts
        if (workoutIndex >= allWorkouts.size) {
            throw IllegalArgumentException("All workouts already completed")
        }
        
        val programmeWorkout = allWorkouts[workoutIndex]
        println("📋 Using workout: ${programmeWorkout.name}")
        
        // Parse workout structure
        val workoutStructure = try {
            val json = kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
                isLenient = true
            }
            json.decodeFromString<com.github.radupana.featherweight.data.programme.WorkoutStructure>(
                programmeWorkout.workoutStructure
            )
        } catch (e: Exception) {
            // Try to fix invalid JSON
            val fixedJson = programmeWorkout.workoutStructure
                .replace("\"reps\":,", "\"reps\":\"8-12\",")
                .replace("\"reps\": ,", "\"reps\":\"8-12\",")
            
            try {
                val json = kotlinx.serialization.json.Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }
                json.decodeFromString<com.github.radupana.featherweight.data.programme.WorkoutStructure>(fixedJson)
            } catch (e2: Exception) {
                throw IllegalArgumentException("Failed to parse workout structure: ${e2.message}")
            }
        }
        
        // Create the workout entry
        val workout = Workout(
            date = LocalDateTime.now(),
            programmeId = programmeId,
            weekNumber = weekNumber,
            dayNumber = dayNumber,
            programmeWorkoutName = workoutStructure.name,
            isProgrammeWorkout = true,
            notes = null,
        )
        val workoutId = workoutDao.insertWorkout(workout)
        
        // Create exercises and sets from the structure
        workoutStructure.exercises.forEachIndexed { exerciseIndex, exerciseStructure ->
            val exerciseLog = createExerciseLogFromStructure(
                workoutId = workoutId,
                exerciseStructure = exerciseStructure,
                exerciseOrder = exerciseIndex,
                userMaxes = userMaxes,
            )
            val exerciseLogId = exerciseLogDao.insertExerciseLog(exerciseLog)
            
            // Create sets for this exercise
            createSetsFromStructure(exerciseLogId, exerciseStructure, userMaxes)
        }
        
        println("✅ Created custom programme workout: ${workoutStructure.name}")
        return workoutId
    }

    private suspend fun createExerciseLogFromStructure(
        workoutId: Long,
        exerciseStructure: com.github.radupana.featherweight.data.programme.ExerciseStructure,
        exerciseOrder: Int,
        userMaxes: Map<String, Float>,
    ): ExerciseLog {
        // Try to find existing exercise in database
        val exercise =
            try {
                searchExercises(exerciseStructure.name).firstOrNull()
            } catch (e: Exception) {
                null
            }

        // If exercise not found, create it as a custom exercise
        val exerciseId =
            if (exercise != null) {
                exercise.id
            } else {
                println("⚠️ Exercise '${exerciseStructure.name}' not found in database, creating as custom exercise")
                try {
                    // Create a generic custom exercise based on the name
                    val category =
                        when {
                            exerciseStructure.name.contains("Squat", ignoreCase = true) -> ExerciseCategory.LEGS
                            exerciseStructure.name.contains("Press", ignoreCase = true) -> ExerciseCategory.CHEST
                            exerciseStructure.name.contains("Row", ignoreCase = true) -> ExerciseCategory.BACK
                            exerciseStructure.name.contains("Deadlift", ignoreCase = true) -> ExerciseCategory.LEGS
                            exerciseStructure.name.contains("Curl", ignoreCase = true) -> ExerciseCategory.ARMS
                            exerciseStructure.name.contains("Lunge", ignoreCase = true) -> ExerciseCategory.LEGS
                            exerciseStructure.name.contains("Fly", ignoreCase = true) || exerciseStructure.name.contains("Flys", ignoreCase = true) -> ExerciseCategory.CHEST
                            exerciseStructure.name.contains("Pull", ignoreCase = true) -> ExerciseCategory.BACK
                            exerciseStructure.name.contains("Raise", ignoreCase = true) -> ExerciseCategory.SHOULDERS
                            exerciseStructure.name.contains("Extension", ignoreCase = true) -> ExerciseCategory.LEGS
                            exerciseStructure.name.contains("Thrust", ignoreCase = true) -> ExerciseCategory.LEGS
                            else -> ExerciseCategory.FULL_BODY
                        }

                    val equipment =
                        when {
                            exerciseStructure.name.contains("Barbell", ignoreCase = true) -> setOf(Equipment.BARBELL)
                            exerciseStructure.name.contains("Dumbbell", ignoreCase = true) -> setOf(Equipment.DUMBBELL)
                            exerciseStructure.name.contains("Cable", ignoreCase = true) -> setOf(Equipment.CABLE_MACHINE)
                            else -> setOf(Equipment.MACHINE)
                        }

                    createCustomExercise(
                        name = exerciseStructure.name,
                        category = category,
                        primaryMuscles = setOf(MuscleGroup.fromCategory(category)),
                        requiredEquipment = equipment,
                        userId = "programme_template",
                    )
                } catch (e: Exception) {
                    println("❌ Failed to create custom exercise: ${e.message}")
                    null
                }
            }

        val notes =
            buildString {
                if (exerciseStructure.note != null) {
                    append(exerciseStructure.note)
                }
                if (exerciseStructure.progression != "linear") {
                    if (isNotEmpty()) append(" | ")
                    append("Progression: ${exerciseStructure.progression}")
                }
                if (exerciseStructure.intensity?.isNotEmpty() == true) {
                    if (isNotEmpty()) append(" | ")
                    append("Intensities: ${exerciseStructure.intensity.joinToString(", ")}%")
                }
            }.takeIf { it.isNotEmpty() }

        return ExerciseLog(
            workoutId = workoutId,
            exerciseName = exerciseStructure.name,
            exerciseId = exerciseId,
            exerciseOrder = exerciseOrder,
            notes = notes,
        )
    }

    private suspend fun createSetsFromStructure(
        exerciseLogId: Long,
        exerciseStructure: com.github.radupana.featherweight.data.programme.ExerciseStructure,
        userMaxes: Map<String, Float>,
    ) {
        repeat(exerciseStructure.sets) { setIndex ->
            val reps =
                com.github.radupana.featherweight.data.programme.ProgrammeWorkoutParser.parseRepsForSet(
                    exerciseStructure.reps,
                    setIndex,
                )

            val intensity =
                exerciseStructure.intensity?.getOrNull(setIndex)
                    ?: exerciseStructure.intensity?.firstOrNull()

            val weight =
                com.github.radupana.featherweight.data.programme.ProgrammeWorkoutParser.calculateWeight(
                    exerciseName = exerciseStructure.name,
                    intensity = intensity,
                    userMaxes = userMaxes,
                    baseWeight = 45f,
                )

            val setLog =
                SetLog(
                    exerciseLogId = exerciseLogId,
                    setOrder = setIndex,
                    reps = reps,
                    weight = weight,
                    // User will fill this in
                    rpe = null,
                    isCompleted = false,
                    completedAt = null,
                )

            setLogDao.insertSetLog(setLog)
        }
    }

    // Create a simple workout from basic parameters (legacy method for compatibility)
    suspend fun createWorkoutFromProgramme(
        programmeId: Long,
        programmeWorkoutId: Long,
        weekNumber: Int,
        dayNumber: Int,
        workoutName: String,
    ): Long =
        withContext(Dispatchers.IO) {
            val workout =
                Workout(
                    date = LocalDateTime.now(),
                    programmeId = programmeId,
                    programmeWorkoutId = programmeWorkoutId,
                    weekNumber = weekNumber,
                    dayNumber = dayNumber,
                    programmeWorkoutName = workoutName,
                    isProgrammeWorkout = true,
                    notes = null,
                )
            workoutDao.insertWorkout(workout)
        }

    // Get workouts for a specific programme
    suspend fun getWorkoutsByProgramme(programmeId: Long): List<Workout> =
        withContext(Dispatchers.IO) {
            workoutDao.getWorkoutsByProgramme(programmeId)
        }

    // Get programme workouts for a specific week
    suspend fun getProgrammeWorkoutsByWeek(
        programmeId: Long,
        weekNumber: Int,
    ): List<Workout> =
        withContext(Dispatchers.IO) {
            workoutDao.getProgrammeWorkoutsByWeek(programmeId, weekNumber)
        }

    // Get programme workout progress
    suspend fun getProgrammeWorkoutProgress(programmeId: Long): Pair<Int, Int> =
        withContext(Dispatchers.IO) {
            // Get from the progress table which is the source of truth
            val progress = programmeDao.getProgressForProgramme(programmeId)
            if (progress != null) {
                println("📊 getProgrammeWorkoutProgress from progress table: ${progress.completedWorkouts}/${progress.totalWorkouts}")
                Pair(progress.completedWorkouts, progress.totalWorkouts)
            } else {
                // Fallback to counting workouts
                val completed = workoutDao.getCompletedProgrammeWorkoutCount(programmeId)
                val total = workoutDao.getTotalProgrammeWorkoutCount(programmeId)
                println("📊 getProgrammeWorkoutProgress from workout count: $completed/$total")
                Pair(completed, total)
            }
        }

    // Check if a specific programme workout has been completed
    suspend fun isProgrammeWorkoutCompleted(
        programmeId: Long,
        programmeWorkoutId: Long,
    ): Boolean =
        withContext(Dispatchers.IO) {
            val lastExecution = workoutDao.getLastProgrammeWorkoutExecution(programmeId, programmeWorkoutId)
            lastExecution?.notes?.contains("[COMPLETED]") == true
        }

    // Get next programme workout to do
    suspend fun getNextProgrammeWorkout(programmeId: Long): NextProgrammeWorkoutInfo? =
        withContext(Dispatchers.IO) {
            println("🔄 getNextProgrammeWorkout called for programmeId: $programmeId")

            val programme = programmeDao.getProgrammeById(programmeId)
            if (programme == null) {
                println("❌ Programme not found for id: $programmeId")
                return@withContext null
            }
            
            val progress = programmeDao.getProgressForProgramme(programmeId)
            if (progress == null) {
                println("❌ Progress not found for programmeId: $programmeId")
                return@withContext null
            }

            println(
                "📊 Current progress: week=${progress.currentWeek}, day=${progress.currentDay}, " +
                    "completed=${progress.completedWorkouts}/${progress.totalWorkouts}",
            )

            // Check if programme is actually complete
            if (progress.completedWorkouts >= progress.totalWorkouts) {
                println("✅ Programme is complete (${progress.completedWorkouts} >= ${progress.totalWorkouts})")
                return@withContext null
            }
            
            // Add debug info for completion check
            val remainingWorkouts = progress.totalWorkouts - progress.completedWorkouts
            println("📊 Remaining workouts: $remainingWorkouts (${progress.completedWorkouts}/${progress.totalWorkouts} completed)")

            // Check if this is a custom programme (AI-generated or user-created) with direct workout storage
            if (programme.isCustom) {
                println("🤖 Handling custom programme (AI-generated or user-created)")
                println("   Programme: ${programme.name}")
                println("   Is custom: ${programme.isCustom}")
                println("   Progress: completedWorkouts=${progress.completedWorkouts}, totalWorkouts=${progress.totalWorkouts}")
                return@withContext getNextWorkoutFromDatabase(programmeId, progress)
            }

            // Handle template-based programmes (existing logic)
            val template =
                programmeDao.getAllTemplates().find { it.name == programme.name }
                    ?: return@withContext null

            // Parse the JSON structure
            val structure =
                com.github.radupana.featherweight.data.programme.ProgrammeWorkoutParser.parseStructure(template.jsonStructure)
                    ?: return@withContext null

            // Calculate workouts per week from template structure
            val workoutsPerWeek = structure.weeks.flatMap { it.workouts }.size / structure.weeks.size
            println("📊 Programme has $workoutsPerWeek workouts per week")

            // Calculate the next workout position based on completed workouts
            val nextWorkoutIndex = progress.completedWorkouts
            val nextWeek = (nextWorkoutIndex / workoutsPerWeek) + 1
            val nextDay = (nextWorkoutIndex % workoutsPerWeek) + 1

            println("🎯 Next workout should be: Week $nextWeek, Day $nextDay (based on ${progress.completedWorkouts} completed)")

            // Make sure we haven't exceeded programme duration
            if (nextWeek > programme.durationWeeks) {
                println("✅ Programme complete - exceeded duration weeks")
                return@withContext null
            }

            // Get the workout structure for this position
            val templateWeekIndex = ((nextWeek - 1) % structure.weeks.size) + 1
            val workoutStructure =
                com.github.radupana.featherweight.data.programme.ProgrammeWorkoutParser.getWorkoutForWeekAndDay(
                    structure,
                    templateWeekIndex,
                    nextDay,
                ) ?: return@withContext null

            println("💫 Found workout: ${workoutStructure.name} on day ${workoutStructure.day}")

            // Return the workout info
            return@withContext NextProgrammeWorkoutInfo(
                workoutStructure = workoutStructure,
                actualWeekNumber = nextWeek,
            )
        }

    // Handle next workout for custom programmes (AI-generated or user-created)
    private suspend fun getNextWorkoutFromDatabase(programmeId: Long, progress: com.github.radupana.featherweight.data.programme.ProgrammeProgress): NextProgrammeWorkoutInfo? {
        println("🔍 Looking up next workout from database for custom programme")
        
        // Get all workouts for this programme, ordered by week and day
        val allWorkouts = programmeDao.getAllWorkoutsForProgramme(programmeId)
        if (allWorkouts.isEmpty()) {
            println("❌ No workouts found for programme $programmeId")
            return null
        }
        
        println("📋 Found ${allWorkouts.size} total workouts for programme")
        println("📊 Progress tracking: completedWorkouts=${progress.completedWorkouts}, totalWorkouts=${progress.totalWorkouts}")
        
        // Find the next workout based on completed workouts count
        val nextWorkoutIndex = progress.completedWorkouts
        println("🎯 Looking for workout at index $nextWorkoutIndex (0-based)")
        
        if (nextWorkoutIndex >= allWorkouts.size) {
            println("✅ All workouts completed ($nextWorkoutIndex >= ${allWorkouts.size})")
            return null
        }
        
        val nextWorkout = allWorkouts[nextWorkoutIndex]
        println("🎯 Next workout: ${nextWorkout.name} (weekId ${nextWorkout.weekId}, day ${nextWorkout.dayNumber})")
        
        // Additional debug info about all workouts
        println("📋 All workouts for debugging:")
        allWorkouts.forEachIndexed { index, workout ->
            val status = if (index < progress.completedWorkouts) "✅ COMPLETED" else if (index == progress.completedWorkouts) "➡️ NEXT" else "⏳ PENDING"
            println("   [$index] ${workout.name} (Week ${workout.weekId}, Day ${workout.dayNumber}) $status")
        }
        
        // Parse the workout structure from JSON with proper configuration
        val workoutStructure = try {
            val json = kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
                isLenient = true
            }
            json.decodeFromString<com.github.radupana.featherweight.data.programme.WorkoutStructure>(
                nextWorkout.workoutStructure
            )
        } catch (e: Exception) {
            println("❌ Failed to parse workout structure: ${e.message}")
            println("   JSON content: ${nextWorkout.workoutStructure}")
            
            // Try to fix invalid JSON by replacing ,"reps":, with ,"reps":"8-12",
            val fixedJson = nextWorkout.workoutStructure
                .replace("\"reps\":,", "\"reps\":\"8-12\",")
                .replace("\"reps\": ,", "\"reps\":\"8-12\",")
            
            println("🔧 Attempting to fix invalid JSON...")
            try {
                val json = kotlinx.serialization.json.Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }
                json.decodeFromString<com.github.radupana.featherweight.data.programme.WorkoutStructure>(fixedJson)
            } catch (e2: Exception) {
                println("❌ Still failed after JSON fix attempt: ${e2.message}")
                e2.printStackTrace()
                return null
            }
        }
        
        // Get the week number from the workout's week
        val week = programmeDao.getWeekById(nextWorkout.weekId)
        val actualWeekNumber = week?.weekNumber ?: 1
        
        println("💫 Returning workout: ${workoutStructure.name} for actual week $actualWeekNumber")
        println("   Workout structure: day=${workoutStructure.day}, exercises=${workoutStructure.exercises.size}")
        
        return NextProgrammeWorkoutInfo(
            workoutStructure = workoutStructure,
            actualWeekNumber = actualWeekNumber
        )
    }
    
    // Helper to get daily workout count for custom programmes
    private suspend fun getDailyWorkoutCount(programmeId: Long): Int {
        val weeks = programmeDao.getWeeksForProgramme(programmeId)
        if (weeks.isEmpty()) return 3 // fallback
        
        // Get the first week's workout count as representative
        val firstWeekWorkouts = programmeDao.getWorkoutsForWeek(weeks[0].id)
        return firstWeekWorkouts.size.coerceAtLeast(1)
    }

    // Calculate total workouts for a programme based on its JSON structure
    private suspend fun calculateTotalWorkoutsFromStructure(programme: com.github.radupana.featherweight.data.programme.Programme): Int {
        return try {
            // Check if this is a custom programme (AI-generated or user-created)
            if (programme.isCustom) {
                // For custom programmes, count actual workouts in the database
                val allWorkouts = programmeDao.getAllWorkoutsForProgramme(programme.id)
                val totalWorkouts = allWorkouts.size
                
                println("📊 Custom programme workout count:")
                println("  - Programme: ${programme.name}")
                println("  - Total workouts in database: $totalWorkouts")
                println("  - Programme duration: ${programme.durationWeeks} weeks")
                
                // Additional validation - make sure we have reasonable number of workouts
                if (totalWorkouts == 0) {
                    println("⚠️ WARNING: Custom programme has 0 workouts!")
                } else if (totalWorkouts < programme.durationWeeks) {
                    println("⚠️ WARNING: Custom programme has fewer workouts ($totalWorkouts) than weeks (${programme.durationWeeks})")
                }
                
                return totalWorkouts
            }
            
            // For template-based programmes, use existing logic
            val template =
                programmeDao.getAllTemplates().find { it.name == programme.name }
                    ?: return programme.durationWeeks * 3 // Fallback to default

            // Parse the JSON structure
            val structure =
                com.github.radupana.featherweight.data.programme.ProgrammeWorkoutParser.parseStructure(template.jsonStructure)
                    ?: return programme.durationWeeks * 3 // Fallback to default

            // Calculate workouts per week based on the ACTUAL number of workouts in the template
            val totalWorkoutsInTemplate = structure.weeks.sumOf { it.workouts.size }
            val templateWeeks = structure.weeks.size
            val workoutsPerWeek = totalWorkoutsInTemplate.toFloat() / templateWeeks.toFloat()

            // Total workouts = workouts per week * programme duration
            val totalWorkouts = (workoutsPerWeek * programme.durationWeeks).toInt()

            println("📊 Programme structure analysis:")
            println("  - Template has $totalWorkoutsInTemplate workouts across $templateWeeks week(s)")
            println("  - Workouts per week: $workoutsPerWeek")
            println("  - Programme duration: ${programme.durationWeeks} weeks")
            println("  - Total calculated workouts: $totalWorkouts")

            totalWorkouts
        } catch (e: Exception) {
            println("❌ Error calculating total workouts: ${e.message}")
            programme.durationWeeks * 3 // Fallback to default
        }
    }

    // ===== PROGRAMME METHODS =====

    // Programme Templates
    suspend fun getAllProgrammeTemplates() =
        withContext(Dispatchers.IO) {
            programmeDao.getAllTemplates()
        }

    suspend fun getTemplatesByDifficulty(difficulty: com.github.radupana.featherweight.data.programme.ProgrammeDifficulty) =
        withContext(Dispatchers.IO) {
            programmeDao.getTemplatesByDifficulty(difficulty)
        }

    suspend fun getTemplateById(id: Long) =
        withContext(Dispatchers.IO) {
            programmeDao.getTemplateById(id)
        }

    // Programme Management
    suspend fun createProgrammeFromTemplate(
        templateId: Long,
        name: String? = null,
        squatMax: Float? = null,
        benchMax: Float? = null,
        deadliftMax: Float? = null,
        ohpMax: Float? = null,
        accessoryCustomizations: Map<String, String> = emptyMap(),
    ): Long =
        withContext(Dispatchers.IO) {
            val template =
                programmeDao.getTemplateById(templateId)
                    ?: throw IllegalArgumentException("Template not found")

            val programme =
                com.github.radupana.featherweight.data.programme.Programme(
                    name = name ?: template.name,
                    description = template.description,
                    durationWeeks = template.durationWeeks,
                    programmeType = template.programmeType,
                    difficulty = template.difficulty,
                    isCustom = false,
                    squatMax = squatMax,
                    benchMax = benchMax,
                    deadliftMax = deadliftMax,
                    ohpMax = ohpMax,
                )

            val programmeId = programmeDao.insertProgramme(programme)

            // TODO: Parse JSON structure and create weeks/workouts
            // For now, just return the programme ID
            programmeId
        }
    
    suspend fun createAIGeneratedProgramme(preview: com.github.radupana.featherweight.data.GeneratedProgrammePreview): Long =
        withContext(Dispatchers.IO) {
            // Deactivate any currently active programme
            programmeDao.deactivateAllProgrammes()
            
            // Create the main programme
            val programme = com.github.radupana.featherweight.data.programme.Programme(
                name = preview.name,
                description = preview.description,
                durationWeeks = preview.durationWeeks,
                programmeType = com.github.radupana.featherweight.data.programme.ProgrammeType.GENERAL_FITNESS,
                difficulty = when (preview.volumeLevel) {
                    com.github.radupana.featherweight.data.VolumeLevel.LOW -> com.github.radupana.featherweight.data.programme.ProgrammeDifficulty.BEGINNER
                    com.github.radupana.featherweight.data.VolumeLevel.MODERATE -> com.github.radupana.featherweight.data.programme.ProgrammeDifficulty.INTERMEDIATE
                    com.github.radupana.featherweight.data.VolumeLevel.HIGH, com.github.radupana.featherweight.data.VolumeLevel.VERY_HIGH -> com.github.radupana.featherweight.data.programme.ProgrammeDifficulty.ADVANCED
                },
                isCustom = true,
                isActive = true,
                startedAt = java.time.LocalDateTime.now()
            )

            val programmeId = programmeDao.insertProgramme(programme)

            // Create weeks and workouts
            preview.weeks.forEach { weekPreview ->
                val week = com.github.radupana.featherweight.data.programme.ProgrammeWeek(
                    programmeId = programmeId,
                    weekNumber = weekPreview.weekNumber,
                    name = "Week ${weekPreview.weekNumber}",
                    description = weekPreview.progressionNotes,
                    focusAreas = null
                )
                
                val weekId = programmeDao.insertProgrammeWeek(week)
                
                // Create workouts for this week
                weekPreview.workouts.forEach { workoutPreview ->
                    // Build workout structure JSON
                    val workoutStructure = com.github.radupana.featherweight.data.programme.WorkoutStructure(
                        day = workoutPreview.dayNumber,
                        name = workoutPreview.name,
                        exercises = workoutPreview.exercises.map { exercisePreview ->
                            com.github.radupana.featherweight.data.programme.ExerciseStructure(
                                name = exercisePreview.exerciseName,
                                sets = exercisePreview.sets,
                                reps = com.github.radupana.featherweight.data.programme.RepsStructure.Range(
                                    min = exercisePreview.repsMin,
                                    max = exercisePreview.repsMax
                                ),
                                note = exercisePreview.notes
                            )
                        },
                        estimatedDuration = workoutPreview.estimatedDuration
                    )
                    
                    val workout = com.github.radupana.featherweight.data.programme.ProgrammeWorkout(
                        weekId = weekId,
                        dayNumber = workoutPreview.dayNumber,
                        name = workoutPreview.name,
                        description = null,
                        estimatedDuration = workoutPreview.estimatedDuration,
                        workoutStructure = kotlinx.serialization.json.Json {
                            ignoreUnknownKeys = true
                            isLenient = true
                        }.encodeToString(
                            com.github.radupana.featherweight.data.programme.WorkoutStructure.serializer(),
                            workoutStructure
                        )
                    )
                    
                    programmeDao.insertProgrammeWorkout(workout)
                }
            }

            // Initialize progress tracking for the new programme
            val totalWorkouts = preview.weeks.sumOf { it.workouts.size }
            val progress = com.github.radupana.featherweight.data.programme.ProgrammeProgress(
                programmeId = programmeId,
                currentWeek = 1,
                currentDay = 1,
                completedWorkouts = 0,
                totalWorkouts = totalWorkouts,
                lastWorkoutDate = null,
                adherencePercentage = 0f,
                strengthProgress = null
            )
            
            println("🔄 Initializing progress for AI-generated programme...")
            println("  - Programme ID: $programmeId")
            println("  - Total workouts: $totalWorkouts")
            programmeDao.insertOrUpdateProgress(progress)
            println("✅ Progress tracking initialized")

            programmeId
        }

    suspend fun getAllProgrammes() =
        withContext(Dispatchers.IO) {
            programmeDao.getAllProgrammes()
        }

    suspend fun getActiveProgramme() =
        withContext(Dispatchers.IO) {
            programmeDao.getActiveProgramme()
        }

    suspend fun getProgrammeById(programmeId: Long) =
        withContext(Dispatchers.IO) {
            programmeDao.getProgrammeById(programmeId)
        }

    suspend fun getProgrammeWithDetails(programmeId: Long) =
        withContext(Dispatchers.IO) {
            programmeDao.getProgrammeWithDetails(programmeId)
        }

    suspend fun activateProgramme(programmeId: Long) =
        withContext(Dispatchers.IO) {
            try {
                println("🔄 Setting programme as active...")
                programmeDao.setActiveProgramme(programmeId)
                println("✅ Programme marked as active")

                // Initialize progress tracking
                println("🔄 Getting programme details...")
                val programme = programmeDao.getProgrammeById(programmeId) ?: return@withContext
                println("✅ Programme found: ${programme.name}")

                // Calculate total workouts based on actual JSON structure
                val totalWorkouts = calculateTotalWorkoutsFromStructure(programme)
                println("📊 Calculated total workouts: $totalWorkouts")

                val progress =
                    com.github.radupana.featherweight.data.programme.ProgrammeProgress(
                        programmeId = programmeId,
                        currentWeek = 1,
                        currentDay = 1,
                        completedWorkouts = 0,
                        totalWorkouts = totalWorkouts,
                        lastWorkoutDate = null,
                        adherencePercentage = 0f,
                        strengthProgress = null,
                    )

                println("🔄 Inserting progress tracking...")
                programmeDao.insertOrUpdateProgress(progress)
                println("✅ Progress tracking initialized")
            } catch (e: Exception) {
                println("❌ Error in activateProgramme: ${e.message}")
                throw e
            }
        }

    suspend fun deactivateActiveProgramme() =
        withContext(Dispatchers.IO) {
            programmeDao.deactivateAllProgrammes()
        }

    suspend fun completeProgramme(programmeId: Long) =
        withContext(Dispatchers.IO) {
            println("🏁 completeProgramme called for programmeId: $programmeId")
            
            // Get programme details for logging
            val programme = programmeDao.getProgrammeById(programmeId)
            val progress = programmeDao.getProgressForProgramme(programmeId)
            
            if (programme != null && progress != null) {
                println("🏁 Marking programme as complete:")
                println("   Programme: ${programme.name}")
                println("   Progress: ${progress.completedWorkouts}/${progress.totalWorkouts} workouts")
                println("   Adherence: ${progress.adherencePercentage}%")
            }
            
            programmeDao.completeProgramme(programmeId, LocalDateTime.now().toString())
            programmeDao.deactivateAllProgrammes()
            println("✅ Programme marked as complete and deactivated")
        }

    suspend fun deleteProgramme(programme: com.github.radupana.featherweight.data.programme.Programme) =
        withContext(Dispatchers.IO) {
            // First deactivate the programme if it's active
            if (programme.isActive) {
                programmeDao.deactivateAllProgrammes()
            }
            // Then delete the programme (will cascade delete progress and related data)
            programmeDao.deleteProgramme(programme)
        }

    // Programme Progress
    suspend fun updateProgrammeProgress(
        programmeId: Long,
        week: Int,
        day: Int,
    ) = withContext(Dispatchers.IO) {
        println("🔄 updateProgrammeProgress called: programmeId=$programmeId, week=$week, day=$day")

        // First check if progress record exists
        var progress = programmeDao.getProgressForProgramme(programmeId)
        println("📊 Initial progress check: $progress")

        if (progress == null) {
            println("⚠️ No progress record found, creating one...")
            // Create a new progress record if it doesn't exist
            val programme = programmeDao.getProgrammeById(programmeId)
            if (programme != null) {
                val totalWorkouts = calculateTotalWorkoutsFromStructure(programme)
                progress =
                    com.github.radupana.featherweight.data.programme.ProgrammeProgress(
                        programmeId = programmeId,
                        currentWeek = week,
                        currentDay = day,
                        completedWorkouts = 0,
                        totalWorkouts = totalWorkouts,
                        lastWorkoutDate = LocalDateTime.now(),
                        adherencePercentage = 0f,
                        strengthProgress = null,
                    )
                programmeDao.insertOrUpdateProgress(progress)
                println("✅ Created new progress record")
            } else {
                println("❌ Programme not found, cannot create progress record")
                return@withContext
            }
        } else {
            // Update current week/day and timestamp
            // This is called BEFORE the workout starts, so we're setting the next workout to do
            programmeDao.updateProgress(programmeId, week, day, LocalDateTime.now().toString())
            println("✅ Updated current week/day in progress table to: week=$week, day=$day")
        }
    }

    // Exercise Substitutions
    suspend fun addExerciseSubstitution(
        programmeId: Long,
        originalExercise: String,
        substitutionExercise: String,
    ) = withContext(Dispatchers.IO) {
        val exercise = getExerciseByName(substitutionExercise)
        val substitution =
            com.github.radupana.featherweight.data.programme.ExerciseSubstitution(
                programmeId = programmeId,
                originalExerciseName = originalExercise,
                substitutionCategory = exercise?.category ?: com.github.radupana.featherweight.data.exercise.ExerciseCategory.FULL_BODY,
                substitutionCriteria = null,
                isUserDefined = true,
            )
        programmeDao.insertSubstitution(substitution)
    }

    suspend fun getExerciseSubstitutions(
        programmeId: Long,
        exerciseName: String,
    ) = withContext(Dispatchers.IO) {
        programmeDao.getSubstitutionsForExercise(programmeId, exerciseName)
    }

    // Helper method to get exercise by name
    private suspend fun getExerciseByName(name: String): com.github.radupana.featherweight.data.exercise.Exercise? {
        return exerciseDao.getAllExercisesWithDetails().find { it.exercise.name == name }?.exercise
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
        createAccessoryExercise(workoutId, "Bulgarian Split Squat", 2, date, (20f..30f), (8..12), 3)
        createAccessoryExercise(workoutId, "Pull-up", 3, date, (0f..10f), (5..10), 3)
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
        createAccessoryExercise(workoutId, "Lateral Raise", 3, date, (8f..15f), (12..15), 4)
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
        createAccessoryExercise(workoutId, "Front Squat", 1, date, (60f..80f), (6..8), 3)
        createAccessoryExercise(workoutId, "Barbell Curl", 2, date, (25f..35f), (8..12), 3)
        createAccessoryExercise(workoutId, "Hanging Leg Raise", 3, date, (0f..10f), (8..15), 3)
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
        createAccessoryExercise(workoutId, "Cable Fly", 2, date, (15f..25f), (10..15), 3)
        createAccessoryExercise(workoutId, "Dumbbell Curl", 3, date, (12f..20f), (10..15), 3)
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
                if (exerciseName.contains("Pull-up") || exerciseName.contains("Hanging")) {
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
    private fun ClosedFloatingPointRange<Float>.random(): Float = start + (endInclusive - start) * kotlin.random.Random.nextFloat()

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

    // ========== Profile & 1RM Management ==========

    suspend fun ensureUserProfile(userId: Long) =
        withContext(Dispatchers.IO) {
            val profile = db.profileDao().getUserProfile(userId)
            if (profile == null) {
                // Should not happen as users are created through seedTestUsers
                throw IllegalStateException("User profile not found for ID: $userId")
            }
        }

    fun getUserProfileFlow() = db.profileDao().getUserProfileFlow(getCurrentUserId())

    suspend fun getUserProfile() =
        withContext(Dispatchers.IO) {
            db.profileDao().getUserProfile(getCurrentUserId())
        }

    suspend fun getAllUsers() =
        withContext(Dispatchers.IO) {
            db.profileDao().getAllUsers()
        }

    suspend fun upsertExerciseMax(
        exerciseId: Long,
        maxWeight: Float,
        isEstimated: Boolean = false,
        notes: String? = null,
    ) = withContext(Dispatchers.IO) {
        val userId = getCurrentUserId()
        ensureUserProfile(userId)
        db.profileDao().upsertExerciseMax(
            userId = userId,
            exerciseId = exerciseId,
            maxWeight = maxWeight,
            isEstimated = isEstimated,
            notes = notes,
        )
    }

    fun getAllCurrentMaxes() = db.profileDao().getAllCurrentMaxes(getCurrentUserId())

    fun getCurrentMaxFlow(exerciseId: Long) =
        db.profileDao().getCurrentMaxFlow(
            userId = getCurrentUserId(),
            exerciseId = exerciseId,
        )

    suspend fun getCurrentMax(exerciseId: Long) =
        withContext(Dispatchers.IO) {
            db.profileDao().getCurrentMax(userId = getCurrentUserId(), exerciseId = exerciseId)
        }

    fun getMaxHistory(exerciseId: Long) =
        db.profileDao().getMaxHistory(
            userId = getCurrentUserId(),
            exerciseId = exerciseId,
        )

    suspend fun getBig4Exercises() =
        withContext(Dispatchers.IO) {
            db.profileDao().getBig4Exercises()
        }

    suspend fun deleteExerciseMax(max: com.github.radupana.featherweight.data.profile.UserExerciseMax) =
        withContext(Dispatchers.IO) {
            db.profileDao().deleteExerciseMax(max)
        }

    // Calculate percentage of 1RM for a given weight
    fun calculatePercentageOf1RM(
        weight: Float,
        oneRepMax: Float,
    ): Int {
        return if (oneRepMax > 0) {
            ((weight / oneRepMax) * 100).toInt()
        } else {
            0
        }
    }

    // Check if a weight would be a new 1RM
    suspend fun isNew1RM(
        exerciseId: Long,
        weight: Float,
    ): Boolean =
        withContext(Dispatchers.IO) {
            val currentMax = getCurrentMax(exerciseId)
            currentMax == null || weight > currentMax.maxWeight
        }

    // ========== Test User Management ==========

    suspend fun seedTestUsers() =
        withContext(Dispatchers.IO) {
            val existingUsers = db.profileDao().getAllUsers()

            if (existingUsers.isEmpty()) {
                // Create test users
                val user1Id =
                    db.profileDao().insertUserProfile(
                        com.github.radupana.featherweight.data.profile.UserProfile(
                            username = "user1",
                            displayName = "Alex Johnson",
                            avatarEmoji = "💪",
                        ),
                    )

                val user2Id =
                    db.profileDao().insertUserProfile(
                        com.github.radupana.featherweight.data.profile.UserProfile(
                            username = "user2",
                            displayName = "Sam Williams",
                            avatarEmoji = "🏋️",
                        ),
                    )

                // Seed some 1RMs for both users
                val big4 = getBig4Exercises()

                // User 1 - Intermediate lifter
                big4.forEach { exercise ->
                    val maxWeight =
                        when (exercise.name) {
                            "Back Squat" -> 140f
                            "Deadlift" -> 180f
                            "Bench Press" -> 100f
                            "Overhead Press" -> 60f
                            else -> 0f
                        }
                    if (maxWeight > 0) {
                        db.profileDao().insertExerciseMax(
                            com.github.radupana.featherweight.data.profile.UserExerciseMax(
                                userId = user1Id,
                                exerciseId = exercise.id,
                                maxWeight = maxWeight,
                                isEstimated = false,
                            ),
                        )
                    }
                }

                // User 2 - Beginner lifter
                big4.forEach { exercise ->
                    val maxWeight =
                        when (exercise.name) {
                            "Back Squat" -> 80f
                            "Deadlift" -> 100f
                            "Bench Press" -> 60f
                            "Overhead Press" -> 40f
                            else -> 0f
                        }
                    if (maxWeight > 0) {
                        db.profileDao().insertExerciseMax(
                            com.github.radupana.featherweight.data.profile.UserExerciseMax(
                                userId = user2Id,
                                exerciseId = exercise.id,
                                maxWeight = maxWeight,
                                isEstimated = false,
                            ),
                        )
                    }
                }

                println("✅ Seeded test users with 1RMs")
            }
        }
}
