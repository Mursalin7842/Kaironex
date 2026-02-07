# 🧠 KAIRONEX STUDY SYSTEM - Complete Architecture
## Gemini 3 Hackathon Battle Plan

**Last Updated:** February 7, 2026  
**Deadline:** February 10, 2026 (3 days!)  
**Target:** $100,000 Grand Prize

---

## 🎯 EXECUTIVE SUMMARY

The Kaironex Study System is a **Cognitive Supply Chain** that:
1. **Ingests** academic resources (syllabus, textbooks, lecture notes)
2. **Strategizes** semester-long learning plans with monthly milestones
3. **Schedules** daily tasks with rich, actionable breakdown
4. **Prepares** content BEFORE the student needs it
5. **Delivers** personalized learning materials (video, audio, flashcards)
6. **Adapts** based on mastery verification (Gatekeeper Quizzes)

---

## 🏗️ SYSTEM ARCHITECTURE

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        KAIRONEX STUDY SYSTEM                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐  │
│  │   INGEST    │───▶│  STRATEGY   │───▶│  TACTICAL   │───▶│  DELIVERY   │  │
│  │   LAYER     │    │   LAYER     │    │   LAYER     │    │   LAYER     │  │
│  └─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘  │
│        │                  │                  │                  │           │
│        ▼                  ▼                  ▼                  ▼           │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                      APPWRITE DATABASE                              │   │
│  │  [resources] [monthly_plans] [schedule] [mastery_scores] [content]  │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                   GEMINI 3 BICAMERAL ENGINE                         │   │
│  │     [Flash/Reflex Mode]  ←──Bridge──▶  [Pro/Deep Mode]              │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 📊 DATA FLOW DIAGRAM

```
STUDENT UPLOADS RESOURCES
         │
         ▼
┌─────────────────────────────────────┐
│  1. RESOURCE INGESTION              │
│  ─────────────────────────────────  │
│  • PDF text extraction (pypdf)      │
│  • Syllabus parsing (topics, dates) │
│  • Save to resources collection     │
│  • Trigger: study_agent             │
└──────────────────┬──────────────────┘
                   │
                   ▼
┌─────────────────────────────────────┐
│  2. CONSTITUTION EXTRACTION         │
│  ─────────────────────────────────  │
│  • Job schedule (blocked times)     │
│  • Prayer times (religious needs)   │
│  • Energy pattern (night owl/early) │
│  • Learning pace (slow/standard)    │
│  • Weakness defense (distractions)  │
└──────────────────┬──────────────────┘
                   │
                   ▼
┌─────────────────────────────────────┐
│  3. STRATEGY LAYER (Layer 1)        │
│  ─────────────────────────────────  │
│  • Extract semester timeline        │
│  • Generate N monthly_plans         │
│  • Each plan has:                   │
│    - goals_context (what to learn)  │
│    - dos_donts (user-specific tips) │
│    - critical_focus (priority)      │
│    - risk_factors (what to avoid)   │
│  • Save to monthly_plans collection │
└──────────────────┬──────────────────┘
                   │
                   ▼
┌─────────────────────────────────────┐
│  4. TACTICAL LAYER (Layer 2)        │
│  ─────────────────────────────────  │
│  • Generate daily tasks for Month 1 │
│  • RICH TASK FORMAT includes:       │
│    - learning_objectives            │
│    - topics (4-phase breakdown)     │
│    - resource_hints (for search)    │
│    - prerequisites                  │
│    - deliverables                   │
│    - verification (quiz config)     │
│    - content_mode (deep/travel/cram)│
│    - difficulty (beginner/adv)      │
│  • Save to schedule collection      │
└──────────────────┬──────────────────┘
                   │
                   ▼
┌─────────────────────────────────────┐
│  5. PROACTIVE CONTENT ENGINE        │
│  ─────────────────────────────────  │
│  • Analyzes upcoming 24hr schedule  │
│  • Prepares content 30min BEFORE:   │
│    - STUDY: summaries, flashcards,  │
│             problems, warmup quiz   │
│    - TRAVEL: commute audio, podcast │
│    - PRE-EXAM: revision audio,      │
│               critical flashcards   │
│    - EXERCISE: gym-friendly audio   │
│  • Uses resource_hints for search   │
│  • Saves to prepared_content        │
└──────────────────┬──────────────────┘
                   │
                   ▼
┌─────────────────────────────────────┐
│  6. CONTENT DELIVERY                │
│  ─────────────────────────────────  │
│  • App shows "Your content is ready"│
│  • Modes:                           │
│    - DEEP_DIVE: videos + articles   │
│    - TRAVEL: audio summaries        │
│    - CRAM: flashcards + key points  │
│    - PRACTICE: quizzes + exercises  │
│  • Track delivery (delivered_at)    │
└──────────────────┬──────────────────┘
                   │
                   ▼
┌─────────────────────────────────────┐
│  7. MASTERY VERIFICATION            │
│  ─────────────────────────────────  │
│  • Gatekeeper Quiz at end of task   │
│  • Pass threshold (default 80%)     │
│  • Updates mastery_scores           │
│  • Feeds back to Content Engine:    │
│    - Low mastery → more flashcards  │
│    - High mastery → harder content  │
└─────────────────────────────────────┘
```

---

## 📝 RICH TASK FORMAT SPECIFICATION

### Example Task (Full Structure)
```json
{
  "startTime": "09:00",
  "endTime": "11:00",
  "title": "Deep Work: CS450 Process Management",
  "type": "study",
  "priority": 8,
  "location": "Library",
  "subject": "CS450 Operating Systems",
  "difficulty": "intermediate",
  "content_mode": "deep_dive",
  
  "learning_objectives": [
    "Define process vs thread and explain key differences",
    "Diagram the 5-state process model with transitions",
    "Implement a simple process scheduler in pseudocode"
  ],
  
  "topics": "PHASE 1 - INPUT (25 min):\n- Read Chapter 1: Process Concepts (pp. 1-15)\n- Watch: Process States Video (search: process scheduling tutorial)\n\nPHASE 2 - PROCESS (45 min):\n- Create concept map: Process vs Thread\n- Draw 5-state diagram from memory\n- Annotate with real-world examples\n\nPHASE 3 - OUTPUT (30 min):\n- Solve Problems 1.1-1.5 (process state transitions)\n- Write pseudocode for Round Robin scheduler\n- Self-quiz on key definitions\n\nPHASE 4 - VERIFY (20 min):\n- Complete Gatekeeper Quiz (80% threshold)\n- Document 3 key insights in notes",
  
  "resource_hints": [
    "process scheduling",
    "thread vs process",
    "5-state process model",
    "round robin scheduler"
  ],
  
  "prerequisites": [
    "Basic programming knowledge",
    "Understanding of CPU execution"
  ],
  
  "deliverables": [
    "Concept map PDF",
    "5-state diagram",
    "Problems 1.1-1.5 solutions"
  ],
  
  "verification": {
    "type": "quiz",
    "pass_threshold": 0.8,
    "topics_covered": ["process states", "thread concepts", "scheduling basics"]
  }
}
```

### Content Modes
| Mode | Content Types | Use Case |
|------|--------------|----------|
| `deep_dive` | Video tutorials, comprehensive articles, visualizations | Standard study sessions |
| `travel` | Audio summaries, podcast-style content | Commute, walking |
| `cram` | Flashcards, key points, quick revision | Before exams, review |
| `practice` | Quizzes, exercises, problem sets | Skill reinforcement |

---

## 🔗 INTEGRATION POINTS

### 1. Study Agent → Proactive Content Engine
```python
# study_agent.py creates task with metadata
task = {
    'topics': "...",
    'resource_hints': ["keyword1", "keyword2"],
    'content_mode': "deep_dive",
    'metadata_json': json.dumps({
        'learning_objectives': [...],
        'verification': {...}
    })
}

# proactive_content_engine.py reads task
async def prepare_for_task(task):
    hints = json.loads(task['metadata_json']).get('resource_hints')
    content = await search_and_prepare(hints, task['content_mode'])
    return content
```

### 2. Content Engine → Content Delivery
```python
# Content prepared 30 min before task
prepared = PreparedContent(
    context=AnticipatedContext.STUDY_SESSION,
    scheduled_for=task_start - timedelta(minutes=30),
    content_data={
        'summaries': {...},
        'flashcards': [...],
        'warmup_quiz': [...]
    }
)

# App fetches when task approaches
ready_content = engine.get_ready_content(user_id, context)
```

### 3. Gatekeeper → Mastery Feedback
```python
# After quiz completion
mastery_score = quiz_result.score
db.update_mastery(user_id, topic, mastery_score)

# Content Engine uses mastery for prioritization
weak_topics = [t for t in topics if mastery[t] < 70]
# Generate more flashcards for weak topics
```

---

## 📱 KAIRONEX APP INTEGRATION

### Schedule Display
```kotlin
// ScheduleRepository.kt
@Serializable
data class ScheduleTask(
    val id: String,
    val title: String,
    val startTime: String,
    val endTime: String,
    val type: String,
    val topics: String?,           // Rich breakdown
    val subject: String?,          // Course name
    val difficulty: String?,       // beginner/intermediate/advanced
    val contentMode: String?,      // deep_dive/travel/cram/practice
    val metadataJson: String?      // Full JSON metadata
)

// Parse metadata for UI
val metadata = json.decodeFromString<TaskMetadata>(task.metadataJson)
val objectives = metadata.learningObjectives
val deliverables = metadata.deliverables
```

### Study Room Integration
- Display `topics` breakdown in phases
- Show `learning_objectives` as checklist
- Track `deliverables` completion
- Trigger `verification` quiz at end

---

## 🚀 HACKATHON PRIORITIES (3 Days Left!)

### Day 1 (Feb 7): Core Pipeline ✅
- [x] Enhanced task format in study_agent.py
- [x] Updated ScheduleRepository with new fields
- [x] Updated db_helper with metadata handling
- [x] Updated blueprint documentation

### Day 2 (Feb 8): Content Integration
- [ ] Wire proactive_content_engine to read new task format
- [ ] Implement content search using resource_hints
- [ ] Test full pipeline: upload → schedule → content ready
- [ ] Connect to Gemini 3 for actual content generation

### Day 3 (Feb 9): Polish & Demo
- [ ] Record 3-minute demo video
- [ ] Ensure public AI Studio link works
- [ ] Clean up codebase for submission
- [ ] Write 200-word Gemini integration description

---

## 🏆 HACKATHON JUDGE APPEAL

### Why This Wins:

**Technical Execution (40%)**
- Bicameral Engine (Gemini 2.5 + Gemini 3 orchestration)
- Two-layer hierarchical planning (Strategy + Tactical)
- Rich task metadata enabling autonomous content preparation
- Marathon agents for long-running tasks

**Innovation/Wow Factor (30%)**
- "Cognitive Supply Chain" - content ready BEFORE student needs it
- User Constitution - AI respects immutable life constraints
- 4-Phase Learning (INPUT → PROCESS → OUTPUT → VERIFY)
- Travel mode - learn during commute with auto-generated audio

**Potential Impact (20%)**
- Addresses THE core problem: "Life interrupting study"
- Hyper-personalization based on job, religion, health
- Scalable to millions of students worldwide

**Presentation (10%)**
- Clean architecture documentation
- Clear demo showing full pipeline
- Visible thought signatures for transparency

---

## 📄 FILES MODIFIED

| File | Changes |
|------|---------|
| `src/agents/study_agent.py` | Enhanced task format with learning_objectives, resource_hints, verification, etc. |
| `src/utils/db_helper.py` | Added handling for new task metadata fields |
| `KAIRONEX_APP_BLUEPRINT.md` | Updated schedule schema with new columns |
| `Kaironex/...ScheduleRepository.kt` | Added new fields to ScheduleTask model |

---

## 🔧 REQUIRED APPWRITE SCHEMA UPDATES

The following columns need to be added to the `schedule` collection:

```
| Column        | Type   | Size     |
|---------------|--------|----------|
| topics        | string | 10000    |
| subject       | string | 255      |
| difficulty    | string | 50       |
| content_mode  | string | 50       |
| metadata_json | string | 100000   |
```

Run this in Appwrite Console or via API.

---

**LET'S WIN THIS! 🚀**
