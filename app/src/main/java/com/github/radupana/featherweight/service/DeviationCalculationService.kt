package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.ExerciseLogDao
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.SetLogDao
import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.data.WorkoutDao
import com.github.radupana.featherweight.data.programme.DeviationType
import com.github.radupana.featherweight.data.programme.ExerciseStructure
import com.github.radupana.featherweight.data.programme.ProgrammeDao
import com.github.radupana.featherweight.data.programme.ProgrammeStatus
import com.github.radupana.featherweight.data.programme.RepsStructure
import com.github.radupana.featherweight.data.programme.WorkoutDeviation
import com.github.radupana.featherweight.data.programme.WorkoutSnapshot
import com.github.radupana.featherweight.util.CloudLogger
import java.time.LocalDateTime
import kotlin.math.abs

class DeviationCalculationService(
    private val workoutDao: WorkoutDao,
    private val exerciseLogDao: ExerciseLogDao,
    private val setLogDao: SetLogDao,
    private val programmeDao: ProgrammeDao,
) {
    companion object {
        private const val DEVIATION_THRESHOLD = 0.10f
        private const val FLOAT_EPSILON = 0.0001f
    }

    suspend fun calculateDeviations(workoutId: String): List<WorkoutDeviation> {
        val workout =
            workoutDao.getWorkoutById(workoutId) ?: run {
                CloudLogger.warn("DeviationCalculationService", "Workout not found: $workoutId")
                return emptyList()
            }

        if (!workout.isProgrammeWorkout || workout.programmeId == null) {
            return emptyList()
        }

        val targetWorkout = findTargetWorkout(workout) ?: return emptyList()

        val workoutExercises = targetWorkout.exercises
        val exerciseLogs = exerciseLogDao.getExerciseLogsForWorkout(workoutId).sortedBy { it.exerciseOrder }
        val deviations = mutableListOf<WorkoutDeviation>()
        val timestamp = LocalDateTime.now()
        val hasExerciseIds = workoutExercises.any { it.exerciseId != null }

        if (hasExerciseIds) {
            val prescribedExercisesById =
                workoutExercises
                    .mapNotNull { exercise -> exercise.exerciseId?.let { id -> id to exercise } }
                    .toMap()

            val actualExercisesById = exerciseLogs.associateBy { it.exerciseId }

            prescribedExercisesById.forEach { (exerciseId, targetExercise) ->
                val actualExercise = actualExercisesById[exerciseId]

                if (actualExercise == null) {
                    deviations.add(
                        WorkoutDeviation(
                            workoutId = workoutId,
                            programmeId = workout.programmeId,
                            exerciseLogId = null,
                            deviationType = DeviationType.EXERCISE_SKIPPED,
                            deviationMagnitude = 1.0f,
                            notes = targetExercise.name,
                            timestamp = timestamp,
                        ),
                    )
                    return@forEach
                }

                processExerciseDeviations(
                    targetExercise = targetExercise,
                    actualExercise = actualExercise,
                    workoutId = workoutId,
                    programmeId = workout.programmeId,
                    timestamp = timestamp,
                    deviations = deviations,
                )
            }

            actualExercisesById.forEach { (exerciseId, actualExercise) ->
                if (!prescribedExercisesById.containsKey(exerciseId)) {
                    deviations.add(
                        WorkoutDeviation(
                            workoutId = workoutId,
                            programmeId = workout.programmeId,
                            exerciseLogId = actualExercise.id,
                            deviationType = DeviationType.EXERCISE_ADDED,
                            deviationMagnitude = 1.0f,
                            timestamp = timestamp,
                        ),
                    )
                }
            }
        } else {
            workoutExercises.forEachIndexed { index, targetExercise ->
                val actualExercise = exerciseLogs.getOrNull(index)

                if (actualExercise == null) {
                    deviations.add(
                        WorkoutDeviation(
                            workoutId = workoutId,
                            programmeId = workout.programmeId,
                            exerciseLogId = null,
                            deviationType = DeviationType.EXERCISE_SKIPPED,
                            deviationMagnitude = 1.0f,
                            notes = targetExercise.name,
                            timestamp = timestamp,
                        ),
                    )
                    return@forEachIndexed
                }

                processExerciseDeviations(
                    targetExercise = targetExercise,
                    actualExercise = actualExercise,
                    workoutId = workoutId,
                    programmeId = workout.programmeId,
                    timestamp = timestamp,
                    deviations = deviations,
                )
            }
        }

        return deviations
    }

    private suspend fun processExerciseDeviations(
        targetExercise: ExerciseStructure,
        actualExercise: com.github.radupana.featherweight.data.ExerciseLog,
        workoutId: String,
        programmeId: String,
        timestamp: LocalDateTime,
        deviations: MutableList<WorkoutDeviation>,
    ) {
        if (actualExercise.isSwapped) {
            deviations.add(
                WorkoutDeviation(
                    workoutId = workoutId,
                    programmeId = programmeId,
                    exerciseLogId = actualExercise.id,
                    deviationType = DeviationType.EXERCISE_SWAP,
                    deviationMagnitude = 1.0f,
                    timestamp = timestamp,
                ),
            )
        }

        val actualSets = setLogDao.getSetLogsForExercise(actualExercise.id)
        val completedSets = actualSets.filter { it.isCompleted }

        if (completedSets.isEmpty()) {
            return
        }

        calculateVolumeDeviation(
            targetExercise = targetExercise,
            completedSets = completedSets,
            workoutId = workoutId,
            programmeId = programmeId,
            exerciseLogId = actualExercise.id,
            timestamp = timestamp,
        )?.let { deviations.add(it) }

        calculateIntensityDeviation(
            targetExercise = targetExercise,
            completedSets = completedSets,
            workoutId = workoutId,
            programmeId = programmeId,
            exerciseLogId = actualExercise.id,
            timestamp = timestamp,
        )?.let { deviations.add(it) }

        calculateSetCountDeviation(
            targetSets = targetExercise.sets,
            actualSetCount = completedSets.size,
            workoutId = workoutId,
            programmeId = programmeId,
            exerciseLogId = actualExercise.id,
            timestamp = timestamp,
        )?.let { deviations.add(it) }

        calculateRepDeviation(
            targetExercise = targetExercise,
            completedSets = completedSets,
            workoutId = workoutId,
            programmeId = programmeId,
            exerciseLogId = actualExercise.id,
            timestamp = timestamp,
        )?.let { deviations.add(it) }

        calculateRpeDeviation(
            targetExercise = targetExercise,
            completedSets = completedSets,
            workoutId = workoutId,
            programmeId = programmeId,
            exerciseLogId = actualExercise.id,
            timestamp = timestamp,
        )?.let { deviations.add(it) }
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

        if (targetWeights.size != targetReps.size) {
            CloudLogger.warn("DeviationCalculationService", "Target weights size (${targetWeights.size}) != target reps size (${targetReps.size}), skipping volume deviation")
            return null
        }

        val targetVolume = targetWeights.zip(targetReps).sumOf { (weight, reps) -> (weight * reps).toDouble() }
        val actualVolume = completedSets.sumOf { (it.actualWeight * it.actualReps).toDouble() }

        if (targetVolume == 0.0) return null

        val deviationMagnitude = ((actualVolume - targetVolume) / targetVolume).toFloat()

        return if (abs(deviationMagnitude) > DEVIATION_THRESHOLD) {
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

        if (abs(targetAvgWeight) < FLOAT_EPSILON) return null

        val deviationMagnitude = (actualAvgWeight - targetAvgWeight) / targetAvgWeight

        return if (abs(deviationMagnitude) > DEVIATION_THRESHOLD) {
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

        return if (abs(deviationMagnitude) > DEVIATION_THRESHOLD) {
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

        return if (abs(deviationMagnitude) > DEVIATION_THRESHOLD) {
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
        if (abs(targetAvgRpe) < FLOAT_EPSILON) return null

        val deviationMagnitude = (actualAvgRpe - targetAvgRpe) / targetAvgRpe

        return if (abs(deviationMagnitude) > DEVIATION_THRESHOLD) {
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

    // Early returns for validation improve readability over nested conditionals
    @Suppress("ReturnCount")
    private suspend fun findTargetWorkout(workout: Workout): WorkoutSnapshot? {
        val programmeId = workout.programmeId ?: return null
        val programme =
            programmeDao.getProgrammeById(programmeId) ?: run {
                CloudLogger.warn("DeviationCalculationService", "Programme not found: $programmeId")
                return null
            }

        if (programme.status == ProgrammeStatus.CANCELLED) return null

        val immutableSnapshot =
            programme.getImmutableProgrammeSnapshot() ?: run {
                CloudLogger.warn("DeviationCalculationService", "No immutable programme snapshot found for programme ${programme.id}")
                return null
            }

        val targetWeek =
            immutableSnapshot.weeks.find { it.weekNumber == workout.weekNumber } ?: run {
                CloudLogger.warn("DeviationCalculationService", "Week ${workout.weekNumber} not found in snapshot")
                return null
            }

        return targetWeek.workouts.find { it.dayNumber == workout.dayNumber } ?: run {
            CloudLogger.warn("DeviationCalculationService", "Day ${workout.dayNumber} not found in week ${workout.weekNumber}")
            null
        }
    }
}
