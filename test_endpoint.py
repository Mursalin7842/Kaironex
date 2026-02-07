"""
Test the Campaign Agent endpoint locally
"""
import asyncio
import json

# Mock context for testing
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

async def test():
    from src.main import main
    
    # Test payload for analyze_resume
    payload = {
        "type": "analyze_resume",
        "userId": "test_user_001",
        "data": {
            "resumeText": "John Doe - Software Engineer with 5 years of experience in Python, Java, Kotlin. Built microservices, REST APIs.",
            "jobDesc": "Looking for a Software Engineer with Python and Kotlin experience. AWS knowledge preferred."
        }
    }
    
    context = MockContext("/campaign", "POST", payload)
    
    print("Testing /campaign endpoint with analyze_resume...")
    try:
        result = await main(context)
        print(f"Result: {json.dumps(result, indent=2)}")
    except Exception as e:
        print(f"Error: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    asyncio.run(test())
