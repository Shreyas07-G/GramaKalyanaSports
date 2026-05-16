# 🏆 Grama-Kalyana Sports App
### Android App for Village-Level Tournament Scoring

---

## 📁 Project Structure

```
GramaKalyanaSports/
├── app/
│   ├── src/main/
│   │   ├── java/com/gramakalyana/sports/
│   │   │   ├── data/
│   │   │   │   ├── models/Models.kt          ← All data classes
│   │   │   │   └── repository/FirebaseRepository.kt  ← All Firebase calls
│   │   │   └── ui/
│   │   │       ├── auth/
│   │   │       │   ├── SplashActivity.kt
│   │   │       │   └── LoginActivity.kt
│   │   │       ├── admin/
│   │   │       │   ├── AdminDashboardActivity.kt
│   │   │       │   ├── TournamentAdapter.kt
│   │   │       │   ├── CreateTournamentActivity.kt
│   │   │       │   └── AddTeamActivity.kt
│   │   │       ├── scorer/
│   │   │       │   └── ScorerActivity.kt     ← Core live scoring
│   │   │       ├── fan/
│   │   │       │   ├── FanViewActivity.kt    ← Public live view
│   │   │       │   └── LiveScoreAdapter.kt
│   │   │       └── stats/
│   │   │           ├── PlayerStatsActivity.kt
│   │   │           └── PlayerStatsAdapter.kt
│   │   ├── res/
│   │   │   ├── layout/  ← All XML layouts
│   │   │   ├── values/  ← colors, strings, themes
│   │   │   └── drawable/ ← Badge backgrounds
│   │   └── AndroidManifest.xml
│   └── build.gradle
└── build.gradle
```

---

## 🚀 SETUP STEPS (Do these TODAY)

### Step 1: Firebase Setup
1. Go to **https://console.firebase.google.com**
2. Click **"Create a project"** → name it `GramaKalyanaSports`
3. Click **"Add Android App"**
   - Package name: `com.gramakalyana.sports`
   - App nickname: Grama Kalyana Sports
4. Download **`google-services.json`**
5. Place it at: `app/google-services.json`

### Step 2: Enable Firebase Services
In Firebase Console:
- **Authentication** → Sign-in method → Enable **Email/Password**
- **Realtime Database** → Create database → Start in **test mode**
- **Firestore Database** → Create database → Start in **test mode**

### Step 3: Firebase Rules (Important!)
Set these Firestore rules:
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Tournaments, teams, matches, players - anyone can read
    match /{collection}/{document=**} {
      allow read: if true;
      allow write: if request.auth != null;
    }
  }
}
```

Realtime Database rules:
```json
{
  "rules": {
    "live_scores": {
      ".read": true,
      ".write": "auth != null"
    },
    "score_events": {
      ".read": "auth != null",
      ".write": "auth != null"
    }
  }
}
```

### Step 4: Open in Android Studio
1. Open Android Studio
2. **File → Open** → select `GramaKalyanaSports` folder
3. Let Gradle sync complete
4. Connect your Android phone (enable Developer Mode + USB Debugging)
5. Click **▶ Run**

---

## 🎮 HOW TO USE

### Flow:
```
Login/Register (Admin)
    ↓
Create Tournament (name, sport, location)
    ↓
Add Teams + Players (2+ teams)
    ↓
Schedule a Match (auto-created after 2 teams)
    ↓
Open Scorer Screen → tap buttons to update live score
    ↓
Fan View → real-time scoreboard (no login needed)
    ↓
Player Stats → career leaderboard
    ↓
Share Scorecard → WhatsApp
```

### User Roles:
| Role | Access | Login Needed |
|------|--------|-------------|
| Admin | Create tournaments, teams, matches | Yes |
| Scorer | Update live scores | Yes |
| Fan/Player | View live scores & stats | NO |

---

## ⚽ Sports Supported

| Sport | Scoring Events |
|-------|---------------|
| **Kabaddi** | Raid Point, Tackle Point, All Out (+2), Super Raid (+3), Bonus |
| **Volleyball** | Point, Ace Serve, Block Point |
| **Cricket** | 1/2/4/6 Runs, Wide, No Ball, Wicket |

---

## ✅ Success Criteria Check

| Requirement | Status |
|-------------|--------|
| Live score updates in <1 second | ✅ Firebase Realtime DB |
| 3+ sport types | ✅ Kabaddi, Volleyball, Cricket |
| Outdoor-readable UI | ✅ High contrast, 72sp score text |
| WhatsApp sharing | ✅ Intent-based sharing |
| Career records | ✅ Firestore player stats |
| No login for fans | ✅ FanViewActivity is public |

---

## 🐛 Common Issues

**Gradle sync fails?**
→ Check internet connection. Make sure `google-services.json` is in the `app/` folder.

**App crashes on launch?**
→ You haven't placed `google-services.json`. Firebase can't initialize.

**Score not updating live?**
→ Check Realtime Database rules. Make sure they allow read: true.

**WhatsApp share not working?**
→ WhatsApp not installed on device. The app falls back to generic share sheet.
