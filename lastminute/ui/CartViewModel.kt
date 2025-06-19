package com.example.lastminute.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.example.lastminute.model.NftItem

class CartViewModel : ViewModel() {
    val cartItems = mutableStateListOf<NftItem>()

    fun addToCart(nft: NftItem) {
        cartItems.add(nft)
    }

    fun getItemCount(): Int = cartItems.size

    fun getTotalPrice(): Double {
        return cartItems.sumOf { it.price.replace(",", ".").replace("$", "").toDoubleOrNull() ?: 0.0 }
    }
}
