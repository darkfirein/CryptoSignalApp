package com.darkfirein.cryptosignal.data.model

// All properties need default values so Kotlin generates a public no-arg
// constructor - Firestore's toObject() deserialization requires one, and
// without it the app crashes as soon as it tries to read this data back
// from Firestore (e.g. right after adding a coin to the watchlist).
data class Signal(
    val symbol: String = "",
    val signal: String = "",        // "BUY" | "SELL" | "NEUTRAL"
    val confidence: String = "",    // "Strong" | "Moderate" | "Weak"
    val price: Double = 0.0,
    val stopLoss: Double? = null,
    val reasoning: String = "",
    val riskNote: String = "",
    val timestamp: Long = 0L
)

data class WatchlistCoin(
    val symbol: String = "",
    val addedAt: Long = System.currentTimeMillis()
)
