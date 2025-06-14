package com.github.radupana.featherweight.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferences(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("featherweight_prefs", Context.MODE_PRIVATE)

    private val _currentUserId = MutableStateFlow(getCurrentUserId())
    val currentUserId: StateFlow<Long> = _currentUserId.asStateFlow()

    fun setCurrentUserId(userId: Long) {
        sharedPreferences.edit().putLong(KEY_CURRENT_USER_ID, userId).apply()
        _currentUserId.value = userId
    }

    fun getCurrentUserId(): Long {
        return sharedPreferences.getLong(KEY_CURRENT_USER_ID, 0L)
    }

    fun hasSelectedUser(): Boolean {
        return getCurrentUserId() != 0L
    }

    fun clearCurrentUser() {
        sharedPreferences.edit().remove(KEY_CURRENT_USER_ID).apply()
        _currentUserId.value = 0L
    }

    companion object {
        private const val KEY_CURRENT_USER_ID = "current_user_id"
    }
}
