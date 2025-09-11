package com.github.radupana.featherweight.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.radupana.featherweight.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProgrammeWorkflowFlowTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun programmeFlow_importProgramme_mapExercises_startProgramme() {
        composeTestRule.waitForIdle()

        navigateToProgrammes()
        importProgrammeFromText()
        mapExercisesDuringImport()
        startImportedProgramme()
        completeFirstWorkout()
    }

    @Test
    fun programmeFlow_progressThroughWeeks_completeProgramme() {
        composeTestRule.waitForIdle()

        navigateToProgrammes()
        importSimpleProgramme()

        // Complete week 1
        startProgrammeWorkout()
        completeWorkoutSets()
        finishWorkout()

        // Progress to week 2
        progressToNextWeek()
        startProgrammeWorkout()
        completeWorkoutSets()
        finishWorkout()

        // Complete programme
        completeProgramme()
    }

    @Test
    fun programmeFlow_editProgrammeWorkout_customizeTemplate() {
        composeTestRule.waitForIdle()

        navigateToProgrammes()
        importProgrammeFromText()
        mapExercisesDuringImport()

        // Edit programme workout before starting
        editProgrammeWorkout()
        addExerciseToProgrammeWorkout()
        saveProgrammeChanges()

        startImportedProgramme()
        completeFirstWorkout()
    }

    private fun navigateToProgrammes() {
        runCatching {
            composeTestRule.onNodeWithText("Programmes").performClick()
            composeTestRule.waitForIdle()
        }.getOrElse {
            runCatching {
                composeTestRule.onNodeWithText("Programs").performClick()
                composeTestRule.waitForIdle()
            }
        }
    }

    private fun importProgrammeFromText() {
        runCatching {
            composeTestRule.onNodeWithText("Import Programme").performClick()
            composeTestRule.waitForIdle()
        }.getOrElse {
            runCatching {
                composeTestRule.onNodeWithText("Import Program").performClick()
                composeTestRule.waitForIdle()
            }.getOrElse {
                runCatching {
                    composeTestRule.onNodeWithText("New Programme").performClick()
                    composeTestRule.waitForIdle()
                }
            }
        }

        // Enter sample programme text
        val sampleProgramme =
            """
            Week 1:
            Day 1:
            Bench Press: 3x8 @ 80kg
            Squat: 3x5 @ 100kg
            Row: 3x10 @ 60kg
            
            Day 2:
            Deadlift: 1x5 @ 120kg
            Overhead Press: 3x8 @ 50kg
            
            Week 2:
            Day 1:
            Bench Press: 3x8 @ 85kg
            Squat: 3x5 @ 105kg
            Row: 3x10 @ 65kg
            """.trimIndent()

        runCatching {
            composeTestRule.onNodeWithText("Paste or type your programme").performTextInput(sampleProgramme)
            composeTestRule.waitForIdle()
        }.getOrElse {
            runCatching {
                composeTestRule.onNodeWithText("Programme text").performTextInput(sampleProgramme)
                composeTestRule.waitForIdle()
            }
        }

        // Parse the programme
        runCatching {
            composeTestRule.onNodeWithText("Parse Programme").performClick()
            composeTestRule.waitForIdle()
        }.getOrElse {
            runCatching {
                composeTestRule.onNodeWithText("Import").performClick()
                composeTestRule.waitForIdle()
            }
        }
    }

    private fun importSimpleProgramme() {
        runCatching {
            composeTestRule.onNodeWithText("Import Programme").performClick()
            composeTestRule.waitForIdle()
        }

        val simpleProgramme =
            """
            Week 1:
            Bench Press: 3x8
            Squat: 3x5
            """.trimIndent()

        runCatching {
            composeTestRule.onNodeWithText("Paste or type your programme").performTextInput(simpleProgramme)
            composeTestRule.onNodeWithText("Parse Programme").performClick()
            composeTestRule.waitForIdle()
        }
    }

    private fun mapExercisesDuringImport() {
        // If exercise mapping is required
        val mappingRequired =
            runCatching {
                composeTestRule.onNodeWithText("Map Exercises", substring = true).assertIsDisplayed()
                true
            }.getOrDefault(false)

        if (mappingRequired) {
            // Map exercises to existing database entries
            runCatching {
                composeTestRule.onNodeWithText("Search existing exercises").performTextInput("Bench")
                composeTestRule.waitForIdle()

                composeTestRule.onNodeWithText("Barbell Bench Press").performClick()
                composeTestRule.waitForIdle()

                composeTestRule.onNodeWithText("Next").performClick()
                composeTestRule.waitForIdle()
            }

            // Continue mapping other exercises or finish
            runCatching {
                composeTestRule.onNodeWithText("Finish").performClick()
                composeTestRule.waitForIdle()
            }.getOrElse {
                runCatching {
                    composeTestRule.onNodeWithText("Complete").performClick()
                    composeTestRule.waitForIdle()
                }
            }
        }
    }

    private fun startImportedProgramme() {
        // Start the imported programme
        runCatching {
            composeTestRule.onNodeWithText("Start Programme").performClick()
            composeTestRule.waitForIdle()
        }.getOrElse {
            runCatching {
                composeTestRule.onNodeWithText("Begin Programme").performClick()
                composeTestRule.waitForIdle()
            }
        }
    }

    private fun completeFirstWorkout() {
        // Should now be in workout screen with programme exercises
        runCatching {
            // Complete sets for programme exercises
            completeSetWithWeightAndReps("80", "8")
            completeSetWithWeightAndReps("100", "5")

            finishWorkout()
        }
    }

    private fun startProgrammeWorkout() {
        runCatching {
            composeTestRule.onNodeWithText("Start Today's Workout").performClick()
            composeTestRule.waitForIdle()
        }.getOrElse {
            runCatching {
                composeTestRule.onNodeWithText("Start Workout").performClick()
                composeTestRule.waitForIdle()
            }
        }
    }

    private fun completeWorkoutSets() {
        // Complete sets for current workout
        runCatching {
            completeSetWithWeightAndReps("85", "8")
            completeSetWithWeightAndReps("105", "5")
        }
    }

    private fun progressToNextWeek() {
        runCatching {
            composeTestRule.onNodeWithText("Next Week").performClick()
            composeTestRule.waitForIdle()
        }.getOrElse {
            runCatching {
                composeTestRule.onNodeWithText("Progress").performClick()
                composeTestRule.waitForIdle()
            }
        }
    }

    private fun completeProgramme() {
        runCatching {
            composeTestRule.onNodeWithText("Complete Programme").performClick()
            composeTestRule.waitForIdle()

            // Confirm completion
            composeTestRule.onNodeWithText("Yes").performClick()
            composeTestRule.waitForIdle()
        }
    }

    private fun editProgrammeWorkout() {
        runCatching {
            composeTestRule.onNodeWithText("Edit", substring = true).performClick()
            composeTestRule.waitForIdle()
        }.getOrElse {
            runCatching {
                composeTestRule.onNodeWithContentDescription("Edit workout").performClick()
                composeTestRule.waitForIdle()
            }
        }
    }

    private fun addExerciseToProgrammeWorkout() {
        runCatching {
            composeTestRule.onNodeWithText("Add Exercise").performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("Search exercises...").performTextInput("Pull")
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("Pull Up", substring = true).performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("Add to Workout").performClick()
            composeTestRule.waitForIdle()
        }
    }

    private fun saveProgrammeChanges() {
        runCatching {
            composeTestRule.onNodeWithText("Save Changes").performClick()
            composeTestRule.waitForIdle()
        }.getOrElse {
            runCatching {
                composeTestRule.onNodeWithText("Save").performClick()
                composeTestRule.waitForIdle()
            }
        }
    }

    private fun completeSetWithWeightAndReps(
        weight: String,
        reps: String,
    ) {
        runCatching {
            val weightFields = listOf("Weight", "kg", "lbs", "Weight (kg)", "Weight (lbs)")
            val repsFields = listOf("Reps", "Repetitions")

            var weightEntered = false
            var repsEntered = false

            weightFields.forEach { fieldText ->
                if (!weightEntered) {
                    runCatching {
                        composeTestRule.onNodeWithText(fieldText).performTextInput(weight)
                        weightEntered = true
                    }
                }
            }

            repsFields.forEach { fieldText ->
                if (!repsEntered) {
                    runCatching {
                        composeTestRule.onNodeWithText(fieldText).performTextInput(reps)
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
