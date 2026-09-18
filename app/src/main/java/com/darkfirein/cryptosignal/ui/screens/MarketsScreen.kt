package com.darkfirein.cryptosignal.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkfirein.cryptosignal.data.model.Ticker24hr
import com.darkfirein.cryptosignal.ui.theme.*
import java.text.DecimalFormat

private val priceFormat = DecimalFormat("#,##0.00######")

@Composable
fun MarketsScreen(
    tickers: List<Ticker24hr>,
    onCoinClick: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(tickers, searchQuery) {
        if (searchQuery.isBlank()) tickers
        else tickers.filter { it.symbol.contains(searchQuery.uppercase()) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(top = 24.dp)
    ) {
        Text(
            text = "Markets",
            color = TextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Text(
            text = "Live prices \u2022 ${tickers.size} pairs",
            color = TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search coin (e.g. BTC)", color = TextSecondary) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GoldAccent,
                unfocusedBorderColor = TextSecondary,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Pair", color = TextSecondary, fontSize = 12.sp)
            Text("Price / 24h Change", color = TextSecondary, fontSize = 12.sp)
        }

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (tickers.isEmpty()) {
                    CircularProgressIndicator(color = GoldAccent)
                } else {
                    Text("No coins match \"$searchQuery\"", color = TextSecondary)
                }
            }
        } else {
            LazyColumn {
                items(filtered, key = { it.symbol }) { ticker ->
                    TickerRow(ticker = ticker, onClick = { onCoinClick(ticker.symbol) })
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
fun TickerRow(ticker: Ticker24hr, onClick: () -> Unit) {
    val isUp = ticker.changePercentValue >= 0
    val changeColor = if (isUp) BuyGreen else SellRed

    // Flash the price color briefly whenever it updates - gives that "live ticking" feel
    var flashColor by remember(ticker.symbol) { mutableStateOf(TextPrimary) }
    var lastPrice by remember(ticker.symbol) { mutableStateOf(ticker.priceValue) }

    LaunchedEffect(ticker.priceValue) {
        if (ticker.priceValue != lastPrice) {
            flashColor = if (ticker.priceValue > lastPrice) BuyGreen else SellRed
            lastPrice = ticker.priceValue
        }
    }
    val animatedColor by animateColorAsState(
        targetValue = flashColor,
        animationSpec = tween(600),
        label = "priceFlash"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = ticker.symbol.removeSuffix("USDT"),
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "/USDT",
                color = TextSecondary,
                fontSize = 11.sp
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$${priceFormat.format(ticker.priceValue)}",
                color = animatedColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${if (isUp) "+" else ""}${String.format("%.2f", ticker.changePercentValue)}%",
                color = changeColor,
                fontSize = 12.sp
            )
        }
    }
}
