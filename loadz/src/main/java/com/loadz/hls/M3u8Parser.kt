package com.loadz.hls

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Handles fetching and parsing M3U8 Master and Media playlists,
 * including segment URLs and AES-128 encryption key references.
 */
class M3u8Parser(private val client: OkHttpClient) {

    suspend fun fetchQualities(masterUrl: String, headers: Map<String, String>): List<HlsVariant> {
        val m3u8Content = fetchString(masterUrl, headers)

        if (m3u8Content.contains("#EXTINF:")) {
            return emptyList() // It's a direct media playlist
        }

        val lines = m3u8Content.split("\n").map { it.trim() }
        val variants = mutableListOf<HlsVariant>()

        var currentResolution = ""
        var currentBitrate = 0L

        for (i in lines.indices) {
            val line = lines[i]
            if (line.startsWith("#EXT-X-STREAM-INF")) {
                // Extract BANDWIDTH
                val bandwidthMatch = Regex("BANDWIDTH=(\\d+)").find(line)
                currentBitrate = bandwidthMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0L

                // Extract RESOLUTION
                val resolutionMatch = Regex("RESOLUTION=([0-9xX]+)").find(line)
                currentResolution = resolutionMatch?.groupValues?.get(1) ?: ""

                // Next non-empty line without # should be the URI
                for (j in (i + 1) until lines.size) {
                    val uriLine = lines[j]
                    if (uriLine.isNotEmpty() && !uriLine.startsWith("#")) {
                        val resolvedUrl = resolveUrl(masterUrl, uriLine)
                        variants.add(HlsVariant(currentResolution, currentBitrate, resolvedUrl))
                        break
                    }
                }
            }
        }

        return variants.sortedByDescending { it.bitrate }
    }

    // FIX: Made this suspend to prevent NetworkOnMainThreadException
    suspend fun parsePlaylist(url: String, headers: Map<String, String>): List<HlsSegment> {
        var currentM3u8Url = url
        var m3u8Content = fetchString(currentM3u8Url, headers)

        // 1. Handle Master Playlist -> Find first Media Playlist
        if (m3u8Content.contains("#EXT-X-STREAM-INF")) {
            val lines = m3u8Content.split("\n").map { it.trim() }
            var foundMediaUrl = ""
            for (i in lines.indices) {
                if (lines[i].startsWith("#EXT-X-STREAM-INF")) {
                    for (j in i + 1 until lines.size) {
                        if (lines[j].isNotEmpty() && !lines[j].startsWith("#")) {
                            foundMediaUrl = resolveUrl(currentM3u8Url, lines[j])
                            break
                        }
                    }
                    if (foundMediaUrl.isNotEmpty()) break
                }
            }
            if (foundMediaUrl.isNotEmpty()) {
                currentM3u8Url = foundMediaUrl
                m3u8Content = fetchString(currentM3u8Url, headers)
            } else {
                throw Exception("Could not find media playlist in master m3u8")
            }
        }

        // 2. Parse Media Playlist for TS segments and AES-128 Encryption info
        val lines = m3u8Content.split("\n").map { it.trim() }
        val segments = mutableListOf<HlsSegment>()

        var currentKeyUrl: String? = null
        var currentIvHex: String? = null
        var segmentIndex = 0
        var mediaSequence = 0
        var currentDurationMs = 0L

        for (line in lines) {
            if (line.startsWith("#EXT-X-MEDIA-SEQUENCE:")) {
                mediaSequence = line.removePrefix("#EXT-X-MEDIA-SEQUENCE:").trim().toIntOrNull() ?: 0
            } else if (line.startsWith("#EXT-X-MAP:")) {
                val mapAttr = parseKeyAttributes(line.removePrefix("#EXT-X-MAP:"))
                var rawUri = mapAttr["URI"] ?: ""
                if (rawUri.startsWith("\"") && rawUri.endsWith("\"")) {
                    rawUri = rawUri.substring(1, rawUri.length - 1)
                }
                
                if (rawUri.isNotEmpty()) {
                    val absoluteUrl = resolveUrl(currentM3u8Url, rawUri)
                    segments.add(
                        HlsSegment(
                            index = segmentIndex,
                            url = absoluteUrl,
                            durationMs = 0L,
                            keyUrl = null,
                            ivHex = null,
                            sequenceNumber = mediaSequence + segmentIndex
                        )
                    )
                    segmentIndex++
                }
            } else if (line.startsWith("#EXTINF:")) {
                val durSecStr = line.removePrefix("#EXTINF:").substringBefore(",").trim()
                val durSec = durSecStr.toDoubleOrNull() ?: 0.0
                currentDurationMs = (durSec * 1000).toLong()
            } else if (line.startsWith("#EXT-X-KEY:")) {
                val keyAttrMap = parseKeyAttributes(line.removePrefix("#EXT-X-KEY:"))
                val method = keyAttrMap["METHOD"] ?: "NONE"

                if (method == "AES-128") {
                    var rawUri = keyAttrMap["URI"] ?: ""
                    if (rawUri.startsWith("\"") && rawUri.endsWith("\"")) {
                        // FIX: Added the missing minus sign
                        rawUri = rawUri.substring(1, rawUri.length - 1)
                    }
                    currentKeyUrl = resolveUrl(currentM3u8Url, rawUri)
                    currentIvHex = keyAttrMap["IV"]
                } else if (method == "NONE") {
                    currentKeyUrl = null
                    currentIvHex = null
                }
            } else if (line.isNotEmpty() && !line.startsWith("#")) {
                val absoluteUrl = resolveUrl(currentM3u8Url, line)
                val seqNum = mediaSequence + segmentIndex

                segments.add(
                    HlsSegment(
                        index = segmentIndex,
                        url = absoluteUrl,
                        durationMs = currentDurationMs,
                        keyUrl = currentKeyUrl,
                        ivHex = currentIvHex,
                        sequenceNumber = seqNum
                    )
                )
                segmentIndex++
                currentDurationMs = 0L
            }
        }

        return segments
    }

    private fun parseKeyAttributes(attrString: String): Map<String, String> {
        val map = HashMap<String, String>()
        val regex = Regex("([A-Z0-9-]+)=([^,\"]+|\"[^\"]*\")")
        regex.findAll(attrString).forEach { match ->
            val (key, value) = match.destructured
            map[key] = value.trim()
        }
        return map
    }

    // FIX: Safely merge query parameters using OkHttp's HttpUrl builder
    private fun resolveUrl(baseUrlStr: String, relativeUrlStr: String): String {
        val baseUrl = baseUrlStr.toHttpUrlOrNull() ?: return relativeUrlStr

        // If it's already an absolute URL, just return it
        if (relativeUrlStr.startsWith("http://") || relativeUrlStr.startsWith("https://")) {
            return relativeUrlStr
        }

        val resolvedUrl = baseUrl.resolve(relativeUrlStr) ?: return relativeUrlStr
        val resolvedBuilder = resolvedUrl.newBuilder()

        // Safely forward base URL query parameters (like ?token=xxx) without destroying
        // existing parameters that might be on the relative URL.
        for (i in 0 until baseUrl.querySize) {
            val queryName = baseUrl.queryParameterName(i)
            // Only add the token if the relative URL didn't specifically override it
            if (resolvedUrl.queryParameter(queryName) == null) {
                resolvedBuilder.addQueryParameter(queryName, baseUrl.queryParameterValue(i))
            }
        }

        return resolvedBuilder.build().toString()
    }

    // FIX: Wrapped in withContext(Dispatchers.IO) to ensure thread safety
    private suspend fun fetchString(url: String, headers: Map<String, String>): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).apply {
            headers.forEach { (k, v) -> addHeader(k, v) }
        }.build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Failed to fetch M3U8 from $url (HTTP ${response.code})")
            response.body?.string() ?: throw Exception("Empty M3U8 playlist response")
        }
    }
}