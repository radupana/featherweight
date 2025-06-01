package com.github.radupana.featherweight.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity
data class Workout(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDateTime
)

@Entity(
    foreignKeys = [ForeignKey(
        entity = Workout::class,
        parentColumns = ["id"],
        childColumns = ["workoutId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class ExerciseLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val name: String,
    val sets: Int,
    val reps: Int,
    val weight: Float
    // Add RPE (optional) later!
)
