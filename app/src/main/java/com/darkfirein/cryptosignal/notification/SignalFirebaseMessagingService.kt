package com.darkfirein.cryptosignal.notification

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.darkfirein.cryptosignal.CryptoSignalApplication
import com.darkfirein.cryptosignal.MainActivity
import com.darkfirein.cryptosignal.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.app.PendingIntent
import android.content.Intent

class SignalFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title
            ?: "${message.data["symbol"]}: ${message.data["signal"]} Signal"
        val body = message.notification?.body
            ?: message.data["reasoning"]
            ?: "Tap to view details"

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CryptoSignalApplication.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // TODO: send token to your backend / save via SignalRepository.saveFcmToken()
    }
}
