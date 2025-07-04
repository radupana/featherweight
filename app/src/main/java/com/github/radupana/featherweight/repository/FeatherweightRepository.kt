package com.github.radupana.featherweight.repository

import android.app.Application
import com.github.radupana.featherweight.ai.ProgrammeType
import com.github.radupana.featherweight.ai.WeightCalculator
import com.github.radupana.featherweight.service.ProgressionService
import com.github.radupana.featherweight.service.GlobalProgressTracker
import com.github.radupana.featherweight.service.FreestyleIntelligenceService
import com.github.radupana.featherweight.service.PRDetectionService
import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.GlobalExerciseProgress
import com.github.radupana.featherweight.data.PersonalRecord
import com.github.radupana.featherweight.data.ProgressTrend
import com.github.radupana.featherweight.data.exercise.MovementPattern
import com.github.radupana.featherweight.data.PendingOneRMUpdate
import com.github.radupana.featherweight.data.exercise.MuscleGroup
import com.github.radupana.featherweight.data.ExerciseSwapHistory
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.GeneratedProgrammePreview
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.SwapHistoryCount
import com.github.radupana.featherweight.data.UserPreferences
import com.github.radupana.featherweight.data.VolumeLevel
import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.data.WorkoutStatus
import com.github.radupana.featherweight.data.exercise.Equipment
import com.github.radupana.featherweight.data.exercise.Exercise
import com.github.radupana.featherweight.data.exercise.ExerciseAliasSeeder
import com.github.radupana.featherweight.data.exercise.ExerciseCategory
import com.github.radupana.featherweight.data.exercise.ExerciseSeeder
import com.github.radupana.featherweight.data.exercise.ExerciseCorrelationSeeder
import com.github.radupana.featherweight.data.exercise.ExerciseWithDetails
import com.github.radupana.featherweight.data.profile.UserExerciseMax
import com.github.radupana.featherweight.data.profile.UserProfile
import com.github.radupana.featherweight.data.programme.ExerciseStructure
import com.github.radupana.featherweight.data.programme.ExerciseSubstitution
import com.github.radupana.featherweight.data.programme.ExercisePerformanceData
import com.github.radupana.featherweight.data.programme.Programme
import com.github.radupana.featherweight.data.programme.ProgrammeDifficulty
import com.github.radupana.featherweight.data.programme.ProgrammeProgress
import com.github.radupana.featherweight.data.programme.ProgrammeSeeder
import com.github.radupana.featherweight.data.programme.ProgrammeWeek
import com.github.radupana.featherweight.data.programme.ProgrammeWorkout
import com.github.radupana.featherweight.data.programme.ProgrammeWorkoutParser
import com.github.radupana.featherweight.data.programme.RepsStructure
import com.github.radupana.featherweight.data.programme.WorkoutStructure
import com.github.radupana.featherweight.data.programme.ProgressionType
import com.github.radupana.featherweight.data.programme.WeightBasis
import com.github.radupana.featherweight.domain.ExerciseHistory
import com.github.radupana.featherweight.domain.ExerciseStats
import com.github.radupana.featherweight.domain.SmartSuggestions
import com.github.radupana.featherweight.validation.ExerciseValidator
import com.github.radupana.featherweight.validation.ValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    // Programme Integration Fields
    val isProgrammeWorkout: Boolean = false,
    val programmeId: Long? = null,
    val programmeName: String? = null,
    val programmeWorkoutName: String? = null,
    val weekNumber: Int? = null,
    val dayNumber: Int? = null,
    val prCount: Int = 0, // Number of PRs achieved in this workout
)

class FeatherweightRepository(
    application: Application,
) {
    init {
        android.util.Log.e("FeatherweightDebug", "FeatherweightRepository: Starting initialization")
    }

    private val db =
        FeatherweightDatabase.getDatabase(application).also {
            android.util.Log.e("FeatherweightDebug", "FeatherweightRepository: Database obtained")
        }
    private val userPreferences =
        UserPreferences(application).also {
            android.util.Log.e("FeatherweightDebug", "FeatherweightRepository: UserPreferences created")
        }
    
    // Sub-repositories
    private val exerciseRepository = ExerciseRepository(db)

    private val workoutDao =
        db.workoutDao().also {
            android.util.Log.e("FeatherweightDebug", "FeatherweightRepository: workoutDao obtained")
        }
    private val exerciseLogDao =
        db.exerciseLogDao().also {
            android.util.Log.e("FeatherweightDebug", "FeatherweightRepository: exerciseLogDao obtained")
        }
    private val setLogDao =
        db.setLogDao().also {
            android.util.Log.e("FeatherweightDebug", "FeatherweightRepository: setLogDao obtained")
        }
    private val exerciseDao =
        db.exerciseDao().also {
            android.util.Log.e("FeatherweightDebug", "FeatherweightRepository: exerciseDao obtained")
        }
    private val programmeDao =
        db.programmeDao().also {
            android.util.Log.e("FeatherweightDebug", "FeatherweightRepository: programmeDao obtained")
        }
    private val profileDao =
        db.profileDao().also {
            android.util.Log.e("FeatherweightDebug", "FeatherweightRepository: profileDao obtained")
        }
    
    private val globalExerciseProgressDao = db.globalExerciseProgressDao()
    private val exerciseCorrelationDao = db.exerciseCorrelationDao()
    private val personalRecordDao = db.personalRecordDao()
    private val userAchievementDao = db.userAchievementDao()
    
    // Initialize GlobalProgressTracker
    private val globalProgressTracker = GlobalProgressTracker(this, db)
    private val freestyleIntelligenceService = FreestyleIntelligenceService(this, db.globalExerciseProgressDao())
    private val prDetectionService = PRDetectionService(personalRecordDao)
    private val achievementDetectionService = com.github.radupana.featherweight.service.AchievementDetectionService(db)
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
        profileDao.upsertExerciseMax(
            userId = userId,
            exerciseId = update.exerciseId,
            maxWeight = update.suggestedMax,
            isEstimated = false,
            notes = "Updated from ${update.source}"
        )
        
        // Remove this update from pending list
        _pendingOneRMUpdates.value = _pendingOneRMUpdates.value.filter { 
            it.exerciseId != update.exerciseId 
        }
    }

    private val exerciseSeeder =
        ExerciseSeeder(exerciseDao, application).also {
            android.util.Log.e("FeatherweightDebug", "FeatherweightRepository: ExerciseSeeder created")
        }
    private val exerciseAliasSeeder =
        ExerciseAliasSeeder(exerciseDao).also {
            android.util.Log.e("FeatherweightDebug", "FeatherweightRepository: ExerciseAliasSeeder created")
        }
    private val programmeSeeder =
        ProgrammeSeeder(programmeDao, exerciseDao).also {
            android.util.Log.e("FeatherweightDebug", "FeatherweightRepository: ProgrammeSeeder created")
        }
    
    private val exerciseCorrelationSeeder = ExerciseCorrelationSeeder(exerciseCorrelationDao)

    fun getCurrentUserId(): Long = userPreferences.getCurrentUserId()

    // Initialize with seed data for testing
    suspend fun seedDatabaseIfEmpty() {
        withContext(Dispatchers.IO) {
            val exerciseCount = exerciseDao.getAllExercisesWithDetails().size
            val workoutCount = workoutDao.getAllWorkouts().size
            val programmeTemplateCount = programmeDao.getAllTemplates().size

            // Always seed exercises if there are none
            if (exerciseCount == 0) {
                exerciseSeeder.seedExercises()
                println("Seeded ${exerciseDao.getAllExercisesWithDetails().size} exercises")

                // Seed aliases after exercises are created
                exerciseAliasSeeder.seedExerciseAliases()
                println("Seeded exercise aliases")
            }

            // Seed programme templates if none exist
            if (programmeTemplateCount == 0) {
                programmeSeeder.seedPopularProgrammes()
                println("Seeded ${programmeDao.getAllTemplates().size} programme templates")
            }
            
            // Seed exercise correlations if none exist
            if (exerciseCorrelationDao.getCount() == 0) {
                exerciseCorrelationSeeder.seedExerciseCorrelations()
                println("Seeded exercise correlations")
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
                count +=
                    exerciseLogs.count { log ->
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

    // ===== EXERCISE METHODS (Delegated to ExerciseRepository) =====

    suspend fun getAllExercises(): List<ExerciseWithDetails> = exerciseRepository.getAllExercises()

    suspend fun getAllExerciseNamesIncludingAliases(): List<String> = exerciseRepository.getAllExerciseNamesIncludingAliases()

    suspend fun getAllExercisesWithUsageStats(): List<Pair<ExerciseWithDetails, Int>> = exerciseRepository.getAllExercisesWithUsageStats()

    suspend fun getExerciseById(id: Long): ExerciseWithDetails? = exerciseRepository.getExerciseById(id)

    suspend fun searchExercises(query: String): List<Exercise> = exerciseRepository.searchExercises(query)

    suspend fun getExercisesByCategory(category: ExerciseCategory): List<Exercise> = exerciseRepository.getExercisesByCategory(category)

    suspend fun getExercisesByMuscleGroup(muscleGroup: String): List<Exercise> = exerciseRepository.getExercisesByMuscleGroup(muscleGroup)

    suspend fun getExercisesByEquipment(equipment: Equipment): List<Exercise> = exerciseRepository.getExercisesByEquipment(equipment)

    // ===== EXISTING WORKOUT METHODS (Updated to work with new Exercise system) =====

    // Basic CRUD operations
    suspend fun insertWorkout(workout: Workout): Long = workoutDao.insertWorkout(workout)

    suspend fun getExercisesForWorkout(workoutId: Long): List<ExerciseLog> = exerciseRepository.getExercisesForWorkout(workoutId)

    suspend fun getSetsForExercise(exerciseLogId: Long): List<SetLog> = exerciseRepository.getSetsForExercise(exerciseLogId)

    suspend fun markSetCompleted(
        setId: Long,
        completed: Boolean,
        completedAt: String?,
    ) = setLogDao.markSetCompleted(setId, completed, completedAt)

    suspend fun insertExerciseLog(exerciseLog: ExerciseLog): Long = exerciseRepository.insertExerciseLog(exerciseLog)

    suspend fun insertSetLog(setLog: SetLog): Long = setLogDao.insertSetLog(setLog)

    suspend fun updateSetLog(setLog: SetLog) = setLogDao.updateSetLog(setLog)

    suspend fun deleteSetLog(setId: Long) = setLogDao.deleteSetLog(setId)

    // Enhanced exercise log creation that links to Exercise entity
    suspend fun insertExerciseLogWithExerciseReference(
        workoutId: Long,
        exercise: ExerciseWithDetails,
        exerciseOrder: Int,
        notes: String? = null,
    ): Long = exerciseRepository.insertExerciseLogWithExerciseReference(workoutId, exercise, exerciseOrder, notes)

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

    suspend fun updateWorkoutName(
        workoutId: Long,
        name: String?,
    ) {
        val workout = workoutDao.getAllWorkouts().find { it.id == workoutId } ?: return
        val isCompleted = workout.notes?.contains("[COMPLETED]") == true

        val newNotes =
            if (isCompleted) {
                name ?: ""
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
                
                // Include completed workouts even if they have no exercises
                if (workout.status != WorkoutStatus.COMPLETED && exercises.isEmpty()) return@mapNotNull null

                val allSets = mutableListOf<SetLog>()
                exercises.forEach { exercise ->
                    allSets.addAll(setLogDao.getSetLogsForExercise(exercise.id))
                }

                val completedSets = allSets.filter { it.isCompleted }
                val totalWeight = completedSets.sumOf { (it.weight * it.reps).toDouble() }.toFloat()

                val isCompleted = workout.status == WorkoutStatus.COMPLETED
                val displayName =
                    if (workout.notes != null && isCompleted) {
                        workout.notes!!
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
                    status = workout.status,
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

        // PRE-FILTER workouts - show completed workouts even without exercises
        val validWorkouts =
            allWorkouts.filter { workout ->
                val exercises = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
                // Show workout if it's completed OR has exercises
                workout.status == WorkoutStatus.COMPLETED || exercises.isNotEmpty()
            }

        println("  ✅ Valid workouts (completed or with exercises): ${validWorkouts.size}")

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

                val isCompleted = workout.status == WorkoutStatus.COMPLETED
                val displayName =
                    if (workout.notes != null && isCompleted) {
                        workout.notes!!
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

                // Calculate PR count for this workout
                val prCount = try {
                    val workoutDate = workout.date.toLocalDate()
                    val startOfDay = workoutDate.atStartOfDay()
                    val endOfDay = workoutDate.atTime(23, 59, 59)
                    
                    // Get PRs for the workout date
                    personalRecordDao.getPRsSince(startOfDay.toString())
                        .filter { pr -> 
                            val prDate = pr.recordDate.toLocalDate()
                            prDate == workoutDate
                        }
                        .size
                } catch (e: Exception) {
                    android.util.Log.w("FeatherweightRepository", "Failed to calculate PR count for workout ${workout.id}", e)
                    0
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
                    status = workout.status,
                    isProgrammeWorkout = workout.isProgrammeWorkout,
                    programmeId = workout.programmeId,
                    programmeName = programmeName,
                    programmeWorkoutName = workout.programmeWorkoutName,
                    weekNumber = workout.weekNumber,
                    dayNumber = workout.dayNumber,
                    prCount = prCount,
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
        val allWorkouts = workoutDao.getAllWorkouts()
            .filter { it.status == WorkoutStatus.COMPLETED } // Only look at completed workouts
            .sortedByDescending { it.date } // Most recent first

        for (workout in allWorkouts) {
            if (workout.id == currentWorkoutId) continue

            val exercises = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
            val matchingExercise = exercises.find { it.exerciseName == exerciseName }

            if (matchingExercise != null) {
                val sets = setLogDao.getSetLogsForExercise(matchingExercise.id)
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

    suspend fun getExerciseStats(exerciseName: String): ExerciseStats? = exerciseRepository.getExerciseStats(exerciseName)

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
                    reasoning = "Based on your last workout performance"
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
                reasoning = "Historical average from your past ${stats.totalSets} sets"
            )
        }

        return null
    }

    
    /**
     * Enhanced smart suggestions using global progress tracking and RPE analysis
     */
    suspend fun getSmartSuggestionsEnhanced(
        exerciseName: String,
        targetReps: Int? = null
    ): SmartSuggestions {
        val userId = getCurrentUserId()
        
        // Use the freestyle intelligence service for advanced suggestions
        return freestyleIntelligenceService.getIntelligentSuggestions(
            exerciseName = exerciseName,
            userId = userId,
            targetReps = targetReps
        )
    }
    
    /**
     * Get real-time suggestions as user types
     */
    fun getSmartSuggestionsFlow(
        exerciseName: String,
        repsFlow: kotlinx.coroutines.flow.Flow<Int>
    ): kotlinx.coroutines.flow.Flow<SmartSuggestions> {
        val userId = getCurrentUserId()
        return freestyleIntelligenceService.getSuggestionsForReps(
            exerciseName = exerciseName,
            userId = userId,
            repsFlow = repsFlow
        )
    }

    suspend fun getWorkoutById(workoutId: Long): Workout? = workoutDao.getWorkoutById(workoutId)

    // Delete an exercise log (will cascade delete all its sets due to foreign key)
    suspend fun deleteExerciseLog(exerciseLogId: Long) = exerciseRepository.deleteExerciseLog(exerciseLogId)

    // Update exercise order
    suspend fun updateExerciseOrder(
        exerciseLogId: Long,
        newOrder: Int,
    ) = exerciseRepository.updateExerciseOrder(exerciseLogId, newOrder)

    // Delete an entire workout (will cascade delete all exercises and sets)
    suspend fun deleteWorkout(workoutId: Long) = workoutDao.deleteWorkout(workoutId)

    // Update an exercise log
    suspend fun updateExerciseLog(exerciseLog: ExerciseLog) = exerciseRepository.updateExerciseLog(exerciseLog)

    // Delete all sets for a specific exercise
    suspend fun deleteSetsForExercise(exerciseLogId: Long) = exerciseRepository.deleteSetsForExercise(exerciseLogId)

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
    suspend fun getSwapHistoryForExercise(exerciseId: Long): List<SwapHistoryCount> {
        val userId = getCurrentUserId()
        return db.exerciseSwapHistoryDao().getSwapHistoryForExercise(userId, exerciseId)
    }

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
    suspend fun getPersonalRecords(exerciseName: String): List<Pair<Float, LocalDateTime>> = exerciseRepository.getPersonalRecords(exerciseName)

    suspend fun getEstimated1RM(exerciseName: String): Float? = exerciseRepository.getEstimated1RM(exerciseName)

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
        val programme = programmeDao.getProgrammeById(programmeId)
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
        weekNumber: Int? = null
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
                            println("🌊 Wave progression: Week $weekNumber (cycle week ${cycleWeek + 1}), Set ${setIndex + 1} -> ${intensity}%")
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
                    programme = programme
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
            lastExecution?.status == WorkoutStatus.COMPLETED
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
            println("🔍 Looking for template with name: '$templateNameToSearch' (programme.name='${programme.name}', programme.templateName='${programme.templateName}')")
            
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

    // Helper to get daily workout count for custom programmes
    private suspend fun getDailyWorkoutCount(programmeId: Long): Int {
        val weeks = programmeDao.getWeeksForProgramme(programmeId)
        if (weeks.isEmpty()) return 3 // fallback

        // Get the first week's workout count as representative
        val firstWeekWorkouts = programmeDao.getWorkoutsForWeek(weeks[0].id)
        return firstWeekWorkouts.size.coerceAtLeast(1)
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

    suspend fun getTemplatesByDifficulty(difficulty: ProgrammeDifficulty) =
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
                val errorMessage = validationErrors.joinToString("\n") { error ->
                    "- ${error.field}: ${error.value} - ${error.error}" +
                    (error.suggestion?.let { " (Try: $it)" } ?: "")
                }
                throw IllegalArgumentException(
                    "Cannot create programme from template '${template.name}':\n$errorMessage"
                )
            }

            val programme =
                Programme(
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
                    
                    val week = ProgrammeWeek(
                        programmeId = programmeId,
                        weekNumber = weekNum,
                        name = "Week $weekNum",
                        description = templateWeek.name,
                        focusAreas = null,
                        intensityLevel = null,
                        volumeLevel = null,
                        isDeload = false,
                        phase = null
                    )
                    
                    val weekId = programmeDao.insertProgrammeWeek(week)
                    
                    // Create workouts for this week
                    templateWeek.workouts.forEach { workoutStructure ->
                        val workout = ProgrammeWorkout(
                            weekId = weekId,
                            dayNumber = workoutStructure.day,
                            name = workoutStructure.name,
                            description = null,
                            estimatedDuration = workoutStructure.estimatedDuration,
                            workoutStructure = kotlinx.serialization.json.Json.encodeToString(
                                WorkoutStructure.serializer(),
                                workoutStructure
                            )
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
                    startedAt = java.time.LocalDateTime.now(),
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
                println("✅ Programme marked as active")

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

    suspend fun deactivateActiveProgramme() =
        withContext(Dispatchers.IO) {
            println("⚠️ deactivateActiveProgramme: Deactivating all programmes")
            val activeProgramme = programmeDao.getActiveProgramme()
            if (activeProgramme != null) {
                println("   Active programme was: ${activeProgramme.name}")
            }
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

                // Only mark this specific programme as complete, don't deactivate all programmes
                programmeDao.completeProgramme(programmeId, LocalDateTime.now().toString())

                // Only deactivate this specific programme, not all programmes
                val updatedProgramme = programme.copy(isActive = false)
                programmeDao.updateProgramme(updatedProgramme)
                println("✅ Programme marked as complete and deactivated (other programmes remain active)")
            } else {
                println("❌ Programme or progress not found for completion")
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

    suspend fun getInProgressWorkoutCountByProgramme(programmeId: Long): Int =
        withContext(Dispatchers.IO) {
            workoutDao.getInProgressWorkoutCountByProgramme(programmeId)
        }

    suspend fun updateWorkoutStatus(workoutId: Long, status: WorkoutStatus) =
        withContext(Dispatchers.IO) {
            val workout = workoutDao.getWorkoutById(workoutId) ?: return@withContext
            val updatedWorkout = workout.copy(status = status)
            workoutDao.updateWorkout(updatedWorkout)
            println("📊 Workout $workoutId status updated to $status")
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
                    ProgrammeProgress(
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
            ExerciseSubstitution(
                programmeId = programmeId,
                originalExerciseName = originalExercise,
                substitutionCategory = exercise?.category ?: ExerciseCategory.FULL_BODY,
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

    // Helper method to get exercise by name or alias
    suspend fun getExerciseByName(name: String): Exercise? = exerciseRepository.getExerciseByName(name)

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

    suspend fun deleteExerciseMax(max: UserExerciseMax) =
        withContext(Dispatchers.IO) {
            db.profileDao().deleteExerciseMax(max)
        }

    suspend fun deleteAllMaxesForExercise(exerciseId: Long) =
        withContext(Dispatchers.IO) {
            db.profileDao().deleteAllMaxesForExercise(getCurrentUserId(), exerciseId)
        }

    // Calculate percentage of 1RM for a given weight
    fun calculatePercentageOf1RM(
        weight: Float,
        oneRepMax: Float,
    ): Int =
        if (oneRepMax > 0) {
            ((weight / oneRepMax) * 100).toInt()
        } else {
            0
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
                        db.profileDao().insertExerciseMax(
                            UserExerciseMax(
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
                            UserExerciseMax(
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

    private suspend fun calculateIntelligentWeight(
        exerciseStructure: ExerciseStructure,
        reps: Int,
        userMaxes: Map<String, Float>,
        intensity: Int? = null,
        programme: Programme? = null
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
                    println("Calculated Weight (${intensity}% of TM): $calculatedWeight kg")
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
                val progressionService = ProgressionService(
                    performanceTrackingDao = db.exercisePerformanceTrackingDao(),
                    exerciseRepository = exerciseRepository,
                    programmeDao = programmeDao
                )
                
                // Calculate the progression decision
                val decision = progressionService.calculateProgressionWeight(
                    exerciseName = exerciseStructure.name,
                    programme = programme,
                    currentWorkoutId = 0L // This is during workout creation, so we don't have an ID yet
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

    private suspend fun getLastPerformanceForExercise(exerciseName: String): ExercisePerformanceData? = exerciseRepository.getLastPerformanceForExercise(exerciseName)
    
    private suspend fun recordWorkoutPerformanceData(workoutId: Long, programmeId: Long) = withContext(Dispatchers.IO) {
        println("📊 Recording performance data for workout $workoutId in programme $programmeId")
        
        val progressionService = ProgressionService(
            performanceTrackingDao = db.exercisePerformanceTrackingDao(),
            exerciseRepository = exerciseRepository,
            programmeDao = programmeDao
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
                    sets = sets
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
    
    suspend fun getOneRMForExercise(exerciseName: String): Float? = exerciseRepository.getOneRMForExercise(exerciseName)
    
    suspend fun getHistoricalPerformance(
        exerciseName: String, 
        minReps: Int, 
        maxReps: Int
    ): List<com.github.radupana.featherweight.service.PerformanceData> = withContext(Dispatchers.IO) {
        // Get exercise logs for this exercise
        val exerciseLogs = db.workoutDao().getExerciseLogsByName(exerciseName)
        val exerciseIds = exerciseLogs.map { it.id }
        
        if (exerciseIds.isEmpty()) return@withContext emptyList()
        
        // Get sets within rep range
        val sets = db.workoutDao().getSetsForExercisesInRepRange(exerciseIds, minReps, maxReps)
        
        sets.map { setLog ->
            com.github.radupana.featherweight.service.PerformanceData(
                targetReps = setLog.targetReps,
                targetWeight = setLog.targetWeight,
                actualReps = setLog.actualReps,
                actualWeight = setLog.actualWeight,
                actualRpe = setLog.actualRpe,
                timestamp = setLog.completedAt ?: ""
            )
        }.sortedByDescending { it.timestamp }
    }
    
    suspend fun getRecentPerformance(
        exerciseName: String, 
        limit: Int = 10
    ): List<com.github.radupana.featherweight.service.PerformanceData> = withContext(Dispatchers.IO) {
        // Get recent exercise logs for this exercise
        val exerciseLogs = db.workoutDao().getExerciseLogsByName(exerciseName)
        val exerciseIds = exerciseLogs.map { it.id }
        
        if (exerciseIds.isEmpty()) return@withContext emptyList()
        
        // Get recent sets
        val sets = db.workoutDao().getRecentSetsForExercises(exerciseIds, limit)
        
        sets.map { setLog ->
            com.github.radupana.featherweight.service.PerformanceData(
                targetReps = setLog.targetReps,
                targetWeight = setLog.targetWeight,
                actualReps = setLog.actualReps,
                actualWeight = setLog.actualWeight,
                actualRpe = setLog.actualRpe,
                timestamp = setLog.completedAt ?: ""
            )
        }.sortedByDescending { it.timestamp }
    }
    
    suspend fun getHistoricalPerformanceForWeight(
        exerciseName: String, 
        minWeight: Float, 
        maxWeight: Float
    ): List<com.github.radupana.featherweight.service.PerformanceData> = withContext(Dispatchers.IO) {
        // Get exercise logs for this exercise
        val exerciseLogs = db.workoutDao().getExerciseLogsByName(exerciseName)
        val exerciseIds = exerciseLogs.map { it.id }
        
        if (exerciseIds.isEmpty()) return@withContext emptyList()
        
        // Get sets within weight range
        val sets = db.workoutDao().getSetsForExercisesInWeightRange(exerciseIds, minWeight, maxWeight)
        
        sets.map { setLog ->
            com.github.radupana.featherweight.service.PerformanceData(
                targetReps = setLog.targetReps,
                targetWeight = setLog.targetWeight,
                actualReps = setLog.actualReps,
                actualWeight = setLog.actualWeight,
                actualRpe = setLog.actualRpe,
                timestamp = setLog.completedAt ?: ""
            )
        }.sortedByDescending { it.timestamp }
    }
    
    // Global Exercise Progress Methods
    suspend fun getGlobalExerciseProgress(exerciseName: String): GlobalExerciseProgress? = withContext(Dispatchers.IO) {
        globalExerciseProgressDao.getProgressForExercise(getCurrentUserId(), exerciseName)
    }
    
    fun observeGlobalExerciseProgress(exerciseName: String) = 
        globalExerciseProgressDao.observeProgressForExercise(getCurrentUserId(), exerciseName)
    
    fun observeAllGlobalProgress() = 
        globalExerciseProgressDao.getAllProgressForUser(getCurrentUserId())
    
    suspend fun getStalledExercises(minStalls: Int = 3) = withContext(Dispatchers.IO) {
        globalExerciseProgressDao.getStalledExercises(getCurrentUserId(), minStalls)
    }
    
    suspend fun getExercisesByTrend(trend: ProgressTrend) = withContext(Dispatchers.IO) {
        globalExerciseProgressDao.getExercisesByTrend(getCurrentUserId(), trend)
    }
    
    suspend fun analyzeExerciseProgress(exerciseName: String) = withContext(Dispatchers.IO) {
        globalProgressTracker.analyzeExerciseProgress(getCurrentUserId(), exerciseName)
    }
    
    // Exercise Correlation Methods
    suspend fun getExerciseCorrelations(exerciseName: String) = withContext(Dispatchers.IO) {
        exerciseCorrelationDao.getCorrelationsForExercise(exerciseName)
    }
    
    suspend fun getExercisesByMovementPattern(pattern: MovementPattern) = withContext(Dispatchers.IO) {
        exerciseCorrelationDao.getExercisesByMovementPattern(pattern)
    }
    
    suspend fun getExercisesByMuscleGroup(muscleGroup: MuscleGroup) = withContext(Dispatchers.IO) {
        exerciseCorrelationDao.getExercisesByMuscleGroup(muscleGroup)
    }
    
    // Progress Analytics Methods
    suspend fun getSetsForExercise(exerciseName: String, days: Int): List<SetLog> = withContext(Dispatchers.IO) {
        val cutoffDate = LocalDateTime.now().minusDays(days.toLong())
        setLogDao.getSetsForExerciseSince(exerciseName, cutoffDate.toString())
    }
    
    suspend fun getSetsForExerciseSince(exerciseName: String, since: LocalDateTime): List<SetLog> = withContext(Dispatchers.IO) {
        setLogDao.getSetsForExerciseSince(exerciseName, since.toString())
    }
    
    suspend fun getAllCompletedExercises(): List<String> = withContext(Dispatchers.IO) {
        setLogDao.getAllCompletedExerciseNames()
    }
    
    suspend fun getPreviousMaxWeight(exerciseName: String, days: Int): Float? = withContext(Dispatchers.IO) {
        val cutoffDate = LocalDateTime.now().minusDays(days.toLong())
        setLogDao.getMaxWeightForExerciseBefore(exerciseName, cutoffDate.toString())
    }
    
    /**
     * Check if a completed set represents a personal record
     * Returns list of PersonalRecord objects if PRs are detected
     */
    suspend fun checkForPR(setLog: SetLog, exerciseName: String): List<PersonalRecord> = withContext(Dispatchers.IO) {
        prDetectionService.checkForPR(setLog, exerciseName)
    }
    
    /**
     * Get recent personal records for an exercise
     */
    suspend fun getRecentPRsForExercise(exerciseName: String, limit: Int = 5): List<PersonalRecord> = withContext(Dispatchers.IO) {
        prDetectionService.getRecentPRsForExercise(exerciseName, limit)
    }
    
    /**
     * Get recent personal records across all exercises
     */
    suspend fun getRecentPRs(limit: Int = 10): List<PersonalRecord> = withContext(Dispatchers.IO) {
        prDetectionService.getRecentPRs(limit)
    }
    
    /**
     * Clear all personal records (for debugging corrupted data)
     */
    suspend fun clearAllPersonalRecords() = withContext(Dispatchers.IO) {
        println("🗑️ CLEARING ALL PERSONAL RECORDS")
        personalRecordDao.clearAllPersonalRecords()
    }
    
    // ===== ACHIEVEMENT METHODS =====
    
    /**
     * Check for newly unlocked achievements after a workout completion
     */
    suspend fun checkForNewAchievements(userId: Long, workoutId: Long): List<com.github.radupana.featherweight.data.achievement.UserAchievement> = withContext(Dispatchers.IO) {
        achievementDetectionService.checkForNewAchievements(userId, workoutId)
    }
    
    /**
     * Get achievement summary for a user
     */
    suspend fun getAchievementSummary(userId: Long): com.github.radupana.featherweight.service.AchievementSummary = withContext(Dispatchers.IO) {
        achievementDetectionService.getAchievementSummary(userId)
    }
    
    /**
     * Get user's unlocked achievements
     */
    suspend fun getUserAchievements(userId: Long): List<com.github.radupana.featherweight.data.achievement.UserAchievement> = withContext(Dispatchers.IO) {
        userAchievementDao.getRecentUserAchievements(userId, 100) // Get all achievements
    }
}
