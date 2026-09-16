package com.darkfirein.cryptosignal

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.darkfirein.cryptosignal.data.model.Signal
import com.darkfirein.cryptosignal.data.model.WatchlistCoin
import com.darkfirein.cryptosignal.data.repository.SignalRepository
import com.darkfirein.cryptosignal.ui.screens.HomeScreen
import com.darkfirein.cryptosignal.ui.screens.SplashScreen
import com.darkfirein.cryptosignal.ui.theme.BackgroundDark
import com.darkfirein.cryptosignal.ui.theme.CryptoSignalTheme
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

@Composable
fun AppRoot() {
    // Anonymous auth keeps this simple; swap for real auth later if needed.
    val uid = remember {
        FirebaseAuth.getInstance().currentUser?.uid
            ?: "local_user" // fallback until anonymous sign-in completes
    }
    val repository = remember { SignalRepository(uid) }
    val scope = rememberCoroutineScope()

    var watchlist by remember { mutableStateOf<List<WatchlistCoin>>(emptyList()) }
    var signals by remember { mutableStateOf<List<Signal>>(emptyList()) }

    LaunchedEffect(Unit) {
        FirebaseAuth.getInstance().signInAnonymously()
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            scope.launch { repository.saveFcmToken(token) }
        }
    }

    LaunchedEffect(uid) {
        repository.observeWatchlist().collect { watchlist = it }
    }

    LaunchedEffect(watchlist) {
        val symbols = watchlist.map { it.symbol }
        repository.observeSignals(symbols).collect { signals = it }
    }

    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        HomeScreen(
            watchlist = watchlist,
            signals = signals,
            onAddCoin = { symbol -> scope.launch { repository.addCoin(symbol) } },
            onRemoveCoin = { symbol -> scope.launch { repository.removeCoin(symbol) } }
        )
    }
}
