package com.github.radupana.featherweight.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = ExerciseLog::class,
            parentColumns = ["id"],
            childColumns = ["exerciseLogId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SetLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseLogId: Long,
    val setOrder: Int,
    val reps: Int,
    val weight: Float,
    val rpe: Float? = null,
    val tag: String? = null, // e.g. "warmup", "drop", "failure"
    val notes: String? = null
)
