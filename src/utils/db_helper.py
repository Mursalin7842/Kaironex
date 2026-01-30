import os
import json
import datetime
from appwrite.client import Client
from appwrite.services.databases import Databases

# Config
DB_ID = os.environ.get('APPWRITE_DATABASE_ID', '697cb20f00110f6d7530')
MEMORY_COL = 'agent_memory'

class KairoDB:
    def __init__(self):
        self.client = Client()
        self.client.set_endpoint(os.environ.get('APPWRITE_ENDPOINT', 'https://cloud.appwrite.io/v1'))
        self.client.set_project(os.environ['APPWRITE_FUNCTION_PROJECT_ID'])
        self.client.set_key(os.environ['APPWRITE_API_KEY'])
        self.db = Databases(self.client)

    def log_heartbeat(self, user_id, source_details):
        """Updates agent_memory with the source and time."""
        try:
            # Find the user's memory doc
            results = self.db.list_documents(DB_ID, MEMORY_COL, [
                f'userId="{user_id}"' # Appwrite query syntax
            ])
            
            data = {
                'last_active': datetime.datetime.now().isoformat(),
                'last_trigger_source': source_details[:1000] # Safe crop
            }

            if results['total'] > 0:
                doc_id = results['documents'][0]['$id']
                self.db.update_document(DB_ID, MEMORY_COL, doc_id, data)
            else:
                # Create if missing
                data['userId'] = user_id
                data['pressure_index'] = 50
                self.db.create_document(DB_ID, MEMORY_COL, 'unique()', data)
                
        except Exception as e:
            print(f"Heartbeat Error: {e}")

    def create_intervention(self, user_id, trigger, message, strategy="DIRECT"):
        """Triggers the Android Voice."""
        self.db.create_document(DB_ID, 'interventions', 'unique()', {
            'userId': user_id,
            'interventionId': 'unique()',
            'trigger_event': trigger,
            'status': 'PENDING',
            'ai_message': message
        })
