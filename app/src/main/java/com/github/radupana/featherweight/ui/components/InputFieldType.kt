package com.github.radupana.featherweight.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class InputFieldType {
    WEIGHT, // Max 7 chars: "1234.56"
    REPS, // Max 2 chars: "99"
    RPE, // Max 2 chars: "10"
}

@Composable
fun CenteredInputField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    fieldType: InputFieldType,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    imeAction: ImeAction = ImeAction.Next,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val coroutineScope = rememberCoroutineScope()

    // Track the last known focus state to detect transitions
    var lastFocusState by remember { mutableStateOf(false) }

    // CRITICAL FIX: Force Number keyboard to prevent auto-decimal insertion
    // Some keyboards (especially Gboard) auto-insert decimals with Decimal type
    // Users can still manually type "." for weight fields
    val keyboardType = KeyboardType.Number

    // Show placeholder when value is empty (regardless of focus state)
    val showPlaceholder = value.text.isEmpty()

    // Handle focus events
    LaunchedEffect(isFocused) {
        // Detect focus gain transition
        if (isFocused && !lastFocusState && value.text.isNotEmpty()) {
            // Use coroutine to ensure the selection happens after the field is ready
            coroutineScope.launch {
                // Very small delay to ensure field is fully focused
                delay(5) // 5ms delay - minimal but reliable
                onValueChange(
                    value.copy(selection = TextRange(0, value.text.length)),
                )
            }
        }

        lastFocusState = isFocused
    }

    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->

            // HARD LIMITS - reject input if it exceeds maximum length or contains invalid chars
            val isValid =
                when (fieldType) {
                    InputFieldType.WEIGHT -> {
                        val text = newValue.text
                        // Allow only digits and one decimal point, max 7 chars total
                        val validChars = text.all { it.isDigit() || it == '.' }
                        val maxLength = text.length <= 7
                        val maxOneDecimal = text.count { it == '.' } <= 1

                        // Check decimal format: max 4 before decimal, max 2 after
                        val validDecimalFormat =
                            if (text.contains('.')) {
                                val parts = text.split('.')
                                parts.size == 2 && parts[0].length <= 4 && parts[1].length <= 2
                            } else {
                                text.length <= 4 // Max 4 digits without decimal
                            }

                        validChars && maxLength && maxOneDecimal && validDecimalFormat
                    }

                    InputFieldType.REPS -> {
                        val text = newValue.text
                        // Allow only digits, max 2 characters
                        text.all { it.isDigit() } && text.length <= 2
                    }

                    InputFieldType.RPE -> {
                        val text = newValue.text
                        // Allow digits and one decimal point, max 4 chars (e.g., "10.0")
                        val validChars = text.all { it.isDigit() || it == '.' }
                        val maxLength = text.length <= 4
                        val maxOneDecimal = text.count { it == '.' } <= 1
                        // Don't allow decimal at start or multiple decimals in a row
                        val validFormat = !text.startsWith(".") && !text.contains("..")

                        validChars && maxLength && maxOneDecimal && validFormat
                    }
                }

            val finalValue =
                if (isValid) {
                    // For RPE, don't auto-round during typing - let user type freely
                    // We'll handle rounding when they finish editing
                    if (fieldType == InputFieldType.RPE && newValue.text.isNotEmpty()) {
                        val rpeValue = newValue.text.toFloatOrNull()
                        if (rpeValue != null && rpeValue > 10f) {
                            // Only clamp if > 10, don't round while typing
                            newValue.copy(text = "10", selection = TextRange(2))
                        } else {
                            newValue
                        }
                    } else {
                        newValue
                    }
                } else {
                    // Input is invalid, reject it and keep old value
                    value
                }

            onValueChange(finalValue)
        },
        placeholder =
            if (showPlaceholder && placeholder.isNotEmpty()) {
                {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = placeholder,
                            style = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                            // Use a darker color for better visibility as target values
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                null
            },
        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
        singleLine = true,
        keyboardOptions =
            KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction,
            ),
        keyboardActions = keyboardActions,
        interactionSource = interactionSource,
        modifier = modifier,
    )
}
