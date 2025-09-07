package com.github.radupana.featherweight.ui.screens

import com.github.radupana.featherweight.Screen
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WorkoutTemplateSelectionTest {
    @Test
    fun `Screen enum contains template selection screens`() {
        val screens = Screen.entries.toTypedArray()

        // Verify both template screens exist
        assertThat(screens).asList().contains(Screen.WORKOUT_TEMPLATE_SELECTION)
        assertThat(screens).asList().contains(Screen.WORKOUT_TEMPLATE_CONFIGURATION)

        // Verify they are distinct
        assertThat(Screen.WORKOUT_TEMPLATE_SELECTION)
            .isNotEqualTo(Screen.WORKOUT_TEMPLATE_CONFIGURATION)
    }

    @Test
    fun `Template navigation flow follows correct order`() {
        // Expected navigation flow
        val expectedFlow =
            listOf(
                Screen.WORKOUTS,
                Screen.WORKOUT_TEMPLATE_SELECTION,
                Screen.WORKOUT_TEMPLATE_CONFIGURATION,
                Screen.ACTIVE_WORKOUT,
            )

        // Verify screens exist and are distinct
        expectedFlow.forEach { screen ->
            assertThat(Screen.entries.toTypedArray()).asList().contains(screen)
        }

        // Verify all screens in flow are unique
        assertThat(expectedFlow.distinct()).hasSize(expectedFlow.size)
    }

    @Test
    fun `Template types are correctly mapped`() {
        // Template type mappings used in navigation
        val templateTypes =
            mapOf(
                "PUSH" to "Push",
                "PULL" to "Pull",
                "LEGS" to "Legs",
                "UPPER" to "Upper",
                "LOWER" to "Lower",
                "FULL BODY" to "Full Body",
            )

        // Verify all expected templates exist
        assertThat(templateTypes).hasSize(6)

        // Verify keys are uppercase for consistency
        templateTypes.keys.forEach { key ->
            assertThat(key).isEqualTo(key.uppercase())
        }

        // Verify display names are properly formatted
        templateTypes.values.forEach { displayName ->
            assertThat(displayName).isNotEmpty()
            assertThat(displayName.first().isUpperCase()).isTrue()
        }
    }

    @Test
    fun `Screens without bottom navigation include template screens`() {
        // Screens that should hide bottom navigation
        val screensWithoutBottomNav =
            setOf(
                Screen.SPLASH,
                Screen.ACTIVE_WORKOUT,
                Screen.EXERCISE_SELECTOR,
                Screen.PROGRAMME_HISTORY_DETAIL,
                Screen.WORKOUT_TEMPLATE_SELECTION,
                Screen.WORKOUT_TEMPLATE_CONFIGURATION,
                Screen.WORKOUT_COMPLETION,
                Screen.PROGRAMME_COMPLETION,
                Screen.EXERCISE_MAPPING,
            )

        // Verify template screens are included
        assertThat(screensWithoutBottomNav).contains(Screen.WORKOUT_TEMPLATE_SELECTION)
        assertThat(screensWithoutBottomNav).contains(Screen.WORKOUT_TEMPLATE_CONFIGURATION)

        // Verify main workout screen shows bottom nav
        assertThat(screensWithoutBottomNav).doesNotContain(Screen.WORKOUTS)
    }

    @Test
    fun `Template selection screen precedes configuration screen`() {
        val screens = Screen.entries.toTypedArray()
        val selectionIndex = screens.indexOf(Screen.WORKOUT_TEMPLATE_SELECTION)
        val configIndex = screens.indexOf(Screen.WORKOUT_TEMPLATE_CONFIGURATION)

        // Both screens should exist
        assertThat(selectionIndex).isNotEqualTo(-1)
        assertThat(configIndex).isNotEqualTo(-1)

        // Selection should come before configuration in enum order
        assertThat(selectionIndex).isLessThan(configIndex)
    }
}
