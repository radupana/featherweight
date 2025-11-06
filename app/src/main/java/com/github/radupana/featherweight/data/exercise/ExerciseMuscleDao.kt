package com.github.radupana.featherweight.data.exercise

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert

/**
 * DAO for managing muscle associations with exercise variations.
 */
@Dao
interface ExerciseMuscleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseMuscle(muscle: ExerciseMuscle)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseMuscles(muscles: List<ExerciseMuscle>)

    @Query("DELETE FROM exercise_muscles WHERE exerciseId = :exerciseId")
    suspend fun deleteMuscleMappingsForExercise(exerciseId: String)

    @Query("SELECT * FROM exercise_muscles WHERE exerciseId = :exerciseId")
    suspend fun getMusclesForVariation(exerciseId: String): List<ExerciseMuscle>

    @Query("SELECT * FROM exercise_muscles")
    suspend fun getAllExerciseMuscles(): List<ExerciseMuscle>

    @Query("DELETE FROM exercise_muscles WHERE exerciseId = :exerciseId")
    suspend fun deleteForVariation(exerciseId: String)

    @Query("DELETE FROM exercise_muscles WHERE exerciseId IN (:exerciseIds)")
    suspend fun deleteForExercises(exerciseIds: List<String>)

    @Query(
        """
        SELECT DISTINCT e.* FROM exercises e
        INNER JOIN exercise_muscles em ON e.id = em.exerciseId
        WHERE em.muscle = :muscleGroup
        ORDER BY e.name ASC
    """,
    )
    suspend fun getExercisesByMuscleGroup(muscleGroup: String): List<Exercise>

    // Sync operations
    @Upsert
    suspend fun upsertExerciseMuscle(muscle: ExerciseMuscle)

    @Query("UPDATE exercise_muscles SET isDeleted = 1 WHERE id = :muscleId")
    suspend fun softDeleteExerciseMuscle(muscleId: String)

    @Query("DELETE FROM exercise_muscles WHERE id = :id")
    suspend fun deleteById(id: String)
}
