import json
import datetime
import hashlib
import uuid
from ..utils.gemini_client import GeminiClient
from ..config import *

def _create_thought_signature(user_id, agent, prompt, response, db_helper, context=None):
    """Create and store thought signature - shared helper."""
    try:
        thought_id = f"thought_{uuid.uuid4().hex[:12]}"
        timestamp = datetime.datetime.now().isoformat()
        context_hash = hashlib.sha256(f"{user_id}:{prompt[:200]}:{timestamp}".encode()).hexdigest()[:16]
        
        thought_data = {
            "thought_id": thought_id, "timestamp": timestamp, "agent": agent,
            "context_hash": context_hash,
            "reasoning_trace": [f"Prompt: {prompt[:100]}...", f"Response: {response[:200]}..."],
            "confidence": 0.85, "tool_calls": [], "action_output": response[:500], "parent_signature": ""
        }
        
        db_helper.create_thought_signature(user_id, thought_data)
        db_helper.update_agent_memory_full(user_id, thought_sig_dict=thought_data, active_agents=agent, reasoning_mode='DEEP')
        
        if context: context.log(f"🧠 Thought signature stored: {thought_id}")
        return thought_id
    except Exception as e:
        print(f"❌ Thought signature error: {e}")
        return None

def run_campaign_agent(db_helper, payload, context):
    """
    Career Strategist: Goals & Schedule.
    """
    user_id = payload.get('userId')
    if not user_id: return context.res.json({"error": "No userId"})

    context.log(f"⚔️ Campaign Brain processing for {user_id}")
    db_helper.log_heartbeat(user_id, f"EVENT:CAMPAIGN | {payload.get('type')}")
    
    ai = GeminiClient()
    event_type = payload.get('type')
    
    campaign_data = {}
    response_actions = []

    if event_type == 'new_goal':
        goal_title = payload.get('goal_title', 'New Project')
        
        prompt = f"User set a goal: {goal_title}. Give 1 strategic first step."
        strategy = ai.generate_response(prompt)
        
        # Create thought signature
        thought_id = _create_thought_signature(user_id, "campaign", prompt, strategy, db_helper, context)
        if thought_id:
            response_actions.append(f"thought:{thought_id}")
        
        db_helper.create_intervention(
            user_id, "NEW_GOAL", strategy, strategy="PROACTIVE"
        )
        response_actions.append("intervention_created")
        
        campaign_data["active_quest"] = goal_title
        campaign_data["current_strategy"] = strategy

    elif event_type == 'schedule_update':
        campaign_data["schedule_status"] = "Optimized"

    elif event_type == 'campaign_calibration':
        # CALIBRATION LOGIC
        # 1. Generate Skill Tree
        target_role = payload.get('data', {}).get('targetRole', 'General Tech')
        skills = payload.get('data', {}).get('skills', 'Python, Java')
        
        prompt_skills = f"""
        Act as a Senior Career Strategist. Create a Skill Tree for a student aiming for '{target_role}' with current skills: {skills}.
        Return ONLY a JSON array of objects. No markdown.
        Schema: [{{ "id": "s1", "name": "Skill Name", "level": 1, "status": "unlocked", "description": "Short desc", "parent": null, "xpCost": 100, "icon": "📚" }}]
        Generate 10-20 distinct nodes (Foundation to Advanced).
        """
        try:
            skill_json_str = ai.generate_response(prompt_skills).strip().replace("```json", "").replace("```", "")
            # Validate JSON
            json.loads(skill_json_str) 
            campaign_data["skill_tree_json"] = skill_json_str
            response_actions.append("skill_tree_generated")
        except Exception as e:
            context.log(f"⚠️ Skill Tree Gen Error: {e}")
            
        # 2. Generate Quest Board
        prompt_quests = f"""
        Create 3 starter quests for a student targeting '{target_role}'.
        Return ONLY a JSON array of objects. No markdown.
        Schema: [{{ "id": "q1", "title": "Quest Title", "type": "SKILL", "description": "Actionable task", "xp": 50, "status": "active", "tags": ["Strategy"] }}]
        Types: SKILL, APPLICATION, NETWORKING.
        """
        try:
            quest_json_str = ai.generate_response(prompt_quests).strip().replace("```json", "").replace("```", "")
            json.loads(quest_json_str)
            campaign_data["quest_board_json"] = quest_json_str
            response_actions.append("quest_board_generated")
        except Exception as e:
            context.log(f"⚠️ Quest Gen Error: {e}")

        # 3. Mark as Calibrated
        campaign_data["is_calibrated"] = True
        
        # 4. Thought Signature
        _create_thought_signature(user_id, "campaign", f"Calibrate for {target_role}", "Generated initial Skill Tree and Quests.", db_helper, context)

    # --- SYNC: UPDATE CACHE ---
    if campaign_data:
        update_payload = {"campaign": campaign_data}
        db_helper.update_state_cache(user_id, update_payload)

def scan_daily_jobs(db_helper, user_id, context):
    """
    Daily Job Search Routine.
    1. Checks if scan is needed (once per 24h).
    2. Uses Gemini Search Grounding to find real listings.
    3. Updates Quest Board.
    """
    try:
        # 1. Get Campaign State
        campaign_doc = db_helper.db.get_document(
            database_id=APPWRITE_DATABASE_ID, 
            collection_id=CAMPAIGN_STATE_COL, 
            document_id=user_id
        )
        
        last_scan_str = campaign_doc.get('last_job_scan')
        if last_scan_str:
            last_scan = datetime.datetime.fromisoformat(last_scan_str.replace("Z", "+00:00"))
            if last_scan.tzinfo is None: last_scan = last_scan.replace(tzinfo=datetime.timezone.utc)
            
            # Check if 24h passed
            now = datetime.datetime.now(datetime.timezone.utc)
            if (now - last_scan).total_seconds() < 86400:
                context.log(f"⏳ Job scan skipped for {user_id}. Last scan: {last_scan_str}")
                return False

        # 2. Get Profile Context (Target Role, Location)
        user_doc = db_helper.get_user_doc(user_id)
        profile_json = user_doc.get('studentprofile_json', '{}')
        profile = json.loads(profile_json)
        
        target_role = profile.get('targetRole')
        target_industry = profile.get('targetIndustry')
        location = profile.get('currentCountry', 'Remote')
        
        if not target_role:
            context.log(f"⚠️ Job scan skipped for {user_id}: No target role set.")
            return False

        context.log(f"🔎 Scanning jobs for {user_id}: {target_role} in {location}")

        # 3. Perform Search
        ai = GeminiClient()
        prompt = f"""
        Find 3 REAL, active job listings for a '{target_role}' role in '{location}' (or Remote).
        Focus on entry-level/internships suitable for a student in '{target_industry}'.
        Return ONLY a JSON array of Quest objects. No markdown.
        Schema: [{{ "id": "job_uuid", "title": "Role @ Company", "type": "APPLICATION", "description": "Apply via [Platform]", "xp": 500, "status": "active", "tags": ["Urgent", "Remote"] }}]
        """
        
        try:
            raw_response = ai.generate_response(prompt, use_search=True)
            job_json_str = raw_response.strip().replace("```json", "").replace("```", "")
            
            # Check for error text in response before parsing
            if "quota" in job_json_str.lower() or "capacity" in job_json_str.lower():
                raise Exception(f"Quota Error in Text: {job_json_str}")
                
            new_quests = json.loads(job_json_str)
            
        except Exception as e:
            error_msg = str(e).lower()
            if "quota" in error_msg or "resource_exhausted" in error_msg or "capacity" in error_msg:
                context.log(f"⚠️ Search Quota Exceeded for {user_id}. Adding System Quest.")
                new_quests = [{
                    "id": f"sys_quota_{uuid.uuid4().hex[:4]}",
                    "title": "Search Paused (Quota)",
                    "type": "APPLICATION", # Fallback to supported type
                    "description": "Daily job scan paused due to Google Search grounding limits. Will retry automatically tomorrow.",
                    "xp": 0,
                    "status": "active",
                    "tags": ["System Alert", "Quota"]
                }]
            else:
                context.log(f"❌ Job Scan Parse Error: {e} | Text: {locals().get('job_json_str', 'N/A')}")
                return False
        
        # 4. Merge with existing quests (avoid duplicates by ID ideally, but simpler append for now)
        current_board = json.loads(campaign_doc.get('quest_board_json', '[]'))
        
        # Add timestamp/unique ID to avoid pure duplicates if needed, 
        # but here we rely on Gemini generating distinct items or we just append.
        # Actually, let's just prepend new findings.
        updated_board = new_quests + current_board[:10] # Keep recent 10 + new 3
        
        # 5. Save
        update_data = {
            "quest_board_json": json.dumps(updated_board),
            "last_job_scan": datetime.datetime.now(datetime.timezone.utc).isoformat()
        }
        
        db_helper.db.update_document(
            database_id=APPWRITE_DATABASE_ID,
            collection_id=CAMPAIGN_STATE_COL,
            document_id=user_id,
            data=update_data
        )
        
        # Log Logic Intervention
        _create_thought_signature(user_id, "campaign", f"Daily Job Scan: {target_role}", f"Found {len(new_quests)} new jobs.", db_helper, context)
        
        return True

    except Exception as e:
        context.log(f"❌ Daily Job Scan Error: {e}")
        return False
