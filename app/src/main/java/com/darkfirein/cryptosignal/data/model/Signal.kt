package com.darkfirein.cryptosignal.data.model

data class Signal(
    val symbol: String,
    val signal: String,        // "BUY" | "SELL" | "NEUTRAL"
    val confidence: String,    // "Strong" | "Moderate" | "Weak"
    val price: Double,
    val stopLoss: Double?,
    val reasoning: String,
    val riskNote: String,
    val timestamp: Long
)

data class WatchlistCoin(
    val symbol: String,
    val addedAt: Long = System.currentTimeMillis()
)
