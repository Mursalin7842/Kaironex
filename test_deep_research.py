"""Quick test for Deep Research fallback."""
import asyncio
from dotenv import load_dotenv
load_dotenv()

from src.tools.deep_research import DeepResearchEngine, ResearchQuery, ResearchType

async def test():
    engine = DeepResearchEngine()
    query = ResearchQuery(
        query_id='test_1',
        user_id='test_user',
        research_type=ResearchType.TOPIC_DEEP_DIVE,
        topic='What are the best study techniques for college exams?'
    )
    print('Testing Deep Research with fallback...')
    result = await engine.research(query)
    print(f'Status: {result.status}')
    print(f'Summary: {result.summary[:300] if result.summary else "No summary"}...')
    print(f'Findings: {len(result.key_findings)}')

if __name__ == "__main__":
    asyncio.run(test())
