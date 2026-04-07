package com.openhealthbridge.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_snapshots")
data class DailySnapshotEntity(
    @PrimaryKey val date: String,
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
    val lastSyncedAt: String,
    val dataAvailabilityJson: String
)

@Entity(tableName = "workout_entries")
data class WorkoutEntryEntity(@PrimaryKey val id: String, val startTime: String, val endTime: String, val sourceType: String, val workoutType: String, val completed: Boolean, val notes: String?, val perceivedDifficulty: Int?, val painFlag: Boolean)
@Entity(tableName = "exercise_set_entries")
data class ExerciseSetEntryEntity(@PrimaryKey val id: String, val workoutId: String, val exerciseName: String, val setIndex: Int, val reps: Int?, val weightKg: Double?, val durationSeconds: Int?, val distanceMeters: Double?, val isBodyweight: Boolean, val notes: String?)
@Entity(tableName = "nutrition_entries")
data class NutritionEntryEntity(@PrimaryKey val id: String, val timestamp: String, val mealName: String, val sourceType: String, val calories: Double, val proteinGrams: Double, val carbsGrams: Double, val fatGrams: Double, val notes: String?)
@Entity(tableName = "recovery_entries")
data class RecoveryEntryEntity(@PrimaryKey val id: String, val timestamp: String, val sleepHours: Double?, val sleepQuality: Int?, val sorenessLevel: Int, val fatigueLevel: Int, val painLevel: Int, val stressLevel: Int, val notes: String?)
@Entity(tableName = "body_metrics_entries")
data class BodyMetricsEntryEntity(@PrimaryKey val id: String, val timestamp: String, val weightKg: Double?, val heightM: Double?, val bodyFatPercent: Double?, val waistCm: Double?)
@Entity(tableName = "cycle_entries")
data class CycleEntryEntity(@PrimaryKey val id: String, val timestamp: String, val sourceType: String, val flow: Int?, val isStartOfCycle: Boolean, val bbtCelsius: Double?, val ovulationTestResult: Int?, val cervicalMucusAppearance: String?, val intermenstrualBleeding: Boolean, val notes: String?)
@Entity(tableName = "activity_entries")
data class ActivityEntryEntity(@PrimaryKey val id: String, val timestamp: String, val sourceType: String, val wasProtected: Boolean?, val notes: String?)
@Entity(tableName = "pr_records")
data class PrRecordEntity(@PrimaryKey val id: String, val exerciseName: String, val prType: String, val value: Double, val unit: String, val previousValue: Double?, val achievedAt: String, val sourceWorkoutId: String?, val deduplicationKey: String)
@Entity(tableName = "sync_events")
data class SyncEventEntity(@PrimaryKey val id: String, val eventTime: String, val dataChanged: Boolean, val status: String, val errorMessage: String?)
@Entity(tableName = "permission_state")
data class PermissionStateEntity(@PrimaryKey val permission: String, val granted: Boolean, val updatedAt: String)
@Entity(tableName = "app_settings")
data class AppSettingsEntity(@PrimaryKey val key: String, val value: String)
