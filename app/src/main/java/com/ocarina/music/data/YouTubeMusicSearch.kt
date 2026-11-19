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

enum class CatalogKind { ARTIST, ALBUM }

data class CatalogEntry(
    val browseId: String,
    val name: String,
    val subtitle: String,
    val image: String,
    val kind: CatalogKind,
)

/**
 * EXPERIMENTAL catalog integration.
 * WEB_REMIX is an undocumented YouTube Music client. It may stop working at any time.
 * No user credentials are used or stored; playback uses the native Media3 player.
 */
class YouTubeMusicSearch {
    private val http = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .build()

    private var cachedApiKey: String? = null
    private var cachedVersion: String? = null
    private val searchCache = object : LinkedHashMap<String, Pair<Long, List<Song>>>(12, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Pair<Long, List<Song>>>): Boolean = size > 12
    }

    suspend fun search(query: String): List<Song> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val cacheKey = query.trim().lowercase(java.util.Locale.ROOT)
        synchronized(searchCache) {
            searchCache[cacheKey]?.let { (stored, results) ->
                if (System.currentTimeMillis() - stored < 120_000L) return@withContext results
            }
        }
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
        val songs = extractSongs(data)
        synchronized(searchCache) { searchCache[cacheKey] = System.currentTimeMillis() to songs }
        songs.sortedWith(compareBy<Song> { when (it.category) {
            "music" -> 0; "video" -> 1; "podcast" -> 2; "live" -> 3; else -> 4
        } }.thenBy { it.title })
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
        // YouTube Music commonly prefixes artist runs with “Música ·”. Skip type labels.
        val artistRuns = cols?.optJSONObject(1)
            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")?.optJSONArray("runs")
        val artist = (0 until (artistRuns?.length() ?: 0))
            .mapNotNull { artistRuns?.optJSONObject(it)?.optString("text") }
            .map { it.trim() }
            .firstOrNull { value ->
                value.isNotBlank() && value !in setOf("Música", "Music", "Video", "Vídeo", "Song", "•", "·") &&
                    !value.all { it == '•' || it == '·' || it.isWhitespace() }
            } ?: col(1).ifBlank { "Artista" }
        val thumbs = row.optJSONObject("thumbnail")
            ?.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")
        val image = thumbs?.optJSONObject((thumbs.length() - 1).coerceAtLeast(0))
            ?.optString("url")?.takeIf { it.startsWith("https://") }
            ?: "https://i.ytimg.com/vi/$id/hqdefault.jpg"
        // Source-provided type markers, not guesses based on the song title.
        val typeMarkers = (0 until (artistRuns?.length() ?: 0))
            .mapNotNull { artistRuns?.optJSONObject(it)?.optString("text") }
            .map { it.trim().lowercase(java.util.Locale.ROOT) }
        val category = when {
            typeMarkers.any { it == "podcast" || it == "podcasts" } -> "podcast"
            typeMarkers.any { it == "ao vivo" || it == "live" || it == "live stream" } -> "live"
            typeMarkers.any { it == "vídeo" || it == "video" || it == "videos" } -> "video"
            else -> "music"
        }
        return Song(id, title, artist, image, category = category)
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

    /** Search real artist/album browse endpoints, not guessed album names. */
    suspend fun findCollections(query: String, kind: CatalogKind): List<CatalogEntry> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val root = musicRequest("search", "query", query.trim())
        val found = linkedMapOf<String, CatalogEntry>()
        fun visit(value: Any?, depth: Int) {
            if (depth > 35 || found.size >= 24) return
            when (value) {
                is JSONObject -> {
                    for (rendererName in listOf("musicResponsiveListItemRenderer", "musicTwoRowItemRenderer")) {
                        value.optJSONObject(rendererName)?.let { row ->
                            val target = findCatalogBrowse(row, kind)
                            if (target != null) {
                                val browseId = target.optString("browseId")
                                val title = if (rendererName == "musicTwoRowItemRenderer")
                                    row.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                                else row.optJSONArray("flexColumns")?.optJSONObject(0)
                                    ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                                    ?.optJSONObject("text")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                                val name = title.orEmpty().trim()
                                if (browseId.isNotBlank() && name.isNotBlank()) {
                                    found.putIfAbsent(browseId, CatalogEntry(browseId, name,
                                        if (kind == CatalogKind.ALBUM) "Álbum" else "Artista",
                                        findThumbnail(row), kind))
                                }
                            }
                        }
                    }
                    val keys = value.keys()
                    while (keys.hasNext() && found.size < 24) visit(value.opt(keys.next()), depth + 1)
                }
                is JSONArray -> for (i in 0 until value.length()) {
                    if (found.size >= 24) break
                    visit(value.opt(i), depth + 1)
                }
            }
        }
        visit(root, 0)
        found.values.toList()
    }

    /** Public album/artist detail; song parser uses existing safe video ID validation. */
    suspend fun browseSongs(browseId: String): List<Song> = withContext(Dispatchers.IO) {
        require(browseId.length in 3..150 && browseId.matches(Regex("[A-Za-z0-9_-]+")))
        extractSongs(musicRequest("browse", "browseId", browseId))
    }

    private fun musicRequest(endpoint: String, field: String, value: String): JSONObject {
        val (key, version) = readPublicClientConfig()
        val client = JSONObject().apply {
            put("clientName", "WEB_REMIX")
            put("clientVersion", version)
            put("hl", "pt")
            put("gl", "BR")
        }
        val payload = JSONObject().apply {
            put("context", JSONObject().put("client", client))
            put(field, value)
        }
        val url = buildString {
            append("https://music.youtube.com/youtubei/v1/$endpoint?prettyPrint=false")
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
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("O catálogo respondeu ${response.code}.")
            return JSONObject(response.body?.string() ?: throw IOException("Resposta vazia."))
        }
    }

    private fun findCatalogBrowse(row: JSONObject, kind: CatalogKind): JSONObject? {
        fun visit(value: Any?, depth: Int): JSONObject? {
            if (depth > 16) return null
            when (value) {
                is JSONObject -> {
                    value.optJSONObject("browseEndpoint")?.let { browse ->
                        val id = browse.optString("browseId")
                        val page = browse.optJSONObject("browseEndpointContextSupportedConfigs")
                            ?.optJSONObject("browseEndpointContextMusicConfig")?.optString("pageType")
                        val match = when (kind) {
                            CatalogKind.ARTIST -> page == "MUSIC_PAGE_TYPE_ARTIST" ||
                                (page.isNullOrBlank() && (id.startsWith("UC") || id.startsWith("MPLA")))
                            CatalogKind.ALBUM -> page == "MUSIC_PAGE_TYPE_ALBUM" ||
                                (page.isNullOrBlank() && id.startsWith("MPRE"))
                        }
                        if (match && id.isNotBlank()) return browse
                    }
                    val keys = value.keys()
                    while (keys.hasNext()) {
                        visit(value.opt(keys.next()), depth + 1)?.let { return it }
                    }
                }
                is JSONArray -> for (i in 0 until value.length()) {
                    visit(value.opt(i), depth + 1)?.let { return it }
                }
            }
            return null
        }
        return visit(row, 0)
    }

    private fun findThumbnail(root: JSONObject): String {
        fun visit(value: Any?, depth: Int): String? {
            if (depth > 14) return null
            when (value) {
                is JSONObject -> {
                    value.optJSONArray("thumbnails")?.let { list ->
                        val url = list.optJSONObject((list.length() - 1).coerceAtLeast(0))?.optString("url")
                        if (url?.startsWith("https://") == true) return url
                    }
                    val keys = value.keys()
                    while (keys.hasNext()) visit(value.opt(keys.next()), depth + 1)?.let { return it }
                }
                is JSONArray -> for (i in 0 until value.length()) {
                    visit(value.opt(i), depth + 1)?.let { return it }
                }
            }
            return null
        }
        return visit(root, 0).orEmpty()
    }

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"
    }
}
