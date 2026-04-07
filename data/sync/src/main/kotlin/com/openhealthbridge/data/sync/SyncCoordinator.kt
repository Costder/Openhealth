package com.openhealthbridge.data.sync

import com.openhealthbridge.data.db.HealthBridgeDao
import com.openhealthbridge.data.db.SyncEventEntity
import com.openhealthbridge.feature.healthconnect.HealthConnectImporter
import java.time.Instant
import java.util.UUID

data class SyncResult(
    val importSummary: String,
    val bundleId: String,
    val eventCount: Int
)

class SyncCoordinator(
    private val importer: HealthConnectImporter,
    private val exporter: OhcBundleExporter,
    private val metadataStore: SyncMetadataStore,
    private val secretStore: PairingSecretStore,
    private val writer: DocumentTreeBundleWriter,
    private val directUploadClient: DirectUploadClient,
    private val dao: HealthBridgeDao
) {
    suspend fun runSync(): SyncResult {
        val startedAt = Instant.now().toString()
        return try {
            val key = secretStore.getKeyB64().orEmpty().ifBlank { error("Pairing key missing. Scan the OHC QR first.") }
            val pairingMode = metadataStore.getPairingMode()
            
            val importSummary = importer.importRecentData()
            val bundle = exporter.exportSnapshotBundle(key)
            
            if (pairingMode == "direct") {
                val hostUrl = secretStore.getDirectHostUrl() ?: error("Direct host URL missing")
                val uploadToken = secretStore.getDirectUploadToken() ?: error("Direct upload token missing")
                val uploadResult = directUploadClient.uploadBundle(
                    hostUrl = hostUrl,
                    token = uploadToken,
                    manifestJson = bundle.manifestJson,
                    payloadCiphertextB64 = android.util.Base64.encodeToString(bundle.payloadCiphertext, android.util.Base64.NO_WRAP)
                )
                uploadResult.getOrThrow()
            } else {
                val exportTreeUri = metadataStore.getExportTreeUri() ?: error("Export folder not configured.")
                writer.writeBundle(exportTreeUri, bundle)
            }

            exporter.markBundleWritten(bundle)
            dao.insertSyncEvent(
                SyncEventEntity(
                    id = UUID.randomUUID().toString(),
                    eventTime = startedAt,
                    dataChanged = true,
                    status = "SUCCESS",
                    errorMessage = null
                )
            )
            val eventCount = bundle.payloadJson.split("\"id\":").size - 1
            SyncResult(
                importSummary = "snapshots=${importSummary.snapshots}, workouts=${importSummary.workouts}, nutrition=${importSummary.nutritionEntries}, recovery=${importSummary.recoveryEntries}",
                bundleId = bundle.bundleId,
                eventCount = eventCount.coerceAtLeast(0)
            )
        } catch (error: Throwable) {
            dao.insertSyncEvent(
                SyncEventEntity(
                    id = UUID.randomUUID().toString(),
                    eventTime = startedAt,
                    dataChanged = false,
                    status = "ERROR",
                    errorMessage = error.message
                )
            )
            throw error
        }
    }
}
