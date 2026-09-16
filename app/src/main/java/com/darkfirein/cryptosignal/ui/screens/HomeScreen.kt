package com.darkfirein.cryptosignal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkfirein.cryptosignal.data.model.Signal
import com.darkfirein.cryptosignal.data.model.WatchlistCoin
import com.darkfirein.cryptosignal.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    watchlist: List<WatchlistCoin>,
    signals: List<Signal>,
    onAddCoin: (String) -> Unit,
    onRemoveCoin: (String) -> Unit
) {
    var newCoin by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(top = 24.dp)
    ) {
        Text(
            text = "Your Signals",
            color = TextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${watchlist.size} coins tracked",
            color = TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Add coin row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newCoin,
                onValueChange = { newCoin = it.uppercase() },
                placeholder = { Text("e.g. SOLUSDT", color = TextSecondary) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldAccent,
                    unfocusedBorderColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilledIconButton(
                onClick = {
                    if (newCoin.isNotBlank()) {
                        onAddCoin(newCoin.trim())
                        newCoin = ""
                    }
                },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = GoldAccent)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add coin", tint = BackgroundDark)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Watchlist chips
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

        if (signals.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No signals yet.\nAdd a coin above to start tracking.",
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
}
