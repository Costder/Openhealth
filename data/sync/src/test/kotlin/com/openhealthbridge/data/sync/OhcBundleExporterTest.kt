package com.openhealthbridge.data.sync

import com.openhealthbridge.core.models.ActivityEntry
import com.openhealthbridge.core.models.CycleEntry
import com.openhealthbridge.core.models.DataSource
import com.openhealthbridge.core.models.DateRange
import com.openhealthbridge.core.models.DailyHealthSnapshot
import com.openhealthbridge.core.models.EntrySourceType
import com.openhealthbridge.core.models.ExerciseSetEntry
import com.openhealthbridge.core.models.HealthBridgeService
import com.openhealthbridge.core.models.NutritionEntry
import com.openhealthbridge.core.models.PRRecord
import com.openhealthbridge.core.models.PrType
import com.openhealthbridge.core.models.RecoveryEntry
import com.openhealthbridge.core.models.TrendSummary
import com.openhealthbridge.core.models.WorkoutEntry
import com.openhealthbridge.data.db.HealthBridgeDao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

class OhcBundleExporterTest {
    @Test
    fun exporterBuildsStableSnapshotBundle() {
        val metadataStore = SyncMetadataStore(FakeDao())
        val exporter = OhcBundleExporter(
            healthBridgeService = FakeHealthBridgeService(),
            metadataStore = metadataStore,
            crypto = FakeCrypto(),
            clock = Clock.fixed(Instant.parse("2026-04-07T15:00:00Z"), ZoneOffset.UTC)
        )

        val bundle = kotlinx.coroutines.runBlocking {
            exporter.exportSnapshotBundle("ZmFrZS1rZXktZmFrZS1rZXktZmFrZS1rZXktZmFrZS0=")
        }

        assertTrue(bundle.bundleId.startsWith("android-install-a"))
        assertTrue(bundle.manifestJson.contains("\"bundleType\": \"snapshot\""))
        assertTrue(bundle.payloadJson.contains("\"id\":\"activity-daily:2026-04-07\""))
        assertTrue(bundle.payloadJson.contains("\"id\":\"workout:w-1\""))
        assertTrue(bundle.payloadJson.contains("\"category\":\"nutrition\""))
        assertTrue(bundle.payloadJson.contains("\"category\":\"sleep\""))
        assertEquals(1, bundle.sequence)
        assertTrue(bundle.manifestJson.contains("\"transportMode\": \"syncthing\""))
    }

    @Test
    fun exporterUsesConfiguredNextcloudTransportMode() {
        val dao = FakeDao(
            mutableMapOf(
                "sync.install_id" to "install-abcd",
                "sync.transport_mode" to TransportMode.NEXTCLOUD.name
            )
        )
        val exporter = OhcBundleExporter(
            healthBridgeService = FakeHealthBridgeService(),
            metadataStore = SyncMetadataStore(dao),
            crypto = FakeCrypto(),
            clock = Clock.fixed(Instant.parse("2026-04-07T15:00:00Z"), ZoneOffset.UTC)
        )

        val bundle = kotlinx.coroutines.runBlocking {
            exporter.exportSnapshotBundle("ZmFrZS1rZXktZmFrZS1rZXktZmFrZS1rZXktZmFrZS0=")
        }

        assertTrue(bundle.manifestJson.contains("\"transportMode\": \"nextcloud\""))
    }

    private class FakeCrypto : BundleCryptoEngine {
        override fun randomNonce(size: Int): ByteArray = ByteArray(size) { 7 }
        override fun encrypt(plaintext: ByteArray, keyB64: String, nonce: ByteArray): ByteArray = plaintext
        override fun decrypt(ciphertext: ByteArray, keyB64: String, nonce: ByteArray): ByteArray = ciphertext
        override fun sha256Hex(input: ByteArray): String = "a".repeat(64)
        override fun encodeBase64(input: ByteArray): String = "bm9uY2U="
        override fun decodeBase64(input: String): ByteArray = ByteArray(32) { 1 }
    }

    private class FakeDao(
        private val settings: MutableMap<String, String> = mutableMapOf("sync.install_id" to "install-abcd")
    ) : HealthBridgeDao {
        override suspend fun getSettingValue(key: String): String? = settings[key]
        override suspend fun upsertSetting(setting: com.openhealthbridge.data.db.AppSettingsEntity) { settings[setting.key] = setting.value }
        override suspend fun upsertSettings(settings: List<com.openhealthbridge.data.db.AppSettingsEntity>) { settings.forEach { this.settings[it.key] = it.value } }

        override suspend fun getDailySnapshot(date: String) = null
        override suspend fun getAllDailySnapshots() = emptyList<com.openhealthbridge.data.db.DailySnapshotEntity>()
        override suspend fun upsertDailySnapshots(entries: List<com.openhealthbridge.data.db.DailySnapshotEntity>) {}
        override suspend fun clearDailySnapshots() {}
        override suspend fun getRecentWorkouts(limit: Int) = emptyList<com.openhealthbridge.data.db.WorkoutEntryEntity>()
        override suspend fun getAllWorkouts() = emptyList<com.openhealthbridge.data.db.WorkoutEntryEntity>()
        override suspend fun upsertWorkouts(entries: List<com.openhealthbridge.data.db.WorkoutEntryEntity>) {}
        override suspend fun clearWorkouts() {}
        override suspend fun getExerciseSetsForWorkout(workoutId: String) = emptyList<com.openhealthbridge.data.db.ExerciseSetEntryEntity>()
        override suspend fun getAllExerciseSets() = emptyList<com.openhealthbridge.data.db.ExerciseSetEntryEntity>()
        override suspend fun upsertExerciseSets(entries: List<com.openhealthbridge.data.db.ExerciseSetEntryEntity>) {}
        override suspend fun deleteExerciseSetsForWorkouts(workoutIds: List<String>) {}
        override suspend fun clearExerciseSets() {}
        override suspend fun getRecentNutritionEntries(limit: Int) = emptyList<com.openhealthbridge.data.db.NutritionEntryEntity>()
        override suspend fun getAllNutritionEntries() = emptyList<com.openhealthbridge.data.db.NutritionEntryEntity>()
        override suspend fun upsertNutritionEntries(entries: List<com.openhealthbridge.data.db.NutritionEntryEntity>) {}
        override suspend fun clearNutritionEntries() {}
        override suspend fun getRecentRecoveryEntries(limit: Int) = emptyList<com.openhealthbridge.data.db.RecoveryEntryEntity>()
        override suspend fun getAllRecoveryEntries() = emptyList<com.openhealthbridge.data.db.RecoveryEntryEntity>()
        override suspend fun upsertRecoveryEntries(entries: List<com.openhealthbridge.data.db.RecoveryEntryEntity>) {}
        override suspend fun clearRecoveryEntries() {}
        override suspend fun getRecentCycleEntries(limit: Int) = emptyList<com.openhealthbridge.data.db.CycleEntryEntity>()
        override suspend fun getAllCycleEntries() = emptyList<com.openhealthbridge.data.db.CycleEntryEntity>()
        override suspend fun upsertCycleEntries(entries: List<com.openhealthbridge.data.db.CycleEntryEntity>) {}
        override suspend fun clearCycleEntries() {}
        override suspend fun getRecentActivityEntries(limit: Int) = emptyList<com.openhealthbridge.data.db.ActivityEntryEntity>()
        override suspend fun getAllActivityEntries() = emptyList<com.openhealthbridge.data.db.ActivityEntryEntity>()
        override suspend fun upsertActivityEntries(entries: List<com.openhealthbridge.data.db.ActivityEntryEntity>) {}
        override suspend fun clearActivityEntries() {}
        override suspend fun getRecentPrRecords(limit: Int) = emptyList<com.openhealthbridge.data.db.PrRecordEntity>()
        override suspend fun getAllPrRecords() = emptyList<com.openhealthbridge.data.db.PrRecordEntity>()
        override suspend fun upsertPrRecords(entries: List<com.openhealthbridge.data.db.PrRecordEntity>) {}
        override suspend fun upsertBodyMetrics(entries: List<com.openhealthbridge.data.db.BodyMetricsEntryEntity>) {}
        override suspend fun getAllBodyMetrics() = emptyList<com.openhealthbridge.data.db.BodyMetricsEntryEntity>()
        override suspend fun clearBodyMetrics() {}
        override suspend fun insertSyncEvent(event: com.openhealthbridge.data.db.SyncEventEntity) {}
        override suspend fun getRecentSyncEvents(limit: Int) = emptyList<com.openhealthbridge.data.db.SyncEventEntity>()
    }

    private class FakeHealthBridgeService : HealthBridgeService {
        private val now = OffsetDateTime.parse("2026-04-07T18:00:00Z")

        override suspend fun getDailySnapshot(date: LocalDate): DailyHealthSnapshot? = null
        override suspend fun getWeeklySummary(start: LocalDate, end: LocalDate): TrendSummary = TrendSummary(start, end, null, null, null, null, null, null, 0, 0)
        override suspend fun getAllSnapshots(): List<DailyHealthSnapshot> = listOf(
            DailyHealthSnapshot(
                date = LocalDate.parse("2026-04-07"),
                weightKg = 80.5,
                heightM = 1.78,
                stepCount = 8450,
                sleepHours = 7.25,
                caloriesConsumed = 650.0,
                activeCaloriesBurned = 420.0,
                totalCaloriesBurned = 2500.0,
                workoutCount = 1,
                proteinGrams = 45.0,
                carbsGrams = 60.0,
                fatGrams = 20.0,
                recoveryScore = 81,
                flags = emptyList(),
                lastSyncedAt = Instant.parse("2026-04-07T15:00:00Z"),
                dataAvailability = mapOf("stepCount" to DataSource.MEASURED)
            )
        )
        override suspend fun getAllWorkouts(): List<WorkoutEntry> = listOf(
            WorkoutEntry(
                id = "w-1",
                startTime = now,
                endTime = now.plusHours(1),
                sourceType = EntrySourceType.MANUAL,
                workoutType = "Strength",
                completed = true,
                notes = null,
                perceivedDifficulty = 7,
                painFlag = false,
                exerciseSets = listOf(
                    ExerciseSetEntry("s-1", "w-1", "Squat", 1, 5, 100.0, null, null, false, null)
                )
            )
        )
        override suspend fun getAllRecoveryEntries(): List<RecoveryEntry> = listOf(
            RecoveryEntry("r-1", now.minusHours(11), 7.25, 4, 2, 3, 1, 2, null)
        )
        override suspend fun getAllNutritionEntries(): List<NutritionEntry> = listOf(
            NutritionEntry("n-1", now.minusHours(6), "Lunch", EntrySourceType.MANUAL, 650.0, 45.0, 60.0, 20.0, null)
        )
        override suspend fun getAllCycleEntries(): List<CycleEntry> = emptyList()
        override suspend fun getAllActivityEntries(): List<ActivityEntry> = emptyList()
        override suspend fun getAllPrRecords(): List<PRRecord> = listOf(
            PRRecord("pr-1", "Squat", PrType.MAX_WEIGHT, 100.0, "kg", null, now, "w-1", "Squat-MAX_WEIGHT-2026-04-07")
        )
        override suspend fun getRecentWorkouts(limit: Int): List<WorkoutEntry> = getAllWorkouts()
        override suspend fun getRecoveryEntries(limit: Int): List<RecoveryEntry> = getAllRecoveryEntries()
        override suspend fun getNutritionEntries(limit: Int): List<NutritionEntry> = getAllNutritionEntries()
        override suspend fun getCycleEntries(limit: Int): List<CycleEntry> = emptyList()
        override suspend fun getActivityEntries(limit: Int): List<ActivityEntry> = emptyList()
        override suspend fun getPrRecords(limit: Int): List<PRRecord> = getAllPrRecords()
        override suspend fun logWorkoutEntry(entry: WorkoutEntry) {}
        override suspend fun logNutritionEntry(entry: NutritionEntry) {}
        override suspend fun logRecoveryEntry(entry: RecoveryEntry) {}
        override suspend fun exportJson(range: DateRange): File = File.createTempFile("fake", ".json")
    }
}
