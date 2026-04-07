package com.openhealthbridge.feature.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalBodyTemperatureRecord
import androidx.health.connect.client.records.CervicalMucusRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.IntermenstrualBleedingRecord
import androidx.health.connect.client.records.MenstruationFlowRecord
import androidx.health.connect.client.records.MenstruationPeriodRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.OvulationTestRecord
import androidx.health.connect.client.records.SexualActivityRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.openhealthbridge.core.models.DataSource
import com.openhealthbridge.data.db.ActivityEntryEntity
import com.openhealthbridge.data.db.BodyMetricsEntryEntity
import com.openhealthbridge.data.db.CycleEntryEntity
import com.openhealthbridge.data.db.DailySnapshotEntity
import com.openhealthbridge.data.db.ExerciseSetEntryEntity
import com.openhealthbridge.data.db.HealthBridgeDao
import com.openhealthbridge.data.db.NutritionEntryEntity
import com.openhealthbridge.data.db.RecoveryEntryEntity
import com.openhealthbridge.data.db.WorkoutEntryEntity
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

data class ImportSummary(
    val snapshots: Int,
    val workouts: Int,
    val nutritionEntries: Int,
    val recoveryEntries: Int,
    val cycleEntries: Int,
    val activityEntries: Int
)

class HealthConnectImporter(
    private val context: Context,
    private val dao: HealthBridgeDao
) {
    fun requiredPermissions(): Set<String> = buildRequiredPermissions()

    suspend fun importRecentData(daysBack: Long = DEFAULT_LOOKBACK_DAYS): ImportSummary {
        val client = HealthConnectClient.getOrCreate(context)
        val end = Instant.now()
        val start = end.minus(Duration.ofDays(daysBack))
        val timeRange = TimeRangeFilter.between(start, end)
        val importedAt = Instant.now().toString()

        val steps = client.readRecords(ReadRecordsRequest(StepsRecord::class, timeRange)).records
        val weights = client.readRecords(ReadRecordsRequest(WeightRecord::class, timeRange)).records
        val heights = client.readRecords(ReadRecordsRequest(HeightRecord::class, timeRange)).records
        val activeCalories = client.readRecords(ReadRecordsRequest(ActiveCaloriesBurnedRecord::class, timeRange)).records
        val totalCalories = client.readRecords(ReadRecordsRequest(TotalCaloriesBurnedRecord::class, timeRange)).records
        val sleepSessions = client.readRecords(ReadRecordsRequest(SleepSessionRecord::class, timeRange)).records
        val nutritionRecords = client.readRecords(ReadRecordsRequest(NutritionRecord::class, timeRange)).records
        val workouts = client.readRecords(ReadRecordsRequest(ExerciseSessionRecord::class, timeRange)).records

        val snapshots = linkedMapOf<LocalDate, SnapshotAccumulator>()
        fun day(date: LocalDate): SnapshotAccumulator = snapshots.getOrPut(date) { SnapshotAccumulator(date) }

        steps.forEach { record ->
            day(dateFor(record.endTime)).stepCount += record.count
        }
        activeCalories.forEach { record ->
            day(dateFor(record.endTime)).activeCaloriesBurned += record.energy.inKilocalories
        }
        totalCalories.forEach { record ->
            day(dateFor(record.endTime)).totalCaloriesBurned += record.energy.inKilocalories
        }
        sleepSessions.forEach { record ->
            day(dateFor(record.endTime)).sleepHours += Duration.between(record.startTime, record.endTime).toMinutes() / 60.0
        }
        workouts.forEach { record ->
            day(dateFor(record.endTime)).workoutCount += 1
        }
        nutritionRecords.forEach { record ->
            val snapshot = day(dateFor(record.endTime))
            snapshot.caloriesConsumed += record.energy?.inKilocalories ?: 0.0
            snapshot.proteinGrams += record.protein?.inGrams ?: 0.0
            snapshot.carbsGrams += record.totalCarbohydrate?.inGrams ?: 0.0
            snapshot.fatGrams += record.totalFat?.inGrams ?: 0.0
        }
        weights.groupBy { dateFor(it.time) }.forEach { (date, records) ->
            day(date).weightKg = records.maxByOrNull { it.time }?.weight?.inKilograms
        }
        heights.groupBy { dateFor(it.time) }.forEach { (date, records) ->
            day(date).heightM = records.maxByOrNull { it.time }?.height?.inMeters
        }

        val workoutEntities = workouts.map { record ->
            WorkoutEntryEntity(
                id = record.metadata.id ?: syntheticId("workout", record.startTime),
                startTime = OffsetDateTime.ofInstant(record.startTime, ZoneOffset.UTC).toString(),
                endTime = OffsetDateTime.ofInstant(record.endTime, ZoneOffset.UTC).toString(),
                sourceType = "HEALTH_CONNECT",
                workoutType = record.exerciseType.toString(),
                completed = true,
                notes = null,
                perceivedDifficulty = null,
                painFlag = false
            )
        }

        val nutritionEntities = nutritionRecords.map { record ->
            NutritionEntryEntity(
                id = record.metadata.id ?: syntheticId("nutrition", record.startTime),
                timestamp = OffsetDateTime.ofInstant(record.startTime, ZoneOffset.UTC).toString(),
                mealName = "Meal ${record.mealType}",
                sourceType = "HEALTH_CONNECT",
                calories = record.energy?.inKilocalories ?: 0.0,
                proteinGrams = record.protein?.inGrams ?: 0.0,
                carbsGrams = record.totalCarbohydrate?.inGrams ?: 0.0,
                fatGrams = record.totalFat?.inGrams ?: 0.0,
                notes = null
            )
        }

        val recoveryEntities = sleepSessions.map { record ->
            RecoveryEntryEntity(
                id = record.metadata.id ?: syntheticId("sleep", record.endTime),
                timestamp = OffsetDateTime.ofInstant(record.endTime, ZoneOffset.UTC).toString(),
                sleepHours = Duration.between(record.startTime, record.endTime).toMinutes() / 60.0,
                sleepQuality = null,
                sorenessLevel = 0,
                fatigueLevel = 0,
                painLevel = 0,
                stressLevel = 0,
                notes = null
            )
        }

        val bodyMetricEntities = buildList {
            weights.forEach { record ->
                add(
                    BodyMetricsEntryEntity(
                        id = record.metadata.id ?: syntheticId("weight", record.time),
                        timestamp = OffsetDateTime.ofInstant(record.time, ZoneOffset.UTC).toString(),
                        weightKg = record.weight.inKilograms,
                        heightM = null,
                        bodyFatPercent = null,
                        waistCm = null
                    )
                )
            }
            heights.forEach { record ->
                add(
                    BodyMetricsEntryEntity(
                        id = record.metadata.id ?: syntheticId("height", record.time),
                        timestamp = OffsetDateTime.ofInstant(record.time, ZoneOffset.UTC).toString(),
                        weightKg = null,
                        heightM = record.height.inMeters,
                        bodyFatPercent = null,
                        waistCm = null
                    )
                )
            }
        }

        val cycleEntities = emptyList<CycleEntryEntity>()
        val activityEntities = emptyList<ActivityEntryEntity>()
        val exerciseSets = emptyList<ExerciseSetEntryEntity>()

        dao.clearDailySnapshots()
        dao.clearWorkouts()
        dao.clearExerciseSets()
        dao.clearNutritionEntries()
        dao.clearRecoveryEntries()
        dao.clearBodyMetrics()
        dao.clearCycleEntries()
        dao.clearActivityEntries()

        dao.upsertDailySnapshots(
            snapshots.values.sortedBy { it.date }.map { accumulator ->
                DailySnapshotEntity(
                    date = accumulator.date.toString(),
                    weightKg = accumulator.weightKg,
                    heightM = accumulator.heightM,
                    stepCount = accumulator.stepCount.takeIf { it > 0 },
                    sleepHours = accumulator.sleepHours.takeIf { it > 0.0 },
                    caloriesConsumed = accumulator.caloriesConsumed.takeIf { it > 0.0 },
                    activeCaloriesBurned = accumulator.activeCaloriesBurned.takeIf { it > 0.0 },
                    totalCaloriesBurned = accumulator.totalCaloriesBurned.takeIf { it > 0.0 },
                    workoutCount = accumulator.workoutCount,
                    proteinGrams = accumulator.proteinGrams.takeIf { it > 0.0 },
                    carbsGrams = accumulator.carbsGrams.takeIf { it > 0.0 },
                    fatGrams = accumulator.fatGrams.takeIf { it > 0.0 },
                    recoveryScore = null,
                    lastSyncedAt = importedAt,
                    dataAvailabilityJson = accumulator.dataAvailabilityJson()
                )
            }
        )
        if (workoutEntities.isNotEmpty()) dao.upsertWorkouts(workoutEntities)
        if (exerciseSets.isNotEmpty()) dao.upsertExerciseSets(exerciseSets)
        if (nutritionEntities.isNotEmpty()) dao.upsertNutritionEntries(nutritionEntities)
        if (recoveryEntities.isNotEmpty()) dao.upsertRecoveryEntries(recoveryEntities)
        if (bodyMetricEntities.isNotEmpty()) dao.upsertBodyMetrics(bodyMetricEntities)
        if (cycleEntities.isNotEmpty()) dao.upsertCycleEntries(cycleEntities)
        if (activityEntities.isNotEmpty()) dao.upsertActivityEntries(activityEntities)

        return ImportSummary(
            snapshots = snapshots.size,
            workouts = workoutEntities.size,
            nutritionEntries = nutritionEntities.size,
            recoveryEntries = recoveryEntities.size,
            cycleEntries = cycleEntities.size,
            activityEntries = activityEntities.size
        )
    }

    private fun syntheticId(prefix: String, instant: Instant): String = "$prefix-${instant.toEpochMilli()}-${UUID.randomUUID()}"

    private fun dateFor(instant: Instant): LocalDate = instant.atZone(ZoneOffset.UTC).toLocalDate()

    private data class SnapshotAccumulator(
        val date: LocalDate,
        var weightKg: Double? = null,
        var heightM: Double? = null,
        var stepCount: Long = 0,
        var sleepHours: Double = 0.0,
        var caloriesConsumed: Double = 0.0,
        var activeCaloriesBurned: Double = 0.0,
        var totalCaloriesBurned: Double = 0.0,
        var workoutCount: Int = 0,
        var proteinGrams: Double = 0.0,
        var carbsGrams: Double = 0.0,
        var fatGrams: Double = 0.0
    ) {
        fun dataAvailabilityJson(): String = JSONObject().apply {
            put("weightKg", source(weightKg != null))
            put("heightM", source(heightM != null))
            put("stepCount", source(stepCount > 0))
            put("sleepHours", source(sleepHours > 0.0))
            put("caloriesConsumed", source(caloriesConsumed > 0.0))
            put("activeCaloriesBurned", source(activeCaloriesBurned > 0.0))
            put("totalCaloriesBurned", source(totalCaloriesBurned > 0.0))
            put("proteinGrams", source(proteinGrams > 0.0))
            put("carbsGrams", source(carbsGrams > 0.0))
            put("fatGrams", source(fatGrams > 0.0))
            put("recoveryScore", DataSource.UNAVAILABLE.name)
        }.toString()

        private fun source(available: Boolean): String = if (available) DataSource.MEASURED.name else DataSource.UNAVAILABLE.name
    }

    companion object {
        const val DEFAULT_LOOKBACK_DAYS = 90L

        fun buildRequiredPermissions(): Set<String> = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(WeightRecord::class),
            HealthPermission.getReadPermission(HeightRecord::class),
            HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(NutritionRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getReadPermission(MenstruationFlowRecord::class),
            HealthPermission.getReadPermission(MenstruationPeriodRecord::class),
            HealthPermission.getReadPermission(BasalBodyTemperatureRecord::class),
            HealthPermission.getReadPermission(OvulationTestRecord::class),
            HealthPermission.getReadPermission(CervicalMucusRecord::class),
            HealthPermission.getReadPermission(IntermenstrualBleedingRecord::class),
            HealthPermission.getReadPermission(SexualActivityRecord::class)
        )
    }
}
