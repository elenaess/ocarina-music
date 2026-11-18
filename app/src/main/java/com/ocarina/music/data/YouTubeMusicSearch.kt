package com.ocarina.music.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * EXPERIMENTAL catalog integration.
 * WEB_REMIX is an undocumented YouTube Music client. It may stop working at any time.
 * No user credentials are used or stored; playback uses an embedded YouTube player.
 */
class YouTubeMusicSearch {
    private val http = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .build()

    private var cachedApiKey: String? = null
    private var cachedVersion: String? = null

    suspend fun search(query: String): List<Song> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val (key, version) = readPublicClientConfig()
        val client = JSONObject().apply {
            put("clientName", "WEB_REMIX")
            put("clientVersion", version)
            put("hl", "pt")
            put("gl", "BR")
        }
        val payload = JSONObject().apply {
            put("context", JSONObject().put("client", client))
            put("query", query.trim())
        }
        val url = buildString {
            append("https://music.youtube.com/youtubei/v1/search?prettyPrint=false")
            if (!key.isNullOrBlank()) append("&key=$key")
        }
        val request = Request.Builder().url(url)
            .header("Origin", "https://music.youtube.com")
            .header("Referer", "https://music.youtube.com/")
            .header("User-Agent", USER_AGENT)
            .header("X-YouTube-Client-Name", "67")
            .header("X-YouTube-Client-Version", version)
            .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val data = http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("O catálogo não respondeu (${response.code}). A integração pode ter mudado.")
            }
            JSONObject(response.body?.string() ?: throw IOException("Resposta vazia do catálogo."))
        }
        extractSongs(data)
    }

    private fun readPublicClientConfig(): Pair<String?, String> {
        if (cachedVersion != null) return cachedApiKey to cachedVersion!!
        var version = "1.20251001.01.00" // Fallback; dynamically discovered when available.
        try {
            val req = Request.Builder().url("https://music.youtube.com/")
                .header("User-Agent", USER_AGENT).get().build()
            http.newCall(req).execute().use { response ->
                if (response.isSuccessful) {
                    val html = response.body?.string().orEmpty()
                    Regex("""INNERTUBE_API_KEY[^:]{0,8}:\s*["']([^"']+)""")
                        .find(html)?.groupValues?.getOrNull(1)?.let { cachedApiKey = it }
                    Regex("""INNERTUBE_CONTEXT_CLIENT_VERSION[^:]{0,8}:\s*["']([^"']+)""")
                        .find(html)?.groupValues?.getOrNull(1)?.let { version = it }
                }
            }
        } catch (_: IOException) { /* The search request reports its own error. */ }
        cachedVersion = version
        return cachedApiKey to version
    }

    private fun extractSongs(root: JSONObject): List<Song> {
        val results = linkedMapOf<String, Song>()
        fun visit(value: Any?, depth: Int) {
            if (depth > 35 || results.size >= 36) return
            when (value) {
                is JSONObject -> {
                    value.optJSONObject("musicResponsiveListItemRenderer")?.let { row ->
                        parseSong(row)?.let { song -> results.putIfAbsent(song.videoId, song) }
                    }
                    val names = value.keys()
                    while (names.hasNext() && results.size < 36) {
                        visit(value.opt(names.next()), depth + 1)
                    }
                }
                is JSONArray -> for (i in 0 until value.length()) {
                    if (results.size >= 36) break
                    visit(value.opt(i), depth + 1)
                }
            }
        }
        visit(root, 0)
        return results.values.toList()
    }

    private fun parseSong(row: JSONObject): Song? {
        val id = row.optJSONObject("playlistItemData")?.optString("videoId")
            ?.takeIf { it.isNotBlank() }
            ?: findWatchId(row)
            ?: return null
        if (!id.matches(Regex("[A-Za-z0-9_-]{11}"))) return null
        val cols = row.optJSONArray("flexColumns")
        fun col(index: Int): String {
            val text = cols?.optJSONObject(index)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text") ?: return ""
            val runs = text.optJSONArray("runs")
            return runs?.optJSONObject(0)?.optString("text").orEmpty()
        }
        val title = col(0).ifBlank { return null }
        val artist = col(1).ifBlank { "YouTube Music" }
        val thumbs = row.optJSONObject("thumbnail")
            ?.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")
        val image = thumbs?.optJSONObject((thumbs.length() - 1).coerceAtLeast(0))
            ?.optString("url")?.takeIf { it.startsWith("https://") }
            ?: "https://i.ytimg.com/vi/$id/hqdefault.jpg"
        return Song(id, title, artist, image)
    }

    private fun findWatchId(value: Any?, depth: Int = 0): String? {
        if (depth > 12) return null
        if (value is JSONObject) {
            value.optJSONObject("watchEndpoint")?.optString("videoId")
                ?.takeIf { it.isNotEmpty() }?.let { return it }
            val keys = value.keys()
            while (keys.hasNext()) {
                findWatchId(value.opt(keys.next()), depth + 1)?.let { return it }
            }
        } else if (value is JSONArray) {
            for (i in 0 until value.length()) {
                findWatchId(value.opt(i), depth + 1)?.let { return it }
            }
        }
        return null
    }

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"
    }
}
