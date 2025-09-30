package com.github.radupana.featherweight.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.ui.components.Big4ExerciseCard
import com.github.radupana.featherweight.ui.components.GlassmorphicCard
import com.github.radupana.featherweight.ui.components.OtherExerciseCard
import com.github.radupana.featherweight.ui.components.SubSection
import com.github.radupana.featherweight.viewmodel.Big4Exercise
import com.github.radupana.featherweight.viewmodel.ExerciseMaxWithName
import com.github.radupana.featherweight.viewmodel.ProfileUiState

@Composable
fun OneRMTab(
    uiState: ProfileUiState,
    onToggleBig4SubSection: () -> Unit,
    onToggleOtherSubSection: () -> Unit,
    onAddMax: (Big4Exercise) -> Unit,
    onEditMax: (Big4Exercise) -> Unit,
    onClearMax: (String) -> Unit,
    onShowExerciseSelector: () -> Unit,
    onEditOtherMax: (ExerciseMaxWithName) -> Unit,
    onDeleteMax: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SubSection(
            title = "Big Four",
            isExpanded = uiState.isBig4SubSectionExpanded,
            onToggle = onToggleBig4SubSection,
        ) {
            uiState.big4Exercises.forEach { exercise ->
                Big4ExerciseCard(
                    exerciseName = getDisplayName(exercise.exerciseName),
                    oneRMValue = exercise.oneRMValue,
                    oneRMType = exercise.oneRMType,
                    oneRMContext = exercise.oneRMContext,
                    oneRMDate = exercise.oneRMDate,
                    sessionCount = exercise.sessionCount,
                    onAdd = { onAddMax(exercise) },
                    onEdit = { onEditMax(exercise) },
                    onClear = { onClearMax(exercise.exerciseId) },
                )
            }
        }

        if (uiState.otherExercises.isNotEmpty()) {
            SubSection(
                title = "Other",
                isExpanded = uiState.isOtherSubSectionExpanded,
                onToggle = onToggleOtherSubSection,
                showDivider = true,
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    IconButton(onClick = onShowExerciseSelector) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Add exercise",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.otherExercises.forEach { max ->
                        OtherExerciseCard(
                            exerciseName = max.exerciseName,
                            oneRMValue = max.oneRMEstimate,
                            oneRMType = max.oneRMType,
                            oneRMContext = max.oneRMContext,
                            oneRMDate = max.oneRMDate,
                            sessionCount = max.sessionCount,
                            onEdit = { onEditOtherMax(max) },
                            onDelete = { onDeleteMax(max.exerciseId) },
                        )
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.height(8.dp))
            GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Track More Exercises",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Add 1RM records for other exercises you want to track",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FilledTonalButton(onClick = onShowExerciseSelector) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Exercise")
                    }
                }
            }
        }
    }
}

@Composable
private fun getDisplayName(exerciseName: String): String =
    when (exerciseName) {
        "Barbell Back Squat" -> "Squat"
        "Barbell Deadlift" -> "Deadlift"
        "Barbell Bench Press" -> "Bench Press"
        "Barbell Overhead Press" -> "Overhead Press"
        else -> exerciseName
    }
