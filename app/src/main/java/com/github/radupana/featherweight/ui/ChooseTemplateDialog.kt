package com.github.radupana.featherweight.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun ChooseTemplateDialog(
    onClose: () -> Unit,
    onTemplateSelected: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = {
            Button(onClick = { onTemplateSelected("Coming soon") }) {
                Text("OK")
            }
        },
        title = { Text("Choose a Template") },
        text = { Text("Template selection is coming soon!") },
    )
}
