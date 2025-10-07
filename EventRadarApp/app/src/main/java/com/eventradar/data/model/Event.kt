package com.eventradar.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

data class Event(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = EventCategory.OTHER.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    val creatorId: String = "",
    val creatorName: String = "",

    val eventTimestamp: Timestamp? = null,
    val ageRestriction: Int = 0,
    val price: Double = 0.0,
    val free: Boolean = true,
    val eventImageUrl: String? = null,

    @get:PropertyName("l") @set:PropertyName("l")
    var location: GeoPoint = GeoPoint(0.0, 0.0),

    @get:PropertyName("g") @set:PropertyName("g")
    var geohash: String? = null

){
    constructor() : this(
        "",
        "",
        "",
        EventCategory.OTHER.name,
        null,
        "",
        "",
        null,
        0,
        0.0,
        true,
        null,
        GeoPoint(0.0, 0.0),
        null)
}

