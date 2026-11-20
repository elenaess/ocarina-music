package com.ocarina.music.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.ocarina.music.data.Song
import com.ocarina.music.data.OcarinaOfflineStore
import com.ocarina.music.data.OcarinaSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Estado nativo do player consumido pelas telas Compose. */
class PlaybackConnector(context: Context) {
    private val appContext = context.applicationContext
    var preferFlac by mutableStateOf(appContext.getSharedPreferences(
        "ocarina_preferences", Context.MODE_PRIVATE).getBoolean("prefer_lossless_v055", true))
        private set

    fun toggleAudioQuality() {
        preferFlac = !preferFlac
        appContext.getSharedPreferences("ocarina_preferences", Context.MODE_PRIVATE)
            .edit().putBoolean("prefer_lossless_v055", preferFlac).apply()
        currentSong?.takeUnless { it.videoId.startsWith("local:") }
            ?.let { play(it, refresh = true) }
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val connected = CompletableDeferred<MediaController>()
    private var resolving: Job? = null
    private var retryingSource = false
    private var playSerial = 0L
    private var endedForSerial = -1L
    var onTrackEnded: ((Song) -> Unit)? = null
    private val offline = OcarinaOfflineStore(appContext)
    private val settings = OcarinaSettings(appContext)
    var repeatMode by mutableIntStateOf(settings.repeatMode())
        private set

    fun cycleRepeat() {
        repeatMode = (repeatMode + 1) % 3
        settings.setRepeatMode(repeatMode)
        controller?.repeatMode = if (repeatMode == 1) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    var currentSong by mutableStateOf<Song?>(null)
        private set
    var playing by mutableStateOf(false)
        private set
    var buffering by mutableStateOf(false)
        private set
    var resolvingAudio by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var positionMs by mutableLongStateOf(0L)
        private set
    var durationMs by mutableLongStateOf(0L)
        private set
    var sourceBitrateKbps by mutableLongStateOf(0L)
        private set
    var sourceCodec by mutableStateOf("")
        private set
    var sourceSampleRateHz by mutableIntStateOf(0)
        private set
    var sourceBitDepth by mutableIntStateOf(0)
        private set
    var sourceProvider by mutableStateOf("")
        private set
    val qualityDescription: String
        get() = when {
            sourceCodec == "FLAC" && sourceBitDepth > 0 && sourceSampleRateHz > 0 ->
                "FLAC • ${sourceBitDepth}-bit/${sourceSampleRateHz / 1000.0} kHz"
            sourceCodec == "FLAC" -> "FLAC • Lossless"
            sourceCodec == "MP3" && sourceBitrateKbps >= 320L -> "MP3 • ${sourceBitrateKbps} kbps"
            sourceBitrateKbps > 0 -> "~${sourceBitrateKbps} kbps"
            else -> if (preferFlac) "HQ • Auto" else "Padrão"
        }

    private val token = SessionToken(appContext, ComponentName(appContext, OcarinaPlaybackService::class.java))
    private val future = MediaController.Builder(appContext, token).buildAsync()
    private var controller: MediaController? = null
    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            sync(player)
            if (player.playbackState == Player.STATE_ENDED && repeatMode != 1 &&
                controller?.currentMediaItem?.mediaId == currentSong?.videoId &&
                endedForSerial != playSerial) {
                endedForSerial = playSerial
                val endedSong = currentSong
                if (endedSong != null) scope.launch { onTrackEnded?.invoke(endedSong) }
            }
        }
        override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
            for (group in tracks.groups) {
                if (group.type != C.TRACK_TYPE_AUDIO) continue
                for (i in 0 until group.length) {
                    if (!group.isTrackSelected(i)) continue
                    val format = group.getTrackFormat(i)
                    if (format.bitrate > 0) sourceBitrateKbps = format.bitrate.toLong() / 1000
                    when (format.sampleMimeType) {
                        "audio/flac", "audio/x-flac" -> sourceCodec = "FLAC"
                        "audio/mpeg" -> sourceCodec = "MP3"
                        "audio/opus" -> sourceCodec = "OPUS"
                        "audio/mp4a-latm" -> sourceCodec = "AAC"
                    }
                    if (format.sampleRate > 0) sourceSampleRateHz = format.sampleRate
                    currentSong?.let { song ->
                        HighQualitySourceResolver.notePlayableFormat(
                            song.videoId, format.sampleMimeType, format.bitrate)
                    }
                    break
                }
            }
        }
        override fun onPlayerError(error: PlaybackException) {
            Log.e("OcarinaPlayback", "Falha Media3 na faixa ${currentSong?.videoId}", error)
            if (resolvingAudio || controller?.currentMediaItem?.mediaId != currentSong?.videoId) return
            if (sourceCodec == "FLAC") currentSong?.let(HighQualitySourceResolver::markUnavailable)
            // Expiring temporary URLs occasionally return 403. Retry the current track once.
            if (!retryingSource && currentSong != null &&
                controller?.currentMediaItem?.mediaId == currentSong?.videoId &&
                (error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ||
                    error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                    sourceCodec == "FLAC")) {
                retryingSource = true
                // Schedule outside Media3's callback, avoiding reentrant player mutation.
                val failed = currentSong!!
                scope.launch { play(failed, refresh = true, isRetry = true) }
            } else {
                this@PlaybackConnector.error = "Não foi possível reproduzir: ${error.errorCodeName}."
                buffering = false
            }
        }
    }

    init {
        future.addListener({
            try {
                val ready = future.get()
                controller = ready
                ready.addListener(listener)
                ready.repeatMode = if (repeatMode == 1) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                sync(ready)
                connected.complete(ready)
            } catch (e: Exception) {
                error = "Não foi possível iniciar o serviço de música: ${e.message}"
                connected.completeExceptionally(e)
            }
        }, ContextCompat.getMainExecutor(appContext))
        scope.launch {
            while (isActive) {
                controller?.let {
                    positionMs = it.currentPosition.coerceAtLeast(0)
                    durationMs = it.duration.takeIf { d -> d != C.TIME_UNSET && d > 0 } ?: 0L
                }
                delay(800)
            }
        }
    }

    private fun sync(player: Player) {
        val item = player.currentMediaItem
        if (item?.mediaId != currentSong?.videoId) {
            playing = false
            buffering = resolvingAudio
            return
        }
        playing = player.isPlaying
        buffering = player.playbackState == Player.STATE_BUFFERING
        // Do not let events from the OLD media item overwrite a freshly selected search result.
        if (item != null && !resolvingAudio && item.mediaId == currentSong?.videoId) {
            val md = item.mediaMetadata
            currentSong = Song(
                item.mediaId,
                md.title?.toString() ?: "Música",
                md.artist?.toString() ?: "Artista",
                md.artworkUri?.toString() ?: "",
                currentSong?.album.orEmpty()
            )
        }
    }

    fun play(song: Song, next: Song? = null, refresh: Boolean = false, isRetry: Boolean = false) {
        // Re-selecting the active track should not re-fetch the same signed URL.
        if (!refresh && !resolvingAudio && controller?.currentMediaItem?.mediaId == song.videoId &&
            controller?.playbackState == Player.STATE_READY) {
            controller?.play()
            return
        }
        resolving?.cancel()
        val thisRequest = ++playSerial
        endedForSerial = -1L
        if (!isRetry) retryingSource = false
        resolvingAudio = true
        // Only pause the previous track: stop()/clearMediaItems() cause extra MediaSession
        // transitions and can synchronously restore stale metadata into Compose.
        controller?.pause()
        controller?.clearMediaItems()
        currentSong = song
        sourceBitrateKbps = 0L
        sourceCodec = ""
        sourceSampleRateHz = 0
        sourceBitDepth = 0
        sourceProvider = ""
        error = null
        resolving = scope.launch {
            try {
                val localUri = if (song.videoId.startsWith("local:")) offline.uriFor(song.videoId) else null
                val source = if (song.videoId.startsWith("local:")) {
                    if (localUri == null) throw java.io.IOException("Arquivo local não encontrado.")
                    ResolvedAudio(localUri.toString(), 0)
                } else HighQualitySourceResolver.resolve(song, preferFlac = preferFlac, refresh = refresh)
                val c = connected.await()
                if (thisRequest != playSerial) return@launch
                sourceBitrateKbps = source.bitrateKbps.toLong()
                sourceCodec = source.codec
                sourceSampleRateHz = source.sampleRateHz
                sourceBitDepth = source.bitDepth
                sourceProvider = source.provider
                val item = MediaItem.Builder()
                    .setMediaId(song.videoId)
                    .setUri(source.url)
                    .setMediaMetadata(MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setArtworkUri(song.artwork.takeIf { it.startsWith("https://") }?.let(Uri::parse))
                        .build())
                    .build()
                c.setMediaItem(item)
                c.prepare()
                c.play()
                // Pré-extração desativada até estabilizar o player.
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (thisRequest != playSerial) return@launch
                Log.e("OcarinaPlayback", "Falha ao resolver ${song.videoId}", e)
                error = "Não foi possível carregar '${song.title}'. ${e.message ?: "Fonte temporariamente indisponível."}"
                buffering = false
            } finally {
                if (thisRequest == playSerial) resolvingAudio = false
            }
        }
    }

    /** Release the former account's playback when authentication ends. */
    fun resetForLogout() {
        playSerial++
        resolving?.cancel()
        resolvingAudio = false
        currentSong = null
        playing = false
        buffering = false
        positionMs = 0L
        durationMs = 0L
        sourceBitrateKbps = 0L
        sourceCodec = ""
        sourceSampleRateHz = 0
        sourceBitDepth = 0
        sourceProvider = ""
        error = null
        controller?.stop()
        controller?.clearMediaItems()
    }

    fun retryCurrent() {
        currentSong?.let { song -> play(song, refresh = true) }
    }
    fun toggle() {
        if (resolvingAudio) return
        val active = controller?.currentMediaItem?.mediaId
        // Never resume the PREVIOUS song if a newly selected result failed to resolve.
        if (active == null || active != currentSong?.videoId || error != null) {
            retryCurrent()
            return
        }
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }
    fun seekTo(ms: Long) {
        if (controller?.currentMediaItem?.mediaId == currentSong?.videoId) {
            controller?.seekTo(ms.coerceAtLeast(0L))
        }
    }
    fun clearError() { error = null }
    fun close() {
        resolving?.cancel()
        onTrackEnded = null
        controller?.removeListener(listener)
        MediaController.releaseFuture(future)
        scope.cancel()
    }
}
