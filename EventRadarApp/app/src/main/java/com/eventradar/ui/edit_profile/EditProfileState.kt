package com.eventradar.ui.edit_profile

import androidx.annotation.StringRes

data class EditProfileState(
    val firstName: String = "",
    val lastName: String = "",
    val username: String = "",
    val phone: String = "",

    val isLoading: Boolean = true,
    val error: String? = null,
    val saveSuccess: Boolean = false,

    // Polja za greške validacije
    @StringRes val firstNameError: Int? = null,
    @StringRes val lastNameError: Int? = null,
    @StringRes val usernameError: Int? = null,
    @StringRes val phoneError: Int? = null
)
