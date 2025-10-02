package com.eventradar.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp

data class Event(
    val id: String = "",

    val name: String = "",
    val description: String = "",
    val category: String = "",

    val location: GeoPoint = GeoPoint(0.0, 0.0),

    @ServerTimestamp // Anotacija koja kaže Firestore-u da sam upiše vreme servera
    val createdAt: Timestamp? = null,

    val creatorId: String = "",
    val creatorName: String = ""
)