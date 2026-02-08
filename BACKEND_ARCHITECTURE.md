# 🧠 Kaironex Deep Brain — Backend Architecture

> **The Marathon Agent that never sleeps.**  
> An autonomous executive-function layer that plans, reasons, self-corrects, and operates across hours and days — not just single prompts — to close the **Prompt Gap**: the moment students most need help but lack the clarity or capacity to ask for it.

---

## Table of Contents

1. [Why Kaironex Exists](#1-why-kaironex-exists)
2. [System Overview — The Three-Brain Architecture](#2-system-overview--the-three-brain-architecture)
3. [Gemini 3 Integration Map](#3-gemini-3-integration-map)
4. [The Marathon Agent Pattern](#4-the-marathon-agent-pattern)
5. [Thought Signatures — The Memory of Reasoning](#5-thought-signatures--the-memory-of-reasoning)
6. [The Bicameral Engine — How Kaironex Thinks](#6-the-bicameral-engine--how-kaironex-thinks)
7. [Agent Architecture — Four Autonomous Brains](#7-agent-architecture--four-autonomous-brains)
8. [Cross-Agent Integration — Agents That Talk](#8-cross-agent-integration--agents-that-talk)
9. [The Orchestration Pipeline](#9-the-orchestration-pipeline)
10. [State Machine — Agent Lifecycle](#10-state-machine--agent-lifecycle)
11. [The DEFCON Survival Protocol](#11-the-defcon-survival-protocol)
12. [Function Calling & Tool System](#12-function-calling--tool-system)
13. [Database Schema — 20 Living Collections](#13-database-schema--20-living-collections)
14. [Test Suite — 149 Offline Tests](#14-test-suite--149-offline-tests)
15. [Why This Wins](#15-why-this-wins)

---

## 1. Why Kaironex Exists

### The Prompt Gap

Education today piles responsibilities onto students until the system outpaces what any one person can manage. Globally, **1.5+ billion learners** navigate crowded curricula and life obligations. About **260 million** are in tertiary education, and roughly **30% of university students leave before getting a degree**. Almost **1 in 4 drop out during the first year**. In the U.S. alone, 43 million adults hold "Some College, No Degree."

These students aren't failing from lack of ability — they're failing because **life and study collide**:

- **60%+ of students work while studying** — shifting schedules break plans
- **6.9 million international students** face harsher stakes: a single GPA drop can mean lost scholarships, visa cancellation, or forced withdrawal
- When overload hits, students don't open planners or chatbots — they **freeze, panic, or fall silent**

That moment is the **Prompt Gap**: when a student most needs help but lacks the clarity or capacity to request it. Existing tools assume the user can still act. They wait for prompts. But the exact people who most need support are the ones who **can't prompt**.

The real failure is not content delivery — it's the absence of **continuous, proactive executive support**: a system that understands classes, work, fatigue, deadlines, and finances, and steps in **before** short-term overload becomes a permanent loss.

Every college and university student faces this intersection:
- **Study** (exams, assignments, focus management, resource ingestion)
- **Survive** (budgets, meals, emergency funds, financial runway)
- **Find work** (ATS-optimized resumes, mock interviews, skill trees, job scanning)
- **Navigate life** (culture shock for internationals, schedule conflicts, life events, exhaustion)

No single prompt can solve this. No chatbot can sustain it. This requires an **autonomous marathon agent** — one that wakes you up with a morning briefing, plans your meals around your budget, detects when your focus drops mid-study, rewrites your resume when a job posting matches your skills, and steps in during the Prompt Gap. All without you asking.

**Kaironex is that agent — built for every student, from first-year freshman to international PhD candidate.**

---

## 2. System Overview — The Three-Brain Architecture

Kaironex uses a **3-model cognitive architecture** inspired by neuroscience's dual-process theory, extended into a tri-layer system:

```mermaid
graph TB
    subgraph "🧠 KAIRONEX THREE-BRAIN ARCHITECTURE"
        direction TB
        
        subgraph Mobile["📱 Kotlin Multiplatform App"]
            REFLEX["⚡ Reflex Agent<br/><code>gemini-3-flash-preview</code><br/>ThinkingLevel: MINIMAL<br/>Response: &lt;500ms"]
            VOICE["🎙️ Voice Engine<br/><code>gemini-2.5-flash-native-audio-preview</code><br/>Live API WebSocket<br/>Real-time conversation"]
        end
        
        subgraph Backend["☁️ Appwrite Function Backend"]
            DEEP["🧠 Deep Brain<br/><code>gemini-3-flash-preview</code><br/>ThinkingLevel: HIGH<br/>thinking_budget: 8192 tokens<br/>Long-horizon reasoning"]
        end
        
        REFLEX -->|"Complex decisions<br/>escalate upstream"| DEEP
        DEEP -->|"Interventions<br/>push downstream"| REFLEX
        VOICE -->|"Context &amp; state<br/>shared via DB"| DEEP
        DEEP -->|"Teaching prompts<br/>scenarios"| VOICE
    end
    
    style REFLEX fill:#4CAF50,color:#fff,stroke:#388E3C
    style VOICE fill:#FF9800,color:#fff,stroke:#F57C00
    style DEEP fill:#1565C0,color:#fff,stroke:#0D47A1
```

| Layer | Model | Thinking Level | Purpose | Latency |
|-------|-------|---------------|---------|---------|
| **Deep Brain** (this backend) | `gemini-3-flash-preview` | `HIGH` (8192 tokens) | Long-horizon planning, marathon steps, ATS analysis, intervention generation | 2–8s |
| **Reflex Agent** (mobile app) | `gemini-3-flash-preview` | `MINIMAL` | Instant UI responses, quick pattern matching, chat | <500ms |
| **Voice Engine** (mobile app) | `gemini-2.5-flash-native-audio-preview-12-2025` | — | Live API WebSocket for real-time teaching, mock interviews, cultural scenario practice | Real-time streaming |

> The same `gemini-3-flash-preview` model serves two cognitive layers — fast reflexes on mobile and deep reasoning on the backend — differentiated purely by **Thinking Level configuration**, not model swaps.

---

## 3. Gemini 3 Integration Map

Every AI call in Kaironex flows through `gemini-3-flash-preview`. Here is exactly where and how:

```mermaid
graph LR
    subgraph "Gemini 3 Flash Preview — Thinking Levels"
        direction TB
        
        HIGH["🧠 HIGH Thinking<br/>budget: 8192 tokens"]
        MINIMAL["⚡ MINIMAL Thinking"]
        
        HIGH --> BC["BicameralEngine<br/>(campaign_brain.py)"]
        HIGH --> DB["DeepBrain<br/>(deep_brain.py)"]
        HIGH --> MR["MarathonRunner<br/>(marathon_runner.py)"]
        
        MINIMAL --> REF["Reflex Mode<br/>(bicameral_engine.py)"]
    end
    
    subgraph "Direct GeminiClient Calls"
        GC["GeminiClient<br/>(gemini_client.py)"]
        GC --> SB["Study Brain<br/>Focus interventions, quizzes"]
        GC --> VB["Vitality Brain<br/>Budget analysis, meal plans"]
        GC --> RB["Radius Brain<br/>Cultural lessons, scenarios"]
        GC --> MB["Main Brain<br/>Drift detection"]
        GC --> SP["SurvivalProtocol<br/>Fridge vision, financial parsing"]
        GC --> DR["DeepResearch<br/>Web research synthesis"]
    end
    
    subgraph "Multimodal Capabilities Used"
        MM["📸 Vision: Fridge scan analysis<br/>🔍 Google Search: Job & local discovery<br/>🛠️ Function Calling: 10 tool groups<br/>💭 Structured Thinking: analyze/strategy/decision tags<br/>📊 JSON Mode: Structured outputs"]
    end
    
    style HIGH fill:#1565C0,color:#fff
    style MINIMAL fill:#4CAF50,color:#fff
```

### Gemini 3 Features Used

| Feature | Where Used | Technical Detail |
|---------|-----------|------------------|
| **Thinking Mode (HIGH)** | `BicameralEngine`, `DeepBrain`, `MarathonRunner` | `thinking_budget=8192`, structured `<analyze>/<strategy>/<decision>/<action>` extraction |
| **Thinking Mode (MINIMAL)** | `BicameralEngine.reflex_generate()`, mobile Reflex Agent | Fast pattern matching, <500ms |
| **Function Calling** | `BicameralEngine.reason_with_tools()` | 10 tool groups, up to 5 iterative tool-call loops per reasoning step |
| **Google Search Grounding** | `campaign_brain.scan_daily_jobs()`, `search_tools.py` | Real-time job discovery, local services, career research |
| **Vision / Multimodal** | `SurvivalProtocol.analyze_fridge()`, `gemini_client.generate_multimodal()` | Fridge photo → ingredient inventory → meal suggestions |
| **JSON Mode** | All brain outputs, quiz generation, ATS scoring | Structured `response_mime_type="application/json"` |
| **Large Context Window** | `ThoughtManager` injects previous reasoning chain | Up to 1M tokens for multi-step marathon context |
| **Structured Output** | `campaign_brain` resume/interview/skill_tree generation | Complex nested JSON schemas for career artifacts |

### Where `gemini-2.5-flash-native-audio-preview-12-2025` Is Used

The **Gemini Live API** with native audio runs on the **mobile app** (Kotlin Multiplatform) for:

| Feature | Description |
|---------|-------------|
| **Cultural Scenario Practice** | Radius Brain generates cultural scenarios → Voice Engine acts them out in real-time conversation with the student |
| **Mock Interview Sessions** | Campaign Brain designs interview question banks → Voice Engine conducts the mock interview as a live audio conversation |
| **Live Teaching Prompts** | Radius Brain creates `live_agent_prompt` data → Voice Engine uses it for interactive language/culture teaching |
| **Real-Time Tutoring** | Study Brain topics → Voice Engine explains concepts conversationally |

> The Deep Brain (this backend) generates the **content and structure** for voice sessions. The mobile app's Voice Engine then **delivers** them through the Live API WebSocket, creating a seamless brain-to-voice pipeline.

---

## 4. The Marathon Agent Pattern

This is the core of what makes Kaironex a **Marathon Agent** — not a chatbot.

### What Makes It Marathon

A Marathon Agent operates **autonomously across hours and days**, maintaining reasoning continuity through:

1. **Thought Chains** — Every AI reasoning step produces a `ThoughtSignature` linked to its parent, forming an unbroken chain of reasoning across time
2. **Persistent State** — Agent state survives process restarts via Appwrite database
3. **Self-Correction** — Confidence scoring triggers escalation from REFLEX → DEEP when the agent is uncertain
4. **Proactive Execution** — Agents fire without user input: morning routines, drift detection, end-of-day summaries
5. **Multi-Step Decomposition** — Complex goals are broken into 5–15 steps, each executed with full reasoning context

```mermaid
sequenceDiagram
    participant User as 👤 Student
    participant App as 📱 Mobile App
    participant Cron as ⏰ CRON Trigger
    participant Orch as 🧠 Orchestrator
    participant Brain as 🔥 Agent Brain
    participant Gem as 💎 Gemini 3
    participant DB as 🗄️ Appwrite DB
    
    Note over Cron,DB: MORNING — Autonomous Marathon Start (no user input)
    Cron->>Orch: CRON trigger (6:00 AM)
    Orch->>Brain: run_supervisor()
    Brain->>DB: Fetch all users from agent_memory
    
    loop For each user
        Brain->>DB: Load thought chain (last N signatures)
        Brain->>Gem: Marathon morning reasoning<br/>(with full thought context)
        Gem-->>Brain: Morning briefing + today's plan
        Brain->>DB: Store ThoughtSignature (chain link)
        Brain->>DB: Create interventions (push notifications)
    end
    
    Note over User,DB: MID-DAY — Reactive + Proactive
    User->>App: Opens study session
    App->>Orch: study_brain (status: IN_PROGRESS)
    Orch->>Brain: Study Brain handler
    Brain->>DB: Load thought chain + state cache
    Brain->>Gem: Focus analysis with context
    Gem-->>Brain: Focus OK / Intervention needed
    
    alt Focus Drop Detected (score < 40)
        Brain->>DB: Create FOCUS_DROP intervention
        Brain->>DB: Store ThoughtSignature (linked to morning thought)
        DB-->>App: Push intervention notification
        App-->>User: "Hey, your focus dropped. Let's try a 5-min break."
    end
    
    Note over Cron,DB: EVENING — Autonomous Summary (no user input)
    Cron->>Orch: CRON trigger (10:00 PM)
    Orch->>Brain: auto_end_of_day for each user
    Brain->>DB: Load full day's thought chain
    Brain->>Gem: Day reflection with all context
    Gem-->>Brain: Daily summary + tomorrow's prep
    Brain->>DB: Store final ThoughtSignature of day
    Brain->>DB: Update daily_snapshots
```

### Marathon Runner — Multi-Step Task Execution

For complex goals (e.g., "Prepare me for a Google SWE interview in 30 days"), the `MarathonRunner` decomposes and executes:

```mermaid
stateDiagram-v2
    [*] --> PENDING: create_marathon()
    PENDING --> RUNNING: run()
    
    RUNNING --> THINKING: Next step
    THINKING --> ACTING: Gemini reasoning complete
    ACTING --> WAITING: Step executed, checkpoint saved
    WAITING --> THINKING: Next step ready
    
    RUNNING --> PAUSED: pause()
    PAUSED --> RUNNING: resume()
    
    WAITING --> COMPLETE: All steps done
    THINKING --> FAILED: Max retries exceeded
    ACTING --> FAILED: Unrecoverable error
    
    FAILED --> RUNNING: Retry with backoff
    
    note right of THINKING
        Each step:
        1. Load previous ThoughtChain
        2. Build context from ThoughtManager
        3. Reason with MARATHON mode
        4. Store ThoughtSignature
        5. Chain via parent_thought
    end note
    
    note right of WAITING
        Checkpoint every 5 steps
        Session persisted to DB
        Survives process restart
    end note
```

**Configuration:**
- Steps per marathon: 5–15 (decomposed by DEEP reasoning)
- Step timeout: 300 seconds
- Max retries per step: 3 (exponential backoff)
- Checkpoint interval: Every 5 steps
- Reasoning mode: `ReasoningMode.MARATHON`

---

## 5. Thought Signatures — The Memory of Reasoning

The **ThoughtSignature** is Kaironex's core innovation for marathon continuity. Every AI reasoning call produces one, and they chain together to form an unbroken record of **why** the system made every decision.

```mermaid
graph LR
    subgraph "Thought Chain — One Day of a Student's Life"
        T1["🌅 Thought #1<br/>Marathon Morning<br/>confidence: 0.92<br/>agent: vitality<br/>action: morning_briefing"]
        T2["📚 Thought #2<br/>Study Session Start<br/>confidence: 0.88<br/>agent: study<br/>action: session_tracked"]
        T3["⚠️ Thought #3<br/>Focus Drop Detected<br/>confidence: 0.85<br/>agent: study<br/>action: intervention_sent"]
        T4["🍽️ Thought #4<br/>Lunch Suggestion<br/>confidence: 0.90<br/>agent: vitality<br/>action: meal_suggested"]
        T5["🌍 Thought #5<br/>Cultural Lesson<br/>confidence: 0.87<br/>agent: radius<br/>action: lesson_generated"]
        T6["⚔️ Thought #6<br/>Job Match Found<br/>confidence: 0.91<br/>agent: campaign<br/>action: job_alert_sent"]
        T7["🌙 Thought #7<br/>End of Day<br/>confidence: 0.89<br/>agent: vitality<br/>action: day_summarized"]
        
        T1 -->|parent| T2
        T2 -->|parent| T3
        T3 -->|parent| T4
        T4 -->|parent| T5
        T5 -->|parent| T6
        T6 -->|parent| T7
    end
    
    style T1 fill:#FF9800,color:#fff
    style T2 fill:#4CAF50,color:#fff
    style T3 fill:#f44336,color:#fff
    style T4 fill:#FF9800,color:#fff
    style T5 fill:#2196F3,color:#fff
    style T6 fill:#9C27B0,color:#fff
    style T7 fill:#607D8B,color:#fff
```

### ThoughtSignature Schema

```python
@dataclass
class ThoughtSignature:
    thought_id: str           # "thought_a1b2c3d4e5f6" (unique per reasoning step)
    timestamp: datetime       # When this thought occurred
    user_id: str              # Which student
    agent: str                # Which brain (study/vitality/campaign/radius)
    context_hash: str         # SHA-256(user_id:prompt:timestamp)[:16] — integrity check
    reasoning_trace: List[str] # Extracted <analyze>, <strategy>, <decision> tags
    confidence: float         # 0.1–1.0 (triggers escalation if < 0.7)
    tool_calls: List[str]     # Names of tools called during reasoning
    action_output: str        # Final action taken
    parent_signature: str     # Links to previous thought → forms the chain
```

### How Continuity Works

1. **Creation**: Every Gemini call → ThoughtSignature with unique ID + SHA-256 context hash
2. **Chaining**: Each new thought links to its `parent_signature`, forming an unbroken chain
3. **Storage**: Dual-write to `thought_signatures` (permanent ledger) AND `agent_memory` (fast lookup)
4. **Injection**: `ThoughtManager.get_reasoning_context()` reads last N thoughts and injects them as `=== PREVIOUS REASONING CONTEXT ===` into the next Gemini prompt
5. **Pressure Analysis**: `ThoughtManager.compute_pressure_index()` analyzes thought frequency and declining confidence patterns to detect student stress

> **Why this matters for Marathon Agents**: A chatbot forgets between messages. Kaironex's thought chain means the 10 PM end-of-day summary has full context of the 6 AM morning plan, the mid-day focus drop, and the afternoon cultural lesson — all linked through thought signatures.

---

## 6. The Bicameral Engine — How Kaironex Thinks

Named after Julian Jaynes' bicameral mind theory, the engine implements **dual-process cognition**:

```mermaid
graph TB
    subgraph "BicameralEngine — Dual-Process Reasoning"
        INPUT["📥 Incoming Request"]
        
        INPUT --> MODE{Reasoning Mode?}
        
        MODE -->|REFLEX| R["⚡ System 1: Fast<br/>ThinkingLevel.MINIMAL<br/>temp: 0.7<br/>max_output: 2048<br/>Pattern matching"]
        MODE -->|DEEP| D["🧠 System 2: Slow<br/>ThinkingLevel.HIGH<br/>thinking_budget: 8192<br/>Structured reasoning tags"]
        MODE -->|HYBRID| H["🔄 Hybrid Path"]
        MODE -->|MARATHON| M["🏃 Marathon Path"]
        
        H --> R
        R --> CONF{Confidence<br/>≥ 0.7?}
        CONF -->|Yes| OUT["📤 Response"]
        CONF -->|No| D
        
        D --> TS["📝 ThoughtSignature<br/>created + stored"]
        TS --> OUT
        
        M --> D
        M --> CP["💾 Checkpoint<br/>to Appwrite"]
        CP --> CHAIN["🔗 Chain to<br/>parent thought"]
        CHAIN --> OUT
    end
    
    style R fill:#4CAF50,color:#fff
    style D fill:#1565C0,color:#fff
    style H fill:#FF9800,color:#fff
    style M fill:#9C27B0,color:#fff
```

### Reasoning Modes

| Mode | Thinking Level | Use Case | Example |
|------|---------------|----------|---------|
| `REFLEX` | MINIMAL | Quick responses, simple lookups | "What's my study streak?" |
| `DEEP` | HIGH (8192 tokens) | Complex analysis, career strategy | ATS resume scoring, interview design |
| `HYBRID` | MINIMAL → HIGH | Uncertain situations | Start fast, escalate if confidence < 0.7 |
| `MARATHON` | HIGH + checkpoints | Multi-step autonomous tasks | 30-day interview prep plan |

### Structured Thinking Tags

When operating in DEEP or MARATHON mode, the engine prompts Gemini to use structured reasoning:

```
<analyze>What is the student's current situation?</analyze>
<strategy>What approach should we take?</strategy>  
<decision>What specific action do we commit to?</decision>
<action>Execute the decided action</action>
```

These tags are extracted into `reasoning_trace` in the ThoughtSignature, creating an auditable trail of **how** the AI reasoned — not just what it output.

---

## 7. Agent Architecture — Four Autonomous Brains

```mermaid
graph TB
    subgraph "🧠 Kaironex Agent System"
        SUP["👑 Supervisor Agent<br/>Drift detection, pressure assessment<br/>Cross-agent coordination<br/>Weekly digest generation"]
        
        SUP --> STUDY["🎓 Study Brain<br/>Focus tracking, quiz gating<br/>Resource analysis<br/>Cognitive supply chain"]
        SUP --> VITAL["🧬 Vitality Brain<br/>DEFCON survival protocol<br/>Proactive meals, emergency fund<br/>Budget management, fridge vision"]
        SUP --> CAMP["⚔️ Campaign Brain<br/>ATS resume analysis<br/>Mock interview design<br/>Skill tree generation<br/>Job scanning with Search"]
        SUP --> RAD["🌍 Radius Brain<br/>Cultural survival engine<br/>Daily lessons, scenarios<br/>Safehouse network<br/>Location-aware modes"]
    end
    
    subgraph "Cross-Agent Channels"
        C1["💰 Financial Defcon<br/>Campaign ↔ Vitality"]
        C2["⏰ Time-Energy Matrix<br/>Study ↔ Vitality"]
        C3["📍 Location Triggers<br/>Radius → Vitality"]
        C4["🎉 Victory Feast<br/>Campaign → Vitality"]
    end
    
    CAMP <--> C1 <--> VITAL
    STUDY <--> C2 <--> VITAL
    RAD --> C3 --> VITAL
    CAMP --> C4 --> VITAL
    
    style SUP fill:#FF5722,color:#fff
    style STUDY fill:#4CAF50,color:#fff
    style VITAL fill:#FF9800,color:#fff
    style CAMP fill:#9C27B0,color:#fff
    style RAD fill:#2196F3,color:#fff
```

### Study Brain — Cognitive Supply Chain

| Event | Trigger | Action |
|-------|---------|--------|
| Focus Drop | `IN_PROGRESS` + `focus_score < 40` | AI-generated intervention, thought signature stored |
| Knowledge Gate | `REQUEST_UNLOCK` | Gemini generates quiz → must pass to unlock next content |
| Resource Ingestion | File upload / `resource_ingestion` | AI analyzes document, creates study summary + intervention |
| Heartbeat | Every study session tick | State cache update, pressure tracking |

### Vitality Brain — Life Logistics Engine

| Event | What Happens |
|-------|-------------|
| `one_shot_setup` | Day-zero financial setup: income, bills, runway → DEFCON level calculated |
| `fridge_scan` | **Gemini Vision** analyzes fridge photo → ingredient inventory → meal suggestions |
| `proactive_meal_plan` | AI generates 3 meal options per slot (breakfast/lunch/dinner) within DEFCON budget |
| `marathon_morning_routine` | **Autonomous**: Budget check + meal plan + energy assessment + daily briefing |
| `emergency_fund_deposit/withdraw` | Emergency fund management with validation |
| `auto_end_of_day` | **Autonomous**: Day summary, savings auto-allocation, next-day prep |
| `daily_budget_check` | Real-time budget vs. runway analysis |
| `defcon_update` | DEFCON level recalculation based on financial changes |

### Campaign Brain — Career Strategist

| Event | AI Technique | Output |
|-------|-------------|--------|
| `analyze_resume` | `BicameralEngine` + `ReasoningMode.DEEP` | ATS score (0–100), breakdown by 5 criteria, keyword matching, gap analysis |
| `generate_resume` | `BicameralEngine` + `ReasoningMode.DEEP` | Tailored resume JSON with professional summary, skills, experience, projects — all matched to JD |
| `design_interview` | `BicameralEngine` + `ReasoningMode.DEEP` | Question bank for **Gemini Live API** mock interview sessions |
| `generate_skill_tree` | `BicameralEngine` + `ReasoningMode.DEEP` | Visual skill tree with learning paths, time-to-job-ready estimate |
| `campaign_calibration` | `BicameralEngine` + `ReasoningMode.DEEP` | Full career init: Skill Tree + Armory (tools/certs) + Quest Board (daily tasks) |
| `scan_jobs` | `GeminiClient` + **Google Search grounding** | Real-time job discovery matching student skills |

### Radius Brain — Cultural Survival Engine

| Event | Purpose |
|-------|---------|
| `cultural_setup` | Profile creation: home country, target language, cultural gaps |
| `daily_lesson` | AI-generated micro-lessons on local culture, slang, customs |
| `live_agent_prompt` | Generates teaching prompts for **Gemini Live API** voice practice |
| `cultural_scenario` | Interactive scenarios (ordering food, talking to professors, etc.) for Live API |
| `location_change` | Adapts mode: library → DEEP_FOCUS, campus → CAMPUS, home → REST |
| `safehouse_add/check` | Safe space network (campus resources, study spots, community hubs) |
| `emergency_cultural` | Urgent cultural help (visa issues, discrimination, isolation) |
| `marathon_cultural_morning` | **Autonomous**: Daily lesson + voice prompt + cultural briefing |

---

## 8. Cross-Agent Integration — Agents That Talk

```mermaid
graph LR
    subgraph "Cross-Agent Integration Channels"
        direction TB
        
        subgraph "💰 Financial Defcon Channel"
            CAMP_FIN["⚔️ Campaign<br/>Got a job!"] -->|employment_status| VIT_FIN["🧬 Vitality<br/>DEFCON 5 → 4 → 3..."]
            VIT_FIN -->|budget_pressure| CAMP_FIN
        end
        
        subgraph "⏰ Time-Energy Matrix Channel"
            STU_TIME["🎓 Study<br/>Exam in 2 days!"] -->|exam_mode: true| VIT_TIME["🧬 Vitality<br/>Switch to convenience meals"]
            VIT_TIME -->|energy_level: low| STU_TIME
        end
        
        subgraph "📍 Location Trigger Channel"
            RAD_LOC["🌍 Radius<br/>Near grocery store"] -->|proximity_alert| VIT_LOC["🧬 Vitality<br/>Shopping alert!"]
        end
        
        subgraph "🎉 Victory Feast Channel"
            CAMP_WIN["⚔️ Campaign<br/>Interview passed!"] -->|career_win| VIT_WIN["🧬 Vitality<br/>Unlock victory feast budget"]
        end
    end
```

The `CrossAgentContext` unifies shared state across all four agents:

```python
class CrossAgentContext:
    vitality: {defcon_level, daily_budget, energy_level, meal_status}
    campaign: {employment_status, recent_wins, active_applications}
    study:    {current_pressure, exam_mode, focus_history}
    radius:   {current_location, nearest_grocery, cultural_comfort_level}
```

**Example flow**: Student gets a job offer → Campaign Brain fires `career_win` → Vitality Brain receives it via cross-agent sync → DEFCON level improves → Meal budget increases → Victory feast unlocked → Push notification: "You earned it. Tonight, order your favorite meal. 🎉"

---

## 9. The Orchestration Pipeline

```mermaid
flowchart TB
    subgraph "Entry Points"
        APP["📱 Mobile App<br/>(HTTP request)"]
        CRON["⏰ CRON Trigger<br/>(Autonomous schedule)"]
        DB_EVT["🗄️ DB Event<br/>(Collection change)"]
    end
    
    APP --> MAIN["src/main.py<br/>Appwrite Function Entry"]
    CRON --> MAIN
    DB_EVT --> MAIN
    
    MAIN --> ROUTE{Route by<br/>Collection or<br/>HTTP Path}
    
    ROUTE -->|"users collection"| CAMP_B["campaign_brain.py"]
    ROUTE -->|"study_logs collection"| STUDY_B["study_brain.py"]
    ROUTE -->|"resources collection"| STUDY_B
    ROUTE -->|"vitality_state collection"| VIT_B["vitality_brain_v2.py"]
    ROUTE -->|"radius_state collection"| RAD_B["radius_brain.py"]
    ROUTE -->|"/brain/deep HTTP"| ORCH["orchestrator.py"]
    ROUTE -->|"/campaign HTTP"| CAMP_B
    ROUTE -->|"CRON fallback"| SUP["main_brain.py<br/>Supervisor"]
    
    ORCH --> REQ_TYPE{Request<br/>Type?}
    REQ_TYPE -->|CHAT| BIC_H["BicameralEngine<br/>HYBRID + tools"]
    REQ_TYPE -->|COMMAND| BIC_D["BicameralEngine<br/>DEEP + tools"]
    REQ_TYPE -->|RESEARCH| DRE["DeepResearchEngine"]
    REQ_TYPE -->|MARATHON| MR["MarathonRunner"]
    REQ_TYPE -->|LIFE_EVENT| LEH["LifeEventHandler"]
    
    subgraph "Every AI Call"
        direction LR
        GEM["💎 Gemini 3 Flash Preview"] --> TS["📝 ThoughtSignature"] --> AW["🗄️ Appwrite DB"]
    end
    
    CAMP_B --> GEM
    STUDY_B --> GEM
    VIT_B --> GEM
    RAD_B --> GEM
    BIC_H --> GEM
    BIC_D --> GEM
    DRE --> GEM
    MR --> GEM
```

---

## 10. State Machine — Agent Lifecycle

Every agent brain follows a formal state machine:

```mermaid
stateDiagram-v2
    [*] --> DORMANT
    DORMANT --> INITIALIZING: trigger_event received
    INITIALIZING --> THINKING: context loaded
    INITIALIZING --> ERROR: init failed
    
    THINKING --> ACTING: Gemini reasoning complete
    THINKING --> WAITING: no action needed
    THINKING --> ERROR: reasoning failed
    
    ACTING --> OBSERVING: action executed
    ACTING --> ERROR: action failed
    
    OBSERVING --> REFLECTING: observe result
    OBSERVING --> ACTING: retry needed
    OBSERVING --> ERROR: observation failed
    
    REFLECTING --> WAITING: reflection complete
    REFLECTING --> THINKING: new insight → re-reason
    REFLECTING --> DORMANT: task complete
    
    WAITING --> THINKING: new trigger
    WAITING --> DORMANT: timeout
    WAITING --> TERMINATED: shutdown
    
    ERROR --> DORMANT: recovered
    ERROR --> WAITING: degraded mode
    ERROR --> TERMINATED: unrecoverable

    note right of THINKING
        Gemini 3 Flash Preview
        ThinkingLevel: HIGH
        thinking_budget: 8192
    end note
    
    note right of REFLECTING
        ThoughtSignature stored
        Parent chain linked
        Confidence evaluated
    end note
```

---

## 11. The DEFCON Survival Protocol

The financial survival system for students operating on tight budgets — from working undergrads to international students on strict visa-tied scholarships:

```mermaid
graph TB
    subgraph "🚨 DEFCON Financial Survival System"
        D5["DEFCON 5 — ABUNDANCE<br/>Runway: 3-6+ months, stable job<br/>Budget: $300/month groceries<br/>Mode: Premium choices, eat well"]
        D4["DEFCON 4 — STABLE<br/>Runway: 3-6 months<br/>Budget: $200/month<br/>Mode: Balanced spending"]
        D3["DEFCON 3 — CAUTION<br/>Runway: 2-3 months<br/>Budget: $120/month<br/>Mode: Value meals, meal prep"]
        D2["DEFCON 2 — CRITICAL<br/>Runway: 1-2 months<br/>Budget: $80/month<br/>Mode: Strict budgeting, rice & beans"]
        D1["DEFCON 1 — SURVIVAL<br/>Runway: &lt;1 month, unemployed<br/>Budget: $40/month<br/>Mode: AUSTERITY — food banks, ramen"]
        
        D5 --> D4
        D4 --> D3
        D3 --> D2
        D2 --> D1
        D1 -->|"Got a job!<br/>(Campaign → Vitality)"| D4
    end
    
    style D5 fill:#4CAF50,color:#fff
    style D4 fill:#8BC34A,color:#000
    style D3 fill:#FFC107,color:#000
    style D2 fill:#FF9800,color:#fff
    style D1 fill:#f44336,color:#fff
```

### Meal Decision Engine

Every meal decision factors in: **Time × Money × Energy**

```python
Decision Matrix:
├── DEFCON 1-2 + Low Energy → EMERGENCY_FUEL (instant noodles, energy bar)
├── DEFCON 1-2 + Has Time   → COOK (rice & beans, eggs)
├── DEFCON 3-4 + Exam Mode  → CONVENIENCE (quick healthy options)
├── DEFCON 4-5 + Normal     → COOK or ORDER (balanced choices)
└── Victory Feast unlocked  → ORDER (favorite restaurant)
```

### Emergency Fund & Auto-Save

- **Auto-save**: Configurable percentage split — half to emergency fund, half to savings
- **Emergency withdrawal**: Validated, tracked, triggers DEFCON recalculation
- **Shopping lists**: Pre-computed per DEFCON level ($40/$80/$120/$200/$300 budgets)

---

## 12. Function Calling & Tool System

Kaironex uses Gemini's native function calling with 10 specialized tool groups:

```mermaid
graph TB
    subgraph "🛠️ Tool Declaration System"
        direction TB
        
        BIC["BicameralEngine.reason_with_tools()"] --> LOOP["Tool Call Loop<br/>(up to 5 iterations)"]
        LOOP --> FC["Gemini Function Calling"]
        FC --> TE["ToolExecutor"]
        
        TE --> T1["📚 Study Planner<br/>create_study_plan<br/>reschedule_session"]
        TE --> T2["📦 Content Delivery<br/>get_study_content<br/>prepare_content_ahead"]
        TE --> T3["🧪 Mastery Evaluator<br/>evaluate_mastery<br/>generate_quiz"]
        TE --> T4["🔍 Web Search<br/>search_web<br/>search_careers<br/>search_local"]
        TE --> T5["💰 Financial<br/>check_financial_status<br/>suggest_meal<br/>assess_job_need"]
        TE --> T6["🌐 International<br/>check_visa_status<br/>check_work_hours<br/>explain_slang<br/>get_cultural_tip"]
        TE --> T7["📍 Local Radius<br/>find_study_spot<br/>get_walking_route<br/>check_housing"]
        TE --> T8["👁️ Presence<br/>check_engagement<br/>send_gentle_nudge<br/>detect_distraction"]
        TE --> T9["📅 Schedule<br/>get_schedule<br/>add_schedule_block<br/>handle_life_event"]
        TE --> T10["⚡ Intervention<br/>trigger_intervention"]
    end
```

**Agent → Tool Mapping:**

| Agent | Tools Available |
|-------|----------------|
| Study Brain | Planner + Content + Mastery + Search + Presence |
| Campaign Brain | Search + Schedule + Intervention |
| Vitality Brain | Financial + Local Radius + Presence + Schedule |
| Radius Brain | Local Radius + International + Search |
| Supervisor | **ALL 10 tool groups** |

---

## 13. Database Schema — 20 Living Collections

```mermaid
erDiagram
    users {
        string userId PK
        string name
        string studentState_json
        json preferences
    }
    
    agent_memory {
        string memoryId PK
        string userId FK
        json state_cache
        json last_thought_ref
        float pressure_index
        datetime last_heartbeat
    }
    
    thought_signatures {
        string thought_id PK
        string userId FK
        string agent
        string context_hash
        json reasoning_trace
        float confidence
        string parent_signature FK
        datetime timestamp
    }
    
    interventions {
        string interventionId PK
        string userId FK
        string agent
        string trigger
        string type
        string urgency
        string channel
        json data
        datetime timestamp
    }
    
    marathon_sessions {
        string sessionId PK
        string userId FK
        string goal_title
        json steps
        float progress
        string status
        json thought_chain
        datetime created_at
    }
    
    campaign_state {
        string stateId PK
        string userId FK
        json skill_tree
        json armory
        json quest_board
        json active_applications
    }
    
    financial_state {
        string stateId PK
        string userId FK
        float balance
        float daily_runway
        int defcon_level
        float emergency_fund
        float savings_balance
    }
    
    users ||--o{ agent_memory : "has"
    users ||--o{ thought_signatures : "generates"
    users ||--o{ interventions : "receives"
    users ||--o{ marathon_sessions : "runs"
    users ||--o{ campaign_state : "has"
    users ||--o{ financial_state : "has"
    thought_signatures ||--o| thought_signatures : "chains via parent"
```

**All 20 Collections:**

| Collection | Purpose |
|-----------|---------|
| `users` | Core user data + state JSON |
| `agent_memory` | Living agent state per user (fast lookup) |
| `thought_signatures` | Permanent thought chain ledger |
| `interventions` | AI-generated push notifications/actions |
| `marathon_sessions` | Long-running task tracking |
| `campaign_state` | Career agent state (skill tree, quests) |
| `financial_state` | DEFCON system financial data |
| `vitality_state` | Health/energy/meal state |
| `radius_state` | Cultural progress + safehouses |
| `schedule` | Student schedule entries |
| `study_logs` | Study session telemetry |
| `daily_snapshots` | End-of-day summaries |
| `policy_episodes` | RL-style policy learning data |
| `resources` | Uploaded learning materials |
| `monthly_plans` | Monthly planning data |
| `student_profiles` | Learned student personality/patterns |
| `life_events` | Life event records |
| `schedule_changes` | Schedule modification history |
| `international_info` | Visa, work hours, cultural data |
| `concept_mastery` | Per-concept mastery tracking |

---

## 14. Test Suite — 149 Offline Tests

The entire brain system is testable offline without API keys or database access:

```
python test_all_agents.py
```

| Suite | Tests | Assertions | Coverage |
|-------|-------|-----------|----------|
| 🌍 Radius Brain | 17 tests | 57 ✅ | All 12 handlers + edge cases |
| 🧬 Vitality Brain | 20 tests | 39 ✅ | All 20+ handlers + DEFCON + marathon |
| 🎓 Study Brain | 9 tests | 26 ✅ | Focus drop, quiz gate, resource ingestion |
| ⚔️ Campaign Brain | 12 tests | 27 ✅ | ATS, resume gen, interview, skill tree |
| **Total** | **58 tests** | **149 ✅** | **All agents, all handlers** |

**Mock Infrastructure** (`test_mocks.py`):
- `MockDBHelper`: 25+ in-memory database methods, full state persistence
- `MockGeminiClient`: Deterministic keyword-based responses (no API needed)
- `MockBicameralEngine`: Async reasoning mock with `MockReasoningResult`
- `MockContext` / `MockRes` / `MockReq`: Appwrite function context simulation

```bash
# Run individual suites
python test_radius_brain.py      # 57/57 PASS
python test_vitality_brain.py    # 39/39 PASS
python test_study_brain.py       # 26/26 PASS
python test_campaign_brain.py    # 27/27 PASS

# Run everything
python test_all_agents.py        # 149/149 PASS  🎉
```

---

## 15. Why This Wins

### Judging Criteria Alignment

| Criterion | Weight | How Kaironex Scores |
|-----------|--------|-------------------|
| **Technical Execution** | 40% | Four autonomous agent brains, BicameralEngine with dual-process reasoning, ThoughtSignature chains for marathon continuity, 20-collection Appwrite schema, 149 passing tests, function calling with 10 tool groups, Gemini Vision for fridge scanning, Google Search grounding for job discovery |
| **Innovation / Wow Factor** | 30% | **Not a chatbot** — a marathon agent system that autonomously manages a student's entire life (study + survival + career + cultural integration) across hours and days. Closes the **Prompt Gap** — acts when students can't ask. Thought Signatures create an auditable reasoning chain. DEFCON survival protocol with real financial modeling. Cross-agent integration where getting a job literally changes your meal budget. |
| **Potential Impact** | 20% | 260M tertiary students globally, 30% dropout rate, 43M "Some College, No Degree" in the U.S. alone. 60%+ work while studying, 6.9M are international with visa-level stakes. Even a 1–5% retention improvement scales to millions of saved trajectories and hundreds of millions in recovered tuition and lifetime earnings. This is infrastructure-level intervention for education's biggest structural failure. |
| **Presentation / Demo** | 10% | Full architecture documentation with Mermaid diagrams, comprehensive test suite judges can run, clear Gemini 3 integration mapping, working Kotlin Multiplatform app |

### What Makes This Different From "Another Chatbot"

> *"In the Action Era, if a single prompt can solve it, it is not an application."* — Hackathon brief

Kaironex cannot be solved by a single prompt. It is:

1. **An orchestrator**, not a wrapper — 4 specialized brains coordinated by a supervisor
2. **A marathon agent** — operates autonomously across days via CRON-triggered morning routines, drift detection, and end-of-day summaries
3. **A multi-model system** — `gemini-3-flash-preview` at two thinking levels plus `gemini-2.5-flash-native-audio-preview` for voice
4. **A reasoning system** — ThoughtSignatures chain every decision with auditable traces
5. **A Prompt Gap closer** — unlike reactive tools that wait for user input, Kaironex senses overload and acts *before* the student freezes
6. **A survival system** — DEFCON financial protocol with real budget math, not generic advice
7. **A cross-agent system** — agents communicate: your job status affects your meal budget, your exam schedule affects your meal complexity, your location triggers shopping alerts

**This is the Marathon Agent track, built for 260 million students who deserve more than a chatbot.**

---

<div align="center">

**Built with Gemini 3 Flash Preview + Gemini 2.5 Flash Native Audio**  
**Kotlin Multiplatform (Mobile) + Python/Appwrite (Backend)**  
**149 tests. 4 brains. 260M students. 1 mission.**

*Kaironex closes the Prompt Gap — acting when students can't ask, so short-term overload never becomes permanent loss.*

</div>
