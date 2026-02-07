
import os
import sys
import logging
from datetime import datetime
from dotenv import load_dotenv

# Setup paths
SRC_PATH = r"D:\Kaironex-Brain"
sys.path.append(SRC_PATH)
load_dotenv(os.path.join(SRC_PATH, '.env'))

from src.utils.db_helper import KairoDB

# Setup logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

def ingest_documents():
    print("🚀 Starting Manual Ingestion...")
    
    try:
        db = KairoDB()
        print("✅ Database Connected")
    except Exception as e:
        print(f"❌ DB Connection Failed: {e}")
        return

    # Folder to ingest
    FOLDER_PATH = r"C:\Users\Admin\Videos\Academic folder"
    if not os.path.exists(FOLDER_PATH):
        print(f"❌ Folder not found: {FOLDER_PATH}")
        return

    # Find a user
    user_id = "67980554002e3b2e95a9" # Fallback
    try:
        users = db.db.list_rows(db.APPWRITE_DATABASE_ID, 'users', queries=[db.Query.limit(1)])
        if users['total'] > 0:
            user_id = users['rows'][0]['userId']
            print(f"👤 Using User ID: {user_id}")
    except Exception as e:
        print(f"⚠️ Failed to fetch user, using fallback: {e}")

    # Process files
    files = [f for f in os.listdir(FOLDER_PATH) if f.endswith('.pdf')]
    print(f"📂 Found {len(files)} PDF files.")

    for filename in files:
        file_path = os.path.join(FOLDER_PATH, filename)
        print(f"\nProcessing: {filename}")
        
        # In a real app, we'd extract text here. 
        # For now, we'll just create a dummy summary to simulate ingestion
        # so the Study Agent can read it.
        
        summary = f"Simulated content for {filename}. This resource covers the topics outlined in the syllabus/routine."
        
        # Check if resource already exists
        # (Simplified check by title)
        existing = db.db.list_rows(
            db.APPWRITE_DATABASE_ID, 
            'resources', 
            queries=[
                db.Query.equal('userId', user_id),
                db.Query.equal('title', filename)
            ]
        )
        
        if existing['total'] > 0:
            print(f"⚠️ Resource already exists: {existing['rows'][0]['$id']}")
            continue

        resource_data = {
            'userId': user_id,
            'title': filename,
            'summaryText': summary, 
            'resourceId': f"res_{os.urandom(4).hex()}",
            'driveLink': "https://kaironex.com/placeholder.pdf", # Must be valid URL format for Appwrite
        }
        
        try:
            # import datetime  <-- Removed
            # from datetime import datetime <-- Removed
            
            res = db.db.create_row(
                db.APPWRITE_DATABASE_ID,
                'resources',
                'unique()',
                resource_data
            )
            print(f"✅ Ingested: {res['$id']}")
        except Exception as e:
            print(f"❌ Failed to ingest {filename}: {e}")

if __name__ == "__main__":
    ingest_documents()
