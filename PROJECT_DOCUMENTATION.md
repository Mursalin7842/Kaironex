# KAIRONEX - Complete Project Documentation
## The Student Life Operating System

---

## 🎯 PROJECT OVERVIEW

**Kaironex** is a Kotlin Multiplatform (KMP) application targeting Android and Desktop (JVM) that serves as a complete "Student Life Operating System." Built for the Gemini 3 Hackathon, it goes beyond being a simple study app by managing ALL aspects of a student's life so they can focus on what matters: studying.

### Core Philosophy: "The Prompt Gap"
> "Life and Study collide. Students fail not because they can't learn, but because LIFE keeps interrupting."

Kaironex handles the "Life" part (career, food, finance, housing) so students can focus on the "Study" part.

---

## 🏗️ ARCHITECTURE

### Tech Stack
- **Framework**: Kotlin Multiplatform (KMP) with Compose Multiplatform
- **Platforms**: Android, Desktop (JVM)
- **UI**: Jetpack Compose / Compose for Desktop
- **Navigation**: Voyager
- **DI**: Koin
- **Networking**: Ktor
- **Database**: Firebase Realtime Database
- **AI**: Gemini 3 API integration (planned)

### Project Structure
```
Kaironex/
├── composeApp/
│   ├── src/
│   │   ├── commonMain/          # Shared code
│   │   │   ├── kotlin/
│   │   │   │   └── com/mursaline/kaironex/
│   │   │   │       ├── core/           # Core logic
│   │   │   │       │   └── gemini/     # Gemini AI integration
│   │   │   │       │       ├── GeminiOrchestrator.kt
│   │   │   │       │       └── agents/
│   │   │   │       │           ├── MarathonAgentEngine.kt
│   │   │   │       │           └── RealTimeTeacherEngine.kt
│   │   │   │       ├── features/       # Feature modules
│   │   │   │       │   ├── dashboard/
│   │   │   │       │   ├── study/
│   │   │   │       │   ├── agents/
│   │   │   │       │   ├── profile/
│   │   │   │       │   └── zones/
│   │   │   │       ├── ui/             # UI components
│   │   │   │       │   ├── components/ # Reusable components
│   │   │   │       │   └── theme/      # Colors, typography
│   │   │   │       ├── platform/       # Platform abstractions
│   │   │   │       └── MainShell.kt    # Main navigation shell
│   │   ├── androidMain/         # Android-specific code
│   │   └── jvmMain/             # Desktop-specific code
```

---

## 📱 NAVIGATION STRUCTURE

### 5-Item Navigation Bar
```
┌────────┬────────┬────────┬────────┬────────┐
│  Home  │ Study  │  ORB   │  More  │Profile │
│   🏠   │   🎓   │   🔮   │   ⋯    │   👤   │
└────────┴────────┴────────┴────────┴────────┘
```

| Tab | Screen | Purpose |
|-----|--------|---------|
| Home | DashboardScreen | Main stats dashboard |
| Study | StudySessionsScreen | Study session management |
| ORB | ImmersiveAssistantPanel | AI Assistant (Gemini) |
| More | LifeSupportAgentsScreen | Life Support Agents |
| Profile | ProfileScreen | User settings |

---

## 🎛️ SCREENS & FEATURES

### 1. Dashboard Screen (Home)
**File**: `features/dashboard/DashboardScreen.kt`

**Sections**:
- **Current Focus (Cortex Hero Card)**: Shows current study session with pressure visualization
- **Study Progress**: Stats including streak, focus score, weekly hours, concepts mastered
- **Today's Schedule**: Upcoming study sessions with times
- **Life Support Status**: Quick view of Campaign/Vitality/Radius stats
- **Weekly Pressure**: Pressure map visualization

**Components**:
- `CortexHeroCard` - Large interactive card to enter study mode
- `StudyStatsCard` - 4 stats + progress bar
- `TodayScheduleCard` - Schedule list
- `LifeSupportSummaryCard` - Mini agent stats
- `PressureMap` - Visual pressure graph

---

### 2. Study Sessions Screen
**File**: `features/study/StudySessionsScreen.kt`

**Tabs**:
- **Current**: Active study session (if any)
- **Upcoming**: Scheduled future sessions
- **Previous**: Past sessions history

**Features**:
- Quick start options: 25min (Pomodoro), 50min (Deep Work), 90min (Flow State)
- Session cards with subject, topic, duration
- "Start Study Session" button navigates to StudyRoomScreen

---

### 3. Study Room Screen
**File**: `features/study/StudyRoomScreen.kt`

**Purpose**: The actual study environment (The Cortex)

**Components**:
- Study viewer (PDF, Video, Web)
- AI tutor assistant
- Timer and session controls
- Knowledge gatekeeper (blocks distractions)

---

### 4. Life Support Agents Screen (More)
**File**: `features/agents/LifeSupportAgentsScreen.kt`

**Shows 3 Life Support Agents with Stats**:

#### Campaign (Career & Growth) 🚀
- Applications: 12
- Interviews: 3
- Skills Progress: 67%

#### Vitality (Food & Finance) ⚡
- Budget Runway: 23 days
- Meals Planned: 14
- Average Sleep: 7.2h

#### Radius (Habitat & Culture) 📡
- Visa Days Remaining: 89
- Places Discovered: 8
- Slang Terms Learned: 24

Each card navigates to `ZoneDetailScreen` with full feature access.

---

### 5. Zone Detail Screen
**File**: `features/zones/ZoneDetailScreen.kt`

**Dynamic screen for each Life Track showing sub-features**:

#### Campaign Sub-Features:
- **Skill Tree**: Visual roadmap (Learn DSA → System Design → Cloud Cert)
- **Quest Board**: Job finder with student-friendly filters
- **The Armory**: AI resume builder
- **Simulacrum**: Mock interview with voice mode

#### Vitality Sub-Features:
- **Bio-Fuel System**: Fridge scanner, discount radar
- **Resource Monitor**: Budget runway, bill splitter
- **Regen Mode**: Sleep optimizer

#### Radius Sub-Features:
- **Signal Decoder**: Slang dictionary, etiquette guide
- **Safehouse**: Rental scam detector, utility setup
- **Local Scan**: Nearby services finder
- **Admin Protocol**: Visa countdown, work hours tracker

---

### 6. Profile Screen
**File**: `features/profile/ProfileScreen.kt`

**Sections**:
- User avatar and basic info
- Account settings
- Preferences (theme, notifications, language)
- System configuration
- Support & About
- Sign out

---

### 7. Immersive Assistant Panel (Orb)
**File**: `MainShell.kt` - `ImmersiveAssistantPanel`

**Features**:
- Full-screen overlay with dark theme
- Large animated orb (KxOrb component)
- "I'm listening..." text
- Close button
- Blocks background interactions
- BackHandler to close on back press (Android)

---

## 🧠 GEMINI 3 INTEGRATION (Core Differentiator)

### Marathon Agent Engine
**File**: `core/gemini/agents/MarathonAgentEngine.kt`

**Purpose**: Autonomous agents that run for DAYS, not minutes.

**Key Features**:
- **Thought Signatures**: Visible reasoning chains
- **Thinking Levels**: FLASH, BALANCED, DEEP, MARATHON
- **Self-Correction**: Detects failures and adjusts
- **Checkpointing**: Survives app restarts
- **Multi-Tool Orchestration**: 50+ tool calls over 72 hours

**Example Use Case**:
```
Day 1: Scan 500 job postings, filter for visa sponsorship
Day 2: Tailor resume for top 20 matches
Day 3: Track applications, self-correct based on rejections
Day 4: Prepare interview questions
```

---

### Real-Time Teacher Engine
**File**: `core/gemini/agents/RealTimeTeacherEngine.kt`

**Purpose**: Adaptive teaching using Gemini Live API

**Capabilities**:
- **Video Understanding**: Watches student solve problems
- **Gaze Detection**: Knows when confused
- **Adaptive Pacing**: Slows down/speeds up
- **Spatial-Temporal Analysis**: Tracks writing patterns

**Teaching Strategies**:
- EXPLAIN, DEMONSTRATE, QUIZ
- SLOW_DOWN, ENCOURAGE, CHALLENGE
- SUMMARIZE, WAIT

---

### Gemini Orchestrator
**File**: `core/gemini/GeminiOrchestrator.kt`

**Data Models**:
- `ThoughtSignature`: Reasoning chain with confidence
- `MarathonTask`: Long-running autonomous task
- `ContextWindow`: 1M token utilization
- `VerificationLoop`: Self-checking systems

---

## 🎨 UI COMPONENTS

### Design System
**File**: `ui/theme/KaironexColors.kt`

**Color Palette**:
- InkBlack: Primary text
- SlateGray: Secondary text
- CloudGray: Background
- CanvasWhite: Cards
- ElectricBlue: Primary accent
- GeminiBlurple: AI/Orb
- Indigo600, Purple600: Gradients
- SuccessGreen, AttentionOrange, AlertRed: Status

### Reusable Components
- `KxCard`: Elevated/Flat/Outline variants
- `KxButton`: Primary/Secondary/Ghost variants
- `KxBadge`: Success/Warning/Error/Info variants
- `KxOrb`: Animated AI orb (Idle/Active/Listening states)

---

## 📊 DATA MODELS

### Zone Models
**File**: `features/zones/ZoneModels.kt`

```kotlin
enum class LifeTrack {
    Campaign,  // 🚀 Career & Growth
    Vitality,  // ⚡ Food & Finance
    Radius     // 📡 Habitat & Culture
}

data class CortexState(
    val currentSubject: String,
    val currentTopic: String,
    val pressure: Float,
    val upcomingDeadlines: Int,
    val studyStreak: Int,
    val conceptMastery: Float,
    val isActive: Boolean
)

data class MarathonAgentState(
    val agentId: String,
    val zone: LifeTrack,
    val objective: String,
    val status: MarathonUIStatus,
    val progress: Float,
    val currentThought: String,
    val toolCallCount: Int,
    val selfCorrectionCount: Int
)
```

---

## 🔧 PLATFORM-SPECIFIC IMPLEMENTATIONS

### Android
- `BackHandler.android.kt`: Uses `androidx.activity.compose.BackHandler`
- Firebase integration
- Ktor Android client

### Desktop (JVM)
- `BackHandler.jvm.kt`: No-op (close button handles dismissal)
- `DesktopEye.kt`: Window title monitoring for distraction detection
- JNA for native system access

---

## 📋 FEATURE COMPLETENESS CHECKLIST

### ✅ Implemented (UI Complete)
- [x] Dashboard with comprehensive stats
- [x] 5-item navigation bar
- [x] Study Sessions screen with tabs
- [x] Life Support Agents screen with stats
- [x] Zone Detail screens for all 3 tracks
- [x] Profile screen
- [x] Immersive Orb overlay
- [x] Back handler for orb dismissal
- [x] Responsive layouts (mobile/desktop)
- [x] CortexHeroCard with pressure visualization
- [x] Theme and color system
- [x] Reusable UI components

### ✅ NEW: Implemented Placeholder Components
- [x] **InAppYouTubePlayer** - Controlled YouTube player placeholder with:
  - No external navigation
  - Controlled playlist support
  - Proof-of-Work tracking indicator
  - Playback controls UI
- [x] **InAppSecureBrowser** - Secure study browser placeholder with:
  - No URL bar for untrusted content
  - Permitted domain list
  - "Request new domain" modal
  - Tab switcher component
  - Domain restriction UI
- [x] **GatekeeperModal** - Non-dismissible quiz modal with:
  - Question/Answer UI
  - Progress tracking
  - Pass/Fail animated states
  - Remediation options
  - "Extend session" controls
- [x] **MockTestModels** - Data models for mock tests
- [x] **DesktopEye** (FIXED) - Now privacy-first:
  - OFF by default (opt-in only)
  - Only monitors during active sessions
  - Controllable start/stop
  - Study-related detection helpers

### ✅ NEW: Dashboard Enhancements
- [x] **ScheduledTasksCard** - Shows all scheduled study blocks:
  - Today's schedule with times
  - Task status (Completed, In Progress, Upcoming)
  - Priority indicators
  - Add task button
- [x] **DriveIngestionCard** - File upload and Drive integration:
  - Google Drive connection status
  - Manual file upload area (drop zone)
  - Recent uploads with ingestion status
  - File type detection (PDF, Video, PPT, etc.)

### ✅ NEW: Presence Engine & Intervention System
- [x] **PresenceEngine** - Cross-platform presence detection (replaces DesktopEye):
  - Tracks if user is focused on app or drifted away
  - States: FOCUSED, DRIFTING, INTERVENTION, NEGOTIATING, ON_BREAK
  - Configurable drift tolerance (default 15s grace, 30s before call)
  - Window focus gained/lost callbacks
  - Break granting with duration
- [x] **GeminiInterventionOverlay** - "Kairo is calling" screen:
  - Full-screen dark overlay with pulsing animations
  - Expanding ring effects around avatar
  - "I'm Back" and "Need a Break" buttons
  - Friendly, non-punishing tone
- [x] **NegotiationDialog** - Break negotiation:
  - User explains why they need a break
  - Break duration chips (2, 5, 10, 15 minutes)
  - "It was nothing, I'm back" option
- [x] **DriftingWarningBadge** - Small warning when starting to drift
- [x] **OnBreakBadge** - Shows when user is on approved break

### ✅ NEW: Light Theme for Onboarding
- [x] **SystemSetupScreen** - Updated to use light/white theme:
  - CloudGray background
  - InkBlack text colors
  - White cards with shadows
  - Consistent with main app theme
- [x] **GenesisInterviewScreen** - Updated to use light theme:
  - White chat bubbles for Kairo messages
  - Purple bubbles for user messages
  - Light header with shadow
  - White input area
  - Profile summary with white cards
  - Drive connection with light styling
- [x] **SplashScreen** - Animated splash with:
  - Robot hero image with pulse animation
  - Glow effects
  - App name and tagline
  - Auto-navigation to login
- [x] **SystemSetupScreen** - Initial setup flow (3 steps):
  - Step 1: Name preference (how system calls user)
  - Step 2: Wake word setup ("Hey Kairo", "OK Kairo", custom)
  - Step 3: Interaction mode selection (Voice or Chat)
- [x] **GenesisInterviewScreen** - Comprehensive student data collection:
  - Voice or Chat selection for interview mode
  - Conversational interview with Kairo avatar
  - Questions collect:
    - Student name
    - University/College
    - Degree type (BTech, BSc, Masters, PhD)
    - Major/Field of study
    - Current semester/year
    - Part-time job details (if any)
    - Work schedule and hours
    - Study goals
    - Biggest challenges
    - Preferred study times
    - Extracurricular activities
    - International student status & visa
    - Sleep schedule
  - Typing indicator animation
  - Progress tracking
  - Profile summary at completion
  - Data feeds into Gemini for personalization
- [x] **AuthUIComponents** - Premium auth UI with:
  - FloatingCard with 3D tilt animation
  - GlassCard with frosted glass effect
  - AnimatedGradientBackground
  - FloatingOrbs background elements
  - GradientButton with premium styling

### 🔶 Implemented (Backend Stubs)
- [x] Marathon Agent Engine (architecture only)
- [x] Real-Time Teacher Engine (architecture only)
- [x] Gemini Orchestrator models
- [x] Thought Signatures system

### ❌ Not Implemented
- [ ] Actual Gemini API integration
- [ ] Database persistence (SQLDelight commented out)
- [ ] User authentication flow
- [ ] Real study session timer
- [ ] Actual WebView for YouTube/Browser (placeholder UI done)
- [ ] Voice input for orb
- [ ] Camera-based features (fridge scan, etc.)
- [ ] Location-based features (local scan)
- [ ] Push notifications
- [ ] Data sync across devices
- [ ] Onboarding Genesis Flow
- [ ] Settings/Consent & Permissions Screen
- [ ] Demo Mode Screen

---

## 🚀 HACKATHON ALIGNMENT

### Strategic Tracks Addressed:

| Track | Implementation |
|-------|---------------|
| 🧠 Marathon Agent | MarathonAgentEngine with Thought Signatures |
| 👨‍🏫 Real-Time Teacher | RealTimeTeacherEngine with adaptive strategies |
| ☯️ Vibe Engineering | VerificationLoop for self-checking |
| 🎨 Creative Autopilot | Skill Tree visualization (planned) |

### What Makes This Win:
1. **Not a chatbot**: Multi-day autonomous agents
2. **Not RAG**: Full 1M context reasoning planned
3. **Not simple vision**: Spatial-temporal video understanding
4. **Not prompt wrapper**: Self-correcting verification loops
5. **Complete Life OS**: 4 zones covering all student needs

---

## 📁 KEY FILES SUMMARY

| File | Purpose |
|------|---------|
| `MainShell.kt` | Navigation shell with 5-item bar & orb overlay |
| `DashboardScreen.kt` | Home screen with all stats |
| `StudySessionsScreen.kt` | Study session management |
| `StudyRoomScreen.kt` | Actual study environment |
| `LifeSupportAgentsScreen.kt` | More screen with agent stats |
| `ZoneDetailScreen.kt` | Dynamic zone feature screens |
| `ProfileScreen.kt` | User settings |
| `ZoneModels.kt` | Data models for zones & states |
| `MarathonAgentEngine.kt` | Autonomous agent system |
| `RealTimeTeacherEngine.kt` | Adaptive teaching system |
| `GeminiOrchestrator.kt` | AI orchestration layer |
| `KaironexColors.kt` | Design system colors |
| `KxOrb.kt` | Animated orb component |

---

## 🎯 SUMMARY

Kaironex is a comprehensive student life management application that:

1. **Protects Study Time**: The Cortex (Study Room) is the hero
2. **Handles Life**: 3 Life Support Agents manage career, food/finance, and local integration
3. **Uses AI Smartly**: Marathon agents run for days, not just responding to prompts
4. **Adapts in Real-Time**: Teacher adjusts based on student comprehension
5. **Works Everywhere**: Kotlin Multiplatform for Android and Desktop

The architecture is solid, the UI is complete, but the AI backend integration is the next major step to make this a winner.
