package com.openhealthbridge.data.sync

data class OhcEvent(
    val id: String,
    val category: String,
    val ts: String,
    val payloadJson: String
)

data class PendingBundle(
    val bundleId: String,
    val sequence: Int,
    val prevBundleId: String?,
    val createdAt: String,
    val nonceB64: String,
    val ciphertextSha256: String,
    val payloadCiphertext: ByteArray,
    val payloadJson: String,
    val manifestJson: String
)
