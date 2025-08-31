package com.github.radupana.featherweight.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    indices = [
        Index(value = ["workoutId"]),
        Index(value = ["exerciseVariationId"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = Workout::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = com.github.radupana.featherweight.data.exercise.ExerciseVariation::class,
            parentColumns = ["id"],
            childColumns = ["exerciseVariationId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
)
data class ExerciseLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val exerciseVariationId: Long,
    val exerciseOrder: Int,
    val supersetGroup: Int? = null,
    val notes: String? = null,
    val originalVariationId: Long? = null,
    val isSwapped: Boolean = false, // Flag to indicate if exercise was swapped
)
