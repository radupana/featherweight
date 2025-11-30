package com.github.radupana.featherweight.repository

import android.app.Application
import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.TemplateExercise
import com.github.radupana.featherweight.data.TemplateSet
import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.data.WorkoutStatus
import com.github.radupana.featherweight.data.WorkoutTemplate
import com.github.radupana.featherweight.di.ServiceLocator
import com.github.radupana.featherweight.domain.TemplateSummary
import com.github.radupana.featherweight.manager.AuthenticationManager
import com.github.radupana.featherweight.util.CloudLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class WorkoutTemplateRepository(
    application: Application,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val db: FeatherweightDatabase = FeatherweightDatabase.getDatabase(application),
    private val authManager: AuthenticationManager = ServiceLocator.provideAuthenticationManager(application),
) {
    private val templateDao = db.workoutTemplateDao()
    private val templateExerciseDao = db.templateExerciseDao()
    private val templateSetDao = db.templateSetDao()
    private val workoutDao = db.workoutDao()
    private val exerciseLogDao = db.exerciseLogDao()
    private val setLogDao = db.setLogDao()

    companion object {
        private const val TAG = "WorkoutTemplateRepository"
    }

    suspend fun getTemplates(): List<TemplateSummary> =
        withContext(ioDispatcher) {
            val userId = authManager.getCurrentUserId() ?: "local"
            val templates = templateDao.getTemplatesByUserId(userId)

            templates.map { template ->
                val exercises = templateExerciseDao.getExercisesForTemplate(template.id)
                val setCount =
                    exercises.sumOf { exercise ->
                        templateSetDao.getSetsForTemplateExercise(exercise.id).size
                    }

                TemplateSummary(
                    id = template.id,
                    name = template.name,
                    description = template.description,
                    exerciseCount = exercises.size,
                    setCount = setCount,
                    createdAt = template.createdAt,
                    updatedAt = template.updatedAt,
                )
            }
        }

    suspend fun getTemplateById(templateId: String): WorkoutTemplate? =
        withContext(ioDispatcher) {
            templateDao.getTemplateById(templateId)
        }

    suspend fun createTemplateFromWorkout(
        workoutId: String,
        templateName: String,
        templateDescription: String?,
    ): String =
        withContext(ioDispatcher) {
            CloudLogger.info(TAG, "createTemplateFromWorkout - workoutId: $workoutId, name: $templateName")

            val userId = authManager.getCurrentUserId() ?: "local"
            val exerciseLogs = exerciseLogDao.getExerciseLogsForWorkout(workoutId)

            val template =
                WorkoutTemplate(
                    userId = userId,
                    name = templateName,
                    description = templateDescription,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                )
            templateDao.insertTemplate(template)

            var copiedExercises = 0
            var copiedSets = 0

            exerciseLogs.forEach { exerciseLog ->
                val completedSets =
                    setLogDao
                        .getSetLogsForExercise(exerciseLog.id)
                        .filter { it.actualReps > 0 }

                if (completedSets.isEmpty()) return@forEach

                val templateExercise =
                    TemplateExercise(
                        userId = userId,
                        templateId = template.id,
                        exerciseId = exerciseLog.exerciseId,
                        exerciseOrder = exerciseLog.exerciseOrder,
                        notes = exerciseLog.notes,
                    )
                templateExerciseDao.insertTemplateExercise(templateExercise)
                copiedExercises++

                completedSets.forEach { set ->
                    val templateSet =
                        TemplateSet(
                            userId = userId,
                            templateExerciseId = templateExercise.id,
                            setOrder = set.setOrder,
                            targetReps = set.actualReps,
                            targetWeight = set.actualWeight.takeIf { it > 0 },
                            targetRpe = set.actualRpe,
                        )
                    templateSetDao.insertTemplateSet(templateSet)
                    copiedSets++
                }
            }

            CloudLogger.info(TAG, "Template created - id: ${template.id}, exercises: $copiedExercises, sets: $copiedSets")
            template.id
        }

    suspend fun startWorkoutFromTemplate(templateId: String): String =
        withContext(ioDispatcher) {
            CloudLogger.info(TAG, "startWorkoutFromTemplate - templateId: $templateId")

            val template =
                templateDao.getTemplateById(templateId)
                    ?: throw IllegalArgumentException("Template not found: $templateId")

            val userId = authManager.getCurrentUserId() ?: "local"

            val workout =
                Workout(
                    userId = userId,
                    name = template.name,
                    notes = template.description,
                    date = LocalDateTime.now(),
                    status = WorkoutStatus.NOT_STARTED,
                )
            workoutDao.insertWorkout(workout)

            val templateExercises = templateExerciseDao.getExercisesForTemplate(templateId)

            templateExercises.forEach { templateExercise ->
                val exerciseLog =
                    ExerciseLog(
                        userId = userId,
                        workoutId = workout.id,
                        exerciseId = templateExercise.exerciseId,
                        exerciseOrder = templateExercise.exerciseOrder,
                        notes = templateExercise.notes,
                    )
                exerciseLogDao.insertExerciseLog(exerciseLog)

                val templateSets = templateSetDao.getSetsForTemplateExercise(templateExercise.id)
                templateSets.forEach { templateSet ->
                    val setLog =
                        SetLog(
                            userId = userId,
                            exerciseLogId = exerciseLog.id,
                            setOrder = templateSet.setOrder,
                            targetReps = templateSet.targetReps,
                            targetWeight = templateSet.targetWeight,
                            targetRpe = templateSet.targetRpe,
                            actualReps = 0,
                            actualWeight = 0f,
                        )
                    setLogDao.insertSetLog(setLog)
                }
            }

            CloudLogger.info(TAG, "Workout created from template - workoutId: ${workout.id}")
            workout.id
        }

    suspend fun deleteTemplate(templateId: String) =
        withContext(ioDispatcher) {
            CloudLogger.info(TAG, "deleteTemplate - id: $templateId")
            templateDao.deleteTemplate(templateId)
        }
}
