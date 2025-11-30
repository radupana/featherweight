package com.github.radupana.featherweight.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert

@Dao
interface TemplateSetDao {
    @Insert
    suspend fun insertTemplateSet(set: TemplateSet)

    @Update
    suspend fun updateTemplateSet(set: TemplateSet)

    @Upsert
    suspend fun upsertTemplateSet(set: TemplateSet)

    @Query("SELECT * FROM template_sets WHERE id = :setId")
    suspend fun getTemplateSetById(setId: String): TemplateSet?

    @Query("SELECT * FROM template_sets WHERE templateExerciseId = :exerciseId ORDER BY setOrder")
    suspend fun getSetsForTemplateExercise(exerciseId: String): List<TemplateSet>

    @Query("SELECT * FROM template_sets WHERE templateExerciseId IN (:exerciseIds) ORDER BY setOrder")
    suspend fun getSetsForTemplateExercises(exerciseIds: List<String>): List<TemplateSet>

    @Query("DELETE FROM template_sets WHERE id = :setId")
    suspend fun deleteTemplateSet(setId: String)

    @Query("DELETE FROM template_sets WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}
