# KAIRONEX - Backend API Guide

## Quick Reference for API Development

---

## 🎯 Priority 1: Core APIs

### Authentication
```
POST /api/v1/auth/login
POST /api/v1/auth/signup
POST /api/v1/auth/google
POST /api/v1/auth/judge
```

### User Profile
```
GET  /api/v1/user/profile
PUT  /api/v1/user/profile
POST /api/v1/user/onboarding
```

---

## 🎯 Priority 2: Study APIs

### Sessions
```
POST /api/v1/study/sessions
PUT  /api/v1/study/sessions/{id}
POST /api/v1/study/sessions/{id}/end
GET  /api/v1/study/sessions
```

### Stats
```
GET /api/v1/stats/home
GET /api/v1/stats/cognitive
GET /api/v1/stats/pressure
GET /api/v1/stats/learning
```

---

## 🎯 Priority 3: AI/Gemini

```
POST /api/v1/ai/chat
POST /api/v1/ai/voice/start
POST /api/v1/ai/analyze
POST /api/v1/ai/flashcards
POST /api/v1/ai/mocktest
POST /api/v1/ai/gatekeeper
```

---

## 🎯 Priority 4: Files

```
POST   /api/v1/files/upload
GET    /api/v1/files
DELETE /api/v1/files/{id}
POST   /api/v1/files/drive/connect
POST   /api/v1/files/drive/sync
POST   /api/v1/files/{id}/index
```

---

## 🎯 Priority 5: Agents

```
GET  /api/v1/agents/campaign/status
GET  /api/v1/agents/vitality/status
GET  /api/v1/agents/radius/status
POST /api/v1/agents/{type}/action
```

---

## See SYSTEM_DOCUMENTATION.md for full details.
