package com.eventradar.data.model

import com.google.firebase.Timestamp

data class User(
    val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val username: String = "",
    val phone: String = "",
    val email: String = "", // Dupliramo email i ovde radi lakšeg pristupa
    val profileImageUrl: String? = null,
    val createdAt: Timestamp = Timestamp.now()
)