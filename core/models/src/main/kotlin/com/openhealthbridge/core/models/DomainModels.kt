package com.openhealthbridge.core.models

import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

enum class DataSource { MEASURED, MANUAL, ESTIMATED, UNAVAILABLE }
enum class HealthFlag { LOW_SLEEP, HIGH_FATIGUE, ELEVATED_STRESS, LOW_ADHERENCE }
enum class EntrySourceType { HEALTH_CONNECT, MANUAL }
enum class PrType {
    MAX_REPS,
    MAX_WEIGHT,
    MAX_VOLUME,
    FASTEST_TIME,
    LONGEST_DURATION,
    HIGHEST_WEEKLY_STEPS,
    LONGEST_ADHERENCE_STREAK
}

data class DateRange(val start: LocalDate, val end: LocalDate)

data class DailyHealthSnapshot(
    val date: LocalDate,
    val weightKg: Double?,
    val heightM: Double?,
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
    val isPeriodDay: Boolean = false,
    val lastSexualActivity: Instant? = null,
    val flags: List<HealthFlag>,
    val lastSyncedAt: Instant,
    val dataAvailability: Map<String, DataSource>
) {
    val bmi: Double?
        get() = if (weightKg != null && heightM != null && heightM > 0.0) weightKg / (heightM * heightM) else null
}

data class WorkoutEntry(
    val id: String,
    val startTime: OffsetDateTime,
    val endTime: OffsetDateTime,
    val sourceType: EntrySourceType,
    val workoutType: String,
    val completed: Boolean,
    val notes: String?,
    val perceivedDifficulty: Int?,
    val painFlag: Boolean,
    val exerciseSets: List<ExerciseSetEntry>
)

data class ExerciseSetEntry(
    val id: String,
    val workoutId: String,
    val exerciseName: String,
    val setIndex: Int,
    val reps: Int?,
    val weightKg: Double?,
    val durationSeconds: Int?,
    val distanceMeters: Double?,
    val isBodyweight: Boolean,
    val notes: String?
)

data class NutritionEntry(
    val id: String,
    val timestamp: OffsetDateTime,
    val mealName: String,
    val sourceType: EntrySourceType,
    val calories: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val notes: String?
)

data class RecoveryEntry(
    val id: String,
    val timestamp: OffsetDateTime,
    val sleepHours: Double?,
    val sleepQuality: Int?,
    val sorenessLevel: Int,
    val fatigueLevel: Int,
    val painLevel: Int,
    val stressLevel: Int,
    val notes: String?
)

data class CycleEntry(
    val id: String,
    val timestamp: OffsetDateTime,
    val sourceType: EntrySourceType,
    val flow: Int?, // 1: Light, 2: Medium, 3: Heavy
    val isStartOfCycle: Boolean,
    val bbtCelsius: Double?,
    val ovulationTestResult: Int?, // 0: Negative, 1: Positive, 2: High
    val cervicalMucusAppearance: String?,
    val intermenstrualBleeding: Boolean,
    val notes: String?
)

data class ActivityEntry(
    val id: String,
    val timestamp: OffsetDateTime,
    val sourceType: EntrySourceType,
    val wasProtected: Boolean?,
    val notes: String?
)

data class PRRecord(
    val id: String,
    val exerciseName: String,
    val prType: PrType,
    val value: Double,
    val unit: String,
    val previousValue: Double?,
    val achievedAt: OffsetDateTime,
    val sourceWorkoutId: String?,
    val deduplicationKey: String
)

data class TrendSummary(
    val rangeStart: LocalDate,
    val rangeEnd: LocalDate,
    val avgWeight: Double?,
    val avgSleep: Double?,
    val avgSteps: Double?,
    val avgCaloriesConsumed: Double?,
    val avgCaloriesBurned: Double?,
    val workoutAdherenceRate: Double?,
    val newPrCount: Int,
    val recoveryRiskCount: Int
)

interface HealthBridgeService {
    suspend fun getDailySnapshot(date: LocalDate): DailyHealthSnapshot?
    suspend fun getWeeklySummary(start: LocalDate, end: LocalDate): TrendSummary
    suspend fun getAllSnapshots(): List<DailyHealthSnapshot>
    suspend fun getAllWorkouts(): List<WorkoutEntry>
    suspend fun getAllRecoveryEntries(): List<RecoveryEntry>
    suspend fun getAllNutritionEntries(): List<NutritionEntry>
    suspend fun getAllCycleEntries(): List<CycleEntry>
    suspend fun getAllActivityEntries(): List<ActivityEntry>
    suspend fun getAllPrRecords(): List<PRRecord>
    suspend fun getRecentWorkouts(limit: Int): List<WorkoutEntry>
    suspend fun getRecoveryEntries(limit: Int): List<RecoveryEntry>
    suspend fun getNutritionEntries(limit: Int): List<NutritionEntry>
    suspend fun getCycleEntries(limit: Int): List<CycleEntry>
    suspend fun getActivityEntries(limit: Int): List<ActivityEntry>
    suspend fun getPrRecords(limit: Int): List<PRRecord>
    suspend fun logWorkoutEntry(entry: WorkoutEntry)
    suspend fun logNutritionEntry(entry: NutritionEntry)
    suspend fun logRecoveryEntry(entry: RecoveryEntry)
    suspend fun exportJson(range: DateRange): File
}
