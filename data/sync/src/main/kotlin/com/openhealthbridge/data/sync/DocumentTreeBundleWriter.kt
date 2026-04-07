package com.openhealthbridge.data.sync

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

class DocumentTreeBundleWriter(
    private val context: Context
) {
    fun writeBundle(treeUri: String, bundle: PendingBundle): Uri {
        val root = DocumentFile.fromTreeUri(context, Uri.parse(treeUri))
            ?: error("Configured export folder is unavailable.")
        val bundleDir = root.findFile(bundle.bundleId) ?: root.createDirectory(bundle.bundleId)
            ?: error("Unable to create export directory for ${bundle.bundleId}.")

        writeBinary(bundleDir, "payload.enc", "application/octet-stream", bundle.payloadCiphertext)
        writeText(bundleDir, "manifest.json", "application/json", bundle.manifestJson)
        return bundleDir.uri
    }

    private fun writeBinary(parent: DocumentFile, name: String, mimeType: String, bytes: ByteArray) {
        parent.findFile(name)?.delete()
        val file = parent.createFile(mimeType, name) ?: error("Unable to create $name")
        context.contentResolver.openOutputStream(file.uri)?.use { it.write(bytes) }
            ?: error("Unable to write $name")
    }

    private fun writeText(parent: DocumentFile, name: String, mimeType: String, content: String) {
        writeBinary(parent, name, mimeType, content.toByteArray(Charsets.UTF_8))
    }
}
