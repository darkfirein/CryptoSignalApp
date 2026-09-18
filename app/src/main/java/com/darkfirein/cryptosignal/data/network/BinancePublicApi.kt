package com.darkfirein.cryptosignal.data.network

import com.darkfirein.cryptosignal.data.model.Candle
import com.darkfirein.cryptosignal.data.model.Ticker24hr
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Talks directly to Binance's public market-data mirror from the phone.
 * data-api.binance.vision serves the same public data as binance.com but
 * without the regional restriction that blocks US-hosted servers
 * (which is why the backend on GitHub Actions needed a different fix).
 * Calling it straight from a phone in Malaysia works fine either way.
 */
interface BinancePublicApi {

    @GET("api/v3/klines")
    suspend fun getKlines(
        @Query("symbol") symbol: String,
        @Query("interval") interval: String = "1h",
        @Query("limit") limit: Int = 100
    ): List<List<Any>>

    @GET("api/v3/ticker/24hr")
    suspend fun getAllTickers(): List<Ticker24hr>

    @GET("api/v3/ticker/24hr")
    suspend fun getSingleTicker(
        @Query("symbol") symbol: String
    ): Ticker24hr

    companion object {
        fun create(): BinancePublicApi {
            return Retrofit.Builder()
                .baseUrl("https://data-api.binance.vision/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(BinancePublicApi::class.java)
        }
    }
}

/** Converts raw Binance kline arrays into typed Candle objects. */
fun List<List<Any>>.toCandles(): List<Candle> = map { row ->
    Candle(
        openTime = (row[0] as Double).toLong(),
        open = (row[1] as String).toDouble(),
        high = (row[2] as String).toDouble(),
        low = (row[3] as String).toDouble(),
        close = (row[4] as String).toDouble(),
        volume = (row[5] as String).toDouble()
    )
}
