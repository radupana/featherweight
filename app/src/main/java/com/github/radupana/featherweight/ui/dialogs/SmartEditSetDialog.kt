package com.github.radupana.featherweight.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.domain.SmartSuggestions
import com.github.radupana.featherweight.ui.components.SmartSuggestionCard
import com.github.radupana.featherweight.viewmodel.WorkoutViewModel
import kotlinx.coroutines.launch

@Composable
fun SmartEditSetDialog(
    set: SetLog?, // null for new set
    exerciseName: String,
    onDismiss: () -> Unit,
    onSave: (reps: Int, weight: Float, rpe: Float?) -> Unit,
    viewModel: WorkoutViewModel,
) {
    var reps by remember { mutableStateOf(set?.actualReps?.toString() ?: "") }
    var weight by remember { mutableStateOf(set?.actualWeight?.toString() ?: "") }
    var rpe by remember { mutableStateOf(set?.actualRpe?.toString() ?: "") }
    var smartSuggestions by remember { mutableStateOf<SmartSuggestions?>(null) }
    var showSuggestions by remember { mutableStateOf(set == null) }

    val focusManager = LocalFocusManager.current
    val repsFocusRequester = remember { FocusRequester() }
    val weightFocusRequester = remember { FocusRequester() }
    val rpeFocusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    // Load smart suggestions for new sets
    LaunchedEffect(exerciseName) {
        if (set == null) {
            scope.launch {
                val suggestions = viewModel.getSmartSuggestions(exerciseName)
                smartSuggestions = suggestions

                // Pre-fill with suggestions if empty
                if (suggestions != null) {
                    if (reps.isEmpty() && suggestions.suggestedReps > 0) {
                        reps = suggestions.suggestedReps.toString()
                    }
                    if (weight.isEmpty() && suggestions.suggestedWeight > 0) {
                        weight = suggestions.suggestedWeight.toString()
                    }
                    if (rpe.isEmpty() && suggestions.suggestedRpe != null) {
                        rpe = suggestions.suggestedRpe.toString()
                    }
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (set != null) "Edit Set ${set.setOrder + 1}" else "Add New Set",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Smart suggestions card (only for new sets)
                if (showSuggestions && smartSuggestions != null) {
                    SmartSuggestionCard(
                        suggestions = smartSuggestions!!,
                        onUseSuggestion = {
                            val suggestions = smartSuggestions!!
                            reps = suggestions.suggestedReps.toString()
                            weight = suggestions.suggestedWeight.toString()
                            suggestions.suggestedRpe?.let { rpe = it.toString() }
                        },
                        onDismiss = { showSuggestions = false },
                    )
                }

                // Input fields
                OutlinedTextField(
                    value = reps,
                    onValueChange = { reps = it },
                    label = { Text("Reps") },
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next,
                        ),
                    keyboardActions =
                        KeyboardActions(
                            onNext = { weightFocusRequester.requestFocus() },
                        ),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .focusRequester(repsFocusRequester),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Weight (kg)") },
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next,
                        ),
                    keyboardActions =
                        KeyboardActions(
                            onNext = { rpeFocusRequester.requestFocus() },
                        ),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .focusRequester(weightFocusRequester),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = rpe,
                    onValueChange = { rpe = it },
                    label = { Text("RPE (1-10, optional)") },
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done,
                        ),
                    keyboardActions =
                        KeyboardActions(
                            onDone = { focusManager.clearFocus() },
                        ),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .focusRequester(rpeFocusRequester),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val repsInt = reps.toIntOrNull() ?: 0
                    val weightFloat = weight.toFloatOrNull() ?: 0f
                    val rpeFloat = rpe.toFloatOrNull()
                    onSave(repsInt, weightFloat, rpeFloat)
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )

    LaunchedEffect(Unit) {
        repsFocusRequester.requestFocus()
    }
}
