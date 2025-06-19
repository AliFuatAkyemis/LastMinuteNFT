package com.example.lastminute.ui

class ChatBotService {
    private val responses = mapOf(
        "payment" to "I understand you're having issues with payment. Here are some common solutions:\n1. Check if your card has sufficient balance\n2. Ensure card details are correct\n3. Try a different payment method",
        "order" to "For order related issues, please provide your order number and I can help you track it.",
        "delivery" to "Regarding delivery, our standard delivery time is 3-5 business days. For specific tracking, please provide your order number.",
        "refund" to "For refund requests, please note that it typically takes 5-7 business days to process once approved.",
        "help" to "I'm here to help! You can ask me about:\n- Payment issues\n- Order tracking\n- Delivery status\n- Refund requests",
    )

    fun generateResponse(userMessage: String): String {
        val lowercaseMessage = userMessage.lowercase()
        
        // Check for greetings
        if (lowercaseMessage.contains("hi") || lowercaseMessage.contains("hello")) {
            return "Hello! How can I assist you today?"
        }

        // Check for keywords in the message
        for ((keyword, response) in responses) {
            if (lowercaseMessage.contains(keyword)) {
                return response
            }
        }

        // Default response if no keyword matches
        return "I'm not sure I understand. Could you please rephrase that or ask about payments, orders, delivery, or refunds?"
    }
} 