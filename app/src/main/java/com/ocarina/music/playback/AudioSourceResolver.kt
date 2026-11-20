package com.ocarina.music.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request as OkHttpRequest
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Audio is streamed as encoded by its source, never upscaled or transcoded.
 * bitrateKbps is an estimate reported by NewPipe, NOT a guarantee of perceived quality.
 */
data class ResolvedAudio(
    val url: String,
    val bitrateKbps: Int,
    val codec: String = "",
    val provider: String = "YouTube",
    val sampleRateHz: Int = 0,
    val bitDepth: Int = 0,
)

/** Public, unofficial catalog adapter. Signed URLs have short lifetimes. */
object AudioSourceResolver {
    private val downloader = OcarinaDownloader()
    private val work = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()
    private const val TTL_MS = 90_000L
    private var initialized = false
    private data class Entry(val source: ResolvedAudio, val storedAt: Long)
    private val cache = object : LinkedHashMap<String, Entry>(24, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry>): Boolean = size > 24
    }
    private val inFlight = mutableMapOf<String, Deferred<ResolvedAudio>>()

    @Synchronized private fun prepare() {
        if (!initialized) {
            NewPipe.init(downloader)
            initialized = true
        }
    }

    /** Concurrent requests for one song share a single extraction; different songs resolve in parallel. */
    suspend fun resolve(videoId: String, refresh: Boolean = false): ResolvedAudio {
        require(videoId.matches(Regex("[A-Za-z0-9_-]{11}"))) { "Identificador de música inválido." }
        val request: Deferred<ResolvedAudio>
        synchronized(lock) {
            if (refresh) cache.remove(videoId)
            if (!refresh) {
                val cached = cache[videoId]
                if (cached != null && System.currentTimeMillis() - cached.storedAt < TTL_MS) return cached.source
            }
            request = inFlight.getOrPut(videoId) { work.async { extract(videoId) } }
        }
        try {
            val resolved = request.await()
            synchronized(lock) { cache[videoId] = Entry(resolved, System.currentTimeMillis()) }
            return resolved
        } finally {
            synchronized(lock) { if (inFlight[videoId] === request) inFlight.remove(videoId) }
        }
    }

    private fun extract(videoId: String): ResolvedAudio {
        prepare()
        val extractor = NewPipe.getService("YouTube")
            .getStreamExtractor("https://www.youtube.com/watch?v=$videoId")
        extractor.fetchPage()
        // Select the best real encoded audio stream exposed for this item.
        val stream = extractor.audioStreams
            .asSequence()
            .filter { it.isUrl && it.content?.startsWith("https://") == true }
            .maxByOrNull { it.bitrate }
            ?: throw IOException("Nenhuma fonte de áudio disponível para esta música.")
        val url = stream.content ?: throw IOException("URL de áudio vazia.")
        // NewPipe's bitrate is usually bits/second; the UI label is kilobits/second.
        val bitrate = stream.bitrate.coerceAtLeast(0)
        return ResolvedAudio(url, if (bitrate >= 1000) (bitrate / 1000) else bitrate)
    }
}

/** Shared lightweight HTTP client for the extractor, no account or restricted-media bypass. */
private class OcarinaDownloader : Downloader() {
    private val http = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(16, TimeUnit.SECONDS)
        .connectionPool(okhttp3.ConnectionPool(5, 5, TimeUnit.MINUTES))
        .followRedirects(true)
        .build()

    override fun execute(request: Request): Response {
        val builder = OkHttpRequest.Builder()
            .url(request.url())
            .header("User-Agent", "Mozilla/5.0 (Android) AppleWebKit/537.36 Chrome/126.0.0.0 Mobile Safari/537.36")
        request.headers()?.forEach { (key, values) -> values.forEach { builder.addHeader(key, it) } }
        val data = request.dataToSend()
        val method = request.httpMethod()
        val body = when {
            data != null -> data.toRequestBody("application/octet-stream".toMediaTypeOrNull())
            method == "POST" || method == "PUT" -> ByteArray(0).toRequestBody(null)
            else -> null
        }
        http.newCall(builder.method(method, body).build()).execute().use { response ->
            return Response(response.code, response.message, response.headers.toMultimap(),
                response.body?.string() ?: "", response.request.url.toString())
        }
    }
}
