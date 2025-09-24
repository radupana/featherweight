package com.github.radupana.featherweight.data.exercise

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface CustomExerciseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomCore(core: CustomExerciseCore): Long

    @Update
    suspend fun updateCustomCore(core: CustomExerciseCore)

    @Query("SELECT * FROM custom_exercise_cores WHERE id = :id")
    suspend fun getCustomCoreById(id: Long): CustomExerciseCore?

    @Query("SELECT * FROM custom_exercise_cores WHERE userId = :userId")
    suspend fun getCustomCoresByUser(userId: String): List<CustomExerciseCore>

    @Query("DELETE FROM custom_exercise_cores WHERE id = :id AND userId = :userId")
    suspend fun deleteCustomCore(
        id: Long,
        userId: String,
    )

    @Query("DELETE FROM custom_exercise_cores WHERE userId = :userId")
    suspend fun deleteAllCustomCoresByUser(userId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomVariation(variation: CustomExerciseVariation): Long

    @Update
    suspend fun updateCustomVariation(variation: CustomExerciseVariation)

    @Query("SELECT * FROM custom_exercise_variations WHERE id = :id")
    suspend fun getCustomVariationById(id: Long): CustomExerciseVariation?

    @Query("SELECT * FROM custom_exercise_variations WHERE userId = :userId")
    suspend fun getCustomVariationsByUser(userId: String): List<CustomExerciseVariation>

    @Query("SELECT * FROM custom_exercise_variations WHERE userId = :userId AND name = :name LIMIT 1")
    suspend fun getCustomVariationByUserAndName(
        userId: String,
        name: String,
    ): CustomExerciseVariation?

    @Query("DELETE FROM custom_exercise_variations WHERE id = :id AND userId = :userId")
    suspend fun deleteCustomVariation(
        id: Long,
        userId: String,
    )

    @Query("DELETE FROM custom_exercise_variations WHERE userId = :userId")
    suspend fun deleteAllCustomVariationsByUser(userId: String)

    // ============== Combined queries ==============

    @Transaction
    @Query(
        """
        SELECT * FROM custom_exercise_variations
        WHERE userId = :userId
        ORDER BY name ASC
    """,
    )
    suspend fun getAllCustomExercisesForUser(userId: String): List<CustomExerciseVariation>

    @Transaction
    @Query(
        """
        SELECT * FROM custom_exercise_variations
        WHERE userId = :userId AND equipment = :equipment
        ORDER BY name ASC
    """,
    )
    suspend fun getCustomVariationsByEquipment(
        userId: String,
        equipment: Equipment,
    ): List<CustomExerciseVariation>

    @Query("SELECT EXISTS(SELECT 1 FROM custom_exercise_variations WHERE id = :variationId AND userId = :userId)")
    suspend fun isCustomExercise(
        variationId: Long,
        userId: String,
    ): Boolean
}
