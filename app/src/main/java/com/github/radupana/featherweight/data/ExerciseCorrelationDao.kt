package com.github.radupana.featherweight.data

import androidx.room.*
import com.github.radupana.featherweight.data.ExerciseCorrelation
import com.github.radupana.featherweight.data.exercise.MovementPattern
import com.github.radupana.featherweight.data.exercise.MuscleGroup
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseCorrelationDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(correlation: ExerciseCorrelation): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(correlations: List<ExerciseCorrelation>)
    
    @Query("""
        SELECT * FROM exercise_correlations 
        WHERE primaryExercise = :exerciseName OR relatedExercise = :exerciseName
        ORDER BY correlationStrength DESC
    """)
    suspend fun getCorrelationsForExercise(exerciseName: String): List<ExerciseCorrelation>
    
    @Query("""
        SELECT * FROM exercise_correlations 
        WHERE movementPattern = :pattern
        ORDER BY correlationStrength DESC
    """)
    suspend fun getExercisesByMovementPattern(pattern: MovementPattern): List<ExerciseCorrelation>
    
    @Query("""
        SELECT * FROM exercise_correlations 
        WHERE primaryMuscleGroup = :muscleGroup
        ORDER BY correlationStrength DESC
    """)
    suspend fun getExercisesByMuscleGroup(muscleGroup: MuscleGroup): List<ExerciseCorrelation>
    
    @Query("""
        SELECT DISTINCT primaryExercise FROM exercise_correlations 
        WHERE primaryMuscleGroup = :muscleGroup
        UNION
        SELECT DISTINCT relatedExercise FROM exercise_correlations 
        WHERE primaryMuscleGroup = :muscleGroup
    """)
    suspend fun getAllExercisesForMuscleGroup(muscleGroup: MuscleGroup): List<String>
    
    @Query("""
        SELECT * FROM exercise_correlations 
        WHERE (primaryExercise = :exercise1 AND relatedExercise = :exercise2)
           OR (primaryExercise = :exercise2 AND relatedExercise = :exercise1)
        LIMIT 1
    """)
    suspend fun getCorrelationBetween(exercise1: String, exercise2: String): ExerciseCorrelation?
    
    @Query("""
        SELECT DISTINCT movementPattern FROM exercise_correlations
    """)
    suspend fun getAllMovementPatterns(): List<MovementPattern>
    
    @Query("""
        SELECT DISTINCT primaryMuscleGroup FROM exercise_correlations
    """)
    suspend fun getAllMuscleGroups(): List<MuscleGroup>
    
    @Query("DELETE FROM exercise_correlations")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM exercise_correlations")
    suspend fun getCount(): Int
}