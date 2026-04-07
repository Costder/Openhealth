package com.openhealthbridge.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.openhealthbridge.app.di.AppModule
import com.openhealthbridge.data.sync.TransportMode
import com.openhealthbridge.feature.settings.SettingAction
import com.openhealthbridge.feature.settings.SettingsFeature
import kotlinx.coroutines.launch
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier, color = MaterialTheme.colorScheme.background) {
                    SyncConsole()
                }
            }
        }
    }
}

@Composable
private fun SyncConsole() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val services = remember { AppModule.from(context) }
    val runtime = services.syncRuntime
    val healthConnectClient = remember {
        runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull()
    }

    var permissionsSummary by remember { mutableStateOf("Unknown") }
    var pairingSummary by remember { mutableStateOf("Not paired") }
    var pairingMode by remember { mutableStateOf("folder-sync") }
    var folderSummary by remember { mutableStateOf("No export folder selected") }
    var transportSummary by remember { mutableStateOf(TransportMode.SYNCTHING.displayName()) }
    var syncSummary by remember { mutableStateOf("Idle") }
    var diagnostics by remember { mutableStateOf("Waiting for action") }

    suspend fun refreshState() {
        val granted = healthConnectClient?.permissionController?.getGrantedPermissions().orEmpty()
        val required = runtime.healthconnectFeature.requiredPermissions()
        val grantedCount = granted.count { it in required }
        permissionsSummary = "$grantedCount/${required.size} granted"
        pairingMode = runtime.metadataStore.getPairingMode()
        val hasKey = !runtime.secretStore.getKeyB64().isNullOrBlank()
        pairingSummary = if (hasKey) "Paired ($pairingMode)" else "Not paired"
        
        if (pairingMode == "direct") {
            transportSummary = "Direct HTTP"
            folderSummary = "Not required"
        } else {
            transportSummary = runtime.metadataStore.getTransportMode().displayName()
            folderSummary = runtime.metadataStore.getExportTreeUri()?.let(Uri::parse)?.lastPathSegment ?: "No export folder selected"
        }
        val recentEvent = runtime.database.healthBridgeDao().getRecentSyncEvents(1).firstOrNull()
        syncSummary = recentEvent?.status ?: "Idle"
        diagnostics = recentEvent?.errorMessage ?: "Ready"
    }

    LaunchedEffect(Unit) {
        refreshState()
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) {
        scope.launch { refreshState() }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            scope.launch {
                runtime.metadataStore.setExportTreeUri(uri.toString())
                refreshState()
            }
        }
    }

    val qrLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val qrContents = result.contents ?: return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val payload = JSONObject(qrContents)
                require(payload.optString("type") == "ohc-pairing") { "Unexpected QR payload type." }
                val version = payload.optInt("version", 1)
                require(version in 1..2) { "Unsupported QR version: ${'$'}version" }
                val parsedMode = payload.optString("pairingMode", "folder-sync")
                
                runtime.secretStore.saveKeyB64(payload.getString("keyB64"))
                runtime.metadataStore.setPairingMode(parsedMode)
                
                if (parsedMode == "direct") {
                    runtime.secretStore.saveDirectHostUrl(payload.getString("directHostUrl"))
                    runtime.secretStore.saveDirectUploadToken(payload.getString("directUploadToken"))
                } else {
                    runtime.secretStore.saveDirectHostUrl(null)
                    runtime.secretStore.saveDirectUploadToken(null)
                    runtime.metadataStore.setTransportMode(TransportMode.fromManifest(payload.optString("transportMode")))
                }
                refreshState()
            } catch (e: Exception) {
                syncSummary = "ERROR"
                diagnostics = "QR Parsing failed: ${e.message}"
            }
        }
    }

    SettingsFeature(
        title = "Open Health Bridge Sync",
        subtitle = "Pair with OpenHealthConnect, choose a shared folder, then export an encrypted snapshot bundle using Syncthing, Nextcloud/WebDAV, or Tailscale metadata.",
        statuses = listOf(
            "Health Connect" to permissionsSummary,
            "Pairing" to pairingSummary,
            "Transport" to transportSummary,
            "Shared folder" to folderSummary,
            "Last sync" to syncSummary,
            "Diagnostics" to diagnostics
        ),
        actions = listOfNotNull(
            SettingAction("Grant Health Connect Permissions") {
                requestPermissionLauncher.launch(runtime.healthconnectFeature.requiredPermissions())
            },
            SettingAction("Scan OHC Pairing QR") {
                qrLauncher.launch(
                    ScanOptions().apply {
                        setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                        setPrompt("Scan the QR produced by `ohc pair qr`")
                        setBeepEnabled(false)
                        setOrientationLocked(false)
                    }
                )
            },
            if (pairingMode == "folder-sync") SettingAction("Choose Shared Folder") {
                folderPickerLauncher.launch(null)
            } else null,
            if (pairingMode == "folder-sync") SettingAction("Use Syncthing") {
                scope.launch {
                    runtime.metadataStore.setTransportMode(TransportMode.SYNCTHING)
                    refreshState()
                }
            } else null,
            if (pairingMode == "folder-sync") SettingAction("Use Nextcloud / WebDAV") {
                scope.launch {
                    runtime.metadataStore.setTransportMode(TransportMode.NEXTCLOUD)
                    refreshState()
                }
            } else null,
            if (pairingMode == "folder-sync") SettingAction("Use Tailscale") {
                scope.launch {
                    runtime.metadataStore.setTransportMode(TransportMode.TAILSCALE)
                    refreshState()
                }
            } else null,
            SettingAction(
                label = "Export Now",
                enabled = healthConnectClient != null
            ) {
                scope.launch {
                    syncSummary = "Running"
                    diagnostics = "Importing Health Connect and writing encrypted bundle"
                    runCatching { runtime.coordinator.runSync() }
                        .onSuccess { result ->
                            syncSummary = "SUCCESS"
                            diagnostics = "Bundle ${result.bundleId} wrote ${result.eventCount} events using ${runtime.metadataStore.getTransportMode().displayName()}"
                        }
                        .onFailure { error ->
                            syncSummary = "ERROR"
                            diagnostics = error.message ?: "Unknown export failure"
                        }
                    refreshState()
                }
            }
        )
    )
}
