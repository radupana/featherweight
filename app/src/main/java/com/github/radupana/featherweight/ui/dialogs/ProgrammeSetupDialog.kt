package com.github.radupana.featherweight.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.radupana.featherweight.data.programme.ProgrammeTemplate
import com.github.radupana.featherweight.viewmodel.ProgrammeUiState
import com.github.radupana.featherweight.viewmodel.ProgrammeViewModel
import com.github.radupana.featherweight.viewmodel.ProfileViewModel
import com.github.radupana.featherweight.viewmodel.SetupStep
import com.github.radupana.featherweight.viewmodel.UserMaxes

@Composable
fun ProgrammeSetupDialog(
    template: ProgrammeTemplate,
    uiState: ProgrammeUiState,
    viewModel: ProgrammeViewModel,
    profileViewModel: ProfileViewModel,
    onProgrammeCreated: (() -> Unit)? = null,
) {
    val userMaxes by viewModel.userMaxes.collectAsState()
    val profileUiState by profileViewModel.uiState.collectAsState()
    var customName by remember { mutableStateOf("") }

    // Auto-populate 1RM values from profile when dialog opens
    LaunchedEffect(template.requiresMaxes) {
        if (template.requiresMaxes && userMaxes.squat == null && userMaxes.bench == null && 
            userMaxes.deadlift == null && userMaxes.ohp == null) {
            val currentMaxes = profileUiState.currentMaxes
            val squatMax = currentMaxes.find { it.exerciseName == "Back Squat" }?.maxWeight
            val benchMax = currentMaxes.find { it.exerciseName == "Bench Press" }?.maxWeight
            val deadliftMax = currentMaxes.find { it.exerciseName == "Conventional Deadlift" }?.maxWeight
            val ohpMax = currentMaxes.find { it.exerciseName == "Overhead Press" }?.maxWeight
            
            // Update all values at once to ensure proper validation
            viewModel.updateUserMaxes(
                UserMaxes(
                    squat = squatMax,
                    bench = benchMax,
                    deadlift = deadliftMax,
                    ohp = ohpMax
                )
            )
        }
    }

    Dialog(
        onDismissRequest = { viewModel.dismissSetupDialog() },
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
            ),
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.85f),
            elevation = CardDefaults.cardElevation(8.dp),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (uiState.setupStep != SetupStep.MAXES_INPUT && template.requiresMaxes) {
                        IconButton(onClick = { viewModel.previousSetupStep() }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(48.dp))
                    }

                    Text(
                        text = "Setup Programme",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )

                    IconButton(onClick = { viewModel.dismissSetupDialog() }) {
                        Icon(Icons.Filled.Check, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Progress indicator
                if (template.requiresMaxes || template.allowsAccessoryCustomization) {
                    SetupProgressIndicator(
                        currentStep = uiState.setupStep,
                        template = template,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Content based on setup step
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 100.dp), // Add bottom padding for keyboard
                ) {
                    item {
                        when (uiState.setupStep) {
                            SetupStep.MAXES_INPUT -> {
                                if (template.requiresMaxes) {
                                    MaxesInputStep(
                                        template = template,
                                        userMaxes = userMaxes,
                                        onMaxesUpdate = viewModel::updateUserMaxes,
                                        profileMaxes = profileUiState.currentMaxes,
                                    )
                                } else {
                                    ConfirmationStep(
                                        template = template,
                                        customName = customName,
                                        onNameChange = { customName = it },
                                        userMaxes = userMaxes,
                                    )
                                }
                            }
                            SetupStep.ACCESSORY_SELECTION -> {
                                AccessorySelectionStep(template = template)
                            }
                            SetupStep.CONFIRMATION -> {
                                ConfirmationStep(
                                    template = template,
                                    customName = customName,
                                    onNameChange = { customName = it },
                                    userMaxes = userMaxes,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = { viewModel.dismissSetupDialog() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (uiState.setupStep == SetupStep.CONFIRMATION) {
                                viewModel.createProgrammeFromTemplate(
                                    customName = customName.takeIf { it.isNotBlank() },
                                    onSuccess = {
                                        onProgrammeCreated?.invoke()
                                    },
                                )
                            } else {
                                viewModel.nextSetupStep()
                            }
                        },
                        enabled =
                            when (uiState.setupStep) {
                                SetupStep.MAXES_INPUT -> {
                                    if (template.requiresMaxes) {
                                        val currentMaxes = profileUiState.currentMaxes
                                        val effectiveSquat = userMaxes.squat ?: currentMaxes.find { it.exerciseName == "Back Squat" }?.maxWeight
                                        val effectiveBench = userMaxes.bench ?: currentMaxes.find { it.exerciseName == "Bench Press" }?.maxWeight
                                        val effectiveDeadlift = userMaxes.deadlift ?: currentMaxes.find { it.exerciseName == "Conventional Deadlift" }?.maxWeight
                                        val effectiveOhp = userMaxes.ohp ?: currentMaxes.find { it.exerciseName == "Overhead Press" }?.maxWeight
                                        
                                        effectiveSquat != null && effectiveSquat > 0 &&
                                        effectiveBench != null && effectiveBench > 0 &&
                                        effectiveDeadlift != null && effectiveDeadlift > 0 &&
                                        effectiveOhp != null && effectiveOhp > 0
                                    } else {
                                        true
                                    }
                                }
                                else -> true
                            } && !uiState.isCreating,
                        modifier = Modifier.weight(1f),
                    ) {
                        if (uiState.isCreating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text(
                                if (uiState.setupStep == SetupStep.CONFIRMATION) "Create" else "Next",
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupProgressIndicator(
    currentStep: SetupStep,
    template: ProgrammeTemplate,
) {
    val steps = mutableListOf<String>()
    if (template.requiresMaxes) steps.add("1RM Input")
    if (template.allowsAccessoryCustomization) steps.add("Accessories")
    steps.add("Confirm")

    val currentIndex =
        when (currentStep) {
            SetupStep.MAXES_INPUT -> 0
            SetupStep.ACCESSORY_SELECTION -> if (template.requiresMaxes) 1 else 0
            SetupStep.CONFIRMATION -> steps.size - 1
        }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        steps.forEachIndexed { index, step ->
            val isActive = index == currentIndex
            val isCompleted = index < currentIndex

            // Step indicator
            Surface(
                modifier = Modifier.size(32.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color =
                    when {
                        isCompleted -> MaterialTheme.colorScheme.primary
                        isActive -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isCompleted) "✓" else (index + 1).toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color =
                            when {
                                isCompleted || isActive -> MaterialTheme.colorScheme.onPrimary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            if (index < steps.size - 1) {
                Divider(
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(2.dp),
                    color =
                        if (isCompleted) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = steps[currentIndex],
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun MaxesInputStep(
    template: ProgrammeTemplate,
    userMaxes: UserMaxes,
    onMaxesUpdate: (UserMaxes) -> Unit,
    profileMaxes: List<com.github.radupana.featherweight.data.profile.ExerciseMaxWithName>,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Enter Your 1RM Values",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = "${template.name} uses percentage-based training. Enter your current 1-rep max for each lift in kg:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Squat 1RM with robust input handling
        val squatProfileMax = profileMaxes.find { it.exerciseName == "Back Squat" }
        val effectiveSquatValue = userMaxes.squat ?: squatProfileMax?.maxWeight
        WeightInputField(
            value = userMaxes.squat,
            onValueChange = { squat -> onMaxesUpdate(userMaxes.copy(squat = squat)) },
            label = "Back Squat 1RM (kg)",
            isError = effectiveSquatValue == null || effectiveSquatValue <= 0,
            profileValue = squatProfileMax?.maxWeight,
        )

        // Bench 1RM with robust input handling
        val benchProfileMax = profileMaxes.find { it.exerciseName == "Bench Press" }
        val effectiveBenchValue = userMaxes.bench ?: benchProfileMax?.maxWeight
        WeightInputField(
            value = userMaxes.bench,
            onValueChange = { bench -> onMaxesUpdate(userMaxes.copy(bench = bench)) },
            label = "Bench Press 1RM (kg)",
            isError = effectiveBenchValue == null || effectiveBenchValue <= 0,
            profileValue = benchProfileMax?.maxWeight,
        )

        // Deadlift 1RM with robust input handling
        val deadliftProfileMax = profileMaxes.find { it.exerciseName == "Conventional Deadlift" }
        val effectiveDeadliftValue = userMaxes.deadlift ?: deadliftProfileMax?.maxWeight
        WeightInputField(
            value = userMaxes.deadlift,
            onValueChange = { deadlift -> onMaxesUpdate(userMaxes.copy(deadlift = deadlift)) },
            label = "Deadlift 1RM (kg)",
            isError = effectiveDeadliftValue == null || effectiveDeadliftValue <= 0,
            profileValue = deadliftProfileMax?.maxWeight,
        )

        // OHP 1RM with robust input handling
        val ohpProfileMax = profileMaxes.find { it.exerciseName == "Overhead Press" }
        val effectiveOhpValue = userMaxes.ohp ?: ohpProfileMax?.maxWeight
        WeightInputField(
            value = userMaxes.ohp,
            onValueChange = { ohp -> onMaxesUpdate(userMaxes.copy(ohp = ohp)) },
            label = "Overhead Press 1RM (kg)",
            isError = effectiveOhpValue == null || effectiveOhpValue <= 0,
            profileValue = ohpProfileMax?.maxWeight,
        )

        Card(
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                Text(
                    text = "💡 Tip",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Don't know your 1RM? Use 90-95% of the heaviest weight you can lift for 3-5 reps with good form.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun AccessorySelectionStep(template: ProgrammeTemplate) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Customize Accessories",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = "This programme includes customizable accessory exercises. You can modify these during workouts:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Card(
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Customizable Categories:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )

                listOf(
                    "Push Accessories (chest, shoulders, triceps)",
                    "Pull Accessories (back, biceps)",
                    "Leg Accessories (quads, hamstrings, calves)",
                    "Core & Conditioning",
                ).forEach { category ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        Text(
                            text = category,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }

        Card(
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                Text(
                    text = "🔄 Exercise Swapping",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "You can substitute any accessory exercise during your workout if you don't have the equipment or prefer a different movement.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun ConfirmationStep(
    template: ProgrammeTemplate,
    customName: String,
    onNameChange: (String) -> Unit,
    userMaxes: UserMaxes,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Ready to Start",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )

        // Custom name input
        OutlinedTextField(
            value = customName,
            onValueChange = onNameChange,
            label = { Text("Programme Name (Optional)") },
            placeholder = { Text(template.name) },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Programme summary
        Card(
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Programme Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                ProgrammeSummaryRow("Template", template.name)
                ProgrammeSummaryRow("Author", template.author)
                ProgrammeSummaryRow("Duration", "${template.durationWeeks} weeks")
                ProgrammeSummaryRow("Difficulty", template.difficulty.name.lowercase().capitalize())

                if (template.requiresMaxes && userMaxes.isValid(true)) {
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "Your 1RM Values",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    userMaxes.squat?.let { ProgrammeSummaryRow("Squat", "${it.toInt()}kg") }
                    userMaxes.bench?.let { ProgrammeSummaryRow("Bench", "${it.toInt()}kg") }
                    userMaxes.deadlift?.let { ProgrammeSummaryRow("Deadlift", "${it.toInt()}kg") }
                    userMaxes.ohp?.let { ProgrammeSummaryRow("OHP", "${it.toInt()}kg") }
                }
            }
        }

        Card(
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                Text(
                    text = "🚀 Ready to begin!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "This programme will become your active training plan. You can view your progress and modify workouts from the Programmes tab.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun ProgrammeSummaryRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun WeightInputField(
    value: Float?,
    onValueChange: (Float?) -> Unit,
    label: String,
    isError: Boolean = false,
    profileValue: Float? = null,
) {
    // Keep text field value separate to avoid conversion loops
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }

    // Only set initial value once when first composed - prefer passed value, fallback to profile
    LaunchedEffect(Unit) {
        val initialValue = value ?: profileValue
        if (initialValue != null && initialValue > 0) {
            val initialText = if (initialValue % 1 == 0f) initialValue.toInt().toString() else initialValue.toString()
            textFieldValue = TextFieldValue(initialText, TextRange(initialText.length))
            // If we're using profile value and current value is null, update the parent state
            if (value == null && profileValue != null) {
                onValueChange(profileValue)
            }
        }
    }

    OutlinedTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            val text = newValue.text
            android.util.Log.d("WeightInput", "onValueChange - text: '$text', old text: '${textFieldValue.text}'")

            if (text.isEmpty()) {
                textFieldValue = newValue
                onValueChange(null)
            } else {
                // Same validation logic as working set weight input
                val parts = text.split(".")
                val isValid =
                    when (parts.size) {
                        1 -> parts[0].all { it.isDigit() } && parts[0].length <= 4
                        2 ->
                            parts[0].all { it.isDigit() } &&
                                parts[0].length <= 4 &&
                                parts[1].all { it.isDigit() } &&
                                parts[1].length <= 2
                        else -> false
                    }
                if (isValid) {
                    textFieldValue = newValue
                    val floatValue = text.toFloatOrNull()
                    android.util.Log.d("WeightInput", "Valid input: '$text' -> $floatValue")
                    onValueChange(floatValue)
                }
            }
        },
        label = { Text(label) },
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next,
            ),
        modifier =
            Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (focusState.isFocused && textFieldValue.text.isNotEmpty()) {
                        // Select all text for easy replacement
                        val text = textFieldValue.text
                        textFieldValue = textFieldValue.copy(selection = TextRange(0, text.length))
                    }
                },
        isError = isError,
        singleLine = true,
        supportingText = if (profileValue != null && profileValue > 0) {
            {
                Text(
                    text = "From profile: ${if (profileValue % 1 == 0f) profileValue.toInt() else profileValue}kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else null,
    )
}
