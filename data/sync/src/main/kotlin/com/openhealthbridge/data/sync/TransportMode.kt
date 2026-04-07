package com.openhealthbridge.data.sync

enum class TransportMode {
    SYNCTHING,
    NEXTCLOUD,
    TAILSCALE;

    fun manifestValue(): String = name.lowercase()

    fun displayName(): String = when (this) {
        SYNCTHING -> "Syncthing"
        NEXTCLOUD -> "Nextcloud / WebDAV"
        TAILSCALE -> "Tailscale"
    }

    companion object {
        fun fromRaw(value: String?): TransportMode = entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
            ?: SYNCTHING

        fun fromManifest(value: String?): TransportMode = when (value?.lowercase()) {
            "nextcloud" -> NEXTCLOUD
            "tailscale" -> TAILSCALE
            else -> SYNCTHING
        }
    }
}
