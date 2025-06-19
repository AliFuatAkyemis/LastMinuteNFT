package com.example.lastminute.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBalanceScreen(navController: NavController) {
    val cards = listOf("Visa - 1234", "Mastercard - 5678", "Amex - 9012")
    var selectedCard by remember { mutableStateOf(cards.first()) }
    var amount by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Balance") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack("main", false) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text("Kart Seç", style = MaterialTheme.typography.titleMedium)

            Box {
                OutlinedButton(onClick = { expanded = true }) {
                    Text(selectedCard)
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    cards.forEach { card ->
                        DropdownMenuItem(
                            text = { Text(card) },
                            onClick = {
                                selectedCard = card
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = amount,
                onValueChange = {
                    amount = it
                    error = false
                },
                label = { Text("Yüklenecek Miktar") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = error,
                modifier = Modifier.fillMaxWidth()
            )

            if (error) {
                Text("Geçerli bir miktar girin", color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val added = amount.toFloatOrNull()
                    if (added != null && added > 0f) {
                        val uid = FirebaseAuth.getInstance().currentUser?.uid
                        if (uid != null) {
                            val db = FirebaseFirestore.getInstance()
                            val userDoc = db.collection("users").document(uid)

                            userDoc.get().addOnSuccessListener { document ->
                                val currentBalance = document.getDouble("balance") ?: 0.0
                                val newBalance = currentBalance + added.toDouble()

                                userDoc.set(
                                    mapOf("balance" to newBalance),
                                    SetOptions.merge()
                                ).addOnSuccessListener {
                                    navController.popBackStack()
                                }
                            }
                        }
                    } else {
                        error = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Bakiyeyi Ekle")
            }
        }
    }
}
