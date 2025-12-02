package com.github.radupana.featherweight.data.voice

sealed class VoiceInputState {
    data object Idle : VoiceInputState()

    data object Listening : VoiceInputState()

    data class Transcribing(
        val partialText: String?,
    ) : VoiceInputState()

    data class Parsing(
        val transcription: String,
    ) : VoiceInputState()

    data class Ready(
        val result: ParsedVoiceWorkoutInput,
    ) : VoiceInputState()

    data class Error(
        val message: String,
        val canRetry: Boolean,
    ) : VoiceInputState()
}
