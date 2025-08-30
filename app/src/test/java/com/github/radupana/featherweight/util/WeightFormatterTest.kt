package com.github.radupana.featherweight.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WeightFormatterTest {
    @Test
    fun `formatWeight formats whole numbers without decimal`() {
        assertThat(WeightFormatter.formatWeight(82.0f)).isEqualTo("82")
        assertThat(WeightFormatter.formatWeight(100.0f)).isEqualTo("100")
        assertThat(WeightFormatter.formatWeight(0.0f)).isEqualTo("0")
        assertThat(WeightFormatter.formatWeight(1.0f)).isEqualTo("1")
    }

    @Test
    fun `formatWeight formats quarter values correctly`() {
        assertThat(WeightFormatter.formatWeight(82.25f)).isEqualTo("82.25")
        assertThat(WeightFormatter.formatWeight(82.5f)).isEqualTo("82.5")
        assertThat(WeightFormatter.formatWeight(82.75f)).isEqualTo("82.75")
    }

    @Test
    fun `formatWeight rounds to nearest quarter`() {
        assertThat(WeightFormatter.formatWeight(82.1f)).isEqualTo("82")
        assertThat(WeightFormatter.formatWeight(82.2f)).isEqualTo("82.25")
        assertThat(WeightFormatter.formatWeight(82.3f)).isEqualTo("82.25")
        assertThat(WeightFormatter.formatWeight(82.4f)).isEqualTo("82.5")
        assertThat(WeightFormatter.formatWeight(82.6f)).isEqualTo("82.5")
        assertThat(WeightFormatter.formatWeight(82.8f)).isEqualTo("82.75")
        assertThat(WeightFormatter.formatWeight(82.9f)).isEqualTo("83")
    }

    @Test
    fun `formatWeightWithUnit appends kg unit`() {
        assertThat(WeightFormatter.formatWeightWithUnit(100.0f)).isEqualTo("100kg")
        assertThat(WeightFormatter.formatWeightWithUnit(82.5f)).isEqualTo("82.5kg")
        assertThat(WeightFormatter.formatWeightWithUnit(0.0f)).isEqualTo("0kg")
        assertThat(WeightFormatter.formatWeightWithUnit(102.25f)).isEqualTo("102.25kg")
    }

    @Test
    fun `roundToNearestQuarter rounds correctly`() {
        assertThat(WeightFormatter.roundToNearestQuarter(10.0f)).isEqualTo(10.0f)
        assertThat(WeightFormatter.roundToNearestQuarter(10.1f)).isEqualTo(10.0f)
        assertThat(WeightFormatter.roundToNearestQuarter(10.12f)).isEqualTo(10.0f)
        assertThat(WeightFormatter.roundToNearestQuarter(10.13f)).isEqualTo(10.25f)
        assertThat(WeightFormatter.roundToNearestQuarter(10.37f)).isEqualTo(10.25f)
        assertThat(WeightFormatter.roundToNearestQuarter(10.38f)).isEqualTo(10.5f)
        assertThat(WeightFormatter.roundToNearestQuarter(10.62f)).isEqualTo(10.5f)
        assertThat(WeightFormatter.roundToNearestQuarter(10.63f)).isEqualTo(10.75f)
        assertThat(WeightFormatter.roundToNearestQuarter(10.87f)).isEqualTo(10.75f)
        assertThat(WeightFormatter.roundToNearestQuarter(10.88f)).isEqualTo(11.0f)
    }

    @Test
    fun `roundUpToNearestQuarter always rounds up`() {
        assertThat(WeightFormatter.roundUpToNearestQuarter(10.0f)).isEqualTo(10.0f)
        assertThat(WeightFormatter.roundUpToNearestQuarter(10.01f)).isEqualTo(10.25f)
        assertThat(WeightFormatter.roundUpToNearestQuarter(10.25f)).isEqualTo(10.25f)
        assertThat(WeightFormatter.roundUpToNearestQuarter(10.26f)).isEqualTo(10.5f)
        assertThat(WeightFormatter.roundUpToNearestQuarter(10.5f)).isEqualTo(10.5f)
        assertThat(WeightFormatter.roundUpToNearestQuarter(10.51f)).isEqualTo(10.75f)
        assertThat(WeightFormatter.roundUpToNearestQuarter(10.75f)).isEqualTo(10.75f)
        assertThat(WeightFormatter.roundUpToNearestQuarter(10.76f)).isEqualTo(11.0f)
    }

    @Test
    fun `formatDecimal formats with specified decimal places`() {
        assertThat(WeightFormatter.formatDecimal(10.0f, 2)).isEqualTo("10")
        assertThat(WeightFormatter.formatDecimal(10.5f, 2)).isEqualTo("10.5")
        assertThat(WeightFormatter.formatDecimal(10.55f, 2)).isEqualTo("10.55")
        assertThat(WeightFormatter.formatDecimal(10.555f, 2)).isEqualTo("10.56")
        assertThat(WeightFormatter.formatDecimal(10.5f, 1)).isEqualTo("10.5")
        assertThat(WeightFormatter.formatDecimal(10.55f, 1)).isEqualTo("10.6")
    }

    @Test
    fun `formatDecimal removes trailing zeros`() {
        assertThat(WeightFormatter.formatDecimal(10.00f, 2)).isEqualTo("10")
        assertThat(WeightFormatter.formatDecimal(10.10f, 2)).isEqualTo("10.1")
        assertThat(WeightFormatter.formatDecimal(10.100f, 3)).isEqualTo("10.1")
    }

    @Test
    fun `formatPercentage converts decimal to percentage`() {
        assertThat(WeightFormatter.formatPercentage(0.0f)).isEqualTo("0%")
        assertThat(WeightFormatter.formatPercentage(0.5f)).isEqualTo("50%")
        assertThat(WeightFormatter.formatPercentage(1.0f)).isEqualTo("100%")
        assertThat(WeightFormatter.formatPercentage(0.75f)).isEqualTo("75%")
        assertThat(WeightFormatter.formatPercentage(0.333f)).isEqualTo("33.3%")
        assertThat(WeightFormatter.formatPercentage(0.666f)).isEqualTo("66.6%")
    }

    @Test
    fun `formatPercentage handles edge cases`() {
        assertThat(WeightFormatter.formatPercentage(0.001f)).isEqualTo("0.1%")
        assertThat(WeightFormatter.formatPercentage(0.999f)).isEqualTo("99.9%")
        assertThat(WeightFormatter.formatPercentage(1.5f)).isEqualTo("150%")
        assertThat(WeightFormatter.formatPercentage(2.0f)).isEqualTo("200%")
    }

    @Test
    fun `formatVolume formats small volumes normally`() {
        assertThat(WeightFormatter.formatVolume(0f)).isEqualTo("0kg")
        assertThat(WeightFormatter.formatVolume(100f)).isEqualTo("100kg")
        assertThat(WeightFormatter.formatVolume(500f)).isEqualTo("500kg")
        assertThat(WeightFormatter.formatVolume(999f)).isEqualTo("999kg")
    }

    @Test
    fun `formatVolume formats medium volumes with k suffix`() {
        assertThat(WeightFormatter.formatVolume(1000f)).isEqualTo("1k kg")
        assertThat(WeightFormatter.formatVolume(1500f)).isEqualTo("1.5k kg")
        assertThat(WeightFormatter.formatVolume(2345f)).isEqualTo("2.35k kg")
        assertThat(WeightFormatter.formatVolume(9999f)).isEqualTo("10k kg")
    }

    @Test
    fun `formatVolume formats large volumes with single decimal`() {
        assertThat(WeightFormatter.formatVolume(10000f)).isEqualTo("10k kg")
        assertThat(WeightFormatter.formatVolume(15000f)).isEqualTo("15k kg")
        assertThat(WeightFormatter.formatVolume(15500f)).isEqualTo("15.5k kg")
        assertThat(WeightFormatter.formatVolume(99999f)).isEqualTo("100k kg")
        assertThat(WeightFormatter.formatVolume(123456f)).isEqualTo("123.5k kg")
    }

    @Test
    fun `formatWeight handles negative values`() {
        assertThat(WeightFormatter.formatWeight(-10f)).isEqualTo("-10")
        assertThat(WeightFormatter.formatWeight(-10.5f)).isEqualTo("-10.5")
        assertThat(WeightFormatter.formatWeight(-10.25f)).isEqualTo("-10.25")
    }

    @Test
    fun `formatWeight handles very small values`() {
        assertThat(WeightFormatter.formatWeight(0.1f)).isEqualTo("0") // 0.1 rounds to 0
        assertThat(WeightFormatter.formatWeight(0.125f)).isEqualTo("0") // 0.125 rounds to 0
        assertThat(WeightFormatter.formatWeight(0.25f)).isEqualTo("0.25")
        assertThat(WeightFormatter.formatWeight(0.5f)).isEqualTo("0.5")
        assertThat(WeightFormatter.formatWeight(0.75f)).isEqualTo("0.75")
    }

    @Test
    fun `formatWeight handles very large values`() {
        assertThat(WeightFormatter.formatWeight(1000f)).isEqualTo("1000")
        assertThat(WeightFormatter.formatWeight(9999.75f)).isEqualTo("9999.75")
        assertThat(WeightFormatter.formatWeight(10000.5f)).isEqualTo("10000.5")
    }

    @Test
    fun `roundToNearestQuarter handles edge cases`() {
        assertThat(WeightFormatter.roundToNearestQuarter(0f)).isEqualTo(0f)
        assertThat(WeightFormatter.roundToNearestQuarter(-1f)).isEqualTo(-1f)
        assertThat(WeightFormatter.roundToNearestQuarter(-1.1f)).isEqualTo(-1f)
        assertThat(WeightFormatter.roundToNearestQuarter(-1.2f)).isEqualTo(-1.25f)
    }

    @Test
    fun `roundUpToNearestQuarter handles zero and negatives`() {
        assertThat(WeightFormatter.roundUpToNearestQuarter(0f)).isEqualTo(0f)
        assertThat(WeightFormatter.roundUpToNearestQuarter(-1f)).isEqualTo(-1f)
        assertThat(WeightFormatter.roundUpToNearestQuarter(-1.1f)).isEqualTo(-1f)
    }

    @Test
    fun `formatVolume handles exact thousands`() {
        assertThat(WeightFormatter.formatVolume(1000f)).isEqualTo("1k kg")
        assertThat(WeightFormatter.formatVolume(2000f)).isEqualTo("2k kg")
        assertThat(WeightFormatter.formatVolume(3000f)).isEqualTo("3k kg")
    }

    @Test
    fun `formatPercentage rounds to one decimal place`() {
        assertThat(WeightFormatter.formatPercentage(0.1234f)).isEqualTo("12.3%")
        assertThat(WeightFormatter.formatPercentage(0.1236f)).isEqualTo("12.4%")
        assertThat(WeightFormatter.formatPercentage(0.1999f)).isEqualTo("20%")
    }
}
