package com.ocarina.music.playback

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
 * Integração não oficial com vídeos públicos do YouTube. Os links são temporários.
 * Não armazena credenciais nem implementa bypass de DRM ou restrições de idade.
 */
object AudioSourceResolver {
    private val downloader = OcarinaDownloader()
    private var initialized = false

    @Synchronized
    private fun prepare() {
        if (!initialized) {
            NewPipe.init(downloader)
            initialized = true
        }
    }

    suspend fun resolve(videoId: String): String = withContext(Dispatchers.IO) {
        require(videoId.matches(Regex("[A-Za-z0-9_-]{11}"))) { "Identificador de música inválido." }
        prepare()
        val extractor = NewPipe.getService("YouTube")
            .getStreamExtractor("https://www.youtube.com/watch?v=$videoId")
        extractor.fetchPage()
        val stream = extractor.audioStreams
            .filter { it.isUrl && it.content?.startsWith("https://") == true }
            .maxByOrNull { it.bitrate }
            ?: throw IOException("Esta música não tem um fluxo de áudio disponível nesta integração.")
        stream.content ?: throw IOException("A fonte retornou um endereço vazio.")
    }
}

/** Adaptador HTTP necessário para o NewPipeExtractor, sem cookies nem conta pessoal. */
private class OcarinaDownloader : Downloader() {
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
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
            return Response(
                response.code,
                response.message,
                response.headers.toMultimap(),
                response.body?.string() ?: "",
                response.request.url.toString()
            )
        }
    }
}
