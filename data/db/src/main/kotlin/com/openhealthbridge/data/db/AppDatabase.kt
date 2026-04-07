package com.openhealthbridge.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        DailySnapshotEntity::class,
        WorkoutEntryEntity::class,
        ExerciseSetEntryEntity::class,
        NutritionEntryEntity::class,
        RecoveryEntryEntity::class,
        BodyMetricsEntryEntity::class,
        PrRecordEntity::class,
        SyncEventEntity::class,
        PermissionStateEntity::class,
        AppSettingsEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase()
