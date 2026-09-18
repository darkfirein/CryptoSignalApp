package com.darkfirein.cryptosignal.data.model

data class Ticker24hr(
    val symbol: String,
    val lastPrice: String,
    val priceChangePercent: String,
    val highPrice: String,
    val lowPrice: String,
    val quoteVolume: String
) {
    val priceValue: Double get() = lastPrice.toDoubleOrNull() ?: 0.0
    val changePercentValue: Double get() = priceChangePercent.toDoubleOrNull() ?: 0.0
    val volumeValue: Double get() = quoteVolume.toDoubleOrNull() ?: 0.0
}

data class Candle(
    val openTime: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)
