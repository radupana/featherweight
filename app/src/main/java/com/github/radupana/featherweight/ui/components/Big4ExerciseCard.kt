package com.github.radupana.featherweight.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.data.profile.OneRMType
import com.github.radupana.featherweight.util.WeightFormatter

@Composable
@Suppress("LongParameterList")
fun Big4ExerciseCard(
    exerciseName: String,
    oneRMValue: Float?,
    oneRMType: OneRMType?,
    oneRMContext: String?,
    oneRMDate: java.time.LocalDateTime?,
    sessionCount: Int,
    onAdd: () -> Unit,
    onEdit: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassmorphicCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )

                if (oneRMValue != null) {
                    Text(
                        text = WeightFormatter.formatWeightWithUnit(oneRMValue),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp),
                    )

                    if (oneRMType != null) {
                        val contextText =
                            when (oneRMType) {
                                OneRMType.MANUALLY_ENTERED -> "Manually set"
                                OneRMType.AUTOMATICALLY_CALCULATED -> {
                                    if (oneRMContext != null && oneRMDate != null) {
                                        val dateStr =
                                            oneRMDate.format(
                                                java.time.format.DateTimeFormatter
                                                    .ofPattern("MMM d"),
                                            )
                                        "$oneRMContext on $dateStr"
                                    } else {
                                        oneRMContext ?: "Calculated"
                                    }
                                }
                            }
                        Text(
                            text = contextText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )

                        Text(
                            text = "$sessionCount sessions",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                } else {
                    Text(
                        text = "No 1RM set",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            if (oneRMValue != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(start = 16.dp),
                ) {
                    FilledTonalButton(
                        onClick = onEdit,
                    ) {
                        Text("Edit")
                    }

                    OutlinedButton(
                        onClick = onClear,
                        colors =
                            ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                    ) {
                        Text("Clear")
                    }
                }
            } else {
                FilledTonalButton(
                    onClick = onAdd,
                    modifier = Modifier.padding(start = 16.dp),
                ) {
                    Text("Add")
                }
            }
        }
    }
}
