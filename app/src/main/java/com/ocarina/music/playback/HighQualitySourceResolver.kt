package com.ocarina.music.playback

import com.ocarina.music.data.Song
import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.Semaphore
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Experimental, no-login FLAC provider for publicly reachable tracks.
 * The app does not decrypt DRM, impersonate user accounts, download media,
 * or pretend to convert a lossy stream into a lossless recording.
 * Availability and rights to stream a recording depend on the external source.
 */
internal object HighQualitySourceResolver {
    private const val HOST = "https://tracks.monochrome.st"
    private const val RESOLUTION_TIMEOUT_MS = 5_500L
    private const val SUCCESS_TTL_MS = 10 * 60 * 1000L
    private const val MISS_TTL_MS = 90 * 1000L
    private val http = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .callTimeout(5, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private data class Cached(val audio: ResolvedAudio?, val createdAt: Long)
    private val lock = Any()
    // Observable single source of truth for the search UI and playback confirmation.
    // FLAC? = ranged FLAC header only; FLAC = Media3 reached READY with FLAC selected.
    val qualityLabels = mutableStateMapOf<String, String>()
    private val previewAt = mutableMapOf<String, Long>()
    private val unavailableUntil = mutableMapOf<String, Long>()
    // Only two simultaneous network checks, never one request per whole search result page.
    private val badgeGate = Semaphore(4)
    private val confirmedPlayback = object : LinkedHashMap<String, Pair<String, Long>>(40, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Pair<String, Long>>) = size > 40
    }

    /** Called ONLY after Media3 has a selected audio track and reached STATE_READY. */
    fun noteReadyFormat(videoId: String, selectedCodec: String, mime: String?, bitrateBitsPerSecond: Int) {
        val label = when {
            selectedCodec == "FLAC" && mime in setOf("audio/flac", "audio/x-flac") -> "FLAC"
            mime in setOf("audio/mpeg", "audio/mp4a-latm", "audio/opus") &&
                bitrateBitsPerSecond >= 320_000 -> "HQ"
            else -> null
        }
        synchronized(lock) {
            if (label == null) {
                confirmedPlayback.remove(videoId)
                qualityLabels.remove(videoId)
                previewAt.remove(videoId)
            } else {
                confirmedPlayback[videoId] = label to System.currentTimeMillis()
                qualityLabels[videoId] = label
                previewAt.remove(videoId)
            }
        }
    }

    /** A ranged FLAC header is a *candidate*, not a successful playback. */
    suspend fun probeQualityBadge(song: Song): String? {
        if (song.category != "music" || song.videoId.startsWith("local:")) return null
        val key = keyFor(song)
        synchronized(lock) {
            val now = System.currentTimeMillis()
            if ((unavailableUntil[key] ?: 0L) > now) {
                qualityLabels.remove(song.videoId)
                return null
            }
            confirmedPlayback[song.videoId]?.let { (label, checked) ->
                if (now - checked < 10 * 60_000L) return label
                confirmedPlayback.remove(song.videoId)
            }
            if (qualityLabels[song.videoId] == "FLAC?" &&
                now - (previewAt[song.videoId] ?: 0L) < 90_000L) return "FLAC?"
            qualityLabels.remove(song.videoId)
            previewAt.remove(song.videoId)
        }
        return try {
            withTimeoutOrNull(4_500L) {
                badgeGate.acquire()
                try {
                    val found = withContext(Dispatchers.IO) { findVerifiedFlac(song, refresh = false) }
                    synchronized(lock) {
                        if ((unavailableUntil[key] ?: 0L) > System.currentTimeMillis()) {
                            qualityLabels.remove(song.videoId)
                            null
                        } else if (qualityLabels[song.videoId] in setOf("FLAC", "HQ")) {
                            qualityLabels[song.videoId]
                        } else if (found?.codec == "FLAC") {
                            qualityLabels[song.videoId] = "FLAC?"
                            previewAt[song.videoId] = System.currentTimeMillis()
                            "FLAC?"
                        } else null
                    }
                } finally {
                    badgeGate.release()
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) { null }
    }

    /** If Media3 cannot decode or stream a FLAC, temporarily fall back to NewPipe. */
    fun markUnavailable(song: Song) {
        synchronized(lock) {
            unavailableUntil[keyFor(song)] = System.currentTimeMillis() + 20 * 60_000L
            cache.remove(keyFor(song))
            confirmedPlayback.remove(song.videoId)
            previewAt.remove(song.videoId)
            qualityLabels.remove(song.videoId)
        }
    }
    private fun keyFor(song: Song): String =
        "${song.title.lowercase(Locale.ROOT)}|${song.artist.lowercase(Locale.ROOT)}"
    private val cache = object : LinkedHashMap<String, Cached>(64, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Cached>): Boolean = size > 64
    }

    suspend fun resolve(song: Song, preferFlac: Boolean, refresh: Boolean = false): ResolvedAudio {
        if (!preferFlac || song.videoId.startsWith("local:")) {
            return AudioSourceResolver.resolve(song.videoId, refresh)
        }
        val candidate = try {
            withTimeoutOrNull(RESOLUTION_TIMEOUT_MS) {
                withContext(Dispatchers.IO) { findVerifiedFlac(song, refresh) }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
        return candidate ?: AudioSourceResolver.resolve(song.videoId, refresh)
    }

    private fun findVerifiedFlac(song: Song, refresh: Boolean): ResolvedAudio? {
        val key = keyFor(song)
        val now = System.currentTimeMillis()
        synchronized(lock) {
            if ((unavailableUntil[key] ?: 0L) > now) return null
            unavailableUntil.remove(key)
            if (refresh) cache.remove(key)
            val saved = cache[key]
            if (saved != null && now - saved.createdAt < if (saved.audio == null) MISS_TTL_MS else SUCCESS_TTL_MS) {
                return saved.audio
            }
        }
        val result = queryFlac(song)
        synchronized(lock) { cache[key] = Cached(result, System.currentTimeMillis()) }
        return result
    }

    private fun queryFlac(song: Song): ResolvedAudio? {
        if (song.title.isBlank() || song.artist.isBlank() || song.artist.equals("Artista", true)) return null
        val guesses = listOf(
            "${song.title} ${song.artist}",
            "${song.title.substringAfterLast(" - ")} ${song.artist}",
        ).distinct().take(2)
        for (query in guesses) {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val request = Request.Builder().url("$HOST/search/tracks?q=$encoded&limit=20")
                .get().header("Accept", "application/json").build()
            val response = http.newCall(request).execute().use { res ->
                if (!res.isSuccessful) null else res.body?.string()
            } ?: continue
            val tracks = JSONObject(response).optJSONArray("tracks") ?: continue
            for (index in 0 until tracks.length()) {
                val track = tracks.optJSONObject(index) ?: continue
                if (!track.optBoolean("playable", true)) continue
                val id = track.optString("id")
                if (!id.matches(Regex("[0-9]{1,20}"))) continue
                if (!matches(song, track)) continue
                val url = "$HOST/track/$id"
                // Probe the original file header; never trust the provider's label alone.
                val meta = flacHeader(url) ?: continue
                return ResolvedAudio(url, 0, "FLAC", "Monochrome", meta.first, meta.second)
            }
        }
        return null
    }

    private fun matches(song: Song, item: JSONObject): Boolean {
        val foundTitle = item.optString("title")
        val wanted = listOf(song.title, song.title.substringAfterLast(" - "))
            .map(::normalizedTitle).filter { it.isNotBlank() }.distinct()
        val actualTitle = normalizedTitle(foundTitle)
        if (actualTitle !in wanted) return false
        // Do not substitute covers, instrumental mixes, or a live version for the original.
        val versionWords = setOf("remix", "live", "karaoke", "instrumental", "acoustic", "cover", "slowed")
        if (versionWords.any { tag -> (tag in actualTitle.split(' ')) != wanted.any { tag in it.split(' ') } }) return false
        val artist = simplified(song.artist)
        if (artist.isBlank() || artist == "various artists") return false
        val names = item.optJSONArray("artistNames") ?: return false
        return (0 until names.length()).any { i ->
            val actual = simplified(names.optString(i))
            actual.isNotBlank() && (actual == artist || artist.startsWith("$actual ") || actual.startsWith("$artist "))
        }
    }

    private fun normalizedTitle(raw: String): String {
        val withoutBrackets = raw.replace(Regex("(?i)\\s*[\\[(](official (music )?video|official audio|lyrics?|visualizer)[\\])]"), "")
        return simplified(withoutBrackets)
    }

    private fun simplified(raw: String): String {
        val latin = Normalizer.normalize(raw.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
        return latin.replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim().replace(Regex("\\s+"), " ")
    }

    /** FLAC stream metadata from the STREAMINFO block (not an assumed 1411 kbps). */
    private fun flacHeader(url: String): Pair<Int, Int>? {
        val req = Request.Builder().url(url).get().header("Range", "bytes=0-41")
            .header("User-Agent", "Ocarina/0.5.5 (Android)")
            .header("Accept-Encoding", "identity").build()
        return http.newCall(req).execute().use { response ->
            // An HTTP 200 full-body reply to a Range request is not verified seekable.
            if (response.code != 206 ||
                !response.header("Content-Range").orEmpty().startsWith("bytes 0-") ||
                response.header("Content-Encoding")?.lowercase(Locale.ROOT) == "gzip") return@use null
            val input = response.body?.byteStream() ?: return@use null
            val bytes = ByteArray(42)
            var read = 0
            while (read < bytes.size) {
                val next = input.read(bytes, read, bytes.size - read)
                if (next <= 0) break
                read += next
            }
            if (read < 22 || bytes.copyOfRange(0, 4).toString(Charsets.US_ASCII) != "fLaC") return@use null
            // Header: magic (4), metadata block header (4), STREAMINFO (34).
            if ((bytes[4].toInt() and 0x7f) != 0) return@use null
            val sampleRate = ((bytes[18].toInt() and 0xff) shl 12) or
                ((bytes[19].toInt() and 0xff) shl 4) or
                ((bytes[20].toInt() and 0xf0) ushr 4)
            val bitDepth = (((bytes[20].toInt() and 0x01) shl 4) or
                ((bytes[21].toInt() and 0xf0) ushr 4)) + 1
            if (sampleRate !in 8_000..384_000 || bitDepth !in 8..32) return@use null
            sampleRate to bitDepth
        }
    }
}
