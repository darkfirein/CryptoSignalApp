package com.darkfirein.cryptosignal.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkfirein.cryptosignal.ui.theme.BackgroundDark
import com.darkfirein.cryptosignal.ui.theme.GoldAccent
import com.darkfirein.cryptosignal.ui.theme.TextPrimary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var scale by remember { mutableStateOf(0.6f) }
    var alpha by remember { mutableStateOf(0f) }

    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "logoScale"
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = alpha,
        animationSpec = tween(700),
        label = "logoAlpha"
    )

    LaunchedEffect(Unit) {
        scale = 1f
        alpha = 1f
        delay(1400)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(animatedScale)
                .alpha(animatedAlpha)
        ) {
            LogoMark()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "CRYPTO SIGNAL",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp
            )
            Text(
                text = "AI-Powered Market Analysis",
                color = GoldAccent,
                fontSize = 12.sp
            )
        }
    }
}

/** Minimalist candlestick + uptrend line mark, drawn in code (no external image needed). */
@Composable
fun LogoMark() {
    Canvas(modifier = Modifier.size(84.dp)) {
        val w = size.width
        val h = size.height

        // Uptrend line
        val path = Path().apply {
            moveTo(w * 0.05f, h * 0.75f)
            lineTo(w * 0.35f, h * 0.5f)
            lineTo(w * 0.55f, h * 0.62f)
            lineTo(w * 0.95f, h * 0.15f)
        }
        drawPath(
            path = path,
            color = GoldAccent,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 6f, cap = StrokeCap.Round
            )
        )

        // Arrow head
        val arrow = Path().apply {
            moveTo(w * 0.95f, h * 0.15f)
            lineTo(w * 0.78f, h * 0.15f)
            moveTo(w * 0.95f, h * 0.15f)
            lineTo(w * 0.95f, h * 0.32f)
        }
        drawPath(
            path = arrow,
            color = GoldAccent,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f, cap = StrokeCap.Round)
        )

        // Base candlesticks
        drawRect(
            color = TextPrimary.copy(alpha = 0.85f),
            topLeft = Offset(w * 0.15f, h * 0.55f),
            size = androidx.compose.ui.geometry.Size(w * 0.08f, h * 0.3f)
        )
        drawRect(
            color = TextPrimary.copy(alpha = 0.85f),
            topLeft = Offset(w * 0.45f, h * 0.4f),
            size = androidx.compose.ui.geometry.Size(w * 0.08f, h * 0.45f)
        )
    }
}
