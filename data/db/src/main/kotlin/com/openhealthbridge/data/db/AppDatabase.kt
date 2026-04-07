package com.openhealthbridge.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.openhealthbridge.data.db.migrations.MIGRATION_1_2

@Database(
    entities = [
        DailySnapshotEntity::class,
        WorkoutEntryEntity::class,
        ExerciseSetEntryEntity::class,
        NutritionEntryEntity::class,
        RecoveryEntryEntity::class,
        BodyMetricsEntryEntity::class,
        CycleEntryEntity::class,
        ActivityEntryEntity::class,
        PrRecordEntity::class,
        SyncEventEntity::class,
        PermissionStateEntity::class,
        AppSettingsEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun healthBridgeDao(): HealthBridgeDao

    companion object {
        val migrations = arrayOf(MIGRATION_1_2)
    }
}
