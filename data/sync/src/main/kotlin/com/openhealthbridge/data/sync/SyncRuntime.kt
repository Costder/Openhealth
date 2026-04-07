package com.openhealthbridge.data.sync

import android.content.Context
import androidx.room.Room
import com.openhealthbridge.data.db.AppDatabase
import com.openhealthbridge.data.repository.RoomHealthBridgeService
import com.openhealthbridge.feature.healthconnect.HealthConnectImporter
import com.openhealthbridge.feature.healthconnect.HealthconnectFeature

data class SyncRuntime(
    val database: AppDatabase,
    val healthconnectFeature: HealthconnectFeature,
    val importer: HealthConnectImporter,
    val service: RoomHealthBridgeService,
    val metadataStore: SyncMetadataStore,
    val secretStore: PairingSecretStore,
    val exporter: OhcBundleExporter,
    val writer: DocumentTreeBundleWriter,
    val directUploadClient: DirectUploadClient,
    val coordinator: SyncCoordinator
)

object SyncRuntimeFactory {
    @Volatile
    private var instance: SyncRuntime? = null

    fun get(context: Context): SyncRuntime {
        return instance ?: synchronized(this) {
            instance ?: build(context.applicationContext).also { instance = it }
        }
    }

    private fun build(context: Context): SyncRuntime {
        val database = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "open-health-bridge.db"
        ).addMigrations(*AppDatabase.migrations).build()
        val dao = database.healthBridgeDao()
        val service = RoomHealthBridgeService(dao)
        val importer = HealthConnectImporter(context, dao)
        val feature = HealthconnectFeature(importer)
        val metadataStore = SyncMetadataStore(dao)
        val secretStore = PairingSecretStore(context)
        val exporter = OhcBundleExporter(service, metadataStore, OhcBundleCrypto())
        val writer = DocumentTreeBundleWriter(context)
        val directUploadClient = DirectUploadClient()
        val coordinator = SyncCoordinator(importer, exporter, metadataStore, secretStore, writer, directUploadClient, dao)
        return SyncRuntime(
            database = database,
            healthconnectFeature = feature,
            importer = importer,
            service = service,
            metadataStore = metadataStore,
            secretStore = secretStore,
            exporter = exporter,
            writer = writer,
            directUploadClient = directUploadClient,
            coordinator = coordinator
        )
    }
}
