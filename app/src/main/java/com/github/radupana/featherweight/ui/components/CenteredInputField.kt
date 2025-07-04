package com.github.radupana.featherweight.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class InputFieldType {
    WEIGHT,   // Max 7 chars: "1234.56" 
    REPS,     // Max 2 chars: "99"
    RPE       // Max 2 chars: "10"
}

@Composable
fun CenteredInputField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    fieldType: InputFieldType,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    imeAction: ImeAction = ImeAction.Next
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
    
    // Show placeholder only when not focused AND value is empty
    val showPlaceholder = !isFocused && value.text.isEmpty()
    
    // Debug logging for focus events
    LaunchedEffect(isFocused) {
        println("FOCUS Debug [${fieldType.name}] - Focus state: $isFocused, Text: '${value.text}', Selection: ${value.selection}")
        
        // Detect focus gain transition
        if (isFocused && !lastFocusState && value.text.isNotEmpty()) {
            println("FOCUS Debug [${fieldType.name}] - FOCUS GAINED! Selecting all text")
            // Use coroutine to ensure the selection happens after the field is ready
            coroutineScope.launch {
                // Very small delay to ensure field is fully focused
                delay(5) // 5ms delay - minimal but reliable
                println("FOCUS Debug [${fieldType.name}] - Applying selection after delay")
                onValueChange(
                    value.copy(selection = TextRange(0, value.text.length))
                )
            }
        }
        
        lastFocusState = isFocused
    }
    
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            println("Input Debug [${fieldType.name}] - Before: '${value.text}', Selection: ${value.selection}")
            println("Input Debug [${fieldType.name}] - New Input: '${newValue.text}', Selection: ${newValue.selection}")
            
            // HARD LIMITS - reject input if it exceeds maximum length or contains invalid chars
            val isValid = when (fieldType) {
                InputFieldType.WEIGHT -> {
                    val text = newValue.text
                    // Allow only digits and one decimal point, max 7 chars total
                    val validChars = text.all { it.isDigit() || it == '.' }
                    val maxLength = text.length <= 7
                    val maxOneDecimal = text.count { it == '.' } <= 1
                    
                    // Check decimal format: max 4 before decimal, max 2 after
                    val validDecimalFormat = if (text.contains('.')) {
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
                    // Allow only digits, max 2 characters
                    val validChars = text.all { it.isDigit() } && text.length <= 2
                    
                    // Additional check: if value > 10, we'll clamp it in the finalValue logic
                    validChars
                }
            }
            
            val finalValue = if (isValid) {
                // Input is valid, but check for RPE clamping
                if (fieldType == InputFieldType.RPE && newValue.text.isNotEmpty()) {
                    val rpeValue = newValue.text.toIntOrNull()
                    if (rpeValue != null && rpeValue > 10) {
                        // Clamp to 10
                        println("Input Debug [${fieldType.name}] - CLAMPED: '${newValue.text}' -> '10'")
                        newValue.copy(text = "10", selection = TextRange(2))
                    } else {
                        newValue
                    }
                } else {
                    newValue
                }
            } else {
                // Input is invalid, reject it and keep old value
                println("Input Debug [${fieldType.name}] - REJECTED: '${newValue.text}'")
                value
            }
            
            println("Input Debug [${fieldType.name}] - Final: '${finalValue.text}', Selection: ${finalValue.selection}")
            onValueChange(finalValue)
        },
        placeholder = if (showPlaceholder) {
            {
                Text(
                    text = placeholder,
                    style = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
                )
            }
        } else null,
        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        keyboardActions = keyboardActions,
        interactionSource = interactionSource,
        modifier = modifier
    )
}