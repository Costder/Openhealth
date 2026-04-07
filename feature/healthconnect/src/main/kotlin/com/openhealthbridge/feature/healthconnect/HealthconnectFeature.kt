package com.openhealthbridge.feature.healthconnect

class HealthconnectFeature(
    private val importer: HealthConnectImporter
) {
    fun requiredPermissions(): Set<String> = importer.requiredPermissions()

    suspend fun refreshRecentData(): ImportSummary = importer.importRecentData()
}
