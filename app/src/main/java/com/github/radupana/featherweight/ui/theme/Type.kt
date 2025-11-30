package com.github.radupana.featherweight.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

private const val TABULAR_NUMERALS = "tnum"

val AppTypography =
    Typography(
        displayLarge =
            Typography().displayLarge.copy(
                fontSize = 45.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.025).em,
                fontFeatureSettings = TABULAR_NUMERALS,
            ),
        displayMedium =
            Typography().displayMedium.copy(
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.025).em,
                fontFeatureSettings = TABULAR_NUMERALS,
            ),
        displaySmall =
            Typography().displaySmall.copy(
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.025).em,
                fontFeatureSettings = TABULAR_NUMERALS,
            ),
        headlineLarge =
            Typography().headlineLarge.copy(
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.02).em,
            ),
        headlineMedium =
            Typography().headlineMedium.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.015).em,
            ),
        headlineSmall =
            Typography().headlineSmall.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.01).em,
                fontFeatureSettings = TABULAR_NUMERALS,
            ),
        titleLarge =
            Typography().titleLarge.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.01).em,
            ),
        titleMedium =
            Typography().titleMedium.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.em,
                fontFeatureSettings = TABULAR_NUMERALS,
            ),
        titleSmall =
            Typography().titleSmall.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.005.em,
                fontFeatureSettings = TABULAR_NUMERALS,
            ),
        bodyLarge =
            Typography().bodyLarge.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 1.5.em,
                letterSpacing = 0.01.em,
            ),
        bodyMedium =
            Typography().bodyMedium.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 1.4.em,
                letterSpacing = 0.01.em,
                fontFeatureSettings = TABULAR_NUMERALS,
            ),
        bodySmall =
            Typography().bodySmall.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 1.3.em,
                letterSpacing = 0.02.em,
            ),
        labelLarge =
            Typography().labelLarge.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.02.em,
                fontFeatureSettings = TABULAR_NUMERALS,
            ),
        labelMedium =
            Typography().labelMedium.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.025.em,
                fontFeatureSettings = TABULAR_NUMERALS,
            ),
        labelSmall =
            Typography().labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.03.em,
                fontFeatureSettings = TABULAR_NUMERALS,
            ),
    )
