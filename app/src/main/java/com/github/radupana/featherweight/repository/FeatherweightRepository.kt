package com.github.radupana.featherweight.repository

import android.app.Application
import android.util.Log
import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.GlobalExerciseProgress
import com.github.radupana.featherweight.data.ParseRequest
import com.github.radupana.featherweight.data.ParseStatus
import com.github.radupana.featherweight.data.PendingOneRMUpdate
import com.github.radupana.featherweight.data.PersonalRecord
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.SwapHistoryCount
import com.github.radupana.featherweight.data.TrainingAnalysis
import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.data.WorkoutStatus
import com.github.radupana.featherweight.data.exercise.Equipment
import com.github.radupana.featherweight.data.exercise.ExerciseCategory
import com.github.radupana.featherweight.data.exercise.ExerciseCorrelationSeeder
import com.github.radupana.featherweight.data.exercise.ExerciseDifficulty
import com.github.radupana.featherweight.data.exercise.ExerciseSeeder
import com.github.radupana.featherweight.data.exercise.ExerciseVariation
import com.github.radupana.featherweight.data.exercise.MovementPattern
import com.github.radupana.featherweight.data.exercise.MuscleGroup
import com.github.radupana.featherweight.data.exercise.VariationAlias
import com.github.radupana.featherweight.data.exercise.VariationMuscle
import com.github.radupana.featherweight.data.profile.OneRMHistory
import com.github.radupana.featherweight.data.profile.OneRMType
import com.github.radupana.featherweight.data.profile.OneRMWithExerciseName
import com.github.radupana.featherweight.data.profile.UserExerciseMax
import com.github.radupana.featherweight.data.programme.ExerciseFrequency
import com.github.radupana.featherweight.data.programme.ExerciseStructure
import com.github.radupana.featherweight.data.programme.Programme
import com.github.radupana.featherweight.data.programme.ProgrammeCompletionStats
import com.github.radupana.featherweight.data.programme.ProgrammeDifficulty
import com.github.radupana.featherweight.data.programme.ProgrammeInsights
import com.github.radupana.featherweight.data.programme.ProgrammeProgress
import com.github.radupana.featherweight.data.programme.ProgrammeStatus
import com.github.radupana.featherweight.data.programme.ProgrammeType
import com.github.radupana.featherweight.data.programme.ProgrammeWeek
import com.github.radupana.featherweight.data.programme.ProgrammeWorkout
import com.github.radupana.featherweight.data.programme.RepsStructure
import com.github.radupana.featherweight.data.programme.StrengthImprovement
import com.github.radupana.featherweight.data.programme.WorkoutStructure
import com.github.radupana.featherweight.domain.ExerciseHistory
import com.github.radupana.featherweight.domain.ExerciseStats
import com.github.radupana.featherweight.domain.ProgrammeHistoryDetails
import com.github.radupana.featherweight.domain.ProgrammeSummary
import com.github.radupana.featherweight.domain.ProgrammeWeekHistory
import com.github.radupana.featherweight.domain.WorkoutDayInfo
import com.github.radupana.featherweight.domain.WorkoutHistoryDetail
import com.github.radupana.featherweight.domain.WorkoutHistoryEntry
import com.github.radupana.featherweight.domain.WorkoutSummary
import com.github.radupana.featherweight.service.GlobalProgressTracker
import com.github.radupana.featherweight.service.ProgressionService
import com.github.radupana.featherweight.util.WeightFormatter
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

class FeatherweightRepository(
    application: Application,
) {
    companion object {
        private const val TAG = "FeatherweightRepository"
    }

    private val db = FeatherweightDatabase.getDatabase(application)

    // Sub-repositories
    private val exerciseRepository = ExerciseRepository(db)
    private val oneRMRepository = OneRMRepository(application)
    private val programmeRepository = ProgrammeRepository(application)
    private val workoutRepository = WorkoutRepository(application)
    private val personalRecordRepository = PersonalRecordRepository(application)

    private val workoutDao = db.workoutDao()
    private val exerciseLogDao = db.exerciseLogDao()
    private val setLogDao = db.setLogDao()
    private val exerciseDao = db.exerciseDao()
    private val exerciseVariationDao = db.exerciseVariationDao()
    private val exerciseCoreDao = db.exerciseCoreDao()
    private val variationMuscleDao = db.variationMuscleDao()
    private val variationAliasDao = db.variationAliasDao()
    private val variationInstructionDao = db.variationInstructionDao()
    private val programmeDao = db.programmeDao()

    private val globalExerciseProgressDao = db.globalExerciseProgressDao()
    private val exerciseCorrelationDao = db.exerciseCorrelationDao()
    private val personalRecordDao = db.personalRecordDao()

    // Initialize GlobalProgressTracker
    private val globalProgressTracker = GlobalProgressTracker(this, db)

    // Delegate OneRM functionality to OneRMRepository
    val pendingOneRMUpdates: StateFlow<List<PendingOneRMUpdate>> = oneRMRepository.pendingOneRMUpdates

    fun clearPendingOneRMUpdates() = oneRMRepository.clearPendingOneRMUpdates()

    suspend fun applyOneRMUpdate(update: PendingOneRMUpdate) = oneRMRepository.applyOneRMUpdate(update)

    private val exerciseSeeder =
        ExerciseSeeder(
            exerciseCoreDao = exerciseCoreDao,
            exerciseVariationDao = exerciseVariationDao,
            variationMuscleDao = variationMuscleDao,
            variationAliasDao = variationAliasDao,
            variationInstructionDao = variationInstructionDao,
            context = application,
        )

    private val exerciseCorrelationSeeder = ExerciseCorrelationSeeder(exerciseCorrelationDao)

    // Initialize with seed data for testing
    suspend fun seedDatabaseIfEmpty() {
        withContext(Dispatchers.IO) {
            val exerciseCount = exerciseDao.getAllExercisesWithDetails().size

            // Always seed exercises if there are none
            if (exerciseCount == 0) {
                exerciseSeeder.seedExercises()
            }

            // Seed exercise correlations if none exist
            if (exerciseCorrelationDao.getCount() == 0) {
                exerciseCorrelationSeeder.seedExerciseCorrelations()
            }
        }
    }

    // ===== EXERCISE METHODS (Delegated to ExerciseRepository) =====

    // Exercise methods - delegated to ExerciseRepository
    suspend fun getAllExercises() = exerciseRepository.getAllExercises()

    suspend fun getAllExercisesWithAliases() = exerciseRepository.getAllExercisesWithAliases()

    suspend fun getExerciseById(id: Long) = exerciseRepository.getExerciseById(id)

    suspend fun getExercisesByCategory(category: ExerciseCategory) = exerciseRepository.getExercisesByCategory(category)

    // Get muscles for a variation
    suspend fun getMusclesForVariation(variationId: Long): List<VariationMuscle> = variationMuscleDao.getMusclesForVariation(variationId)

    // Get aliases for a variation
    suspend fun getAliasesForVariation(variationId: Long): List<VariationAlias> = variationAliasDao.getAliasesForVariation(variationId)

    // Delete custom exercise
    suspend fun canDeleteExercise(exerciseVariationId: Long): Result<Boolean> = exerciseRepository.canDeleteExercise(exerciseVariationId)

    suspend fun deleteCustomExercise(exerciseVariationId: Long): Result<Unit> = exerciseRepository.deleteCustomExercise(exerciseVariationId)

    // Create custom exercise
    suspend fun createCustomExercise(
        name: String,
        category: ExerciseCategory,
        primaryMuscles: Set<MuscleGroup>,
        secondaryMuscles: Set<MuscleGroup> = emptySet(),
        equipment: Equipment,
        difficulty: ExerciseDifficulty = ExerciseDifficulty.BEGINNER,
        requiresWeight: Boolean = true,
        movementPattern: MovementPattern = MovementPattern.PUSH,
    ): Result<ExerciseVariation> =
        exerciseRepository.createCustomExercise(
            name = name,
            category = category,
            primaryMuscles = primaryMuscles,
            secondaryMuscles = secondaryMuscles,
            equipment = equipment,
            difficulty = difficulty,
            requiresWeight = requiresWeight,
            movementPattern = movementPattern,
        )

    // ===== EXISTING WORKOUT METHODS (Updated to work with new Exercise system) =====

    // Basic CRUD operations
    suspend fun insertWorkout(workout: Workout): Long = workoutRepository.createWorkout(workout)

    suspend fun getExercisesForWorkout(workoutId: Long): List<ExerciseLog> = exerciseRepository.getExercisesForWorkout(workoutId)

    suspend fun getSetsForExercise(exerciseLogId: Long): List<SetLog> = exerciseRepository.getSetsForExercise(exerciseLogId)

    suspend fun getLastPerformanceForExercise(exerciseVariationId: Long): SetLog? =
        withContext(Dispatchers.IO) {
            setLogDao.getLastCompletedSetForExercise(exerciseVariationId)
        }

    suspend fun getSetsForWorkout(workoutId: Long): List<SetLog> =
        withContext(Dispatchers.IO) {
            val exercises = getExercisesForWorkout(workoutId)
            exercises.flatMap { exercise ->
                getSetsForExercise(exercise.id)
            }
        }

    suspend fun markSetCompleted(
        setId: Long,
        completed: Boolean,
        completedAt: String?,
    ) = setLogDao.markSetCompleted(setId, completed, completedAt)

    suspend fun insertExerciseLog(exerciseLog: ExerciseLog): Long = exerciseRepository.insertExerciseLog(exerciseLog)

    suspend fun incrementExerciseUsageCount(exerciseId: Long) = db.exerciseDao().incrementUsageCount(exerciseId)

    suspend fun insertSetLog(setLog: SetLog): Long {
        // Round all weight fields to nearest 0.25
        val roundedSetLog =
            setLog.copy(
                targetWeight = setLog.targetWeight?.let { WeightFormatter.roundToNearestQuarter(it) },
                actualWeight = WeightFormatter.roundToNearestQuarter(setLog.actualWeight),
                suggestedWeight = setLog.suggestedWeight?.let { WeightFormatter.roundToNearestQuarter(it) },
            )
        return setLogDao.insertSetLog(roundedSetLog)
    }

    suspend fun updateSetLog(setLog: SetLog) {
        // Round all weight fields to nearest 0.25
        val roundedSetLog =
            setLog.copy(
                targetWeight = setLog.targetWeight?.let { WeightFormatter.roundToNearestQuarter(it) },
                actualWeight = WeightFormatter.roundToNearestQuarter(setLog.actualWeight),
                suggestedWeight = setLog.suggestedWeight?.let { WeightFormatter.roundToNearestQuarter(it) },
            )
        setLogDao.updateSetLog(roundedSetLog)
    }

    suspend fun deleteSetLog(setId: Long) = setLogDao.deleteSetLog(setId)

    // Enhanced exercise log creation that links to Exercise entity
    suspend fun insertExerciseLogWithExerciseReference(
        workoutId: Long,
        exercise: ExerciseVariation,
        exerciseOrder: Int,
        notes: String? = null,
    ): Long = exerciseRepository.insertExerciseLogWithExerciseReference(workoutId, exercise, exerciseOrder, notes)

    // Workout state management
    suspend fun getOngoingWorkout(): Workout? {
        val allWorkouts = workoutDao.getAllWorkouts()
        return allWorkouts.find { workout ->
            // Check if workout has exercises but no completion marker
            val exercises = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
            val hasExercises = exercises.isNotEmpty()
            val isCompleted = workout.status == WorkoutStatus.COMPLETED

            hasExercises && !isCompleted
        }
    }

    suspend fun completeWorkout(
        workoutId: Long,
        durationSeconds: Long? = null,
    ) {
        val workout = workoutDao.getWorkoutById(workoutId)
        if (workout == null) {
            return
        }

        // Check if already completed
        if (workout.status == WorkoutStatus.COMPLETED) {
            return
        }

        // Validate that workout has at least one completed set
        val sets = getSetsForWorkout(workoutId)
        val completedSetsCount = sets.count { it.isCompleted }
        if (completedSetsCount == 0) {
            // Don't mark as completed if no sets were completed
            return
        }

        val updatedWorkout = workout.copy(status = WorkoutStatus.COMPLETED, durationSeconds = durationSeconds)

        workoutDao.updateWorkout(updatedWorkout)

        // Record performance data for programme workouts before updating progress
        if (workout.isProgrammeWorkout && workout.programmeId != null) {
            recordWorkoutPerformanceData(workoutId, workout.programmeId)
        }

        // Update global exercise progress for ALL workouts (programme or freestyle)
        val pendingOneRMUpdates = globalProgressTracker.updateProgressAfterWorkout(workoutId)

        // Store pending 1RM updates for later retrieval by UI
        if (pendingOneRMUpdates.isNotEmpty()) {
            pendingOneRMUpdates.forEach { update ->
                oneRMRepository.addPendingOneRMUpdate(update)
            }
        }

        // Update programme progress if this is a programme workout
        val hasProgrammeDetails = workout.programmeId != null && workout.weekNumber != null && workout.dayNumber != null
        if (workout.isProgrammeWorkout && hasProgrammeDetails) {
            updateProgrammeProgressAfterWorkout(workout.programmeId!!)
        }
    }

    private suspend fun updateProgrammeProgressAfterWorkout(
        programmeId: Long,
    ) {
        try {
            // Always increment completed workouts when a workout is completed
            programmeDao.incrementCompletedWorkouts(programmeId)

            // Now find the next workout and update current week/day to that
            val nextWorkoutInfo = getNextProgrammeWorkout(programmeId)
            if (nextWorkoutInfo != null) {
                // Update progress to point to the next workout
                programmeDao.updateProgress(
                    programmeId,
                    nextWorkoutInfo.actualWeekNumber,
                    nextWorkoutInfo.workoutStructure.day,
                    LocalDateTime.now().toString(),
                )
            } else {
                // Double-check the completion logic before marking complete
                val finalProgress = programmeDao.getProgressForProgramme(programmeId)
                if (finalProgress != null) {
                    // CRITICAL FIX: Recalculate total workouts for custom programmes to ensure accuracy
                    val programme = programmeDao.getProgrammeById(programmeId)
                    if (programme != null && programme.isCustom) {
                        val actualTotalWorkouts = programmeDao.getAllWorkoutsForProgramme(programmeId).size

                        if (finalProgress.completedWorkouts < actualTotalWorkouts) {
                            // Update the total workouts to fix the mismatch
                            val correctedProgress = finalProgress.copy(totalWorkouts = actualTotalWorkouts)
                            programmeDao.insertOrUpdateProgress(correctedProgress)
                            return
                        }
                    }

                    if (finalProgress.completedWorkouts >= finalProgress.totalWorkouts) {
                        completeProgramme(programmeId)
                    }
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

                val updatedProgress =
                    progress.copy(
                        adherencePercentage = adherence,
                        lastWorkoutDate = LocalDateTime.now(),
                    )
                programmeDao.insertOrUpdateProgress(updatedProgress)
            }
        } catch (e: android.database.sqlite.SQLiteException) {
            Log.e("FeatherweightRepository", "Programme progress update failed", e)
            // Programme progress update failed - this is not critical for app functionality
        }
    }

    // History functionality
    suspend fun getWorkoutHistory(): List<WorkoutSummary> = workoutRepository.getWorkoutHistory()

    // Smart suggestions functionality (enhanced with exercise data)
    suspend fun getExerciseHistory(
        exerciseVariationId: Long,
        currentWorkoutId: Long,
    ): ExerciseHistory? {
        val allWorkouts =
            workoutDao
                .getAllWorkouts()
                .filter { it.status == WorkoutStatus.COMPLETED } // Only look at completed workouts
                .sortedByDescending { it.date } // Most recent first

        for (workout in allWorkouts) {
            if (workout.id == currentWorkoutId) continue

            val exercises = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
            val matchingExercise = exercises.find { it.exerciseVariationId == exerciseVariationId }

            if (matchingExercise != null) {
                val sets =
                    setLogDao
                        .getSetLogsForExercise(matchingExercise.id)
                        .filter { it.isCompleted } // Only completed sets

                if (sets.isNotEmpty()) {
                    return ExerciseHistory(
                        exerciseVariationId = exerciseVariationId,
                        lastWorkoutDate = workout.date,
                        sets = sets,
                    )
                }
            }
        }
        return null
    }

    private suspend fun getExerciseStats(exerciseVariationId: Long): ExerciseStats? = exerciseRepository.getExerciseStats(exerciseVariationId)

    suspend fun getWorkoutById(workoutId: Long): Workout? = workoutRepository.getWorkoutById(workoutId)

    suspend fun getExerciseLogsForWorkout(workoutId: Long): List<ExerciseLog> = workoutRepository.getExerciseLogsForWorkout(workoutId)

    suspend fun getSetLogsForExercise(exerciseLogId: Long): List<SetLog> = setLogDao.getSetLogsForExercise(exerciseLogId)

    // Delete an exercise log (will cascade delete all its sets due to foreign key)
    suspend fun deleteExerciseLog(exerciseLogId: Long) = exerciseRepository.deleteExerciseLog(exerciseLogId)

    // Update exercise order
    suspend fun updateExerciseOrder(
        exerciseLogId: Long,
        newOrder: Int,
    ) = exerciseRepository.updateExerciseOrder(exerciseLogId, newOrder)

    // Delete an entire workout (will cascade delete all exercises and sets)
    suspend fun deleteWorkout(workoutId: Long) = workoutRepository.deleteWorkoutById(workoutId)

    suspend fun deleteSetsForExerciseLog(exerciseLogId: Long) = exerciseRepository.deleteSetsForExercise(exerciseLogId)

    // Get exercise by ID (returns ExerciseVariation)
    suspend fun getExerciseEntityById(exerciseId: Long): ExerciseVariation? = exerciseRepository.getExerciseEntityById(exerciseId)

    // Swap exercise
    suspend fun swapExercise(
        exerciseLogId: Long,
        newExerciseVariationId: Long,
        originalExerciseVariationId: Long,
    ) = exerciseRepository.swapExercise(exerciseLogId, newExerciseVariationId, originalExerciseVariationId)

    // Record an exercise swap in history
    suspend fun recordExerciseSwap(
        originalExerciseId: Long,
        swappedToExerciseId: Long,
        workoutId: Long? = null,
        programmeId: Long? = null,
    ) = exerciseRepository.recordExerciseSwap(originalExerciseId, swappedToExerciseId, workoutId, programmeId)

    // Get swap history for exercise
    suspend fun getSwapHistoryForExercise(
        exerciseId: Long,
    ): List<SwapHistoryCount> = exerciseRepository.getSwapHistoryForExercise(exerciseId)

    // ===== ANALYTICS METHODS =====

    suspend fun getRecentSetLogsForExercise(
        exerciseVariationId: Long,
        daysBack: Int,
    ): List<SetLog> =
        withContext(Dispatchers.IO) {
            val sinceDate = LocalDateTime.now().minusDays(daysBack.toLong())
            setLogDao.getSetsForExerciseSince(exerciseVariationId, sinceDate.toString())
        }

    // ===== PROGRAMME-WORKOUT INTEGRATION METHODS =====

    // Create a workout from a programme template with full exercise structure
    suspend fun createWorkoutFromProgrammeTemplate(
        programmeId: Long,
        weekNumber: Int,
        dayNumber: Int,
    ): Long = withContext(Dispatchers.IO) {
        Log.d("FeatherweightRepository", "=== CREATING WORKOUT FROM PROGRAMME ===")
        Log.d("FeatherweightRepository", "Programme ID: $programmeId, Week: $weekNumber, Day: $dayNumber")

        programmeDao.getProgrammeById(programmeId)
            ?: throw IllegalArgumentException("Programme not found")

        val progress = programmeDao.getProgressForProgramme(programmeId)
        if (progress == null) {
            Log.e("FeatherweightRepository", "No progress found for programme $programmeId")
            error("Programme progress not found")
        }

        val nextWorkoutInfo = getNextProgrammeWorkout(programmeId)
        if (nextWorkoutInfo == null) {
            Log.e("FeatherweightRepository", "No next workout found for programme $programmeId")
            error("No workout available for this programme")
        }

        val workoutStructure = nextWorkoutInfo.workoutStructure
        Log.d("FeatherweightRepository", "Found workout structure: ${workoutStructure.name}")
        Log.d("FeatherweightRepository", "Exercises in structure: ${workoutStructure.exercises.size}")

        val now = LocalDateTime.now()
        val workout = Workout(
            date = now,
            status = WorkoutStatus.IN_PROGRESS,
            programmeId = programmeId,
            weekNumber = nextWorkoutInfo.actualWeekNumber,
            dayNumber = workoutStructure.day,
            programmeWorkoutName = workoutStructure.name,
            isProgrammeWorkout = true,
        )
        val workoutId = workoutDao.insertWorkout(workout)
        Log.d("FeatherweightRepository", "Created workout with ID: $workoutId")

        workoutStructure.exercises.forEachIndexed { index, exerciseStructure ->
            Log.d("FeatherweightRepository", "Adding exercise ${index + 1}: ${exerciseStructure.name}")
            Log.d("FeatherweightRepository", "  exerciseId: ${exerciseStructure.exerciseId}")

            val exerciseVariationId = exerciseStructure.exerciseId
            if (exerciseVariationId != null) {
                val exerciseLog = ExerciseLog(
                    workoutId = workoutId,
                    exerciseVariationId = exerciseVariationId,
                    exerciseOrder = index,
                    notes = exerciseStructure.note,
                )
                val exerciseLogId = exerciseLogDao.insertExerciseLog(exerciseLog)
                Log.d("FeatherweightRepository", "  Created exercise log with ID: $exerciseLogId")

                val weights = exerciseStructure.weights
                val rpeList = exerciseStructure.rpeValues

                Log.d("FeatherweightRepository", "  Weights: $weights, RPE values: $rpeList")

                when (val reps = exerciseStructure.reps) {
                    is RepsStructure.PerSet -> {
                        reps.values.forEachIndexed { setIndex, repValue ->
                            val targetReps = repValue.toIntOrNull() ?: 10
                            val targetWeight = weights?.getOrNull(setIndex) ?: 0f
                            val targetRpe = rpeList?.getOrNull(setIndex)

                            val setLog = SetLog(
                                exerciseLogId = exerciseLogId,
                                setOrder = setIndex + 1,
                                targetReps = targetReps,
                                targetWeight = targetWeight,
                                targetRpe = targetRpe,
                                actualReps = 0,
                                actualWeight = 0f,
                                actualRpe = null,
                            )
                            Log.d("FeatherweightRepository", "    Creating SetLog: targetReps=$targetReps, targetWeight=$targetWeight, targetRpe=$targetRpe")
                            setLogDao.insertSetLog(setLog)
                        }
                    }
                    is RepsStructure.Range -> {
                        repeat(exerciseStructure.sets) { setIndex ->
                            val targetWeight = weights?.getOrNull(setIndex) ?: 0f
                            val targetRpe = rpeList?.getOrNull(setIndex)

                            val setLog = SetLog(
                                exerciseLogId = exerciseLogId,
                                setOrder = setIndex + 1,
                                targetReps = reps.min,
                                targetWeight = targetWeight,
                                targetRpe = targetRpe,
                                actualReps = 0,
                                actualWeight = 0f,
                                actualRpe = null,
                            )
                            setLogDao.insertSetLog(setLog)
                        }
                    }
                    is RepsStructure.Single -> {
                        repeat(exerciseStructure.sets) { setIndex ->
                            val targetWeight = weights?.getOrNull(setIndex) ?: 0f

                            val setLog = SetLog(
                                exerciseLogId = exerciseLogId,
                                setOrder = setIndex + 1,
                                targetReps = reps.value,
                                targetWeight = targetWeight,
                                actualReps = 0,
                                actualWeight = 0f,
                                actualRpe = null,
                            )
                            setLogDao.insertSetLog(setLog)
                        }
                    }
                    is RepsStructure.RangeString -> {
                        val parts = reps.value.split("-")
                        val minReps = parts.getOrNull(0)?.toIntOrNull() ?: 8
                        val maxReps = parts.getOrNull(1)?.toIntOrNull() ?: 12
                        val targetReps = (minReps + maxReps) / 2

                        repeat(exerciseStructure.sets) { setIndex ->
                            val targetWeight = weights?.getOrNull(setIndex) ?: 0f
                            val targetRpe = rpeList?.getOrNull(setIndex)

                            val setLog = SetLog(
                                exerciseLogId = exerciseLogId,
                                setOrder = setIndex + 1,
                                targetReps = targetReps,
                                targetWeight = targetWeight,
                                targetRpe = targetRpe,
                                actualReps = 0,
                                actualWeight = 0f,
                                actualRpe = null,
                            )
                            Log.d("FeatherweightRepository", "    Creating SetLog: targetReps=$targetReps, targetWeight=$targetWeight, targetRpe=$targetRpe")
                            setLogDao.insertSetLog(setLog)
                        }
                    }
                }
                Log.d("FeatherweightRepository", "  Created ${exerciseStructure.sets} sets")
            } else {
                Log.w("FeatherweightRepository", "  Skipping exercise '${exerciseStructure.name}' - no matched exercise ID")
            }
        }

        Log.d("FeatherweightRepository", "=== WORKOUT CREATION COMPLETE ===")
        Log.d("FeatherweightRepository", "Workout ID: $workoutId with ${workoutStructure.exercises.size} exercises")

        workoutId
    }

    // Get next programme workout to do
    suspend fun getNextProgrammeWorkout(programmeId: Long): NextProgrammeWorkoutInfo? =
        withContext(Dispatchers.IO) {
            val programme = programmeDao.getProgrammeById(programmeId)
            if (programme == null) {
                return@withContext null
            }

            val progress = programmeDao.getProgressForProgramme(programmeId)
            if (progress == null) {
                return@withContext null
            }

            // Check if programme is actually complete
            if (progress.completedWorkouts >= progress.totalWorkouts) {
                return@withContext null
            }

            // Add debug info for completion check
            progress.totalWorkouts - progress.completedWorkouts

            // All programmes are now custom (AI-generated or user-created) with direct workout storage
            return@withContext getNextWorkoutFromDatabase(programmeId, progress)
        }

    // Handle next workout for custom programmes (AI-generated or user-created)
    private suspend fun getNextWorkoutFromDatabase(
        programmeId: Long,
        progress: ProgrammeProgress,
    ): NextProgrammeWorkoutInfo? {
        Log.d("FeatherweightRepository", "=== LOADING NEXT WORKOUT FROM PROGRAMME ===")
        Log.d("FeatherweightRepository", "Programme ID: $programmeId")
        Log.d("FeatherweightRepository", "Progress - completed workouts: ${progress.completedWorkouts}")

        // Get all workouts for this programme, ordered by week and day
        val allWorkouts = programmeDao.getAllWorkoutsForProgramme(programmeId)
        Log.d("FeatherweightRepository", "Total workouts in programme: ${allWorkouts.size}")

        if (allWorkouts.isEmpty()) {
            Log.d("FeatherweightRepository", "No workouts found for programme!")
            return null
        }

        // Find the next workout based on completed workouts count
        val nextWorkoutIndex = progress.completedWorkouts

        if (nextWorkoutIndex >= allWorkouts.size) {
            Log.d("FeatherweightRepository", "All workouts completed (index $nextWorkoutIndex >= ${allWorkouts.size})")
            return null
        }

        val nextWorkout = allWorkouts[nextWorkoutIndex]
        Log.d("FeatherweightRepository", "Loading workout: ${nextWorkout.name}")
        Log.d("FeatherweightRepository", "Workout structure JSON length: ${nextWorkout.workoutStructure.length}")

        // Parse the workout structure from JSON with proper configuration
        val workoutStructure =
            try {
                val json =
                    kotlinx.serialization.json.Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    }
                val structure =
                    json.decodeFromString<WorkoutStructure>(
                        nextWorkout.workoutStructure,
                    )
                Log.d("FeatherweightRepository", "Parsed workout structure successfully")
                Log.d("FeatherweightRepository", "  Exercises in workout: ${structure.exercises.size}")
                structure.exercises.forEachIndexed { index, exercise ->
                    Log.d("FeatherweightRepository", "  Exercise ${index + 1}: ${exercise.name}")
                    Log.d("FeatherweightRepository", "    exerciseId: ${exercise.exerciseId}")
                    Log.d("FeatherweightRepository", "    sets: ${exercise.sets}")
                }
                structure
            } catch (e: kotlinx.serialization.SerializationException) {
                // Try to fix invalid JSON by replacing ,"reps":, with ,"reps":"8-12",
                val fixedJson =
                    nextWorkout.workoutStructure
                        .replace("\"reps\":,", "\"reps\":\"8-12\",")
                        .replace("\"reps\": ,", "\"reps\":\"8-12\",")

                try {
                    val json =
                        kotlinx.serialization.json.Json {
                            ignoreUnknownKeys = true
                            isLenient = true
                        }
                    json.decodeFromString<WorkoutStructure>(fixedJson)
                } catch (e2: kotlinx.serialization.SerializationException) {
                    Log.e("FeatherweightRepository", "Failed to parse workout structure", e2)
                    return null
                }
            }

        // Get the week number from the workout's week
        val week = programmeDao.getWeekById(nextWorkout.weekId)
        val actualWeekNumber = week?.weekNumber ?: 1

        return NextProgrammeWorkoutInfo(
            workoutStructure = workoutStructure,
            actualWeekNumber = actualWeekNumber,
        )
    }

    // Calculate total workouts for a programme based on its JSON structure
    private suspend fun calculateTotalWorkoutsFromStructure(programme: Programme): Int {
        return try {
            // All programmes are now custom (AI-generated or user-created)
            // For custom programmes, count actual workouts in the database
            val allWorkouts = programmeDao.getAllWorkoutsForProgramme(programme.id)
            val totalWorkouts = allWorkouts.size

            return totalWorkouts
        } catch (e: android.database.sqlite.SQLiteException) {
            Log.e(TAG, "Failed to get total workouts for programme: ${programme.id} (${programme.name})", e)
            programme.durationWeeks * 3 // Fallback to default
        }
    }

    suspend fun createImportedProgramme(
        name: String,
        description: String,
        durationWeeks: Int,
        jsonStructure: String,
        programmeType: ProgrammeType = ProgrammeType.GENERAL_FITNESS,
        difficulty: ProgrammeDifficulty = ProgrammeDifficulty.INTERMEDIATE,
    ): Long =
        withContext(Dispatchers.IO) {
            Log.d("FeatherweightRepository", "=== REPOSITORY: CREATING IMPORTED PROGRAMME ===")
            Log.d("FeatherweightRepository", "Programme name: $name")
            Log.d("FeatherweightRepository", "Duration: $durationWeeks weeks")
            Log.d("FeatherweightRepository", "JSON structure size: ${jsonStructure.length} chars")

            val programme =
                Programme(
                    name = name,
                    description = description,
                    durationWeeks = durationWeeks,
                    programmeType = programmeType,
                    difficulty = difficulty,
                    isCustom = true,
                    isActive = false,
                )

            val programmeId = programmeDao.insertProgramme(programme)
            Log.d("FeatherweightRepository", "Programme inserted with ID: $programmeId")

            // Parse the JSON structure and create weeks/workouts
            @Suppress("UNCHECKED_CAST")
            val parsedData =
                Gson().fromJson(
                    jsonStructure,
                    Map::class.java,
                ) as Map<String, Any>

            @Suppress("UNCHECKED_CAST")
            val weeks = parsedData["weeks"] as List<Map<String, Any>>

            Log.d("FeatherweightRepository", "Parsed JSON - Found ${weeks.size} weeks")

            weeks.forEach { weekData ->
                val weekNumber = (weekData["weekNumber"] as Double).toInt()
                val weekName = weekData["name"] as? String ?: "Week $weekNumber"
                val weekDescription = weekData["description"] as? String
                val focusAreas = weekData["focusAreas"] as? String
                val intensityLevel = weekData["intensityLevel"] as? String
                val volumeLevel = weekData["volumeLevel"] as? String
                val isDeload = weekData["isDeload"] as? Boolean ?: false
                val phase = weekData["phase"] as? String

                @Suppress("UNCHECKED_CAST")
                val workouts = weekData["workouts"] as List<Map<String, Any>>

                Log.d("FeatherweightRepository", "Processing Week $weekNumber: $weekName")
                Log.d("FeatherweightRepository", "  Workouts in week: ${workouts.size}")

                val week =
                    ProgrammeWeek(
                        programmeId = programmeId,
                        weekNumber = weekNumber,
                        name = weekName,
                        description = weekDescription,
                        focusAreas = focusAreas,
                        intensityLevel = intensityLevel,
                        volumeLevel = volumeLevel,
                        isDeload = isDeload,
                        phase = phase,
                    )

                val weekId = programmeDao.insertProgrammeWeek(week)

                workouts.forEachIndexed { index, workoutData ->
                    val workoutName = workoutData["name"] as String
                    val dayOfWeek = workoutData["dayOfWeek"] as? String // Can be null
                    val estimatedDuration = (workoutData["estimatedDurationMinutes"] as? Double)?.toInt() ?: 60

                    // Convert the exercises to WorkoutStructure format - preserving individual sets
                    @Suppress("UNCHECKED_CAST")
                    val exercisesList = workoutData["exercises"] as? List<Map<String, Any>> ?: emptyList()

                    Log.d("FeatherweightRepository", "  Processing Workout: $workoutName ($dayOfWeek)")
                    Log.d("FeatherweightRepository", "    Exercises in workout: ${exercisesList.size}")

                    // For imported programmes, we need to store the exact sets data
                    // We'll encode it in a special format that createSetsFromStructure can decode
                    val exerciseStructures =
                        exercisesList.map { exerciseData ->
                            val exerciseName = exerciseData["exerciseName"] as String
                            val exerciseId = (exerciseData["exerciseId"] as? Double)?.toLong()

                            @Suppress("UNCHECKED_CAST")
                            val sets = exerciseData["sets"] as? List<Map<String, Any>> ?: emptyList()

                            Log.d("FeatherweightRepository", "    Exercise: $exerciseName")
                            Log.d("FeatherweightRepository", "      exerciseId from JSON: $exerciseId")
                            Log.d("FeatherweightRepository", "      sets count: ${sets.size}")

                            // Extract individual values from each set
                            val repsList = sets.map { (it["reps"] as? Double)?.toInt()?.toString() ?: "0" }
                            val weightsList = sets.map { (it["weight"] as? Double)?.toFloat() ?: 0f }
                            // Keep nulls in the list to preserve set indices, but map to null instead of missing
                            val rpeList =
                                sets
                                    .map { setData ->
                                        val rpeValue = setData["rpe"] as? Double
                                        Log.d("FeatherweightRepository", "        Set data keys: ${setData.keys}, rpe raw value: $rpeValue")
                                        rpeValue?.toFloat()
                                    }.takeIf { list -> list.any { it != null } }
                            Log.d("FeatherweightRepository", "      RPE list after parsing: $rpeList")

                            // Store reps as PerSet to preserve individual values
                            val repsStructure = RepsStructure.PerSet(repsList)

                            ExerciseStructure(
                                name = exerciseName,
                                sets = sets.size,
                                reps = repsStructure,
                                weightSource = "imported",
                                note = null, // Notes are for user comments, not data
                                exerciseId = exerciseId,
                                weights = weightsList,
                                rpeValues = rpeList,
                            )
                        }

                    val workoutStructure =
                        WorkoutStructure(
                            day = index + 1,
                            name = workoutName,
                            exercises = exerciseStructures,
                            estimatedDuration = estimatedDuration,
                        )

                    // Serialize with kotlinx.serialization for consistency
                    val workoutStructureJson =
                        kotlinx.serialization.json.Json.encodeToString(
                            WorkoutStructure.serializer(),
                            workoutStructure,
                        )

                    val workout =
                        ProgrammeWorkout(
                            weekId = weekId,
                            dayNumber = index + 1,
                            name = workoutName,
                            description = dayOfWeek,
                            estimatedDuration = estimatedDuration,
                            workoutStructure = workoutStructureJson,
                        )

                    programmeDao.insertProgrammeWorkout(workout)
                }
            }

            programmeId
        }

    suspend fun getAllProgrammes() = programmeRepository.getAllProgrammes()

    suspend fun getActiveProgramme() = programmeRepository.getActiveProgramme()

    suspend fun getProgrammeById(programmeId: Long) = programmeRepository.getProgrammeById(programmeId)

    suspend fun getProgrammeWithDetails(programmeId: Long) = programmeRepository.getProgrammeWithDetails(programmeId)

    suspend fun calculateProgrammeCompletionStats(programmeId: Long): ProgrammeCompletionStats? =
        withContext(Dispatchers.IO) {
            val programme = programmeDao.getProgrammeById(programmeId) ?: return@withContext null
            val progress = programmeDao.getProgressForProgramme(programmeId) ?: return@withContext null
            val workouts = workoutDao.getWorkoutsByProgramme(programmeId)

            // Calculate total volume
            var totalVolume = 0.0
            val exerciseFrequencyMap = mutableMapOf<Pair<Long, String>, Int>()
            val programmeExerciseVariationIds = mutableSetOf<Long>()
            
            for (workout in workouts.filter { it.status == WorkoutStatus.COMPLETED }) {
                val exercises = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
                for (exercise in exercises) {
                    programmeExerciseVariationIds.add(exercise.exerciseVariationId)
                    val exerciseVariation = exerciseVariationDao.getExerciseVariationById(exercise.exerciseVariationId)
                    if (exerciseVariation != null) {
                        val key = exercise.exerciseVariationId to exerciseVariation.name
                        exerciseFrequencyMap[key] = exerciseFrequencyMap.getOrDefault(key, 0) + 1
                    }
                    
                    val sets = setLogDao.getSetLogsForExercise(exercise.id)
                    for (set in sets.filter { it.isCompleted }) {
                        totalVolume += (set.actualWeight * set.actualReps).toDouble()
                    }
                }
            }

            // Calculate top 3 exercises by frequency
            val topExercises = exerciseFrequencyMap
                .map { ExerciseFrequency(it.key.second, it.value) }
                .sortedByDescending { it.frequency }
                .take(3)

            // Calculate average workout duration
            val completedWorkouts = workouts.filter { it.status == WorkoutStatus.COMPLETED && it.durationSeconds != null }
            val averageDuration =
                if (completedWorkouts.isNotEmpty()) {
                    val avgSeconds = completedWorkouts.mapNotNull { it.durationSeconds }.average().toLong()
                    Duration.ofSeconds(avgSeconds)
                } else {
                    Duration.ZERO
                }

            // Count PRs during programme - FILTERED to programme exercises only
            val allPrs =
                personalRecordDao.getPersonalRecordsInDateRange(
                    programme.startedAt ?: programme.createdAt,
                    programme.completedAt ?: LocalDateTime.now(),
                )
            val programmePrs = allPrs.filter { pr ->
                programmeExerciseVariationIds.contains(pr.exerciseVariationId)
            }

            // Calculate strength improvements
            val strengthImprovements = calculateStrengthImprovements(programmeId)
            val avgImprovement =
                if (strengthImprovements.isNotEmpty()) {
                    strengthImprovements.map { it.improvementPercentage }.average().toFloat()
                } else {
                    0f
                }

            // Calculate insights
            val insights = calculateProgrammeInsights(workouts)

            ProgrammeCompletionStats(
                programmeId = programmeId,
                programmeName = programme.name,
                startDate = programme.startedAt ?: programme.createdAt,
                endDate = programme.completedAt ?: LocalDateTime.now(),
                totalWorkouts = progress.totalWorkouts,
                completedWorkouts = progress.completedWorkouts,
                totalVolume = totalVolume.toFloat(),
                averageWorkoutDuration = averageDuration,
                totalPRs = programmePrs.size,
                strengthImprovements = strengthImprovements,
                averageStrengthImprovement = avgImprovement,
                insights = insights,
                topExercises = topExercises,
            )
        }

    private suspend fun calculateStrengthImprovements(programmeId: Long): List<StrengthImprovement> =
        withContext(Dispatchers.IO) {
            val programme = programmeDao.getProgrammeById(programmeId) ?: return@withContext emptyList()
            val startDate = programme.startedAt ?: programme.createdAt
            val endDate = programme.completedAt ?: LocalDateTime.now()

            // Get all exercises used in the programme
            val exerciseVariationIds =
                workoutDao
                    .getWorkoutsByProgramme(programmeId)
                    .flatMap { workout ->
                        exerciseLogDao
                            .getExerciseLogsForWorkout(workout.id)
                            .map { it.exerciseVariationId }
                    }.distinct()

            // For each exercise, find 1RM improvements
            val improvements = mutableListOf<StrengthImprovement>()

            for (exerciseVariationId in exerciseVariationIds) {
                val exercise = exerciseVariationDao.getExerciseVariationById(exerciseVariationId) ?: continue

                // Get 1RM history in programme date range
                val history = db.oneRMDao().getOneRMHistoryInRange(exerciseVariationId, startDate, endDate)
                // Only include exercises with 3+ data points to reduce noise
                if (history.size >= 3) {
                    val startingMax = history.first().oneRMEstimate
                    val endingMax = history.last().oneRMEstimate

                    if (endingMax > startingMax) {
                        val improvement = endingMax - startingMax
                        val percentage = (improvement / startingMax) * 100

                        improvements.add(
                            StrengthImprovement(
                                exerciseName = exercise.name,
                                startingMax = startingMax,
                                endingMax = endingMax,
                                improvementKg = improvement,
                                improvementPercentage = percentage,
                            ),
                        )
                    }
                }
            }

            improvements.sortedByDescending { it.improvementPercentage }
        }

    private fun calculateProgrammeInsights(workouts: List<Workout>): ProgrammeInsights {
        val completedWorkouts = workouts.filter { it.status == WorkoutStatus.COMPLETED }

        // Calculate most consistent training day
        val dayCount =
            completedWorkouts
                .groupingBy {
                    it.date.dayOfWeek.name
                }.eachCount()

        val mostConsistentDay = dayCount.maxByOrNull { it.value }?.key

        // Calculate average rest days between workouts
        val sortedWorkouts = completedWorkouts.sortedBy { it.date }
        val restDays =
            if (sortedWorkouts.size > 1) {
                sortedWorkouts
                    .zipWithNext { a, b ->
                        java.time.temporal.ChronoUnit.DAYS
                            .between(a.date, b.date)
                            .toFloat() - 1
                    }.average()
                    .toFloat()
            } else {
                0f
            }

        return ProgrammeInsights(
            totalTrainingDays = completedWorkouts.size,
            mostConsistentDay = mostConsistentDay,
            averageRestDaysBetweenWorkouts = restDays,
        )
    }

    suspend fun updateProgrammeCompletionNotes(
        programmeId: Long,
        notes: String?,
    ) = programmeRepository.updateProgrammeCompletionNotes(programmeId, notes)

    suspend fun activateProgramme(programmeId: Long) =
        withContext(Dispatchers.IO) {
            try {
                programmeDao.setActiveProgramme(programmeId)

                // Update status to IN_PROGRESS
                programmeDao.updateProgrammeStatus(programmeId, ProgrammeStatus.IN_PROGRESS)

                // Initialize progress tracking
                val programme = programmeDao.getProgrammeById(programmeId) ?: return@withContext

                // Calculate total workouts based on actual JSON structure
                val totalWorkouts = calculateTotalWorkoutsFromStructure(programme)

                val progress =
                    ProgrammeProgress(
                        programmeId = programmeId,
                        currentWeek = 1,
                        currentDay = 1,
                        completedWorkouts = 0,
                        totalWorkouts = totalWorkouts,
                        lastWorkoutDate = null,
                        adherencePercentage = 0f,
                        strengthProgress = null,
                    )

                programmeDao.insertOrUpdateProgress(progress)
            } catch (e: android.database.sqlite.SQLiteException) {
                Log.e("FeatherweightRepository", "Error completing workout", e)
            }
        }

    private suspend fun completeProgramme(programmeId: Long) =
        withContext(Dispatchers.IO) {
            // Get programme details for logging
            val programme = programmeDao.getProgrammeById(programmeId)
            val progress = programmeDao.getProgressForProgramme(programmeId)

            if (programme != null && progress != null) {
                // ATOMIC update - sets status=COMPLETED, isActive=false, and completedAt in one transaction
                val completionTime = LocalDateTime.now()
                programmeDao.completeProgrammeAtomic(programmeId, completionTime)
            }
        }

    suspend fun deleteProgramme(programme: Programme) =
        withContext(Dispatchers.IO) {
            // First deactivate the programme if it's active
            if (programme.isActive) {
                programmeDao.deactivateAllProgrammes()
            }

            // Delete all workouts associated with this programme to prevent orphaned workouts
            workoutDao.deleteWorkoutsByProgramme(programme.id)

            // Then delete the programme (will cascade delete progress and related data)
            programmeDao.deleteProgramme(programme)
        }

    suspend fun getInProgressWorkoutCountByProgramme(programmeId: Long): Int = programmeRepository.getInProgressWorkoutCountByProgramme(programmeId)

    suspend fun updateWorkoutStatus(
        workoutId: Long,
        status: WorkoutStatus,
    ) = workoutRepository.updateWorkoutStatus(workoutId, status)

    suspend fun updateWorkoutTimerStart(
        workoutId: Long,
        timerStartTime: LocalDateTime,
    ) = withContext(Dispatchers.IO) {
        val workout = workoutDao.getWorkoutById(workoutId) ?: return@withContext
        val updatedWorkout = workout.copy(timerStartTime = timerStartTime)
        workoutDao.updateWorkout(updatedWorkout)
    }

    // Helper method to get exercise by name or alias
    suspend fun getExerciseByName(name: String): ExerciseVariation? = exerciseRepository.getExerciseByName(name)

    suspend fun getExerciseVariationById(id: Long): ExerciseVariation? =
        withContext(Dispatchers.IO) {
            exerciseRepository.getExerciseById(id)
        }

    // ========== Profile & 1RM Management ==========

    suspend fun upsertExerciseMax(
        exerciseVariationId: Long,
        oneRMEstimate: Float,
        oneRMContext: String,
        oneRMType: OneRMType,
        notes: String? = null,
        workoutDate: LocalDateTime? = null,
    ) = withContext(Dispatchers.IO) {
        // Round weight to nearest 0.25
        val roundedWeight = WeightFormatter.roundToNearestQuarter(oneRMEstimate)
        val dateToUse = workoutDate ?: LocalDateTime.now()
        val userExerciseMax =
            UserExerciseMax(
                exerciseVariationId = exerciseVariationId,
                mostWeightLifted = roundedWeight,
                mostWeightReps = 1,
                mostWeightRpe = null,
                mostWeightDate = dateToUse,
                oneRMEstimate = roundedWeight,
                oneRMContext = oneRMContext,
                oneRMConfidence = if (oneRMType == OneRMType.MANUALLY_ENTERED) 1.0f else 0.9f,
                oneRMDate = dateToUse,
                oneRMType = oneRMType,
                notes = notes,
            )
        db.oneRMDao().insertOrUpdateExerciseMax(userExerciseMax)

        // Also save to OneRMHistory for chart tracking with the correct date
        oneRMRepository.saveOneRMToHistory(
            exerciseVariationId = exerciseVariationId,
            estimatedMax = roundedWeight,
            source = oneRMContext,
            date = dateToUse,
        )
    }

    // Remove getAllCurrentMaxes - this should only be used in Insights

    suspend fun getCurrentMaxesForExercises(exerciseIds: List<Long>) = oneRMRepository.getCurrentMaxesForExercises(exerciseIds)

    fun getAllCurrentMaxesWithNames(): Flow<List<OneRMWithExerciseName>> = oneRMRepository.getAllCurrentMaxesWithNames()

    fun getBig4ExercisesWithMaxes() = oneRMRepository.getBig4ExercisesWithMaxes()

    fun getOtherExercisesWithMaxes() = oneRMRepository.getOtherExercisesWithMaxes()

    suspend fun getOneRMForExercise(exerciseVariationId: Long): Float? = oneRMRepository.getOneRMForExercise(exerciseVariationId)

    suspend fun deleteAllMaxesForExercise(exerciseId: Long) = oneRMRepository.deleteAllMaxesForExercise(exerciseId)

    suspend fun updateOrInsertOneRM(oneRMRecord: UserExerciseMax) = oneRMRepository.updateOrInsertOneRM(oneRMRecord)

    private suspend fun recordWorkoutPerformanceData(
        workoutId: Long,
        programmeId: Long,
    ) = withContext(Dispatchers.IO) {
        val progressionService =
            ProgressionService(
                performanceTrackingDao = db.exercisePerformanceTrackingDao(),
                programmeDao = programmeDao,
                repository = this@FeatherweightRepository,
            )

        // Get all exercises and sets for this workout
        val exercises = exerciseLogDao.getExerciseLogsForWorkout(workoutId)

        for (exercise in exercises) {
            val sets = setLogDao.getSetLogsForExercise(exercise.id)
            if (sets.isNotEmpty()) {
                // Get exercise name from variation for progression tracking
                val variation = exerciseVariationDao.getExerciseVariationById(exercise.exerciseVariationId)
                val exerciseName = variation?.name ?: "Unknown Exercise"

                progressionService.recordWorkoutPerformance(
                    workoutId = workoutId,
                    programmeId = programmeId,
                    exerciseName = exerciseName,
                    sets = sets,
                )
            }
        }
    }

    // ===== INTELLIGENT SUGGESTIONS SUPPORT =====

    // getOneRMForExercise is defined above at line 2015

    suspend fun getGlobalExerciseProgress(
        exerciseVariationId: Long,
    ): GlobalExerciseProgress? =
        withContext(Dispatchers.IO) {
            globalExerciseProgressDao.getProgressForExercise(exerciseVariationId)
        }

    /**
     * Check if a completed set represents a personal record
     * Returns list of PersonalRecord objects if PRs are detected
     */
    suspend fun checkForPR(
        setLog: SetLog,
        exerciseVariationId: Long,
    ): List<PersonalRecord> = personalRecordRepository.checkForPR(setLog, exerciseVariationId, ::updateOrInsertOneRM)

    /**
     * Get recent personal records across all exercises
     */
    suspend fun getRecentPRs(limit: Int = 10): List<PersonalRecord> = personalRecordRepository.getRecentPRs(limit)

    /**
     * Get personal records achieved during a specific workout
     */
    suspend fun getPersonalRecordsForWorkout(workoutId: Long): List<PersonalRecord> = personalRecordRepository.getPersonalRecordsForWorkout(workoutId)

    suspend fun getCurrentOneRMEstimate(exerciseId: Long): Float? = oneRMRepository.getCurrentOneRMEstimate(exerciseId)

    suspend fun getOneRMHistoryForExercise(
        exerciseName: String,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): List<OneRMHistory> =
        withContext(Dispatchers.IO) {
            val exercise = getExerciseByName(exerciseName)
            if (exercise != null) {
                db.oneRMDao().getOneRMHistoryInRange(exercise.id, startDate, endDate)
            } else {
                emptyList()
            }
        }

    /**
     * Update workout notes
     */
    suspend fun updateWorkoutNotes(
        workoutId: Long,
        notes: String?,
    ) = withContext(Dispatchers.IO) {
        val workout = workoutDao.getWorkoutById(workoutId)
        if (workout != null) {
            val updatedWorkout =
                workout.copy(
                    notes = notes?.takeIf { it.isNotBlank() },
                    notesUpdatedAt = if (notes?.isNotBlank() == true) LocalDateTime.now() else null,
                )
            workoutDao.updateWorkout(updatedWorkout)
        }
    }

    /**
     * Get workout notes
     */
    suspend fun getWorkoutNotes(workoutId: Long): String? =
        withContext(Dispatchers.IO) {
            workoutDao.getWorkoutById(workoutId)?.notes
        }

    /**
     * Update workout name (separate from notes)
     */
    suspend fun updateWorkoutName(
        workoutId: Long,
        name: String?,
    ) = withContext(Dispatchers.IO) {
        val workout = workoutDao.getWorkoutById(workoutId)
        if (workout != null) {
            val updatedWorkout =
                workout.copy(
                    name = name?.takeIf { it.isNotBlank() },
                )
            workoutDao.updateWorkout(updatedWorkout)
        }
    }

    /**
     * Get programme completion notes
     */

    /**
     * Get workout for a specific date
     */
    suspend fun getWorkoutForDate(date: LocalDate): Workout? =
        withContext(Dispatchers.IO) {
            val startOfDay = date.atStartOfDay()
            val endOfDay = date.atTime(23, 59, 59)
            workoutDao.getWorkoutsInDateRange(startOfDay, endOfDay).firstOrNull()
        }

    suspend fun getWorkoutCountsByMonth(
        year: Int,
        month: Int,
    ): Map<LocalDate, Int> =
        withContext(Dispatchers.IO) {
            val startOfMonth = LocalDate.of(year, month, 1)
            val endOfMonth = startOfMonth.plusMonths(1)

            val dateCountList =
                workoutDao.getWorkoutCountsByDateRange(
                    startDate = startOfMonth.atStartOfDay(),
                    endDate = endOfMonth.atStartOfDay(),
                )

            dateCountList.associate {
                it.date.toLocalDate() to it.count
            }
        }

    suspend fun getWorkoutCountsByMonthWithStatus(
        year: Int,
        month: Int,
    ): Map<LocalDate, WorkoutDayInfo> =
        withContext(Dispatchers.IO) {
            val startOfMonth = LocalDate.of(year, month, 1)
            val endOfMonth = startOfMonth.plusMonths(1)
            val dateStatusCountList =
                workoutDao.getWorkoutCountsByDateRangeWithStatus(
                    startDate = startOfMonth.atStartOfDay(),
                    endDate = endOfMonth.atStartOfDay(),
                )

            // Group by date and aggregate counts by status
            val dayInfoMap = mutableMapOf<LocalDate, WorkoutDayInfo>()

            // First pass: Initialize all dates that have workouts
            val uniqueDates = dateStatusCountList.map { it.date.toLocalDate() }.toSet()
            uniqueDates.forEach { date ->
                dayInfoMap[date] = WorkoutDayInfo(0, 0, 0)
            }

            // Second pass: Fill in the counts for each status
            dateStatusCountList.forEach { item ->
                val date = item.date.toLocalDate()
                val currentInfo = dayInfoMap[date] ?: WorkoutDayInfo(0, 0, 0)

                // Update only the specific count for this status, preserving other counts
                dayInfoMap[date] =
                    when (item.status) {
                        WorkoutStatus.COMPLETED -> currentInfo.copy(completedCount = item.count)
                        WorkoutStatus.IN_PROGRESS -> currentInfo.copy(inProgressCount = item.count)
                        WorkoutStatus.NOT_STARTED -> currentInfo.copy(notStartedCount = item.count)
                    }
            }

            dayInfoMap
        }

    suspend fun getWorkoutsByWeek(weekStart: LocalDate): List<WorkoutSummary> =
        withContext(Dispatchers.IO) {
            val startOfWeek = weekStart.atStartOfDay()
            val endOfWeek = weekStart.plusDays(7).atStartOfDay()

            val workouts = workoutDao.getWorkoutsByWeek(startOfWeek, endOfWeek)
            workouts.map { workout ->
                val exercises = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
                val allSets = mutableListOf<SetLog>()
                exercises.forEach { exercise ->
                    allSets.addAll(setLogDao.getSetLogsForExercise(exercise.id))
                }

                val completedSets = allSets.filter { it.isCompleted }
                val totalWeight = completedSets.sumOf { (it.actualWeight * it.actualReps).toDouble() }.toFloat()

                WorkoutSummary(
                    id = workout.id,
                    date = workout.date,
                    name = workout.name,
                    exerciseCount = exercises.size,
                    setCount = completedSets.size,
                    totalWeight = totalWeight,
                    duration = workout.durationSeconds,
                    status = workout.status,
                    hasNotes = !workout.notes.isNullOrBlank(),
                    programmeId = workout.programmeId,
                    programmeName = workout.programmeWorkoutName,
                    weekNumber = workout.weekNumber,
                    dayNumber = workout.dayNumber,
                )
            }
        }

    suspend fun getWorkoutsByDateRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<WorkoutSummary> =
        withContext(Dispatchers.IO) {
            val workouts =
                workoutDao.getWorkoutsInDateRange(
                    startDate = startDate.atStartOfDay(),
                    endDate = endDate.atTime(23, 59, 59),
                )

            workouts.map { workout ->
                val exercises = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
                val allSets = mutableListOf<SetLog>()
                exercises.forEach { exercise ->
                    allSets.addAll(setLogDao.getSetLogsForExercise(exercise.id))
                }

                val completedSets = allSets.filter { it.isCompleted }
                val totalWeight = completedSets.sumOf { (it.actualWeight * it.actualReps).toDouble() }.toFloat()

                WorkoutSummary(
                    id = workout.id,
                    date = workout.date,
                    name = workout.name,
                    exerciseCount = exercises.size,
                    setCount = completedSets.size,
                    totalWeight = totalWeight,
                    duration = workout.durationSeconds,
                    status = workout.status,
                    hasNotes = !workout.notes.isNullOrBlank(),
                    programmeId = workout.programmeId,
                    programmeName = workout.programmeWorkoutName,
                    weekNumber = workout.weekNumber,
                    dayNumber = workout.dayNumber,
                )
            }
        }

    suspend fun getWorkoutCountByDateRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Int =
        withContext(Dispatchers.IO) {
            workoutDao
                .getWorkoutsInDateRange(
                    startDate = startDate.atStartOfDay(),
                    endDate = endDate.atTime(23, 59, 59),
                ).count { it.status == WorkoutStatus.COMPLETED }
        }

    /**
     * Get exercise 1RM from user profile
     */
    suspend fun getExercise1RM(exerciseName: String): Float? =
        withContext(Dispatchers.IO) {
            val exercise = getExerciseByName(exerciseName) ?: return@withContext null
            oneRMRepository.getExercise1RM(exercise.id)
        }

    /**
     * Clear all workout data (for developer tools)
     */
    suspend fun clearAllWorkoutData() =
        withContext(Dispatchers.IO) {
            // Delete all workouts and related data
            workoutDao.deleteAllWorkouts()
            exerciseLogDao.deleteAllExerciseLogs()
            setLogDao.deleteAllSetLogs()
            personalRecordDao.deleteAllPersonalRecords()
            globalExerciseProgressDao.deleteAllGlobalProgress()

            // CRITICAL: Also delete all 1RM related data
            // Delete all UserExerciseMax entries
            db.oneRMDao().deleteAllUserExerciseMaxes()

            // Delete all OneRMHistory entries
            db.oneRMDao().deleteAllOneRMHistory()

            // Reset all exercise usage counts to 0
            exerciseDao.resetAllUsageCounts()
        }

    // ===== INSIGHTS SECTION =====

    // Exercise Progress Analytics Methods
    data class ExerciseWorkoutSummary(
        val exerciseLogId: Long,
        val workoutId: Long,
        val workoutDate: LocalDateTime,
        val actualWeight: Float,
        val actualReps: Int,
        val sets: Int,
        val totalVolume: Float,
    )

    suspend fun getExerciseWorkoutsInDateRange(
        exerciseVariationId: Long,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<ExerciseWorkoutSummary> =
        withContext(Dispatchers.IO) {
            val startDateTime = startDate.atStartOfDay()
            val endDateTime = endDate.atTime(23, 59, 59)
            val exerciseLogs = exerciseLogDao.getExerciseLogsInDateRange(exerciseVariationId, startDateTime, endDateTime)

            val results =
                exerciseLogs.mapNotNull { exerciseLog ->
                    val workout = workoutDao.getWorkoutById(exerciseLog.workoutId)

                    if (workout == null) {
                        // Orphaned exercise log - skip
                        return@mapNotNull null
                    }

                    val sets = setLogDao.getSetLogsForExercise(exerciseLog.id)
                    val maxWeight = sets.filter { it.isCompleted }.maxOfOrNull { it.actualWeight } ?: 0f
                    val maxReps = sets.filter { it.isCompleted && it.actualWeight == maxWeight }.maxOfOrNull { it.actualReps } ?: 0
                    val totalSets = sets.count { it.isCompleted }
                    val totalVolume = sets.filter { it.isCompleted }.sumOf { (it.actualWeight * it.actualReps).toDouble() }.toFloat()

                    ExerciseWorkoutSummary(
                        exerciseLogId = exerciseLog.id,
                        workoutId = exerciseLog.workoutId,
                        workoutDate = workout.date,
                        actualWeight = maxWeight,
                        actualReps = maxReps,
                        sets = totalSets,
                        totalVolume = totalVolume,
                    )
                }
            results
        }

    suspend fun getSetLogsForExerciseInDateRange(
        exerciseVariationId: Long,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<SetLog> =
        withContext(Dispatchers.IO) {
            val startDateTime = startDate.atStartOfDay()
            val endDateTime = endDate.atTime(23, 59, 59)
            val exerciseLogs = exerciseLogDao.getExerciseLogsInDateRange(exerciseVariationId, startDateTime, endDateTime)

            val allSets = mutableListOf<SetLog>()
            exerciseLogs.forEach { exerciseLog ->
                val sets = setLogDao.getSetLogsForExercise(exerciseLog.id)
                allSets.addAll(sets)
            }
            allSets
        }

    suspend fun getPersonalRecordForExercise(
        exerciseVariationId: Long,
    ): PersonalRecord? = personalRecordRepository.getPersonalRecordForExercise(exerciseVariationId)

    private suspend fun getTotalSessionsForExercise(
        exerciseVariationId: Long,
    ): Int =
        withContext(Dispatchers.IO) {
            exerciseLogDao.getTotalSessionsForExercise(exerciseVariationId)
        }

    suspend fun getMaxWeightForExerciseInDateRange(
        exerciseVariationId: Long,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Float? =
        withContext(Dispatchers.IO) {
            val startDateTime = startDate.atStartOfDay()
            val endDateTime = endDate.atTime(23, 59, 59)
            setLogDao.getMaxWeightForExerciseInDateRange(
                exerciseVariationId,
                startDateTime.toString(),
                endDateTime.toString(),
            )
        }

    suspend fun getDistinctWorkoutDatesForExercise(
        exerciseVariationId: Long,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<LocalDate> =
        withContext(Dispatchers.IO) {
            val startDateTime = startDate.atStartOfDay()
            val endDateTime = endDate.atTime(23, 59, 59)
            exerciseLogDao
                .getDistinctWorkoutsForExercise(exerciseVariationId, startDateTime, endDateTime)
                .map { it.date.toLocalDate() }
                .distinct()
        }

    suspend fun getDateOfMaxWeightForExercise(
        exerciseVariationId: Long,
        weight: Float,
        startDate: LocalDate,
        endDate: LocalDate,
    ): LocalDate? =
        withContext(Dispatchers.IO) {
            // Get the actual workout date when this weight was achieved
            val startDateTime = startDate.atStartOfDay()
            val endDateTime = endDate.plusDays(1).atStartOfDay()

            val workoutDate = workoutDao.getWorkoutDateForMaxWeight(exerciseVariationId, weight, startDateTime, endDateTime)

            workoutDate?.toLocalDate()
        }

    suspend fun getExercisesSummary(): com.github.radupana.featherweight.service.GroupedExerciseSummary =
        withContext(Dispatchers.IO) {
            // Define Big Four exercises
            val bigFourNames =
                listOf(
                    "Barbell Back Squat",
                    "Barbell Deadlift",
                    "Barbell Bench Press",
                    "Barbell Overhead Press",
                )

            // Get all unique exercises from completed workouts
            val allExerciseVariationIds = exerciseLogDao.getAllUniqueExerciseVariationIds()

            val allSummaries =
                allExerciseVariationIds
                    .mapNotNull { exerciseVariationId ->
                        val globalProgress = getGlobalExerciseProgress(exerciseVariationId)
                        if (globalProgress != null) {
                            // Get exercise name for display
                            val exercise = exerciseVariationDao.getExerciseVariationById(exerciseVariationId)
                            val exerciseName = exercise?.name ?: "Unknown Exercise"

                            // Get actual PR for this exercise (not estimated)
                            val personalRecord = getPersonalRecordForExercise(exerciseVariationId)
                            val actualMaxWeight = personalRecord?.weight ?: globalProgress.currentWorkingWeight

                            // Get recent workout data for mini chart
                            val recentWorkouts =
                                getExerciseWorkoutsInDateRange(
                                    exerciseVariationId = exerciseVariationId,
                                    startDate =
                                        LocalDate
                                            .now()
                                            .minusDays(90),
                                    endDate = LocalDate.now(),
                                )

                            // Calculate progress percentage (last 30 days)
                            val thirtyDaysAgo =
                                LocalDate
                                    .now()
                                    .minusDays(30)
                            val monthlyWorkouts =
                                recentWorkouts.filter {
                                    it.workoutDate.toLocalDate().isAfter(thirtyDaysAgo)
                                }

                            var progressPercentage = 0f
                            if (monthlyWorkouts.size >= 2) {
                                val oldestWeight = monthlyWorkouts.lastOrNull()?.actualWeight ?: 0f
                                val newestWeight = monthlyWorkouts.firstOrNull()?.actualWeight ?: 0f
                                if (oldestWeight > 0) {
                                    progressPercentage = ((newestWeight - oldestWeight) / oldestWeight) * 100
                                }
                            }

                            // Create mini chart data (last 8 data points)
                            val miniChartData =
                                recentWorkouts
                                    .take(8)
                                    .map { log ->
                                        // Show actual weight, not estimated 1RM
                                        log.actualWeight
                                    }.reversed() // Oldest to newest for chart

                            com.github.radupana.featherweight.service.ExerciseSummary(
                                exerciseName = exerciseName,
                                currentMax = actualMaxWeight, // Show actual PR weight
                                progressPercentage = progressPercentage,
                                lastWorkout = globalProgress.lastUpdated,
                                sessionCount = getTotalSessionsForExercise(exerciseVariationId),
                                miniChartData = miniChartData,
                            )
                        } else {
                            null
                        }
                    }

            // Partition into Big Four and Others
            val (bigFourExercises, otherExercises) =
                allSummaries.partition { summary ->
                    summary.exerciseName in bigFourNames
                }

            // Sort Big Four by predefined order
            val sortedBigFour =
                bigFourExercises.sortedBy { summary ->
                    bigFourNames.indexOf(summary.exerciseName)
                }

            // Sort Others by session count (most used first)
            val sortedOthers = otherExercises.sortedByDescending { it.sessionCount }

            com.github.radupana.featherweight.service.GroupedExerciseSummary(
                bigFourExercises = sortedBigFour,
                otherExercises = sortedOthers,
            )
        }

    suspend fun getCompletedWorkoutCountSince(since: LocalDateTime): Int =
        withContext(Dispatchers.IO) {
            val workouts = db.workoutDao().getAllWorkouts()
            workouts.count { workout ->
                workout.status == WorkoutStatus.COMPLETED &&
                    workout.date.isAfter(since)
            }
        }

    suspend fun getWeeklyStreak(): Int =
        withContext(Dispatchers.IO) {
            val workouts =
                db
                    .workoutDao()
                    .getAllWorkouts()
                    .filter { it.status == WorkoutStatus.COMPLETED }
                    .sortedByDescending { it.date }

            if (workouts.isEmpty()) return@withContext 0

            var streak = 0
            var currentWeek = LocalDate.now().with(java.time.DayOfWeek.MONDAY)

            while (true) {
                val weekStart = currentWeek.atStartOfDay()
                val weekEnd = currentWeek.plusDays(6).atTime(23, 59, 59)

                val workoutsInWeek =
                    workouts.count { workout ->
                        workout.date.isAfter(weekStart) && workout.date.isBefore(weekEnd)
                    }

                if (workoutsInWeek >= 1) {
                    streak++
                    currentWeek = currentWeek.minusWeeks(1)
                } else {
                    break
                }
            }

            streak
        }

    // Get completed workouts with programme info using proper SQL pagination

    // Get completed programmes with workout count
    suspend fun getCompletedProgrammesPaged(
        page: Int,
        pageSize: Int = 20,
    ): List<ProgrammeSummary> =
        withContext(Dispatchers.IO) {
            val offset = page * pageSize
            // Use efficient paginated query at database level
            val programmes = programmeDao.getCompletedProgrammesPaged(pageSize, offset)

            programmes.map { programme ->
                val workouts = workoutDao.getWorkoutsByProgramme(programme.id)
                val completedWorkouts = workouts.count { it.status == WorkoutStatus.COMPLETED }

                ProgrammeSummary(
                    id = programme.id,
                    name = programme.name,
                    startDate = programme.startedAt ?: programme.createdAt,
                    completionDate = programme.completedAt!!,
                    completedAt = programme.completedAt,
                    durationWeeks = programme.durationWeeks,
                    completedWorkouts = completedWorkouts,
                    totalWorkouts = workouts.size,
                    programmeType = programme.programmeType,
                    difficulty = programme.difficulty,
                    completionNotes = programme.completionNotes,
                )
            }
        }

    // Get programme history details
    suspend fun getProgrammeHistoryDetails(programmeId: Long): ProgrammeHistoryDetails? =
        withContext(Dispatchers.IO) {
            val programme = programmeDao.getProgrammeById(programmeId) ?: return@withContext null
            val weeks = programmeDao.getWeeksForProgramme(programmeId)

            // Get all completed workouts for this programme
            val workouts =
                workoutDao
                    .getWorkoutsByProgramme(programmeId)
                    .filter { it.status == WorkoutStatus.COMPLETED }
                    .sortedBy { workout -> (workout.weekNumber ?: 0) * 10 + (workout.dayNumber ?: 0) }

            // Group workouts by week
            val workoutsByWeek = workouts.groupBy { it.weekNumber ?: 0 }

            (1..programme.durationWeeks).map { weekNum ->
                val week = weeks.find { it.weekNumber == weekNum }
                val weekWorkouts = workoutsByWeek[weekNum] ?: emptyList()

                ProgrammeWeekHistory(
                    weekNumber = weekNum,
                    weekName = week?.name ?: "Week $weekNum",
                    workouts =
                        weekWorkouts
                            .map { workout ->
                                val exercises = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
                                val sets =
                                    exercises.flatMap { exercise ->
                                        setLogDao.getSetLogsForExercise(exercise.id)
                                    }

                                WorkoutHistoryDetail(
                                    id = workout.id,
                                    name = workout.programmeWorkoutName ?: "Workout",
                                    date = workout.date,
                                    exerciseNames =
                                        exercises.take(3).mapNotNull { exerciseLog ->
                                            exerciseVariationDao.getExerciseVariationById(exerciseLog.exerciseVariationId)?.name
                                        },
                                    totalExercises = exercises.size,
                                    totalVolume =
                                        sets
                                            .filter { set -> set.isCompleted }
                                            .sumOf { set -> (set.actualWeight * set.actualReps).toDouble() }
                                            .toFloat(),
                                    duration = workout.durationSeconds,
                                )
                            }.sortedBy { it.date },
                )
            }

            // Get all workouts for this programme (including incomplete ones)
            val allWorkouts = workoutDao.getWorkoutsByProgramme(programmeId)
            val completedWorkouts = allWorkouts.filter { it.status == WorkoutStatus.COMPLETED }

            // Calculate program duration in days
            val programDurationDays =
                if (programme.completedAt != null && programme.startedAt != null) {
                    java.time.temporal.ChronoUnit.DAYS
                        .between(programme.startedAt, programme.completedAt)
                        .toInt() + 1
                } else {
                    programme.durationWeeks * 7
                }

            // Create workout history entries for all workouts
            val workoutHistory =
                allWorkouts
                    .map { workout ->
                        WorkoutHistoryEntry(
                            workoutId = workout.id,
                            workoutName = workout.programmeWorkoutName ?: "Workout",
                            weekNumber = workout.weekNumber ?: 0,
                            dayNumber = workout.dayNumber ?: 0,
                            completed = workout.status == WorkoutStatus.COMPLETED,
                            completedAt = if (workout.status == WorkoutStatus.COMPLETED) workout.date else null,
                        )
                    }.sortedBy { it.weekNumber * 10 + it.dayNumber }

            ProgrammeHistoryDetails(
                id = programme.id,
                name = programme.name,
                programmeType = programme.programmeType,
                difficulty = programme.difficulty,
                durationWeeks = programme.durationWeeks,
                startedAt = programme.startedAt,
                completedAt = programme.completedAt,
                completedWorkouts = completedWorkouts.size,
                totalWorkouts = allWorkouts.size,
                programDurationDays = programDurationDays,
                workoutHistory = workoutHistory,
                completionNotes = programme.completionNotes,
            )
        }

    // Copy workout for "Repeat Workout" feature
    suspend fun copyWorkoutAsFreestyle(workoutId: Long): Long =
        withContext(Dispatchers.IO) {
            val originalWorkout =
                workoutDao.getWorkoutById(workoutId)
                    ?: throw IllegalArgumentException("Workout not found")

            val exercises = exerciseLogDao.getExerciseLogsForWorkout(workoutId)

            // Create new freestyle workout
            val newWorkout =
                Workout(
                    date = LocalDateTime.now(),
                    notes = "Repeated from ${originalWorkout.programmeWorkoutName ?: "previous workout"}",
                    status = WorkoutStatus.NOT_STARTED,
                    isProgrammeWorkout = false,
                )
            val newWorkoutId = workoutDao.insertWorkout(newWorkout)

            // Copy exercises and sets
            exercises.forEach { exercise ->
                val sets = setLogDao.getSetLogsForExercise(exercise.id)

                val newExercise =
                    ExerciseLog(
                        workoutId = newWorkoutId,
                        exerciseVariationId = exercise.exerciseVariationId,
                        exerciseOrder = exercise.exerciseOrder,
                        notes = exercise.notes,
                    )
                val newExerciseId = exerciseLogDao.insertExerciseLog(newExercise)

                // Copy sets with actual values pre-populated in both target and actual fields
                sets.forEach { set ->
                    val newSet =
                        SetLog(
                            exerciseLogId = newExerciseId,
                            setOrder = set.setOrder,
                            // Freestyle workouts should NOT have target values - this is false data
                            targetReps = null,
                            targetWeight = null,
                            // Pre-populate actual values so sets can be immediately marked complete
                            actualReps = set.actualReps,
                            actualWeight = set.actualWeight,
                            // Don't copy RPE - let user set it fresh
                            actualRpe = null,
                            isCompleted = false,
                        )
                    insertSetLog(newSet)
                }
            }

            newWorkoutId
        }

    // Training Analysis methods
    suspend fun saveTrainingAnalysis(analysis: TrainingAnalysis) =
        withContext(Dispatchers.IO) {
            db.trainingAnalysisDao().insertAnalysis(analysis)
        }

    suspend fun getLatestTrainingAnalysis(): TrainingAnalysis? =
        withContext(Dispatchers.IO) {
            db.trainingAnalysisDao().getLatestAnalysis()
        }

    // ParseRequest methods
    suspend fun createParseRequest(rawText: String): Long =
        withContext(Dispatchers.IO) {
            val request =
                ParseRequest(
                    rawText = rawText,
                    status = ParseStatus.PROCESSING,
                    createdAt = LocalDateTime.now(),
                )
            db.parseRequestDao().insert(request)
        }

    suspend fun updateParseRequest(request: ParseRequest) =
        withContext(Dispatchers.IO) {
            db.parseRequestDao().update(request)
        }

    suspend fun getParseRequest(id: Long): ParseRequest? =
        withContext(Dispatchers.IO) {
            db.parseRequestDao().getRequest(id)
        }

    fun getAllParseRequests(): Flow<List<ParseRequest>> = db.parseRequestDao().getAllRequests()

    suspend fun hasPendingParseRequest(): Boolean =
        withContext(Dispatchers.IO) {
            db.parseRequestDao().getPendingRequestCount() > 0
        }

    suspend fun deleteParseRequest(request: ParseRequest) =
        withContext(Dispatchers.IO) {
            Log.d("FeatherweightRepository", "Deleting parse request with ID: ${request.id}, status: ${request.status}")
            try {
                // Use deleteById for more reliable deletion
                db.parseRequestDao().deleteById(request.id)
            } catch (e: android.database.sqlite.SQLiteException) {
                Log.e("FeatherweightRepository", "Failed to delete parse request ${request.id}", e)
                throw e
            }
        }
}
