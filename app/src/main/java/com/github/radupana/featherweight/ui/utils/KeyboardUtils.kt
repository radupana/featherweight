package com.github.radupana.featherweight.ui.utils

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.Modifier

/**
 * Extension function to apply consistent keyboard handling across all screens.
 * This adds proper padding for both the keyboard (IME) and navigation bars.
 */
fun Modifier.keyboardHandling(): Modifier = this
    .imePadding() // Adds padding when keyboard is visible
    .navigationBarsPadding() // Adds padding for navigation bars

/**
 * Extension function for content that should respond to keyboard but not navigation bars.
 * Useful for dialogs or full-screen content.
 */
fun Modifier.keyboardPadding(): Modifier = this
    .imePadding() // Only keyboard padding