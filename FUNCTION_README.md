# Appwrite Function - Kaironex Brain

This is the Deep Brain backend for Kaironex, deployed as an Appwrite Function.

## Triggers

- **Database Events**: study_logs, vitality_state, radius_state, schedule changes
- **Cron**: Every 15 minutes for supervisor check

## Environment Variables Required

Set these in Appwrite Console → Functions → Settings → Variables:

| Variable | Description |
|----------|-------------|
| `GEMINI_API_KEY` | Your Google AI Studio API key |
| `APPWRITE_ENDPOINT` | `https://nyc.cloud.appwrite.io/v1` |
| `APPWRITE_PROJECT_ID` | `696e9248002198ef6273` |
| `APPWRITE_API_KEY` | Server API key from Appwrite Console |
| `APPWRITE_DATABASE_ID` | `697cb20f00110f6d7530` |
