import asyncio
import os
from datetime import datetime
from src.core.thought_manager import ThoughtManager, ThoughtSignature
from src.utils.db_helper import KairoDB

async def test_thought_storage():
    print("🧪 Testing Thought Storage...")
    
    # Init DB
    db = KairoDB()
    manager = ThoughtManager(db)
    
    # Create a fake thought
    thought = ThoughtSignature(
        thought_id=f"debug_{datetime.now().strftime('%M%S')}",
        timestamp=datetime.now(),
        user_id="demo_user_001",
        agent="DEBUGGER",
        context_hash="debug_hash",
        reasoning_trace=["Testing storage mechanics"],
        confidence=0.99,
        tool_calls=[],
        action_output="Debug thought storage test",
        parent_signature=None
    )
    
    print(f"📝 Generated thought: {thought.thought_id}")
    
    # Store it
    success = await manager.store(thought)
    
    if success:
        print("✅ Storage reported success.")
    else:
        print("❌ Storage reported failure.")

if __name__ == "__main__":
    asyncio.run(test_thought_storage())
