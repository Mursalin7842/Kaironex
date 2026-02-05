from appwrite.client import Client
from appwrite.services.databases import Databases
import time
from src.config import *

def setup_indexes():
    print("🔧 Starting Appwrite Index Setup...")
    
    client = Client()
    client.set_endpoint(APPWRITE_ENDPOINT)
    client.set_project(APPWRITE_PROJECT_ID)
    client.set_key(APPWRITE_API_KEY)
    
    db = Databases(client)
    
    # 1. Schedule Table Indexes
    print(f"\n📅 Checking 'schedule' collection ({SCHEDULE_COL})...")
    try:
        # Create Index for userId
        print("  - Creating index: idx_userId (key)")
        try:
            db.create_index(
                database_id=APPWRITE_DATABASE_ID,
                collection_id=SCHEDULE_COL,
                key='idx_userId',
                type='key',
                attributes=['userId'],
                orders=['ASC']
            )
            print("    ✅ Created idx_userId")
        except Exception as e:
            if "already exists" in str(e):
                print("    ⚠️ Index idx_userId already exists")
            else:
                print(f"    ❌ Error: {e}")

        # Create Index for startTime
        print("  - Creating index: idx_startTime (key)")
        try:
            db.create_index(
                database_id=APPWRITE_DATABASE_ID,
                collection_id=SCHEDULE_COL,
                key='idx_startTime',
                type='key',
                attributes=['startTime'],
                orders=['ASC']
            )
            print("    ✅ Created idx_startTime")
        except Exception as e:
            if "already exists" in str(e):
                print("    ⚠️ Index idx_startTime already exists")
            else:
                print(f"    ❌ Error: {e}")
                
    except Exception as e:
        print(f"❌ Failed to accesses schedule collection: {e}")

    # 2. Resources Table Indexes
    print(f"\n📂 Checking 'resources' collection ({RESOURCES_COL})...")
    try:
        # Create Index for userId
        print("  - Creating index: idx_userId (key)")
        try:
            db.create_index(
                database_id=APPWRITE_DATABASE_ID,
                collection_id=RESOURCES_COL,
                key='idx_userId',
                type='key',
                attributes=['userId'],
                orders=['ASC']
            )
            print("    ✅ Created idx_userId")
        except Exception as e:
            if "already exists" in str(e):
                print("    ⚠️ Index idx_userId already exists")
            else:
                print(f"    ❌ Error: {e}")

        # Note: $createdAt is usually indexed by system, skipping explicit index 
        # unless user reports specific sorting issues with it.
                
    except Exception as e:
        print(f"❌ Failed to accesses resources collection: {e}")

    print("\n✨ Index setup complete! Appwrite may take a few minutes to build them.")

if __name__ == "__main__":
    setup_indexes()
