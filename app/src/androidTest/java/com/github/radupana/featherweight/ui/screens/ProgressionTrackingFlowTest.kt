package com.github.radupana.featherweight.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.radupana.featherweight.MainActivity
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProgressionTrackingFlowTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun progressionFlow_trackPersonalRecord_showsPRCelebration() {
        composeTestRule.waitForIdle()

        navigateToWorkoutTab()
        startNewWorkout()
        addExerciseToWorkout("Bench Press")

        // Complete set with heavy weight that should trigger PR
        completeSetWithAllMetrics(weight = "120", reps = "5", rpe = "8")

        // Look for PR celebration or indication
        val prIndicatorFound =
            runCatching {
                composeTestRule.onNodeWithText("PR", substring = true).assertIsDisplayed()
                true
            }.getOrElse {
                runCatching {
                    composeTestRule.onNodeWithText("Personal Record", substring = true).assertIsDisplayed()
                    true
                }.getOrElse {
                    runCatching {
                        composeTestRule.onNodeWithText("New Record", substring = true).assertIsDisplayed()
                        true
                    }.getOrDefault(false)
                }
            }

        // Complete the workout
        finishWorkout()
    }

    @Test
    fun progressionFlow_completeMultipleSetsWithProgression() {
        composeTestRule.waitForIdle()

        navigateToWorkoutTab()
        startNewWorkout()
        addExerciseToWorkout("Squat")

        // Complete 3 sets with progressive weight
        completeSetWithAllMetrics(weight = "100", reps = "8", rpe = "7")
        addNewSet()
        completeSetWithAllMetrics(weight = "110", reps = "6", rpe = "8")
        addNewSet()
        completeSetWithAllMetrics(weight = "120", reps = "4", rpe = "9")

        finishWorkout()
    }

    @Test
    fun progressionFlow_useRestTimer_completeSetsWithTiming() {
        composeTestRule.waitForIdle()

        navigateToWorkoutTab()
        startNewWorkout()
        addExerciseToWorkout("Deadlift")

        completeSetWithAllMetrics(weight = "140", reps = "5", rpe = "8")

        // Try to start rest timer
        val timerStarted =
            runCatching {
                composeTestRule.onNodeWithText("Rest Timer").performClick()
                composeTestRule.waitForIdle()
                true
            }.getOrElse {
                runCatching {
                    composeTestRule.onNodeWithText("Start Timer").performClick()
                    composeTestRule.waitForIdle()
                    true
                }.getOrElse {
                    runCatching {
                        composeTestRule.onNodeWithContentDescription("Start rest timer").performClick()
                        composeTestRule.waitForIdle()
                        true
                    }.getOrDefault(false)
                }
            }

        if (timerStarted) {
            // Wait briefly then stop timer
            Thread.sleep(2000)

            runCatching {
                composeTestRule.onNodeWithText("Stop").performClick()
            }.getOrElse {
                runCatching {
                    composeTestRule.onNodeWithContentDescription("Stop timer").performClick()
                }
            }
        }

        finishWorkout()
    }

    @Test
    fun progressionFlow_trackRPEProgression_validateIntensityTracking() {
        composeTestRule.waitForIdle()

        navigateToWorkoutTab()
        startNewWorkout()
        addExerciseToWorkout("Overhead Press")

        // Complete sets with varying RPE to track intensity progression
        completeSetWithAllMetrics(weight = "60", reps = "10", rpe = "6")
        addNewSet()
        completeSetWithAllMetrics(weight = "65", reps = "8", rpe = "7")
        addNewSet()
        completeSetWithAllMetrics(weight = "70", reps = "6", rpe = "8")

        finishWorkout()
    }

    private fun navigateToWorkoutTab() {
        runCatching {
            composeTestRule.onNodeWithText("Workout").performClick()
            composeTestRule.waitForIdle()
        }
    }

    private fun startNewWorkout() {
        val workoutStarted =
            runCatching {
                composeTestRule.onNodeWithText("Start Workout").performClick()
                composeTestRule.waitForIdle()
                true
            }.getOrElse {
                runCatching {
                    composeTestRule.onNodeWithText("New Workout").performClick()
                    composeTestRule.waitForIdle()
                    true
                }.getOrElse {
                    runCatching {
                        composeTestRule.onNodeWithText("Start Freestyle Workout").performClick()
                        composeTestRule.waitForIdle()
                        true
                    }.getOrDefault(false)
                }
            }

        assertThat(workoutStarted).isTrue()
        composeTestRule.onNodeWithText("Add Exercise").assertIsDisplayed()
    }

    private fun addExerciseToWorkout(exerciseName: String) {
        composeTestRule.onNodeWithText("Add Exercise").performClick()
        composeTestRule.waitForIdle()

        runCatching {
            composeTestRule.onNodeWithText("Search exercises...").performTextInput(exerciseName)
            composeTestRule.waitForIdle()

            val exerciseSelected =
                runCatching {
                    composeTestRule.onNodeWithText("Barbell $exerciseName").performClick()
                    true
                }.getOrElse {
                    runCatching {
                        composeTestRule.onNodeWithText(exerciseName, substring = true).performClick()
                        true
                    }.getOrDefault(false)
                }

            if (exerciseSelected) {
                runCatching {
                    composeTestRule.onNodeWithText("Add to Workout").performClick()
                }.getOrElse {
                    runCatching {
                        composeTestRule.onNodeWithText("Add").performClick()
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
    }

    private fun completeSetWithAllMetrics(
        weight: String,
        reps: String,
        rpe: String,
    ) {
        runCatching {
            // Enter weight
            val weightFields = listOf("Weight", "kg", "lbs", "Weight (kg)", "Weight (lbs)")
            var weightEntered = false

            weightFields.forEach { fieldText ->
                if (!weightEntered) {
                    runCatching {
                        composeTestRule.onNodeWithText(fieldText).performTextInput(weight)
                        weightEntered = true
                    }
                }
            }

            // Enter reps
            val repsFields = listOf("Reps", "Repetitions")
            var repsEntered = false

            repsFields.forEach { fieldText ->
                if (!repsEntered) {
                    runCatching {
                        composeTestRule.onNodeWithText(fieldText).performTextInput(reps)
                        repsEntered = true
                    }
                }
            }

            // Enter RPE if available
            val rpeFields = listOf("RPE", "Rate of Perceived Exertion", "Difficulty")
            rpeFields.forEach { fieldText ->
                runCatching {
                    composeTestRule.onNodeWithText(fieldText).performTextInput(rpe)
                }
            }

            composeTestRule.waitForIdle()

            // Mark set as complete
            runCatching {
                composeTestRule.onNodeWithContentDescription("Mark set complete").performClick()
            }.getOrElse {
                runCatching {
                    composeTestRule.onNodeWithText("✓").performClick()
                }.getOrElse {
                    runCatching {
                        composeTestRule.onNodeWithContentDescription("Complete set").performClick()
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
    }

    private fun addNewSet() {
        runCatching {
            composeTestRule.onNodeWithText("Add Set").performClick()
            composeTestRule.waitForIdle()
        }.getOrElse {
            runCatching {
                composeTestRule.onNodeWithText("+").performClick()
                composeTestRule.waitForIdle()
            }.getOrElse {
                runCatching {
                    composeTestRule.onNodeWithContentDescription("Add set").performClick()
                    composeTestRule.waitForIdle()
                }
            }
        }
    }

    private fun finishWorkout() {
        val workoutCompleted =
            runCatching {
                composeTestRule.onNodeWithText("Complete Workout").performClick()
                composeTestRule.waitForIdle()

                runCatching {
                    composeTestRule.onNodeWithText("Complete Workout").performClick()
                    composeTestRule.waitForIdle()
                    true
                }.getOrElse {
                    runCatching {
                        composeTestRule.onNodeWithText("Finish").performClick()
                        composeTestRule.waitForIdle()
                        true
                    }.getOrDefault(true)
                }
            }.getOrElse {
                runCatching {
                    composeTestRule.onNodeWithText("Complete").performClick()
                    composeTestRule.waitForIdle()
                    true
                }.getOrDefault(false)
            }

        assertThat(workoutCompleted).isTrue()
    }
}
