package com.github.radupana.featherweight.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WeightFormatterPRTest {
    @Test
    fun formatLastAndPR_formatsCorrectlyWithWholeNumbers() {
        val result =
            WeightFormatter.formatLastAndPR(
                lastReps = 5,
                lastWeight = 80f,
                prReps = 3,
                prWeight = 100f,
            )

        assertThat(result).isEqualTo("Last: 5x80kg (PR: 3x100kg)")
    }

    @Test
    fun formatLastAndPR_formatsCorrectlyWithDecimals() {
        val result =
            WeightFormatter.formatLastAndPR(
                lastReps = 8,
                lastWeight = 82.5f,
                prReps = 5,
                prWeight = 102.75f,
            )

        assertThat(result).isEqualTo("Last: 8x82.5kg (PR: 5x102.75kg)")
    }

    @Test
    fun formatLastAndPR_roundsToNearestQuarter() {
        val result =
            WeightFormatter.formatLastAndPR(
                lastReps = 5,
                lastWeight = 80.13f,
                prReps = 3,
                prWeight = 100.87f,
            )

        assertThat(result).isEqualTo("Last: 5x80.25kg (PR: 3x100.75kg)")
    }

    @Test
    fun formatLastAndPR_handlesEqualWeights() {
        val result =
            WeightFormatter.formatLastAndPR(
                lastReps = 5,
                lastWeight = 100f,
                prReps = 3,
                prWeight = 100f,
            )

        assertThat(result).isEqualTo("Last: 5x100kg (PR: 3x100kg)")
    }

    @Test
    fun formatLastAndPR_handlesSingleRep() {
        val result =
            WeightFormatter.formatLastAndPR(
                lastReps = 1,
                lastWeight = 120f,
                prReps = 1,
                prWeight = 140f,
            )

        assertThat(result).isEqualTo("Last: 1x120kg (PR: 1x140kg)")
    }

    @Test
    fun formatLastAndPR_handlesHighReps() {
        val result =
            WeightFormatter.formatLastAndPR(
                lastReps = 15,
                lastWeight = 60f,
                prReps = 20,
                prWeight = 50f,
            )

        assertThat(result).isEqualTo("Last: 15x60kg (PR: 20x50kg)")
    }
}
