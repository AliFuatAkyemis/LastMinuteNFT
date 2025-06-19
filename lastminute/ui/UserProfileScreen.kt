package com.example.lastminute.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.lastminute.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: String,
    currentUserId: String,
    navController: NavController
) {
    var user by remember { mutableStateOf<User?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Load user data
    LaunchedEffect(userId) {
        try {
            val doc = FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .await()

            user = doc.toObject(User::class.java)?.copy(uid = doc.id)
        } catch (e: Exception) {
            error = e.message
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(user?.name ?: "User Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack("user_selection", false) }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (error != null) {
                Text(
                    text = "Error loading profile",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.error
                )
            } else if (user == null) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Profile Icon
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = "Profile Picture",
                        modifier = Modifier.size(120.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    // User Name
                    Text(
                        text = user?.name ?: "",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )

                    // Email
                    Text(
                        text = user?.email ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Start Chat Button
                    Button(
                        onClick = {
                            val chatId = listOf(currentUserId, userId).sorted().joinToString("_")
                            navController.navigate("chat/$chatId/${user?.name}")
                        },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Icon(
                            Icons.Default.Chat,
                            contentDescription = "Chat",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Chat")
                    }
                }
            }
        }
    }
} 