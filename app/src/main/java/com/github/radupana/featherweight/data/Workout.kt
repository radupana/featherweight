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
    ],
    indices = [
        Index("programmeId"),
        Index(value = ["status", "date"]),
        Index(value = ["isProgrammeWorkout", "status", "date"]),
    ],
)
data class Workout(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDateTime,
    val name: String? = null, // Custom workout name (separate from notes)
    val notes: String? = null,
    val notesUpdatedAt: LocalDateTime? = null,
    // Programme Integration Fields
    val programmeId: Long? = null, // Links to Programme table (null for freestyle workouts)
    val weekNumber: Int? = null, // Which week in the programme (1-based)
    val dayNumber: Int? = null, // Which day in the week (1-7)
    val programmeWorkoutName: String? = null, // Name of the programme workout for display
    val isProgrammeWorkout: Boolean = false, // Quick flag to distinguish programme vs freestyle
    // Workout Status
    val status: WorkoutStatus = WorkoutStatus.NOT_STARTED, // Tracks workout completion state
    val durationSeconds: Long? = null, // Total workout duration in seconds
    // Timer fields
    val timerStartTime: LocalDateTime? = null, // When the timer started (first set completed)
    val timerElapsedSeconds: Int = 0, // For persistence during app restarts
)
