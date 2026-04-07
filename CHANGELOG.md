# CHANGELOG

## 0.1.3 - 2026-04-07

- Integrated Cycle Tracking and Sexual Activity into core domain models (`CycleEntry`, `ActivityEntry`).
- Updated `DailyHealthSnapshot` with metadata for cycle state and sexual activity.
- Added 7 new Health Connect permission declarations for fertility and sexual health tracking.
- Implemented a "Test Health Connect" dashboard in `MainActivity` with live permission status indicators (Green/Red).
- Expanded tracked wellness metrics to include Nutrition and Heart Rate.

## 0.1.2 - 2026-04-07

- Fixed build-time JVM target mismatch by synchronizing Java and Kotlin targets to JVM 17 across all modules.
- Resolved theme resource linking error by adding the Material components dependency.
- Configured Google Health Connect integration in the manifest, including permission declarations and rationale activity registration.
- Added package queries for Health Connect visibility on Android 11+.
- Updated `healthconnect` feature module with the required Health Connect Client dependency.

## 0.1.1 - 2026-04-07

- Added Gradle wrapper (`gradlew`, `gradlew.bat`, wrapper JAR/properties) so the project can be built consistently.
- Fixed Kotlin 2.0 Compose plugin configuration by applying `org.jetbrains.kotlin.plugin.compose`.
- Expanded local API server to include `/v1/snapshot` and `/v1/summary`, plus query parameter support for date ranges and limits.
- Added API response envelopes with `schema_version`, `lastSyncedAt`, and `dataAvailability` fields.
- Improved in-memory service with seeded sample data and non-null snapshot/summary responses.

## 0.1.0 - 2026-04-07

- Initial scaffold aligned to Open Health Bridge Android spec v2.0.
- Added modular architecture, domain model layer, database schema, sync constraints, loopback API server, and docs.
