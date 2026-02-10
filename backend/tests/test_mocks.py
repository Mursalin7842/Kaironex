"""
🧪 SHARED TEST MOCKS
=====================
Reusable mock classes for all agent brain tests.
No external services needed - tests run fully offline.
"""

import json
import datetime
import uuid


# =============================================================================
# MOCK APPWRITE CONTEXT
# =============================================================================

class MockRes:
    """Mock Appwrite Functions response."""
    def json(self, data, status=200):
        return {"status": status, "data": data}


class MockReq:
    """Mock Appwrite Functions request."""
    def __init__(self, path="/", method="POST", body=None):
        self.path = path
        self.method = method
        self.body = body or {}
        self.query = {}


class MockContext:
    """Mock Appwrite Functions context."""
    def __init__(self, path="/", method="POST", body=None):
        self.req = MockReq(path, method, body)
        self.res = MockRes()
        self._logs = []

    def log(self, msg):
        self._logs.append(f"[LOG] {msg}")
        print(f"  [LOG] {msg}")

    def error(self, msg):
        self._logs.append(f"[ERR] {msg}")
        print(f"  [ERR] {msg}")


# =============================================================================
# MOCK DB HELPER
# =============================================================================

class MockDBHelper:
    """
    Mock database helper that stores everything in memory.
    Mimics KairoDB interface without Appwrite connection.
    """

    def __init__(self):
        self._heartbeats = []
        self._thought_signatures = []
        self._agent_memories = {}
        self._interventions = []
        self._state_cache = {}
        self._user_docs = {}
        self._marathon_sessions = []
        self._policy_episodes = []
        self._campaign_state = {}
        self._resources = []
        self._schedules = {}
        self._monthly_plans = {}

    # -- Heartbeat --
    def log_heartbeat(self, user_id, source_details, thought_signature=None, reasoning_mode=None, session_id=None):
        self._heartbeats.append({
            "user_id": user_id,
            "source": source_details,
            "time": datetime.datetime.now().isoformat()
        })

    # -- Agent Memory --
    def update_agent_memory_full(self, user_id, thought_sig_dict=None, active_agents=None,
                                  pressure_index=None, session_id=None, reasoning_mode=None):
        self._agent_memories[user_id] = {
            "thought_sig": thought_sig_dict,
            "active_agents": active_agents,
            "pressure_index": pressure_index,
            "reasoning_mode": reasoning_mode,
            "session_id": session_id
        }

    # -- Thought Signatures --
    def create_thought_signature(self, user_id, thought_data):
        thought_data["user_id"] = user_id
        self._thought_signatures.append(thought_data)

    def get_latest_thought(self, user_id, agent=None):
        for t in reversed(self._thought_signatures):
            if t.get("user_id") == user_id:
                if agent is None or t.get("agent") == agent:
                    return t
        return None

    # -- Interventions --
    def create_intervention(self, user_id, trigger, message, status="PENDING", strategy="NEUTRAL"):
        intervention = {
            "id": f"int_{uuid.uuid4().hex[:8]}",
            "user_id": user_id,
            "trigger": trigger,
            "message": message,
            "status": status,
            "strategy": strategy,
            "time": datetime.datetime.now().isoformat()
        }
        self._interventions.append(intervention)
        return intervention["id"]

    # -- State Cache --
    def update_state_cache(self, user_id, specific_state_data):
        if user_id not in self._state_cache:
            self._state_cache[user_id] = {}
        self._state_cache[user_id].update(specific_state_data)

        # Also update user doc's studentState_json so _get_cultural_state / _get_survival_state can read it
        if user_id not in self._user_docs:
            self._user_docs[user_id] = {"userId": user_id, "studentState_json": "{}", "onboarding_json": "{}"}
        try:
            existing = json.loads(self._user_docs[user_id].get("studentState_json", "{}"))
            existing.update(specific_state_data)
            self._user_docs[user_id]["studentState_json"] = json.dumps(existing)
        except:
            pass

    # -- User Documents --
    def get_user_doc(self, user_id):
        return self._user_docs.get(user_id, {
            "userId": user_id,
            "studentState_json": "{}",
            "onboarding_json": "{}"
        })

    def set_user_doc(self, user_id, doc):
        """Test helper to preload user data."""
        self._user_docs[user_id] = doc

    # -- Marathon Sessions --
    def create_marathon_session(self, session_data):
        self._marathon_sessions.append(session_data)

    def update_marathon_session(self, session_id, updates):
        for s in self._marathon_sessions:
            if s.get("session_id") == session_id:
                s.update(updates)
                return

    def get_marathon_session(self, session_id):
        for s in self._marathon_sessions:
            if s.get("session_id") == session_id:
                return s
        return None

    def get_user_marathons(self, user_id, status=None):
        return [s for s in self._marathon_sessions if s.get("user_id") == user_id]

    # -- Policy Episodes --
    def create_policy_episode(self, episode_data):
        self._policy_episodes.append(episode_data)

    def get_policy_episodes(self, user_id, agent=None, limit=100):
        eps = [e for e in self._policy_episodes if e.get("user_id") == user_id]
        if agent:
            eps = [e for e in eps if e.get("agent") == agent]
        return eps[:limit]

    # -- Campaign State --
    def update_campaign_state(self, user_id, campaign_data):
        self._campaign_state[user_id] = campaign_data

    def get_campaign_state(self, user_id):
        return self._campaign_state.get(user_id, {})

    # -- Resources --
    def count_user_resources(self, user_id):
        return len([r for r in self._resources if r.get("user_id") == user_id])

    def get_user_resources(self, user_id, limit=20):
        return [r for r in self._resources if r.get("user_id") == user_id][:limit]

    # -- Schedule --
    def create_schedule_task(self, task_data):
        user_id = task_data.get("userId", "unknown")
        if user_id not in self._schedules:
            self._schedules[user_id] = []
        self._schedules[user_id].append(task_data)

    def batch_create_schedule_tasks(self, tasks):
        for t in tasks:
            self.create_schedule_task(t)
        return len(tasks)

    def get_schedule(self, user_id, limit=1000):
        return self._schedules.get(user_id, [])[:limit]

    # -- Monthly Plans --
    def create_monthly_plan(self, user_id, plan_data):
        if user_id not in self._monthly_plans:
            self._monthly_plans[user_id] = []
        self._monthly_plans[user_id].append(plan_data)

    def get_monthly_plans(self, user_id):
        return self._monthly_plans.get(user_id, [])

    # -- DB property (for agents that access self.db directly) --
    @property
    def db(self):
        return self

    # -- Direct Appwrite SDK-like methods (used by supervisor/campaign) --
    def list_rows(self, database_id=None, table_id=None, queries=None):
        """Mock Appwrite list_rows for supervisor."""
        class MockListResult:
            def __init__(self, documents):
                self.documents = documents
                self.total = len(documents)
        # Return agent memory entries as documents
        docs = []
        for uid, mem in self._agent_memories.items():
            docs.append({"userId": uid, "$id": f"mem_{uid}", **mem})
        return MockListResult(docs)

    def get_document(self, database_id=None, collection_id=None, document_id=None):
        """Mock Appwrite get_document."""
        return self._user_docs.get(document_id, {"userId": document_id, "studentState_json": "{}", "onboarding_json": "{}"})

    def update_document(self, database_id=None, collection_id=None, document_id=None, data=None):
        """Mock Appwrite update_document."""
        if document_id in self._user_docs:
            self._user_docs[document_id].update(data or {})
        else:
            self._user_docs[document_id] = data or {}
        return self._user_docs[document_id]

    def list_documents(self, database_id=None, collection_id=None, queries=None):
        """Mock Appwrite list_documents."""
        class MockListResult:
            def __init__(self, documents):
                self.documents = documents
                self.total = len(documents)
        return MockListResult(list(self._user_docs.values()))


# =============================================================================
# MOCK GEMINI CLIENT
# =============================================================================

_MOCK_RESPONSES = {
    "default": "Mock AI response for testing.",
    "welcome": "Welcome! I'll help you adapt to your new cultural environment.",
    "lesson": '{"title":"Polite Greetings","content":"Today we learn greetings.","practice_scenario":"You meet a professor.","key_phrases":["Good morning - Formal greeting","Thank you - Expression of gratitude"],"cultural_tip":"In the US, a firm handshake is common.","difficulty":"intermediate"}',
    "scenario": '{"scenario_id":"sc_001","title":"Coffee Shop Order","setting":"Local coffee shop","characters":["Barista","Student"],"cultural_challenge":"Tipping etiquette","expected_behavior":"Tip 15-20%","common_mistakes":["Not tipping","Counting change loudly"],"dialogue":[{"speaker":"Barista","line":"Hi, what can I get you?"},{"speaker":"Student","line":"I would like a latte, please."}],"key_vocabulary":[{"word":"latte","meaning":"espresso with steamed milk"}],"tips":"Smile and be polite!"}',
    "live_prompt": '{"live_agent_system_prompt":"You are a cultural coach helping an international student.","opening_line":"Hi! Let us practice some scenarios.","key_teaching_points":["Greetings","Small talk","Polite requests"],"practice_dialogue":"Ordering at a restaurant.","success_criteria":"Student completes the order confidently."}',
    "scan": '{"area_name":"Campus Library","safety_rating":9,"overview":"Safe student area.","useful_services":[{"name":"Info Desk","type":"help","tip":"Ask anything"}],"cultural_norms":["Silence expected","Food not allowed"],"emergency_info":"Campus security: ext 5555","quick_tips":["Use whisper voice","Reserve study rooms online"],"student_friendly":true}',
    "meal_plan": '{"meals":[{"name":"Oatmeal Bowl","type":"breakfast","cost":2.50},{"name":"Rice & Beans","type":"lunch","cost":4.00},{"name":"Pasta Primavera","type":"dinner","cost":5.50}],"total_cost":12.00}',
    "morning": "Good morning! Day 3 of your cultural journey. Todays lesson focuses on polite greetings in academic settings. Remember, a simple How are you goes a long way! Lets make today count!",
    "celebration": "Fantastic work completing today's lesson! Now go practice those phrases in real life.",
    "mode": "Deep Focus mode activated. Notifications minimized.",
    "emergency": "Stay calm. Say: 'I need help, please.' Steps: 1) Find a staff member 2) Show your student ID 3) Explain your situation simply.",
    "budget": "Today's budget: $15.00. You're on track with savings.",
    "savings": "Emergency fund: $150. Savings goal: $500. Auto-save: 10%.",
    "study_focus": "Focus boost activated. Let's stay on track.",
    "quiz": '{"question":"What is O(n)?","options":["Linear time complexity","Constant time","Quadratic time"],"correct":"Linear time complexity"}',
    "resume": '{"resume":{"professional_summary":"Experienced engineer with strong Python skills.","skills_section":["Python","React","AWS"],"experience_section":[{"title":"Software Engineer","organization":"Tech Corp","duration":"2023-2024","bullets":["Built REST APIs serving 10K requests/day"]}],"projects_section":[{"name":"AI Project","technologies":["Python","ML"],"description":"ML pipeline","achievements":["95% accuracy"]}],"education_section":{"degree":"BS Computer Science","institution":"State University","year":"2024","relevant_coursework":["Algorithms","ML"]}},"suggestions":[{"category":"skills","suggestion":"Add cloud certs","priority":"HIGH"}],"skill_gaps":[{"skill":"Kubernetes","importance":"MEDIUM","learning_path":"2 weeks"}],"interview_prep_topics":["REST API design","ML basics"],"ats_optimization_notes":"Optimized for 12 keywords","estimated_ats_score":82}',
    "interview": '{"interview_id":"int_001","duration_minutes":25,"interviewer":{"name":"Dr. Patel","role":"Senior Engineer","company":"TechCorp","personality":"Professional"},"question_bank":[{"id":"q1","category":"icebreaker","question":"Tell me about yourself.","follow_ups":["Why this role?"],"good_answer_criteria":["Shows passion"],"red_flags":["Too generic"]}],"gemini_live_prompt":"You are Dr. Patel interviewing a candidate.","difficulty_modifiers":{"current":"medium","instruction":"Be professional."},"post_interview_rubric":{"technical_competency":"1-10","communication":"1-10","overall":"1-10"},"candidate_prep_notes":["Prepare STAR stories"]}',
    "ats_analysis": '{"ats_score":78,"score_breakdown":{"keyword_match":72,"experience_fit":85,"format_structure":80,"quantification":65,"action_verbs":90},"matched_keywords":["Python","API"],"missing_keywords":["AWS"],"critical_improvements":[{"issue":"Missing cloud","current":"No cloud","suggested":"Add AWS","priority":"HIGH"}],"bullet_improvements":[{"original":"Worked on systems","improved":"Engineered APIs serving 10K requests","reason":"Quantification"}],"checklist":[{"item":"Add AWS cert","status":"pending","impact":"HIGH"}],"overall_assessment":"Strong technical foundation.","interview_ready":false,"estimated_pass_rate":"60%"}',
    "skill_tree": '{"skill_tree":[{"id":"skill_1","name":"Python","level":1,"status":"unlocked","description":"Core Python","parent":null,"xpCost":200,"icon":"🐍","learning_resources":[],"verification":"Build CLI tool"}],"recommended_path":["skill_1"],"time_to_job_ready":"3 months","weekly_commitment":"15 hours","milestones":[{"week":4,"title":"Fundamentals","skills":["skill_1"]}]}',
}


def _get_mock_response(prompt: str) -> str:
    """Pick a mock response based on prompt keywords.
    
    Order matters! Most specific patterns first, broadest last.
    """
    prompt_lower = prompt.lower()

    # === CAMPAIGN-SPECIFIC (most specific first) ===
    # "tailored" only appears in generate_tailored_resume, check BEFORE ats
    if "tailor" in prompt_lower and "resume" in prompt_lower:
        return _MOCK_RESPONSES["resume"]
    if "ats" in prompt_lower and ("resume" in prompt_lower or "analyz" in prompt_lower):
        return _MOCK_RESPONSES["ats_analysis"]
    if "skill tree" in prompt_lower or "skill_tree" in prompt_lower or ("career" in prompt_lower and "skill" in prompt_lower):
        return _MOCK_RESPONSES["skill_tree"]
    if "interview" in prompt_lower and ("design" in prompt_lower or "question" in prompt_lower or "mock" in prompt_lower or "interviewer" in prompt_lower):
        return _MOCK_RESPONSES["interview"]
    if "resume" in prompt_lower and ("generat" in prompt_lower or "write" in prompt_lower or "creat" in prompt_lower):
        return _MOCK_RESPONSES["resume"]

    # === STUDY-SPECIFIC ===
    if "quiz" in prompt_lower or "multiple choice" in prompt_lower or "question about" in prompt_lower:
        return _MOCK_RESPONSES["quiz"]

    # === RADIUS CULTURAL ===
    if "welcome" in prompt_lower and "cultural" in prompt_lower:
        return _MOCK_RESPONSES["welcome"]
    if "micro-lesson" in prompt_lower or ("lesson" in prompt_lower and ("generat" in prompt_lower or "daily" in prompt_lower)):
        return _MOCK_RESPONSES["lesson"]
    if "cultural" in prompt_lower and "scenario" in prompt_lower:
        return _MOCK_RESPONSES["scenario"]
    if "live" in prompt_lower and ("teaching" in prompt_lower or "agent" in prompt_lower) and "prompt" in prompt_lower:
        return _MOCK_RESPONSES["live_prompt"]
    if ("local" in prompt_lower or "area" in prompt_lower) and "scan" in prompt_lower:
        return _MOCK_RESPONSES["scan"]

    # === VITALITY ===
    if "meal" in prompt_lower and ("plan" in prompt_lower or "option" in prompt_lower):
        return _MOCK_RESPONSES["meal_plan"]
    if "marathon" in prompt_lower and "morning" in prompt_lower:
        return _MOCK_RESPONSES["morning"]
    if "morning" in prompt_lower and ("briefing" in prompt_lower or "ritual" in prompt_lower or "cultural" in prompt_lower):
        return _MOCK_RESPONSES["morning"]
    if "celebrat" in prompt_lower or ("completed" in prompt_lower and "lesson" in prompt_lower):
        return _MOCK_RESPONSES["celebration"]
    if "mode" in prompt_lower and "activat" in prompt_lower:
        return _MOCK_RESPONSES["mode"]
    if "emergency" in prompt_lower:
        return _MOCK_RESPONSES["emergency"]
    if "budget" in prompt_lower:
        return _MOCK_RESPONSES["budget"]
    if "saving" in prompt_lower or ("fund" in prompt_lower and "emergency" not in prompt_lower):
        return _MOCK_RESPONSES["savings"]

    # === GENERIC FALLBACKS ===
    if "setup" in prompt_lower or "welcome" in prompt_lower:
        return _MOCK_RESPONSES["welcome"]
    if "focus" in prompt_lower and "lost" in prompt_lower:
        return _MOCK_RESPONSES["study_focus"]
    if "focus" in prompt_lower or "study" in prompt_lower:
        return _MOCK_RESPONSES["study_focus"]
    if "lesson" in prompt_lower:
        return _MOCK_RESPONSES["lesson"]
    if "scenario" in prompt_lower:
        return _MOCK_RESPONSES["scenario"]
    if "scan" in prompt_lower:
        return _MOCK_RESPONSES["scan"]
    if "resume" in prompt_lower:
        return _MOCK_RESPONSES["resume"]
    if "interview" in prompt_lower:
        return _MOCK_RESPONSES["interview"]
    if "morning" in prompt_lower:
        return _MOCK_RESPONSES["morning"]
    if "goal" in prompt_lower:
        return _MOCK_RESPONSES["study_focus"]

    return _MOCK_RESPONSES["default"]


class MockGeminiClient:
    """Mock GeminiClient that returns deterministic responses."""

    def __init__(self, *args, **kwargs):
        pass

    def generate_response(self, prompt, json_mode=False, *args, **kwargs):
        return _get_mock_response(prompt)

    def generate_with_thinking(self, prompt, thinking_budget=8192, json_mode=False, *args, **kwargs):
        return _get_mock_response(prompt), "Mock thinking trace"

    def generate_multimodal(self, prompt, parts=None, json_mode=False, *args, **kwargs):
        return _get_mock_response(prompt)

    def generate_response_with_image(self, prompt, image_data=None, *args, **kwargs):
        """Mock for fridge scan and image-based analysis."""
        return _get_mock_response(prompt)


# =============================================================================
# MOCK BICAMERAL ENGINE (for Campaign Brain)
# =============================================================================

class MockReasoningResult:
    """Mock result from BicameralEngine.reason()."""
    def __init__(self, response_text):
        self.content = response_text
        self.mode_used = "DEEP"
        self.reasoning_trace = "Mock reasoning trace"
        self.model_used = "gemini-3-flash-preview"
        self.thinking_tokens = 100
        self.thought_signature = None
        self.latency_ms = 50.0
        self.tool_results = []
        self.confidence = 0.9
        self.thinking_content = "Mock thinking"


class MockBicameralEngine:
    """Mock BicameralEngine for campaign brain tests."""

    def __init__(self, *args, **kwargs):
        pass

    async def reason(self, request, *args, **kwargs):
        prompt = getattr(request, 'prompt', '') or ''
        return MockReasoningResult(_get_mock_response(prompt))


# =============================================================================
# MOCK APPWRITE QUERY (for Supervisor)
# =============================================================================

class MockQuery:
    """Mock Appwrite Query class."""
    @staticmethod
    def limit(n):
        return f"limit({n})"

    @staticmethod
    def equal(field, value):
        return f"equal({field},{value})"
