package com.openhealthbridge.data.sync

import com.openhealthbridge.core.models.ActivityEntry
import com.openhealthbridge.core.models.CycleEntry
import com.openhealthbridge.core.models.HealthBridgeService
import com.openhealthbridge.core.models.NutritionEntry
import com.openhealthbridge.core.models.PRRecord
import com.openhealthbridge.core.models.RecoveryEntry
import com.openhealthbridge.core.models.WorkoutEntry
import org.json.JSONArray
import org.json.JSONObject
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class OhcBundleExporter(
    private val healthBridgeService: HealthBridgeService,
    private val metadataStore: SyncMetadataStore,
    private val crypto: BundleCryptoEngine,
    private val clock: Clock = Clock.systemUTC()
) {
    suspend fun exportSnapshotBundle(keyB64: String): PendingBundle {
        val sequence = metadataStore.getNextSequence()
        val prevBundleId = metadataStore.getPrevBundleId()
        val transportMode = metadataStore.getTransportMode()
        val installId = metadataStore.getOrCreateInstallId().take(8)
        val createdAt = Instant.now(clock)
        val createdAtIso = createdAt.toString()
        val bundleId = "android-$installId-${sequence.toString().padStart(6, '0')}-${BUNDLE_TIMESTAMP_FORMAT.format(createdAt)}"

        val events = buildEventsJson()
        val payloadJson = JSONObject().put("events", events).toString()
        val nonce = crypto.randomNonce()
        val nonceB64 = crypto.encodeBase64(nonce)
        val ciphertext = crypto.encrypt(payloadJson.toByteArray(Charsets.UTF_8), keyB64, nonce)
        val ciphertextSha256 = crypto.sha256Hex(ciphertext)

        val manifestJson = JSONObject()
            .put("formatVersion", 1)
            .put("bundleType", "snapshot")
            .put("bundleId", bundleId)
            .put("sequence", sequence)
            .put("prevBundleId", prevBundleId)
            .put("createdAt", createdAtIso)
            .put("transportMode", transportMode.manifestValue())
            .put("integrity", JSONObject().put("algorithm", "sha256").put("ciphertextSha256", ciphertextSha256))
            .put("encryption", JSONObject().put("algorithm", "xchacha20poly1305").put("nonceB64", nonceB64))
            .toString(2)

        return PendingBundle(
            bundleId = bundleId,
            sequence = sequence,
            prevBundleId = prevBundleId,
            createdAt = createdAtIso,
            nonceB64 = nonceB64,
            ciphertextSha256 = ciphertextSha256,
            payloadCiphertext = ciphertext,
            payloadJson = payloadJson,
            manifestJson = manifestJson
        )
    }

    suspend fun markBundleWritten(bundle: PendingBundle) {
        metadataStore.markBundleWritten(bundle.bundleId, bundle.sequence)
    }

    private suspend fun buildEventsJson(): JSONArray {
        val events = mutableListOf<JSONObject>()

        healthBridgeService.getAllSnapshots().sortedBy { it.date }.forEach { snapshot ->
            events += JSONObject()
                .put("id", "activity-daily:${snapshot.date}")
                .put("category", "activity")
                .put("ts", snapshot.date.atTime(23, 59, 59).toInstant(ZoneOffset.UTC).toString())
                .put(
                    "payload",
                    JSONObject()
                        .put("snapshotDate", snapshot.date.toString())
                        .put("stepCount", snapshot.stepCount)
                        .put("activeCaloriesBurned", snapshot.activeCaloriesBurned)
                        .put("totalCaloriesBurned", snapshot.totalCaloriesBurned)
                        .put("sourceType", "HEALTH_CONNECT")
                )

            if (snapshot.weightKg != null || snapshot.heightM != null) {
                events += JSONObject()
                    .put("id", "body:${snapshot.date}")
                    .put("category", "activity")
                    .put("ts", snapshot.date.atStartOfDay().plusHours(8).toInstant(ZoneOffset.UTC).toString())
                    .put(
                        "payload",
                        JSONObject()
                            .put("recordedAt", snapshot.date.atStartOfDay().plusHours(8).toInstant(ZoneOffset.UTC).toString())
                            .put("weightKg", snapshot.weightKg)
                            .put("heightM", snapshot.heightM)
                            .put("sourceType", "HEALTH_CONNECT")
                    )
            }
        }

        healthBridgeService.getAllWorkouts().forEach { workout -> events += workout.toOhcEvent() }
        healthBridgeService.getAllPrRecords().forEach { pr -> events += pr.toOhcEvent() }
        healthBridgeService.getAllNutritionEntries().forEach { nutrition -> events += nutrition.toOhcEvent() }
        healthBridgeService.getAllRecoveryEntries().forEach { recovery -> events += recovery.toOhcEvent() }
        healthBridgeService.getAllCycleEntries().forEach { cycle -> events += cycle.toOhcEvent() }
        healthBridgeService.getAllActivityEntries().forEach { activity -> events += activity.toOhcEvent() }

        return JSONArray(events.sortedBy { it.getString("ts") })
    }

    private fun WorkoutEntry.toOhcEvent(): JSONObject = JSONObject()
        .put("id", "workout:$id")
        .put("category", "activity")
        .put("ts", startTime.toInstant().toString())
        .put(
            "payload",
            JSONObject()
                .put("startTime", startTime.toInstant().toString())
                .put("endTime", endTime.toInstant().toString())
                .put("workoutType", workoutType)
                .put("completed", completed)
                .put("notes", notes)
                .put("perceivedDifficulty", perceivedDifficulty)
                .put("painFlag", painFlag)
                .put("exerciseSets", JSONArray(exerciseSets.sortedBy { it.setIndex }.map { set ->
                    JSONObject()
                        .put("id", set.id)
                        .put("exerciseName", set.exerciseName)
                        .put("setIndex", set.setIndex)
                        .put("reps", set.reps)
                        .put("weightKg", set.weightKg)
                        .put("durationSeconds", set.durationSeconds)
                        .put("distanceMeters", set.distanceMeters)
                        .put("isBodyweight", set.isBodyweight)
                        .put("notes", set.notes)
                }))
                .put("sourceType", sourceType.name)
        )

    private fun PRRecord.toOhcEvent(): JSONObject = JSONObject()
        .put("id", "pr:$id")
        .put("category", "activity")
        .put("ts", achievedAt.toInstant().toString())
        .put(
            "payload",
            JSONObject()
                .put("exerciseName", exerciseName)
                .put("prType", prType.name)
                .put("value", value)
                .put("unit", unit)
                .put("previousValue", previousValue)
                .put("achievedAt", achievedAt.toInstant().toString())
                .put("sourceWorkoutId", sourceWorkoutId)
                .put("deduplicationKey", deduplicationKey)
        )

    private fun NutritionEntry.toOhcEvent(): JSONObject = JSONObject()
        .put("id", "nutrition:$id")
        .put("category", "nutrition")
        .put("ts", timestamp.toInstant().toString())
        .put(
            "payload",
            JSONObject()
                .put("timestamp", timestamp.toInstant().toString())
                .put("mealName", mealName)
                .put("calories", calories)
                .put("proteinGrams", proteinGrams)
                .put("carbsGrams", carbsGrams)
                .put("fatGrams", fatGrams)
                .put("notes", notes)
                .put("sourceType", sourceType.name)
        )

    private fun RecoveryEntry.toOhcEvent(): JSONObject = JSONObject()
        .put("id", "recovery:$id")
        .put("category", "sleep")
        .put("ts", timestamp.toInstant().toString())
        .put(
            "payload",
            JSONObject()
                .put("timestamp", timestamp.toInstant().toString())
                .put("sleepHours", sleepHours)
                .put("sleepQuality", sleepQuality)
                .put("sorenessLevel", sorenessLevel)
                .put("fatigueLevel", fatigueLevel)
                .put("painLevel", painLevel)
                .put("stressLevel", stressLevel)
                .put("notes", notes)
        )

    private fun CycleEntry.toOhcEvent(): JSONObject = JSONObject()
        .put("id", "cycle:$id")
        .put("category", "reproductive_health")
        .put("ts", timestamp.toInstant().toString())
        .put(
            "payload",
            JSONObject()
                .put("timestamp", timestamp.toInstant().toString())
                .put("flow", flow)
                .put("isStartOfCycle", isStartOfCycle)
                .put("bbtCelsius", bbtCelsius)
                .put("ovulationTestResult", ovulationTestResult)
                .put("cervicalMucusAppearance", cervicalMucusAppearance)
                .put("intermenstrualBleeding", intermenstrualBleeding)
                .put("notes", notes)
                .put("sourceType", sourceType.name)
        )

    private fun ActivityEntry.toOhcEvent(): JSONObject = JSONObject()
        .put("id", "sexual:$id")
        .put("category", "sexual_health")
        .put("ts", timestamp.toInstant().toString())
        .put(
            "payload",
            JSONObject()
                .put("timestamp", timestamp.toInstant().toString())
                .put("wasProtected", wasProtected)
                .put("notes", notes)
                .put("sourceType", sourceType.name)
        )

    companion object {
        private val BUNDLE_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
            .withZone(ZoneOffset.UTC)
    }
}
