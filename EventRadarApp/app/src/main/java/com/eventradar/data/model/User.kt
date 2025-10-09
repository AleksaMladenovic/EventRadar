package com.eventradar.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

data class User(
    val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val username: String = "",
    val phone: String = "",
    val email: String = "",
    val profileImageUrl: String? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val points: Long = 0,
    val attendingEventIds: List<String> = emptyList(),
    var lastKnownLocation: GeoPoint? = null
)