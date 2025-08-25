package com.github.radupana.featherweight.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/**
 * A searchable selection dialog that supports both single and multi-selection modes.
 *
 * @param T The type of items in the selection list
 * @param title The title of the dialog
 * @param items List of items to select from
 * @param selectedItems Currently selected items (for multi-select)
 * @param selectedItem Currently selected item (for single-select)
 * @param itemLabel Function to get display label for each item
 * @param searchHint Hint text for search field
 * @param multiSelect Whether to allow multiple selections
 * @param required Whether selection is required
 * @param onDismiss Called when dialog is dismissed
 * @param onConfirm Called when selection is confirmed (multi-select)
 * @param onSelect Called when item is selected (single-select)
 */
@Composable
fun <T> SearchableSelectionDialog(
    title: String,
    items: List<T>,
    selectedItems: Set<T> = emptySet(),
    selectedItem: T? = null,
    itemLabel: (T) -> String,
    searchHint: String = "Search...",
    multiSelect: Boolean = true,
    required: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: ((Set<T>) -> Unit)? = null,
    onSelect: ((T) -> Unit)? = null,
) {
    var searchQuery by remember { mutableStateOf("") }
    var tempSelectedItems by remember { mutableStateOf(selectedItems) }
    var tempSelectedItem by remember { mutableStateOf(selectedItem) }

    // Filter items based on search query (keyword matching, not exact order)
    val filteredItems =
        remember(searchQuery, items) {
            if (searchQuery.isBlank()) {
                items
            } else {
                val queryWords = searchQuery.lowercase().split(" ").filter { it.isNotBlank() }
                items.filter { item ->
                    val label = itemLabel(item).lowercase()
                    queryWords.all { queryWord ->
                        label.contains(queryWord)
                    }
                }
            }
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxSize(0.9f),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                if (required) {
                    Text(
                        text = if (multiSelect) "Select at least one" else "Required",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(searchHint) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Show selected count for multi-select
                if (multiSelect) {
                    Text(
                        text = "${tempSelectedItems.size} selected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                HorizontalDivider()

                // Item list
                if (filteredItems.isEmpty()) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No items found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        items(filteredItems) { item ->
                            Surface(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (multiSelect) {
                                                tempSelectedItems =
                                                    if (item in tempSelectedItems) {
                                                        tempSelectedItems - item
                                                    } else {
                                                        tempSelectedItems + item
                                                    }
                                            } else {
                                                tempSelectedItem = item
                                                onSelect?.invoke(item)
                                                onDismiss()
                                            }
                                        },
                                color = run {
                                    val isSelectedInMultiMode = multiSelect && item in tempSelectedItems
                                    val isSelectedInSingleMode = !multiSelect && item == tempSelectedItem
                                    val isSelected = isSelectedInMultiMode || isSelectedInSingleMode
                                    
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    }
                                },
                            ) {
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = itemLabel(item),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                    )

                                    if (multiSelect) {
                                        Checkbox(
                                            checked = item in tempSelectedItems,
                                            onCheckedChange = null, // Handled by row click
                                        )
                                    } else if (item == tempSelectedItem) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick actions for multi-select
                if (multiSelect && filteredItems.isNotEmpty()) {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        TextButton(
                            onClick = {
                                tempSelectedItems = tempSelectedItems + filteredItems
                            },
                        ) {
                            Text("Select All Visible")
                        }
                        TextButton(
                            onClick = {
                                tempSelectedItems = tempSelectedItems - filteredItems.toSet()
                            },
                        ) {
                            Text("Deselect All Visible")
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (multiSelect) {
                Button(
                    onClick = {
                        onConfirm?.invoke(tempSelectedItems)
                        onDismiss()
                    },
                    enabled = !required || tempSelectedItems.isNotEmpty(),
                ) {
                    Text("Done")
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
