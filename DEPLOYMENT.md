# Deployment Guide — InnoAI

## 1. Create your signing key (one time)

```
keytool -genkeypair -v -keystore innoai-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias innoai
```

Keep `innoai-release.jks` somewhere safe and **never commit it** (already gitignored).

## 2. Create `keystore.properties` in the project root

```
storeFile=C:/path/to/innoai-release.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=innoai
keyPassword=YOUR_KEY_PASSWORD
```

This file is gitignored. When it exists, release builds are signed automatically;
when it doesn't, the build produces an unsigned APK (fine for local testing).

## 3. Build

```
gradlew.bat :app:bundleRelease      # AAB for Play Store upload
gradlew.bat :app:assembleRelease    # APK for direct distribution
```

Outputs:
- `app/build/outputs/bundle/release/app-release.aab`
- `app/build/outputs/apk/release/app-release.apk`

## 4. Before each Play Store upload

- Bump `versionCode` (must increase every upload) and `versionName` in `app/build.gradle.kts`.
- The application ID is `com.dailyorbitstudio.innoai` — it is **permanent after the
  first upload**; change it now if you want a different one.
- Run the checks: `gradlew.bat :app:testDebugUnitTest :app:lintRelease`

## Notes

- Release builds use R8 minification + resource shrinking; the llama.cpp JNI bridge
  is kept via `app/proguard-rules.pro`.
- ABIs shipped: `arm64-v8a`, `x86_64` (covers modern phones + emulators).
