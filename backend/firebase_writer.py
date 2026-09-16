"""
firebase_writer.py
Saves the final signal to Firestore and sends an FCM push notification
to every user who has this symbol in their watchlist.
"""

import os
import time
import firebase_admin
from firebase_admin import credentials, firestore, messaging

_initialized = False


def _init_firebase():
    global _initialized
    if _initialized:
        return
    cred_path = os.environ["FIREBASE_SERVICE_ACCOUNT_PATH"]
    cred = credentials.Certificate(cred_path)
    firebase_admin.initialize_app(cred)
    _initialized = True


def save_signal_and_notify(symbol: str, final_signal: dict, price: float):
    """
    final_signal: dict from ai_analysis.get_ai_analysis()
    """
    _init_firebase()
    db = firestore.client()

    doc = {
        "symbol": symbol,
        "signal": final_signal["signal"],
        "confidence": final_signal["confidence"],
        "price": price,
        "stopLoss": final_signal.get("suggested_stop_loss"),
        "reasoning": final_signal["reasoning"],
        "riskNote": final_signal.get("risk_note", ""),
        "timestamp": int(time.time() * 1000),
    }

    # Save/overwrite latest signal for this symbol
    db.collection("signals").document(symbol).set(doc)

    # Skip notifications for HOLD/NEUTRAL - avoid alert fatigue
    if final_signal["signal"] not in ("BUY", "SELL"):
        return

    # Find every user tracking this symbol
    users_ref = db.collection("users")
    watchers = []
    for user_doc in users_ref.stream():
        watchlist_ref = users_ref.document(user_doc.id).collection("watchlist").document(symbol)
        if watchlist_ref.get().exists:
            token = user_doc.to_dict().get("fcmToken")
            if token:
                watchers.append(token)

    for token in watchers:
        message = messaging.Message(
            notification=messaging.Notification(
                title=f"{symbol}: {final_signal['signal']} Signal ({final_signal['confidence']})",
                body=final_signal["reasoning"],
            ),
            data={
                "symbol": symbol,
                "signal": final_signal["signal"],
                "reasoning": final_signal["reasoning"],
            },
            token=token,
        )
        try:
            messaging.send(message)
        except Exception as e:
            print(f"[firebase_writer] Failed to notify token {token[:12]}...: {e}")
