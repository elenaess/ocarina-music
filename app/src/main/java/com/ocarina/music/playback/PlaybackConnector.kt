package com.ocarina.music.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
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

    private val token = SessionToken(appContext, ComponentName(appContext, OcarinaPlaybackService::class.java))
    private val future = MediaController.Builder(appContext, token).buildAsync()
    private var controller: MediaController? = null
    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) { sync(player) }
        override fun onPlayerError(error: PlaybackException) {
            this@PlaybackConnector.error = "O áudio falhou: ${error.errorCodeName}. Tente outra faixa."
            buffering = false
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
                delay(500)
            }
        }
    }

    private fun sync(player: Player) {
        playing = player.isPlaying
        buffering = player.playbackState == Player.STATE_BUFFERING
        val item = player.currentMediaItem
        if (item != null) {
            val md = item.mediaMetadata
            currentSong = Song(
                item.mediaId,
                md.title?.toString() ?: "Música",
                md.artist?.toString() ?: "Artista",
                md.artworkUri?.toString() ?: ""
            )
        }
    }

    fun play(song: Song) {
        resolving?.cancel()
        currentSong = song
        resolvingAudio = true
        error = null
        resolving = scope.launch {
            try {
                val url = AudioSourceResolver.resolve(song.videoId)
                val c = connected.await()
                val item = MediaItem.Builder()
                    .setMediaId(song.videoId)
                    .setUri(url)
                    .setMediaMetadata(MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setArtworkUri(Uri.parse(song.artwork))
                        .build())
                    .build()
                c.setMediaItem(item)
                c.prepare()
                c.play()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = "Não foi possível carregar '${song.title}': ${e.message ?: "falha na fonte"}"
            } finally {
                resolvingAudio = false
            }
        }
    }

    fun toggle() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }
    fun seekTo(ms: Long) { controller?.seekTo(ms.coerceAtLeast(0L)) }
    fun clearError() { error = null }
    fun close() {
        resolving?.cancel()
        controller?.removeListener(listener)
        MediaController.releaseFuture(future)
        scope.cancel()
    }
}
