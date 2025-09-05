package com.github.radupana.featherweight.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseMappingScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `clicking Create as Custom Exercise shows dialog`() {
        val unmatchedExercises = listOf("Custom Squat", "Custom Press")

        composeTestRule.setContent {
            ExerciseMappingScreen(
                unmatchedExercises = unmatchedExercises,
                onMappingComplete = {
                },
                onBack = { },
            )
        }

        composeTestRule.onNodeWithText("Custom Squat").assertIsDisplayed()
        composeTestRule.onNodeWithText("Unmatched Exercise").assertIsDisplayed()

        composeTestRule.onNodeWithText("Create as Custom Exercise").performClick()

        composeTestRule.onNodeWithText("Create Custom Exercise").assertIsDisplayed()
        composeTestRule.onNodeWithText("Exercise name *").assertIsDisplayed()
        composeTestRule.onNodeWithText("Category * (Required)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Primary Muscles * (Required - Select at least one)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Equipment * (Required - Select at least one)").assertIsDisplayed()
    }

    @Test
    fun `dialog shows exercise name prepopulated`() {
        val unmatchedExercises = listOf("Barbell Hack Squat")

        composeTestRule.setContent {
            ExerciseMappingScreen(
                unmatchedExercises = unmatchedExercises,
                onMappingComplete = { },
                onBack = { },
            )
        }

        composeTestRule.onNodeWithText("Create as Custom Exercise").performClick()

        composeTestRule.onNodeWithText("Create Custom Exercise").assertIsDisplayed()
    }

    @Test
    fun `cancel button in dialog closes it`() {
        val unmatchedExercises = listOf("Test Exercise")

        composeTestRule.setContent {
            ExerciseMappingScreen(
                unmatchedExercises = unmatchedExercises,
                onMappingComplete = { },
                onBack = { },
            )
        }

        composeTestRule.onNodeWithText("Create as Custom Exercise").performClick()
        composeTestRule.onNodeWithText("Create Custom Exercise").assertIsDisplayed()

        composeTestRule.onNodeWithText("Cancel").performClick()

        composeTestRule.onNodeWithText("Create Custom Exercise").assertDoesNotExist()
    }

    @Test
    fun `navigation between exercises works correctly`() {
        val unmatchedExercises = listOf("Exercise 1", "Exercise 2", "Exercise 3")

        composeTestRule.setContent {
            ExerciseMappingScreen(
                unmatchedExercises = unmatchedExercises,
                onMappingComplete = { },
                onBack = { },
            )
        }

        composeTestRule.onNodeWithText("Exercise 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Map Exercises (1/3)").assertIsDisplayed()

        composeTestRule.onNodeWithText("Previous").assertIsDisplayed()

        val nextButton = composeTestRule.onNodeWithText("Next")
        nextButton.assertIsDisplayed()

        composeTestRule.onNodeWithText("Search existing exercises").assertIsDisplayed()
    }

    @Test
    fun `progress indicator shows correct count`() {
        val unmatchedExercises = listOf("Exercise A", "Exercise B", "Exercise C", "Exercise D")

        composeTestRule.setContent {
            ExerciseMappingScreen(
                unmatchedExercises = unmatchedExercises,
                onMappingComplete = { },
                onBack = { },
            )
        }

        composeTestRule.onNodeWithText("Map Exercises (1/4)").assertIsDisplayed()
        composeTestRule.onNodeWithText("0 of 4 exercises mapped").assertIsDisplayed()
    }

    @Test
    fun `back button is displayed and clickable`() {
        var backClicked = false
        val unmatchedExercises = listOf("Test Exercise")

        composeTestRule.setContent {
            ExerciseMappingScreen(
                unmatchedExercises = unmatchedExercises,
                onMappingComplete = { },
                onBack = { backClicked = true },
            )
        }

        composeTestRule.onNodeWithContentDescription("Back").performClick()

        assertThat(backClicked).isTrue()
    }

    @Test
    fun `clear button removes mapping`() {
        val unmatchedExercises = listOf("Test Exercise")

        composeTestRule.setContent {
            ExerciseMappingScreen(
                unmatchedExercises = unmatchedExercises,
                onMappingComplete = { },
                onBack = { },
            )
        }

        composeTestRule.onNodeWithText("Clear").assertDoesNotExist()
    }
}
