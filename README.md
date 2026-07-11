# InnoAI — Offline Android AI Chat

InnoAI is a local-first Android chat application that runs supported large language models directly on the device with llama.cpp. After a model is downloaded, text generation works without a cloud API, account, or internet connection.

## Features

- Fully on-device, streaming text generation
- CPU inference via a bundled llama.cpp native runtime (a GPU selection in Settings automatically falls back to CPU)
- Downloadable, revision-pinned GGUF models with exact file-size and SHA-256 validation
- Multiple local conversations with rename, delete, search, and history restoration
- Conversation export as plain text or JSON
- Configurable model, backend, temperature, and maximum output tokens
- Local device/model/chat statistics
- Responsive Jetpack Compose interface for phones, tablets, and emulators
- Optional Hugging Face token stored with Android Keystore encryption
- Background model downloads through WorkManager
- Local SQLite chat storage and DataStore preferences

> InnoAI is currently a text-inference app. Image and audio attachments can be represented in local chat data, but multimodal model inference is intentionally disabled until the required Android pipelines are implemented and verified.

## Requirements

| Requirement | Value |
| --- | --- |
| Android Studio | Recent stable version with Android SDK 35 |
| NDK + CMake | Auto-provisioned by Gradle (CMake 3.22.1) for the bundled llama.cpp build |
| JDK | 21 |
| Minimum Android version | Android 9 / API 28 |
| Target Android version | Android 15 / API 35 |
| Supported ABIs | `arm64-v8a`, `x86_64` |
| Recommended test target | Physical 64-bit Android device |
| Free storage | At least the selected model size plus installation/build space |
| RAM | Model-dependent; smaller models are recommended for low-memory devices |

The first build needs internet access to download Gradle and Maven dependencies. The app also needs internet access when downloading a model. Chat inference itself is local after the model is installed.

## Quick start

### 1. Open the project

Clone or copy the repository, then open its root directory in Android Studio:

```text
ChatApp/
├── app/
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── gradlew.bat
```

Allow Gradle sync to finish and make sure Android Studio uses JDK 21.

### 2. Prepare a device

Use either:

- a physical `arm64-v8a` Android device running API 28 or newer, or
- an `x86_64` emulator image.

For practical model performance, a modern physical device with sufficient free RAM and storage is preferred.

### 3. Build and install

On Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
```

Alternatively, select the `app` configuration in Android Studio and click **Run**.

### 4. Download and use a model

1. Complete the onboarding screen.
2. Open **Models** and choose an available model.
3. All cataloged models are public and ungated; a Hugging Face read token in **Settings** is optional.
4. Download the model and wait for integrity verification to complete.
5. Tap **Use model** to initialize it.
6. Open **Chat** and send a message.

The default model is Qwen 3 0.6B. CPU is the default backend. This llama.cpp build is CPU-only; selecting GPU in Settings automatically falls back to CPU.

## Available models

Only artifacts pinned to an immutable repository revision, expected byte count, and SHA-256 checksum are downloadable.

| Model | Approx. download | Context | Quantization | Hugging Face token |
| --- | ---: | ---: | --- | --- |
| Qwen 3 0.6B | 610 MB | 4096 tokens | Q8_0 | Not required |
| Gemma 3 270M IT | 278 MB | 4096 tokens | Q8_0 | Not required |
| Gemma 3 1B IT | 769 MB | 4096 tokens | Q4_K_M | Not required |
| Qwen 2.5 1.5B Instruct | 1.76 GB | 4096 tokens | Q8_0 | Not required |
| DeepSeek R1 Qwen 1.5B | 1.76 GB | 4096 tokens | Q8_0 | Not required |
| Phi 4 Mini Instruct | 2.32 GB | 4096 tokens | Q4_K_M | Not required |

Other model cards may appear as unavailable. Multimodal and tool-use models are deliberately disabled in this text-first build.

Model licensing is controlled by each model publisher. Review the linked model page and license before downloading or redistributing an artifact.

## Configuration

Open **Settings** to configure:

- **Backend:** CPU or GPU. CPU is the compatibility default; GPU currently falls back to CPU.
- **Model:** any verified model already supported by the catalog.
- **Temperature:** controls response randomness.
- **Maximum tokens:** configurable from 512 to 2048 output tokens.
- **Hugging Face token:** optional for public models and needed for gated repositories.
- **History:** export or permanently clear local conversations.

Changing inference settings may require the engine to be reinitialized before the new values take effect.

## Architecture

```text
Compose screens
    ↓
ViewModels
    ↓
Use cases / repositories
    ├── llama.cpp engine (JNI) and conversation
    ├── WorkManager model downloads
    ├── SQLite conversation history + FTS search
    ├── DataStore app preferences
    └── Android Keystore encrypted token storage
```

Main packages:

| Package | Responsibility |
| --- | --- |
| `ui/screens` | Onboarding, model download, chat, settings, and profile screens |
| `ui/components` | Reusable Compose chat and navigation components |
| `ui/navigation` | App routes and screen transitions |
| `data/repository` | Chat engine, model files, context management, and repository logic |
| `data/local` | SQLite conversation persistence, search, statistics, and export |
| `data/preferences` | DataStore preferences and encrypted Hugging Face token |
| `data/model` | Chat messages, model catalog, attachments, and validation results |
| `domain/usecase` | Send-message and download-model operations |
| `worker` | Resumable foreground/background model download work |
| `di` | Hilt dependency injection bindings |

## Privacy and security

- Prompts, responses, conversation metadata, and model files remain in the app's private local storage.
- Inference does not send chat content to a server.
- Network access is used for model downloads.
- The optional Hugging Face token is encrypted using Android Keystore and is not embedded in the APK or read from `local.properties`.
- Android backup is disabled for the application.
- Downloaded models are validated against the catalog's exact size and SHA-256 checksum before use.
- Diagnostic logs record engine state and generated chunk sizes, not prompt or response text.

Uninstalling the app removes its private conversations, preferences, token, and downloaded models unless the device manufacturer provides behavior outside standard Android app-data handling.

## Build and verification

Run the complete local verification suite from the repository root:

```powershell
.\gradlew.bat clean testDebugUnitTest lintDebug assembleDebug assembleRelease
```

Useful individual commands:

```powershell
# Unit tests
.\gradlew.bat testDebugUnitTest

# Android lint
.\gradlew.bat lintDebug

# Debug APK
.\gradlew.bat assembleDebug

# Release APK
.\gradlew.bat assembleRelease
```

Generated APKs are placed under:

```text
app/build/outputs/apk/debug/
app/build/outputs/apk/release/
```

The release build is minified and resource-shrunk. It is not automatically production-signed; configure a private signing key before distribution.

## Tests

The unit-test suite covers core behavior including:

- model catalog and artifact validation
- context-window retention and rebuilding
- assistant response cleanup
- chat repository streaming behavior
- chat ViewModel state and error handling

An Android instrumentation test scaffold is included under `app/src/androidTest`.

## Troubleshooting

### The model will not download

- Confirm the device has a stable internet connection and enough free storage.
- Allow notifications on Android 13+ so foreground download progress can be shown.
- All cataloged models are ungated; if a repository ever requires access, add a valid Hugging Face read token in Settings.
- Retry the download. A partial or corrupted artifact will not be accepted as a valid model.

### Model validation failed

The downloaded file does not match its pinned size or checksum. Delete/re-download the model from the app. Do not rename an unrelated model file to one of the catalog filenames.

### Engine initialization failed

- Verify that the device ABI is `arm64-v8a` or `x86_64`.
- Choose a smaller model if the device is low on memory.
- Switch the backend to CPU.
- Close memory-heavy apps and retry.

### GPU mode does not work

The bundled llama.cpp runtime is compiled for CPU only. Selecting GPU automatically falls back to CPU. CPU is the expected and most portable option.

### Responses stop or the app feels slow

- Use Qwen 3 0.6B or Gemma 3 270M on lower-memory hardware.
- Reduce maximum output tokens.
- Start a new conversation to reduce retained context.
- Performance varies by device and CPU core count.

### Logcat diagnosis

Filter Logcat by package `com.example.chatapp` or by these tags:

```text
ChatEngineManager
ChatViewModel
```

Messages from `com.google.android.gms` are normally unrelated to local inference.

## Technology stack

- Kotlin 2.2.20 and Java 21 toolchain
- Android Gradle Plugin 9.0.1
- Jetpack Compose with Material 3
- llama.cpp (vendored, pinned; see `app/src/main/cpp/LLAMA_CPP_VERSION.txt`) built with the Android NDK and CMake
- Hilt dependency injection
- Kotlin coroutines and Flow
- WorkManager
- DataStore Preferences
- SQLite with full-text search
- OkHttp
- JUnit, Turbine, MockK, and AndroidX test libraries

## Current limitations

- Text generation only; vision, audio inference, and tool calling are not implemented.
- Only the cataloged `.gguf` artifacts are supported.
- GPU inference is not compiled into this build; the GPU setting falls back to CPU.
- Model performance varies significantly across devices.
- Downloaded models consume substantial private app storage.
- No cloud synchronization or cross-device history is provided.
- Release signing and store distribution configuration are not included.

## Project status

The project is an Android application intended for local development and on-device testing. Before publishing it, provide your own application ID, signing configuration, store listing, privacy disclosures, model-license review, and device compatibility testing.

## License

No repository-level software license is currently included. Unless a license is added, source-code reuse and redistribution rights are not granted by default. Model artifacts have separate licenses supplied by their respective publishers.
