package com.github.radupana.featherweight.manager

import android.content.Context
import android.content.SharedPreferences
import com.github.radupana.featherweight.model.WeightUnit
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

class WeightUnitManagerLbsTest {
    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var manager: WeightUnitManager

    private companion object {
        private const val EPSILON = 0.01f
    }

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        sharedPreferences = mockk(relaxed = true)
        editor = mockk(relaxed = true)

        every { context.getSharedPreferences("weight_prefs", Context.MODE_PRIVATE) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { sharedPreferences.getString("weight_unit", any()) } returns WeightUnit.KG.name

        manager = WeightUnitManagerImpl(context)
    }

    @Test
    fun `kg to lbs conversion is accurate`() {
        manager.setUnit(WeightUnit.LBS)

        assertThat(manager.convertFromKg(100f)).isWithin(EPSILON).of(220.462f)
        assertThat(manager.convertFromKg(50f)).isWithin(EPSILON).of(110.231f)
        assertThat(manager.convertFromKg(75f)).isWithin(EPSILON).of(165.347f)
        assertThat(manager.convertFromKg(0f)).isEqualTo(0f)
    }

    @Test
    fun `lbs to kg conversion is accurate`() {
        manager.setUnit(WeightUnit.LBS)

        assertThat(manager.convertToKg(220f)).isWithin(EPSILON).of(99.79f)
        assertThat(manager.convertToKg(100f)).isWithin(EPSILON).of(45.359f)
        assertThat(manager.convertToKg(165f)).isWithin(EPSILON).of(74.843f)
        assertThat(manager.convertToKg(0f)).isEqualTo(0f)
    }

    @Test
    fun `round trip kg-lbs-kg preserves precision within tolerance`() {
        manager.setUnit(WeightUnit.LBS)

        val testWeights = listOf(0f, 20f, 50f, 75.5f, 100f, 125.25f, 200f)

        for (originalKg in testWeights) {
            val lbs = manager.convertFromKg(originalKg)
            val backToKg = manager.convertToKg(lbs)
            val rounded = manager.roundToStoragePrecision(backToKg)

            // After rounding to storage precision, should be very close
            assertThat(abs(rounded - originalKg)).isLessThan(0.25f)
        }
    }

    @Test
    fun `formatWeight displays lbs correctly when in LBS mode`() {
        manager.setUnit(WeightUnit.LBS)

        assertThat(manager.formatWeight(45.359f)).isEqualTo("100") // 100 lbs
        assertThat(manager.formatWeight(100f)).isEqualTo("220.5") // 220.46 lbs rounds to 220.5
        assertThat(manager.formatWeight(50f)).isEqualTo("110") // 110.23 lbs rounds to 110
    }

    @Test
    fun `formatWeightWithUnit includes lbs suffix in LBS mode`() {
        manager.setUnit(WeightUnit.LBS)

        assertThat(manager.formatWeightWithUnit(45.359f)).isEqualTo("100lbs")
        assertThat(manager.formatWeightWithUnit(100f)).isEqualTo("220.5lbs")
    }

    @Test
    fun `formatVolume converts to lbs for display`() {
        manager.setUnit(WeightUnit.LBS)

        // 1000kg = 2204.62 lbs
        assertThat(manager.formatVolume(1000f)).isEqualTo("2.2k lbs")

        // 5000kg = 11023.1 lbs
        assertThat(manager.formatVolume(5000f)).isEqualTo("11k lbs")

        // Small volume
        assertThat(manager.formatVolume(50f)).isEqualTo("110lbs")
    }

    @Test
    fun `parseUserInput converts lbs input to kg when in LBS mode`() {
        manager.setUnit(WeightUnit.LBS)

        // User enters 225 (assuming lbs since in LBS mode)
        val result = manager.parseUserInput("225")
        // 225 lbs = 102.0582 kg, rounds to 102.0 kg (nearest 0.25)
        assertThat(result).isEqualTo(102.0f)

        // Verify it's rounded to storage precision
        assertThat(result % 0.25f).isLessThan(EPSILON)
    }

    @Test
    fun `parseUserInput handles explicit unit suffixes`() {
        manager.setUnit(WeightUnit.LBS)

        // Even in LBS mode, can parse kg explicitly
        assertThat(manager.parseUserInput("100kg")).isEqualTo(100f)
        assertThat(manager.parseUserInput("100 kg")).isEqualTo(100f)
        // 100 lbs = 45.3592 kg, rounds to 45.25 kg (nearest 0.25)
        assertThat(manager.parseUserInput("100lbs")).isEqualTo(45.25f)
        assertThat(manager.parseUserInput("100 lbs")).isEqualTo(45.25f)
    }

    @Test
    fun `getExportWeight returns values in current unit`() {
        manager.setUnit(WeightUnit.LBS)

        val result = manager.getExportWeight(100f) // 100kg
        assertThat(result.first).isWithin(EPSILON).of(220.462f) // Weight in lbs
        assertThat(result.second).isEqualTo("lbs") // Unit suffix
    }

    @Test
    fun `parseImportWeight correctly handles lbs values`() {
        // Import 225 lbs
        val result = manager.parseImportWeight(225f, "lbs")
        // 225 lbs = 102.0582 kg, rounds to 102.0 kg
        assertThat(result).isEqualTo(102.0f)

        // Import 185 lbs
        val result2 = manager.parseImportWeight(185f, "lbs")
        // 185 lbs = 83.9145 kg, rounds to 84.0 kg (nearest 0.25kg)
        assertThat(result2).isEqualTo(84.0f)
    }

    @Test
    fun `display precision uses 0_5 lb increments in LBS mode`() {
        manager.setUnit(WeightUnit.LBS)

        // Test rounding to 0.5 lb increments
        assertThat(manager.roundToDisplayPrecision(100.1f, WeightUnit.LBS)).isEqualTo(100.0f)
        assertThat(manager.roundToDisplayPrecision(100.2f, WeightUnit.LBS)).isEqualTo(100.0f)
        assertThat(manager.roundToDisplayPrecision(100.3f, WeightUnit.LBS)).isEqualTo(100.5f)
        assertThat(manager.roundToDisplayPrecision(100.7f, WeightUnit.LBS)).isEqualTo(100.5f)
        assertThat(manager.roundToDisplayPrecision(100.8f, WeightUnit.LBS)).isEqualTo(101.0f)
    }

    @Test
    fun `switching units does not affect stored kg values`() {
        // Start in KG
        manager.setUnit(WeightUnit.KG)
        val storedKg = manager.parseUserInput("100")
        assertThat(storedKg).isEqualTo(100f)

        // Switch to LBS
        manager.setUnit(WeightUnit.LBS)

        // The stored value should still be 100kg
        // But display should show it as lbs
        assertThat(manager.formatWeight(storedKg)).isEqualTo("220.5")

        // Switch back to KG
        manager.setUnit(WeightUnit.KG)
        assertThat(manager.formatWeight(storedKg)).isEqualTo("100")
    }

    @Test
    fun `preference persistence for LBS mode`() {
        manager.setUnit(WeightUnit.LBS)

        verify { editor.putString("weight_unit", "LBS") }
        verify { editor.apply() }
    }

    @Test
    fun `loads saved LBS preference on init`() {
        // Setup to return LBS from preferences
        every { sharedPreferences.getString("weight_unit", any()) } returns WeightUnit.LBS.name

        val newManager = WeightUnitManagerImpl(context)
        assertThat(newManager.getCurrentUnit()).isEqualTo(WeightUnit.LBS)
    }

    @Test
    fun `handles invalid saved preference gracefully`() {
        // Setup to return invalid value from preferences
        every { sharedPreferences.getString("weight_unit", any()) } returns "INVALID"

        val newManager = WeightUnitManagerImpl(context)
        assertThat(newManager.getCurrentUnit()).isEqualTo(WeightUnit.KG) // Defaults to KG
    }

    @Test
    fun `common weightlifting weights convert correctly`() {
        manager.setUnit(WeightUnit.LBS)

        // Common barbell weights
        val testCases =
            mapOf(
                20f to 44.09f, // 20kg = 44 lbs (empty barbell)
                60f to 132.28f, // 60kg = 132 lbs
                80f to 176.37f, // 80kg = 176 lbs
                100f to 220.46f, // 100kg = 220 lbs
                140f to 308.65f, // 140kg = 308 lbs
                180f to 396.83f, // 180kg = 396 lbs
                220f to 485.02f, // 220kg = 485 lbs
            )

        for ((kg, expectedLbs) in testCases) {
            val actualLbs = manager.convertFromKg(kg)
            assertThat(actualLbs).isWithin(0.1f).of(expectedLbs)
        }
    }

    @Test
    fun `common lbs plates convert to kg correctly`() {
        manager.setUnit(WeightUnit.LBS)

        val testCases =
            mapOf(
                45f to 20.41f, // 45 lbs plate
                35f to 15.88f, // 35 lbs plate
                25f to 11.34f, // 25 lbs plate
                10f to 4.54f, // 10 lbs plate
                5f to 2.27f, // 5 lbs plate
                2.5f to 1.13f, // 2.5 lbs plate
            )

        for ((lbs, expectedKg) in testCases) {
            val actualKg = manager.convertToKg(lbs)
            assertThat(actualKg).isWithin(0.01f).of(expectedKg)
        }
    }
}
