"""
The Radius Agent: Habitat Manager.
"""

import json
from typing import Dict, Any, List, Optional
from datetime import datetime

from .base_agent import BaseAgent, AgentConfig, AgentResult
from ..core.bicameral_engine import ReasoningMode, ReasoningRequest
from ..core.state_machine import StateContext


class RadiusAgent(BaseAgent):
    """
    The Radius Agent: Habitat Manager.
    
    Handles event types:
    - location_change: User moved to new location
    - mode_request: User requested specific mode
    - safehouse_status: Check safehouse status
    - environment_scan: Analyze current environment
    """
    
    RADIUS_SYSTEM_PROMPT = """You are the Radius Controller for Kaironex, a Student Life Operating System.

Your role is to manage the user's spatial context - understanding where they are and optimizing their experience for that environment.

PERSONA:
- Tone: Calm, environmental awareness
- Style: Brief, contextual messages
- Focus: Seamless environment transitions

LOCATION MODES:
- LIBRARY/STUDY_SPACE: DEEP_FOCUS mode (whisper notifications, study focus)
- GYM/FITNESS: WORKOUT mode (energetic, activity tracking)
- HOME: REST mode (relaxed, casual assistance)
- COMMUTE/TRANSIT: MOBILE mode (quick interactions, podcasts)
- SOCIAL/CAFE: LIGHT_FOCUS mode (balanced attention)
- UNKNOWN: STANDARD mode (default assistance)

RULES:
1. Auto-detect mode from location keywords
2. Welcome messages should be 1 sentence max
3. Adjust notification strategy per environment
4. Track "safehouse" locations (regular study spots)
5. Never announce location publicly

OUTPUT FORMAT:
<analyze>Environmental assessment</analyze>
<action>Brief, context-appropriate message</action>"""

    # Location to mode mapping
    LOCATION_MODES = {
        'library': ('DEEP_FOCUS', 'WHISPER'),
        'study': ('DEEP_FOCUS', 'WHISPER'),
        'classroom': ('FOCUS', 'SILENT'),
        'lecture': ('FOCUS', 'SILENT'),
        'gym': ('WORKOUT', 'ENERGETIC'),
        'fitness': ('WORKOUT', 'ENERGETIC'),
        'home': ('REST', 'WARM'),
        'dorm': ('REST', 'WARM'),
        'apartment': ('REST', 'WARM'),
        'cafe': ('LIGHT_FOCUS', 'CASUAL'),
        'coffee': ('LIGHT_FOCUS', 'CASUAL'),
        'restaurant': ('SOCIAL', 'MINIMAL'),
        'commute': ('MOBILE', 'BRIEF'),
        'transit': ('MOBILE', 'BRIEF'),
        'bus': ('MOBILE', 'BRIEF'),
        'train': ('MOBILE', 'BRIEF'),
        'work': ('FOCUS', 'PROFESSIONAL'),
        'office': ('FOCUS', 'PROFESSIONAL'),
    }

    def get_config(self) -> AgentConfig:
        return AgentConfig(
            agent_type="radius",
            display_name="Radius Controller",
            system_instruction=self.RADIUS_SYSTEM_PROMPT,
            default_reasoning_mode=ReasoningMode.REFLEX,
            max_thinking_tokens=4096,
            enable_thought_signatures=False,
            enable_marathon=False
        )
    
    async def process(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """Main processing logic for radius events."""
        
        location = payload.get('user_location', payload.get('location', 'Unknown'))
        self.log_heartbeat(user_id, f"EVENT:RADIUS | {location}")
        
        event_type = payload.get('type', 'location_change')
        
        handlers = {
            'location_change': self._handle_location_change,
            'mode_request': self._handle_mode_request,
            'safehouse_status': self._handle_safehouse_status,
            'environment_scan': self._handle_environment_scan,
        }
        
        handler = handlers.get(event_type, self._handle_location_change)
        return await handler(user_id, payload, context)
    
    def _detect_mode(self, location: str) -> tuple[str, str]:
        """Detect mode and notification strategy from location."""
        location_lower = location.lower()
        
        for keyword, (mode, strategy) in self.LOCATION_MODES.items():
            if keyword in location_lower:
                return mode, strategy
        
        return 'STANDARD', 'NEUTRAL'
    
    async def _handle_location_change(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """Handle location change event."""
        
        location = payload.get('user_location', payload.get('location', 'Unknown'))
        trigger_voice = payload.get('trigger_voice', False)
        
        # Detect mode
        mode, strategy = self._detect_mode(location)
        
        # Generate welcome if requested
        interventions = []
        response_text = f"Mode: {mode}"
        
        if trigger_voice:
            prompt = f"""
LOCATION CHANGE:
New Location: {location}
Detected Mode: {mode}
Notification Strategy: {strategy}

Generate a brief (1 sentence) welcome message appropriate for this environment.
Consider the time of day and the location context.
"""
            
            response = await self.engine.reason(ReasoningRequest(
                prompt=prompt,
                user_id=user_id,
                agent="radius",
                mode=ReasoningMode.REFLEX
            ))
            
            response_text = response.content
            
            intervention_id = self.create_intervention(
                user_id,
                "LOCATION_CHANGE",
                response_text,
                strategy=strategy
            )
            interventions.append(intervention_id)
        
        # Check if this is a new safehouse
        is_safehouse = self._check_safehouse(user_id, location)
        
        # Update state
        radius_update = {
            "radius": {
                "current_location": location,
                "active_mode": mode,
                "notification_strategy": strategy,
                "safehouse_status": "SECURE" if is_safehouse else "STANDARD",
                "last_location_change": datetime.now().isoformat()
            }
        }
        self.update_state_cache(user_id, radius_update)
        
        return AgentResult(
            success=True,
            response=response_text,
            actions_taken=["location_detected", f"mode_{mode.lower()}"],
            interventions_created=interventions,
            state_updates=radius_update
        )
    
    async def _handle_mode_request(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """Handle manual mode change request."""
        
        requested_mode = payload.get('mode', 'STANDARD')
        duration_minutes = payload.get('duration_minutes')
        
        prompt = f"""
MODE CHANGE REQUESTED:
Requested Mode: {requested_mode}
Duration: {duration_minutes or 'Until further notice'} minutes

Confirm the mode activation with a brief message.
"""
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="radius",
            mode=ReasoningMode.REFLEX
        ))
        
        # Determine strategy
        strategy_map = {
            'DEEP_FOCUS': 'WHISPER',
            'WORKOUT': 'ENERGETIC',
            'REST': 'WARM',
            'MOBILE': 'BRIEF',
            'SILENT': 'SILENT',
        }
        strategy = strategy_map.get(requested_mode, 'NEUTRAL')
        
        radius_update = {
            "radius": {
                "active_mode": requested_mode,
                "mode_source": "manual",
                "notification_strategy": strategy,
                "mode_expires": None  # TODO: Calculate if duration provided
            }
        }
        self.update_state_cache(user_id, radius_update)
        
        return AgentResult(
            success=True,
            response=response.content,
            actions_taken=["mode_changed", f"mode_{requested_mode.lower()}"],
            state_updates=radius_update
        )
    
    async def _handle_safehouse_status(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """Check and report safehouse status."""
        
        # Get current state
        user_doc = self.db.get_user_doc(user_id)
        current_state = {}
        if user_doc:
            try:
                current_state = json.loads(user_doc.get('studentState_json', '{}'))
            except:
                pass
        
        radius = current_state.get('radius', {})
        current_location = radius.get('current_location', 'Unknown')
        
        is_safehouse = self._check_safehouse(user_id, current_location)
        
        status = "SECURE" if is_safehouse else "STANDARD"
        
        prompt = f"""
SAFEHOUSE STATUS CHECK:
Current Location: {current_location}
Status: {status}

Provide a brief status report (1-2 sentences).
If secure, acknowledge. If not, offer suggestions.
"""
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="radius",
            mode=ReasoningMode.REFLEX
        ))
        
        radius_update = {
            "radius": {
                **radius,
                "safehouse_status": status,
                "last_status_check": datetime.now().isoformat()
            }
        }
        self.update_state_cache(user_id, radius_update)
        
        return AgentResult(
            success=True,
            response=response.content,
            actions_taken=["safehouse_checked"],
            state_updates=radius_update
        )
    
    async def _handle_environment_scan(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """Perform comprehensive environment analysis."""
        
        location = payload.get('location', 'Unknown')
        noise_level = payload.get('noise_level')
        lighting = payload.get('lighting')
        temperature = payload.get('temperature')
        
        prompt = f"""
ENVIRONMENT SCAN:
Location: {location}
Noise Level: {noise_level or 'Not measured'}
Lighting: {lighting or 'Not measured'}
Temperature: {temperature or 'Not measured'}

TASK:
1. Assess environment suitability for focus work
2. Suggest optimizations if any
3. Recommend best activities for this environment

Be concise and practical.
"""
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="radius",
            mode=ReasoningMode.HYBRID
        ))
        
        # Calculate environment score
        env_score = self._calculate_environment_score(
            noise_level,
            lighting,
            temperature
        )
        
        radius_update = {
            "radius": {
                "environment_score": env_score,
                "last_scan": datetime.now().isoformat(),
                "scan_result": response.content[:500]
            }
        }
        self.update_state_cache(user_id, radius_update)
        
        return AgentResult(
            success=True,
            response=response.content,
            thought_id=response.thought_signature.thought_id if response.thought_signature else None,
            actions_taken=["environment_scanned"],
            state_updates=radius_update
        )
    
    def _check_safehouse(self, user_id: str, location: str) -> bool:
        """Check if location is a known safehouse."""
        # In a full implementation, this would check user preferences
        safehouse_keywords = ['home', 'dorm', 'library', 'apartment', 'room']
        location_lower = location.lower()
        
        for keyword in safehouse_keywords:
            if keyword in location_lower:
                return True
        
        return False
    
    def _calculate_environment_score(
        self,
        noise_level: Optional[int] = None,
        lighting: Optional[str] = None,
        temperature: Optional[int] = None
    ) -> int:
        """Calculate environment suitability score (0-100)."""
        score = 70  # Base score
        
        if noise_level is not None:
            if noise_level < 30:
                score += 15  # Quiet
            elif noise_level > 70:
                score -= 15  # Loud
        
        if lighting:
            if lighting.lower() in ['good', 'natural', 'bright']:
                score += 10
            elif lighting.lower() in ['dim', 'dark']:
                score -= 10
        
        if temperature is not None:
            if 68 <= temperature <= 75:  # Fahrenheit
                score += 5
            elif temperature < 60 or temperature > 85:
                score -= 10
        
        return max(0, min(100, score))


# Legacy compatibility wrapper
def run_radius_agent(db_helper, payload, context):
    """Legacy compatibility wrapper."""
    import asyncio
    from ..core.bicameral_engine import BicameralEngine
    from ..core.thought_manager import ThoughtManager
    
    engine = BicameralEngine()
    thought_manager = ThoughtManager(db_helper)
    
    agent = RadiusAgent(engine, thought_manager, db_helper)
    
    user_id = payload.get('userId')
    if not user_id:
        return context.res.json({"error": "No userId"})
    
    trigger_event = payload.get('type', 'location_change')
    
    try:
        loop = asyncio.get_event_loop()
    except RuntimeError:
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
    
    result = loop.run_until_complete(
        agent.run(user_id, payload, trigger_event)
    )
    
    return context.res.json(result.to_dict())
