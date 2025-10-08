package com.eventradar.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Comment(
    var id: String = "",
    var eventId: String = "",
    var authorId: String = "",
    var text: String = "",
    @ServerTimestamp
    var timestamp: Timestamp? = null
)