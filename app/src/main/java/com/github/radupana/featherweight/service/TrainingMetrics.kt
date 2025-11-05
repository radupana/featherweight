package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.exercise.Exercise

data class VolumeMetrics(
    val totalSets: Int,
    val totalCompletedSets: Int,
    val compoundSets: Int,
    val isolationSets: Int,
    val pushSets: Int,
    val pullSets: Int,
    val squatSets: Int,
    val hingeSets: Int,
    val setsByCategory: Map<String, Int>,
)

data class IntensityMetrics(
    val avgRpe: Float,
    val setsWithRpe: Int,
    val setsAboveRpe8: Int,
    val setsBelowRpe6: Int,
)

data class ExerciseProgression(
    val exerciseId: String,
    val exerciseName: String,
    val sessions: List<SessionData>,
    val isProgressing: Boolean,
    val isPlateaued: Boolean,
)

data class SessionData(
    val date: String,
    val maxWeight: Float,
    val totalVolume: Float,
)

data class TrainingMetrics(
    val volumeMetrics: VolumeMetrics,
    val intensityMetrics: IntensityMetrics,
    val progressionMetrics: List<ExerciseProgression>,
)

object TrainingMetricsCalculator {
    fun calculateVolumeMetrics(
        exercises: Map<String, Exercise>,
        setsByExercise: Map<String, List<SetLog>>,
    ): VolumeMetrics {
        var totalSets = 0
        var totalCompletedSets = 0
        var compoundSets = 0
        var isolationSets = 0
        var pushSets = 0
        var pullSets = 0
        var squatSets = 0
        var hingeSets = 0
        val setsByCategory = mutableMapOf<String, Int>()

        setsByExercise.forEach { (exerciseId, sets) ->
            val exercise = exercises[exerciseId]
            val completedSets = sets.filter { it.isCompleted }

            totalSets += sets.size
            totalCompletedSets += completedSets.size

            if (exercise != null) {
                if (exercise.isCompound) {
                    compoundSets += completedSets.size
                } else {
                    isolationSets += completedSets.size
                }

                when (exercise.movementPattern) {
                    "PUSH", "VERTICAL_PUSH", "HORIZONTAL_PUSH" -> pushSets += completedSets.size
                    "PULL", "VERTICAL_PULL", "HORIZONTAL_PULL" -> pullSets += completedSets.size
                    "SQUAT" -> squatSets += completedSets.size
                    "HINGE" -> hingeSets += completedSets.size
                }

                setsByCategory[exercise.category] =
                    setsByCategory.getOrDefault(exercise.category, 0) + completedSets.size
            }
        }

        return VolumeMetrics(
            totalSets = totalSets,
            totalCompletedSets = totalCompletedSets,
            compoundSets = compoundSets,
            isolationSets = isolationSets,
            pushSets = pushSets,
            pullSets = pullSets,
            squatSets = squatSets,
            hingeSets = hingeSets,
            setsByCategory = setsByCategory,
        )
    }

    fun calculateIntensityMetrics(sets: List<SetLog>): IntensityMetrics {
        val completedSets = sets.filter { it.isCompleted }
        val setsWithRpe = completedSets.filter { it.actualRpe != null }

        val avgRpe = if (setsWithRpe.isNotEmpty()) {
            setsWithRpe.mapNotNull { it.actualRpe }.average().toFloat()
        } else {
            0f
        }

        val setsAboveRpe8 = setsWithRpe.count { (it.actualRpe ?: 0f) > 8f }
        val setsBelowRpe6 = setsWithRpe.count { (it.actualRpe ?: 0f) < 6f }

        return IntensityMetrics(
            avgRpe = avgRpe,
            setsWithRpe = setsWithRpe.size,
            setsAboveRpe8 = setsAboveRpe8,
            setsBelowRpe6 = setsBelowRpe6,
        )
    }

    fun calculateProgressionMetrics(
        exercises: Map<String, Exercise>,
        workoutSessions: List<WorkoutSessionData>,
    ): List<ExerciseProgression> {
        val exerciseMap = mutableMapOf<String, MutableList<SessionData>>()

        workoutSessions.forEach { session ->
            session.exerciseData.forEach { (exerciseId, maxWeight, totalVolume) ->
                exerciseMap.getOrPut(exerciseId) { mutableListOf() }
                    .add(SessionData(session.date, maxWeight, totalVolume))
            }
        }

        return exerciseMap.mapNotNull { (exerciseId, sessions) ->
            val exercise = exercises[exerciseId] ?: return@mapNotNull null
            if (sessions.size < 2) return@mapNotNull null

            val sortedSessions = sessions.sortedBy { it.date }
            val isProgressing = detectProgression(sortedSessions)
            val isPlateaued = detectPlateau(sortedSessions)

            ExerciseProgression(
                exerciseId = exerciseId,
                exerciseName = exercise.name,
                sessions = sortedSessions,
                isProgressing = isProgressing,
                isPlateaued = isPlateaued,
            )
        }
    }

    private fun detectProgression(sessions: List<SessionData>): Boolean {
        if (sessions.size < 2) return false
        val recent = sessions.takeLast(3)
        if (recent.size < 2) return false

        val firstWeight = recent.first().maxWeight
        val lastWeight = recent.last().maxWeight

        return lastWeight > firstWeight
    }

    private fun detectPlateau(sessions: List<SessionData>): Boolean {
        if (sessions.size < 3) return false
        val recent = sessions.takeLast(3)
        if (recent.size < 3) return false

        val weights = recent.map { it.maxWeight }
        val allSame = weights.distinct().size == 1

        return allSame && weights.first() > 0f
    }
}

data class WorkoutSessionData(
    val date: String,
    val exerciseData: List<ExerciseSessionData>,
)

data class ExerciseSessionData(
    val exerciseId: String,
    val maxWeight: Float,
    val totalVolume: Float,
)
