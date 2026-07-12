# Repository Guidelines

## Project Structure & Module Organization

This is a single-module Android application. Kotlin production code lives in `app/src/main/java/com/example/chatapp`, organized by responsibility: `ui/` for Compose screens and components, `domain/` for use cases, `data/` for models, persistence, preferences, and repositories, `worker/` for model downloads, and `di/` for Hilt bindings. Android resources are in `app/src/main/res`. JNI integration is in `app/src/main/cpp`; `llama.cpp/` is vendored upstream code and has its own `AGENTS.md`. Local JVM tests mirror production packages under `app/src/test`; device tests belong in `app/src/androidTest`. Project documentation is under `docs/`.

## Build, Test, and Development Commands

Run commands from the repository root. On Windows use the checked-in wrapper:

- `.\gradlew.bat assembleDebug` builds the debug APK.
- `.\gradlew.bat installDebug` installs it on a connected device or emulator.
- `.\gradlew.bat testDebugUnitTest` runs local JVM tests.
- `.\gradlew.bat connectedDebugAndroidTest` runs instrumentation tests on a device.
- `.\gradlew.bat lintDebug` performs Android static analysis.
- `.\gradlew.bat clean testDebugUnitTest lintDebug assembleDebug assembleRelease` matches the main CI verification sequence.

Use JDK 21, Android SDK 35, and CMake 3.22.1. The first build downloads Gradle and native dependencies.

## Coding Style & Naming Conventions

Follow standard Kotlin and Jetpack Compose conventions with four-space indentation and trailing commas in multiline declarations. Use `PascalCase` for classes, composables, and test classes; `camelCase` for functions and properties; and descriptive suffixes such as `Screen`, `ViewModel`, `Repository`, and `UseCase`. Keep UI state in ViewModels and isolate storage, networking, and inference behind repository interfaces. Resource names use lowercase `snake_case`. Fix production lint warnings rather than adding baselines; see `docs/lint.md`.

## Testing Guidelines

JUnit, MockK, Turbine, and coroutine-test support local tests; AndroidX JUnit and Espresso support instrumentation tests. Name tests `*Test.kt`, mirror the source package, and use behavior-focused test names. Add unit coverage for ViewModel state transitions, repositories, validation, and context-window logic. Use instrumentation tests only for Android framework or UI behavior.

## Commit & Pull Request Guidelines

History uses short, imperative fix summaries; improve on generic messages such as `fixed issue` by naming the behavior, for example `Fix chat history restoration`. Keep commits focused. Pull requests should explain the problem and solution, list verification commands, link relevant issues, and include screenshots or recordings for UI changes. Call out changes to model metadata, native code, permissions, persistence, or release configuration. Never commit `local.properties`, `keystore.properties`, tokens, signing keys, model files, or generated build output.
