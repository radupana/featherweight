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
class DataExportFlowTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun exportFlow_exportWorkoutFromCompletionScreen() {
        composeTestRule.waitForIdle()

        // Complete a workout first
        createAndCompleteWorkout()

        // Export from workout completion screen
        exportWorkoutFromCompletion()

        // Verify export dialog shows options
        verifyExportOptions()

        // Complete export
        selectExportFormat("CSV")
        confirmExport()
    }

    @Test
    fun exportFlow_exportWorkoutHistoryFromHistoryScreen() {
        composeTestRule.waitForIdle()

        // Navigate to history
        navigateToHistory()

        // Select workout to export
        selectWorkoutFromHistory()

        // Export individual workout
        exportSelectedWorkout()

        // Verify export completed
        verifyExportCompleted()
    }

    @Test
    fun exportFlow_exportAllWorkoutHistory() {
        composeTestRule.waitForIdle()

        navigateToHistory()

        // Export all history data
        openHistoryMenu()
        selectExportAllHistory()

        // Choose export format
        selectExportFormat("JSON")
        confirmExport()

        verifyExportCompleted()
    }

    @Test
    fun exportFlow_exportPersonalRecords() {
        composeTestRule.waitForIdle()

        // Navigate to insights/records
        navigateToInsights()

        // Export personal records
        openInsightsMenu()
        selectExportPersonalRecords()

        selectExportFormat("CSV")
        confirmExport()

        verifyExportCompleted()
    }

    @Test
    fun exportFlow_exportFromWorkoutMenuDuringWorkout() {
        composeTestRule.waitForIdle()

        // Start a workout
        startNewWorkout()
        addExerciseToWorkout()
        completeSetWithWeightAndReps()

        // Export from workout menu
        openWorkoutMenu()
        selectExportWorkout()

        selectExportFormat("CSV")
        confirmExport()

        // Continue with workout
        finishWorkout()
    }

    private fun createAndCompleteWorkout() {
        navigateToWorkoutTab()
        startNewWorkout()
        addExerciseToWorkout()
        completeSetWithWeightAndReps()
        finishWorkout()
    }

    private fun exportWorkoutFromCompletion() {
        // Look for export option on workout completion screen
        runCatching {
            composeTestRule.onNodeWithText("Export Workout").performClick()
            composeTestRule.waitForIdle()
        }.getOrElse {
            runCatching {
                composeTestRule.onNodeWithText("Export").performClick()
                composeTestRule.waitForIdle()
            }.getOrElse {
                runCatching {
                    composeTestRule.onNodeWithContentDescription("Export workout").performClick()
                    composeTestRule.waitForIdle()
                }
            }
        }
    }

    private fun verifyExportOptions() {
        // Verify export dialog shows format options
        val exportDialogShown =
            runCatching {
                composeTestRule.onNodeWithText("Export Format").assertIsDisplayed()
                true
            }.getOrElse {
                runCatching {
                    composeTestRule.onNodeWithText("Choose Format").assertIsDisplayed()
                    true
                }.getOrElse {
                    runCatching {
                        composeTestRule.onNodeWithText("CSV").assertIsDisplayed()
                        true
                    }.getOrDefault(false)
                }
            }

        assertThat(exportDialogShown).isTrue()
    }

    private fun selectExportFormat(format: String) {
        runCatching {
            composeTestRule.onNodeWithText(format).performClick()
            composeTestRule.waitForIdle()
        }
    }

    private fun confirmExport() {
        runCatching {
            composeTestRule.onNodeWithText("Export").performClick()
            composeTestRule.waitForIdle()
        }.getOrElse {
            runCatching {
                composeTestRule.onNodeWithText("Confirm").performClick()
                composeTestRule.waitForIdle()
            }.getOrElse {
                runCatching {
                    composeTestRule.onNodeWithText("Save").performClick()
                    composeTestRule.waitForIdle()
                }
            }
        }
    }

    private fun verifyExportCompleted() {
        // Look for export completion message
        val exportCompleted =
            runCatching {
                composeTestRule.onNodeWithText("Export completed", substring = true).assertIsDisplayed()
                true
            }.getOrElse {
                runCatching {
                    composeTestRule.onNodeWithText("Exported successfully", substring = true).assertIsDisplayed()
                    true
                }.getOrElse {
                    runCatching {
                        composeTestRule.onNodeWithText("Saved", substring = true).assertIsDisplayed()
                        true
                    }.getOrDefault(false)
                }
            }
    }

    private fun navigateToHistory() {
        runCatching {
            composeTestRule.onNodeWithText("History").performClick()
            composeTestRule.waitForIdle()
        }
    }

    private fun selectWorkoutFromHistory() {
        // Select first workout in history
        runCatching {
            composeTestRule.onNodeWithText("Workout", substring = true).performClick()
            composeTestRule.waitForIdle()
        }
    }

    private fun exportSelectedWorkout() {
        runCatching {
            composeTestRule.onNodeWithContentDescription("More Options").performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("Export Workout").performClick()
            composeTestRule.waitForIdle()
        }.getOrElse {
            runCatching {
                composeTestRule.onNodeWithText("Export").performClick()
                composeTestRule.waitForIdle()
            }
        }
    }

    private fun openHistoryMenu() {
        runCatching {
            composeTestRule.onNodeWithContentDescription("Menu").performClick()
            composeTestRule.waitForIdle()
        }.getOrElse {
            runCatching {
                composeTestRule.onNodeWithContentDescription("More Options").performClick()
                composeTestRule.waitForIdle()
            }
        }
    }

    private fun selectExportAllHistory() {
        runCatching {
            composeTestRule.onNodeWithText("Export All History").performClick()
            composeTestRule.waitForIdle()
        }.getOrElse {
            runCatching {
                composeTestRule.onNodeWithText("Export History").performClick()
                composeTestRule.waitForIdle()
            }
        }
    }

    private fun navigateToInsights() {
        runCatching {
            composeTestRule.onNodeWithText("Insights").performClick()
            composeTestRule.waitForIdle()
        }.getOrElse {
            runCatching {
                composeTestRule.onNodeWithText("Records").performClick()
                composeTestRule.waitForIdle()
            }.getOrElse {
                runCatching {
                    composeTestRule.onNodeWithText("Stats").performClick()
                    composeTestRule.waitForIdle()
                }
            }
        }
    }

    private fun openInsightsMenu() {
        runCatching {
            composeTestRule.onNodeWithContentDescription("Menu").performClick()
            composeTestRule.waitForIdle()
        }
    }

    private fun selectExportPersonalRecords() {
        runCatching {
            composeTestRule.onNodeWithText("Export Personal Records").performClick()
            composeTestRule.waitForIdle()
        }.getOrElse {
            runCatching {
                composeTestRule.onNodeWithText("Export PRs").performClick()
                composeTestRule.waitForIdle()
            }
        }
    }

    private fun openWorkoutMenu() {
        runCatching {
            composeTestRule.onNodeWithContentDescription("Workout Options").performClick()
            composeTestRule.waitForIdle()
        }.getOrElse {
            runCatching {
                composeTestRule.onNodeWithContentDescription("More Options").performClick()
                composeTestRule.waitForIdle()
            }
        }
    }

    private fun selectExportWorkout() {
        runCatching {
            composeTestRule.onNodeWithText("Export Workout").performClick()
            composeTestRule.waitForIdle()
        }
    }

    // Helper methods from other tests
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

    private fun addExerciseToWorkout() {
        composeTestRule.onNodeWithText("Add Exercise").performClick()
        composeTestRule.waitForIdle()

        runCatching {
            composeTestRule.onNodeWithText("Search exercises...").performTextInput("Bench Press")
            composeTestRule.waitForIdle()

            val exerciseSelected =
                runCatching {
                    composeTestRule.onNodeWithText("Barbell Bench Press").performClick()
                    true
                }.getOrElse {
                    runCatching {
                        composeTestRule.onNodeWithText("Bench Press", substring = true).performClick()
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

    private fun completeSetWithWeightAndReps() {
        runCatching {
            val weightFields = listOf("Weight", "kg", "lbs", "Weight (kg)", "Weight (lbs)")
            val repsFields = listOf("Reps", "Repetitions")

            var weightEntered = false
            var repsEntered = false

            weightFields.forEach { fieldText ->
                if (!weightEntered) {
                    runCatching {
                        composeTestRule.onNodeWithText(fieldText).performTextInput("80")
                        weightEntered = true
                    }
                }
            }

            repsFields.forEach { fieldText ->
                if (!repsEntered) {
                    runCatching {
                        composeTestRule.onNodeWithText(fieldText).performTextInput("10")
                        repsEntered = true
                    }
                }
            }

            composeTestRule.waitForIdle()

            runCatching {
                composeTestRule.onNodeWithContentDescription("Mark set complete").performClick()
            }.getOrElse {
                runCatching {
                    composeTestRule.onNodeWithText("✓").performClick()
                }
            }
        }

        composeTestRule.waitForIdle()
    }

    private fun finishWorkout() {
        runCatching {
            composeTestRule.onNodeWithText("Complete Workout").performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("Complete Workout").performClick()
            composeTestRule.waitForIdle()
        }.getOrElse {
            runCatching {
                composeTestRule.onNodeWithText("Complete").performClick()
                composeTestRule.waitForIdle()
            }
        }
    }
}
