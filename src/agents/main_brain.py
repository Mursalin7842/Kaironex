import datetime
from dateutil.parser import parse

DB_ID = 'kaironex_db'
MEMORY_COL = 'agent_memory'

def run_supervisor(db_helper, context):
    context.log("🛡️ Supervisor: Running Safety Checks...")
    
    # 1. Fetch all agent memories
    # (Limit 100 for hackathon)
    memories = db_helper.db.list_documents(DB_ID, MEMORY_COL)
    
    now = datetime.datetime.now(datetime.timezone.utc)
    activated_count = 0

    for mem in memories['documents']:
        user_id = mem['userId']
        last_active_str = mem.get('last_active')
        
        # Check gap
        should_run = False
        if not last_active_str:
            should_run = True
        else:
            # If Appwrite stores naive time, assume UTC
            last_active = parse(last_active_str).replace(tzinfo=datetime.timezone.utc)
            diff_mins = (now - last_active).total_seconds() / 60
            
            if diff_mins > 60:
                should_run = True
                context.log(f"⚠️ User {user_id} is stale ({int(diff_mins)}m).")

        if should_run:
            # --- SUPERVISOR ACTION ---
            # 1. Log that we are running
            db_helper.log_heartbeat(user_id, "CRON:SUPERVISOR | Check for Drift")
            
            # 2. Check for Issues (Simple Logic for now)
            # You would normally fetch Vitality/Campaign here.
            # For now, let's just trigger a "Check-in" if they are totally silent.
            
            db_helper.create_intervention(
                user_id, 
                "LONG_SILENCE", 
                "Hey, I haven't seen any activity in a while. Everything okay with the schedule?"
            )
            activated_count += 1

    return context.res.json({
        "status": "supervisor_cycle_complete",
        "woke_up": activated_count
    })
