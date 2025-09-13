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

class WeightUnitManagerTest {
    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var manager: WeightUnitManager

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        sharedPreferences = mockk(relaxed = true)
        editor = mockk(relaxed = true)

        every { context.getSharedPreferences("weight_prefs", Context.MODE_PRIVATE) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor

        manager = WeightUnitManagerImpl(context)
    }

    @Test
    fun `initial unit is KG`() {
        assertThat(manager.getCurrentUnit()).isEqualTo(WeightUnit.KG)
    }

    @Test
    fun `setUnit persists to preferences`() {
        manager.setUnit(WeightUnit.LBS)

        verify { editor.putString("weight_unit", "LBS") }
        verify { editor.apply() }
    }

    @Test
    fun `kg to kg returns same value`() {
        val weight = 100f
        assertThat(manager.convertFromKg(weight)).isEqualTo(weight)
    }

    @Test
    fun `kg to kg conversion round trip`() {
        val weight = 82.5f
        val converted = manager.convertFromKg(weight)
        val backToKg = manager.convertToKg(converted)
        assertThat(backToKg).isEqualTo(weight)
    }

    @Test
    fun `storage precision rounds to quarter kg`() {
        assertThat(manager.roundToStoragePrecision(100.0f)).isEqualTo(100.0f)
        assertThat(manager.roundToStoragePrecision(100.1f)).isEqualTo(100.0f)
        assertThat(manager.roundToStoragePrecision(100.2f)).isEqualTo(100.25f)
        assertThat(manager.roundToStoragePrecision(100.3f)).isEqualTo(100.25f)
        assertThat(manager.roundToStoragePrecision(100.4f)).isEqualTo(100.5f)
        assertThat(manager.roundToStoragePrecision(100.6f)).isEqualTo(100.5f)
        assertThat(manager.roundToStoragePrecision(100.7f)).isEqualTo(100.75f)
        assertThat(manager.roundToStoragePrecision(100.8f)).isEqualTo(100.75f)
        assertThat(manager.roundToStoragePrecision(100.9f)).isEqualTo(101.0f)
    }

    @Test
    fun `display precision rounds to unit increment`() {
        assertThat(manager.roundToDisplayPrecision(100.1f, WeightUnit.KG)).isEqualTo(100.0f)
        assertThat(manager.roundToDisplayPrecision(100.2f, WeightUnit.KG)).isEqualTo(100.25f)
        assertThat(manager.roundToDisplayPrecision(100.3f, WeightUnit.KG)).isEqualTo(100.25f)
        assertThat(manager.roundToDisplayPrecision(100.4f, WeightUnit.KG)).isEqualTo(100.5f)

        assertThat(manager.roundToDisplayPrecision(100.2f, WeightUnit.LBS)).isEqualTo(100.0f)
        assertThat(manager.roundToDisplayPrecision(100.3f, WeightUnit.LBS)).isEqualTo(100.5f)
        assertThat(manager.roundToDisplayPrecision(100.7f, WeightUnit.LBS)).isEqualTo(100.5f)
        assertThat(manager.roundToDisplayPrecision(100.8f, WeightUnit.LBS)).isEqualTo(101.0f)
    }

    @Test
    fun `formatWeight formats integer weights without decimal`() {
        assertThat(manager.formatWeight(100.0f)).isEqualTo("100")
        assertThat(manager.formatWeight(0.0f)).isEqualTo("0")
        assertThat(manager.formatWeight(50.0f)).isEqualTo("50")
    }

    @Test
    fun `formatWeight formats decimal weights correctly`() {
        assertThat(manager.formatWeight(100.25f)).isEqualTo("100.25")
        assertThat(manager.formatWeight(100.5f)).isEqualTo("100.5")
        assertThat(manager.formatWeight(100.75f)).isEqualTo("100.75")
    }

    @Test
    fun `formatWeightWithUnit includes kg suffix`() {
        assertThat(manager.formatWeightWithUnit(100.0f)).isEqualTo("100kg")
        assertThat(manager.formatWeightWithUnit(82.5f)).isEqualTo("82.5kg")
        assertThat(manager.formatWeightWithUnit(0.0f)).isEqualTo("0kg")
    }

    @Test
    fun `formatVolume handles small volumes`() {
        assertThat(manager.formatVolume(0f)).isEqualTo("0kg")
        assertThat(manager.formatVolume(100f)).isEqualTo("100kg")
        assertThat(manager.formatVolume(500f)).isEqualTo("500kg")
        assertThat(manager.formatVolume(999f)).isEqualTo("999kg")
    }

    @Test
    fun `formatVolume handles medium volumes with k suffix`() {
        assertThat(manager.formatVolume(1000f)).isEqualTo("1k kg")
        assertThat(manager.formatVolume(1500f)).isEqualTo("1.5k kg")
        assertThat(manager.formatVolume(2345f)).isEqualTo("2.35k kg")
        assertThat(manager.formatVolume(9999f)).isEqualTo("10k kg")
    }

    @Test
    fun `formatVolume handles large volumes`() {
        assertThat(manager.formatVolume(10000f)).isEqualTo("10k kg")
        assertThat(manager.formatVolume(15000f)).isEqualTo("15k kg")
        assertThat(manager.formatVolume(15500f)).isEqualTo("15.5k kg")
        assertThat(manager.formatVolume(99999f)).isEqualTo("100k kg")
    }

    @Test
    fun `parseUserInput handles plain numbers`() {
        assertThat(manager.parseUserInput("100")).isEqualTo(100f)
        assertThat(manager.parseUserInput("82.5")).isEqualTo(82.5f)
        assertThat(manager.parseUserInput("0")).isEqualTo(0f)
    }

    @Test
    fun `parseUserInput handles numbers with kg suffix`() {
        assertThat(manager.parseUserInput("100kg")).isEqualTo(100f)
        assertThat(manager.parseUserInput("100 kg")).isEqualTo(100f)
        assertThat(manager.parseUserInput("100KG")).isEqualTo(100f)
        assertThat(manager.parseUserInput("82.5kg")).isEqualTo(82.5f)
    }

    @Test
    fun `parseUserInput handles numbers with lbs suffix in KG mode`() {
        // When user explicitly types "lbs", convert from lbs to kg
        // 100 lbs = 45.3592 kg, rounds to 45.25 kg
        assertThat(manager.parseUserInput("100lbs")).isEqualTo(45.25f)
        assertThat(manager.parseUserInput("100 lbs")).isEqualTo(45.25f)
        assertThat(manager.parseUserInput("100LBS")).isEqualTo(45.25f)
        assertThat(manager.parseUserInput("100lb")).isEqualTo(45.25f)
    }

    @Test
    fun `parseUserInput handles invalid input`() {
        assertThat(manager.parseUserInput("")).isEqualTo(0f)
        assertThat(manager.parseUserInput("abc")).isEqualTo(0f)
        assertThat(manager.parseUserInput("kg")).isEqualTo(0f)
        assertThat(manager.parseUserInput("lbs")).isEqualTo(0f)
    }

    @Test
    fun `parseUserInput rounds to storage precision`() {
        assertThat(manager.parseUserInput("100.1")).isEqualTo(100.0f)
        assertThat(manager.parseUserInput("100.2")).isEqualTo(100.25f)
        assertThat(manager.parseUserInput("100.3")).isEqualTo(100.25f)
        assertThat(manager.parseUserInput("100.4")).isEqualTo(100.5f)
    }

    @Test
    fun `getExportWeight returns weight and unit`() {
        val result = manager.getExportWeight(100f)
        assertThat(result.first).isEqualTo(100f)
        assertThat(result.second).isEqualTo("kg")
    }

    @Test
    fun `parseImportWeight handles kg values`() {
        assertThat(manager.parseImportWeight(100f, "kg")).isEqualTo(100f)
        assertThat(manager.parseImportWeight(82.5f, "KG")).isEqualTo(82.5f)
        assertThat(manager.parseImportWeight(100f, null)).isEqualTo(100f)
    }

    @Test
    fun `parseImportWeight handles lbs values`() {
        val lbsValue = 220f
        val expectedKg = 220f * 0.453592f
        val rounded = manager.roundToStoragePrecision(expectedKg)
        assertThat(manager.parseImportWeight(lbsValue, "lbs")).isEqualTo(rounded)
        assertThat(manager.parseImportWeight(lbsValue, "LBS")).isEqualTo(rounded)
        assertThat(manager.parseImportWeight(lbsValue, "lb")).isEqualTo(rounded)
    }

    @Test
    fun `parseImportWeight rounds to storage precision`() {
        val result = manager.parseImportWeight(100.123f, "kg")
        assertThat(result).isEqualTo(100.0f)
    }

    @Test
    fun `edge case - zero weight`() {
        assertThat(manager.convertFromKg(0f)).isEqualTo(0f)
        assertThat(manager.convertToKg(0f)).isEqualTo(0f)
        assertThat(manager.formatWeight(0f)).isEqualTo("0")
        assertThat(manager.formatWeightWithUnit(0f)).isEqualTo("0kg")
        assertThat(manager.parseUserInput("0")).isEqualTo(0f)
    }

    @Test
    fun `edge case - very small weights`() {
        assertThat(manager.roundToStoragePrecision(0.1f)).isEqualTo(0f)
        assertThat(manager.roundToStoragePrecision(0.2f)).isEqualTo(0.25f)
        assertThat(manager.formatWeight(0.25f)).isEqualTo("0.25")
    }

    @Test
    fun `edge case - very large weights`() {
        val largeWeight = 500f
        assertThat(manager.convertFromKg(largeWeight)).isEqualTo(largeWeight)
        assertThat(manager.formatWeight(largeWeight)).isEqualTo("500")
        assertThat(manager.formatVolume(150000f)).isEqualTo("150k kg")
    }
}
