package com.github.radupana.featherweight.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.service.AIProgrammeService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProgrammeGeneratorUiState(
    val inputText: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val generationCount: Int = 0,
    val maxDailyGenerations: Int = 5
)

class ProgrammeGeneratorViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FeatherweightRepository(application)
    private val aiService = AIProgrammeService()
    
    private val _uiState = MutableStateFlow(ProgrammeGeneratorUiState())
    val uiState = _uiState.asStateFlow()
    
    fun updateInputText(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text, errorMessage = null)
    }
    
    fun generateProgramme() {
        if (_uiState.value.generationCount >= _uiState.value.maxDailyGenerations) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Daily generation limit reached. Please try again tomorrow."
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                // For Phase 1, we'll just simulate the generation
                // In Phase 2+, this will actually call the AI service
                kotlinx.coroutines.delay(2000) // Simulate API call
                
                // Increment generation count
                _uiState.value = _uiState.value.copy(
                    generationCount = _uiState.value.generationCount + 1,
                    isLoading = false,
                    errorMessage = "Programme generation coming soon! This is Phase 1 - infrastructure only."
                )
                
                // TODO: In next phases:
                // 1. Call AI service with input text
                // 2. Parse response into Programme structure
                // 3. Show preview screen
                // 4. Allow user to activate programme
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to generate programme: ${e.message}"
                )
            }
        }
    }
}