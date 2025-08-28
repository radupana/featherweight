package com.github.radupana.featherweight.viewmodel

import android.app.Application
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import org.junit.Before
import org.junit.Test

class WorkoutViewModelTest {
    private lateinit var viewModel: WorkoutViewModel
    private lateinit var mockApplication: Application

    @Before
    fun setup() {
        mockApplication = mockk(relaxed = true)
        viewModel = WorkoutViewModel(mockApplication)
    }

    @Test
    fun `lastPerformance_initialState_isEmpty`() {
        // Assert - initial state should be empty
        assertThat(viewModel.lastPerformance.value).isEmpty()
    }

    @Test
    fun `lastPerformance_isStateFlow_exposed`() {
        // Assert - verify it's properly exposed as StateFlow
        assertThat(viewModel.lastPerformance).isNotNull()
        assertThat(viewModel.lastPerformance.value).isNotNull()
    }

    @Test
    fun `lastPerformance_stateFlow_returnsEmptyMapInitially`() {
        // Arrange
        val lastPerformanceMap = viewModel.lastPerformance.value

        // Assert
        assertThat(lastPerformanceMap).isInstanceOf(Map::class.java)
        assertThat(lastPerformanceMap).isEmpty()
    }
}
