package com.eventradar.ui.edit_profile

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eventradar.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Efekat koji reaguje na uspešno čuvanje
    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            Toast.makeText(context, R.string.profile_update_success, Toast.LENGTH_SHORT).show()
            onNavigateBack()
        }
    }

    // Efekat koji reaguje na opštu grešku
    LaunchedEffect(state.error) {
        state.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.edit_profile_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {
            if (state.isLoading && state.firstName.isBlank()) { // Prikaži loading samo pri inicijalnom učitavanju
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Polja forme, identična kao u RegisterScreen
                    OutlinedTextField(
                        value = state.firstName,
                        onValueChange = { viewModel.onFirstNameChanged(it) },
                        label = { Text(stringResource(id = R.string.first_name_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = state.firstNameError != null,
                        supportingText = { state.firstNameError?.let { Text(stringResource(id = it)) } }
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = state.lastName,
                        onValueChange = { viewModel.onLastNameChanged(it) },
                        label = { Text(stringResource(id = R.string.last_name_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = state.lastNameError != null,
                        supportingText = { state.lastNameError?.let { Text(stringResource(id = it)) } }
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = state.username,
                        onValueChange = { viewModel.onUsernameChanged(it) },
                        label = { Text(stringResource(id = R.string.username_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = state.usernameError != null,
                        supportingText = { state.usernameError?.let { Text(stringResource(id = it)) } }
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = state.phone,
                        onValueChange = { viewModel.onPhoneChanged(it) },
                        label = { Text(stringResource(id = R.string.phone_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = state.phoneError != null,
                        supportingText = { state.phoneError?.let { Text(stringResource(id = it)) } }
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    // Dugme za čuvanje
                    Button(
                        onClick = { viewModel.onSaveClicked() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isLoading
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Text(stringResource(id = R.string.save_changes_button))
                        }
                    }
                }
            }
        }
    }
}
