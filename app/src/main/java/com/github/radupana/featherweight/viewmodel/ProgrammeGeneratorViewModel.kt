package com.github.radupana.featherweight.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.data.*
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.service.AIProgrammeService
import com.github.radupana.featherweight.service.AIProgrammeRequest
import com.github.radupana.featherweight.service.AIProgrammeResponse
import com.github.radupana.featherweight.service.AIProgrammeQuotaManager
import com.github.radupana.featherweight.service.InputAnalyzer
import com.github.radupana.featherweight.service.PlaceholderGenerator
import com.github.radupana.featherweight.ai.WeightExtractionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProgrammeGeneratorViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FeatherweightRepository(application)
    private val aiService = AIProgrammeService()
    private val quotaManager = AIProgrammeQuotaManager(application)
    private val inputAnalyzer = InputAnalyzer()
    private val placeholderGenerator = PlaceholderGenerator()
    private val weightExtractor = WeightExtractionService()
    
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
            detectedElements,
            currentState.usedChips
        )
        
        _uiState.value = currentState.copy(
            inputText = text,
            detectedElements = detectedElements,
            inputCompleteness = completeness,
            availableChips = contextualChips,
            errorMessage = null
        )
    }
    
    fun selectGenerationMode(mode: GenerationMode) {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            generationMode = mode,
            // Clear text when switching modes
            inputText = "",
            // Reset chips and analysis for fresh start
            usedChips = emptySet(),
            detectedElements = emptySet(),
            inputCompleteness = 0f
        )
    }

    fun selectGoal(goal: ProgrammeGoal) {
        val currentState = _uiState.value
        // Toggle selection - if already selected, deselect it
        val newGoal = if (currentState.selectedGoal == goal) null else goal
        _uiState.value = currentState.copy(selectedGoal = newGoal)
        
        // Re-analyze input with new goal
        if (currentState.inputText.isNotEmpty()) {
            updateInputText(currentState.inputText)
        } else {
            updateContextualChips()
        }
    }
    
    fun selectFrequency(frequency: Int) {
        val currentState = _uiState.value
        // Toggle selection - if already selected, deselect it
        val newFrequency = if (currentState.selectedFrequency == frequency) null else frequency
        _uiState.value = currentState.copy(selectedFrequency = newFrequency)
        
        // Re-analyze input with new frequency
        if (currentState.inputText.isNotEmpty()) {
            updateInputText(currentState.inputText)
        } else {
            updateContextualChips()
        }
    }
    
    fun selectDuration(duration: SessionDuration) {
        val currentState = _uiState.value
        // Toggle selection - if already selected, deselect it
        val newDuration = if (currentState.selectedDuration == duration) null else duration
        _uiState.value = currentState.copy(selectedDuration = newDuration)
        
        // Update contextual chips
        updateContextualChips()
    }
    
    fun addChipText(chipText: String) {
        val currentState = _uiState.value
        val newText = if (currentState.inputText.isEmpty()) {
            chipText
        } else {
            "${currentState.inputText}. $chipText"
        }
        
        // Find the full chip info to get the actual text to append
        val chip = currentState.availableChips.find { it.text == chipText }
        val textToAppend = chip?.appendText ?: chipText
        
        val finalText = if (currentState.inputText.isEmpty()) {
            textToAppend
        } else {
            "${currentState.inputText}. $textToAppend"
        }
        
        // Add chip to used chips and update text
        _uiState.value = currentState.copy(
            usedChips = currentState.usedChips + chipText
        )
        updateInputText(finalText)
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
        return when (currentState.generationMode) {
            GenerationMode.SIMPLIFIED -> placeholderGenerator.generatePlaceholder(
                currentState.selectedGoal,
                currentState.selectedFrequency,
                currentState.selectedDuration
            )
            GenerationMode.ADVANCED -> """Paste your complete programme description here...

For example:
"I want a 12-week strength programme, 4 days per week, focusing on powerlifting. 
Week 1-4: Build base strength with compound movements
Week 5-8: Intensify with heavier loads
Week 9-12: Peak for competition
Include squat, bench press, deadlift as main lifts with assistance work..."

Or paste existing programmes from ChatGPT, other AI tools, or coaches."""
        }
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
            currentState.detectedElements,
            currentState.usedChips
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
        
        // Check quota before attempting generation
        if (!quotaManager.canGenerateProgramme()) {
            val quotaStatus = quotaManager.getQuotaStatus()
            _uiState.value = currentState.copy(
                errorMessage = "Daily generation limit reached (${quotaStatus.totalQuota} per day). Please try again tomorrow."
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = true, errorMessage = null)
            
            try {
                // Get exercise database for AI context (including aliases)
                val exerciseNames = repository.getAllExerciseNamesIncludingAliases()
                
                // Get user's 1RMs for context
                val user1RMs = repository.getAllCurrentMaxes().first()
                
                // Create AI request
                val request = AIProgrammeRequest(
                    userInput = buildEnhancedUserInputWithWeights(currentState, user1RMs),
                    exerciseDatabase = exerciseNames,
                    maxDays = 7,
                    maxWeeks = 16
                )
                
                // Call real AI service (with fallback to mock if API key not configured)
                val response = aiService.generateProgramme(request)
                
                if (response.success) {
                    // Validate that all exercises in the response exist in our database
                    val validationResult = validateExercisesInProgramme(response, exerciseNames)
                    
                    if (validationResult.isValid) {
                        // Increment quota usage
                        if (quotaManager.incrementUsage()) {
                            // Store the response for the preview screen
                            GeneratedProgrammeHolder.setGeneratedProgramme(response)
                            
                            // Update UI state
                            val quotaStatus = quotaManager.getQuotaStatus()
                            _uiState.value = _uiState.value.copy(
                                generationCount = quotaStatus.totalQuota - quotaStatus.remainingGenerations,
                                isLoading = false
                            )
                            
                            // Navigate to preview
                            onNavigateToPreview?.invoke()
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = "Daily generation limit reached. Please try again tomorrow."
                            )
                        }
                    } else {
                        // Exercise validation failed - show error to user
                        println("⚠️ Exercise validation failed: ${validationResult.invalidExercises}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Generated programme contains unsupported exercises: ${validationResult.invalidExercises.joinToString(", ")}. Please try again with different requirements or use Browse Templates for proven programmes."
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = response.error ?: response.clarificationNeeded ?: "Unknown error"
                    )
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to generate programme: ${e.message}"
                )
            }
        }
    }
    
    private fun buildEnhancedUserInput(state: GuidedInputState): String {
        val parts = mutableListOf<String>()
        
        // Add user's raw input
        if (state.inputText.isNotBlank()) {
            parts.add(state.inputText.trim())
        }
        
        // Add structured selections
        if (state.selectedGoal != null) {
            val goalText = when (state.selectedGoal) {
                ProgrammeGoal.BUILD_STRENGTH -> "Primary goal: Build maximum strength"
                ProgrammeGoal.BUILD_MUSCLE -> "Primary goal: Build muscle mass and size"
                ProgrammeGoal.LOSE_FAT -> "Primary goal: Lose fat while maintaining muscle"
                ProgrammeGoal.ATHLETIC_PERFORMANCE -> "Primary goal: Improve athletic performance"
                ProgrammeGoal.CUSTOM -> "Primary goal: General fitness and health"
            }
            parts.add(goalText)
        }
        
        if (state.selectedFrequency != null) {
            parts.add("Training frequency: ${state.selectedFrequency} days per week")
        }
        
        if (state.selectedDuration != null) {
            val durationText = when (state.selectedDuration) {
                SessionDuration.QUICK -> "Session duration: 30-45 minutes (quick sessions)"
                SessionDuration.STANDARD -> "Session duration: 45-60 minutes (standard sessions)"
                SessionDuration.EXTENDED -> "Session duration: 60-75 minutes (extended sessions)"
                SessionDuration.LONG -> "Session duration: 75-90 minutes (long sessions)"
            }
            parts.add(durationText)
        }
        
        // Add information about detected elements
        if (state.detectedElements.contains(DetectedElement.EXPERIENCE_LEVEL)) {
            parts.add("Note: User has indicated their experience level")
        }
        
        if (state.detectedElements.contains(DetectedElement.EQUIPMENT)) {
            parts.add("Note: User has mentioned equipment preferences")
        }
        
        if (state.detectedElements.contains(DetectedElement.INJURIES)) {
            parts.add("Note: User has mentioned injuries or limitations - please accommodate")
        }
        
        if (state.detectedElements.contains(DetectedElement.SCHEDULE)) {
            parts.add("Note: User has mentioned schedule constraints")
        }
        
        // No need to extract weights - let AI handle it directly
        
        return if (parts.isNotEmpty()) {
            parts.joinToString("\n\n")
        } else {
            "Create a general fitness programme suitable for someone looking to improve their overall health and fitness."
        }
    }
    
    private fun buildEnhancedUserInputWithWeights(
        state: GuidedInputState, 
        user1RMs: List<com.github.radupana.featherweight.data.profile.ExerciseMaxWithName>
    ): String {
        val baseInput = buildEnhancedUserInput(state)
        val parts = mutableListOf(baseInput)
        
        // Add user's saved 1RMs if available
        if (user1RMs.isNotEmpty()) {
            val maxes = user1RMs.joinToString(", ") { max ->
                "${max.exerciseName}: ${max.maxWeight}kg"
            }
            parts.add("USER'S SAVED 1RMs from their profile: $maxes")
            parts.add("Use these for calculating percentages ONLY if the user hasn't specified exact weights")
        } else {
            parts.add("NO SAVED 1RMs - use average gym-goer weights as specified in guidelines")
        }
        
        return parts.joinToString("\n\n")
    }

    private fun validateExercisesInProgramme(response: AIProgrammeResponse, validExerciseNames: List<String>): ExerciseValidationResult {
        if (!response.success || response.programme == null) {
            return ExerciseValidationResult(false, emptyList())
        }

        val validExerciseSet = validExerciseNames.toSet()
        val invalidExercises = mutableListOf<String>()
        
        // Check all exercises in all workouts across all weeks
        response.programme.weeks.forEach { week ->
            week.workouts.forEach { workout ->
                workout.exercises.forEach { exercise ->
                    if (exercise.exerciseName !in validExerciseSet) {
                        invalidExercises.add(exercise.exerciseName)
                    }
                }
            }
        }
        
        return ExerciseValidationResult(
            isValid = invalidExercises.isEmpty(),
            invalidExercises = invalidExercises.distinct()
        )
    }


    data class ExerciseValidationResult(
        val isValid: Boolean,
        val invalidExercises: List<String>
    )
}