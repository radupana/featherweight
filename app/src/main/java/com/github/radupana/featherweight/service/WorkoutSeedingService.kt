package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.data.WorkoutStatus
import com.github.radupana.featherweight.data.seeding.WorkoutSeedConfig
import com.github.radupana.featherweight.repository.FeatherweightRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

class WorkoutSeedingService(
    private val repository: FeatherweightRepository,
) {
    private val exerciseNames =
        mapOf(
            "squat" to "Barbell Back Squat",
            "bench" to "Barbell Bench Press",
            "deadlift" to "Barbell Deadlift",
            "ohp" to "Barbell Overhead Press",
            "row" to "Barbell Bent-Over Row",
            "incline" to "Dumbbell Incline Bench Press",
            "curl" to "Barbell Bicep Curl",
            "legpress" to "Leg Press",
        )

    suspend fun seedWorkouts(config: WorkoutSeedConfig): Int =
        withContext(Dispatchers.IO) {
            println("🌱 WorkoutSeedingService: Starting workout generation")

            // Get user ID
            val userId = repository.getCurrentUserId()

            // Get exercise IDs
            val exerciseIds = mutableMapOf<String, Long>()
            exerciseNames.forEach { (key, name) ->
                val exercise = repository.getExerciseByName(name)
                if (exercise != null) {
                    exerciseIds[key] = exercise.id
                    println("✅ Found exercise: $name (ID: ${exercise.id})")
                } else {
                    println("❌ Exercise not found: $name")
                }
            }

            // Calculate workout dates
            val endDate = LocalDate.now().minusDays(1)
            val startDate = endDate.minusDays((config.numberOfWorkouts / config.workoutsPerWeek * 7).toLong())
            val workoutDates =
                generateWorkoutDates(startDate, endDate, config.workoutsPerWeek)
                    .take(config.numberOfWorkouts)

            println("📅 Generated ${workoutDates.size} workout dates from $startDate to $endDate")

            // Track generated workout IDs for post-processing
            val generatedWorkoutIds = mutableListOf<Long>()

            // Generate workouts
            workoutDates.forEachIndexed { index, date ->
                val workoutId =
                    generateWorkout(
                        date = date,
                        weekNumber = index / config.workoutsPerWeek + 1,
                        workoutIndex = index,
                        config = config,
                        exerciseIds = exerciseIds,
                    )
                generatedWorkoutIds.add(workoutId)

                println("✅ Generated workout ${index + 1}/${workoutDates.size} for $date")
            }

            println("🎉 WorkoutSeedingService: Completed generating ${workoutDates.size} workouts")

            // Process all generated workouts to update analytics
            println("📊 WorkoutSeedingService: Processing analytics for generated workouts...")
            processGeneratedWorkouts(generatedWorkoutIds, userId)

            workoutDates.size // Return value at the end of the coroutine
        }

    private fun generateWorkoutDates(
        startDate: LocalDate,
        endDate: LocalDate,
        workoutsPerWeek: Int,
    ): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        var current = startDate

        while (current <= endDate) {
            // Generate workouts for this week
            val weekDates =
                when (workoutsPerWeek) {
                    3 -> listOf(1, 3, 5) // Mon, Wed, Fri
                    4 -> listOf(1, 2, 4, 5) // Mon, Tue, Thu, Fri
                    5 -> listOf(1, 2, 3, 4, 5) // Mon-Fri
                    6 -> listOf(1, 2, 3, 4, 5, 6) // Mon-Sat
                    else -> listOf(1, 3, 5) // Default to 3 days
                }

            weekDates.forEach { dayOfWeek ->
                val workoutDate = current.plusDays(dayOfWeek - 1L)
                if (workoutDate <= endDate) {
                    dates.add(workoutDate)
                }
            }

            current = current.plusWeeks(1)
        }

        return dates
    }

    private suspend fun generateWorkout(
        date: LocalDate,
        weekNumber: Int,
        workoutIndex: Int,
        config: WorkoutSeedConfig,
        exerciseIds: Map<String, Long>,
    ): Long {
        // Create workout as IN_PROGRESS so completeWorkout() will process it
        val workout =
            Workout(
                date = date.atTime(18, 0), // 6 PM
                notes = "Generated workout ${workoutIndex + 1}",
                status = WorkoutStatus.IN_PROGRESS, // CRITICAL: Must be IN_PROGRESS for analytics to run
                weekNumber = weekNumber,
                dayNumber = (workoutIndex % config.workoutsPerWeek) + 1,
                isProgrammeWorkout = false,
                durationSeconds = null, // Will be set when completed
            )

        // Insert workout and get ID
        val workoutId = repository.insertWorkout(workout)

        // Generate exercises for this workout
        generateWorkoutExercises(
            workoutId = workoutId,
            workoutIndex = workoutIndex,
            config = config,
            exerciseIds = exerciseIds,
        )

        return workoutId
    }

    private suspend fun generateWorkoutExercises(
        workoutId: Long,
        workoutIndex: Int,
        config: WorkoutSeedConfig,
        exerciseIds: Map<String, Long>,
    ) {
        // Calculate progression - which cycle and week we're in
        val cycleNumber = workoutIndex / 16 // 16 workouts per cycle (4 weeks x 4 workouts/week)
        val weekInCycle = (workoutIndex % 16) / 4 + 1 // Which week (1-4) in current cycle

        // Calculate progressive 1RMs based on cycle
        val progressedSquatRM = config.squatRM + (cycleNumber * 5f) // +5kg per cycle for squats
        val progressedBenchRM = config.benchRM + (cycleNumber * 2.5f) // +2.5kg per cycle for bench
        val progressedDeadliftRM = config.deadliftRM + (cycleNumber * 5f) // +5kg per cycle for deadlifts
        val progressedOhpRM = config.ohpRM + (cycleNumber * 2.5f) // +2.5kg per cycle for OHP

        println("🏋️ Workout ${workoutIndex + 1}: Cycle $cycleNumber, Week $weekInCycle")
        println(
            "   Progressed RMs: Squat ${progressedSquatRM}kg, Bench ${progressedBenchRM}kg, Deadlift ${progressedDeadliftRM}kg, OHP ${progressedOhpRM}kg",
        )

        // Determine workout type based on index
        val workoutType =
            when (workoutIndex % 4) {
                0 -> "squat_bench" // Squat + Bench focus
                1 -> "deadlift_row" // Deadlift + Row focus
                2 -> "squat_ohp" // Squat + OHP focus
                3 -> "bench_accessories" // Bench + accessories
                else -> "squat_bench"
            }

        val exerciseOrder = mutableMapOf<String, Int>()
        var order = 1

        when (workoutType) {
            "squat_bench" -> {
                exerciseOrder["squat"] = order++
                exerciseOrder["bench"] = order++
                exerciseOrder["row"] = order++

                generateExercise(workoutId, exerciseOrder["squat"]!!, "squat", progressedSquatRM, config, exerciseIds, weekInCycle)
                generateExercise(workoutId, exerciseOrder["bench"]!!, "bench", progressedBenchRM, config, exerciseIds, weekInCycle)
                generateExercise(workoutId, exerciseOrder["row"]!!, "row", progressedSquatRM * 0.8f, config, exerciseIds, weekInCycle)
            }

            "deadlift_row" -> {
                exerciseOrder["deadlift"] = order++
                exerciseOrder["row"] = order++
                exerciseOrder["curl"] = order++

                generateExercise(workoutId, exerciseOrder["deadlift"]!!, "deadlift", progressedDeadliftRM, config, exerciseIds, weekInCycle)
                generateExercise(workoutId, exerciseOrder["row"]!!, "row", progressedDeadliftRM * 0.6f, config, exerciseIds, weekInCycle)
                generateExercise(workoutId, exerciseOrder["curl"]!!, "curl", progressedBenchRM * 0.5f, config, exerciseIds, weekInCycle)
            }

            "squat_ohp" -> {
                exerciseOrder["squat"] = order++
                exerciseOrder["ohp"] = order++
                exerciseOrder["incline"] = order++

                generateExercise(workoutId, exerciseOrder["squat"]!!, "squat", progressedSquatRM, config, exerciseIds, weekInCycle)
                generateExercise(workoutId, exerciseOrder["ohp"]!!, "ohp", progressedOhpRM, config, exerciseIds, weekInCycle)
                generateExercise(
                    workoutId,
                    exerciseOrder["incline"]!!,
                    "incline",
                    progressedBenchRM * 0.7f,
                    config,
                    exerciseIds,
                    weekInCycle,
                )
            }

            "bench_accessories" -> {
                exerciseOrder["bench"] = order++
                exerciseOrder["incline"] = order++
                exerciseOrder["curl"] = order++

                generateExercise(workoutId, exerciseOrder["bench"]!!, "bench", progressedBenchRM, config, exerciseIds, weekInCycle)
                generateExercise(
                    workoutId,
                    exerciseOrder["incline"]!!,
                    "incline",
                    progressedBenchRM * 0.7f,
                    config,
                    exerciseIds,
                    weekInCycle,
                )
                generateExercise(workoutId, exerciseOrder["curl"]!!, "curl", progressedBenchRM * 0.5f, config, exerciseIds, weekInCycle)
            }
        }
    }

    private suspend fun generateExercise(
        workoutId: Long,
        exerciseOrder: Int,
        exerciseKey: String,
        baseRM: Float,
        config: WorkoutSeedConfig,
        exerciseIds: Map<String, Long>,
        weekInCycle: Int,
    ) {
        exerciseIds[exerciseKey] ?: return
        val exerciseName = exerciseNames[exerciseKey] ?: return

        // Create exercise log
        val exerciseLog =
            ExerciseLog(
                workoutId = workoutId,
                exerciseName = exerciseName,
                exerciseOrder = exerciseOrder,
                notes = null,
            )

        // Insert exercise log and get ID
        val exerciseLogId = repository.insertExerciseLog(exerciseLog)

        // Generate sets based on program style
        when (config.programStyle) {
            "5/3/1" -> generate531Sets(exerciseLogId, baseRM, config, weekInCycle)
            "Linear" -> generateLinearSets(exerciseLogId, baseRM, config, weekInCycle)
            "Random" -> generateRandomSets(exerciseLogId, baseRM, config)
            else -> generate531Sets(exerciseLogId, baseRM, config, weekInCycle)
        }
    }

    private suspend fun generate531Sets(
        exerciseLogId: Long,
        baseRM: Float,
        config: WorkoutSeedConfig,
        weekInCycle: Int,
    ) {
        val trainingMax = baseRM * 0.9f // 5/3/1 uses 90% of 1RM

        // Proper 5/3/1 week cycling
        val (percentages, reps, weekName) =
            when (weekInCycle) {
                1 -> Triple(listOf(0.65f, 0.75f, 0.85f), listOf(5, 5, 5), "Week 1 (5/5/5+)")
                2 -> Triple(listOf(0.70f, 0.80f, 0.90f), listOf(3, 3, 3), "Week 2 (3/3/3+)")
                3 -> Triple(listOf(0.75f, 0.85f, 0.95f), listOf(5, 3, 1), "Week 3 (5/3/1+)")
                4 -> Triple(listOf(0.40f, 0.50f, 0.60f), listOf(5, 5, 5), "Week 4 (Deload)")
                else -> Triple(listOf(0.65f, 0.75f, 0.85f), listOf(5, 5, 5), "Week 1 (5/5/5+)")
            }

        println("     $weekName - Training Max: ${trainingMax}kg")

        percentages.zip(reps).forEachIndexed { index, (percentage, targetReps) ->
            val weight =
                (trainingMax * percentage).let { w ->
                    if (config.includeVariation) {
                        w * Random.nextFloat().let { 0.95f + it * 0.1f } // ±5%
                    } else {
                        w
                    }
                }

            val actualReps =
                if (config.includeFailures && Random.nextFloat() < 0.05f) {
                    maxOf(1, targetReps - Random.nextInt(3))
                } else {
                    // AMRAP set (last set gets bonus reps, except on deload week)
                    if (index == 2 && weekInCycle != 4) {
                        val bonusReps =
                            when (weekInCycle) {
                                1 -> Random.nextInt(2, 6) // 5+ set: 2-5 bonus reps
                                2 -> Random.nextInt(1, 4) // 3+ set: 1-3 bonus reps
                                3 -> Random.nextInt(1, 3) // 1+ set: 1-2 bonus reps
                                else -> 0
                            }
                        targetReps + bonusReps
                    } else {
                        targetReps
                    }
                }

            val setLog =
                SetLog(
                    exerciseLogId = exerciseLogId,
                    setOrder = index + 1,
                    targetReps = targetReps,
                    targetWeight = weight,
                    actualReps = actualReps,
                    actualWeight = weight,
                    isCompleted = true,
                    actualRpe =
                        when (weekInCycle) {
                            1 -> Random.nextInt(6, 8).toFloat() // Week 1: moderate intensity
                            2 -> Random.nextInt(7, 9).toFloat() // Week 2: higher intensity
                            3 -> Random.nextInt(8, 10).toFloat() // Week 3: highest intensity
                            4 -> Random.nextInt(4, 6).toFloat() // Week 4: deload, easy
                            else -> Random.nextInt(6, 8).toFloat()
                        },
                    notes = if (index == 2 && weekInCycle != 4) "AMRAP set" else null,
                    completedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                )

            println("       Set ${index + 1}: ${weight.toInt()}kg x $actualReps @ RPE ${setLog.actualRpe}")
            repository.insertSetLog(setLog)
        }
    }

    private suspend fun generateLinearSets(
        exerciseLogId: Long,
        baseRM: Float,
        config: WorkoutSeedConfig,
        weekInCycle: Int,
    ) {
        // Linear progression - deload every 4th week, otherwise steady increase
        val workingWeight =
            if (weekInCycle == 4) {
                baseRM * 0.7f // Deload week: 70% of 1RM
            } else {
                baseRM * 0.8f // Normal weeks: 80% of 1RM
            }

        val weekName = if (weekInCycle == 4) "Deload" else "Working"
        println("     Linear $weekName - Working Weight: ${workingWeight.toInt()}kg")

        // 3 sets of 5 reps
        repeat(3) { index ->
            val weight =
                workingWeight.let { w ->
                    if (config.includeVariation) {
                        w * Random.nextFloat().let { 0.95f + it * 0.1f }
                    } else {
                        w
                    }
                }

            val actualReps =
                if (config.includeFailures && Random.nextFloat() < 0.05f) {
                    maxOf(1, 5 - Random.nextInt(3))
                } else {
                    5
                }

            val setLog =
                SetLog(
                    exerciseLogId = exerciseLogId,
                    setOrder = index + 1,
                    targetReps = 5,
                    targetWeight = weight,
                    actualReps = actualReps,
                    actualWeight = weight,
                    isCompleted = true,
                    actualRpe = if (weekInCycle == 4) Random.nextInt(4, 6).toFloat() else Random.nextInt(6, 9).toFloat(),
                    notes = if (weekInCycle == 4) "Deload week" else null,
                    completedAt = null,
                )

            println("       Set ${index + 1}: ${weight.toInt()}kg x $actualReps @ RPE ${setLog.actualRpe}")
            repository.insertSetLog(setLog)
        }
    }

    private suspend fun generateRandomSets(
        exerciseLogId: Long,
        baseRM: Float,
        config: WorkoutSeedConfig,
    ) {
        val numSets = Random.nextInt(3, 6)
        val repRange = listOf(3, 5, 8, 10, 12)

        repeat(numSets) { index ->
            val targetReps = repRange.random()
            val percentage =
                when (targetReps) {
                    3 -> 0.85f
                    5 -> 0.8f
                    8 -> 0.75f
                    10 -> 0.7f
                    12 -> 0.65f
                    else -> 0.75f
                }

            val weight =
                (baseRM * percentage).let { w ->
                    if (config.includeVariation) {
                        w * Random.nextFloat().let { 0.95f + it * 0.1f }
                    } else {
                        w
                    }
                }

            val actualReps =
                if (config.includeFailures && Random.nextFloat() < 0.05f) {
                    maxOf(1, targetReps - Random.nextInt(3))
                } else {
                    targetReps
                }

            val setLog =
                SetLog(
                    exerciseLogId = exerciseLogId,
                    setOrder = index + 1,
                    targetReps = targetReps,
                    targetWeight = weight,
                    actualReps = actualReps,
                    actualWeight = weight,
                    isCompleted = true,
                    actualRpe = Random.nextInt(6, 10).toFloat(),
                    notes = null,
                    completedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                )

            repository.insertSetLog(setLog)
        }
    }

    private suspend fun processGeneratedWorkouts(
        workoutIds: List<Long>,
        userId: Long,
    ) {
        println("🔄 Processing ${workoutIds.size} workouts for analytics...")

        workoutIds.forEach { workoutId ->
            try {
                // Process each workout as if it was just completed
                // The most critical part is updating global exercise progress

                // 1. Complete the workout - this triggers ALL analytics
                // Since we created workouts as IN_PROGRESS, completeWorkout() will:
                // - Update status to COMPLETED
                // - Update GlobalExerciseProgress (populates Exercise tab!)
                // - Calculate estimated 1RMs
                // - Record performance data
                println("📊 Completing workout $workoutId to trigger all analytics")
                repository.completeWorkout(workoutId, (90 * 60).toLong())

                // 2. Manually trigger PR detection for seeded workouts
                // Since seeded sets don't go through markSetCompleted, we need to check PRs manually
                println("🏆 Manually checking for PRs in seeded workout...")
                val workout = repository.getWorkoutById(workoutId)
                if (workout != null) {
                    val exerciseLogs = repository.getExerciseLogsForWorkout(workoutId)
                    exerciseLogs.forEach { exerciseLog ->
                        val sets = repository.getSetLogsForExercise(exerciseLog.id)
                        sets.filter { it.isCompleted }.forEach { set ->
                            try {
                                val prs = repository.checkForPR(set, exerciseLog.exerciseName)
                                if (prs.isNotEmpty()) {
                                    println("🏆 Found ${prs.size} PRs for ${exerciseLog.exerciseName}")
                                }
                            } catch (e: Exception) {
                                println("❌ Error checking PR: ${e.message}")
                            }
                        }
                    }
                }
                println("🏆 PR detection completed for workout $workoutId")
            } catch (e: Exception) {
                println("❌ Error processing workout $workoutId: ${e.message}")
                e.printStackTrace()
            }
        }

        println("✅ Analytics processing complete!")

        // Verify PR data was saved
        val allPRs = repository.getAllPersonalRecordsFromDB()
        println("📊 Personal Records in database after seeding: ${allPRs.size}")
        allPRs.forEach { pr ->
            println("  🏆 ${pr.exerciseName}: ${pr.weight}kg (${pr.recordType}) on ${pr.recordDate}")
        }

        // Verify exercise-specific PRs
        val exercisesToCheck = listOf("Barbell Back Squat", "Barbell Bench Press", "Barbell Deadlift", "Barbell Overhead Press")
        exercisesToCheck.forEach { exercise ->
            val prs = repository.getPersonalRecords(exercise)
            println("📊 PRs for $exercise: ${prs.size} records")
        }
    }
}
