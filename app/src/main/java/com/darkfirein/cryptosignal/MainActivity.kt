package com.darkfirein.cryptosignal

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.darkfirein.cryptosignal.data.model.Candle
import com.darkfirein.cryptosignal.data.model.Signal
import com.darkfirein.cryptosignal.data.model.Ticker24hr
import com.darkfirein.cryptosignal.data.model.WatchlistCoin
import com.darkfirein.cryptosignal.data.repository.MarketRepository
import com.darkfirein.cryptosignal.data.repository.SignalRepository
import com.darkfirein.cryptosignal.ui.screens.ChartScreen
import com.darkfirein.cryptosignal.ui.screens.HomeScreen
import com.darkfirein.cryptosignal.ui.screens.MarketsScreen
import com.darkfirein.cryptosignal.ui.screens.SplashScreen
import com.darkfirein.cryptosignal.ui.theme.BackgroundDark
import com.darkfirein.cryptosignal.ui.theme.CryptoSignalTheme
import com.darkfirein.cryptosignal.ui.theme.GoldAccent
import com.darkfirein.cryptosignal.ui.theme.SurfaceDark
import com.darkfirein.cryptosignal.ui.theme.TextSecondary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            CryptoSignalTheme {
                var showSplash by remember { mutableStateOf(true) }

                if (showSplash) {
                    SplashScreen(onFinished = { showSplash = false })
                } else {
                    AppRoot()
                }
            }
        }
    }
}

private sealed class Screen {
    data object Signals : Screen()
    data object Markets : Screen()
    data class CoinDetail(val symbol: String) : Screen()
}

@Composable
fun AppRoot() {
    val uid = remember {
        FirebaseAuth.getInstance().currentUser?.uid ?: "local_user"
    }
    val signalRepository = remember { SignalRepository(uid) }
    val marketRepository = remember { MarketRepository() }
    val scope = rememberCoroutineScope()

    var watchlist by remember { mutableStateOf<List<WatchlistCoin>>(emptyList()) }
    var signals by remember { mutableStateOf<List<Signal>>(emptyList()) }
    var allTickers by remember { mutableStateOf<List<Ticker24hr>>(emptyList()) }
    var candles by remember { mutableStateOf<List<Candle>>(emptyList()) }
    var chartInterval by remember { mutableStateOf("1h") }

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Signals) }

    LaunchedEffect(Unit) {
        FirebaseAuth.getInstance().signInAnonymously()
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            scope.launch { signalRepository.saveFcmToken(token) }
        }
    }

    LaunchedEffect(uid) {
        signalRepository.observeWatchlist().collect { watchlist = it }
    }

    LaunchedEffect(watchlist) {
        val symbols = watchlist.map { it.symbol }
        signalRepository.observeSignals(symbols).collect { signals = it }
    }

    // Live market list - always polling in the background once the app is open
    LaunchedEffect(Unit) {
        marketRepository.observeAllUsdtTickers().collect { allTickers = it }
    }

    // Live candles for whichever coin is currently open in the chart screen
    LaunchedEffect(currentScreen, chartInterval) {
        val screen = currentScreen
        if (screen is Screen.CoinDetail) {
            marketRepository.observeCandles(symbol = screen.symbol, interval = chartInterval)
                .collect { candles = it }
        }
    }

    Scaffold(
        containerColor = BackgroundDark,
        bottomBar = {
            if (currentScreen !is Screen.CoinDetail) {
                NavigationBar(containerColor = SurfaceDark) {
                    NavigationBarItem(
                        selected = currentScreen is Screen.Signals,
                        onClick = { currentScreen = Screen.Signals },
                        icon = { Icon(Icons.Default.Notifications, contentDescription = "Signals") },
                        label = { Text("Signals") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GoldAccent,
                            selectedTextColor = GoldAccent,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = SurfaceDark
                        )
                    )
                    NavigationBarItem(
                        selected = currentScreen is Screen.Markets,
                        onClick = { currentScreen = Screen.Markets },
                        icon = { Icon(Icons.Default.ShowChart, contentDescription = "Markets") },
                        label = { Text("Markets") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GoldAccent,
                            selectedTextColor = GoldAccent,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = SurfaceDark
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding).background(BackgroundDark)) {
            when (val screen = currentScreen) {
                is Screen.CoinDetail -> {
                    val liveTicker = allTickers.find { it.symbol == screen.symbol }
                    ChartScreen(
                        symbol = screen.symbol,
                        ticker = liveTicker,
                        candles = candles,
                        selectedInterval = chartInterval,
                        onIntervalChange = { chartInterval = it },
                        onBack = { currentScreen = Screen.Markets; candles = emptyList() }
                    )
                }
                Screen.Signals -> {
                    HomeScreen(
                        watchlist = watchlist,
                        signals = signals,
                        onAddCoin = { symbol -> scope.launch { signalRepository.addCoin(symbol) } },
                        onRemoveCoin = { symbol -> scope.launch { signalRepository.removeCoin(symbol) } }
                    )
                }
                Screen.Markets -> {
                    MarketsScreen(
                        tickers = allTickers,
                        onCoinClick = { symbol -> currentScreen = Screen.CoinDetail(symbol) }
                    )
                }
            }
        }
    }
}
