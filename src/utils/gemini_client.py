import os
import google.generativeai as genai

class GeminiClient:
    def __init__(self):
        # Ensure GEMINI_API_KEY is set in Appwrite Function Environment Variables
        api_key = os.environ.get('GEMINI_API_KEY')
        if not api_key:
            print("⚠️ Warning: GEMINI_API_KEY not found in environment variables.")
        
        genai.configure(api_key=api_key)
        self.model = genai.GenerativeModel('gemini-1.5-flash') # Using a fast, efficient model

    def generate_response(self, prompt, system_instruction=None):
        """Generates a response from Gemini."""
        try:
            # Note: simplistic implementation. 
            # For system instructions, we often prepend to prompt or use specific API features if available/needed.
            # evolving API: checking if system_instruction is supported directly in this version
            
            full_prompt = prompt
            if system_instruction:
                full_prompt = f"System Instruction: {system_instruction}\n\nUser: {prompt}"
                
            response = self.model.generate_content(full_prompt)
            return response.text
        except Exception as e:
            print(f"Gemini Error: {e}")
            return "I'm having trouble connecting to my creative center right now."
