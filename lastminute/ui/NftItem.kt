package com.example.lastminute.model

data class NftItem(
    val id: String = "0",
    val price: String = "",
    val owner: String = "",
    val imageUrl: String = "",
    val isFavorite: Boolean = false
)