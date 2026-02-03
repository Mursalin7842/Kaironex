"""
💭 THOUGHT MANAGER
==================
Persistent storage and retrieval of thought signatures.

Thought signatures are the cryptographic proof of AI reasoning,
enabling:
- Multi-turn conversation integrity
- Marathon agent state recovery
- Debugging and auditing
- Reinforcement learning from past decisions
"""

import json
import hashlib
from typing import Optional, List, Dict, Any
from datetime import datetime, timedelta
from dataclasses import dataclass

from .bicameral_engine import ThoughtSignature
from ..config import APPWRITE_DATABASE_ID, AGENT_MEMORY_COL


@dataclass
class ThoughtChain:
    """A linked chain of thought signatures for a session."""
    chain_id: str
    user_id: str
    agent: str
    thoughts: List[ThoughtSignature]
    created_at: datetime
    last_active: datetime
    status: str  # 'active', 'complete', 'abandoned'
    
    @property
    def depth(self) -> int:
        return len(self.thoughts)
    
    @property
    def total_confidence(self) -> float:
        if not self.thoughts:
            return 0.0
        return sum(t.confidence for t in self.thoughts) / len(self.thoughts)
    
    def get_latest(self) -> Optional[ThoughtSignature]:
        return self.thoughts[-1] if self.thoughts else None
    
    def get_reasoning_summary(self) -> str:
        """Get a summary of all reasoning traces."""
        traces = []
        for thought in self.thoughts:
            traces.extend(thought.reasoning_trace)
        return "\n".join(traces[-10:])  # Last 10 traces


class ThoughtManager:
    """
    Manages thought signature persistence and retrieval.
    
    Storage backends:
    - Appwrite (primary, for persistence across restarts)
    - In-memory cache (for fast access during sessions)
    
    Usage:
        manager = ThoughtManager(db_helper)
        
        # Store a thought
        await manager.store(thought_signature)
        
        # Get thought chain for a session
        chain = await manager.get_chain(user_id, session_id)
        
        # Get last thought for continuation
        last = await manager.get_latest(user_id, agent)
    """
    
    # Cache settings
    CACHE_TTL_SECONDS = 3600  # 1 hour
    MAX_CACHE_SIZE = 1000
    
    def __init__(self, db_helper=None):
        self.db = db_helper
        self._cache: Dict[str, ThoughtSignature] = {}
        self._chains: Dict[str, ThoughtChain] = {}
        self._cache_timestamps: Dict[str, datetime] = {}
    
    def _get_cache_key(self, thought_id: str) -> str:
        return f"thought:{thought_id}"
    
    def _get_chain_key(self, user_id: str, agent: str) -> str:
        return f"chain:{user_id}:{agent}"
    
    def _is_cache_valid(self, key: str) -> bool:
        if key not in self._cache_timestamps:
            return False
        age = datetime.now() - self._cache_timestamps[key]
        return age.total_seconds() < self.CACHE_TTL_SECONDS
    
    def _evict_old_cache(self):
        """Remove expired cache entries."""
        now = datetime.now()
        expired = [
            key for key, ts in self._cache_timestamps.items()
            if (now - ts).total_seconds() > self.CACHE_TTL_SECONDS
        ]
        for key in expired:
            self._cache.pop(key, None)
            self._cache_timestamps.pop(key, None)
    
    async def store(self, thought: ThoughtSignature) -> bool:
        """
        Store a thought signature.
        
        1. Add to in-memory cache
        2. Persist to Appwrite
        3. Update thought chain
        """
        try:
            # 1. Cache locally
            cache_key = self._get_cache_key(thought.thought_id)
            self._cache[cache_key] = thought
            self._cache_timestamps[cache_key] = datetime.now()
            
            # 2. Update chain
            chain_key = self._get_chain_key(thought.user_id, thought.agent)
            if chain_key not in self._chains:
                self._chains[chain_key] = ThoughtChain(
                    chain_id=f"chain_{thought.user_id}_{thought.agent}_{datetime.now().strftime('%Y%m%d%H%M%S')}",
                    user_id=thought.user_id,
                    agent=thought.agent,
                    thoughts=[],
                    created_at=datetime.now(),
                    last_active=datetime.now(),
                    status='active'
                )
            
            chain = self._chains[chain_key]
            chain.thoughts.append(thought)
            chain.last_active = datetime.now()
            
            # 3. Persist to Appwrite
            if self.db:
                await self._persist_to_appwrite(thought)
            
            # 4. Cleanup
            if len(self._cache) > self.MAX_CACHE_SIZE:
                self._evict_old_cache()
            
            return True
            
        except Exception as e:
            print(f"ThoughtManager.store error: {e}")
            return False
    
    async def _persist_to_appwrite(self, thought: ThoughtSignature):
        """Persist thought to Appwrite database."""
        if not self.db:
            return
        
        try:
            # Update agent_memory with latest thought
            from appwrite.query import Query
            print(f"🧠 [DEBUG-TM] Persisting thought {thought.thought_id} to Appwrite... DB:{APPWRITE_DATABASE_ID} COL:{AGENT_MEMORY_COL}")
            
            results = self.db.db.list_rows(
                database_id=APPWRITE_DATABASE_ID,
                table_id=AGENT_MEMORY_COL,
                queries=[Query.equal('userId', thought.user_id)]
            )
            
            data = {
                'current_thought_signature': json.dumps(thought.to_dict())[:999999],
                'last_active': thought.timestamp.isoformat(),
                'active_agents': thought.agent
            }
            
            if results['total'] > 0:
                doc_id = results['rows'][0]['$id']
                print(f"🧠 [DEBUG-TM] Updating existing row {doc_id}...")
                self.db.db.update_row(APPWRITE_DATABASE_ID, AGENT_MEMORY_COL, doc_id, data)
            else:
                print(f"🧠 [DEBUG-TM] Creating NEW memory row...")
                data['userId'] = thought.user_id
                data['pressure_index'] = "50"  # String for Appwrite compatibility
                self.db.db.create_row(APPWRITE_DATABASE_ID, AGENT_MEMORY_COL, 'unique()', data)
            
            print(f"🧠 [DEBUG-TM] Successfully persisted thought signature.")
                
        except Exception as e:
            print(f"❌ [DEBUG-TM] Appwrite persist error: {e}")
            import traceback
            traceback.print_exc()
    
    async def get(self, thought_id: str) -> Optional[ThoughtSignature]:
        """Retrieve a thought by ID."""
        cache_key = self._get_cache_key(thought_id)
        
        # Check cache first
        if cache_key in self._cache and self._is_cache_valid(cache_key):
            return self._cache[cache_key]
        
        # TODO: Query Appwrite if not in cache
        return None
    
    async def get_latest(self, user_id: str, agent: str) -> Optional[ThoughtSignature]:
        """Get the most recent thought for a user/agent combo."""
        chain_key = self._get_chain_key(user_id, agent)
        
        if chain_key in self._chains:
            return self._chains[chain_key].get_latest()
        
        # Try loading from Appwrite
        if self.db:
            try:
                from appwrite.query import Query
                
                results = self.db.db.list_rows(
                    database_id=APPWRITE_DATABASE_ID,
                    table_id=AGENT_MEMORY_COL,
                    queries=[Query.equal('userId', user_id)]
                )
                
                if results['total'] > 0:
                    row = results['rows'][0]
                    sig_json = row.get('current_thought_signature')
                    if sig_json:
                        data = json.loads(sig_json)
                        return ThoughtSignature.from_dict(data)
            except Exception as e:
                print(f"Get latest error: {e}")
        
        return None
    
    async def get_chain(self, user_id: str, agent: str) -> Optional[ThoughtChain]:
        """Get the full thought chain for a user/agent."""
        chain_key = self._get_chain_key(user_id, agent)
        return self._chains.get(chain_key)
    
    async def get_reasoning_context(self, user_id: str, agent: str, max_thoughts: int = 5) -> str:
        """
        Get formatted reasoning context for prompt injection.
        
        Returns a string summarizing recent reasoning that can be
        injected into the next prompt for continuity.
        """
        chain = await self.get_chain(user_id, agent)
        
        if not chain or not chain.thoughts:
            return ""
        
        recent = chain.thoughts[-max_thoughts:]
        
        context_parts = ["=== PREVIOUS REASONING CONTEXT ==="]
        
        for i, thought in enumerate(recent, 1):
            context_parts.append(f"\n[Thought {i}] ({thought.timestamp.strftime('%H:%M')})")
            context_parts.append(f"Confidence: {thought.confidence:.2f}")
            if thought.reasoning_trace:
                context_parts.append("Reasoning:")
                for trace in thought.reasoning_trace[:3]:
                    context_parts.append(f"  - {trace[:100]}...")
            context_parts.append(f"Output: {thought.action_output[:200]}...")
        
        context_parts.append("\n=== END CONTEXT ===\n")
        
        return "\n".join(context_parts)
    
    async def clear_chain(self, user_id: str, agent: str):
        """Clear the thought chain (e.g., when a marathon completes)."""
        chain_key = self._get_chain_key(user_id, agent)
        
        if chain_key in self._chains:
            chain = self._chains[chain_key]
            chain.status = 'complete'
            
            # Archive to policy_episodes for learning
            # TODO: Implement policy episode creation
            
            del self._chains[chain_key]
    
    async def get_all_active_chains(self) -> List[ThoughtChain]:
        """Get all currently active thought chains."""
        return [
            chain for chain in self._chains.values()
            if chain.status == 'active'
        ]
    
    async def compute_pressure_index(self, user_id: str) -> int:
        """
        Compute pressure index based on thought history.
        
        Factors:
        - Frequency of low-confidence thoughts
        - Intervention density
        - Failed action patterns
        """
        pressure = 50  # Base pressure
        
        # Check all chains for this user
        user_chains = [
            chain for key, chain in self._chains.items()
            if chain.user_id == user_id
        ]
        
        for chain in user_chains:
            recent = chain.thoughts[-10:]
            
            # Low confidence increases pressure
            avg_conf = sum(t.confidence for t in recent) / len(recent) if recent else 0.7
            if avg_conf < 0.6:
                pressure += 15
            
            # High thought frequency = stressed user
            if len(recent) > 5:
                time_span = (recent[-1].timestamp - recent[0].timestamp).total_seconds()
                if time_span < 300:  # 5+ thoughts in 5 minutes
                    pressure += 10
        
        return min(max(pressure, 0), 100)
    
    def store_sync(self, thought: ThoughtSignature) -> bool:
        """Synchronous wrapper for store()."""
        import asyncio
        return asyncio.run(self.store(thought))
