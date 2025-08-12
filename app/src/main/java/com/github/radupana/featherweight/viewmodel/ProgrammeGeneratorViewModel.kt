package com.github.radupana.featherweight.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.github.radupana.featherweight.ai.WeightExtractionService
import com.github.radupana.featherweight.data.EquipmentAvailability
import com.github.radupana.featherweight.data.ExperienceLevel
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.GuidedInputState
import com.github.radupana.featherweight.data.ProgrammeGoal
import com.github.radupana.featherweight.data.ProgrammeValidationResult
import com.github.radupana.featherweight.data.SessionDuration
import com.github.radupana.featherweight.data.TrainingFrequency
import com.github.radupana.featherweight.data.WizardStep
import com.github.radupana.featherweight.repository.AIProgrammeRepository
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.service.AIProgrammeQuotaManager
import com.github.radupana.featherweight.service.AIProgrammeResponse
import com.github.radupana.featherweight.service.AIProgrammeService
import com.github.radupana.featherweight.service.ExerciseMatchingService
import com.github.radupana.featherweight.service.InputAnalyzer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProgrammeGeneratorViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = FeatherweightRepository(application)
    private val database = FeatherweightDatabase.getDatabase(application)
    private val aiProgrammeRepository =
        AIProgrammeRepository(
            database.aiProgrammeRequestDao(),
            WorkManager.getInstance(application),
        )
    private val aiService = AIProgrammeService(application)
    private val quotaManager = AIProgrammeQuotaManager(application)
    private val inputAnalyzer = InputAnalyzer()
    private val exerciseMatchingService = ExerciseMatchingService()
    private val weightExtractor = WeightExtractionService()

    private val _uiState = MutableStateFlow(GuidedInputState())
    val uiState = _uiState.asStateFlow()

    fun resetState() {
        _uiState.value = GuidedInputState()
    }

    fun updateCustomInstructions(text: String) {
        _uiState.value =
            _uiState.value.copy(
                customInstructions = text,
                errorMessage = null,
            )
    }

    // GenerationMode selection removed - only using simplified approach

    fun selectGoal(goal: ProgrammeGoal) {
        val currentState = _uiState.value
        // Toggle selection - if already selected, deselect it
        val newGoal = if (currentState.selectedGoal == goal) null else goal
        _uiState.value = currentState.copy(selectedGoal = newGoal)

        // Goal selection is now simpler without input analysis
    }

    fun selectFrequency(frequency: TrainingFrequency) {
        val currentState = _uiState.value
        // Toggle selection - if already selected, deselect it
        val newFrequency = if (currentState.selectedFrequency == frequency) null else frequency
        _uiState.value = currentState.copy(selectedFrequency = newFrequency)
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

    // Wizard navigation functions
    fun navigateToNextStep() {
        val currentState = _uiState.value
        when (currentState.currentStep) {
            WizardStep.QUICK_SETUP -> {
                // Only proceed if required fields are filled
                if (currentState.selectedGoal != null &&
                    currentState.selectedFrequency != null &&
                    currentState.selectedDuration != null
                ) {
                    _uiState.value = currentState.copy(currentStep = WizardStep.ABOUT_YOU)
                }
            }

            WizardStep.ABOUT_YOU -> {
                // Only proceed if required fields are filled
                if (currentState.selectedExperience != null &&
                    currentState.selectedEquipment != null
                ) {
                    _uiState.value = currentState.copy(currentStep = WizardStep.CUSTOMIZE)
                }
            }

            WizardStep.CUSTOMIZE -> {
                // This is the last step, don't navigate further
            }
        }
    }

    fun navigateToPreviousStep() {
        val currentState = _uiState.value
        when (currentState.currentStep) {
            WizardStep.QUICK_SETUP -> {
                // This is the first step, can't go back
            }

            WizardStep.ABOUT_YOU -> {
                _uiState.value = currentState.copy(currentStep = WizardStep.QUICK_SETUP)
            }

            WizardStep.CUSTOMIZE -> {
                _uiState.value = currentState.copy(currentStep = WizardStep.ABOUT_YOU)
            }
        }
    }

    fun navigateToStep(step: WizardStep) {
        _uiState.value = _uiState.value.copy(currentStep = step)
    }

    // Advanced options toggle removed - only using simplified approach

    fun canProceedToNextStep(): Boolean {
        val state = _uiState.value
        return when (state.currentStep) {
            WizardStep.QUICK_SETUP -> {
                state.selectedGoal != null &&
                    state.selectedFrequency != null &&
                    state.selectedDuration != null
            }

            WizardStep.ABOUT_YOU -> {
                state.selectedExperience != null &&
                    state.selectedEquipment != null
            }

            WizardStep.CUSTOMIZE -> true // Can always generate from customize step
        }
    }

    fun isStepCompleted(step: WizardStep): Boolean {
        val state = _uiState.value
        return when (step) {
            WizardStep.QUICK_SETUP -> {
                state.selectedGoal != null &&
                    state.selectedFrequency != null &&
                    state.selectedDuration != null
            }

            WizardStep.ABOUT_YOU -> {
                state.selectedExperience != null &&
                    state.selectedEquipment != null
            }

            WizardStep.CUSTOMIZE -> state.customInstructions.isNotEmpty()
        }
    }

    // Input analyzer removed - using simplified approach

    // Get contextual hints based on current selections
    fun getContextualHints(): List<String> {
        val state = _uiState.value
        val hints = mutableListOf<String>()

        when (state.currentStep) {
            WizardStep.CUSTOMIZE -> {
                // Add hints based on what user has selected
                state.selectedGoal?.let { goal ->
                    when (goal) {
                        ProgrammeGoal.BUILD_STRENGTH -> {
                            hints.add("Consider mentioning your current 1RM numbers")
                            hints.add("Specify if you prefer powerlifting or Olympic lifting")
                        }

                        ProgrammeGoal.BUILD_MUSCLE -> {
                            hints.add("Mention any muscle groups you want to prioritize")
                            hints.add("Include your preferred training split if you have one")
                        }

                        ProgrammeGoal.LOSE_FAT -> {
                            hints.add("Note if you're doing any cardio alongside weight training")
                            hints.add("Mention if you have any dietary restrictions")
                        }

                        ProgrammeGoal.ATHLETIC_PERFORMANCE -> {
                            hints.add("Specify which sport or activity you're training for")
                            hints.add("Include any speed/agility work requirements")
                        }
                    }
                }

                // Add equipment-specific hints
                state.selectedEquipment?.let { equipment ->
                    when (equipment) {
                        EquipmentAvailability.LIMITED -> {
                            hints.add("List the specific equipment you have available")
                        }

                        EquipmentAvailability.BODYWEIGHT -> {
                            hints.add("Mention if you have access to pull-up bars or dip stations")
                        }
                        else -> { /* Other equipment types don't need specific hints */ }
                    }
                }

                // Generic helpful hints
                hints.add("Mention any injuries or areas to work around")
                hints.add("Note your schedule preferences or time constraints")
            }
            else -> { /* Other steps don't need hints */ }
        }

        return hints
    }

    // Get example prompts for custom instructions
    fun getExamplePrompts(): List<String> {
        val state = _uiState.value
        val examples = mutableListOf<String>()

        when (state.selectedGoal) {
            ProgrammeGoal.BUILD_STRENGTH -> {
                examples.add("I've been stuck at a 315lb squat for months and want to break through")
                examples.add("My deadlift is weak compared to my squat, need to bring it up")
                examples.add("Want to compete in powerlifting next year")
            }

            ProgrammeGoal.BUILD_MUSCLE -> {
                examples.add("My chest is lagging, want extra volume there")
                examples.add("Prefer higher rep ranges (8-12) for hypertrophy")
                examples.add("Want to focus on arms and shoulders this cycle")
            }

            ProgrammeGoal.LOSE_FAT -> {
                examples.add("Need to maintain strength while cutting 20 pounds")
                examples.add("Prefer circuit-style training to keep heart rate up")
                examples.add("Limited to 45 minutes per session due to schedule")
            }

            ProgrammeGoal.ATHLETIC_PERFORMANCE -> {
                examples.add("Training for basketball season, need explosive power")
                examples.add("Rugby player needing strength and conditioning")
                examples.add("Want to improve my 40-yard dash time")
            }

            else -> {
                examples.add("Recovering from shoulder injury, need modifications")
                examples.add("Want to focus on functional fitness and mobility")
                examples.add("Training for a specific event or competition")
            }
        }

        return examples
    }

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
            // Check if there's already an active programme
            val activeProgramme = repository.getActiveProgramme()
            if (activeProgramme != null) {
                _uiState.value =
                    currentState.copy(
                        errorMessage = "You already have an active programme: ${activeProgramme.name}. Please delete it before generating a new one.",
                    )
                return@launch
            }
            _uiState.value = currentState.copy(isLoading = false) // Don't show loading anymore

            try {
                // Fetch user's current 1RMs
                val userId = repository.getCurrentUserId()
                val maxes = repository.getAllCurrentMaxesWithNames(userId).first()
                val user1RMs =
                    maxes.associate { max ->
                        max.exerciseName to max.oneRMEstimate
                    }

                // Create generation request
                aiProgrammeRepository.createGenerationRequest(
                    userInput = buildFullUserInput(currentState),
                    selectedGoal = currentState.selectedGoal?.name,
                    selectedFrequency = currentState.selectedFrequency?.daysPerWeek,
                    selectedDuration = currentState.selectedDuration?.name,
                    selectedExperience = currentState.selectedExperience?.name,
                    selectedEquipment = currentState.selectedEquipment?.name,
                    generationMode = "SIMPLIFIED",
                    user1RMs = user1RMs.ifEmpty { null },
                )

                // Increment quota usage
                quotaManager.incrementUsage()

                // Show toast message
                Toast
                    .makeText(
                        getApplication(),
                        "Your programme is being created! Check the Programmes section",
                        Toast.LENGTH_LONG,
                    ).show()

                // Navigate to programmes screen
                onNavigateToProgrammes?.invoke()
            } catch (e: Exception) {
                _uiState.value =
                    currentState.copy(
                        errorMessage = "Failed to start programme generation: ${e.message}",
                    )
            }
        }
    }

    private fun buildFullUserInput(state: GuidedInputState): String = buildEnhancedUserInput(state)

    private fun buildEnhancedUserInput(state: GuidedInputState): String {
        val parts = mutableListOf<String>()

        // Add user's custom instructions
        if (state.customInstructions.isNotBlank()) {
            parts.add(state.customInstructions.trim())
        }

        // Add structured selections
        if (state.selectedGoal != null) {
            val goalText =
                when (state.selectedGoal) {
                    ProgrammeGoal.BUILD_STRENGTH -> "Primary goal: Build maximum strength"
                    ProgrammeGoal.BUILD_MUSCLE -> "Primary goal: Build muscle mass and size"
                    ProgrammeGoal.LOSE_FAT -> "Primary goal: Lose fat while maintaining muscle"
                    ProgrammeGoal.ATHLETIC_PERFORMANCE -> "Primary goal: Improve athletic performance"
                }
            parts.add(goalText)
        }

        if (state.selectedFrequency != null) {
            parts.add("Training frequency: ${state.selectedFrequency.daysPerWeek} days per week")
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

        // Simplified approach - no element detection needed

        // No need to extract weights - let AI handle it directly

        return if (parts.isNotEmpty()) {
            parts.joinToString("\n\n")
        } else {
            "Create a general fitness programme suitable for someone looking to improve their overall health and fitness."
        }
    }

    private suspend fun validateAndMatchExercises(
        response: AIProgrammeResponse,
        exercises: List<com.github.radupana.featherweight.data.exercise.ExerciseVariation>,
        aliases: List<com.github.radupana.featherweight.data.exercise.VariationAlias>,
    ): ProgrammeValidationResult {
        if (!response.success || response.programme == null) {
            return ProgrammeValidationResult(
                isValid = false,
                validatedExercises = emptyMap(),
                unmatchedExercises = emptyList(),
                validationScore = 0f,
                canProceedWithPartial = false,
                errors = listOf("Invalid programme response"),
            )
        }

        val validatedExercises = mutableMapOf<String, com.github.radupana.featherweight.data.exercise.ExerciseVariation>()
        val unmatchedExercises = mutableListOf<ExerciseMatchingService.UnmatchedExercise>()

        // Process all exercises in the programme
        response.programme.weeks.forEachIndexed { weekIndex, week ->
            week.workouts.forEachIndexed { workoutIndex, workout ->
                workout.exercises.forEachIndexed { exerciseIndex, exercise ->
                    val match =
                        exerciseMatchingService.findExerciseMatch(
                            aiName = exercise.exerciseName,
                            exercises = exercises,
                            aliases = aliases,
                            minConfidence = 0.7f,
                        )

                    if (match != null) {
                        validatedExercises[exercise.exerciseName] = match.exercise
                    } else {
                        // Find best matches for UI
                        val bestMatches =
                            exerciseMatchingService.findBestMatches(
                                aiName = exercise.exerciseName,
                                exercises = exercises,
                                aliases = aliases,
                                limit = 5,
                            )

                        val searchHints = exerciseMatchingService.extractSearchHints(exercise.exerciseName)

                        unmatchedExercises.add(
                            ExerciseMatchingService.UnmatchedExercise(
                                aiSuggested = exercise.exerciseName,
                                weekNumber = weekIndex + 1,
                                workoutNumber = workoutIndex + 1,
                                exerciseIndex = exerciseIndex,
                                bestMatches = bestMatches,
                                searchHints = searchHints,
                            ),
                        )
                    }
                }
            }
        }

        val totalExercises = validatedExercises.size + unmatchedExercises.size
        val validationScore =
            if (totalExercises > 0) {
                validatedExercises.size.toFloat() / totalExercises
            } else {
                0f
            }

        return ProgrammeValidationResult(
            isValid = unmatchedExercises.isEmpty(),
            validatedExercises = validatedExercises,
            unmatchedExercises = unmatchedExercises,
            validationScore = validationScore,
            canProceedWithPartial = validationScore >= 0.5f,
            warnings =
                if (unmatchedExercises.isNotEmpty()) {
                    listOf("${unmatchedExercises.size} exercises need to be manually selected")
                } else {
                    emptyList()
                },
        )
    }
}
