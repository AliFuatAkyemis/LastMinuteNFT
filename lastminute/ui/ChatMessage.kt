package com.example.lastminute.ui

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class ChatMessage(
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",
    @ServerTimestamp
    val timestamp: Timestamp? = null,
    val isRead: Boolean = false
) {
    // Required empty constructor for Firestore
    constructor() : this("", "", "", null, false)
} 