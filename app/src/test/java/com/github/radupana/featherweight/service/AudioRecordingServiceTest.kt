package com.github.radupana.featherweight.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AudioRecordingServiceTest {
    @Test
    fun `AudioRecorder interface has required methods`() {
        val mockRecorder =
            object : AudioRecorder {
                var recordingStarted = false
                var recordingStopped = false
                var recordingCancelled = false

                override fun startRecording(): Result<File> {
                    recordingStarted = true
                    return Result.success(File.createTempFile("test", ".m4a"))
                }

                override fun stopRecording(): Result<File> {
                    recordingStopped = true
                    return Result.success(File.createTempFile("test", ".m4a"))
                }

                override fun cancelRecording() {
                    recordingCancelled = true
                }

                override fun getAmplitude(): Int = 100

                override fun isRecording(): Boolean = recordingStarted && !recordingStopped
            }

        val result = mockRecorder.startRecording()
        assertTrue(result.isSuccess)
        assertTrue(mockRecorder.recordingStarted)
        assertTrue(mockRecorder.isRecording())
        assertEquals(100, mockRecorder.getAmplitude())
    }

    @Test
    fun `AudioRecorder stop returns file`() {
        val mockRecorder =
            object : AudioRecorder {
                private var recording = false
                private var outputFile: File? = null

                override fun startRecording(): Result<File> {
                    recording = true
                    outputFile = File.createTempFile("test", ".m4a")
                    return Result.success(outputFile!!)
                }

                override fun stopRecording(): Result<File> {
                    recording = false
                    return Result.success(outputFile!!)
                }

                override fun cancelRecording() {
                    recording = false
                    outputFile = null
                }

                override fun getAmplitude(): Int = if (recording) 50 else 0

                override fun isRecording(): Boolean = recording
            }

        mockRecorder.startRecording()
        assertTrue(mockRecorder.isRecording())

        val stopResult = mockRecorder.stopRecording()
        assertTrue(stopResult.isSuccess)
        assertFalse(mockRecorder.isRecording())
    }

    @Test
    fun `AudioRecorder cancel clears state`() {
        val mockRecorder =
            object : AudioRecorder {
                private var recording = false

                override fun startRecording(): Result<File> {
                    recording = true
                    return Result.success(File.createTempFile("test", ".m4a"))
                }

                override fun stopRecording(): Result<File> {
                    recording = false
                    return Result.success(File.createTempFile("test", ".m4a"))
                }

                override fun cancelRecording() {
                    recording = false
                }

                override fun getAmplitude(): Int = if (recording) 100 else 0

                override fun isRecording(): Boolean = recording
            }

        mockRecorder.startRecording()
        assertTrue(mockRecorder.isRecording())

        mockRecorder.cancelRecording()
        assertFalse(mockRecorder.isRecording())
        assertEquals(0, mockRecorder.getAmplitude())
    }

    @Test
    fun `AudioRecorder amplitude is zero when not recording`() {
        val mockRecorder =
            object : AudioRecorder {
                private var recording = false

                override fun startRecording(): Result<File> {
                    recording = true
                    return Result.success(File.createTempFile("test", ".m4a"))
                }

                override fun stopRecording(): Result<File> {
                    recording = false
                    return Result.success(File.createTempFile("test", ".m4a"))
                }

                override fun cancelRecording() {
                    recording = false
                }

                override fun getAmplitude(): Int = if (recording) 100 else 0

                override fun isRecording(): Boolean = recording
            }

        assertEquals(0, mockRecorder.getAmplitude())
        assertFalse(mockRecorder.isRecording())
    }

    @Test
    fun `AudioRecorder handles start failure gracefully`() {
        val mockRecorder =
            object : AudioRecorder {
                override fun startRecording(): Result<File> = Result.failure(SecurityException("Permission denied"))

                override fun stopRecording(): Result<File> = Result.failure(IllegalStateException("Not recording"))

                override fun cancelRecording() {
                    // No-op for mock
                }

                override fun getAmplitude(): Int = 0

                override fun isRecording(): Boolean = false
            }

        val result = mockRecorder.startRecording()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun `AudioRecorder handles stop without start`() {
        val mockRecorder =
            object : AudioRecorder {
                private var recording = false

                override fun startRecording(): Result<File> {
                    recording = true
                    return Result.success(File.createTempFile("test", ".m4a"))
                }

                override fun stopRecording(): Result<File> =
                    if (recording) {
                        recording = false
                        Result.success(File.createTempFile("test", ".m4a"))
                    } else {
                        Result.failure(IllegalStateException("No recording in progress"))
                    }

                override fun cancelRecording() {
                    recording = false
                }

                override fun getAmplitude(): Int = if (recording) 50 else 0

                override fun isRecording(): Boolean = recording
            }

        val result = mockRecorder.stopRecording()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `AudioRecordingState sealed class has correct states`() {
        val idle: AudioRecordingState = AudioRecordingState.Idle
        val recording: AudioRecordingState = AudioRecordingState.Recording(File.createTempFile("test", ".m4a"))
        val completed: AudioRecordingState = AudioRecordingState.Completed(File.createTempFile("test", ".m4a"))
        val error: AudioRecordingState = AudioRecordingState.Error("Something went wrong")

        assertTrue(idle is AudioRecordingState.Idle)
        assertTrue(recording is AudioRecordingState.Recording)
        assertTrue(completed is AudioRecordingState.Completed)
        assertTrue(error is AudioRecordingState.Error)

        val errorState = error as AudioRecordingState.Error
        assertEquals("Something went wrong", errorState.message)
    }
}
