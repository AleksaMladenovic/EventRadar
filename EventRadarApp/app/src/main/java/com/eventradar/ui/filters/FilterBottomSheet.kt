package com.eventradar.ui.filters

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eventradar.data.model.EventCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    viewModel: FilterViewModel = hiltViewModel()
) {
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val categories = EventCategory.values()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text("Filter by Category", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        // Chip-ovi za kategorije
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories) { category ->
                FilterChip(
                    selected = category in filters.categories,
                    onClick = {
                        viewModel.onCategoryToggled(category)
                    },
                    label = { Text(stringResource(id = category.displayNameResId)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = category.color.copy(alpha = 0.2f),
                        selectedLabelColor = category.color
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Filter by Radius", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        // Slider za radijus
        val radiusValue = filters.radiusInKm?.toFloat() ?: FilterViewModel.MAX_RADIUS
        Slider(
            value = radiusValue,
            onValueChange = { viewModel.onRadiusChange(it) },
            valueRange = 1f..FilterViewModel.MAX_RADIUS,
            steps = 100 // (50-1) / 10 = 4.9 -> 4 koraka
        )
        Text(
            text = if (radiusValue == FilterViewModel.MAX_RADIUS) "Any distance"
            else "Within ${radiusValue.toInt()} km",
            modifier = Modifier.align(Alignment.End)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Dugme za resetovanje
        OutlinedButton(
            onClick = { viewModel.onResetFilters() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reset Filters")
        }
    }
}