package com.github.radupana.featherweight.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Workout::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE,
        ),
        // Optional foreign key to Exercise - can be null for backward compatibility
        ForeignKey(
            entity = com.github.radupana.featherweight.data.exercise.Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
)
data class ExerciseLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val exerciseName: String, // Keep for backward compatibility
    val exerciseId: Long? = null, // Add optional reference to Exercise
    val exerciseOrder: Int,
    val supersetGroup: Int? = null,
    val notes: String? = null,
    val originalExerciseId: Long? = null, // Track original exercise if swapped
    val isSwapped: Boolean = false, // Flag to indicate if exercise was swapped
)
