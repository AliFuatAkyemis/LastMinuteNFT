package com.example.lastminute.utils

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class FriendRequest(
    val id: String,
    val senderId: String,
    val receiverId: String,
    val status: String,
    val timestamp: Long
)

object FriendManager {
    private val db = FirebaseFirestore.getInstance()
    private const val TAG = "FriendManager"
    
    suspend fun sendFriendRequest(userId: String, friendId: String) {
        val requestData = mapOf(
            "senderId" to userId,
            "receiverId" to friendId,
            "status" to "pending",
            "timestamp" to System.currentTimeMillis()
        )
        
        db.collection("friendRequests")
            .add(requestData)
            .await()
    }
    
    suspend fun acceptFriendRequest(requestId: String) {
        try {
            // Get the request document
            val request = db.collection("friendRequests")
                .document(requestId)
                .get()
                .await()

            if (!request.exists()) {
                throw Exception("Request not found")
            }

            val senderId = request.getString("senderId")
            val receiverId = request.getString("receiverId")

            if (senderId == null || receiverId == null) {
                throw Exception("Invalid request data")
            }

            // Check if friendship already exists
            val existingFriendship = db.collection("friendships")
                .whereEqualTo("userId", senderId)
                .whereEqualTo("friendId", receiverId)
                .get()
                .await()

            if (!existingFriendship.isEmpty) {
                throw Exception("Friendship already exists")
            }

            // Create friendship entries for both users
            val friendshipData1 = mapOf(
                "userId" to senderId,
                "friendId" to receiverId,
                "timestamp" to System.currentTimeMillis()
            )
            
            val friendshipData2 = mapOf(
                "userId" to receiverId,
                "friendId" to senderId,
                "timestamp" to System.currentTimeMillis()
            )

            try {
                db.runBatch { batch ->
                    // Add both friendship entries
                    val doc1 = db.collection("friendships").document()
                    val doc2 = db.collection("friendships").document()
                    batch.set(doc1, friendshipData1)
                    batch.set(doc2, friendshipData2)
                    
                    // Update request status to accepted (keeping it for history)
                    batch.update(
                        db.collection("friendRequests").document(requestId),
                        mapOf(
                            "status" to "accepted",
                            "acceptedAt" to System.currentTimeMillis()
                        )
                    )
                }.await()
            } catch (e: Exception) {
                Log.e("FriendManager", "Batch operation failed", e)
                throw Exception("Failed to update database")
            }
        } catch (e: Exception) {
            Log.e("FriendManager", "Accept friend request failed", e)
            throw e
        }
    }
    
    suspend fun rejectFriendRequest(requestId: String) {
        try {
            // Delete the rejected request
            db.collection("friendRequests")
                .document(requestId)
                .delete()
                .await()
        } catch (e: Exception) {
            throw e
        }
    }
    
    suspend fun removeFriend(userId: String, friendId: String) {
        val friendships = db.collection("friendships")
            .whereEqualTo("userId", userId)
            .whereEqualTo("friendId", friendId)
            .get()
            .await()
            
        for (doc in friendships.documents) {
            doc.reference.delete().await()
        }
        
        // Also remove the reverse friendship
        val reverseFriendships = db.collection("friendships")
            .whereEqualTo("userId", friendId)
            .whereEqualTo("friendId", userId)
            .get()
            .await()
            
        for (doc in reverseFriendships.documents) {
            doc.reference.delete().await()
        }
    }
    
    suspend fun getFriends(userId: String): List<String> {
        val friendships = db.collection("friendships")
            .whereEqualTo("userId", userId)
            .get()
            .await()
            
        return friendships.documents.mapNotNull { it.getString("friendId") }
    }
    
    suspend fun getPendingRequests(userId: String): List<FriendRequest> {
        val requests = db.collection("friendRequests")
            .whereEqualTo("receiverId", userId)
            .whereEqualTo("status", "pending")
            .get()
            .await()
            
        return requests.documents.mapNotNull { doc ->
            FriendRequest(
                id = doc.id,
                senderId = doc.getString("senderId") ?: return@mapNotNull null,
                receiverId = doc.getString("receiverId") ?: return@mapNotNull null,
                status = doc.getString("status") ?: return@mapNotNull null,
                timestamp = doc.getLong("timestamp") ?: return@mapNotNull null
            )
        }
    }
    
    suspend fun hasPendingRequest(userId: String, friendId: String): Boolean {
        val requests = db.collection("friendRequests")
            .whereEqualTo("senderId", userId)
            .whereEqualTo("receiverId", friendId)
            .whereEqualTo("status", "pending")
            .get()
            .await()
            
        return !requests.isEmpty
    }
    
    suspend fun getSentRequests(userId: String): List<FriendRequest> {
        return try {
            db.collection("friendRequests")
                .whereEqualTo("senderId", userId)
                .whereIn("status", listOf("pending"))
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    val id = doc.id
                    val senderId = doc.getString("senderId") ?: return@mapNotNull null
                    val receiverId = doc.getString("receiverId") ?: return@mapNotNull null
                    val status = doc.getString("status") ?: return@mapNotNull null
                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    
                    FriendRequest(id, senderId, receiverId, status, timestamp)
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun deleteFriend(userId: String, friendId: String) {
        try {
            // Get both friendship documents
            val friendships = db.collection("friendships")
                .whereEqualTo("userId", userId)
                .whereEqualTo("friendId", friendId)
                .get()
                .await()
                .documents

            val reverseFriendships = db.collection("friendships")
                .whereEqualTo("userId", friendId)
                .whereEqualTo("friendId", userId)
                .get()
                .await()
                .documents

            // Delete both friendship documents in a batch
            db.runBatch { batch ->
                friendships.forEach { doc ->
                    batch.delete(doc.reference)
                }
                reverseFriendships.forEach { doc ->
                    batch.delete(doc.reference)
                }
            }.await()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun getUnreadMessageCounts(userId: String): Map<String, Int> {
        return db.collection("users")
            .document(userId)
            .collection("unreadMessages")
            .get()
            .await()
            .documents
            .associate { doc ->
                doc.id to (doc.getLong("count")?.toInt() ?: 0)
            }
    }
}