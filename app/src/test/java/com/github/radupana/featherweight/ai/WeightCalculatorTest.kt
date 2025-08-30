package com.github.radupana.featherweight.ai

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class WeightCalculatorTest {
    private lateinit var calculator: WeightCalculator

    @Before
    fun setUp() {
        calculator = WeightCalculator()
    }

    // calculateWorkingWeight tests - Priority hierarchy

    @Test
    fun calculateWorkingWeight_withExtractedWeight_returnsPriorityOne() {
        val result =
            calculator.calculateWorkingWeight(
                exerciseName = "Barbell Squat",
                repRange = 3..5,
                programmeType = ProgrammeType.STRENGTH,
                user1RM = 100f,
                extractedWeight = 80f,
                aiSuggestedWeight = 70f,
            )

        assertThat(result.weight).isEqualTo(80f)
        assertThat(result.percentageOf1RM).isEqualTo(80f)
        assertThat(result.source).isEqualTo("user_input")
    }

    @Test
    fun calculateWorkingWeight_withExtractedWeightNoRM_returnsCorrectly() {
        val result =
            calculator.calculateWorkingWeight(
                exerciseName = "Barbell Squat",
                repRange = 3..5,
                programmeType = ProgrammeType.STRENGTH,
                user1RM = null,
                extractedWeight = 80f,
                aiSuggestedWeight = 70f,
            )

        assertThat(result.weight).isEqualTo(80f)
        assertThat(result.percentageOf1RM).isNull()
        assertThat(result.source).isEqualTo("user_input")
    }

    @Test
    fun calculateWorkingWeight_withUser1RM_returnsPriorityTwo() {
        val result =
            calculator.calculateWorkingWeight(
                exerciseName = "Barbell Squat",
                repRange = 3..5,
                programmeType = ProgrammeType.STRENGTH,
                user1RM = 100f,
                extractedWeight = null,
                aiSuggestedWeight = 70f,
            )

        // For STRENGTH, 3-5 reps average = 4, should be 80%
        assertThat(result.weight).isEqualTo(80f)
        assertThat(result.percentageOf1RM).isEqualTo(80f)
        assertThat(result.source).isEqualTo("user_1rm")
    }

    @Test
    fun calculateWorkingWeight_withAISuggested_returnsPriorityThree() {
        val result =
            calculator.calculateWorkingWeight(
                exerciseName = "Barbell Squat",
                repRange = 3..5,
                programmeType = ProgrammeType.STRENGTH,
                user1RM = null,
                extractedWeight = null,
                aiSuggestedWeight = 70f,
            )

        assertThat(result.weight).isEqualTo(70f)
        assertThat(result.percentageOf1RM).isNull()
        assertThat(result.source).isEqualTo("ai_suggested")
    }

    @Test
    fun calculateWorkingWeight_withAISuggestedZero_skipsToDefault() {
        val result =
            calculator.calculateWorkingWeight(
                exerciseName = "Barbell Squat",
                repRange = 3..5,
                programmeType = ProgrammeType.STRENGTH,
                user1RM = null,
                extractedWeight = null,
                aiSuggestedWeight = 0f,
            )

        // Should use smart default for squat at 3-5 reps (heavy compound)
        assertThat(result.weight).isEqualTo(70f)
        assertThat(result.percentageOf1RM).isNull()
        assertThat(result.source).isEqualTo("smart_default")
    }

    @Test
    fun calculateWorkingWeight_withNoInputs_returnsSmartDefault() {
        val result =
            calculator.calculateWorkingWeight(
                exerciseName = "Barbell Squat",
                repRange = 3..5,
                programmeType = ProgrammeType.STRENGTH,
                user1RM = null,
                extractedWeight = null,
                aiSuggestedWeight = null,
            )

        // Heavy compound, 3-5 reps
        assertThat(result.weight).isEqualTo(70f)
        assertThat(result.percentageOf1RM).isNull()
        assertThat(result.source).isEqualTo("smart_default")
    }

    // Percentage calculation tests for different programme types

    @Test
    fun calculateWorkingWeight_strengthProgramme_lowReps() {
        val result =
            calculator.calculateWorkingWeight(
                exerciseName = "Deadlift",
                repRange = 1..3,
                programmeType = ProgrammeType.STRENGTH,
                user1RM = 200f,
                extractedWeight = null,
                aiSuggestedWeight = null,
            )

        // Average 2 reps, should be 85%
        assertThat(result.weight).isEqualTo(170f)
        assertThat(result.percentageOf1RM).isEqualTo(85f)
    }

    @Test
    fun calculateWorkingWeight_strengthProgramme_mediumReps() {
        val result =
            calculator.calculateWorkingWeight(
                exerciseName = "Deadlift",
                repRange = 4..5,
                programmeType = ProgrammeType.STRENGTH,
                user1RM = 200f,
                extractedWeight = null,
                aiSuggestedWeight = null,
            )

        // Average 4.5 reps, should be 80%
        assertThat(result.weight).isEqualTo(160f)
        assertThat(result.percentageOf1RM).isEqualTo(80f)
    }

    @Test
    fun calculateWorkingWeight_strengthProgramme_higherReps() {
        val result =
            calculator.calculateWorkingWeight(
                exerciseName = "Deadlift",
                repRange = 6..8,
                programmeType = ProgrammeType.STRENGTH,
                user1RM = 200f,
                extractedWeight = null,
                aiSuggestedWeight = null,
            )

        // Average 7 reps, should be 75%
        assertThat(result.weight).isEqualTo(150f)
        assertThat(result.percentageOf1RM).isEqualTo(75f)
    }

    @Test
    fun calculateWorkingWeight_strengthProgramme_veryHighReps() {
        val result =
            calculator.calculateWorkingWeight(
                exerciseName = "Deadlift",
                repRange = 10..12,
                programmeType = ProgrammeType.STRENGTH,
                user1RM = 200f,
                extractedWeight = null,
                aiSuggestedWeight = null,
            )

        // Average 11 reps, should be 70%
        assertThat(result.weight).isEqualTo(140f)
        assertThat(result.percentageOf1RM).isEqualTo(70f)
    }

    @Test
    fun calculateWorkingWeight_hypertrophyProgramme_lowReps() {
        val result =
            calculator.calculateWorkingWeight(
                exerciseName = "Bench Press",
                repRange = 4..6,
                programmeType = ProgrammeType.HYPERTROPHY,
                user1RM = 100f,
                extractedWeight = null,
                aiSuggestedWeight = null,
            )

        // Average 5 reps, should be 75%
        assertThat(result.weight).isEqualTo(75f)
        assertThat(result.percentageOf1RM).isEqualTo(75f)
    }

    @Test
    fun calculateWorkingWeight_hypertrophyProgramme_mediumReps() {
        val result =
            calculator.calculateWorkingWeight(
                exerciseName = "Bench Press",
                repRange = 8..10,
                programmeType = ProgrammeType.HYPERTROPHY,
                user1RM = 100f,
                extractedWeight = null,
                aiSuggestedWeight = null,
            )

        // Average 9 reps, should be 70%
        assertThat(result.weight).isEqualTo(70f)
        assertThat(result.percentageOf1RM).isEqualTo(70f)
    }

    @Test
    fun calculateWorkingWeight_hypertrophyProgramme_highReps() {
        val result =
            calculator.calculateWorkingWeight(
                exerciseName = "Bench Press",
                repRange = 12..15,
                programmeType = ProgrammeType.HYPERTROPHY,
                user1RM = 100f,
                extractedWeight = null,
                aiSuggestedWeight = null,
            )

        // Average 13.5 reps, should be 65%
        assertThat(result.weight).isEqualTo(65f)
        assertThat(result.percentageOf1RM).isEqualTo(65f)
    }

    @Test
    fun calculateWorkingWeight_hypertrophyProgramme_veryHighReps() {
        val result =
            calculator.calculateWorkingWeight(
                exerciseName = "Bench Press",
                repRange = 16..20,
                programmeType = ProgrammeType.HYPERTROPHY,
                user1RM = 100f,
                extractedWeight = null,
                aiSuggestedWeight = null,
            )

        // Average 18 reps, should be 60%
        assertThat(result.weight).isEqualTo(60f)
        assertThat(result.percentageOf1RM).isEqualTo(60f)
    }

    @Test
    fun calculateWorkingWeight_enduranceProgramme_lowReps() {
        val result =
            calculator.calculateWorkingWeight(
                exerciseName = "Squat",
                repRange = 10..12,
                programmeType = ProgrammeType.ENDURANCE,
                user1RM = 100f,
                extractedWeight = null,
                aiSuggestedWeight = null,
            )

        // Average 11 reps, should be 65%
        assertThat(result.weight).isEqualTo(65f)
        assertThat(result.percentageOf1RM).isEqualTo(65f)
    }

    @Test
    fun calculateWorkingWeight_enduranceProgramme_mediumReps() {
        val result =
            calculator.calculateWorkingWeight(
                exerciseName = "Squat",
                repRange = 15..20,
                programmeType = ProgrammeType.ENDURANCE,
                user1RM = 100f,
                extractedWeight = null,
                aiSuggestedWeight = null,
            )

        // Average 17.5 reps, should be 55%
        assertThat(result.weight).isEqualTo(55f)
        assertThat(result.percentageOf1RM).isEqualTo(55f)
    }

    @Test
    fun calculateWorkingWeight_enduranceProgramme_highReps() {
        val result =
            calculator.calculateWorkingWeight(
                exerciseName = "Squat",
                repRange = 25..30,
                programmeType = ProgrammeType.ENDURANCE,
                user1RM = 100f,
                extractedWeight = null,
                aiSuggestedWeight = null,
            )

        // Average 27.5 reps, should be 50%
        assertThat(result.weight).isEqualTo(50f)
        assertThat(result.percentageOf1RM).isEqualTo(50f)
    }

    @Test
    fun calculateWorkingWeight_generalProgramme_variousReps() {
        // Low reps
        var result =
            calculator.calculateWorkingWeight(
                exerciseName = "Row",
                repRange = 3..5,
                programmeType = ProgrammeType.GENERAL,
                user1RM = 100f,
                extractedWeight = null,
                aiSuggestedWeight = null,
            )
        assertThat(result.percentageOf1RM).isEqualTo(75f)

        // Medium reps
        result =
            calculator.calculateWorkingWeight(
                exerciseName = "Row",
                repRange = 8..10,
                programmeType = ProgrammeType.GENERAL,
                user1RM = 100f,
                extractedWeight = null,
                aiSuggestedWeight = null,
            )
        assertThat(result.percentageOf1RM).isEqualTo(70f)

        // High reps
        result =
            calculator.calculateWorkingWeight(
                exerciseName = "Row",
                repRange = 12..15,
                programmeType = ProgrammeType.GENERAL,
                user1RM = 100f,
                extractedWeight = null,
                aiSuggestedWeight = null,
            )
        assertThat(result.percentageOf1RM).isEqualTo(65f)

        // Very high reps
        result =
            calculator.calculateWorkingWeight(
                exerciseName = "Row",
                repRange = 20..25,
                programmeType = ProgrammeType.GENERAL,
                user1RM = 100f,
                extractedWeight = null,
                aiSuggestedWeight = null,
            )
        assertThat(result.percentageOf1RM).isEqualTo(60f)
    }

    // Smart default tests for different exercise categories

    @Test
    fun calculateWorkingWeight_heavyCompound_smartDefaults() {
        // Squat
        var result =
            calculator.calculateWorkingWeight(
                exerciseName = "Back Squat",
                repRange = 3..5,
                programmeType = ProgrammeType.GENERAL,
                user1RM = null,
                extractedWeight = null,
                aiSuggestedWeight = null,
            )
        assertThat(result.weight).isEqualTo(70f)

        // Deadlift
        result =
            calculator.calculateWorkingWeight(
                exerciseName = "Conventional Deadlift",
                repRange = 8..10,
                programmeType = ProgrammeType.GENERAL,
                user1RM = null,
                extractedWeight = null,
                aiSuggestedWeight = null,
            )
        assertThat(result.weight).isEqualTo(60f)

        // High reps
        result =
            calculator.calculateWorkingWeight(
                exerciseName = "Squat",
                repRange = 12..15,
                programmeType = ProgrammeType.GENERAL,
                user1RM = null,
                extractedWeight = null,
                aiSuggestedWeight = null,
            )
        assertThat(result.weight).isEqualTo(50f)
    }

    @Test
    fun calculateWorkingWeight_mediumCompound_smartDefaults() {
        // Bench Press
        var result =
            calculator.calculateWorkingWeight(
                exerciseName = "Barbell Bench Press",
                repRange = 3..5,
                programmeType = ProgrammeType.GENERAL,
                user1RM = null,
                extractedWeight = null,
                aiSuggestedWeight = null,
            )
        assertThat(result.weight).isEqualTo(50f)

        // Overhead Press
        result =
            calculator.calculateWorkingWeight(
                exerciseName = "Overhead Press",
                repRange = 8..10,
                programmeType = ProgrammeType.GENERAL,
                user1RM = null,
                extractedWeight = null,
                aiSuggestedWeight = null,
            )
        assertThat(result.weight).isEqualTo(40f)

        // Row
        result =
            calculator.calculateWorkingWeight(
                exerciseName = "Barbell Row",
                repRange = 12..15,
                programmeType = ProgrammeType.GENERAL,
                user1RM = null,
                extractedWeight = null,
                aiSuggestedWeight = null,
            )
        assertThat(result.weight).isEqualTo(35f)

        // Pull-up (not assisted)
        result =
            calculator.calculateWorkingWeight(
                exerciseName = "Pull-Up",
                repRange = 8..10,
                programmeType = ProgrammeType.GENERAL,
                user1RM = null,
                extractedWeight = null,
                aiSuggestedWeight = null,
            )
        assertThat(result.weight).isEqualTo(40f)
    }

    @Test
    fun calculateWorkingWeight_lightCompound_smartDefaults() {
        // Lunge
        var result =
            calculator.calculateWorkingWeight(
                exerciseName = "Walking Lunge",
                repRange = 3..5,
                programmeType = ProgrammeType.GENERAL,
                user1RM = null,
                extractedWeight = null,
                aiSuggestedWeight = null,
            )
        assertThat(result.weight).isEqualTo(35f)

        // Step Up
        result =
            calculator.calculateWorkingWeight(
                exerciseName = "Box Step Up",
                repRange = 8..10,
                programmeType = ProgrammeType.GENERAL,
                user1RM = null,
                extractedWeight = null,
                aiSuggestedWeight = null,
            )
        assertThat(result.weight).isEqualTo(30f)

        // Bulgarian Split Squat
        result =
            calculator.calculateWorkingWeight(
                exerciseName = "Bulgarian Split Squat",
                repRange = 12..15,
                programmeType = ProgrammeType.GENERAL,
                user1RM = null,
                extractedWeight = null,
                aiSuggestedWeight = null,
            )
        assertThat(result.weight).isEqualTo(25f)

        // Front Squat
        result =
            calculator.calculateWorkingWeight(
                exerciseName = "Front Squat",
                repRange = 8..10,
                programmeType = ProgrammeType.GENERAL,
                user1RM = null,
                extractedWeight = null,
                aiSuggestedWeight = null,
            )
        assertThat(result.weight).isEqualTo(30f)
    }

    @Test
    fun calculateWorkingWeight_isolation_smartDefaults() {
        // Curl
        var result =
            calculator.calculateWorkingWeight(
                exerciseName = "Barbell Curl",
                repRange = 6..8,
                programmeType = ProgrammeType.GENERAL,
                user1RM = null,
                extractedWeight = null,
                aiSuggestedWeight = null,
            )
        assertThat(result.weight).isEqualTo(25f)

        // Extension
        result =
            calculator.calculateWorkingWeight(
                exerciseName = "Tricep Extension",
                repRange = 12..15,
                programmeType = ProgrammeType.GENERAL,
                user1RM = null,
                extractedWeight = null,
                aiSuggestedWeight = null,
            )
        assertThat(result.weight).isEqualTo(20f)

        // Raise
        result =
            calculator.calculateWorkingWeight(
                exerciseName = "Lateral Raise",
                repRange = 15..20,
                programmeType = ProgrammeType.GENERAL,
                user1RM = null,
                extractedWeight = null,
                aiSuggestedWeight = null,
            )
        assertThat(result.weight).isEqualTo(15f)

        // Fly
        result =
            calculator.calculateWorkingWeight(
                exerciseName = "Cable Fly",
                repRange = 10..12,
                programmeType = ProgrammeType.GENERAL,
                user1RM = null,
                extractedWeight = null,
                aiSuggestedWeight = null,
            )
        assertThat(result.weight).isEqualTo(20f)

        // Flye (alternate spelling)
        result =
            calculator.calculateWorkingWeight(
                exerciseName = "Dumbbell Flye",
                repRange = 10..12,
                programmeType = ProgrammeType.GENERAL,
                user1RM = null,
                extractedWeight = null,
                aiSuggestedWeight = null,
            )
        assertThat(result.weight).isEqualTo(20f)
    }

    @Test
    fun calculateWorkingWeight_bodyweight_returnsZero() {
        // Push-up
        var result =
            calculator.calculateWorkingWeight(
                exerciseName = "Push-Up",
                repRange = 10..15,
                programmeType = ProgrammeType.GENERAL,
                user1RM = null,
                extractedWeight = null,
                aiSuggestedWeight = null,
            )
        assertThat(result.weight).isEqualTo(0f)

        // Push up (with space) - this should work correctly
        result =
            calculator.calculateWorkingWeight(
                exerciseName = "Diamond Push Up",
                repRange = 10..15,
                programmeType = ProgrammeType.GENERAL,
                user1RM = null,
                extractedWeight = null,
                aiSuggestedWeight = null,
            )
        assertThat(result.weight).isEqualTo(0f)

        // Bodyweight - this will correctly categorize as BODYWEIGHT
        result =
            calculator.calculateWorkingWeight(
                exerciseName = "Bodyweight Squat",
                repRange = 20..30,
                programmeType = ProgrammeType.GENERAL,
                user1RM = null,
                extractedWeight = null,
                aiSuggestedWeight = null,
            )
        assertThat(result.weight).isEqualTo(0f)

        // Plank - this will correctly categorize as BODYWEIGHT
        result =
            calculator.calculateWorkingWeight(
                exerciseName = "Plank",
                repRange = 1..1,
                programmeType = ProgrammeType.GENERAL,
                user1RM = null,
                extractedWeight = null,
                aiSuggestedWeight = null,
            )
        assertThat(result.weight).isEqualTo(0f)
    }

    @Test
    fun calculateWorkingWeight_unknownExercise_returnsFallback() {
        val result =
            calculator.calculateWorkingWeight(
                exerciseName = "Some Random Exercise Name",
                repRange = 8..10,
                programmeType = ProgrammeType.GENERAL,
                user1RM = null,
                extractedWeight = null,
                aiSuggestedWeight = null,
            )
        assertThat(result.weight).isEqualTo(45f)
        assertThat(result.source).isEqualTo("smart_default")
    }

    // determineProgrammeType tests

    @Test
    fun determineProgrammeType_strengthKeywords() {
        assertThat(calculator.determineProgrammeType("Strength training program"))
            .isEqualTo(ProgrammeType.STRENGTH)
        assertThat(calculator.determineProgrammeType("Powerlifting routine"))
            .isEqualTo(ProgrammeType.STRENGTH)
        assertThat(calculator.determineProgrammeType("Build max strength"))
            .isEqualTo(ProgrammeType.STRENGTH)
        assertThat(calculator.determineProgrammeType("Increase 1RM"))
            .isEqualTo(ProgrammeType.STRENGTH)
        assertThat(calculator.determineProgrammeType("STRENGTH"))
            .isEqualTo(ProgrammeType.STRENGTH)
    }

    @Test
    fun determineProgrammeType_hypertrophyKeywords() {
        assertThat(calculator.determineProgrammeType("Hypertrophy program"))
            .isEqualTo(ProgrammeType.HYPERTROPHY)
        assertThat(calculator.determineProgrammeType("Build muscle mass"))
            .isEqualTo(ProgrammeType.HYPERTROPHY)
        assertThat(calculator.determineProgrammeType("Bodybuilding routine"))
            .isEqualTo(ProgrammeType.HYPERTROPHY)
        assertThat(calculator.determineProgrammeType("Increase size"))
            .isEqualTo(ProgrammeType.HYPERTROPHY)
        assertThat(calculator.determineProgrammeType("HYPERTROPHY"))
            .isEqualTo(ProgrammeType.HYPERTROPHY)
    }

    @Test
    fun determineProgrammeType_enduranceKeywords() {
        assertThat(calculator.determineProgrammeType("Endurance training"))
            .isEqualTo(ProgrammeType.ENDURANCE)
        assertThat(calculator.determineProgrammeType("Cardio workout"))
            .isEqualTo(ProgrammeType.ENDURANCE)
        assertThat(calculator.determineProgrammeType("Conditioning program"))
            .isEqualTo(ProgrammeType.ENDURANCE)
        assertThat(calculator.determineProgrammeType("ENDURANCE"))
            .isEqualTo(ProgrammeType.ENDURANCE)
    }

    @Test
    fun determineProgrammeType_noKeywords_returnsGeneral() {
        assertThat(calculator.determineProgrammeType("Regular workout"))
            .isEqualTo(ProgrammeType.GENERAL)
        assertThat(calculator.determineProgrammeType("Fitness program"))
            .isEqualTo(ProgrammeType.GENERAL)
        assertThat(calculator.determineProgrammeType(""))
            .isEqualTo(ProgrammeType.GENERAL)
        assertThat(calculator.determineProgrammeType("Some random text"))
            .isEqualTo(ProgrammeType.GENERAL)
    }

    @Test
    fun determineProgrammeType_caseInsensitive() {
        assertThat(calculator.determineProgrammeType("STRENGTH"))
            .isEqualTo(ProgrammeType.STRENGTH)
        assertThat(calculator.determineProgrammeType("Strength"))
            .isEqualTo(ProgrammeType.STRENGTH)
        assertThat(calculator.determineProgrammeType("strength"))
            .isEqualTo(ProgrammeType.STRENGTH)
        assertThat(calculator.determineProgrammeType("StReNgTh"))
            .isEqualTo(ProgrammeType.STRENGTH)
    }

    // Edge cases

    @Test
    fun calculateWorkingWeight_singleRepRange() {
        val result =
            calculator.calculateWorkingWeight(
                exerciseName = "Deadlift",
                repRange = 1..1,
                programmeType = ProgrammeType.STRENGTH,
                user1RM = 200f,
                extractedWeight = null,
                aiSuggestedWeight = null,
            )

        // Average 1 rep, should be 85%
        assertThat(result.weight).isEqualTo(170f)
        assertThat(result.percentageOf1RM).isEqualTo(85f)
    }

    @Test
    fun calculateWorkingWeight_veryWideRepRange() {
        val result =
            calculator.calculateWorkingWeight(
                exerciseName = "Squat",
                repRange = 1..20,
                programmeType = ProgrammeType.GENERAL,
                user1RM = 100f,
                extractedWeight = null,
                aiSuggestedWeight = null,
            )

        // Average 10.5 reps, should be 65%
        assertThat(result.weight).isEqualTo(65f)
        assertThat(result.percentageOf1RM).isEqualTo(65f)
    }

    @Test
    fun calculateWorkingWeight_negativeAISuggested_skipsToDefault() {
        val result =
            calculator.calculateWorkingWeight(
                exerciseName = "Squat",
                repRange = 3..5,
                programmeType = ProgrammeType.STRENGTH,
                user1RM = null,
                extractedWeight = null,
                aiSuggestedWeight = -10f,
            )

        // Should skip negative AI weight and use smart default
        assertThat(result.weight).isEqualTo(70f)
        assertThat(result.source).isEqualTo("smart_default")
    }

    @Test
    fun calculateWorkingWeight_complexExerciseNames() {
        // Exercise with multiple keywords
        var result =
            calculator.calculateWorkingWeight(
                exerciseName = "Paused Front Squat with Chains",
                repRange = 5..5,
                programmeType = ProgrammeType.GENERAL,
                user1RM = null,
                extractedWeight = null,
                aiSuggestedWeight = null,
            )
        // Should match "front squat" -> light compound
        assertThat(result.weight).isEqualTo(35f)

        // Exercise with numbers
        result =
            calculator.calculateWorkingWeight(
                exerciseName = "21s Barbell Curl",
                repRange = 21..21,
                programmeType = ProgrammeType.GENERAL,
                user1RM = null,
                extractedWeight = null,
                aiSuggestedWeight = null,
            )
        // Should match "curl" -> isolation
        assertThat(result.weight).isEqualTo(15f)
    }

    @Test
    fun calculateWorkingWeight_mixedCaseExerciseNames() {
        var result =
            calculator.calculateWorkingWeight(
                exerciseName = "BARBELL SQUAT",
                repRange = 3..5,
                programmeType = ProgrammeType.GENERAL,
                user1RM = null,
                extractedWeight = null,
                aiSuggestedWeight = null,
            )
        assertThat(result.weight).isEqualTo(70f)

        result =
            calculator.calculateWorkingWeight(
                exerciseName = "BaRbElL cUrL",
                repRange = 8..10,
                programmeType = ProgrammeType.GENERAL,
                user1RM = null,
                extractedWeight = null,
                aiSuggestedWeight = null,
            )
        assertThat(result.weight).isEqualTo(20f)
    }

    @Test
    fun calculateWorkingWeight_percentageCalculation_precision() {
        val result =
            calculator.calculateWorkingWeight(
                exerciseName = "Squat",
                repRange = 3..4, // Average 3.5
                programmeType = ProgrammeType.STRENGTH,
                user1RM = 137.5f,
                extractedWeight = null,
                aiSuggestedWeight = null,
            )

        // Should be 80% of 137.5 = 110
        assertThat(result.weight).isEqualTo(110f)
        assertThat(result.percentageOf1RM).isEqualTo(80f)
    }

    @Test
    fun calculateWorkingWeight_extractedWeightPercentageCalculation() {
        val result =
            calculator.calculateWorkingWeight(
                exerciseName = "Squat",
                repRange = 5..5,
                programmeType = ProgrammeType.STRENGTH,
                user1RM = 150f,
                extractedWeight = 112.5f,
                aiSuggestedWeight = null,
            )

        assertThat(result.weight).isEqualTo(112.5f)
        assertThat(result.percentageOf1RM).isEqualTo(75f)
        assertThat(result.source).isEqualTo("user_input")
    }
}
