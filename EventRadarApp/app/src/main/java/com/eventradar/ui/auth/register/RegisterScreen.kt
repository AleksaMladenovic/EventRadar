package com.eventradar.ui.auth.register

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eventradar.R

@Composable
fun RegisterScreen(
    registerViewModel: RegisterViewModel,
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    // Čitamo stanje forme iz novog RegisterViewModel-a
    val formState by registerViewModel.formState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Slušamo događaje (uspeh/greška) iz RegisterViewModel-a
    LaunchedEffect(Unit) {
        registerViewModel.registerEvent.collect { event ->
            when (event) {
                is RegisterEvent.RegisterSuccess -> {
                    Toast.makeText(context, "Registration successful!", Toast.LENGTH_SHORT).show()
                    onRegisterSuccess() // Javljamo navigaciji da ide dalje
                }
                is RegisterEvent.RegisterError -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.register_title),
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(24.dp))

        // --- Polja forme sada zovu funkcije iz RegisterViewModel-a ---

        OutlinedTextField(
            value = formState.firstName,
            onValueChange = { registerViewModel.onFirstNameChanged(it) },
            label = { Text(stringResource(id = R.string.first_name_label)) },
            modifier = Modifier.fillMaxWidth(),
            isError = formState.firstNameError != null,
            supportingText = {
                formState.firstNameError?.let { Text(stringResource(id = it)) }
            }
        )
        Spacer(modifier = Modifier.height(8.dp)) // Smanjio sam razmak

        OutlinedTextField(
            value = formState.lastName,
            onValueChange = { registerViewModel.onLastNameChanged(it) },
            label = { Text(stringResource(id = R.string.last_name_label)) },
            modifier = Modifier.fillMaxWidth(),
            isError = formState.lastNameError != null,
            supportingText = {
                formState.lastNameError?.let { Text(stringResource(id = it)) }
            }
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = formState.username,
            onValueChange = { registerViewModel.onUsernameChanged(it) },
            label = { Text(stringResource(id = R.string.username_label)) },
            modifier = Modifier.fillMaxWidth(),
            isError = formState.usernameError != null,
            supportingText = {
                formState.usernameError?.let { Text(stringResource(id = it)) }
            }
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = formState.phone,
            onValueChange = { registerViewModel.onPhoneChanged(it) },
            label = { Text(stringResource(id = R.string.phone_label)) },
            modifier = Modifier.fillMaxWidth(),
            isError = formState.phoneError != null,
            supportingText = {
                formState.phoneError?.let { Text(stringResource(id = it)) }
            }
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = formState.email,
            onValueChange = { registerViewModel.onEmailChanged(it) },
            label = { Text(stringResource(id = R.string.email_label)) },
            modifier = Modifier.fillMaxWidth(),
            isError = formState.emailError != null,
            supportingText = {
                formState.emailError?.let { Text(stringResource(id = it)) }
            }
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = formState.password,
            onValueChange = { registerViewModel.onPasswordChanged(it) },
            label = { Text(stringResource(id = R.string.password_label)) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            isError = formState.passwordError != null,
            supportingText = {
                formState.passwordError?.let { Text(stringResource(id = it)) }
            }
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { registerViewModel.register() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !formState.isLoading
        ) {
            if (formState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(text = stringResource(id = R.string.register_button))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onNavigateToLogin) {
            Text(stringResource(id = R.string.already_have_account))
        }
    }
}