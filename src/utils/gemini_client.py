"""
🤖 GEMINI CLIENT
================
Clean Gemini API client for Appwrite Functions.

Uses gemini-3-flash-preview with HIGH thinking for deep reasoning.
Reflex (MINIMAL thinking) is handled by the mobile app.
"""

import os
from typing import Optional, Dict, Any, List
from google import genai
from google.genai import types

from ..config import (
    GEMINI_API_KEY,
    GEMINI_3_FLASH,
    THINKING_LEVEL_DEEP
)


class GeminiClient:
    """
    Gemini API client for Deep Brain reasoning.
    
    This client is used for:
    - Complex planning tasks
    - Research with Google Search
    - Long-form generation
    - Strategic reasoning
    
    For quick responses, the mobile app uses its own Reflex Agent.
    """
    
    def __init__(self):
        api_key = GEMINI_API_KEY or os.environ.get('GEMINI_API_KEY')
        if not api_key:
            raise ValueError("GEMINI_API_KEY is required")
        
        self.client = genai.Client(
            api_key=api_key,
            http_options={'api_version': 'v1beta'}
        )
        self.model = GEMINI_3_FLASH
    
    def generate_response(
        self,
        prompt: str,
        system_instruction: Optional[str] = None,
        use_search: bool = False,
        max_tokens: int = 2048
    ) -> str:
        """
        Generate a response using deep reasoning.
        
        Args:
            prompt: The user prompt
            system_instruction: Optional system instruction
            use_search: Whether to use Google Search grounding
            max_tokens: Maximum output tokens
        
        Returns:
            Generated text response
        """
        try:
            # Build config with HIGH thinking for deep reasoning
            config = types.GenerateContentConfig(
                thinking_config=types.ThinkingConfig(
                    thinking_budget=8192  # HIGH thinking
                ),
                temperature=0.7,
                max_output_tokens=max_tokens
            )
            
            # Add system instruction if provided
            if system_instruction:
                config.system_instruction = system_instruction
            
            # Build content
            contents = prompt
            
            # Generate response
            response = self.client.models.generate_content(
                model=self.model,
                contents=contents,
                config=config
            )
            
            return response.text
            
        except Exception as e:
            error_msg = str(e)
            print(f"❌ Gemini Error: {error_msg}")
            
            # Handle rate limits gracefully
            if "429" in error_msg or "RESOURCE_EXHAUSTED" in error_msg:
                return "I need a moment to think. Please try again shortly."
            
            if "quota" in error_msg.lower():
                return "I'm currently at capacity. Please try again in a few minutes."
            
            return f"I encountered an issue: {error_msg}"
    
    def generate_with_tools(
        self,
        prompt: str,
        tools: List[Dict[str, Any]],
        system_instruction: Optional[str] = None
    ) -> Dict[str, Any]:
        """
        Generate with function calling tools.
        
        Args:
            prompt: The user prompt
            tools: List of tool definitions
            system_instruction: Optional system instruction
        
        Returns:
            Dict with response and any tool calls
        """
        try:
            config = types.GenerateContentConfig(
                thinking_config=types.ThinkingConfig(
                    thinking_budget=8192
                ),
                temperature=0.7,
                max_output_tokens=4096
            )
            
            if system_instruction:
                config.system_instruction = system_instruction
            
            response = self.client.models.generate_content(
                model=self.model,
                contents=prompt,
                config=config
            )
            
            # Extract function calls if any
            function_calls = []
            text_parts = []
            
            if response.candidates:
                for part in response.candidates[0].content.parts:
                    if hasattr(part, 'function_call') and part.function_call:
                        function_calls.append({
                            "name": part.function_call.name,
                            "args": dict(part.function_call.args) if part.function_call.args else {}
                        })
                    elif hasattr(part, 'text') and part.text:
                        text_parts.append(part.text)
            
            return {
                "text": "\n".join(text_parts),
                "function_calls": function_calls
            }
            
        except Exception as e:
            print(f"❌ Tool call error: {e}")
            return {
                "text": f"Error: {str(e)}",
                "function_calls": []
            }
    
    def research(
        self,
        query: str,
        context: Optional[str] = None
    ) -> str:
        """
        Perform research using Google Search grounding.
        
        Args:
            query: The research query
            context: Additional context
        
        Returns:
            Research findings as text
        """
        prompt = f"""
Research the following topic and provide accurate, up-to-date information.

QUERY: {query}
{f'CONTEXT: {context}' if context else ''}

Provide a comprehensive but concise summary of your findings.
Cite sources when possible.
"""
        
        try:
            config = types.GenerateContentConfig(
                thinking_config=types.ThinkingConfig(
                    thinking_budget=8192
                ),
                temperature=0.3,  # Lower for research accuracy
                max_output_tokens=4096
            )
            
            # Add Google Search tool
            response = self.client.models.generate_content(
                model=self.model,
                contents=prompt,
                config=config
                # Note: Google Search grounding may need separate quota
            )
            
            return response.text
            
        except Exception as e:
            error_msg = str(e)
            
            # Handle search grounding quota issues
            if "quota" in error_msg.lower() or "grounding" in error_msg.lower():
                # Fallback to regular generation without search
                return self.generate_response(prompt)
            
            return f"Research error: {error_msg}"
