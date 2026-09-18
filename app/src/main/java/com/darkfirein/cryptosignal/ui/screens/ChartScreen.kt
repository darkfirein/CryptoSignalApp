package com.darkfirein.cryptosignal.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkfirein.cryptosignal.data.model.Candle
import com.darkfirein.cryptosignal.data.model.Ticker24hr
import com.darkfirein.cryptosignal.ui.theme.*
import java.text.DecimalFormat

private val priceFormat = DecimalFormat("#,##0.00######")
private val intervals = listOf("1s", "1m", "5m", "15m", "1h", "4h", "1d")

private enum class ChartType { CANDLE, LINE }

@Composable
fun ChartScreen(
    symbol: String,
    ticker: Ticker24hr?,
    candles: List<Candle>,
    selectedInterval: String,
    onIntervalChange: (String) -> Unit,
    onBack: () -> Unit
) {
    // Candlestick is the default view - matches what traders expect to see first
    var chartType by remember { mutableStateOf(ChartType.CANDLE) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(top = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = symbol.removeSuffix("USDT") + "/USDT",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                ticker?.let {
                    val isUp = it.changePercentValue >= 0
                    Text(
                        text = "$${priceFormat.format(it.priceValue)}  " +
                            "${if (isUp) "+" else ""}${String.format("%.2f", it.changePercentValue)}%",
                        color = if (isUp) BuyGreen else SellRed,
                        fontSize = 14.sp
                    )
                }
            }
            ChartTypeToggle(selected = chartType, onSelect = { chartType = it })
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            intervals.forEach { interval ->
                val selected = interval == selectedInterval
                Box(
                    modifier = Modifier
                        .background(
                            if (selected) GoldAccent else SurfaceDark,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { onIntervalChange(interval) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = interval,
                        color = if (selected) BackgroundDark else TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (candles.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(280.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = GoldAccent)
            }
        } else {
            when (chartType) {
                ChartType.CANDLE -> CandlestickChartView(candles = candles, modifier = Modifier.padding(horizontal = 12.dp))
                ChartType.LINE -> PriceLineChart(candles = candles, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (candles.isNotEmpty()) {
            val last = candles.last()
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatColumn(label = "24h High", value = "$${priceFormat.format(ticker?.highPrice?.toDoubleOrNull() ?: last.high)}")
                StatColumn(label = "24h Low", value = "$${priceFormat.format(ticker?.lowPrice?.toDoubleOrNull() ?: last.low)}")
                StatColumn(label = "Volume", value = formatVolume(ticker?.volumeValue ?: 0.0))
            }
        }
    }
}

@Composable
private fun ChartTypeToggle(selected: ChartType, onSelect: (ChartType) -> Unit) {
    Row(
        modifier = Modifier
            .background(SurfaceDark, RoundedCornerShape(12.dp))
            .padding(4.dp)
    ) {
        ToggleIcon(
            icon = Icons.Default.CandlestickChart,
            isSelected = selected == ChartType.CANDLE,
            onClick = { onSelect(ChartType.CANDLE) }
        )
        ToggleIcon(
            icon = Icons.Default.ShowChart,
            isSelected = selected == ChartType.LINE,
            onClick = { onSelect(ChartType.LINE) }
        )
    }
}

@Composable
private fun ToggleIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                if (isSelected) GoldAccent else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) BackgroundDark else TextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = TextSecondary, fontSize = 11.sp)
        Text(text = value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

private fun formatVolume(v: Double): String = when {
    v >= 1_000_000_000 -> "%.2fB".format(v / 1_000_000_000)
    v >= 1_000_000 -> "%.2fM".format(v / 1_000_000)
    v >= 1_000 -> "%.2fK".format(v / 1_000)
    else -> "%.2f".format(v)
}

/** Hand-drawn line chart with a gradient fill - no external charting library needed. */
@Composable
private fun PriceLineChart(candles: List<Candle>, modifier: Modifier = Modifier) {
    val closes = candles.map { it.close }
    val minPrice = closes.min()
    val maxPrice = closes.max()
    val priceRange = (maxPrice - minPrice).takeIf { it > 0 } ?: 1.0

    val isUpOverall = closes.last() >= closes.first()
    val lineColor = if (isUpOverall) BuyGreen else SellRed

    Canvas(modifier = modifier.fillMaxWidth().height(240.dp)) {
        val widthStep = size.width / (closes.size - 1).coerceAtLeast(1)

        fun yFor(price: Double): Float {
            val ratio = (price - minPrice) / priceRange
            return size.height - (ratio * size.height).toFloat()
        }

        val linePath = Path()
        val fillPath = Path()

        closes.forEachIndexed { index, price ->
            val x = index * widthStep
            val y = yFor(price)
            if (index == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, size.height)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(size.width, size.height)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.25f), lineColor.copy(alpha = 0f))
            )
        )
        drawPath(path = linePath, color = lineColor, style = Stroke(width = 4f, cap = StrokeCap.Round))
    }
}

/**
 * Hand-drawn candlestick chart - each candle shows a wick (high-low range)
 * and a body (open-close range), colored green when close >= open and red
 * otherwise, exactly like Binance/TradingView candlestick charts.
 */
@Composable
private fun CandlestickChartView(candles: List<Candle>, modifier: Modifier = Modifier) {
    val minPrice = candles.minOf { it.low }
    val maxPrice = candles.maxOf { it.high }
    val priceRange = (maxPrice - minPrice).takeIf { it > 0 } ?: 1.0

    Canvas(modifier = modifier.fillMaxWidth().height(280.dp)) {
        val slotWidth = size.width / candles.size
        val bodyWidth = (slotWidth * 0.6f).coerceAtLeast(2f)

        fun yFor(price: Double): Float {
            val ratio = (price - minPrice) / priceRange
            return size.height - (ratio * size.height).toFloat()
        }

        candles.forEachIndexed { index, candle ->
            val centerX = index * slotWidth + slotWidth / 2f
            val isBullish = candle.close >= candle.open
            val color = if (isBullish) BuyGreen else SellRed

            val highY = yFor(candle.high)
            val lowY = yFor(candle.low)
            val openY = yFor(candle.open)
            val closeY = yFor(candle.close)

            // Wick (high-low line)
            drawLine(
                color = color,
                start = Offset(centerX, highY),
                end = Offset(centerX, lowY),
                strokeWidth = 2f
            )

            // Body (open-close rectangle)
            val bodyTop = minOf(openY, closeY)
            val bodyBottom = maxOf(openY, closeY).coerceAtLeast(bodyTop + 2f)
            drawRect(
                color = color,
                topLeft = Offset(centerX - bodyWidth / 2f, bodyTop),
                size = androidx.compose.ui.geometry.Size(bodyWidth, bodyBottom - bodyTop)
            )
        }
    }
}
