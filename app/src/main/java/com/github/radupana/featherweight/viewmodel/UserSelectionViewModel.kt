package com.github.radupana.featherweight.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.data.UserPreferences
import com.github.radupana.featherweight.data.profile.UserProfile
import com.github.radupana.featherweight.repository.FeatherweightRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UserSelectionUiState(
    val isLoading: Boolean = false,
    val users: List<UserProfile> = emptyList(),
    val error: String? = null,
)

class UserSelectionViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = FeatherweightRepository(application)
    private val userPreferences = UserPreferences(application)

    private val _uiState = MutableStateFlow(UserSelectionUiState())
    val uiState: StateFlow<UserSelectionUiState> = _uiState.asStateFlow()

    init {
        loadUsers()
    }

    private fun loadUsers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val users = repository.getAllUsers()

                // If no users exist, seed test users
                if (users.isEmpty()) {
                    repository.seedTestUsers()
                    val seededUsers = repository.getAllUsers()
                    _uiState.value =
                        _uiState.value.copy(
                            users = seededUsers,
                            isLoading = false,
                        )
                } else {
                    _uiState.value =
                        _uiState.value.copy(
                            users = users,
                            isLoading = false,
                        )
                }
            } catch (e: Exception) {
                _uiState.value =
                    _uiState.value.copy(
                        error = "Failed to load users: ${e.message}",
                        isLoading = false,
                    )
            }
        }
    }

    fun selectUser(user: UserProfile) {
        userPreferences.setCurrentUserId(user.id)
    }
}
