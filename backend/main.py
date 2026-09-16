"""
main.py
Entry point run by GitHub Actions on a schedule (e.g. every 15 min).

Flow:
  1. Read the union of all users' watchlist coins from Firestore
  2. For each coin: calculate indicators
  3. Only call the AI (cost control) if indicators already show a lean
     (|score| >= 1) - otherwise save a NEUTRAL/HOLD signal directly
  4. Save result + push notification via firebase_writer
"""

import os
import firebase_admin
from firebase_admin import credentials, firestore

from indicators import fetch_indicator_data
from ai_analysis import get_ai_analysis
from firebase_writer import save_signal_and_notify, _init_firebase

AI_TRIGGER_THRESHOLD = 1  # only call Claude if |score| >= this


def get_all_tracked_symbols() -> set:
    _init_firebase()
    db = firestore.client()
    symbols = set()
    for user_doc in db.collection("users").stream():
        for coin_doc in db.collection("users").document(user_doc.id).collection("watchlist").stream():
            symbols.add(coin_doc.id)
    return symbols


def process_symbol(symbol: str):
    print(f"Analyzing {symbol}...")
    data = fetch_indicator_data(symbol)
    if data is None:
        print(f"  Skipping {symbol} - no data")
        return

    if abs(data["score"]) >= AI_TRIGGER_THRESHOLD:
        ai_result = get_ai_analysis(data)
    else:
        ai_result = {
            "signal": "HOLD",
            "confidence": "Weak",
            "reasoning": "No strong indicator confluence this cycle.",
            "suggested_stop_loss": None,
            "risk_note": "Market showing no clear direction - staying neutral.",
        }

    save_signal_and_notify(symbol, ai_result, data["price"])
    print(f"  {symbol}: {ai_result['signal']} ({ai_result['confidence']})")


def main():
    symbols = get_all_tracked_symbols()
    if not symbols:
        print("No symbols in any watchlist. Nothing to do.")
        return

    print(f"Found {len(symbols)} unique tracked symbols: {symbols}")
    for symbol in symbols:
        try:
            process_symbol(symbol)
        except Exception as e:
            print(f"Error processing {symbol}: {e}")


if __name__ == "__main__":
    main()
