package com.example.lastminute.ui

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class MessageRequest(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderEmail: String = "",
    val lastMessage: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class UnreadMessage(
    val chatId: String = "",
    val senderId: String = "",
    val count: Int = 0,
    val lastMessageTime: Long = System.currentTimeMillis()
)

object MessageRequestManager {
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "MessageRequestManager"

    suspend fun sendMessageRequest(
        senderId: String,
        receiverId: String,
        message: String
    ) {
        try {
            Log.d(TAG, "Sending message request from $senderId to $receiverId")
            // Get sender info
            val senderDoc = db.collection("users")
                .document(senderId)
                .get()
                .await()

            val messageRequest = MessageRequest(
                senderId = senderId,
                senderName = senderDoc.getString("name") ?: "Unknown",
                senderEmail = senderDoc.getString("email") ?: "",
                lastMessage = message,
                timestamp = System.currentTimeMillis()
            )

            // Store in message requests collection
            db.collection("users")
                .document(receiverId)
                .collection("messageRequests")
                .document(senderId)  // Use senderId as document ID to prevent duplicates
                .set(messageRequest)
                .await()
            
            Log.d(TAG, "Message request sent successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message request", e)
            throw e
        }
    }

    suspend fun getMessageRequests(userId: String): List<MessageRequest> {
        return db.collection("users")
            .document(userId)
            .collection("messageRequests")
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                doc.toObject(MessageRequest::class.java)?.copy(id = doc.id)
            }
    }

    suspend fun acceptMessageRequest(userId: String, requestId: String) {
        // Delete the message request
        db.collection("users")
            .document(userId)
            .collection("messageRequests")
            .document(requestId)
            .delete()
            .await()
    }

    suspend fun rejectMessageRequest(userId: String, requestId: String) {
        // Delete the message request
        db.collection("users")
            .document(userId)
            .collection("messageRequests")
            .document(requestId)
            .delete()
            .await()
    }

    suspend fun markMessageAsUnread(chatId: String, senderId: String, receiverId: String) {
        try {
            Log.d(TAG, "Marking message as unread in chat: $chatId")
            val unreadRef = db.collection("users")
                .document(receiverId)
                .collection("unreadMessages")
                .document(chatId)

            val unreadDoc = unreadRef.get().await()
            if (unreadDoc.exists()) {
                val currentCount = unreadDoc.getLong("count")?.toInt() ?: 0
                unreadRef.update(
                    mapOf(
                        "count" to (currentCount + 1),
                        "lastMessageTime" to System.currentTimeMillis()
                    )
                ).await()
                Log.d(TAG, "Updated unread count to ${currentCount + 1}")
            } else {
                val unreadMessage = UnreadMessage(
                    chatId = chatId,
                    senderId = senderId,
                    count = 1,
                    lastMessageTime = System.currentTimeMillis()
                )
                unreadRef.set(unreadMessage).await()
                Log.d(TAG, "Created new unread message entry")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error marking message as unread", e)
            throw e
        }
    }

    suspend fun markMessagesAsRead(chatId: String, userId: String) {
        try {
            Log.d(TAG, "Marking messages as read for chat: $chatId")
            db.collection("users")
                .document(userId)
                .collection("unreadMessages")
                .document(chatId)
                .delete()
                .await()
            Log.d(TAG, "Messages marked as read successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error marking messages as read", e)
            throw e
        }
    }

    suspend fun getUnreadMessageCounts(userId: String): Map<String, Int> {
        return try {
            Log.d(TAG, "Getting unread message counts for user: $userId")
            val counts = db.collection("users")
                .document(userId)
                .collection("unreadMessages")
                .get()
                .await()
                .documents
                .associate { doc ->
                    doc.id to (doc.getLong("count")?.toInt() ?: 0)
                }
            Log.d(TAG, "Retrieved unread counts: $counts")
            counts
        } catch (e: Exception) {
            Log.e(TAG, "Error getting unread message counts", e)
            emptyMap()
        }
    }
} 