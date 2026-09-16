"""
ai_analysis.py
Sends pre-calculated indicator data (never raw guessing) to Claude for
human-like reasoning, confidence scoring, and a risk note.
"""

import os
import json
import anthropic

client = anthropic.Anthropic(api_key=os.environ["ANTHROPIC_API_KEY"])


def get_ai_analysis(indicator_data: dict, recent_news: str = "No recent news available") -> dict:
    prompt = f"""You are a disciplined crypto trading analyst who prioritizes risk management over chasing gains.

Analyze this data and respond with a signal:

Coin: {indicator_data['symbol']}
Current Price: {indicator_data['price']}
RSI (14): {indicator_data['rsi']}
EMA50 vs EMA200 trend: {indicator_data['ema_trend']}
MACD status: {indicator_data['macd_status']}
Bollinger Band position: {indicator_data['bb_position']}
Indicator confluence score (-4 to +4): {indicator_data['score']}
Reasons from indicators: {', '.join(indicator_data['reasons']) if indicator_data['reasons'] else 'None strong'}
Recent 10-candle low: {indicator_data['recent_low']}
Recent 10-candle high: {indicator_data['recent_high']}
Recent news: {recent_news}

Respond ONLY with valid JSON, no markdown fences, no extra text:
{{
  "signal": "BUY" or "SELL" or "HOLD",
  "confidence": "Strong" or "Moderate" or "Weak",
  "reasoning": "2-3 sentence explanation combining technical and news factors",
  "suggested_stop_loss": number or null,
  "risk_note": "short specific risk warning for this coin's current situation"
}}"""

    response = client.messages.create(
        model="claude-sonnet-4-6",
        max_tokens=400,
        messages=[{"role": "user", "content": prompt}]
    )

    raw_text = response.content[0].text.strip()
    raw_text = raw_text.replace("```json", "").replace("```", "").strip()

    try:
        return json.loads(raw_text)
    except json.JSONDecodeError:
        print(f"[ai_analysis] Failed to parse AI response for {indicator_data['symbol']}: {raw_text}")
        return {
            "signal": "HOLD",
            "confidence": "Weak",
            "reasoning": "AI response could not be parsed; defaulting to HOLD for safety.",
            "suggested_stop_loss": None,
            "risk_note": "Analysis inconclusive this cycle - treat with caution."
        }
