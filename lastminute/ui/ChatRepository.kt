package com.example.lastminute.ui


import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChatRepository {
    private val db = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val TAG = "ChatRepository"

    companion object {
        fun generateChatId(userId1: String, userId2: String): String {
            // Always sort IDs to ensure consistent chat ID generation
            val sortedIds = listOf(userId1, userId2).sorted()
            return "${sortedIds[0]}_${sortedIds[1]}"
        }
    }

    private fun isChatParticipant(chatId: String, userId: String): Boolean {
        val participants = chatId.split("_")
        return participants.size == 2 && participants.contains(userId)
    }

    suspend fun sendMessage(
        chatId: String,
        message: ChatMessage,
        isFriend: Boolean
    ) {
        try {
            Log.d(TAG, "Starting to send message in chat: $chatId")
            val participants = chatId.split("_")
            val otherUserId = participants.find { it != message.senderId }
            
            Log.d(TAG, "Sender: ${message.senderId}, Receiver: $otherUserId")
            
            // Create message with receiver ID and timestamp
            val messageWithReceiver = message.copy(
                receiverId = otherUserId ?: "",
                isRead = false
            )

            // First ensure the chat document exists
            val chatDoc = db.collection("chats").document(chatId)
            Log.d(TAG, "Checking if chat document exists")
            
            if (!chatDoc.get().await().exists()) {
                Log.d(TAG, "Creating new chat document with participants: $participants")
                // Create chat document with participants
                chatDoc.set(mapOf(
                    "participants" to participants,
                    "lastMessageTime" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp()
                )).await()
            }

            Log.d(TAG, "Adding message to chat")
            // Add message to chat
            val messageRef = db.collection("chats")
                .document(chatId)
                .collection("messages")
                .add(messageWithReceiver)
                .await()

            Log.d(TAG, "Message added with ID: ${messageRef.id}")

            // Update chat's last message time
            chatDoc.update(
                mapOf(
                    "lastMessageTime" to FieldValue.serverTimestamp(),
                    "lastMessage" to messageWithReceiver.message
                )
            ).await()

            Log.d(TAG, "Updated chat document with last message time")

            if (otherUserId != null) {
                if (isFriend) {
                    Log.d(TAG, "Marking message as unread for friend")
                    // If they're friends, mark message as unread
                    MessageRequestManager.markMessageAsUnread(
                        chatId = chatId,
                        senderId = message.senderId,
                        receiverId = otherUserId
                    )
                } else {
                    Log.d(TAG, "Creating message request for non-friend")
                    // If they're not friends, create a message request
                    MessageRequestManager.sendMessageRequest(
                        senderId = message.senderId,
                        receiverId = otherUserId,
                        message = message.message
                    )
                }
            }
            Log.d(TAG, "Message send completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message", e)
            throw e // Rethrow to handle in UI
        }
    }

    fun listenMessages(
        chatId: String,
        currentUserId: String,
        onNewMessages: (List<ChatMessage>) -> Unit
    ): ListenerRegistration {
        Log.d(TAG, "Starting to listen for messages in chat: $chatId")
        return db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to messages: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    val messages = snapshot.documents.mapNotNull { 
                        it.toObject(ChatMessage::class.java) 
                    }
                    Log.d(TAG, "Received ${messages.size} messages")
                    onNewMessages(messages)
                    
                    // Mark messages as read in background
                    scope.launch {
                        try {
                            val unreadMessages = messages.filter { 
                                it.receiverId == currentUserId && !it.isRead 
                            }
                            if (unreadMessages.isNotEmpty()) {
                                Log.d(TAG, "Marking ${unreadMessages.size} messages as read")
                                snapshot.documents.forEach { doc ->
                                    val message = doc.toObject(ChatMessage::class.java)
                                    if (message?.receiverId == currentUserId && message.isRead == false) {
                                        doc.reference.update("isRead", true).await()
                                    }
                                }
                                MessageRequestManager.markMessagesAsRead(chatId, currentUserId)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error marking messages as read", e)
                        }
                    }
                } else {
                    Log.d(TAG, "No messages in chat")
                    onNewMessages(emptyList())
                }
            }
    }

    suspend fun getUnreadMessages(userId: String): Map<String, List<ChatMessage>> {
        val unreadMessages = mutableMapOf<String, List<ChatMessage>>()
        
        // Get all chats
        val chats = db.collection("chats")
            .get()
            .await()
            .documents
            .filter { doc -> isChatParticipant(doc.id, userId) }

        // For each chat, get unread messages
        chats.forEach { chat ->
            val messages = chat.reference
                .collection("messages")
                .whereEqualTo("receiverId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(ChatMessage::class.java) }
            
            if (messages.isNotEmpty()) {
                unreadMessages[chat.id] = messages
            }
        }
        
        return unreadMessages
    }
} 