"""
🔍 WEB SEARCH TOOLS
===================
Search grounding tools for Kaironex agents.

Uses Gemini's search grounding capability to find
real-time information from the web.
"""

import json
from typing import Dict, Any, List, Optional
from dataclasses import dataclass, asdict
from datetime import datetime
from enum import Enum


class SearchIntent(str, Enum):
    """Types of search intents."""
    ACADEMIC = "academic"          # Educational content
    NEWS = "news"                  # Current events
    CAREER = "career"              # Job/internship search
    LOCAL = "local"                # Location-based search
    RESOURCE = "resource"          # Tools, software, resources
    FACTUAL = "factual"            # Quick facts


@dataclass
class SearchResult:
    """A single search result."""
    title: str
    url: str
    snippet: str
    source: str
    relevance_score: float = 0.0
    metadata: Optional[Dict[str, Any]] = None
    
    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


@dataclass
class SearchResponse:
    """Complete search response."""
    query: str
    intent: SearchIntent
    results: List[SearchResult]
    summary: Optional[str] = None
    timestamp: Optional[datetime] = None
    
    def __post_init__(self):
        if self.timestamp is None:
            self.timestamp = datetime.now()
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            "query": self.query,
            "intent": self.intent.value,
            "results": [r.to_dict() for r in self.results],
            "summary": self.summary,
            "timestamp": self.timestamp.isoformat() if self.timestamp else None
        }


class WebSearchTool:
    """
    Web search tool using Gemini's search grounding.
    
    This tool enables agents to fetch real-time information
    from the web for:
    - Academic research
    - Career opportunities
    - Local information
    - Current news
    """
    
    def __init__(self, gemini_client=None):
        self.gemini = gemini_client
    
    def get_tool_definition(self) -> Dict[str, Any]:
        """Return the function calling definition for Gemini."""
        return {
            "name": "web_search",
            "description": "Search the web for current information. Use for research, finding resources, or getting up-to-date facts.",
            "parameters": {
                "type": "object",
                "properties": {
                    "query": {
                        "type": "string",
                        "description": "The search query"
                    },
                    "intent": {
                        "type": "string",
                        "enum": ["academic", "news", "career", "local", "resource", "factual"],
                        "description": "The type of search (academic for educational, career for jobs, etc.)"
                    },
                    "max_results": {
                        "type": "integer",
                        "description": "Maximum number of results to return (default 5)"
                    },
                    "location": {
                        "type": "string",
                        "description": "Optional location for local searches"
                    }
                },
                "required": ["query"]
            }
        }
    
    async def execute(
        self,
        query: str,
        intent: str = "academic",
        max_results: int = 5,
        location: Optional[str] = None,
        user_context: Optional[Dict[str, Any]] = None
    ) -> SearchResponse:
        """
        Execute a web search with Gemini's search grounding.
        
        Args:
            query: Search query
            intent: Type of search
            max_results: Max results to return
            location: Optional location context
            user_context: Optional user state
        
        Returns:
            SearchResponse with results
        """
        search_intent = SearchIntent(intent)
        
        # Enhance query based on intent
        enhanced_query = self._enhance_query(query, search_intent, location)
        
        # In production, this would use Gemini's search grounding
        # The model would be called with google_search tool enabled
        results = await self._perform_search(enhanced_query, search_intent, max_results)
        
        # Generate summary if multiple results
        summary = None
        if len(results) > 1:
            summary = self._generate_summary(query, results)
        
        return SearchResponse(
            query=query,
            intent=search_intent,
            results=results,
            summary=summary
        )
    
    def _enhance_query(
        self,
        query: str,
        intent: SearchIntent,
        location: Optional[str]
    ) -> str:
        """Enhance query based on intent."""
        enhancements = {
            SearchIntent.ACADEMIC: f"{query} tutorial education learn",
            SearchIntent.NEWS: f"{query} news latest 2026",
            SearchIntent.CAREER: f"{query} internship job student visa sponsorship",
            SearchIntent.LOCAL: f"{query} near {location}" if location else query,
            SearchIntent.RESOURCE: f"{query} tool software free student",
            SearchIntent.FACTUAL: query
        }
        return enhancements.get(intent, query)
    
    async def _perform_search(
        self,
        query: str,
        intent: SearchIntent,
        max_results: int
    ) -> List[SearchResult]:
        """
        Perform the actual search.
        
        In production, this would call Gemini with:
        tools=[{"google_search": {}}]
        
        The model would return grounded results.
        """
        # Placeholder results - in production, Gemini handles this
        return [
            SearchResult(
                title=f"Result for: {query}",
                url=f"https://search.google.com/search?q={query.replace(' ', '+')}",
                snippet=f"Search results for {query} will appear here via Gemini search grounding.",
                source="Google Search",
                relevance_score=0.95
            )
        ]
    
    def _generate_summary(self, query: str, results: List[SearchResult]) -> str:
        """Generate a summary of search results."""
        return f"Found {len(results)} relevant results for '{query}'"


class CareerSearchTool(WebSearchTool):
    """
    Specialized search for career/job opportunities.
    
    Features:
    - Student-friendly job filtering
    - Visa sponsorship detection
    - Part-time/flexible hour preferences
    """
    
    def get_tool_definition(self) -> Dict[str, Any]:
        return {
            "name": "career_search",
            "description": "Search for internships, jobs, and career opportunities. Optimized for students with visa/hour restrictions.",
            "parameters": {
                "type": "object",
                "properties": {
                    "role": {
                        "type": "string",
                        "description": "Job role or title"
                    },
                    "location": {
                        "type": "string",
                        "description": "Preferred location or 'remote'"
                    },
                    "requires_sponsorship": {
                        "type": "boolean",
                        "description": "Whether visa sponsorship is needed"
                    },
                    "max_hours_per_week": {
                        "type": "integer",
                        "description": "Maximum hours per week (for students)"
                    },
                    "skills": {
                        "type": "array",
                        "items": {"type": "string"},
                        "description": "Required or preferred skills"
                    }
                },
                "required": ["role"]
            }
        }
    
    async def search_jobs(
        self,
        role: str,
        location: str = "remote",
        requires_sponsorship: bool = False,
        max_hours_per_week: int = 20,
        skills: Optional[List[str]] = None
    ) -> SearchResponse:
        """Search for student-friendly job opportunities."""
        
        # Build specialized query
        query_parts = [role]
        
        if requires_sponsorship:
            query_parts.append("visa sponsorship")
        
        if max_hours_per_week <= 20:
            query_parts.append("part-time student")
        
        if skills:
            query_parts.extend(skills[:3])  # Top 3 skills
        
        query = " ".join(query_parts)
        
        return await self.execute(
            query=query,
            intent="career",
            location=location
        )


class LocalSearchTool(WebSearchTool):
    """
    Specialized search for local information.
    
    Features:
    - Nearby services
    - Student discounts
    - Safe housing
    """
    
    def get_tool_definition(self) -> Dict[str, Any]:
        return {
            "name": "local_search",
            "description": "Search for local services, housing, food, and amenities near the user's location.",
            "parameters": {
                "type": "object",
                "properties": {
                    "query": {
                        "type": "string",
                        "description": "What to search for"
                    },
                    "category": {
                        "type": "string",
                        "enum": ["food", "housing", "study_space", "gym", "transport", "services"],
                        "description": "Category of search"
                    },
                    "location": {
                        "type": "string",
                        "description": "User's current location or area"
                    },
                    "student_discount": {
                        "type": "boolean",
                        "description": "Filter for student discounts"
                    }
                },
                "required": ["query", "location"]
            }
        }
    
    async def search_local(
        self,
        query: str,
        category: str,
        location: str,
        student_discount: bool = False
    ) -> SearchResponse:
        """Search for local amenities and services."""
        
        enhanced_query = query
        if student_discount:
            enhanced_query += " student discount"
        
        return await self.execute(
            query=enhanced_query,
            intent="local",
            location=location
        )
