package com.eventradar.ui.add_event

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eventradar.R
import com.eventradar.data.model.EventCategory
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventScreen(
    addEventViewModel: AddEventViewModel = hiltViewModel(),
    onEventAdded: () -> Unit // Lambda za povratak na prethodni ekran
) {
    val formState by addEventViewModel.formState.collectAsStateWithLifecycle()
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
