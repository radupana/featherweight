package com.github.radupana.featherweight.data.profile

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val displayName: String,
    val avatarEmoji: String = "💪", // Default avatar
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    // Future fields: email, age, bodyweight, units preference, etc.
)
