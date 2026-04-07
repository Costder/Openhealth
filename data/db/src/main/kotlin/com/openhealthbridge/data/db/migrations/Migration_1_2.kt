package com.openhealthbridge.data.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Placeholder migration template. Any schema update must add a real migration and matching test.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS cycle_entries (
                id TEXT NOT NULL PRIMARY KEY,
                timestamp TEXT NOT NULL,
                sourceType TEXT NOT NULL,
                flow INTEGER,
                isStartOfCycle INTEGER NOT NULL,
                bbtCelsius REAL,
                ovulationTestResult INTEGER,
                cervicalMucusAppearance TEXT,
                intermenstrualBleeding INTEGER NOT NULL,
                notes TEXT
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS activity_entries (
                id TEXT NOT NULL PRIMARY KEY,
                timestamp TEXT NOT NULL,
                sourceType TEXT NOT NULL,
                wasProtected INTEGER,
                notes TEXT
            )
            """.trimIndent()
        )
    }
}
