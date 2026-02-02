import os
import json
import datetime
from appwrite.client import Client
from appwrite.services.tables_db import TablesDB

from appwrite.query import Query
from ..config import *

class KairoDB:
    def __init__(self):
        self.client = Client()
        self.client.set_endpoint(APPWRITE_ENDPOINT)
        self.client.set_project(APPWRITE_PROJECT_ID)
        self.client.set_key(APPWRITE_API_KEY)
        self.db = TablesDB(self.client)



    def log_heartbeat(self, user_id, source_details):
        """Updates agent_memory with the source and time."""
        if not user_id: return
        try:
            results = self.db.list_rows(
                database_id=APPWRITE_DATABASE_ID, 
                table_id=AGENT_MEMORY_COL, 
                queries=[Query.equal('userId', user_id)] 
            )

            
            data = {
                'last_active': datetime.datetime.now().isoformat(),
                'last_trigger_source': source_details[:999]
            }

            if results['total'] > 0:
                doc_id = results['rows'][0]['$id']

                self.db.update_row(APPWRITE_DATABASE_ID, AGENT_MEMORY_COL, doc_id, data)
            else:
                data['userId'] = user_id
                data['pressure_index'] = 50
                self.db.create_row(APPWRITE_DATABASE_ID, AGENT_MEMORY_COL, 'unique()', data)

                
        except Exception as e:
            print(f"Heartbeat Error: {e}")

    def create_intervention(self, user_id, trigger, message, status="PENDING", strategy="NEUTRAL"):
        """
        Writes to 'interventions' table.
        Strictly follows your schema: interventionId, userId, trigger_event, ai_message, status, ai_response_strategy.
        """
        try:
            self.db.create_row(APPWRITE_DATABASE_ID, INTERVENTIONS_COL, 'unique()', {
                'interventionId': 'unique()', 
                'userId': user_id,
                'trigger_event': trigger[:999],
                'ai_message': message,
                'status': status,
                'ai_response_strategy': strategy[:499]
            })

            print(f"📢 Intervention Created: {trigger}")
        except Exception as e:
            print(f"Intervention Error: {e}")

    def get_user_doc(self, user_id):
        """Fetches the user row."""
        try:
            results = self.db.list_rows(
                database_id=APPWRITE_DATABASE_ID, 
                table_id=USERS_COL, 
                queries=[Query.equal('userId', user_id)] 
            )

            if results['total'] > 0:
                return results['rows'][0]

            return None
        except Exception as e:
            print(f"Get User Error: {e}")
            return None

    def update_state_cache(self, user_id, specific_state_data):
        """
        THE GOD MODE SYNC:
        Updates 'studentState_json' in 'users' table so Android loads everything in 1 call.
        """
        try:
            # 1. Get current user doc
            user_doc = self.get_user_doc(user_id)
            if not user_doc: 
                print(f"User {user_id} not found for sync.")
                return

            # 2. Parse existing cache
            current_cache = {}
            if user_doc.get('studentState_json'):
                try:
                    current_cache = json.loads(user_doc['studentState_json'])
                except:
                    current_cache = {}

            # 3. Merge new data
            # specific_state_data example: {'vitality': {'steps': 5000}}
            for key, value in specific_state_data.items():
                current_cache[key] = value

            # 4. Save back to Users table
            self.db.update_row(
                APPWRITE_DATABASE_ID, 
                USERS_COL, 
                user_doc['$id'], 
                {'studentState_json': json.dumps(current_cache)}
            )

            print(f"🔄 State Cache Synced for {user_id}")

        except Exception as e:
            print(f"State Sync Error: {e}")
