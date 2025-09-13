package com.github.radupana.featherweight.viewmodel

import com.github.radupana.featherweight.model.WeightUnit
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ProfileViewModelTest {
    @Test
    fun `ProfileUiState has correct default values`() {
        val state = ProfileUiState()

        assertThat(state.isLoading).isFalse()
        assertThat(state.currentMaxes).isEmpty()
        assertThat(state.big4Exercises).isEmpty()
        assertThat(state.otherExercises).isEmpty()
        assertThat(state.error).isNull()
        assertThat(state.successMessage).isNull()
        assertThat(state.isOneRMSectionExpanded).isTrue()
        assertThat(state.isBig4SubSectionExpanded).isTrue()
        assertThat(state.isOtherSubSectionExpanded).isTrue()
        assertThat(state.isDataManagementSectionExpanded).isTrue()
        assertThat(state.seedingState).isEqualTo(SeedingState.Idle)
        assertThat(state.seedingWeeks).isEqualTo(12)
        assertThat(state.isExporting).isFalse()
        assertThat(state.exportedFilePath).isNull()
        assertThat(state.currentTab).isEqualTo(ProfileTab.ONE_RM)
        assertThat(state.currentWeightUnit).isEqualTo(WeightUnit.KG)
    }

    @Test
    fun `ProfileUiState copy function works correctly with tab`() {
        val original =
            ProfileUiState(
                isLoading = true,
                currentTab = ProfileTab.SETTINGS,
                seedingWeeks = 10,
            )

        val modified =
            original.copy(
                isLoading = false,
                currentTab = ProfileTab.DATA,
            )

        assertThat(modified.isLoading).isFalse()
        assertThat(modified.currentTab).isEqualTo(ProfileTab.DATA)
        assertThat(modified.seedingWeeks).isEqualTo(10)
    }

    @Test
    fun `ProfileUiState correctly updates weight unit`() {
        val original = ProfileUiState(currentWeightUnit = WeightUnit.KG)

        val modified = original.copy(currentWeightUnit = WeightUnit.LBS)

        assertThat(original.currentWeightUnit).isEqualTo(WeightUnit.KG)
        assertThat(modified.currentWeightUnit).isEqualTo(WeightUnit.LBS)
    }

    @Test
    fun `ProfileTab enum should have correct order`() {
        assertThat(ProfileTab.ONE_RM.ordinal).isEqualTo(0)
        assertThat(ProfileTab.SETTINGS.ordinal).isEqualTo(1)
        assertThat(ProfileTab.DATA.ordinal).isEqualTo(2)
        assertThat(ProfileTab.DEVELOPER.ordinal).isEqualTo(3)
    }

    @Test
    fun `ProfileTab enum should have all expected values`() {
        val tabs = ProfileTab.values()
        assertThat(tabs).hasLength(4)
        assertThat(tabs).asList().containsExactly(
            ProfileTab.ONE_RM,
            ProfileTab.SETTINGS,
            ProfileTab.DATA,
            ProfileTab.DEVELOPER,
        )
    }

    @Test
    fun `ProfileTab enum values are in expected order`() {
        val tabs = ProfileTab.values()
        assertThat(tabs[0]).isEqualTo(ProfileTab.ONE_RM)
        assertThat(tabs[1]).isEqualTo(ProfileTab.SETTINGS)
        assertThat(tabs[2]).isEqualTo(ProfileTab.DATA)
        assertThat(tabs[3]).isEqualTo(ProfileTab.DEVELOPER)
    }

    @Test
    fun `SeedingState sealed class has expected types`() {
        val idle = SeedingState.Idle
        val inProgress = SeedingState.InProgress
        val success = SeedingState.Success(workoutsCreated = 10)
        val error = SeedingState.Error(message = "Test error")

        assertThat(idle).isInstanceOf(SeedingState::class.java)
        assertThat(inProgress).isInstanceOf(SeedingState::class.java)
        assertThat(success).isInstanceOf(SeedingState.Success::class.java)
        assertThat(success.workoutsCreated).isEqualTo(10)
        assertThat(error).isInstanceOf(SeedingState.Error::class.java)
        assertThat(error.message).isEqualTo("Test error")
    }

    @Test
    fun `ProfileUiState can update multiple tab-related fields`() {
        val state = ProfileUiState()

        val updated =
            state.copy(
                currentTab = ProfileTab.DEVELOPER,
                seedingWeeks = 24,
                seedingState = SeedingState.InProgress,
            )

        assertThat(updated.currentTab).isEqualTo(ProfileTab.DEVELOPER)
        assertThat(updated.seedingWeeks).isEqualTo(24)
        assertThat(updated.seedingState).isEqualTo(SeedingState.InProgress)
    }
}
