"""
🤖 KAIRONEX AGENTS
==================
The intelligent agents that power the Student Life OS.

Each agent specializes in a different domain:
- Study Agent: Cognitive supply chain management
- Vitality Agent: Bio-fuel and resource management (v2.0: Survival & Growth Protocol)
- Campaign Agent: Goals, career strategy, and marathons
- Radius Agent: Spatial context and environment
- Supervisor: Meta-controller and drift detection
"""

# Base
from .base_agent import BaseAgent, AgentConfig, AgentResult

# Agents (v2.0 - Current)
from .campaign_agent import CampaignAgent, run_campaign_agent
from .vitality_agent_v2 import VitalityAgent  # v2.0: Survival & Growth Protocol (default)
from .vitality_brain_v2 import run_vitality_agent  # v2.0: Default runner
from .study_agent import StudyAgent, run_study_agent
from .radius_agent import RadiusAgent, run_radius_agent
from .supervisor_agent import SupervisorAgent, run_supervisor

# Legacy imports (backward compatibility)
try:
    from .study_brain import run_study_agent as legacy_study
except ImportError:
    legacy_study = run_study_agent

try:
    from .radius_brain import run_radius_agent as legacy_radius
except ImportError:
    legacy_radius = run_radius_agent

try:
    from .main_brain import run_supervisor as legacy_supervisor
except ImportError:
    legacy_supervisor = run_supervisor

__all__ = [
    # Base
    'BaseAgent',
    'AgentConfig', 
    'AgentResult',
    
    # v2.0 Agents (Current)
    'CampaignAgent',
    'VitalityAgent',        # v2.0: Survival & Growth Protocol
    'StudyAgent',
    'RadiusAgent',
    'SupervisorAgent',
    
    # Runners (v2.0)
    'run_campaign_agent',
    'run_vitality_agent',   # v2.0: Survival & Growth Protocol
    'run_study_agent',
    'run_radius_agent',
    'run_supervisor',
    
    # Legacy (other agents)
    'legacy_study',
    'legacy_radius',
    'legacy_supervisor',
]
