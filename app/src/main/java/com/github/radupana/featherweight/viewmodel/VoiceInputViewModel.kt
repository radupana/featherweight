package com.github.radupana.featherweight.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.data.exercise.ExerciseWithAliases
import com.github.radupana.featherweight.data.voice.ParsedExerciseData
import com.github.radupana.featherweight.data.voice.ParsedVoiceWorkoutInput
import com.github.radupana.featherweight.data.voice.VoiceInputState
import com.github.radupana.featherweight.model.WeightUnit
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.service.AudioRecorder
import com.github.radupana.featherweight.service.AudioRecordingService
import com.github.radupana.featherweight.service.ExerciseMatchSuggestions
import com.github.radupana.featherweight.service.VoiceExerciseMatchingService
import com.github.radupana.featherweight.service.VoiceParser
import com.github.radupana.featherweight.service.VoiceParsingService
import com.github.radupana.featherweight.service.VoiceTranscriber
import com.github.radupana.featherweight.service.VoiceTranscriptionService
import com.github.radupana.featherweight.util.CloudLogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

data class ConfirmableExercise(
    val parsedData: ParsedExerciseData,
    val matchSuggestions: ExerciseMatchSuggestions,
    val selectedExerciseId: String?,
    val selectedExerciseName: String?,
    val isConfirmed: Boolean,
)

class VoiceInputViewModel(
    application: Application,
    private val repository: FeatherweightRepository,
    private val audioRecorder: AudioRecorder = AudioRecordingService(application),
    private val transcriber: VoiceTranscriber = VoiceTranscriptionService(),
    private val parser: VoiceParser = VoiceParsingService(),
    private val exerciseMatcher: VoiceExerciseMatchingService = VoiceExerciseMatchingService(),
) : AndroidViewModel(application) {
    constructor(application: Application) : this(
        application,
        FeatherweightRepository(application),
    )

    companion object {
        private const val TAG = "VoiceInputViewModel"

        /** Maximum recording duration in milliseconds (2 minutes) */
        private const val MAX_RECORDING_DURATION_MS = 2 * 60 * 1000L

        /** Amplitude polling interval in milliseconds */
        private const val AMPLITUDE_POLLING_INTERVAL_MS = 100L
    }

    init {
        // Clean up any orphaned audio files from previous crashed sessions
        audioRecorder.cleanupOrphanedFiles()
    }

    private val _voiceInputState = MutableStateFlow<VoiceInputState>(VoiceInputState.Idle)
    val voiceInputState: StateFlow<VoiceInputState> = _voiceInputState

    private val _confirmableExercises = MutableStateFlow<List<ConfirmableExercise>>(emptyList())
    val confirmableExercises: StateFlow<List<ConfirmableExercise>> = _confirmableExercises

    private val allExercises = MutableStateFlow<List<ExerciseWithAliases>>(emptyList())

    private val mutableAmplitude = MutableStateFlow(0)
    val amplitude: StateFlow<Int> = mutableAmplitude

    /**
     * Tracks which voice exercise index is pending selection from the exercise selector screen.
     * When non-null, the next exercise selection should update this voice exercise instead of
     * adding a new exercise to the workout.
     */
    private val _pendingVoiceExerciseIndex = MutableStateFlow<Int?>(null)
    val pendingVoiceExerciseIndex: StateFlow<Int?> = _pendingVoiceExerciseIndex

    private var amplitudePollingJob: Job? = null
    private var maxDurationJob: Job? = null
    private var currentAudioFile: File? = null
    private var preferredWeightUnit: WeightUnit = WeightUnit.KG

    fun setPreferredWeightUnit(unit: WeightUnit) {
        preferredWeightUnit = unit
    }

    fun startRecording() {
        viewModelScope.launch {
            // Show preparing state while loading exercises
            _voiceInputState.value = VoiceInputState.Preparing

            // Block on exercise loading BEFORE starting recording to prevent race condition
            // where transcription+parsing completes before exercises are loaded
            loadExercisesIfNeeded()

            val result = audioRecorder.startRecording()
            result.fold(
                onSuccess = { file ->
                    currentAudioFile = file
                    _voiceInputState.value = VoiceInputState.Listening
                    startAmplitudePolling()
                    startMaxDurationTimer()
                    CloudLogger.info(TAG, "Recording started")
                },
                onFailure = { error ->
                    _voiceInputState.value =
                        VoiceInputState.Error(
                            message = error.message ?: "Failed to start recording",
                            canRetry = true,
                        )
                    CloudLogger.error(TAG, "Failed to start recording", error)
                },
            )
        }
    }

    fun stopRecording() {
        viewModelScope.launch {
            stopAmplitudePolling()
            stopMaxDurationTimer()

            val result = audioRecorder.stopRecording()
            result.fold(
                onSuccess = { file ->
                    currentAudioFile = file
                    processRecording(file)
                },
                onFailure = { error ->
                    _voiceInputState.value =
                        VoiceInputState.Error(
                            message = error.message ?: "Failed to stop recording",
                            canRetry = true,
                        )
                    CloudLogger.error(TAG, "Failed to stop recording", error)
                },
            )
        }
    }

    fun cancelRecording() {
        stopAmplitudePolling()
        stopMaxDurationTimer()
        audioRecorder.cancelRecording()
        currentAudioFile?.delete()
        currentAudioFile = null
        _voiceInputState.value = VoiceInputState.Idle
        _confirmableExercises.value = emptyList()
        CloudLogger.info(TAG, "Recording cancelled")
    }

    fun retry() {
        _voiceInputState.value = VoiceInputState.Idle
        _confirmableExercises.value = emptyList()
    }

    fun selectExerciseForParsed(
        parsedIndex: Int,
        exerciseId: String,
        exerciseName: String,
    ) {
        val current = _confirmableExercises.value.toMutableList()
        if (parsedIndex in current.indices) {
            current[parsedIndex] =
                current[parsedIndex].copy(
                    selectedExerciseId = exerciseId,
                    selectedExerciseName = exerciseName,
                )
            _confirmableExercises.value = current
        }
    }

    fun confirmExercise(parsedIndex: Int) {
        val current = _confirmableExercises.value.toMutableList()
        if (parsedIndex in current.indices && current[parsedIndex].selectedExerciseId != null) {
            current[parsedIndex] = current[parsedIndex].copy(isConfirmed = true)
            _confirmableExercises.value = current
        }
    }

    fun removeExercise(parsedIndex: Int) {
        val current = _confirmableExercises.value.toMutableList()
        if (parsedIndex in current.indices) {
            current.removeAt(parsedIndex)
            _confirmableExercises.value = current
        }
    }

    fun getConfirmedExercises(): List<ConfirmableExercise> = _confirmableExercises.value.filter { it.isConfirmed && it.selectedExerciseId != null }

    fun getAllReadyExercises(): List<ConfirmableExercise> = _confirmableExercises.value.filter { it.selectedExerciseId != null }

    fun areAllExercisesReady(): Boolean =
        _confirmableExercises.value.isNotEmpty() &&
            _confirmableExercises.value.all { it.selectedExerciseId != null }

    /**
     * Sets the pending voice exercise index before navigating to exercise selector.
     * Call this when user clicks "Search all exercises..." from the voice confirmation dialog.
     */
    fun setPendingVoiceExerciseIndex(index: Int) {
        _pendingVoiceExerciseIndex.value = index
        CloudLogger.debug(TAG, "Set pending voice exercise index: $index")
    }

    /**
     * Called when user selects an exercise from the exercise selector screen
     * while in voice input mode. Updates the confirmable exercise and confirms it.
     */
    fun handleExerciseSelectedFromSearch(
        exerciseId: String,
        exerciseName: String,
    ) {
        val index = _pendingVoiceExerciseIndex.value
        if (index != null && index in _confirmableExercises.value.indices) {
            selectExerciseForParsed(index, exerciseId, exerciseName)
            confirmExercise(index)
            CloudLogger.info(TAG, "Updated voice exercise at index $index to: $exerciseName")
        }
        _pendingVoiceExerciseIndex.value = null
    }

    /**
     * Clears the pending voice exercise index (e.g., when user cancels).
     */
    fun clearPendingVoiceExerciseIndex() {
        _pendingVoiceExerciseIndex.value = null
    }

    fun dismiss() {
        currentAudioFile?.delete()
        currentAudioFile = null
        _voiceInputState.value = VoiceInputState.Idle
        _confirmableExercises.value = emptyList()
        _pendingVoiceExerciseIndex.value = null
    }

    private fun processRecording(audioFile: File) {
        viewModelScope.launch {
            _voiceInputState.value = VoiceInputState.Transcribing(null)

            val transcriptionResult = transcriber.transcribe(audioFile)
            transcriptionResult.fold(
                onSuccess = { transcription ->
                    parseTranscription(transcription)
                },
                onFailure = { error ->
                    _voiceInputState.value =
                        VoiceInputState.Error(
                            message = error.message ?: "Transcription failed",
                            canRetry = true,
                        )
                    CloudLogger.error(TAG, "Transcription failed", error)
                },
            )
        }
    }

    private suspend fun parseTranscription(transcription: String) {
        _voiceInputState.value = VoiceInputState.Parsing(transcription)

        val parsingResult = parser.parseTranscription(transcription, preferredWeightUnit)
        parsingResult.fold(
            onSuccess = { parsed ->
                processParseResult(parsed)
            },
            onFailure = { error ->
                _voiceInputState.value =
                    VoiceInputState.Error(
                        message = error.message ?: "Parsing failed",
                        canRetry = true,
                    )
                CloudLogger.error(TAG, "Parsing failed", error)
            },
        )
    }

    private fun processParseResult(parsed: ParsedVoiceWorkoutInput) {
        if (parsed.exercises.isEmpty()) {
            _voiceInputState.value =
                VoiceInputState.Error(
                    message = "No exercises detected in your recording",
                    canRetry = true,
                )
            return
        }

        val exercises = allExercises.value
        val confirmableList =
            parsed.exercises.map { exerciseData ->
                val matchSuggestions =
                    exerciseMatcher.findMatchesForExercise(
                        spokenName = exerciseData.spokenName,
                        interpretedName = exerciseData.interpretedName,
                        allExercises = exercises,
                    )

                val autoMatch = matchSuggestions.bestMatch?.takeIf { it.isAutoMatch }

                ConfirmableExercise(
                    parsedData =
                        exerciseData.copy(
                            matchedExerciseId = autoMatch?.exerciseId,
                            matchedExerciseName = autoMatch?.exerciseName,
                        ),
                    matchSuggestions = matchSuggestions,
                    selectedExerciseId = autoMatch?.exerciseId,
                    selectedExerciseName = autoMatch?.exerciseName,
                    isConfirmed = autoMatch != null,
                )
            }

        _confirmableExercises.value = confirmableList
        _voiceInputState.value = VoiceInputState.Ready(parsed)
        CloudLogger.info(TAG, "Parsed ${parsed.exercises.size} exercises")
    }

    /**
     * Lazily loads exercises when recording starts.
     * This gives ample time for the DB query to complete before exercises are needed
     * in processParseResult() (after transcription + parsing complete).
     * Preloading in init would cause unnecessary DB queries if the user opens
     * the voice modal but doesn't record anything.
     */
    private suspend fun loadExercisesIfNeeded() {
        if (allExercises.value.isEmpty()) {
            allExercises.value = repository.getAllExercisesWithAliases()
            CloudLogger.info(TAG, "Loaded ${allExercises.value.size} exercises for matching")
        }
    }

    private fun startAmplitudePolling() {
        amplitudePollingJob =
            viewModelScope.launch {
                while (audioRecorder.isRecording()) {
                    mutableAmplitude.value = audioRecorder.getAmplitude()
                    // 100ms polling interval provides smooth visual feedback for the pulsing
                    // microphone animation (10fps) while keeping CPU overhead minimal.
                    // MediaRecorder.getMaxAmplitude() returns the max amplitude since the last
                    // call, so this interval also determines the sampling window.
                    kotlinx.coroutines.delay(AMPLITUDE_POLLING_INTERVAL_MS)
                }
            }
    }

    private fun stopAmplitudePolling() {
        amplitudePollingJob?.cancel()
        amplitudePollingJob = null
        mutableAmplitude.value = 0
    }

    /**
     * Starts a timer that automatically stops recording after MAX_RECORDING_DURATION_MS.
     * This prevents excessive battery drain and ensures reasonable audio file sizes.
     */
    private fun startMaxDurationTimer() {
        maxDurationJob =
            viewModelScope.launch {
                kotlinx.coroutines.delay(MAX_RECORDING_DURATION_MS)
                if (audioRecorder.isRecording()) {
                    CloudLogger.info(TAG, "Max recording duration reached, auto-stopping")
                    stopRecording()
                }
            }
    }

    private fun stopMaxDurationTimer() {
        maxDurationJob?.cancel()
        maxDurationJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopAmplitudePolling()
        stopMaxDurationTimer()
        if (audioRecorder.isRecording()) {
            audioRecorder.cancelRecording()
        }
        currentAudioFile?.delete()
    }
}
