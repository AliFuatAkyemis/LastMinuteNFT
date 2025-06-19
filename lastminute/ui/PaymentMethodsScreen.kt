package com.example.lastminute.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.lastminute.R

@Composable
fun PaymentMethodsScreen(navController: NavHostController) {
    var defaultCard by remember { mutableStateOf("card1") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Üst bar
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .clickable { navController.popBackStack("main", false) }
                    .size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "Payment methods", 
                fontSize = 20.sp, 
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Kart 1
        Image(
            painter = painterResource(id = R.drawable.card1),
            contentDescription = "Card 1",
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = defaultCard == "card1",
                onCheckedChange = { if (it) defaultCard = "card1" },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Text(
                "Use as default payment method", 
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Kart 2
        Image(
            painter = painterResource(id = R.drawable.card2),
            contentDescription = "Card 2",
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = defaultCard == "card2",
                onCheckedChange = { if (it) defaultCard = "card2" },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Text(
                "Use as default payment method", 
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Add Card butonu (kırmızı, sağ altta)
        Button(
            onClick = { navController.navigate("add-card") },
            modifier = Modifier.align(Alignment.End),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                "+", 
                fontSize = 20.sp, 
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
