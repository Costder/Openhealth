package com.openhealthbridge.feature.healthconnect

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthConnectImporterTest {
    @Test
    fun requiredPermissionsIncludeCoreSyncRecords() {
        val permissions = HealthConnectImporter.buildRequiredPermissions()

        assertTrue(permissions.contains(HealthPermission.getReadPermission(StepsRecord::class)))
        assertTrue(permissions.contains(HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)))
        assertTrue(permissions.contains(HealthPermission.getReadPermission(ExerciseSessionRecord::class)))
    }
}
