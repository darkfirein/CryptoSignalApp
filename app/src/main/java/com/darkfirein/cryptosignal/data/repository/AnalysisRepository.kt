package com.darkfirein.cryptosignal.data.repository

import com.darkfirein.cryptosignal.BuildConfig
import com.darkfirein.cryptosignal.analysis.IndicatorMath
import com.darkfirein.cryptosignal.analysis.IndicatorSnapshot
import com.darkfirein.cryptosignal.data.model.Signal
import com.darkfirein.cryptosignal.data.network.BinancePublicApi
import com.darkfirein.cryptosignal.data.network.GeminiApi
import com.darkfirein.cryptosignal.data.network.GeminiContent
import com.darkfirein.cryptosignal.data.network.GeminiPart
import com.darkfirein.cryptosignal.data.network.GeminiRequest
import com.darkfirein.cryptosignal.data.network.toCandles
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

private const val AI_TRIGGER_THRESHOLD = 1

class AnalysisRepository {

    private val binanceApi = BinancePublicApi.create()
    private val geminiApi = GeminiApi.create()
    private val gson = Gson()

    /**
     * Runs the same logic as the backend's main.py, but instantly on-device:
     * fetch candles -> compute indicators -> call Gemini only if indicators
     * already lean one way -> return a ready-to-save Signal.
     */
    suspend fun analyzeSymbol(symbol: String): Signal {
        val klines = binanceApi.getKlines(symbol = symbol, interval = "1h", limit = 200)
        val candles = klines.toCandles()

        val snapshot = IndicatorMath.analyze(candles)
            ?: return holdSignal(symbol, candles.lastOrNull()?.close ?: 0.0, "Not enough price history yet.")

        return if (kotlin.math.abs(snapshot.score) >= AI_TRIGGER_THRESHOLD) {
            try {
                callGemini(symbol, snapshot)
            } catch (e: Exception) {
                fallbackFromIndicators(symbol, snapshot, note = "AI reasoning unavailable right now - showing indicator-only result.")
            }
        } else {
            holdSignal(symbol, snapshot.price, "No strong indicator confluence right now.")
        }
    }

    private suspend fun callGemini(symbol: String, snapshot: IndicatorSnapshot): Signal {
        val prompt = """
            You are a disciplined crypto trading analyst who prioritizes risk management over chasing gains.

            Analyze this data and respond with a signal:

            Coin: $symbol
            Current Price: ${snapshot.price}
            RSI (14): ${snapshot.rsi}
            EMA50 vs EMA200 trend: ${snapshot.emaTrend}
            MACD status: ${snapshot.macdStatus}
            Bollinger Band position: ${snapshot.bbPosition}
            Indicator confluence score (-4 to +4): ${snapshot.score}
            Reasons from indicators: ${if (snapshot.reasons.isEmpty()) "None strong" else snapshot.reasons.joinToString(", ")}
            Recent 10-candle low: ${snapshot.recentLow}
            Recent 10-candle high: ${snapshot.recentHigh}

            Respond ONLY with valid JSON, no markdown fences, no extra text:
            {
              "signal": "BUY" or "SELL" or "HOLD",
              "confidence": "Strong" or "Moderate" or "Weak",
              "reasoning": "2-3 sentence explanation combining technical factors",
              "suggested_stop_loss": number or null,
              "risk_note": "short specific risk warning for this coin's current situation"
            }
        """.trimIndent()

        val response = geminiApi.generateContent(
            apiKey = BuildConfig.GEMINI_API_KEY,
            request = GeminiRequest(contents = listOf(GeminiContent(parts = listOf(GeminiPart(prompt)))))
        )

        val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: return fallbackFromIndicators(symbol, snapshot, "AI returned no response - showing indicator-only result.")

        val cleaned = rawText.replace("```json", "").replace("```", "").trim()

        return try {
            val parsed = gson.fromJson(cleaned, GeminiSignalJson::class.java)
            Signal(
                symbol = symbol,
                signal = parsed.signal ?: "HOLD",
                confidence = parsed.confidence ?: "Weak",
                price = snapshot.price,
                stopLoss = parsed.suggested_stop_loss,
                reasoning = parsed.reasoning ?: "No reasoning provided.",
                riskNote = parsed.risk_note ?: "",
                timestamp = System.currentTimeMillis()
            )
        } catch (e: JsonSyntaxException) {
            fallbackFromIndicators(symbol, snapshot, "AI response could not be parsed - showing indicator-only result.")
        }
    }

    private fun fallbackFromIndicators(symbol: String, snapshot: IndicatorSnapshot, note: String): Signal {
        val signal = when {
            snapshot.score >= 1 -> "BUY"
            snapshot.score <= -1 -> "SELL"
            else -> "HOLD"
        }
        val confidence = if (kotlin.math.abs(snapshot.score) >= 3) "Strong" else "Moderate"
        return Signal(
            symbol = symbol,
            signal = signal,
            confidence = confidence,
            price = snapshot.price,
            stopLoss = if (signal == "BUY") snapshot.recentLow * 0.98
                       else if (signal == "SELL") snapshot.recentHigh * 1.02
                       else null,
            reasoning = if (snapshot.reasons.isEmpty()) note else snapshot.reasons.joinToString(", ") + ". $note",
            riskNote = note,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun holdSignal(symbol: String, price: Double, note: String): Signal = Signal(
        symbol = symbol,
        signal = "HOLD",
        confidence = "Weak",
        price = price,
        stopLoss = null,
        reasoning = note,
        riskNote = note,
        timestamp = System.currentTimeMillis()
    )

    private data class GeminiSignalJson(
        val signal: String?,
        val confidence: String?,
        val reasoning: String?,
        val suggested_stop_loss: Double?,
        val risk_note: String?
    )
}
