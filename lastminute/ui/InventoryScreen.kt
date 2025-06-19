package com.example.lastminute.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import com.example.lastminute.util.readBytesBase64
import com.example.lastminute.util.uploadImageToImgur

@Composable
fun ImageViewerDialog(
    imageUrl: String,
    onDismiss: () -> Unit,
    onSell: () -> Unit,
    onEndSale: () -> Unit,
    showSellButton: Boolean,
    showEndSaleButton: Boolean
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Image
            AsyncImage(
                model = imageUrl,
                contentDescription = "Full size NFT Image",
                modifier = Modifier.fillMaxSize()
            )
            
            // Top bar with close button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            ) {
                Row(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onDismiss,
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            
            // Bottom bar with buttons
            if (showSellButton || showEndSaleButton) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (showSellButton) {
                            Button(
                                onClick = onSell,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .height(48.dp)
                            ) {
                                Text("Sell This NFT", fontSize = MaterialTheme.typography.titleMedium.fontSize)
                            }
                        }
                        if (showEndSaleButton) {
                            Button(
                                onClick = onEndSale,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .height(48.dp)
                            ) {
                                Text("End Sale", fontSize = MaterialTheme.typography.titleMedium.fontSize)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    navController: NavHostController,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userEmail = currentUser?.email ?: ""
    val userId = currentUser?.uid ?: ""
    val snackbarHostState = remember { SnackbarHostState() }
    
    var nftsForSale by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var ownedNfts by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }
    var selectedNft by remember { mutableStateOf<Map<String, Any>?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val base64Image = readBytesBase64(context, uri)
                    val imageUrl = uploadImageToImgur("3c159a82cc243e2", base64Image)

                    if (imageUrl != null) {
                        val nftId = UUID.randomUUID().toString()
                        val nftData = hashMapOf(
                            "id" to nftId,
                            "owner" to userEmail,
                            "imageUrl" to imageUrl,
                            "createdAt" to System.currentTimeMillis(),
                            "status" to "owned"
                        )

                        // Add to user's NFT collection
                        FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(userId)
                            .collection("userNFTs")
                            .document(nftId)
                            .set(nftData)
                            .await()

                        // Refresh owned NFTs list
                        val userNftsRef = FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(userId)
                            .collection("userNFTs")

                        val ownedNftsSnapshot = userNftsRef.get().await()
                        ownedNfts = ownedNftsSnapshot.documents.map { doc ->
                            val data = doc.data?.toMutableMap() ?: mutableMapOf()
                            data.plus(mapOf("id" to doc.id))
                        }

                        snackbarHostState.showSnackbar("NFT added to your collection!")
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Failed to add NFT: ${e.message}")
                }
            }
        }
    }

    // Fetch both NFTs for sale and owned NFTs
    LaunchedEffect(userEmail) {
        try {
            val db = FirebaseFirestore.getInstance()
            
            // 1. Get NFTs for sale from main collection
            val nftSnapshot = db.collection("nfts")
                .whereEqualTo("owner", userEmail)
                .get()
                .await()

            nftsForSale = nftSnapshot.documents.map { doc ->
                val data = doc.data ?: mapOf()
                data.plus(mapOf("id" to doc.id))
            }

            // 2. Get owned NFTs from user's collection
            val userNftsRef = db.collection("users")
                .document(userId)
                .collection("userNFTs")

            val ownedNftsSnapshot = userNftsRef.get().await()
            ownedNfts = ownedNftsSnapshot.documents.map { doc ->
                val data = doc.data?.toMutableMap() ?: mutableMapOf()
                data.plus(mapOf("id" to doc.id))
            }

            isLoading = false
        } catch (e: Exception) {
            isLoading = false
            snackbarHostState.showSnackbar("Error loading NFTs: ${e.message}")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My NFTs") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack("main", false) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { launcher.launch("image/*") }) {
                        Icon(Icons.Default.Add, contentDescription = "Add to Collection")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            } else {
                // NFTs for Sale Section
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "NFTs For Sale",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                    
                    if (nftsForSale.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("You don't have any NFTs for sale")
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(nftsForSale) { nft ->
                                NFTInventoryCard(
                                    nft = nft,
                                    onImageClick = { url, nftData ->
                                        selectedImageUrl = url
                                        selectedNft = nftData
                                    }
                                )
                            }
                        }
                    }
                }

                Divider(modifier = Modifier.padding(horizontal = 16.dp))

                // Owned NFTs Section
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "My Collection",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                    
                    if (ownedNfts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("You don't own any NFTs yet")
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(ownedNfts) { nft ->
                                NFTInventoryCard(
                                    nft = nft,
                                    onImageClick = { url, nftData ->
                                        selectedImageUrl = url
                                        selectedNft = nftData
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Show image viewer dialog when an image is selected
    selectedImageUrl?.let { imageUrl ->
        ImageViewerDialog(
            imageUrl = imageUrl,
            onDismiss = { 
                selectedImageUrl = null
                selectedNft = null
            },
            onSell = {
                selectedNft?.let { nft ->
                    val encodedUrl = java.net.URLEncoder.encode(nft["imageUrl"] as String, "UTF-8")
                    navController.navigate("upload?imageUrl=$encodedUrl&nftId=${nft["id"]}")
                }
            },
            onEndSale = {
                selectedNft?.let { nft ->
                    scope.launch {
                        try {
                            val nftId = nft["id"] as String
                            
                            // Add to userNFTs collection
                            FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(userId)
                                .collection("userNFTs")
                                .document(nftId)
                                .set(nft.plus(mapOf(
                                    "status" to "owned",
                                    "endedAt" to System.currentTimeMillis()
                                )))
                                .await()

                            // Remove from nfts collection
                            FirebaseFirestore.getInstance()
                                .collection("nfts")
                                .document(nftId)
                                .delete()
                                .await()

                            // Update local state
                            nftsForSale = nftsForSale.filter { it["id"] != nftId }
                            val newOwnedNft = nft.plus(mapOf(
                                "status" to "owned",
                                "endedAt" to System.currentTimeMillis()
                            ))
                            ownedNfts = ownedNfts + listOf(newOwnedNft)

                            selectedImageUrl = null
                            selectedNft = null
                            snackbarHostState.showSnackbar("NFT removed from sale and returned to your collection")
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Failed to end sale: ${e.message}")
                        }
                    }
                }
            },
            // Show sell button for NFTs in the owned collection (not for sale)
            showSellButton = selectedNft?.let { nft ->
                ownedNfts.any { it["id"] == nft["id"] }
            } ?: false,
            // Show end sale button for NFTs in the for sale collection
            showEndSaleButton = selectedNft?.let { nft ->
                nftsForSale.any { it["id"] == nft["id"] }
            } ?: false
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NFTInventoryCard(
    nft: Map<String, Any>,
    onImageClick: (String, Map<String, Any>) -> Unit
) {
    val imageUrl = nft["imageUrl"] as String

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        onClick = { 
            onImageClick(imageUrl, nft)
        }
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "NFT Image",
            modifier = Modifier.fillMaxSize()
        )
    }
} 