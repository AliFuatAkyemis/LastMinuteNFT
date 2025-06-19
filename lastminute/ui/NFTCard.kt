package com.example.lastminute.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lastminute.model.NftItem

@Composable
fun NFTCard(
    nft: NftItem,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit,
    showFavorite: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clickable { onClick() }
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (nft.imageUrl.isNotBlank()) {
                ImgurImage(
                    url = nft.imageUrl,
                    contentDescription = "NFT Image #${nft.id}",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                // Fallback placeholder or empty box
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                nft.owner, 
                fontSize = 14.sp, 
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                nft.price, 
                fontSize = 16.sp, 
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(5.dp))

            if (showFavorite) {
                IconButton(onClick = onFavoriteClick) {
                    if (nft.isFavorite) {
                        Icon(
                            Icons.Filled.Favorite, 
                            contentDescription = "Unfavorite", 
                            tint = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            Icons.Outlined.FavoriteBorder, 
                            contentDescription = "Favorite", 
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

