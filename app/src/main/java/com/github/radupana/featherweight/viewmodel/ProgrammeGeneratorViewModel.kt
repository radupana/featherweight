package com.github.radupana.featherweight.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.data.*
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.service.AIProgrammeService
import com.github.radupana.featherweight.service.InputAnalyzer
import com.github.radupana.featherweight.service.PlaceholderGenerator
import com.github.radupana.featherweight.service.MockProgrammeGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProgrammeGeneratorViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FeatherweightRepository(application)
    private val aiService = AIProgrammeService()
    private val inputAnalyzer = InputAnalyzer()
    private val placeholderGenerator = PlaceholderGenerator()
    
    private val _uiState = MutableStateFlow(GuidedInputState())
    val uiState = _uiState.asStateFlow()
    
    fun updateInputText(text: String) {
        val currentState = _uiState.value
        val detectedElements = inputAnalyzer.analyzeInput(text, currentState.selectedGoal)
        val completeness = inputAnalyzer.calculateCompleteness(
            detectedElements,
            currentState.selectedGoal != null,
            currentState.selectedFrequency != null,
            text.length
        )
        val contextualChips = inputAnalyzer.getContextualChips(
            currentState.selectedGoal,
            currentState.selectedFrequency,
            detectedElements
        )
        
        _uiState.value = currentState.copy(
            inputText = text,
            detectedElements = detectedElements,
            inputCompleteness = completeness,
            availableChips = contextualChips,
            errorMessage = null
        )
    }
    
    fun selectGoal(goal: ProgrammeGoal) {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(selectedGoal = goal)
        
        // Re-analyze input with new goal
        if (currentState.inputText.isNotEmpty()) {
            updateInputText(currentState.inputText)
        } else {
            updateContextualChips()
        }
    }
    
    fun selectFrequency(frequency: Int) {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(selectedFrequency = frequency)
        
        // Re-analyze input with new frequency
        if (currentState.inputText.isNotEmpty()) {
            updateInputText(currentState.inputText)
        } else {
            updateContextualChips()
        }
    }
    
    fun selectDuration(duration: SessionDuration) {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(selectedDuration = duration)
        
        // Update completeness and chips
        updateContextualChips()
    }
    
    fun addChipText(chipText: String) {
        val currentState = _uiState.value
        val newText = if (currentState.inputText.isEmpty()) {
            chipText
        } else {
            "${currentState.inputText}. $chipText"
        }
        updateInputText(newText)
    }
    
    fun toggleExamples() {
        _uiState.value = _uiState.value.copy(showExamples = !_uiState.value.showExamples)
    }
    
    fun loadTemplate(template: ExampleTemplate) {
        _uiState.value = _uiState.value.copy(
            selectedGoal = template.goal,
            selectedFrequency = template.frequency,
            selectedDuration = template.duration,
            showExamples = false
        )
        updateInputText(template.exampleText)
    }
    
    fun getPlaceholderText(): String {
        val currentState = _uiState.value
        return placeholderGenerator.generatePlaceholder(
            currentState.selectedGoal,
            currentState.selectedFrequency,
            currentState.selectedDuration
        )
    }
    
    fun getSuggestions(): List<String> {
        return inputAnalyzer.generateSuggestions(_uiState.value.detectedElements)
    }
    
    fun getFilteredTemplates(): List<ExampleTemplate> {
        val currentState = _uiState.value
        
        // If no selections made, show all templates
        if (currentState.selectedGoal == null && currentState.selectedFrequency == null && currentState.selectedDuration == null) {
            return ExampleTemplates.templates.take(4)
        }
        
        // Show templates that match any selected criteria, prioritizing more matches
        return ExampleTemplates.templates
            .map { template ->
                var score = 0
                if (currentState.selectedGoal == template.goal) score += 3
                if (currentState.selectedFrequency == template.frequency) score += 2
                if (currentState.selectedDuration == template.duration) score += 1
                template to score
            }
            .filter { it.second > 0 } // At least one match
            .sortedByDescending { it.second } // Best matches first
            .map { it.first }
            .take(4)
    }
    
    private fun updateContextualChips() {
        val currentState = _uiState.value
        val contextualChips = inputAnalyzer.getContextualChips(
            currentState.selectedGoal,
            currentState.selectedFrequency,
            currentState.detectedElements
        )
        
        val completeness = inputAnalyzer.calculateCompleteness(
            currentState.detectedElements,
            currentState.selectedGoal != null,
            currentState.selectedFrequency != null,
            currentState.inputText.length
        )
        
        _uiState.value = currentState.copy(
            availableChips = contextualChips,
            inputCompleteness = completeness
        )
    }
    
    fun generateProgramme(onNavigateToPreview: (() -> Unit)? = null) {
        val currentState = _uiState.value
        
        if (currentState.generationCount >= currentState.maxDailyGenerations) {
            _uiState.value = currentState.copy(
                errorMessage = "Daily generation limit reached. Please try again tomorrow."
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = true, errorMessage = null)
            
            try {
                // Generate mock programme data for testing
                kotlinx.coroutines.delay(2000) // Simulate API call
                
                val response = MockProgrammeGenerator.generateMockProgramme(
                    goal = currentState.selectedGoal,
                    frequency = currentState.selectedFrequency,
                    duration = currentState.selectedDuration,
                    inputText = currentState.inputText
                )
                
                // Store the response for the preview screen
                GeneratedProgrammeHolder.setGeneratedProgramme(response)
                
                // Increment generation count
                _uiState.value = _uiState.value.copy(
                    generationCount = currentState.generationCount + 1,
                    isLoading = false
                )
                
                // Navigate to preview
                onNavigateToPreview?.invoke()
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to generate programme: ${e.message}"
                )
            }
        }
    }
}