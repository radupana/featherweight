package com.github.radupana.featherweight.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WeightFormatterRoundingTest {
    @Test
    fun `roundToNearestQuarter rounds all possible values correctly`() {
        // Test exact quarters
        assertThat(WeightFormatter.roundToNearestQuarter(0.0f)).isEqualTo(0.0f)
        assertThat(WeightFormatter.roundToNearestQuarter(0.25f)).isEqualTo(0.25f)
        assertThat(WeightFormatter.roundToNearestQuarter(0.5f)).isEqualTo(0.5f)
        assertThat(WeightFormatter.roundToNearestQuarter(0.75f)).isEqualTo(0.75f)
        assertThat(WeightFormatter.roundToNearestQuarter(1.0f)).isEqualTo(1.0f)

        // Test values that should round down
        assertThat(WeightFormatter.roundToNearestQuarter(0.1f)).isEqualTo(0.0f)
        assertThat(WeightFormatter.roundToNearestQuarter(0.12f)).isEqualTo(0.0f)
        assertThat(WeightFormatter.roundToNearestQuarter(0.124f)).isEqualTo(0.0f)

        // Test values that should round up to 0.25
        assertThat(WeightFormatter.roundToNearestQuarter(0.13f)).isEqualTo(0.25f)
        assertThat(WeightFormatter.roundToNearestQuarter(0.2f)).isEqualTo(0.25f)
        assertThat(WeightFormatter.roundToNearestQuarter(0.37f)).isEqualTo(0.25f)

        // Test values that should round to 0.5
        assertThat(WeightFormatter.roundToNearestQuarter(0.38f)).isEqualTo(0.5f)
        assertThat(WeightFormatter.roundToNearestQuarter(0.4f)).isEqualTo(0.5f)
        assertThat(WeightFormatter.roundToNearestQuarter(0.6f)).isEqualTo(0.5f)
        assertThat(WeightFormatter.roundToNearestQuarter(0.62f)).isEqualTo(0.5f)

        // Test values that should round to 0.75
        assertThat(WeightFormatter.roundToNearestQuarter(0.63f)).isEqualTo(0.75f)
        assertThat(WeightFormatter.roundToNearestQuarter(0.7f)).isEqualTo(0.75f)
        assertThat(WeightFormatter.roundToNearestQuarter(0.87f)).isEqualTo(0.75f)

        // Test values that should round to 1.0
        assertThat(WeightFormatter.roundToNearestQuarter(0.88f)).isEqualTo(1.0f)
        assertThat(WeightFormatter.roundToNearestQuarter(0.9f)).isEqualTo(1.0f)
        assertThat(WeightFormatter.roundToNearestQuarter(1.1f)).isEqualTo(1.0f)
        assertThat(WeightFormatter.roundToNearestQuarter(1.12f)).isEqualTo(1.0f)

        // Test larger values
        assertThat(WeightFormatter.roundToNearestQuarter(17.612f)).isEqualTo(17.5f)
        assertThat(WeightFormatter.roundToNearestQuarter(17.735f)).isEqualTo(17.75f)
        assertThat(WeightFormatter.roundToNearestQuarter(100.123f)).isEqualTo(100.0f)
        assertThat(WeightFormatter.roundToNearestQuarter(100.876f)).isEqualTo(101.0f)
    }

    @Test
    fun `roundRPE rounds all possible values correctly`() {
        // Test exact halves
        assertThat(WeightFormatter.roundRPE(0.0f)).isEqualTo(0.0f)
        assertThat(WeightFormatter.roundRPE(0.5f)).isEqualTo(0.5f)
        assertThat(WeightFormatter.roundRPE(1.0f)).isEqualTo(1.0f)
        assertThat(WeightFormatter.roundRPE(7.5f)).isEqualTo(7.5f)
        assertThat(WeightFormatter.roundRPE(10.0f)).isEqualTo(10.0f)

        // Test values that should round down
        assertThat(WeightFormatter.roundRPE(0.1f)).isEqualTo(0.0f)
        assertThat(WeightFormatter.roundRPE(0.2f)).isEqualTo(0.0f)
        assertThat(WeightFormatter.roundRPE(0.24f)).isEqualTo(0.0f)
        assertThat(WeightFormatter.roundRPE(7.1f)).isEqualTo(7.0f)
        assertThat(WeightFormatter.roundRPE(7.24f)).isEqualTo(7.0f)

        // Test values that should round up to 0.5
        assertThat(WeightFormatter.roundRPE(0.26f)).isEqualTo(0.5f)
        assertThat(WeightFormatter.roundRPE(0.3f)).isEqualTo(0.5f)
        assertThat(WeightFormatter.roundRPE(0.74f)).isEqualTo(0.5f)
        assertThat(WeightFormatter.roundRPE(7.26f)).isEqualTo(7.5f)
        assertThat(WeightFormatter.roundRPE(7.3f)).isEqualTo(7.5f)
        assertThat(WeightFormatter.roundRPE(7.74f)).isEqualTo(7.5f)

        // Test values that should round to next integer
        assertThat(WeightFormatter.roundRPE(0.75f)).isEqualTo(1.0f)
        assertThat(WeightFormatter.roundRPE(0.8f)).isEqualTo(1.0f)
        assertThat(WeightFormatter.roundRPE(7.75f)).isEqualTo(8.0f)
        assertThat(WeightFormatter.roundRPE(7.8f)).isEqualTo(8.0f)
        assertThat(WeightFormatter.roundRPE(9.75f)).isEqualTo(10.0f)

        // Test null handling
        assertThat(WeightFormatter.roundRPE(null)).isNull()

        // Test clamping to 0-10 range
        assertThat(WeightFormatter.roundRPE(-1.0f)).isEqualTo(0.0f)
        assertThat(WeightFormatter.roundRPE(-5.0f)).isEqualTo(0.0f)
        assertThat(WeightFormatter.roundRPE(10.5f)).isEqualTo(10.0f)
        assertThat(WeightFormatter.roundRPE(11.0f)).isEqualTo(10.0f)
        assertThat(WeightFormatter.roundRPE(15.0f)).isEqualTo(10.0f)
    }

    @Test
    fun `isValidWeight correctly validates weight values`() {
        // Valid weights (multiples of 0.25)
        assertThat(WeightFormatter.isValidWeight(0.0f)).isTrue()
        assertThat(WeightFormatter.isValidWeight(0.25f)).isTrue()
        assertThat(WeightFormatter.isValidWeight(0.5f)).isTrue()
        assertThat(WeightFormatter.isValidWeight(0.75f)).isTrue()
        assertThat(WeightFormatter.isValidWeight(1.0f)).isTrue()
        assertThat(WeightFormatter.isValidWeight(17.5f)).isTrue()
        assertThat(WeightFormatter.isValidWeight(100.25f)).isTrue()
        assertThat(WeightFormatter.isValidWeight(100.5f)).isTrue()
        assertThat(WeightFormatter.isValidWeight(100.75f)).isTrue()

        // Invalid weights (not multiples of 0.25)
        assertThat(WeightFormatter.isValidWeight(0.1f)).isFalse()
        assertThat(WeightFormatter.isValidWeight(0.2f)).isFalse()
        assertThat(WeightFormatter.isValidWeight(0.3f)).isFalse()
        assertThat(WeightFormatter.isValidWeight(0.6f)).isFalse()
        assertThat(WeightFormatter.isValidWeight(0.8f)).isFalse()
        assertThat(WeightFormatter.isValidWeight(17.612f)).isFalse()
        assertThat(WeightFormatter.isValidWeight(17.735f)).isFalse()
        assertThat(WeightFormatter.isValidWeight(100.1f)).isFalse()
        assertThat(WeightFormatter.isValidWeight(100.33f)).isFalse()
        assertThat(WeightFormatter.isValidWeight(100.876f)).isFalse()
    }

    @Test
    fun `isValidRPE correctly validates RPE values`() {
        // Valid RPE values (multiples of 0.5 between 0 and 10)
        assertThat(WeightFormatter.isValidRPE(null)).isTrue()
        assertThat(WeightFormatter.isValidRPE(0.0f)).isTrue()
        assertThat(WeightFormatter.isValidRPE(0.5f)).isTrue()
        assertThat(WeightFormatter.isValidRPE(1.0f)).isTrue()
        assertThat(WeightFormatter.isValidRPE(1.5f)).isTrue()
        assertThat(WeightFormatter.isValidRPE(7.0f)).isTrue()
        assertThat(WeightFormatter.isValidRPE(7.5f)).isTrue()
        assertThat(WeightFormatter.isValidRPE(8.0f)).isTrue()
        assertThat(WeightFormatter.isValidRPE(9.5f)).isTrue()
        assertThat(WeightFormatter.isValidRPE(10.0f)).isTrue()

        // Invalid RPE values (not multiples of 0.5)
        assertThat(WeightFormatter.isValidRPE(0.1f)).isFalse()
        assertThat(WeightFormatter.isValidRPE(0.25f)).isFalse()
        assertThat(WeightFormatter.isValidRPE(0.3f)).isFalse()
        assertThat(WeightFormatter.isValidRPE(0.75f)).isFalse()
        assertThat(WeightFormatter.isValidRPE(7.25f)).isFalse()
        assertThat(WeightFormatter.isValidRPE(7.3f)).isFalse()
        assertThat(WeightFormatter.isValidRPE(7.75f)).isFalse()
        assertThat(WeightFormatter.isValidRPE(8.2f)).isFalse()
        assertThat(WeightFormatter.isValidRPE(9.7f)).isFalse()
    }

    @Test
    fun `formatWeight displays correctly rounded values`() {
        // Test that weights are displayed correctly after rounding
        assertThat(WeightFormatter.formatWeight(17.612f)).isEqualTo("17.5")
        assertThat(WeightFormatter.formatWeight(17.735f)).isEqualTo("17.75")
        assertThat(WeightFormatter.formatWeight(17.5f)).isEqualTo("17.5")
        assertThat(WeightFormatter.formatWeight(17.75f)).isEqualTo("17.75")
        assertThat(WeightFormatter.formatWeight(18.0f)).isEqualTo("18")
        assertThat(WeightFormatter.formatWeight(100.0f)).isEqualTo("100")
        assertThat(WeightFormatter.formatWeight(100.25f)).isEqualTo("100.25")
        assertThat(WeightFormatter.formatWeight(100.5f)).isEqualTo("100.5")
        assertThat(WeightFormatter.formatWeight(100.75f)).isEqualTo("100.75")
    }

    @Test
    fun `formatWeightWithUnit displays correctly rounded values with unit`() {
        assertThat(WeightFormatter.formatWeightWithUnit(17.612f)).isEqualTo("17.5kg")
        assertThat(WeightFormatter.formatWeightWithUnit(17.735f)).isEqualTo("17.75kg")
        assertThat(WeightFormatter.formatWeightWithUnit(17.5f)).isEqualTo("17.5kg")
        assertThat(WeightFormatter.formatWeightWithUnit(17.75f)).isEqualTo("17.75kg")
        assertThat(WeightFormatter.formatWeightWithUnit(18.0f)).isEqualTo("18kg")
    }

    @Test
    fun `formatRPE displays correctly`() {
        assertThat(WeightFormatter.formatRPE(null)).isEqualTo("")
        assertThat(WeightFormatter.formatRPE(7.0f)).isEqualTo("7")
        assertThat(WeightFormatter.formatRPE(7.5f)).isEqualTo("7.5")
        assertThat(WeightFormatter.formatRPE(8.0f)).isEqualTo("8")
        assertThat(WeightFormatter.formatRPE(8.5f)).isEqualTo("8.5")
        assertThat(WeightFormatter.formatRPE(10.0f)).isEqualTo("10")
    }

    @Test
    fun `roundUpToNearestQuarter always rounds up correctly`() {
        assertThat(WeightFormatter.roundUpToNearestQuarter(0.0f)).isEqualTo(0.0f)
        assertThat(WeightFormatter.roundUpToNearestQuarter(0.01f)).isEqualTo(0.25f)
        assertThat(WeightFormatter.roundUpToNearestQuarter(0.25f)).isEqualTo(0.25f)
        assertThat(WeightFormatter.roundUpToNearestQuarter(0.26f)).isEqualTo(0.5f)
        assertThat(WeightFormatter.roundUpToNearestQuarter(0.5f)).isEqualTo(0.5f)
        assertThat(WeightFormatter.roundUpToNearestQuarter(0.51f)).isEqualTo(0.75f)
        assertThat(WeightFormatter.roundUpToNearestQuarter(0.75f)).isEqualTo(0.75f)
        assertThat(WeightFormatter.roundUpToNearestQuarter(0.76f)).isEqualTo(1.0f)
        assertThat(WeightFormatter.roundUpToNearestQuarter(100.01f)).isEqualTo(100.25f)
        assertThat(WeightFormatter.roundUpToNearestQuarter(100.25f)).isEqualTo(100.25f)
    }
}
