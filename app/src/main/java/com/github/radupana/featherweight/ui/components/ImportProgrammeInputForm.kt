package com.github.radupana.featherweight.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
@Suppress("LongParameterList")
fun ImportProgrammeInputForm(
    inputText: String,
    error: String?,
    successMessage: String?,
    editingFailedRequestId: String?,
    onTextChange: (String, String?) -> Unit,
    onShowFormatTips: () -> Unit,
    onParseProgramme: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header card with format tips button
        ImportHeader(
            onShowFormatTips = onShowFormatTips,
        )

        ProgrammeTextInput(
            text = inputText,
            onTextChange = { newText ->
                // Preserve the editing request ID when updating text
                onTextChange(newText, editingFailedRequestId)
            },
        )

        error?.let { errorMessage ->
            ImportErrorCard(errorMessage = errorMessage)
        }

        successMessage?.let { message ->
            ImportSuccessCard(successMessage = message)
        }

        Button(
            onClick = onParseProgramme,
            enabled = inputText.length >= 50 && inputText.length <= 10000,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(56.dp),
        ) {
            Text(
                text = "Parse Programme",
                style = MaterialTheme.typography.titleMedium,
            )
        }

        if (inputText.isNotEmpty() && inputText.length < 50) {
            Text(
                text = "Minimum 50 characters needed (${inputText.length}/50)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
