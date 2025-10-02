package com.eventradar.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp

data class Event(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = EventCategory.OTHER.name,
    val location: GeoPoint = GeoPoint(0.0, 0.0),
    @ServerTimestamp val createdAt: Timestamp? = null,
    val creatorId: String = "",
    val creatorName: String = "",

    val eventTimestamp: Timestamp? = null,
    val ageRestriction: Int = 0,
    val price: Double = 0.0,
    val isFree: Boolean = true,
    val eventImageUrl: String? = null
)
