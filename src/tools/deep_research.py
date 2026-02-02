"""
🔬 DEEP RESEARCH ENGINE
========================
Gemini 3 Deep Research API integration for autonomous research.

This is a FLAGSHIP FEATURE for the hackathon!
- Uses Gemini 3 Flash with Google Search grounding
- Autonomously researches syllabi, forums, resources
- Creates comprehensive research reports
- Uses HIGH thinking level for deep analysis

"The AI that does your research while you sleep."
"""

import os
import json
import asyncio
from typing import Dict, Any, List, Optional
from dataclasses import dataclass, field
from datetime import datetime
from enum import Enum

from google import genai
from google.genai import types

from ..config import GEMINI_API_KEY, GEMINI_3_FLASH
from ..utils.rate_limited_client import rate_limited_generate, check_quota


class ResearchType(str, Enum):
    """Types of research tasks."""
    SYLLABUS_ANALYSIS = "syllabus_analysis"
    TOPIC_DEEP_DIVE = "topic_deep_dive"
    CAREER_RESEARCH = "career_research"
    UNIVERSITY_FORUMS = "university_forums"
    BEST_RESOURCES = "best_resources"
    EXAM_PREP = "exam_prep"
    INTERNSHIP_HUNT = "internship_hunt"


class ResearchStatus(str, Enum):
    """Status of a research task."""
    PENDING = "pending"
    IN_PROGRESS = "in_progress"
    BROWSING = "browsing"
    ANALYZING = "analyzing"
    COMPLETED = "completed"
    FAILED = "failed"


@dataclass
class ResearchQuery:
    """A research query for the Deep Research agent."""
    query_id: str
    user_id: str
    research_type: ResearchType
    topic: str
    context: Dict[str, Any] = field(default_factory=dict)
    constraints: List[str] = field(default_factory=list)
    max_sources: int = 10
    include_forums: bool = True
    created_at: datetime = field(default_factory=datetime.now)


@dataclass
class ResearchSource:
    """A source found during research."""
    url: str
    title: str
    snippet: str
    relevance_score: float
    source_type: str  # website, forum, academic, video
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            "url": self.url,
            "title": self.title,
            "snippet": self.snippet,
            "relevance_score": self.relevance_score,
            "source_type": self.source_type
        }


@dataclass
class ResearchReport:
    """Result of a deep research task."""
    query_id: str
    user_id: str
    research_type: ResearchType
    topic: str
    status: ResearchStatus
    summary: str
    key_findings: List[str]
    sources: List[ResearchSource]
    recommendations: List[str]
    thinking_summaries: List[str]
    total_sources_analyzed: int
    research_duration_seconds: float
    created_at: datetime = field(default_factory=datetime.now)
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            "query_id": self.query_id,
            "user_id": self.user_id,
            "research_type": self.research_type.value,
            "topic": self.topic,
            "status": self.status.value,
            "summary": self.summary,
            "key_findings": self.key_findings,
            "sources": [s.to_dict() for s in self.sources],
            "recommendations": self.recommendations,
            "thinking_summaries": self.thinking_summaries,
            "total_sources_analyzed": self.total_sources_analyzed,
            "research_duration_seconds": self.research_duration_seconds,
            "created_at": self.created_at.isoformat()
        }


class DeepResearchEngine:
    """
    Gemini 3 Deep Research API integration.
    
    This autonomously:
    - Searches the web for relevant information
    - Browses multiple sources
    - Synthesizes findings into actionable insights
    
    Usage:
        engine = DeepResearchEngine()
        report = await engine.research(ResearchQuery(
            query_id="q1",
            user_id="user_123",
            research_type=ResearchType.SYLLABUS_ANALYSIS,
            topic="CS 201 Data Structures",
            context={"university": "Stanford", "professor": "Dr. Smith"}
        ))
    """
    
    # Use Gemini 3 Flash for research (with Google Search grounding)
    RESEARCH_MODEL = GEMINI_3_FLASH
    
    def __init__(self, api_key: Optional[str] = None, db=None):
        self.api_key = api_key or GEMINI_API_KEY or os.environ.get('GEMINI_API_KEY')
        self.db = db
        
        if not self.api_key:
            raise ValueError("GEMINI_API_KEY is required")
        
        self.client = genai.Client(
            api_key=self.api_key,
            http_options={'api_version': 'v1beta'}
        )
        
        # Track ongoing research
        self._active_research: Dict[str, ResearchQuery] = {}
        self._callbacks: Dict[str, List[Any]] = {}
    
    async def research(
        self,
        query: ResearchQuery,
        progress_callback: Optional[Any] = None
    ) -> ResearchReport:
        """
        Execute a deep research task.
        
        Args:
            query: The research query to execute
            progress_callback: Optional callback for progress updates
        
        Returns:
            ResearchReport with findings
        """
        import time
        start_time = time.time()
        
        self._active_research[query.query_id] = query
        thinking_summaries = []
        
        if progress_callback:
            await progress_callback(query.query_id, ResearchStatus.IN_PROGRESS, "Starting research...")
        
        try:
            # Build the research prompt based on type
            research_prompt = self._build_research_prompt(query)
            
            if progress_callback:
                await progress_callback(query.query_id, ResearchStatus.BROWSING, "Searching the web...")
            
            # Try with Google Search grounding first, fallback to non-grounded if quota exceeded
            use_grounding = True
            response = None
            
            for attempt in range(2):  # Try grounded, then non-grounded
                try:
                    # Configure based on whether we're using grounding
                    if use_grounding:
                        config = types.GenerateContentConfig(
                            temperature=0.3,  # Lower temp for factual research
                            max_output_tokens=16384,
                            thinking_config=types.ThinkingConfig(
                                thinking_level=types.ThinkingLevel.HIGH,  # Maximum reasoning for research
                                include_thoughts=True
                            ),
                            # Enable grounding with Google Search - CRITICAL for research!
                            tools=[
                                types.Tool(google_search=types.GoogleSearch())
                            ]
                        )
                    else:
                        # Fallback: No grounding, but still use deep thinking
                        config = types.GenerateContentConfig(
                            temperature=0.3,
                            max_output_tokens=16384,
                            thinking_config=types.ThinkingConfig(
                                thinking_level=types.ThinkingLevel.HIGH,
                                include_thoughts=True
                            )
                        )
                        if progress_callback:
                            await progress_callback(query.query_id, ResearchStatus.ANALYZING, "Using knowledge-based research (grounding quota exceeded)...")
                    
                    # Use rate-limited generate to respect API quotas (5 RPM, 20 RPD)
                    response = await rate_limited_generate(
                        self.client,
                        model=self.RESEARCH_MODEL,
                        contents=research_prompt,
                        config=config
                    )
                    break  # Success, exit retry loop
                    
                except Exception as e:
                    error_str = str(e).lower()
                    # If grounded search quota exceeded, retry without grounding
                    if use_grounding and ('429' in str(e) or 'quota' in error_str or 'resource_exhausted' in error_str):
                        print(f"⚠️ Google Search grounding quota exceeded, retrying without grounding...")
                        use_grounding = False
                        continue
                    else:
                        raise  # Re-raise other errors
            
            if response is None:
                raise Exception("Failed to get response after retries")
            
            if progress_callback:
                await progress_callback(query.query_id, ResearchStatus.ANALYZING, "Analyzing findings...")
            
            # Extract response text
            raw_text = response.text or ""
            
            # Extract thinking traces if available
            if hasattr(response, 'candidates') and response.candidates:
                candidate = response.candidates[0]
                # Try multiple attribute names for thinking content
                try:
                    thinking_content = None
                    for attr_name in ['thinking_content', 'thinking', 'thought_summary', 'reasoning']:
                        if hasattr(candidate, attr_name):
                            thinking_content = getattr(candidate, attr_name)
                            break
                    if thinking_content:
                        thinking_summaries.append(str(thinking_content))
                except Exception:
                    pass
            
            # Parse the research results
            report = self._parse_research_response(
                query=query,
                raw_response=raw_text,
                thinking_summaries=thinking_summaries,
                duration=time.time() - start_time
            )
            
            if progress_callback:
                await progress_callback(query.query_id, ResearchStatus.COMPLETED, "Research complete!")
            
            return report
            
        except Exception as e:
            duration = time.time() - start_time
            
            if progress_callback:
                await progress_callback(query.query_id, ResearchStatus.FAILED, f"Error: {str(e)}")
            
            return ResearchReport(
                query_id=query.query_id,
                user_id=query.user_id,
                research_type=query.research_type,
                topic=query.topic,
                status=ResearchStatus.FAILED,
                summary=f"Research failed: {str(e)}",
                key_findings=[],
                sources=[],
                recommendations=["Try a more specific query", "Check your internet connection"],
                thinking_summaries=thinking_summaries,
                total_sources_analyzed=0,
                research_duration_seconds=duration
            )
        
        finally:
            if query.query_id in self._active_research:
                del self._active_research[query.query_id]
    
    def _build_research_prompt(self, query: ResearchQuery) -> str:
        """Build the research prompt based on query type."""
        
        base_context = f"""
You are a research assistant for Kaironex, a Student Life Operating System.
You are researching on behalf of a student.

USER ID: {query.user_id}
RESEARCH TYPE: {query.research_type.value}
TOPIC: {query.topic}
CONTEXT: {json.dumps(query.context)}
CONSTRAINTS: {', '.join(query.constraints) if query.constraints else 'None'}
"""
        
        type_prompts = {
            ResearchType.SYLLABUS_ANALYSIS: f"""
{base_context}

TASK: Analyze the syllabus for "{query.topic}"

RESEARCH STEPS:
1. Search for the official syllabus or course page
2. Identify the main topics covered
3. Find the hardest concepts based on student forums (Reddit, Course Hero, etc.)
4. Identify common exam questions and pain points
5. Find supplementary resources (YouTube videos, tutorials)

OUTPUT FORMAT:
## Course Overview
[Brief description]

## Key Topics (in order of difficulty)
1. [Topic] - [Why it's challenging]
...

## Common Pain Points (from student forums)
- [Pain point 1]
- [Pain point 2]

## Recommended Resources
1. [Resource] - [URL] - [Why it helps]
...

## Study Strategy
[Personalized recommendations]
""",
            
            ResearchType.TOPIC_DEEP_DIVE: f"""
{base_context}

TASK: Deep dive into "{query.topic}"

RESEARCH STEPS:
1. Search for comprehensive explanations
2. Find visual aids and diagrams
3. Locate practice problems
4. Find real-world applications
5. Identify prerequisites

OUTPUT FORMAT:
## Concept Explained Simply
[ELI5 explanation]

## The Full Picture
[Detailed explanation]

## Visual Resources
1. [Resource] - [URL]
...

## Practice Problems
[Where to find them]

## Real-World Applications
[How this is used]
""",
            
            ResearchType.CAREER_RESEARCH: f"""
{base_context}

TASK: Research career opportunities related to "{query.topic}"

RESEARCH STEPS:
1. Search for job postings matching skills
2. Find salary information
3. Identify required skills and certifications
4. Find internship opportunities
5. Research company cultures

OUTPUT FORMAT:
## Career Overview
[Description of career path]

## In-Demand Skills
1. [Skill] - [Importance level]
...

## Top Companies Hiring
1. [Company] - [Why they're great] - [Careers URL]
...

## Salary Expectations
- Entry level: $X
- Mid-level: $Y
- Senior: $Z

## Internship Opportunities
[Current openings or where to find them]
""",
            
            ResearchType.EXAM_PREP: f"""
{base_context}

TASK: Prepare exam strategy for "{query.topic}"

RESEARCH STEPS:
1. Find past exams and practice tests
2. Identify frequently tested concepts
3. Find study guides and cheat sheets
4. Locate tutoring resources
5. Find study group communities

OUTPUT FORMAT:
## Exam Format
[What to expect]

## Most Tested Topics
1. [Topic] - [Frequency] - [Study tip]
...

## Practice Resources
1. [Resource] - [URL]
...

## Study Schedule Recommendation
[Based on typical preparation needs]

## Last-Minute Tips
[Quick wins before the exam]
""",
            
            ResearchType.INTERNSHIP_HUNT: f"""
{base_context}

TASK: Find internship opportunities for "{query.topic}"

RESEARCH STEPS:
1. Search major job boards (LinkedIn, Indeed, Handshake)
2. Find company career pages
3. Identify application deadlines
4. Find networking opportunities
5. Research interview preparation

OUTPUT FORMAT:
## Current Openings
1. [Company] - [Role] - [Application URL] - [Deadline if known]
...

## Companies Known for Good Internships
1. [Company] - [Why]
...

## Application Tips
[Industry-specific advice]

## Interview Preparation
[Common questions and topics]

## Networking Opportunities
[Events, communities, etc.]
"""
        }
        
        return type_prompts.get(query.research_type, f"""
{base_context}

TASK: Research "{query.topic}"

Please provide:
1. Key findings from web search
2. Relevant sources with URLs
3. Actionable recommendations

Be thorough but concise.
""")
    
    def _parse_research_response(
        self,
        query: ResearchQuery,
        raw_response: str,
        thinking_summaries: List[str],
        duration: float
    ) -> ResearchReport:
        """Parse the raw response into a structured report."""
        import re
        
        # Extract key findings (lines starting with numbers or bullets)
        key_findings = []
        finding_patterns = [
            r'^\d+\.\s+(.+)$',
            r'^[-•]\s+(.+)$',
            r'^##\s+(.+)$'
        ]
        
        for line in raw_response.split('\n'):
            for pattern in finding_patterns:
                match = re.match(pattern, line.strip())
                if match and len(key_findings) < 10:
                    key_findings.append(match.group(1))
                    break
        
        # Extract URLs as sources
        sources = []
        url_pattern = r'https?://[^\s\)>\]\'"]+' 
        urls_found = re.findall(url_pattern, raw_response)
        
        for i, url in enumerate(urls_found[:query.max_sources]):
            # Try to extract title from context
            url_context = raw_response[max(0, raw_response.find(url)-100):raw_response.find(url)+len(url)]
            title_match = re.search(r'\[([^\]]+)\]', url_context)
            title = title_match.group(1) if title_match else f"Source {i+1}"
            
            sources.append(ResearchSource(
                url=url,
                title=title,
                snippet=url_context[:100] if url_context else "",
                relevance_score=1.0 - (i * 0.1),  # Decay by order
                source_type="website"
            ))
        
        # Extract recommendations
        recommendations = []
        rec_section = re.search(r'(?:Recommend|Tips|Strategy|Action).*?:(.*?)(?:##|$)', raw_response, re.IGNORECASE | re.DOTALL)
        if rec_section:
            rec_lines = rec_section.group(1).strip().split('\n')
            for line in rec_lines[:5]:
                if line.strip() and len(line.strip()) > 10:
                    recommendations.append(line.strip())
        
        # Create summary (first paragraph or first 500 chars)
        summary = raw_response[:500].split('\n\n')[0]
        if len(summary) > 400:
            summary = summary[:400] + "..."
        
        return ResearchReport(
            query_id=query.query_id,
            user_id=query.user_id,
            research_type=query.research_type,
            topic=query.topic,
            status=ResearchStatus.COMPLETED,
            summary=summary,
            key_findings=key_findings if key_findings else ["Research completed - see full response"],
            sources=sources,
            recommendations=recommendations if recommendations else ["Review the findings and create a study plan"],
            thinking_summaries=thinking_summaries,
            total_sources_analyzed=len(sources),
            research_duration_seconds=duration
        )
    
    # =========================================================================
    # CONVENIENCE METHODS
    # =========================================================================
    
    async def analyze_syllabus(
        self,
        user_id: str,
        course_name: str,
        university: Optional[str] = None,
        professor: Optional[str] = None
    ) -> ResearchReport:
        """Convenience method to analyze a course syllabus."""
        import uuid
        
        query = ResearchQuery(
            query_id=f"syllabus_{uuid.uuid4().hex[:8]}",
            user_id=user_id,
            research_type=ResearchType.SYLLABUS_ANALYSIS,
            topic=course_name,
            context={
                "university": university,
                "professor": professor
            } if university or professor else {}
        )
        
        return await self.research(query)
    
    async def find_study_resources(
        self,
        user_id: str,
        topic: str,
        difficulty_level: str = "intermediate"
    ) -> ResearchReport:
        """Find the best study resources for a topic."""
        import uuid
        
        query = ResearchQuery(
            query_id=f"resources_{uuid.uuid4().hex[:8]}",
            user_id=user_id,
            research_type=ResearchType.BEST_RESOURCES,
            topic=topic,
            context={"difficulty": difficulty_level}
        )
        
        return await self.research(query)
    
    async def prepare_for_exam(
        self,
        user_id: str,
        exam_subject: str,
        exam_date: Optional[str] = None
    ) -> ResearchReport:
        """Prepare a comprehensive exam prep guide."""
        import uuid
        
        query = ResearchQuery(
            query_id=f"exam_{uuid.uuid4().hex[:8]}",
            user_id=user_id,
            research_type=ResearchType.EXAM_PREP,
            topic=exam_subject,
            context={"exam_date": exam_date} if exam_date else {}
        )
        
        return await self.research(query)
    
    async def hunt_internships(
        self,
        user_id: str,
        role: str,
        location: Optional[str] = None,
        skills: Optional[List[str]] = None
    ) -> ResearchReport:
        """Search for internship opportunities."""
        import uuid
        
        query = ResearchQuery(
            query_id=f"intern_{uuid.uuid4().hex[:8]}",
            user_id=user_id,
            research_type=ResearchType.INTERNSHIP_HUNT,
            topic=role,
            context={
                "location": location,
                "skills": skills
            } if location or skills else {}
        )
        
        return await self.research(query)
