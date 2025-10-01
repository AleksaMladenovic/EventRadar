package com.eventradar.ui.auth.register

data class RegistrationFormState(
    val firstName: String = "",
    val lastName: String = "",
    val username: String = "",
    val phone: String = "",
    val email: String = "",
    val password: String = "",
    val firstNameError: Int? = null,
    val lastNameError: Int? = null,
    val usernameError: Int? = null,
    val phoneError: Int? = null,
    val emailError: Int? = null,
    val passwordError: Int? = null,
    val isLoading: Boolean = false
)