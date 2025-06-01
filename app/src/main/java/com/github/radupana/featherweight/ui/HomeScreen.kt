package com.github.radupana.featherweight.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onFreestyle: () -> Unit,
    onTemplate: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Text(
                "Featherweight",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Welcome to Featherweight! Let's get started.",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(48.dp))
            Button(
                onClick = onFreestyle,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp),
            ) {
                Text("Start Freestyle Workout")
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onTemplate,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp),
            ) {
                Text("Start From Template")
            }
        }
    }
}
