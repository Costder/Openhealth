package com.openhealthbridge.data.sync

import android.util.Base64
import com.goterl.lazysodium.SodiumAndroid
import java.security.MessageDigest
import java.security.SecureRandom

interface BundleCryptoEngine {
    fun randomNonce(size: Int = NONCE_BYTES): ByteArray
    fun encrypt(plaintext: ByteArray, keyB64: String, nonce: ByteArray): ByteArray
    fun decrypt(ciphertext: ByteArray, keyB64: String, nonce: ByteArray): ByteArray
    fun sha256Hex(input: ByteArray): String
    fun encodeBase64(input: ByteArray): String
    fun decodeBase64(input: String): ByteArray

    companion object {
        const val NONCE_BYTES = 24
        const val KEY_BYTES = 32
        const val AUTH_TAG_BYTES = 16
    }
}

class OhcBundleCrypto : BundleCryptoEngine {
    private val secureRandom = SecureRandom()
    private val sodium = SodiumAndroid()

    override fun randomNonce(size: Int): ByteArray = ByteArray(size).also(secureRandom::nextBytes)

    override fun encrypt(plaintext: ByteArray, keyB64: String, nonce: ByteArray): ByteArray {
        val key = decodeBase64(keyB64)
        require(key.size == BundleCryptoEngine.KEY_BYTES) { "OHC pairing key must be 32 bytes." }
        require(nonce.size == BundleCryptoEngine.NONCE_BYTES) { "OHC nonce must be 24 bytes." }

        val cipher = ByteArray(plaintext.size + BundleCryptoEngine.AUTH_TAG_BYTES)
        val outputSize = longArrayOf(0L)
        val result = sodium.crypto_aead_xchacha20poly1305_ietf_encrypt(
            cipher,
            outputSize,
            plaintext,
            plaintext.size.toLong(),
            null,
            0L,
            null,
            nonce,
            key
        )
        check(result == 0) { "Failed to encrypt OHC payload." }
        return cipher.copyOf(outputSize[0].toInt())
    }

    override fun decrypt(ciphertext: ByteArray, keyB64: String, nonce: ByteArray): ByteArray {
        val key = decodeBase64(keyB64)
        val plain = ByteArray(ciphertext.size - BundleCryptoEngine.AUTH_TAG_BYTES)
        val outputSize = longArrayOf(0L)
        val result = sodium.crypto_aead_xchacha20poly1305_ietf_decrypt(
            plain,
            outputSize,
            null,
            ciphertext,
            ciphertext.size.toLong(),
            null,
            0L,
            nonce,
            key
        )
        check(result == 0) { "Failed to decrypt OHC payload." }
        return plain.copyOf(outputSize[0].toInt())
    }

    override fun sha256Hex(input: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(input)
        .joinToString("") { byte -> "%02x".format(byte) }

    override fun encodeBase64(input: ByteArray): String = Base64.encodeToString(input, Base64.NO_WRAP)

    override fun decodeBase64(input: String): ByteArray = Base64.decode(input, Base64.DEFAULT)
}
