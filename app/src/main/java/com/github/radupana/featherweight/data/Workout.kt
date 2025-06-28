package com.github.radupana.featherweight.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = com.github.radupana.featherweight.data.programme.Programme::class,
            parentColumns = ["id"],
            childColumns = ["programmeId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = com.github.radupana.featherweight.data.programme.ProgrammeWorkout::class,
            parentColumns = ["id"],
            childColumns = ["programmeWorkoutId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("programmeId"),
        Index("programmeWorkoutId"),
    ],
)
data class Workout(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDateTime,
    val notes: String? = null,
    // Programme Integration Fields
    val programmeId: Long? = null, // Links to Programme table (null for freestyle workouts)
    val programmeWorkoutId: Long? = null, // Links to ProgrammeWorkout template
    val weekNumber: Int? = null, // Which week in the programme (1-based)
    val dayNumber: Int? = null, // Which day in the week (1-7)
    val programmeWorkoutName: String? = null, // Name of the programme workout for display
    val isProgrammeWorkout: Boolean = false, // Quick flag to distinguish programme vs freestyle
    // Workout Timer
    val durationSeconds: Long? = null, // Total workout duration in seconds
)
