import os
from google import genai

from ..config import GEMINI_MODEL_NAME

class GeminiClient:
    def __init__(self):
        # 1. Setup API Key
        api_key = os.environ.get('GEMINI_API_KEY')
        if not api_key:
            print("⚠️ Warning: GEMINI_API_KEY not found.")
        
        self.client = genai.Client(api_key=api_key, http_options={'api_version': 'v1beta'})


        
        try:
            self.model_name = GEMINI_MODEL_NAME
            # Verify if model exists or just set it
            pass
        except Exception as e:
            print(f"⚠️ Model Setup Error: {e}")
            self.model_name = 'gemini-1.5-flash'


    def generate_response(self, prompt, system_instruction=None):
        """Generates a text response."""
        try:
            full_prompt = prompt
            if system_instruction:
                full_prompt = f"System Instruction: {system_instruction}\n\nUser: {prompt}"
                
            response = self.client.models.generate_content(
                model=self.model_name,
                contents=full_prompt
            )
            return response.text

        except Exception as e:
            error_msg = str(e)
            print(f"Gemini Error: {error_msg}")
            
            if "429" in error_msg:
                return "My creative energy is low right now (Rate Limit)."
            return "I'm having trouble connecting to my creative center."
