package com.github.radupana.featherweight.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private val Emerald500 = Color(0xFF10B981)
private val Emerald300 = Color(0xFF6EE7B7)
private val Emerald200 = Color(0xFFA7F3D0)
private val Emerald100 = Color(0xFFD1FAE5)
private val Emerald800 = Color(0xFF065F46)
private val Emerald900 = Color(0xFF064E3B)

private val Indigo600 = Color(0xFF4F46E5)
private val Indigo700 = Color(0xFF4338CA)
private val Indigo300 = Color(0xFFA5B4FC)
private val Indigo200 = Color(0xFFC7D2FE)
private val Indigo100 = Color(0xFFE0E7FF)
private val Indigo900 = Color(0xFF312E81)

private val Amber500 = Color(0xFFF59E0B)
private val Amber700 = Color(0xFFB45309)
private val Amber300 = Color(0xFFFCD34D)
private val Amber200 = Color(0xFFFDE68A)
private val Amber100 = Color(0xFFFEF3C7)
private val Amber900 = Color(0xFF78350F)

private val Red400 = Color(0xFFF87171)
private val Red800 = Color(0xFF991B1B)
private val Red900 = Color(0xFF7F1D1D)
private val Red200 = Color(0xFFFECACA)

private val Gray50 = Color(0xFFF9FAFB)
private val Gray100 = Color(0xFFF3F4F6)
private val Gray200 = Color(0xFFE5E7EB)
private val Gray300 = Color(0xFFD1D5DB)
private val Gray400 = Color(0xFF9CA3AF)
private val Gray500 = Color(0xFF6B7280)
private val Gray700 = Color(0xFF374151)
private val Gray800 = Color(0xFF1F2937)

private val Slate500 = Color(0xFF64748B)
private val Slate600 = Color(0xFF475569)
private val Slate700 = Color(0xFF334155)
private val Slate800 = Color(0xFF1E293B)
private val Slate900 = Color(0xFF0F172A)

private val CardGradientTopColor = Color(0xFF2A374A)
private val CardGradientBottomColor = Color(0xFF1E293B)
private val CardBorderColor = Color(0xFF334155)
private val AccentBrightColor = Color(0xFF34D399)

object CardColors {
    val gradientTop: Color = CardGradientTopColor
    val gradientBottom: Color = CardGradientBottomColor
    val border: Color = CardBorderColor
}

object ButtonColors {
    val primaryGradientStart: Color = AccentBrightColor
    val primaryGradientEnd: Color = Color(0xFF10B981)
}

val LightColorScheme =
    lightColorScheme(
        primary = Emerald500,
        onPrimary = Color.White,
        primaryContainer = Emerald100,
        onPrimaryContainer = Emerald900,
        secondary = Indigo600,
        onSecondary = Color.White,
        secondaryContainer = Indigo100,
        onSecondaryContainer = Indigo900,
        tertiary = Amber500,
        onTertiary = Color.White,
        tertiaryContainer = Amber100,
        onTertiaryContainer = Amber900,
        error = Color(0xFFB3261E),
        onError = Color.White,
        errorContainer = Color(0xFFF9DEDC),
        onErrorContainer = Color(0xFF410E0B),
        background = Color(0xFFFAFAFA),
        onBackground = Gray800,
        surface = Color.White,
        onSurface = Gray800,
        surfaceVariant = Gray100,
        onSurfaceVariant = Gray500,
        surfaceTint = Emerald500,
        outline = Gray400,
        outlineVariant = Gray200,
        inverseSurface = Gray800,
        inverseOnSurface = Gray50,
        inversePrimary = Emerald300,
        scrim = Color.Black,
    )

val DarkColorScheme =
    darkColorScheme(
        primary = Emerald300,
        onPrimary = Emerald900,
        primaryContainer = Emerald800,
        onPrimaryContainer = Emerald200,
        secondary = Indigo300,
        onSecondary = Indigo900,
        secondaryContainer = Indigo700,
        onSecondaryContainer = Indigo200,
        tertiary = Amber300,
        onTertiary = Amber900,
        tertiaryContainer = Amber700,
        onTertiaryContainer = Amber200,
        error = Red400,
        onError = Red900,
        errorContainer = Red800,
        onErrorContainer = Red200,
        background = Slate900,
        onBackground = Gray50,
        surface = Gray800,
        onSurface = Gray100,
        surfaceVariant = Gray700,
        onSurfaceVariant = Gray300,
        surfaceTint = Emerald300,
        surfaceContainerLowest = Slate900,
        surfaceContainerLow = Slate800,
        surfaceContainer = Slate700,
        surfaceContainerHigh = Slate600,
        surfaceContainerHighest = Slate500,
        outline = Gray500,
        outlineVariant = Gray700,
        inverseSurface = Gray100,
        inverseOnSurface = Gray800,
        inversePrimary = Emerald500,
        scrim = Color.Black,
    )
