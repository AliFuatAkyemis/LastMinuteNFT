package com.example.lastminute.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lastminute.model.NftItem
import com.example.lastminute.util.CartManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NFTDetailScreen(
    id: String,
    imageUrl: String,
    ownerName: String,
    price: String,
    onBackClick: () -> Unit,
    navController: NavController
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val currentUserEmail = currentUser?.email ?: ""
    val userId = currentUser?.uid ?: ""
    val isOwnNft = ownerName == currentUserEmail

    val firestore = FirebaseFirestore.getInstance()

    var comments by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var commentText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val snapshot = firestore
            .collection("nfts")
            .document(ownerName+id.toString())
            .collection("comments")
            .orderBy("timestamp")
            .get()
            .await()

        val commentsData = snapshot.documents.mapNotNull { it.data }
        
        // Fetch user names for all comments
        val commentsWithNames = commentsData.map { comment ->
            val userEmail = comment["user"]?.toString() ?: ""
            val userSnapshot = firestore.collection("users")
                .whereEqualTo("email", userEmail)
                .get()
                .await()
            
            val userName = if (!userSnapshot.isEmpty) {
                userSnapshot.documents[0].getString("name") ?: userEmail
            } else {
                userEmail
            }
            
            comment + mapOf("displayName" to userName)
        }
        
        comments = commentsWithNames
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    // NFT Image
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(450.dp)
                    ) {
                        ImgurImage(
                            url = imageUrl,
                            contentDescription = "NFT Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .padding(16.dp)
                                .size(36.dp)
                                .clickable { onBackClick() }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Owner & Price
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Owner",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = ownerName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        Text(
                            text = price,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Show End Sale button for own NFTs
                    if (isOwnNft) {
                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        // Get NFT data from nfts collection
                                        val nftDoc = FirebaseFirestore.getInstance()
                                            .collection("nfts")
                                            .document(id)
                                            .get()
                                            .await()

                                        val nftData = nftDoc.data
                                        if (nftData != null) {
                                            // Add to userNFTs collection
                                            FirebaseFirestore.getInstance()
                                                .collection("users")
                                                .document(userId)
                                                .collection("userNFTs")
                                                .document(id)
                                                .set(nftData.plus(mapOf(
                                                    "status" to "owned",
                                                    "endedAt" to System.currentTimeMillis()
                                                )))
                                                .await()

                                            // Remove from nfts collection
                                            FirebaseFirestore.getInstance()
                                                .collection("nfts")
                                                .document(id)
                                                .delete()
                                                .await()

                                            snackbarHostState.showSnackbar("NFT removed from sale and returned to your collection")
                                            navController.popBackStack("main", false)
                                        }
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Failed to end sale: ${e.message}")
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(
                                "End Sale",
                                color = MaterialTheme.colorScheme.onSecondary,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Add to Cart button
                    Button(
                        onClick = {
                            if (isOwnNft) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "You cannot buy your own NFT!",
                                        duration = SnackbarDuration.Long
                                    )
                                }
                            } else {
                                val nft = NftItem(
                                    id = id,
                                    price = price,
                                    owner = ownerName,
                                    imageUrl = imageUrl
                                )

                                if (CartManager.isInCart(nft)) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("This NFT is already in the cart.")
                                    }
                                } else {
                                    CartManager.addToCart(nft)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Added to cart successfully!")
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isOwnNft) MaterialTheme.colorScheme.surfaceVariant
                                          else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            if (isOwnNft) "Your Own NFT" else "Add To Cart",
                            color = if (isOwnNft) MaterialTheme.colorScheme.onSurfaceVariant
                                  else MaterialTheme.colorScheme.onPrimary,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Comments",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Comments List
                items(comments) { comment ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = comment["displayName"]?.toString() ?: "Anonymous",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = comment["text"]?.toString() ?: "",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Comment Input Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("Write a comment...") },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp)),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val comment = hashMapOf(
                            "user" to currentUserEmail,
                            "text" to commentText,
                            "timestamp" to System.currentTimeMillis()
                        )

                        firestore.collection("nfts")
                            .document(ownerName+id.toString())
                            .collection("comments")
                            .add(comment)
                            .addOnSuccessListener {
                                commentText = ""
                                scope.launch {
                                    snackbarHostState.showSnackbar("Comment added")
                                }
                                // Refresh comments with user names
                                firestore.collection("nfts")
                                    .document(ownerName+id.toString())
                                    .collection("comments")
                                    .orderBy("timestamp")
                                    .get()
                                    .addOnSuccessListener { snapshot ->
                                        scope.launch {
                                            val commentsData = snapshot.documents.mapNotNull { it.data }
                                            val commentsWithNames = commentsData.map { comment ->
                                                val userEmail = comment["user"]?.toString() ?: ""
                                                val userSnapshot = firestore.collection("users")
                                                    .whereEqualTo("email", userEmail)
                                                    .get()
                                                    .await()
                                                
                                                val userName = if (!userSnapshot.isEmpty) {
                                                    userSnapshot.documents[0].getString("name") ?: userEmail
                                                } else {
                                                    userEmail
                                                }
                                                
                                                comment + mapOf("displayName" to userName)
                                            }
                                            comments = commentsWithNames
                                        }
                                    }
                            }
                    }
                ) {
                    Text("Send", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

