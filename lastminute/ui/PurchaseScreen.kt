package com.example.lastminute.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.lastminute.util.CartManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseScreen(navController: NavHostController) {
    val context = LocalContext.current
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    val totalPrice = CartManager.getTotalPrice()

    var balance by remember { mutableStateOf(0.0) }
    var selectedPayment by remember { mutableStateOf("balance") }

    // Kullanıcı bakiyesi çekiliyor
    LaunchedEffect(userId) {
        if (userId != null) {
            val snapshot = FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .await()
            balance = snapshot.getDouble("balance") ?: 0.0
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Purchase") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack("cart", false) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Text("Select Payment Method:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selectedPayment == "balance",
                    onClick = { selectedPayment = "balance" }
                )
                Text("Use Balance (%.2f ₺)".format(balance))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selectedPayment == "card",
                    onClick = { selectedPayment = "card" }
                )
                Text("Use Credit Card")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Cart Total: %.2f ₺".format(totalPrice), style = MaterialTheme.typography.titleLarge)

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (userId == null) {
                        Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val db = FirebaseFirestore.getInstance()
                    val userDoc = db.collection("users").document(userId)
                    val purchasesCollection = userDoc.collection("purchases")

                    // Fonksiyon: satın alma kaydı ekle
                    fun savePurchases(onSuccess: () -> Unit) {
                        if (CartManager.cartItems.isEmpty()) {
                            Toast.makeText(context, "Cart is empty", Toast.LENGTH_SHORT).show()
                            return
                        }

                        var processedItems = 0
                        val totalItems = CartManager.cartItems.size

                        CartManager.cartItems.forEach { nft ->
                            // Get the NFT document reference
                            val nftRef = db.collection("nfts").document(nft.id)

                            // First, verify the NFT exists and get its data
                            nftRef.get().addOnSuccessListener { nftDoc ->
                                if (nftDoc.exists()) {
                                    val ownerEmail = nftDoc.getString("owner")
                                    val price = nft.price.removeSuffix("$").toDoubleOrNull() ?: 0.0

                                    // Get the owner's user document for balance update
                                    db.collection("users")
                                        .whereEqualTo("email", ownerEmail)
                                        .get()
                                        .addOnSuccessListener { userQuery ->
                                            if (!userQuery.isEmpty) {
                                                val userDoc = userQuery.documents[0]
                                                val currentBalance = userDoc.getDouble("balance") ?: 0.0
                                                val newBalance = currentBalance + price

                                                // Create a batch for atomic operations
                                                val batch = db.batch()

                                                // 1. Update owner's balance
                                                batch.update(userDoc.reference, "balance", newBalance)

                                                // 2. Add purchase record
                                                val purchaseDoc = purchasesCollection.document()
                                                val purchaseData = mapOf(
                                                    "owner" to ownerEmail,
                                                    "price" to nft.price,
                                                    "imageRes" to nft.imageUrl,
                                                    "timestamp" to System.currentTimeMillis()
                                                )
                                                batch.set(purchaseDoc, purchaseData)

                                                // 3. Transfer NFT to buyer's personal collection
                                                val buyerNftCollection = db.collection("users")
                                                    .document(userId!!)
                                                    .collection("userNFTs")
                                                
                                                val nftData = nftDoc.data?.toMutableMap() ?: mutableMapOf()
                                                nftData["purchaseDate"] = System.currentTimeMillis()
                                                nftData["previousOwner"] = nftData["owner"]
                                                nftData["owner"] = userId
                                                
                                                // Create the NFT in buyer's collection first
                                                batch.set(buyerNftCollection.document(nft.id), nftData)

                                                // 4. Delete the NFT from original collection
                                                batch.delete(nftRef)

                                                // Commit all operations
                                                batch.commit()
                                                    .addOnSuccessListener {
                                                        processedItems++
                                                        if (processedItems == totalItems) {
                                                            CartManager.clearCart()
                                                            onSuccess()
                                                        }
                                                    }
                                                    .addOnFailureListener { e ->
                                                        Toast.makeText(
                                                            context,
                                                            "Failed to process purchase: ${e.message}",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Owner not found for NFT",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(
                                                context,
                                                "Failed to find owner: ${e.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                } else {
                                    Toast.makeText(
                                        context,
                                        "NFT no longer exists",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    processedItems++
                                    if (processedItems == totalItems) {
                                        CartManager.clearCart()
                                        onSuccess()
                                    }
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    context,
                                    "Failed to fetch NFT: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }

                    if (selectedPayment == "balance") {
                        if (balance >= totalPrice) {
                            val newBalance = balance - totalPrice
                            userDoc.update("balance", newBalance)
                                .addOnSuccessListener {
                                    savePurchases {
                                        Toast.makeText(context, "Purchase successful!", Toast.LENGTH_SHORT).show()
                                        navController.navigate("purchasefinal")
                                    }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Failed to update balance", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(context, "Insufficient balance!", Toast.LENGTH_SHORT).show()
                        }
                    } else if (selectedPayment == "card") {
                        savePurchases {
                            Toast.makeText(context, "Paid with card!", Toast.LENGTH_SHORT).show()
                            navController.navigate("purchasefinal")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Complete Purchase", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}
