package com.github.radupana.featherweight.ui.dialogs

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.utils.ExportHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
@Suppress("LongMethod")
fun ExportShareDialog(
    filePath: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val exportHandler = remember { ExportHandler(context) }
    val file = remember { File(filePath) }
    val fileSizeKB = remember { (file.length() / 1024).toInt() }

    var showCopiedMessage by remember { mutableStateOf(false) }
    var savingFile by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Launcher for document creation
    val createDocumentLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/json"),
        ) { uri ->
            uri?.let {
                savingFile = true
                coroutineScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            context.contentResolver.openOutputStream(it)?.use { outputStream ->
                                file.inputStream().use { inputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                            }
                        }
                        Toast.makeText(context, "File saved successfully", Toast.LENGTH_LONG).show()
                        onDismiss()
                    } catch (e: java.io.IOException) {
                        Toast.makeText(context, "Failed to save file: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        savingFile = false
                    }
                }
            }
        }

    AlertDialog(
        onDismissRequest = { if (!savingFile) onDismiss() },
        icon = {
            Icon(Icons.Default.Share, contentDescription = "Export complete")
        },
        title = {
            Text(
                "Export Complete",
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    "Your workout data has been exported successfully.",
                    style = MaterialTheme.typography.bodyMedium,
                )

                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                file.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "Size: $fileSizeKB KB",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }

                if (showCopiedMessage) {
                    Card(
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            ),
                    ) {
                        Text(
                            "JSON copied to clipboard!",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilledTonalButton(
                        onClick = {
                            exportHandler.shareJsonFile(file)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !savingFile,
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share file",
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share File")
                    }

                    OutlinedButton(
                        onClick = {
                            exportHandler.copyJsonToClipboard(file)
                            showCopiedMessage = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !savingFile,
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy to clipboard",
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy to Clipboard")
                    }

                    OutlinedButton(
                        onClick = {
                            createDocumentLauncher.launch(file.name)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !savingFile,
                    ) {
                        if (savingFile) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                Icons.Default.Save,
                                contentDescription = "Save to device",
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (savingFile) "Saving..." else "Save to Device")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !savingFile,
            ) {
                Text("Done")
            }
        },
    )
}
