package com.github.radupana.featherweight.data.programme

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test
import java.time.LocalDateTime
import kotlin.test.assertFailsWith

class ProgrammeTest {
    @Test
    fun programme_withValidData_shouldCreateSuccessfully() {
        val programme =
            Programme(
                id = 1L,
                name = "StrongLifts 5x5",
                description = "Simple strength programme",
                durationWeeks = 12,
                programmeType = ProgrammeType.STRENGTH,
                difficulty = ProgrammeDifficulty.BEGINNER,
                isCustom = false,
                isActive = true,
                status = ProgrammeStatus.NOT_STARTED,
            )

        assertThat(programme.name).isEqualTo("StrongLifts 5x5")
        assertThat(programme.durationWeeks).isEqualTo(12)
        assertThat(programme.programmeType).isEqualTo(ProgrammeType.STRENGTH)
        assertThat(programme.difficulty).isEqualTo(ProgrammeDifficulty.BEGINNER)
        assertThat(programme.isActive).isTrue()
        assertThat(programme.status).isEqualTo(ProgrammeStatus.NOT_STARTED)
    }

    @Test
    fun programme_withInProgressStatus_canBeActive() {
        val now = LocalDateTime.now()
        val programme =
            Programme(
                name = "5/3/1",
                description = "Wendler's 5/3/1",
                durationWeeks = 4,
                programmeType = ProgrammeType.POWERLIFTING,
                difficulty = ProgrammeDifficulty.INTERMEDIATE,
                isActive = true,
                status = ProgrammeStatus.IN_PROGRESS,
                startedAt = now,
            )

        assertThat(programme.status).isEqualTo(ProgrammeStatus.IN_PROGRESS)
        assertThat(programme.isActive).isTrue()
        assertThat(programme.startedAt).isEqualTo(now)
    }

    @Test
    fun programme_withCompletedStatus_mustHaveCompletedAt() {
        val now = LocalDateTime.now()
        val programme =
            Programme(
                name = "Completed Programme",
                description = null,
                durationWeeks = 8,
                programmeType = ProgrammeType.BODYBUILDING,
                difficulty = ProgrammeDifficulty.INTERMEDIATE,
                isActive = false,
                status = ProgrammeStatus.COMPLETED,
                startedAt = now.minusWeeks(8),
                completedAt = now,
            )

        assertThat(programme.status).isEqualTo(ProgrammeStatus.COMPLETED)
        assertThat(programme.completedAt).isNotNull()
        assertThat(programme.isActive).isFalse()
    }

    @Test
    fun programme_completedStatusWithoutCompletedAt_shouldThrowException() {
        assertFailsWith<IllegalArgumentException> {
            Programme(
                name = "Invalid Programme",
                description = null,
                durationWeeks = 8,
                programmeType = ProgrammeType.STRENGTH,
                difficulty = ProgrammeDifficulty.BEGINNER,
                isActive = false,
                status = ProgrammeStatus.COMPLETED,
                startedAt = LocalDateTime.now(),
                completedAt = null, // Invalid: COMPLETED must have completedAt
            )
        }
    }

    @Test
    fun programme_notCompletedStatusWithCompletedAt_shouldThrowException() {
        assertFailsWith<IllegalArgumentException> {
            Programme(
                name = "Invalid Programme",
                description = null,
                durationWeeks = 8,
                programmeType = ProgrammeType.STRENGTH,
                difficulty = ProgrammeDifficulty.BEGINNER,
                isActive = true,
                status = ProgrammeStatus.IN_PROGRESS,
                startedAt = LocalDateTime.now(),
                completedAt = LocalDateTime.now(), // Invalid: IN_PROGRESS cannot have completedAt
            )
        }
    }

    @Test
    fun programme_completedStatusCannotBeActive_shouldThrowException() {
        assertFailsWith<IllegalArgumentException> {
            Programme(
                name = "Invalid Programme",
                description = null,
                durationWeeks = 8,
                programmeType = ProgrammeType.STRENGTH,
                difficulty = ProgrammeDifficulty.BEGINNER,
                isActive = true, // Invalid: COMPLETED cannot be active
                status = ProgrammeStatus.COMPLETED,
                startedAt = LocalDateTime.now().minusWeeks(8),
                completedAt = LocalDateTime.now(),
            )
        }
    }

    @Test
    fun programme_cancelledStatusCannotBeActive_shouldThrowException() {
        assertFailsWith<IllegalArgumentException> {
            Programme(
                name = "Cancelled Programme",
                description = null,
                durationWeeks = 12,
                programmeType = ProgrammeType.HYBRID,
                difficulty = ProgrammeDifficulty.ADVANCED,
                isActive = true, // Invalid: CANCELLED cannot be active
                status = ProgrammeStatus.CANCELLED,
                startedAt = LocalDateTime.now().minusWeeks(2),
            )
        }
    }

    @Test
    fun programme_notStartedStatusCannotHaveStartedAt_shouldThrowException() {
        assertFailsWith<IllegalArgumentException> {
            Programme(
                name = "Not Started Programme",
                description = null,
                durationWeeks = 6,
                programmeType = ProgrammeType.GENERAL_FITNESS,
                difficulty = ProgrammeDifficulty.NOVICE,
                isActive = false,
                status = ProgrammeStatus.NOT_STARTED,
                startedAt = LocalDateTime.now(), // Invalid: NOT_STARTED cannot have startedAt
            )
        }
    }

    @Test
    fun programme_inProgressStatusMustHaveStartedAt_shouldThrowException() {
        assertFailsWith<IllegalArgumentException> {
            Programme(
                name = "In Progress Programme",
                description = null,
                durationWeeks = 10,
                programmeType = ProgrammeType.OLYMPIC_LIFTING,
                difficulty = ProgrammeDifficulty.EXPERT,
                isActive = true,
                status = ProgrammeStatus.IN_PROGRESS,
                startedAt = null, // Invalid: IN_PROGRESS must have startedAt
            )
        }
    }

    @Test
    fun programme_withMaxValues_shouldStoreCorrectly() {
        val programme =
            Programme(
                name = "Powerlifting Programme",
                description = "Focus on the big three",
                durationWeeks = 16,
                programmeType = ProgrammeType.POWERLIFTING,
                difficulty = ProgrammeDifficulty.ADVANCED,
                squatMax = 180f,
                benchMax = 120f,
                deadliftMax = 220f,
                ohpMax = 80f,
            )

        assertThat(programme.squatMax).isEqualTo(180f)
        assertThat(programme.benchMax).isEqualTo(120f)
        assertThat(programme.deadliftMax).isEqualTo(220f)
        assertThat(programme.ohpMax).isEqualTo(80f)
    }

    @Test
    fun programme_withWeightCalculationRules_shouldSerializeCorrectly() {
        val rules =
            WeightCalculationRules(
                baseOn = WeightBasis.TRAINING_MAX,
                trainingMaxPercentage = 0.9f,
                roundingIncrement = 2.5f,
                minimumBarWeight = 20f,
                unit = WeightUnit.KG,
            )

        val programme =
            Programme(
                name = "Programme with Rules",
                description = null,
                durationWeeks = 8,
                programmeType = ProgrammeType.STRENGTH,
                difficulty = ProgrammeDifficulty.INTERMEDIATE,
                weightCalculationRules = Programme.encodeWeightCalculationRules(rules),
            )

        val decodedRules = programme.getWeightCalculationRulesObject()
        assertThat(decodedRules).isNotNull()
        assertThat(decodedRules?.baseOn).isEqualTo(WeightBasis.TRAINING_MAX)
        assertThat(decodedRules?.trainingMaxPercentage).isEqualTo(0.9f)
    }

    @Test
    fun programme_withProgressionRules_shouldSerializeCorrectly() {
        val rules =
            ProgressionRules(
                type = ProgressionType.LINEAR,
                incrementRules = mapOf("squat" to 5f, "bench" to 2.5f),
                autoProgressionEnabled = true,
            )

        val programme =
            Programme(
                name = "Programme with Progression",
                description = null,
                durationWeeks = 12,
                programmeType = ProgrammeType.STRENGTH,
                difficulty = ProgrammeDifficulty.BEGINNER,
                progressionRules = Programme.encodeProgressionRules(rules),
            )

        val decodedRules = programme.getProgressionRulesObject()
        assertThat(decodedRules).isNotNull()
        assertThat(decodedRules?.type).isEqualTo(ProgressionType.LINEAR)
        assertThat(decodedRules?.incrementRules?.get("squat")).isEqualTo(5f)
        assertThat(decodedRules?.autoProgressionEnabled).isTrue()
    }

    // Note: Tests for invalid JSON parsing are omitted because they require Android's Log class
    // which is not available in unit tests. The implementation correctly returns null for invalid JSON.

    @Test
    fun programme_withNullRules_shouldReturnNull() {
        val programme =
            Programme(
                name = "Programme without Rules",
                description = null,
                durationWeeks = 8,
                programmeType = ProgrammeType.GENERAL_FITNESS,
                difficulty = ProgrammeDifficulty.BEGINNER,
                weightCalculationRules = null,
                progressionRules = null,
            )

        assertThat(programme.getWeightCalculationRulesObject()).isNull()
        assertThat(programme.getProgressionRulesObject()).isNull()
    }

    @Test
    fun programme_withCustomFlag_shouldIdentifyAsCustom() {
        val programme =
            Programme(
                name = "My Custom Programme",
                description = "Created by user",
                durationWeeks = 10,
                programmeType = ProgrammeType.HYBRID,
                difficulty = ProgrammeDifficulty.INTERMEDIATE,
                isCustom = true,
            )

        assertThat(programme.isCustom).isTrue()
    }

    @Test
    fun programme_withTemplateName_shouldStoreTemplateReference() {
        val programme =
            Programme(
                name = "My Modified 5/3/1",
                description = "Based on Wendler's 5/3/1",
                durationWeeks = 4,
                programmeType = ProgrammeType.POWERLIFTING,
                difficulty = ProgrammeDifficulty.INTERMEDIATE,
                isCustom = true,
                templateName = "5/3/1",
            )

        assertThat(programme.templateName).isEqualTo("5/3/1")
        assertThat(programme.isCustom).isTrue()
    }

    @Test
    fun programme_withCompletionNotes_shouldStoreNotesWithTimestamp() {
        val now = LocalDateTime.now()
        val programme =
            Programme(
                name = "Completed Programme",
                description = null,
                durationWeeks = 8,
                programmeType = ProgrammeType.STRENGTH,
                difficulty = ProgrammeDifficulty.INTERMEDIATE,
                status = ProgrammeStatus.COMPLETED,
                isActive = false,
                startedAt = now.minusWeeks(8),
                completedAt = now,
                completionNotes = "Great progress! Increased squat by 20kg",
                notesCreatedAt = now,
            )

        assertThat(programme.completionNotes).contains("Great progress")
        assertThat(programme.notesCreatedAt).isEqualTo(now)
    }

    @Test
    fun programme_allProgrammeTypes_shouldBeDefined() {
        val types = ProgrammeType.values()

        assertThat(types).asList().contains(ProgrammeType.STRENGTH)
        assertThat(types).asList().contains(ProgrammeType.POWERLIFTING)
        assertThat(types).asList().contains(ProgrammeType.BODYBUILDING)
        assertThat(types).asList().contains(ProgrammeType.GENERAL_FITNESS)
        assertThat(types).asList().contains(ProgrammeType.OLYMPIC_LIFTING)
        assertThat(types).asList().contains(ProgrammeType.HYBRID)
        assertThat(types).hasLength(6)
    }

    @Test
    fun programme_allDifficultyLevels_shouldBeDefined() {
        val difficulties = ProgrammeDifficulty.values()

        assertThat(difficulties).asList().contains(ProgrammeDifficulty.BEGINNER)
        assertThat(difficulties).asList().contains(ProgrammeDifficulty.NOVICE)
        assertThat(difficulties).asList().contains(ProgrammeDifficulty.INTERMEDIATE)
        assertThat(difficulties).asList().contains(ProgrammeDifficulty.ADVANCED)
        assertThat(difficulties).asList().contains(ProgrammeDifficulty.EXPERT)
        assertThat(difficulties).hasLength(5)
    }

    @Test
    fun programme_allStatuses_shouldBeDefined() {
        val statuses = ProgrammeStatus.values()

        assertThat(statuses).asList().contains(ProgrammeStatus.NOT_STARTED)
        assertThat(statuses).asList().contains(ProgrammeStatus.IN_PROGRESS)
        assertThat(statuses).asList().contains(ProgrammeStatus.COMPLETED)
        assertThat(statuses).asList().contains(ProgrammeStatus.CANCELLED)
        assertThat(statuses).hasLength(4)
    }

    @Test
    fun programme_withDefaultValues_shouldUseDefaults() {
        val programme =
            Programme(
                name = "Basic Programme",
                description = null,
                durationWeeks = 4,
                programmeType = ProgrammeType.GENERAL_FITNESS,
                difficulty = ProgrammeDifficulty.BEGINNER,
            )

        assertThat(programme.id).isEqualTo(0L)
        assertThat(programme.isCustom).isFalse()
        assertThat(programme.isActive).isFalse()
        assertThat(programme.status).isEqualTo(ProgrammeStatus.NOT_STARTED)
        assertThat(programme.startedAt).isNull()
        assertThat(programme.completedAt).isNull()
    }

    @Test
    fun programme_withLongDuration_shouldHandleExtendedProgrammes() {
        val programme =
            Programme(
                name = "Year-long Programme",
                description = "52 week programme",
                durationWeeks = 52,
                programmeType = ProgrammeType.HYBRID,
                difficulty = ProgrammeDifficulty.ADVANCED,
            )

        assertThat(programme.durationWeeks).isEqualTo(52)
    }

    @Test
    fun programme_dataClassCopy_shouldWorkCorrectly() {
        val original =
            Programme(
                name = "Original",
                description = "Test programme",
                durationWeeks = 8,
                programmeType = ProgrammeType.STRENGTH,
                difficulty = ProgrammeDifficulty.INTERMEDIATE,
            )

        val modified =
            original.copy(
                name = "Modified",
                durationWeeks = 12,
                isActive = true,
            )

        assertThat(modified.name).isEqualTo("Modified")
        assertThat(modified.durationWeeks).isEqualTo(12)
        assertThat(modified.isActive).isTrue()
        assertThat(modified.description).isEqualTo(original.description)
        assertThat(modified.programmeType).isEqualTo(original.programmeType)
    }

    @Test
    fun programme_cancelledStatus_withStartedAt_shouldBeValid() {
        val programme =
            Programme(
                name = "Cancelled Programme",
                description = null,
                durationWeeks = 12,
                programmeType = ProgrammeType.STRENGTH,
                difficulty = ProgrammeDifficulty.INTERMEDIATE,
                status = ProgrammeStatus.CANCELLED,
                isActive = false,
                startedAt = LocalDateTime.now().minusWeeks(2),
            )

        assertThat(programme.status).isEqualTo(ProgrammeStatus.CANCELLED)
        assertThat(programme.startedAt).isNotNull()
        assertThat(programme.isActive).isFalse()
    }

    @Test
    fun programme_withPartialMaxes_shouldHandleMissingData() {
        val programme =
            Programme(
                name = "Upper Body Focus",
                description = null,
                durationWeeks = 8,
                programmeType = ProgrammeType.BODYBUILDING,
                difficulty = ProgrammeDifficulty.INTERMEDIATE,
                squatMax = null,
                benchMax = 100f,
                deadliftMax = null,
                ohpMax = 60f,
            )

        assertThat(programme.squatMax).isNull()
        assertThat(programme.benchMax).isEqualTo(100f)
        assertThat(programme.deadliftMax).isNull()
        assertThat(programme.ohpMax).isEqualTo(60f)
    }

    @Test
    fun programme_encodeWeightCalculationRules_shouldProduceValidJson() {
        val rules =
            WeightCalculationRules(
                baseOn = WeightBasis.TRAINING_MAX, // Use non-default value to ensure it's serialized
                trainingMaxPercentage = 0.85f,
                roundingIncrement = 5f,
                minimumBarWeight = 45f,
                unit = WeightUnit.LB,
            )

        val json = Programme.encodeWeightCalculationRules(rules)

        assertThat(json).isNotEmpty()
        assertThat(json).contains("TRAINING_MAX") // Non-default value should be included
        assertThat(json).contains("0.85")
        assertThat(json).contains("LB")

        val decoded = Json.decodeFromString<WeightCalculationRules>(json)
        assertThat(decoded.baseOn).isEqualTo(WeightBasis.TRAINING_MAX)
        assertThat(decoded.trainingMaxPercentage).isEqualTo(0.85f)
        assertThat(decoded.unit).isEqualTo(WeightUnit.LB)
    }

    @Test
    fun programme_encodeProgressionRules_shouldProduceValidJson() {
        val rules =
            ProgressionRules(
                type = ProgressionType.WAVE,
                cycleLength = 3,
                autoProgressionEnabled = false,
            )

        val json = Programme.encodeProgressionRules(rules)

        assertThat(json).isNotEmpty()
        assertThat(json).contains("WAVE")
        assertThat(json).contains("3")

        val decoded = Json.decodeFromString<ProgressionRules>(json)
        assertThat(decoded.type).isEqualTo(ProgressionType.WAVE)
        assertThat(decoded.cycleLength).isEqualTo(3)
    }
}
