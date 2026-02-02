import datetime
from appwrite.query import Query
from ..utils.gemini_client import GeminiClient
from ..config import *

def run_supervisor(db_helper, context):
    """
    Supervisor: Checks for 'Drift' (User silence) and runs audits.
    Run via CRON.
    """
    context.log("🛡️ Supervisor: Running Safety Checks...")
    
    # 1. Fetch active users (via Agent Memory)
    # Limit 100 for safety
    try:
        memories = db_helper.db.list_rows(
            database_id=APPWRITE_DATABASE_ID, table_id=AGENT_MEMORY_COL, queries=[Query.limit(100)]
        )


    except Exception as e:
        return context.res.json({"error": str(e)})

    ai = GeminiClient()
    interventions_triggered = 0
    now = datetime.datetime.now(datetime.timezone.utc)

    for mem in memories['rows']:

        user_id = mem['userId']
        last_active_str = mem.get('last_active')
        
        # Check Drift
        if last_active_str:
            try:
                # Basic parsing, handle TZ
                last_active = datetime.datetime.fromisoformat(last_active_str)
                if last_active.tzinfo is None:
                    last_active = last_active.replace(tzinfo=datetime.timezone.utc)
                
                diff_mins = (now - last_active).total_seconds() / 60
                
                # Logic: If silent > 4 hours, check in
                if diff_mins > 240:
                    context.log(f"User {user_id} drift detected ({int(diff_mins)}m).")
                    
                    prompt = "User has been silent for 4+ hours. Write a gentle check-in."
                    msg = ai.generate_response(prompt)
                    
                    db_helper.create_intervention(
                        user_id, "DRIFT_CHECK", msg, strategy="WARM"
                    )
                    interventions_triggered += 1
            except Exception as e:
                context.log(f"Date parse error for {user_id}: {e}")

    return context.res.json({
        "status": "supervisor_complete",
        "interventions": interventions_triggered
    })
