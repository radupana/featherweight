package com.github.radupana.featherweight.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert

@Dao
interface WorkoutTemplateDao {
    @Insert
    suspend fun insertTemplate(template: WorkoutTemplate)

    @Update
    suspend fun updateTemplate(template: WorkoutTemplate)

    @Upsert
    suspend fun upsertTemplate(template: WorkoutTemplate)

    @Query("SELECT * FROM workout_templates WHERE id = :templateId")
    suspend fun getTemplateById(templateId: String): WorkoutTemplate?

    @Query("SELECT * FROM workout_templates WHERE userId = :userId ORDER BY updatedAt DESC")
    suspend fun getTemplates(userId: String): List<WorkoutTemplate>

    @Query("SELECT * FROM workout_templates WHERE userId = :userId ORDER BY updatedAt DESC")
    suspend fun getTemplatesByUserId(userId: String): List<WorkoutTemplate>

    @Query("DELETE FROM workout_templates WHERE id = :templateId")
    suspend fun deleteTemplate(templateId: String)

    @Query("DELETE FROM workout_templates WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}
