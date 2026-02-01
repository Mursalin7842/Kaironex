# 🧠 KAIRONEX BRAIN: Trillion-Dollar Backend Architecture

## Executive Summary

This document outlines the complete backend architecture for the Kaironex "Student Life Operating System." The system transforms the current single-pass agent scripts into a **Stateful Marathon Agent Framework** with:

- **Bicameral Reasoning Engine** (Fast Reflex + Deep Thinking)
- **Thought Signature Persistence** for multi-turn state integrity
- **Event-Driven Agent Orchestration** with async background processing
- **Real-Time WebSocket Communication** with the Kotlin client
- **Policy Episode Learning** for reinforcement-based improvement

---

## 🏗️ System Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           KAIRONEX BRAIN v2.0                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    🌐 API GATEWAY (FastAPI)                          │   │
│  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌──────────────┐   │   │
│  │  │ /study  │ │/vitality│ │ /radius │ │/campaign│ │/ws/realtime  │   │   │
│  │  └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘ └──────┬───────┘   │   │
│  └───────┼──────────┼──────────┼──────────┼──────────────┼─────────────┘   │
│          │          │          │          │              │                  │
│  ┌───────▼──────────▼──────────▼──────────▼──────────────▼─────────────┐   │
│  │                    🎯 EVENT ROUTER & DISPATCHER                      │   │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────┐  │   │
│  │  │ Priority Queue  │  │  Event Classifier│  │ Background Scheduler│  │   │
│  │  └────────┬────────┘  └────────┬────────┘  └──────────┬──────────┘  │   │
│  └───────────┼────────────────────┼──────────────────────┼─────────────┘   │
│              │                    │                      │                  │
│  ┌───────────▼────────────────────▼──────────────────────▼─────────────┐   │
│  │                    🧠 BICAMERAL REASONING ENGINE                     │   │
│  │  ┌─────────────────────────┐  ┌─────────────────────────────────┐   │   │
│  │  │  ⚡ REFLEX CORTEX       │  │  🔮 DEEP COGNITION ENGINE        │   │   │
│  │  │  (Gemini 2.5 Flash)     │  │  (Gemini 3 Thinking )│   │   │
│  │  │  - Instant Responses    │  │  - thinking_level="HIGH"         │   │   │
│  │  │  - Pattern Matching     │  │  - Thought Signatures            │   │   │
│  │  │  - Quick Interventions  │  │  - Multi-Step Planning           │   │   │
│  │  └─────────────────────────┘  └─────────────────────────────────┘   │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                         │                                    │
│  ┌──────────────────────────────────────▼───────────────────────────────┐   │
│  │                    🤖 MARATHON AGENT ORCHESTRATOR                     │   │
│  │  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────────────┐ │   │
│  │  │STUDY_AGENT │ │VITAL_AGENT │ │RADIUS_AGENT│ │  CAMPAIGN_AGENT    │ │   │
│  │  │            │ │            │ │            │ │                    │ │   │
│  │  │ Cognitive  │ │ Bio-Fuel   │ │  Spatial   │ │  Goal Decomposer   │ │   │
│  │  │ Supply     │ │ Manager    │ │  Context   │ │  Career Strategist │ │   │
│  │  │ Chain      │ │            │ │  Engine    │ │  Marathon Runner   │ │   │
│  │  └────────────┘ └────────────┘ └────────────┘ └────────────────────┘ │   │
│  │                                                                       │   │
│  │  ┌────────────────────────────────────────────────────────────────┐  │   │
│  │  │              🛡️ SUPERVISOR AGENT (Meta-Controller)              │  │   │
│  │  │  - Drift Detection    - Pressure Index Calculation              │  │   │
│  │  │  - Cross-Agent Sync   - Emergency Interventions                 │  │   │
│  │  └────────────────────────────────────────────────────────────────┘  │   │
│  └───────────────────────────────────────────────────────────────────────┘   │
│                                         │                                    │
│  ┌──────────────────────────────────────▼───────────────────────────────┐   │
│  │                    💾 STATE PERSISTENCE LAYER                         │   │
│  │  ┌────────────────────┐  ┌────────────────────────────────────────┐ │   │
│  │  │  APPWRITE TABLES   │  │  LOCAL AGENT MEMORY (Redis/SQLite)     │ │   │
│  │  │  - users           │  │  - thought_signature_cache             │ │   │
│  │  │  - radius_state    │  │  - marathon_state_machine              │ │   │
│  │  │  - campaign_state  │  │  - policy_episode_buffer               │ │   │
│  │  │  - vitality_state  │  │  - intervention_queue                  │ │   │
│  │  │  - agent_memory    │  │  - websocket_session_pool              │ │   │
│  │  │  - policy_episodes │  │                                        │ │   │
│  │  └────────────────────┘  └────────────────────────────────────────┘ │   │
│  └───────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 📁 New Directory Structure

```
Kaironex-Brain/
├── .env.example                    # Environment template
├── requirements.txt                # Updated dependencies
├── docker-compose.yml              # Local dev with Redis
├── ARCHITECTURE.md                 # This file
│
├── server.py                       # Main FastAPI server entry
│
├── src/
│   ├── __init__.py
│   ├── config.py                   # Enhanced configuration
│   │
│   ├── core/                       # 🆕 Core Framework
│   │   ├── __init__.py
│   │   ├── bicameral_engine.py     # Dual-model reasoning
│   │   ├── thought_manager.py      # Thought signature persistence
│   │   ├── marathon_runner.py      # Long-running agent orchestration
│   │   ├── state_machine.py        # Agent state management
│   │   └── event_bus.py            # Internal pub/sub system
│   │
│   ├── agents/                     # 🔄 Upgraded Agents
│   │   ├── __init__.py
│   │   ├── base_agent.py           # 🆕 Abstract base class
│   │   ├── supervisor.py           # 🔄 Enhanced main_brain
│   │   ├── study_agent.py          # 🔄 Cognitive supply chain
│   │   ├── vitality_agent.py       # 🔄 Bio-fuel manager
│   │   ├── campaign_agent.py       # 🔄 Marathon goal tracker
│   │   └── radius_agent.py         # 🔄 Spatial context engine
│   │
│   ├── tools/                      # 🆕 Agent Tools (Function Calling)
│   │   ├── __init__.py
│   │   ├── schedule_tools.py       # Calendar manipulation
│   │   ├── research_tools.py       # Deep research integration
│   │   ├── notification_tools.py   # Push notification dispatch
│   │   └── resource_tools.py       # Drive/file management
│   │
│   ├── api/                        # 🆕 FastAPI Routes
│   │   ├── __init__.py
│   │   ├── routes.py               # REST endpoints
│   │   ├── websocket.py            # Real-time communication
│   │   └── middleware.py           # Auth, logging, rate limiting
│   │
│   ├── models/                     # 🆕 Pydantic Models
│   │   ├── __init__.py
│   │   ├── user.py
│   │   ├── state.py
│   │   ├── events.py
│   │   └── agent.py
│   │
│   └── utils/
│       ├── __init__.py
│       ├── db_helper.py            # 🔄 Enhanced with batch ops
│       ├── gemini_client.py        # 🔄 Bicameral support
│       ├── cache.py                # 🆕 Redis/local cache
│       └── logger.py               # 🆕 Structured logging
│
└── tests/
    ├── __init__.py
    ├── test_agents.py
    ├── test_marathon.py
    └── test_integration.py
```

---

## 🧠 Core Components Deep Dive

### 1. Bicameral Reasoning Engine

The brain uses two AI models in tandem:

| Component | Model | Purpose | Latency |
|-----------|-------|---------|---------|
| **Reflex Cortex** | Gemini 2.5 Flash | Instant pattern matching, simple responses | <500ms |
| **Deep Cognition** | Gemini 3 Thinking | Complex planning, thought signatures | 2-10s |

**Decision Logic:**
```python
if requires_planning(event) or pressure_index > 70:
    use_deep_cognition()
elif is_simple_query(event):
    use_reflex()
else:
    # Hybrid: Reflex responds, Deep validates
    response = reflex.generate()
    if confidence(response) < 0.8:
        response = deep.validate_and_enhance(response)
```

### 2. Marathon Agent State Machine

Each agent operates as a finite state machine:

```
                    ┌─────────────┐
                    │   DORMANT   │
                    └──────┬──────┘
                           │ trigger_event
                           ▼
                    ┌─────────────┐
        ┌──────────►│   THINKING  │◄──────────┐
        │           └──────┬──────┘           │
        │                  │ thought_ready    │ need_more_context
        │                  ▼                  │
        │           ┌─────────────┐           │
        │           │   ACTING    │───────────┘
        │           └──────┬──────┘
        │                  │ action_complete
        │                  ▼
        │           ┌─────────────┐
        │           │  OBSERVING  │
        │           └──────┬──────┘
        │                  │ observation_processed
        │                  ▼
        │           ┌─────────────┐
        │           │  REFLECTING │
        │           └──────┬──────┘
        │                  │ reflection_stored
        │                  ▼
        │           ┌─────────────┐
        └───────────│   WAITING   │───────► (next cycle or DORMANT)
                    └─────────────┘
```

### 3. Thought Signature Protocol

Every deep reasoning operation produces a cryptographic thought chain:

```json
{
  "thought_id": "ts_7a8b9c",
  "timestamp": "2026-02-01T15:30:00Z",
  "user_id": "user_123",
  "agent": "campaign",
  "signature": {
    "context_hash": "sha256:abc123...",
    "reasoning_trace": [
      "<analyze>User has 3 deadlines in 48 hours. Sleep deficit: 6 hours.</analyze>",
      "<strategy>Prioritize by urgency. Defer low-impact tasks.</strategy>",
      "<decision>Recommend 4-hour focus block on Assignment A.</decision>"
    ],
    "confidence": 0.87,
    "tool_calls": ["schedule_tools.get_deadlines", "vitality_tools.get_sleep_data"]
  },
  "action_output": "Focus on Assignment A for the next 4 hours. I've muted notifications.",
  "parent_signature": "ts_6x7y8z"  // For multi-turn chaining
}
```

---

## 🔌 API Specification

### REST Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/brain/trigger` | Universal event trigger |
| `POST` | `/api/v1/brain/study` | Study session events |
| `POST` | `/api/v1/brain/vitality` | Vitality state updates |
| `POST` | `/api/v1/brain/campaign` | Goal/campaign events |
| `POST` | `/api/v1/brain/radius` | Location/context updates |
| `GET`  | `/api/v1/brain/state/{userId}` | Get full user state |
| `GET`  | `/api/v1/brain/marathon/{userId}` | Get marathon agent status |
| `POST` | `/api/v1/brain/intervention/respond` | User response to intervention |
| `GET`  | `/api/v1/health` | Health check |

### WebSocket Protocol

```
ws://localhost:8000/ws/brain/{userId}

// Client → Server Messages
{
  "type": "voice_input",
  "audio_base64": "...",
  "context": { "current_screen": "campaign" }
}

{
  "type": "state_update",
  "domain": "vitality",
  "data": { "sleep_hours": 6, "steps": 4500 }
}

// Server → Client Messages
{
  "type": "thought_stream",
  "agent": "campaign",
  "thought": "Analyzing your schedule for conflicts...",
  "state": "THINKING"
}

{
  "type": "intervention",
  "intervention_id": "int_xyz",
  "message": "You've been stationary for 3 hours. Quick stretch?",
  "strategy": "WARM",
  "actions": [
    { "id": "accept", "label": "Good idea!" },
    { "id": "snooze", "label": "In 30 mins" },
    { "id": "dismiss", "label": "I'm fine" }
  ]
}

{
  "type": "marathon_update",
  "agent": "campaign",
  "progress": 0.35,
  "current_step": "Researching company background",
  "eta_minutes": 45,
  "thought_signature": "ts_abc123"
}
```

---

## 📊 Database Schema Extensions

### New Tables Required

#### `marathon_sessions`
Tracks long-running agent campaigns:

| Column | Type | Description |
|--------|------|-------------|
| `sessionId` | string (PK) | Unique session identifier |
| `userId` | string (FK) | User reference |
| `agentType` | string | `campaign`, `study`, `research` |
| `goalJson` | string (1M) | Goal definition and decomposition |
| `stateJson` | string (1M) | Current state machine state |
| `thoughtChainJson` | string (1M) | Linked thought signatures |
| `progress` | float | 0.0 - 1.0 completion |
| `status` | string | `RUNNING`, `PAUSED`, `COMPLETE`, `FAILED` |
| `startedAt` | datetime | Session start |
| `lastActiveAt` | datetime | Last activity |
| `estimatedCompletionAt` | datetime | ETA |

#### `thought_signatures`
Persistent thought chain storage:

| Column | Type | Description |
|--------|------|-------------|
| `thoughtId` | string (PK) | Unique thought identifier |
| `userId` | string (FK) | User reference |
| `sessionId` | string (FK) | Marathon session reference |
| `agentType` | string | Originating agent |
| `signatureJson` | string (1M) | Full signature data |
| `parentThoughtId` | string | For chaining |
| `createdAt` | datetime | Timestamp |

---

## 🚀 Implementation Phases

### Phase 1: Core Infrastructure (Week 1)
- [ ] Set up new directory structure
- [ ] Implement `BicameralEngine` with model switching
- [ ] Create `ThoughtManager` for signature persistence
- [ ] Build base `MarathonAgent` class
- [ ] Set up FastAPI server with WebSocket support

### Phase 2: Agent Upgrades (Week 2)
- [ ] Refactor all agents to extend `BaseAgent`
- [ ] Implement state machine logic in each agent
- [ ] Add thought signature generation to deep reasoning calls
- [ ] Create agent-specific tool definitions

### Phase 3: Marathon Mode (Week 3)
- [ ] Implement `MarathonRunner` orchestrator
- [ ] Add background task scheduling (APScheduler/Celery)
- [ ] Create marathon session management
- [ ] Build progress streaming via WebSocket

### Phase 4: Integration & Polish (Week 4)
- [ ] End-to-end testing with Kotlin client
- [ ] Performance optimization
- [ ] Monitoring and logging
- [ ] Documentation

---

## 🔐 Security Considerations

1. **API Key Management**: All secrets via environment variables
2. **WebSocket Authentication**: JWT-based session tokens
3. **Rate Limiting**: Per-user request quotas
4. **Input Sanitization**: All LLM inputs sanitized
5. **Medical Content Filter**: Vitality agent strictly uses gamified language

---

## 📈 Metrics & Monitoring

| Metric | Description |
|--------|-------------|
| `brain_latency_ms` | Response time per agent |
| `thought_depth` | Average reasoning steps |
| `marathon_completion_rate` | % of marathons completed |
| `intervention_acceptance_rate` | User response to nudges |
| `pressure_index_avg` | System-wide stress indicator |

---

## 🎯 Success Criteria

- [ ] Sub-500ms reflex responses
- [ ] Marathon agents persist across server restarts
- [ ] 100% thought signature integrity (no orphaned chains)
- [ ] Real-time WebSocket latency <100ms
- [ ] Zero medical terminology in Vitality outputs
