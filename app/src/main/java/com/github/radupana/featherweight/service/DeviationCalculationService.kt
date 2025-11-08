package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.ExerciseLogDao
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.SetLogDao
import com.github.radupana.featherweight.data.WorkoutDao
import com.github.radupana.featherweight.data.programme.DeviationType
import com.github.radupana.featherweight.data.programme.ExerciseStructure
import com.github.radupana.featherweight.data.programme.ProgrammeDao
import com.github.radupana.featherweight.data.programme.RepsStructure
import com.github.radupana.featherweight.data.programme.WorkoutDeviation
import com.github.radupana.featherweight.data.programme.WorkoutStructure
import kotlinx.serialization.json.Json
import java.time.LocalDateTime

class DeviationCalculationService(
    private val workoutDao: WorkoutDao,
    private val exerciseLogDao: ExerciseLogDao,
    private val setLogDao: SetLogDao,
    private val programmeDao: ProgrammeDao,
) {
    companion object {
        private const val DEVIATION_THRESHOLD = 0.05f
    }

    suspend fun calculateDeviations(workoutId: String): List<WorkoutDeviation> {
        val workout = workoutDao.getWorkoutById(workoutId) ?: return emptyList()

        if (!workout.isProgrammeWorkout || workout.programmeId == null) {
            return emptyList()
        }

        val programmeWorkouts = programmeDao.getAllWorkoutsForProgramme(workout.programmeId)
        val targetWorkout =
            programmeWorkouts.find { programmeWorkout ->
                val week = programmeDao.getWeekById(programmeWorkout.weekId)
                week?.weekNumber == workout.weekNumber && programmeWorkout.dayNumber == workout.dayNumber
            } ?: return emptyList()

        val workoutStructure =
            try {
                Json.decodeFromString<WorkoutStructure>(targetWorkout.workoutStructure)
            } catch (e: kotlinx.serialization.SerializationException) {
                return emptyList()
            }

        val exerciseLogs = exerciseLogDao.getExerciseLogsForWorkout(workoutId)
        val deviations = mutableListOf<WorkoutDeviation>()
        val timestamp = LocalDateTime.now()

        workoutStructure.exercises.forEachIndexed { index, targetExercise ->
            val actualExercise = exerciseLogs.getOrNull(index) ?: return@forEachIndexed

            if (actualExercise.isSwapped) {
                deviations.add(
                    WorkoutDeviation(
                        workoutId = workoutId,
                        programmeId = workout.programmeId,
                        exerciseLogId = actualExercise.id,
                        deviationType = DeviationType.EXERCISE_SWAP,
                        deviationMagnitude = 1.0f,
                        timestamp = timestamp,
                    ),
                )
            }

            val actualSets = setLogDao.getSetLogsForExercise(actualExercise.id)
            val completedSets = actualSets.filter { it.isCompleted }

            if (completedSets.isEmpty()) return@forEachIndexed

            calculateVolumeDeviation(
                targetExercise = targetExercise,
                completedSets = completedSets,
                workoutId = workoutId,
                programmeId = workout.programmeId,
                exerciseLogId = actualExercise.id,
                timestamp = timestamp,
            )?.let { deviations.add(it) }

            calculateIntensityDeviation(
                targetExercise = targetExercise,
                completedSets = completedSets,
                workoutId = workoutId,
                programmeId = workout.programmeId,
                exerciseLogId = actualExercise.id,
                timestamp = timestamp,
            )?.let { deviations.add(it) }

            calculateSetCountDeviation(
                targetSets = targetExercise.sets,
                actualSetCount = completedSets.size,
                workoutId = workoutId,
                programmeId = workout.programmeId,
                exerciseLogId = actualExercise.id,
                timestamp = timestamp,
            )?.let { deviations.add(it) }

            calculateRepDeviation(
                targetExercise = targetExercise,
                completedSets = completedSets,
                workoutId = workoutId,
                programmeId = workout.programmeId,
                exerciseLogId = actualExercise.id,
                timestamp = timestamp,
            )?.let { deviations.add(it) }

            calculateRpeDeviation(
                targetExercise = targetExercise,
                completedSets = completedSets,
                workoutId = workoutId,
                programmeId = workout.programmeId,
                exerciseLogId = actualExercise.id,
                timestamp = timestamp,
            )?.let { deviations.add(it) }
        }

        return deviations
    }

    private fun calculateVolumeDeviation(
        targetExercise: ExerciseStructure,
        completedSets: List<SetLog>,
        workoutId: String,
        programmeId: String,
        exerciseLogId: String,
        timestamp: LocalDateTime,
    ): WorkoutDeviation? {
        val targetWeights = targetExercise.weights ?: return null
        val targetReps = getTargetRepsPerSet(targetExercise.reps, targetExercise.sets)

        if (targetWeights.size != targetReps.size) return null

        val targetVolume = targetWeights.zip(targetReps).sumOf { (weight, reps) -> (weight * reps).toDouble() }
        val actualVolume = completedSets.sumOf { (it.actualWeight * it.actualReps).toDouble() }

        if (targetVolume == 0.0) return null

        val deviationMagnitude = ((actualVolume - targetVolume) / targetVolume).toFloat()

        return if (kotlin.math.abs(deviationMagnitude) > DEVIATION_THRESHOLD) {
            WorkoutDeviation(
                workoutId = workoutId,
                programmeId = programmeId,
                exerciseLogId = exerciseLogId,
                deviationType = DeviationType.VOLUME_DEVIATION,
                deviationMagnitude = deviationMagnitude,
                timestamp = timestamp,
            )
        } else {
            null
        }
    }

    private fun calculateIntensityDeviation(
        targetExercise: ExerciseStructure,
        completedSets: List<SetLog>,
        workoutId: String,
        programmeId: String,
        exerciseLogId: String,
        timestamp: LocalDateTime,
    ): WorkoutDeviation? {
        val targetWeights = targetExercise.weights ?: return null
        val targetAvgWeight = targetWeights.average().toFloat()
        val actualAvgWeight = completedSets.map { it.actualWeight }.average().toFloat()

        if (targetAvgWeight == 0f) return null

        val deviationMagnitude = (actualAvgWeight - targetAvgWeight) / targetAvgWeight

        return if (kotlin.math.abs(deviationMagnitude) > DEVIATION_THRESHOLD) {
            WorkoutDeviation(
                workoutId = workoutId,
                programmeId = programmeId,
                exerciseLogId = exerciseLogId,
                deviationType = DeviationType.INTENSITY_DEVIATION,
                deviationMagnitude = deviationMagnitude,
                timestamp = timestamp,
            )
        } else {
            null
        }
    }

    private fun calculateSetCountDeviation(
        targetSets: Int,
        actualSetCount: Int,
        workoutId: String,
        programmeId: String,
        exerciseLogId: String,
        timestamp: LocalDateTime,
    ): WorkoutDeviation? {
        if (targetSets == 0) return null

        val deviationMagnitude = (actualSetCount - targetSets).toFloat() / targetSets

        return if (kotlin.math.abs(deviationMagnitude) > DEVIATION_THRESHOLD) {
            WorkoutDeviation(
                workoutId = workoutId,
                programmeId = programmeId,
                exerciseLogId = exerciseLogId,
                deviationType = DeviationType.SET_COUNT_DEVIATION,
                deviationMagnitude = deviationMagnitude,
                timestamp = timestamp,
            )
        } else {
            null
        }
    }

    private fun calculateRepDeviation(
        targetExercise: ExerciseStructure,
        completedSets: List<SetLog>,
        workoutId: String,
        programmeId: String,
        exerciseLogId: String,
        timestamp: LocalDateTime,
    ): WorkoutDeviation? {
        val targetReps = getTargetRepsPerSet(targetExercise.reps, targetExercise.sets)
        val targetTotalReps = targetReps.sum()
        val actualTotalReps = completedSets.sumOf { it.actualReps }

        if (targetTotalReps == 0) return null

        val deviationMagnitude = (actualTotalReps - targetTotalReps).toFloat() / targetTotalReps

        return if (kotlin.math.abs(deviationMagnitude) > DEVIATION_THRESHOLD) {
            WorkoutDeviation(
                workoutId = workoutId,
                programmeId = programmeId,
                exerciseLogId = exerciseLogId,
                deviationType = DeviationType.REP_DEVIATION,
                deviationMagnitude = deviationMagnitude,
                timestamp = timestamp,
            )
        } else {
            null
        }
    }

    private fun calculateRpeDeviation(
        targetExercise: ExerciseStructure,
        completedSets: List<SetLog>,
        workoutId: String,
        programmeId: String,
        exerciseLogId: String,
        timestamp: LocalDateTime,
    ): WorkoutDeviation? {
        val targetRpeValues = targetExercise.rpeValues ?: return null
        val validTargetRpes = targetRpeValues.filterNotNull()
        if (validTargetRpes.isEmpty()) return null

        val targetAvgRpe = validTargetRpes.average().toFloat()

        val actualRpes = completedSets.mapNotNull { it.actualRpe }
        if (actualRpes.isEmpty()) return null

        val actualAvgRpe = actualRpes.average().toFloat()

        if (targetAvgRpe == 0f) return null

        val deviationMagnitude = (actualAvgRpe - targetAvgRpe) / targetAvgRpe

        return if (kotlin.math.abs(deviationMagnitude) > DEVIATION_THRESHOLD) {
            WorkoutDeviation(
                workoutId = workoutId,
                programmeId = programmeId,
                exerciseLogId = exerciseLogId,
                deviationType = DeviationType.RPE_DEVIATION,
                deviationMagnitude = deviationMagnitude,
                timestamp = timestamp,
            )
        } else {
            null
        }
    }

    private fun getTargetRepsPerSet(
        repsStructure: RepsStructure,
        sets: Int,
    ): List<Int> =
        when (repsStructure) {
            is RepsStructure.Single -> List(sets) { repsStructure.value }
            is RepsStructure.Range -> List(sets) { repsStructure.min }
            is RepsStructure.RangeString -> {
                val min =
                    repsStructure.value
                        .split("-")
                        .firstOrNull()
                        ?.toIntOrNull() ?: 0
                List(sets) { min }
            }
            is RepsStructure.PerSet -> {
                val parsedValues =
                    repsStructure.values.map { repString ->
                        when {
                            repString.endsWith("+") -> repString.dropLast(1).toIntOrNull() ?: 1
                            else -> repString.toIntOrNull() ?: 5
                        }
                    }
                if (parsedValues.size >= sets) {
                    parsedValues.take(sets)
                } else {
                    parsedValues + List(sets - parsedValues.size) { parsedValues.lastOrNull() ?: 0 }
                }
            }
        }
}
