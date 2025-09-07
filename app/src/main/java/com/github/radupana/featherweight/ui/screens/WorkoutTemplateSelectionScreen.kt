package com.github.radupana.featherweight.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.ui.components.GlassmorphicCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutTemplateSelectionScreen(
    onTemplateSelected: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Choose Template") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Select a workout template",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            TemplateCard(
                name = "Push",
                description = "Chest, Shoulders, Triceps",
                details = "Focus on pressing movements and pushing exercises",
                onClick = { onTemplateSelected("PUSH") },
            )

            TemplateCard(
                name = "Pull",
                description = "Back, Biceps",
                details = "Emphasize pulling movements and back development",
                onClick = { onTemplateSelected("PULL") },
            )

            TemplateCard(
                name = "Legs",
                description = "Quads, Hamstrings, Glutes, Calves",
                details = "Complete lower body workout for strength and size",
                onClick = { onTemplateSelected("LEGS") },
            )

            TemplateCard(
                name = "Upper",
                description = "All upper body muscles",
                details = "Comprehensive upper body training session",
                onClick = { onTemplateSelected("UPPER") },
            )

            TemplateCard(
                name = "Lower",
                description = "All lower body muscles",
                details = "Full lower body development workout",
                onClick = { onTemplateSelected("LOWER") },
            )

            TemplateCard(
                name = "Full Body",
                description = "Balanced mix of all muscle groups",
                details = "Total body workout for overall development",
                onClick = { onTemplateSelected("FULL BODY") },
            )
        }
    }
}

@Composable
private fun TemplateCard(
    name: String,
    description: String,
    details: String,
    onClick: () -> Unit,
) {
    GlassmorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        }
    }
}
