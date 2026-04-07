package com.openhealthbridge.integration.api

import com.openhealthbridge.core.models.DailyHealthSnapshot
import com.openhealthbridge.core.models.HealthBridgeService
import com.openhealthbridge.core.models.NutritionEntry
import com.openhealthbridge.core.models.PRRecord
import com.openhealthbridge.core.models.RecoveryEntry
import com.openhealthbridge.core.models.TrendSummary
import com.openhealthbridge.core.models.WorkoutEntry
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate

class LocalApiServer(
    private val healthBridgeService: HealthBridgeService,
    private val port: Int = 18432
) {
    private val server = embeddedServer(CIO, host = "127.0.0.1", port = port) {
        install(ContentNegotiation) { json() }
        routing {
            get("/v1/health") {
                call.respond(HealthStatusResponse("1.0", "ok", Instant.now().toString()))
            }

            get("/v1/snapshot") {
                val date = call.request.queryParameters["date"]?.let(LocalDate::parse) ?: LocalDate.now()
                val snapshot = healthBridgeService.getDailySnapshot(date)
                if (snapshot == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("schema_version" to "1.0", "error" to "snapshot_not_found"))
                } else {
                    call.respond(SnapshotResponse.from(snapshot))
                }
            }

            get("/v1/summary") {
                val end = call.request.queryParameters["end"]?.let(LocalDate::parse) ?: LocalDate.now()
                val start = call.request.queryParameters["start"]?.let(LocalDate::parse) ?: end.minusDays(6)
                call.respond(SummaryResponse.from(healthBridgeService.getWeeklySummary(start, end)))
            }

            get("/v1/workouts") {
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                call.respond(ListResponse("1.0", Instant.now().toString(), healthBridgeService.getRecentWorkouts(limit).map { it.toApi() }))
            }
            get("/v1/nutrition") {
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                call.respond(ListResponse("1.0", Instant.now().toString(), healthBridgeService.getNutritionEntries(limit).map { it.toApi() }))
            }
            get("/v1/recovery") {
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                call.respond(ListResponse("1.0", Instant.now().toString(), healthBridgeService.getRecoveryEntries(limit).map { it.toApi() }))
            }
            get("/v1/prs") {
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                call.respond(ListResponse("1.0", Instant.now().toString(), healthBridgeService.getPrRecords(limit).map { it.toApi() }))
            }
        }
    }

    fun start() = server.start(wait = false)
    fun stop() = server.stop()
}

@Serializable
data class HealthStatusResponse(val schema_version: String, val status: String, val lastSyncedAt: String)

@Serializable
data class SnapshotResponse(
    val schema_version: String,
    val lastSyncedAt: String,
    val dataAvailability: Map<String, String>,
    val snapshot: DailyHealthSnapshotApi
) {
    companion object {
        fun from(snapshot: DailyHealthSnapshot): SnapshotResponse = SnapshotResponse(
            schema_version = "1.0",
            lastSyncedAt = snapshot.lastSyncedAt.toString(),
            dataAvailability = snapshot.dataAvailability.mapValues { it.value.name },
            snapshot = DailyHealthSnapshotApi(
                date = snapshot.date.toString(),
                weightKg = snapshot.weightKg,
                heightM = snapshot.heightM,
                bmi = snapshot.bmi,
                stepCount = snapshot.stepCount,
                sleepHours = snapshot.sleepHours,
                caloriesConsumed = snapshot.caloriesConsumed,
                activeCaloriesBurned = snapshot.activeCaloriesBurned,
                totalCaloriesBurned = snapshot.totalCaloriesBurned,
                workoutCount = snapshot.workoutCount,
                proteinGrams = snapshot.proteinGrams,
                carbsGrams = snapshot.carbsGrams,
                fatGrams = snapshot.fatGrams,
                recoveryScore = snapshot.recoveryScore,
                flags = snapshot.flags.map { it.name }
            )
        )
    }
}

@Serializable
data class SummaryResponse(
    val schema_version: String,
    val lastSyncedAt: String,
    val dataAvailability: Map<String, String>,
    val summary: TrendSummaryApi
) {
    companion object {
        fun from(summary: TrendSummary): SummaryResponse = SummaryResponse(
            schema_version = "1.0",
            lastSyncedAt = Instant.now().toString(),
            dataAvailability = mapOf(
                "avgWeight" to if (summary.avgWeight == null) "UNAVAILABLE" else "MEASURED",
                "avgSleep" to if (summary.avgSleep == null) "UNAVAILABLE" else "MEASURED",
                "avgSteps" to if (summary.avgSteps == null) "UNAVAILABLE" else "MEASURED"
            ),
            summary = TrendSummaryApi(
                rangeStart = summary.rangeStart.toString(),
                rangeEnd = summary.rangeEnd.toString(),
                avgWeight = summary.avgWeight,
                avgSleep = summary.avgSleep,
                avgSteps = summary.avgSteps,
                avgCaloriesConsumed = summary.avgCaloriesConsumed,
                avgCaloriesBurned = summary.avgCaloriesBurned,
                workoutAdherenceRate = summary.workoutAdherenceRate,
                newPrCount = summary.newPrCount,
                recoveryRiskCount = summary.recoveryRiskCount
            )
        )
    }
}

@Serializable
data class ListResponse<T>(val schema_version: String, val lastSyncedAt: String, val items: List<T>)

@Serializable
data class DailyHealthSnapshotApi(
    val date: String,
    val weightKg: Double?,
    val heightM: Double?,
    val bmi: Double?,
    val stepCount: Long?,
    val sleepHours: Double?,
    val caloriesConsumed: Double?,
    val activeCaloriesBurned: Double?,
    val totalCaloriesBurned: Double?,
    val workoutCount: Int,
    val proteinGrams: Double?,
    val carbsGrams: Double?,
    val fatGrams: Double?,
    val recoveryScore: Int?,
    val flags: List<String>
)

@Serializable
data class TrendSummaryApi(
    val rangeStart: String,
    val rangeEnd: String,
    val avgWeight: Double?,
    val avgSleep: Double?,
    val avgSteps: Double?,
    val avgCaloriesConsumed: Double?,
    val avgCaloriesBurned: Double?,
    val workoutAdherenceRate: Double?,
    val newPrCount: Int,
    val recoveryRiskCount: Int
)

@Serializable
data class WorkoutApi(val id: String, val startTime: String, val endTime: String, val workoutType: String, val completed: Boolean)
@Serializable
data class NutritionApi(val id: String, val timestamp: String, val mealName: String, val calories: Double)
@Serializable
data class RecoveryApi(val id: String, val timestamp: String, val sleepHours: Double?, val fatigueLevel: Int, val stressLevel: Int)
@Serializable
data class PrApi(val id: String, val exerciseName: String, val prType: String, val value: Double, val unit: String, val achievedAt: String)

private fun WorkoutEntry.toApi() = WorkoutApi(id, startTime.toString(), endTime.toString(), workoutType, completed)
private fun NutritionEntry.toApi() = NutritionApi(id, timestamp.toString(), mealName, calories)
private fun RecoveryEntry.toApi() = RecoveryApi(id, timestamp.toString(), sleepHours, fatigueLevel, stressLevel)
private fun PRRecord.toApi() = PrApi(id, exerciseName, prType.name, value, unit, achievedAt.toString())
