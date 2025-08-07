package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.data.WorkoutStatus
import com.github.radupana.featherweight.repository.FeatherweightRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlin.random.Random

class WorkoutSeedingService(
    private val repository: FeatherweightRepository,
) {
    companion object {
        // Weight thresholds and improvements
        private const val MIN_MEANINGFUL_WEIGHT = 40f // Minimum weight for 1RM calculations (kg)
        private const val MIN_IMPROVEMENT_THRESHOLD = 0.01f // 1% improvement required for 1RM update
        private const val RPE_TOLERANCE = 0.15f // 15% tolerance for RPE-based weight validation

        // Standard percentage of 1RM for different rep ranges (at RPE 10)
        private const val ONE_REP_PERCENTAGE = 1.0f
        private const val THREE_REP_PERCENTAGE = 0.90f
        private const val FIVE_REP_PERCENTAGE = 0.85f
        private const val EIGHT_REP_PERCENTAGE = 0.75f
    }

    data class SeedConfig(
        val numberOfWeeks: Int,
        val workoutsPerWeek: Int = 4,
        val includeAccessories: Boolean = true,
        val progressionRate: Float = 0.025f,
        val variationRange: Float = 2.5f,
    )

    data class WorkoutPlan(
        val mainLifts: List<PlannedExercise>,
        val accessories: List<PlannedExercise>,
    )

    data class PlannedExercise(
        val name: String,
        val sets: List<PlannedSet>,
    )

    data class PlannedSet(
        val reps: Int,
        val rpe: Int,
        val weightMultiplier: Float = 1.0f,
    )

    // Default 1RMs if not set in profile
    private val default1RMs =
        mapOf(
            "Barbell Back Squat" to 130f,
            "Barbell Bench Press" to 105f,
            "Barbell Deadlift" to 180f,
            "Barbell Overhead Press" to 70f,
        )

    suspend fun seedRealisticWorkouts(config: SeedConfig): Int =
        withContext(Dispatchers.IO) {
            val userId = repository.getCurrentUserId()
            if (userId == -1L) {
                throw IllegalStateException("No user selected")
            }

            // Get 1RMs from profile or use defaults
            val oneRMs = get1RMs(userId)

            // Generate workout dates, skipping existing workouts
            val workoutDates = generateAvailableWorkoutDates(config)

            var workoutsCreated = 0

            workoutDates.forEachIndexed { index, date ->
                val weekNumber = (index / config.workoutsPerWeek) + 1
                val dayInWeek = (index % config.workoutsPerWeek) + 1
                val cycleWeek = ((weekNumber - 1) % 4) + 1 // 4-week cycle

                // Determine if this is a 5-day week (every 4th week)
                val is5DayWeek = cycleWeek == 4 && dayInWeek == 5

                val workoutPlan = generateWorkoutPlan(weekNumber, cycleWeek, dayInWeek, is5DayWeek)
                val progressionMultiplier = calculateProgressionMultiplier(weekNumber)
                val isDeloadWeek = weekNumber % 4 == 0

                val workoutId =
                    createWorkout(
                        date = date,
                        weekNumber = weekNumber,
                        dayInWeek = dayInWeek,
                        workoutPlan = workoutPlan,
                        oneRMs = oneRMs,
                        progressionMultiplier = progressionMultiplier,
                        isDeloadWeek = isDeloadWeek,
                        config = config,
                    )

                // Process workout completion to trigger all side effects
                processWorkoutCompletion(workoutId, userId)

                workoutsCreated++
            }

            workoutsCreated
        }

    private suspend fun get1RMs(userId: Long): Map<String, Float> {
        val profile1RMs = mutableMapOf<String, Float>()

        // Get 1RMs from profile if available
        for (exerciseName in default1RMs.keys) {
            val profileRM = repository.getExercise1RM(exerciseName, userId)
            if (profileRM != null && profileRM > 0) {
                profile1RMs[exerciseName] = profileRM
            } else {
                profile1RMs[exerciseName] = default1RMs[exerciseName] ?: 100f
            }
        }

        return profile1RMs
    }

    private suspend fun generateAvailableWorkoutDates(config: SeedConfig): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        val endDate = LocalDate.now().minusDays(1) // Start from yesterday
        var currentDate = endDate
        var weeksGenerated = 0
        var daysInCurrentWeek = 0

        while (weeksGenerated < config.numberOfWeeks && currentDate.isAfter(endDate.minusYears(1))) {
            // Check if this date already has a workout
            val existingWorkout = repository.getWorkoutForDate(currentDate)

            if (existingWorkout == null) {
                // Determine which day of the training week this should be
                val dayOfWeek = currentDate.dayOfWeek.value // 1=Monday, 7=Sunday
                val cycleWeek = (weeksGenerated % 4) + 1 // 4-week cycle
                val workoutsThisWeek = if (cycleWeek == 4) 5 else 4

                // Training days based on the pattern
                val trainingDays =
                    when (workoutsThisWeek) {
                        5 -> listOf(1, 2, 4, 5, 6) // Mon, Tue, Thu, Fri, Sat
                        else -> listOf(1, 2, 4, 5) // Mon, Tue, Thu, Fri
                    }

                if (dayOfWeek in trainingDays && daysInCurrentWeek < workoutsThisWeek) {
                    dates.add(0, currentDate) // Add to beginning to maintain chronological order
                    daysInCurrentWeek++

                    if (daysInCurrentWeek >= workoutsThisWeek) {
                        weeksGenerated++
                        daysInCurrentWeek = 0
                    }
                }
            }

            currentDate = currentDate.minusDays(1)

            // Reset week counter on Sunday
            if (currentDate.dayOfWeek.value == 7 && daysInCurrentWeek > 0) {
                weeksGenerated++
                daysInCurrentWeek = 0
            }
        }

        return dates
    }

    private fun isTestWeek(weekNumber: Int): Boolean = weekNumber % 8 == 0

    private fun isOpenerWeek(weekNumber: Int): Boolean {
        return (weekNumber + 1) % 8 == 0 // Week before test week
    }

    private fun isRecoveryWeek(weekNumber: Int): Boolean {
        return (weekNumber - 1) % 8 == 0 // Week after test week
    }

    private fun generateWorkoutPlan(
        weekNumber: Int,
        cycleWeek: Int,
        dayInWeek: Int,
        is5DayWeek: Boolean,
    ): WorkoutPlan =
        when {
            // Monday: Squat (Heavy) + Back accessories
            dayInWeek == 1 -> generateSquatWorkout(weekNumber)

            // Tuesday: Bench (Heavy) + Tricep/Chest accessories
            dayInWeek == 2 -> generateBenchWorkout(weekNumber)

            // Thursday: Deadlift + Squat (Light) + Back accessories
            dayInWeek == 3 -> generateDeadliftWorkout(weekNumber)

            // Friday: Bench (Medium) + OHP + Arm accessories
            dayInWeek == 4 ->
                WorkoutPlan(
                    mainLifts =
                        listOf(
                            PlannedExercise(
                                "Barbell Bench Press",
                                listOf(
                                    PlannedSet(8, 7),
                                    PlannedSet(8, 7),
                                    PlannedSet(8, 7),
                                ),
                            ),
                            PlannedExercise(
                                "Barbell Overhead Press",
                                listOf(
                                    PlannedSet(8, 7),
                                    PlannedSet(8, 7),
                                    PlannedSet(8, 7),
                                ),
                            ),
                        ),
                    accessories =
                        listOf(
                            PlannedExercise("Barbell Bicep Curl", listOf(PlannedSet(10, 7), PlannedSet(10, 7), PlannedSet(10, 7))),
                            PlannedExercise("Dumbbell Bicep Curl", listOf(PlannedSet(12, 7), PlannedSet(12, 7), PlannedSet(12, 7))),
                            PlannedExercise("Cable Tricep Pushdown", listOf(PlannedSet(12, 7), PlannedSet(12, 7), PlannedSet(12, 7))),
                        ),
                )

            // Saturday (Week 4 only): Bench (Light) + Full accessories
            is5DayWeek ->
                WorkoutPlan(
                    mainLifts =
                        listOf(
                            PlannedExercise(
                                "Barbell Bench Press",
                                listOf(
                                    PlannedSet(8, 6),
                                    PlannedSet(8, 6),
                                    PlannedSet(8, 6),
                                ),
                            ),
                        ),
                    accessories =
                        listOf(
                            PlannedExercise("Dumbbell Bench Press", listOf(PlannedSet(10, 6), PlannedSet(10, 6), PlannedSet(10, 6))),
                            PlannedExercise("Dumbbell Incline Bench Press", listOf(PlannedSet(10, 6), PlannedSet(10, 6), PlannedSet(10, 6))),
                            PlannedExercise("Dumbbell Fly", listOf(PlannedSet(12, 6), PlannedSet(12, 6))),
                            PlannedExercise("Barbell Bicep Curl", listOf(PlannedSet(12, 6), PlannedSet(12, 6), PlannedSet(12, 6))),
                        ),
                )

            else -> WorkoutPlan(mainLifts = emptyList(), accessories = emptyList())
        }

    private fun generateSquatWorkout(weekNumber: Int): WorkoutPlan {
        val isTest = isTestWeek(weekNumber)
        val isOpener = isOpenerWeek(weekNumber)
        val isRecovery = isRecoveryWeek(weekNumber)

        val mainSets =
            when {
                isTest ->
                    listOf(
                        PlannedSet(1, 10), // Max attempt at 105% of estimated 1RM
                        PlannedSet(3, 7), // Back-off set
                        PlannedSet(3, 7), // Back-off set
                    )
                isOpener ->
                    listOf(
                        PlannedSet(1, 6), // Opener
                        PlannedSet(3, 8),
                        PlannedSet(3, 8),
                        PlannedSet(3, 8),
                    )
                isRecovery ->
                    listOf(
                        // No singles on recovery week
                        PlannedSet(3, 7),
                        PlannedSet(3, 7),
                        PlannedSet(3, 7),
                    )
                else ->
                    listOf(
                        PlannedSet(1, Random.nextInt(7, 9)), // Regular weeks: RPE 7-8
                        PlannedSet(3, 8),
                        PlannedSet(3, 8),
                        PlannedSet(3, 8),
                    )
            }

        val accessories =
            if (isTest) {
                // Reduced accessory volume on test weeks
                listOf(
                    PlannedExercise("Barbell Row", listOf(PlannedSet(8, 7), PlannedSet(8, 7))),
                )
            } else {
                listOf(
                    PlannedExercise("Barbell Row", listOf(PlannedSet(8, 7), PlannedSet(8, 7), PlannedSet(8, 7))),
                    PlannedExercise("Pull Up", listOf(PlannedSet(8, 7), PlannedSet(8, 7), PlannedSet(8, 7))),
                )
            }

        return WorkoutPlan(
            mainLifts = listOf(PlannedExercise("Barbell Back Squat", mainSets)),
            accessories = accessories,
        )
    }

    private fun generateBenchWorkout(weekNumber: Int): WorkoutPlan {
        val isTest = isTestWeek(weekNumber)
        val isOpener = isOpenerWeek(weekNumber)
        val isRecovery = isRecoveryWeek(weekNumber)

        val mainSets =
            when {
                isTest ->
                    listOf(
                        PlannedSet(1, 10), // Max attempt
                        PlannedSet(3, 7), // Back-off set
                        PlannedSet(3, 7), // Back-off set
                    )
                isOpener ->
                    listOf(
                        PlannedSet(1, 6), // Opener
                        PlannedSet(3, 8),
                        PlannedSet(3, 8),
                        PlannedSet(3, 8),
                    )
                isRecovery ->
                    listOf(
                        // No singles on recovery week
                        PlannedSet(3, 7),
                        PlannedSet(3, 7),
                        PlannedSet(3, 7),
                    )
                else ->
                    listOf(
                        PlannedSet(1, Random.nextInt(7, 9)), // Regular weeks: RPE 7-8
                        PlannedSet(3, 8),
                        PlannedSet(3, 8),
                        PlannedSet(3, 8),
                    )
            }

        val accessories =
            if (isTest) {
                // Reduced accessory volume on test weeks
                listOf(
                    PlannedExercise("Dumbbell Incline Bench Press", listOf(PlannedSet(8, 7), PlannedSet(8, 7))),
                )
            } else {
                listOf(
                    PlannedExercise("Dumbbell Incline Bench Press", listOf(PlannedSet(8, 7), PlannedSet(8, 7), PlannedSet(8, 7))),
                    PlannedExercise("Cable Tricep Pushdown", listOf(PlannedSet(12, 7), PlannedSet(12, 7), PlannedSet(12, 7))),
                    PlannedExercise("Dumbbell Fly", listOf(PlannedSet(12, 6), PlannedSet(12, 6), PlannedSet(12, 6))),
                )
            }

        return WorkoutPlan(
            mainLifts = listOf(PlannedExercise("Barbell Bench Press", mainSets)),
            accessories = accessories,
        )
    }

    private fun generateDeadliftWorkout(weekNumber: Int): WorkoutPlan {
        val isTest = isTestWeek(weekNumber)
        val isOpener = isOpenerWeek(weekNumber)
        val isRecovery = isRecoveryWeek(weekNumber)

        val mainSets =
            when {
                isTest ->
                    listOf(
                        PlannedSet(1, 10), // Max attempt
                        PlannedSet(3, 7), // Back-off set
                        PlannedSet(3, 7), // Back-off set
                    )
                isOpener ->
                    listOf(
                        PlannedSet(1, 6), // Opener
                        PlannedSet(3, 7),
                        PlannedSet(3, 7),
                        PlannedSet(3, 7),
                    )
                isRecovery ->
                    listOf(
                        // No singles on recovery week
                        PlannedSet(3, 6),
                        PlannedSet(3, 6),
                        PlannedSet(3, 6),
                    )
                else ->
                    listOf(
                        PlannedSet(1, Random.nextInt(7, 9)), // Regular weeks: RPE 7-8
                        PlannedSet(3, 7),
                        PlannedSet(3, 7),
                        PlannedSet(3, 7),
                    )
            }

        val squatSets =
            if (isTest) {
                // Lighter squat on test day
                listOf(PlannedSet(8, 6), PlannedSet(8, 6))
            } else {
                listOf(PlannedSet(8, 7), PlannedSet(8, 7), PlannedSet(8, 7))
            }

        val accessories =
            if (isTest) {
                // Reduced accessory volume on test weeks
                listOf(
                    PlannedExercise("Barbell Row", listOf(PlannedSet(8, 6), PlannedSet(8, 6))),
                )
            } else {
                listOf(
                    PlannedExercise("Barbell Row", listOf(PlannedSet(8, 7), PlannedSet(8, 7), PlannedSet(8, 7))),
                    PlannedExercise("Pull Up", listOf(PlannedSet(8, 6), PlannedSet(8, 6), PlannedSet(8, 6))),
                )
            }

        return WorkoutPlan(
            mainLifts =
                listOf(
                    PlannedExercise("Barbell Deadlift", mainSets),
                    PlannedExercise("Barbell Back Squat", squatSets),
                ),
            accessories = accessories,
        )
    }

    private fun calculateProgressionMultiplier(weekNumber: Int): Float =
        when {
            weekNumber <= 4 -> 0.95f // 95% of 1RM baseline
            weekNumber <= 8 -> 0.98f // +3%
            weekNumber <= 12 -> 1.01f // +6%
            weekNumber <= 16 -> 1.03f // +8%
            else -> 1.03f + ((weekNumber - 16) * 0.0125f) // Continue scaling
        }

    private fun calculateWorkingWeight(
        oneRM: Float,
        reps: Int,
        rpe: Int,
    ): Float {
        // More realistic weight calculations based on RPE and rep ranges
        // Using Epley formula inverse: weight = 1RM / (1 + reps/30) adjusted for RPE

        // Base percentage based on reps (assuming RPE 10)
        val basePercentage =
            when (reps) {
                1 -> 1.0f
                2 -> 0.95f
                3 -> 0.90f
                4 -> 0.87f
                5 -> 0.85f
                6 -> 0.82f
                7 -> 0.80f
                8 -> 0.77f
                9 -> 0.75f
                10 -> 0.73f
                11 -> 0.71f
                12 -> 0.69f
                else -> 0.65f
            }

        // Adjust for RPE (reps in reserve)
        val rpeAdjustment =
            when (rpe) {
                6 -> 0.85f // 4 reps in reserve
                7 -> 0.90f // 3 reps in reserve
                8 -> 0.94f // 2 reps in reserve
                9 -> 0.97f // 1 rep in reserve
                10 -> 1.0f // 0 reps in reserve (max effort)
                else -> 0.90f
            }

        return oneRM * basePercentage * rpeAdjustment
    }

    private fun applyRandomVariation(
        weight: Float,
        range: Float,
    ): Float {
        val variation = Random.nextFloat() * range * 2 - range // -range to +range
        return (weight + variation).coerceAtLeast(20f) // Minimum 20kg (empty barbell)
    }

    private suspend fun createWorkout(
        date: LocalDate,
        weekNumber: Int,
        dayInWeek: Int,
        workoutPlan: WorkoutPlan,
        oneRMs: Map<String, Float>,
        progressionMultiplier: Float,
        isDeloadWeek: Boolean,
        config: SeedConfig,
    ): Long {
        // Create workout as IN_PROGRESS for proper analytics processing
        val workout =
            Workout(
                date = date.atTime(18, 0), // 6 PM
                notes = "Seeded workout - Week $weekNumber Day $dayInWeek",
                status = WorkoutStatus.IN_PROGRESS,
                weekNumber = weekNumber,
                dayNumber = dayInWeek,
                isProgrammeWorkout = false,
                durationSeconds = null,
            )

        val workoutId = repository.insertWorkout(workout)

        // Add exercises and sets
        val allExercises = workoutPlan.mainLifts + if (config.includeAccessories) workoutPlan.accessories else emptyList()

        allExercises.forEach { plannedExercise ->
            val exercise = repository.getExerciseByName(plannedExercise.name)
            if (exercise != null) {
                val exerciseLog =
                    ExerciseLog(
                        workoutId = workoutId,
                        exerciseId = exercise.id,
                        exerciseName = plannedExercise.name,
                        exerciseOrder = allExercises.indexOf(plannedExercise) + 1,
                        notes = null,
                    )

                val exerciseLogId = repository.insertExerciseLog(exerciseLog)

                // Get base 1RM for this exercise or use a default
                val exerciseRM =
                    oneRMs[plannedExercise.name] ?: when {
                        plannedExercise.name.contains("Row") -> 80f
                        plannedExercise.name.contains("Pull Up") -> 0f // Bodyweight
                        plannedExercise.name.contains("Dumbbell") -> 30f // Per dumbbell
                        plannedExercise.name.contains("Cable") -> 40f
                        else -> 60f
                    }

                // Create sets with realistic variations
                var setStartTime = date.atTime(18, 0)
                plannedExercise.sets.forEachIndexed { setIndex, plannedSet ->
                    val baseWeight = calculateWorkingWeight(exerciseRM, plannedSet.reps, plannedSet.rpe)
                    var adjustedWeight = baseWeight * progressionMultiplier

                    // Special handling for test week max attempts (RPE 10)
                    if (plannedSet.rpe == 10 && plannedSet.reps == 1 && isTestWeek(weekNumber)) {
                        // For true 1RM attempts, use 100-102% of current estimated 1RM
                        adjustedWeight = exerciseRM * (1.0f + Random.nextFloat() * 0.02f)
                    }

                    // Apply deload if needed
                    if (isDeloadWeek) {
                        adjustedWeight *= 0.85f // 15% reduction for deload
                    }

                    // Apply smaller random variation for more realistic workouts
                    val smallerVariation = config.variationRange * 0.5f // Half the variation
                    val finalWeight = applyRandomVariation(adjustedWeight, smallerVariation)

                    // Round to nearest 2.5kg
                    val roundedWeight = (finalWeight / 2.5f).roundToInt() * 2.5f

                    // Simulate slight rep variation (±1 rep occasionally)
                    val actualReps =
                        when {
                            Random.nextFloat() < 0.8f -> plannedSet.reps // 80% hit target
                            Random.nextFloat() < 0.5f -> plannedSet.reps + 1 // 10% do extra rep
                            else -> (plannedSet.reps - 1).coerceAtLeast(1) // 10% miss by 1 rep
                        }

                    // Add rest time between sets (2-3 minutes)
                    setStartTime = setStartTime.plusMinutes(Random.nextLong(2, 4))

                    val setLog =
                        SetLog(
                            exerciseLogId = exerciseLogId,
                            setOrder = setIndex + 1,
                            targetReps = plannedSet.reps,
                            targetWeight = roundedWeight,
                            actualReps = actualReps,
                            actualWeight = roundedWeight,
                            actualRpe = plannedSet.rpe.toFloat(),
                            notes = null,
                            isCompleted = true,
                            completedAt = setStartTime.toString(),
                        )

                    repository.insertSetLog(setLog)
                }
            }
        }

        return workoutId
    }

    private suspend fun processWorkoutCompletion(
        workoutId: Long,
        userId: Long,
    ) {
        // Use OneRMService for proper validation
        val oneRMService = OneRMService()

        // Process each exercise's sets with proper 1RM validation
        val workout = repository.getWorkoutById(workoutId)
        if (workout != null) {
            val exerciseLogs = repository.getExerciseLogsForWorkout(workoutId)

            exerciseLogs.forEach { exerciseLog ->
                if (exerciseLog.exerciseId != null) {
                    val sets = repository.getSetLogsForExercise(exerciseLog.id)

                    // Get current 1RM for this exercise
                    val currentMax =
                        repository
                            .getCurrentMaxesForExercises(userId, listOf(exerciseLog.exerciseId))
                            .firstOrNull()
                            ?.oneRMEstimate

                    // Find the best set that would actually update the 1RM
                    var shouldUpdate = false
                    var bestEstimated1RM = 0f
                    var bestSet: SetLog? = null

                    sets.filter { it.isCompleted }.forEach { set ->
                        // Check for PRs
                        try {
                            repository.checkForPR(set, exerciseLog.exerciseName)
                        } catch (e: Exception) {
                            // Silently continue if PR check fails
                        }

                        // Only calculate 1RM for meaningful sets (not warmups)
                        if (set.actualWeight >= MIN_MEANINGFUL_WEIGHT && set.actualReps <= 10) {
                            // Calculate estimated 1RM using the service
                            val estimated1RM = oneRMService.calculateEstimated1RM(set.actualWeight, set.actualReps)

                            if (estimated1RM != null) {
                                // Only update if this is truly better than current max
                                // Add a threshold to avoid tiny improvements from seeded data
                                val improvementThreshold = currentMax?.times(1 + MIN_IMPROVEMENT_THRESHOLD) ?: 0f

                                if (estimated1RM > improvementThreshold && estimated1RM > bestEstimated1RM) {
                                    // Verify the set makes sense (not artificially inflated)
                                    val expectedWeight =
                                        when (set.actualReps) {
                                            1 -> estimated1RM * ONE_REP_PERCENTAGE
                                            3 -> estimated1RM * THREE_REP_PERCENTAGE
                                            5 -> estimated1RM * FIVE_REP_PERCENTAGE
                                            8 -> estimated1RM * EIGHT_REP_PERCENTAGE
                                            else -> estimated1RM * 0.70f
                                        }

                                    // Only accept if the actual weight is within reasonable range
                                    val tolerance = RPE_TOLERANCE
                                    if (Math.abs(set.actualWeight - expectedWeight) / expectedWeight <= tolerance) {
                                        bestEstimated1RM = estimated1RM
                                        bestSet = set
                                        shouldUpdate = true
                                    }
                                }
                            }
                        }
                    }

                    // Only update 1RM if we found a valid update that passes all checks
                    if (shouldUpdate && bestSet != null) {
                        // Build context with ACTUAL weight from the set, not calculated values
                        val context =
                            "${bestSet!!.actualWeight}kg × ${bestSet!!.actualReps}" +
                                if (bestSet!!.actualRpe != null && bestSet!!.actualRpe > 0) {
                                    " @ RPE ${bestSet!!.actualRpe.toInt()}"
                                } else {
                                    ""
                                }

                        try {
                            repository.upsertExerciseMax(
                                userId = userId,
                                exerciseId = exerciseLog.exerciseId,
                                oneRMEstimate = bestEstimated1RM,
                                oneRMContext = context,
                                oneRMType = com.github.radupana.featherweight.data.profile.OneRMType.AUTOMATICALLY_CALCULATED,
                                notes = "Generated from seeded workout",
                                workoutDate = workout.date,
                            )
                        } catch (e: Exception) {
                            // Silently continue if 1RM update fails
                        }
                    }
                }
            }
        }

        // Complete the workout to trigger all analytics
        val duration = (60 + Random.nextInt(30)) * 60L // 60-90 minutes
        repository.completeWorkout(workoutId, duration)
    }

    // Removed unused cleanup methods - these were temporary debug utilities
    // The proper fix is in clearAllWorkoutData() in FeatherweightRepository
}
