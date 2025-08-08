package com.github.radupana.featherweight.ui.components.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class HistorySearchState(
    val query: String = "",
    val selectedExercises: Set<String> = emptySet(),
    val hasDateRange: Boolean = false,
    val showFilters: Boolean = false,
    val recentSearches: List<String> = emptyList(),
    val showRecentSearches: Boolean = false,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistorySearchBar(
    searchState: HistorySearchState,
    onSearchChange: (String) -> Unit,
    onExerciseFilterToggle: (String) -> Unit,
    onDateRangeFilterToggle: () -> Unit,
    onClearFilters: () -> Unit,
    onAdvancedFiltersClick: () -> Unit,
    onRecentSearchClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        // Main search field
        OutlinedTextField(
            value = searchState.query,
            onValueChange = onSearchChange,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        isFocused = focusState.isFocused
                    },
            placeholder = { Text("Search workouts...") },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = "Search")
            },
            trailingIcon = {
                Row {
                    if (searchState.query.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                        }
                    }
                    IconButton(onClick = onAdvancedFiltersClick) {
                        Icon(
                            Icons.Filled.FilterList,
                            contentDescription = "Advanced filters",
                            tint =
                                if (hasActiveFilters(searchState)) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                ),
        )

        // Filter chips
        AnimatedVisibility(
            visible = hasActiveFilters(searchState),
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut(),
        ) {
            FlowRow(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Exercise filters
                searchState.selectedExercises.forEach { exercise ->
                    FilterChip(
                        selected = true,
                        onClick = { onExerciseFilterToggle(exercise) },
                        label = {
                            Text(
                                text = exercise,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.FitnessCenter,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        colors =
                            FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                    )
                }

                // Date range filter
                if (searchState.hasDateRange) {
                    FilterChip(
                        selected = true,
                        onClick = onDateRangeFilterToggle,
                        label = {
                            Text(
                                text = "Date Range",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        colors =
                            FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                    )
                }

                // Clear all filters chip
                if (hasActiveFilters(searchState)) {
                    FilterChip(
                        selected = false,
                        onClick = onClearFilters,
                        label = {
                            Text(
                                text = "Clear All",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                        trailingIcon = {
                            Icon(
                                Icons.Filled.Clear,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        colors =
                            FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                labelColor = MaterialTheme.colorScheme.onErrorContainer,
                            ),
                    )
                }
            }
        }

        // Recent searches dropdown
        AnimatedVisibility(
            visible = isFocused && searchState.recentSearches.isNotEmpty() && searchState.query.isEmpty(),
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut(),
        ) {
            RecentSearchesDropdown(
                recentSearches = searchState.recentSearches,
                onSearchClick = onRecentSearchClick,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun RecentSearchesDropdown(
    recentSearches: List<String>,
    onSearchClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(8.dp),
        shape = RoundedCornerShape(12.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
        ) {
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(6.dp),
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Filled.History,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Recent Searches",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            LazyColumn {
                items(recentSearches) { search ->
                    RecentSearchItem(
                        searchText = search,
                        onClick = { onSearchClick(search) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentSearchItem(
    searchText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = searchText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private fun hasActiveFilters(searchState: HistorySearchState): Boolean = searchState.selectedExercises.isNotEmpty() || searchState.hasDateRange
