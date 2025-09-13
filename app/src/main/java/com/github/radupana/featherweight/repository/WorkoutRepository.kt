package com.github.radupana.featherweight.repository

import android.app.Application
import android.util.Log
import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.data.WorkoutStatus
import com.github.radupana.featherweight.domain.WorkoutSummary
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/**
 * Repository for managing Workout-related data
 */
class WorkoutRepository(
    application: Application,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val db: FeatherweightDatabase = FeatherweightDatabase.getDatabase(application),
) {
    private val workoutDao = db.workoutDao()
    private val exerciseLogDao = db.exerciseLogDao()
    private val setLogDao = db.setLogDao()
    private val programmeDao = db.programmeDao()

    companion object {
        private const val TAG = "WorkoutRepository"
    }

    suspend fun createWorkout(workout: Workout): Long =
        withContext(ioDispatcher) {
            val trace = safeNewTrace("workout_creation")
            trace?.start()
            val id = workoutDao.insertWorkout(workout)
            Log.i(TAG, "Created workout - id: $id, name: ${workout.name}, status: ${workout.status}, programmeId: ${workout.programmeId}")
            trace?.stop()
            id
        }

    suspend fun getWorkoutById(workoutId: Long): Workout? =
        withContext(ioDispatcher) {
            workoutDao.getWorkoutById(workoutId)
        }

    suspend fun deleteWorkout(workout: Workout) =
        withContext(ioDispatcher) {
            Log.i(TAG, "Deleting workout - id: ${workout.id}, name: ${workout.name}")
            val exerciseLogs = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
            exerciseLogs.forEach { exerciseLog ->
                val setLogs = setLogDao.getSetLogsForExercise(exerciseLog.id)
                setLogs.forEach { setLogDao.deleteSetLog(it.id) }
                exerciseLogDao.deleteExerciseLog(exerciseLog.id)
            }
            workoutDao.deleteWorkout(workout.id)
            Log.i(TAG, "Workout deleted - id: ${workout.id}, had ${exerciseLogs.size} exercises")
        }

    suspend fun deleteWorkoutById(workoutId: Long) =
        withContext(ioDispatcher) {
            val workout = workoutDao.getWorkoutById(workoutId)
            if (workout != null) {
                deleteWorkout(workout)
            }
        }

    suspend fun updateWorkoutStatus(
        workoutId: Long,
        status: WorkoutStatus,
    ) = withContext(ioDispatcher) {
        val workout = workoutDao.getWorkoutById(workoutId) ?: return@withContext
        val oldStatus = workout.status
        workoutDao.updateWorkout(workout.copy(status = status))
        Log.i(TAG, "Updated workout status - id: $workoutId, from: $oldStatus to: $status")
    }

    suspend fun completeWorkout(
        workoutId: Long,
        duration: Long? = null,
    ) = withContext(ioDispatcher) {
        val trace = safeNewTrace("workout_completion")
        trace?.start()

        val workout = workoutDao.getWorkoutById(workoutId) ?: return@withContext
        workoutDao.updateWorkout(
            workout.copy(
                status = WorkoutStatus.COMPLETED,
                durationSeconds = duration,
            ),
        )

        val exerciseCount = exerciseLogDao.getExerciseLogsForWorkout(workoutId).size
        val setCount =
            exerciseLogDao.getExerciseLogsForWorkout(workoutId).sumOf {
                setLogDao.getSetLogsForExercise(it.id).size
            }

        trace?.putAttribute("exercise_count", exerciseCount.toString())
        trace?.putAttribute("set_count", setCount.toString())
        trace?.putMetric("duration_seconds", duration ?: 0)

        Log.i(
            TAG,
            "Workout completed - id: $workoutId, name: ${workout.name ?: "Unnamed"}, " +
                "duration: ${duration ?: 0}s, exercises: $exerciseCount, sets: $setCount, " +
                "programmeId: ${workout.programmeId ?: "none"}",
        )

        trace?.stop()
    }

    private fun safeNewTrace(name: String): Trace? =
        try {
            FirebasePerformance.getInstance().newTrace(name)
        } catch (e: IllegalStateException) {
            Log.d(TAG, "Firebase Performance not available - likely in test environment")
            null
        } catch (e: RuntimeException) {
            Log.d(TAG, "Firebase Performance trace creation failed: ${e.message}")
            null
        }

    suspend fun getExerciseLogsForWorkout(workoutId: Long): List<ExerciseLog> =
        withContext(ioDispatcher) {
            exerciseLogDao.getExerciseLogsForWorkout(workoutId)
        }

    suspend fun getSetLogsForExercise(exerciseLogId: Long): List<SetLog> =
        withContext(ioDispatcher) {
            setLogDao.getSetLogsForExercise(exerciseLogId)
        }

    suspend fun getWorkoutHistory(): List<WorkoutSummary> =
        withContext(ioDispatcher) {
            val startTime = System.currentTimeMillis()
            val allWorkouts = workoutDao.getAllWorkouts()
            val result =
                allWorkouts
                    .filter { it.status != WorkoutStatus.TEMPLATE } // Exclude templates from history
                    .mapNotNull { workout ->
                        val exercises = exerciseLogDao.getExerciseLogsForWorkout(workout.id)

                        if (workout.status != WorkoutStatus.COMPLETED && exercises.isEmpty()) return@mapNotNull null

                        val allSets = mutableListOf<SetLog>()
                        exercises.forEach { exercise ->
                            allSets.addAll(setLogDao.getSetLogsForExercise(exercise.id))
                        }

                        val completedSets = allSets.filter { it.isCompleted }
                        val totalWeight = completedSets.sumOf { (it.actualWeight * it.actualReps).toDouble() }.toFloat()

                        val programmeName =
                            if (workout.isProgrammeWorkout && workout.programmeId != null) {
                                try {
                                    programmeDao.getProgrammeById(workout.programmeId)?.name
                                } catch (e: android.database.sqlite.SQLiteException) {
                                    Log.e(TAG, "Failed to get programme name for programmeId: ${workout.programmeId}", e)
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

            Log.d(
                TAG,
                "getWorkoutHistory took ${System.currentTimeMillis() - startTime}ms - " +
                    "total workouts: ${allWorkouts.size}, summaries: ${result.size}, " +
                    "completed: ${result.count { it.status == WorkoutStatus.COMPLETED }}",
            )
            result
        }

    suspend fun createTemplateFromWorkout(
        workoutId: Long,
        templateName: String,
        templateDescription: String?,
    ): Long =
        withContext(ioDispatcher) {
            Log.i(TAG, "createTemplateFromWorkout START - workoutId: $workoutId, name: $templateName, desc: $templateDescription")

            val exerciseLogs = exerciseLogDao.getExerciseLogsForWorkout(workoutId)
            Log.i(TAG, "Found ${exerciseLogs.size} exercises in workout $workoutId")

            // Create template workout
            val templateWorkout =
                Workout(
                    name = templateName,
                    notes = templateDescription,
                    date = LocalDateTime.now(),
                    status = WorkoutStatus.TEMPLATE,
                    isTemplate = true,
                )
            Log.i(TAG, "Creating template workout with status: ${templateWorkout.status}, isTemplate: ${templateWorkout.isTemplate}")
            val templateId = workoutDao.insertWorkout(templateWorkout)
            Log.i(TAG, "Template workout created with ID: $templateId")

            // Copy all exercises and their sets exactly as they were
            var copiedExercises = 0
            var copiedSets = 0
            exerciseLogs.forEach { exerciseLog ->
                Log.d(TAG, "Processing exercise ${exerciseLog.id} with variation ${exerciseLog.exerciseVariationId}")
                val originalSets =
                    setLogDao
                        .getSetLogsForExercise(exerciseLog.id)
                        .filter { it.actualReps > 0 } // Only include completed sets

                Log.d(TAG, "Found ${originalSets.size} completed sets for exercise ${exerciseLog.id}")

                if (originalSets.isEmpty()) {
                    Log.d(TAG, "Skipping exercise ${exerciseLog.id} - no completed sets")
                    return@forEach // Skip exercises with no completed sets
                }

                // Create exercise log for template
                val templateExerciseLog =
                    ExerciseLog(
                        workoutId = templateId,
                        exerciseVariationId = exerciseLog.exerciseVariationId,
                        exerciseOrder = exerciseLog.exerciseOrder,
                    )
                val templateExerciseId = exerciseLogDao.insertExerciseLog(templateExerciseLog)
                Log.d(TAG, "Created template exercise with ID: $templateExerciseId")
                copiedExercises++

                // Copy all completed sets with actual values mapped to target values
                originalSets.forEach { originalSet ->
                    val templateSet =
                        SetLog(
                            exerciseLogId = templateExerciseId,
                            setOrder = originalSet.setOrder,
                            targetReps = originalSet.actualReps,
                            targetWeight = originalSet.actualWeight.takeIf { it > 0 },
                            targetRpe = originalSet.actualRpe,
                            actualReps = 0,
                            actualWeight = 0f,
                        )
                    Log.d(TAG, "Creating template set - targetReps: ${templateSet.targetReps}, targetWeight: ${templateSet.targetWeight}, targetRpe: ${templateSet.targetRpe}")
                    setLogDao.insertSetLog(templateSet)
                    copiedSets++
                }
            }

            Log.i(TAG, "TEMPLATE CREATION COMPLETE - templateId: $templateId, name: $templateName, copiedExercises: $copiedExercises, copiedSets: $copiedSets")
            templateId
        }

    suspend fun getTemplates(): List<WorkoutSummary> =
        withContext(ioDispatcher) {
            Log.i(TAG, "getTemplates() called")
            val startTime = System.currentTimeMillis()
            val allWorkouts = workoutDao.getAllWorkouts()
            Log.d(TAG, "Total workouts in database: ${allWorkouts.size}")

            val templates = allWorkouts.filter { it.status == WorkoutStatus.TEMPLATE }
            Log.i(TAG, "Found ${templates.size} templates out of ${allWorkouts.size} total workouts")

            val result =
                templates
                    .mapNotNull { workout ->
                        Log.d(TAG, "Processing template: id=${workout.id}, name=${workout.name}, status=${workout.status}, isTemplate=${workout.isTemplate}")

                        val exercises = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
                        Log.d(TAG, "Template ${workout.id} has ${exercises.size} exercises")

                        val allSets = mutableListOf<SetLog>()
                        exercises.forEach { exercise ->
                            val sets = setLogDao.getSetLogsForExercise(exercise.id)
                            allSets.addAll(sets)
                        }
                        Log.d(TAG, "Template ${workout.id} has ${allSets.size} total sets")

                        WorkoutSummary(
                            id = workout.id,
                            date = workout.date,
                            name = workout.name,
                            exerciseCount = exercises.size,
                            setCount = allSets.size,
                            totalWeight = 0f, // Templates don't have actual weight
                            duration = null,
                            status = workout.status,
                            hasNotes = !workout.notes.isNullOrBlank(),
                            isProgrammeWorkout = false,
                            programmeId = null,
                            programmeName = null,
                            programmeWorkoutName = null,
                            weekNumber = null,
                            dayNumber = null,
                        )
                    }.sortedByDescending { it.date }

            Log.i(TAG, "getTemplates() returning ${result.size} templates in ${System.currentTimeMillis() - startTime}ms")
            result
        }

    suspend fun startWorkoutFromTemplate(templateId: Long): Long =
        withContext(ioDispatcher) {
            Log.i(TAG, "startWorkoutFromTemplate called with templateId: $templateId")

            val template = workoutDao.getWorkoutById(templateId)
            if (template == null || template.status != WorkoutStatus.TEMPLATE) {
                Log.e(TAG, "Template not found or not a template: id=$templateId, status=${template?.status}")
                throw IllegalArgumentException("Invalid template ID: $templateId")
            }

            Log.i(TAG, "Found template: ${template.name}")

            // Create new workout from template
            val newWorkout =
                Workout(
                    name = template.name,
                    notes = template.notes,
                    date = LocalDateTime.now(),
                    status = WorkoutStatus.NOT_STARTED,
                    isProgrammeWorkout = false,
                    isTemplate = false,
                    fromTemplateId = templateId,
                )
            val newWorkoutId = workoutDao.insertWorkout(newWorkout)
            Log.i(TAG, "Created new workout with ID: $newWorkoutId from template: ${template.name} (templateId: $templateId)")

            // Copy exercises and sets from template
            val templateExercises = exerciseLogDao.getExerciseLogsForWorkout(templateId)
            Log.d(TAG, "Copying ${templateExercises.size} exercises from template")

            var copiedExercises = 0
            var copiedSets = 0

            templateExercises.forEach { templateExercise ->
                val newExercise =
                    ExerciseLog(
                        workoutId = newWorkoutId,
                        exerciseVariationId = templateExercise.exerciseVariationId,
                        exerciseOrder = templateExercise.exerciseOrder,
                        notes = templateExercise.notes,
                    )
                val newExerciseId = exerciseLogDao.insertExerciseLog(newExercise)
                copiedExercises++

                // Copy sets with target values
                val templateSets = setLogDao.getSetLogsForExercise(templateExercise.id)
                Log.d(TAG, "Copying ${templateSets.size} sets for exercise ${templateExercise.id}")

                templateSets.forEach { templateSet ->
                    val newSet =
                        SetLog(
                            exerciseLogId = newExerciseId,
                            setOrder = templateSet.setOrder,
                            targetReps = templateSet.targetReps,
                            targetWeight = templateSet.targetWeight,
                            targetRpe = templateSet.targetRpe,
                            actualReps = 0,
                            actualWeight = 0f,
                            actualRpe = null,
                            isCompleted = false,
                        )
                    setLogDao.insertSetLog(newSet)
                    copiedSets++
                    Log.d(TAG, "Created set with targetReps: ${newSet.targetReps}, targetWeight: ${newSet.targetWeight}")
                }
            }

            Log.i(TAG, "WORKOUT FROM TEMPLATE CREATED - workoutId: $newWorkoutId, name: ${template.name}, fromTemplateId: $templateId, exercises: $copiedExercises, sets: $copiedSets")
            newWorkoutId
        }
}
