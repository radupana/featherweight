package com.github.radupana.featherweight.repository

import android.app.Application
import android.util.Log
import com.github.radupana.featherweight.ai.ProgrammeType
import com.github.radupana.featherweight.ai.WeightCalculator
import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.GeneratedProgrammePreview
import com.github.radupana.featherweight.data.GlobalExerciseProgress
import com.github.radupana.featherweight.data.PRType
import com.github.radupana.featherweight.data.PendingOneRMUpdate
import com.github.radupana.featherweight.data.PersonalRecord
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.SwapHistoryCount
import com.github.radupana.featherweight.data.TrainingAnalysis
import com.github.radupana.featherweight.data.UserPreferences
import com.github.radupana.featherweight.data.VolumeLevel
import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.data.WorkoutStatus
import com.github.radupana.featherweight.data.exercise.Equipment
import com.github.radupana.featherweight.data.exercise.ExerciseCategory
import com.github.radupana.featherweight.data.exercise.ExerciseCorrelationSeeder
import com.github.radupana.featherweight.data.exercise.ExerciseDifficulty
import com.github.radupana.featherweight.data.exercise.ExerciseSeeder
import com.github.radupana.featherweight.data.exercise.ExerciseVariation
import com.github.radupana.featherweight.data.exercise.ExerciseWithDetails
import com.github.radupana.featherweight.data.exercise.MovementPattern
import com.github.radupana.featherweight.data.exercise.MuscleGroup
import com.github.radupana.featherweight.data.exercise.VariationMuscle
import com.github.radupana.featherweight.data.model.WorkoutTemplate
import com.github.radupana.featherweight.data.model.WorkoutTemplateConfig
import com.github.radupana.featherweight.data.profile.OneRMHistory
import com.github.radupana.featherweight.data.profile.OneRMType
import com.github.radupana.featherweight.data.profile.OneRMWithExerciseName
import com.github.radupana.featherweight.data.profile.UserExerciseMax
import com.github.radupana.featherweight.data.profile.UserProfile
import com.github.radupana.featherweight.data.programme.ExerciseStructure
import com.github.radupana.featherweight.data.programme.Programme
import com.github.radupana.featherweight.data.programme.ProgrammeCompletionStats
import com.github.radupana.featherweight.data.programme.ProgrammeDifficulty
import com.github.radupana.featherweight.data.programme.ProgrammeInsights
import com.github.radupana.featherweight.data.programme.ProgrammeProgress
import com.github.radupana.featherweight.data.programme.ProgrammeSeeder
import com.github.radupana.featherweight.data.programme.ProgrammeStatus
import com.github.radupana.featherweight.data.programme.ProgrammeWeek
import com.github.radupana.featherweight.data.programme.ProgrammeWorkout
import com.github.radupana.featherweight.data.programme.ProgrammeWorkoutParser
import com.github.radupana.featherweight.data.programme.ProgressionType
import com.github.radupana.featherweight.data.programme.RepsStructure
import com.github.radupana.featherweight.data.programme.StrengthImprovement
import com.github.radupana.featherweight.data.programme.WeightBasis
import com.github.radupana.featherweight.data.programme.WorkoutStructure
import com.github.radupana.featherweight.domain.ExerciseHistory
import com.github.radupana.featherweight.domain.ExerciseStats
import com.github.radupana.featherweight.domain.SmartSuggestions
import com.github.radupana.featherweight.service.FreestyleIntelligenceService
import com.github.radupana.featherweight.service.GlobalProgressTracker
import com.github.radupana.featherweight.service.OneRMService
import com.github.radupana.featherweight.service.PRDetectionService
import com.github.radupana.featherweight.service.ProgressionService
import com.github.radupana.featherweight.service.WorkoutTemplateGeneratorService
import com.github.radupana.featherweight.service.WorkoutTemplateWeightService
import com.github.radupana.featherweight.util.WeightFormatter
import com.github.radupana.featherweight.validation.ExerciseValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import com.github.radupana.featherweight.data.programme.ProgrammeType as DataProgrammeType

data class NextProgrammeWorkoutInfo(
    val workoutStructure: WorkoutStructure,
    val actualWeekNumber: Int,
)

data class WorkoutFilters(
    val dateRange: Pair<LocalDate, LocalDate>? = null,
    val exercises: List<String> = emptyList(),
    val muscleGroups: List<String> = emptyList(),
    val programmeId: Long? = null,
)

data class WorkoutDayInfo(
    val completedCount: Int,
    val inProgressCount: Int,
    val notStartedCount: Int,
)

data class WorkoutSummary(
    val id: Long,
    val date: LocalDateTime,
    val name: String?,
    val exerciseCount: Int,
    val setCount: Int,
    val totalWeight: Float,
    val duration: Long?, // seconds
    val status: WorkoutStatus,
    val hasNotes: Boolean = false,
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
    private val userPreferences = UserPreferences(application)

    // Sub-repositories
    private val exerciseRepository = ExerciseRepository(db)

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
    private val freestyleIntelligenceService = FreestyleIntelligenceService(this, db.globalExerciseProgressDao())
    private val prDetectionService = PRDetectionService(personalRecordDao, setLogDao, exerciseVariationDao)
    private val workoutTemplateGeneratorService =
        WorkoutTemplateGeneratorService(exerciseVariationDao)
    private val workoutTemplateWeightService =
        WorkoutTemplateWeightService(this, db.oneRMDao(), setLogDao, exerciseLogDao, freestyleIntelligenceService)

    // StateFlow for pending 1RM updates
    private val _pendingOneRMUpdates = MutableStateFlow<List<PendingOneRMUpdate>>(emptyList())
    val pendingOneRMUpdates: StateFlow<List<PendingOneRMUpdate>> = _pendingOneRMUpdates.asStateFlow()

    // Clear pending updates after user has seen them
    fun clearPendingOneRMUpdates() {
        _pendingOneRMUpdates.value = emptyList()
    }

    // Apply a pending 1RM update by updating the user's max
    suspend fun applyOneRMUpdate(update: PendingOneRMUpdate) {
        val userId = getCurrentUserId()
        // Round weight to nearest 0.25
        val roundedWeight = WeightFormatter.roundToNearestQuarter(update.suggestedMax)

        db.oneRMDao().upsertExerciseMax(
            userId = userId,
            exerciseVariationId = update.exerciseVariationId,
            maxWeight = roundedWeight,
            notes = "Updated from ${update.source}",
        )

        // Save to history with the correct date from the workout
        saveOneRMToHistory(
            userId = userId,
            exerciseVariationId = update.exerciseVariationId,
            oneRM = roundedWeight,
            context = update.source, // Use the source from the update (e.g., "3×100kg @ RPE 9")
            recordedAt = update.workoutDate, // Use the workout date from the update
        )

        // Remove this update from pending list
        _pendingOneRMUpdates.value =
            _pendingOneRMUpdates.value.filter {
                it.exerciseVariationId != update.exerciseVariationId
            }
    }

    private val exerciseSeeder =
        ExerciseSeeder(
            exerciseDao = exerciseDao,
            exerciseCoreDao = exerciseCoreDao,
            exerciseVariationDao = exerciseVariationDao,
            variationMuscleDao = variationMuscleDao,
            variationAliasDao = variationAliasDao,
            variationInstructionDao = variationInstructionDao,
            context = application,
        )
    private val programmeSeeder = ProgrammeSeeder(programmeDao, exerciseDao)

    private val exerciseCorrelationSeeder = ExerciseCorrelationSeeder(exerciseCorrelationDao)

    fun getCurrentUserId(): Long = userPreferences.getCurrentUserId()

    // Initialize with seed data for testing
    suspend fun seedDatabaseIfEmpty() {
        withContext(Dispatchers.IO) {
            val exerciseCount = exerciseDao.getAllExercisesWithDetails().size

            // Always seed exercises if there are none
            if (exerciseCount == 0) {
                exerciseSeeder.seedExercises()
            }

            // Always check and seed missing programme templates
            programmeSeeder.seedPopularProgrammes()

            // Seed exercise correlations if none exist
            if (exerciseCorrelationDao.getCount() == 0) {
                exerciseCorrelationSeeder.seedExerciseCorrelations()
            }
        }
    }

    // ===== EXERCISE METHODS (Delegated to ExerciseRepository) =====

    suspend fun getAllExercises(): List<ExerciseVariation> = exerciseRepository.getAllExercises()

    suspend fun getExerciseById(id: Long): ExerciseVariation? = exerciseRepository.getExerciseById(id)

    suspend fun searchExercises(query: String): List<ExerciseVariation> = exerciseRepository.searchExercises(query)

    suspend fun getExercisesByCategory(category: ExerciseCategory): List<ExerciseVariation> = exerciseRepository.getExercisesByCategory(category)

    // Get exercise with full details including muscles
    suspend fun getExerciseWithDetails(id: Long): ExerciseWithDetails? {
        val variation = exerciseRepository.getExerciseById(id) ?: return null
        val muscles = variationMuscleDao.getMusclesForVariation(id)
        val aliases = variationAliasDao.getAliasesForVariation(id)
        val instructions = variationInstructionDao.getInstructionsForVariation(id)
        return ExerciseWithDetails(
            variation = variation,
            muscles = muscles,
            aliases = aliases,
            instructions = instructions,
        )
    }

    // Get muscles for a variation
    suspend fun getMusclesForVariation(variationId: Long): List<VariationMuscle> = variationMuscleDao.getMusclesForVariation(variationId)

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
            userId = getCurrentUserId(),
        )

    // ===== EXISTING WORKOUT METHODS (Updated to work with new Exercise system) =====

    // Basic CRUD operations
    suspend fun insertWorkout(workout: Workout): Long = workoutDao.insertWorkout(workout)

    suspend fun getExercisesForWorkout(workoutId: Long): List<ExerciseLog> = exerciseRepository.getExercisesForWorkout(workoutId)

    suspend fun getSetsForExercise(exerciseLogId: Long): List<SetLog> = exerciseRepository.getSetsForExercise(exerciseLogId)

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

        val updatedWorkout = workout.copy(status = WorkoutStatus.COMPLETED, durationSeconds = durationSeconds)

        workoutDao.updateWorkout(updatedWorkout)

        // Record performance data for programme workouts before updating progress
        if (workout.isProgrammeWorkout && workout.programmeId != null) {
            recordWorkoutPerformanceData(workoutId, workout.programmeId)
        }

        // Update global exercise progress for ALL workouts (programme or freestyle)
        val userId = getCurrentUserId()
        val pendingOneRMUpdates = globalProgressTracker.updateProgressAfterWorkout(workoutId, userId)

        // Store pending 1RM updates for later retrieval by UI
        if (pendingOneRMUpdates.isNotEmpty()) {
            _pendingOneRMUpdates.value = pendingOneRMUpdates
        }

        // Update programme progress if this is a programme workout
        if (workout.isProgrammeWorkout && workout.programmeId != null && workout.weekNumber != null && workout.dayNumber != null) {
            updateProgrammeProgressAfterWorkout(workout.programmeId, workout.weekNumber, workout.dayNumber)
        }
    }

    private suspend fun updateProgrammeProgressAfterWorkout(
        programmeId: Long,
        weekNumber: Int?,
        dayNumber: Int?,
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
        } catch (e: Exception) {
            Log.e("FeatherweightRepository", "Programme progress update failed", e)
            // Programme progress update failed - this is not critical for app functionality
        }
    }

    // History functionality
    suspend fun getWorkoutHistory(): List<WorkoutSummary> {
        val allWorkouts = workoutDao.getAllWorkouts()
        return allWorkouts
            .mapNotNull { workout ->
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
    }

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

    suspend fun getSmartSuggestions(
        exerciseVariationId: Long,
        currentWorkoutId: Long,
    ): SmartSuggestions? {
        // Try history first
        val history = getExerciseHistory(exerciseVariationId, currentWorkoutId)
        if (history != null && history.sets.isNotEmpty()) {
            val lastCompletedSets = history.sets.filter { it.isCompleted }
            if (lastCompletedSets.isNotEmpty()) {
                val mostCommonWeight =
                    lastCompletedSets
                        .groupBy { it.actualWeight }
                        .maxByOrNull { it.value.size }
                        ?.key ?: 0f
                val mostCommonReps =
                    lastCompletedSets
                        .groupBy { it.actualReps }
                        .maxByOrNull { it.value.size }
                        ?.key ?: 0
                val avgRpe =
                    lastCompletedSets
                        .mapNotNull { it.actualRpe }
                        .average()
                        .takeIf { !it.isNaN() }
                        ?.toFloat()

                return SmartSuggestions(
                    suggestedWeight = mostCommonWeight,
                    suggestedReps = mostCommonReps,
                    suggestedRpe = avgRpe,
                    lastWorkoutDate = history.lastWorkoutDate,
                    confidence = "Last workout",
                    reasoning = "Based on your last workout performance",
                )
            }
        }

        // Fallback to overall stats
        val stats = getExerciseStats(exerciseVariationId)
        if (stats != null) {
            return SmartSuggestions(
                suggestedWeight = stats.avgWeight,
                suggestedReps = stats.avgReps,
                suggestedRpe = stats.avgRpe,
                lastWorkoutDate = null,
                confidence = "Average from ${stats.totalSets} sets",
                reasoning = "Historical average from your past ${stats.totalSets} sets",
            )
        }

        return null
    }

    /**
     * Enhanced smart suggestions using global progress tracking and RPE analysis
     */
    suspend fun getSmartSuggestionsEnhanced(
        exerciseVariationId: Long,
        targetReps: Int? = null,
    ): SmartSuggestions {
        val userId = getCurrentUserId()

        // Use the freestyle intelligence service for advanced suggestions
        return freestyleIntelligenceService.getIntelligentSuggestions(
            exerciseVariationId = exerciseVariationId,
            userId = userId,
            targetReps = targetReps,
        )
    }

    suspend fun getWorkoutById(workoutId: Long): Workout? = workoutDao.getWorkoutById(workoutId)

    suspend fun getExerciseLogsForWorkout(workoutId: Long): List<ExerciseLog> = exerciseLogDao.getExerciseLogsForWorkout(workoutId)

    suspend fun getSetLogsForExercise(exerciseLogId: Long): List<SetLog> = setLogDao.getSetLogsForExercise(exerciseLogId)

    // Delete an exercise log (will cascade delete all its sets due to foreign key)
    suspend fun deleteExerciseLog(exerciseLogId: Long) = exerciseRepository.deleteExerciseLog(exerciseLogId)

    // Update exercise order
    suspend fun updateExerciseOrder(
        exerciseLogId: Long,
        newOrder: Int,
    ) = exerciseRepository.updateExerciseOrder(exerciseLogId, newOrder)

    // Delete an entire workout (will cascade delete all exercises and sets)
    suspend fun deleteWorkout(workoutId: Long) = workoutDao.deleteWorkout(workoutId)

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
        userId: Long,
        originalExerciseId: Long,
        swappedToExerciseId: Long,
        workoutId: Long? = null,
        programmeId: Long? = null,
    ) = exerciseRepository.recordExerciseSwap(userId, originalExerciseId, swappedToExerciseId, workoutId, programmeId)

    // Get swap history for exercise
    suspend fun getSwapHistoryForExercise(
        userId: Long,
        exerciseId: Long,
    ): List<SwapHistoryCount> = exerciseRepository.getSwapHistoryForExercise(userId, exerciseId)

    // ===== ANALYTICS METHODS =====

    // Volume analytics
    suspend fun getWeeklyVolumeTotal(startDate: LocalDateTime): Float =
        withContext(Dispatchers.IO) {
            val endDate = startDate.plusDays(7)
            val allWorkouts = workoutDao.getAllWorkouts()
            val workouts =
                allWorkouts.filter {
                    it.date >= startDate && it.date < endDate && it.status == WorkoutStatus.COMPLETED
                }

            var totalVolume = 0f
            workouts.forEach { workout ->
                val exercises = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
                exercises.forEach { exercise ->
                    val sets = setLogDao.getSetLogsForExercise(exercise.id)
                    sets.filter { it.isCompleted }.forEach { set ->
                        totalVolume += set.actualWeight * set.actualReps
                    }
                }
            }
            totalVolume
        }

    suspend fun getMonthlyVolumeTotal(startDate: LocalDateTime): Float =
        withContext(Dispatchers.IO) {
            val endDate = startDate.plusMonths(1)
            val allWorkouts = workoutDao.getAllWorkouts()
            val workouts =
                allWorkouts.filter {
                    it.date >= startDate && it.date < endDate && it.status == WorkoutStatus.COMPLETED
                }

            var totalVolume = 0f
            workouts.forEach { workout ->
                val exercises = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
                exercises.forEach { exercise ->
                    val sets = setLogDao.getSetLogsForExercise(exercise.id)
                    sets.filter { it.isCompleted }.forEach { set ->
                        totalVolume += set.actualWeight * set.actualReps
                    }
                }
            }
            totalVolume
        }

    // Strength progression analytics
    suspend fun getPersonalRecords(exerciseVariationId: Long): List<Pair<Float, LocalDateTime>> =
        withContext(Dispatchers.IO) {
            // Get PRs from PersonalRecord table
            val prs = personalRecordDao.getRecentPRsForExercise(exerciseVariationId, 100) // Get up to 100 PRs

            // Convert to the expected format: List of (estimated1RM, date) pairs
            val records =
                prs
                    .map { pr ->
                        Pair(pr.estimated1RM ?: pr.weight, pr.recordDate)
                    }.sortedBy { it.second } // Sort by date ascending

            records
        }

    // getEstimated1RM is defined above at line 2023

    suspend fun getRecentSetLogsForExercise(
        exerciseVariationId: Long,
        daysBack: Int,
    ): List<SetLog> =
        withContext(Dispatchers.IO) {
            val sinceDate = LocalDateTime.now().minusDays(daysBack.toLong())
            setLogDao.getSetsForExerciseSince(exerciseVariationId, sinceDate.toString())
        }

    suspend fun generateWorkoutFromTemplate(
        template: WorkoutTemplate,
        config: WorkoutTemplateConfig,
    ): Long =
        withContext(Dispatchers.IO) {
            // Create workout directly here since the service method doesn't exist
            val workoutId =
                workoutDao.insertWorkout(
                    Workout(
                        id = 0,
                        name = template.name,
                        date = java.time.LocalDateTime.now(),
                        status = WorkoutStatus.IN_PROGRESS,
                    ),
                )
            workoutId
        }

    suspend fun applyTemplateWeightSuggestions(
        workoutId: Long,
        config: WorkoutTemplateConfig,
        userId: Long,
    ) = workoutTemplateWeightService.applyWeightSuggestions(workoutId, config, userId)

    // Performance insights
    suspend fun getTrainingFrequency(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): Int =
        withContext(Dispatchers.IO) {
            workoutDao.getAllWorkouts().count { workout ->
                workout.date >= startDate &&
                    workout.date < endDate &&
                    workout.status == WorkoutStatus.COMPLETED
            }
        }

    suspend fun getAverageRPE(
        exerciseVariationId: Long? = null,
        daysSince: Int = 30,
    ): Float? =
        withContext(Dispatchers.IO) {
            val cutoffDate = LocalDateTime.now().minusDays(daysSince.toLong())
            val allWorkouts = workoutDao.getAllWorkouts().filter { it.date >= cutoffDate }
            val allRPEs = mutableListOf<Float>()

            allWorkouts.forEach { workout ->
                val exercises = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
                val targetExercises =
                    if (exerciseVariationId != null) {
                        exercises.filter { it.exerciseVariationId == exerciseVariationId }
                    } else {
                        exercises
                    }

                targetExercises.forEach { exercise ->
                    val sets = setLogDao.getSetLogsForExercise(exercise.id)
                    sets.filter { it.isCompleted && it.actualRpe != null }.forEach { set ->
                        set.actualRpe?.let { allRPEs.add(it) }
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
                    .filter { it.status == WorkoutStatus.COMPLETED }
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
            // Get the most recent PR from the PersonalRecord table
            val recentPRs = personalRecordDao.getRecentPRs(1)

            if (recentPRs.isNotEmpty()) {
                val mostRecentPR = recentPRs.first()

                // Get exercise name from variation
                val variation = exerciseVariationDao.getExerciseVariationById(mostRecentPR.exerciseVariationId)
                val exerciseName = variation?.name ?: "Unknown Exercise"

                // Return exercise name and the 1RM value (not the actual weight lifted)
                return@withContext Pair(exerciseName, mostRecentPR.estimated1RM ?: mostRecentPR.weight)
            }

            return@withContext null
        }

    suspend fun getAverageTrainingDaysPerWeek(): Float =
        withContext(Dispatchers.IO) {
            val allWorkouts =
                workoutDao
                    .getAllWorkouts()
                    .filter { it.status == WorkoutStatus.COMPLETED }
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
            val exerciseNames = listOf("Barbell Bench Press", "Barbell Back Squat", "Barbell Deadlift", "Barbell Overhead Press")
            val improvements = mutableListOf<Float>()

            exerciseNames.forEach { exerciseName ->
                val exercise = getExerciseByName(exerciseName)
                if (exercise == null) return@forEach
                val records = getPersonalRecords(exercise.id)
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

            val result = if (improvements.isNotEmpty()) improvements.average().toFloat() else null
            result
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
        userMaxes: Map<Long, Float> = emptyMap(),
    ): Long =
        withContext(Dispatchers.IO) {
            val programme =
                programmeDao.getProgrammeById(programmeId)
                    ?: throw IllegalArgumentException("Programme not found")

            // Check if this is a custom programme (AI-generated or user-created)
            if (programme.isCustom) {
                return@withContext createWorkoutFromCustomProgramme(
                    programmeId = programmeId,
                    weekNumber = weekNumber,
                    dayNumber = dayNumber,
                    userMaxes = userMaxes,
                )
            }

            // Find the template this programme was created from
            val template =
                programmeDao.getAllTemplates().find { it.name == programme.name }
                    ?: throw IllegalArgumentException("Programme template not found")

            // Parse the JSON structure
            val structure =
                ProgrammeWorkoutParser.parseStructure(template.jsonStructure)
                    ?: throw IllegalArgumentException("Invalid programme structure")

            // For programmes with single week templates that repeat (like Starting Strength)
            // Use modulo to find the template week
            val templateWeekNumber =
                if (structure.weeks.size == 1) {
                    1 // Always use week 1 if only one week is defined
                } else {
                    ((weekNumber - 1) % structure.weeks.size) + 1
                }

            // Find the specific workout for this week and day
            // All programmes MUST have sequential days (1,2,3,4...)
            val workoutStructure =
                ProgrammeWorkoutParser.getWorkoutForWeekAndDay(
                    structure,
                    templateWeekNumber,
                    dayNumber,
                ) ?: throw IllegalArgumentException("Workout not found for template week $templateWeekNumber, day $dayNumber")

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
                    )
                val exerciseLogId = exerciseLogDao.insertExerciseLog(exerciseLog)

                // Create sets for this exercise
                createSetsFromStructure(exerciseLogId, exerciseStructure, userMaxes, programme, weekNumber)
            }

            workoutId
        }

    private suspend fun createWorkoutFromCustomProgramme(
        programmeId: Long,
        weekNumber: Int,
        dayNumber: Int,
        userMaxes: Map<Long, Float> = emptyMap(),
    ): Long {
        // Get the programme
        val programme =
            requireNotNull(programmeDao.getProgrammeById(programmeId)) {
                "Programme not found"
            }

        // Get the progress to find which workout to use
        val progress =
            requireNotNull(programmeDao.getProgressForProgramme(programmeId)) {
                "Programme progress not found"
            }

        // Get all workouts for this programme
        val allWorkouts = programmeDao.getAllWorkoutsForProgramme(programmeId)
        require(allWorkouts.isNotEmpty()) { "No workouts found for custom programme" }

        // Find the workout index based on completed workouts
        val workoutIndex = progress.completedWorkouts
        require(workoutIndex < allWorkouts.size) { "All workouts already completed" }

        val programmeWorkout = allWorkouts[workoutIndex]

        // Parse workout structure
        val workoutStructure =
            try {
                val json =
                    kotlinx.serialization.json.Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    }
                json.decodeFromString<WorkoutStructure>(
                    programmeWorkout.workoutStructure,
                )
            } catch (e: Exception) {
                // Try to fix invalid JSON
                val fixedJson =
                    programmeWorkout.workoutStructure
                        .replace("\"reps\":,", "\"reps\":\"8-12\",")
                        .replace("\"reps\": ,", "\"reps\":\"8-12\",")

                try {
                    val json =
                        kotlinx.serialization.json.Json {
                            ignoreUnknownKeys = true
                            isLenient = true
                        }
                    json.decodeFromString<WorkoutStructure>(fixedJson)
                } catch (e2: Exception) {
                    error("Failed to parse workout structure: ${e2.message}")
                }
            }

        // Create the workout entry
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
                )
            val exerciseLogId = exerciseLogDao.insertExerciseLog(exerciseLog)

            // Create sets for this exercise
            createSetsFromStructure(exerciseLogId, exerciseStructure, userMaxes, programme, weekNumber)
        }

        return workoutId
    }

    private suspend fun createExerciseLogFromStructure(
        workoutId: Long,
        exerciseStructure: ExerciseStructure,
        exerciseOrder: Int,
    ): ExerciseLog {
        // Try to find existing exercise in database
        val exercise =
            try {
                searchExercises(exerciseStructure.name).firstOrNull()
            } catch (e: Exception) {
                null
            }

        // If exercise not found, log and use null (we'll handle this in the UI)
        val exerciseId = exercise?.id

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
            exerciseVariationId = exerciseId ?: 0L,
            exerciseOrder = exerciseOrder,
            notes = notes,
        )
    }

    private suspend fun createSetsFromStructure(
        exerciseLogId: Long,
        exerciseStructure: ExerciseStructure,
        userMaxes: Map<Long, Float>,
        programme: Programme? = null,
        weekNumber: Int? = null,
    ) {
        repeat(exerciseStructure.sets) { setIndex ->
            val reps =
                ProgrammeWorkoutParser.parseRepsForSet(
                    exerciseStructure.reps,
                    setIndex,
                )

            var intensity =
                exerciseStructure.intensity?.getOrNull(setIndex)
                    ?: exerciseStructure.intensity?.firstOrNull()

            // Apply week-specific modifications for wave progression (e.g., Wendler 5/3/1)
            if (programme != null && weekNumber != null && intensity != null) {
                val progressionRules = programme.getProgressionRulesObject()
                if (progressionRules?.type == ProgressionType.WAVE && progressionRules.weeklyPercentages != null) {
                    // For wave progression, override intensity with week-specific percentage
                    val cycleLength = progressionRules.cycleLength ?: 3
                    val cycleWeek = ((weekNumber - 1) % cycleLength)

                    // Get the percentages for this week of the cycle
                    if (cycleWeek < progressionRules.weeklyPercentages.size) {
                        val weekPercentages = progressionRules.weeklyPercentages[cycleWeek]

                        // Override the intensity with the week-specific value
                        if (setIndex < weekPercentages.size) {
                            // Convert from decimal to percentage (0.65 -> 65)
                            intensity = (weekPercentages[setIndex] * 100).toInt()
                        }
                    }
                }
            }

            val weight =
                calculateIntelligentWeight(
                    exerciseStructure = exerciseStructure,
                    reps = reps,
                    userMaxes = userMaxes,
                    intensity = intensity,
                    programme = programme,
                )

            val setLog =
                SetLog(
                    exerciseLogId = exerciseLogId,
                    setOrder = setIndex,
                    targetReps = reps,
                    targetWeight = weight,
                    // Pre-populate actual values with target values for programme workouts
                    actualReps = reps,
                    actualWeight = weight ?: 0f,
                    actualRpe = null,
                    isCompleted = false,
                    completedAt = null,
                )

            insertSetLog(setLog)
        }
    }

    // Get workouts for a specific programme
    suspend fun getWorkoutsByProgramme(programmeId: Long): List<Workout> =
        withContext(Dispatchers.IO) {
            workoutDao.getWorkoutsByProgramme(programmeId)
        }

    // Get programme workout progress
    suspend fun getProgrammeWorkoutProgress(programmeId: Long): Pair<Int, Int> =
        withContext(Dispatchers.IO) {
            // Get from the progress table which is the source of truth
            val progress = programmeDao.getProgressForProgramme(programmeId)
            if (progress != null) {
                Pair(progress.completedWorkouts, progress.totalWorkouts)
            } else {
                // Fallback to counting workouts
                val completed = workoutDao.getCompletedProgrammeWorkoutCount(programmeId)
                val total = workoutDao.getTotalProgrammeWorkoutCount(programmeId)
                Pair(completed, total)
            }
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
            val remainingWorkouts = progress.totalWorkouts - progress.completedWorkouts

            // Check if this is a custom programme (AI-generated or user-created) with direct workout storage
            if (programme.isCustom) {
                return@withContext getNextWorkoutFromDatabase(programmeId, progress)
            }

            // Handle template-based programmes (existing logic)
            // Use templateName if available (for custom-named programmes), otherwise use programme name
            val templateNameToSearch = programme.templateName ?: programme.name
            val allTemplates = programmeDao.getAllTemplates()

            val template =
                allTemplates.find { it.name == templateNameToSearch }
                    ?: run {
                        return@withContext null
                    }

            // Parse the JSON structure
            val structure =
                ProgrammeWorkoutParser.parseStructure(template.jsonStructure)
                    ?: return@withContext null

            // Calculate workouts per week from template structure
            val workoutsPerWeek = structure.weeks.flatMap { it.workouts }.size / structure.weeks.size

            // Calculate the next workout position based on completed workouts
            val nextWorkoutIndex = progress.completedWorkouts
            val nextWeek = (nextWorkoutIndex / workoutsPerWeek) + 1
            val nextDay = (nextWorkoutIndex % workoutsPerWeek) + 1

            // Make sure we haven't exceeded programme duration
            if (nextWeek > programme.durationWeeks) {
                return@withContext null
            }

            // Get the workout structure for this position
            val templateWeekIndex = ((nextWeek - 1) % structure.weeks.size) + 1
            val workoutStructure =
                ProgrammeWorkoutParser.getWorkoutForWeekAndDay(
                    structure,
                    templateWeekIndex,
                    nextDay,
                ) ?: return@withContext null

            // Return the workout info
            return@withContext NextProgrammeWorkoutInfo(
                workoutStructure = workoutStructure,
                actualWeekNumber = nextWeek,
            )
        }

    // Handle next workout for custom programmes (AI-generated or user-created)
    private suspend fun getNextWorkoutFromDatabase(
        programmeId: Long,
        progress: ProgrammeProgress,
    ): NextProgrammeWorkoutInfo? {
        // Get all workouts for this programme, ordered by week and day
        val allWorkouts = programmeDao.getAllWorkoutsForProgramme(programmeId)
        if (allWorkouts.isEmpty()) {
            return null
        }

        // Find the next workout based on completed workouts count
        val nextWorkoutIndex = progress.completedWorkouts

        if (nextWorkoutIndex >= allWorkouts.size) {
            return null
        }

        val nextWorkout = allWorkouts[nextWorkoutIndex]

        // Parse the workout structure from JSON with proper configuration
        val workoutStructure =
            try {
                val json =
                    kotlinx.serialization.json.Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    }
                json.decodeFromString<WorkoutStructure>(
                    nextWorkout.workoutStructure,
                )
            } catch (e: Exception) {
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
                } catch (e2: Exception) {
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
            // Check if this is a custom programme (AI-generated or user-created)
            if (programme.isCustom) {
                // For custom programmes, count actual workouts in the database
                val allWorkouts = programmeDao.getAllWorkoutsForProgramme(programme.id)
                val totalWorkouts = allWorkouts.size

                return totalWorkouts
            }

            // For template-based programmes, use existing logic
            val template =
                programmeDao.getAllTemplates().find { it.name == programme.name }
                    ?: return programme.durationWeeks * 3 // Fallback to default

            // Parse the JSON structure
            val structure =
                ProgrammeWorkoutParser.parseStructure(template.jsonStructure)
                    ?: return programme.durationWeeks * 3 // Fallback to default

            // Calculate workouts per week based on the ACTUAL number of workouts in the template
            val totalWorkoutsInTemplate = structure.weeks.sumOf { it.workouts.size }
            val templateWeeks = structure.weeks.size
            val workoutsPerWeek = totalWorkoutsInTemplate.toFloat() / templateWeeks.toFloat()

            // Total workouts = workouts per week * programme duration
            val totalWorkouts = (workoutsPerWeek * programme.durationWeeks).toInt()

            totalWorkouts
        } catch (e: Exception) {
            programme.durationWeeks * 3 // Fallback to default
        }
    }

    // ===== PROGRAMME METHODS =====

    // Programme Templates
    suspend fun getAllProgrammeTemplates() =
        withContext(Dispatchers.IO) {
            programmeDao.getAllTemplates()
        }

    // Programme Management
    suspend fun createProgrammeFromTemplate(
        templateId: Long,
        name: String? = null,
        squatMax: Float? = null,
        benchMax: Float? = null,
        deadliftMax: Float? = null,
        ohpMax: Float? = null,
    ): Long =
        withContext(Dispatchers.IO) {
            val template =
                programmeDao.getTemplateById(templateId)
                    ?: throw IllegalArgumentException("Template not found")

            // Validate all exercises in the template
            val validator = ExerciseValidator(exerciseDao)
            validator.initialize()

            val validationErrors = validator.validateProgrammeStructure(template.jsonStructure)
            if (validationErrors.isNotEmpty()) {
                val errorMessage =
                    validationErrors.joinToString("\n") { error ->
                        "${error.field}: ${error.error}" +
                            (error.suggestion?.let { " (Try: $it)" } ?: "")
                    }
                throw IllegalArgumentException(errorMessage)
            }

            val programme =
                Programme(
                    name = template.name, // Always use template name, ignore custom name
                    description = template.description,
                    durationWeeks = template.durationWeeks,
                    programmeType = template.programmeType,
                    difficulty = template.difficulty,
                    isCustom = false,
                    isActive = false,
                    status = ProgrammeStatus.NOT_STARTED,
                    squatMax = squatMax,
                    benchMax = benchMax,
                    deadliftMax = deadliftMax,
                    ohpMax = ohpMax,
                    weightCalculationRules = template.weightCalculationRules,
                    progressionRules = template.progressionRules,
                    templateName = template.name, // Always store the original template name
                )

            val programmeId = programmeDao.insertProgramme(programme)

            // Parse JSON structure and create weeks/workouts
            val structure = ProgrammeWorkoutParser.parseStructure(template.jsonStructure)
            if (structure != null) {
                // For programmes with single week templates that repeat (like StrongLifts)
                val templateWeeks = structure.weeks.size
                val totalWeeks = template.durationWeeks

                // Create all weeks
                for (weekNum in 1..totalWeeks) {
                    val templateWeekIndex = ((weekNum - 1) % templateWeeks)
                    val templateWeek = structure.weeks[templateWeekIndex]

                    val week =
                        ProgrammeWeek(
                            programmeId = programmeId,
                            weekNumber = weekNum,
                            name = "Week $weekNum",
                            description = templateWeek.name,
                            focusAreas = null,
                            intensityLevel = null,
                            volumeLevel = null,
                            isDeload = false,
                            phase = null,
                        )

                    val weekId = programmeDao.insertProgrammeWeek(week)

                    // Create workouts for this week
                    templateWeek.workouts.forEach { workoutStructure ->
                        val workout =
                            ProgrammeWorkout(
                                weekId = weekId,
                                dayNumber = workoutStructure.day,
                                name = workoutStructure.name,
                                description = null,
                                estimatedDuration = workoutStructure.estimatedDuration,
                                workoutStructure =
                                    kotlinx.serialization.json.Json.encodeToString(
                                        WorkoutStructure.serializer(),
                                        workoutStructure,
                                    ),
                            )

                        programmeDao.insertProgrammeWorkout(workout)
                    }
                }
            }

            programmeId
        }

    suspend fun createAIGeneratedProgramme(preview: GeneratedProgrammePreview): Long =
        withContext(Dispatchers.IO) {
            // Deactivate any currently active programme
            programmeDao.deactivateAllProgrammes()

            // Create the main programme
            val programme =
                Programme(
                    name = preview.name,
                    description = preview.description,
                    durationWeeks = preview.durationWeeks,
                    programmeType = DataProgrammeType.GENERAL_FITNESS,
                    difficulty =
                        when (preview.volumeLevel) {
                            VolumeLevel.LOW -> ProgrammeDifficulty.BEGINNER
                            VolumeLevel.MODERATE -> ProgrammeDifficulty.INTERMEDIATE
                            VolumeLevel.HIGH, VolumeLevel.VERY_HIGH -> ProgrammeDifficulty.ADVANCED
                        },
                    isCustom = true,
                    isActive = true,
                    status = ProgrammeStatus.IN_PROGRESS,
                    startedAt = LocalDateTime.now(),
                )

            val programmeId = programmeDao.insertProgramme(programme)

            // Create weeks and workouts
            preview.weeks.forEach { weekPreview ->
                val week =
                    ProgrammeWeek(
                        programmeId = programmeId,
                        weekNumber = weekPreview.weekNumber,
                        name = "Week ${weekPreview.weekNumber}",
                        description = weekPreview.progressionNotes,
                        focusAreas = null,
                        intensityLevel = weekPreview.intensityLevel,
                        volumeLevel = weekPreview.volumeLevel,
                        isDeload = weekPreview.isDeload,
                        phase = weekPreview.progressionNotes?.substringBefore(":")?.trim(),
                    )

                val weekId = programmeDao.insertProgrammeWeek(week)

                // Create workouts for this week
                weekPreview.workouts.forEach { workoutPreview ->
                    // Build workout structure JSON
                    val workoutStructure =
                        WorkoutStructure(
                            day = workoutPreview.dayNumber,
                            name = workoutPreview.name,
                            exercises =
                                workoutPreview.exercises.map { exercisePreview ->
                                    ExerciseStructure(
                                        name = exercisePreview.exerciseName,
                                        sets = exercisePreview.sets,
                                        reps =
                                            RepsStructure.Range(
                                                min = exercisePreview.repsMin,
                                                max = exercisePreview.repsMax,
                                            ),
                                        note = exercisePreview.notes,
                                        suggestedWeight = exercisePreview.suggestedWeight,
                                        weightSource = exercisePreview.weightSource,
                                    )
                                },
                            estimatedDuration = workoutPreview.estimatedDuration,
                        )

                    val workout =
                        ProgrammeWorkout(
                            weekId = weekId,
                            dayNumber = workoutPreview.dayNumber,
                            name = workoutPreview.name,
                            description = null,
                            estimatedDuration = workoutPreview.estimatedDuration,
                            workoutStructure =
                                kotlinx.serialization.json
                                    .Json {
                                        ignoreUnknownKeys = true
                                        isLenient = true
                                    }.encodeToString(
                                        WorkoutStructure.serializer(),
                                        workoutStructure,
                                    ),
                        )

                    programmeDao.insertProgrammeWorkout(workout)
                }
            }

            // Initialize progress tracking for the new programme
            // CRITICAL FIX: Calculate total workouts based on programme duration, not just preview data
            val totalWorkouts = preview.daysPerWeek * preview.durationWeeks
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

    suspend fun calculateProgrammeCompletionStats(programmeId: Long): ProgrammeCompletionStats? =
        withContext(Dispatchers.IO) {
            val programme = programmeDao.getProgrammeById(programmeId) ?: return@withContext null
            val progress = programmeDao.getProgressForProgramme(programmeId) ?: return@withContext null
            val workouts = workoutDao.getWorkoutsByProgramme(programmeId)

            // Calculate total volume
            var totalVolume = 0.0
            for (workout in workouts.filter { it.status == WorkoutStatus.COMPLETED }) {
                val exercises = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
                for (exercise in exercises) {
                    val sets = setLogDao.getSetLogsForExercise(exercise.id)
                    for (set in sets.filter { it.isCompleted }) {
                        totalVolume += (set.actualWeight * set.actualReps).toDouble()
                    }
                }
            }

            // Calculate average workout duration
            val completedWorkouts = workouts.filter { it.status == WorkoutStatus.COMPLETED && it.durationSeconds != null }
            val averageDuration =
                if (completedWorkouts.isNotEmpty()) {
                    val avgSeconds = completedWorkouts.mapNotNull { it.durationSeconds }.average().toLong()
                    Duration.ofSeconds(avgSeconds)
                } else {
                    Duration.ZERO
                }

            // Count PRs during programme
            val prs =
                personalRecordDao.getPersonalRecordsInDateRange(
                    programme.startedAt ?: programme.createdAt,
                    programme.completedAt ?: LocalDateTime.now(),
                )

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
                totalPRs = prs.size,
                strengthImprovements = strengthImprovements,
                averageStrengthImprovement = avgImprovement,
                insights = insights,
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
                if (history.isNotEmpty()) {
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
    ) = withContext(Dispatchers.IO) {
        val programme = programmeDao.getProgrammeById(programmeId) ?: return@withContext
        programmeDao.updateProgramme(
            programme.copy(
                completionNotes = notes,
                notesCreatedAt = if (notes != null) LocalDateTime.now() else null,
            ),
        )
    }

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
            } catch (e: Exception) {
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
            val deletedWorkouts = workoutDao.deleteWorkoutsByProgramme(programme.id)

            // Then delete the programme (will cascade delete progress and related data)
            programmeDao.deleteProgramme(programme)
        }

    suspend fun getInProgressWorkoutCountByProgramme(programmeId: Long): Int =
        withContext(Dispatchers.IO) {
            workoutDao.getInProgressWorkoutCountByProgramme(programmeId)
        }

    suspend fun updateWorkoutStatus(
        workoutId: Long,
        status: WorkoutStatus,
    ) = withContext(Dispatchers.IO) {
        val workout = workoutDao.getWorkoutById(workoutId) ?: return@withContext
        val updatedWorkout = workout.copy(status = status)
        workoutDao.updateWorkout(updatedWorkout)
    }

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

    // ========== Profile & 1RM Management ==========

    suspend fun ensureUserProfile(userId: Long) =
        withContext(Dispatchers.IO) {
            db.profileDao().getUserProfile(userId)
                ?: // Should not happen as users are created through seedTestUsers
                error("User profile not found for ID: $userId")
        }

    suspend fun getAllUsers() =
        withContext(Dispatchers.IO) {
            db.profileDao().getAllUsers()
        }

    suspend fun upsertExerciseMax(
        userId: Long,
        exerciseVariationId: Long,
        oneRMEstimate: Float,
        oneRMContext: String,
        oneRMType: OneRMType,
        notes: String? = null,
        workoutDate: LocalDateTime? = null,
    ) = withContext(Dispatchers.IO) {
        ensureUserProfile(userId)
        // Round weight to nearest 0.25
        val roundedWeight = WeightFormatter.roundToNearestQuarter(oneRMEstimate)
        val dateToUse = workoutDate ?: LocalDateTime.now()
        val userExerciseMax =
            UserExerciseMax(
                userId = userId,
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
        saveOneRMToHistory(
            userId = userId,
            exerciseVariationId = exerciseVariationId,
            oneRM = roundedWeight,
            context = oneRMContext,
            recordedAt = dateToUse,
        )
    }

    // Remove getAllCurrentMaxes - this should only be used in Insights

    suspend fun getCurrentMaxesForExercises(
        userId: Long,
        exerciseIds: List<Long>,
    ) = withContext(Dispatchers.IO) {
        db.oneRMDao().getCurrentMaxesForExercises(userId, exerciseIds)
    }

    suspend fun getBig4Exercises() =
        withContext(Dispatchers.IO) {
            db.exerciseDao().getBig4Exercises()
        }

    fun getAllCurrentMaxesWithNames(userId: Long): Flow<List<OneRMWithExerciseName>> =
        db.oneRMDao().getAllCurrentMaxesWithNames(userId).map { maxes ->
            maxes.map { max ->
                OneRMWithExerciseName(
                    id = max.id,
                    userId = max.userId,
                    exerciseVariationId = max.exerciseVariationId,
                    exerciseName = max.exerciseName,
                    oneRMEstimate = max.oneRMEstimate,
                    oneRMDate = max.oneRMDate,
                    oneRMContext = max.oneRMContext,
                    mostWeightLifted = max.mostWeightLifted,
                    mostWeightReps = max.mostWeightReps,
                    mostWeightRpe = max.mostWeightRpe,
                    mostWeightDate = max.mostWeightDate,
                    oneRMConfidence = max.oneRMConfidence,
                    oneRMType = max.oneRMType,
                    notes = max.notes,
                )
            }
        }

    fun getBig4ExercisesWithMaxes(userId: Long): Flow<List<com.github.radupana.featherweight.data.profile.Big4ExerciseWithOptionalMax>> = db.oneRMDao().getBig4ExercisesWithMaxes(userId)

    fun getOtherExercisesWithMaxes(userId: Long): Flow<List<OneRMWithExerciseName>> = db.oneRMDao().getOtherExercisesWithMaxes(userId)

    suspend fun getOneRMForExercise(exerciseVariationId: Long): Float? =
        withContext(Dispatchers.IO) {
            val userId = getCurrentUserId()
            val exerciseMax = db.oneRMDao().getCurrentMax(userId, exerciseVariationId)
            return@withContext exerciseMax?.oneRMEstimate
        }

    suspend fun getEstimated1RM(exerciseVariationId: Long): Float? =
        withContext(Dispatchers.IO) {
            // Try to get from 1RM records first
            val oneRM = getOneRMForExercise(exerciseVariationId)
            if (oneRM != null) {
                return@withContext oneRM
            }

            // If no 1RM record, calculate from recent workout history
            val recentSets =
                getRecentSetLogsForExercise(exerciseVariationId, daysBack = 30)
                    .filter { it.isCompleted && it.actualWeight > 0 && it.actualReps > 0 }
                    .sortedByDescending { it.actualWeight }

            if (recentSets.isEmpty()) {
                return@withContext null
            }

            // Use Brzycki formula on the heaviest recent set
            val bestSet = recentSets.first()
            val estimated1RM =
                if (bestSet.actualReps == 1) {
                    bestSet.actualWeight
                } else {
                    bestSet.actualWeight * (36f / (37f - bestSet.actualReps))
                }

            return@withContext estimated1RM
        }

    suspend fun deleteAllMaxesForExercise(exerciseId: Long) =
        withContext(Dispatchers.IO) {
            db.oneRMDao().deleteAllMaxesForExercise(getCurrentUserId(), exerciseId)
        }

    suspend fun updateOrInsertOneRM(oneRMRecord: UserExerciseMax) =
        withContext(Dispatchers.IO) {
            // Check if we have an existing record for this user/exercise
            val existing = db.oneRMDao().getCurrentMax(oneRMRecord.userId, oneRMRecord.exerciseVariationId)

            val shouldSaveHistory =
                if (existing != null) {
                    // Update existing record if new estimate is higher
                    if (oneRMRecord.oneRMEstimate > existing.oneRMEstimate) {
                        db.oneRMDao().updateExerciseMax(oneRMRecord.copy(id = existing.id))
                        true
                    } else {
                        false
                    }
                } else {
                    // Insert new record
                    db.oneRMDao().insertExerciseMax(oneRMRecord)
                    true
                }

            // Save to history if we made a change
            if (shouldSaveHistory) {
                saveOneRMToHistory(
                    userId = oneRMRecord.userId,
                    exerciseVariationId = oneRMRecord.exerciseVariationId,
                    oneRM = oneRMRecord.oneRMEstimate,
                    context = oneRMRecord.oneRMContext,
                )
            }
        }

    // ========== Test User Management ==========

    suspend fun seedTestUsers() =
        withContext(Dispatchers.IO) {
            val existingUsers = db.profileDao().getAllUsers()

            if (existingUsers.isEmpty()) {
                // Create test users
                val user1Id =
                    db.profileDao().insertUserProfile(
                        UserProfile(
                            username = "user1",
                            displayName = "Alex Johnson",
                            avatarEmoji = "💪",
                        ),
                    )

                val user2Id =
                    db.profileDao().insertUserProfile(
                        UserProfile(
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
                        db.oneRMDao().insertExerciseMax(
                            UserExerciseMax(
                                userId = user1Id,
                                exerciseVariationId = exercise.id,
                                mostWeightLifted = maxWeight,
                                mostWeightReps = 1,
                                mostWeightRpe = null,
                                mostWeightDate = LocalDateTime.now(),
                                oneRMEstimate = maxWeight,
                                oneRMContext = "${maxWeight}kg × 1",
                                oneRMConfidence = 1.0f,
                                oneRMDate = LocalDateTime.now(),
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
                        db.oneRMDao().insertExerciseMax(
                            UserExerciseMax(
                                userId = user2Id,
                                exerciseVariationId = exercise.id,
                                mostWeightLifted = maxWeight,
                                mostWeightReps = 1,
                                mostWeightRpe = null,
                                mostWeightDate = LocalDateTime.now(),
                                oneRMEstimate = maxWeight,
                                oneRMContext = "${maxWeight}kg × 1",
                                oneRMConfidence = 1.0f,
                                oneRMDate = LocalDateTime.now(),
                            ),
                        )
                    }
                }
            }
        }

    private suspend fun calculateIntelligentWeight(
        exerciseStructure: ExerciseStructure,
        reps: Int,
        userMaxes: Map<Long, Float>,
        intensity: Int? = null,
        programme: Programme? = null,
    ): Float {
        // First check if we have intensity-based calculation (e.g., Wendler 5/3/1)
        if (intensity != null && programme != null) {
            val weightCalcRules = programme.getWeightCalculationRulesObject()
            if (weightCalcRules?.baseOn == WeightBasis.ONE_REP_MAX) {
                // Get user's 1RM for this exercise
                // First get the exercise variation ID for this exercise name
                val exerciseVariation = exerciseVariationDao.getExerciseVariationByName(exerciseStructure.name)
                val user1RM = exerciseVariation?.id?.let { userMaxes[it] }
                if (user1RM != null && user1RM > 0) {
                    // Apply training max percentage if specified
                    val trainingMax = user1RM * (weightCalcRules.trainingMaxPercentage ?: 1.0f)

                    // Calculate weight based on intensity percentage
                    val calculatedWeight = trainingMax * (intensity / 100f)

                    // Round to increment
                    val roundingIncrement = weightCalcRules.roundingIncrement ?: 2.5f
                    val rounded = (calculatedWeight / roundingIncrement).toInt() * roundingIncrement

                    // Ensure minimum bar weight
                    val minimumWeight = weightCalcRules.minimumBarWeight ?: 20f

                    return rounded.coerceAtLeast(minimumWeight)
                }
            }
        }

        // Check for LAST_WORKOUT based calculation (e.g., StrongLifts)
        if (programme != null) {
            val weightCalcRules = programme.getWeightCalculationRulesObject()
            if (weightCalcRules?.baseOn == WeightBasis.LAST_WORKOUT) {
                // Use ProgressionService for intelligent weight calculation with deload support
                val progressionService =
                    ProgressionService(
                        performanceTrackingDao = db.exercisePerformanceTrackingDao(),
                        programmeDao = programmeDao,
                        repository = this@FeatherweightRepository,
                    )

                // Calculate the progression decision
                val decision =
                    progressionService.calculateProgressionWeight(
                        exerciseName = exerciseStructure.name,
                        programme = programme,
                    )

                return decision.weight
            }
        }

        // Check if we have AI-suggested weight with a valid source
        if (exerciseStructure.suggestedWeight != null &&
            exerciseStructure.suggestedWeight > 0 &&
            exerciseStructure.weightSource != null &&
            exerciseStructure.weightSource != "average_estimate"
        ) {
            // AI made an informed decision based on user's 1RMs or specific input
            return exerciseStructure.suggestedWeight
        }

        // Fallback to existing intelligent weight calculation
        val weightCalculator = WeightCalculator()

        // Parse rep range from the exercise structure
        val repRange =
            when (val repsStructure = exerciseStructure.reps) {
                is RepsStructure.Single ->
                    repsStructure.value..repsStructure.value

                is RepsStructure.Range ->
                    repsStructure.min..repsStructure.max

                is RepsStructure.RangeString ->
                    reps..reps // Fallback to actual reps
                is RepsStructure.PerSet ->
                    reps..reps // Fallback to actual reps
            }

        // Determine programme type from exercise structure or use general
        val programmeType = ProgrammeType.GENERAL // Could be enhanced to detect from context

        // Get user's 1RM for this exercise
        val exerciseVariation = exerciseVariationDao.getExerciseVariationByName(exerciseStructure.name)
        val user1RM = exerciseVariation?.id?.let { userMaxes[it] }

        // Use the intelligent weight calculation
        val weightCalculation =
            weightCalculator.calculateWorkingWeight(
                exerciseName = exerciseStructure.name,
                repRange = repRange,
                programmeType = programmeType,
                user1RM = user1RM,
                extractedWeight = null, // This would come from user input analysis
                aiSuggestedWeight = exerciseStructure.suggestedWeight,
            )

        return weightCalculation.weight
    }

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

    suspend fun getHistoricalPerformance(
        exerciseVariationId: Long,
        minReps: Int,
        maxReps: Int,
    ): List<com.github.radupana.featherweight.service.PerformanceData> =
        withContext(Dispatchers.IO) {
            // Get all workouts and find exercise logs for this variation
            val allWorkouts = db.workoutDao().getAllWorkouts()
            val exerciseLogIds = mutableListOf<Long>()

            for (workout in allWorkouts) {
                val logs = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
                exerciseLogIds.addAll(logs.filter { it.exerciseVariationId == exerciseVariationId }.map { it.id })
            }

            if (exerciseLogIds.isEmpty()) return@withContext emptyList()

            // Get sets within rep range
            val sets = db.workoutDao().getSetsForExercisesInRepRange(exerciseLogIds, minReps, maxReps)

            sets
                .map { setLog ->
                    com.github.radupana.featherweight.service.PerformanceData(
                        targetReps = setLog.targetReps,
                        targetWeight = setLog.targetWeight,
                        actualReps = setLog.actualReps,
                        actualWeight = setLog.actualWeight,
                        actualRpe = setLog.actualRpe,
                        timestamp = setLog.completedAt ?: "",
                    )
                }.sortedByDescending { it.timestamp }
        }

    suspend fun getRecentPerformance(
        exerciseVariationId: Long,
        limit: Int = 10,
    ): List<com.github.radupana.featherweight.service.PerformanceData> =
        withContext(Dispatchers.IO) {
            // Get all workouts and find exercise logs for this variation
            val allWorkouts = db.workoutDao().getAllWorkouts()
            val exerciseLogIds = mutableListOf<Long>()

            for (workout in allWorkouts) {
                val logs = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
                exerciseLogIds.addAll(logs.filter { it.exerciseVariationId == exerciseVariationId }.map { it.id })
            }

            if (exerciseLogIds.isEmpty()) return@withContext emptyList()

            // Get recent sets
            val sets = db.workoutDao().getRecentSetsForExercises(exerciseLogIds, limit)

            sets
                .map { setLog ->
                    com.github.radupana.featherweight.service.PerformanceData(
                        targetReps = setLog.targetReps,
                        targetWeight = setLog.targetWeight,
                        actualReps = setLog.actualReps,
                        actualWeight = setLog.actualWeight,
                        actualRpe = setLog.actualRpe,
                        timestamp = setLog.completedAt ?: "",
                    )
                }.sortedByDescending { it.timestamp }
        }

    suspend fun getHistoricalPerformanceForWeight(
        exerciseVariationId: Long,
        minWeight: Float,
        maxWeight: Float,
    ): List<com.github.radupana.featherweight.service.PerformanceData> =
        withContext(Dispatchers.IO) {
            // Get exercise logs for this exercise
            val exerciseLogs = db.workoutDao().getExerciseLogsByVariationId(exerciseVariationId)
            val exerciseLogIds = exerciseLogs.map { it.id }

            if (exerciseLogIds.isEmpty()) return@withContext emptyList()

            // Get sets within weight range
            val sets = db.workoutDao().getSetsForExercisesInWeightRange(exerciseLogIds, minWeight, maxWeight)

            sets
                .map { setLog ->
                    com.github.radupana.featherweight.service.PerformanceData(
                        targetReps = setLog.targetReps,
                        targetWeight = setLog.targetWeight,
                        actualReps = setLog.actualReps,
                        actualWeight = setLog.actualWeight,
                        actualRpe = setLog.actualRpe,
                        timestamp = setLog.completedAt ?: "",
                    )
                }.sortedByDescending { it.timestamp }
        }

    suspend fun getGlobalExerciseProgress(
        userId: Long,
        exerciseVariationId: Long,
    ): GlobalExerciseProgress? =
        withContext(Dispatchers.IO) {
            globalExerciseProgressDao.getProgressForExercise(userId, exerciseVariationId)
        }

    /**
     * Check if a completed set represents a personal record
     * Returns list of PersonalRecord objects if PRs are detected
     */
    suspend fun checkForPR(
        setLog: SetLog,
        exerciseVariationId: Long,
    ): List<PersonalRecord> =
        withContext(Dispatchers.IO) {
            val prs = prDetectionService.checkForPR(setLog, exerciseVariationId)

            // If we detected an estimated 1RM PR, update the UserExerciseMax table
            val oneRMPR = prs.find { it.recordType == PRType.ESTIMATED_1RM }
            if (oneRMPR != null && oneRMPR.estimated1RM != null) {
                val userId = getCurrentUserId()
                val oneRMService = OneRMService()

                // Create UserExerciseMax record from the PR
                val userExerciseMax =
                    oneRMService.createOneRMRecord(
                        userId = userId,
                        exerciseId = exerciseVariationId,
                        set = setLog,
                        estimate = oneRMPR.estimated1RM,
                        confidence = 1.0f, // High confidence since it's a PR
                    )

                // Update or insert the 1RM record
                updateOrInsertOneRM(userExerciseMax)
            } else if (prs.isNotEmpty()) {
                // PRs detected but no 1RM update needed
            }

            prs
        }

    /**
     * Get recent personal records across all exercises
     */
    suspend fun getRecentPRs(limit: Int = 10): List<PersonalRecord> =
        withContext(Dispatchers.IO) {
            prDetectionService.getRecentPRs(limit)
        }

    /**
     * Get all personal records from database for debugging
     */
    suspend fun getAllPersonalRecordsFromDB(): List<PersonalRecord> =
        withContext(Dispatchers.IO) {
            personalRecordDao.getAllPersonalRecords()
        }

    /**
     * Get personal records achieved during a specific workout
     */
    suspend fun getPersonalRecordsForWorkout(workoutId: Long): List<PersonalRecord> =
        withContext(Dispatchers.IO) {
            personalRecordDao.getPersonalRecordsForWorkout(workoutId)
        }

    suspend fun getCurrentOneRMEstimate(
        userId: Long,
        exerciseId: Long,
    ): Float? =
        withContext(Dispatchers.IO) {
            db.oneRMDao().getCurrentOneRMEstimate(userId, exerciseId)
        }

    suspend fun saveOneRMToHistory(
        userId: Long,
        exerciseVariationId: Long,
        oneRM: Float,
        context: String,
        recordedAt: LocalDateTime? = null,
    ) = withContext(Dispatchers.IO) {
        db.oneRMDao().insertOneRMHistory(
            OneRMHistory(
                userId = userId,
                exerciseVariationId = exerciseVariationId,
                oneRMEstimate = oneRM,
                context = context,
                recordedAt = recordedAt ?: LocalDateTime.now(),
            ),
        )
    }

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
                val currentInfo = dayInfoMap[date]!!

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
            workoutDao.getWorkoutsInDateRange(
                startDate = startDate.atStartOfDay(),
                endDate = endDate.atTime(23, 59, 59),
            ).count { it.status == WorkoutStatus.COMPLETED }
        }

    /**
     * Get exercise 1RM from user profile
     */
    suspend fun getExercise1RM(
        exerciseName: String,
        userId: Long,
    ): Float? =
        withContext(Dispatchers.IO) {
            val exercise = getExerciseByName(exerciseName) ?: return@withContext null
            db.oneRMDao().getCurrentMax(userId, exercise.id)?.oneRMEstimate
        }

    /**
     * Clear all workout data (for developer tools)
     */
    suspend fun clearAllWorkoutData() =
        withContext(Dispatchers.IO) {
            val userId = getCurrentUserId()

            // Delete all workouts and related data
            workoutDao.deleteAllWorkouts()
            exerciseLogDao.deleteAllExerciseLogs()
            setLogDao.deleteAllSetLogs()
            personalRecordDao.deleteAllPersonalRecords()
            globalExerciseProgressDao.deleteAllGlobalProgress()

            // CRITICAL: Also delete all 1RM related data that was NOT being cleared!
            // This is why phantom data persisted across "Clear All Workout Data"
            if (userId != -1L) {
                // Delete all UserExerciseMax entries for this user
                db.oneRMDao().deleteAllUserExerciseMaxes(userId)

                // Delete all OneRMHistory entries for this user
                db.oneRMDao().deleteAllOneRMHistory(userId)
            }

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
    ): PersonalRecord? =
        withContext(Dispatchers.IO) {
            personalRecordDao.getLatestRecordForExercise(exerciseVariationId)
        }

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
            val userId = userPreferences.getCurrentUserId()
            if (userId == -1L) {
                return@withContext com.github.radupana.featherweight.service
                    .GroupedExerciseSummary(emptyList(), emptyList())
            }

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
                        val globalProgress = getGlobalExerciseProgress(userId, exerciseVariationId)
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
                    programmeType = programme.programmeType.toAiProgrammeType(),
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
                programmeType = programme.programmeType.toAiProgrammeType(),
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
            val userId = getCurrentUserId()
            db.trainingAnalysisDao().getLatestAnalysis(userId)
        }

    suspend fun deleteOldAnalyses(olderThan: LocalDateTime) =
        withContext(Dispatchers.IO) {
            val userId = getCurrentUserId()
            db.trainingAnalysisDao().deleteOldAnalyses(userId, olderThan)
        }
}

// New data classes for history feature
data class WorkoutSummaryWithProgramme(
    val id: Long,
    val date: LocalDateTime,
    val name: String?,
    val programmeName: String?,
    val exerciseCount: Int,
    val setCount: Int,
    val totalWeight: Float,
    val duration: Long?,
    val status: WorkoutStatus,
    val hasNotes: Boolean = false,
)

data class ProgrammeSummary(
    val id: Long,
    val name: String,
    val startDate: LocalDateTime,
    val completionDate: LocalDateTime,
    val completedAt: LocalDateTime? = null,
    val durationWeeks: Int,
    val completedWorkouts: Int,
    val totalWorkouts: Int = 0,
    val programmeType: ProgrammeType = ProgrammeType.GENERAL,
    val difficulty: ProgrammeDifficulty = ProgrammeDifficulty.INTERMEDIATE,
    val completionNotes: String? = null,
)

data class ProgrammeHistoryDetails(
    val id: Long,
    val name: String,
    val programmeType: ProgrammeType,
    val difficulty: ProgrammeDifficulty,
    val durationWeeks: Int,
    val startedAt: LocalDateTime?,
    val completedAt: LocalDateTime?,
    val completedWorkouts: Int,
    val totalWorkouts: Int,
    val programDurationDays: Int,
    val workoutHistory: List<WorkoutHistoryEntry>,
    val completionNotes: String? = null,
)

data class ProgrammeWeekHistory(
    val weekNumber: Int,
    val weekName: String?,
    val workouts: List<WorkoutHistoryDetail>,
)

data class WorkoutHistoryDetail(
    val id: Long,
    val name: String,
    val date: LocalDateTime,
    val exerciseNames: List<String>,
    val totalExercises: Int,
    val totalVolume: Float,
    val duration: Long?,
)

data class WorkoutHistoryEntry(
    val workoutId: Long,
    val workoutName: String,
    val weekNumber: Int,
    val dayNumber: Int,
    val completed: Boolean,
    val completedAt: LocalDateTime?,
)

// Extension function to convert from data.programme.ProgrammeType to ai.ProgrammeType
private fun DataProgrammeType.toAiProgrammeType(): ProgrammeType =
    when (this) {
        DataProgrammeType.STRENGTH -> ProgrammeType.STRENGTH
        DataProgrammeType.POWERLIFTING -> ProgrammeType.STRENGTH
        DataProgrammeType.BODYBUILDING -> ProgrammeType.HYPERTROPHY
        DataProgrammeType.GENERAL_FITNESS -> ProgrammeType.GENERAL
        DataProgrammeType.OLYMPIC_LIFTING -> ProgrammeType.STRENGTH
        DataProgrammeType.HYBRID -> ProgrammeType.GENERAL
    }
