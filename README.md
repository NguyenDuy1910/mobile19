# MinLish Implementation Plan

MinLish is a mobile-first English vocabulary learning app focused on flashcards, spaced repetition, context-based learning, learning progress, reminders, and simple AI-assisted vocabulary support.

This document is the planning source for the first implementation. It intentionally avoids over-engineering and keeps the architecture simple, modular, and maintainable.

## Product Goal

Help users learn and retain English vocabulary through a focused learning flow:

1. Register or log in
2. Create a learning profile
3. Create a vocabulary deck
4. Add vocabulary items
5. Learn words with flashcards
6. Review words using SM-2 spaced repetition
7. Track progress
8. Receive learning reminders
9. Use simple AI help when needed

## Target Users

- Students
- IELTS and TOEIC learners
- Working professionals improving English vocabulary for communication or business

## Technology Stack

### Mobile

- Kotlin
- Jetpack Compose
- Compose Navigation
- Retrofit or Ktor Client for HTTP
- Kotlinx Serialization or Moshi for JSON
- DataStore for auth token and simple settings
- Room can be added later for offline caching, but should not be required for the first backend-connected MVP

### Backend

- Python
- FastAPI
- SQLAlchemy
- Alembic
- PostgreSQL
- Pydantic
- JWT authentication
- bcrypt password hashing

### Notification

- Store notification preferences in backend
- Store mobile device tokens
- Use scheduled backend jobs later for daily reminders
- Push notification is preferred for mobile
- Email can be planned later

### AI Agent

- Simple modular backend agent package
- No generic chatbot for MVP
- Agent endpoints must use learning context such as user level, goal, current deck, weak words, and review history
- First implementation can use mocked or provider-backed responses behind a small service interface

## Final Recommended Architecture

MinLish should use a simple client-server architecture:

- Mobile app handles UI, auth state, flashcard interactions, and API calls
- FastAPI backend owns business logic, authentication, data access, SRS calculations, progress aggregation, notification settings, and agent endpoints
- PostgreSQL stores all user, vocabulary, learning, progress, notification, and agent data
- AI agent is a backend module, not a separate service in the first version

### High-Level Flow

```text
Kotlin Jetpack Compose App
        |
        | HTTPS JSON API
        v
FastAPI Backend
        |
        | SQLAlchemy ORM
        v
PostgreSQL
```

### Backend Responsibilities

- Validate input
- Authenticate users with JWT
- Hash passwords with bcrypt
- Enforce per-user ownership of decks, words, learning progress, and notifications
- Calculate SM-2 review scheduling
- Aggregate progress and analytics
- Store notification settings and device tokens
- Provide simple AI learning endpoints

### Mobile Responsibilities

- Show clean, one-hand-friendly learning UI
- Manage auth token locally
- Navigate between auth, onboarding, home, deck, vocabulary, learning, progress, practice, agent, and settings screens
- Render flashcards with flip animation
- Submit review ratings
- Show progress and reminders

## MVP Scope

### P0 Must Have

- Email and password registration
- Email and password login
- JWT access token flow
- User profile creation and update
- Deck CRUD
- Vocabulary item CRUD
- Flashcard learning screen
- SM-2 review logic
- Daily learning plan API
- Due words API
- Review submission API
- Progress summary API
- Basic notification settings
- Simple AI explain-word endpoint

### P1 Should Have

- CSV import/export
- Practice quiz generation
- Practice answer submission
- Weak word review
- AI generate examples
- AI generate deck
- Push notification delivery

### P2 Later

- Google login
- Excel import
- Advanced analytics
- Community decks
- Payment
- Speaking practice
- Teacher dashboard
- Offline-first mobile learning
- Advanced AI tutoring

## Non-Goals for First Version

- Microservices
- Complex event-driven architecture
- Real-time chat
- Full offline sync
- Payment system
- Community marketplace
- Teacher/admin dashboard
- Advanced AI agent memory or autonomous workflows

## Folder Structure

### Backend

```text
backend/
├── main.py
├── config/
│   ├── settings.py
│   └── security.py
├── database/
│   ├── session.py
│   ├── base.py
│   └── migrations/
├── auth/
│   ├── models.py
│   ├── schemas.py
│   ├── service.py
│   ├── router.py
│   └── dependencies.py
├── users/
│   ├── models.py
│   ├── schemas.py
│   ├── service.py
│   └── router.py
├── decks/
│   ├── models.py
│   ├── schemas.py
│   ├── service.py
│   └── router.py
├── vocabulary/
│   ├── models.py
│   ├── schemas.py
│   ├── service.py
│   ├── import_export.py
│   └── router.py
├── learning/
│   ├── models.py
│   ├── schemas.py
│   ├── service.py
│   └── router.py
├── srs/
│   ├── sm2.py
│   └── schemas.py
├── practice/
│   ├── models.py
│   ├── schemas.py
│   ├── service.py
│   └── router.py
├── progress/
│   ├── schemas.py
│   ├── service.py
│   └── router.py
├── notifications/
│   ├── models.py
│   ├── schemas.py
│   ├── service.py
│   └── router.py
├── agent/
│   ├── models.py
│   ├── schemas.py
│   ├── service.py
│   ├── prompts.py
│   └── router.py
└── shared/
    ├── errors.py
    ├── pagination.py
    ├── datetime.py
    └── permissions.py
```

### Mobile

```text
mobile/
├── app/
│   ├── MainActivity.kt
│   ├── MinLishApp.kt
│   └── navigation/
├── core/
│   ├── network/
│   ├── database/
│   ├── datastore/
│   ├── ui/
│   └── utils/
├── auth/
│   ├── data/
│   ├── domain/
│   └── ui/
├── onboarding/
│   ├── data/
│   └── ui/
├── home/
│   └── ui/
├── deck/
│   ├── data/
│   ├── domain/
│   └── ui/
├── vocabulary/
│   ├── data/
│   ├── domain/
│   └── ui/
├── learning/
│   ├── data/
│   ├── domain/
│   └── ui/
├── practice/
│   ├── data/
│   ├── domain/
│   └── ui/
├── agent/
│   ├── data/
│   ├── domain/
│   └── ui/
├── progress/
│   ├── data/
│   ├── domain/
│   └── ui/
└── settings/
    ├── data/
    └── ui/
```

## Database Schema

Use UUID primary keys where possible. All timestamps should be timezone-aware. Add indexes for foreign keys and frequently queried review fields.

### users

Stores authentication identity.

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid | Primary key |
| email | varchar | Unique, indexed |
| password_hash | varchar | bcrypt hash |
| provider | varchar | `email`, later `google` |
| created_at | timestamptz | Required |
| updated_at | timestamptz | Required |

### user_profiles

Stores learning preferences and profile data.

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid | Primary key |
| user_id | uuid | FK users.id, unique |
| name | varchar | Required |
| learning_goal | varchar | IELTS, TOEIC, communication, business, etc. |
| english_level | varchar | A1, A2, B1, B2, C1, C2 |
| daily_new_words | integer | Default 10 |
| daily_review_limit | integer | Default 50 |
| notification_time | time | Nullable |
| timezone | varchar | Default UTC |

### vocab_decks

Stores user-created vocabulary collections.

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid | Primary key |
| user_id | uuid | FK users.id, indexed |
| name | varchar | Required |
| description | text | Optional |
| tags | jsonb | Array of strings |
| created_at | timestamptz | Required |
| updated_at | timestamptz | Required |

### vocab_items

Stores words and context fields.

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid | Primary key |
| deck_id | uuid | FK vocab_decks.id, indexed |
| word | varchar | Required |
| pronunciation | varchar | Optional |
| meaning | text | Required |
| description_en | text | Optional |
| example | text | Optional |
| collocations | jsonb | Array of strings |
| related_words | jsonb | Array of strings |
| note | text | Optional |
| created_at | timestamptz | Required |
| updated_at | timestamptz | Required |

### user_vocab_progress

Stores per-user review state for each vocabulary item.

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid | Primary key |
| user_id | uuid | FK users.id, indexed |
| vocab_item_id | uuid | FK vocab_items.id, indexed |
| status | varchar | `new`, `learning`, `review`, `mastered` |
| repetitions | integer | Default 0 |
| interval_days | integer | Default 0 |
| ease_factor | numeric | Default 2.5 |
| due_at | timestamptz | Indexed |
| last_reviewed_at | timestamptz | Nullable |
| total_reviews | integer | Default 0 |
| correct_count | integer | Default 0 |
| wrong_count | integer | Default 0 |

Add a unique constraint on `(user_id, vocab_item_id)`.

### review_logs

Stores review history.

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid | Primary key |
| user_id | uuid | FK users.id, indexed |
| vocab_item_id | uuid | FK vocab_items.id, indexed |
| rating | varchar | `again`, `hard`, `good`, `easy` |
| old_interval | integer | Required |
| new_interval | integer | Required |
| old_ease_factor | numeric | Required |
| new_ease_factor | numeric | Required |
| reviewed_at | timestamptz | Indexed |

### learning_sessions

Stores learning session summary.

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid | Primary key |
| user_id | uuid | FK users.id, indexed |
| started_at | timestamptz | Required |
| ended_at | timestamptz | Nullable |
| new_words_count | integer | Default 0 |
| review_words_count | integer | Default 0 |
| correct_count | integer | Default 0 |
| wrong_count | integer | Default 0 |
| duration_seconds | integer | Nullable |

### practice_answers

Stores practice quiz answers.

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid | Primary key |
| user_id | uuid | FK users.id, indexed |
| vocab_item_id | uuid | FK vocab_items.id, indexed |
| question_type | varchar | Required |
| question | text | Required |
| user_answer | text | Required |
| correct_answer | text | Required |
| is_correct | boolean | Required |
| ai_feedback | text | Optional |
| created_at | timestamptz | Indexed |

### notifications

Stores scheduled notification records and delivery state.

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid | Primary key |
| user_id | uuid | FK users.id, indexed |
| type | varchar | `daily_reminder`, `due_review`, `test` |
| title | varchar | Required |
| body | text | Required |
| scheduled_at | timestamptz | Indexed |
| sent_at | timestamptz | Nullable |
| status | varchar | `pending`, `sent`, `failed`, `cancelled` |

### notification_devices

Recommended addition for push support.

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid | Primary key |
| user_id | uuid | FK users.id, indexed |
| platform | varchar | `android`, later `ios` |
| device_token | text | Required |
| is_active | boolean | Default true |
| created_at | timestamptz | Required |
| updated_at | timestamptz | Required |

### agent_conversations

Stores optional learning-agent conversation sessions.

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid | Primary key |
| user_id | uuid | FK users.id, indexed |
| title | varchar | Optional |
| created_at | timestamptz | Required |
| updated_at | timestamptz | Required |

### agent_messages

Stores structured agent messages.

| Column | Type | Notes |
| --- | --- | --- |
| id | uuid | Primary key |
| conversation_id | uuid | FK agent_conversations.id, indexed |
| role | varchar | `user`, `assistant`, `system` |
| content | text | Required |
| metadata | jsonb | Optional context and structured output |
| created_at | timestamptz | Required |

## SM-2 Review Design

User rating maps to quality values:

| Rating | Quality | Behavior |
| --- | --- | --- |
| Again | 1 | Reset repetition, due soon |
| Hard | 3 | Small interval increase, reduce ease |
| Good | 4 | Normal SM-2 interval |
| Easy | 5 | Larger interval increase, increase ease |

Simple first-version rules:

- New progress starts with `ease_factor = 2.5`, `interval_days = 0`, `repetitions = 0`
- Again sets interval to 0 or 1 and status to `learning`
- First successful review gives 1 day
- Second successful review gives 6 days
- Later successful reviews multiply interval by ease factor
- Ease factor should not go below 1.3
- Due words are selected by `due_at <= now()` and current user ownership

## Backend API Design

All protected endpoints require `Authorization: Bearer <access_token>`.

### Auth

#### POST /auth/register

Registers a user with email and password.

Request:

```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

Response:

```json
{
  "access_token": "jwt",
  "refresh_token": "jwt",
  "token_type": "bearer",
  "user": {
    "id": "uuid",
    "email": "user@example.com"
  }
}
```

#### POST /auth/login

Logs in with email and password.

#### POST /auth/refresh

Returns a new access token from a refresh token.

#### POST /auth/logout

First version can be client-side token removal. Backend token revocation can be added later.

### User

#### GET /me

Returns current user and profile.

#### PATCH /me/profile

Updates profile fields.

Request:

```json
{
  "name": "Duy",
  "learning_goal": "IELTS",
  "english_level": "B1",
  "daily_new_words": 10,
  "daily_review_limit": 50,
  "notification_time": "20:00:00",
  "timezone": "Asia/Ho_Chi_Minh"
}
```

### Deck

#### GET /decks

Returns decks owned by current user.

#### POST /decks

Creates a deck.

#### GET /decks/{deck_id}

Returns deck detail if owned by current user.

#### PATCH /decks/{deck_id}

Updates deck if owned by current user.

#### DELETE /decks/{deck_id}

Deletes deck if owned by current user.

### Vocabulary

#### GET /decks/{deck_id}/words

Returns words in a deck owned by current user.

#### POST /decks/{deck_id}/words

Creates a vocabulary item in a deck.

#### PATCH /words/{word_id}

Updates a vocabulary item if current user owns its deck.

#### DELETE /words/{word_id}

Deletes a vocabulary item if current user owns its deck.

#### POST /decks/{deck_id}/import

Imports CSV rows into a deck.

#### GET /decks/{deck_id}/export

Exports deck vocabulary as CSV.

### Learning

#### GET /learning/daily-plan

Returns daily new word count and due review count.

Response:

```json
{
  "date": "2026-05-24",
  "new_words_today": 10,
  "due_reviews_today": 24,
  "daily_review_limit": 50
}
```

#### GET /learning/due-words

Returns due words for flashcard review.

#### POST /learning/review

Submits a review rating and updates SRS state.

Request:

```json
{
  "vocab_item_id": "uuid",
  "rating": "good"
}
```

Response:

```json
{
  "status": "review",
  "repetitions": 3,
  "interval_days": 8,
  "ease_factor": 2.42,
  "due_at": "2026-06-01T20:00:00Z"
}
```

#### POST /learning/session/start

Creates a learning session.

#### POST /learning/session/end

Ends a learning session and stores summary stats.

### Practice

#### POST /practice/generate

Generates simple quiz questions from user vocabulary.

#### POST /practice/submit

Submits an answer and stores result.

#### GET /practice/history

Returns previous practice answers.

### Progress

#### GET /progress/summary

Returns learned words, streak, accuracy, due count, and level estimate.

Response:

```json
{
  "learned_words": 120,
  "streak_days": 7,
  "accuracy_percentage": 82.5,
  "due_reviews": 18,
  "level_estimation": "Intermediate"
}
```

#### GET /progress/daily-activity

Returns daily activity counts.

#### GET /progress/retention

Returns simple retention metrics from review logs.

#### GET /progress/decks/{deck_id}

Returns progress for a specific deck.

### Notification

#### POST /notifications/register-device

Stores or updates a device token.

#### PATCH /notifications/settings

Updates notification preferences in user profile.

#### POST /notifications/test

Creates or sends a test notification.

### Agent

Agent APIs should return structured JSON suitable for mobile UI.

#### POST /agent/explain-word

Explains a word using user profile context.

Request:

```json
{
  "word": "resilient",
  "deck_id": "uuid"
}
```

Response:

```json
{
  "word": "resilient",
  "simple_explanation": "Able to recover quickly after difficulty.",
  "level": "B1",
  "goal": "IELTS",
  "examples": [
    "She is resilient after many challenges."
  ],
  "collocations": [
    "resilient person",
    "resilient economy"
  ]
}
```

#### POST /agent/generate-examples

Generates examples for a word using level and goal.

#### POST /agent/generate-deck

Generates a small topic-based deck suggestion.

#### POST /agent/check-sentence

Planned for later; checks user sentence quality.

#### POST /agent/daily-coach

Planned for later; suggests daily learning plan from weak words and due reviews.

## Kotlin Mobile Screen Structure

### Auth Screens

- LoginScreen
- RegisterScreen
- ForgotPasswordScreen planned later

### Onboarding Screens

- ProfileSetupScreen
- GoalSelectionScreen
- LevelSelectionScreen

### Home Screens

- HomeScreen
- DailyPlanCard
- DueReviewCard
- ContinueLearningAction

### Deck Screens

- DeckListScreen
- DeckDetailScreen
- CreateDeckScreen
- EditDeckScreen

### Vocabulary Screens

- WordListScreen
- WordDetailScreen
- AddWordScreen
- EditWordScreen
- ImportExportScreen planned for P1

### Learning Screens

- FlashcardScreen
- ReviewResultScreen
- DailyPlanScreen

Flashcard UI requirements:

- Front side shows word
- Back side shows meaning and example
- Flip animation
- Large Again, Hard, Good, Easy buttons
- Usable with one hand
- Clear loading, empty, and error states

### Practice Screens

- PracticeSetupScreen
- QuizScreen
- PracticeResultScreen
- PracticeHistoryScreen

### Progress Screens

- ProgressDashboardScreen
- DailyActivityScreen
- DeckProgressScreen

### Notification and Settings Screens

- NotificationSettingsScreen
- SettingsScreen
- ProfileSettingsScreen

### Agent Screens

- WordHelpScreen
- GenerateExamplesScreen
- GenerateDeckScreen

## Milestone Checklist

### Milestone 1: Backend Foundation

Goal: Working FastAPI backend with authentication and user profile.

Tasks:

- [ ] Create backend project structure
- [ ] Configure FastAPI app in `backend/main.py`
- [ ] Configure environment settings
- [ ] Configure PostgreSQL connection
- [ ] Configure SQLAlchemy session
- [ ] Configure Alembic migrations
- [ ] Create users table model
- [ ] Create user_profiles table model
- [ ] Implement bcrypt password hashing
- [ ] Implement JWT access token creation
- [ ] Implement register API
- [ ] Implement login API
- [ ] Implement refresh API
- [ ] Implement current-user dependency
- [ ] Implement GET /me
- [ ] Implement PATCH /me/profile
- [ ] Add basic validation and error handling
- [ ] Add tests for register, login, and profile update

Testing steps:

- [ ] Start backend locally
- [ ] Run database migration
- [ ] Register user
- [ ] Login user
- [ ] Fetch current user
- [ ] Update profile

### Milestone 2: Mobile Foundation

Goal: Working Compose app with auth screens and token storage.

Tasks:

- [ ] Create mobile project structure
- [ ] Configure Compose
- [ ] Configure app theme
- [ ] Configure navigation graph
- [ ] Configure API client
- [ ] Configure token storage with DataStore
- [ ] Create auth repository
- [ ] Build LoginScreen
- [ ] Build RegisterScreen
- [ ] Build ProfileSetupScreen
- [ ] Implement auth state routing
- [ ] Connect login/register screens to backend
- [ ] Add loading and error states

Testing steps:

- [ ] Launch app
- [ ] Register new account
- [ ] Login with existing account
- [ ] Persist auth state after app restart
- [ ] Complete profile setup

### Milestone 3: Vocabulary Core

Goal: Users can create decks and manage vocabulary items.

Backend tasks:

- [ ] Create vocab_decks model and migration
- [ ] Create vocab_items model and migration
- [ ] Implement deck CRUD APIs
- [ ] Implement vocabulary CRUD APIs
- [ ] Enforce ownership checks
- [ ] Add indexes for deck and word lookups
- [ ] Add tests for deck and vocabulary APIs

Mobile tasks:

- [ ] Build DeckListScreen
- [ ] Build CreateDeckScreen
- [ ] Build DeckDetailScreen
- [ ] Build EditDeckScreen
- [ ] Build WordListScreen
- [ ] Build AddWordScreen
- [ ] Build EditWordScreen
- [ ] Connect deck and vocabulary APIs
- [ ] Add empty states for no decks and no words

Testing steps:

- [ ] Create deck
- [ ] Edit deck
- [ ] Delete deck
- [ ] Add word
- [ ] Edit word
- [ ] Delete word
- [ ] Confirm users cannot access another user's deck or words

### Milestone 4: Learning Core

Goal: Users can learn flashcards and review words with SM-2.

Backend tasks:

- [ ] Create user_vocab_progress model and migration
- [ ] Create review_logs model and migration
- [ ] Create learning_sessions model and migration
- [ ] Implement SM-2 algorithm in `backend/srs/sm2.py`
- [ ] Create initial progress row when a word is added or first reviewed
- [ ] Implement GET /learning/daily-plan
- [ ] Implement GET /learning/due-words
- [ ] Implement POST /learning/review
- [ ] Implement POST /learning/session/start
- [ ] Implement POST /learning/session/end
- [ ] Add tests for SM-2 ratings
- [ ] Add tests for due word ownership and review updates

Mobile tasks:

- [ ] Build DailyPlanScreen or home daily plan section
- [ ] Build FlashcardScreen
- [ ] Add flip animation
- [ ] Add Again, Hard, Good, Easy buttons
- [ ] Submit review result
- [ ] Show next card after review
- [ ] Show completion state when no cards remain

Testing steps:

- [ ] Add words to deck
- [ ] Open flashcard learning
- [ ] Flip card
- [ ] Submit each rating type
- [ ] Confirm next review date changes correctly
- [ ] Confirm due words disappear after successful review

### Milestone 5: Progress

Goal: Users can see learning progress and basic analytics.

Backend tasks:

- [ ] Implement learned words count
- [ ] Implement streak calculation from review logs or sessions
- [ ] Implement accuracy calculation
- [ ] Implement level estimation rule
- [ ] Implement GET /progress/summary
- [ ] Implement GET /progress/daily-activity
- [ ] Implement GET /progress/retention
- [ ] Implement GET /progress/decks/{deck_id}
- [ ] Add progress API tests

Mobile tasks:

- [ ] Build ProgressDashboardScreen
- [ ] Show learned words
- [ ] Show streak
- [ ] Show accuracy percentage
- [ ] Show simple daily activity chart
- [ ] Show deck progress

Testing steps:

- [ ] Complete several reviews
- [ ] Confirm summary values update
- [ ] Confirm daily activity reflects review history
- [ ] Confirm deck progress only includes selected deck

### Milestone 6: Practice

Goal: Users can practice words with simple quizzes.

Backend tasks:

- [ ] Create practice_answers model and migration
- [ ] Implement simple multiple-choice generation
- [ ] Implement POST /practice/generate
- [ ] Implement POST /practice/submit
- [ ] Implement GET /practice/history
- [ ] Save answer correctness
- [ ] Add tests for quiz generation and answer submission

Mobile tasks:

- [ ] Build PracticeSetupScreen
- [ ] Build QuizScreen
- [ ] Build PracticeResultScreen
- [ ] Build PracticeHistoryScreen
- [ ] Connect practice APIs

Testing steps:

- [ ] Generate quiz
- [ ] Submit correct answer
- [ ] Submit wrong answer
- [ ] Confirm practice history is saved

### Milestone 7: Notification

Goal: Users can configure reminders and register device token.

Backend tasks:

- [ ] Create notifications model and migration
- [ ] Create notification_devices model and migration
- [ ] Implement POST /notifications/register-device
- [ ] Implement PATCH /notifications/settings
- [ ] Implement POST /notifications/test
- [ ] Add simple notification scheduling design
- [ ] Add tests for settings and device registration

Mobile tasks:

- [ ] Build NotificationSettingsScreen
- [ ] Request notification permission where required
- [ ] Register device token
- [ ] Update reminder time
- [ ] Show notification enabled state

Testing steps:

- [ ] Save notification time
- [ ] Register device token
- [ ] Trigger test notification endpoint

### Milestone 8: Simple AI Agent

Goal: Provide simple contextual learning help.

Backend tasks:

- [ ] Create agent_conversations model and migration
- [ ] Create agent_messages model and migration
- [ ] Implement agent context builder using user profile, deck, weak words, and review history
- [ ] Implement POST /agent/explain-word
- [ ] Implement POST /agent/generate-examples
- [ ] Implement POST /agent/generate-deck
- [ ] Return structured JSON responses
- [ ] Add tests with mocked agent provider

Mobile tasks:

- [ ] Build WordHelpScreen
- [ ] Build GenerateExamplesScreen
- [ ] Build GenerateDeckScreen
- [ ] Show structured agent output
- [ ] Allow saving generated words to a deck later

Testing steps:

- [ ] Explain a word from a deck
- [ ] Confirm response uses user level and goal
- [ ] Generate examples
- [ ] Generate a small deck suggestion

### Milestone 9: Import and Export

Goal: Users can import and export deck vocabulary with CSV.

Backend tasks:

- [ ] Define CSV template
- [ ] Implement CSV row validation
- [ ] Implement POST /decks/{deck_id}/import
- [ ] Implement GET /decks/{deck_id}/export
- [ ] Return import summary with success and failed rows
- [ ] Add tests for valid and invalid CSV files

Mobile tasks:

- [ ] Build ImportExportScreen
- [ ] Pick CSV file
- [ ] Upload CSV file
- [ ] Show import result
- [ ] Download or share exported CSV

Testing steps:

- [ ] Import valid CSV
- [ ] Import CSV with invalid rows
- [ ] Export deck
- [ ] Re-import exported deck into another deck

### Milestone 10: Polish

Goal: Improve reliability, usability, and maintainability.

Backend tasks:

- [ ] Add indexes for common queries
- [ ] Optimize due words query
- [ ] Add consistent error response format
- [ ] Add pagination where needed
- [ ] Add request validation coverage
- [ ] Add basic API documentation examples
- [ ] Add health check endpoint

Mobile tasks:

- [ ] Improve loading states
- [ ] Improve error states
- [ ] Improve empty states
- [ ] Improve one-hand flashcard usability
- [ ] Add retry actions
- [ ] Add basic accessibility labels
- [ ] Improve navigation transitions

Testing steps:

- [ ] Run backend tests
- [ ] Run mobile unit tests where available
- [ ] Manually test core learning flow end to end
- [ ] Test slow network and error cases

## Suggested First Implementation Order

The first working slice should be small and complete:

1. Backend auth and profile
2. Mobile auth and profile setup
3. Backend deck and vocabulary CRUD
4. Mobile deck and vocabulary screens
5. Backend SM-2 review APIs
6. Mobile flashcard review flow
7. Backend progress summary
8. Mobile progress dashboard
9. Notification settings
10. Simple AI explain-word endpoint

This order protects the core learning loop before adding import/export, practice, push delivery, or advanced AI.

## Security Plan

- Use bcrypt for password hashing
- Use JWT access tokens
- Use refresh tokens for longer sessions
- Validate all request payloads with Pydantic
- Normalize and validate email addresses
- Enforce ownership checks on every deck, vocabulary, progress, practice, notification, and agent endpoint
- Never return password hashes
- Use HTTPS in production
- Store mobile tokens securely through platform-backed storage where possible
- Add rate limiting later for auth endpoints

## Performance Plan

- Keep initial mobile screen lightweight
- Avoid loading all vocabulary at app startup
- Paginate large deck and word lists later if needed
- Add indexes on `user_id`, `deck_id`, `vocab_item_id`, `due_at`, and timestamp fields used by analytics
- Use efficient due-word queries filtered by current user
- Keep backend stateless so multiple workers can be added later
- Target support for around 1,000 concurrent users with proper database pooling and indexed queries

## UX Principles

- Keep screens simple and focused
- Use large flashcard review buttons
- Keep the learning flow usable with one hand
- Avoid complex dashboard UI in the MVP
- Provide clear empty states for new users
- Provide clear loading and error states
- Prioritize daily action: new words, due reviews, progress

## CSV Import Format

First-version CSV columns:

```csv
word,pronunciation,meaning,description_en,example,collocations,related_words,note
```

Rules:

- `word` is required
- `meaning` is required
- `collocations` can be separated by `;`
- `related_words` can be separated by `;`
- Invalid rows should be skipped and reported
- Import should never create words in decks not owned by the current user

## Level Estimation Rules

Simple initial rule:

- Beginner: fewer than 300 learned words or accuracy below 60%
- Intermediate: 300 to 1500 learned words and accuracy from 60% to 85%
- Advanced: more than 1500 learned words and accuracy above 85%

This is intentionally simple and can be replaced later.

## Definition of Done for MVP

The MVP is complete when a user can:

- Register with email and password
- Log in
- Create or update profile
- Create a deck
- Add vocabulary items
- Learn words through flashcards
- Rate reviews with Again, Hard, Good, and Easy
- Have the backend calculate next review time with SM-2
- See daily plan
- See progress summary
- Configure reminder settings
- Ask for a simple AI explanation of a word

## End-to-End MVP Test Script

1. Start backend and database
2. Run migrations
3. Start mobile app
4. Register a new account
5. Complete profile with goal and level
6. Create a deck named `IELTS Basics`
7. Add at least five vocabulary items
8. Open learning flow
9. Review each word using different ratings
10. Confirm due dates and progress update
11. Open progress dashboard
12. Configure daily reminder time
13. Use AI explain-word for one vocabulary item
14. Log out and log back in
15. Confirm user data is still available

## Implementation Rule Reminder

Do not build all features at once. Each milestone should result in working code with clear testing steps. Keep the project simple, readable, and maintainable. Build the core learning flow first, then improve around it.
