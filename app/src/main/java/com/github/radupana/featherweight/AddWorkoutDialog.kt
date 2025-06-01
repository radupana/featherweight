package com.github.radupana.featherweight

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AddWorkoutDialog(
    onAdd: (List<Pair<String, List<Triple<Int, Float, Float?>>>>) -> Unit,
    onDismiss: () -> Unit,
) {
    var exercises by remember {
        mutableStateOf(
            listOf(MutableExerciseInput()),
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                val result =
                    exercises.map { exercise ->
                        exercise.name to
                            exercise.sets.map { set ->
                                Triple(
                                    set.reps.toIntOrNull() ?: 0,
                                    set.weight.toFloatOrNull() ?: 0f,
                                    set.rpe.toFloatOrNull().takeIf { set.rpe.isNotBlank() },
                                )
                            }
                    }
                onAdd(result)
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Add Workout") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                exercises.forEachIndexed { idx, exercise ->
                    ExerciseInput(
                        exercise = exercise,
                        onRemove =
                            if (exercises.size > 1) {
                                { exercises = exercises.toMutableList().also { it.removeAt(idx) } }
                            } else {
                                null
                            },
                    )
                }
                Button(
                    onClick = { exercises = exercises + MutableExerciseInput() },
                    modifier = Modifier.padding(top = 8.dp),
                ) { Text("Add Exercise") }
            }
        },
    )
}

// Helper classes for dialog state
class MutableExerciseInput(
    var name: String = "",
    var sets: List<MutableSetInput> = listOf(MutableSetInput()),
)

class MutableSetInput(
    var reps: String = "",
    var weight: String = "",
    var rpe: String = "",
)

@Composable
private fun ExerciseInput(
    exercise: MutableExerciseInput,
    onRemove: (() -> Unit)?,
) {
    Column(Modifier.padding(8.dp)) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            OutlinedTextField(
                value = exercise.name,
                onValueChange = { exercise.name = it },
                label = { Text("Exercise Name") },
                modifier = Modifier.weight(1f),
            )
            if (onRemove != null) {
                IconButton(onClick = onRemove) { Text("-") }
            }
        }
        exercise.sets.forEachIndexed { idx, set ->
            SetInput(
                setInput = set,
                onRemove =
                    if (exercise.sets.size > 1) {
                        {
                            exercise.sets = exercise.sets.toMutableList().also { it.removeAt(idx) }
                        }
                    } else {
                        null
                    },
            )
        }
        Button(
            onClick = { exercise.sets = exercise.sets + MutableSetInput() },
            modifier = Modifier.padding(top = 4.dp),
        ) { Text("Add Set") }
    }
}

@Composable
private fun SetInput(
    setInput: MutableSetInput,
    onRemove: (() -> Unit)?,
) {
    Row(Modifier.padding(start = 8.dp, bottom = 4.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        OutlinedTextField(
            value = setInput.reps,
            onValueChange = { setInput.reps = it },
            label = { Text("Reps") },
            modifier = Modifier.width(80.dp),
        )
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(
            value = setInput.weight,
            onValueChange = { setInput.weight = it },
            label = { Text("Weight (kg)") },
            modifier = Modifier.width(100.dp),
        )
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(
            value = setInput.rpe,
            onValueChange = { setInput.rpe = it },
            label = { Text("RPE") },
            modifier = Modifier.width(80.dp),
        )
        if (onRemove != null) {
            IconButton(onClick = onRemove) { Text("-") }
        }
    }
}
