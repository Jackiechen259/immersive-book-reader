# GitHub Release Auto-Update

Date: 2026-09-09  
Status: Approved for implementation planning  
Repo: `Jackiechen259/immersive-book-reader`

## Problem

The app is distributed as a sideloaded APK, not through Play Store. There are no GitHub Releases, no CI, and no `INTERNET` permission. Users have no in-app path to learn about or install a newer build.

## Goals

- Check GitHub Latest Release from inside the app.
- Download the release APK and install it with the system installer.
- Silently check when the library opens (no active reading session); always allow a manual check in Settings.
- Publish a signed release APK to GitHub Releases when a `vX.Y.Z` tag is pushed.

## Non-goals

- Play Store in-app updates, F-Droid, or third-party updater libraries.
- Pre-release / beta channels (`/releases/latest` already ignores GitHub pre-releases).
- Forced updates that block reading.
- Background download via `DownloadManager` or WorkManager.
- Updating a debug-signed install with a release-signed APK (signature mismatch is expected).
- Instrumented tests that talk to real GitHub or complete an APK install.

## Architecture

The updater is an isolated `update` package. It does not read or write reading-session state. UI observes a `StateFlow` and never calls GitHub directly.

| Unit | Responsibility | Depends on |
| --- | --- | --- |
| `GitHubReleaseClient` | `GET /repos/Jackiechen259/immersive-book-reader/releases/latest`, return parsed release | HTTPS |
| `GitHubReleaseParser` | Pure function: response JSON → `GitHubRelease` | none |
| `VersionComparator` | Semver compare, optional `v` prefix | none |
| `ApkDownloader` | Stream APK into app cache with progress and cancellation | client / URL |
| `ApkInstaller` | Unknown-sources permission + FileProvider + system install UI | Android |
| `UpdatePreferences` | Last check epoch, skipped tag | DataStore |
| `UpdateCoordinator` | Orchestrates check / download / install; sole owner of `UpdateState` | units above |

Hilt `@Singleton` for `UpdateCoordinator`, client, downloader, installer, and preferences. Bindings live in `di/UpdateModule.kt`.

`ReadingSessionCoordinator` stays unaware of updates. The UI layer decides whether an auto-prompt is allowed by looking at session recovery state.

### `UpdateState`

```text
Idle → Checking → UpToDate
                → Available(release)
                → Error(message)
Available → Downloading(progress 0f..1f) → ReadyToInstall
                                         → Error
ReadyToInstall → (system installer; app may be killed)
```

`Idle` is only the pre-check default. After a check, the coordinator stays on `UpToDate`, `Available`, `ReadyToInstall`, or `Error` until the next check or user action.

## Data flow

1. Library is shown and session recovery is null.
2. `UpdateCoordinator.maybeAutoCheck()` runs if last check is older than 6 hours.
3. Client fetches Latest Release. Parser picks the APK asset. Comparator compares `BuildConfig.VERSION_NAME` to `tag_name`.
4. If remote is newer and the tag is not skipped, state becomes `Available`. ImmersiveReaderApp shows a dialog (not during recovery).
5. User chooses Update → download to cache → `ReadyToInstall`.
6. If `canRequestPackageInstalls()` is false, open `ACTION_MANAGE_UNKNOWN_APP_SOURCES` for this package; on return, launch the installer.
7. Settings always offers a manual `check()` that bypasses the 6-hour cache. Skip does not hide availability in Settings.

### GitHub request

- URL: `https://api.github.com/repos/Jackiechen259/immersive-book-reader/releases/latest`
- Headers: `Accept: application/vnd.github+json`, `User-Agent: ImmersiveReader/{versionName}`, `X-GitHub-Api-Version: 2022-11-28`
- No auth token in the app.
- Use `HttpURLConnection` (no new network library). Follow redirects.
- Unauthenticated limit is 60 requests/hour/IP; 6-hour auto-check cache keeps this negligible.

### Asset selection

From `assets[]`, choose the first whose `name` ends with `.apk` and does not contain `debug` (case-insensitive). Prefer `browser_download_url`. If none, the check fails with a clear error.

Published asset name: `immersive-reader-{versionName}.apk`.

### Version comparison

- Strip a single leading `v`/`V` from the tag.
- Parse `MAJOR.MINOR.PATCH` (missing segments = 0). Extra suffix after `-` is a pre-release and compares as older than the same triple without a suffix.
- Update is available iff remote version is strictly greater than `BuildConfig.VERSION_NAME`.
- `versionCode` is not present in the GitHub payload. Release process must increment `versionCode` or Android will refuse the install. The in-app checker uses versionName/semver only.

## When the UI may prompt

`ImmersiveReaderApp` calls `maybeAutoCheck()` once the main UI is up and `recovery == null`. It does not call into `ReadingSessionCoordinator`.

Auto-dialog is shown only when all of these hold:

- `UpdateState` is `Available`.
- `SessionStateViewModel.recovery == null` (recovery dialog wins).
- Latest tag is not in persisted `skippedUpdateTag`.
- Latest tag is not in in-memory `autoPromptDismissedTag` (this process, after Later).

`maybeAutoCheck()` itself runs only if the last auto-check timestamp is older than 6 hours. Failed auto-checks still write that timestamp so a down network does not retry on every recomposition.

Manual Settings `check()` bypasses the 6-hour cache. Settings still shows `Available` for a skipped tag; the library auto-dialog does not.

**Later** sets `autoPromptDismissedTag` and does not persist skip. The 6-hour stamp (already written by the check) prevents another auto-check this window; the in-memory flag prevents the same `Available` state from immediately reopening the dialog.

**Skip** persists `skippedUpdateTag` and also sets `autoPromptDismissedTag`.

## UI

No new screen. Match existing `SettingsGroup` / `AlertDialog` patterns. Copy lives in `values/strings.xml` and `values-zh/strings.xml`. New keys are omitted from `values-zh-rCN`; `zh-CN` falls back to `values-zh`.

### Settings: About group

Placed below the reading-statistics group.

| State | Content |
| --- | --- |
| Default / after idle | Current version + "Check for updates" |
| Checking | Indeterminate progress |
| Up to date | "You're on the latest version" |
| Available | Remote version, truncated notes, "Download update" |
| Downloading | Determinate progress + percent |
| Ready to install | "Install update" |
| Error | Short message + "Retry" |

### Library dialog

Shown from `ImmersiveReaderApp` (same layer as session recovery) when state is `Available`, auto-prompt is allowed, and recovery is null.

- Title: new version available
- Body: version + GitHub release body truncated to 500 characters
- Buttons: Update / Later / Skip this version
- Update starts download. Progress uses a non-cancellable-by-backdrop dialog with an explicit cancel that deletes the partial file.

Do not show the update dialog while the recovery dialog is visible. Prefer recovery.

## Install

Manifest:

- `android.permission.INTERNET`
- `android.permission.REQUEST_INSTALL_PACKAGES`
- `FileProvider` authority `${applicationId}.fileprovider`, exported false, grant URI permissions
- Paths: cache `updates/`

APK path: `cacheDir/updates/update.apk` (single file, overwritten). Delete on failed download and when a check reports up to date.

Install intent: `ACTION_VIEW`, type `application/vnd.android.package-archive`, FileProvider URI, `FLAG_GRANT_READ_URI_PERMISSION`. If the user leaves the system installer, state remains `ReadyToInstall`.

Unknown-sources round trip is owned by `MainActivity` (or a small helper used from there): start settings, on resume if still `ReadyToInstall` and permission is now granted, launch the installer.

## Signing and CI

Release APKs that should overlay each other must share one upload keystore. The keystore is not committed.

### GitHub Actions secrets

- `SIGNING_KEYSTORE_BASE64`
- `SIGNING_STORE_PASSWORD`
- `SIGNING_KEY_ALIAS`
- `SIGNING_KEY_PASSWORD`

The release job fails immediately if any secret is missing. It never falls back to the debug keystore.

Local `assembleRelease` without those env vars stays **unsigned** (no debug-keystore fallback on the release build type). Only CI publishes a signed APK.

### Workflow

File: `.github/workflows/release.yml`

Trigger: push of tags matching `v*.*.*` (example: `v0.2.0`).

Steps:

1. Checkout, JDK 17, Android SDK.
2. Assert tag without the leading `v` equals `versionName` in `app/build.gradle.kts`.
3. Decode the keystore to a temp file; export the four signing env vars.
4. `./gradlew :app:assembleRelease`.
5. Rename `app/build/outputs/apk/release/app-release.apk` to `immersive-reader-{versionName}.apk`.
6. Create a GitHub Release for that tag (generate notes) and attach the APK (`softprops/action-gh-release`, `contents: write`).

Do not trigger on `release: published` (the workflow creates the release; that would loop).

Gradle reads signing config from env: `SIGNING_STORE_FILE`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`.

### Human release steps

1. Bump `versionName` and `versionCode` together in `app/build.gradle.kts`. `versionCode` must increase.
2. Commit.
3. Tag `v{versionName}` and push the tag.
4. Actions builds, signs, and attaches the APK.

Document keystore generation and secret setup in README (how to `keytool` a upload key, base64 it, add Actions secrets). Do not put sample passwords in the repo.

## Error handling

| Case | Auto-check (library) | Manual check (Settings) |
| --- | --- | --- |
| Network / HTTP failure / rate limit | Stay quiet; record check time; Settings later shows error if the user checks | `Error` + Retry |
| JSON missing APK asset | Same as network (no dialog) | `Error` that the release has no APK |
| Download failure | N/A (download is user-initiated) | Delete partial file; `Error` + Retry |
| User cancels download | Return to `Available` | Return to `Available` |
| User cancels installer | Remain `ReadyToInstall` | Remain `ReadyToInstall` |
| Signature mismatch | System installer error; app cannot recover | Same |

Coordinator work runs on `Dispatchers.IO`. Nothing hits the main thread for network or file I/O.

## Testing

JVM unit tests with fakes. No real GitHub, no APK install.

- `VersionComparator`: `v` prefix, equal, newer, older, missing patch (`1.2` vs `1.2.0`), pre-release suffix.
- `GitHubReleaseParser`: fixture with one APK, no APK, debug+release APKs (pick non-debug), malformed JSON.
- Auto-check policy: 6-hour cache, skipped tag does not auto-prompt, active recovery suppresses prompt, failed auto-check still stamps the cache time.
- Coordinator with fake client/downloader: Checking → Available → Downloading(progress) → ReadyToInstall; download failure deletes the partial file and goes to `Error`.

`org.json` is Android-only; add `testImplementation("org.json:json:…")` if the parser uses `JSONObject` on the JVM.

## Files (expected)

- `app/src/main/java/com/immersive/reader/update/` — client, parser, comparator, downloader, installer, preferences, coordinator, models
- `app/src/main/java/com/immersive/reader/ui/update/` — ViewModel + small composables if needed
- `app/src/main/res/xml/file_paths.xml`
- `app/src/test/java/com/immersive/reader/update/`
- `.github/workflows/release.yml`
- Manifest, `app/build.gradle.kts` signing, `values` + `values-zh` strings, README release section
- Settings About group; `ImmersiveReaderApp` dialog + auto-check trigger

## Open decisions (resolved)

| Topic | Decision |
| --- | --- |
| User-facing flow | In-app download + system install |
| Publishing | Tag `vX.Y.Z` → Actions attaches signed APK |
| When to check | Library silent check + Settings manual check |
| Implementation | Custom updater, no third-party updater library |
| HTTP stack | `HttpURLConnection`, no OkHttp |
| Pre-releases | Ignored via GitHub Latest Release |
| Skip vs Later | Skip persists tag; Later is in-memory dismiss for this process |
| CI signing fallback | None; missing secrets fail the job |
