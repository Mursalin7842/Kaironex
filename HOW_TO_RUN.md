# 🚀 KAIRONEX - HOW TO RUN & TEST PRODUCTION FEATURES

## Quick Start

### 1. Start the Backend Server (Required for full functionality)

```bash
cd Kaironex-Brain
pip install -r requirements.txt
python server.py
```

The server will start at `http://localhost:8000`

### 2. Run the Android App

```bash
cd Kaironex
./gradlew installDebug
```

Or run from Android Studio.

### 3. Run the Desktop App (JVM)

```bash
cd Kaironex
./gradlew run
```

---

## 🎙️ Testing Wake Word Detection

### On Android:
1. Grant microphone permission when prompted
2. Say **"Hey Kairo"** or **"Kaironex"**
3. The Voice Call Screen should appear
4. Speak your command (e.g., "I have a wedding this weekend")
5. The AI will respond and process your request

### Wake Word Configuration:
- Default wake word: "kaironex"
- Default agent name: "Kairo"
- Alternative patterns: "hey kairo", "okay kairo", "kairo"

These are configured in the user's `StudentProfile` (set during Genesis/onboarding).

---

## 🧠 Testing the Agent Dashboard (Judge Mode)

1. Open the app
2. Go to **Life Command Center** (More tab)
3. Scroll down and tap **"Agent Dashboard"**
4. You'll see real-time brain activity:
   - Brain status (ONLINE/PROCESSING)
   - Live activity feed
   - Active marathons
   - Recent interventions

---

## 📡 Backend API Endpoints

### Health Check
```
GET http://localhost:8000/
```

### Quick Prompt (Reflex Response)
```
POST http://localhost:8000/api/v1/brain/quick
{
  "userId": "user123",
  "prompt": "What should I study today?",
  "agent": "generic",
  "mode": "reflex"
}
```

### Trigger Agent
```
POST http://localhost:8000/api/v1/brain/trigger
{
  "userId": "user123",
  "type": "schedule_update",
  "data": {"reason": "wedding this weekend"}
}
```

### Agent-Specific Endpoints
- `POST /api/v1/brain/campaign` - Career & Goals
- `POST /api/v1/brain/vitality` - Health & Energy
- `POST /api/v1/brain/radius` - Location & Social

### WebSocket (Real-time)
```
WS ws://localhost:8000/ws/brain/{userId}
```

---

## 🔧 Environment Configuration

### Required Environment Variables

Create a `.env` file in `Kaironex-Brain/`:
```
GEMINI_API_KEY=your_gemini_api_key
APPWRITE_ENDPOINT=https://nyc.cloud.appwrite.io/v1
APPWRITE_PROJECT_ID=696e9248002198ef6273
APPWRITE_DATABASE_ID=697cb20f00110f6d7530
APPWRITE_API_KEY=your_appwrite_api_key
```

### App Configuration

The app reads the brain server URL from:
- Environment variable: `KAIRONEX_BRAIN_URL`
- Default: `http://localhost:8000` (development)
- Production: Set in `AppConfig.kt`

---

## ✅ What's Working

1. **Wake Word Detection** - Android SpeechRecognizer listens for "Hey Kairo"
2. **Voice Call Screen** - Beautiful UI with call states and animations
3. **Brain API Client** - REST + WebSocket connection to backend
4. **Reflex Agent** - Fast local responses with caching
5. **Agent Dashboard** - Real-time brain monitoring
6. **All 3 Zone Agents** - Campaign, Vitality, Radius
7. **Marathon Sessions** - Long-running AI tasks

## ⚠️ Known Limitations

1. **Voice Call** - Uses demo responses if no API key is set
2. **Appwrite Sync** - Requires valid Appwrite credentials
3. **Wake Word (JVM)** - Desktop uses stub implementation

---

## 🐛 Troubleshooting

### Wake word not working?
1. Check microphone permission is granted
2. Speak clearly: "Hey Kairo" or "Kaironex"
3. Check logcat for `🎯 Wake word detected` messages

### Voice call not connecting?
1. Ensure `GEMINI_API_KEY` is set in `local.properties`
2. Check network connectivity
3. The app falls back to demo mode if connection fails

### Backend not responding?
1. Start the server: `python server.py`
2. Check `http://localhost:8000` returns health status
3. Verify `.env` file has correct credentials

---

## 📁 Key Files

| File | Purpose |
|------|---------|
| `MainShell.kt` | Wake word initialization & navigation |
| `VoiceCallScreen.kt` | Voice call UI & Gemini integration |
| `AgentDashboardScreen.kt` | Real-time brain monitoring |
| `KaironexSessionManager.kt` | Session & wake word management |
| `BrainApiClient.kt` | Backend REST & WebSocket client |
| `WakeWordDetector.android.kt` | Android SpeechRecognizer impl |
| `server.py` | Python backend server |

---

*Last updated: February 3, 2026*
