# CHANGELOG

## 0.1.1 - 2026-04-07

- Added Gradle wrapper (`gradlew`, `gradlew.bat`, wrapper JAR/properties) so the project can be built consistently.
- Fixed Kotlin 2.0 Compose plugin configuration by applying `org.jetbrains.kotlin.plugin.compose`.
- Expanded local API server to include `/v1/snapshot` and `/v1/summary`, plus query parameter support for date ranges and limits.
- Added API response envelopes with `schema_version`, `lastSyncedAt`, and `dataAvailability` fields.
- Improved in-memory service with seeded sample data and non-null snapshot/summary responses.

## 0.1.0 - 2026-04-07

- Initial scaffold aligned to Open Health Bridge Android spec v2.0.
- Added modular architecture, domain model layer, database schema, sync constraints, loopback API server, and docs.
