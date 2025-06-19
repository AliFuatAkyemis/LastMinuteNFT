package com.example.lastminute.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.lastminute.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

@Composable
fun LoginScreen(navController: NavHostController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val auth: FirebaseAuth = FirebaseAuth.getInstance()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                modifier = Modifier
                    .size(48.dp)
                    .clickable { navController.popBackStack("signup", false) },
                tint = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Login", 
                fontSize = 28.sp, 
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Forgot your password?",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.clickable { navController.navigate("forgot") }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("→", fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    loginUser(email, password, auth, context) {
                        navController.navigate("main")
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(50.dp))
            ) {
                Text("LOGIN", fontSize = 18.sp, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

fun loginUser(
    email: String,
    password: String,
    auth: FirebaseAuth,
    context: Context,
    success: () -> Unit
) {
    if (email.trim().isBlank() || password.isBlank()) {
        Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
        return
    }

    auth.signInWithEmailAndPassword(email.trim(), password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val uid = auth.currentUser?.uid ?: return@addOnCompleteListener
                val userEmail = auth.currentUser?.email ?: "user@email.com"
                val userName = userEmail.substringBefore("@").replaceFirstChar { it.uppercase() }

                val db = FirebaseFirestore.getInstance()
                val userDoc = db.collection("users").document(uid)

                userDoc.get().addOnSuccessListener { document ->
                    if (!document.exists()) {
                        val userData = mapOf(
                            "name" to userName,
                            "balance" to 0.0
                        )
                        userDoc.set(userData)
                    } else {
                        val updates = mutableMapOf<String, Any>()
                        if (!document.contains("balance")) updates["balance"] = 0.0
                        if (!document.contains("name")) updates["name"] = userName
                        if (updates.isNotEmpty()) {
                            userDoc.set(updates, SetOptions.merge())
                        }
                    }
                }

                Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show()
                success()
            } else {
                Toast.makeText(context, "Email or password is wrong!", Toast.LENGTH_SHORT).show()
            }
        }
}
