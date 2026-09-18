package com.darkfirein.cryptosignal.data.repository

import com.darkfirein.cryptosignal.data.model.Candle
import com.darkfirein.cryptosignal.data.model.Ticker24hr
import com.darkfirein.cryptosignal.data.network.BinancePublicApi
import com.darkfirein.cryptosignal.data.network.toCandles
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Live market data - polls Binance's public REST API on an interval to
 * simulate a live feed (a true WebSocket stream is a further upgrade,
 * but polling every few seconds already feels live for a price list).
 */
class MarketRepository {

    private val api = BinancePublicApi.create()

    /**
     * Emits the full USDT-pair ticker list every [intervalMs], sorted by
     * quote volume (highest first) - same ordering Binance's own app uses
     * by default for its market overview.
     */
    fun observeAllUsdtTickers(intervalMs: Long = 4000): Flow<List<Ticker24hr>> = flow {
        while (true) {
            try {
                val all = api.getAllTickers()
                val usdtPairs = all
                    .filter { it.symbol.endsWith("USDT") }
                    .sortedByDescending { it.volumeValue }
                emit(usdtPairs)
            } catch (e: Exception) {
                // Network hiccup - keep the old list on screen, try again next tick
            }
            delay(intervalMs)
        }
    }

    /** Emits live candles for one symbol every [intervalMs] - powers the chart screen. */
    fun observeCandles(symbol: String, interval: String = "1h", intervalMs: Long = 5000): Flow<List<Candle>> = flow {
        while (true) {
            try {
                val raw = api.getKlines(symbol = symbol, interval = interval, limit = 100)
                emit(raw.toCandles())
            } catch (e: Exception) {
                // keep previous candles on screen if a poll fails
            }
            delay(intervalMs)
        }
    }

    suspend fun getSingleTicker(symbol: String): Ticker24hr? {
        return try {
            api.getSingleTicker(symbol)
        } catch (e: Exception) {
            null
        }
    }
}
