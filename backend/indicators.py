"""
indicators.py
Fetches candlestick data directly from Binance's public data mirror
(data-api.binance.vision) using plain HTTP requests, and calculates
technical indicators with the 'ta' library.

We use plain `requests` instead of the python-binance package here because
that package's Client() pings binance.com on init, and binance.com blocks
requests from US-hosted IPs (which is what GitHub Actions runners use).
data-api.binance.vision serves the same public market data without that
restriction, and calling it directly avoids the problematic ping.
"""

import requests
import pandas as pd
from ta.momentum import RSIIndicator
from ta.trend import EMAIndicator, MACD
from ta.volatility import BollingerBands

BASE_URL = "https://data-api.binance.vision/api/v3/klines"


def fetch_indicator_data(symbol: str, interval: str = "4h", limit: int = 200) -> dict:
    """
    Fetch candles for `symbol` and return latest indicator snapshot.
    Returns None if the symbol is invalid or has no data.
    """
    try:
        response = requests.get(
            BASE_URL,
            params={"symbol": symbol, "interval": interval, "limit": limit},
            timeout=15,
        )
        response.raise_for_status()
        klines = response.json()
    except Exception as e:
        print(f"[indicators] Failed to fetch {symbol}: {e}")
        return None

    if not klines or not isinstance(klines, list):
        return None

    df = pd.DataFrame(klines, columns=[
        "timestamp", "open", "high", "low", "close", "volume",
        "close_time", "qav", "trades", "tbbav", "tbqav", "ignore"
    ])
    for col in ["open", "high", "low", "close", "volume"]:
        df[col] = df[col].astype(float)

    df["rsi"] = RSIIndicator(close=df["close"], window=14).rsi()
    df["ema50"] = EMAIndicator(close=df["close"], window=50).ema_indicator()
    df["ema200"] = EMAIndicator(close=df["close"], window=200).ema_indicator()

    macd_calc = MACD(close=df["close"])
    df["macd"] = macd_calc.macd()
    df["macd_signal"] = macd_calc.macd_signal()

    bb = BollingerBands(close=df["close"], window=20, window_dev=2)
    df["bb_lower"] = bb.bollinger_lband()
    df["bb_upper"] = bb.bollinger_hband()

    latest = df.iloc[-1]

    score = 0
    reasons = []

    if pd.notna(latest["rsi"]):
        if latest["rsi"] < 30:
            score += 1
            reasons.append(f"RSI oversold ({latest['rsi']:.1f})")
        elif latest["rsi"] > 70:
            score -= 1
            reasons.append(f"RSI overbought ({latest['rsi']:.1f})")

    if pd.notna(latest["ema50"]) and pd.notna(latest["ema200"]):
        if latest["ema50"] > latest["ema200"]:
            score += 1
            reasons.append("EMA50 above EMA200 (bullish trend)")
        else:
            score -= 1
            reasons.append("EMA50 below EMA200 (bearish trend)")

    if pd.notna(latest["macd"]) and pd.notna(latest["macd_signal"]):
        if latest["macd"] > latest["macd_signal"]:
            score += 1
            reasons.append("MACD bullish crossover")
        else:
            score -= 1
            reasons.append("MACD bearish crossover")

    if pd.notna(latest["bb_lower"]) and latest["close"] <= latest["bb_lower"]:
        score += 1
        reasons.append("Price at lower Bollinger Band")
    elif pd.notna(latest["bb_upper"]) and latest["close"] >= latest["bb_upper"]:
        score -= 1
        reasons.append("Price at upper Bollinger Band")

    recent_low = df["low"].tail(10).min()
    recent_high = df["high"].tail(10).max()

    return {
        "symbol": symbol,
        "price": round(float(latest["close"]), 6),
        "rsi": round(float(latest["rsi"]), 2) if pd.notna(latest["rsi"]) else None,
        "ema_trend": "bullish" if latest["ema50"] > latest["ema200"] else "bearish",
        "macd_status": "bullish" if latest["macd"] > latest["macd_signal"] else "bearish",
        "bb_position": (
            "lower_band" if pd.notna(latest["bb_lower"]) and latest["close"] <= latest["bb_lower"]
            else "upper_band" if pd.notna(latest["bb_upper"]) and latest["close"] >= latest["bb_upper"]
            else "mid_range"
        ),
        "score": score,
        "reasons": reasons,
        "recent_low": round(float(recent_low), 6),
        "recent_high": round(float(recent_high), 6),
    }
