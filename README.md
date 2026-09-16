# Crypto Signal App

AI-powered Binance coin analysis app. Backend calculates technical indicators (RSI, EMA, MACD, Bollinger Bands),
then sends the data to Claude for reasoning + confidence + stop-loss. Signals push straight to the Android app
via Firebase Cloud Messaging.

## Project Structure

```
CryptoSignalApp/
├── app/                    # Android app (Kotlin + Jetpack Compose)
├── backend/                # Python analysis engine (runs on GitHub Actions cron)
└── .github/workflows/      # CI/CD: run-analysis.yml (every 15 min) + build-app.yml (APK build)
```

## One-Time Setup

### 1. Firebase Project
1. Go to https://console.firebase.google.com → Create project
2. Add an Android app with package name `com.darkfirein.cryptosignal`
3. Download `google-services.json` — you'll paste its contents into a GitHub secret (not committed to repo)
4. Enable **Firestore Database** and **Cloud Messaging** in the Firebase console
5. Project Settings → Service Accounts → Generate new private key → download the JSON (this is for the backend)

### 2. Anthropic API Key
Get one from https://console.anthropic.com

### 3. GitHub Secrets
In your repo: Settings → Secrets and variables → Actions → New repository secret. Add these three:

| Secret name | Value |
|---|---|
| `GOOGLE_SERVICES_JSON` | Full contents of `google-services.json` |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | Full contents of the service account key JSON |
| `ANTHROPIC_API_KEY` | Your Claude API key |

## Termux Commands (push this project to GitHub)

```bash
cd ~/CryptoSignalApp

git init
git add .
git commit -m "Initial commit: Crypto Signal App scaffold"

git remote add origin https://github.com/darkfirein/CryptoSignalApp.git
git branch -M main
git push -u origin main
```

After pushing:
- `run-analysis.yml` will start running every 15 minutes automatically (or trigger manually from the Actions tab → "Run workflow")
- `build-app.yml` will build the APK on every push to `main` — download it from the Actions tab → the workflow run → Artifacts → `app-debug`

## Testing the backend locally in Termux (optional, before relying on Actions)

```bash
pkg install python -y
cd ~/CryptoSignalApp/backend
pip install -r requirements.txt

export ANTHROPIC_API_KEY="your-key-here"
export FIREBASE_SERVICE_ACCOUNT_PATH="/path/to/service-account.json"

python main.py
```

## Notes
- Backend only calls Claude when indicators already show a lean (confluence score ≥ 1) — keeps API cost down.
- HOLD/NEUTRAL signals are saved but don't trigger push notifications, to avoid alert fatigue.
- No Binance API key is needed for market data (candles are public). You'd only need Binance keys if you later add auto-trading — not included here for safety.
- The app currently uses Firebase Anonymous Auth so users don't need to sign up. Each install gets a unique watchlist.
