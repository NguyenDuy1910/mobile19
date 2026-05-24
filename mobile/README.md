# MinLish Mobile

Android app built with Kotlin and Jetpack Compose.

## Open in Android Studio or IntelliJ IDEA

Open the repository root, not only this folder. The Gradle project includes the app module as `:mobile:app`.

## Run

1. Install Android Studio or IntelliJ IDEA with the Android plugin.
2. Open the repository root.
3. Copy `local.properties.example` to `local.properties` if the IDE does not create it automatically.
4. Set `sdk.dir` to your Android SDK path.
5. Let Gradle sync.
6. Select the `mobile:app` run configuration.
7. Run on an emulator or Android device.

Common SDK paths:

- macOS: `/Users/your-name/Library/Android/sdk`
- Windows: `C:\Users\your-name\AppData\Local\Android\Sdk`

The app currently starts with a lightweight Compose shell. Feature screens will be added milestone by milestone.
