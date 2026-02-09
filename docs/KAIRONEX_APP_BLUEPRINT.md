# 🧠 KAIRONEX APP BLUEPRINT
## Complete Implementation Guide for Kotlin Multiplatform (Android)

---

## 📋 TABLE OF CONTENTS
1. [Architecture Overview](#architecture-overview)
2. [Model Configuration](#model-configuration)
3. [Database Schema (Complete)](#database-schema-complete)
4. [Agent Architecture (Brain vs App)](#agent-architecture)
5. [Wake Word & Voice System](#wake-word--voice-system)
6. [All App Screens (Complete List)](#all-app-screens)
7. [Data Flow & Sync](#data-flow--sync)
8. [Demo Dashboard](#demo-dashboard)
9. [Implementation Checklist](#implementation-checklist)

---

## 🏗️ ARCHITECTURE OVERVIEW

```
┌─────────────────────────────────────────────────────────────────┐
│                     KAIRONEX SYSTEM                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────┐         ┌─────────────────────────┐   │
│  │   📱 ANDROID APP    │         │   ☁️ APPWRITE BRAIN     │   │
│  │   (Kotlin/KMP)      │◄───────►│   (Python Backend)      │   │
│  │                     │   API   │                         │   │
│  │  ┌───────────────┐  │         │  ┌───────────────────┐  │   │
│  │  │ REFLEX AGENT  │  │         │  │ DEEP BRAIN        │  │   │
│  │  │ (LOCAL)       │  │         │  │ (gemini-3-flash)  │  │   │
│  │  │ - Instant UI  │  │         │  │ - Complex tasks   │  │   │
│  │  │ - Quick reply │  │         │  │ - Research        │  │   │
│  │  │ - Caching     │  │         │  │ - Planning        │  │   │
│  │  └───────────────┘  │         │  └───────────────────┘  │   │
│  │                     │         │                         │   │
│  │  ┌───────────────┐  │         │  ┌───────────────────┐  │   │
│  │  │ LIVE AUDIO    │  │         │  │ APPWRITE DB       │  │   │
│  │  │ (gemini-2.5   │◄─┼─────────┼─►│ (18 Collections)  │  │   │
│  │  │  audio)       │  │         │  │                   │  │   │
│  │  └───────────────┘  │         │  └───────────────────┘  │   │
│  └─────────────────────┘         └─────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Key Principle: REFLEX in App, DEEP in Cloud

| Component | Location | Model | Purpose |
|-----------|----------|-------|---------|
| **Reflex Agent** | 📱 App | gemini-3-flash (MINIMAL thinking) | Instant responses, UI updates |
| **Live Voice** | 📱 App | gemini-2.5-audio (WebSocket) | Real-time conversation |
| **Deep Brain** | ☁️ Cloud | gemini-3-flash (HIGH thinking) | Planning, research, complex reasoning |

---

## 🤖 MODEL CONFIGURATION

### ⚠️ CRITICAL: Use ONLY These Two Models

```kotlin
// ============================================================
// KAIRONEX USES EXACTLY TWO GEMINI MODELS - NO EXCEPTIONS
// ============================================================

object KaironexModels {
    
    // PRIMARY MODEL: All text reasoning, function calling, thinking
    const val GEMINI_3_FLASH = "gemini-3-flash-preview"
    
    // AUDIO MODEL: Real-time voice via WebSocket (Live API)
    const val GEMINI_AUDIO = "gemini-2.5-flash-native-audio-preview-12-2025"
    
    // DO NOT USE any other models!
}
```

### Model Usage Matrix

| Use Case | Model | API Type | Thinking Level |
|----------|-------|----------|----------------|
| Quick UI responses | gemini-3-flash | REST | MINIMAL |
| Voice conversation | gemini-2.5-audio | WebSocket (Live) | N/A |
| Deep planning | gemini-3-flash | REST | HIGH |
| Research | gemini-3-flash | REST + Google Search | HIGH |
| Function calling | gemini-3-flash | REST | MINIMAL/HIGH |

### Kotlin Implementation

```kotlin
// For text/reasoning (Reflex in app)
val reflexClient = GenerativeModel(
    modelName = "gemini-3-flash-preview",
    generationConfig = generationConfig {
        thinkingConfig {
            thinkingLevel = ThinkingLevel.MINIMAL  // Fast!
        }
    }
)

// For voice (Live API via WebSocket)
val liveSession = genAI.live.connect(
    model = "gemini-2.5-flash-native-audio-preview-12-2025",
    config = LiveConnectConfig(
        responseModalities = listOf(Modality.AUDIO),
        speechConfig = SpeechConfig(
            voiceConfig = VoiceConfig(prebuiltVoiceConfig = PrebuiltVoiceConfig("Kore"))
        )
    )
)
```

---

## 🗄️ DATABASE SCHEMA (COMPLETE)

### Appwrite Configuration
```
Endpoint: https://nyc.cloud.appwrite.io/v1
Project ID: 696e9248002198ef6273
Database ID: 697cb20f00110f6d7530
```

### 18 Collections Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                    KAIRONEX DATABASE                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  🔵 CORE USER DATA                                              │
│  ├── users                 (Main user profile + state cache)   │
│  ├── student_profiles      (Learning patterns, personality)    │
│  └── resources             (Drive links, study materials)      │
│                                                                 │
│  🟢 ACTIVITY TRACKING                                           │
│  ├── schedule              (Tasks, lectures, deadlines)        │
│  ├── study_logs            (Focus sessions, duration)          │
│  ├── daily_snapshots       (End-of-day summaries)              │
│  └── schedule_changes      (Proposed/applied changes)          │
│                                                                 │
│  🟡 THE 4 ZONES (Gamified State)                                │
│  ├── vitality_state        (BioFuel, RegenMode, Resources)     │
│  ├── campaign_state        (SkillTree, Quests, Armory)         │
│  ├── radius_state          (Safehouse, LocalScan, Social)      │
│  └── concept_mastery       (Subject mastery, quiz tracking)    │
│                                                                 │
│  🔴 AGENT SYSTEM                                                │
│  ├── agent_memory          (Current state, pressure index)     │
│  ├── interventions         (AI nudges, user responses)         │
│  ├── marathon_sessions     (Long-running agent tasks)          │
│  ├── thought_signatures    (Reasoning chain, signatures)       │
│  └── policy_episodes       (RL training data)                  │
│                                                                 │
│  🟣 SURVIVAL SYSTEMS                                            │
│  ├── life_events           (Family, health, emergencies)       │
│  ├── financial_state       (Budget, job, paycheck)             │
│  └── international_info    (Visa, work limits)                 │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

### 📊 DETAILED SCHEMA

#### 1. `users` - Main User Table
```
PURPOSE: Central user profile with cached state for fast app loading
SYNC: App reads this on startup for instant UI

| Column             | Type    | Required | Description                          |
|--------------------|---------|----------|--------------------------------------|
| $id                | string  | auto     | Appwrite document ID                 |
| userId             | string  | ✅       | Unique user identifier               |
| studentprofile_json| string  | ✅       | Basic profile (name, uni, major)     |
| studentState_json  | string  | ✅       | CACHED STATE - All zones in 1 call   |
| preferences        | string  | ❌       | App settings, notifications          |
```

**`studentState_json` Structure (God Mode Sync):**
```json
{
  "vitality": { "bio_fuel": {...}, "regen_mode": {...} },
  "campaign": { "skill_tree": {...}, "quests": [...] },
  "radius": { "safehouse": {...}, "social_graph": {...} },
  "pressure_index": 45,
  "last_sync": "2026-02-03T12:00:00Z"
}
```

---

#### 2. `agent_memory` - Brain State
```
PURPOSE: Tracks what the AI agent is currently thinking/doing
SYNC: Real-time updates for demo dashboard

| Column                    | Type     | Required | Description                    |
|---------------------------|----------|----------|--------------------------------|
| $id                       | string   | auto     | Document ID                    |
| userId                    | string   | ✅       | User this memory belongs to    |
| current_thought_signature | string   | ❌       | Hash of current reasoning      |
| pressure_index            | integer  | ✅       | 0-100 stress level             |
| active_agents             | string   | ❌       | Which agents are running       |
| last_active               | datetime | ✅       | Last brain activity            |
| last_trigger_source       | string   | ❌       | What triggered the brain       |
| session_id                | string   | ❌       | Current marathon session       |
| reasoning_mode            | string   | ❌       | REFLEX / DEEP / MARATHON       |
```

---

#### 3. `schedule` - Task Schedule
```
PURPOSE: All scheduled items (lectures, study, deadlines)
SYNC: Displayed on home screen calendar

| Column          | Type     | Required | Description                                |
|-----------------|----------|----------|--------------------------------------------|
| $id             | string   | auto     | Document ID                                |
| taskId          | string   | ✅       | Unique task identifier                     |
| userId          | string   | ✅       | Owner                                      |
| title           | string   | ✅       | Task title                                 |
| startTime       | datetime | ✅       | When it starts                             |
| endTime         | datetime | ✅       | When it ends                               |
| status          | string   | ✅       | PENDING/ACTIVE/COMPLETED/SKIP              |
| type            | string   | ✅       | LECTURE/STUDY/DEADLINE/WORK/TRAVEL/BLOCKED |
| location        | string   | ❌       | Where                                      |
| is_flexible     | boolean  | ❌       | Can agent reschedule?                      |
| priority        | integer  | ❌       | 1-10                                       |
| linked_deadline | string   | ❌       | Related deadline ID                        |
| topics          | string   | ❌       | Rich task breakdown (see format below)     |
| subject         | string   | ❌       | Subject/Course name for content generation |
| difficulty      | string   | ❌       | beginner/intermediate/advanced             |
| content_mode    | string   | ❌       | deep_dive/travel/cram/practice             |
| metadata_json   | string   | ❌       | Full metadata for Proactive Content Engine |

RICH TOPICS FORMAT (for content generation):
The 'topics' field should contain a structured breakdown:
- PHASE 1 - INPUT: Reading, watching, listening
- PHASE 2 - PROCESS: Active learning (concept maps, diagrams)
- PHASE 3 - OUTPUT: Practice problems, coding, writing
- PHASE 4 - VERIFY: Self-quiz, gatekeeper quiz

METADATA_JSON STRUCTURE:
{
  "learning_objectives": ["List of specific, measurable objectives"],
  "resource_hints": ["Keywords for content search"],
  "prerequisites": ["What student must know before"],
  "deliverables": ["Concrete outputs expected"],
  "verification": {"type": "quiz", "pass_threshold": 0.8, "topics_covered": []}
}
```

---

#### 4. `study_logs` - Focus Sessions
```
PURPOSE: Track study time and focus quality
SYNC: Used for analytics and AI learning

| Column           | Type     | Required | Description                    |
|------------------|----------|----------|--------------------------------|
| $id              | string   | auto     | Document ID                    |
| logId            | string   | ✅       | Unique log identifier          |
| userId           | string   | ✅       | Owner                          |
| taskId           | string   | ❌       | Which task was being studied   |
| duration_seconds | integer  | ✅       | How long                       |
| focus_score      | integer  | ❌       | 0-100 focus quality            |
| timestamp        | datetime | ✅       | When                           |
```

---

#### 5. `vitality_state` - The Vitality Zone
```
PURPOSE: Gamified health/energy tracking (NO medical advice!)
SYNC: Updates vitality zone in app

| Column              | Type   | Required | Description                      |
|---------------------|--------|----------|----------------------------------|
| $id                 | string | auto     | Document ID                      |
| userId              | string | ✅       | Owner                            |
| bio_fuel_json       | string | ❌       | Energy levels, sleep, hydration  |
| regen_mode_json     | string | ❌       | Rest patterns, recovery          |
| resource_monitor_json| string| ❌       | Tracking resources               |
| bill_splitter_json  | string | ❌       | Shared expenses with roommates   |
```

**`bio_fuel_json` Example:**
```json
{
  "energy_level": 75,
  "hydration": "good",
  "last_meal": "2026-02-03T12:30:00Z",
  "sleep_hours_last_night": 7,
  "caffeine_intake": 2
}
```

---

#### 6. `campaign_state` - The Campaign Zone
```
PURPOSE: Gamified goals, quests, achievements
SYNC: Updates campaign zone in app

| Column              | Type   | Required | Description                      |
|---------------------|--------|----------|----------------------------------|
| $id                 | string | auto     | Document ID                      |
| userId              | string | ✅       | Owner                            |
| skill_tree_json     | string | ❌       | Skills being developed           |
| quest_board_json    | string | ❌       | Active/completed quests          |
| the_armory_json     | string | ❌       | Tools, achievements unlocked     |
| simulacrum_data_json| string | ❌       | AI persona customization         |
```

**`quest_board_json` Example:**
```json
{
  "active_quests": [
    {
      "quest_id": "q_midterm_prep",
      "title": "Midterm Mastery",
      "progress": 0.6,
      "deadline": "2026-02-15",
      "rewards": ["focus_boost", "xp_500"]
    }
  ],
  "completed_quests": ["q_first_week", "q_syllabus_scan"]
}
```

---

#### 7. `radius_state` - The Radius Zone
```
PURPOSE: Social connections, location, external awareness
SYNC: Updates radius zone in app

| Column              | Type   | Required | Description                      |
|---------------------|--------|----------|----------------------------------|
| $id                 | string | auto     | Document ID                      |
| userId              | string | ✅       | Owner                            |
| safehouse_json      | string | ❌       | Safe spaces (dorm, library)      |
| local_scan_json     | string | ❌       | Nearby resources                 |
| admin_protocol_json | string | ❌       | University admin contacts        |
| signal_decoder_json | string | ❌       | Communication preferences        |
| social_graph_json   | string | ❌       | Friends, study groups            |
| user_location       | string | ✅       | Current city/campus              |
```

---

#### 8. `concept_mastery` - Learning Progress
```
PURPOSE: Track subject/topic mastery for spaced repetition
SYNC: Shown in study analytics

| Column           | Type     | Required | Description                    |
|------------------|----------|----------|--------------------------------|
| $id              | string   | auto     | Document ID                    |
| userId           | string   | ✅       | Owner                          |
| concept_id       | string   | ✅       | Unique concept identifier      |
| subject          | string   | ❌       | e.g., "Computer Science"       |
| topic            | string   | ❌       | e.g., "Binary Trees"           |
| mastery_level    | string   | ❌       | NOVICE/LEARNING/PROFICIENT/MASTER |
| quiz_attempts    | integer  | ❌       | How many times quizzed         |
| last_quiz_score  | double   | ❌       | Last score 0-1                 |
| gate_passed      | boolean  | ❌       | Passed mastery gate?           |
| gate_passed_at   | datetime | ❌       | When mastered                  |
| next_review_date | datetime | ❌       | Spaced repetition next date    |
| learning_notes   | string   | ❌       | AI-generated notes             |
```

---

#### 9. `interventions` - AI Nudges
```
PURPOSE: Track when AI intervenes and user responses
SYNC: For demo - show recent interventions

| Column              | Type   | Required | Description                      |
|---------------------|--------|----------|----------------------------------|
| $id                 | string | auto     | Document ID                      |
| interventionId      | string | ✅       | Unique intervention ID           |
| userId              | string | ✅       | Who received it                  |
| trigger_event       | string | ✅       | What caused it                   |
| ai_message          | string | ✅       | What AI said                     |
| status              | string | ✅       | PENDING/ACKNOWLEDGED/DISMISSED   |
| user_response       | string | ❌       | What user replied                |
| outcome             | string | ❌       | POSITIVE/NEGATIVE/NEUTRAL        |
| ai_response_strategy| string | ❌       | How AI approached it             |
```

---

#### 10. `marathon_sessions` - Long-Running Tasks
```
PURPOSE: Track multi-step AI tasks that span time
SYNC: Show progress in demo dashboard

| Column               | Type     | Required | Description                    |
|----------------------|----------|----------|--------------------------------|
| $id                  | string   | auto     | Document ID                    |
| sessionId            | string   | ✅       | Unique session ID              |
| userId               | string   | ✅       | Owner                          |
| agentType            | string   | ❌       | study/vitality/campaign/radius |
| goal_json            | string   | ❌       | What the marathon is achieving |
| state_json           | string   | ❌       | Current state                  |
| thought_chain_json   | string   | ❌       | Reasoning history              |
| progress             | double   | ❌       | 0.0 - 1.0                      |
| status               | string   | ✅       | RUNNING/PAUSED/COMPLETED/FAILED|
| started_at           | datetime | ✅       | When started                   |
| last_active_at       | datetime | ❌       | Last activity                  |
| estimated_completion | datetime | ❌       | ETA                            |
```

---

#### 11. `thought_signatures` - Reasoning Chain
```
PURPOSE: Cryptographic verification of AI reasoning
SYNC: For debugging and demo

| Column            | Type     | Required | Description                    |
|-------------------|----------|----------|--------------------------------|
| $id               | string   | auto     | Document ID                    |
| thoughtId         | string   | ✅       | Unique thought ID              |
| userId            | string   | ✅       | Owner                          |
| sessionId         | string   | ✅       | Which marathon session         |
| agentType         | string   | ✅       | Which agent                    |
| signature_json    | string   | ✅       | Full signature data            |
| parent_thought_id | string   | ❌       | Chain link                     |
| confidence        | double   | ❌       | 0.0 - 1.0                      |
| reasoning_trace   | string   | ❌       | Human-readable reasoning       |
| created_at        | datetime | ❌       | When created                   |
```

---

#### 12. `life_events` - Life Disruptions
```
PURPOSE: Track events that affect schedule (family, emergencies)
SYNC: Used by AI to adjust schedule

| Column                   | Type     | Required | Description                 |
|--------------------------|----------|----------|-----------------------------|
| $id                      | string   | auto     | Document ID                 |
| userId                   | string   | ✅       | Owner                       |
| event_id                 | string   | ✅       | Unique event ID             |
| category                 | string   | ❌       | family/work/personal/travel |
| severity                 | string   | ❌       | minor/moderate/major/crisis |
| description              | string   | ❌       | What happened               |
| start_date               | datetime | ❌       | When it starts              |
| end_date                 | datetime | ❌       | When it ends                |
| affected_schedule_blocks | string   | ❌       | Which tasks affected        |
| recovery_plan            | string   | ❌       | AI's plan to handle it      |
| escalated_to_main        | boolean  | ❌       | Sent to main brain?         |
| resolved                 | boolean  | ❌       | Is it over?                 |
```

---

#### 13. `student_profiles` - Learning Profile
```
PURPOSE: AI's understanding of the student
SYNC: Read by all agents

| Column                 | Type     | Required | Description                    |
|------------------------|----------|----------|--------------------------------|
| $id                    | string   | auto     | Document ID                    |
| userId                 | string   | ✅       | Owner                          |
| confidence_level       | string   | ❌       | low/medium/high                |
| learning_style         | string   | ❌       | visual/auditory/kinesthetic    |
| study_pattern          | string   | ❌       | morning/afternoon/night        |
| communication_style    | string   | ❌       | formal/casual/motivational     |
| average_session_length | integer  | ❌       | Minutes                        |
| completion_rate        | double   | ❌       | 0.0 - 1.0                      |
| strong_subjects        | string   | ❌       | JSON array                     |
| struggling_subjects    | string   | ❌       | JSON array                     |
| personality_notes      | string   | ❌       | AI observations                |
| last_assessed          | datetime | ❌       | When profile was updated       |
```

---

#### 14. `financial_state` - Money Tracking
```
PURPOSE: Budget, job, financial stress
SYNC: Shown in vitality zone

| Column                | Type     | Required | Description                    |
|-----------------------|----------|----------|--------------------------------|
| $id                   | string   | auto     | Document ID                    |
| userId                | string   | ✅       | Owner                          |
| monthly_budget        | double   | ❌       | Budget amount                  |
| current_balance       | double   | ❌       | Current balance                |
| last_transaction      | string   | ❌       | Last expense/income            |
| expense_categories    | string   | ❌       | JSON breakdown                 |
| job_status            | string   | ❌       | employed/unemployed/part-time  |
| work_hours_this_week  | integer  | ❌       | Hours worked                   |
| next_paycheck         | datetime | ❌       | When next pay                  |
| financial_stress_level| string   | ❌       | low/medium/high/critical       |
```

---

#### 15. `international_info` - Visa & Cultural
```
PURPOSE: For international students
SYNC: Affects work hour limits

| Column            | Type     | Required | Description                    |
|-------------------|----------|----------|--------------------------------|
| $id               | string   | auto     | Document ID                    |
| userId            | string   | ✅       | Owner                          |
| visa_type         | string   | ❌       | F1/J1/H1B etc                  |
| visa_expiry       | datetime | ❌       | Expiry date                    |
| work_hour_limit   | integer  | ❌       | Max allowed (e.g., 20)         |
| work_hours_used   | integer  | ❌       | Hours used this week           |
| country_of_origin | string   | ❌       | Home country                   |
| slang_confidence  | string   | ❌       | Comfort with local slang       |
| cultural_notes    | string   | ❌       | Cultural preferences           |
| advisor_contact   | string   | ❌       | International office contact   |
| current_country   | string   | ❌       | Where they are now             |
```

---

#### 16. `schedule_changes` - Change Proposals
```
PURPOSE: Track AI-proposed and user-applied changes
SYNC: For change history

| Column            | Type     | Required | Description                    |
|-------------------|----------|----------|--------------------------------|
| $id               | string   | auto     | Document ID                    |
| userId            | string   | ✅       | Owner                          |
| change_id         | string   | ✅       | Unique change ID               |
| change_type       | string   | ❌       | RESCHEDULE/CANCEL/ADD          |
| target_block_id   | string   | ❌       | Which task affected            |
| new_block_data    | string   | ❌       | New task data                  |
| validation_result | string   | ❌       | AI validation                  |
| conflicts         | string   | ❌       | Any conflicts found            |
| user_confirmed    | boolean  | ❌       | User approved?                 |
| applied_at        | datetime | ❌       | When applied                   |
| proposed_at       | datetime | ❌       | When proposed                  |
| warnings          | string   | ❌       | Any warnings                   |
```

---

#### 17. `daily_snapshots` - End of Day Summary
```
PURPOSE: Daily summary for analytics
SYNC: Historical data

| Column       | Type     | Required | Description                    |
|--------------|----------|----------|--------------------------------|
| $id          | string   | auto     | Document ID                    |
| snapshotId   | string   | ✅       | Unique snapshot ID             |
| userId       | string   | ✅       | Owner                          |
| date         | string   | ✅       | YYYY-MM-DD                     |
| summary_json | string   | ✅       | Full day summary               |
| mood_rating  | integer  | ❌       | 1-10                           |
```

---

#### 18. `policy_episodes` - RL Training Data
```
PURPOSE: Reinforcement learning data
SYNC: Background training

| Column       | Type     | Required | Description                    |
|--------------|----------|----------|--------------------------------|
| $id          | string   | auto     | Document ID                    |
| episodeId    | string   | ✅       | Unique episode ID              |
| userId       | string   | ✅       | Owner                          |
| context_hash | string   | ✅       | State hash                     |
| action_taken | string   | ✅       | What AI did                    |
| reward       | integer  | ❌       | Reward signal                  |
| timestamp    | datetime | ✅       | When                           |
```

---

## 🎙️ WAKE WORD & VOICE SYSTEM

### Wake Word Detection

```kotlin
// Wake word: "Hey Kairo" or "Kaironex"
class WakeWordDetector {
    private val wakeWords = listOf("hey kairo", "kaironex", "okay kairo")
    
    fun startListening(onWakeWord: () -> Unit) {
        // Use Android SpeechRecognizer or Vosk for offline detection
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle) {
                val matches = results.getStringArrayList(RESULTS_RECOGNITION)
                if (matches?.any { wakeWords.contains(it.lowercase()) } == true) {
                    onWakeWord()  // Trigger calling screen!
                }
            }
        })
    }
}
```

### Voice Conversation Flow

```
┌──────────────────────────────────────────────────────────────┐
│                    VOICE INTERACTION FLOW                     │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  1. USER SAYS: "Hey Kairo"                                   │
│     └─► Wake word detected                                   │
│         └─► Show CALLING SCREEN (pulse animation)            │
│                                                              │
│  2. KAIRO RESPONDS: "Hey! What's up?"                        │
│     └─► Live API connected (WebSocket)                       │
│         └─► Show ACTIVE CALL SCREEN                          │
│                                                              │
│  3. USER SPEAKS: "I have a wedding this weekend"             │
│     └─► Audio sent to Gemini 2.5 Audio                       │
│         └─► Real-time transcription shown                    │
│                                                              │
│  4. KAIRO PROCESSES:                                         │
│     └─► Detects: life_event                                  │
│     └─► Extracts: {category: "family", severity: "minor"}    │
│     └─► Creates: life_events record in DB                    │
│                                                              │
│  5. KAIRO RESPONDS (voice):                                  │
│     "Got it! A wedding this weekend. I'll adjust your        │
│      schedule to give you Friday afternoon off for prep.     │
│      Should I reschedule your study session?"                │
│                                                              │
│  6. CONTEXT PASSED TO UI:                                    │
│     └─► Schedule screen updates                              │
│     └─► Toast: "Life event logged: Wedding"                  │
│     └─► Intervention created for follow-up                   │
│                                                              │
│  7. USER ENDS: "Thanks, bye"                                 │
│     └─► Show END CALL SCREEN                                 │
│         └─► Sync all changes to Appwrite                     │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

### Calling Screen States

```kotlin
enum class CallState {
    IDLE,           // Normal app state
    WAKING,         // Wake word detected, connecting...
    RINGING,        // Pulse animation, waiting for AI
    ACTIVE,         // In conversation
    PROCESSING,     // AI is thinking
    ENDING          // Call ending, syncing
}

@Composable
fun CallingScreen(state: CallState) {
    when (state) {
        CallState.WAKING -> WakingAnimation()      // "Connecting to Kairo..."
        CallState.RINGING -> RingingAnimation()    // Pulse circle
        CallState.ACTIVE -> ActiveCallUI()         // Waveform + transcript
        CallState.PROCESSING -> ThinkingAnimation() // Brain icon pulsing
        CallState.ENDING -> EndingAnimation()      // "Syncing changes..."
    }
}
```

---

## 📱 ALL APP SCREENS (COMPLETE LIST)

### Screen Categories

```
┌─────────────────────────────────────────────────────────────────┐
│                    KAIRONEX APP SCREENS                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  🏠 CORE NAVIGATION (5 screens)                                 │
│  ├── 1. Splash Screen                                          │
│  ├── 2. Onboarding Flow (3 sub-screens)                        │
│  ├── 3. Home Dashboard                                         │
│  ├── 4. Settings                                               │
│  └── 5. Profile                                                │
│                                                                 │
│  📞 VOICE SYSTEM (4 screens)                                    │
│  ├── 6. Calling Screen (Wake Word Detected)                    │
│  ├── 7. Active Call Screen                                     │
│  ├── 8. Call Summary Screen                                    │
│  └── 9. Voice History                                          │
│                                                                 │
│  📅 SCHEDULE SYSTEM (4 screens)                                 │
│  ├── 10. Weekly Schedule View                                  │
│  ├── 11. Daily Schedule View                                   │
│  ├── 12. Task Detail / Edit                                    │
│  └── 13. Add Task / Event                                      │
│                                                                 │
│  📚 STUDY SYSTEM (5 screens)                                    │
│  ├── 14. Study Dashboard                                       │
│  ├── 15. Active Study Session (Timer + Focus)                  │
│  ├── 16. Study Analytics                                       │
│  ├── 17. Concept Mastery View                                  │
│  └── 18. Resource Library                                      │
│                                                                 │
│  🎮 THE 4 ZONES (4 screens)                                     │
│  ├── 19. Vitality Zone                                         │
│  ├── 20. Campaign Zone                                         │
│  ├── 21. Radius Zone                                           │
│  └── 22. Zone Hub (Overview of all 4)                          │
│                                                                 │
│  🤖 AGENT SYSTEM (4 screens) - FOR DEMO                         │
│  ├── 23. Agent Dashboard ⭐ NEW                                 │
│  ├── 24. Intervention Feed                                     │
│  ├── 25. Marathon Progress                                     │
│  └── 26. Thought Chain Viewer                                  │
│                                                                 │
│  ⚠️ LIFE EVENTS (2 screens)                                     │
│  ├── 27. Life Events List                                      │
│  └── 28. Add/Edit Life Event                                   │
│                                                                 │
│  💰 SURVIVAL (2 screens)                                        │
│  ├── 29. Financial Overview                                    │
│  └── 30. International Student Info                            │
│                                                                 │
│  🔔 NOTIFICATIONS (1 screen)                                    │
│  └── 31. Notification Center                                   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

### Detailed Screen Specifications

#### 1. Splash Screen
```
- Kaironex logo animation
- Loading state check
- Navigate to: Onboarding (first time) or Home (returning)
```

#### 2. Onboarding Flow (3 sub-screens)
```
2a. Welcome
    - "Welcome to Kaironex"
    - Brief intro animation
    
2b. Profile Setup
    - Name, University, Major
    - Study preferences (morning/night)
    - International student? (yes/no)
    
2c. Connect Services
    - Google Sign-in
    - Google Drive access
    - Notification permissions
    - Microphone permission (for voice)
```

#### 3. Home Dashboard ⭐
```
┌─────────────────────────────────────────┐
│  Good Morning, [Name]!                  │
│  📊 Pressure: 45/100 [=========    ]    │
├─────────────────────────────────────────┤
│                                         │
│  📅 TODAY'S SCHEDULE                    │
│  ┌─────────────────────────────────┐    │
│  │ 9:00  📖 CS 101 Lecture         │    │
│  │ 11:00 📚 Study: Data Structures │    │
│  │ 14:00 🏋️ Gym                    │    │
│  │ 16:00 📖 Physics Lab            │    │
│  └─────────────────────────────────┘    │
│                                         │
│  🎯 ACTIVE QUEST                        │
│  "Midterm Mastery" - 60% complete       │
│  [██████████░░░░░░░░░░]                 │
│                                         │
│  🔔 KAIRO SAYS                          │
│  "You have 2 hours before your next     │
│   class. Perfect time for a quick       │
│   review of binary trees!"              │
│  [Start Study] [Dismiss]                │
│                                         │
├─────────────────────────────────────────┤
│  [🏠] [📅] [🎙️] [📚] [⚙️]              │
└─────────────────────────────────────────┘
```

#### 6. Calling Screen (Wake Word) ⭐ NEW
```
┌─────────────────────────────────────────┐
│                                         │
│                                         │
│           ╭─────────────╮               │
│           │   ◉    ◉    │               │
│           │     ◡       │               │
│           ╰─────────────╯               │
│                                         │
│           Connecting...                 │
│                                         │
│           ┌─────────┐                   │
│           │ ● ● ● ● │  (pulse)          │
│           └─────────┘                   │
│                                         │
│                                         │
│           [Cancel Call]                 │
│                                         │
└─────────────────────────────────────────┘
```

#### 7. Active Call Screen ⭐
```
┌─────────────────────────────────────────┐
│                                         │
│           KAIRO                         │
│           ╭─────────────╮               │
│           │   ◉    ◉    │               │
│           │     ◡       │               │
│           ╰─────────────╯               │
│                                         │
│      ∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿              │
│      (audio waveform)                   │
│                                         │
│  ┌─────────────────────────────────┐    │
│  │ "I have a wedding this weekend" │    │
│  │ (live transcription)            │    │
│  └─────────────────────────────────┘    │
│                                         │
│  ┌─────────────────────────────────┐    │
│  │ 🧠 Processing: life_event       │    │
│  │    → Adjusting schedule...      │    │
│  └─────────────────────────────────┘    │
│                                         │
│     [🔇 Mute]    [📞 End Call]          │
│                                         │
└─────────────────────────────────────────┘
```

#### 23. Agent Dashboard ⭐⭐ NEW (FOR DEMO)
```
┌─────────────────────────────────────────┐
│  🤖 AGENT CONTROL CENTER                │
├─────────────────────────────────────────┤
│                                         │
│  BRAIN STATUS: 🟢 ONLINE                │
│  Mode: REFLEX | Pressure: 45            │
│  Last Active: 2 min ago                 │
│                                         │
│  ┌─────────────────────────────────┐    │
│  │ 📡 LIVE ACTIVITY FEED           │    │
│  │ ─────────────────────────────── │    │
│  │ 12:45:03 → Detected: user_idle  │    │
│  │ 12:45:05 → Action: check_engage │    │
│  │ 12:45:08 → Result: engagement=7 │    │
│  │ 12:45:10 → Decision: no_nudge   │    │
│  │ 12:46:00 → Detected: life_event │    │
│  │ 12:46:02 → Processing: wedding  │    │
│  │ 12:46:05 → Updated: schedule    │    │
│  └─────────────────────────────────┘    │
│                                         │
│  ┌─────────────────────────────────┐    │
│  │ 🧠 CURRENT THOUGHT              │    │
│  │ ID: ts_20260203_0042            │    │
│  │ Confidence: 0.92                │    │
│  │ Chain: 5 thoughts deep          │    │
│  │ [View Full Chain →]             │    │
│  └─────────────────────────────────┘    │
│                                         │
│  ┌─────────────────────────────────┐    │
│  │ 🏃 ACTIVE MARATHONS             │    │
│  │ • "Week Planning" - 80%         │    │
│  │   ETA: 5 min remaining          │    │
│  │ [View Details]                  │    │
│  └─────────────────────────────────┘    │
│                                         │
│  ┌─────────────────────────────────┐    │
│  │ 🔔 RECENT INTERVENTIONS         │    │
│  │ • "Time for a break?" - PENDING │    │
│  │ • "Review notes?" - ACKNOWLEDGED│    │
│  │ [View All →]                    │    │
│  └─────────────────────────────────┘    │
│                                         │
└─────────────────────────────────────────┘
```

#### 19. Vitality Zone
```
┌─────────────────────────────────────────┐
│  ⚡ VITALITY ZONE                       │
├─────────────────────────────────────────┤
│                                         │
│  🔋 BIO-FUEL                            │
│  Energy: ████████░░ 80%                 │
│  Hydration: 💧💧💧💧🔘 Good             │
│  Last Meal: 2h ago                      │
│  Sleep: 7h last night ✓                 │
│                                         │
│  ♻️ REGEN MODE                          │
│  Status: Active Recovery                │
│  Next Break: in 45 min                  │
│  Suggested: 10 min walk                 │
│                                         │
│  📊 RESOURCE MONITOR                    │
│  Weekly Budget: $150 remaining          │
│  Caffeine Today: 2 cups                 │
│  Screen Time: 4h 32m                    │
│                                         │
│  [Log Meal] [Log Sleep] [Quick Break]   │
│                                         │
└─────────────────────────────────────────┘
```

#### 20. Campaign Zone
```
┌─────────────────────────────────────────┐
│  🎯 CAMPAIGN ZONE                       │
├─────────────────────────────────────────┤
│                                         │
│  🌳 SKILL TREE                          │
│       [CS101]                           │
│         ↓                               │
│  [Data Struct] ← You are here           │
│      ↓    ↓                             │
│  [Algo] [DB]                            │
│                                         │
│  📋 QUEST BOARD                         │
│  ┌─────────────────────────────────┐    │
│  │ 🎯 Midterm Mastery              │    │
│  │    Progress: 60% [██████░░░░]   │    │
│  │    Deadline: 12 days            │    │
│  │    Reward: +500 XP, Focus Boost │    │
│  └─────────────────────────────────┘    │
│                                         │
│  🏆 THE ARMORY                          │
│  Unlocked: 📚 Speed Reader              │
│  Unlocked: 🎯 Focus Master              │
│  Next: 🧠 Deep Thinker (80% XP)         │
│                                         │
└─────────────────────────────────────────┘
```

---

## 🔄 DATA FLOW & SYNC

### App Startup Flow
```kotlin
// On app launch, load EVERYTHING from users.studentState_json
suspend fun loadUserState(userId: String): UserState {
    val userDoc = appwrite.databases.getDocument(
        databaseId = DATABASE_ID,
        collectionId = "users",
        documentId = userId
    )
    
    // This ONE call gives us everything!
    val stateJson = userDoc.data["studentState_json"] as String
    return Json.decodeFromString<UserState>(stateJson)
}
```

### Real-Time Sync
```kotlin
// Subscribe to agent_memory for live updates
fun subscribeToAgentUpdates(userId: String, onUpdate: (AgentMemory) -> Unit) {
    appwrite.realtime.subscribe("databases.$DATABASE_ID.collections.agent_memory.documents") {
        if (it.payload["userId"] == userId) {
            onUpdate(parseAgentMemory(it.payload))
        }
    }
}
```

### Voice Context Passing
```kotlin
// After voice conversation ends, pass context to UI
data class VoiceContext(
    val transcript: String,
    val detectedIntent: String,      // e.g., "life_event"
    val extractedData: Map<String, Any>,
    val actionstak: List<String>,    // What AI did
    val uiUpdates: List<UIUpdate>    // What to refresh
)

fun onCallEnded(context: VoiceContext) {
    // 1. Show summary
    showCallSummary(context)
    
    // 2. Apply UI updates
    context.uiUpdates.forEach { update ->
        when (update.type) {
            "schedule" -> refreshScheduleScreen()
            "life_event" -> showLifeEventToast(update.data)
            "intervention" -> showInterventionCard(update.data)
        }
    }
    
    // 3. Sync to Appwrite (already done by brain)
}
```

---

## 🖥️ DEMO DASHBOARD

### Real-Time Agent Monitor (For Demo Video)

```kotlin
@Composable
fun DemoAgentDashboard(viewModel: AgentViewModel) {
    val agentState by viewModel.agentState.collectAsState()
    val activityFeed by viewModel.activityFeed.collectAsState()
    val currentThought by viewModel.currentThought.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Brain Status Header
        BrainStatusHeader(
            status = agentState.status,          // ONLINE/PROCESSING/IDLE
            mode = agentState.reasoningMode,     // REFLEX/DEEP
            pressureIndex = agentState.pressureIndex
        )
        
        // Live Activity Feed (scrolling log)
        LiveActivityFeed(
            activities = activityFeed,
            modifier = Modifier.weight(1f)
        )
        
        // Current Thought Display
        CurrentThoughtCard(thought = currentThought)
        
        // Active Marathons
        ActiveMarathonsList(marathons = agentState.marathons)
        
        // Recent Interventions
        RecentInterventions(interventions = agentState.interventions)
    }
}

// Activity Feed Item
@Composable
fun ActivityFeedItem(activity: AgentActivity) {
    Row(modifier = Modifier.padding(4.dp)) {
        Text(
            text = activity.timestamp.format("HH:mm:ss"),
            style = MaterialTheme.typography.caption,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "→ ${activity.action}: ${activity.detail}",
            style = MaterialTheme.typography.body2
        )
    }
}
```

### What to Show in Demo Video

```
1. WAKE WORD ACTIVATION
   - Say "Hey Kairo"
   - Show calling screen animation
   - Kairo responds with voice
   
2. NATURAL CONVERSATION
   - "I have a wedding this weekend"
   - Show live transcription
   - Show "Processing: life_event" indicator
   - Kairo adjusts schedule
   
3. AGENT DASHBOARD
   - Switch to Agent Dashboard screen
   - Show live activity feed scrolling
   - Show current thought signature
   - Show marathon progress bar
   
4. SCHEDULE UPDATE
   - Navigate to Schedule screen
   - Show the adjusted schedule
   - Highlight: "Friday PM - Free for wedding prep"
   
5. INTERVENTION
   - Wait for nudge
   - Show notification: "Time for a break?"
   - Tap to see intervention detail
```

---

## ✅ IMPLEMENTATION CHECKLIST

### Phase 1: Core Setup
- [ ] Appwrite SDK integration
- [ ] User authentication (Google Sign-in)
- [ ] Load `users.studentState_json` on startup
- [ ] Basic navigation (Bottom nav)

### Phase 2: Essential Screens
- [ ] Home Dashboard
- [ ] Weekly Schedule View
- [ ] Daily Schedule View
- [ ] Settings

### Phase 3: Voice System
- [ ] Wake word detection ("Hey Kairo")
- [ ] Calling Screen UI
- [ ] Gemini Live API WebSocket connection
- [ ] Audio streaming (record + playback)
- [ ] Live transcription display
- [ ] Call Summary Screen

### Phase 4: Agent Integration
- [ ] Agent Dashboard (FOR DEMO)
- [ ] Real-time subscribe to `agent_memory`
- [ ] Activity feed display
- [ ] Current thought display
- [ ] Intervention notifications

### Phase 5: The 4 Zones
- [ ] Zone Hub overview
- [ ] Vitality Zone
- [ ] Campaign Zone
- [ ] Radius Zone

### Phase 6: Polish for Demo
- [ ] Calling screen animations
- [ ] Smooth transitions
- [ ] Toast notifications for context passing
- [ ] Loading states

---

## 🎬 DEMO SCRIPT (2 MINUTES)

```
0:00 - App opens, Home Dashboard
       "This is Kaironex, your AI life operating system"

0:15 - Say "Hey Kairo"
       Calling screen appears, pulse animation
       "Watch as I wake up the AI..."

0:25 - Kairo responds
       "Hey! What's going on?"
       Show active call screen

0:35 - User speaks
       "I have a wedding this weekend and need Friday off"
       Show live transcription

0:45 - Show processing
       "See how it detects this as a life event..."
       Processing indicator shows

0:55 - Kairo responds
       "Got it! I've cleared your Friday afternoon..."
       Show voice response + schedule updating

1:10 - Switch to Agent Dashboard
       "Behind the scenes, here's what the AI is doing..."
       Show live activity feed

1:25 - Show Schedule
       "The schedule is automatically updated"
       Highlight freed time slot

1:40 - Show intervention
       "Later, Kairo will check in..."
       Show notification card

1:55 - Closing
       "Kaironex - Your AI that actually understands life"
```

---

## 📝 NOTES FOR HACKATHON

1. **Google Search Grounding**: Has separate quota - will fallback gracefully
2. **Rate Limits**: 5 RPM, 20 RPD - built-in auto-delay
3. **Recovery**: All state persisted to Appwrite, can resume after crash
4. **Reflex in App**: Fast responses from local SDK, complex tasks go to cloud
5. **Only 2 Models**: `gemini-3-flash-preview` + `gemini-2.5-audio` - NO OTHERS!

---

*Last Updated: February 3, 2026*
*Kaironex Brain Version: 2.0*
