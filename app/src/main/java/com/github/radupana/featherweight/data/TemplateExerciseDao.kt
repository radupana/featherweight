package com.github.radupana.featherweight.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert

@Dao
interface TemplateExerciseDao {
    @Insert
    suspend fun insertTemplateExercise(exercise: TemplateExercise)

    @Update
    suspend fun updateTemplateExercise(exercise: TemplateExercise)

    @Upsert
    suspend fun upsertTemplateExercise(exercise: TemplateExercise)

    @Query("SELECT * FROM template_exercises WHERE id = :exerciseId")
    suspend fun getTemplateExerciseById(exerciseId: String): TemplateExercise?

    @Query("SELECT * FROM template_exercises WHERE templateId = :templateId ORDER BY exerciseOrder")
    suspend fun getExercisesForTemplate(templateId: String): List<TemplateExercise>

    @Query("SELECT * FROM template_exercises WHERE templateId IN (:templateIds) ORDER BY exerciseOrder")
    suspend fun getExercisesForTemplates(templateIds: List<String>): List<TemplateExercise>

    @Query("DELETE FROM template_exercises WHERE id = :exerciseId")
    suspend fun deleteTemplateExercise(exerciseId: String)

    @Query("DELETE FROM template_exercises WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}
