package com.github.radupana.featherweight.data

import androidx.room.*

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
}
