package com.github.radupana.featherweight.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.github.radupana.featherweight.service.WeightSuggestion
import com.github.radupana.featherweight.util.WeightFormatter
import kotlin.math.roundToInt

@Composable
fun IntelligentSetInput(
    targetReps: Int,
    targetWeight: Float?,
    actualReps: Int,
    actualWeight: Float,
    actualRpe: Float?,
    weightSuggestion: WeightSuggestion?,
    onActualRepsChange: (Int) -> Unit,
    onActualWeightChange: (Float) -> Unit,
    onActualRpeChange: (Float?) -> Unit,
    onRequestSuggestion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSuggestionModal by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Target section
            if (targetWeight != null) {
                Text(
                    text = "Target: $targetReps reps @ ${WeightFormatter.formatWeightWithUnit(targetWeight)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
            } else {
                Text(
                    text = "Target: $targetReps reps",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
            }

            // Input section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Reps input with suggestion button
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = if (actualReps > 0) actualReps.toString() else "",
                        onValueChange = { value ->
                            value.toIntOrNull()?.let { onActualRepsChange(it) }
                        },
                        label = { Text("Reps") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )

                    // Suggestion button
                    if (weightSuggestion != null && weightSuggestion.weight > 0) {
                        Box(
                            modifier =
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        onRequestSuggestion()
                                        showSuggestionModal = true
                                    },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.Lightbulb,
                                contentDescription = "Weight suggestion",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }

                // Weight input
                OutlinedTextField(
                    value = if (actualWeight > 0) actualWeight.toString() else "",
                    onValueChange = { value ->
                        value.toFloatOrNull()?.let { onActualWeightChange(it) }
                    },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )

                // RPE input
                OutlinedTextField(
                    value = actualRpe?.toString() ?: "",
                    onValueChange = { value ->
                        onActualRpeChange(value.toFloatOrNull())
                    },
                    label = { Text("RPE") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(0.8f),
                    singleLine = true,
                )
            }

            // Quick suggestion preview (if available)
            if (weightSuggestion != null && weightSuggestion.weight > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Suggested: ${WeightFormatter.formatWeightWithUnit(weightSuggestion.weight)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        ConfidenceIndicator(
                            confidence = weightSuggestion.confidence,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }

    // Suggestion modal
    if (showSuggestionModal && weightSuggestion != null) {
        WeightSuggestionModal(
            suggestion = weightSuggestion,
            currentWeight = actualWeight,
            onUseWeight = { weight ->
                onActualWeightChange(weight)
                showSuggestionModal = false
            },
            onDismiss = { showSuggestionModal = false },
        )
    }
}

@Composable
private fun ConfidenceIndicator(
    confidence: Float,
    modifier: Modifier = Modifier,
) {
    val dots = 4
    val filledDots = (confidence * dots).roundToInt()

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        repeat(dots) { index ->
            Box(
                modifier =
                    Modifier
                        .size(3.dp)
                        .clip(CircleShape)
                        .background(
                            if (index < filledDots) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            },
                        ),
            )
        }
    }
}

@Composable
private fun WeightSuggestionModal(
    suggestion: WeightSuggestion,
    currentWeight: Float,
    onUseWeight: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(8.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Weight Suggestion",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )

                    ConfidenceIndicator(
                        confidence = suggestion.confidence,
                        modifier = Modifier.size(20.dp),
                    )
                }

                // Main suggestion
                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = WeightFormatter.formatWeightWithUnit(suggestion.weight),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )

                        val confidenceText =
                            when {
                                suggestion.confidence > 0.8f -> "High confidence"
                                suggestion.confidence > 0.6f -> "Moderate confidence"
                                suggestion.confidence > 0.4f -> "Low confidence"
                                else -> "Very low confidence"
                            }

                        Text(
                            text = confidenceText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                }

                // Explanation
                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = suggestion.explanation,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp),
                    )
                }

                // Alternative weights
                if (suggestion.alternativeWeights.isNotEmpty()) {
                    Column {
                        Text(
                            text = "Try different reps:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            suggestion.alternativeWeights.entries.take(3).forEach { (reps, weight) ->
                                OutlinedButton(
                                    onClick = { onUseWeight(weight) },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(
                                        text = "$reps→${WeightFormatter.formatWeightWithUnit(weight)}",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Keep ${WeightFormatter.formatWeightWithUnit(currentWeight)}")
                    }

                    Button(
                        onClick = { onUseWeight(suggestion.weight) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Use ${WeightFormatter.formatWeightWithUnit(suggestion.weight)}")
                    }
                }
            }
        }
    }
}
