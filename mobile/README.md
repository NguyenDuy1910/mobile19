# MinLish Mobile

Android MVP built with Kotlin, Jetpack Compose, Material 3, Navigation, Retrofit, DataStore, ViewModel, coroutines, and StateFlow.

## Features

- Persistent JWT login, registration, refresh, logout, and first-run profile setup
- Home daily plan, review queue, deck summary, and progress snapshot
- Deck and vocabulary CRUD with DictionaryAPI.dev lookup and enriched save
- SM-2 flashcard review with flip animation and Again, Hard, Good, Easy ratings
- Progress activity, retention, and deck progress sections
- Practice quiz generation, feedback, results, and history
- Notification settings, placeholder device registration, and simulated test notifications
- Structured word explanation, generated examples, and generated deck ideas

## Configure Backend URL

The default debug URL is the Android emulator loopback:

```text
http://10.0.2.2:8000/
```

Override it with a Gradle property. Keep the trailing slash.

For a physical device on the same Wi-Fi network:

```bash
./gradlew :app:installDebug -PMINLISH_API_BASE_URL=http://192.168.1.10:8000/
```

Replace `192.168.1.10` with the development machine's LAN IP. The FastAPI server must listen on the network, for example:

```bash
uv run uvicorn main:app --reload --host 0.0.0.0
```

For a production build, configure the HTTPS API endpoint:

```bash
./gradlew :app:assembleRelease -PMINLISH_API_BASE_URL=https://api.example.com/
```

## Run

1. Install Android Studio with Android SDK 35.
2. Open the `mobile` folder as a Gradle project.
3. Ensure `local.properties` contains your SDK path, such as `sdk.dir=/Users/your-name/Library/Android/sdk`.
4. Start the MinLish backend.
5. Run the `app` configuration on an emulator or connected Android device.

Command-line checks:

```bash
cd mobile
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

## MVP Limitations

- CSV export uses Android's share sheet with CSV text.
- CSV import uses Android's document picker and reports imported and skipped row counts.
- Notification push delivery is simulated because FCM is not configured.
- Generated deck ideas are read-only until the backend adds an endpoint to persist a generated deck in one request.
- Room is intentionally not required; the backend remains the source of truth.
