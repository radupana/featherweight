package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.ExerciseLogDao
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.SetLogDao
import com.github.radupana.featherweight.data.WorkoutDao
import com.github.radupana.featherweight.data.programme.DeviationType
import com.github.radupana.featherweight.data.programme.ExerciseStructure
import com.github.radupana.featherweight.data.programme.ProgrammeDao
import com.github.radupana.featherweight.data.programme.ProgrammeStatus
import com.github.radupana.featherweight.data.programme.RepsStructure
import com.github.radupana.featherweight.data.programme.WorkoutDeviation
import com.github.radupana.featherweight.util.CloudLogger
import java.time.LocalDateTime

class DeviationCalculationService(
    private val workoutDao: WorkoutDao,
    private val exerciseLogDao: ExerciseLogDao,
    private val setLogDao: SetLogDao,
    private val programmeDao: ProgrammeDao,
) {
    companion object {
        private const val DEVIATION_THRESHOLD = 0.10f
    }

    suspend fun calculateDeviations(workoutId: String): List<WorkoutDeviation> {
        CloudLogger.debug("DeviationCalculationService", "Starting deviation calculation for workout: $workoutId")

        val workout = workoutDao.getWorkoutById(workoutId)
        if (workout == null) {
            CloudLogger.warn("DeviationCalculationService", "Workout not found: $workoutId")
            return emptyList()
        }

        if (!workout.isProgrammeWorkout || workout.programmeId == null) {
            CloudLogger.debug("DeviationCalculationService", "Workout is not a programme workout, skipping deviations")
            return emptyList()
        }

        CloudLogger.debug("DeviationCalculationService", "Workout is programme workout, programmeId: ${workout.programmeId}, week: ${workout.weekNumber}, day: ${workout.dayNumber}")

        val programme = programmeDao.getProgrammeById(workout.programmeId)
        if (programme == null) {
            CloudLogger.warn("DeviationCalculationService", "Programme not found: ${workout.programmeId}")
            return emptyList()
        }

        // Skip deviation calculation for cancelled programmes
        if (programme.status == ProgrammeStatus.CANCELLED) {
            CloudLogger.debug("DeviationCalculationService", "Programme status is CANCELLED, skipping deviation calculation")
            return emptyList()
        }

        val immutableSnapshot = programme.getImmutableProgrammeSnapshot()
        if (immutableSnapshot == null) {
            CloudLogger.warn("DeviationCalculationService", "No immutable programme snapshot found for programme ${workout.programmeId}")
            return emptyList()
        }

        CloudLogger.debug("DeviationCalculationService", "Found immutable programme snapshot, searching for week ${workout.weekNumber}, day ${workout.dayNumber}")

        val targetWeek = immutableSnapshot.weeks.find { it.weekNumber == workout.weekNumber }
        if (targetWeek == null) {
            CloudLogger.warn("DeviationCalculationService", "Week ${workout.weekNumber} not found in snapshot")
            return emptyList()
        }

        val targetWorkout = targetWeek.workouts.find { it.dayNumber == workout.dayNumber }
        if (targetWorkout == null) {
            CloudLogger.warn("DeviationCalculationService", "Day ${workout.dayNumber} not found in week ${workout.weekNumber}")
            return emptyList()
        }

        CloudLogger.debug("DeviationCalculationService", "Found prescribed workout: ${targetWorkout.workoutName}, exercises: ${targetWorkout.exercises.size}")

        val workoutExercises = targetWorkout.exercises

        val exerciseLogs = exerciseLogDao.getExerciseLogsForWorkout(workoutId).sortedBy { it.exerciseOrder }
        CloudLogger.debug("DeviationCalculationService", "Found ${exerciseLogs.size} exercise logs for workout")

        val deviations = mutableListOf<WorkoutDeviation>()
        val timestamp = LocalDateTime.now()

        val hasExerciseIds = workoutExercises.any { it.exerciseId != null }
        CloudLogger.debug("DeviationCalculationService", "Using ${if (hasExerciseIds) "ID-based" else "index-based"} exercise matching")

        if (hasExerciseIds) {
            CloudLogger.debug("DeviationCalculationService", "Prescribed exercises: ${workoutExercises.filter { it.exerciseId != null }.map { "${it.name} (${it.exerciseId})" }}")
            CloudLogger.debug("DeviationCalculationService", "Actual exercises: ${exerciseLogs.map { it.exerciseId }}")
            val prescribedExercisesById =
                workoutExercises
                    .filter { it.exerciseId != null }
                    .associateBy { it.exerciseId!! }

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

        CloudLogger.debug("DeviationCalculationService", "Calculated ${deviations.size} deviations: ${deviations.groupBy { it.deviationType }.mapValues { it.value.size }}")
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
        CloudLogger.debug("DeviationCalculationService", "Processing deviations for exercise: ${actualExercise.exerciseId}")

        if (actualExercise.isSwapped) {
            CloudLogger.debug("DeviationCalculationService", "Exercise was swapped")
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
        CloudLogger.debug("DeviationCalculationService", "Exercise has ${completedSets.size} completed sets out of ${actualSets.size} total sets")

        if (completedSets.isEmpty()) {
            CloudLogger.debug("DeviationCalculationService", "No completed sets, skipping deviation calculations")
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
        val targetWeights = targetExercise.weights
        if (targetWeights == null) {
            CloudLogger.debug("DeviationCalculationService", "No target weights, skipping volume deviation")
            return null
        }

        val targetReps = getTargetRepsPerSet(targetExercise.reps, targetExercise.sets)

        if (targetWeights.size != targetReps.size) {
            CloudLogger.warn("DeviationCalculationService", "Target weights size (${targetWeights.size}) != target reps size (${targetReps.size}), skipping volume deviation")
            return null
        }

        val targetVolume = targetWeights.zip(targetReps).sumOf { (weight, reps) -> (weight * reps).toDouble() }
        val actualVolume = completedSets.sumOf { (it.actualWeight * it.actualReps).toDouble() }

        if (targetVolume == 0.0) {
            CloudLogger.debug("DeviationCalculationService", "Target volume is 0, skipping volume deviation")
            return null
        }

        val deviationMagnitude = ((actualVolume - targetVolume) / targetVolume).toFloat()
        CloudLogger.debug("DeviationCalculationService", "Volume deviation: target=$targetVolume, actual=$actualVolume, magnitude=$deviationMagnitude (threshold=$DEVIATION_THRESHOLD)")

        return if (kotlin.math.abs(deviationMagnitude) > DEVIATION_THRESHOLD) {
            CloudLogger.debug("DeviationCalculationService", "Volume deviation exceeds threshold, recording")
            WorkoutDeviation(
                workoutId = workoutId,
                programmeId = programmeId,
                exerciseLogId = exerciseLogId,
                deviationType = DeviationType.VOLUME_DEVIATION,
                deviationMagnitude = deviationMagnitude,
                timestamp = timestamp,
            )
        } else {
            CloudLogger.debug("DeviationCalculationService", "Volume deviation within threshold")
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
        val targetWeights = targetExercise.weights
        if (targetWeights == null) {
            CloudLogger.debug("DeviationCalculationService", "No target weights, skipping intensity deviation")
            return null
        }

        val targetAvgWeight = targetWeights.average().toFloat()
        val actualAvgWeight = completedSets.map { it.actualWeight }.average().toFloat()

        if (targetAvgWeight == 0f) {
            CloudLogger.debug("DeviationCalculationService", "Target average weight is 0, skipping intensity deviation")
            return null
        }

        val deviationMagnitude = (actualAvgWeight - targetAvgWeight) / targetAvgWeight
        CloudLogger.debug("DeviationCalculationService", "Intensity deviation: targetAvg=$targetAvgWeight, actualAvg=$actualAvgWeight, magnitude=$deviationMagnitude")

        return if (kotlin.math.abs(deviationMagnitude) > DEVIATION_THRESHOLD) {
            CloudLogger.debug("DeviationCalculationService", "Intensity deviation exceeds threshold, recording")
            WorkoutDeviation(
                workoutId = workoutId,
                programmeId = programmeId,
                exerciseLogId = exerciseLogId,
                deviationType = DeviationType.INTENSITY_DEVIATION,
                deviationMagnitude = deviationMagnitude,
                timestamp = timestamp,
            )
        } else {
            CloudLogger.debug("DeviationCalculationService", "Intensity deviation within threshold")
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
        if (targetSets == 0) {
            CloudLogger.debug("DeviationCalculationService", "Target sets is 0, skipping set count deviation")
            return null
        }

        val deviationMagnitude = (actualSetCount - targetSets).toFloat() / targetSets
        CloudLogger.debug("DeviationCalculationService", "Set count deviation: target=$targetSets, actual=$actualSetCount, magnitude=$deviationMagnitude")

        return if (kotlin.math.abs(deviationMagnitude) > DEVIATION_THRESHOLD) {
            CloudLogger.debug("DeviationCalculationService", "Set count deviation exceeds threshold, recording")
            WorkoutDeviation(
                workoutId = workoutId,
                programmeId = programmeId,
                exerciseLogId = exerciseLogId,
                deviationType = DeviationType.SET_COUNT_DEVIATION,
                deviationMagnitude = deviationMagnitude,
                timestamp = timestamp,
            )
        } else {
            CloudLogger.debug("DeviationCalculationService", "Set count deviation within threshold")
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

        if (targetTotalReps == 0) {
            CloudLogger.debug("DeviationCalculationService", "Target total reps is 0, skipping rep deviation")
            return null
        }

        val deviationMagnitude = (actualTotalReps - targetTotalReps).toFloat() / targetTotalReps
        CloudLogger.debug("DeviationCalculationService", "Rep deviation: target=$targetTotalReps, actual=$actualTotalReps, magnitude=$deviationMagnitude")

        return if (kotlin.math.abs(deviationMagnitude) > DEVIATION_THRESHOLD) {
            CloudLogger.debug("DeviationCalculationService", "Rep deviation exceeds threshold, recording")
            WorkoutDeviation(
                workoutId = workoutId,
                programmeId = programmeId,
                exerciseLogId = exerciseLogId,
                deviationType = DeviationType.REP_DEVIATION,
                deviationMagnitude = deviationMagnitude,
                timestamp = timestamp,
            )
        } else {
            CloudLogger.debug("DeviationCalculationService", "Rep deviation within threshold")
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
        val targetRpeValues = targetExercise.rpeValues
        if (targetRpeValues == null) {
            CloudLogger.debug("DeviationCalculationService", "No target RPE values, skipping RPE deviation")
            return null
        }

        val validTargetRpes = targetRpeValues.filterNotNull()
        if (validTargetRpes.isEmpty()) {
            CloudLogger.debug("DeviationCalculationService", "No valid target RPE values, skipping RPE deviation")
            return null
        }

        val targetAvgRpe = validTargetRpes.average().toFloat()

        val actualRpes = completedSets.mapNotNull { it.actualRpe }
        if (actualRpes.isEmpty()) {
            CloudLogger.debug("DeviationCalculationService", "No actual RPE values, skipping RPE deviation")
            return null
        }

        val actualAvgRpe = actualRpes.average().toFloat()

        if (targetAvgRpe == 0f) {
            CloudLogger.debug("DeviationCalculationService", "Target average RPE is 0, skipping RPE deviation")
            return null
        }

        val deviationMagnitude = (actualAvgRpe - targetAvgRpe) / targetAvgRpe
        CloudLogger.debug("DeviationCalculationService", "RPE deviation: targetAvg=$targetAvgRpe, actualAvg=$actualAvgRpe, magnitude=$deviationMagnitude")

        return if (kotlin.math.abs(deviationMagnitude) > DEVIATION_THRESHOLD) {
            CloudLogger.debug("DeviationCalculationService", "RPE deviation exceeds threshold, recording")
            WorkoutDeviation(
                workoutId = workoutId,
                programmeId = programmeId,
                exerciseLogId = exerciseLogId,
                deviationType = DeviationType.RPE_DEVIATION,
                deviationMagnitude = deviationMagnitude,
                timestamp = timestamp,
            )
        } else {
            CloudLogger.debug("DeviationCalculationService", "RPE deviation within threshold")
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
