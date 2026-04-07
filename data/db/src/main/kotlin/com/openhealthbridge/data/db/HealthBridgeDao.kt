package com.openhealthbridge.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface HealthBridgeDao {
    @Query("SELECT * FROM daily_snapshots WHERE date = :date LIMIT 1")
    suspend fun getDailySnapshot(date: String): DailySnapshotEntity?

    @Query("SELECT * FROM daily_snapshots ORDER BY date ASC")
    suspend fun getAllDailySnapshots(): List<DailySnapshotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailySnapshots(entries: List<DailySnapshotEntity>)

    @Query("DELETE FROM daily_snapshots")
    suspend fun clearDailySnapshots()

    @Query("SELECT * FROM workout_entries ORDER BY startTime DESC LIMIT :limit")
    suspend fun getRecentWorkouts(limit: Int): List<WorkoutEntryEntity>

    @Query("SELECT * FROM workout_entries ORDER BY startTime DESC")
    suspend fun getAllWorkouts(): List<WorkoutEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkouts(entries: List<WorkoutEntryEntity>)

    @Query("DELETE FROM workout_entries")
    suspend fun clearWorkouts()

    @Query("SELECT * FROM exercise_set_entries WHERE workoutId = :workoutId ORDER BY setIndex ASC")
    suspend fun getExerciseSetsForWorkout(workoutId: String): List<ExerciseSetEntryEntity>

    @Query("SELECT * FROM exercise_set_entries ORDER BY workoutId ASC, setIndex ASC")
    suspend fun getAllExerciseSets(): List<ExerciseSetEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExerciseSets(entries: List<ExerciseSetEntryEntity>)

    @Query("DELETE FROM exercise_set_entries WHERE workoutId IN (:workoutIds)")
    suspend fun deleteExerciseSetsForWorkouts(workoutIds: List<String>)

    @Query("DELETE FROM exercise_set_entries")
    suspend fun clearExerciseSets()

    @Query("SELECT * FROM nutrition_entries ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentNutritionEntries(limit: Int): List<NutritionEntryEntity>

    @Query("SELECT * FROM nutrition_entries ORDER BY timestamp DESC")
    suspend fun getAllNutritionEntries(): List<NutritionEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNutritionEntries(entries: List<NutritionEntryEntity>)

    @Query("DELETE FROM nutrition_entries")
    suspend fun clearNutritionEntries()

    @Query("SELECT * FROM recovery_entries ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentRecoveryEntries(limit: Int): List<RecoveryEntryEntity>

    @Query("SELECT * FROM recovery_entries ORDER BY timestamp DESC")
    suspend fun getAllRecoveryEntries(): List<RecoveryEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecoveryEntries(entries: List<RecoveryEntryEntity>)

    @Query("DELETE FROM recovery_entries")
    suspend fun clearRecoveryEntries()

    @Query("SELECT * FROM cycle_entries ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentCycleEntries(limit: Int): List<CycleEntryEntity>

    @Query("SELECT * FROM cycle_entries ORDER BY timestamp DESC")
    suspend fun getAllCycleEntries(): List<CycleEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCycleEntries(entries: List<CycleEntryEntity>)

    @Query("DELETE FROM cycle_entries")
    suspend fun clearCycleEntries()

    @Query("SELECT * FROM activity_entries ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentActivityEntries(limit: Int): List<ActivityEntryEntity>

    @Query("SELECT * FROM activity_entries ORDER BY timestamp DESC")
    suspend fun getAllActivityEntries(): List<ActivityEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertActivityEntries(entries: List<ActivityEntryEntity>)

    @Query("DELETE FROM activity_entries")
    suspend fun clearActivityEntries()

    @Query("SELECT * FROM pr_records ORDER BY achievedAt DESC LIMIT :limit")
    suspend fun getRecentPrRecords(limit: Int): List<PrRecordEntity>

    @Query("SELECT * FROM pr_records ORDER BY achievedAt DESC")
    suspend fun getAllPrRecords(): List<PrRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPrRecords(entries: List<PrRecordEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBodyMetrics(entries: List<BodyMetricsEntryEntity>)

    @Query("SELECT * FROM body_metrics_entries ORDER BY timestamp DESC")
    suspend fun getAllBodyMetrics(): List<BodyMetricsEntryEntity>

    @Query("DELETE FROM body_metrics_entries")
    suspend fun clearBodyMetrics()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncEvent(event: SyncEventEntity)

    @Query("SELECT * FROM sync_events ORDER BY eventTime DESC LIMIT :limit")
    suspend fun getRecentSyncEvents(limit: Int): List<SyncEventEntity>

    @Query("SELECT value FROM app_settings WHERE key = :key LIMIT 1")
    suspend fun getSettingValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSetting(setting: AppSettingsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: List<AppSettingsEntity>)
}
