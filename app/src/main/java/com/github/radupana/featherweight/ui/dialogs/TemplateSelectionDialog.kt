package com.github.radupana.featherweight.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.radupana.featherweight.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateSelectionDialog(
    onTemplateSelected: (ExampleTemplate) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilters by remember { mutableStateOf<Set<TemplateFilter>>(emptySet()) }
    var expandedCategory by remember { mutableStateOf<TemplateFilterCategory?>(null) }

    // Filter templates based on search and selected filters
    val filteredTemplates =
        remember(searchQuery, selectedFilters) {
            ExampleTemplates.templates.filter { template ->
                // Search filter
                val matchesSearch =
                    searchQuery.isEmpty() ||
                        template.title.contains(searchQuery, ignoreCase = true) ||
                        template.tags.any { it.contains(searchQuery, ignoreCase = true) } ||
                        template.exampleText.contains(searchQuery, ignoreCase = true)

                // Category filters (if any selected, template must match at least one per category)
                val matchesFilters =
                    if (selectedFilters.isEmpty()) {
                        true
                    } else {
                        // Group filters by category
                        val filtersByCategory = selectedFilters.groupBy { it.category }
                        // For each category, template must match at least one filter
                        filtersByCategory.all { (_, filters) ->
                            filters.any { filter -> TemplateFilters.matchesFilter(template, filter) }
                        }
                    }

                matchesSearch && matchesFilters
            }
        }

    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
            ),
    ) {
        Surface(
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Choose Template",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search templates...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    },
                    singleLine = true,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Filter categories
                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TemplateFilterCategory.values().forEach { category ->
                        item {
                            Column {
                                // Category header
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                expandedCategory = if (expandedCategory == category) null else category
                                            }.padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = category.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Medium,
                                    )

                                    val categoryFilters = TemplateFilters.getFiltersByCategory(category)
                                    val selectedCount = categoryFilters.count { it in selectedFilters }
                                    if (selectedCount > 0) {
                                        Text(
                                            text = "$selectedCount selected",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }

                                // Filter chips (expanded)
                                if (expandedCategory == category) {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(vertical = 8.dp),
                                    ) {
                                        items(TemplateFilters.getFiltersByCategory(category)) { filter ->
                                            FilterChip(
                                                selected = filter in selectedFilters,
                                                onClick = {
                                                    selectedFilters =
                                                        if (filter in selectedFilters) {
                                                            selectedFilters - filter
                                                        } else {
                                                            selectedFilters + filter
                                                        }
                                                },
                                                label = { Text(filter.displayName) },
                                                modifier = Modifier.height(32.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Clear filters button (if any filters selected)
                if (selectedFilters.isNotEmpty()) {
                    TextButton(
                        onClick = { selectedFilters = emptySet() },
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text("Clear all filters")
                    }
                }

                // Templates count
                Text(
                    text =
                        when {
                            searchQuery.isNotEmpty() && selectedFilters.isNotEmpty() ->
                                "${filteredTemplates.size} templates match your search and filters"
                            searchQuery.isNotEmpty() ->
                                "${filteredTemplates.size} templates found"
                            selectedFilters.isNotEmpty() ->
                                "${filteredTemplates.size} templates match your filters"
                            else ->
                                "${filteredTemplates.size} templates available"
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Templates list
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    items(filteredTemplates) { template ->
                        ExpandedTemplateCard(
                            template = template,
                            onClick = {
                                onTemplateSelected(template)
                                onDismiss()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterTag(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun ExpandedTemplateCard(
    template: ExampleTemplate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { onClick() },
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header with title and metadata
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = template.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )

                Column(
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        text = "${template.goal.emoji} ${template.goal.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "${template.frequency}×/week • ${template.duration.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Tags
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(template.tags) { tag ->
                    Surface(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            // Example text preview
            Text(
                text = template.exampleText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            // Action hint
            Text(
                text = "Tap to use this template",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
