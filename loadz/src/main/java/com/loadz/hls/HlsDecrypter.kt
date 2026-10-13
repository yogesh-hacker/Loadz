package com.loadz.hls

import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Handles AES-128 key fetching and segment decryption according to RFC 8216.
 */
class HlsDecrypter(private val client: OkHttpClient) {

    private val keyCache = ConcurrentHashMap<String, ByteArray>()

    fun fetchKey(keyUrl: String, headers: Map<String, String>): ByteArray {
        keyCache[keyUrl]?.let { return it }

        val request = Request.Builder().url(keyUrl).apply {
            headers.forEach { (k, v) -> addHeader(k, v) }
        }.build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Failed to fetch AES key from $keyUrl")
            val keyBytes = response.body?.bytes() ?: throw Exception("Empty AES key payload")
            if (keyBytes.size != 16) {
                throw Exception("Invalid AES-128 key length: ${keyBytes.size} bytes (expected 16)")
            }
            keyCache[keyUrl] = keyBytes
            return keyBytes
        }
    }

    fun decryptSegment(
        encryptedBytes: ByteArray,
        keyBytes: ByteArray,
        ivHex: String?,
        sequenceNumber: Int
    ): ByteArray {
        val ivBytes = parseOrGenerateIv(ivHex, sequenceNumber)
        val keySpec = SecretKeySpec(keyBytes, "AES")
        val ivSpec = IvParameterSpec(ivBytes)

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
        return cipher.doFinal(encryptedBytes)
    }

    private fun parseOrGenerateIv(ivHex: String?, sequenceNumber: Int): ByteArray {
        if (!ivHex.isNullOrEmpty()) {
            val cleanHex = ivHex.removePrefix("0x").removePrefix("0X")
            val iv = ByteArray(16)
            for (i in 0 until 16) {
                val index = i * 2
                if (index + 2 <= cleanHex.length) {
                    iv[i] = cleanHex.substring(index, index + 2).toInt(16).toByte()
                }
            }
            return iv
        } else {
            // Default IV is sequence number formatted as 16-byte big-endian integer
            val iv = ByteArray(16)
            iv[12] = (sequenceNumber shr 24 and 0xFF).toByte()
            iv[13] = (sequenceNumber shr 16 and 0xFF).toByte()
            iv[14] = (sequenceNumber shr 8 and 0xFF).toByte()
            iv[15] = (sequenceNumber and 0xFF).toByte()
            return iv
        }
    }
}