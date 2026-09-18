package com.darkfirein.cryptosignal.analysis

import com.darkfirein.cryptosignal.data.model.Candle
import kotlin.math.sqrt

/**
 * Same indicator logic as the Python backend (backend/indicators.py) -
 * RSI, EMA50/200, MACD, Bollinger Bands, and a simple confluence score.
 * Kept in Kotlin so the app can run an instant analysis the moment a coin
 * is added, without waiting for the next backend cron cycle.
 */
data class IndicatorSnapshot(
    val price: Double,
    val rsi: Double?,
    val emaTrend: String,       // "bullish" | "bearish"
    val macdStatus: String,     // "bullish" | "bearish"
    val bbPosition: String,     // "lower_band" | "upper_band" | "mid_range"
    val score: Int,
    val reasons: List<String>,
    val recentLow: Double,
    val recentHigh: Double
)

object IndicatorMath {

    private fun ema(values: List<Double>, period: Int): List<Double?> {
        val result = MutableList<Double?>(values.size) { null }
        if (values.size < period) return result
        val k = 2.0 / (period + 1)
        var prevEma = values.take(period).average()
        result[period - 1] = prevEma
        for (i in period until values.size) {
            prevEma = values[i] * k + prevEma * (1 - k)
            result[i] = prevEma
        }
        return result
    }

    private fun rsi(closes: List<Double>, period: Int = 14): List<Double?> {
        val result = MutableList<Double?>(closes.size) { null }
        if (closes.size <= period) return result

        var avgGain = 0.0
        var avgLoss = 0.0
        for (i in 1..period) {
            val change = closes[i] - closes[i - 1]
            if (change > 0) avgGain += change else avgLoss -= change
        }
        avgGain /= period
        avgLoss /= period
        result[period] = calcRsi(avgGain, avgLoss)

        for (i in period + 1 until closes.size) {
            val change = closes[i] - closes[i - 1]
            val gain = if (change > 0) change else 0.0
            val loss = if (change < 0) -change else 0.0
            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period
            result[i] = calcRsi(avgGain, avgLoss)
        }
        return result
    }

    private fun calcRsi(avgGain: Double, avgLoss: Double): Double {
        if (avgLoss == 0.0) return 100.0
        val rs = avgGain / avgLoss
        return 100 - (100 / (1 + rs))
    }

    private fun sma(values: List<Double>, period: Int): List<Double?> {
        val result = MutableList<Double?>(values.size) { null }
        for (i in values.indices) {
            if (i >= period - 1) {
                result[i] = values.subList(i - period + 1, i + 1).average()
            }
        }
        return result
    }

    private fun stddev(values: List<Double>, period: Int): List<Double?> {
        val result = MutableList<Double?>(values.size) { null }
        for (i in values.indices) {
            if (i >= period - 1) {
                val window = values.subList(i - period + 1, i + 1)
                val mean = window.average()
                val variance = window.sumOf { (it - mean) * (it - mean) } / period
                result[i] = sqrt(variance)
            }
        }
        return result
    }

    fun analyze(candles: List<Candle>): IndicatorSnapshot? {
        if (candles.size < 30) return null // not enough data for reliable indicators

        val closes = candles.map { it.close }
        val lastIndex = closes.lastIndex

        val rsiValues = rsi(closes, 14)
        val ema50 = ema(closes, 50)
        val ema200 = ema(closes, 200)
        val macdLine = run {
            val ema12 = ema(closes, 12)
            val ema26 = ema(closes, 26)
            closes.indices.map { i ->
                val a = ema12[i]; val b = ema26[i]
                if (a != null && b != null) a - b else null
            }
        }
        val macdSignal = run {
            val macdNonNull = macdLine.map { it ?: 0.0 }
            ema(macdNonNull, 9)
        }
        val smaValues = sma(closes, 20)
        val stdValues = stddev(closes, 20)

        val price = closes[lastIndex]
        val rsiNow = rsiValues[lastIndex]
        val ema50Now = ema50[lastIndex]
        val ema200Now = ema200[lastIndex]
        val macdNow = macdLine[lastIndex]
        val macdSignalNow = macdSignal.getOrNull(lastIndex)
        val smaNow = smaValues[lastIndex]
        val stdNow = stdValues[lastIndex]

        var score = 0
        val reasons = mutableListOf<String>()

        if (rsiNow != null) {
            if (rsiNow < 30) { score += 1; reasons.add("RSI oversold (%.1f)".format(rsiNow)) }
            else if (rsiNow > 70) { score -= 1; reasons.add("RSI overbought (%.1f)".format(rsiNow)) }
        }

        val emaTrend: String
        if (ema50Now != null && ema200Now != null) {
            if (ema50Now > ema200Now) {
                score += 1; reasons.add("EMA50 above EMA200 (bullish trend)"); emaTrend = "bullish"
            } else {
                score -= 1; reasons.add("EMA50 below EMA200 (bearish trend)"); emaTrend = "bearish"
            }
        } else emaTrend = "bearish"

        val macdStatus: String
        if (macdNow != null && macdSignalNow != null) {
            if (macdNow > macdSignalNow) {
                score += 1; reasons.add("MACD bullish crossover"); macdStatus = "bullish"
            } else {
                score -= 1; reasons.add("MACD bearish crossover"); macdStatus = "bearish"
            }
        } else macdStatus = "bearish"

        var bbPosition = "mid_range"
        if (smaNow != null && stdNow != null) {
            val lowerBand = smaNow - 2 * stdNow
            val upperBand = smaNow + 2 * stdNow
            if (price <= lowerBand) {
                score += 1; reasons.add("Price at lower Bollinger Band"); bbPosition = "lower_band"
            } else if (price >= upperBand) {
                score -= 1; reasons.add("Price at upper Bollinger Band"); bbPosition = "upper_band"
            }
        }

        val recentLows = candles.takeLast(10).map { it.low }
        val recentHighs = candles.takeLast(10).map { it.high }

        return IndicatorSnapshot(
            price = price,
            rsi = rsiNow,
            emaTrend = emaTrend,
            macdStatus = macdStatus,
            bbPosition = bbPosition,
            score = score,
            reasons = reasons,
            recentLow = recentLows.min(),
            recentHigh = recentHighs.max()
        )
    }
}
