@file:Suppress("TooGenericExceptionCaught")

package com.github.radupana.featherweight.service

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.github.radupana.featherweight.util.CloudLogger
import java.io.File
import java.io.IOException

interface AudioRecorder {
    fun startRecording(): Result<File>

    fun stopRecording(): Result<File>

    fun cancelRecording()

    fun getAmplitude(): Int

    fun isRecording(): Boolean
}

sealed class AudioRecordingState {
    data object Idle : AudioRecordingState()

    data class Recording(
        val outputFile: File,
    ) : AudioRecordingState()

    data class Completed(
        val audioFile: File,
    ) : AudioRecordingState()

    data class Error(
        val message: String,
    ) : AudioRecordingState()
}

class AudioRecordingService(
    private val context: Context,
) : AudioRecorder {
    companion object {
        private const val TAG = "AudioRecordingService"
        private const val AUDIO_FILE_PREFIX = "voice_workout_"
        private const val AUDIO_FILE_SUFFIX = ".m4a"
        private const val SAMPLE_RATE = 44100
        private const val BIT_RATE = 128000
    }

    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var isCurrentlyRecording = false

    override fun startRecording(): Result<File> {
        if (isCurrentlyRecording) {
            return Result.failure(IllegalStateException("Recording already in progress"))
        }

        return try {
            val file = createTempAudioFile()
            outputFile = file

            @Suppress("DEPRECATION")
            val recorder =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(context)
                } else {
                    MediaRecorder()
                }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(SAMPLE_RATE)
                setAudioEncodingBitRate(BIT_RATE)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            isCurrentlyRecording = true
            CloudLogger.info(TAG, "Recording started: ${file.name}")
            Result.success(file)
        } catch (e: IOException) {
            CloudLogger.error(TAG, "Failed to start recording - IO error", e)
            cleanup()
            Result.failure(e)
        } catch (e: IllegalStateException) {
            CloudLogger.error(TAG, "Failed to start recording - illegal state", e)
            cleanup()
            Result.failure(e)
        } catch (e: SecurityException) {
            CloudLogger.error(TAG, "Failed to start recording - permission denied", e)
            cleanup()
            Result.failure(e)
        } catch (e: RuntimeException) {
            CloudLogger.error(TAG, "Failed to start recording - audio source unavailable", e)
            cleanup()
            Result.failure(e)
        }
    }

    override fun stopRecording(): Result<File> {
        if (!isCurrentlyRecording) {
            return Result.failure(IllegalStateException("No recording in progress"))
        }

        val file = outputFile ?: return Result.failure(IllegalStateException("Output file not set"))

        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isCurrentlyRecording = false
            CloudLogger.info(TAG, "Recording stopped: ${file.name}")
            Result.success(file)
        } catch (e: IllegalStateException) {
            CloudLogger.error(TAG, "Failed to stop recording", e)
            cleanup()
            Result.failure(e)
        } catch (e: RuntimeException) {
            CloudLogger.error(TAG, "Failed to stop recording - runtime error", e)
            cleanup()
            Result.failure(e)
        }
    }

    override fun cancelRecording() {
        CloudLogger.info(TAG, "Recording cancelled")
        cleanup()
    }

    override fun getAmplitude(): Int =
        try {
            if (isCurrentlyRecording) {
                mediaRecorder?.maxAmplitude ?: 0
            } else {
                0
            }
        } catch (e: IllegalStateException) {
            // Expected when recording is stopped - return 0 silently
            CloudLogger.debug(TAG, "getAmplitude called when recorder not ready: ${e.message}")
            0
        }

    override fun isRecording(): Boolean = isCurrentlyRecording

    private fun createTempAudioFile(): File {
        val cacheDir = context.cacheDir
        return File.createTempFile(
            AUDIO_FILE_PREFIX,
            AUDIO_FILE_SUFFIX,
            cacheDir,
        )
    }

    private fun cleanup() {
        try {
            mediaRecorder?.release()
        } catch (e: IllegalStateException) {
            CloudLogger.debug(TAG, "MediaRecorder already released: ${e.message}")
        }
        mediaRecorder = null
        isCurrentlyRecording = false
        outputFile?.let { file ->
            if (file.exists()) {
                file.delete()
            }
        }
        outputFile = null
    }
}
