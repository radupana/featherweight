package com.github.radupana.featherweight.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.github.radupana.featherweight.ai.WeightExtractionService
import com.github.radupana.featherweight.data.*
import com.github.radupana.featherweight.data.profile.ExerciseMaxWithName
import com.github.radupana.featherweight.repository.AIProgrammeRepository
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.service.AIProgrammeQuotaManager
import com.github.radupana.featherweight.service.AIProgrammeRequest
import com.github.radupana.featherweight.service.AIProgrammeResponse
import com.github.radupana.featherweight.service.AIProgrammeService
import com.github.radupana.featherweight.service.InputAnalyzer
import com.github.radupana.featherweight.service.ExerciseMatchingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProgrammeGeneratorViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = FeatherweightRepository(application)
    private val database = FeatherweightDatabase.getDatabase(application)
    private val aiProgrammeRepository = AIProgrammeRepository(
        database.aiProgrammeRequestDao(),
        WorkManager.getInstance(application)
    )
    private val aiService = AIProgrammeService(application)
    private val quotaManager = AIProgrammeQuotaManager(application)
    private val inputAnalyzer = InputAnalyzer()
    private val exerciseMatchingService = ExerciseMatchingService()
    private val weightExtractor = WeightExtractionService()

    private val _uiState = MutableStateFlow(GuidedInputState())
    val uiState = _uiState.asStateFlow()

    fun updateInputText(text: String) {
        val currentState = _uiState.value
        val detectedElements = inputAnalyzer.analyzeInput(text, currentState.selectedGoal)
        val completeness =
            inputAnalyzer.calculateCompleteness(
                detectedElements,
                currentState.selectedGoal != null,
                currentState.selectedFrequency != null,
                text.length,
            )

        _uiState.value =
            currentState.copy(
                inputText = text,
                detectedElements = detectedElements,
                inputCompleteness = completeness,
                errorMessage = null,
            )
    }

    fun selectGenerationMode(mode: GenerationMode) {
        val currentState = _uiState.value
        _uiState.value =
            currentState.copy(
                generationMode = mode,
                // Clear text when switching modes
                inputText = "",
                // Reset analysis for fresh start
                detectedElements = emptySet(),
                inputCompleteness = 0f,
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
        }
    }

    fun selectDuration(duration: SessionDuration) {
        val currentState = _uiState.value
        // Toggle selection - if already selected, deselect it
        val newDuration = if (currentState.selectedDuration == duration) null else duration
        _uiState.value = currentState.copy(selectedDuration = newDuration)
    }

    fun selectExperienceLevel(experience: ExperienceLevel) {
        val currentState = _uiState.value
        // Toggle selection - if already selected, deselect it
        val newExperience = if (currentState.selectedExperience == experience) null else experience
        _uiState.value = currentState.copy(selectedExperience = newExperience)
    }

    fun selectEquipment(equipment: EquipmentAvailability) {
        val currentState = _uiState.value
        // Toggle selection - if already selected, deselect it
        val newEquipment = if (currentState.selectedEquipment == equipment) null else equipment
        _uiState.value = currentState.copy(selectedEquipment = newEquipment)
    }




    fun getSuggestions(): List<String> = inputAnalyzer.generateSuggestions(_uiState.value.detectedElements)


    fun generateProgramme(onNavigateToProgrammes: (() -> Unit)? = null) {
        val currentState = _uiState.value

        // Check quota before attempting generation
        if (!quotaManager.canGenerateProgramme()) {
            val quotaStatus = quotaManager.getQuotaStatus()
            _uiState.value =
                currentState.copy(
                    errorMessage = "Daily generation limit reached (${quotaStatus.totalQuota} per day). Please try again tomorrow.",
                )
            return
        }

        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = false) // Don't show loading anymore
            
            try {
                // Create generation request
                val requestId = aiProgrammeRepository.createGenerationRequest(
                    userInput = currentState.inputText,
                    selectedGoal = currentState.selectedGoal?.name,
                    selectedFrequency = currentState.selectedFrequency,
                    selectedDuration = currentState.selectedDuration?.name,
                    selectedExperience = currentState.selectedExperience?.name,
                    selectedEquipment = currentState.selectedEquipment?.name,
                    generationMode = currentState.generationMode.name
                )
                
                // Increment quota usage
                quotaManager.incrementUsage()
                
                // Show toast message
                Toast.makeText(
                    getApplication(),
                    "Your programme is being created! Check the Programmes section",
                    Toast.LENGTH_LONG
                ).show()
                
                // Navigate to programmes screen
                onNavigateToProgrammes?.invoke()
                
            } catch (e: Exception) {
                _uiState.value = currentState.copy(
                    errorMessage = "Failed to start programme generation: ${e.message}"
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
            val goalText =
                when (state.selectedGoal) {
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
            val durationText =
                when (state.selectedDuration) {
                    SessionDuration.QUICK -> "Session duration: 30-45 minutes (quick sessions)"
                    SessionDuration.STANDARD -> "Session duration: 45-60 minutes (standard sessions)"
                    SessionDuration.EXTENDED -> "Session duration: 60-75 minutes (extended sessions)"
                    SessionDuration.LONG -> "Session duration: 75-90 minutes (long sessions)"
                }
            parts.add(durationText)
        }

        if (state.selectedExperience != null) {
            val experienceText =
                when (state.selectedExperience) {
                    ExperienceLevel.BEGINNER -> "Experience level: Beginner (less than 1 year of training)"
                    ExperienceLevel.INTERMEDIATE -> "Experience level: Intermediate (1-3 years of training)"
                    ExperienceLevel.ADVANCED -> "Experience level: Advanced (3-5 years of training)"
                    ExperienceLevel.ELITE -> "Experience level: Elite (5+ years, competitive)"
                }
            parts.add(experienceText)
        }

        if (state.selectedEquipment != null) {
            val equipmentText =
                when (state.selectedEquipment) {
                    EquipmentAvailability.BARBELL_AND_RACK -> "Equipment available: Barbell and rack (home gym setup)"
                    EquipmentAvailability.FULL_GYM -> "Equipment available: Full commercial gym with all equipment"
                    EquipmentAvailability.DUMBBELLS_ONLY -> "Equipment available: Dumbbells only"
                    EquipmentAvailability.BODYWEIGHT -> "Equipment available: Bodyweight only, no equipment"
                    EquipmentAvailability.LIMITED -> "Equipment available: Limited equipment (mix of basics)"
                }
            parts.add(equipmentText)
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
        user1RMs: List<ExerciseMaxWithName>,
    ): String {
        val baseInput = buildEnhancedUserInput(state)
        val parts = mutableListOf(baseInput)

        // Add user's saved 1RMs if available
        if (user1RMs.isNotEmpty()) {
            val maxes =
                user1RMs.joinToString(", ") { max ->
                    "${max.exerciseName}: ${max.maxWeight}kg"
                }
            parts.add("USER'S SAVED 1RMs from their profile: $maxes")
            parts.add("Use these for calculating percentages ONLY if the user hasn't specified exact weights")
        } else {
            parts.add("NO SAVED 1RMs - use average gym-goer weights as specified in guidelines")
        }

        return parts.joinToString("\n\n")
    }

    private suspend fun validateAndMatchExercises(
        response: AIProgrammeResponse,
        exercises: List<com.github.radupana.featherweight.data.exercise.Exercise>,
        aliases: List<com.github.radupana.featherweight.data.exercise.ExerciseAlias>,
    ): ProgrammeValidationResult {
        if (!response.success || response.programme == null) {
            return ProgrammeValidationResult(
                isValid = false,
                validatedExercises = emptyMap(),
                unmatchedExercises = emptyList(),
                validationScore = 0f,
                canProceedWithPartial = false,
                errors = listOf("Invalid programme response")
            )
        }

        val validatedExercises = mutableMapOf<String, com.github.radupana.featherweight.data.exercise.Exercise>()
        val unmatchedExercises = mutableListOf<ExerciseMatchingService.UnmatchedExercise>()

        // Process all exercises in the programme
        response.programme.weeks.forEachIndexed { weekIndex, week ->
            week.workouts.forEachIndexed { workoutIndex, workout ->
                workout.exercises.forEachIndexed { exerciseIndex, exercise ->
                    val match = exerciseMatchingService.findExerciseMatch(
                        aiName = exercise.exerciseName,
                        exercises = exercises,
                        aliases = aliases,
                        minConfidence = 0.7f
                    )
                    
                    if (match != null) {
                        validatedExercises[exercise.exerciseName] = match.exercise
                    } else {
                        // Find best matches for UI
                        val bestMatches = exerciseMatchingService.findBestMatches(
                            aiName = exercise.exerciseName,
                            exercises = exercises,
                            aliases = aliases,
                            limit = 5
                        )
                        
                        val searchHints = exerciseMatchingService.extractSearchHints(exercise.exerciseName)
                        
                        unmatchedExercises.add(
                            ExerciseMatchingService.UnmatchedExercise(
                                aiSuggested = exercise.exerciseName,
                                weekNumber = weekIndex + 1,
                                workoutNumber = workoutIndex + 1,
                                exerciseIndex = exerciseIndex,
                                bestMatches = bestMatches,
                                searchHints = searchHints
                            )
                        )
                    }
                }
            }
        }

        val totalExercises = validatedExercises.size + unmatchedExercises.size
        val validationScore = if (totalExercises > 0) {
            validatedExercises.size.toFloat() / totalExercises
        } else 0f

        return ProgrammeValidationResult(
            isValid = unmatchedExercises.isEmpty(),
            validatedExercises = validatedExercises,
            unmatchedExercises = unmatchedExercises,
            validationScore = validationScore,
            canProceedWithPartial = validationScore >= 0.5f,
            warnings = if (unmatchedExercises.isNotEmpty()) {
                listOf("${unmatchedExercises.size} exercises need to be manually selected")
            } else emptyList()
        )
    }
}
