import os
import json
import datetime
from typing import Any
from appwrite.client import Client
from appwrite.services.tables_db import TablesDB
from appwrite.services.storage import Storage

from appwrite.query import Query
from ..config import *

class KairoDB:
    def __init__(self):
        self.client = Client()
        self.client.set_endpoint(APPWRITE_ENDPOINT)
        self.client.set_project(APPWRITE_PROJECT_ID)
        self.client.set_key(APPWRITE_API_KEY)
        self.db = TablesDB(self.client)
        self.storage = Storage(self.client)
        
        # Expose config for agents
        self.APPWRITE_DATABASE_ID = APPWRITE_DATABASE_ID
        self.THOUGHT_SIGNATURES_COL = THOUGHT_SIGNATURES_COL
        self.Query = Query

    # =========================================================================
    # AGENT MEMORY - The Living Memory of Each User's Agent State
    # =========================================================================
    def log_heartbeat(self, user_id, source_details, thought_signature=None, reasoning_mode=None, session_id=None):
        """
        Updates agent_memory with comprehensive state.
        
        THIS IS CRITICAL - The app reads this to know what the brain is doing!
        """
        if not user_id: 
            print("⚠️ Heartbeat skipped - no user_id")
            return
        try:
            results = self.db.list_rows(
                database_id=APPWRITE_DATABASE_ID, 
                table_id=AGENT_MEMORY_COL, 
                queries=[Query.equal('userId', user_id)] 
            )
            
            data: dict[str, Any] = {
                'last_active': datetime.datetime.now().isoformat(),
                'last_trigger_source': source_details[:999] if source_details else ''
            }
            
            # Add optional fields if provided
            if thought_signature:
                data['current_thought_signature'] = json.dumps(thought_signature)[:999999]
            if reasoning_mode:
                data['reasoning_mode'] = reasoning_mode[:99]
            if session_id:
                data['session_id'] = session_id[:255]

            if results['total'] > 0:
                doc_id = results['rows'][0]['$id']
                self.db.update_row(APPWRITE_DATABASE_ID, AGENT_MEMORY_COL, doc_id, data)
                print(f"💾 Agent Memory Updated for {user_id}")
            else:
                data['userId'] = user_id
                data['pressure_index'] = 50
                self.db.create_row(APPWRITE_DATABASE_ID, AGENT_MEMORY_COL, 'unique()', data)
                print(f"💾 Agent Memory Created for {user_id}")
                
        except Exception as e:
            print(f"❌ Heartbeat Error: {e}")
            import traceback
            traceback.print_exc()

    def update_agent_memory_full(self, user_id, thought_sig_dict=None, active_agents=None, 
                                  pressure_index=None, session_id=None, reasoning_mode=None):
        """
        Full agent_memory update with all fields.
        """
        if not user_id: return
        try:
            results = self.db.list_rows(
                database_id=APPWRITE_DATABASE_ID, 
                table_id=AGENT_MEMORY_COL, 
                queries=[Query.equal('userId', user_id)] 
            )
            
            data: dict[str, Any] = {'last_active': datetime.datetime.now().isoformat()}
            
            if thought_sig_dict:
                data['current_thought_signature'] = json.dumps(thought_sig_dict)[:999999]
            if active_agents:
                data['active_agents'] = active_agents[:255]
            if pressure_index is not None:
                try:
                    data['pressure_index'] = int(pressure_index)
                except:
                    data['pressure_index'] = 50
            if session_id:
                data['session_id'] = session_id[:255]
            if reasoning_mode:
                data['reasoning_mode'] = reasoning_mode[:99]

            if results['total'] > 0:
                doc_id = results['rows'][0]['$id']
                self.db.update_row(APPWRITE_DATABASE_ID, AGENT_MEMORY_COL, doc_id, data)
            else:
                data['userId'] = user_id
                if 'pressure_index' not in data:
                    data['pressure_index'] = 50
                self.db.create_row(APPWRITE_DATABASE_ID, AGENT_MEMORY_COL, 'unique()', data)
                
            print(f"💾 Agent Memory Full Update for {user_id}")
        except Exception as e:
            print(f"❌ Agent Memory Update Error: {e}")

    # =========================================================================
    # THOUGHT SIGNATURES - The Brain's Reasoning History
    # =========================================================================
    def create_thought_signature(self, user_id, thought_data):
        """
        Store a thought signature matching strict Appwrite Schema.
        """
        try:
            # Construct signature_json as required by schema
            signature_blob = {
                'context_hash': thought_data.get('context_hash'),
                'thought': thought_data.get('thought'),
                'tool_calls': thought_data.get('tool_calls'),
                'action_output': thought_data.get('action_output')
            }

            row_data = {
                'userId': user_id,
                'thoughtId': thought_data.get('thought_id', ''),
                'sessionId': thought_data.get('session_id', f"sess_{datetime.datetime.now().strftime('%Y%m%d')}"), # Fallback session
                'agentType': thought_data.get('agent', 'general'),
                'signature_json': json.dumps(signature_blob)[:999999],
                'confidence': float(thought_data.get('confidence', 0.0)),
                'reasoning_trace': json.dumps(thought_data.get('reasoning_trace', []))[:999999],
                'parent_thought_id': thought_data.get('parent_signature', ''),
                'created_at': thought_data.get('timestamp', datetime.datetime.now().isoformat())
            }
            
            self.db.create_row(APPWRITE_DATABASE_ID, THOUGHT_SIGNATURES_COL, 'unique()', row_data)
            print(f"🧠 Thought Signature Created: {thought_data.get('thought_id', 'unknown')}")
            return True
        except Exception as e:
            print(f"❌ Thought Signature Error: {e}")
            import traceback
            traceback.print_exc()
            return False

    def get_latest_thought(self, user_id, agent=None):
        """Get the most recent thought signature for a user."""
        try:
            queries = [Query.equal('userId', user_id), Query.order_desc('$createdAt'), Query.limit(1)]
            if agent:
                queries.insert(1, Query.equal('agentType', agent)) # Updated column name
                
            results = self.db.list_rows(
                database_id=APPWRITE_DATABASE_ID,
                table_id=THOUGHT_SIGNATURES_COL,
                queries=queries
            )
            
            if results['total'] > 0:
                return results['rows'][0]
            return None
        except Exception as e:
            print(f"❌ Get Latest Thought Error: {e}")
            return None

    # =========================================================================
    # MARATHON SESSIONS - Long-Running Goal Tracking
    # =========================================================================
    def create_marathon_session(self, session_data):
        """
        Create a new marathon session matching strict Appwrite Schema.
        """
        try:
            row_data = {
                'sessionId': session_data.get('session_id', ''),
                'userId': session_data.get('user_id', ''),
                'agentType': session_data.get('agent_type', ''),
                'goal_json': json.dumps(session_data.get('goal', {}))[:999999],
                'status': session_data.get('status', 'pending'),
                'state_json': json.dumps(session_data.get('state', {}))[:999999], # Added state_json
                'thought_chain_json': json.dumps(session_data.get('thought_chain', []))[:999999],
                'progress': float(session_data.get('progress', 0.0)),
                'started_at': datetime.datetime.now().isoformat(),
                'estimated_completion': session_data.get('estimated_completion', '')
            }
            
            self.db.create_row(APPWRITE_DATABASE_ID, MARATHON_SESSIONS_COL, 'unique()', row_data)
            print(f"🏃 Marathon Session Created: {session_data.get('session_id', 'unknown')}")
            return True
        except Exception as e:
            print(f"❌ Marathon Session Create Error: {e}")
            import traceback
            traceback.print_exc()
            return False

    def update_marathon_session(self, session_id, updates):
        """Update an existing marathon session."""
        try:
            results = self.db.list_rows(
                database_id=APPWRITE_DATABASE_ID,
                table_id=MARATHON_SESSIONS_COL,
                queries=[Query.equal('sessionId', session_id)] # Key is sessionId
            )
            
            if results['total'] > 0:
                doc_id = results['rows'][0]['$id']
                
                # Convert complex fields to JSON
                update_data = {}
                for key, value in updates.items():
                    if key in ['goal', 'state', 'thought_chain']:
                        update_data[f'{key}_json'] = json.dumps(value)
                    else:
                        update_data[key] = value
                
                self.db.update_row(APPWRITE_DATABASE_ID, MARATHON_SESSIONS_COL, doc_id, update_data)
                print(f"🏃 Marathon Session Updated: {session_id}")
                return True
            return False
        except Exception as e:
            print(f"❌ Marathon Session Update Error: {e}")
            return False

    def get_marathon_session(self, session_id):
        """Get a marathon session by ID."""
        try:
            results = self.db.list_rows(
                database_id=APPWRITE_DATABASE_ID,
                table_id=MARATHON_SESSIONS_COL,
                queries=[Query.equal('sessionId', session_id)]
            )
            
            if results['total'] > 0:
                return results['rows'][0]
            return None
        except Exception as e:
            print(f"❌ Get Marathon Session Error: {e}")
            return None

    def get_user_marathons(self, user_id, status=None):
        """Get all marathons for a user."""
        try:
            queries = [Query.equal('userId', user_id)]
            if status:
                queries.append(Query.equal('status', status))
                
            results = self.db.list_rows(
                database_id=APPWRITE_DATABASE_ID,
                table_id=MARATHON_SESSIONS_COL,
                queries=queries
            )
            return results.get('rows', [])
        except Exception as e:
            print(f"❌ Get User Marathons Error: {e}")
            return []

    # =========================================================================
    # POLICY EPISODES - Learning from Agent Decisions
    # =========================================================================
    def create_policy_episode(self, episode_data):
        """
        Store a policy episode matching strict Appwrite Schema.
        """
        try:
            row_data = {
                'episodeId': episode_data.get('episode_id', f"ep_{datetime.datetime.now().strftime('%Y%m%d%H%M%S')}"), # Generate ID if missing
                'userId': episode_data.get('user_id', ''),
                 # Note: Schema doesn't show 'agent' column in policy_episodes provided by user, 
                 # but usually it should be there. Assuming strict schema from user input:
                 # Columns: episodeId, userId, context_hash, action_taken, reward, timestamp
                 # I will skip 'agent' if not in schema, but logical to include it.
                 # User schema list: episodeId, userId, context_hash, action_taken, reward, timestamp
                'context_hash': episode_data.get('context_hash', '')[:9999],
                'action_taken': episode_data.get('action_taken', '')[:99999],
                'reward': int(episode_data.get('reward', 0)), # Integer
                'timestamp': datetime.datetime.now().isoformat()
            }
            
            self.db.create_row(APPWRITE_DATABASE_ID, POLICY_EPISODES_COL, 'unique()', row_data)
            print(f"📊 Policy Episode Created for {episode_data.get('user_id', 'unknown')}")
            return True
        except Exception as e:
            print(f"❌ Policy Episode Create Error: {e}")
            import traceback
            traceback.print_exc()
            return False

    def get_policy_episodes(self, user_id, agent=None, limit=100):
        """Get policy episodes for learning."""
        try:
            queries = [Query.equal('userId', user_id), Query.limit(limit)]
            if agent:
                queries.insert(1, Query.equal('agent', agent))
                
            results = self.db.list_rows(
                database_id=APPWRITE_DATABASE_ID,
                table_id=POLICY_EPISODES_COL,
                queries=queries
            )
            return results.get('rows', [])
        except Exception as e:
            print(f"❌ Get Policy Episodes Error: {e}")
            return []

    # =========================================================================
    # INTERVENTIONS - AI Messages to User
    # =========================================================================
    def create_intervention(self, user_id, trigger, message, status="PENDING", strategy="NEUTRAL"):
        """
        Writes to 'interventions' table.
        THIS IS HOW THE APP SEES AI RESPONSES!
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
            return True
        except Exception as e:
            print(f"❌ Intervention Error: {e}")
            import traceback
            traceback.print_exc()
            return False

    # =========================================================================
    # USER DATA - Core User Table Operations
    # =========================================================================
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
            print(f"❌ Get User Error: {e}")
            return None

    def update_state_cache(self, user_id, specific_state_data):
        """
        THE GOD MODE SYNC:
        Updates 'studentState_json' in 'users' table so Android loads everything in 1 call.
        """
        try:
            user_doc = self.get_user_doc(user_id)
            if not user_doc: 
                print(f"⚠️ User {user_id} not found for sync.")
                return False

            current_cache = {}
            if user_doc.get('studentState_json'):
                try:
                    current_cache = json.loads(user_doc['studentState_json'])
                except:
                    current_cache = {}

            for key, value in specific_state_data.items():
                current_cache[key] = value

            self.db.update_row(
                APPWRITE_DATABASE_ID, 
                USERS_COL, 
                user_doc['$id'], 
                {'studentState_json': json.dumps(current_cache)}
            )

            print(f"🔄 State Cache Synced for {user_id}")
            return True
        except Exception as e:
            print(f"❌ State Sync Error: {e}")
            import traceback
            traceback.print_exc()
            return False

    # =========================================================================
    # RESOURCES - Academic Files
    # =========================================================================
    def count_user_resources(self, user_id):
        """Checks if user has uploaded any academic files."""
        try:
            results = self.db.list_rows(
                database_id=APPWRITE_DATABASE_ID, 
                table_id=RESOURCES_COL, 
                queries=[Query.equal('userId', user_id), Query.limit(1)] 
            )
            return results['total']
        except Exception as e:
            print(f"❌ Count Resources Error: {e}")
            return 0

    def get_user_resources(self, user_id, limit=20):
        """Get list of user's resources for context."""
        try:
            results = self.db.list_rows(
                database_id=APPWRITE_DATABASE_ID,
                table_id=RESOURCES_COL,
                queries=[
                    Query.equal('userId', user_id),
                    Query.order_desc('$createdAt'),
                    Query.limit(limit)
                ]
            )
            return results.get('rows', [])
        except Exception as e:
            print(f"❌ Get User Resources Error: {e}")
            return []

    def update_resource_summary(self, resource_id, summary_text):
        """Update the summaryText field of a resource."""
        try:
            self.db.update_row(
                APPWRITE_DATABASE_ID,
                RESOURCES_COL,
                resource_id,
                {'summaryText': summary_text[:999999]}
            )
            print(f"✅ Resource Summary Updated: {resource_id}")
            return True
        except Exception as e:
            print(f"❌ Resource Summary Update Error: {e}")
            return False

    def update_resource_metadata(self, resource_id, metadata):
        """
        L7: Update resource with mined semantic data.
        Maps 'mined_data' -> 'summaryText' to fit existing schema.
        Maps 'resource_type' -> 'type' (if column exists, otherwise ignored/handled by DB).
        """
        try:
            # Schema Mapping
            update_data = {
                'summaryText': metadata.get('mined_data', '')[:999999]
            }
            if 'resource_type' in metadata:
                update_data['type'] = metadata['resource_type']
            
            self.db.update_row(
                APPWRITE_DATABASE_ID,
                RESOURCES_COL,
                resource_id,
                update_data
            )
            print(f"✅ Resource Metadata Updated: {resource_id} ({metadata.get('resource_type')})")
            return True
        except Exception as e:
            print(f"❌ Resource Metadata Update Error: {e}")
            return False

    
    def find_real_file_id(self, bucket_id, file_name):
        """
        Fallback: Find the ACTUAL file ID by searching for the filename.
        Useful if the DB has a mismatched/custom ID.
        """
        try:
            # List files searching for the name
            # NOTE: Removed 'limit' arg as it might be invalid for this SDK version.
            # Using search only.
            result = self.storage.list_files(
                bucket_id=bucket_id,
                search=file_name
            )
            
            print(f"🔎 Search for '{file_name}' returned {result.get('total')} results.")
            
            if result['total'] > 0:
                real_id = result['files'][0]['$id']
                print(f"✅ FOUND Real File ID: {real_id}")
                return real_id
            return None
            return None
        except Exception as e:
            print(f"⚠️ File Search Error: {e}")
            return None

    def get_file_content(self, file_id, bucket_id=None):
        """Downloads file content as bytes."""
        try:
            target_bucket = bucket_id or STORAGE_BUCKET_ID
            return self.storage.get_file_download(target_bucket, file_id)
        except Exception as e:
            print(f"❌ File Download Error: {e}")
            return None

    # =========================================================================
    # CAMPAIGN STATE - Career & Game Progression
    # =========================================================================
    def update_campaign_state(self, user_id, campaign_data):
        """
        Updates the campaign_state table (Skill Tree, Quest Board, Armory).
        """
        try:
            # 1. Check if row exists
            results = self.db.list_rows(
                database_id=self.APPWRITE_DATABASE_ID,
                table_id=CAMPAIGN_STATE_COL,
                queries=[self.Query.equal('userId', user_id)]
            )
            
            # 2. Prepare Data (ensure JSON serialization)
            data = {'userId': user_id}
            if 'skill_tree' in campaign_data:
                data['skill_tree_json'] = json.dumps(campaign_data['skill_tree'])[:999999]
            if 'quest_board' in campaign_data:
                data['quest_board_json'] = json.dumps(campaign_data['quest_board'])[:999999]
            if 'armory' in campaign_data:
                data['the_armory_json'] = json.dumps(campaign_data['armory'])[:999999]
            if 'simulacrum' in campaign_data:
                data['simulacrum_data_json'] = json.dumps(campaign_data['simulacrum'])[:999999]
                
            # 3. Update or Create
            if results['total'] > 0:
                doc_id = results['rows'][0]['$id']
                self.db.update_row(self.APPWRITE_DATABASE_ID, CAMPAIGN_STATE_COL, doc_id, data)
                print(f"🏰 Campaign State Updated for {user_id}")
            else:
                self.db.create_row(self.APPWRITE_DATABASE_ID, CAMPAIGN_STATE_COL, 'unique()', data)
                print(f"🏰 Campaign State Created for {user_id}")
                
            return True
        except Exception as e:
            print(f"❌ Campaign State Error: {e}")
            return False

    def get_campaign_state(self, user_id):
        """Fetch full campaign state."""
        try:
            results = self.db.list_rows(
                database_id=self.APPWRITE_DATABASE_ID,
                table_id=CAMPAIGN_STATE_COL,
                queries=[self.Query.equal('userId', user_id)]
            )
            if results['total'] > 0:
                row = results['rows'][0]
                # Parse JSONs back to dicts
                return {
                    'skill_tree': json.loads(row.get('skill_tree_json') or '{}'),
                    'quest_board': json.loads(row.get('quest_board_json') or '[]'),
                    'armory': json.loads(row.get('the_armory_json') or '{}'),
                    'simulacrum': json.loads(row.get('simulacrum_data_json') or '{}')
                }
            return {}
        except Exception as e:
            print(f"❌ Get Campaign State Error: {e}")
            return {}

    # =========================================================================
    # STUDENT PROFILE - detailed bio
    # =========================================================================
    def get_student_profile(self, user_id):
        """Fetch advanced student profile."""
        try:
            results = self.db.list_rows(
                database_id=self.APPWRITE_DATABASE_ID,
                table_id=STUDENT_PROFILES_COL,
                queries=[self.Query.equal('userId', user_id)]
            )
            if results['total'] > 0:
                return results['rows'][0]
            return None
        except Exception as e:
            print(f"❌ Get Student Profile Error: {e}")
            return None

    # =========================================================================
    # SCHEDULE - Study Plan Management
    # =========================================================================
    def create_schedule_task(self, task_data):
        """
        Create a single schedule task.
        """
        try:
            # Ensure required fields
            if 'taskId' not in task_data:
                task_data['taskId'] = f"task_{datetime.datetime.now().strftime('%Y%m%d%H%M%S')}_{os.urandom(4).hex()}"
            
            # Default values if missing
            if 'status' not in task_data: task_data['status'] = 'pending'
            if 'type' not in task_data: task_data['type'] = 'study'
            if 'is_flexible' not in task_data: task_data['is_flexible'] = True
            if 'priority' not in task_data: task_data['priority'] = 5
            
            self.db.create_row(
                APPWRITE_DATABASE_ID, 
                SCHEDULE_COL, 
                'unique()', 
                task_data
            )
            # print(f"📅 Task Created: {task_data['title']}") # Reduce verbosity
            return True
        except Exception as e:
            print(f"❌ Create Task Error: {e}")
            return False

    def batch_create_schedule_tasks(self, tasks: list) -> int:
        """
        Creates multiple tasks in parallel to avoid timeouts.
        Returns number of successful creations.
        """
        from concurrent.futures import ThreadPoolExecutor, as_completed
        
        success_count = 0
        with ThreadPoolExecutor(max_workers=10) as executor:
            # Submit all tasks
            future_to_task = {executor.submit(self.create_schedule_task, t): t for t in tasks}
            
            for future in as_completed(future_to_task):
                try:
                    if future.result():
                        success_count += 1
                except Exception as e:
                    print(f"⚠️ Batch Insert Error: {e}")
        
        print(f"✅ Batch Complete: {success_count}/{len(tasks)} created.")
        return success_count

    def get_schedule(self, user_id, limit=1000):
        """Get user's schedule."""
        try:
            results = self.db.list_rows(
                database_id=APPWRITE_DATABASE_ID,
                table_id=SCHEDULE_COL,
                queries=[
                    self.Query.equal('userId', user_id),
                    self.Query.order_asc('startTime'),
                    self.Query.limit(limit)
                ]
            )
            return results.get('rows', [])
        except Exception as e:
            print(f"❌ Get Schedule Error: {e}")
            return []

    def clear_future_schedule(self, user_id):
        """Clear upcoming schedule tasks (e.g. before regenerating)."""
        try:
            # Loop to clear all pages (Appwrite limits to 100)
            total_deleted = 0
            while True:
                now_iso = datetime.datetime.now().isoformat()
                results = self.db.list_rows(
                    database_id=APPWRITE_DATABASE_ID,
                    table_id=SCHEDULE_COL,
                    queries=[
                        self.Query.equal('userId', user_id),
                        self.Query.greater_than('startTime', now_iso),
                        self.Query.limit(100)
                    ]
                )
                
                rows = results.get('rows', [])
                if not rows:
                    break
                    
                for row in rows:
                    try:
                        self.db.delete_row(APPWRITE_DATABASE_ID, SCHEDULE_COL, row['$id'])
                        total_deleted += 1
                    except:
                        pass
            
            print(f"🧹 Cleared {total_deleted} future tasks for {user_id}")
            return True
        except Exception as e:
            print(f"❌ Clear Schedule Error: {e}")
            return False
