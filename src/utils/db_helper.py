import os
import json
import datetime
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
            
            data = {
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
                data['pressure_index'] = '50'
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
            
            data = {'last_active': datetime.datetime.now().isoformat()}
            
            if thought_sig_dict:
                data['current_thought_signature'] = json.dumps(thought_sig_dict)[:999999]
            if active_agents:
                data['active_agents'] = active_agents[:255]
            if pressure_index is not None:
                data['pressure_index'] = pressure_index
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
                    data['pressure_index'] = '50'
                self.db.create_row(APPWRITE_DATABASE_ID, AGENT_MEMORY_COL, 'unique()', data)
                
            print(f"💾 Agent Memory Full Update for {user_id}")
        except Exception as e:
            print(f"❌ Agent Memory Update Error: {e}")

    # =========================================================================
    # THOUGHT SIGNATURES - The Brain's Reasoning History
    # =========================================================================
    def create_thought_signature(self, user_id, thought_data):
        """
        Store a thought signature in the thought_signatures table.
        
        Args:
            user_id: The user ID
            thought_data: Dict with thought_id, agent, context_hash, reasoning_trace, 
                          confidence, tool_calls, action_output, parent_signature
        """
        try:
            row_data = {
                'userId': user_id,
                'thoughtId': thought_data.get('thought_id', ''),
                'agent': thought_data.get('agent', ''),
                'timestamp': thought_data.get('timestamp', datetime.datetime.now().isoformat()),
                'context_hash': thought_data.get('context_hash', '')[:999],
                'reasoning_trace': json.dumps(thought_data.get('reasoning_trace', []))[:9999],
                'confidence': float(thought_data.get('confidence', 0.0)),
                'tool_calls': json.dumps(thought_data.get('tool_calls', []))[:999],
                'action_output': thought_data.get('action_output', '')[:9999],
                'parent_signature': thought_data.get('parent_signature', '')
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
                queries.insert(1, Query.equal('agent', agent))
                
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
        Create a new marathon session.
        
        Args:
            session_data: Dict with session_id, user_id, agent_type, goal, status,
                          steps, progress, thought_chain, estimated_completion
        """
        try:
            row_data = {
                'session_id': session_data.get('session_id', ''),
                'userId': session_data.get('user_id', ''),
                'agent_type': session_data.get('agent_type', ''),
                'goal_json': json.dumps(session_data.get('goal', {}))[:9999],
                'status': session_data.get('status', 'pending'),
                'steps_json': json.dumps(session_data.get('steps', []))[:99999],
                'current_step_index': session_data.get('current_step_index', 0),
                'progress': float(session_data.get('progress', 0.0)),
                'thought_chain': json.dumps(session_data.get('thought_chain', []))[:9999],
                'estimated_completion': session_data.get('estimated_completion', ''),
                'metadata_json': json.dumps(session_data.get('metadata', {}))[:9999]
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
                queries=[Query.equal('session_id', session_id)]
            )
            
            if results['total'] > 0:
                doc_id = results['rows'][0]['$id']
                
                # Convert complex fields to JSON
                update_data = {}
                for key, value in updates.items():
                    if key in ['goal', 'steps', 'thought_chain', 'metadata']:
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
                queries=[Query.equal('session_id', session_id)]
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
        Store a policy episode for reinforcement learning.
        
        Args:
            episode_data: Dict with user_id, agent, trigger, state_before, action_taken,
                          outcome, reward, state_after, thought_chain_summary
        """
        try:
            row_data = {
                'userId': episode_data.get('user_id', ''),
                'agent': episode_data.get('agent', ''),
                'trigger_event': episode_data.get('trigger', '')[:999],
                'state_before_json': json.dumps(episode_data.get('state_before', {}))[:9999],
                'action_taken': episode_data.get('action_taken', '')[:999],
                'outcome': episode_data.get('outcome', '')[:999],
                'reward': float(episode_data.get('reward', 0.0)),
                'state_after_json': json.dumps(episode_data.get('state_after', {}))[:9999],
                'thought_chain_summary': episode_data.get('thought_chain_summary', '')[:9999]
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

    def get_file_content(self, file_id, bucket_id=None):
        """Downloads file content as bytes."""
        try:
            target_bucket = bucket_id or STORAGE_BUCKET_ID
            return self.storage.get_file_download(target_bucket, file_id)
        except Exception as e:
            print(f"❌ File Download Error: {e}")
            return None
