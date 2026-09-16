package com.darkfirein.cryptosignal.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkfirein.cryptosignal.data.model.Signal
import com.darkfirein.cryptosignal.ui.theme.*

@Composable
fun SignalCard(signal: Signal, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(signal.symbol) { visible = true }

    val signalColor = when (signal.signal) {
        "BUY" -> BuyGreen
        "SELL" -> SellRed
        else -> TextSecondary
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(400)) + slideInVertically(
            initialOffsetY = { it / 3 },
            animationSpec = tween(400, easing = FastOutSlowInEasing)
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = signal.symbol,
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    PulsingBadge(text = signal.signal, color = signalColor)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Price: $${signal.price}",
                    color = TextSecondary,
                    fontSize = 14.sp
                )

                signal.stopLoss?.let {
                    Text(
                        text = "Stop-Loss: $$it",
                        color = GoldAccent,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = signal.reasoning,
                    color = TextSecondary,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    ConfidenceDot(confidence = signal.confidence)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${signal.confidence} confidence",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                if (signal.riskNote.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "⚠ ${signal.riskNote}",
                        color = GoldAccent.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PulsingBadge(text: String, color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scaleAnim by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scaleAnim"
    )

    Box(
        modifier = Modifier
            .scale(scaleAnim)
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(text = text, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
fun ConfidenceDot(confidence: String) {
    val dotColor = when (confidence) {
        "Strong" -> BuyGreen
        "Moderate" -> GoldAccent
        else -> TextSecondary
    }
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(dotColor, CircleShape)
    )
}
