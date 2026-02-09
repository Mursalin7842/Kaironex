
import asyncio
import base64
import json
import os
import sys
from dotenv import load_dotenv

# Add project root to path
sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))

# Load environment variables from .env file
load_dotenv()

# Mock classes to simulate Appwrite Function environment
class MockRes:
    def json(self, data, status=200):
        return {"status": status, "data": data}

class MockReq:
    def __init__(self, path, method, body):
        self.path = path
        self.method = method
        self.body = body
        self.query = {}

class MockContext:
    def __init__(self, path, method, body):
        self.req = MockReq(path, method, body)
        self.res = MockRes()
        
    def log(self, msg):
        print(f"[LOG] {msg}")
    
    def error(self, msg):
        print(f"[ERROR] {msg}")

async def test_resume_upload(file_path):
    from src.main import main
    
    if not os.path.exists(file_path):
        print(f"File not found: {file_path}")
        return

    print(f"Reading file: {file_path}")
    with open(file_path, "rb") as f:
        pdf_bytes = f.read()
        pdf_base64 = base64.b64encode(pdf_bytes).decode('utf-8')
    
    print(f"Encoded PDF size: {len(pdf_base64)} chars")

    # Construct Payload matching Android App
    payload = {
        "type": "analyze_resume",
        "userId": "local_test_user",
        "data": {
            "resumePdf": pdf_base64, # Matches Android 'resumePdfBase64' key mapping if aligned, or we align key here
            "jobDesc": "Looking for a Software Engineer with Python and Kotlin experience.",
            "resumeText": "" # Android sends empty string if PDF is used
        }
    }
    
    # Android sends to /campaign
    context = MockContext("/campaign", "POST", payload)
    
    print("🚀 Triggering Campaign Agent (Local)...")
    try:
        result = await main(context)
        print("\n✅ Execution Result:")
        print(json.dumps(result, indent=2))
        
        if result.get('status') == 200:
            print("\n🎉 Success! The logic is working locally.")
        else:
            print("\n❌ Failed with non-200 status.")
            
    except Exception as e:
        print(f"\n💥 Error during execution: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python test_resume_local.py <path_to_pdf>")
    else:
        asyncio.run(test_resume_upload(sys.argv[1]))
