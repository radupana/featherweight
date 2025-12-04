package com.github.radupana.featherweight.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongMethod")
fun NotesInputModal(
    isVisible: Boolean,
    title: String,
    initialNotes: String,
    onNotesChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
) {
    if (isVisible) {
        Dialog(
            onDismissRequest = onDismiss,
            properties =
                DialogProperties(
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true,
                    usePlatformDefaultWidth = false,
                ),
        ) {
            Card(
                modifier =
                    modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )

                        TextButton(onClick = onDismiss) {
                            Text("Done")
                        }
                    }

                    // Notes input
                    var notes by remember { mutableStateOf(initialNotes) }
                    val maxLength = 200

                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 300.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        BasicTextField(
                            value = notes,
                            onValueChange = { newValue ->
                                if (!readOnly && newValue.length <= maxLength) {
                                    notes = newValue
                                    onNotesChanged(newValue)
                                }
                            },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                            textStyle =
                                TextStyle(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                                ),
                            keyboardOptions =
                                KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences,
                                ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            readOnly = readOnly,
                            decorationBox = { innerTextField ->
                                if (notes.isEmpty() && !readOnly) {
                                    Text(
                                        text = "Add your notes here...",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                innerTextField()
                            },
                        )
                    }

                    // Character counter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Text(
                            text = "${notes.length}/$maxLength",
                            style = MaterialTheme.typography.bodySmall,
                            color =
                                if (notes.length > maxLength * 0.9) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                    }
                }
            }
        }
    }
}
