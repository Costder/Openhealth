package com.openhealthbridge.data.repository

import com.openhealthbridge.core.models.DataSource
import com.openhealthbridge.core.models.DateRange
import com.openhealthbridge.core.models.DailyHealthSnapshot
import com.openhealthbridge.core.models.EntrySourceType
import com.openhealthbridge.core.models.ExerciseSetEntry
import com.openhealthbridge.core.models.HealthBridgeService
import com.openhealthbridge.core.models.HealthFlag
import com.openhealthbridge.core.models.NutritionEntry
import com.openhealthbridge.core.models.PRRecord
import com.openhealthbridge.core.models.PrType
import com.openhealthbridge.core.models.RecoveryEntry
import com.openhealthbridge.core.models.TrendSummary
import com.openhealthbridge.core.models.WorkoutEntry
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import kotlin.math.roundToInt

class InMemoryHealthBridgeService : HealthBridgeService {
    private val workouts = mutableListOf<WorkoutEntry>()
    private val recovery = mutableListOf<RecoveryEntry>()
    private val nutrition = mutableListOf<NutritionEntry>()
    private val prs = mutableListOf<PRRecord>()
    private val snapshots = mutableMapOf<LocalDate, DailyHealthSnapshot>()

    init {
        val now = OffsetDateTime.now()
        val workout = WorkoutEntry(
            id = "w-1",
            startTime = now.minusHours(1),
            endTime = now,
            sourceType = EntrySourceType.MANUAL,
            workoutType = "Strength",
            completed = true,
            notes = "Starter sample",
            perceivedDifficulty = 7,
            painFlag = false,
            exerciseSets = listOf(
                ExerciseSetEntry("s-1", "w-1", "Squat", 1, 5, 100.0, null, null, false, null)
            )
        )
        workouts += workout
        recovery += RecoveryEntry("r-1", now, 7.2, 4, 2, 2, 1, 2, null)
        nutrition += NutritionEntry("n-1", now, "Lunch", EntrySourceType.MANUAL, 650.0, 45.0, 60.0, 20.0, null)
        prs += PRRecord("pr-1", "Squat", PrType.MAX_WEIGHT, 100.0, "kg", null, now, "w-1", "Squat-MAX_WEIGHT-${LocalDate.now()}")
        snapshots[LocalDate.now()] = DailyHealthSnapshot(
            date = LocalDate.now(),
            weightKg = 80.5,
            heightM = 1.78,
            stepCount = 8450,
            sleepHours = 7.2,
            caloriesConsumed = 2100.0,
            activeCaloriesBurned = 420.0,
            totalCaloriesBurned = 2500.0,
            workoutCount = 1,
            proteinGrams = 140.0,
            carbsGrams = 220.0,
            fatGrams = 70.0,
            recoveryScore = 76,
            flags = listOf(HealthFlag.LOW_ADHERENCE),
            lastSyncedAt = Instant.now(),
            dataAvailability = mapOf(
                "stepCount" to DataSource.MEASURED,
                "sleepHours" to DataSource.MANUAL,
                "weightKg" to DataSource.MANUAL
            )
        )
    }

    override suspend fun getDailySnapshot(date: LocalDate): DailyHealthSnapshot? = snapshots[date]

    override suspend fun getWeeklySummary(start: LocalDate, end: LocalDate): TrendSummary {
        val period = snapshots.values.filter { it.date >= start && it.date <= end }
        fun avg(values: List<Double>) = if (values.isEmpty()) null else values.average()

        val avgWeight = avg(period.mapNotNull { it.weightKg })
        val avgSleep = avg(period.mapNotNull { it.sleepHours })
        val avgSteps = avg(period.mapNotNull { it.stepCount?.toDouble() })
        val avgCaloriesConsumed = avg(period.mapNotNull { it.caloriesConsumed })
        val avgCaloriesBurned = avg(period.mapNotNull { it.totalCaloriesBurned })
        val adherence = if (period.isEmpty()) null else period.count { it.workoutCount > 0 }.toDouble() / period.size
        val riskCount = period.count { (it.recoveryScore ?: 100) < 50 }

        return TrendSummary(
            rangeStart = start,
            rangeEnd = end,
            avgWeight = avgWeight,
            avgSleep = avgSleep,
            avgSteps = avgSteps,
            avgCaloriesConsumed = avgCaloriesConsumed,
            avgCaloriesBurned = avgCaloriesBurned,
            workoutAdherenceRate = adherence?.times(100.0)?.roundToInt()?.toDouble(),
            newPrCount = prs.count { it.achievedAt.toLocalDate() in start..end },
            recoveryRiskCount = riskCount
        )
    }

    override suspend fun getRecentWorkouts(limit: Int): List<WorkoutEntry> = workouts.takeLast(limit.coerceAtLeast(1))
    override suspend fun getRecoveryEntries(limit: Int): List<RecoveryEntry> = recovery.takeLast(limit.coerceAtLeast(1))
    override suspend fun getNutritionEntries(limit: Int): List<NutritionEntry> = nutrition.takeLast(limit.coerceAtLeast(1))
    override suspend fun getPrRecords(limit: Int): List<PRRecord> = prs.takeLast(limit.coerceAtLeast(1))

    override suspend fun logWorkoutEntry(entry: WorkoutEntry) {
        workouts += entry
    }

    override suspend fun logNutritionEntry(entry: NutritionEntry) {
        nutrition += entry
    }

    override suspend fun logRecoveryEntry(entry: RecoveryEntry) {
        recovery += entry
    }

    override suspend fun exportJson(range: DateRange): File {
        val file = File.createTempFile("open-health-bridge-export-${Instant.now().toEpochMilli()}", ".json")
        file.writeText(
            """
            {
              "schema_version": "1.0",
              "range": {"start": "${range.start}", "end": "${range.end}"},
              "counts": {
                "workouts": ${workouts.size},
                "nutrition": ${nutrition.size},
                "recovery": ${recovery.size},
                "prs": ${prs.size}
              }
            }
            """.trimIndent()
        )
        return file
    }
}
