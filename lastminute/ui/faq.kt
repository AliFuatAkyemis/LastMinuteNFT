package com.example.lastminute.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FAQScreen(onBackClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Top Bar
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
                "Frequently Asked Questions", 
                fontSize = 20.sp, 
                fontWeight = FontWeight.Bold, 
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        FAQItem(
            question = "How do I purchase an NFT?",
            answer = "You can purchase an NFT by selecting it from the Explore tab, adding it to your cart, and proceeding to payment."
        )

        FAQItem(
            question = "Can I return an NFT after buying?",
            answer = "Unfortunately, NFTs are non-refundable once purchased. Please review carefully before buying."
        )
    }
}

@Composable
fun FAQItem(question: String, answer: String) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = question, 
            fontWeight = FontWeight.SemiBold, 
            fontSize = 16.sp, 
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = answer, 
            fontSize = 14.sp, 
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
