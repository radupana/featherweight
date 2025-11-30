@file:Suppress("MatchingDeclarationName")

package com.github.radupana.featherweight.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object FeatherweightColors {
    @Composable
    fun success(): Color = if (isSystemInDarkTheme()) Color(0xFF6EE7B7) else Color(0xFF10B981)

    @Composable
    fun successContainer(): Color = if (isSystemInDarkTheme()) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFF10B981).copy(alpha = 0.1f)

    @Composable
    fun onSuccess(): Color = if (isSystemInDarkTheme()) Color(0xFF064E3B) else Color.White

    @Composable
    fun warning(): Color = if (isSystemInDarkTheme()) Color(0xFFFCD34D) else Color(0xFFF59E0B)

    @Composable
    fun warningContainer(): Color = if (isSystemInDarkTheme()) Color(0xFFF59E0B).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.1f)

    @Composable
    fun onWarning(): Color = if (isSystemInDarkTheme()) Color(0xFF78350F) else Color.White

    @Composable
    fun danger(): Color = if (isSystemInDarkTheme()) Color(0xFFF87171) else Color(0xFFEF4444)

    @Composable
    fun dangerContainer(): Color = if (isSystemInDarkTheme()) Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.1f)

    @Composable
    fun gold(): Color = if (isSystemInDarkTheme()) Color(0xFFFCD34D) else Color(0xFFF59E0B)

    @Composable
    fun splashBackground(): Color = Color(0xFF111827)

    @Composable
    fun inProgressBackground(): Color = if (isSystemInDarkTheme()) Color(0xFFF59E0B).copy(alpha = 0.15f) else Color(0xFFFEF3C7)

    @Composable
    fun inProgressText(): Color = if (isSystemInDarkTheme()) Color(0xFFFCD34D) else Color(0xFF92400E)

    @Composable
    fun notStartedBackground(): Color = if (isSystemInDarkTheme()) Color(0xFF374151) else Color(0xFFF3F4F6)

    @Composable
    fun notStartedText(): Color = if (isSystemInDarkTheme()) Color(0xFF9CA3AF) else Color(0xFF6B7280)

    @Composable
    fun rpeLight(): Color = if (isSystemInDarkTheme()) Color(0xFF6EE7B7) else Color(0xFF10B981)

    @Composable
    fun rpeMedium(): Color = if (isSystemInDarkTheme()) Color(0xFFFCD34D) else Color(0xFFF59E0B)

    @Composable
    fun rpeHeavy(): Color = if (isSystemInDarkTheme()) Color(0xFFF87171) else Color(0xFFEF4444)

    @Composable
    fun scoreExcellent(): Color = if (isSystemInDarkTheme()) Color(0xFF6EE7B7) else Color(0xFF10B981)

    @Composable
    fun scoreGood(): Color = if (isSystemInDarkTheme()) Color(0xFF86EFAC) else Color(0xFF22C55E)

    @Composable
    fun scoreFair(): Color = if (isSystemInDarkTheme()) Color(0xFFFCD34D) else Color(0xFFF59E0B)

    @Composable
    fun scorePoor(): Color = if (isSystemInDarkTheme()) Color(0xFFF87171) else Color(0xFFEF4444)
}

@Composable
fun FeatherweightTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}
