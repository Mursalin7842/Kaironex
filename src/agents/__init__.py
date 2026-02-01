"""
🤖 KAIRONEX AGENTS
==================
The intelligent agents that power the Student Life OS.

Each agent specializes in a different domain:
- Study Agent: Cognitive supply chain management
- Vitality Agent: Bio-fuel and resource management
- Campaign Agent: Goals, career strategy, and marathons
- Radius Agent: Spatial context and environment
- Supervisor: Meta-controller and drift detection
"""

# Base
from .base_agent import BaseAgent, AgentConfig, AgentResult

# Agents (v2.0)
from .campaign_agent import CampaignAgent, run_campaign_agent
from .vitality_agent import VitalityAgent, run_vitality_agent
from .study_agent import StudyAgent, run_study_agent
from .radius_agent import RadiusAgent, run_radius_agent
from .supervisor_agent import SupervisorAgent, run_supervisor

# Legacy imports (for backward compatibility with original _brain files)
try:
    from .campaign_brain import run_campaign_agent as legacy_campaign
except ImportError:
    legacy_campaign = run_campaign_agent

try:
    from .vitality_brain import run_vitality_agent as legacy_vitality
except ImportError:
    legacy_vitality = run_vitality_agent

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
    
    # v2.0 Agents
    'CampaignAgent',
    'VitalityAgent',
    'StudyAgent',
    'RadiusAgent',
    'SupervisorAgent',
    
    # Runners (v2.0 - preferred)
    'run_campaign_agent',
    'run_vitality_agent',
    'run_study_agent',
    'run_radius_agent',
    'run_supervisor',
    
    # Legacy compatibility
    'legacy_campaign',
    'legacy_vitality',
    'legacy_study',
    'legacy_radius',
    'legacy_supervisor',
]
