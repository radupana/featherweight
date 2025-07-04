package com.github.radupana.featherweight.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.github.radupana.featherweight.data.exercise.MovementPattern
import com.github.radupana.featherweight.data.exercise.MuscleGroup

@Entity(tableName = "exercise_correlations")
data class ExerciseCorrelation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val primaryExercise: String,
    val relatedExercise: String,
    val correlationStrength: Float,        // 0.0-1.0, how closely related
    val movementPattern: MovementPattern,
    val primaryMuscleGroup: MuscleGroup,
    val secondaryMuscleGroups: String? = null, // JSON array of MuscleGroup names
    
    // Correlation metadata
    val isCompound: Boolean = true,        // Compound vs isolation
    val equipmentCategory: String? = null, // Barbell, Dumbbell, Cable, etc.
    val correlationType: String? = null    // "variation", "antagonist", "synergist"
)