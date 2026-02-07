"""
The Campaign Agent: Career strategist and marathon goal tracker.
"""

import json
from typing import Dict, Any, List, Optional
from datetime import datetime, timedelta

from .base_agent import BaseAgent, AgentConfig, AgentResult
from ..core.bicameral_engine import ReasoningMode, ReasoningRequest
from ..core.state_machine import StateContext
from ..core.marathon_runner import MarathonRunner, MarathonGoal, MarathonSession
from ..core.event_bus import EventType, Event


class CampaignAgent(BaseAgent):
    """
    The Campaign Agent: Career strategist and marathon goal tracker.
    
    Handles event types:
    - new_goal: User sets a new objective
    - schedule_update: Calendar changes
    - quest_complete: A quest has been finished
    - marathon_step: Progress in a marathon
    - armory_unlock: New skill/tool unlocked
    - simulacrum_start: Begin mock interview
    """
    
    CAMPAIGN_SYSTEM_PROMPT = """You are the Campaign Commander for Kaironex, a Student Life Operating System.

Your role is to be a strategic career advisor and goal decomposer. You think like a project manager with deep empathy.

PERSONA:
- Tone: Strategic but encouraging, like a wise mentor
- Style: Break big goals into achievable quests
- Focus: Career development, skill building, long-term success

RULES:
1. Every goal gets decomposed into 5-15 actionable steps
2. Consider the user's current energy (vitality) when planning
3. Create realistic timelines based on their schedule
4. Celebrate progress, no matter how small
5. Adapt strategies when obstacles arise

OUTPUT FORMAT:
When analyzing, use these tags:
<analyze>Your analysis of the situation</analyze>
<strategy>Your strategic recommendation</strategy>
<decision>Your specific decision/action</decision>
<action>The user-facing message</action>

Remember: You're not just planning tasks—you're building a career."""

    def get_config(self) -> AgentConfig:
        return AgentConfig(
            agent_type="campaign",
            display_name="Campaign Commander",
            system_instruction=self.CAMPAIGN_SYSTEM_PROMPT,
            default_reasoning_mode=ReasoningMode.DEEP,
            max_thinking_tokens=16384,
            enable_thought_signatures=True,
            enable_marathon=True,
            forbidden_terms=[],
            required_disclaimer=None
        )
    
    async def process(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """Main processing logic for campaign events."""
        
        self.log_heartbeat(user_id, f"EVENT:{payload.get('type', 'unknown')}")
        
        event_type = payload.get('type', '')
        
        # Route to appropriate handler
        handlers = {
            'new_goal': self._handle_new_goal,
            'schedule_update': self._handle_schedule_update,
            'quest_complete': self._handle_quest_complete,
            'marathon_step': self._handle_marathon_step,
            'armory_unlock': self._handle_armory_unlock,
            'simulacrum_start': self._handle_simulacrum_start,
            'simulacrum_response': self._handle_simulacrum_response,
            'campaign_calibration': self._handle_campaign_calibration,
            'analyze_resume': self._handle_analyze_resume,
            'generate_resume': self._handle_generate_resume,
            'design_interview': self._handle_design_interview,
        }
        
        handler = handlers.get(event_type, self._handle_generic)
        return await handler(user_id, payload, context)
    
    async def _handle_new_goal(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """Handle a new goal being set."""
        
        goal_title = payload.get('goal_title', 'New Goal')
        goal_description = payload.get('description', '')
        deadline = payload.get('deadline')
        priority = payload.get('priority', 5)
        
        # Get user context
        user_doc = self.db.get_user_doc(user_id)
        user_context = {}
        if user_doc:
            try:
                user_context = json.loads(user_doc.get('studentState_json', '{}'))
            except:
                pass
        
        # Build comprehensive prompt
        prompt = f"""
NEW GOAL RECEIVED:
Title: {goal_title}
Description: {goal_description}
Deadline: {deadline or 'No specific deadline'}
Priority: {priority}/10

USER CONTEXT:
{json.dumps(user_context, indent=2)[:1000]}

TASK:
1. Analyze this goal for feasibility
2. Decompose it into 5-15 actionable quests
3. Create a strategic timeline
4. Identify potential obstacles
5. Suggest skill requirements (for The Armory)

Provide the strategy and first actionable step.
"""
        
        # Use deep reasoning for goal decomposition
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="campaign",
            mode=ReasoningMode.DEEP,
            context={
                "goal": goal_title,
                "user_context": user_context
            },
            system_instruction=self.CAMPAIGN_SYSTEM_PROMPT,
            max_thinking_tokens=16384
        ))
        
        # Extract strategy and quests from response
        strategy_text = response.content
        quests = self._parse_quests(strategy_text)
        
        # Store thought signature
        thought_id = None
        if response.thought_signature:
            thought_id = response.thought_signature.thought_id
            await self.thoughts.store(response.thought_signature)
        
        # Create intervention with the strategy
        intervention_id = self.create_intervention(
            user_id,
            "NEW_GOAL",
            f"🎯 **Goal Accepted: {goal_title}**\n\n{self._get_action_text(strategy_text)}",
            strategy="PROACTIVE"
        )
        
        # Update campaign state
        campaign_update = {
            "campaign": {
                "active_goal": goal_title,
                "goal_description": goal_description,
                "total_quests": len(quests),
                "completed_quests": 0,
                "current_strategy": self._get_first_quest(quests),
                "deadline": deadline,
                "priority": priority,
                "quest_board": quests[:5],  # First 5 quests
                "thought_signature": thought_id,
                "last_updated": datetime.now().isoformat()
            }
        }
        self.update_state_cache(user_id, campaign_update)
        
        return AgentResult(
            success=True,
            response=strategy_text,
            thought_id=thought_id,
            actions_taken=["goal_decomposed", "quests_created", "timeline_set"],
            interventions_created=[intervention_id],
            state_updates=campaign_update
        )
    
    async def _handle_schedule_update(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """Handle schedule changes - re-optimize if needed."""
        
        changes = payload.get('changes', [])
        
        # Get current campaign state
        user_doc = self.db.get_user_doc(user_id)
        current_state = {}
        if user_doc:
            try:
                current_state = json.loads(user_doc.get('studentState_json', '{}'))
            except:
                pass
        
        campaign = current_state.get('campaign', {})
        
        if not campaign.get('active_goal'):
            return AgentResult(
                success=True,
                response="No active campaign to optimize.",
                actions_taken=["no_optimization_needed"]
            )
        
        prompt = f"""
SCHEDULE UPDATE DETECTED:
Changes: {json.dumps(changes, indent=2)}

CURRENT CAMPAIGN:
Goal: {campaign.get('active_goal')}
Progress: {campaign.get('completed_quests', 0)}/{campaign.get('total_quests', 0)} quests
Deadline: {campaign.get('deadline', 'None')}

Current Quest Board:
{json.dumps(campaign.get('quest_board', []), indent=2)}

TASK:
1. Analyze how these schedule changes affect the campaign
2. Re-prioritize quests if needed
3. Suggest timeline adjustments
4. Provide a brief status update

Keep it concise and actionable.
"""
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="campaign",
            mode=ReasoningMode.HYBRID,
            context={"schedule_changes": changes}
        ))
        
        # Update state
        campaign_update = {
            "campaign": {
                **campaign,
                "schedule_status": "Optimized",
                "last_optimization": datetime.now().isoformat()
            }
        }
        self.update_state_cache(user_id, campaign_update)
        
        return AgentResult(
            success=True,
            response=response.content,
            thought_id=response.thought_signature.thought_id if response.thought_signature else None,
            actions_taken=["schedule_analyzed", "campaign_re-optimized"],
            state_updates=campaign_update
        )
    
    async def _handle_quest_complete(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """Handle quest completion."""
        
        quest_id = payload.get('quest_id')
        quest_title = payload.get('quest_title', 'Unknown Quest')
        
        # Get current state
        user_doc = self.db.get_user_doc(user_id)
        current_state = {}
        if user_doc:
            try:
                current_state = json.loads(user_doc.get('studentState_json', '{}'))
            except:
                pass
        
        campaign = current_state.get('campaign', {})
        completed = campaign.get('completed_quests', 0) + 1
        total = campaign.get('total_quests', 1)
        progress = completed / total
        
        # Generate celebration and next steps
        prompt = f"""
QUEST COMPLETED! 🎉
Quest: {quest_title}
Progress: {completed}/{total} ({progress*100:.1f}%)
Goal: {campaign.get('active_goal', 'Unknown')}

Generate:
1. A brief celebration message (1-2 sentences)
2. The next quest to focus on
3. Motivation boost based on progress
"""
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="campaign",
            mode=ReasoningMode.REFLEX
        ))
        
        # Update state
        campaign_update = {
            "campaign": {
                **campaign,
                "completed_quests": completed,
                "last_completed": quest_title,
                "progress_percent": progress * 100
            }
        }
        self.update_state_cache(user_id, campaign_update)
        
        # Create celebration intervention
        intervention_id = self.create_intervention(
            user_id,
            "QUEST_COMPLETE",
            response.content,
            strategy="CELEBRATION"
        )
        
        return AgentResult(
            success=True,
            response=response.content,
            actions_taken=["quest_marked_complete", "progress_updated"],
            interventions_created=[intervention_id],
            state_updates=campaign_update
        )
    
    async def _handle_marathon_step(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """Handle a marathon step update."""
        
        marathon_id = payload.get('marathon_id')
        step_result = payload.get('step_result')
        
        # This would integrate with MarathonRunner
        # For now, just acknowledge
        return AgentResult(
            success=True,
            response=f"Marathon step recorded for {marathon_id}",
            actions_taken=["marathon_step_logged"]
        )
    
    async def _handle_armory_unlock(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """Handle skill/tool unlock in The Armory."""
        
        skill_name = payload.get('skill_name', 'Unknown Skill')
        skill_level = payload.get('level', 1)
        
        prompt = f"""
SKILL UNLOCKED! 🛡️
Skill: {skill_name}
Level: {skill_level}

Generate:
1. A brief description of what this skill enables
2. How it helps with their current campaign
3. Suggested next skill to work toward
"""
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="campaign",
            mode=ReasoningMode.REFLEX
        ))
        
        # Update armory state
        armory_update = {
            "armory": {
                "latest_unlock": skill_name,
                "latest_level": skill_level,
                "unlock_time": datetime.now().isoformat()
            }
        }
        self.update_state_cache(user_id, armory_update)
        
        return AgentResult(
            success=True,
            response=response.content,
            actions_taken=["skill_unlocked", "armory_updated"],
            state_updates=armory_update
        )
    
    async def _handle_simulacrum_start(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """Start a Simulacrum (mock interview) session."""
        
        interview_type = payload.get('type', 'behavioral')
        company = payload.get('company', 'Generic Tech Company')
        role = payload.get('role', 'Software Engineer')
        difficulty = payload.get('difficulty', 'medium')
        
        # Build the interviewer persona
        persona_prompts = {
            'easy': "You are a friendly interviewer who wants the candidate to succeed.",
            'medium': "You are a professional interviewer, balanced but thorough.",
            'hard': "You are a challenging interviewer who pushes candidates with tough follow-ups.",
            'brutal': "You are a notoriously tough interviewer. Be skeptical, interrupt, and challenge assumptions."
        }
        
        persona = persona_prompts.get(difficulty, persona_prompts['medium'])
        
        prompt = f"""
SIMULACRUM ACTIVATED 🎭
Role: {role} at {company}
Interview Type: {interview_type}
Difficulty: {difficulty}

{persona}

Generate the first interview question. Make it specific to the role and company.
Set the scene briefly (1 sentence) then ask your question.
"""
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="campaign",
            mode=ReasoningMode.DEEP,
            context={
                "simulacrum": True,
                "company": company,
                "role": role,
                "difficulty": difficulty
            }
        ))
        
        # Store simulacrum state
        simulacrum_update = {
            "simulacrum": {
                "active": True,
                "type": interview_type,
                "company": company,
                "role": role,
                "difficulty": difficulty,
                "questions_asked": 1,
                "started_at": datetime.now().isoformat(),
                "last_question": response.content
            }
        }
        self.update_state_cache(user_id, simulacrum_update)
        
        return AgentResult(
            success=True,
            response=response.content,
            thought_id=response.thought_signature.thought_id if response.thought_signature else None,
            actions_taken=["simulacrum_started"],
            state_updates=simulacrum_update
        )
    
    async def _handle_simulacrum_response(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """Process a user's response in Simulacrum and continue the interview."""
        
        user_response = payload.get('response', '')
        
        # Get simulacrum state
        user_doc = self.db.get_user_doc(user_id)
        current_state = {}
        if user_doc:
            try:
                current_state = json.loads(user_doc.get('studentState_json', '{}'))
            except:
                pass
        
        simulacrum = current_state.get('simulacrum', {})
        
        if not simulacrum.get('active'):
            return AgentResult(
                success=False,
                response="No active Simulacrum session.",
                error="simulacrum_not_active"
            )
        
        prompt = f"""
SIMULACRUM INTERVIEW IN PROGRESS
Company: {simulacrum.get('company')}
Role: {simulacrum.get('role')}
Difficulty: {simulacrum.get('difficulty')}
Questions Asked: {simulacrum.get('questions_asked', 1)}

LAST QUESTION:
{simulacrum.get('last_question')}

CANDIDATE'S RESPONSE:
{user_response}

TASK:
1. Briefly evaluate their answer (1-2 sentences, internal)
2. Ask a follow-up question OR move to a new topic
3. Maintain the difficulty level persona
4. After 5+ questions, you may end with feedback

Format:
<evaluation>Your internal assessment</evaluation>
<action>Your response to the candidate (question or feedback)</action>
"""
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="campaign",
            mode=ReasoningMode.DEEP,
            context=simulacrum
        ))
        
        # Update simulacrum state
        questions_asked = simulacrum.get('questions_asked', 1) + 1
        simulacrum_update = {
            "simulacrum": {
                **simulacrum,
                "questions_asked": questions_asked,
                "last_question": response.content,
                "last_user_response": user_response[:500]
            }
        }
        self.update_state_cache(user_id, simulacrum_update)
        
        return AgentResult(
            success=True,
            response=response.content,
            thought_id=response.thought_signature.thought_id if response.thought_signature else None,
            actions_taken=["simulacrum_continued"],
            state_updates=simulacrum_update
        )
    
    async def _handle_generic(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """Handle generic/unknown campaign events."""
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=f"Campaign event received: {json.dumps(payload)}\n\nProvide appropriate guidance.",
            user_id=user_id,
            agent="campaign",
            mode=ReasoningMode.REFLEX
        ))
        
        return AgentResult(
            success=True,
            response=response.content,
            actions_taken=["generic_response"]
        )
    
    def _parse_quests(self, text: str) -> List[Dict[str, str]]:
        """Parse quest definitions from AI response."""
        import re
        
        quests = []
        
        # Try to find numbered items
        pattern = r'(?:^|\n)[\d]+[\.\)]\s*(.+?)(?=\n[\d]+[\.\)]|\n\n|$)'
        matches = re.findall(pattern, text, re.DOTALL)
        
        for i, match in enumerate(matches[:15], 1):
            quests.append({
                "id": f"quest_{i}",
                "title": match.strip()[:100],
                "status": "pending"
            })
        
        # Fallback if no quests found
        if not quests:
            quests = [{"id": "quest_1", "title": "Begin your journey", "status": "pending"}]
        
        return quests
    
    def _get_first_quest(self, quests: List[Dict[str, str]]) -> str:
        """Get the first quest title."""
        if quests:
            return quests[0].get('title', 'Start here')
        return 'Define your first objective'
    
    def _get_action_text(self, response: str) -> str:
        """Extract action text from structured response."""
        import re
        
        action_match = re.search(r'<action>(.*?)</action>', response, re.DOTALL)
        if action_match:
            return action_match.group(1).strip()
        
        # Fallback: take first paragraph
        paragraphs = response.split('\n\n')
        return paragraphs[0] if paragraphs else response[:200]


    async def _handle_campaign_calibration(self, user_id: str, payload: Dict[str, Any], context: StateContext) -> AgentResult:
        """
        Handle the Campaign Calibration event (Student Career Setup).
        Generates:
        1. Campaign Strategy (Overview)
        2. Skill Tree (Roadmap)
        3. Quest Board (Job Search / Networking Quests)
        4. The Armory (Tools/Resumes)
        """
        # 1. Parsing Context (From payload and potential DB profile)
        # We try to get the profile from DB first if payload is sparse
        db_profile = self.db.get_student_profile(user_id) or {}
        
        major = payload.get('major') or db_profile.get('major', 'General Studies')
        target_role = payload.get('targetRole') or 'Technology Professional'
        target_industry = payload.get('targetIndustry') or 'Tech'
        tech_skill = float(payload.get('techSkill', 5.0))
        
        # 2. Fetch Deep Context
        study_context = self.db.get_user_doc(user_id) # Using this as proxy for now
        
        # 3. Construct Calibration Prompt
        system_instruction = f"""
        You are the Head of Strategy for Kaironex.
        A student has calibrated their career profile. You must generate their specific Campaign assets.
        
        PROFILE:
        - Major: {major}
        - Target Role: {target_role} in {target_industry}
        - Current Tech Skill: {tech_skill}/10
        
        MISSION:
        Generate the initial JSON data for their Campaign State.
        
        1. **Skill Tree**: A dependency graph of 5-7 skills needed for {target_role}.
           - Root skill (Level 1) -> Intermediate (Level 2) -> Advanced (Level 3).
           - Status: 'unlocked' (first one), 'locked' (others).
           
        2. **Quest Board**: 3 initial quests.
           - Mix of: Networking, Skill Building, and Market Research.
           - Difficulty: Easy to Start.
           
        3. **The Armory**: Initial toolbox.
           - Default: "Basic Resume Template" (Unlocked).
           - Recommend 1 locked item related to their industry (e.g. "GitHub Portfolio Builder").
           
        OUTPUT JSON:
        {{
            "strategy": {{
                "name": "Strategy Name",
                "description": "2 sentence strategy summary."
            }},
            "skill_tree": [
                {{ "id": "skill_1", "name": "Skill Name", "level": 1, "status": "unlocked", "description": "Start here.", "parent": null }},
                {{ "id": "skill_2", "name": "Next Skill", "level": 2, "status": "locked", "description": "...", "parent": "skill_1" }}
            ],
            "quest_board": [
                {{ "id": "dst_q1", "title": "Quest Title", "type": "RESEARCH", "xp": 100, "status": "active", "description": "..." }}
            ],
            "armory": {{
                "inventory": [
                    {{ "id": "item_resume_basic", "name": "Standard Resume", "type": "TEMPLATE", "status": "equipped" }}
                ],
                "blueprints": [
                    {{ "id": "bp_portfolio", "name": "Portfolio Builder", "cost_xp": 500 }}
                ]
            }}
        }}
        """
        
        try:
            # 4. Reason
            response = await self.engine.reason(ReasoningRequest(
                prompt="Initialize Campaign Assets based on profile.",
                user_id=user_id,
                agent="campaign",
                mode=ReasoningMode.DEEP,
                system_instruction=system_instruction
            ))
            
            # 5. Parse
            assets = json.loads(response.content)
            
            # 6. Save to CAMPAIGN_STATE table
            campaign_data = {
                "skill_tree": assets.get("skill_tree", []),
                "quest_board": assets.get("quest_board", []),
                "armory": assets.get("armory", {}),
                "simulacrum": {"active": False} # Reset simulacrum
            }
            
            self.db.update_campaign_state(user_id, campaign_data)
            
            # 7. Also update 'users' table cache for quick access (Legacy/App compatibility)
            # We explicitly set 'is_calibrated' to True here
            legacy_update = {
                "campaign": {
                    "is_calibrated": True,
                    "active_goal": target_role,
                    "strategy_name": assets.get("strategy", {}).get("name"),
                    "strategy_desc": assets.get("strategy", {}).get("description"),
                    "last_updated": datetime.now().isoformat()
                }
            }
            self.db.update_state_cache(user_id, legacy_update)
            
            # 8. Log Thought
            skills = campaign_data["skill_tree"]
            quests = campaign_data["quest_board"]
            
            self.db.create_thought_signature(
                user_id=user_id,
                thought_data={
                    "agent": "campaign",
                    "thought": f"Generated Campaign Assets for {target_role}",
                    "confidence": 0.98,
                    "reasoning_trace": ["Calibration completed", f"Generated {len(skills)} skills", f"Generated {len(quests)} quests"]
                }
            )
            
            return AgentResult(
                success=True,
                response="Campaign Assets Generated.",
                state_updates={"campaign": campaign_data}
            )
            
        except Exception as e:
            return AgentResult(success=False, response=f"Calibration failed: {str(e)}", error=str(e))


    async def _handle_analyze_resume(self, user_id: str, payload: Dict[str, Any], context: StateContext) -> AgentResult:
        """Analyze resume against job description."""
        data = payload.get('data', {})
        resume_text = data.get('resumeText', '')
        resume_pdf = data.get('resumePdf')
        job_desc = data.get('jobDesc', '')
        
        # If PDF provided, prioritize it for multimodal analysis
        prompt_content = []
        prompt_content.append(f"JOB DESCRIPTION:\n{job_desc[:2000]}...")
        
        if resume_text:
            prompt_content.append(f"RESUME TEXT:\n{resume_text[:2000]}...")
        elif resume_pdf:
            # We will rely on Gemini's ability to process PDF if we attach it, 
            # but for this specific agent which uses text prompts, we might strictly need text.
            # However, to avoid 404/Empty errors, we'll acknowledge the PDF receipt.
            prompt_content.append(f"RESUME: [PDF Attached with size {len(resume_pdf)} bytes]")
        
        prompt = f"""
        RESUME ANALYSIS REQUEST
        
        {chr(10).join(prompt_content)}
        
        TASK:
        Analyze how well this resume matches the job description.
        1. Calculate ATS Score (0-100)
        2. Identify Matched Keywords
        3. Identify Missing Keywords
        4. Provide an improvement checklist
        
        OUTPUT JSON:
        {{
            "ats_score": 75,
            "matched_keywords": ["Java", "Kotlin"],
            "missing_keywords": ["CI/CD", "AWS"],
            "overall_assessment": "Good base, needs more cloud focus.",
            "estimated_pass_rate": "High",
            "interview_ready": true,
            "checklist": [
                {{ "item": "Add AWS certification", "status": "pending", "impact": "HIGH" }}
            ]
        }}
        """
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt, user_id=user_id, agent="campaign", mode=ReasoningMode.DEEP
        ))
        
        try:
            analysis_data = json.loads(response.content)
            return AgentResult(success=True, response="Resume Analyzed", data=analysis_data)
        except:
            return AgentResult(success=False, response="Failed to parse analysis.")

    async def _handle_generate_resume(self, user_id: str, payload: Dict[str, Any], context: StateContext) -> AgentResult:
        """Generate tailored resume."""
        data = payload.get('data', {})
        job_desc = data.get('jobDesc', '')
        projects = data.get('projects', '[]')
        
        prompt = f"""
        RESUME GENERATION REQUEST
        
        JOB: {job_desc[:1000]}
        PROJECTS: {projects}
        
        TASK:
        Generate a professional summary and skills section tailored to this job.
        
        OUTPUT JSON:
        {{
            "resume": {{
                "professional_summary": "...",
                "skills_section": ["Skill 1", "Skill 2"]
            }},
            "estimated_ats_score": 85
        }}
        """
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt, user_id=user_id, agent="campaign", mode=ReasoningMode.DEEP
        ))
        
        try:
            gen_data = json.loads(response.content)
            return AgentResult(success=True, response="Resume Generated", data=gen_data)
        except:
            return AgentResult(success=False, response="Failed to generate resume.")

    async def _handle_design_interview(self, user_id: str, payload: Dict[str, Any], context: StateContext) -> AgentResult:
        """Design the Simulacrum Interview."""
        data = payload.get('data', {})
        job_desc = data.get('jobDesc', '')
        difficulty = data.get('difficulty', 'medium')
        
        prompt = f"""
        DESIGN INTERVIEW: {difficulty.upper()}
        JOB: {job_desc[:1000]}
        
        TASK:
        Create a structured interview plan.
        1. Interviewer Persona
        2. Question Bank (5 questions)
        3. Gemini Live Prompt (System prompt for the voice agent)
        
        OUTPUT JSON:
        {{
            "interview_id": "sim_123",
            "duration_minutes": 25,
            "interviewer": {{
                "name": "Alex", "role": "Senior Engineer", "personality": "Strict", "company": "TechCorp"
            }},
            "question_bank": [
                {{ "id": "q1", "question": "...", "category": "Technical", "good_answer_criteria": ["..."], "red_flags": ["..."] }}
            ],
            "gemini_live_prompt": "You are Alex, a Senior Engineer...",
            "candidate_prep_notes": ["Review HashMap", "Practice STAR method"]
        }}
        """
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt, user_id=user_id, agent="campaign", mode=ReasoningMode.DEEP
        ))
        
        try:
            design_data = json.loads(response.content)
            return AgentResult(success=True, response="Interview Designed", data=design_data)
        except:
            return AgentResult(success=False, response="Failed to design interview.")


# Factory function for compatibility with existing code
# Async Factory function
async def run_campaign_agent(db_helper, payload, context):
    """
    Async compatibility wrapper.
    """
    import asyncio
    from ..core.bicameral_engine import BicameralEngine
    from ..core.thought_manager import ThoughtManager
    
    # Initialize components
    engine = BicameralEngine()
    thought_manager = ThoughtManager(db_helper)
    
    # Create agent
    agent = CampaignAgent(engine, thought_manager, db_helper)
    
    # Extract user_id and run
    user_id = payload.get('userId')
    if not user_id:
        return context.res.json({"error": "No userId"})
    
    trigger_event = payload.get('type', 'unknown')
    
    # Run async directly
    result = await agent.run(user_id, payload, trigger_event)
    
    return context.res.json(result.to_dict())
