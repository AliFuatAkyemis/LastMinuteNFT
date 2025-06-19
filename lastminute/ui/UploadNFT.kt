package com.example.lastminute.ui

import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.InputStream
import java.util.UUID
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold

@Composable
fun UploadNFT(
    navController: NavController,
    imageUrl: String? = null,
    nftId: String? = null
) {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var uploadedImageUrl by remember { mutableStateOf(imageUrl) }
    var price by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val clientId = "3c159a82cc243e2"  // Replace with your Imgur Client ID
    val context = LocalContext.current
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userEmail = currentUser?.email
    val userId = currentUser?.uid

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        uploadedImageUrl = null // reset previous upload
    }

    fun handlePriceInput(input: String) {
        if (input.isEmpty()) {
            price = ""
            return
        }

        // Only allow digits, single decimal point, and limit length
        if (!input.matches(Regex("^\\d*\\.?\\d*$"))) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Please enter only numbers and decimal point")
            }
            return
        }

        val parts = input.split(".")
        when {
            // Handle whole numbers
            !input.contains(".") -> {
                if (input.matches(Regex("^\\d+$"))) {
                    price = "$input.00"
                }
            }
            // Handle decimal numbers
            parts.size == 2 -> {
                val wholeNumber = parts[0]
                val decimal = parts[1].take(2) // Take only first two decimal places
                price = "$wholeNumber.$decimal"
            }
            // Invalid format
            else -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Invalid price format")
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                modifier = Modifier
                    .clickable { navController.popBackStack() }
                    .size(48.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (uploadedImageUrl != null) {
                AsyncImage(
                    model = uploadedImageUrl,
                    contentDescription = "Selected image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            } else if (selectedImageUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(selectedImageUri),
                    contentDescription = "Selected image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uploadedImageUrl == null) {
                Button(
                    onClick = { launcher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (selectedImageUri == null) "Select Image" else "Change Image")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Add price statistics chart
            NFTPriceStats(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = price,
                onValueChange = { handlePriceInput(it) },
                label = { Text("Price") },
                placeholder = { Text("12.15") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    coroutineScope.launch {
                        try {
                            val finalImageUrl = if (selectedImageUri != null) {
                                val base64Image = readBytesBase64(context, selectedImageUri!!)
                                uploadImageToImgur(clientId, base64Image)
                            } else {
                                uploadedImageUrl
                            }

                            finalImageUrl?.let {
                                val finalNftId = nftId ?: UUID.randomUUID().toString()
                                val nftData = mapOf(
                                    "id" to finalNftId,
                                    "owner" to userEmail,
                                    "imageUrl" to it,
                                    "price" to price+"$",
                                    "listingDate" to System.currentTimeMillis(),
                                    "source" to if (nftId != null) "userNFTs" else "gallery"
                                )

                                // Add to main NFTs collection (for sale)
                                FirebaseFirestore.getInstance()
                                    .collection("nfts")
                                    .document(finalNftId)
                                    .set(nftData)
                                    .await()

                                // Only remove from userNFTs if this was an existing NFT
                                if (nftId != null && userId != null) {
                                    // This NFT came from userNFTs, so remove it
                                    FirebaseFirestore.getInstance()
                                        .collection("users")
                                        .document(userId)
                                        .collection("userNFTs")
                                        .document(nftId)
                                        .delete()
                                        .await()
                                }

                                snackbarHostState.showSnackbar(
                                    if (nftId != null) "NFT moved to marketplace successfully!"
                                    else "New NFT listed for sale successfully!"
                                )
                                
                                navController.navigate("main") {
                                    popUpTo("main") { inclusive = true }
                                }
                            }
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Failed to list NFT: ${e.message}")
                        }
                    }
                },
                enabled = (selectedImageUri != null || uploadedImageUrl != null) && price.isNotBlank() && userEmail != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (nftId != null) "Move to Marketplace" else "List New NFT")
            }
        }
    }
}

// Reads bytes from Uri and encodes as Base64 string
suspend fun readBytesBase64(context: android.content.Context, uri: Uri): String = withContext(Dispatchers.IO) {
    val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
    val bytes = inputStream?.readBytes() ?: ByteArray(0)
    Base64.encodeToString(bytes, Base64.DEFAULT)
}

// Upload image to Imgur, return image URL or null on failure
suspend fun uploadImageToImgur(clientId: String, base64Image: String): String? = withContext(Dispatchers.IO) {
    try {
        val client = OkHttpClient()
        val requestBody = FormBody.Builder()
            .add("image", base64Image)
            .build()

        val request = Request.Builder()
            .url("https://api.imgur.com/3/image")
            .addHeader("Authorization", "Client-ID $clientId")
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Upload failed: ${response.message}")

        val json = JSONObject(response.body?.string() ?: "")
        json.getJSONObject("data").getString("link")
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
