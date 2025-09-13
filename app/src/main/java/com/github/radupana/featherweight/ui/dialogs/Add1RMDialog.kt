package com.github.radupana.featherweight.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.util.WeightFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Add1RMDialog(
    exerciseName: String,
    currentMax: Float? = null,
    onSave: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    var weightText by remember {
        mutableStateOf(
            currentMax?.let { WeightFormatter.formatWeight(it) } ?: "",
        )
    }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set 1RM for $exerciseName") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { value ->
                        weightText = value
                        isError = value.isNotEmpty() && value.toFloatOrNull() == null
                    },
                    label = { Text("1RM ${WeightFormatter.getWeightLabel()}") },
                    isError = isError,
                    supportingText =
                        if (isError) {
                            { Text("Please enter a valid weight") }
                        } else {
                            null
                        },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (currentMax != null) {
                    Text(
                        text = "Current: ${WeightFormatter.formatWeightWithUnit(currentMax)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Parse and convert to kg based on current unit setting
                    val weightInKg = WeightFormatter.parseUserInput(weightText)
                    if (weightInKg > 0) {
                        onSave(weightInKg)
                    }
                },
                enabled = weightText.isNotEmpty() && !isError,
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
