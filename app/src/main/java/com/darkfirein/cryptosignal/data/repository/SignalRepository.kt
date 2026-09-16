package com.darkfirein.cryptosignal.data.repository

import com.darkfirein.cryptosignal.data.model.Signal
import com.darkfirein.cryptosignal.data.model.WatchlistCoin
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Talks to Firestore. Collections:
 *   users/{uid}/watchlist/{symbol}   -> WatchlistCoin
 *   signals/{symbol}                 -> latest Signal (written by backend cron job)
 */
class SignalRepository(private val userId: String) {

    private val db = FirebaseFirestore.getInstance()

    fun observeSignals(symbols: List<String>): Flow<List<Signal>> = callbackFlow {
        if (symbols.isEmpty()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val listener = db.collection("signals")
            .whereIn("symbol", symbols)
            .addSnapshotListener { snapshot, _ ->
                val signals = snapshot?.documents?.mapNotNull { it.toObject(Signal::class.java) }
                    ?: emptyList()
                trySend(signals)
            }
        awaitClose { listener.remove() }
    }

    fun observeWatchlist(): Flow<List<WatchlistCoin>> = callbackFlow {
        val listener = db.collection("users").document(userId).collection("watchlist")
            .addSnapshotListener { snapshot, _ ->
                val coins = snapshot?.documents?.mapNotNull { it.toObject(WatchlistCoin::class.java) }
                    ?: emptyList()
                trySend(coins)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addCoin(symbol: String) {
        val coin = WatchlistCoin(symbol = symbol.uppercase())
        db.collection("users").document(userId)
            .collection("watchlist").document(coin.symbol)
            .set(coin).await()
    }

    suspend fun removeCoin(symbol: String) {
        db.collection("users").document(userId)
            .collection("watchlist").document(symbol.uppercase())
            .delete().await()
    }

    suspend fun saveFcmToken(token: String) {
        db.collection("users").document(userId)
            .set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
            .await()
    }
}
