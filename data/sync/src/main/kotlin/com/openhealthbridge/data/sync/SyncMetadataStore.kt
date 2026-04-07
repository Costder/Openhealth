package com.openhealthbridge.data.sync

import com.openhealthbridge.data.db.AppSettingsEntity
import com.openhealthbridge.data.db.HealthBridgeDao
import java.util.UUID

class SyncMetadataStore(
    private val dao: HealthBridgeDao
) {
    suspend fun getOrCreateInstallId(): String {
        val existing = dao.getSettingValue(KEY_INSTALL_ID)
        if (!existing.isNullOrBlank()) return existing
        val generated = UUID.randomUUID().toString()
        dao.upsertSetting(AppSettingsEntity(KEY_INSTALL_ID, generated))
        return generated
    }

    suspend fun getExportTreeUri(): String? = dao.getSettingValue(KEY_EXPORT_TREE_URI)

    suspend fun setExportTreeUri(uri: String) {
        dao.upsertSetting(AppSettingsEntity(KEY_EXPORT_TREE_URI, uri))
    }

    suspend fun getTransportMode(): TransportMode = TransportMode.fromRaw(dao.getSettingValue(KEY_TRANSPORT_MODE))

    suspend fun setTransportMode(mode: TransportMode) {
        dao.upsertSetting(AppSettingsEntity(KEY_TRANSPORT_MODE, mode.name))
    }

    suspend fun getNextSequence(): Int = (dao.getSettingValue(KEY_LAST_SEQUENCE)?.toIntOrNull() ?: 0) + 1

    suspend fun getPrevBundleId(): String? = dao.getSettingValue(KEY_PREV_BUNDLE_ID)

    suspend fun markBundleWritten(bundleId: String, sequence: Int) {
        dao.upsertSettings(
            listOf(
                AppSettingsEntity(KEY_PREV_BUNDLE_ID, bundleId),
                AppSettingsEntity(KEY_LAST_SEQUENCE, sequence.toString())
            )
        )
    }

    suspend fun getPairingMode(): String = dao.getSettingValue(KEY_PAIRING_MODE) ?: "folder-sync"
    
    suspend fun setPairingMode(mode: String) {
        dao.upsertSetting(AppSettingsEntity(KEY_PAIRING_MODE, mode))
    }

    companion object {
        private const val KEY_INSTALL_ID = "sync.install_id"
        private const val KEY_EXPORT_TREE_URI = "sync.export_tree_uri"
        private const val KEY_TRANSPORT_MODE = "sync.transport_mode"
        private const val KEY_LAST_SEQUENCE = "sync.last_sequence"
        private const val KEY_PREV_BUNDLE_ID = "sync.prev_bundle_id"
        private const val KEY_PAIRING_MODE = "sync.pairing_mode"
    }
}
