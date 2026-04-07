package com.openhealthbridge.data.sync

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class PairingSecretStore(
    context: Context
) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveKeyB64(keyB64: String) {
        prefs.edit().putString(KEY_PAIRING_SECRET, keyB64).apply()
    }

    fun getKeyB64(): String? = prefs.getString(KEY_PAIRING_SECRET, null)

    fun clear() {
        prefs.edit()
            .remove(KEY_PAIRING_SECRET)
            .remove(KEY_DIRECT_HOST_URL)
            .remove(KEY_DIRECT_UPLOAD_TOKEN)
            .apply()
    }

    fun saveDirectHostUrl(url: String?) {
        if (url == null) {
            prefs.edit().remove(KEY_DIRECT_HOST_URL).apply()
        } else {
            prefs.edit().putString(KEY_DIRECT_HOST_URL, url).apply()
        }
    }

    fun getDirectHostUrl(): String? = prefs.getString(KEY_DIRECT_HOST_URL, null)

    fun saveDirectUploadToken(token: String?) {
        if (token == null) {
            prefs.edit().remove(KEY_DIRECT_UPLOAD_TOKEN).apply()
        } else {
            prefs.edit().putString(KEY_DIRECT_UPLOAD_TOKEN, token).apply()
        }
    }

    fun getDirectUploadToken(): String? = prefs.getString(KEY_DIRECT_UPLOAD_TOKEN, null)

    companion object {
        private const val FILE_NAME = "ohc_pairing_secret"
        private const val KEY_PAIRING_SECRET = "pairing.key_b64"
        private const val KEY_DIRECT_HOST_URL = "pairing.direct_host_url"
        private const val KEY_DIRECT_UPLOAD_TOKEN = "pairing.direct_upload_token"
    }
}
