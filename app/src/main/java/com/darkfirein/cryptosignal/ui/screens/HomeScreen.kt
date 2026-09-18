package com.darkfirein.cryptosignal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.darkfirein.cryptosignal.data.model.Signal
import com.darkfirein.cryptosignal.data.model.Ticker24hr
import com.darkfirein.cryptosignal.data.model.WatchlistCoin
import com.darkfirein.cryptosignal.ui.theme.*

@Composable
fun HomeScreen(
    watchlist: List<WatchlistCoin>,
    signals: List<Signal>,
    availableCoins: List<Ticker24hr>,
    onAddCoin: (String) -> Unit,
    onRemoveCoin: (String) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(top = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Your Signals",
                    color = TextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${watchlist.size} coins tracked",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
            FilledIconButton(
                onClick = { showPicker = true },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = GoldAccent)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add coin", tint = BackgroundDark)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (watchlist.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                watchlist.forEach { coin ->
                    AssistChip(
                        onClick = { onRemoveCoin(coin.symbol) },
                        label = { Text(coin.symbol, fontSize = 12.sp) },
                        trailingIcon = {
                            Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp))
                        },
                        modifier = Modifier.padding(end = 6.dp),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = SurfaceDark,
                            labelColor = TextPrimary
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (signals.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (watchlist.isEmpty())
                        "No coins yet.\nTap + to add one from the live market list."
                    else
                        "Waiting for the next analysis cycle\u2026",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn {
                items(signals) { signal ->
                    SignalCard(signal = signal)
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }

    if (showPicker) {
        CoinPickerDialog(
            availableCoins = availableCoins,
            alreadyAdded = watchlist.map { it.symbol }.toSet(),
            onPick = { symbol ->
                onAddCoin(symbol)
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }
}

@Composable
private fun CoinPickerDialog(
    availableCoins: List<Ticker24hr>,
    alreadyAdded: Set<String>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    val filtered = remember(availableCoins, query, alreadyAdded) {
        availableCoins
            .filter { it.symbol !in alreadyAdded }
            .filter { query.isBlank() || it.symbol.contains(query.uppercase()) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = SurfaceDark,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Add a coin to track",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search (e.g. BTC)", color = TextSecondary) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldAccent,
                        unfocusedBorderColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (availableCoins.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = GoldAccent)
                    }
                } else if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No matching coins", color = TextSecondary)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(filtered, key = { it.symbol }) { ticker ->
                            val isUp = ticker.changePercentValue >= 0
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPick(ticker.symbol) }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = ticker.symbol.removeSuffix("USDT") + "/USDT",
                                    color = TextPrimary,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${if (isUp) "+" else ""}${String.format("%.2f", ticker.changePercentValue)}%",
                                    color = if (isUp) BuyGreen else SellRed,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        }
    }
}
