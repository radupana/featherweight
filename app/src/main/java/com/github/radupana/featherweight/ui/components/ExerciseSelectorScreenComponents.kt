package com.github.radupana.featherweight.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.data.exercise.ExerciseCategory

@Composable
fun CategoryFilterChips(
    categories: List<ExerciseCategory>,
    selectedCategory: ExerciseCategory?,
    onCategorySelected: (ExerciseCategory?) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
) {
    LazyRow(
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        items(categories) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = {
                    onCategorySelected(
                        if (selectedCategory == category) null else category,
                    )
                },
                label = {
                    Text(
                        category.displayName,
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
            )
        }
    }
}
