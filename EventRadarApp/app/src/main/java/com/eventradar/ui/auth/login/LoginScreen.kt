package com.eventradar.ui.auth.login

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eventradar.R

@Composable
fun LoginScreen(
    loginViewModel: LoginViewModel, // PRIMA NOVI VIEWMODEL
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit // Lambda za uspešan login
) {
    // Čitamo stanje forme
    val formState by loginViewModel.formState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Slušamo jednokratne događaje iz ViewModel-a
    LaunchedEffect(Unit) {
        loginViewModel.loginEvent.collect { event ->
            when (event) {
                is LoginEvent.LoginSuccess -> {
                    Toast.makeText(context, "Login Successful!", Toast.LENGTH_SHORT).show()
                    onLoginSuccess() // Javljamo navigaciji da je vreme da se pređe dalje
                }
                is LoginEvent.LoginError -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(id = R.string.login_title),
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = formState.email,
            onValueChange = { loginViewModel.onEmailChanged(it) },
            label = { Text(stringResource(id = R.string.email_label)) },
            modifier = Modifier.fillMaxWidth(),
            isError = formState.emailError != null,
            supportingText = {
                formState.emailError?.let { Text(stringResource(id = it)) }
            }
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = formState.password,
            onValueChange = { loginViewModel.onPasswordChanged(it) },
            label = { Text(stringResource(id = R.string.password_label)) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            isError = formState.passwordError != null,
            supportingText = {
                formState.passwordError?.let { Text(stringResource(id = it)) }
            }
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { loginViewModel.login() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !formState.isLoading
        ) {
            if (formState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(text = stringResource(id = R.string.login_button))
            }
        }

        TextButton(onClick = onNavigateToRegister) {
            Text(stringResource(id = R.string.no_account_prompt))
        }
    }
}