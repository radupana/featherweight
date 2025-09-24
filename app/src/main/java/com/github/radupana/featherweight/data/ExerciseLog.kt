package com.github.radupana.featherweight.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercise_logs",
    indices = [
        Index(value = ["workoutId"]),
        Index(value = ["exerciseVariationId"]),
        Index("userId"),
    ],
    foreignKeys = [
        ForeignKey(
            entity = Workout::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ExerciseLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String? = null,
    val workoutId: Long,
    val exerciseVariationId: Long, // Can reference either system or custom exercise
    val isCustomExercise: Boolean = false, // true = custom exercise, false = system exercise
    val exerciseOrder: Int,
    val supersetGroup: Int? = null,
    val notes: String? = null,
    val originalVariationId: Long? = null,
    val originalIsCustom: Boolean = false, // true if original was custom exercise
    val isSwapped: Boolean = false, // Flag to indicate if exercise was swapped
)
