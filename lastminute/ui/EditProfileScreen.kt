package com.example.lastminute.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.lastminute.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBackClick: () -> Unit,
    navController: NavHostController
) {
    val context = LocalContext.current
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""

    var name by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    // Veriyi Firestore'dan al
    LaunchedEffect(userId) {
        if (userId != null) {
            val doc = FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .await()

            name = doc.getString("name") ?: ""
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Geri butonu ve başlık
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .clickable { onBackClick() }
                    .size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "Edit Profile", 
                fontSize = 20.sp, 
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Profil harfi
        Box(
            modifier = Modifier
                .size(100.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    name.firstOrNull()?.uppercase() ?: "?", 
                    color = MaterialTheme.colorScheme.onPrimary, 
                    fontSize = 40.sp
                )
            }

            Image(
                painter = painterResource(id = R.drawable.edit),
                contentDescription = "Edit Icon",
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // İsim düzenleme alanı
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (userId != null && name.isNotBlank()) {
                    val db = FirebaseFirestore.getInstance()
                    db.collection("users")
                        .document(userId)
                        .update("name", name)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Name updated!", Toast.LENGTH_SHORT).show()
                            onBackClick()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Error updating name.", Toast.LENGTH_SHORT).show()
                        }
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(50))
        ) {
            Text(
                "Save", 
                fontSize = 18.sp, 
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
