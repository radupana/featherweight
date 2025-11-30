package com.github.radupana.featherweight.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.ui.theme.CardColors
import com.github.radupana.featherweight.util.WeightFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList")
fun CleanSetRow(
    set: SetLog,
    setNumber: Int,
    oneRMEstimate: Float?,
    isProgrammeWorkout: Boolean,
    onUpdateSet: (Int, Float, Float?) -> Unit,
    onToggleCompleted: (Boolean) -> Unit,
    onDeleteSet: () -> Unit,
    canMarkComplete: Boolean,
    readOnly: Boolean,
) {
    val showTargetValues = isProgrammeWorkout
    val dismissState = createSetRowDismissState(readOnly, onDeleteSet)

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            SetRowDeleteBackground(dismissState)
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = !readOnly,
    ) {
        SetRowContent(
            set = set,
            setNumber = setNumber,
            oneRMEstimate = oneRMEstimate,
            showTargetValues = showTargetValues,
            onUpdateSet = onUpdateSet,
            onToggleCompleted = onToggleCompleted,
            canMarkComplete = canMarkComplete,
            readOnly = readOnly,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun createSetRowDismissState(
    readOnly: Boolean,
    onDeleteSet: () -> Unit,
): SwipeToDismissBoxState =
    rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    if (!readOnly) {
                        onDeleteSet()
                        true
                    } else {
                        false
                    }
                }
                else -> false
            }
        },
        positionalThreshold = { totalDistance ->
            totalDistance * 0.33f
        },
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetRowDeleteBackground(dismissState: SwipeToDismissBoxState) {
    if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
        val progress = dismissState.progress.coerceIn(0f, 1f)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterEnd,
        ) {
            val minWidth = 40.dp
            val maxAdditionalWidth = 160.dp
            val currentWidth = minWidth + (maxAdditionalWidth.value * progress).dp

            Surface(
                modifier =
                    Modifier
                        .width(currentWidth)
                        .fillMaxHeight()
                        .padding(vertical = 2.dp),
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
                shape =
                    RoundedCornerShape(
                        topStart = 8.dp,
                        bottomStart = 8.dp,
                        topEnd = 0.dp,
                        bottomEnd = 0.dp,
                    ),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    val iconOffset = ((1 - progress) * 15).dp
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint =
                            MaterialTheme.colorScheme.onError.copy(
                                alpha = 0.6f + (0.4f * progress),
                            ),
                        modifier =
                            Modifier
                                .size((20 + (4 * progress)).dp)
                                .offset(x = iconOffset),
                    )
                }
            }
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun SetRowContent(
    set: SetLog,
    setNumber: Int,
    oneRMEstimate: Float?,
    showTargetValues: Boolean,
    onUpdateSet: (Int, Float, Float?) -> Unit,
    onToggleCompleted: (Boolean) -> Unit,
    canMarkComplete: Boolean,
    readOnly: Boolean,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(CardColors.gradientBottom),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            SetNumberColumn(setNumber)

            WeightInputColumn(
                set = set,
                oneRMEstimate = oneRMEstimate,
                showTargetValues = showTargetValues,
                onUpdateSet = onUpdateSet,
                readOnly = readOnly,
            )

            RepsInputColumn(
                set = set,
                showTargetValues = showTargetValues,
                onUpdateSet = onUpdateSet,
                readOnly = readOnly,
            )

            RPEInputColumn(
                set = set,
                showTargetValues = showTargetValues,
                onUpdateSet = onUpdateSet,
                readOnly = readOnly,
            )

            CompletionCheckboxColumn(
                set = set,
                onToggleCompleted = onToggleCompleted,
                canMarkComplete = canMarkComplete,
                readOnly = readOnly,
            )
        }
    }
}

@Composable
private fun SetNumberColumn(setNumber: Int) {
    Box(
        modifier =
            Modifier
                .width(32.dp)
                .height(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$setNumber",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun RowScope.WeightInputColumn(
    set: SetLog,
    oneRMEstimate: Float?,
    showTargetValues: Boolean,
    onUpdateSet: (Int, Float, Float?) -> Unit,
    readOnly: Boolean,
) {
    Box(
        modifier = Modifier.weight(1f),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            WeightInputField(
                set = set,
                showTargetValues = showTargetValues,
                onUpdateSet = onUpdateSet,
                readOnly = readOnly,
            )

            OneRMPercentageLabel(
                set = set,
                oneRMEstimate = oneRMEstimate,
            )
        }
    }
}

@Composable
private fun WeightInputField(
    set: SetLog,
    showTargetValues: Boolean,
    onUpdateSet: (Int, Float, Float?) -> Unit,
    readOnly: Boolean,
) {
    Box(
        modifier = Modifier.height(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        var weightValue by remember(set.id) {
            mutableStateOf(
                TextFieldValue(
                    text = if (set.actualWeight > 0) WeightFormatter.formatWeight(set.actualWeight) else "",
                    selection = TextRange.Zero,
                ),
            )
        }

        val weightPlaceholder =
            if (showTargetValues && set.targetWeight != null && set.targetWeight > 0) {
                WeightFormatter.formatWeight(set.targetWeight)
            } else {
                ""
            }

        CenteredInputField(
            value = weightValue,
            onValueChange = { newValue ->
                if (!set.isCompleted && !readOnly) {
                    weightValue = newValue
                    val parsedWeight = WeightFormatter.parseUserInput(newValue.text)
                    onUpdateSet(set.actualReps, parsedWeight, set.actualRpe)
                }
            },
            fieldType = InputFieldType.WEIGHT,
            placeholder = weightPlaceholder,
            modifier =
                Modifier
                    .fillMaxSize()
                    .alpha(if (set.isCompleted) 0.6f else 1f),
            enabled = !set.isCompleted && !readOnly,
            readOnly = set.isCompleted || readOnly,
        )
    }
}

@Composable
private fun OneRMPercentageLabel(
    set: SetLog,
    oneRMEstimate: Float?,
) {
    Box(
        modifier = Modifier.height(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        val displayWeight = if (set.actualWeight > 0) set.actualWeight else set.targetWeight ?: 0f
        if (oneRMEstimate != null && oneRMEstimate > 0 && displayWeight > 0) {
            val percentage = ((displayWeight / oneRMEstimate) * 100).toInt()
            Text(
                text = "$percentage% of 1RM",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
private fun RowScope.RepsInputColumn(
    set: SetLog,
    showTargetValues: Boolean,
    onUpdateSet: (Int, Float, Float?) -> Unit,
    readOnly: Boolean,
) {
    Box(
        modifier = Modifier.weight(0.7f),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Box(
                modifier = Modifier.height(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                var repsValue by remember(set.id) {
                    mutableStateOf(
                        TextFieldValue(
                            text = if (set.actualReps > 0) set.actualReps.toString() else "",
                            selection = TextRange.Zero,
                        ),
                    )
                }

                val repsPlaceholder =
                    if (showTargetValues && set.targetReps != null && set.targetReps > 0) {
                        set.targetReps.toString()
                    } else {
                        ""
                    }

                CenteredInputField(
                    value = repsValue,
                    onValueChange = { newValue ->
                        if (!set.isCompleted && !readOnly) {
                            repsValue = newValue
                            val parsedReps = newValue.text.toIntOrNull() ?: 0
                            onUpdateSet(parsedReps, set.actualWeight, set.actualRpe)
                        }
                    },
                    fieldType = InputFieldType.REPS,
                    placeholder = repsPlaceholder,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .alpha(if (set.isCompleted) 0.6f else 1f),
                    enabled = !set.isCompleted && !readOnly,
                    readOnly = set.isCompleted || readOnly,
                )
            }
            Box(
                modifier = Modifier.height(20.dp),
            )
        }
    }
}

@Composable
private fun RowScope.RPEInputColumn(
    set: SetLog,
    showTargetValues: Boolean,
    onUpdateSet: (Int, Float, Float?) -> Unit,
    readOnly: Boolean,
) {
    Box(
        modifier = Modifier.weight(0.7f),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Box(
                modifier = Modifier.height(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                var rpeValue by remember(set.id) {
                    mutableStateOf(
                        TextFieldValue(
                            text = set.actualRpe?.toString() ?: "",
                            selection = TextRange.Zero,
                        ),
                    )
                }

                val rpePlaceholder =
                    if (showTargetValues && set.targetRpe != null && set.targetRpe > 0) {
                        set.targetRpe.toString()
                    } else {
                        ""
                    }

                CenteredInputField(
                    value = rpeValue,
                    onValueChange = { newValue ->
                        if (!set.isCompleted && !readOnly) {
                            rpeValue = newValue
                            val parsedRpe =
                                newValue.text.toFloatOrNull()?.let { value ->
                                    WeightFormatter.roundRPE(value)
                                }
                            onUpdateSet(set.actualReps, set.actualWeight, parsedRpe)
                        }
                    },
                    fieldType = InputFieldType.RPE,
                    placeholder = rpePlaceholder,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .alpha(if (set.isCompleted) 0.6f else 1f),
                    enabled = !set.isCompleted && !readOnly,
                    readOnly = set.isCompleted || readOnly,
                )
            }
            Box(
                modifier = Modifier.height(20.dp),
            )
        }
    }
}

@Composable
private fun CompletionCheckboxColumn(
    set: SetLog,
    onToggleCompleted: (Boolean) -> Unit,
    canMarkComplete: Boolean,
    readOnly: Boolean,
) {
    Box(
        modifier =
            Modifier
                .width(48.dp)
                .height(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clickable(
                        enabled = !readOnly && (canMarkComplete || set.isCompleted),
                        onClick = {
                            val newChecked = !set.isCompleted
                            if (!newChecked || canMarkComplete) {
                                onToggleCompleted(newChecked)
                            }
                        },
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Checkbox(
                checked = set.isCompleted,
                onCheckedChange = null,
                enabled = !readOnly && (canMarkComplete || set.isCompleted),
                colors =
                    CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                    ),
            )
        }
    }
}
