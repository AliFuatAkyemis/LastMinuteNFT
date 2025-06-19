package com.example.lastminute.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.lastminute.MainActivity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object NotificationManager {
    private const val CHANNEL_ID = "chat_messages"
    private const val CHANNEL_NAME = "Chat Messages"
    private var notificationId = 0

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Chat message notifications"
                enableVibration(true)
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    suspend fun sendMessageNotification(
        context: Context,
        senderId: String,
        message: String,
        chatId: String
    ) {
        val db = FirebaseFirestore.getInstance()
        
        // Get sender's name
        val senderDoc = db.collection("users")
            .document(senderId)
            .get()
            .await()
        
        val senderName = senderDoc.getString("name") ?: "Unknown"

        // Create an intent for when the notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("chatId", chatId)
            putExtra("otherUserName", senderName)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle("New message from $senderName")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // Show the notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId++, notification)
    }
} 