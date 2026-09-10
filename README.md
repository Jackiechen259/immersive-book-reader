# Immersive Reader

Immersive Reader is a native Android EPUB reader built around calm, persistent reading sessions. A session owns the timer, exit policy, focus capability, and recovery state; the reader UI does not keep a second copy of that state.

## What is implemented

- SAF EPUB import (`application/epub+zip`) into app-private storage.
- Readium Kotlin Toolkit 3.3.0 for validation, metadata, covers, EPUB navigation, and Locators.
- Room-backed books and reading sessions, with automatic Locator persistence.
- Unlimited and arbitrary-duration countdown sessions using epoch timestamps.
- Session recovery after Activity recreation or process restart, including automatic expiry completion.
- Compose reader controls with auto-hide, font size, line height, theme, paginated/scrolling mode, and progress.
- Immersive system-bar handling, optional DND with permission checks, hold-to-exit, and emergency exit.
- Consumer screen-pinning fallback and a separate Device Owner Deep Focus path.
- Session summary and statistics: total time, sessions, completed/interrupted sessions, and time by book.

## Architecture

```text
Library / Start Session / Settings (Compose)
            ↓
       ViewModels
            ↓
ReadingSessionCoordinator ── ReadingSessionRepository ── Room
            ↓
 ReaderActivity + EpubNavigatorFragment + Compose overlay
            ↓
 FocusController (immersive / pinned / Device Owner Lock Task)
```

The independent product axes are `TimerMode`, `ExitPolicy`, and `FocusCapabilities`. An unlimited session can still use hold-to-exit and focus locking; a countdown is not automatically treated as a kiosk session.

## Build

Requirements:

- JDK 17 or newer
- Android SDK platform/build tools 36
- Android API 26 or newer

PowerShell:

```powershell
$env:JAVA_HOME = 'path-to-jdk-17-or-newer'
$env:ANDROID_HOME = 'path-to-android-sdk'
./gradlew.bat :app:testDebugUnitTest
./gradlew.bat :app:assembleDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Releases and in-app updates

Sideloaded installs can check GitHub Latest Release, download the APK, and use the system installer. The app looks for a non-debug `.apk` on `Jackiechen259/immersive-book-reader`.

### Publish a version

1. Bump `versionName` and `versionCode` together in `app/build.gradle.kts`. `versionCode` must increase or Android will refuse the install.
2. Commit the bump.
3. Tag `v{versionName}` (example: `v0.2.0`) and push the tag.
4. GitHub Actions builds a signed release APK and attaches `immersive-reader-{versionName}.apk` to the GitHub Release.

The tag without the leading `v` must equal `versionName`.

### Signing secrets

Create an upload keystore (do not commit it) and add these repository secrets:

- `SIGNING_KEYSTORE_BASE64` — `base64 -w0 upload.jks` (on macOS: `base64 -i upload.jks | tr -d '\n'`)
- `SIGNING_STORE_PASSWORD`
- `SIGNING_KEY_ALIAS`
- `SIGNING_KEY_PASSWORD`

The release job fails if any secret is missing. Local `assembleRelease` without those environment variables stays unsigned.

A debug-signed install cannot be updated by a release-signed APK.

## Focus capability matrix

| Capability | Regular consumer device | Device Owner test device |
| --- | --- | --- |
| Immersive system bars | Available | Available |
| Exit confirmation / hold | Available | Available |
| DND | Only after notification-policy access | Only after notification-policy access |
| `startLockTask()` fallback | Screen pinning, subject to system UX | True Lock Task after allowlisting |
| Deep Focus (`TIME_LOCKED`) | Disabled in the setup UI | Available |

The app never uses Accessibility to fight Android navigation, settings, uninstall, or emergency system UI. Screen pinning is not advertised as true kiosk mode.

## Device Owner development provisioning

Deep Focus is optional and intended for a clean emulator or a dedicated managed test device. Provisioning can fail on an already-used device and may require wiping it first. The app does not attempt to elevate itself silently.

After installing the debug APK on a suitable test device/emulator, provision the receiver from a host machine:

```bash
adb shell dpm set-device-owner \
  com.immersive.reader/.focus.FocusDeviceAdminReceiver
```

The Settings screen reports whether Device Owner is actually available. Remove management using the normal Android device-management flow or by wiping the test device; do not run this command on a personal production phone.

## Timer and recovery rules

`OPEN_ENDED` stores `startEpochMillis` and computes elapsed time from the current wall clock. `COUNTDOWN` stores both the duration and `plannedEndEpochMillis`; it never decrements an in-memory counter. The coordinator is a singleton state machine backed by Room, so Activity recreation does not reset the session.

If the app starts with an active session, the main screen offers to continue it. If a countdown has already expired, the coordinator marks it `COMPLETED`, restores focus policies, and avoids re-locking the device. DND changes persist the previous interruption filter so a process restart has a restoration path.

## Testing

The current automated coverage includes timestamp-based session timing tests. Run:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

Manual device validation is still required for EPUB rendering, Storage Access Framework imports, gesture/navigation modes, screen pinning, DND permission flows, rotation/process death, calls, screen-off behavior, and Device Owner provisioning.

## Known limitations

- No PDF, audiobook, TTS, DRM/LCP production integration, cloud sync, accounts, or online bookstore.
- The repository has no emulator/physical-device session attached to this development environment, so device-only behavior must be exercised on Android hardware or an emulator.
- Room currently uses destructive migration for the pre-release MVP schema.
- Android system UI and screen-pinning prompts remain controlled by the OS; no consumer app can guarantee absolute prevention of leaving the app.
