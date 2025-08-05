package com.github.radupana.featherweight.data.profile

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    // User Profile operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfile): Long

    @Query("SELECT * FROM user_profiles WHERE id = :userId")
    suspend fun getUserProfile(userId: Long): UserProfile?

    @Query("SELECT * FROM user_profiles WHERE id = :userId")
    fun getUserProfileFlow(userId: Long): Flow<UserProfile?>

    @Query("SELECT * FROM user_profiles ORDER BY displayName ASC")
    suspend fun getAllUsers(): List<UserProfile>

    @Query("SELECT * FROM user_profiles LIMIT 1")
    suspend fun getUserProfile(): UserProfile?
}
