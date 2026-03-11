# Kana Quiz

Three-part project: a Wear OS watch app (`app/`), a phone app (`phone/`), and a web admin panel (separate repo at `../watchWeb/`).

## Critical Rules

- **Any feature added to one mobile app MUST be added to the other.** Watch and phone apps must stay in sync functionality-wise.
- **API changes in the web backend must be reflected in both mobile apps.**

## Android Project (this repo)

### Architecture

- `app/` — Wear OS module (`com.kana.watch`), uses `androidx.wear.compose.material`
- `phone/` — Phone module (`com.kana.phone`), uses `androidx.compose.material3`
- Both consume the same API backend at `https://watch.osrs.lv`

### Features

- Kana quiz (hiragana/katakana flashcards)
- Word pack download via 4-digit token
- Audio playback for words (cached locally, tap question text to play)
- Periodic notification reminders with configurable interval
- Auto-update packs on app launch (compares `updated_at` field)
- Settings: toggle hiragana/katakana, manage word packs, set reminder interval

### Key Differences Between Modules

- Watch: `ScalingLazyColumn`, `CompactChip`, `Chip`, `ToggleChip` (Wear Compose), separate Activities
- Phone: `LazyColumn`, `Button`, `Switch`, `Card` (Material3), state-based navigation in single Activity (no NavHost route params — Japanese characters break URI parsing)

### Build

- Kotlin 1.9.22, Compose compiler 1.5.8, compileSdk 34
- Watch: minSdk 30, Phone: minSdk 26

## Web Backend & Admin Panel (`../watchWeb/`)

### Stack

- **Frontend:** Vue 3 + TypeScript + Vite + Tailwind CSS
- **Backend:** Node.js + Express
- **Database:** PostgreSQL 16
- **Deployment:** Docker, Traefik reverse proxy at `watch.osrs.lv`

### API Endpoints

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/api/words/:token` | Public — get pack for mobile apps (enabled words only) |
| GET | `/api/packs` | List all packs with word counts |
| GET | `/api/packs/:token/edit` | Get full pack data for editor (includes disabled words) |
| POST | `/api/packs` | Create new pack (auto-generates 4-digit token) |
| PUT | `/api/packs/:token` | Update pack (deletes old words, inserts new) |
| DELETE | `/api/packs/:token` | Delete pack (cascades to words + audio) |
| POST | `/api/audio` | Upload audio (base64 encoded), returns filename |
| GET | `/api/audio/:filename` | Serve audio file |
| DELETE | `/api/audio/:filename` | Delete audio file |

### Pack JSON Format (from `/api/words/:token`)

```json
{"name": "...", "updated_at": "...", "words": [{"question": "...", "answer": "...", "reading": "...", "audio": "uuid.webm"}]}
```

### Database Schema

- `packs`: id, token (VARCHAR 4, unique), name, created_at, updated_at
- `words`: id, pack_id (FK), question, answer, reading, enabled (boolean), audio (filename)

### Web Pages

- `/` — Landing page with instructions
- `/create` — Create new word pack with audio recording
- `/packs` — List all packs
- `/edit/:token` — Edit existing pack

### Key Details

- No authentication — security by 4-digit token obscurity
- Audio recorded in browser via Web Audio API, converted to WAV, stored as files in `server/uploads/`
- Bulk word import via pipe-delimited format: `question|answer|reading`
