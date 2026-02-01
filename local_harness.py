import os
import json
import logging
from fastapi import FastAPI, Request, HTTPException
from pydantic import BaseModel
from typing import Dict, Any, Optional
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

# Import your Brain
# Note: Ensure an __init__.py exists in /src for this to work
from src.main import main as kairo_main

# --- MOCKING INFRASTRUCTURE ---
class MockResponse:
    def __init__(self):
        self.body = {}
        self.status_code = 200

    def json(self, data: dict):
        self.body = data
        return data

    def send(self, text: str):
        self.body = text
        return text

class MockContext:
    """Simulates the Appwrite Function Context Object"""
    def __init__(self, body: Any, trigger_type: str):
        self.req = type('obj', (object,), {'body': body})
        self.res = MockResponse()
        self.trigger_type = trigger_type

    def log(self, message: str):
        print(f"🔵 [LOG] {message}")

    def error(self, message: str):
        print(f"mw [ERR] {message}")

# --- SERVER SETUP ---
app = FastAPI(title="Kaironex Brain | Local Simulation Harness")

class Payload(BaseModel):
    userId: str
    data: Optional[Dict[str, Any]] = {}

@app.get("/")
def health_check():
    return {"status": "NEURAL_CORE_ONLINE", "mode": "LOCAL_SIMULATION"}

def run_simulation(trigger_event: str, payload: dict):
    """Injects the event into the environment and runs the brain."""
    
    # 1. Mock the Environment Trigger
    os.environ['APPWRITE_FUNCTION_EVENT'] = trigger_event
    
    # 2. Mock the Context
    context = MockContext(payload, trigger_event)
    
    # 3. Execute Core Logic
    try:
        response = kairo_main(context)
        return response
    except Exception as e:
        print(f"🔥 CRITICAL FAILURE: {e}")
        return {"error": str(e)}

# --- ENDPOINTS MAPPED TO YOUR AGENTS ---

@app.post("/simulate/study_log")
async def trigger_study(payload: Dict[str, Any]):
    """Simulates a new document in 'study_logs' collection."""
    # The 'data' usually comes flat in Appwrite events, but we adapt here
    return run_simulation("databases.study_logs.create", payload)

@app.post("/simulate/vitality")
async def trigger_vitality(payload: Dict[str, Any]):
    """Simulates a change in 'vitality_state'."""
    return run_simulation("databases.vitality_state.update", payload)

@app.post("/simulate/radius")
async def trigger_radius(payload: Dict[str, Any]):
    """Simulates a location update in 'radius_state'."""
    return run_simulation("databases.radius_state.update", payload)

@app.post("/simulate/campaign")
async def trigger_campaign(payload: Dict[str, Any]):
    """Simulates a goal update or schedule change."""
    return run_simulation("databases.campaign_state.update", payload)

@app.post("/simulate/cron")
async def trigger_supervisor():
    """Simulates the CRON schedule (Drift Check)."""
    return run_simulation("schedule.cron", {})

# Use: uvicorn local_harness:app --reload
