package com.example.lastminute.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import com.example.lastminute.util.CartManager
import com.example.lastminute.model.NftItem
import com.example.lastminute.R

@Composable
fun CartScreen(navController: NavHostController) {
    val cartItems = CartManager.cartItems
    val total = cartItems.sumOf {
        it.price.replace("$", "").replace(",", ".").toDoubleOrNull() ?: 0.0
    }
    var showPurchase by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "My Cart",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                "Close",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                modifier = Modifier.clickable { navController.popBackStack("main", false) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(cartItems) { item ->
                Box {
                    NFTCard(
                        nft = item,
                        onFavoriteClick = {},
                        onClick = {},
                        false
                    )

                    Image(
                        painter = painterResource(id = R.drawable.delete),
                        contentDescription = "Remove",
                        modifier = Modifier
                            .size(36.dp)
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clickable {
                                CartManager.removeFromCart(item)
                            }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Total: $${String.format("%.2f", total)}",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { showPurchase = true },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Purchase", color = MaterialTheme.colorScheme.onPrimary, fontSize = 16.sp)
        }
    }

    if (showPurchase) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 60.dp, end = 16.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            navController.navigate("purchase")
        }
    }
}
