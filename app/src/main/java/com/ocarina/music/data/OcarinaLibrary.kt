package com.ocarina.music.data

import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.UUID

/** A playlist belongs to one account on this device. Private by default. */
data class OcarinaPlaylist(
    val id: String,
    val name: String,
    val cover: String = "",
    val songs: List<Song> = emptyList(),
)

internal fun songToJson(song: Song): JSONObject = JSONObject().apply {
    put("id", song.videoId)
    put("title", song.title)
    put("artist", song.artist)
    put("artwork", song.artwork)
    put("album", song.album)
}

internal fun songFromJson(item: JSONObject): Song? {
    val id = item.optString("id")
    if (id.isBlank()) return null
    return Song(id, item.optString("title", "Faixa"),
        item.optString("artist", "Artista"), item.optString("artwork"), item.optString("album"))
}

/** Local playlist storage. Never mixes guest and signed-in playlists. */
class OcarinaPlaylistStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("ocarina_playlists", Context.MODE_PRIVATE)
    private fun key(uid: String?) = "playlists_" + (uid ?: "guest")

    fun load(uid: String?): List<OcarinaPlaylist> = try {
        val raw = JSONArray(prefs.getString(key(uid), "[]") ?: "[]")
        (0 until raw.length()).mapNotNull { index ->
            val item = raw.optJSONObject(index) ?: return@mapNotNull null
            val id = item.optString("id")
            if (id.isBlank()) return@mapNotNull null
            val tracks = item.optJSONArray("songs") ?: JSONArray()
            OcarinaPlaylist(id, item.optString("name", "Minha playlist"), item.optString("cover"),
                (0 until tracks.length()).mapNotNull { tracks.optJSONObject(it)?.let(::songFromJson) })
        }
    } catch (_: Exception) { emptyList() }

    fun save(uid: String?, lists: List<OcarinaPlaylist>) {
        val array = JSONArray()
        lists.forEach { list ->
            array.put(JSONObject().apply {
                put("id", list.id)
                put("name", list.name.take(80))
                put("cover", list.cover)
                put("songs", JSONArray().apply { list.songs.forEach { put(songToJson(it)) } })
            })
        }
        prefs.edit().putString(key(uid), array.toString()).apply()
    }

    fun create(title: String): OcarinaPlaylist = OcarinaPlaylist(
        id = UUID.randomUUID().toString(), name = title.trim().take(80).ifBlank { "Nova playlist" }
    )

    /** Permanent private copy: gallery permission is not needed after picking a cover. */
    fun saveCover(id: String, uri: Uri): String {
        val dir = File(context.filesDir, "ocarina_playlist_covers").apply { mkdirs() }
        val output = File(dir, "${id.replace(Regex("[^a-zA-Z0-9-]"), "")}.jpg")
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Imagem inválida." }
        var sample = 1
        while (bounds.outWidth / sample > 1024 || bounds.outHeight / sample > 1024) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val source = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IOException("Não foi possível abrir a capa.")
        val side = 512
        val square = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(square)
        val scale = maxOf(side.toFloat() / source.width, side.toFloat() / source.height)
        val w = source.width * scale
        val h = source.height * scale
        canvas.drawBitmap(source, null,
            android.graphics.RectF((side - w) / 2, (side - h) / 2, (side + w) / 2, (side + h) / 2),
            android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG))
        output.outputStream().use { square.compress(Bitmap.CompressFormat.JPEG, 83, it) }
        source.recycle()
        square.recycle()
        return Uri.fromFile(output).toString()
    }
}

/** Offline playback only for audio chosen by the user through Android's file picker. */
class OcarinaOfflineStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("ocarina_offline_audio", Context.MODE_PRIVATE)
    private val directory = File(context.filesDir, "ocarina_offline_audio").apply { mkdirs() }

    fun load(): List<Song> = try {
        val arr = JSONArray(prefs.getString("tracks", "[]") ?: "[]")
        (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let(::songFromJson) }
            .filter { fileFor(it.videoId)?.isFile == true }
    } catch (_: Exception) { emptyList() }

    private fun save(songs: List<Song>) {
        val arr = JSONArray()
        songs.forEach { arr.put(songToJson(it)) }
        prefs.edit().putString("tracks", arr.toString()).apply()
    }

    /** IDs are generated locally; never concatenate untrusted paths. */
    fun fileFor(videoId: String): File? {
        if (!videoId.startsWith("local:")) return null
        val id = videoId.removePrefix("local:")
        if (!id.matches(Regex("[a-f0-9-]{36}"))) return null
        return directory.listFiles()?.firstOrNull { it.name.startsWith("$id.") }
    }

    fun uriFor(videoId: String): Uri? = fileFor(videoId)?.takeIf { it.isFile }?.let(Uri::fromFile)

    /** Copy user-owned audio into private app storage. Limits prevent accidental huge imports. */
    fun import(uris: List<Uri>): List<Song> {
        val result = mutableListOf<Song>()
        for (uri in uris.take(30)) {
            var target: File? = null
            try {
                val displayName = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME),
                    null, null, null)?.use { cursor: Cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                } ?: "Áudio"
                val extension = displayName.substringAfterLast('.', "").lowercase()
                    .takeIf { it in setOf("mp3", "m4a", "aac", "ogg", "opus", "flac", "wav", "mp4") }
                    ?: "mp3"
                val id = UUID.randomUUID().toString()
                val outFile = File(directory, "$id.$extension")
                target = outFile
                context.contentResolver.openInputStream(uri)?.use { input ->
                    outFile.outputStream().use { output ->
                        val buf = ByteArray(32_768)
                        var total = 0L
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            total += n
                            if (total > 150L * 1024 * 1024) throw IOException("Arquivo acima de 150 MB")
                            output.write(buf, 0, n)
                        }
                    }
                } ?: throw IOException("Arquivo inacessível")
                val metadata = MediaMetadataRetriever()
                var title: String? = null
                var artist: String? = null
                try {
                    metadata.setDataSource(outFile.absolutePath)
                    title = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    artist = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                } catch (_: Exception) { /* Use the file name. */ }
                finally { runCatching { metadata.release() } }
                result.add(Song("local:$id", title?.takeIf { it.isNotBlank() }
                    ?: displayName.substringBeforeLast('.').take(160),
                    artist?.takeIf { it.isNotBlank() } ?: "Áudio local", ""))
            } catch (_: Exception) {
                target?.delete() // Never persist a partial file.
            }
        }
        if (result.isNotEmpty()) save((result + load()).distinctBy { it.videoId })
        return result
    }

    /** Download an audio file the user is licensed/authorized to keep, from a direct HTTPS URL. */
    suspend fun downloadAuthorizedAudio(link: String): Song = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val url = Uri.parse(link.trim())
        val host = url.host.orEmpty().lowercase(java.util.Locale.ROOT)
        require(url.scheme == "https" && host.contains('.') &&
            host.matches(Regex("[a-z0-9.-]{4,253}")) &&
            !host.matches(Regex("[0-9.]+")) && !host.endsWith(".local") &&
            !host.endsWith(".internal") && !host.endsWith(".lan") &&
            !host.contains("youtube.com") && !host.contains("youtu.be") &&
            !host.contains("googlevideo.com")) {
            "Use um link HTTPS direto para um arquivo de áudio autorizado, não uma página de vídeo."
        }
        val client = okhttp3.OkHttpClient.Builder().followRedirects(false)
            .connectTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(25, java.util.concurrent.TimeUnit.SECONDS).build()
        val request = okhttp3.Request.Builder().url(link.trim()).get().build()
        var saved: File? = null
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Servidor respondeu ${response.code}.")
                val mime = response.header("Content-Type").orEmpty().substringBefore(';').lowercase()
                val suffix = url.lastPathSegment.orEmpty().substringAfterLast('.', "").lowercase()
                val known = suffix in setOf("mp3", "flac", "wav", "m4a", "aac", "ogg", "opus")
                if (!mime.startsWith("audio/") && !(mime == "application/octet-stream" && known))
                    throw IOException("O link não apontou para um arquivo de áudio compatível.")
                val extension = when {
                    known -> suffix
                    mime.contains("flac") -> "flac"
                    mime.contains("mpeg") -> "mp3"
                    mime.contains("wav") -> "wav"
                    mime.contains("ogg") -> "ogg"
                    mime.contains("aac") -> "aac"
                    else -> "m4a"
                }
                if ((response.body?.contentLength() ?: -1L) > 150L * 1024 * 1024)
                    throw IOException("Arquivo maior que 150 MB.")
                val id = UUID.randomUUID().toString()
                val target = File(directory, "$id.$extension")
                saved = target
                val input = response.body?.byteStream() ?: throw IOException("Resposta sem áudio.")
                input.use { stream -> target.outputStream().use { out ->
                    val buffer = ByteArray(32768)
                    var bytes = 0L
                    while (true) {
                        val read = stream.read(buffer)
                        if (read < 0) break
                        bytes += read
                        if (bytes > 150L * 1024 * 1024) throw IOException("Áudio acima de 150 MB.")
                        out.write(buffer, 0, read)
                    }
                    if (bytes == 0L) throw IOException("O arquivo estava vazio.")
                } }
                val metadata = MediaMetadataRetriever()
                var name: String? = null
                var artist: String? = null
                try {
                    metadata.setDataSource(target.absolutePath)
                    name = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    artist = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                } catch (_: Exception) { /* Fall back to filename. */ }
                finally { runCatching { metadata.release() } }
                val label = url.lastPathSegment.orEmpty().substringBeforeLast('.').ifBlank { "Áudio offline" }
                val song = Song("local:$id", name?.takeIf { it.isNotBlank() }?.take(160)
                    ?: Uri.decode(label).take(160), artist?.takeIf { it.isNotBlank() }?.take(120)
                    ?: "Arquivo autorizado", "")
                save(listOf(song) + load())
                song
            }
        } catch (e: Exception) {
            saved?.delete()
            throw e
        }
    }

    fun remove(song: Song) {
        if (!song.videoId.startsWith("local:")) return
        save(load().filterNot { it.videoId == song.videoId })
        fileFor(song.videoId)?.delete()
    }
}
