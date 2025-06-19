package com.example.lastminute.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun ProfilePopup(
    onClose: () -> Unit,
    navController: NavHostController
) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: "unknown@email.com"

    var balance by remember { mutableStateOf(0.0) }
    var name by remember { mutableStateOf("User") }

    // Firestore'dan balance ve ad bilgisini çek
    LaunchedEffect(userId) {
        if (userId != null) {
            val doc = FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .await()

            balance = doc.getDouble("balance") ?: 0.0
            name = doc.getString("name") ?: "User"
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.width(250.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profil Dairesi
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.firstOrNull()?.uppercase() ?: "?",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "My Profile", 
                fontSize = 18.sp, 
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                name, 
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                userEmail, 
                fontSize = 13.sp, 
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "Purchase History",
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate("purchase-history") }
                    .padding(vertical = 4.dp)
            )

            Text(
                "Payment Methods",
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate("payment-methods") }
                    .padding(vertical = 4.dp)
            )
            Text(
                "Inventory",
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate("inventory") }
                    .padding(vertical = 4.dp)
            )
            Text(
                "Edit Profile",
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate("edit-profile") }
                    .padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "Account Balance", 
                fontSize = 13.sp, 
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "%.2f ₺".format(balance), 
                fontSize = 20.sp, 
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { navController.navigate("add_balance") },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    "Add Balance", 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Close",
                modifier = Modifier.clickable { onClose() },
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
