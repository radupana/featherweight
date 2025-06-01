package com.github.radupana.featherweight.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp

@Composable
fun AddWorkoutDialog(
    onAdd: (exerciseName: String, sets: Int, reps: Int, weight: Float) -> Unit,
    onDismiss: () -> Unit
) {
    var exerciseName by remember { mutableStateOf("Squat") }
    var sets by remember { mutableStateOf("5") }
    var reps by remember { mutableStateOf("5") }
    var weight by remember { mutableStateOf("60") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                onAdd(
                    exerciseName,
                    sets.toIntOrNull() ?: 0,
                    reps.toIntOrNull() ?: 0,
                    weight.toFloatOrNull() ?: 0f
                )
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Log a Workout") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Exercise:")
                // Hardcoded for now, you can replace with DropdownMenu later
                OutlinedTextField(
                    value = exerciseName,
                    onValueChange = { exerciseName = it },
                    label = { Text("Exercise name") }
                )
                OutlinedTextField(
                    value = sets,
                    onValueChange = { sets = it },
                    label = { Text("Sets") }
                )
                OutlinedTextField(
                    value = reps,
                    onValueChange = { reps = it },
                    label = { Text("Reps") }
                )
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Weight (kg)") }
                )
            }
        }
    )
}
