package com.example.lastminute.util

import android.content.Context
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

// Reads bytes from Uri and encodes as Base64 string
suspend fun readBytesBase64(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
    val inputStream = context.contentResolver.openInputStream(uri)
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