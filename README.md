# Open Health Bridge (Android)

Open Health Bridge is a local-first Android health data bridge for Health Connect + manual logs.

## Current status (Developer Preview)

This repository now contains a runnable **developer-preview foundation** aligned with the April 2026 v2.0 spec:

- Multi-module Android structure (`app`, `core/*`, `data/*`, `feature/*`, `integration/*`)
- Core domain model + `HealthBridgeService` boundary
- **Nutrition, Wellness (Heart Rate, Sleep, Recovery) & Cycle Tracking (Fertility, Menstruation, Sexual Activity)**
- **Health Connect Integration** with permission rationale and data visibility (Android 11+ support)
- **Live Test Dashboard** in-app for verifying Health Connect data access
- Room table definitions for MVP entities
- WorkManager sync constraints for charger + idle behavior
- Local loopback API server (`127.0.0.1`) with `/v1/health`, `/v1/snapshot`, `/v1/summary`, `/v1/workouts`, `/v1/nutrition`, `/v1/recovery`, `/v1/prs`
- Schema and setup docs

## Build

```bash
./gradlew :app:assembleDebug
```

> Requirements: Android SDK installed locally (`ANDROID_HOME` or `local.properties` with `sdk.dir`).


## Repo note for PR tooling

Some PR tools reject binary files. This repo therefore excludes `gradle/wrapper/gradle-wrapper.jar`.
If missing after clone, regenerate with:

```bash
gradle wrapper --gradle-version 8.14.3
```

## Privacy defaults

- No cloud sync
- No analytics SDKs

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](./LICENSE) for details.
- No `INTERNET` permission in manifest
- Local HTTP server binds to `127.0.0.1` only
