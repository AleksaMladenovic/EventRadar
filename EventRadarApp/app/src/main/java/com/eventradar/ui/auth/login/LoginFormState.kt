package com.eventradar.ui.auth.login

data class LoginFormState(
    val email: String = "",
    val password: String = "",
    val emailError: Int? = null, // ID string resursa za grešku
    val passwordError: Int? = null,
    val isLoading: Boolean = false
)