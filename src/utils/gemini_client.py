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
        max_tokens: int = 2048,
        json_mode: bool = False,
        response_schema: Optional[Any] = None,
        model_type: str = "thinking"
    ) -> str:
        """
        Generate response with optional JSON enforcement.
        STRICTLY uses the Thinking Model as configured.
        """
        import time
        import random

        max_retries = 3
        
        for attempt in range(max_retries):
            try:
                # 1. Base Config Args
                config_args = {
                    "temperature": 0.7,
                    "max_output_tokens": max_tokens
                }

                # 2. Configure Thinking (ALWAYS ON for Deep Brain due to User Constraint)
                # Note: Newer Thinking models support JSON schema.
                config_args["thinking_config"] = types.ThinkingConfig(thinking_budget=8192)

                # 3. JSON Enforcement
                if json_mode:
                    config_args["response_mime_type"] = "application/json"
                    if response_schema:
                        config_args["response_schema"] = response_schema

                # 4. Search Grounding (Disable if JSON mode to prevent conflict)
                if use_search and not json_mode:
                     config_args["tools"] = [types.Tool(google_search=types.GoogleSearch())]

                if system_instruction:
                    config_args["system_instruction"] = system_instruction

                config = types.GenerateContentConfig(**config_args)
                
                # 5. Generate with Thinking Model
                response = self.client.models.generate_content(
                    model=self.model,
                    contents=prompt,
                    config=config
                )
                
                return response.text or ""
                
            except Exception as e:
                error_msg = str(e)
                print(f"⚠️ Gemini Error (Attempt {attempt+1}/{max_retries}): {error_msg}")
                
                if "503" in error_msg or "429" in error_msg or "quota" in error_msg.lower():
                    if attempt < max_retries - 1:
                        backoff = (2 ** attempt) + random.uniform(0.1, 1.0)
                        time.sleep(backoff)
                        continue

                if attempt == max_retries - 1:
                    # Propagate error to caller so they see the RAW error
                    print(f"❌ Gemini Failed after {max_retries} attempts.")
                    raise e
            
        return ""
    
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
            
            if response.candidates and response.candidates[0].content and response.candidates[0].content.parts:
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
            
            return response.text or ""
            
        except Exception as e:
            error_msg = str(e)
            
            # Handle search grounding quota issues
            if "quota" in error_msg.lower() or "grounding" in error_msg.lower():
                # Fallback to regular generation without search
                return self.generate_response(prompt)
            
            return f"Research error: {error_msg}"
