package com.github.radupana.featherweight.ui.screens

import com.github.radupana.featherweight.Screen
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WorkoutTemplateSelectionTest {
    @Test
    fun `Screen enum contains template selection screen`() {
        val screens = Screen.entries.toTypedArray()

        assertThat(screens).asList().contains(Screen.WORKOUT_TEMPLATE_SELECTION)
    }

    @Test
    fun `Template navigation flow follows correct order`() {
        val expectedFlow =
            listOf(
                Screen.WORKOUTS,
                Screen.WORKOUT_TEMPLATE_SELECTION,
            )

        expectedFlow.forEach { screen ->
            assertThat(Screen.entries.toTypedArray()).asList().contains(screen)
        }

        assertThat(expectedFlow.distinct()).hasSize(expectedFlow.size)
    }

    @Test
    fun `Screens without bottom navigation include template selection screen`() {
        val screensWithoutBottomNav =
            setOf(
                Screen.SPLASH,
                Screen.ACTIVE_WORKOUT,
                Screen.EXERCISE_SELECTOR,
                Screen.PROGRAMME_HISTORY_DETAIL,
                Screen.WORKOUT_TEMPLATE_SELECTION,
                Screen.WORKOUT_COMPLETION,
                Screen.PROGRAMME_COMPLETION,
                Screen.EXERCISE_MAPPING,
            )

        assertThat(screensWithoutBottomNav).contains(Screen.WORKOUT_TEMPLATE_SELECTION)
        assertThat(screensWithoutBottomNav).doesNotContain(Screen.WORKOUTS)
    }
}
