package com.github.radupana.featherweight.viewmodel

import com.github.radupana.featherweight.data.profile.UserProfile
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDateTime

class UserSelectionUiStateTest {
    
    @Test
    fun `UserSelectionUiState default values`() {
        // Arrange & Act
        val state = UserSelectionUiState()
        
        // Assert
        assertThat(state.isLoading).isFalse()
        assertThat(state.users).isEmpty()
        assertThat(state.error).isNull()
    }
    
    @Test
    fun `UserSelectionUiState with loading true`() {
        // Arrange & Act
        val state = UserSelectionUiState(isLoading = true)
        
        // Assert
        assertThat(state.isLoading).isTrue()
        assertThat(state.users).isEmpty()
        assertThat(state.error).isNull()
    }
    
    @Test
    fun `UserSelectionUiState with users`() {
        // Arrange
        val users = listOf(
            UserProfile(
                id = 1L,
                username = "johndoe",
                displayName = "John Doe",
                avatarEmoji = "💪",
                createdAt = LocalDateTime.now()
            ),
            UserProfile(
                id = 2L,
                username = "janesmith",
                displayName = "Jane Smith",
                avatarEmoji = "🏋️",
                createdAt = LocalDateTime.now()
            )
        )
        
        // Act
        val state = UserSelectionUiState(users = users)
        
        // Assert
        assertThat(state.isLoading).isFalse()
        assertThat(state.users).hasSize(2)
        assertThat(state.users[0].displayName).isEqualTo("John Doe")
        assertThat(state.users[1].displayName).isEqualTo("Jane Smith")
        assertThat(state.error).isNull()
    }
    
    @Test
    fun `UserSelectionUiState with error`() {
        // Arrange & Act
        val state = UserSelectionUiState(error = "Database error")
        
        // Assert
        assertThat(state.isLoading).isFalse()
        assertThat(state.users).isEmpty()
        assertThat(state.error).isEqualTo("Database error")
    }
    
    @Test
    fun `UserSelectionUiState copy with modifications`() {
        // Arrange
        val original = UserSelectionUiState(
            isLoading = true,
            users = emptyList(),
            error = null
        )
        
        val users = listOf(
            UserProfile(
                id = 1L,
                username = "testuser",
                displayName = "Test User",
                avatarEmoji = "💪",
                createdAt = LocalDateTime.now()
            )
        )
        
        // Act
        val updated = original.copy(
            isLoading = false,
            users = users
        )
        
        // Assert
        assertThat(updated.isLoading).isFalse()
        assertThat(updated.users).hasSize(1)
        assertThat(updated.error).isNull()
        assertThat(original.isLoading).isTrue() // Original unchanged
    }
    
    @Test
    fun `UserSelectionUiState equals and hashCode`() {
        // Arrange
        val state1 = UserSelectionUiState(
            isLoading = false,
            users = emptyList(),
            error = "Error"
        )
        
        val state2 = UserSelectionUiState(
            isLoading = false,
            users = emptyList(),
            error = "Error"
        )
        
        val state3 = UserSelectionUiState(
            isLoading = true,
            users = emptyList(),
            error = "Error"
        )
        
        // Assert
        assertThat(state1).isEqualTo(state2)
        assertThat(state1.hashCode()).isEqualTo(state2.hashCode())
        assertThat(state1).isNotEqualTo(state3)
    }
}