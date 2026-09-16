package com.darkfirein.cryptosignal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BackgroundDark = Color(0xFF0D0D1A)
val SurfaceDark = Color(0xFF1A1A2E)
val GoldAccent = Color(0xFFF0B90B)
val BuyGreen = Color(0xFF00C853)
val SellRed = Color(0xFFD32F2F)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFA0A0B8)

private val PremiumDarkColorScheme = darkColorScheme(
    primary = GoldAccent,
    secondary = BuyGreen,
    error = SellRed,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = BackgroundDark,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun CryptoSignalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PremiumDarkColorScheme,
        typography = Typography,
        content = content
    )
}
