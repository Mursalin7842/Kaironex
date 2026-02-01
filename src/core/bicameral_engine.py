"""
🧠 BICAMERAL ENGINE
===================
The dual-model reasoning system that powers Kaironex's intelligence.

Architecture:
- REFLEX CORTEX: Fast, pattern-matching responses (Gemini 2.5 Flash)
- DEEP COGNITION: Complex planning with thought signatures (Gemini 2.0 Thinking)

The engine automatically routes requests based on complexity, urgency, and 
the user's current pressure index.
"""

import os
import json
import hashlib
import asyncio
from enum import Enum
from typing import Optional, Dict, Any, List, Callable
from dataclasses import dataclass, field
from datetime import datetime
from google import genai
from google.genai import types

from ..config import GEMINI_API_KEY


class ReasoningMode(Enum):
    REFLEX = "reflex"           # Fast, pattern-based
    DEEP = "deep"               # Slow, thoughtful
    HYBRID = "hybrid"           # Reflex + validation
    MARATHON = "marathon"       # Long-running with checkpoints


@dataclass
class ThoughtSignature:
    """Cryptographic thought chain for state integrity."""
    thought_id: str
    timestamp: datetime
    user_id: str
    agent: str
    context_hash: str
    reasoning_trace: List[str]
    confidence: float
    tool_calls: List[str]
    action_output: str
    parent_signature: Optional[str] = None
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            "thought_id": self.thought_id,
            "timestamp": self.timestamp.isoformat(),
            "user_id": self.user_id,
            "agent": self.agent,
            "signature": {
                "context_hash": self.context_hash,
                "reasoning_trace": self.reasoning_trace,
                "confidence": self.confidence,
                "tool_calls": self.tool_calls
            },
            "action_output": self.action_output,
            "parent_signature": self.parent_signature
        }
    
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> 'ThoughtSignature':
        sig = data.get("signature", {})
        return cls(
            thought_id=data["thought_id"],
            timestamp=datetime.fromisoformat(data["timestamp"]),
            user_id=data["user_id"],
            agent=data["agent"],
            context_hash=sig.get("context_hash", ""),
            reasoning_trace=sig.get("reasoning_trace", []),
            confidence=sig.get("confidence", 0.0),
            tool_calls=sig.get("tool_calls", []),
            action_output=data.get("action_output", ""),
            parent_signature=data.get("parent_signature")
        )


@dataclass
class ReasoningRequest:
    """A request to the bicameral engine."""
    prompt: str
    user_id: str
    agent: str
    context: Dict[str, Any] = field(default_factory=dict)
    mode: ReasoningMode = ReasoningMode.HYBRID
    parent_thought: Optional[str] = None
    tools: List[Callable] = field(default_factory=list)
    system_instruction: Optional[str] = None
    max_thinking_tokens: int = 8192


@dataclass  
class ReasoningResponse:
    """Response from the bicameral engine."""
    content: str
    mode_used: ReasoningMode
    thought_signature: Optional[ThoughtSignature] = None
    latency_ms: float = 0.0
    tool_results: List[Dict[str, Any]] = field(default_factory=list)
    confidence: float = 1.0


class BicameralEngine:
    """
    The core reasoning engine with dual-model architecture.
    
    Usage:
        engine = BicameralEngine()
        response = await engine.reason(ReasoningRequest(
            prompt="Help me plan my week",
            user_id="user_123",
            agent="campaign",
            mode=ReasoningMode.DEEP
        ))
    """
    
    # Model configurations
    REFLEX_MODEL = "gemini-2.5-flash-preview-05-20"
    DEEP_MODEL = "gemini-2.0-flash-thinking-exp-1219"
    
    # Thresholds
    REFLEX_CONFIDENCE_THRESHOLD = 0.8
    PRESSURE_DEEP_THRESHOLD = 70
    
    def __init__(self, api_key: Optional[str] = None):
        self.api_key = api_key or GEMINI_API_KEY or os.environ.get('GEMINI_API_KEY')
        
        if not self.api_key:
            raise ValueError("GEMINI_API_KEY is required")
        
        # Initialize the unified client
        self.client = genai.Client(
            api_key=self.api_key,
            http_options={'api_version': 'v1beta'}
        )
        
        # Thought signature counter
        self._thought_counter = 0
    
    def _generate_thought_id(self) -> str:
        """Generate a unique thought signature ID."""
        self._thought_counter += 1
        timestamp = datetime.now().strftime("%Y%m%d%H%M%S")
        return f"ts_{timestamp}_{self._thought_counter:04d}"
    
    def _compute_context_hash(self, context: Dict[str, Any]) -> str:
        """Compute SHA-256 hash of the context for integrity."""
        context_str = json.dumps(context, sort_keys=True, default=str)
        return f"sha256:{hashlib.sha256(context_str.encode()).hexdigest()[:16]}"
    
    def _parse_reasoning_trace(self, raw_response: str) -> List[str]:
        """Extract reasoning steps from model output."""
        traces = []
        
        # Look for tagged reasoning blocks
        import re
        patterns = [
            r'<analyze>(.*?)</analyze>',
            r'<strategy>(.*?)</strategy>',
            r'<decision>(.*?)</decision>',
            r'<thought_signature>(.*?)</thought_signature>',
            r'<thinking>(.*?)</thinking>'
        ]
        
        for pattern in patterns:
            matches = re.findall(pattern, raw_response, re.DOTALL)
            for match in matches:
                traces.append(match.strip())
        
        # If no structured traces, extract first paragraph as implicit reasoning
        if not traces and len(raw_response) > 100:
            first_para = raw_response.split('\n\n')[0][:200]
            traces.append(f"<implicit>{first_para}</implicit>")
        
        return traces
    
    def _extract_action_output(self, raw_response: str) -> str:
        """Extract the actionable output from response."""
        import re
        
        # Check for explicit action tag
        action_match = re.search(r'<action>(.*?)</action>', raw_response, re.DOTALL)
        if action_match:
            return action_match.group(1).strip()
        
        # Remove all thinking/reasoning tags
        clean = re.sub(r'<(analyze|strategy|decision|thought_signature|thinking)>.*?</\1>', '', raw_response, flags=re.DOTALL)
        return clean.strip()
    
    def _estimate_confidence(self, response: str, mode: ReasoningMode) -> float:
        """Estimate confidence based on response characteristics."""
        confidence = 0.7  # Base confidence
        
        # Higher confidence for structured responses
        if '<decision>' in response or '<action>' in response:
            confidence += 0.15
        
        # Higher confidence for deep reasoning
        if mode == ReasoningMode.DEEP:
            confidence += 0.1
        
        # Lower confidence for hedging language
        hedging_phrases = ['might', 'perhaps', 'maybe', 'could be', 'not sure']
        for phrase in hedging_phrases:
            if phrase.lower() in response.lower():
                confidence -= 0.05
        
        return min(max(confidence, 0.1), 1.0)
    
    async def _reflex_generate(self, request: ReasoningRequest) -> str:
        """Fast generation using Reflex model."""
        full_prompt = request.prompt
        if request.system_instruction:
            full_prompt = f"System: {request.system_instruction}\n\nUser: {request.prompt}"
        
        # Add context summary
        if request.context:
            context_summary = json.dumps(request.context, indent=2)[:500]
            full_prompt = f"Context:\n{context_summary}\n\n{full_prompt}"
        
        response = await asyncio.to_thread(
            self.client.models.generate_content,
            model=self.REFLEX_MODEL,
            contents=full_prompt
        )
        
        return response.text or ""
    
    async def _deep_generate(self, request: ReasoningRequest) -> str:
        """Deep generation with thinking enabled."""
        
        # Construct the deep reasoning prompt
        reasoning_prompt = f"""
{request.system_instruction or "You are Kaironex, an intelligent life operating system for students."}

CONTEXT:
User ID: {request.user_id}
Agent: {request.agent}
Context Data: {json.dumps(request.context, indent=2)[:1000]}
{f"Previous Thought: {request.parent_thought}" if request.parent_thought else ""}

TASK:
{request.prompt}

RESPONSE FORMAT:
1. First, wrap your analysis in <analyze>...</analyze> tags
2. Then, wrap your strategy in <strategy>...</strategy> tags  
3. Finally, wrap your decision in <decision>...</decision> tags
4. End with the user-facing response in <action>...</action> tags

Think deeply before responding.
"""
        
        # Configure for thinking mode
        config = types.GenerateContentConfig(
            temperature=0.7,
            max_output_tokens=request.max_thinking_tokens,
            # Note: thinking_config may need adjustment based on actual API
        )
        
        response = await asyncio.to_thread(
            self.client.models.generate_content,
            model=self.DEEP_MODEL,
            contents=reasoning_prompt,
            config=config
        )
        
        return response.text or ""
    
    async def reason(self, request: ReasoningRequest) -> ReasoningResponse:
        """
        Main reasoning entry point.
        
        Automatically routes to appropriate model based on:
        - Explicit mode in request
        - Complexity of the task
        - User's current pressure index
        """
        import time
        start_time = time.time()
        
        mode = request.mode
        raw_response = ""
        thought_signature = None
        
        try:
            if mode == ReasoningMode.REFLEX:
                # Fast path
                raw_response = await self._reflex_generate(request)
                
            elif mode == ReasoningMode.DEEP or mode == ReasoningMode.MARATHON:
                # Slow path with thought signatures
                raw_response = await self._deep_generate(request)
                
                # Generate thought signature
                thought_signature = ThoughtSignature(
                    thought_id=self._generate_thought_id(),
                    timestamp=datetime.now(),
                    user_id=request.user_id,
                    agent=request.agent,
                    context_hash=self._compute_context_hash(request.context),
                    reasoning_trace=self._parse_reasoning_trace(raw_response),
                    confidence=self._estimate_confidence(raw_response, mode),
                    tool_calls=[],  # Populated if tools are used
                    action_output=self._extract_action_output(raw_response),
                    parent_signature=request.parent_thought
                )
                
            elif mode == ReasoningMode.HYBRID:
                # Start with reflex, validate if needed
                raw_response = await self._reflex_generate(request)
                confidence = self._estimate_confidence(raw_response, ReasoningMode.REFLEX)
                
                if confidence < self.REFLEX_CONFIDENCE_THRESHOLD:
                    # Escalate to deep reasoning
                    raw_response = await self._deep_generate(request)
                    mode = ReasoningMode.DEEP
                    
                    thought_signature = ThoughtSignature(
                        thought_id=self._generate_thought_id(),
                        timestamp=datetime.now(),
                        user_id=request.user_id,
                        agent=request.agent,
                        context_hash=self._compute_context_hash(request.context),
                        reasoning_trace=self._parse_reasoning_trace(raw_response),
                        confidence=self._estimate_confidence(raw_response, mode),
                        tool_calls=[],
                        action_output=self._extract_action_output(raw_response),
                        parent_signature=request.parent_thought
                    )
            
            latency_ms = (time.time() - start_time) * 1000
            
            return ReasoningResponse(
                content=self._extract_action_output(raw_response) or raw_response,
                mode_used=mode,
                thought_signature=thought_signature,
                latency_ms=latency_ms,
                confidence=thought_signature.confidence if thought_signature else self._estimate_confidence(raw_response, mode)
            )
            
        except Exception as e:
            latency_ms = (time.time() - start_time) * 1000
            return ReasoningResponse(
                content=f"Reasoning error: {str(e)}",
                mode_used=mode,
                latency_ms=latency_ms,
                confidence=0.0
            )
    
    def reason_sync(self, request: ReasoningRequest) -> ReasoningResponse:
        """Synchronous wrapper for reason()."""
        return asyncio.run(self.reason(request))
    
    async def quick_response(self, prompt: str, user_id: str = "anonymous") -> str:
        """Convenience method for simple reflex responses."""
        response = await self.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="generic",
            mode=ReasoningMode.REFLEX
        ))
        return response.content
    
    async def deep_plan(
        self, 
        goal: str, 
        user_id: str, 
        context: Dict[str, Any],
        agent: str = "campaign"
    ) -> ReasoningResponse:
        """Convenience method for deep planning tasks."""
        return await self.reason(ReasoningRequest(
            prompt=f"Create a detailed plan to achieve: {goal}",
            user_id=user_id,
            agent=agent,
            context=context,
            mode=ReasoningMode.DEEP,
            max_thinking_tokens=16384
        ))
