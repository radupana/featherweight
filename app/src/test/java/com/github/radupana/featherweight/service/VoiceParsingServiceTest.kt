package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.model.WeightUnit
import com.google.common.truth.Truth.assertThat
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.Test

class VoiceParsingServiceTest {
    @Test
    fun `parseAIResponse extracts single exercise correctly`() {
        val responseJson =
            """
            {
                "exercises": [
                    {
                        "spokenName": "bench press",
                        "interpretedName": "Barbell Bench Press",
                        "sets": [
                            {"setNumber": 1, "reps": 8, "weight": 100.0, "unit": "kg", "rpe": null, "isToFailure": false, "notes": null},
                            {"setNumber": 2, "reps": 8, "weight": 100.0, "unit": "kg", "rpe": null, "isToFailure": false, "notes": null},
                            {"setNumber": 3, "reps": 8, "weight": 100.0, "unit": "kg", "rpe": null, "isToFailure": false, "notes": null}
                        ],
                        "confidence": 0.95,
                        "notes": null
                    }
                ],
                "overallConfidence": 0.95,
                "warnings": []
            }
            """.trimIndent()

        val json = JsonParser.parseString(responseJson).asJsonObject
        val exercisesArray = json.getAsJsonArray("exercises")

        assertThat(exercisesArray.size()).isEqualTo(1)

        val exercise = exercisesArray[0].asJsonObject
        assertThat(exercise.get("spokenName").asString).isEqualTo("bench press")
        assertThat(exercise.get("interpretedName").asString).isEqualTo("Barbell Bench Press")

        val sets = exercise.getAsJsonArray("sets")
        assertThat(sets.size()).isEqualTo(3)

        val firstSet = sets[0].asJsonObject
        assertThat(firstSet.get("reps").asInt).isEqualTo(8)
        assertThat(firstSet.get("weight").asDouble).isEqualTo(100.0)
        assertThat(firstSet.get("unit").asString).isEqualTo("kg")
    }

    @Test
    fun `parseAIResponse extracts multiple exercises correctly`() {
        val responseJson =
            """
            {
                "exercises": [
                    {
                        "spokenName": "bench",
                        "interpretedName": "Barbell Bench Press",
                        "sets": [
                            {"setNumber": 1, "reps": 8, "weight": 100.0, "unit": "kg", "rpe": null, "isToFailure": false, "notes": null}
                        ],
                        "confidence": 0.95,
                        "notes": null
                    },
                    {
                        "spokenName": "curls",
                        "interpretedName": "Dumbbell Bicep Curl",
                        "sets": [
                            {"setNumber": 1, "reps": 12, "weight": 25.0, "unit": "kg", "rpe": null, "isToFailure": false, "notes": null}
                        ],
                        "confidence": 0.90,
                        "notes": null
                    },
                    {
                        "spokenName": "tricep pushdowns",
                        "interpretedName": "Cable Tricep Pushdown",
                        "sets": [
                            {"setNumber": 1, "reps": 15, "weight": 30.0, "unit": "kg", "rpe": null, "isToFailure": false, "notes": null}
                        ],
                        "confidence": 0.92,
                        "notes": null
                    }
                ],
                "overallConfidence": 0.92,
                "warnings": []
            }
            """.trimIndent()

        val json = JsonParser.parseString(responseJson).asJsonObject
        val exercisesArray = json.getAsJsonArray("exercises")

        assertThat(exercisesArray.size()).isEqualTo(3)
        assertThat(exercisesArray[0].asJsonObject.get("interpretedName").asString).isEqualTo("Barbell Bench Press")
        assertThat(exercisesArray[1].asJsonObject.get("interpretedName").asString).isEqualTo("Dumbbell Bicep Curl")
        assertThat(exercisesArray[2].asJsonObject.get("interpretedName").asString).isEqualTo("Cable Tricep Pushdown")
    }

    @Test
    fun `parseAIResponse handles RPE values`() {
        val responseJson =
            """
            {
                "exercises": [
                    {
                        "spokenName": "deadlifts RPE 8",
                        "interpretedName": "Barbell Deadlift",
                        "sets": [
                            {"setNumber": 1, "reps": 5, "weight": 180.0, "unit": "kg", "rpe": 8.0, "isToFailure": false, "notes": null}
                        ],
                        "confidence": 0.95,
                        "notes": null
                    }
                ],
                "overallConfidence": 0.95,
                "warnings": []
            }
            """.trimIndent()

        val json = JsonParser.parseString(responseJson).asJsonObject
        val set =
            json
                .getAsJsonArray("exercises")[0]
                .asJsonObject
                .getAsJsonArray("sets")[0]
                .asJsonObject

        assertThat(set.get("rpe").asDouble).isEqualTo(8.0)
        assertThat(set.get("isToFailure").asBoolean).isFalse()
    }

    @Test
    fun `parseAIResponse handles to-failure sets`() {
        val responseJson =
            """
            {
                "exercises": [
                    {
                        "spokenName": "curls to failure",
                        "interpretedName": "Dumbbell Bicep Curl",
                        "sets": [
                            {"setNumber": 1, "reps": 12, "weight": 20.0, "unit": "kg", "rpe": 10.0, "isToFailure": true, "notes": null}
                        ],
                        "confidence": 0.90,
                        "notes": null
                    }
                ],
                "overallConfidence": 0.90,
                "warnings": []
            }
            """.trimIndent()

        val json = JsonParser.parseString(responseJson).asJsonObject
        val set =
            json
                .getAsJsonArray("exercises")[0]
                .asJsonObject
                .getAsJsonArray("sets")[0]
                .asJsonObject

        assertThat(set.get("isToFailure").asBoolean).isTrue()
        assertThat(set.get("rpe").asDouble).isEqualTo(10.0)
    }

    @Test
    fun `parseAIResponse handles lbs unit`() {
        val responseJson =
            """
            {
                "exercises": [
                    {
                        "spokenName": "bench 225",
                        "interpretedName": "Barbell Bench Press",
                        "sets": [
                            {"setNumber": 1, "reps": 5, "weight": 225.0, "unit": "lbs", "rpe": null, "isToFailure": false, "notes": null}
                        ],
                        "confidence": 0.95,
                        "notes": null
                    }
                ],
                "overallConfidence": 0.95,
                "warnings": []
            }
            """.trimIndent()

        val json = JsonParser.parseString(responseJson).asJsonObject
        val set =
            json
                .getAsJsonArray("exercises")[0]
                .asJsonObject
                .getAsJsonArray("sets")[0]
                .asJsonObject

        assertThat(set.get("weight").asDouble).isEqualTo(225.0)
        assertThat(set.get("unit").asString).isEqualTo("lbs")
    }

    @Test
    fun `parseAIResponse handles warnings`() {
        val responseJson =
            """
            {
                "exercises": [
                    {
                        "spokenName": "curls",
                        "interpretedName": "Bicep Curl",
                        "sets": [
                            {"setNumber": 1, "reps": 10, "weight": 20.0, "unit": "kg", "rpe": null, "isToFailure": false, "notes": null}
                        ],
                        "confidence": 0.70,
                        "notes": null
                    }
                ],
                "overallConfidence": 0.70,
                "warnings": ["Could not determine equipment type for 'curls'", "Weight unit assumed to be kg"]
            }
            """.trimIndent()

        val json = JsonParser.parseString(responseJson).asJsonObject
        val warnings = json.getAsJsonArray("warnings")

        assertThat(warnings.size()).isEqualTo(2)
        assertThat(warnings[0].asString).contains("equipment type")
        assertThat(warnings[1].asString).contains("kg")
    }

    @Test
    fun `parseAIResponse handles variable weight sets`() {
        val responseJson =
            """
            {
                "exercises": [
                    {
                        "spokenName": "squats pyramid",
                        "interpretedName": "Barbell Back Squat",
                        "sets": [
                            {"setNumber": 1, "reps": 10, "weight": 60.0, "unit": "kg", "rpe": null, "isToFailure": false, "notes": "warm-up"},
                            {"setNumber": 2, "reps": 8, "weight": 80.0, "unit": "kg", "rpe": null, "isToFailure": false, "notes": null},
                            {"setNumber": 3, "reps": 5, "weight": 100.0, "unit": "kg", "rpe": null, "isToFailure": false, "notes": null},
                            {"setNumber": 4, "reps": 12, "weight": 60.0, "unit": "kg", "rpe": null, "isToFailure": false, "notes": "drop set"}
                        ],
                        "confidence": 0.85,
                        "notes": "Pyramid set structure"
                    }
                ],
                "overallConfidence": 0.85,
                "warnings": []
            }
            """.trimIndent()

        val json = JsonParser.parseString(responseJson).asJsonObject
        val sets = json.getAsJsonArray("exercises")[0].asJsonObject.getAsJsonArray("sets")

        assertThat(sets.size()).isEqualTo(4)
        assertThat(sets[0].asJsonObject.get("weight").asDouble).isEqualTo(60.0)
        assertThat(sets[1].asJsonObject.get("weight").asDouble).isEqualTo(80.0)
        assertThat(sets[2].asJsonObject.get("weight").asDouble).isEqualTo(100.0)
        assertThat(sets[3].asJsonObject.get("weight").asDouble).isEqualTo(60.0)
        assertThat(sets[3].asJsonObject.get("notes").asString).isEqualTo("drop set")
    }

    @Test
    fun `parseAIResponse handles low confidence`() {
        val responseJson =
            """
            {
                "exercises": [
                    {
                        "spokenName": "something heavy",
                        "interpretedName": "Unknown Exercise",
                        "sets": [
                            {"setNumber": 1, "reps": 10, "weight": 50.0, "unit": "kg", "rpe": null, "isToFailure": false, "notes": null}
                        ],
                        "confidence": 0.30,
                        "notes": null
                    }
                ],
                "overallConfidence": 0.30,
                "warnings": ["Could not clearly identify exercise"]
            }
            """.trimIndent()

        val json = JsonParser.parseString(responseJson).asJsonObject

        assertThat(json.get("overallConfidence").asDouble).isEqualTo(0.30)
        assertThat(
            json
                .getAsJsonArray("exercises")[0]
                .asJsonObject
                .get("confidence")
                .asDouble,
        ).isEqualTo(0.30)
    }

    @Test
    fun `VoiceParser interface defines parseTranscription method`() {
        val parser =
            object : VoiceParser {
                override suspend fun parseTranscription(
                    transcription: String,
                    preferredWeightUnit: WeightUnit,
                ): Result<com.github.radupana.featherweight.data.voice.ParsedVoiceWorkoutInput> = Result.failure(NotImplementedError())
            }

        assertThat(parser).isNotNull()
    }

    @Test
    fun `buildRequestBody contains correct model`() {
        val expectedModel = "gpt-5-mini"

        val requestBody =
            JsonObject().apply {
                addProperty("model", expectedModel)
                addProperty("max_completion_tokens", 4000)
                add(
                    "response_format",
                    JsonObject().apply {
                        addProperty("type", "json_object")
                    },
                )
            }

        assertThat(requestBody.get("model").asString).isEqualTo("gpt-5-mini")
        assertThat(requestBody.get("max_completion_tokens").asInt).isEqualTo(4000)
    }

    @Test
    fun `systemPrompt contains gym slang interpretation rules`() {
        val systemPromptExcerpt =
            """
            - "plates" = 20kg/45lbs each side
            - "two plates" = 4 plates total = 100kg/225lbs (bar + 2 per side)
            - "three plates" = 6 plates total = 140kg/315lbs
            - "3x8" or "3 by 8" = 3 sets of 8 reps
            """.trimIndent()

        assertThat(systemPromptExcerpt).contains("plates")
        assertThat(systemPromptExcerpt).contains("100kg/225lbs")
        assertThat(systemPromptExcerpt).contains("3x8")
    }

    @Test
    fun `parseWeightUnit handles various unit formats`() {
        val unitMappings =
            mapOf(
                "kg" to "kg",
                "lbs" to "lbs",
                "lb" to "lbs",
                "pounds" to "lbs",
                "KG" to "kg",
                "LBS" to "lbs",
            )

        unitMappings.forEach { (input, expected) ->
            val unit =
                when (input.lowercase()) {
                    "lbs", "lb", "pounds" -> WeightUnit.LBS
                    else -> WeightUnit.KG
                }
            val expectedUnit = if (expected == "lbs") WeightUnit.LBS else WeightUnit.KG
            assertThat(unit).isEqualTo(expectedUnit)
        }
    }

    @Test
    fun `parseAIResponse handles empty exercises array`() {
        val responseJson =
            """
            {
                "exercises": [],
                "overallConfidence": 0.0,
                "warnings": ["No exercises detected in transcription"]
            }
            """.trimIndent()

        val json = JsonParser.parseString(responseJson).asJsonObject
        val exercises = json.getAsJsonArray("exercises")

        assertThat(exercises.size()).isEqualTo(0)
        assertThat(json.getAsJsonArray("warnings")[0].asString).contains("No exercises")
    }
}
