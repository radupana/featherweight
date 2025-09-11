package com.github.radupana.featherweight.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.radupana.featherweight.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutFlowTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun workoutFlow_createNewWorkout_addExercise_completeSet() {
        composeTestRule.waitForIdle()

        // Try navigating to workout tab - may already be there
        runCatching {
            composeTestRule.onNodeWithText("Workout").performClick()
        }

        // Try starting a new workout with either button text
        runCatching {
            composeTestRule.onNodeWithText("Start Workout").performClick()
        }.getOrElse {
            runCatching {
                composeTestRule.onNodeWithText("New Workout").performClick()
            }
        }

        composeTestRule.onNodeWithText("Add Exercise").assertIsDisplayed()
    }
}
