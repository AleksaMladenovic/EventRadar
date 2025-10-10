package com.eventradar.ui.change_password

import androidx.annotation.StringRes

data class ChangePasswordState(
    val oldPassword: String = "",
    val newPassword: String = "",
    val confirmNewPassword: String = "",
    val isLoading: Boolean = false,
    @StringRes val error: Int? = null,
    val success: Boolean = false
)