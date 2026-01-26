# KAIRONEX - Complete System Documentation
## Student Life Operating System

**Version:** 1.0.0-alpha  
**Last Updated:** January 25, 2026  
**Platform:** Kotlin Multiplatform (Android + Desktop/JVM)  
**Framework:** Compose Multiplatform with Voyager Navigation

---

## 📋 Table of Contents

1. [System Overview](#1-system-overview)
2. [Architecture](#2-architecture)
3. [Navigation Flow](#3-navigation-flow)
4. [Feature Modules](#4-feature-modules)
5. [UI Components Library](#5-ui-components-library)
6. [Data Models](#6-data-models)
7. [Core Systems](#7-core-systems)
8. [Theme & Design System](#8-theme--design-system)
9. [Backend Integration Points](#9-backend-integration-points)
10. [TODO: Features Pending Implementation](#10-todo-features-pending-implementation)

---

## 1. System Overview

### 1.1 What is Kaironex?

Kaironex is a **Student Life Operating System** designed to solve the "Prompt Gap" - the collision between life responsibilities and study time. It's not just a productivity app; it's an AI-powered companion that manages the student's entire life context.

### 1.2 Core Philosophy

| Principle | Description |
|-----------|-------------|
| **Cortex (Study)** | The protected "safe space" for deep work |
| **Life Support Agents** | AI agents that handle life (jobs, food, finance) so students CAN study |
| **Presence, Not Surveillance** | Kairo is a caring friend, not a cop |
| **Stats Drive Behavior** | Psychological metrics, not just productivity counts |

### 1.3 Target Platforms

- **Android:** Mobile-first experience
- **Desktop (JVM):** Windows/macOS/Linux with minimum window size enforcement
- **Future:** iOS, Web

---

## 2. Architecture

### 2.1 Project Structure

```
composeApp/
├── src/
│   ├── commonMain/kotlin/com/mursaline/kaironex/
│   │   ├── App.kt                    # Entry point
│   │   ├── MainShellScreen.kt        # Main navigation shell
│   │   ├── core/                     # Core systems
│   │   │   ├── presence/             # Presence detection engine
│   │   │   └── stats/                # Stats data models
│   │   ├── features/                 # Feature modules
│   │   │   ├── auth/                 # Authentication
│   │   │   ├── dashboard/            # Home dashboard
│   │   │   ├── genesis/              # Legacy onboarding
│   │   │   ├── lifesupport/          # Life Support Agents
│   │   │   ├── onboarding/           # New onboarding flow
│   │   │   ├── profile/              # User profile
│   │   │   ├── splash/               # Splash screen
│   │   │   ├── study/                # Study room
│   │   │   └── zones/                # Life zones
│   │   ├── platform/                 # Platform-specific code
│   │   └── ui/                       # UI components & theme
│   │       ├── components/           # Reusable components
│   │       └── theme/                # Colors, typography
│   ├── androidMain/                  # Android-specific
│   └── jvmMain/                      # Desktop-specific
```

### 2.2 Technology Stack

| Layer | Technology |
|-------|------------|
| **UI Framework** | Compose Multiplatform |
| **Navigation** | Voyager |
| **State Management** | Compose State + StateFlow |
| **Async** | Kotlin Coroutines |
| **Resources** | Compose Resources |
| **Platform Bridge** | Expect/Actual |

### 2.3 Key Dependencies

```text
// Navigation
cafe.adriel.voyager:voyager-navigator
cafe.adriel.voyager:voyager-transitions

// UI
androidx.compose.material3
androidx.compose.material.icons-extended

// Platform (Desktop)
com.sun.jna:jna-platform  // Window management
```

---

## 3. Navigation Flow

### 3.1 Complete User Journey

```
┌─────────────────────────────────────────────────────────────────┐
│                        APP LAUNCH                                │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      SPLASH SCREEN                               │
│  • Robot hero animation                                          │
│  • "KAIRONEX" branding                                          │
│  • Auto-navigate after 2.5s                                      │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      LOGIN SCREEN                                │
│  • Email/Password login                                          │
│  • Google Sign-In                                                │
│  • Judge Access button                                           │
│  • Navigate to: Signup / MainShell                               │
└─────────────────────────────────────────────────────────────────┘
           │                                    │
           ▼                                    ▼
┌─────────────────────┐            ┌─────────────────────────────┐
│   JUDGE LOGIN       │            │      SIGNUP SCREEN          │
│   (Hackathon Demo)  │            │  • Name, Email, Password    │
└─────────────────────┘            │  • Navigate to: Setup       │
                                   └─────────────────────────────┘
                                                │
                                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                   SYSTEM SETUP (3 Steps)                         │
│  Step 1: Name Preference - "What should I call you?"            │
│  Step 2: Wake Word - "Hey Kairo", "OK Kairo", Custom            │
│  Step 3: Interaction Mode - Voice 🎤 or Chat 💬                  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   GENESIS INTERVIEW                              │
│  • Conversational data collection (Voice or Chat)               │
│  • 15 questions about student life                              │
│  • Profile summary at completion                                │
│  • Google Drive connection option                               │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      MAIN SHELL                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                    CONTENT AREA                          │    │
│  │  • Dashboard (Home)                                      │    │
│  │  • Study Room                                            │    │
│  │  • Life Support Agents                                   │    │
│  │  • Profile                                               │    │
│  └─────────────────────────────────────────────────────────┘    │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                   NAVIGATION BAR                         │    │
│  │   [Home]  [Study]  [◉ ORB]  [More]  [Profile]           │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 Navigation Bar Structure

| Tab | Icon | Destination | Description |
|-----|------|-------------|-------------|
| **Home** | 🏠 | DashboardScreen | Daily command center with stats |
| **Study** | 📚 | StudyRoomScreen | Protected study environment |
| **Orb** | ◉ | ImmersiveAssistantPanel | AI assistant overlay |
| **More** | ➕ | LifeSupportAgentsScreen | Life support agents hub |
| **Profile** | 👤 | ProfileScreen | User settings & account |

### 3.3 Screen Registry

| Screen Object | File Location | Purpose |
|---------------|---------------|---------|
| `SplashScreen` | `features/splash/` | Animated app launch |
| `LoginScreen` | `features/auth/` | User authentication |
| `SignupScreen` | `features/auth/` | New user registration |
| `JudgeLoginScreen` | `features/auth/` | Hackathon demo access |
| `SystemSetupScreen` | `features/onboarding/` | Initial configuration |
| `GenesisInterviewScreen` | `features/onboarding/` | Student data collection |
| `MainShellScreen` | Root | Main navigation container |
| `DashboardScreen` | `features/dashboard/` | Home/Stats dashboard |
| `StudyRoomScreen` | `features/study/` | Study environment |
| `LifeSupportAgentsScreen` | `features/lifesupport/` | Agents overview |
| `ProfileScreen` | `features/profile/` | User profile & settings |
| `ZoneDetailScreen` | `features/zones/` | Individual zone details |

---

## 4. Feature Modules

### 4.1 Authentication (`features/auth/`)

#### Files:
- `LoginScreen.kt` - Main login UI
- `SignupScreen.kt` - Registration UI
- `JudgeLoginScreen.kt` - Demo access
- `AuthLayout.kt` - Shared auth layout
- `MockAuthRepository.kt` - Temporary auth logic
- `components/AuthUIComponents.kt` - Premium UI components

#### Features:
- Email/Password authentication
- Google Sign-In (placeholder)
- Judge demo access with code
- 3D floating card effects
- Glassmorphism styling
- Robot hero image integration

---

### 4.2 Onboarding (`features/onboarding/`)

#### Files:
- `SystemSetupScreen.kt` - 3-step initial setup
- `GenesisInterviewScreen.kt` - AI interview flow

#### SystemSetupScreen Steps:
1. **Name Preference** - How Kairo addresses user
2. **Wake Word** - Voice activation phrase
3. **Interaction Mode** - Voice or Chat preference

#### GenesisInterviewScreen:
**Data Collected:**
| Field | Type | Example |
|-------|------|---------|
| `name` | String | "Alex" |
| `university` | String | "MIT" |
| `degreeType` | String | "BTech, BSc, Masters, PhD" |
| `major` | String | "Computer Science" |
| `semester` | String | "3rd Semester" |
| `hasPartTimeJob` | Boolean | true/false |
| `jobTitle` | String | "Barista at Starbucks" |
| `workSchedule` | String | "Mon-Fri 4PM-8PM" |
| `weeklyWorkHours` | Int | 20 |
| `studyGoals` | List<String> | ["Pass exams", "Get internship"] |
| `challenges` | List<String> | ["Time management", "Focus"] |
| `preferredStudyTime` | String | "Early morning" |
| `extracurriculars` | List<String> | ["Basketball", "Chess club"] |
| `internationalStudent` | Boolean | true/false |
| `sleepSchedule` | String | "11PM-7AM" |

#### Interview Flow States:
```
INTERVIEWING → PROFILE_COMPLETE → DRIVE_CONNECT → Main App
```

---

### 4.3 Dashboard (`features/dashboard/`)

#### Files:
- `DashboardScreen.kt` - Main dashboard container

#### Components (`components/`):
| Component | Purpose |
|-----------|---------|
| `CortexHeroCard.kt` | Study focus card with pressure visualization |
| `HomeStatCards.kt` | Cognitive, Learning, Mental, Pressure stats |
| `ScheduledTasksCard.kt` | Today's scheduled study blocks |
| `DriveIngestionCard.kt` | File upload & Drive integration |
| `PressureMap.kt` | Weekly pressure heatmap visualization |
| `LifeTracksGrid.kt` | Life zone cards grid |
| `ActiveAgentDeck.kt` | Active AI agents display |

#### Dashboard Sections (Top to Bottom):
1. **Header** - Current mode, burnout status
2. **Cortex Hero Card** - Main study focus area
3. **Cognitive Performance** - Focus score, deep work, distractions
4. **Learning Progress** - Concept mastery, weak areas
5. **Mental State** - Motivation, burnout risk, confidence
6. **Pressure & Risk** - Pressure index, life interference
7. **Scheduled Tasks** - Today's study schedule
8. **Drive Ingestion** - File upload area
9. **Pressure Map** - Weekly visualization

---

### 4.4 Study Room (`features/study/`)

#### Files:
- `StudyRoomScreen.kt` - Main study environment

#### Components (`components/`):
| Component | Purpose |
|-----------|---------|
| `StudyHeader.kt` | Timer, session info |
| `StudyViewer.kt` | Content viewer (PDF, Video, Browser) |
| `ResourceIndex.kt` | Study materials sidebar |
| `StudyTools.kt` | AI tools panel |
| `GeminiIntervention.kt` | Drift intervention overlay |
| `InAppYouTubePlayer.kt` | Controlled video player (placeholder) |
| `InAppSecureBrowser.kt` | Secure study browser (placeholder) |

#### Gatekeeper (`gatekeeper/`):
- `GatekeeperModal.kt` - Quiz before leaving study
- `MockTestModels.kt` - Mock test data structures

#### Study Room Features:
- **Resource Index** - PDFs, videos, notes
- **AI Assistant** - Chat with Kairo
- **Flashcard Reviewer** - Spaced repetition
- **Mock Tests** - Practice quizzes
- **Timer** - Pomodoro/custom timers
- **Focus Mode** - Distraction blocking

---

### 4.5 Life Support Agents (`features/lifesupport/`)

#### Files:
- `LifeSupportAgentsScreen.kt` - Agents hub

#### Life Stability Score Components:
- Global life stability (0-100)
- Contribution breakdown by agent
- System Intelligence meta card

#### Three Main Agents:

**🚀 Campaign Agent (Career & Growth)**
| Stat | Description |
|------|-------------|
| Hiring Probability | AI-predicted job success % |
| Applications Sent | Total applications |
| Interviews Scheduled | Upcoming interviews |
| Resume Strength | ATS optimization score |
| Skills Progress | Learning completion % |

**⚡ Vitality Agent (Food & Finance)**
| Stat | Description |
|------|-------------|
| Budget Runway | Days until broke |
| Burnout Risk | Low/Medium/High |
| Sleep Quality | Rest effectiveness |
| Energy Level | Current energy state |
| Financial Stability | Overall money health |

**📡 Radius Agent (Habitat & Social)**
| Stat | Description |
|------|-------------|
| Visa Days Remaining | For international students |
| Housing Stability | Living situation security |
| Cultural Comfort | Adaptation level |
| Safety Score | Environment safety |

---

### 4.6 Zones (`features/zones/`)

#### Files:
- `ZoneModels.kt` - Zone/Track data models
- `ZoneDetailScreen.kt` - Individual zone view

#### Components:
- `MarathonAgentMonitor.kt` - Campaign agent details
- `ZoneToolCard.kt` - Individual tool cards

#### Life Tracks Enum:
```kotlin
enum class LifeTrack {
    Marathon,   // Career & Food → "The Campaign"
    RealTime,   // Teacher & Chef → "The Vitality"
    VibeCheck,  // Finance & Resume → "The Radius"
    Creative    // Brand & Design (optional)
}
```

---

### 4.7 Profile (`features/profile/`)

#### Files:
- `ProfileScreen.kt` - User profile & settings

#### Sections:
- User avatar & info
- Account settings
- Notification preferences
- Data & privacy
- Help & support
- Sign out

---

## 5. UI Components Library

### 5.1 Core Components (`ui/components/`)

| Component | File | Purpose |
|-----------|------|---------|
| `KxButton` | `KxButton.kt` | Styled buttons with variants |
| `KxCard` | `KxCard.kt` | Card containers with variants |
| `KxBadge` | `KxBadge.kt` | Status badges |
| `KxOrb` | `KxOrb.kt` | AI orb with states |
| `KxTextField` | `KxTextField.kt` | Input fields |

### 5.2 KxButton Variants
```kotlin
enum class KxButtonVariant {
    Primary,    // Blue filled
    Secondary,  // Gray filled
    Outline,    // Bordered
    Ghost,      // Text only
    Danger      // Red destructive
}
```

### 5.3 KxCard Variants
```kotlin
enum class KxCardVariant {
    Elevated,   // Shadow elevation
    Outlined,   // Border only
    Flat        // No elevation
}
```

### 5.4 KxOrb States
```kotlin
enum class KxOrbState {
    Idle,       // Calm pulse
    Listening,  // Mic active
    Thinking,   // Processing
    Speaking,   // Outputting
    Active      // Engaged
}
```

### 5.5 KxBadge Variants
```kotlin
enum class KxBadgeVariant {
    Default,
    Primary,
    Success,
    Warning,
    Error
}
```

---

## 6. Data Models

### 6.1 Stats Models (`core/stats/`)

#### HomeStats
```kotlin
data class HomeStats(
    val cognitive: CognitiveStats,
    val learning: LearningStats,
    val habit: HabitStats,
    val mentalState: MentalStateStats,
    val pressure: PressureStats,
    val direction: DirectionStats
)
```

#### CognitiveStats
```kotlin
data class CognitiveStats(
    val focusScore: Int,           // 0-100
    val focusTrend: StatTrend,     // UP, DOWN, STABLE
    val deepWorkMinutes: Int,
    val deepWorkTarget: Int,
    val distractionsBlocked: Int,
    val cognitiveLoad: CognitiveLoad,
    val retentionStrength: Int
)
```

#### MentalStateStats
```kotlin
data class MentalStateStats(
    val motivationLevel: MotivationLevel,
    val burnoutRisk: BurnoutRisk,
    val confidenceTrend: StatTrend,
    val currentMode: StudyMode,
    val aiInsight: String
)
```

#### PressureStats
```kotlin
data class PressureStats(
    val pressureIndex: Int,        // 0-100
    val lifeInterferenceRatio: Float,
    val studyCapacityRemaining: Float,
    val crisisProximity: List<CrisisItem>
)
```

### 6.2 Study Models

#### CortexState
```kotlin
data class CortexState(
    val currentTopic: String,
    val topicIcon: String,
    val focusScore: Int,
    val streakDays: Int,
    val progress: Float,
    val sessionTime: String,
    val aiStatus: String
)
```

#### ScheduledTask
```kotlin
data class ScheduledTask(
    val id: String,
    val title: String,
    val subject: String,
    val startTime: String,
    val endTime: String,
    val duration: String,
    val status: TaskStatus,
    val priority: TaskPriority
)
```

#### UploadedFile
```kotlin
data class UploadedFile(
    val id: String,
    val name: String,
    val type: FileType,
    val size: String,
    val uploadedAt: String,
    val status: IngestionStatus
)
```

### 6.3 Onboarding Models

#### SystemSetupConfig
```kotlin
data class SystemSetupConfig(
    val userName: String,
    val preferredName: String,
    val wakeWord: String,
    val interactionMode: InteractionMode,
    val isDriveConnected: Boolean
)
```

#### StudentProfile
```kotlin
data class StudentProfile(
    val name: String,
    val university: String,
    val degreeType: String,
    val major: String,
    val semester: String,
    val hasPartTimeJob: Boolean,
    val jobTitle: String,
    val workSchedule: String,
    val weeklyWorkHours: Int,
    val studyGoals: List<String>,
    val challenges: List<String>,
    val preferredStudyTime: String,
    val extracurriculars: List<String>,
    val sleepSchedule: String,
    val internationalStudent: Boolean,
    val visaType: String
)
```

---

## 7. Core Systems

### 7.1 Presence Engine (`core/presence/`)

**Purpose:** Detects if user is focused on app or drifted away

#### States:
```kotlin
enum class PresenceState {
    FOCUSED,        // User is in the app
    DRIFTING,       // User left, warning timer ticking
    INTERVENTION,   // Time up! Kairo is "calling"
    NEGOTIATING,    // User explaining their absence
    ON_BREAK        // User on approved break
}
```

#### Key Functions:
| Function | Description |
|----------|-------------|
| `startSession()` | Begin monitoring |
| `stopSession()` | Stop monitoring |
| `onWindowFocusGained()` | App came to foreground |
| `onWindowFocusLost()` | App went to background |
| `startNegotiation()` | User picked up the "call" |
| `grantBreak(duration)` | Approve user break |
| `resumeFromNegotiation()` | User returned |

#### Configuration:
```kotlin
data class PresenceConfig(
    val driftToleranceMs: Long = 15_000L,      // 15s grace
    val maxDriftBeforeCallMs: Long = 30_000L, // 30s intervention
    val defaultBreakDurationMs: Long = 300_000L // 5min break
)
```

### 7.2 Gemini Intervention System

**Components:**
| Component | Purpose |
|-----------|---------|
| `GeminiInterventionOverlay` | Full-screen "call" UI |
| `NegotiationDialog` | Break negotiation dialog |
| `DriftingWarningBadge` | Small warning indicator |
| `OnBreakBadge` | Break status indicator |

### 7.3 Platform Systems (`platform/`)

#### SystemMonitor Interface:
```kotlin
interface SystemMonitor {
    fun isWindowFocused(): Boolean
    fun getCurrentProcessName(): String
}
```

#### Desktop Eye (Legacy):
- Window title monitoring
- Focus detection for desktop
- Privacy-first design (opt-in only)

---

## 8. Theme & Design System

### 8.1 Color Palette (`ui/theme/KaironexColors.kt`)

#### Primary Colors:
| Color | Hex | Usage |
|-------|-----|-------|
| `ElectricBlue` | `#2563EB` | Primary actions |
| `GeminiBlurple` | `#6366F1` | AI/Orb elements |
| `InkBlack` | `#1A1A2E` | Text, dark elements |
| `SlateGray` | `#64748B` | Secondary text |
| `CloudGray` | `#F1F5F9` | Backgrounds |
| `CanvasWhite` | `#FFFFFF` | Cards, surfaces |

#### Semantic Colors:
| Color | Hex | Usage |
|-------|-----|-------|
| `SuccessGreen` | `#22C55E` | Success states |
| `AttentionOrange` | `#F59E0B` | Warnings |
| `CriticalRed` | `#EF4444` | Errors, alerts |

#### Gradient Colors:
| Name | Colors |
|------|--------|
| `IndigoGradient` | Purple → Indigo |
| `GeminiGradient` | Blue → Purple |

### 8.2 Typography

Uses Material 3 typography scale:
- `headlineLarge` - Major titles
- `headlineMedium` - Section headers
- `titleMedium` - Card titles
- `bodyMedium` - Body text
- `labelMedium` - Labels, captions

### 8.3 Spacing System

| Size | Value |
|------|-------|
| `xs` | 4.dp |
| `sm` | 8.dp |
| `md` | 12.dp |
| `lg` | 16.dp |
| `xl` | 24.dp |
| `2xl` | 32.dp |

### 8.4 Border Radius

| Size | Value |
|------|-------|
| `small` | 8.dp |
| `medium` | 12.dp |
| `large` | 16.dp |
| `xl` | 20.dp |
| `2xl` | 24.dp |
| `full` | CircleShape |

---

## 9. Backend Integration Points

### 9.1 Authentication

**Required APIs:**
| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/auth/login` | POST | Email/password login |
| `/auth/signup` | POST | User registration |
| `/auth/google` | POST | Google OAuth |
| `/auth/judge` | POST | Judge demo access |
| `/auth/refresh` | POST | Token refresh |

### 9.2 User Profile

**Required APIs:**
| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/user/profile` | GET | Fetch user profile |
| `/user/profile` | PUT | Update profile |
| `/user/onboarding` | POST | Save interview data |
| `/user/preferences` | PUT | Update preferences |

### 9.3 Study Sessions

**Required APIs:**
| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/study/sessions` | GET | List sessions |
| `/study/sessions` | POST | Start session |
| `/study/sessions/{id}` | PUT | Update session |
| `/study/sessions/{id}/end` | POST | End session |
| `/study/stats` | GET | Get study statistics |

### 9.4 File Ingestion

**Required APIs:**
| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/files/upload` | POST | Upload file |
| `/files/drive/connect` | POST | Connect Google Drive |
| `/files/drive/sync` | POST | Sync from Drive |
| `/files` | GET | List files |
| `/files/{id}/index` | POST | Index file content |

### 9.5 AI/Gemini Integration

**Required APIs:**
| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/ai/chat` | POST | Chat with Kairo |
| `/ai/voice` | POST | Voice interaction |
| `/ai/analyze` | POST | Analyze content |
| `/ai/flashcards` | POST | Generate flashcards |
| `/ai/mocktest` | POST | Generate mock test |
| `/ai/gatekeeper` | POST | Gatekeeper quiz |

### 9.6 Life Support Agents

**Required APIs:**
| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/agents/campaign/status` | GET | Career agent status |
| `/agents/vitality/status` | GET | Vitality agent status |
| `/agents/radius/status` | GET | Radius agent status |
| `/agents/{type}/action` | POST | Trigger agent action |

### 9.7 Stats & Analytics

**Required APIs:**
| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/stats/home` | GET | Dashboard stats |
| `/stats/cognitive` | GET | Cognitive metrics |
| `/stats/pressure` | GET | Pressure map data |
| `/stats/progress` | GET | Learning progress |

---

## 10. TODO: Features Pending Implementation

### 10.1 Critical (Must Have Before Launch)

| Feature | Status | Notes |
|---------|--------|-------|
| **Authentication Backend** | ❌ Placeholder | Need real OAuth + session |
| **Database Persistence** | ❌ Missing | SQLDelight or Room |
| **Gemini API Integration** | ❌ Placeholder | Connect to Gemini 3 Pro |
| **Voice Input (STT)** | ❌ Placeholder | Speech-to-text |
| **Voice Output (TTS)** | ❌ Placeholder | Text-to-speech |
| **Google Drive OAuth** | ❌ Placeholder | Real Drive integration |
| **File Indexing** | ❌ Placeholder | PDF/content parsing |
| **Push Notifications** | ❌ Missing | Intervention alerts |

### 10.2 High Priority

| Feature | Status | Notes |
|---------|--------|-------|
| **PDF Viewer** | ❌ Placeholder | Real PDF rendering |
| **YouTube Player** | ❌ Placeholder | Embedded player |
| **Secure Browser** | ❌ Placeholder | WebView with controls |
| **Gatekeeper Quiz Logic** | ❌ Placeholder | AI-generated questions |
| **Flashcard System** | ❌ Placeholder | Spaced repetition |
| **Mock Test Engine** | ❌ Placeholder | Quiz generation |
| **Timer System** | ❌ Basic | Pomodoro logic |

### 10.3 Medium Priority

| Feature | Status | Notes |
|---------|--------|-------|
| **Keyboard Shortcuts** | ❌ Missing | Desktop power users |
| **Haptic Feedback** | ❌ Missing | Mobile micro-interactions |
| **Shared Element Transitions** | ❌ Basic | Card expansions |
| **Skill Tree Visualization** | ❌ Missing | Canvas node graph |
| **Interactive Pressure Map** | ❌ Basic | Tap for details |
| **Empty State Animations** | ❌ Basic | "Scanning..." effects |

### 10.4 Low Priority (Polish)

| Feature | Status | Notes |
|---------|--------|-------|
| **Themes/Skins** | ❌ Missing | Dark mode, custom themes |
| **Avatar Customization** | ❌ Missing | Profile personalization |
| **Gamification/Badges** | ❌ Missing | Achievement system |
| **Social Features** | ❌ Missing | Study groups |
| **Admin Dashboard** | ❌ Missing | Institution view |

---

## Appendix A: File Registry

### Complete File List

```
composeApp/src/commonMain/kotlin/com/mursaline/kaironex/
├── App.kt
├── MainShellScreen.kt
├── core/
│   ├── presence/
│   │   └── PresenceEngine.kt
│   └── stats/
│       ├── StatsModels.kt
│       └── StatsProvider.kt
├── features/
│   ├── auth/
│   │   ├── AuthLayout.kt
│   │   ├── JudgeLoginScreen.kt
│   │   ├── LoginScreen.kt
│   │   ├── MockAuthRepository.kt
│   │   ├── SignupScreen.kt
│   │   └── components/
│   │       └── AuthUIComponents.kt
│   ├── dashboard/
│   │   ├── DashboardScreen.kt
│   │   ├── components/
│   │   │   ├── ActiveAgentDeck.kt
│   │   │   ├── CortexHeroCard.kt
│   │   │   ├── DriveIngestionCard.kt
│   │   │   ├── HomeStatCards.kt
│   │   │   ├── LifeTracksGrid.kt
│   │   │   ├── OmniMenuDrawer.kt
│   │   │   ├── PressureMap.kt
│   │   │   └── ScheduledTasksCard.kt
│   │   └── tabs/
│   │       ├── ChatScreen.kt
│   │       └── SettingsScreen.kt
│   ├── genesis/
│   │   └── GenesisInterviewScreen.kt (Legacy)
│   ├── lifesupport/
│   │   └── LifeSupportAgentsScreen.kt
│   ├── onboarding/
│   │   ├── GenesisInterviewScreen.kt
│   │   └── SystemSetupScreen.kt
│   ├── profile/
│   │   └── ProfileScreen.kt
│   ├── splash/
│   │   └── SplashScreen.kt
│   ├── study/
│   │   ├── StudyRoomScreen.kt
│   │   ├── components/
│   │   │   ├── GeminiIntervention.kt
│   │   │   ├── InAppSecureBrowser.kt
│   │   │   ├── InAppYouTubePlayer.kt
│   │   │   ├── ResourceIndex.kt
│   │   │   ├── StudyHeader.kt
│   │   │   ├── StudyTools.kt
│   │   │   └── StudyViewer.kt
│   │   └── gatekeeper/
│   │       ├── GatekeeperModal.kt
│   │       └── MockTestModels.kt
│   └── zones/
│       ├── ZoneDetailScreen.kt
│       ├── ZoneModels.kt
│       └── components/
│           └── MarathonAgentMonitor.kt
├── platform/
│   ├── DesktopEye.kt
│   └── SystemMonitor.kt
└── ui/
    ├── components/
    │   ├── KxBadge.kt
    │   ├── KxButton.kt
    │   ├── KxCard.kt
    │   ├── KxOrb.kt
    │   └── KxTextField.kt
    └── theme/
        └── KaironexColors.kt
```

---

## Appendix B: Keyboard Shortcuts (Planned)

| Shortcut | Action |
|----------|--------|
| `Space` | Pause/Play timer |
| `Cmd/Ctrl + K` | Summon Orb assistant |
| `Cmd/Ctrl + S` | Save session |
| `Cmd/Ctrl + N` | New study block |
| `Esc` | Close overlay/modal |
| `F` | Toggle fullscreen (desktop) |

---

## Appendix C: Error Codes (Planned)

| Code | Description |
|------|-------------|
| `AUTH_001` | Invalid credentials |
| `AUTH_002` | Session expired |
| `AUTH_003` | Account locked |
| `STUDY_001` | Session not found |
| `STUDY_002` | Timer error |
| `FILE_001` | Upload failed |
| `FILE_002` | Indexing failed |
| `AI_001` | Gemini API error |
| `AI_002` | Context too large |

---

**Document End**

*This documentation should be updated as features are implemented.*
