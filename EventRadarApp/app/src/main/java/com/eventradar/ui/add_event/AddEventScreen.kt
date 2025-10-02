package com.eventradar.ui.add_event

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eventradar.R
import com.eventradar.data.model.EventCategory
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventScreen(
    addEventViewModel: AddEventViewModel = hiltViewModel(),
    onEventAdded: () -> Unit // Lambda za povratak na prethodni ekran
) {
    val formState by addEventViewModel.formState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState()

    val focusManager = LocalFocusManager.current

    val context = LocalContext.current

    // Slušamo događaje za uspeh/grešku
    LaunchedEffect(Unit) {
        addEventViewModel.addEventResult.collectLatest { result ->
            when (result) {
                is AddEventResult.Success -> {
                    Toast.makeText(context, "Event added successfully!", Toast.LENGTH_SHORT).show()
                    onEventAdded() // Vrati se na mapu
                }
                is AddEventResult.Error -> {
                    Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(id = R.string.add_event_screen_title),
            modifier = Modifier.align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Polje za ime događaja
        OutlinedTextField(
            value = formState.name,
            onValueChange = { addEventViewModel.onNameChange(it) },
            label = { Text(stringResource(id = R.string.event_name_label)) },
            modifier = Modifier.fillMaxWidth(),
            isError = formState.nameError != null,
            supportingText = {
                formState.nameError?.let { Text(stringResource(id = it)) }
            }
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Polje za opis
        OutlinedTextField(
            value = formState.description,
            onValueChange = { addEventViewModel.onDescriptionChange(it) },
            label = { Text(stringResource(id = R.string.description_label)) },
            modifier = Modifier.fillMaxWidth(),
            isError = formState.descriptionError != null,
            supportingText = {
                formState.descriptionError?.let { Text(stringResource(id = it)) }
            }
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Dropdown za kategoriju
        CategoryDropdown(
            selectedCategory = formState.category,
            onCategorySelected = { addEventViewModel.onCategoryChange(it) }
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Polje za datum
        val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        OutlinedTextField(
            value = if (formState.eventDate != null) dateFormatter.format(formState.eventDate) else "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Event Date") },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged({focusState-> if(focusState.isFocused) showDatePicker = true }),
            isError = formState.dateError != null,
            supportingText = { /* ... prikaži grešku ... */ }
        )



        Spacer(modifier = Modifier.height(16.dp))


        // Polje za vreme
        OutlinedTextField(
            value = formState.eventTime,
            onValueChange = {},
            readOnly = true,
            label = { Text("Event Time") }, // Dodaj string resurs
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged({focusState-> if(focusState.isFocused) showTimePicker = true }),
            isError = formState.timeError != null,
            supportingText = { /* prikaži grešku */ }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("18+ Event", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = formState.ageRestriction,
                onCheckedChange = { addEventViewModel.onAgeRestrictionChanged(it) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Polje za Cenu
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Free Event", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = formState.isFree,
                onCheckedChange = { addEventViewModel.onIsFreeChanged(it) }
            )
        }

        // AnimatedVisibility će lepo prikazati ili sakriti polje za cenu
        AnimatedVisibility(visible = !formState.isFree) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = formState.price,
                    onValueChange = { addEventViewModel.onPriceChanged(it) },
                    label = { Text("Price (RSD)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = formState.priceError != null,
                    supportingText = {
                        formState.priceError?.let { Text(stringResource(id = it)) }
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))

        // Dugme za čuvanje
        Button(
            onClick = { addEventViewModel.onSaveEvent() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !formState.isLoading
        ) {
            if (formState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text(stringResource(id = R.string.save_event_button))
            }
        }


    }
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            // Pozivamo ViewModel sa izabranim datumom
                            addEventViewModel.onDateChanged(Date(millis))
                            focusManager.clearFocus()
                        }
                        showDatePicker = false
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

    if (showTimePicker) {

        // TimePickerDialog je malo drugačiji, pravimo ga od AlertDialog-a
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time") },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        addEventViewModel.onTimeChanged(timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                        focusManager.clearFocus()
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showTimePicker = false
                    focusManager.clearFocus()
                }) { Text("Cancel") }
            }
        )
    }


}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdown(
    selectedCategory: EventCategory,
    onCategorySelected: (EventCategory) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val categories = EventCategory.values()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedCategory.name,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(id = R.string.category_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}
