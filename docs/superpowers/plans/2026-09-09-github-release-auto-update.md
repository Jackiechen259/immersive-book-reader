# GitHub Release Auto-Update Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** In-app GitHub Latest Release check, APK download/install, and tag-triggered signed release publishing.

**Architecture:** Isolated `update` package owned by `UpdateCoordinator` (`StateFlow<UpdateState>`). UI observes state and never calls GitHub. CI on `v*.*.*` tags builds and attaches `immersive-reader-{versionName}.apk`.

**Tech Stack:** Kotlin, Hilt, DataStore, HttpURLConnection, org.json, Compose, GitHub Actions, FileProvider.

## Global Constraints

- No third-party updater libraries; no OkHttp; no JitPack.
- HTTP: `GET https://api.github.com/repos/Jackiechen259/immersive-book-reader/releases/latest`
- Headers: `Accept: application/vnd.github+json`, `User-Agent: ImmersiveReader/{versionName}`, `X-GitHub-Api-Version: 2022-11-28`
- Auto-check every 6 hours when library is up and recovery is null; Settings always manual.
- Skip persists tag; Later is in-memory; recovery dialog wins.
- CI never debug-signs; missing signing secrets fail the job.
- Strings in `values` and `values-zh` only.
- Tests: JVM fakes only; add `testImplementation("org.json:json:20240303")`.

## File map

Create:

- `app/src/main/java/com/immersive/reader/update/UpdateModels.kt`
- `app/src/main/java/com/immersive/reader/update/VersionComparator.kt`
- `app/src/main/java/com/immersive/reader/update/GitHubReleaseParser.kt`
- `app/src/main/java/com/immersive/reader/update/UpdatePolicy.kt`
- `app/src/main/java/com/immersive/reader/update/GitHubReleaseClient.kt`
- `app/src/main/java/com/immersive/reader/update/ApkDownloader.kt`
- `app/src/main/java/com/immersive/reader/update/ApkInstaller.kt`
- `app/src/main/java/com/immersive/reader/update/UpdatePreferences.kt`
- `app/src/main/java/com/immersive/reader/update/UpdateCoordinator.kt`
- `app/src/main/java/com/immersive/reader/di/UpdateModule.kt`
- `app/src/main/java/com/immersive/reader/ui/update/UpdateViewModel.kt`
- `app/src/main/res/xml/file_paths.xml`
- `app/src/test/java/com/immersive/reader/update/*.kt`
- `.github/workflows/release.yml`

Modify: Manifest, `app/build.gradle.kts`, Settings, ImmersiveReaderApp, MainActivity, strings (en/zh), README.

## Tasks

1. VersionComparator (TDD)
2. GitHubReleaseParser (TDD)
3. UpdatePolicy (TDD)
4. UpdateCoordinator with fakes (TDD)
5. Android client/downloader/installer/preferences/Hilt
6. UI + install round-trip
7. Gradle signing + Actions + README
8. Verify `testDebugUnitTest` and `assembleDebug`
