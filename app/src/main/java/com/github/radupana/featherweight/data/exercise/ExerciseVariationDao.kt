package com.github.radupana.featherweight.data.exercise

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/**
 * DAO for ExerciseVariation operations.
 */
@Dao
interface ExerciseVariationDao {
    // Basic CRUD operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseVariation(variation: ExerciseVariation): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseVariations(variations: List<ExerciseVariation>): List<Long>

    @Update
    suspend fun updateExerciseVariation(variation: ExerciseVariation)

    @Query("DELETE FROM exercise_variations WHERE id = :id")
    suspend fun deleteExerciseVariation(id: Long)

    @Query("DELETE FROM exercise_variations WHERE coreExerciseId = :coreExerciseId")
    suspend fun deleteVariationsByCore(coreExerciseId: Long)

    // Basic queries
    @Query("SELECT * FROM exercise_variations WHERE id = :id")
    suspend fun getExerciseVariationById(id: Long): ExerciseVariation?

    @Query("SELECT * FROM exercise_variations WHERE name = :name LIMIT 1")
    suspend fun getExerciseVariationByName(name: String): ExerciseVariation?

    @Query("SELECT * FROM exercise_variations WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findVariationByName(name: String): ExerciseVariation?

    @Query("SELECT * FROM exercise_variations ORDER BY usageCount DESC, name ASC")
    suspend fun getAllExerciseVariations(): List<ExerciseVariation>

    // Variations for a specific core exercise
    @Query("SELECT * FROM exercise_variations WHERE coreExerciseId = :coreExerciseId ORDER BY usageCount DESC, name ASC")
    suspend fun getVariationsByCore(coreExerciseId: Long): List<ExerciseVariation>

    @Query(
        """
        SELECT * FROM exercise_variations 
        WHERE coreExerciseId = :coreExerciseId 
          AND equipment = :equipment 
        ORDER BY usageCount DESC, name ASC
    """,
    )
    suspend fun getVariationsByCoreAndEquipment(
        coreExerciseId: Long,
        equipment: Equipment,
    ): List<ExerciseVariation>

    // Search and filtering
    @Query("SELECT * FROM exercise_variations WHERE name LIKE '%' || :query || '%' ORDER BY usageCount DESC, name ASC")
    suspend fun searchExerciseVariations(query: String): List<ExerciseVariation>

    @Query("SELECT * FROM exercise_variations WHERE equipment = :equipment ORDER BY usageCount DESC, name ASC")
    suspend fun getExerciseVariationsByEquipment(equipment: Equipment): List<ExerciseVariation>

    @Query("SELECT * FROM exercise_variations WHERE requiresWeight = :requiresWeight ORDER BY usageCount DESC, name ASC")
    suspend fun getExerciseVariationsByWeightRequirement(requiresWeight: Boolean): List<ExerciseVariation>

    @Query("SELECT * FROM exercise_variations WHERE isCustom = :isCustom ORDER BY usageCount DESC, name ASC")
    suspend fun getExerciseVariationsByCustomStatus(isCustom: Boolean): List<ExerciseVariation>

    @Query("SELECT * FROM exercise_variations WHERE createdBy = :userId ORDER BY usageCount DESC, name ASC")
    suspend fun getCustomExerciseVariationsByUser(userId: Long): List<ExerciseVariation>

    // Usage tracking
    @Query("UPDATE exercise_variations SET usageCount = usageCount + 1 WHERE id = :id")
    suspend fun incrementUsageCount(id: Long)

    @Query("UPDATE exercise_variations SET usageCount = 0")
    suspend fun resetAllUsageCounts()

    @Query("UPDATE exercise_variations SET usageCount = 0 WHERE coreExerciseId = :coreExerciseId")
    suspend fun resetUsageCountsByCore(coreExerciseId: Long)

    // Most used variations
    @Query("SELECT * FROM exercise_variations ORDER BY usageCount DESC LIMIT :limit")
    suspend fun getMostUsedExerciseVariations(limit: Int): List<ExerciseVariation>

    @Query(
        """
        SELECT * FROM exercise_variations 
        WHERE coreExerciseId = :coreExerciseId 
        ORDER BY usageCount DESC LIMIT :limit
    """,
    )
    suspend fun getMostUsedVariationsByCore(
        coreExerciseId: Long,
        limit: Int,
    ): List<ExerciseVariation>

    // Equipment availability filtering
    @Query(
        """
        SELECT * FROM exercise_variations 
        WHERE equipment IN (:availableEquipment) 
        ORDER BY usageCount DESC, name ASC
    """,
    )
    suspend fun getExerciseVariationsByAvailableEquipment(availableEquipment: List<Equipment>): List<ExerciseVariation>

    @Query(
        """
        SELECT * FROM exercise_variations 
        WHERE coreExerciseId = :coreExerciseId 
          AND equipment IN (:availableEquipment)
        ORDER BY usageCount DESC, name ASC
    """,
    )
    suspend fun getVariationsByCoreAndAvailableEquipment(
        coreExerciseId: Long,
        availableEquipment: List<Equipment>,
    ): List<ExerciseVariation>

    // Difficulty-based queries
    @Query("SELECT * FROM exercise_variations WHERE difficulty <= :maxDifficulty ORDER BY difficulty ASC, name ASC")
    suspend fun getExerciseVariationsByMaxDifficulty(maxDifficulty: ExerciseDifficulty): List<ExerciseVariation>

    // Recommended rep range filtering
    @Query("SELECT * FROM exercise_variations WHERE recommendedRepRange = :repRange ORDER BY usageCount DESC, name ASC")
    suspend fun getExerciseVariationsByRepRange(repRange: String): List<ExerciseVariation>

    // Big 4 variations (Squat, Deadlift, Bench Press, Overhead Press variations)
    @Query(
        """
        SELECT v.* FROM exercise_variations v
        INNER JOIN exercise_cores c ON v.coreExerciseId = c.id
        WHERE c.name IN ('Squat', 'Deadlift', 'Bench Press', 'Overhead Press')
        ORDER BY 
            CASE c.name
                WHEN 'Squat' THEN 1
                WHEN 'Deadlift' THEN 2
                WHEN 'Bench Press' THEN 3
                WHEN 'Overhead Press' THEN 4
            END,
            v.usageCount DESC
    """,
    )
    suspend fun getBig4ExerciseVariations(): List<ExerciseVariation>

    // Statistics
    @Query("SELECT COUNT(*) FROM exercise_variations")
    suspend fun getTotalExerciseVariationCount(): Int

    @Query("SELECT COUNT(*) FROM exercise_variations WHERE coreExerciseId = :coreExerciseId")
    suspend fun getVariationCountByCore(coreExerciseId: Long): Int

    @Query("SELECT COUNT(*) FROM exercise_variations WHERE isCustom = 1")
    suspend fun getCustomExerciseVariationCount(): Int

    @Query(
        """
        SELECT equipment, COUNT(*) as count 
        FROM exercise_variations 
        GROUP BY equipment 
        ORDER BY count DESC
    """,
    )
    suspend fun getExerciseVariationCountByEquipment(): List<EquipmentCount>
}

/**
 * Data class for equipment statistics.
 */
data class EquipmentCount(
    val equipment: Equipment,
    val count: Int,
)
