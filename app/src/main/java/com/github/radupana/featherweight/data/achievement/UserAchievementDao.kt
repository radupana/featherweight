package com.github.radupana.featherweight.data.achievement

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface UserAchievementDao {
    
    @Query("SELECT * FROM user_achievements WHERE userId = :userId ORDER BY unlockedDate DESC")
    fun getUserAchievements(userId: Long): Flow<List<UserAchievement>>
    
    @Query("SELECT * FROM user_achievements WHERE userId = :userId ORDER BY unlockedDate DESC LIMIT :limit")
    suspend fun getRecentUserAchievements(userId: Long, limit: Int): List<UserAchievement>
    
    @Query("SELECT * FROM user_achievements WHERE userId = :userId AND achievementId = :achievementId")
    suspend fun getUserAchievement(userId: Long, achievementId: String): UserAchievement?
    
    @Query("SELECT COUNT(*) FROM user_achievements WHERE userId = :userId")
    suspend fun getUserAchievementCount(userId: Long): Int
    
    @Query("SELECT achievementId FROM user_achievements WHERE userId = :userId")
    suspend fun getUnlockedAchievementIds(userId: Long): List<String>
    
    @Query("SELECT * FROM user_achievements WHERE userId = :userId AND unlockedDate >= :since ORDER BY unlockedDate DESC")
    suspend fun getAchievementsSince(userId: Long, since: LocalDateTime): List<UserAchievement>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserAchievement(achievement: UserAchievement): Long
    
    @Delete
    suspend fun deleteUserAchievement(achievement: UserAchievement)
    
    @Query("DELETE FROM user_achievements WHERE userId = :userId")
    suspend fun deleteAllUserAchievements(userId: Long)
    
    @Query("SELECT COUNT(*) > 0 FROM user_achievements WHERE userId = :userId AND achievementId = :achievementId")
    suspend fun hasUnlockedAchievement(userId: Long, achievementId: String): Boolean
}