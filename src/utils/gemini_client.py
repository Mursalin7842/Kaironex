import os
import google.generativeai as genai
from ..config import GEMINI_MODEL_NAME

class GeminiClient:
    def __init__(self):
        # 1. Setup API Key
        api_key = os.environ.get('GEMINI_API_KEY')
        if not api_key:
            print("⚠️ Warning: GEMINI_API_KEY not found.")
        
        genai.configure(api_key=api_key)
        
        # 2. Model Selection
        self.model_name = GEMINI_MODEL_NAME
        print(f"✨ AI Engine Online: {self.model_name}")
        
        try:
            self.model = genai.GenerativeModel(self.model_name)
        except Exception as e:
            print(f"⚠️ Model Init Error: {e}. Falling back to gemini-1.5-flash")
            self.model = genai.GenerativeModel('gemini-1.5-flash')

    def generate_response(self, prompt, system_instruction=None):
        """Generates a text response."""
        try:
            full_prompt = prompt
            if system_instruction:
                full_prompt = f"System Instruction: {system_instruction}\n\nUser: {prompt}"
                
            response = self.model.generate_content(full_prompt)
            return response.text
        except Exception as e:
            error_msg = str(e)
            print(f"Gemini Error: {error_msg}")
            
            if "429" in error_msg:
                return "My creative energy is low right now (Rate Limit)."
            return "I'm having trouble connecting to my creative center."
