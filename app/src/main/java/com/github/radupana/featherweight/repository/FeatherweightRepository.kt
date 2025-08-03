package com.github.radupana.featherweight.repository

import android.app.Application
import android.util.Log
import com.github.radupana.featherweight.ai.ProgrammeType
import com.github.radupana.featherweight.ai.WeightCalculator
import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.GeneratedProgrammePreview
import com.github.radupana.featherweight.data.GlobalExerciseProgress
import com.github.radupana.featherweight.data.PendingOneRMUpdate
import com.github.radupana.featherweight.data.PersonalRecord
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.SwapHistoryCount
import com.github.radupana.featherweight.data.UserPreferences
import com.github.radupana.featherweight.data.VolumeLevel
import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.data.WorkoutStatus
import com.github.radupana.featherweight.data.exercise.Exercise
import com.github.radupana.featherweight.data.exercise.ExerciseAliasSeeder
import com.github.radupana.featherweight.data.exercise.ExerciseCategory
import com.github.radupana.featherweight.data.exercise.ExerciseCorrelationSeeder
import com.github.radupana.featherweight.data.profile.OneRMWithExerciseName
import com.github.radupana.featherweight.data.profile.OneRMType
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow
import com.github.radupana.featherweight.data.exercise.ExerciseSeeder
import com.github.radupana.featherweight.data.exercise.ExerciseWithDetails
import com.github.radupana.featherweight.data.profile.UserExerciseMax
import com.github.radupana.featherweight.data.profile.UserProfile
import com.github.radupana.featherweight.data.programme.ExerciseStructure
import com.github.radupana.featherweight.data.programme.Programme
import com.github.radupana.featherweight.data.programme.ProgrammeDifficulty
import com.github.radupana.featherweight.data.programme.ProgrammeProgress
import com.github.radupana.featherweight.data.programme.ProgrammeSeeder
import com.github.radupana.featherweight.data.programme.ProgrammeStatus
import com.github.radupana.featherweight.data.programme.ProgrammeWeek
import com.github.radupana.featherweight.data.programme.ProgrammeWorkout
import com.github.radupana.featherweight.data.programme.ProgrammeWorkoutParser
import com.github.radupana.featherweight.data.programme.ProgressionType
import com.github.radupana.featherweight.data.programme.RepsStructure
import com.github.radupana.featherweight.data.programme.WeightBasis
import com.github.radupana.featherweight.data.programme.WorkoutStructure
import com.github.radupana.featherweight.domain.ExerciseHistory
import com.github.radupana.featherweight.domain.ExerciseStats
import com.github.radupana.featherweight.domain.SmartSuggestions
import com.github.radupana.featherweight.service.FreestyleIntelligenceService
import com.github.radupana.featherweight.service.GlobalProgressTracker
import com.github.radupana.featherweight.service.PRDetectionService
import com.github.radupana.featherweight.service.ProgressionService
import com.github.radupana.featherweight.service.WorkoutTemplateGeneratorService
import com.github.radupana.featherweight.service.WorkoutTemplateWeightService
import com.github.radupana.featherweight.util.WeightFormatter
import com.github.radupana.featherweight.validation.ExerciseValidator
import com.radu.featherweight.data.model.WorkoutTemplate
import com.radu.featherweight.data.model.WorkoutTemplateConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import com.github.radupana.featherweight.data.programme.ProgrammeType as DataProgrammeType

data class NextProgrammeWorkoutInfo(
    val workoutStructure: WorkoutStructure,
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
    init {
        Log.e("FeatherweightDebug", "FeatherweightRepository: Starting initialization")
    }

    private val db =
        FeatherweightDatabase.getDatabase(application).also {
            Log.e("FeatherweightDebug", "FeatherweightRepository: Database obtained")
        }
    private val userPreferences =
        UserPreferences(application).also {
            Log.e("FeatherweightDebug", "FeatherweightRepository: UserPreferences created")
        }

    // Sub-repositories
    private val exerciseRepository = ExerciseRepository(db)

    private val workoutDao =
        db.workoutDao().also {
            Log.e("FeatherweightDebug", "FeatherweightRepository: workoutDao obtained")
        }
    private val exerciseLogDao =
        db.exerciseLogDao().also {
            Log.e("FeatherweightDebug", "FeatherweightRepository: exerciseLogDao obtained")
        }
    private val setLogDao =
        db.setLogDao().also {
            Log.e("FeatherweightDebug", "FeatherweightRepository: setLogDao obtained")
        }
    private val exerciseDao =
        db.exerciseDao().also {
            Log.e("FeatherweightDebug", "FeatherweightRepository: exerciseDao obtained")
        }
    private val programmeDao =
        db.programmeDao().also {
            Log.e("FeatherweightDebug", "FeatherweightRepository: programmeDao obtained")
        }
    private val profileDao =
        db.profileDao().also {
            Log.e("FeatherweightDebug", "FeatherweightRepository: profileDao obtained")
        }

    private val globalExerciseProgressDao = db.globalExerciseProgressDao()
    private val exerciseCorrelationDao = db.exerciseCorrelationDao()
    private val personalRecordDao = db.personalRecordDao()

    // Initialize GlobalProgressTracker
    private val globalProgressTracker = GlobalProgressTracker(this, db)
    private val freestyleIntelligenceService = FreestyleIntelligenceService(this, db.globalExerciseProgressDao())
    private val prDetectionService = PRDetectionService(personalRecordDao, setLogDao)
    private val workoutTemplateGeneratorService =
        WorkoutTemplateGeneratorService(workoutDao, exerciseDao, exerciseLogDao, setLogDao)
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
        println("🎯 Applying 1RM update: exercise=${update.exerciseName}, exerciseId=${update.exerciseId}, weight=$roundedWeight, userId=$userId")

        db.oneRMDao().upsertExerciseMax(
            userId = userId,
            exerciseId = update.exerciseId,
            maxWeight = roundedWeight,
            notes = "Updated from ${update.source}",
        )

        println("✅ 1RM update applied successfully")

        // Remove this update from pending list
        _pendingOneRMUpdates.value =
            _pendingOneRMUpdates.value.filter {
                it.exerciseId != update.exerciseId
            }
    }

    private val exerciseSeeder =
        ExerciseSeeder(exerciseDao, application).also {
            Log.e("FeatherweightDebug", "FeatherweightRepository: ExerciseSeeder created")
        }
    private val exerciseAliasSeeder =
        ExerciseAliasSeeder(exerciseDao).also {
            Log.e("FeatherweightDebug", "FeatherweightRepository: ExerciseAliasSeeder created")
        }
    private val programmeSeeder =
        ProgrammeSeeder(programmeDao, exerciseDao).also {
            Log.e("FeatherweightDebug", "FeatherweightRepository: ProgrammeSeeder created")
        }

    private val exerciseCorrelationSeeder = ExerciseCorrelationSeeder(exerciseCorrelationDao)

    fun getCurrentUserId(): Long = userPreferences.getCurrentUserId()

    // Initialize with seed data for testing
    suspend fun seedDatabaseIfEmpty() {
        withContext(Dispatchers.IO) {
            val exerciseCount = exerciseDao.getAllExercisesWithDetails().size
            val workoutCount = workoutDao.getAllWorkouts().size

            // Always seed exercises if there are none
            if (exerciseCount == 0) {
                exerciseSeeder.seedExercises()
                println("Seeded ${exerciseDao.getAllExercisesWithDetails().size} exercises")

                // Seed aliases after exercises are created
                exerciseAliasSeeder.seedExerciseAliases()
                println("Seeded exercise aliases")
            }

            // Always check and seed missing programme templates
            programmeSeeder.seedPopularProgrammes()

            // Seed exercise correlations if none exist
            if (exerciseCorrelationDao.getCount() == 0) {
                exerciseCorrelationSeeder.seedExerciseCorrelations()
                println("Seeded exercise correlations")
            }
        }
    }

    // ===== EXERCISE METHODS (Delegated to ExerciseRepository) =====

    suspend fun getAllExercises(): List<ExerciseWithDetails> = exerciseRepository.getAllExercises()

    suspend fun getExerciseById(id: Long): ExerciseWithDetails? = exerciseRepository.getExerciseById(id)

    suspend fun searchExercises(query: String): List<Exercise> = exerciseRepository.searchExercises(query)

    suspend fun getExercisesByCategory(category: ExerciseCategory): List<Exercise> = exerciseRepository.getExercisesByCategory(category)

    // ===== EXISTING WORKOUT METHODS (Updated to work with new Exercise system) =====

    // Basic CRUD operations
    suspend fun insertWorkout(workout: Workout): Long = workoutDao.insertWorkout(workout)

    suspend fun getExercisesForWorkout(workoutId: Long): List<ExerciseLog> = exerciseRepository.getExercisesForWorkout(workoutId)

    suspend fun getSetsForExercise(exerciseLogId: Long): List<SetLog> = exerciseRepository.getSetsForExercise(exerciseLogId)
    
    suspend fun getSetsForWorkout(workoutId: Long): List<SetLog> = withContext(Dispatchers.IO) {
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
        exercise: ExerciseWithDetails,
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
        println("🔄 Repository.completeWorkout called with workoutId: $workoutId, duration: $durationSeconds")

        // Get all workouts to verify
        val allWorkouts = workoutDao.getAllWorkouts()
        println("📊 Total workouts in database: ${allWorkouts.size}")

        val workout = allWorkouts.find { it.id == workoutId }
        if (workout == null) {
            println("❌ ERROR: Workout $workoutId not found in database!")
            return
        }

        println(
            "📊 Workout found: id=${workout.id}, isProgrammeWorkout=${workout.isProgrammeWorkout}, " +
                "programmeId=${workout.programmeId}, week=${workout.weekNumber}, day=${workout.dayNumber}, " +
                "currentStatus=${workout.status}, notes='${workout.notes}'",
        )

        // Check if already completed
        if (workout.status == WorkoutStatus.COMPLETED) {
            println("⚠️ Workout already marked as completed, skipping")
            return
        }

        println("📝 Creating updated workout with COMPLETED status")
        val updatedWorkout = workout.copy(status = WorkoutStatus.COMPLETED, durationSeconds = durationSeconds)

        println("💾 Saving to database...")
        workoutDao.updateWorkout(updatedWorkout)
        println("✅ Workout marked as completed in database")

        // Verify the update
        val verifyWorkout = workoutDao.getAllWorkouts().find { it.id == workoutId }
        println("🔍 Verification - workout status after update: ${verifyWorkout?.status}")

        // Record performance data for programme workouts before updating progress
        if (workout.isProgrammeWorkout && workout.programmeId != null) {
            println("📊 Recording performance data for programme workout...")
            recordWorkoutPerformanceData(workoutId, workout.programmeId)
        }

        // Update global exercise progress for ALL workouts (programme or freestyle)
        val userId = getCurrentUserId()
        println("📈 Updating global exercise progress for user $userId...")
        val pendingOneRMUpdates = globalProgressTracker.updateProgressAfterWorkout(workoutId, userId)

        // Store pending 1RM updates for later retrieval by UI
        if (pendingOneRMUpdates.isNotEmpty()) {
            println("💪 ${pendingOneRMUpdates.size} pending 1RM updates available")
            _pendingOneRMUpdates.value = pendingOneRMUpdates
        }

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
            println(
                "📊 Progress AFTER increment: completedWorkouts=${progressAfterIncrement?.completedWorkouts}/${progressAfterIncrement?.totalWorkouts}",
            )

            // Add additional debugging for programme details
            val programme = programmeDao.getProgrammeById(programmeId)
            if (programme != null) {
                println(
                    "📋 Programme details: name='${programme.name}', isCustom=${programme.isCustom}, durationWeeks=${programme.durationWeeks}",
                )
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
                println("🎆 No more workouts - checking if programme is actually complete...")
                // Double-check the completion logic before marking complete
                val finalProgress = programmeDao.getProgressForProgramme(programmeId)
                if (finalProgress != null) {
                    println(
                        "🔍 Final completion check: ${finalProgress.completedWorkouts} completed out of ${finalProgress.totalWorkouts} total",
                    )

                    // CRITICAL FIX: Recalculate total workouts for custom programmes to ensure accuracy
                    val programme = programmeDao.getProgrammeById(programmeId)
                    if (programme != null && programme.isCustom) {
                        val actualTotalWorkouts = programmeDao.getAllWorkoutsForProgramme(programmeId).size
                        println("🔍 Rechecking custom programme workout count: $actualTotalWorkouts workouts in database")

                        if (finalProgress.completedWorkouts < actualTotalWorkouts) {
                            println(
                                "⚠️ CRITICAL: Programme marked as complete but only ${finalProgress.completedWorkouts}/$actualTotalWorkouts workouts done!",
                            )
                            println("⚠️ There's a mismatch in workout tracking - NOT marking as complete")

                            // Update the total workouts to fix the mismatch
                            val correctedProgress = finalProgress.copy(totalWorkouts = actualTotalWorkouts)
                            programmeDao.insertOrUpdateProgress(correctedProgress)
                            println(
                                "✅ Fixed progress tracking: now shows ${correctedProgress.completedWorkouts}/${correctedProgress.totalWorkouts}",
                            )
                            return
                        }
                    }

                    if (finalProgress.completedWorkouts >= finalProgress.totalWorkouts) {
                        println("✅ Programme truly complete - marking as finished")
                        completeProgramme(programmeId)
                    } else {
                        println("⚠️ WARNING: getNextProgrammeWorkout returned null but programme not complete!")
                        println("   Completed: ${finalProgress.completedWorkouts}, Total: ${finalProgress.totalWorkouts}")
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


    // History functionality
    suspend fun getWorkoutHistory(): List<WorkoutSummary> {
        println("🔵 Repository.getWorkoutHistory called")
        val allWorkouts = workoutDao.getAllWorkouts()
        println("🔵 Repository: Found ${allWorkouts.size} total workouts in database")
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

                val isCompleted = workout.status == WorkoutStatus.COMPLETED

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
                    // TODO: Calculate duration
                    duration = null,
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

    suspend fun getTotalWorkoutCount(): Int {
        val allWorkouts = workoutDao.getAllWorkouts()
        println("🔍 DEBUG: Checking all workouts:")
        allWorkouts.forEach { workout ->
            val exercises = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
            println("  Workout ${workout.id}: ${workout.notes ?: "Unnamed"} - ${exercises.size} exercises, status: ${workout.status}")
        }

        val count =
            allWorkouts.count { workout ->
                val exercises = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
                // Count workout if it's completed OR has exercises
                workout.status == WorkoutStatus.COMPLETED || exercises.isNotEmpty()
            }
        println("🔍 getTotalWorkoutCount: $count valid workouts out of ${allWorkouts.size} total")
        return count
    }

    // Smart suggestions functionality (enhanced with exercise data)
    suspend fun getExerciseHistory(
        exerciseName: String,
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
            val matchingExercise = exercises.find { it.exerciseName == exerciseName }

            if (matchingExercise != null) {
                val sets =
                    setLogDao
                        .getSetLogsForExercise(matchingExercise.id)
                        .filter { it.isCompleted } // Only completed sets

                if (sets.isNotEmpty()) {
                    return ExerciseHistory(
                        exerciseName = exerciseName,
                        lastWorkoutDate = workout.date,
                        sets = sets,
                    )
                }
            }
        }
        return null
    }

    private suspend fun getExerciseStats(exerciseName: String): ExerciseStats? = exerciseRepository.getExerciseStats(exerciseName)

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
        val stats = getExerciseStats(exerciseName)
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
        exerciseName: String,
        targetReps: Int? = null,
    ): SmartSuggestions {
        val userId = getCurrentUserId()

        // Use the freestyle intelligence service for advanced suggestions
        return freestyleIntelligenceService.getIntelligentSuggestions(
            exerciseName = exerciseName,
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

    suspend fun deleteSetsForExerciseLog(exerciseLogId: Long) = exerciseRepository.deleteSetsForExerciseLog(exerciseLogId)

    // Get exercise by ID (returns basic Exercise)
    suspend fun getExerciseEntityById(exerciseId: Long): Exercise? = exerciseRepository.getExerciseEntityById(exerciseId)

    // Swap exercise
    suspend fun swapExercise(
        exerciseLogId: Long,
        newExerciseId: Long,
        newExerciseName: String,
        originalExerciseId: Long,
    ) = exerciseRepository.swapExercise(exerciseLogId, newExerciseId, newExerciseName, originalExerciseId)

    // Record an exercise swap in history
    suspend fun recordExerciseSwap(
        userId: Long,
        originalExerciseId: Long,
        swappedToExerciseId: Long,
        workoutId: Long? = null,
        programmeId: Long? = null,
    ) = exerciseRepository.recordExerciseSwap(userId, originalExerciseId, swappedToExerciseId, workoutId, programmeId)

    // Get swap history for exercise
    suspend fun getSwapHistoryForExercise(userId: Long, exerciseId: Long): List<SwapHistoryCount> {
        return exerciseRepository.getSwapHistoryForExercise(userId, exerciseId)
    }

    // ===== ANALYTICS METHODS =====

    // Volume analytics
    suspend fun getWeeklyVolumeTotal(startDate: LocalDateTime): Float =
        withContext(Dispatchers.IO) {
            println("🔵 Repository.getWeeklyVolumeTotal called with startDate: $startDate")
            val endDate = startDate.plusDays(7)
            val allWorkouts = workoutDao.getAllWorkouts()
            println("🔵 Repository: Total workouts in DB: ${allWorkouts.size}")
            val workouts =
                allWorkouts.filter {
                    it.date >= startDate && it.date < endDate && it.status == WorkoutStatus.COMPLETED
                }
            println("🔵 Repository: Filtered to ${workouts.size} workouts in date range")

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
            println("🔵 Repository: Weekly volume total: $totalVolume kg")
            totalVolume
        }

    suspend fun getMonthlyVolumeTotal(startDate: LocalDateTime): Float =
        withContext(Dispatchers.IO) {
            println("🔵 Repository.getMonthlyVolumeTotal called with startDate: $startDate")
            val endDate = startDate.plusMonths(1)
            val allWorkouts = workoutDao.getAllWorkouts()
            val workouts =
                allWorkouts.filter {
                    it.date >= startDate && it.date < endDate && it.status == WorkoutStatus.COMPLETED
                }
            println("🔵 Repository: Filtered to ${workouts.size} workouts in month range")

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
            println("🔵 Repository: Weekly volume total: $totalVolume kg")
            totalVolume
        }

    // Strength progression analytics
    suspend fun getPersonalRecords(exerciseName: String): List<Pair<Float, LocalDateTime>> =
        withContext(Dispatchers.IO) {
            println("🔵 Repository.getPersonalRecords called for $exerciseName - NOW USING PersonalRecord TABLE")

            // Get PRs from PersonalRecord table
            val prs = personalRecordDao.getRecentPRsForExercise(exerciseName, 100) // Get up to 100 PRs

            // Convert to the expected format: List of (estimated1RM, date) pairs
            val records =
                prs
                    .map { pr ->
                        Pair(pr.estimated1RM ?: pr.weight, pr.recordDate)
                    }.sortedBy { it.second } // Sort by date ascending

            println("🔵 Found ${records.size} PRs for $exerciseName from PersonalRecord table")
            records
        }

    // getEstimated1RM is defined above at line 2023

    suspend fun getRecentSetLogsForExercise(
        exerciseName: String,
        daysBack: Int,
    ): List<SetLog> =
        withContext(Dispatchers.IO) {
            val sinceDate = LocalDateTime.now().minusDays(daysBack.toLong())
            setLogDao.getSetsForExerciseSince(exerciseName, sinceDate.toString())
        }

    suspend fun generateWorkoutFromTemplate(
        template: WorkoutTemplate,
        config: WorkoutTemplateConfig,
    ): Long = workoutTemplateGeneratorService.generateWorkout(template, config)

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
                    sets.filter { it.isCompleted && it.actualRpe != null }.forEach { set ->
                        set.actualRpe?.let { allRPEs.add(it) }
                    }
                }
            }

            if (allRPEs.isNotEmpty()) allRPEs.average().toFloat() else null
        }

    suspend fun getTrainingStreak(): Int =
        withContext(Dispatchers.IO) {
            println("🔵 Repository.getTrainingStreak called")
            val allWorkouts =
                workoutDao
                    .getAllWorkouts()
                    .filter { it.status == WorkoutStatus.COMPLETED }
                    .sortedByDescending { it.date }
            println("🔵 Repository: Found ${allWorkouts.size} completed workouts for streak calculation")

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
            println("🔵 Repository.getRecentPR called - NOW USING PersonalRecord TABLE")
            Log.d("Analytics", "=== getRecentPR called - USING PersonalRecord TABLE ===")

            // Get the most recent PR from the PersonalRecord table
            val recentPRs = personalRecordDao.getRecentPRs(1)

            if (recentPRs.isNotEmpty()) {
                val mostRecentPR = recentPRs.first()
                Log.d(
                    "Analytics",
                    "Most recent PR from DB: ${mostRecentPR.exerciseName} - ${mostRecentPR.weight}kg x ${mostRecentPR.reps} = ${mostRecentPR.estimated1RM}kg 1RM",
                )

                // Return exercise name and the 1RM value (not the actual weight lifted)
                return@withContext Pair(mostRecentPR.exerciseName, mostRecentPR.estimated1RM ?: mostRecentPR.weight)
            }

            Log.d("Analytics", "No PRs found in PersonalRecord table")
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
            println("🔵 Repository.getProgressPercentage called with daysSince: $daysSince")
            val cutoffDate = LocalDateTime.now().minusDays(daysSince.toLong())
            val exerciseNames = listOf("Barbell Bench Press", "Barbell Back Squat", "Barbell Deadlift", "Barbell Overhead Press")
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

            val result = if (improvements.isNotEmpty()) improvements.average().toFloat() else null
            println("🔵 Repository: Progress percentage calculated: $result%")
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
                    ProgrammeWorkoutParser.getWorkoutForWeekAndDay(
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
                        )
                    val exerciseLogId = exerciseLogDao.insertExerciseLog(exerciseLog)

                    // Create sets for this exercise
                    createSetsFromStructure(exerciseLogId, exerciseStructure, userMaxes, programme, weekNumber)
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

        // Get the programme
        val programme =
            programmeDao.getProgrammeById(programmeId)
                ?: throw IllegalArgumentException("Programme not found")

        // Get the progress to find which workout to use
        val progress =
            programmeDao.getProgressForProgramme(programmeId)
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
                    throw IllegalArgumentException("Failed to parse workout structure: ${e2.message}")
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

        println("✅ Created custom programme workout: ${workoutStructure.name}")
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
            exerciseName = exerciseStructure.name,
            exerciseId = exerciseId,
            exerciseOrder = exerciseOrder,
            notes = notes,
        )
    }

    private suspend fun createSetsFromStructure(
        exerciseLogId: Long,
        exerciseStructure: ExerciseStructure,
        userMaxes: Map<String, Float>,
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
                            println(
                                "🌊 Wave progression: Week $weekNumber (cycle week ${cycleWeek + 1}), Set ${setIndex + 1} -> $intensity%",
                            )
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
            // Use templateName if available (for custom-named programmes), otherwise use programme name
            val templateNameToSearch = programme.templateName ?: programme.name
            println(
                "🔍 Looking for template with name: '$templateNameToSearch' (programme.name='${programme.name}', programme.templateName='${programme.templateName}')",
            )

            val allTemplates = programmeDao.getAllTemplates()
            println("📋 Available templates: ${allTemplates.map { it.name }}")

            val template =
                allTemplates.find { it.name == templateNameToSearch }
                    ?: run {
                        println("❌ Template not found for name: '$templateNameToSearch'")
                        return@withContext null
                    }

            // Parse the JSON structure
            val structure =
                ProgrammeWorkoutParser.parseStructure(template.jsonStructure)
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
                ProgrammeWorkoutParser.getWorkoutForWeekAndDay(
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
    private suspend fun getNextWorkoutFromDatabase(
        programmeId: Long,
        progress: ProgrammeProgress,
    ): NextProgrammeWorkoutInfo? {
        println("🔍 Looking up next workout from database for custom programme")

        // Get all workouts for this programme, ordered by week and day
        val allWorkouts = programmeDao.getAllWorkoutsForProgramme(programmeId)
        if (allWorkouts.isEmpty()) {
            println("❌ No workouts found for programme $programmeId")
            return null
        }

        println("📋 Found ${allWorkouts.size} total workouts for programme")
        println("📊 Progress tracking: completedWorkouts=${progress.completedWorkouts}, totalWorkouts=${progress.totalWorkouts}")

        // CRITICAL DEBUG: Check for mismatch
        if (allWorkouts.size != progress.totalWorkouts) {
            println("⚠️ WARNING: Workout count mismatch!")
            println("   Database has ${allWorkouts.size} workouts")
            println("   Progress tracking expects ${progress.totalWorkouts} workouts")
            println("   This may cause premature programme completion!")
        }

        // Find the next workout based on completed workouts count
        val nextWorkoutIndex = progress.completedWorkouts
        println("🎯 Looking for workout at index $nextWorkoutIndex (0-based)")

        if (nextWorkoutIndex >= allWorkouts.size) {
            println("✅ All workouts completed ($nextWorkoutIndex >= ${allWorkouts.size})")
            println("⚠️ CRITICAL: Programme being marked complete after ${progress.completedWorkouts} workouts")
            return null
        }

        val nextWorkout = allWorkouts[nextWorkoutIndex]
        println("🎯 Next workout: ${nextWorkout.name} (weekId ${nextWorkout.weekId}, day ${nextWorkout.dayNumber})")

        // Additional debug info about all workouts
        println("📋 All workouts for debugging:")
        allWorkouts.forEachIndexed { index, workout ->
            val status =
                if (index <
                    progress.completedWorkouts
                ) {
                    "✅ COMPLETED"
                } else if (index == progress.completedWorkouts) {
                    "➡️ NEXT"
                } else {
                    "⏳ PENDING"
                }
            println("   [$index] ${workout.name} (Week ${workout.weekId}, Day ${workout.dayNumber}) $status")
        }

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
                println("❌ Failed to parse workout structure: ${e.message}")
                println("   JSON content: ${nextWorkout.workoutStructure}")

                // Try to fix invalid JSON by replacing ,"reps":, with ,"reps":"8-12",
                val fixedJson =
                    nextWorkout.workoutStructure
                        .replace("\"reps\":,", "\"reps\":\"8-12\",")
                        .replace("\"reps\": ,", "\"reps\":\"8-12\",")

                println("🔧 Attempting to fix invalid JSON...")
                try {
                    val json =
                        kotlinx.serialization.json.Json {
                            ignoreUnknownKeys = true
                            isLenient = true
                        }
                    json.decodeFromString<WorkoutStructure>(fixedJson)
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
                ProgrammeWorkoutParser.parseStructure(template.jsonStructure)
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
                        "- ${error.field}: ${error.value} - ${error.error}" +
                            (error.suggestion?.let { " (Try: $it)" } ?: "")
                    }
                throw IllegalArgumentException(
                    "Cannot create programme from template '${template.name}':\n$errorMessage",
                )
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

                println("✅ Created programme structure: $totalWeeks weeks with ${structure.weeks[0].workouts.size} workouts per week")
            }

            programmeId
        }

    suspend fun createAIGeneratedProgramme(preview: GeneratedProgrammePreview): Long =
        withContext(Dispatchers.IO) {
            // Deactivate any currently active programme
            println("⚠️ createAIGeneratedProgramme: Deactivating all programmes before creating new one")
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

            println("🔄 Initializing progress for AI-generated programme...")
            println("  - Programme ID: $programmeId")
            println("  - Duration: ${preview.durationWeeks} weeks")
            println("  - Days per week: ${preview.daysPerWeek}")
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

                // Update status to IN_PROGRESS
                programmeDao.updateProgrammeStatus(programmeId, ProgrammeStatus.IN_PROGRESS)
                println("✅ Programme marked as active with status IN_PROGRESS")

                // Initialize progress tracking
                println("🔄 Getting programme details...")
                val programme = programmeDao.getProgrammeById(programmeId) ?: return@withContext
                println("✅ Programme found: ${programme.name}")

                // Calculate total workouts based on actual JSON structure
                val totalWorkouts = calculateTotalWorkoutsFromStructure(programme)
                println("📊 Calculated total workouts: $totalWorkouts")

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

                println("🔄 Inserting progress tracking...")
                programmeDao.insertOrUpdateProgress(progress)
                println("✅ Progress tracking initialized")
            } catch (e: Exception) {
                println("❌ Error in activateProgramme: ${e.message}")
                throw e
            }
        }

    private suspend fun completeProgramme(programmeId: Long) =
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

                // ATOMIC update - sets status=COMPLETED, isActive=false, and completedAt in one transaction
                val completionTime = LocalDateTime.now()
                programmeDao.completeProgrammeAtomic(programmeId, completionTime)

                println("✅ Programme marked as complete atomically:")
                println("   - status: COMPLETED")
                println("   - isActive: false")
                println("   - completedAt: $completionTime")
            } else {
                println("❌ Programme or progress not found for completion")
            }
        }

    suspend fun deleteProgramme(programme: Programme) =
        withContext(Dispatchers.IO) {
            println("🗑️ Repository: Starting deletion of programme ${programme.name} (ID: ${programme.id})")

            // First deactivate the programme if it's active
            if (programme.isActive) {
                println("🔄 Programme is active, deactivating all programmes first")
                programmeDao.deactivateAllProgrammes()
            }

            // Delete all workouts associated with this programme to prevent orphaned workouts
            println("🗑️ Deleting workouts associated with programme ${programme.id}")
            val deletedWorkouts = workoutDao.deleteWorkoutsByProgramme(programme.id)
            println("✅ Deleted $deletedWorkouts workouts")

            // Then delete the programme (will cascade delete progress and related data)
            println("🗑️ Deleting programme from database")
            programmeDao.deleteProgramme(programme)
            println("✅ Programme ${programme.name} deleted successfully")
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
        println("📊 Workout $workoutId status updated to $status")
    }

    suspend fun updateWorkoutTimerStart(
        workoutId: Long,
        timerStartTime: LocalDateTime,
    ) = withContext(Dispatchers.IO) {
        val workout = workoutDao.getWorkoutById(workoutId) ?: return@withContext
        val updatedWorkout = workout.copy(timerStartTime = timerStartTime)
        workoutDao.updateWorkout(updatedWorkout)
        println("⏱️ Workout $workoutId timer started at $timerStartTime")
    }

    // Helper method to get exercise by name or alias
    suspend fun getExerciseByName(name: String): Exercise? = exerciseRepository.getExerciseByName(name)

    // ========== Profile & 1RM Management ==========

    suspend fun ensureUserProfile(userId: Long) =
        withContext(Dispatchers.IO) {
            db.profileDao().getUserProfile(userId)
                ?: // Should not happen as users are created through seedTestUsers
                throw IllegalStateException("User profile not found for ID: $userId")
        }

    suspend fun getAllUsers() =
        withContext(Dispatchers.IO) {
            db.profileDao().getAllUsers()
        }

    suspend fun upsertExerciseMax(
        userId: Long,
        exerciseId: Long,
        oneRMEstimate: Float,
        oneRMContext: String,
        oneRMType: OneRMType,
        notes: String? = null,
    ) = withContext(Dispatchers.IO) {
        ensureUserProfile(userId)
        // Round weight to nearest 0.25
        val roundedWeight = WeightFormatter.roundToNearestQuarter(oneRMEstimate)
        val userExerciseMax = UserExerciseMax(
            userId = userId,
            exerciseId = exerciseId,
            mostWeightLifted = roundedWeight,
            mostWeightReps = 1,
            mostWeightRpe = null,
            mostWeightDate = LocalDateTime.now(),
            oneRMEstimate = roundedWeight,
            oneRMContext = oneRMContext,
            oneRMConfidence = if (oneRMType == OneRMType.MANUALLY_ENTERED) 1.0f else 0.9f,
            oneRMDate = LocalDateTime.now(),
            oneRMType = oneRMType,
            notes = notes,
        )
        db.oneRMDao().insertOrUpdateExerciseMax(userExerciseMax)
    }

    // Remove getAllCurrentMaxes - this should only be used in Insights

    suspend fun getCurrentMaxesForExercises(userId: Long, exerciseIds: List<Long>) =
        withContext(Dispatchers.IO) {
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
                    exerciseId = max.exerciseId,
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
                    notes = max.notes
                )
            }
        }

    suspend fun deleteExerciseMax(max: UserExerciseMax) =
        withContext(Dispatchers.IO) {
            db.oneRMDao().deleteExerciseMax(max)
        }
    
    fun getBig4ExercisesWithMaxes(userId: Long): Flow<List<com.github.radupana.featherweight.data.profile.Big4ExerciseWithOptionalMax>> =
        db.oneRMDao().getBig4ExercisesWithMaxes(userId)
    
    fun getOtherExercisesWithMaxes(userId: Long): Flow<List<OneRMWithExerciseName>> =
        db.oneRMDao().getOtherExercisesWithMaxes(userId)
        
    suspend fun getOneRMForExercise(exerciseName: String): Float? =
        withContext(Dispatchers.IO) {
            val exercise = getExerciseByName(exerciseName) ?: return@withContext null
            val userId = getCurrentUserId()
            val exerciseMax = db.oneRMDao().getCurrentMax(userId, exercise.id)
            return@withContext exerciseMax?.oneRMEstimate
        }
        
    suspend fun getEstimated1RM(exerciseName: String): Float? =
        withContext(Dispatchers.IO) {
            Log.d("Analytics", "=== getEstimated1RM for $exerciseName ===")
            
            // Try to get from 1RM records first
            val oneRM = getOneRMForExercise(exerciseName)
            if (oneRM != null) {
                Log.d("Analytics", "Found 1RM record: $oneRM kg")
                return@withContext oneRM
            }
            
            // If no 1RM record, calculate from recent workout history
            val recentSets = getRecentSetLogsForExercise(exerciseName, daysBack = 30)
                .filter { it.isCompleted && it.actualWeight > 0 && it.actualReps > 0 }
                .sortedByDescending { it.actualWeight }
            
            if (recentSets.isEmpty()) {
                Log.d("Analytics", "No completed sets found for $exerciseName")
                return@withContext null
            }
            
            // Use Brzycki formula on the heaviest recent set
            val bestSet = recentSets.first()
            val estimated1RM = if (bestSet.actualReps == 1) {
                bestSet.actualWeight
            } else {
                bestSet.actualWeight * (36f / (37f - bestSet.actualReps))
            }
            
            Log.d("Analytics", "Calculated 1RM from ${bestSet.actualWeight}kg x ${bestSet.actualReps} = $estimated1RM kg")
            return@withContext estimated1RM
        }

    suspend fun deleteAllMaxesForExercise(exerciseId: Long) =
        withContext(Dispatchers.IO) {
            db.oneRMDao().deleteAllMaxesForExercise(getCurrentUserId(), exerciseId)
        }
        
    suspend fun updateOrInsertOneRM(oneRMRecord: UserExerciseMax) =
        withContext(Dispatchers.IO) {
            // Check if we have an existing record for this user/exercise
            val existing = db.oneRMDao().getCurrentMax(oneRMRecord.userId, oneRMRecord.exerciseId)
            
            if (existing != null) {
                // Update existing record if new estimate is higher
                if (oneRMRecord.oneRMEstimate > existing.oneRMEstimate) {
                    db.oneRMDao().updateExerciseMax(oneRMRecord.copy(id = existing.id))
                }
            } else {
                // Insert new record
                db.oneRMDao().insertExerciseMax(oneRMRecord)
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
                                exerciseId = exercise.id,
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
                                exerciseId = exercise.id,
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

                println("✅ Seeded test users with 1RMs")
            }
        }

    private suspend fun calculateIntelligentWeight(
        exerciseStructure: ExerciseStructure,
        reps: Int,
        userMaxes: Map<String, Float>,
        intensity: Int? = null,
        programme: Programme? = null,
    ): Float {
        println("\n=== WEIGHT CALCULATION LOG ===")
        println("Exercise: ${exerciseStructure.name}")
        println("Reps: $reps")
        println("Programme: ${programme?.name ?: "None"}")
        println("Intensity: ${intensity ?: "None"}")
        // First check if we have intensity-based calculation (e.g., Wendler 5/3/1)
        if (intensity != null && programme != null) {
            val weightCalcRules = programme.getWeightCalculationRulesObject()
            if (weightCalcRules?.baseOn == WeightBasis.ONE_REP_MAX) {
                // Get user's 1RM for this exercise
                val user1RM = getUserMaxForExercise(exerciseStructure.name, userMaxes)
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

                    println("Calculation Method: 1RM-based with intensity")
                    println("User 1RM: $user1RM kg")
                    println("Training Max (${(weightCalcRules.trainingMaxPercentage * 100).toInt()}%): $trainingMax kg")
                    println("Calculated Weight ($intensity% of TM): $calculatedWeight kg")
                    println("Rounded to increment ($roundingIncrement kg): $rounded kg")
                    println("Final Weight: ${rounded.coerceAtLeast(minimumWeight)} kg")
                    println("==============================\n")
                    return rounded.coerceAtLeast(minimumWeight)
                }
            }
        }

        // Check for LAST_WORKOUT based calculation (e.g., StrongLifts)
        if (programme != null) {
            val weightCalcRules = programme.getWeightCalculationRulesObject()
            if (weightCalcRules?.baseOn == WeightBasis.LAST_WORKOUT) {
                println("Calculation Method: Last Workout + Progression")

                // Use ProgressionService for intelligent weight calculation with deload support
                val progressionService =
                    ProgressionService(
                        performanceTrackingDao = db.exercisePerformanceTrackingDao(),
                        exerciseRepository = exerciseRepository,
                        programmeDao = programmeDao,
                        repository = this@FeatherweightRepository,
                    )

                // Calculate the progression decision
                val decision =
                    progressionService.calculateProgressionWeight(
                        exerciseName = exerciseStructure.name,
                        programme = programme,
                        currentWorkoutId = 0L, // This is during workout creation, so we don't have an ID yet
                    )

                println("Progression Decision: ${decision.action}")
                println("Weight: ${decision.weight} kg")
                println("Reason: ${decision.reason}")
                if (decision.isDeload) {
                    println("⚠️ DELOAD WORKOUT")
                }
                println("==============================\n")

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
            println("Calculation Method: AI-suggested with context")
            println("AI Suggested Weight: ${exerciseStructure.suggestedWeight} kg")
            println("Weight Source: ${exerciseStructure.weightSource}")
            println("==============================\n")
            return exerciseStructure.suggestedWeight
        }

        // Fallback to existing intelligent weight calculation
        println("Calculation Method: AI/Default calculation")
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
        val user1RM = getUserMaxForExercise(exerciseStructure.name, userMaxes)

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

        println("Calculated Weight: ${weightCalculation.weight} kg")
        println("Weight Source: ${weightCalculation.source}")
        println("==============================\n")

        return weightCalculation.weight
    }

    private suspend fun recordWorkoutPerformanceData(
        workoutId: Long,
        programmeId: Long,
    ) = withContext(Dispatchers.IO) {
        println("📊 Recording performance data for workout $workoutId in programme $programmeId")

        val progressionService =
            ProgressionService(
                performanceTrackingDao = db.exercisePerformanceTrackingDao(),
                exerciseRepository = exerciseRepository,
                programmeDao = programmeDao,
                repository = this@FeatherweightRepository,
            )

        // Get all exercises and sets for this workout
        val exercises = exerciseLogDao.getExerciseLogsForWorkout(workoutId)

        for (exercise in exercises) {
            val sets = setLogDao.getSetLogsForExercise(exercise.id)
            if (sets.isNotEmpty()) {
                progressionService.recordWorkoutPerformance(
                    workoutId = workoutId,
                    programmeId = programmeId,
                    exerciseName = exercise.exerciseName,
                    exerciseLogId = exercise.id,
                    sets = sets,
                )
            }
        }

        println("✅ Performance data recorded for ${exercises.size} exercises")
    }

    private fun getUserMaxForExercise(
        exerciseName: String,
        userMaxes: Map<String, Float>,
    ): Float? {
        // Map exercise names to user max keys (similar to existing logic)
        val maxKey =
            when {
                exerciseName.contains("Squat", ignoreCase = true) -> "squat"
                exerciseName.contains("Bench", ignoreCase = true) -> "bench"
                exerciseName.contains("Deadlift", ignoreCase = true) -> "deadlift"
                exerciseName.contains("Press", ignoreCase = true) &&
                    !exerciseName.contains("Bench", ignoreCase = true) -> "ohp"

                else -> null
            }

        return maxKey?.let { userMaxes[it] }
    }

    // ===== INTELLIGENT SUGGESTIONS SUPPORT =====

    // getOneRMForExercise is defined above at line 2015

    suspend fun getHistoricalPerformance(
        exerciseName: String,
        minReps: Int,
        maxReps: Int,
    ): List<com.github.radupana.featherweight.service.PerformanceData> =
        withContext(Dispatchers.IO) {
            // Get exercise logs for this exercise
            val exerciseLogs = db.workoutDao().getExerciseLogsByName(exerciseName)
            val exerciseIds = exerciseLogs.map { it.id }

            if (exerciseIds.isEmpty()) return@withContext emptyList()

            // Get sets within rep range
            val sets = db.workoutDao().getSetsForExercisesInRepRange(exerciseIds, minReps, maxReps)

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
        exerciseName: String,
        limit: Int = 10,
    ): List<com.github.radupana.featherweight.service.PerformanceData> =
        withContext(Dispatchers.IO) {
            // Get recent exercise logs for this exercise
            val exerciseLogs = db.workoutDao().getExerciseLogsByName(exerciseName)
            val exerciseIds = exerciseLogs.map { it.id }

            if (exerciseIds.isEmpty()) return@withContext emptyList()

            // Get recent sets
            val sets = db.workoutDao().getRecentSetsForExercises(exerciseIds, limit)

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
        exerciseName: String,
        minWeight: Float,
        maxWeight: Float,
    ): List<com.github.radupana.featherweight.service.PerformanceData> =
        withContext(Dispatchers.IO) {
            // Get exercise logs for this exercise
            val exerciseLogs = db.workoutDao().getExerciseLogsByName(exerciseName)
            val exerciseIds = exerciseLogs.map { it.id }

            if (exerciseIds.isEmpty()) return@withContext emptyList()

            // Get sets within weight range
            val sets = db.workoutDao().getSetsForExercisesInWeightRange(exerciseIds, minWeight, maxWeight)

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
        exerciseName: String,
    ): GlobalExerciseProgress? =
        withContext(Dispatchers.IO) {
            globalExerciseProgressDao.getProgressForExercise(userId, exerciseName)
        }

    /**
     * Check if a completed set represents a personal record
     * Returns list of PersonalRecord objects if PRs are detected
     */
    suspend fun checkForPR(
        setLog: SetLog,
        exerciseName: String,
    ): List<PersonalRecord> =
        withContext(Dispatchers.IO) {
            prDetectionService.checkForPR(setLog, exerciseName)
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
    
    suspend fun getCurrentOneRMEstimate(userId: Long, exerciseId: Long): Float? =
        withContext(Dispatchers.IO) {
            db.oneRMDao().getCurrentOneRMEstimate(userId, exerciseId)
        }
    
    /**
     * Update workout notes
     */
    suspend fun updateWorkoutNotes(workoutId: Long, notes: String?) =
        withContext(Dispatchers.IO) {
            val workout = workoutDao.getWorkoutById(workoutId)
            if (workout != null) {
                val updatedWorkout = workout.copy(
                    notes = notes?.takeIf { it.isNotBlank() },
                    notesUpdatedAt = if (notes?.isNotBlank() == true) LocalDateTime.now() else null
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
    suspend fun updateWorkoutName(workoutId: Long, name: String?) =
        withContext(Dispatchers.IO) {
            val workout = workoutDao.getWorkoutById(workoutId)
            if (workout != null) {
                val updatedWorkout = workout.copy(
                    name = name?.takeIf { it.isNotBlank() }
                )
                workoutDao.updateWorkout(updatedWorkout)
            }
        }
    
    
    /**
     * Get programme completion notes
     */
    suspend fun getProgrammeCompletionNotes(programmeId: Long): String? =
        withContext(Dispatchers.IO) {
            programmeDao.getProgrammeById(programmeId)?.completionNotes
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
        exerciseName: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<ExerciseWorkoutSummary> =
        withContext(Dispatchers.IO) {
            val startDateTime = startDate.atStartOfDay()
            val endDateTime = endDate.atTime(23, 59, 59)
            val exerciseLogs = exerciseLogDao.getExerciseLogsInDateRange(exerciseName, startDateTime, endDateTime)

            exerciseLogs.map { exerciseLog ->
                val sets = setLogDao.getSetLogsForExercise(exerciseLog.id)
                val maxWeight = sets.filter { it.isCompleted }.maxOfOrNull { it.actualWeight } ?: 0f
                val maxReps = sets.filter { it.isCompleted && it.actualWeight == maxWeight }.maxOfOrNull { it.actualReps } ?: 0
                val totalSets = sets.count { it.isCompleted }
                val totalVolume = sets.filter { it.isCompleted }.sumOf { (it.actualWeight * it.actualReps).toDouble() }.toFloat()

                val workout = workoutDao.getWorkoutById(exerciseLog.workoutId)

                ExerciseWorkoutSummary(
                    exerciseLogId = exerciseLog.id,
                    workoutId = exerciseLog.workoutId,
                    workoutDate = workout?.date ?: LocalDateTime.now(),
                    actualWeight = maxWeight,
                    actualReps = maxReps,
                    sets = totalSets,
                    totalVolume = totalVolume,
                )
            }
        }

    suspend fun getPersonalRecordForExercise(
        exerciseName: String,
    ): PersonalRecord? =
        withContext(Dispatchers.IO) {
            personalRecordDao.getLatestRecordForExercise(exerciseName)
        }

    private suspend fun getTotalSessionsForExercise(
        exerciseName: String,
    ): Int =
        withContext(Dispatchers.IO) {
            exerciseLogDao.getTotalSessionsForExercise(exerciseName)
        }

    suspend fun getMaxWeightForExerciseInDateRange(
        exerciseName: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Float? =
        withContext(Dispatchers.IO) {
            val startDateTime = startDate.atStartOfDay()
            val endDateTime = endDate.atTime(23, 59, 59)
            setLogDao.getMaxWeightForExerciseInDateRange(
                exerciseName,
                startDateTime.toString(),
                endDateTime.toString(),
            )
        }

    suspend fun getDistinctWorkoutDatesForExercise(
        exerciseName: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<LocalDate> =
        withContext(Dispatchers.IO) {
            val startDateTime = startDate.atStartOfDay()
            val endDateTime = endDate.atTime(23, 59, 59)
            exerciseLogDao
                .getDistinctWorkoutsForExercise(exerciseName, startDateTime, endDateTime)
                .map { it.date.toLocalDate() }
                .distinct()
        }

    suspend fun getDateOfMaxWeightForExercise(
        exerciseName: String,
        weight: Float,
        startDate: LocalDate,
        endDate: LocalDate,
    ): LocalDate? =
        withContext(Dispatchers.IO) {
            // Get the actual workout date when this weight was achieved
            val startDateTime = startDate.atStartOfDay()
            val endDateTime = endDate.plusDays(1).atStartOfDay()

            Log.d("Repository", "🔍 getDateOfMaxWeightForExercise DEBUG:")
            Log.d("Repository", "  - Exercise: $exerciseName")
            Log.d("Repository", "  - Weight: $weight")
            Log.d("Repository", "  - Date range: $startDate to $endDate")

            val workoutDate = workoutDao.getWorkoutDateForMaxWeight(exerciseName, weight, startDateTime, endDateTime)
            Log.d("Repository", "  - Found workout date: $workoutDate")

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
            val allExercises = exerciseLogDao.getAllUniqueExercises()

            val allSummaries =
                allExercises
                    .mapNotNull { exerciseName ->
                        val globalProgress = getGlobalExerciseProgress(userId, exerciseName)
                        if (globalProgress != null) {
                            // Get actual PR for this exercise (not estimated)
                            val personalRecord = getPersonalRecordForExercise(exerciseName)
                            val actualMaxWeight = personalRecord?.weight ?: globalProgress.currentWorkingWeight

                            // Get recent workout data for mini chart
                            val recentWorkouts =
                                getExerciseWorkoutsInDateRange(
                                    exerciseName = exerciseName,
                                    startDate =
                                        java.time.LocalDate
                                            .now()
                                            .minusDays(90),
                                    endDate = java.time.LocalDate.now(),
                                )

                            // Calculate progress percentage (last 30 days)
                            val thirtyDaysAgo =
                                java.time.LocalDate
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
                                sessionCount = getTotalSessionsForExercise(exerciseName),
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

    suspend fun deleteAllWorkouts() {
        db.workoutDao().deleteAllWorkouts()
        db.exerciseLogDao().deleteAllExerciseLogs()
        db.setLogDao().deleteAllSetLogs()
        db.personalRecordDao().deleteAllPersonalRecords()
        db.globalExerciseProgressDao().deleteAllGlobalProgress()
        println("🗑️ Repository: Deleted all workout-related data")
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
    suspend fun getCompletedWorkoutsPagedOptimized(
        page: Int,
        pageSize: Int = 20,
    ): List<WorkoutSummaryWithProgramme> =
        withContext(Dispatchers.IO) {
            val offset = page * pageSize
            // Use efficient paginated query at database level
            val allWorkouts = workoutDao.getCompletedWorkoutsPaged(pageSize, offset)

            allWorkouts.map { workout ->
                val exercises = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
                val sets =
                    exercises.flatMap { exercise ->
                        setLogDao.getSetLogsForExercise(exercise.id)
                    }
                val programmeName =
                    workout.programmeId?.let {
                        programmeDao.getProgrammeById(it)?.name
                    }

                WorkoutSummaryWithProgramme(
                    id = workout.id,
                    date = workout.date,
                    name = workout.name ?: workout.programmeWorkoutName,
                    programmeName = programmeName,
                    exerciseCount = exercises.size,
                    setCount = sets.size,
                    totalWeight =
                        sets
                            .filter { set -> set.isCompleted }
                            .sumOf { set -> (set.actualWeight * set.actualReps).toDouble() }
                            .toFloat(),
                    duration = workout.durationSeconds?.let { it / 60 },
                    status = workout.status,
                    hasNotes = !workout.notes.isNullOrBlank(),
                )
            }
        }

    // Get completed programmes with workout count
    suspend fun getCompletedProgrammesPaged(
        page: Int,
        pageSize: Int = 20,
    ): List<ProgrammeSummary> =
        withContext(Dispatchers.IO) {
            println("📋 getCompletedProgrammesPaged: page=$page, pageSize=$pageSize")
            val offset = page * pageSize
            // Use efficient paginated query at database level
            val programmes = programmeDao.getCompletedProgrammesPaged(pageSize, offset)
            println("📋 Found ${programmes.size} completed programmes from database")

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

            val weekDetails =
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
                                        exerciseNames = exercises.take(3).map { it.exerciseName },
                                        totalExercises = exercises.size,
                                        totalVolume =
                                            sets
                                                .filter { set -> set.isCompleted }
                                                .sumOf { set -> (set.actualWeight * set.actualReps).toDouble() }
                                                .toFloat(),
                                        duration = workout.durationSeconds?.let { it / 60 },
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
                        exerciseName = exercise.exerciseName,
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
                            // Set target values from previous performance
                            targetReps = set.actualReps,
                            targetWeight = set.actualWeight,
                            // Pre-populate actual values so sets can be immediately marked complete
                            actualReps = set.actualReps,
                            actualWeight = set.actualWeight,
                            // Don't copy RPE - let user set it fresh
                            actualRpe = null,
                            isCompleted = false,
                        )
                    println("🔄 Copying set: targetReps=${newSet.targetReps}, targetWeight=${newSet.targetWeight}, actualReps=${newSet.actualReps}, actualWeight=${newSet.actualWeight}")
                    insertSetLog(newSet)
                }
            }

            newWorkoutId
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
    val programmeType: com.github.radupana.featherweight.data.programme.ProgrammeType = com.github.radupana.featherweight.data.programme.ProgrammeType.GENERAL_FITNESS,
    val difficulty: ProgrammeDifficulty = ProgrammeDifficulty.INTERMEDIATE,
)

data class ProgrammeHistoryDetails(
    val id: Long,
    val name: String,
    val programmeType: com.github.radupana.featherweight.data.programme.ProgrammeType,
    val difficulty: ProgrammeDifficulty,
    val durationWeeks: Int,
    val startedAt: LocalDateTime?,
    val completedAt: LocalDateTime?,
    val completedWorkouts: Int,
    val totalWorkouts: Int,
    val programDurationDays: Int,
    val workoutHistory: List<WorkoutHistoryEntry>,
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
