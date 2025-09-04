package com.github.radupana.featherweight.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.radupana.featherweight.data.PRType
import com.github.radupana.featherweight.data.PersonalRecord
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class PRCelebrationDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    @Test
    fun `dialog displays New Personal Record title for weight PRs`() {
        val weightPR = createWeightPR()
        val records = listOf(weightPR)
        val exerciseNames = mapOf(1L to "Barbell Bench Press")
        var dismissCalled = false

        composeTestRule.setContent {
            PRCelebrationDialog(
                personalRecords = records,
                exerciseNames = exerciseNames,
                onDismiss = { dismissCalled = true }
            )
        }

        // Verify the title is displayed
        composeTestRule.onNodeWithText("New Personal Record!").assertIsDisplayed()
        
        // Verify exercise name is displayed
        composeTestRule.onNodeWithText("Barbell Bench Press").assertIsDisplayed()
        
        // Verify weight and reps are displayed (100kg × 5)
        composeTestRule.onNodeWithText("100kg × 5 @ RPE 8").assertIsDisplayed()
        
        // Verify Continue Workout button exists
        composeTestRule.onNodeWithText("Continue Workout").assertIsDisplayed()
    }

    @Test
    fun `dialog displays New Personal Record title for 1RM PRs`() {
        val oneRmPR = createOneRMPR()
        val records = listOf(oneRmPR)
        val exerciseNames = mapOf(1L to "Barbell Squat")

        composeTestRule.setContent {
            PRCelebrationDialog(
                personalRecords = records,
                exerciseNames = exerciseNames,
                onDismiss = { }
            )
        }

        // Verify the title is displayed
        composeTestRule.onNodeWithText("New Personal Record!").assertIsDisplayed()
        
        // Verify exercise name is displayed
        composeTestRule.onNodeWithText("Barbell Squat").assertIsDisplayed()
        
        // For 1RM PR, should show the 1RM value
        composeTestRule.onNodeWithText("150kg").assertIsDisplayed()
        composeTestRule.onNodeWithText("Estimated One Rep Max").assertIsDisplayed()
        
        // Should show what achieved it with
        composeTestRule.onNodeWithText("Achieved with: 100kg × 5 @ RPE 8").assertIsDisplayed()
    }

    @Test
    fun `dialog shows only first PR when multiple PRs exist`() {
        val weightPR = createWeightPR()
        val oneRmPR = createOneRMPR()
        val records = listOf(weightPR, oneRmPR)
        val exerciseNames = mapOf(1L to "Barbell Deadlift")

        composeTestRule.setContent {
            PRCelebrationDialog(
                personalRecords = records,
                exerciseNames = exerciseNames,
                onDismiss = { }
            )
        }

        // Should show the first PR (weight PR)
        composeTestRule.onNodeWithText("Barbell Deadlift").assertIsDisplayed()
        composeTestRule.onNodeWithText("100kg × 5 @ RPE 8").assertIsDisplayed()
        
        // Should also show the new 1RM from the weight PR
        composeTestRule.onNodeWithText("New One Rep Max: 120kg").assertIsDisplayed()
        
        // Should NOT show the second PR's specific 150kg value (it shows 120kg from weight PR's 1RM)
        composeTestRule.onNodeWithText("150kg").assertDoesNotExist()
    }

    @Test
    fun `dialog shows New One Rep Max text when weight PR has estimated 1RM`() {
        val weightPR = createWeightPR().copy(estimated1RM = 125f)
        val records = listOf(weightPR)
        val exerciseNames = mapOf(1L to "Barbell Row")

        composeTestRule.setContent {
            PRCelebrationDialog(
                personalRecords = records,
                exerciseNames = exerciseNames,
                onDismiss = { }
            )
        }

        // Should display the New One Rep Max section for weight PR with 1RM
        composeTestRule.onNodeWithText("New One Rep Max: 125kg").assertIsDisplayed()
    }

    @Test
    fun `dialog handles dismiss callback when Continue Workout is clicked`() {
        val weightPR = createWeightPR()
        val records = listOf(weightPR)
        val exerciseNames = mapOf(1L to "Barbell Press")
        var dismissCalled = false

        composeTestRule.setContent {
            PRCelebrationDialog(
                personalRecords = records,
                exerciseNames = exerciseNames,
                onDismiss = { dismissCalled = true }
            )
        }

        // Click Continue Workout button
        composeTestRule.onNodeWithText("Continue Workout").performClick()
        
        // Verify dismiss was called
        assertThat(dismissCalled).isTrue()
    }

    @Test
    fun `dialog displays previous record comparison when available`() {
        val prWithPrevious = createWeightPR().copy(
            previousWeight = 95f,
            previousReps = 5,
            previousDate = LocalDateTime.of(2024, 1, 1, 10, 0),
            improvementPercentage = 5.26f
        )
        val records = listOf(prWithPrevious)
        val exerciseNames = mapOf(1L to "Barbell Curl")

        composeTestRule.setContent {
            PRCelebrationDialog(
                personalRecords = records,
                exerciseNames = exerciseNames,
                onDismiss = { }
            )
        }

        // Should show previous record
        composeTestRule.onNodeWithText("Previous: 95kg × 5").assertIsDisplayed()
        composeTestRule.onNodeWithText("Jan 01, 2024").assertIsDisplayed()
        composeTestRule.onNodeWithText("+5.3% improvement").assertIsDisplayed()
    }

    @Test
    fun `dialog displays PR without RPE when RPE is null`() {
        val prWithoutRpe = createWeightPR().copy(rpe = null)
        val records = listOf(prWithoutRpe)
        val exerciseNames = mapOf(1L to "Cable Row")

        composeTestRule.setContent {
            PRCelebrationDialog(
                personalRecords = records,
                exerciseNames = exerciseNames,
                onDismiss = { }
            )
        }

        // Should show weight and reps without RPE
        composeTestRule.onNodeWithText("100kg × 5").assertIsDisplayed()
        // Should NOT show RPE
        composeTestRule.onNodeWithText("@ RPE", substring = true).assertDoesNotExist()
    }

    @Test
    fun `dialog does not render when PR list is empty`() {
        val records = emptyList<PersonalRecord>()
        val exerciseNames = emptyMap<Long, String>()
        var dialogRendered = false

        composeTestRule.setContent {
            PRCelebrationDialog(
                personalRecords = records,
                exerciseNames = exerciseNames,
                onDismiss = { dialogRendered = true }
            )
        }

        // Dialog should not be displayed at all
        composeTestRule.onNodeWithText("New Personal Record!").assertDoesNotExist()
        composeTestRule.onNodeWithText("Continue Workout").assertDoesNotExist()
    }

    @Test
    fun `dialog handles unknown exercise name gracefully`() {
        val weightPR = createWeightPR()
        val records = listOf(weightPR)
        val exerciseNames = emptyMap<Long, String>() // No exercise name mapping

        composeTestRule.setContent {
            PRCelebrationDialog(
                personalRecords = records,
                exerciseNames = exerciseNames,
                onDismiss = { }
            )
        }

        // Should show "Unknown Exercise" when no mapping exists
        composeTestRule.onNodeWithText("Unknown Exercise").assertIsDisplayed()
        // Should still show the PR details
        composeTestRule.onNodeWithText("100kg × 5 @ RPE 8").assertIsDisplayed()
    }

    @Test
    fun `dialog shows potential lift suggestion when notes contain could potentially lift`() {
        val prWithNotes = createWeightPR().copy(
            notes = "Great lift! (Based on your 130.5kg 1RM, you could potentially lift more)"
        )
        val records = listOf(prWithNotes)
        val exerciseNames = mapOf(1L to "Barbell Squat")

        composeTestRule.setContent {
            PRCelebrationDialog(
                personalRecords = records,
                exerciseNames = exerciseNames,
                onDismiss = { }
            )
        }

        // Should show the potential lift suggestion
        composeTestRule.onNodeWithText("Your 130.5kg 1RM suggests you could lift more!").assertIsDisplayed()
    }

    // Helper functions
    private fun createWeightPR() =
        PersonalRecord(
            id = 1,
            exerciseVariationId = 1,
            weight = 100f,
            reps = 5,
            rpe = 8.5f,
            recordDate = LocalDateTime.now(),
            previousWeight = null,
            previousReps = null,
            previousDate = null,
            improvementPercentage = 0f,
            recordType = PRType.WEIGHT,
            volume = 500f,
            estimated1RM = 120f,
            notes = null,
            workoutId = 1,
        )

    private fun createOneRMPR() =
        PersonalRecord(
            id = 2,
            exerciseVariationId = 1,
            weight = 100f,
            reps = 5,
            rpe = 8.5f,
            recordDate = LocalDateTime.now(),
            previousWeight = null,
            previousReps = null,
            previousDate = null,
            improvementPercentage = 0f,
            recordType = PRType.ESTIMATED_1RM,
            volume = 500f,
            estimated1RM = 150f,
            notes = null,
            workoutId = 1,
        )

}
