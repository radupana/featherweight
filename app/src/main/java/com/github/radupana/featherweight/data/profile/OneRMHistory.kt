package com.github.radupana.featherweight.data.profile

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.github.radupana.featherweight.data.exercise.ExerciseVariation
import java.time.LocalDateTime

@Entity(
    tableName = "one_rm_history",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseVariation::class,
            parentColumns = ["id"],
            childColumns = ["exerciseVariationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["exerciseVariationId", "recordedAt"]),
        Index("userId"),
    ],
)
data class OneRMHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String? = null,
    val exerciseVariationId: Long,
    val oneRMEstimate: Float,
    val context: String, // e.g., "140kg × 3 @ RPE 8"
    val recordedAt: LocalDateTime = LocalDateTime.now(),
)
