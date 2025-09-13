package com.github.radupana.featherweight.util

import com.github.radupana.featherweight.manager.WeightUnitManager
import com.github.radupana.featherweight.manager.WeightUnitManagerImpl
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import org.junit.Before
import org.junit.Test

class WeightFormatterBackwardCompatibilityTest {
    @Before
    fun setUp() {
        // Reset WeightFormatter to test both initialized and uninitialized states
        val field = WeightFormatter::class.java.getDeclaredField("weightUnitManager")
        field.isAccessible = true
        field.set(WeightFormatter, null)
    }

    @Test
    fun `formatWeight works without manager (backward compatibility)`() {
        // Test without manager - should use original logic
        assertThat(WeightFormatter.formatWeight(100.0f)).isEqualTo("100")
        assertThat(WeightFormatter.formatWeight(82.5f)).isEqualTo("82.5")
        assertThat(WeightFormatter.formatWeight(100.25f)).isEqualTo("100.25")
        assertThat(WeightFormatter.formatWeight(100.75f)).isEqualTo("100.75")
    }

    @Test
    fun `formatWeightWithUnit works without manager (backward compatibility)`() {
        // Test without manager - should use original logic
        assertThat(WeightFormatter.formatWeightWithUnit(100.0f)).isEqualTo("100kg")
        assertThat(WeightFormatter.formatWeightWithUnit(82.5f)).isEqualTo("82.5kg")
        assertThat(WeightFormatter.formatWeightWithUnit(0.0f)).isEqualTo("0kg")
    }

    @Test
    fun `formatVolume works without manager (backward compatibility)`() {
        // Test without manager - should use original logic
        assertThat(WeightFormatter.formatVolume(0f)).isEqualTo("0kg")
        assertThat(WeightFormatter.formatVolume(100f)).isEqualTo("100kg")
        assertThat(WeightFormatter.formatVolume(500f)).isEqualTo("500kg")
        assertThat(WeightFormatter.formatVolume(999f)).isEqualTo("999kg")
        assertThat(WeightFormatter.formatVolume(1000f)).isEqualTo("1k kg")
        assertThat(WeightFormatter.formatVolume(1500f)).isEqualTo("1.5k kg")
        assertThat(WeightFormatter.formatVolume(10000f)).isEqualTo("10k kg")
        assertThat(WeightFormatter.formatVolume(15500f)).isEqualTo("15.5k kg")
    }

    @Test
    fun `formatWeight uses manager when initialized`() {
        // Initialize with a mock manager
        val mockManager = mockk<WeightUnitManager>(relaxed = true)
        WeightFormatter.initialize(mockManager)

        // Verify that the manager is used
        io.mockk.every { mockManager.formatWeight(100.0f) } returns "100"
        assertThat(WeightFormatter.formatWeight(100.0f)).isEqualTo("100")
        io.mockk.verify { mockManager.formatWeight(100.0f) }
    }

    @Test
    fun `formatWeightWithUnit uses manager when initialized`() {
        // Initialize with a mock manager
        val mockManager = mockk<WeightUnitManager>(relaxed = true)
        WeightFormatter.initialize(mockManager)

        // Verify that the manager is used
        io.mockk.every { mockManager.formatWeightWithUnit(100.0f) } returns "100kg"
        assertThat(WeightFormatter.formatWeightWithUnit(100.0f)).isEqualTo("100kg")
        io.mockk.verify { mockManager.formatWeightWithUnit(100.0f) }
    }

    @Test
    fun `formatVolume uses manager when initialized`() {
        // Initialize with a mock manager
        val mockManager = mockk<WeightUnitManager>(relaxed = true)
        WeightFormatter.initialize(mockManager)

        // Verify that the manager is used
        io.mockk.every { mockManager.formatVolume(1000f) } returns "1k kg"
        assertThat(WeightFormatter.formatVolume(1000f)).isEqualTo("1k kg")
        io.mockk.verify { mockManager.formatVolume(1000f) }
    }

    @Test
    fun `other formatting methods work unchanged`() {
        // These methods should not be affected by the manager
        assertThat(WeightFormatter.formatRPE(8.5f)).isEqualTo("8.5")
        assertThat(WeightFormatter.formatRPE(8.0f)).isEqualTo("8")
        assertThat(WeightFormatter.formatPercentage(0.75f)).isEqualTo("75%")
        assertThat(WeightFormatter.roundToNearestQuarter(100.1f)).isEqualTo(100.0f)
        assertThat(WeightFormatter.roundUpToNearestQuarter(100.1f)).isEqualTo(100.25f)
        assertThat(WeightFormatter.isValidWeight(100.25f)).isTrue()
        assertThat(WeightFormatter.isValidRPE(8.5f)).isTrue()
    }

    @Test
    fun `integration with real WeightUnitManager`() {
        // Test with real implementation
        val context = mockk<android.content.Context>(relaxed = true)
        val sharedPrefs = mockk<android.content.SharedPreferences>(relaxed = true)
        val editor = mockk<android.content.SharedPreferences.Editor>(relaxed = true)

        io.mockk.every { context.getSharedPreferences(any(), any()) } returns sharedPrefs
        io.mockk.every { sharedPrefs.edit() } returns editor
        io.mockk.every { editor.putString(any(), any()) } returns editor

        val realManager = WeightUnitManagerImpl(context)
        WeightFormatter.initialize(realManager)

        // Should work with real manager (currently in KG mode)
        assertThat(WeightFormatter.formatWeight(100.0f)).isEqualTo("100")
        assertThat(WeightFormatter.formatWeightWithUnit(100.0f)).isEqualTo("100kg")
        assertThat(WeightFormatter.formatVolume(1000f)).isEqualTo("1k kg")
    }

    @Test
    fun `new display methods work without manager (backward compatibility)`() {
        // Reset to no manager
        val field = WeightFormatter::class.java.getDeclaredField("weightUnitManager")
        field.isAccessible = true
        field.set(WeightFormatter, null)

        // Test new methods with fallback behavior
        assertThat(WeightFormatter.getWeightLabel()).isEqualTo("Weight (kg)")
        assertThat(WeightFormatter.formatLastSet(3, 110f)).isEqualTo("Last: 3x110kg")
        assertThat(WeightFormatter.formatSetInfo(3, 5, 100f)).isEqualTo("3 sets × 5 reps × 100kg")
    }

    @Test
    fun `new display methods work with manager in KG mode`() {
        val mockManager = mockk<WeightUnitManager>(relaxed = true)
        WeightFormatter.initialize(mockManager)

        io.mockk.every { mockManager.getCurrentUnit() } returns com.github.radupana.featherweight.model.WeightUnit.KG
        io.mockk.every { mockManager.formatWeight(100f) } returns "100"

        assertThat(WeightFormatter.getWeightLabel()).isEqualTo("Weight (kg)")
        assertThat(WeightFormatter.formatLastSet(3, 100f)).isEqualTo("Last: 3x100kg")
        assertThat(WeightFormatter.formatSetInfo(3, 5, 100f)).isEqualTo("3 sets × 5 reps × 100kg")
    }

    @Test
    fun `new display methods work with manager in LBS mode`() {
        val mockManager = mockk<WeightUnitManager>(relaxed = true)
        WeightFormatter.initialize(mockManager)

        io.mockk.every { mockManager.getCurrentUnit() } returns com.github.radupana.featherweight.model.WeightUnit.LBS
        io.mockk.every { mockManager.formatWeight(100f) } returns "220.5"

        assertThat(WeightFormatter.getWeightLabel()).isEqualTo("Weight (lbs)")
        assertThat(WeightFormatter.formatLastSet(3, 100f)).isEqualTo("Last: 3x220.5lbs")
        assertThat(WeightFormatter.formatSetInfo(3, 5, 100f)).isEqualTo("3 sets × 5 reps × 220.5lbs")
    }

    @Test
    fun `parseUserInput works without manager (backward compatibility)`() {
        val field = WeightFormatter::class.java.getDeclaredField("weightUnitManager")
        field.isAccessible = true
        field.set(WeightFormatter, null)

        // Without manager, should treat as kg and round to quarter
        assertThat(WeightFormatter.parseUserInput("100")).isEqualTo(100f)
        assertThat(WeightFormatter.parseUserInput("100.1")).isEqualTo(100f)
        assertThat(WeightFormatter.parseUserInput("100.3")).isEqualTo(100.25f)
    }

    @Test
    fun `parseUserInput converts from LBS to KG when in LBS mode`() {
        val mockManager = mockk<WeightUnitManager>(relaxed = true)
        WeightFormatter.initialize(mockManager)

        // In LBS mode, "225" should be converted to kg (225 * 0.453592 = ~102kg)
        io.mockk.every { mockManager.parseUserInput("225") } returns 102f

        assertThat(WeightFormatter.parseUserInput("225")).isEqualTo(102f)
        io.mockk.verify { mockManager.parseUserInput("225") }
    }

    @Test
    fun `parseUserInput keeps KG values when in KG mode`() {
        val mockManager = mockk<WeightUnitManager>(relaxed = true)
        WeightFormatter.initialize(mockManager)

        // In KG mode, "100" should stay as 100kg
        io.mockk.every { mockManager.parseUserInput("100") } returns 100f

        assertThat(WeightFormatter.parseUserInput("100")).isEqualTo(100f)
        io.mockk.verify { mockManager.parseUserInput("100") }
    }
}
