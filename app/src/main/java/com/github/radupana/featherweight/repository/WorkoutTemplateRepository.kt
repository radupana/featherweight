package com.github.radupana.featherweight.repository

import android.app.Application
import android.util.Log
import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.data.WorkoutStatus
import com.github.radupana.featherweight.data.model.SkillLevel
import com.github.radupana.featherweight.data.model.WorkoutTemplate
import com.github.radupana.featherweight.data.model.WorkoutTemplateConfig
import com.github.radupana.featherweight.data.model.WorkoutTemplateGenerationConfig
import com.github.radupana.featherweight.data.programme.RepsStructure
import com.github.radupana.featherweight.service.WorkoutTemplateGeneratorService
import com.github.radupana.featherweight.service.WorkoutTemplateWeightService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/**
 * Repository for managing workout template generation and application
 */
class WorkoutTemplateRepository(
    application: Application,
    private val featherweightRepository: FeatherweightRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val db = FeatherweightDatabase.getDatabase(application)
    private val workoutDao = db.workoutDao()
    private val exerciseLogDao = db.exerciseLogDao()
    private val setLogDao = db.setLogDao()
    private val programmeDao = db.programmeDao()
    private val exerciseVariationDao = db.exerciseVariationDao()
    private val oneRMDao = db.oneRMDao()
    private val exerciseDao = db.exerciseDao()
    private val globalExerciseProgressDao = db.globalExerciseProgressDao()
    
    private val workoutTemplateGeneratorService = WorkoutTemplateGeneratorService(exerciseVariationDao)
    private val workoutTemplateWeightService = WorkoutTemplateWeightService(
        featherweightRepository,
        oneRMDao,
        setLogDao,
        exerciseLogDao,
        com.github.radupana.featherweight.service.FreestyleIntelligenceService(globalExerciseProgressDao)
    )
    
    suspend fun generateWorkoutFromTemplate(
        template: WorkoutTemplate,
        config: WorkoutTemplateConfig
    ): Long = withContext(ioDispatcher) {
        val workoutId = workoutDao.insertWorkout(
            Workout(
                id = 0,
                name = template.name,
                date = LocalDateTime.now(),
                status = WorkoutStatus.IN_PROGRESS
            )
        )
        
        val generationConfig = WorkoutTemplateGenerationConfig(
            time = config.timeAvailable,
            goal = config.goal,
            skillLevel = SkillLevel.INTERMEDIATE
        )
        
        try {
            val exercises = workoutTemplateGeneratorService.generateTemplate(
                templateName = template.name,
                config = generationConfig
            )
            
            if (exercises.isEmpty()) {
                Log.w("WorkoutTemplateRepository", "Template generator returned empty exercise list for: ${template.name}")
                return@withContext workoutId
            }
            
            exercises.forEachIndexed { index, (variation, sets, reps) ->
                val exerciseLogId = exerciseLogDao.insertExerciseLog(
                    ExerciseLog(
                        workoutId = workoutId,
                        exerciseVariationId = variation.id,
                        exerciseOrder = index,
                        notes = null
                    )
                )
                
                repeat(sets) { setIndex ->
                    val setLog = SetLog(
                        exerciseLogId = exerciseLogId,
                        setOrder = setIndex + 1,
                        targetReps = reps,
                        targetWeight = null,
                        actualReps = 0,
                        actualWeight = 0f,
                        actualRpe = null
                    )
                    setLogDao.insertSetLog(setLog)
                }
            }
        } catch (e: android.database.sqlite.SQLiteException) {
            Log.e("WorkoutTemplateRepository", "Failed to generate template exercises: ${e.message}", e)
        } catch (e: IllegalArgumentException) {
            Log.e("WorkoutTemplateRepository", "Failed to generate template exercises: ${e.message}", e)
        }
        
        workoutId
    }
    
    suspend fun applyTemplateWeightSuggestions(
        workoutId: Long,
        config: WorkoutTemplateConfig
    ) = withContext(ioDispatcher) {
        workoutTemplateWeightService.applyWeightSuggestions(workoutId, config)
    }
    
    suspend fun createWorkoutFromProgrammeTemplate(
        programmeId: Long,
        weekNumber: Int,
        dayNumber: Int,
        getNextProgrammeWorkout: suspend (Long) -> Any?
    ): Long = withContext(ioDispatcher) {
        Log.d("WorkoutTemplateRepository", "=== CREATING WORKOUT FROM PROGRAMME TEMPLATE ===")
        Log.d("WorkoutTemplateRepository", "Programme ID: $programmeId, Week: $weekNumber, Day: $dayNumber")
        
        programmeDao.getProgrammeById(programmeId)
            ?: throw IllegalArgumentException("Programme not found")
        
        val progress = programmeDao.getProgressForProgramme(programmeId)
        if (progress == null) {
            Log.e("WorkoutTemplateRepository", "No progress found for programme $programmeId")
            error("Programme progress not found")
        }
        
        val nextWorkoutInfo = getNextProgrammeWorkout(programmeId) as? NextProgrammeWorkoutInfo
        if (nextWorkoutInfo == null) {
            Log.e("WorkoutTemplateRepository", "No next workout found for programme $programmeId")
            error("No workout available for this programme")
        }
        
        val workoutStructure = nextWorkoutInfo.workoutStructure
        Log.d("WorkoutTemplateRepository", "Found workout structure: ${workoutStructure.name}")
        Log.d("WorkoutTemplateRepository", "Exercises in structure: ${workoutStructure.exercises.size}")
        
        val now = LocalDateTime.now()
        val workout = Workout(
            date = now,
            status = WorkoutStatus.IN_PROGRESS,
            programmeId = programmeId,
            weekNumber = nextWorkoutInfo.actualWeekNumber,
            dayNumber = workoutStructure.day,
            programmeWorkoutName = workoutStructure.name,
            isProgrammeWorkout = true
        )
        val workoutId = workoutDao.insertWorkout(workout)
        Log.d("WorkoutTemplateRepository", "Created workout with ID: $workoutId")
        
        workoutStructure.exercises.forEachIndexed { index, exerciseStructure ->
            Log.d("WorkoutTemplateRepository", "Adding exercise ${index + 1}: ${exerciseStructure.name}")
            Log.d("WorkoutTemplateRepository", "  exerciseId: ${exerciseStructure.exerciseId}")
            
            val exerciseVariationId = exerciseStructure.exerciseId
            if (exerciseVariationId != null) {
                val exerciseLog = ExerciseLog(
                    workoutId = workoutId,
                    exerciseVariationId = exerciseVariationId,
                    exerciseOrder = index,
                    notes = exerciseStructure.note
                )
                val exerciseLogId = exerciseLogDao.insertExerciseLog(exerciseLog)
                Log.d("WorkoutTemplateRepository", "  Created exercise log with ID: $exerciseLogId")
                
                val weights = exerciseStructure.weights
                val rpeList = exerciseStructure.rpeValues
                
                Log.d("WorkoutTemplateRepository", "  Weights: $weights, RPE values: $rpeList")
                
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
                                actualRpe = null
                            )
                            Log.d("WorkoutTemplateRepository", "    Creating SetLog: targetReps=$targetReps, targetWeight=$targetWeight, targetRpe=$targetRpe")
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
                                actualRpe = null
                            )
                            setLogDao.insertSetLog(setLog)
                        }
                    }
                    is RepsStructure.Single -> {
                        repeat(exerciseStructure.sets) { setIndex ->
                            val targetWeight = weights?.getOrNull(setIndex)?.toFloat() ?: 0f
                            
                            val setLog = SetLog(
                                exerciseLogId = exerciseLogId,
                                setOrder = setIndex + 1,
                                targetReps = reps.value,
                                targetWeight = targetWeight,
                                actualReps = 0,
                                actualWeight = 0f,
                                actualRpe = null
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
                                actualRpe = null
                            )
                            Log.d("WorkoutTemplateRepository", "    Creating SetLog: targetReps=$targetReps, targetWeight=$targetWeight, targetRpe=$targetRpe")
                            setLogDao.insertSetLog(setLog)
                        }
                    }
                }
                Log.d("WorkoutTemplateRepository", "  Created ${exerciseStructure.sets} sets")
            } else {
                Log.w("WorkoutTemplateRepository", "  Skipping exercise '${exerciseStructure.name}' - no matched exercise ID")
            }
        }
        
        Log.d("WorkoutTemplateRepository", "=== WORKOUT CREATION COMPLETE ===")
        Log.d("WorkoutTemplateRepository", "Workout ID: $workoutId with ${workoutStructure.exercises.size} exercises")
        
        workoutId
    }
}

data class NextProgrammeWorkoutInfo(
    val workoutStructure: com.github.radupana.featherweight.data.programme.WorkoutStructure,
    val actualWeekNumber: Int
)

