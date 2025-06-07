package com.github.radupana.featherweight.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em

// Premium Athletic Color Palette
// Primary: Deep Athletic Blue → Purple gradient
private val Primary = Color(0xFF1A237E) // Deep blue
private val PrimaryLight = Color(0xFF3949AB) // Lighter blue
private val PrimaryDark = Color(0xFF0D1B5F) // Darker blue
private val Accent = Color(0xFF7C4DFF) // Purple accent for CTAs

// Success/Progress: Energetic gradient
private val Success = Color(0xFF00E676) // Vibrant green
private val SuccessLight = Color(0xFF69F0AE) // Light green
private val SuccessDark = Color(0xFF00BFA5) // Teal

// Warning & Error
private val Warning = Color(0xFFFF6F00) // Energetic orange
private val Error = Color(0xFFE53935) // Bold red

// Light Theme Colors
private val LightBackground = Color(0xFFF8F9FE) // Off-white with blue tint
private val LightSurface = Color(0xFFFFFFFF) // Pure white
private val LightSurfaceVariant = Color(0xFFF0F2F8) // Light blue-gray
private val LightSurfaceTint = Color(0xFFF5F7FF) // Very light blue

private val LightOnBackground = Color(0xFF0D1421) // Almost black
private val LightOnSurface = Color(0xFF1A1A1A) // Dark gray
private val LightOnSurfaceVariant = Color(0xFF5A6373) // Medium gray

// Dark Theme Colors
private val DarkBackground = Color(0xFF0A0E27) // Almost black with blue tint
private val DarkSurface = Color(0xFF1C1F33) // Dark blue-gray
private val DarkSurfaceVariant = Color(0xFF2A2D42) // Lighter dark surface
private val DarkSurfaceTint = Color(0xFF252847) // Blue-tinted dark

private val DarkOnBackground = Color(0xFFE8EAED) // Light gray
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
        // Secondary colors (using accent purple)
        secondary = Accent,
        onSecondary = Color.White,
        secondaryContainer = Accent.copy(alpha = 0.12f),
        onSecondaryContainer = Color(0xFF4A148C),
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
        primaryContainer = Primary,
        onPrimaryContainer = Color(0xFFBBC5FF),
        // Secondary colors (using accent purple)
        secondary = Color(0xFFB085F5),
        onSecondary = Color(0xFF2E1A47),
        secondaryContainer = Color(0xFF4A2C6A),
        onSecondaryContainer = Color(0xFFD1B3FF),
        // Tertiary colors (success green)
        tertiary = SuccessLight,
        onTertiary = Color(0xFF00402A),
        tertiaryContainer = Success,
        onTertiaryContainer = Color(0xFFBBF0D1),
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

// Premium Typography System
private val AppTypography =
    Typography(
        // Display styles - for hero text and major headings
        displayLarge =
            Typography().displayLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.025).em,
            ),
        displayMedium =
            Typography().displayMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.025).em,
            ),
        displaySmall =
            Typography().displaySmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.025).em,
            ),
        // Headlines - for section headers
        headlineLarge =
            Typography().headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.02).em,
            ),
        headlineMedium =
            Typography().headlineMedium.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.015).em,
            ),
        headlineSmall =
            Typography().headlineSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.01).em,
            ),
        // Titles - for card headers and important content
        titleLarge =
            Typography().titleLarge.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.01).em,
            ),
        titleMedium =
            Typography().titleMedium.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.em,
            ),
        titleSmall =
            Typography().titleSmall.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.005.em,
            ),
        // Body text - for content and descriptions
        bodyLarge =
            Typography().bodyLarge.copy(
                fontWeight = FontWeight.Normal,
                lineHeight = 1.6.em,
                letterSpacing = 0.01.em,
            ),
        bodyMedium =
            Typography().bodyMedium.copy(
                fontWeight = FontWeight.Normal,
                lineHeight = 1.5.em,
                letterSpacing = 0.01.em,
            ),
        bodySmall =
            Typography().bodySmall.copy(
                fontWeight = FontWeight.Normal,
                lineHeight = 1.4.em,
                letterSpacing = 0.02.em,
            ),
        // Labels - for captions and supporting text
        labelLarge =
            Typography().labelLarge.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.02.em,
            ),
        labelMedium =
            Typography().labelMedium.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.025.em,
            ),
        labelSmall =
            Typography().labelSmall.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.03.em,
            ),
    )

// Extended color palette for custom use cases
object FeatherweightColors {
    val successGradientStart = Success
    val successGradientEnd = SuccessDark
    val primaryGradientStart = Primary
    val primaryGradientEnd = Accent

    @Composable
    fun cardGlassBackground() = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)

    @Composable
    fun cardGlassBorder() = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    @Composable
    fun shimmerBase() = MaterialTheme.colorScheme.surfaceVariant

    @Composable
    fun shimmerHighlight() = MaterialTheme.colorScheme.surface
}

@Composable
fun FeatherweightTheme(
    darkTheme: Boolean = false, // Force light theme always
    content: @Composable () -> Unit,
) {
    // Always use light theme regardless of system setting
    val colorScheme = LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}
