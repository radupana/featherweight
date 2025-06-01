package com.github.radupana.featherweight.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SetLogDao {
    @Insert
    suspend fun insertSetLog(setLog: SetLog): Long

    @Query("SELECT * FROM SetLog WHERE exerciseLogId = :exerciseLogId ORDER BY setOrder")
    suspend fun getSetLogsForExercise(exerciseLogId: Long): List<SetLog>
}
