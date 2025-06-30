package com.github.radupana.featherweight.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface SetLogDao {
    @Insert
    suspend fun insertSetLog(setLog: SetLog): Long

    @Query("SELECT * FROM SetLog WHERE exerciseLogId = :exerciseLogId ORDER BY setOrder")
    suspend fun getSetLogsForExercise(exerciseLogId: Long): List<SetLog>

    @Query("UPDATE SetLog SET isCompleted = :completed, completedAt = :completedAt WHERE id = :setId")
    suspend fun markSetCompleted(
        setId: Long,
        completed: Boolean,
        completedAt: String?,
    )

    @Update
    suspend fun updateSetLog(setLog: SetLog)

    @Query("DELETE FROM SetLog WHERE id = :setId")
    suspend fun deleteSetLog(setId: Long)
    
    @Query("DELETE FROM SetLog WHERE exerciseLogId = :exerciseLogId")
    suspend fun deleteAllSetsForExercise(exerciseLogId: Long)
}
