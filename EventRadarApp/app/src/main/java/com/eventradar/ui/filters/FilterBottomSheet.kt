package com.eventradar.ui.filters

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eventradar.data.model.EventCategory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    viewModel: FilterViewModel = hiltViewModel(),
    onApplyFilters: () -> Unit
) {
    val filters by viewModel.temporaryFilters.collectAsStateWithLifecycle()
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


        // Slider za radijus
        Text("Filter by Radius", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

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

        Spacer(modifier = Modifier.height(16.dp))
        Text("Filter by Date Range", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DatePickerField(
                label = "From",
                date = filters.startDate,
                onDateSelected = { viewModel.onStartDateChanged(it) },
                modifier = Modifier.weight(1f)
            )
            DatePickerField(
                label = "To",
                date = filters.endDate,
                onDateSelected = { viewModel.onEndDateChanged(it) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Dugmici za resetovanje i apply
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.resetFilters() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Reset")
            }
            Button(
                onClick = {
                    viewModel.applyFilters() // PRIMENI FILTERE
                    onApplyFilters()       // ZATVORI BOTTOM SHEET
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Apply Filters")
            }
        }

    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerField(
    modifier: Modifier = Modifier,
    label: String,
    date: Date?,
    onDateSelected: (Date) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    OutlinedTextField(
        value = date?.let { dateFormatter.format(it) } ?: "",
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        modifier = modifier.onFocusChanged { if (it.isFocused) showDatePicker = true }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = date?.time)
        DatePickerDialog(
            onDismissRequest = {
                showDatePicker = false
                focusManager.clearFocus()
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { onDateSelected(Date(it)) }
                        showDatePicker = false
                        focusManager.clearFocus()
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    focusManager.clearFocus()
                }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}