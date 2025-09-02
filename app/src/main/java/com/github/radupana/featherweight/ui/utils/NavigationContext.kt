package com.github.radupana.featherweight.ui.utils

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity

enum class NavigationContext {
    BOTTOM_NAVIGATION, // Screen is inside bottom navigation
    FULL_SCREEN, // Screen is full-screen (modal/standalone)
    DIALOG, // Screen is a dialog
}

@Composable
fun rememberKeyboardState(): State<Boolean> {
    val ime = WindowInsets.ime
    val density = LocalDensity.current
    return remember {
        derivedStateOf {
            ime.getBottom(density) > 0
        }
    }
}

/**
 * Context-aware system padding that only applies what's needed.
 * For screens in bottom navigation, navigation bar padding is NOT needed.
 * For full-screen content, both IME and navigation bar padding are needed.
 */
fun Modifier.systemBarsPadding(
    navigationContext: NavigationContext = NavigationContext.BOTTOM_NAVIGATION,
    includeIme: Boolean = true,
): Modifier =
    when (navigationContext) {
        NavigationContext.BOTTOM_NAVIGATION -> {
            // Bottom nav already handles navigation bar space
            if (includeIme) this.imePadding() else this
        }

        NavigationContext.FULL_SCREEN -> {
            // Full-screen needs both
            var modifier = this.navigationBarsPadding()
            if (includeIme) modifier = modifier.imePadding()
            modifier
        }

        NavigationContext.DIALOG -> {
            // Dialogs typically only need IME padding
            if (includeIme) this.imePadding() else this
        }
    }
