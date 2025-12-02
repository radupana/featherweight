package com.github.radupana.featherweight.service

import android.util.Base64
import com.github.radupana.featherweight.util.CloudLogger
import com.github.radupana.featherweight.util.ExceptionLogger
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

interface VoiceTranscriber {
    suspend fun transcribe(audioFile: File): Result<String>
}

class VoiceTranscriptionService(
    private val functionCaller: CloudFunctionCaller = FirebaseCloudFunctionCaller(),
) : VoiceTranscriber {
    companion object {
        private const val TAG = "VoiceTranscriptionService"
        private const val FUNCTION_NAME = "transcribeAudio"

        /** Maximum recommended file size (5MB). Larger files may cause memory pressure. */
        private const val RECOMMENDED_MAX_FILE_SIZE = 5 * 1024 * 1024L

        /** Absolute maximum file size (25MB - Whisper API limit) */
        private const val ABSOLUTE_MAX_FILE_SIZE = 25 * 1024 * 1024L
    }

    override suspend fun transcribe(audioFile: File): Result<String> =
        withContext(Dispatchers.IO) {
            CloudLogger.info(TAG, "Starting audio transcription")

            if (!audioFile.exists()) {
                CloudLogger.error(TAG, "Audio file does not exist: ${audioFile.path}")
                return@withContext Result.failure(
                    IllegalArgumentException("Audio file does not exist"),
                )
            }

            val fileSize = audioFile.length()
            if (fileSize > ABSOLUTE_MAX_FILE_SIZE) {
                CloudLogger.error(TAG, "Audio file too large: $fileSize bytes (max: $ABSOLUTE_MAX_FILE_SIZE)")
                return@withContext Result.failure(
                    IllegalArgumentException("Audio file too large. Please record a shorter message."),
                )
            }

            if (fileSize > RECOMMENDED_MAX_FILE_SIZE) {
                CloudLogger.warn(TAG, "Large audio file: $fileSize bytes. May cause memory pressure.")
            }

            try {
                val audioBytes = audioFile.readBytes()
                val audioBase64 = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
                val mimeType = getMimeType(audioFile.name)

                CloudLogger.debug(TAG, "Calling $FUNCTION_NAME, file size: ${audioBytes.size} bytes")

                val data =
                    hashMapOf<String, Any>(
                        "audioBase64" to audioBase64,
                        "mimeType" to mimeType,
                    )

                val resultData = functionCaller.call(FUNCTION_NAME, data)

                if (resultData == null) {
                    CloudLogger.error(TAG, "Null response from Cloud Function")
                    return@withContext Result.failure(
                        IOException("No response from server. Please try again."),
                    )
                }

                @Suppress("UNCHECKED_CAST")
                val responseMap = resultData as? Map<String, Any>
                val transcription = responseMap?.get("text") as? String

                if (transcription.isNullOrEmpty()) {
                    CloudLogger.error(TAG, "Empty transcription in response")
                    return@withContext Result.failure(
                        IOException("Failed to transcribe audio. Please try again."),
                    )
                }

                CloudLogger.info(TAG, "Transcription completed, length: ${transcription.length}")
                Result.success(transcription)
            } catch (e: FirebaseFunctionsException) {
                handleCloudFunctionError(e)
            } catch (e: IOException) {
                ExceptionLogger.logException(TAG, "File read error", e)
                Result.failure(e)
            } catch (e: IllegalStateException) {
                ExceptionLogger.logException(TAG, "Transcription failed", e)
                Result.failure(IOException("Transcription failed. Please try again."))
            }
        }

    private fun getMimeType(fileName: String): String =
        when {
            fileName.endsWith(".m4a") -> "audio/m4a"
            fileName.endsWith(".mp3") -> "audio/mpeg"
            fileName.endsWith(".wav") -> "audio/wav"
            fileName.endsWith(".webm") -> "audio/webm"
            else -> "audio/m4a"
        }

    private fun handleCloudFunctionError(e: FirebaseFunctionsException): Result<String> {
        CloudLogger.error(TAG, "Cloud Function error: ${e.code} - ${e.message}")

        return when (e.code) {
            FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED -> {
                Result.failure(
                    IOException("Voice input quota exceeded. Please try again later."),
                )
            }

            FirebaseFunctionsException.Code.UNAUTHENTICATED -> {
                Result.failure(
                    IOException("Sign in required to use voice input."),
                )
            }

            FirebaseFunctionsException.Code.INVALID_ARGUMENT -> {
                Result.failure(
                    IOException(e.message ?: "Invalid audio file."),
                )
            }

            else -> {
                Result.failure(
                    IOException("Transcription failed. Please try again."),
                )
            }
        }
    }
}
