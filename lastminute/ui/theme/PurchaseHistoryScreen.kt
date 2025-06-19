package com.example.lastminute.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun PurchaseHistoryScreen(onBackClick: () -> Unit) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    var purchases by remember { mutableStateOf<List<Map<String, Any>>?>(null) }

    LaunchedEffect(Unit) {
        if (userId != null) {
            val snapshot = FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("purchases")
                .get()
                .await()

            purchases = snapshot.documents.mapNotNull { it.data }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Purchase History", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        when {
            purchases == null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            purchases!!.isEmpty() -> {
                Text(
                    text = "You haven't purchased anything yet.",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            else -> {
                LazyColumn {
                    items(purchases!!) { item ->
                        val owner = item["owner"] as? String ?: "Unknown"
                        val price = item["price"] as? String ?: "-"
                        Text(
                            text = "Owner: $owner | Price: $price",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onBackClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Main", color = Color.White)
        }
    }
}
