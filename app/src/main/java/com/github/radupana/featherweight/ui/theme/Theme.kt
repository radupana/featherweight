package com.github.radupana.featherweight.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// Premium Athletic Color Palette
// Primary: Modern teal accent for dark theme
private val Primary = Color(0xFF4ECDC4) // Teal accent
private val PrimaryLight = Color(0xFF80E5DB) // Lighter teal
private val PrimaryDark = Color(0xFF1A9A8F) // Darker teal

// Success/Progress: Energetic gradient
private val Success = Color(0xFF00E676) // Vibrant green
private val SuccessLight = Color(0xFF69F0AE) // Light green
private val SuccessDark = Color(0xFF00BFA5) // Teal

// Error
private val Error = Color(0xFFE53935) // Bold red

// Light Theme Colors
private val LightBackground = Color(0xFFF8F9FE) // Off-white with blue tint
private val LightSurface = Color(0xFFFFFFFF) // Pure white
private val LightSurfaceVariant = Color(0xFFF0F2F8) // Light blue-gray

private val LightOnBackground = Color(0xFF0D1421) // Almost black
private val LightOnSurface = Color(0xFF1A1A1A) // Dark gray
private val LightOnSurfaceVariant = Color(0xFF5A6373) // Medium gray

// Dark Theme Colors - Premium dark design (softer)
private val DarkBackground = Color(0xFF121212) // Softer dark for better comfort
private val DarkSurface = Color(0xFF1C1C1C) // Elevated surface
private val DarkSurfaceVariant = Color(0xFF2A2A2A) // Higher elevation

private val DarkOnBackground = Color(0xFFE1E3E6) // Off-white
private val DarkOnSurface = Color(0xFFE1E3E6) // Off-white
private val DarkOnSurfaceVariant = Color(0xFF9AA0A6) // Medium light gray

// Light Color Scheme
private val LightColors =
    lightColorScheme(
        // Primary colors
        primary = Primary,
        onPrimary = Color.White,
        primaryContainer = PrimaryLight.copy(alpha = 0.12f),
        onPrimaryContainer = PrimaryDark,
        // Secondary colors (neutral teal-based)
        secondary = PrimaryDark,
        onSecondary = Color.White,
        secondaryContainer = PrimaryDark.copy(alpha = 0.12f),
        onSecondaryContainer = Color(0xFF003735),
        // Tertiary colors (success green)
        tertiary = Success,
        onTertiary = Color.White,
        tertiaryContainer = SuccessLight.copy(alpha = 0.12f),
        onTertiaryContainer = SuccessDark,
        // Error colors
        error = Error,
        onError = Color.White,
        errorContainer = Error.copy(alpha = 0.12f),
        onErrorContainer = Color(0xFFB71C1C),
        // Surface colors
        background = LightBackground,
        onBackground = LightOnBackground,
        surface = LightSurface,
        onSurface = LightOnSurface,
        surfaceVariant = LightSurfaceVariant,
        onSurfaceVariant = LightOnSurfaceVariant,
        surfaceTint = Primary,
        // Outline colors
        outline = LightOnSurfaceVariant.copy(alpha = 0.4f),
        outlineVariant = LightOnSurfaceVariant.copy(alpha = 0.12f),
        // Inverse colors
        inverseSurface = DarkSurface,
        inverseOnSurface = DarkOnSurface,
        inversePrimary = PrimaryLight,
        // Scrim
        scrim = Color.Black.copy(alpha = 0.6f),
    )

// Dark Color Scheme
private val DarkColors =
    darkColorScheme(
        // Primary colors
        primary = PrimaryLight,
        onPrimary = Color(0xFF000051),
        primaryContainer = PrimaryDark.copy(alpha = 0.3f),
        onPrimaryContainer = PrimaryLight,
        // Secondary colors (neutral teal-based)
        secondary = Color(0xFF6DB6B2),
        onSecondary = Color(0xFF003735),
        secondaryContainer = Color(0xFF003735).copy(alpha = 0.5f),
        onSecondaryContainer = Color(0xFF6DB6B2),
        // Tertiary colors (success green)
        tertiary = SuccessLight,
        onTertiary = Color(0xFF00402A),
        tertiaryContainer = SuccessDark.copy(alpha = 0.3f),
        onTertiaryContainer = SuccessLight,
        // Error colors
        error = Color(0xFFFF6B6B),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        // Surface colors
        background = DarkBackground,
        onBackground = DarkOnBackground,
        surface = DarkSurface,
        onSurface = DarkOnSurface,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = DarkOnSurfaceVariant,
        surfaceTint = PrimaryLight,
        // Outline colors
        outline = DarkOnSurfaceVariant.copy(alpha = 0.6f),
        outlineVariant = DarkOnSurfaceVariant.copy(alpha = 0.2f),
        // Inverse colors
        inverseSurface = LightSurface,
        inverseOnSurface = LightOnSurface,
        inversePrimary = Primary,
        // Scrim
        scrim = Color.Black.copy(alpha = 0.8f),
    )

// Compact Typography System - Space efficient
private val AppTypography =
    Typography(
        // Display styles - reduced sizes
        displayLarge =
            Typography().displayLarge.copy(
                fontSize = 45.sp, // Was 57sp
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.025).em,
            ),
        displayMedium =
            Typography().displayMedium.copy(
                fontSize = 36.sp, // Was 45sp
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.025).em,
            ),
        displaySmall =
            Typography().displaySmall.copy(
                fontSize = 30.sp, // Was 36sp
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.025).em,
            ),
        // Headlines - compact for sections
        headlineLarge =
            Typography().headlineLarge.copy(
                fontSize = 28.sp, // Was 32sp
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.02).em,
            ),
        headlineMedium =
            Typography().headlineMedium.copy(
                fontSize = 24.sp, // Was 28sp
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.015).em,
            ),
        headlineSmall =
            Typography().headlineSmall.copy(
                fontSize = 20.sp, // Was 24sp
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.01).em,
            ),
        // Titles - efficient card headers
        titleLarge =
            Typography().titleLarge.copy(
                fontSize = 18.sp, // Was 22sp
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.01).em,
            ),
        titleMedium =
            Typography().titleMedium.copy(
                fontSize = 16.sp, // Unchanged
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.em,
            ),
        titleSmall =
            Typography().titleSmall.copy(
                fontSize = 14.sp, // Unchanged
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.005.em,
            ),
        // Body text - compact content
        bodyLarge =
            Typography().bodyLarge.copy(
                fontSize = 14.sp, // Was 16sp
                fontWeight = FontWeight.Normal,
                lineHeight = 1.5.em, // Tighter line height
                letterSpacing = 0.01.em,
            ),
        bodyMedium =
            Typography().bodyMedium.copy(
                fontSize = 13.sp, // Was 14sp
                fontWeight = FontWeight.Normal,
                lineHeight = 1.4.em, // Tighter line height
                letterSpacing = 0.01.em,
            ),
        bodySmall =
            Typography().bodySmall.copy(
                fontSize = 12.sp, // Unchanged
                fontWeight = FontWeight.Normal,
                lineHeight = 1.3.em, // Tighter line height
                letterSpacing = 0.02.em,
            ),
        // Labels - compact supporting text
        labelLarge =
            Typography().labelLarge.copy(
                fontSize = 13.sp, // Was 14sp
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.02.em,
            ),
        labelMedium =
            Typography().labelMedium.copy(
                fontSize = 12.sp, // Unchanged
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.025.em,
            ),
        labelSmall =
            Typography().labelSmall.copy(
                fontSize = 10.sp, // Was 11sp
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.03.em,
            ),
    )

// Extended color palette for custom use cases
object FeatherweightColors {
    @Composable
    fun cardGlassBackground() = MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)

    @Composable
    fun cardGlassBorder() = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

    @Composable
    fun primaryGradientColors() =
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
        )

    @Composable
    fun cardGlassGradient() =
        listOf(
            Color.White.copy(alpha = 0.05f),
            Color.White.copy(alpha = 0.02f),
        )
}

@Composable
fun FeatherweightTheme(
    darkTheme: Boolean = true, // Dark theme by default
    content: @Composable () -> Unit,
) {
    // Use dark theme by default for premium feel
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}
