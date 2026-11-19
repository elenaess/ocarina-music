package com.ocarina.music.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val connected = CompletableDeferred<MediaController>()
    private var resolving: Job? = null
    private var prefetchJob: Job? = null
    private var retryingSource = false
    private var playSerial = 0L

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

    private val token = SessionToken(appContext, ComponentName(appContext, OcarinaPlaybackService::class.java))
    private val future = MediaController.Builder(appContext, token).buildAsync()
    private var controller: MediaController? = null
    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) { sync(player) }
        override fun onPlayerError(error: PlaybackException) {
            Log.e("OcarinaPlayback", "Falha Media3 na faixa ${currentSong?.videoId}", error)
            // Expiring temporary URLs occasionally return 403. Retry the current track once.
            if (!retryingSource && currentSong != null &&
                controller?.currentMediaItem?.mediaId == currentSong?.videoId &&
                (error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ||
                    error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED)) {
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
        playing = player.isPlaying
        buffering = player.playbackState == Player.STATE_BUFFERING
        val item = player.currentMediaItem
        // Do not let events from the OLD media item overwrite a freshly selected search result.
        if (item != null && !resolvingAudio && item.mediaId == currentSong?.videoId) {
            val md = item.mediaMetadata
            currentSong = Song(
                item.mediaId,
                md.title?.toString() ?: "Música",
                md.artist?.toString() ?: "Artista",
                md.artworkUri?.toString() ?: ""
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
        prefetchJob?.cancel()
        val thisRequest = ++playSerial
        if (!isRetry) retryingSource = false
        resolvingAudio = true
        // Only pause the previous track: stop()/clearMediaItems() cause extra MediaSession
        // transitions and can synchronously restore stale metadata into Compose.
        controller?.pause()
        currentSong = song
        sourceBitrateKbps = 0L
        error = null
        resolving = scope.launch {
            try {
                val source = AudioSourceResolver.resolve(song.videoId, refresh = refresh)
                val c = connected.await()
                if (thisRequest != playSerial) return@launch
                sourceBitrateKbps = source.bitrateKbps.toLong()
                val item = MediaItem.Builder()
                    .setMediaId(song.videoId)
                    .setUri(source.url)
                    .setCustomCacheKey("ocarina-${song.videoId}-${source.bitrateKbps}")
                    .setMediaMetadata(MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setArtworkUri(Uri.parse(song.artwork))
                        .build())
                    .build()
                c.setMediaItem(item)
                c.prepare()
                c.play()
                // Refresh signed URLs shortly before they are needed, not three minutes early.
                // Avoid competing with initial playback; URL cache expires after 90 seconds.
                if (next != null && next.videoId != song.videoId) {
                    prefetchJob = scope.launch {
                        while (isActive) {
                            val remaining = durationMs - positionMs
                            if (durationMs > 0 && remaining in 1..25_000L) break
                            delay(1_000)
                        }
                        if (isActive) runCatching { AudioSourceResolver.resolve(next.videoId) }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (thisRequest != playSerial) return@launch
                Log.e("OcarinaPlayback", "Falha ao resolver ${song.videoId}", e)
                error = "Não foi possível carregar '${song.title}'. ${e.message ?: "Fonte temporariamente indisponível."}"
                buffering = false
            } finally {
                resolvingAudio = false
            }
        }
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
        prefetchJob?.cancel()
        controller?.removeListener(listener)
        MediaController.releaseFuture(future)
        scope.cancel()
    }
}
