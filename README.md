# Immersive Reader

Immersive Reader is a native Android EPUB reading app designed around a calm, focused reading session. The product model keeps the book, reading session, reader state, and focus capabilities separate so timer rules can evolve without coupling them to device locking.

## Current foundation

- Kotlin + Jetpack Compose
- MVVM-shaped UI with Hilt injection
- Room entities for books and reading sessions
- DataStore-backed reading preferences
- SAF EPUB import into app-private storage
- Library, session setup, and settings surfaces

The Readium Kotlin Toolkit, session coordinator, reader activity, focus tiers, and statistics are added in the following implementation phases.

## Build

```bash
./gradlew :app:assembleDebug
```

The project targets Android API 35 and supports Android 8.0 (API 26) and later.
