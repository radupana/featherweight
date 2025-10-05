package com.github.radupana.featherweight.data.profile

import java.time.LocalDateTime

data class OneRMWithExerciseName(
    val id: String,
    val exerciseId: String,
    val exerciseName: String,
    val oneRMEstimate: Float,
    val oneRMDate: LocalDateTime,
    val oneRMContext: String,
    val mostWeightLifted: Float,
    val mostWeightReps: Int,
    val mostWeightRpe: Float?,
    val mostWeightDate: LocalDateTime,
    val oneRMConfidence: Float,
    val oneRMType: OneRMType,
    val notes: String?,
    val sessionCount: Int = 0,
)

data class Big4ExerciseWithOptionalMax(
    val id: String,
    val exerciseId: String,
    val exerciseName: String,
    val oneRMEstimate: Float?,
    val oneRMDate: LocalDateTime?,
    val oneRMContext: String?,
    val mostWeightLifted: Float?,
    val mostWeightReps: Int?,
    val mostWeightRpe: Float?,
    val mostWeightDate: LocalDateTime?,
    val oneRMConfidence: Float?,
    val oneRMType: OneRMType?,
    val notes: String?,
    val sessionCount: Int = 0,
)

data class OneRMHistoryWithName(
    val id: String,
    val exerciseId: String,
    val exerciseName: String,
    val oneRMEstimate: Float,
    val context: String,
    val recordedAt: LocalDateTime,
)

data class UserExerciseMaxWithName(
    val id: String,
    val exerciseId: String,
    val exerciseName: String,
    val mostWeightLifted: Float,
    val mostWeightReps: Int,
    val mostWeightRpe: Float?,
    val mostWeightDate: LocalDateTime,
    val oneRMEstimate: Float,
    val oneRMContext: String,
    val oneRMConfidence: Float,
    val oneRMDate: LocalDateTime,
    val oneRMType: OneRMType,
    val notes: String?,
)
