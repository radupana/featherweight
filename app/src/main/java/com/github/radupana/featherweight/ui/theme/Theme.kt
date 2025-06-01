package com.github.radupana.featherweight.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

// Modern color palette inspired by fitness/strength apps
private val Primary = Color(0xFF2196F3) // Strong blue
private val PrimaryVariant = Color(0xFF1976D2) // Darker blue
private val Secondary = Color(0xFF00BCD4) // Cyan accent
private val SecondaryVariant = Color(0xFF0097A7) // Darker cyan

private val Success = Color(0xFF4CAF50) // Green for completed sets
private val Warning = Color(0xFFFF9800) // Orange for RPE warnings
private val Error = Color(0xFFE53935) // Red for delete actions

private val Surface = Color(0xFFFAFAFA) // Very light gray
private val SurfaceVariant = Color(0xFFF5F5F5) // Slightly darker gray
private val Background = Color(0xFFFFFFFF) // Pure white

private val OnSurface = Color(0xFF1A1A1A) // Almost black
private val OnSurfaceVariant = Color(0xFF666666) // Medium gray
private val OnBackground = Color(0xFF1A1A1A) // Almost black

private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = Primary.copy(alpha = 0.1f),
    onPrimaryContainer = PrimaryVariant,

    secondary = Secondary,
    onSecondary = Color.White,
    secondaryContainer = Secondary.copy(alpha = 0.1f),
    onSecondaryContainer = SecondaryVariant,

    tertiary = Success,
    onTertiary = Color.White,
    tertiaryContainer = Success.copy(alpha = 0.1f),
    onTertiaryContainer = Success,

    error = Error,
    onError = Color.White,
    errorContainer = Error.copy(alpha = 0.1f),
    onErrorContainer = Error,

    background = Background,
    onBackground = OnBackground,

    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,

    inverseSurface = OnSurface,
    inverseOnSurface = Surface,

    outline = OnSurfaceVariant.copy(alpha = 0.3f),
    outlineVariant = OnSurfaceVariant.copy(alpha = 0.1f),

    scrim = Color.Black.copy(alpha = 0.5f)
)

// Enhanced typography with better hierarchy
private val Typography = Typography(
    displayLarge = Typography().displayLarge.copy(
        fontWeight = FontWeight.Bold,
        color = OnSurface
    ),
    displayMedium = Typography().displayMedium.copy(
        fontWeight = FontWeight.Bold,
        color = OnSurface
    ),
    displaySmall = Typography().displaySmall.copy(
        fontWeight = FontWeight.Bold,
        color = OnSurface
    ),

    headlineLarge = Typography().headlineLarge.copy(
        fontWeight = FontWeight.Bold,
        color = OnSurface
    ),
    headlineMedium = Typography().headlineMedium.copy(
        fontWeight = FontWeight.SemiBold,
        color = OnSurface
    ),
    headlineSmall = Typography().headlineSmall.copy(
        fontWeight = FontWeight.SemiBold,
        color = OnSurface
    ),

    titleLarge = Typography().titleLarge.copy(
        fontWeight = FontWeight.SemiBold,
        color = OnSurface
    ),
    titleMedium = Typography().titleMedium.copy(
        fontWeight = FontWeight.Medium,
        color = OnSurface
    ),
    titleSmall = Typography().titleSmall.copy(
        fontWeight = FontWeight.Medium,
        color = OnSurface
    ),

    bodyLarge = Typography().bodyLarge.copy(
        fontWeight = FontWeight.Normal,
        color = OnSurface
    ),
    bodyMedium = Typography().bodyMedium.copy(
        fontWeight = FontWeight.Normal,
        color = OnSurface
    ),
    bodySmall = Typography().bodySmall.copy(
        fontWeight = FontWeight.Normal,
        color = OnSurfaceVariant
    ),

    labelLarge = Typography().labelLarge.copy(
        fontWeight = FontWeight.Medium,
        color = OnSurface
    ),
    labelMedium = Typography().labelMedium.copy(
        fontWeight = FontWeight.Medium,
        color = OnSurfaceVariant
    ),
    labelSmall = Typography().labelSmall.copy(
        fontWeight = FontWeight.Medium,
        color = OnSurfaceVariant
    )
)

@Composable
fun FeatherweightTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography,
        content = content
    )
}
