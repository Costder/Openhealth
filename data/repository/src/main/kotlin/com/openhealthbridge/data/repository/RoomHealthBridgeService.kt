package com.openhealthbridge.data.repository

import com.openhealthbridge.core.models.ActivityEntry
import com.openhealthbridge.core.models.CycleEntry
import com.openhealthbridge.core.models.DateRange
import com.openhealthbridge.core.models.DailyHealthSnapshot
import com.openhealthbridge.core.models.HealthBridgeService
import com.openhealthbridge.core.models.NutritionEntry
import com.openhealthbridge.core.models.PRRecord
import com.openhealthbridge.core.models.RecoveryEntry
import com.openhealthbridge.core.models.TrendSummary
import com.openhealthbridge.core.models.WorkoutEntry
import com.openhealthbridge.data.db.HealthBridgeDao
import java.io.File
import java.time.Instant
import java.time.LocalDate
import kotlin.math.roundToInt

class RoomHealthBridgeService(
    private val dao: HealthBridgeDao
) : HealthBridgeService {
    override suspend fun getDailySnapshot(date: LocalDate): DailyHealthSnapshot? = dao.getDailySnapshot(date.toString())?.toDomain()

    override suspend fun getWeeklySummary(start: LocalDate, end: LocalDate): TrendSummary {
        val snapshots = dao.getAllDailySnapshots()
            .map { it.toDomain() }
            .filter { it.date in start..end }

        fun avg(values: List<Double>) = if (values.isEmpty()) null else values.average()

        val avgWeight = avg(snapshots.mapNotNull { it.weightKg })
        val avgSleep = avg(snapshots.mapNotNull { it.sleepHours })
        val avgSteps = avg(snapshots.mapNotNull { it.stepCount?.toDouble() })
        val avgCaloriesConsumed = avg(snapshots.mapNotNull { it.caloriesConsumed })
        val avgCaloriesBurned = avg(snapshots.mapNotNull { it.totalCaloriesBurned })
        val adherence = if (snapshots.isEmpty()) null else snapshots.count { it.workoutCount > 0 }.toDouble() / snapshots.size
        val riskCount = snapshots.count { (it.recoveryScore ?: 100) < 50 }

        return TrendSummary(
            rangeStart = start,
            rangeEnd = end,
            avgWeight = avgWeight,
            avgSleep = avgSleep,
            avgSteps = avgSteps,
            avgCaloriesConsumed = avgCaloriesConsumed,
            avgCaloriesBurned = avgCaloriesBurned,
            workoutAdherenceRate = adherence?.times(100.0)?.roundToInt()?.toDouble(),
            newPrCount = dao.getAllPrRecords().map { it.toDomain() }.count { it.achievedAt.toLocalDate() in start..end },
            recoveryRiskCount = riskCount
        )
    }

    override suspend fun getAllSnapshots(): List<DailyHealthSnapshot> = dao.getAllDailySnapshots().map { it.toDomain() }

    override suspend fun getAllWorkouts(): List<WorkoutEntry> {
        val setsByWorkout = dao.getAllExerciseSets().groupBy { it.workoutId }
        return dao.getAllWorkouts().map { entity ->
            entity.toDomain(setsByWorkout[entity.id].orEmpty().map { it.toDomain() })
        }
    }

    override suspend fun getAllRecoveryEntries(): List<RecoveryEntry> = dao.getAllRecoveryEntries().map { it.toDomain() }

    override suspend fun getAllNutritionEntries(): List<NutritionEntry> = dao.getAllNutritionEntries().map { it.toDomain() }

    override suspend fun getAllCycleEntries(): List<CycleEntry> = dao.getAllCycleEntries().map { it.toDomain() }

    override suspend fun getAllActivityEntries(): List<ActivityEntry> = dao.getAllActivityEntries().map { it.toDomain() }

    override suspend fun getAllPrRecords(): List<PRRecord> = dao.getAllPrRecords().map { it.toDomain() }

    override suspend fun getRecentWorkouts(limit: Int): List<WorkoutEntry> {
        val workouts = dao.getRecentWorkouts(limit)
        return workouts.map { entity ->
            entity.toDomain(dao.getExerciseSetsForWorkout(entity.id).map { it.toDomain() })
        }
    }

    override suspend fun getRecoveryEntries(limit: Int): List<RecoveryEntry> = dao.getRecentRecoveryEntries(limit).map { it.toDomain() }

    override suspend fun getNutritionEntries(limit: Int): List<NutritionEntry> = dao.getRecentNutritionEntries(limit).map { it.toDomain() }

    override suspend fun getCycleEntries(limit: Int): List<CycleEntry> = dao.getRecentCycleEntries(limit).map { it.toDomain() }

    override suspend fun getActivityEntries(limit: Int): List<ActivityEntry> = dao.getRecentActivityEntries(limit).map { it.toDomain() }

    override suspend fun getPrRecords(limit: Int): List<PRRecord> = dao.getRecentPrRecords(limit).map { it.toDomain() }

    override suspend fun logWorkoutEntry(entry: WorkoutEntry) {
        dao.upsertWorkouts(listOf(entry.toEntity()))
        dao.deleteExerciseSetsForWorkouts(listOf(entry.id))
        dao.upsertExerciseSets(entry.exerciseSets.map { it.toEntity() })
    }

    override suspend fun logNutritionEntry(entry: NutritionEntry) {
        dao.upsertNutritionEntries(listOf(entry.toEntity()))
    }

    override suspend fun logRecoveryEntry(entry: RecoveryEntry) {
        dao.upsertRecoveryEntries(listOf(entry.toEntity()))
    }

    override suspend fun exportJson(range: DateRange): File {
        val file = File.createTempFile("open-health-bridge-export-${Instant.now().toEpochMilli()}", ".json")
        val snapshotCount = getAllSnapshots().count { it.date in range.start..range.end }
        val workoutCount = getAllWorkouts().count { it.startTime.toLocalDate() in range.start..range.end }
        val nutritionCount = getAllNutritionEntries().count { it.timestamp.toLocalDate() in range.start..range.end }
        val recoveryCount = getAllRecoveryEntries().count { it.timestamp.toLocalDate() in range.start..range.end }
        val prCount = getAllPrRecords().count { it.achievedAt.toLocalDate() in range.start..range.end }
        file.writeText(
            """
            {
              "schema_version": "1.0",
              "range": {"start": "${range.start}", "end": "${range.end}"},
              "counts": {
                "snapshots": $snapshotCount,
                "workouts": $workoutCount,
                "nutrition": $nutritionCount,
                "recovery": $recoveryCount,
                "prs": $prCount
              }
            }
            """.trimIndent()
        )
        return file
    }
}
