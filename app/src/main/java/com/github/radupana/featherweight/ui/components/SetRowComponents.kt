package com.github.radupana.featherweight.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.util.WeightFormatter
import kotlinx.coroutines.launch

private const val DELETE_ICON_SIZE_DP = 48
private const val SWIPE_THRESHOLD_FRACTION = 0.3f
private const val ICON_VISIBILITY_THRESHOLD = 0.5f

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
    val density = LocalDensity.current
    val maxSwipePx = with(density) { DELETE_ICON_SIZE_DP.dp.toPx() }

    // Use mutableFloatStateOf for drag (no coroutine needed), Animatable for snap animation
    var isDragging by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val animatedOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    // Use drag offset while dragging, animated offset otherwise
    val currentOffset = if (isDragging) dragOffset else animatedOffset.value
    val revealedWidth = (-currentOffset).coerceIn(0f, maxSwipePx)
    val revealedWidthDp = with(density) { revealedWidth.toDp() }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(
                    if (!readOnly) {
                        Modifier.pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragStart = {
                                    isDragging = true
                                    dragOffset = animatedOffset.value
                                },
                                onDragEnd = {
                                    isDragging = false
                                    val shouldReveal = dragOffset < -maxSwipePx * SWIPE_THRESHOLD_FRACTION
                                    scope.launch {
                                        animatedOffset.snapTo(dragOffset)
                                        animatedOffset.animateTo(if (shouldReveal) -maxSwipePx else 0f)
                                    }
                                },
                                onDragCancel = {
                                    isDragging = false
                                    scope.launch {
                                        animatedOffset.snapTo(dragOffset)
                                        animatedOffset.animateTo(0f)
                                    }
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    dragOffset = (dragOffset + dragAmount).coerceIn(-maxSwipePx, 0f)
                                },
                            )
                        }
                    } else {
                        Modifier
                    },
                ),
        verticalAlignment = Alignment.Top,
    ) {
        // Row content takes remaining space, shrinks as delete icon is revealed
        Box(modifier = Modifier.weight(1f)) {
            SetRowContent(
                set = set,
                setNumber = setNumber,
                oneRMEstimate = oneRMEstimate,
                showTargetValues = isProgrammeWorkout,
                onUpdateSet = onUpdateSet,
                onToggleCompleted = onToggleCompleted,
                canMarkComplete = canMarkComplete,
                readOnly = readOnly,
            )
        }

        // Delete icon - grows from 0 width as user swipes
        if (!readOnly && revealedWidth > 0f) {
            Box(
                modifier =
                    Modifier
                        .width(revealedWidthDp)
                        .height(DELETE_ICON_SIZE_DP.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Only show icon when enough space is revealed
                if (revealedWidth > maxSwipePx * ICON_VISIBILITY_THRESHOLD) {
                    IconButton(
                        onClick = {
                            scope.launch { animatedOffset.animateTo(0f) }
                            onDeleteSet()
                        },
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete set",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp),
                        )
                    }
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
        modifier = Modifier.fillMaxWidth(),
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
                    // Show "0" for bodyweight exercises where actualWeight is pre-populated from target
                    text =
                        if (set.actualWeight > 0 || (set.actualWeight == 0f && set.targetWeight == 0f)) {
                            WeightFormatter.formatWeight(set.actualWeight)
                        } else {
                            ""
                        },
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
