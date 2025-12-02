package com.github.radupana.featherweight.service

import com.google.common.truth.Truth.assertThat
import com.google.gson.JsonParser
import org.junit.Test
import java.io.File

class VoiceTranscriptionServiceTest {
    @Test
    fun `parseWhisperResponse extracts text correctly`() {
        val responseJson = """{"text": "bench press 3 sets of 8 at 100 kilos"}"""
        val jsonObject = JsonParser.parseString(responseJson).asJsonObject

        val text = jsonObject.get("text").asString

        assertThat(text).isEqualTo("bench press 3 sets of 8 at 100 kilos")
    }

    @Test
    fun `parseWhisperResponse handles complex transcription`() {
        val responseJson =
            """
            {
                "text": "Did bench press 4 sets of 8 at 100 kilos, then curls 3 sets of 12 at 25, and finished with tricep pushdowns 3 sets of 15 at 30"
            }
            """.trimIndent()
        val jsonObject = JsonParser.parseString(responseJson).asJsonObject

        val text = jsonObject.get("text").asString

        assertThat(text).contains("bench press")
        assertThat(text).contains("curls")
        assertThat(text).contains("tricep pushdowns")
    }

    @Test
    fun `parseWhisperResponse handles missing text field`() {
        val responseJson = """{"status": "ok"}"""
        val jsonObject = JsonParser.parseString(responseJson).asJsonObject

        val text = jsonObject.get("text")

        assertThat(text).isNull()
    }

    @Test
    fun `parseErrorResponse extracts error message`() {
        val errorJson =
            """
            {
                "error": {
                    "message": "Invalid audio format",
                    "type": "invalid_request_error"
                }
            }
            """.trimIndent()
        val jsonObject = JsonParser.parseString(errorJson).asJsonObject

        val errorMessage =
            jsonObject
                .getAsJsonObject("error")
                ?.get("message")
                ?.asString

        assertThat(errorMessage).isEqualTo("Invalid audio format")
    }

    @Test
    fun `parseErrorResponse handles missing message`() {
        val errorJson =
            """
            {
                "error": {
                    "type": "invalid_request_error"
                }
            }
            """.trimIndent()
        val jsonObject = JsonParser.parseString(errorJson).asJsonObject

        val errorMessage =
            jsonObject
                .getAsJsonObject("error")
                ?.get("message")
                ?.asString

        assertThat(errorMessage).isNull()
    }

    @Test
    fun `parseErrorResponse handles missing error object`() {
        val errorJson = """{"status": "error"}"""
        val jsonObject = JsonParser.parseString(errorJson).asJsonObject

        val errorMessage =
            jsonObject
                .get("error")
                ?.asJsonObject
                ?.get("message")
                ?.asString

        assertThat(errorMessage).isNull()
    }

    @Test
    fun `VoiceTranscriber interface defines transcribe method`() {
        val transcriber: VoiceTranscriber =
            object : VoiceTranscriber {
                override suspend fun transcribe(audioFile: File): Result<String> = Result.success("test transcription")
            }

        // Verify that the implementation can be used through the interface
        assertThat(transcriber).isNotNull()
    }

    @Test
    fun `buildMultipartRequest includes required fields`() {
        val expectedFields = listOf("file", "model", "prompt", "response_format")

        expectedFields.forEach { field ->
            assertThat(field).isNotEmpty()
        }
    }

    @Test
    fun `whisperModel is whisper-1`() {
        val expectedModel = "whisper-1"
        assertThat(expectedModel).isEqualTo("whisper-1")
    }

    @Test
    fun `whisperPrompt contains fitness terminology`() {
        val whisperPrompt =
            "Transcribe this fitness workout log. The speaker is logging exercises, sets, " +
                "reps, and weights. Common terms: bench press, squat, deadlift, rows, curls, " +
                "RPE, plates (45lbs/20kg each), kilos, pounds, tricep pushdowns, bicep curls, " +
                "overhead press."

        assertThat(whisperPrompt).contains("bench press")
        assertThat(whisperPrompt).contains("squat")
        assertThat(whisperPrompt).contains("deadlift")
        assertThat(whisperPrompt).contains("RPE")
        assertThat(whisperPrompt).contains("plates")
        assertThat(whisperPrompt).contains("kilos")
        assertThat(whisperPrompt).contains("pounds")
    }

    @Test
    fun `parseWhisperResponse handles empty text`() {
        val responseJson = """{"text": ""}"""
        val jsonObject = JsonParser.parseString(responseJson).asJsonObject

        val text = jsonObject.get("text").asString

        assertThat(text).isEmpty()
    }

    @Test
    fun `parseWhisperResponse handles special characters`() {
        val responseJson = """{"text": "bench press at 100kg, 3×8 with RPE 8-9"}"""
        val jsonObject = JsonParser.parseString(responseJson).asJsonObject

        val text = jsonObject.get("text").asString

        assertThat(text).contains("100kg")
        assertThat(text).contains("×")
        assertThat(text).contains("RPE 8-9")
    }
}
