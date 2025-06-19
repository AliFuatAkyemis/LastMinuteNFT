package com.example.lastminute.ui

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.lastminute.model.User
import com.example.lastminute.utils.FriendManager
import com.example.lastminute.utils.FriendRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSelectionScreen(
    currentUserId: String,
    navController: NavController
) {
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var friends by remember { mutableStateOf<List<String>>(emptyList()) }
    var friendRequests by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var showFriendRequestsDialog by remember { mutableStateOf(false) }
    var sentRequests by remember { mutableStateOf<List<String>>(emptyList()) }
    var userToRemove by remember { mutableStateOf<User?>(null) }
    var unreadMessageCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var unreadMessages by remember { mutableStateOf<Map<String, List<ChatMessage>>>(emptyMap()) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val chatRepository = remember { ChatRepository() }
    val TAG = "UserSelectionScreen"

    // Load data
    LaunchedEffect(currentUserId) {
        try {
            Log.d(TAG, "Loading data for user: $currentUserId")
            
            // Load users
            val snapshot = FirebaseFirestore.getInstance()
                .collection("users")
                .get()
                .await()

            users = snapshot.documents.mapNotNull { doc ->
                val id = doc.id
                if (id == currentUserId) null
                else doc.toObject(User::class.java)?.copy(uid = id)
            }
            Log.d(TAG, "Loaded ${users.size} users")

            // Load friends
            friends = FriendManager.getFriends(currentUserId)
            Log.d(TAG, "Loaded ${friends.size} friends")

            // Load friend requests
            friendRequests = FriendManager.getPendingRequests(currentUserId)
            Log.d(TAG, "Loaded ${friendRequests.size} friend requests")

            // Load sent requests
            sentRequests = FriendManager.getSentRequests(currentUserId).map { it.receiverId }
            Log.d(TAG, "Loaded ${sentRequests.size} sent requests")

            // Load unread messages
            unreadMessageCounts = MessageRequestManager.getUnreadMessageCounts(currentUserId)
            Log.d(TAG, "Loaded unread message counts: $unreadMessageCounts")

            unreadMessages = chatRepository.getUnreadMessages(currentUserId)
            Log.d(TAG, "Loaded unread messages for ${unreadMessages.size} chats")

        } catch (e: Exception) {
            Log.e(TAG, "Error loading data", e)
            snackbarHostState.showSnackbar("Failed to load data: ${e.message}")
        }
    }

    // Filter users based on search and mode
    val filteredUsers = remember(users, searchQuery.text, selectedTab, friends, unreadMessages) {
        val query = searchQuery.text.trim().lowercase()
        when (selectedTab) {
            0 -> users.filter { user -> // Friends tab
                friends.contains(user.uid) &&
                (query.isEmpty() || user.name.lowercase().contains(query) ||
                (user.email?.lowercase()?.contains(query) == true))
            }
            1 -> users.filter { user -> // Messages tab
                val chatId = ChatRepository.generateChatId(currentUserId, user.uid)
                val hasUnreadMessages = unreadMessageCounts[chatId]?.let { it > 0 } ?: false
                hasUnreadMessages &&
                (query.isEmpty() || user.name.lowercase().contains(query) ||
                (user.email?.lowercase()?.contains(query) == true))
            }
            else -> users.filter { user ->
                query.isEmpty() || user.name.lowercase().contains(query) ||
                (user.email?.lowercase()?.contains(query) == true)
            }
        }
    }

    // Confirmation dialog for removing friend
    userToRemove?.let { user ->
        AlertDialog(
            onDismissRequest = { userToRemove = null },
            title = { Text("Remove Friend") },
            text = { Text("Are you sure you want to remove ${user.name} from your friends?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                FriendManager.deleteFriend(currentUserId, user.uid)
                                friends = FriendManager.getFriends(currentUserId)
                                snackbarHostState.showSnackbar("Friend removed")
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Failed to remove friend")
                            }
                        }
                        userToRemove = null
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToRemove = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showFriendRequestsDialog) {
        AlertDialog(
            onDismissRequest = { showFriendRequestsDialog = false },
            title = { Text("Friend Requests") },
            text = {
                LazyColumn {
                    items(friendRequests) { request ->
                        val senderUser = users.find { it.uid == request.senderId }
                        ListItem(
                            headlineContent = { Text(senderUser?.name ?: "Unknown User") },
                            supportingContent = { Text(senderUser?.email ?: "") },
                            trailingContent = {
                                Row {
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                try {
                                                    FriendManager.acceptFriendRequest(request.id)
                                                    friendRequests = FriendManager.getPendingRequests(currentUserId)
                                                    friends = FriendManager.getFriends(currentUserId)
                                                    showFriendRequestsDialog = false
                                                    snackbarHostState.showSnackbar("Friend request accepted")
                                                } catch (e: Exception) {
                                                    snackbarHostState.showSnackbar("Failed to accept request")
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                try {
                                                    FriendManager.rejectFriendRequest(request.id)
                                                    friendRequests = FriendManager.getPendingRequests(currentUserId)
                                                    snackbarHostState.showSnackbar("Friend request rejected")
                                                } catch (e: Exception) {
                                                    snackbarHostState.showSnackbar("Failed to reject request")
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFriendRequestsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Chat") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack("main", false) }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (friendRequests.isNotEmpty()) {
                            Box(modifier = Modifier.padding(end = 16.dp)) {
                                IconButton(onClick = { showFriendRequestsDialog = true }) {
                                    BadgedBox(
                                        badge = {
                                            Badge { Text(friendRequests.size.toString()) }
                                        }
                                    ) {
                                        Icon(Icons.Default.Notifications, contentDescription = "Friend Requests")
                                    }
                                }
                            }
                        }
                    }
                )
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Friends") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Messages") },
                        icon = {
                            if (unreadMessageCounts.values.sum() > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge {
                                            Text(unreadMessageCounts.values.sum().toString())
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Message, contentDescription = null)
                                }
                            } else {
                                Icon(Icons.Default.Message, contentDescription = null)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("All Users") }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search field for all tabs
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { 
                    Text(when (selectedTab) {
                        0 -> "Search friends"
                        1 -> "Search messages"
                        else -> "Search all users"
                    })
                },
                leadingIcon = { Icon(Icons.Default.Search, null) }
            )

            if (filteredUsers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (selectedTab) {
                            0 -> if (searchQuery.text.isEmpty()) "No friends yet" else "No matching friends"
                            1 -> if (searchQuery.text.isEmpty()) "No unread messages" else "No matching messages"
                            else -> if (searchQuery.text.isEmpty()) "Type to search users" else "No matching users"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn {
                    items(filteredUsers) { user ->
                        val chatId = ChatRepository.generateChatId(currentUserId, user.uid)
                        val unreadCount = unreadMessages[chatId]?.size ?: 0
                        UserListItem(
                            user = user,
                            currentUserId = currentUserId,
                            isFriend = friends.contains(user.uid),
                            isRequestSent = sentRequests.contains(user.uid),
                            unreadCount = unreadCount,
                            onAction = { 
                                if (friends.contains(user.uid)) {
                                    userToRemove = user
                                } else if (!sentRequests.contains(user.uid)) {
                                    scope.launch {
                                        try {
                                            FriendManager.sendFriendRequest(currentUserId, user.uid)
                                            sentRequests = FriendManager.getSentRequests(currentUserId).map { it.receiverId }
                                            snackbarHostState.showSnackbar("Friend request sent")
                                        } catch (e: Exception) {
                                            snackbarHostState.showSnackbar("Failed to send friend request")
                                        }
                                    }
                                }
                            },
                            onClick = { navController.navigate("userProfile/${user.uid}") }
                        )
                    }
                }
            }
        }
    }

    // Show snackbar for notifications
    LaunchedEffect(snackbarHostState) {
        snackbarHostState.showSnackbar("Welcome to chat")
    }
}

@Composable
private fun UserListItem(
    user: User,
    currentUserId: String,
    isFriend: Boolean,
    isRequestSent: Boolean,
    unreadCount: Int,
    onAction: () -> Unit,
    onClick: () -> Unit
) {
    val chatId = ChatRepository.generateChatId(currentUserId, user.uid)
    
    ListItem(
        headlineContent = { Text(user.name) },
        supportingContent = { Text(user.email ?: "") },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (unreadCount > 0) {
                    Badge {
                        Text(unreadCount.toString())
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                if (isFriend) {
                    IconButton(onClick = onAction) {
                        Icon(Icons.Default.PersonRemove, contentDescription = "Remove Friend")
                    }
                } else if (isRequestSent) {
                    Icon(Icons.Default.Pending, contentDescription = "Request Pending")
                } else {
                    IconButton(onClick = onAction) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add Friend")
                    }
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
