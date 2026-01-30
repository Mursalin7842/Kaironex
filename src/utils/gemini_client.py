import os
import google.generativeai as genai

class GeminiClient:
    def __init__(self):
        # 1. Setup API Key
        api_key = os.environ.get('GEMINI_API_KEY')
        if not api_key:
            print("⚠️ Warning: GEMINI_API_KEY not found.")
        
        genai.configure(api_key=api_key)
        
        # 2. THE TWIN ENGINE STRATEGY
        # If 'GEMINI_MODEL' is set in .env, use it. Otherwise, default to the safe 1.5 Flash.
        # Options: 'gemini-1.5-flash' (Dev) or 'gemini-3-flash-preview' (Win)
        self.model_name = os.environ.get('GEMINI_MODEL', 'gemini-1.5-flash')
        
        print(f"✨ AI Engine Online: {self.model_name}")
        self.model = genai.GenerativeModel(self.model_name)

    def generate_response(self, prompt, system_instruction=None):
        """Generates a response using the selected engine."""
        try:
            full_prompt = prompt
            if system_instruction:
                full_prompt = f"System Instruction: {system_instruction}\n\nUser: {prompt}"
                
            response = self.model.generate_content(full_prompt)
            return response.text
        except Exception as e:
            # Handle Quota Errors gracefully
            error_msg = str(e)
            if "429" in error_msg:
                print(f"❌ Quota Exceeded on {self.model_name}. Switch to gemini-1.5-flash!")
                return "My creative energy is low right now (Rate Limit)."
            print(f"Gemini Error: {error_msg}")
            return "I'm having trouble connecting to my creative center."
