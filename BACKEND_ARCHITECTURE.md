# KAIRONEX DEEP BRAIN — Backend Architecture

> **Kaironex** manages every sector of a student's life — study, survival, career, and cultural adaptation — through a multi-agent AI brain that runs continuously, adapts daily, and never stops learning about the student it serves.

---

## Table of Contents

1. [System Overview](#system-overview)
2. [AI Models & Reasoning Architecture](#ai-models--reasoning-architecture)
3. [The Continuous Marathon Loop](#the-continuous-marathon-loop)
4. [Entry Point & Routing](#entry-point--routing)
5. [Agent System — Full Capabilities](#agent-system--full-capabilities)
   - [Study Agent — Academic Strategist](#study-agent--academic-strategist)
   - [Vitality Brain — Life Logistics Engine](#vitality-brain--life-logistics-engine)
   - [Campaign Agent — Career Strategist](#campaign-agent--career-strategist)
   - [Radius Agent — Cultural Survival Engine](#radius-agent--cultural-survival-engine)
   - [Supervisor Agent — Meta-Controller](#supervisor-agent--meta-controller)
6. [Two-Layer Hierarchical Scheduling](#two-layer-hierarchical-scheduling)
7. [Bicameral Engine — Dual-Process Reasoning](#bicameral-engine--dual-process-reasoning)
8. [Cross-Agent Communication](#cross-agent-communication)
9. [Live API Voice Integration](#live-api-voice-integration)
10. [Database Schema — 20 Collections](#database-schema--20-collections)
11. [Thought Signature System](#thought-signature-system)
12. [Project Structure](#project-structure)
13. [Test Suite](#test-suite)

---

## System Overview

Kaironex is a **multi-agent AI system** that manages the complete life of a university student. It is not a chatbot. It is not a planner. It is an autonomous brain that wakes up every morning, makes decisions, intervenes when things go wrong, and runs continuously from the first day of semester until the last.

```
┌─────────────────────────────────────────────────────────────────┐
│                      KAIRONEX ARCHITECTURE                       │
│                                                                   │
│  ┌───────────────────┐       ┌────────────────────────────────┐  │
│  │   MOBILE APP       │       │   DEEP BRAIN (this backend)    │  │
│  │   (KMP/Compose)    │◄─────►│   Appwrite Functions           │  │
│  │                     │       │                                 │  │
│  │   Reflex Agent      │       │   Study Agent                   │  │
│  │   gemini-3-flash    │       │   Vitality Brain                │  │
│  │   MINIMAL thinking  │       │   Campaign Agent                │  │
│  │                     │       │   Radius Agent                  │  │
│  │   Voice Engine      │       │   Supervisor Agent              │  │
│  │   gemini-2.5-flash  │       │   gemini-3-flash-preview        │  │
│  │   native-audio      │       │   HIGH thinking (budget: 8192)  │  │
│  │   Live API          │       │                                 │  │
│  └───────────────────┘       └────────────────────────────────┘  │
│             │                             │                        │
│             └─────────────┬───────────────┘                        │
│                           ▼                                        │
│                  ┌─────────────────┐                               │
│                  │   Appwrite DB    │                               │
│                  │  20 Collections  │                               │
│                  │   TablesDB       │                               │
│                  └─────────────────┘                               │
└─────────────────────────────────────────────────────────────────┘
```

**Key Design Principles:**
- **Deep Brain (backend)** handles all complex reasoning — scheduling, planning, intervention, marathon operations
- **Reflex Agent (mobile app)** handles instant user-facing responses with MINIMAL thinking
- **Voice Engine (mobile app)** uses Gemini Live API for real-time voice interaction — mock interviews, cultural practice, tutoring
- **Appwrite Database** is the single source of truth — agents read/write state, the app reads state changes reactively

---

## AI Models & Reasoning Architecture

### Models Used

| Model | Where | Purpose | Thinking Level |
|-------|-------|---------|----------------|
| `gemini-3-flash-preview` | Backend (Deep Brain) | Complex reasoning, planning, scheduling, interventions | HIGH (thinking_budget: 8192) |
| `gemini-3-flash-preview` | Mobile (Reflex Agent) | Quick responses, UI interactions, lightweight decisions | MINIMAL |
| `gemini-2.5-flash-native-audio-preview-12-2025` | Mobile (Voice Engine) | Real-time voice via Live API WebSocket — mock interviews, cultural practice, tutoring | Native audio streaming |

### Thinking Levels

```python
THINKING_LEVEL_REFLEX = 'MINIMAL'   # App's Reflex Agent — instant responses
THINKING_LEVEL_BALANCED = 'MEDIUM'  # Balanced reasoning
THINKING_LEVEL_DEEP = 'HIGH'        # Backend's Deep Brain — planning & strategy
```

The backend NEVER does reflex work. The app NEVER does deep planning. Each model instance has a single responsibility.

---

## The Continuous Marathon Loop

Kaironex runs **continuously**. Not for 72 hours. Not as a one-shot. It is always on, always watching, always adapting.

```
                    ┌──────────────────────┐
                    │   STUDENT SIGNS UP    │
                    │   (Day Zero)          │
                    └──────────┬───────────┘
                               ▼
                    ┌──────────────────────┐
                    │  CALIBRATION PHASE    │
                    │  • Profile extraction │
                    │  • Constitution build │
                    │  • Financial setup    │
                    │  • Cultural profile   │
                    │  • Career calibration │
                    │  • Semester schedule  │
                    └──────────┬───────────┘
                               ▼
          ┌────────────────────────────────────────┐
          │                                        │
          │         THE CONTINUOUS LOOP             │
          │                                        │
          │    ┌───────────────────────┐            │
          │    │  CRON: MORNING        │            │
          │    │  • Budget check       │            │
          │    │  • Meal planning      │◄───────┐   │
          │    │  • Cultural lesson    │        │   │
          │    │  • Daily briefing     │        │   │
          │    └──────────┬────────────┘        │   │
          │               ▼                     │   │
          │    ┌───────────────────────┐        │   │
          │    │  STUDENT LIVES DAY    │        │   │
          │    │  • Study sessions     │        │   │
          │    │  • Location changes   │        │   │
          │    │  • Meals logged       │        │   │
          │    │  • Focus tracked      │        │   │
          │    │  • Events happen      │        │   │
          │    └──────────┬────────────┘        │   │
          │               ▼                     │   │
          │    ┌───────────────────────┐        │   │
          │    │  AGENTS RESPOND       │        │   │
          │    │  • Interventions      │        │   │
          │    │  • Drift detection    │        │   │
          │    │  • Schedule adapt     │        │   │
          │    │  • Emergency help     │        │   │
          │    └──────────┬────────────┘        │   │
          │               ▼                     │   │
          │    ┌───────────────────────┐        │   │
          │    │  CRON: EVENING        │        │   │
          │    │  • Day summary        │        │   │
          │    │  • Savings allocation  │        │   │
          │    │  • Tomorrow prep      │        │   │
          │    │  • Weekly digest      │        │   │
          │    └──────────┬────────────┘        │   │
          │               ▼                     │   │
          │    ┌───────────────────────┐        │   │
          │    │  SUPERVISOR CHECK     │        │   │
          │    │  • Cross-domain health│        │   │
          │    │  • Pressure level     │        │   │
          │    │  • Drift detection    │        │   │
          │    │  • Agent coordination │        │   │
          │    └──────────┬────────────┘        │   │
          │               │                     │   │
          │               └─────────────────────┘   │
          │         (REPEAT EVERY DAY FOREVER)       │
          │                                          │
          │    ┌─────────────────────────────┐       │
          │    │  MONTHLY: NEW MONTH BEGINS  │       │
          │    │  • Next month daily schedule │       │
          │    │  • Re-evaluate semester plan │       │
          │    │  • Adaptation from last month│       │
          │    └─────────────────────────────┘       │
          │                                          │
          └────────────────────────────────────────┘
```

Every cycle feeds into the next. Morning routine produces context for the day. The day produces events for agents. Agents produce interventions. Evening summarizes. Supervisor checks cross-domain health. Next morning starts again. Monthly, the semester plan is re-evaluated and next month's daily schedule is generated based on what happened.

This is not a pipeline. It is a **continuous feedback loop** with no termination point until the semester ends.

---

## Entry Point & Routing

**File:** `src/main.py` (~230 lines)

The backend runs as an **Appwrite Function** — no HTTP server, no FastAPI. Appwrite triggers execution based on:

### Trigger Types

| Trigger | Source | Routes To |
|---------|--------|-----------|
| Database: `users` collection | Profile created/updated | Campaign Agent (if resources exist) |
| Database: `resources` collection | File uploaded | Study Agent (resource ingestion) |
| Database: `study_logs` collection | Study session event | Study Agent |
| Database: `vitality_state` collection | Vitality event | Vitality Brain |
| Database: `radius_state` collection | Radius event | Radius Agent |
| Database: `schedule`/`campaign` | Schedule/campaign change | Campaign Agent |
| CRON | Scheduled timer | Supervisor Agent |
| HTTP: `POST /brain/deep` | App → backend deep request | Routes to specified agent |
| HTTP: `POST /campaign` | Direct campaign request | Campaign Agent |
| HTTP: `GET /state` | State sync request | Returns user state |
| HTTP: `GET /health` | Health check | Returns status |

### Routing Flow

```
Appwrite Event → main() → _async_main()
                              │
                              ├─ Parse trigger_event (environment variable)
                              ├─ Parse request_path (HTTP path)
                              ├─ Parse payload (JSON body)
                              │
                              ├─ DB Event: users → Campaign Agent
                              ├─ DB Event: study_logs → Study Agent
                              ├─ DB Event: vitality_state → Vitality Brain
                              ├─ DB Event: radius_state → Radius Agent
                              ├─ HTTP: /brain/deep → Routes by agent param
                              ├─ HTTP: /campaign → Campaign Agent
                              ├─ CRON → Supervisor Agent
                              └─ Console Test → Routes by event type
```

---

## Agent System — Full Capabilities

### Study Agent — Academic Strategist

**File:** `src/agents/study_agent.py` (1,263 lines)  
**Mode:** MARATHON (DEEP + checkpoints)  
**Thinking:** HIGH (thinking_budget: 8192)

The Study Agent owns the student's entire academic timeline. It doesn't just make schedules — it extracts the student's **Constitution** (immutable life constraints), builds a **semester-wide strategy**, generates **daily tactical plans**, and prepares **Just-In-Time content** for every task.

#### Capabilities

| Handler | What It Does |
|---------|--------------|
| `schedule_request` | **Full hierarchical planning.** Extracts UserConstitution (job hours, prayer blocks, energy pattern, learning pace, weakness subjects, academic goals). Builds semester timeline. Generates TWO layers in a SINGLE API call: Layer 1 = monthly strategy for entire semester, Layer 2 = daily tasks for current month. Calculates study hours from CGPA target. Respects all blocked times. Triggers JIT content preparation after schedule generation. |
| `content_request` | **On-demand JIT content.** Returns pre-generated study content by taskId or date. Content modes: `deep_dive` (full learning), `travel` (mobile-friendly), `cram` (pre-exam), `practice` (exercises). |
| `resource_ingestion` | **Resource processing.** Accepts uploaded PDFs, documents, and links. Passes to Gemini for AI analysis. Extracts key concepts, summaries, and study recommendations. Creates intervention with analysis results. |
| Focus tracking | When `status=IN_PROGRESS` and `focus_score < 40`, generates a focus intervention. Above 40, logs normally. |
| `REQUEST_UNLOCK` (quiz) | Generates quiz questions to gate topic progression. Student must demonstrate understanding before moving to next topic. |
| State caching | Every event updates the agent memory state cache with study session data, actions taken, and timestamps. |
| Heartbeat logging | Every study session event is logged for drift detection and progress tracking. |

#### UserConstitution (extracted once, immutable)

```
Job Schedule → blocked work hours
Prayer Blocks → blocked prayer times
Health Routine → gym/exercise windows
Energy Pattern → night owl or early bird → peak study hours
Learning Pace → affects task density
Weakness Defense → subjects needing more time
Academic Goal → target CGPA → required study hours calculation
```

#### Two-Layer Planning (Single API Call)

```
Layer 1 — STRATEGY (Semester-wide):
  Month 1: [subjects, topics, weights, exam prep windows]
  Month 2: [subjects, topics, weights, exam prep windows]
  ... (all months until semester end)

Layer 2 — TACTICAL (Current month daily):
  Day 1: [task1{subject, topics, difficulty, content_mode, objectives}, task2, ...]
  Day 2: [task1, task2, ...]
  ... (all days in current month)
```

**Smart date handling:** If a student signs up mid-semester, planning starts from TODAY, not from semester start.

#### Just-In-Time Content via ProactiveContentEngine

**File:** `src/tools/proactive_content_engine.py` (839 lines)

After schedule generation, the engine pre-generates study content for upcoming tasks. Content modes:

- **deep_dive** — Comprehensive learning material with explanations, examples, and practice
- **travel** — Mobile-optimized bite-sized content for commuting
- **cram** — Condensed review material for pre-exam periods
- **practice** — Problem sets and exercises for skill building

---

### Vitality Brain — Life Logistics Engine

**File:** `src/agents/vitality_brain_v2.py` (1,650 lines)  
**Style:** Functional dispatch (not class-based)  
**Mode:** Per-handler basis

The Vitality Brain handles everything that isn't academic — finances, food, sleep, energy, wellness. It operates on a **DEFCON system** for financial urgency and a **decision matrix** for daily choices. It runs autonomous morning and evening routines.

#### Capabilities

| Handler | What It Does |
|---------|--------------|
| `one_shot_setup` | **Day Zero financial calibration.** Parses income sources, bills, rent, subscription costs. Calculates daily budget runway. Sets DEFCON level (1-5). Creates survival state document. |
| `fridge_scan` | **Gemini Vision analysis.** Student photographs fridge contents → Gemini identifies ingredients → generates meal suggestions with estimated costs within DEFCON budget. |
| `proactive_meal_plan` | **AI-generated meal options.** Produces 3 meal options per slot (breakfast/lunch/dinner) that fit the daily budget. Considers DEFCON level, available ingredients, and time constraints. |
| `select_meal_option` | **Meal selection.** Student picks from generated options. Updates daily spending tracker. |
| `get_todays_meals` | **Current meal plan status.** Returns today's planned and consumed meals with remaining budget. |
| `daily_budget_check` | **Morning financial ritual.** Calculates today's available budget. Shows runway (days until broke). Generates spending guidance based on DEFCON level. |
| `defcon_update` | **Financial urgency recalculation.** Recalculates DEFCON after income received, unexpected expense, or budget change. |
| `cross_agent_sync` | **Multi-agent state sync.** Pushes vitality state to other agents so they can adjust (e.g., Study Agent knows student is under financial stress). |
| `marathon_morning_routine` | **AUTONOMOUS morning routine.** Runs without user input: budget check → meal planning → energy assessment → daily briefing. Produces a complete morning report. |
| `auto_end_of_day` | **AUTONOMOUS evening routine.** Day summary → savings auto-allocation (if surplus) → tomorrow preparation → writes intervention. |
| `emergency_fund_withdraw` | **Emergency fund access.** Validates amount, checks fund balance, processes withdrawal. |
| `emergency_fund_deposit` | **Emergency fund deposit.** Adds to emergency savings. |
| `savings_status` | **Fund overview.** Emergency fund balance, savings rate, projected safety net. |
| `decision_matrix` | **Cook vs Order decision engine.** Evaluates along 3 axes: Time available × Money available × Energy level. Recommends optimal choice. |
| `shopping_alert` | **Grocery trigger.** Detects low supplies, generates shopping list within budget. |
| `unlock_reward` | **Victory feast reward.** When student hits a major milestone, unlocks a meal upgrade as celebration. |
| `sleep_log` | Logs sleep hours and quality. |
| `activity_log` | Logs physical activity and duration. |
| `meal_log` | Logs consumed meals with cost. |
| `energy_check` | Current energy level assessment with Gemini analysis. |
| `regen_request` | Activates recovery protocol (rest recommendation). |
| `resource_update` | Logs resource consumption (water, food supplies, etc.). |

#### DEFCON System

```
DEFCON 1 — CRITICAL:  < 3 days of runway. Emergency mode.
DEFCON 2 — SEVERE:    3-7 days of runway. Strict budgeting.
DEFCON 3 — MODERATE:  1-3 weeks of runway. Careful spending.
DEFCON 4 — STABLE:    3-6 weeks of runway. Normal operations.
DEFCON 5 — SECURE:    6+ weeks of runway. Relaxed constraints.
```

#### Safety: Medical Boundary

The Vitality Brain has a hard-coded list of **forbidden medical terms** in config. It never provides health advice, diagnoses, or medical recommendations. It is a logistics engine, not a health advisor.

---

### Campaign Agent — Career Strategist

**File:** `src/agents/campaign_agent.py` (950 lines)  
**Mode:** DEEP  
**Thinking:** HIGH (thinking_tokens: 46,384)

The Campaign Agent manages the student's entire career trajectory — from first resume to mock interview to job offer. It gamifies career progression with Skill Trees, Quest Boards, and an Armory.

#### Capabilities

| Handler | What It Does |
|---------|--------------|
| `campaign_calibration` | **Full career initialization.** Generates: (1) **Skill Tree** — 5-7 node dependency graph of skills needed for target role, (2) **Quest Board** — 3 actionable quests with deadlines and XP rewards, (3) **Armory** — tools, templates, and resources unlocked based on current skill level. |
| `analyze_resume` | **ATS Resume Scoring (0-100).** Keyword matching against job description. Gap analysis. Improvement suggestions. Supports PDF uploads via Gemini multimodal vision. |
| `generate_resume` | **Tailored resume generation.** Takes job description + student's projects/experience → generates an ATS-optimized resume targeted to that specific role. |
| `design_interview` | **Interview preparation system.** Creates interview plan with question bank categorized by type (behavioral, technical, system design). Generates a **Gemini Live API system prompt** so the Voice Engine on the mobile app can conduct a real-time voice mock interview. |
| `simulacrum_start` | **Mock interview launch.** Starts a live mock interview session with configurable difficulty: `easy` (friendly), `medium` (standard), `hard` (challenging), `brutal` (adversarial). Each level has a different interviewer persona. |
| `simulacrum_response` | **Ongoing mock interview.** Processes student's answer, evaluates quality, asks follow-up questions. Maintains conversation state throughout the interview. |
| `new_goal` | **Goal decomposition.** Takes a career goal → breaks into 5-15 actionable quests with dependencies, deadlines, and verification criteria. |
| `schedule_update` | Calendar change handler. Adjusts campaign quests when student's schedule changes. |
| `quest_complete` | Quest completion tracking. Awards XP, updates Skill Tree, may unlock new Armory items. |
| `marathon_step` | Long-running career marathon progress checkpoint. |
| `armory_unlock` | Skill/tool unlock tracking. Records new capabilities gained. |

#### Career Gamification

```
SKILL TREE:
  Python ──► Backend Dev ──► System Design
     │                           │
     ▼                           ▼
  Data Structures ──► Algorithms ──► Interview Ready

QUEST BOARD:
  [Quest 1] Build a REST API (200 XP, 3 days)
  [Quest 2] Solve 10 LeetCode mediums (300 XP, 7 days)
  [Quest 3] Deploy to AWS (250 XP, 5 days)

ARMORY:
  [Unlocked] Resume Template v2
  [Unlocked] STAR Method Guide
  [Locked] System Design Template (needs: System Design skill)
```

---

### Radius Agent — Cultural Survival Engine

**File:** `src/agents/radius_agent.py` (868 lines)  
**Mode:** MARATHON (HYBRID reasoning)

The Radius Agent serves international students navigating a foreign culture. It provides micro-lessons on local culture, generates voice practice scenarios for the Gemini Live API, tracks location for context-aware mode switching, and maintains a network of safe spaces.

#### Capabilities

| Handler | What It Does |
|---------|--------------|
| `cultural_setup` | **International student profile.** Home country, target language, cultural gaps, comfort areas, arrival status. |
| `daily_lesson` | **AI-generated micro-lesson.** Daily cultural lesson on local customs, phrases, social norms. Idempotent — second call returns existing lesson. |
| `complete_lesson` | **Lesson completion with streak tracking.** Updates streak counter, cultural wins count. |
| `live_agent_prompt` | **Gemini Live API teaching prompt.** Generates a system prompt for the Voice Engine to conduct a real-time voice lesson on a cultural topic. The student speaks to Gemini in real-time for language practice. |
| `cultural_scenario` | **Interactive scenarios for Live API.** Generates real-world scenarios (ordering food, talking to a professor, navigating public transport) that the Voice Engine runs as voice role-play. |
| `location_change` | **Auto-mode detection.** Detects student's location → sets appropriate mode. Library → DEEP_FOCUS. Campus → CAMPUS. Home → REST. Gym → WORKOUT. Café → LIGHT_FOCUS. Restaurant → SOCIAL. Commute → MOBILE. |
| `local_scan` | **Area analysis.** Scans current location for student resources, safety info, nearby amenities. |
| `safehouse_add` | **Safe space network.** Student registers safe spaces (friend's apartment, cultural center, library). |
| `safehouse_check` | **Safe space query.** Lists all registered safe spaces with proximity. |
| `emergency_cultural` | **Urgent cultural help.** Immediate assistance for: visa issues, discrimination incidents, cultural isolation, communication emergencies. Stores intervention with high urgency. |
| `marathon_cultural_morning` | **AUTONOMOUS morning routine.** Runs without input: daily lesson generation → Live API voice prompt preparation → cultural briefing. |
| `mode_request` | **Manual mode activation.** Student can manually override auto-detected mode. |

#### Location Modes

```python
LOCATION_MODE_MAP = {
    "library":    "DEEP_FOCUS",     # Silence, deep study
    "study":      "DEEP_FOCUS",     # Study room
    "classroom":  "CAMPUS",         # Lecture mode
    "campus":     "CAMPUS",         # General campus
    "gym":        "WORKOUT",        # Physical activity
    "home":       "REST",           # Recovery
    "dorm":       "REST",           # Recovery
    "cafe":       "LIGHT_FOCUS",    # Light work
    "restaurant": "SOCIAL",         # Social interaction
    "commute":    "MOBILE",         # Transit
    "transit":    "MOBILE",         # Bus/train
}
```

Each mode adjusts how ALL agents behave — Study Agent reduces notification intensity in REST mode, Campaign Agent pauses quests in WORKOUT mode, etc.

---

### Supervisor Agent — Meta-Controller

**File:** `src/agents/supervisor_agent.py` (730 lines)  
**Trigger:** CRON schedule

The Supervisor doesn't serve the student directly. It monitors all other agents, detects drift across domains, assesses pressure, and coordinates cross-agent actions. It is the quality control layer.

#### Capabilities

| Handler | What It Does |
|---------|--------------|
| `health_check` | **Cross-domain health scoring.** Scores each domain: Study (session consistency, quiz pass rate), Vitality (budget adherence, meal regularity), Campaign (quest completion rate), Radius (lesson streaks, mode usage). Returns aggregate health. |
| `drift_detection` | **Multi-dimensional drift analysis.** Detects: Schedule drift (missing study sessions), Academic drift (declining quiz scores), Wellness drift (irregular sleep/meals), Social drift (increasing isolation), Career drift (stalled quests). |
| `pressure_assessment` | **Pressure level classification.** ZEN → NORMAL → ELEVATED → HIGH → CRITICAL. Each level triggers different agent behaviors — CRITICAL activates survival protocol across all agents. |
| `agent_coordination` | **Cross-agent task delegation.** When one agent detects something that affects another domain (e.g., financial crisis affects study time), Supervisor coordinates the response. |
| `weekly_digest` | **Weekly summary generation.** Comprehensive report: what was accomplished, what drifted, pressure trend, next week's priorities. |
| `delegate` | **Task delegation.** Routes specific tasks to the most appropriate agent. |

#### Pressure Levels

```
ZEN      — Everything optimal. All agents in normal mode.
NORMAL   — Minor deviations. Standard interventions.
ELEVATED — Multiple small issues accumulating. Increased monitoring.
HIGH     — Significant problems in 2+ domains. Agents shift to protective mode.
CRITICAL — Survival mode. Non-essential activities paused. Focus on immediate needs.
```

---

## Two-Layer Hierarchical Scheduling

The scheduling system is the core of how Kaironex manages academic life. It operates on two layers, generated in a **single API call** for efficiency.

```
STUDENT SIGNS UP
       │
       ▼
┌─────────────────────────────┐
│  CONSTITUTION EXTRACTION     │
│  Job schedule, prayer times, │
│  energy pattern (night owl/  │
│  early bird), learning pace, │
│  weakness subjects, target   │
│  CGPA, health routine        │
└──────────┬──────────────────┘
           │
           ▼
┌─────────────────────────────┐
│  TIMELINE EXTRACTION         │
│  Semester start → end date   │
│  Exam dates, holidays        │
│  Current date (if mid-sem)   │
└──────────┬──────────────────┘
           │
           ▼
┌─────────────────────────────────────────────────┐
│  SINGLE API CALL TO GEMINI 3 FLASH               │
│  (HIGH thinking, budget: 8192)                    │
│                                                    │
│  INPUT:                                            │
│    • UserConstitution (immutable constraints)      │
│    • Timeline (semester dates)                     │
│    • Subjects + topics per subject                 │
│    • Target CGPA → required study hours            │
│    • Current date                                  │
│                                                    │
│  OUTPUT (both layers at once):                     │
│                                                    │
│  LAYER 1 — STRATEGY (all months):                 │
│    Month 1: {subjects, weights, exam_prep}         │
│    Month 2: {subjects, weights, exam_prep}         │
│    ...                                             │
│    Month N: {revision, finals}                     │
│                                                    │
│  LAYER 2 — TACTICAL (current month):              │
│    Day 1: [{subject, topics[4-phase],              │
│             content_mode, difficulty,               │
│             learning_objectives,                    │
│             resource_hints, verification}]          │
│    Day 2: [...]                                    │
│    ...                                             │
│    Day 30: [...]                                   │
│                                                    │
└──────────────────┬──────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────┐
│  JIT CONTENT PREPARATION                           │
│  ProactiveContentEngine pre-generates               │
│  study material for upcoming tasks                  │
│  Content modes: deep_dive, travel, cram, practice   │
└──────────────────┬──────────────────────────────┘
                   │
                   ▼
         STUDENT STARTS STUDYING
                   │
        ┌──────────┴──────────┐
        │  END OF MONTH       │
        │  Next month's daily │
        │  schedule generated │
        │  based on what      │
        │  happened this month│
        └──────────┬──────────┘
                   │
                   ▼
      (REPEAT MONTHLY UNTIL SEMESTER ENDS)
```

### Task Format

Each task in the daily schedule contains:

```json
{
  "subject": "Data Structures",
  "topics": ["Arrays", "Linked Lists", "BST Traversal", "Practice Problems"],
  "content_mode": "deep_dive",
  "difficulty": 3,
  "learning_objectives": ["Implement BFS/DFS", "Analyze time complexity"],
  "resource_hints": ["Textbook Ch. 5", "LeetCode Easy #1-10"],
  "verification": "Quiz on tree traversal",
  "metadata_json": { "estimated_minutes": 90, "energy_required": "high" }
}
```

### Adaptation

The system adapts continuously:
- **Daily:** Focus drops trigger interventions. Quizzes gate topic progression.
- **Weekly:** Supervisor detects drift and adjusts.
- **Monthly:** New daily schedule generated from updated semester strategy.
- **Event-driven:** Life events (job change, illness, family emergency) trigger immediate re-planning via Life Event Handler.

---

## Bicameral Engine — Dual-Process Reasoning

**File:** `src/core/bicameral_engine.py` (564 lines)

The Bicameral Engine is the reasoning core. Every agent call passes through it. It implements **dual-process theory** — fast intuitive responses (System 1) and slow deliberate reasoning (System 2).

### Reasoning Modes

| Mode | Thinking | Use Case |
|------|----------|----------|
| `REFLEX` | MINIMAL | Quick pattern-matching responses |
| `DEEP` | HIGH (budget: 8192) | Complex planning, scheduling, analysis |
| `HYBRID` | MINIMAL → HIGH | Try reflex first. If confidence < 0.7, escalate to deep thinking. |
| `MARATHON` | HIGH + checkpoints | Long-running operations with intermediate saves |

### Structured Thinking Tags

Every DEEP reasoning call uses structured tags:
```xml
<analyze>Problem decomposition and context assessment</analyze>
<strategy>Approach selection and tradeoff analysis</strategy>
<decision>Final choice with reasoning</decision>
<action>Concrete output to execute</action>
```

### Thought Signatures

Every reasoning output generates a **ThoughtSignature** — a cryptographic chain of:
- `thought_id`: Unique identifier
- `context_hash`: SHA-256 of input context
- `reasoning_trace`: Steps taken
- `confidence`: 0.0 - 1.0 confidence score
- `tool_calls`: Tools invoked
- `parent_signature`: Links to previous thought in chain

This creates an **auditable chain of every decision** Kaironex makes for every student.

---

## Cross-Agent Communication

Agents don't operate in silos. They communicate via:

### Event Bus (`src/core/event_bus.py`)
Publish-subscribe event system for real-time cross-agent notifications.

### Cross-Agent Integration (`src/core/cross_agent_integration.py`)
Direct state sharing between agents. When Vitality detects financial crisis, it pushes state to Study Agent which reduces study load intensity. When Campaign detects an upcoming interview, it pushes to Study Agent to free up preparation time.

### Supervisor Coordination
The Supervisor monitors all agent states via CRON and coordinates responses when multiple domains are affected simultaneously.

### State Machine (`src/core/state_machine.py`)
Shared state context (StateContext) that all agents read/write. Ensures consistent view of the student's current situation.

### Intervention Engine (`src/core/intervention_engine.py`)
Creates interventions (notifications, schedule changes, emergency protocols) that are visible to all agents and the mobile app.

---

## Live API Voice Integration

The mobile app connects to `gemini-2.5-flash-native-audio-preview-12-2025` via WebSocket (Gemini Live API) for real-time voice interaction. The **backend generates the system prompts** that control what the Voice Engine does.

### How It Works

```
BACKEND                          MOBILE APP
───────                          ──────────
Campaign Agent                   Voice Engine
  design_interview() ──────►     (Live API WebSocket)
  Generates system prompt         gemini-2.5-flash-native-audio
  with interviewer persona,      Student speaks → AI responds
  question bank, difficulty       in real-time voice
  level, evaluation criteria

Radius Agent                     Voice Engine
  live_agent_prompt() ──────►    (Live API WebSocket)
  Generates teaching prompt       Cultural practice via voice
  with topic, language level,     Student practices ordering food,
  correction style                talking to professors, etc.

  cultural_scenario() ──────►    Voice Engine
  Generates scenario prompt       Role-play scenarios via voice
  with setting, characters,       Student navigates real-world
  objectives, success criteria    situations with AI partner

Study Agent                      Voice Engine
  (content prep) ──────────►     (Live API WebSocket)
  Topic material + quiz data      AI tutors student on topic
                                  via real-time conversation
```

The backend is the **brain**. The Voice Engine is the **mouth**. The backend decides WHAT to teach/practice/ask. The Voice Engine delivers it in real-time voice.

---

## Database Schema — 20 Collections

All data is stored in **Appwrite TablesDB**. The mobile app reads state reactively — when an agent writes to a collection, the app receives the update.

| Collection | Purpose |
|------------|---------|
| `users` | Student profiles, settings, studentState_json |
| `schedule` | Daily and monthly schedule data |
| `study_logs` | Study session events (focus, duration, topic) |
| `daily_snapshots` | End-of-day summaries |
| `interventions` | Agent-generated interventions (notifications, changes) |
| `agent_memory` | Agent state cache (per-user, per-agent) |
| `policy_episodes` | Learning episodes for policy improvement |
| `thought_signatures` | Auditable chain of every AI decision |
| `vitality_state` | Financial state, DEFCON, meal plans |
| `campaign_state` | Skill tree, quest board, armory, interview data |
| `radius_state` | Cultural profile, lessons, safehouses, location modes |
| `resources` | Uploaded files and processed documents |
| `marathon_sessions` | Long-running operation progress |
| `monthly_plans` | Semester strategy (monthly breakdown) |
| `student_profiles` | Extracted UserConstitution |
| `life_events` | Major life events (job change, illness, etc.) |
| `schedule_changes` | Schedule modification history |
| `financial_state` | Budget, runway, emergency fund, savings |
| `international_info` | Country, language, visa status, cultural data |
| `concept_mastery` | Per-concept mastery tracking for quiz gating |

---

## Thought Signature System

Every AI decision creates a ThoughtSignature stored in the `thought_signatures` collection:

```json
{
  "thought_id": "thought_a1b2c3d4e5f6",
  "timestamp": "2026-02-09T06:00:00.000Z",
  "user_id": "user_001",
  "agent": "study_agent",
  "signature": {
    "context_hash": "sha256:abc123...",
    "reasoning_trace": [
      "Student focus dropped to 25",
      "Below threshold of 40",
      "Generating focus intervention",
      "Micro-break + topic switch recommended"
    ],
    "confidence": 0.87,
    "tool_calls": ["generate_intervention"]
  },
  "action_output": "Take a 5-minute break, then switch to practice problems",
  "parent_signature": "thought_x9y8z7w6v5u4"
}
```

This creates a **complete, auditable chain** of every decision Kaironex makes. Every thought links to its parent. Every action can be traced back through the reasoning chain.

---

## Project Structure

```
Kaironex-Brain/
├── src/
│   ├── main.py                          # Appwrite Functions entry point
│   ├── config.py                        # Centralized configuration
│   ├── orchestrator.py                  # Agent orchestration
│   ├── agents/
│   │   ├── study_agent.py               # Academic strategist (1,263 lines)
│   │   ├── vitality_brain_v2.py         # Life logistics engine (1,650 lines)
│   │   ├── campaign_agent.py            # Career strategist (950 lines)
│   │   ├── radius_agent.py              # Cultural survival engine (868 lines)
│   │   ├── supervisor_agent.py          # Meta-controller (730 lines)
│   │   ├── main_brain.py               # Supervisor runner
│   │   ├── study_brain.py              # Study event processor
│   │   ├── campaign_brain.py           # Campaign event processor
│   │   ├── radius_brain.py             # Radius event processor
│   │   ├── vitality_agent_v2.py        # Vitality adapter
│   │   └── base_agent.py              # Base agent class
│   ├── core/
│   │   ├── bicameral_engine.py          # Dual-process reasoning (564 lines)
│   │   ├── cross_agent_integration.py   # Agent communication
│   │   ├── deep_brain.py              # Deep reasoning interface
│   │   ├── event_bus.py               # Pub/sub event system
│   │   ├── intervention_engine.py     # Intervention creation
│   │   ├── life_event_handler.py      # Major life event processing
│   │   ├── marathon_runner.py         # Long-running operation manager
│   │   ├── schedule_validator.py      # Schedule integrity checks
│   │   ├── state_machine.py           # Shared state context
│   │   ├── student_growth_engine.py   # Growth tracking
│   │   ├── survival_protocol.py       # Financial survival system
│   │   └── thought_manager.py         # Thought chain management
│   ├── tools/
│   │   ├── proactive_content_engine.py  # JIT content generation (839 lines)
│   │   ├── content_delivery.py        # Content delivery
│   │   ├── deep_research.py           # Research capabilities
│   │   ├── financial_survival.py      # Financial calculations
│   │   ├── international_student.py   # International student tools
│   │   ├── local_radius.py           # Location-based tools
│   │   ├── mastery_evaluator.py      # Quiz/mastery evaluation
│   │   ├── presence_engagement.py    # Engagement tracking
│   │   ├── search_tools.py           # Search capabilities
│   │   ├── study_planner.py          # Planning utilities
│   │   ├── tool_declarations.py      # Tool definitions
│   │   └── tool_executor.py          # Tool execution
│   └── utils/
│       ├── db_helper.py               # Appwrite database helper
│       ├── gemini_client.py           # Gemini API client
│       └── rate_limited_client.py     # Rate limiting
├── tests/
│   ├── test_all_agents.py             # Master test runner (149 tests)
│   ├── test_campaign_brain.py         # Campaign agent tests (27 tests)
│   ├── test_vitality_brain.py         # Vitality brain tests (39 tests)
│   ├── test_study_brain.py            # Study brain tests (26 tests)
│   ├── test_radius_brain.py           # Radius brain tests (57 tests)
│   ├── test_survival_protocol.py      # Survival protocol tests
│   ├── test_mocks.py                 # Shared mock classes
│   ├── test_endpoint.py              # Endpoint integration test
│   ├── test_god_mode.py              # God mode test
│   ├── test_resume_local.py          # Resume upload test
│   └── test_schedule_generation.py   # Schedule generation test
├── docs/
│   ├── ARCHITECTURE.md
│   ├── Architecture_APP_KMP.md
│   ├── AGENT_HANDOFF.md
│   ├── DEPLOYMENT_CHECKLIST.md
│   ├── FUNCTION_README.md
│   ├── KAIRONEX_APP_BLUEPRINT.md
│   ├── STUDY_SYSTEM_ARCHITECTURE.md
│   └── VITALITY_UI_DESIGN.md
├── main.py                            # Root entry point
├── requirements.txt                   # Python dependencies
├── appwrite.json                      # Appwrite deployment config
├── BACKEND_ARCHITECTURE.md           # This file
├── README.md
└── LICENSE
```

---

## Test Suite

**149 tests, 0 failures.**

All tests use mocked Gemini calls and mocked database operations — they verify agent logic, routing, error handling, and state management without real API calls.

| Suite | Tests | Scope |
|-------|-------|-------|
| Radius Brain | 57 | All 12 handlers, location modes, streaks, marathon morning, emergency |
| Vitality Brain | 39 | All 20+ handlers, DEFCON flow, morning/evening routines, savings |
| Campaign Brain | 27 | Resume analysis/generation, interviews, skill trees, calibration, goals |
| Study Brain | 26 | Focus interventions, quiz gating, resource ingestion, state caching |

Run all: `python tests/test_all_agents.py`

---

*Built for the Gemini 3 AI Hackathon. Kaironex manages study. Manages survival. Manages career. Manages culture. Manages the student's complete life — continuously, autonomously, and intelligently.*
