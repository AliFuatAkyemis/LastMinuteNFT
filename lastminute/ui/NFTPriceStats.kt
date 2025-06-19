package com.example.lastminute.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class PriceRange(
    val range: String,
    val count: Int
)

@Composable
fun NFTPriceStats(
    modifier: Modifier = Modifier
) {
    var priceRanges by remember { mutableStateOf<List<PriceRange>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var maxCount by remember { mutableStateOf(0) }

    // Fetch price data
    LaunchedEffect(Unit) {
        try {
            val snapshot = FirebaseFirestore.getInstance()
                .collection("nfts")
                .get()
                .await()

            val prices = snapshot.documents.mapNotNull { doc ->
                val priceStr = doc.getString("price")?.removeSuffix("$") ?: return@mapNotNull null
                priceStr.toDoubleOrNull()
            }

            // Create price ranges
            val ranges = mutableMapOf<String, Int>()
            prices.forEach { price ->
                val range = when {
                    price < 10 -> "0-10"
                    price < 50 -> "10-50"
                    price < 100 -> "50-100"
                    price < 500 -> "100-500"
                    else -> "500+"
                }
                ranges[range] = (ranges[range] ?: 0) + 1
            }

            // Convert to list and sort
            priceRanges = listOf(
                "0-10", "10-50", "50-100", "100-500", "500+"
            ).map { range ->
                PriceRange(range, ranges[range] ?: 0)
            }

            maxCount = priceRanges.maxOfOrNull { it.count } ?: 0
            isLoading = false
        } catch (e: Exception) {
            // Handle error
            isLoading = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(16.dp)
    ) {
        Text(
            text = "NFT Price Distribution ($)",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (isLoading) {
            Text("Loading price statistics...")
        } else if (priceRanges.isEmpty()) {
            Text("No price data available")
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp)
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val barWidth = size.width / priceRanges.size
                    val maxHeight = size.height - 40f

                    // Draw bars
                    priceRanges.forEachIndexed { index, range ->
                        val barHeight = if (maxCount > 0) {
                            (range.count.toFloat() / maxCount) * maxHeight
                        } else 0f

                        drawRect(
                            color = Color.Red.copy(alpha = 0.7f),
                            topLeft = Offset(
                                x = index * barWidth,
                                y = size.height - barHeight
                            ),
                            size = Size(
                                width = barWidth * 0.8f,
                                height = barHeight
                            )
                        )
                    }
                }

                // Draw labels
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    priceRanges.forEach { range ->
                        Text(
                            text = range.range,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
} 