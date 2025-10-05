package com.github.radupana.featherweight.data.exercise

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * DAO for managing aliases for exercise variations.
 */
@Dao
interface ExerciseAliasDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlias(alias: ExerciseAlias)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAliases(aliases: List<ExerciseAlias>)

    @Query("SELECT * FROM exercise_aliases WHERE exerciseId = :exerciseId")
    suspend fun getAliasesForExercise(exerciseId: String): List<ExerciseAlias>

    @Query(
        """
        SELECT v.* FROM exercises v
        INNER JOIN exercise_aliases a ON v.id = a.exerciseId
        WHERE LOWER(a.alias) = LOWER(:alias)
        LIMIT 1
    """,
    )
    suspend fun findExerciseByAlias(alias: String): Exercise?

    @Query("SELECT * FROM exercise_aliases")
    suspend fun getAllAliases(): List<ExerciseAlias>

    @Query("SELECT * FROM exercise_aliases WHERE id = :id")
    suspend fun getAliasById(id: String): ExerciseAlias?

    @Query("DELETE FROM exercise_aliases WHERE exerciseId = :exerciseId")
    suspend fun deleteForVariation(exerciseId: String)
}
