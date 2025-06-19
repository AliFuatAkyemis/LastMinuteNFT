@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.lastminute.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lastminute.utils.FriendManager
import kotlinx.coroutines.launch
import com.google.firebase.Timestamp
import android.util.Log

@Composable
fun ChatScreen(
    chatId: String,
    currentUserId: String,
    otherUserName: String,
    chatRepository: ChatRepository = ChatRepository(),
    onBackClick: () -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var isFriend by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val TAG = "ChatScreen"

    // Check if they are friends
    LaunchedEffect(currentUserId, chatId) {
        try {
            Log.d(TAG, "Checking friend status for chat: $chatId")
            val otherUserId = chatId.split("_").find { it != currentUserId } ?: return@LaunchedEffect
            val friends = FriendManager.getFriends(currentUserId)
            isFriend = friends.contains(otherUserId)
            Log.d(TAG, "Friend status: $isFriend")
        } catch (e: Exception) {
            Log.e(TAG, "Error checking friend status", e)
            error = "Failed to check friend status: ${e.message}"
            snackbarHostState.showSnackbar("Error: $error")
        }
    }

    // Listen to messages
    DisposableEffect(chatId, currentUserId) {
        Log.d(TAG, "Setting up message listener for chat: $chatId")
        val listener = chatRepository.listenMessages(chatId, currentUserId) { newMessages ->
            Log.d(TAG, "Received ${newMessages.size} messages")
            messages = newMessages.sortedWith(compareBy(
                { it.timestamp ?: Timestamp(0, 0) }
            ))
        }
        onDispose {
            Log.d(TAG, "Removing message listener")
            listener.remove()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(otherUserName) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Messages list
            LazyColumn(
                modifier = Modifier.weight(1f),
                reverseLayout = true,
                contentPadding = PaddingValues(8.dp)
            ) {
                items(messages.reversed()) { msg ->
                    MessageItem(msg = msg, isCurrentUser = msg.senderId == currentUserId)
                }
            }

            // Message input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)) },
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                    ),
                    enabled = !isLoading,
                    singleLine = true,
                    shape = MaterialTheme.shapes.small
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            scope.launch {
                                try {
                                    isLoading = true
                                    error = null
                                    val otherUserId = chatId.split("_").find { it != currentUserId }
                                    Log.d(TAG, "Preparing to send message to: $otherUserId")
                                    
                                    if (otherUserId == null) {
                                        throw IllegalStateException("Could not determine recipient ID")
                                    }
                                    
                                    val newMsg = ChatMessage(
                                        senderId = currentUserId,
                                        receiverId = otherUserId,
                                        message = messageText.trim(),
                                        timestamp = null,  // Let Firestore set the server timestamp
                                        isRead = false
                                    )
                                    messageText = "" // Reset immediately
                                    
                                    Log.d(TAG, "Sending message: ${newMsg.message}")
                                    chatRepository.sendMessage(
                                        chatId = chatId,
                                        message = newMsg,
                                        isFriend = isFriend
                                    )
                                    Log.d(TAG, "Message sent successfully")
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to send message", e)
                                    error = "Failed to send message: ${e.message}"
                                    snackbarHostState.showSnackbar("Error: Failed to send message - ${e.message}")
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    },
                    enabled = messageText.isNotBlank() && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Send")
                    }
                }
            }
        }
    }
}

@Composable
fun MessageItem(msg: ChatMessage, isCurrentUser: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = if (isCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.padding(4.dp)
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = msg.message,
                    color = if (isCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isCurrentUser) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (msg.isRead) Icons.Filled.DoneAll else Icons.Filled.Done,
                        contentDescription = if (msg.isRead) "Read" else "Sent",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
