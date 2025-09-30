package com.github.radupana.featherweight.data.profile

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.github.radupana.featherweight.util.IdGenerator
import java.time.LocalDateTime

@Entity(
    tableName = "one_rm_history",
    // Note: Removed foreign key since it can reference either system or custom exercises
    indices = [
        Index(value = ["exerciseVariationId", "recordedAt"]),
        Index("userId"),
    ],
)
data class OneRMHistory(
    @PrimaryKey
    val id: String = IdGenerator.generateId(),
    val userId: String? = null,
    val exerciseVariationId: String, // References exercise_variations table
    val oneRMEstimate: Float,
    val context: String, // e.g., "140kg × 3 @ RPE 8"
    val recordedAt: LocalDateTime = LocalDateTime.now(),
)
