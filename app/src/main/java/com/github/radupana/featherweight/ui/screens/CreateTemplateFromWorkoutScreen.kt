package com.github.radupana.featherweight.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.radupana.featherweight.viewmodel.CreateTemplateFromWorkoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTemplateFromWorkoutScreen(
    workoutId: String,
    onBack: () -> Unit,
    onTemplateCreated: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateTemplateFromWorkoutViewModel = viewModel(),
) {
    val templateName by viewModel.templateName.collectAsState()
    val templateDescription by viewModel.templateDescription.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()

    LaunchedEffect(workoutId) {
        android.util.Log.i("CreateTemplateScreen", "LaunchedEffect(workoutId) - workoutId: $workoutId")
        viewModel.initialize(workoutId)
    }

    LaunchedEffect(saveSuccess) {
        android.util.Log.i("CreateTemplateScreen", "LaunchedEffect(saveSuccess) - saveSuccess: $saveSuccess")
        if (viewModel.isReadyForNavigation()) {
            android.util.Log.i("CreateTemplateScreen", "Ready for navigation (initialized + saveSuccess), calling onTemplateCreated")
            viewModel.consumeNavigationEvent()
            onTemplateCreated()
        } else {
            android.util.Log.i("CreateTemplateScreen", "Not ready for navigation - either not initialized or saveSuccess is stale")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Save as Template") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Give your template a name and it will be ready to use for future workouts.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = templateName,
                onValueChange = viewModel::updateTemplateName,
                label = { Text("Template Name *") },
                placeholder = { Text("e.g., Push Day, Leg Day") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = templateDescription,
                onValueChange = viewModel::updateTemplateDescription,
                label = { Text("Description (optional)") },
                placeholder = { Text("e.g., Chest, shoulders, and triceps") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 3,
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = viewModel::saveTemplate,
                modifier = Modifier.fillMaxWidth(),
                enabled = templateName.isNotBlank() && !isSaving,
            ) {
                Text(if (isSaving) "Creating Template..." else "Create Template")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
