package com.example.lastminute.util

import androidx.compose.runtime.mutableStateListOf
import com.example.lastminute.model.NftItem

object CartManager {

    // Sepetteki ürünler
    val cartItems = mutableStateListOf<NftItem>()

    // NFT sepete eklenir, eğer zaten ekliyse tekrar eklenmez
    fun addToCart(item: NftItem) {
        if (!isInCart(item)) {
            cartItems.add(item)
        }
    }

    // NFT sepetten çıkarılır
    fun removeFromCart(item: NftItem) {
        cartItems.removeAll { it.id == item.id }
    }

    // Sepette bu NFT var mı?
    fun isInCart(item: NftItem): Boolean {
        return cartItems.any { it.id == item.id }
    }

    // Sepette kaç ürün var?
    fun getCartSize(): Int = cartItems.size

    // Sepetteki toplam fiyatı hesaplar
    fun getTotalPrice(): Double {
        return cartItems.sumOf {
            it.price
                .replace(",", ".") // 15,49 → 15.49
                .replace(Regex("[^\\d.]"), "") // $ gibi sembolleri sil
                .toDoubleOrNull() ?: 0.0
        }
    }

    // Sepeti temizle
    fun clearCart() {
        cartItems.clear()
    }
}
