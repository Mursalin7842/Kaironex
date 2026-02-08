# 🧠 KAIRONEX DEEP BRAIN

> **Gemini 3 Hackathon Entry** | Track: **🏃 The Marathon Agent**

**Kaironex is a Student Life Operating System** - an autonomous multi-agent orchestrator that manages the complete lifecycle of student success across academics, career, health, and environment.

![Gemini 3](https://img.shields.io/badge/Gemini%203-Flash%20Preview-blue)
![Marathon Agent](https://img.shields.io/badge/Track-Marathon%20Agent-green)
![Agents](https://img.shields.io/badge/Agents-5%20Specialized-purple)

---

## 🎯 Why Kaironex is NOT a Generic Chatbot

| ❌ What We're NOT | ✅ What We ARE |
|-------------------|----------------|
| Single-prompt wrapper | Multi-agent orchestrator with cross-agent context |
| Basic RAG retrieval | Persistent Thought Signatures with state recovery |
| Simple vision analyzer | Spatial-temporal decision engine (fridge analysis → meal planning → budget impact) |
| Generic nutrition bot | **Survival & Growth Protocol** with Financial Defcon System |
| Chat interface | Autonomous Marathon Agent running tasks over hours/days |

---

## 🏗️ Architecture: The Bicameral Engine

```
┌─────────────────────────────────────────────────────────────────┐
│                    KAIRONEX DEEP BRAIN                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │
│  │   STUDY     │  │  VITALITY   │  │  CAMPAIGN   │            │
│  │   Agent     │  │   Agent     │  │   Agent     │            │
│  │ (Cognitive  │  │ (Survival & │  │ (72-hour    │            │
│  │  Supply)    │  │  Growth)    │  │  Marathons) │            │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘            │
│         │                │                │                    │
│         └────────────────┼────────────────┘                    │
│                          ▼                                     │
│              ┌───────────────────────┐                        │
│              │   SUPERVISOR AGENT    │                        │
│              │   (Meta-Controller)   │                        │
│              └───────────┬───────────┘                        │
│                          │                                     │
│         ┌────────────────┼────────────────┐                   │
│         ▼                ▼                ▼                   │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐          │
│  │  BICAMERAL  │  │  MARATHON   │  │  THOUGHT    │          │
│  │   ENGINE    │  │   RUNNER    │  │   MANAGER   │          │
│  │ REFLEX|DEEP │  │ Hours/Days  │  │ Signatures  │          │
│  └─────────────┘  └─────────────┘  └─────────────┘          │
│                                                               │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │               RADIUS AGENT (Spatial Context)             │ │
│  │          Location-aware triggers & environment           │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                               │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🧬 Flagship Feature: Survival & Growth Protocol (Vitality Agent v2.0)

The **Survival & Growth Protocol** transforms Vitality from a health tracker into a **Life Logistics Engine** that balances survival (food/money) with growth (career/study).

### Financial Defcon System

```python
class DefconLevel(IntEnum):
    DEFCON_1_SURVIVAL = 1   # < $8/day  → Austerity Mode
    DEFCON_2_CRITICAL = 2   # $8-15/day → Essential spending only
    DEFCON_3_CAUTION = 3    # $15-30/day → Value conscious
    DEFCON_4_STABLE = 4     # $30-50/day → Balanced living
    DEFCON_5_ABUNDANCE = 5  # > $50/day → Quality & enjoyment
```

### Gemini 3 Vision: Smart Fridge Analysis

```python
# Scans fridge contents using Gemini 3 Vision
inventory = await survival_protocol.analyze_fridge(image_data)
# Returns: ingredients, servings, days_remaining, needs_shopping
```

### Cook vs Order Decision Engine

```
INPUT: time_available, energy_level, budget, fridge_contents, schedule_pressure

DECISION MATRIX:
├─ Low Time + High Budget → ORDER (healthy delivery)
├─ High Time + Low Budget → COOK (use fridge ingredients)
├─ Low Time + Low Budget → EMERGENCY_FUEL (quick cheap options)
├─ Low Energy (any budget) → ORDER (rest priority)
└─ Exam Week → CONVENIENCE (brain fuel priority)
```

### Cross-Agent Context Engine

```python
# Vitality receives context from ALL agents:
campaign_ctx = extract_campaign_financial_context(...)  # Urgent job hunt? → DEFCON 1
study_ctx = extract_study_schedule_context(...)          # Exam week? → Convenience mode
radius_ctx = extract_radius_location_context(...)        # Near grocery? → Shopping alert
```

---

## 🏃 Marathon Agent Capabilities

### What is a Marathon?

A **Marathon** is a multi-step goal that requires autonomous execution over **hours or days**:

```python
@dataclass
class MarathonGoal:
    title: str                    # "Get a Job Interview at Google"
    description: str              # Detailed goal context
    success_criteria: List[str]   # Measurable completion criteria
    deadline: Optional[datetime]  # 72+hours timeline
    priority: int                 # 1-10 scale
```

### State Persistence with Thought Signatures

Every reasoning step creates a **Thought Signature** for state recovery:

```python
@dataclass
class ThoughtSignature:
    thought_id: str           # Unique identifier
    timestamp: datetime       # When reasoning occurred
    context_hash: str         # Cryptographic proof of context
    reasoning_trace: List[str] # Step-by-step reasoning
    confidence: float         # Self-assessed confidence
    parent_signature: str     # Chain to previous thought
```

### Self-Correction Across Sessions

```python
# Marathon Runner checkpoints and recovers across server restarts
class MarathonStatus(Enum):
    PENDING = "pending"
    RUNNING = "running"
    PAUSED = "paused"
    WAITING = "waiting"      # Waiting for user input
    THINKING = "thinking"    # Deep reasoning in progress
    ACTING = "acting"        # Executing an action
    COMPLETE = "complete"
```

---

## 🧠 Bicameral Engine: Dual-Model Reasoning

```python
class ReasoningMode(Enum):
    REFLEX = "reflex"       # Fast, pattern-based (MINIMAL thinking)
    DEEP = "deep"           # Slow, thoughtful (HIGH thinking)
    HYBRID = "hybrid"       # Reflex + validation
    MARATHON = "marathon"   # Long-running with checkpoints
```

**Automatic routing** based on complexity, urgency, and pressure index.

---

## 📱 Mobile Integration

Kaironex operates as the **backend brain** for a Kotlin Multiplatform (KMP) Android app:

- **REFLEX responses** handled by mobile app (fast, under 200ms)
- **DEEP reasoning** handled by this backend (complex planning)
- **State sync** via Appwrite Database (no WebSockets required)

---

## 🔧 Technical Stack

| Component | Technology |
|-----------|------------|
| AI Model | Gemini 3 Flash Preview (Thinking Model) |
| Vision | Gemini 3 Native Multimodal |
| Backend | Appwrite Functions (Python) |
| State | Appwrite Database |
| Mobile | Kotlin Multiplatform (KMP) |

---

## 📊 Project Structure

```
kairo-brain/
├── src/
│   ├── agents/           # 5 Specialized Agents
│   │   ├── campaign_agent.py    # 72-hour career marathons
│   │   ├── vitality_agent_v2.py # Survival & Growth Protocol
│   │   ├── study_agent.py       # Cognitive supply chain
│   │   ├── radius_agent.py      # Spatial context
│   │   └── supervisor_agent.py  # Meta-controller
│   ├── core/             # Engine Components
│   │   ├── bicameral_engine.py  # REFLEX | DEEP routing
│   │   ├── marathon_runner.py   # Long-running orchestration
│   │   ├── thought_manager.py   # Signature persistence
│   │   └── survival_protocol.py # Financial Defcon System
│   ├── tools/            # Agent Capabilities
│   └── utils/            # Shared utilities
└── main.py               # Appwrite Functions entry
```

---

## 🚀 Running the Project

### Prerequisites
- Python 3.11+
- Appwrite Account
- Gemini API Key

### Setup
```bash
# Clone & setup
git clone https://github.com/your-repo/kairo-brain
cd kairo-brain
python -m venv .venv
.venv\\Scripts\\activate  # Windows
pip install -r requirements.txt

# Configure
cp .env.example .env
# Edit .env with your API keys

# Test
python test_survival_protocol.py
```

### Deploy to Appwrite
```bash
appwrite deploy function
```

---

## 🏆 Hackathon Alignment

### Track: Marathon Agent ✅

| Requirement | Our Implementation |
|-------------|-------------------|
| Tasks spanning hours/days | ✅ 72-hour career campaigns, week-long study marathons |
| Thought Signatures | ✅ Full thought chain with cryptographic hashes |
| Thinking Levels | ✅ REFLEX (MINIMAL) / DEEP (HIGH) / MARATHON |
| Self-correction | ✅ MarathonRunner with checkpoints and recovery |
| Multi-step tool calls | ✅ Cross-agent context engine, cascading decisions |
| No human supervision | ✅ Autonomous decision making with drift detection |

### Why We're Different

1. **Not a chatbot** - Multi-agent orchestrator with persistent state
2. **Not simple RAG** - Bicameral reasoning with context-aware routing
3. **Not basic vision** - Full causal decision chain (see fridge → decide meal → calculate budget impact → trigger shopping alert if near store)
4. **Real autonomy** - Tasks complete over 72+ hours without user intervention

---

## 📝 License

MIT License - See [LICENSE](LICENSE)

---

## 👥 Team

Built for **Google DeepMind Gemini 3 Hackathon** - February 2026

---

*"In the Action Era, if a single prompt can solve it, it is not an application."* - We build orchestrators.