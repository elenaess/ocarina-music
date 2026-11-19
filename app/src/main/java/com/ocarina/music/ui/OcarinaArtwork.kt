package com.ocarina.music.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.ocarina.music.data.Song

/**
 * Prefer the larger variants of the SAME artwork URL; never invent a different cover.
 * Music thumbnails commonly use Google's `=w120-h120-l90-rj` sizing suffix.
 * Some uploads only have a small source image; no client can restore missing pixels.
 */
private fun preferredArtwork(original: String, videoId: String, highQuality: Boolean): String {
    val size = if (highQuality) 720 else 360
    val sized = Regex("=w\\d+-h\\d+[^&?]*").find(original)
    if (sized != null) return original.replaceRange(sized.range, "=w${size}-h${size}-l90-rj")
    val yt = Regex("https?://i\\.ytimg\\.com/vi/([a-zA-Z0-9_-]{11})/")
    if (yt.containsMatchIn(original)) {
        return "https://i.ytimg.com/vi/$videoId/sddefault.jpg"
    }
    return original
}

@Composable
fun AlbumArtwork(
    song: Song,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    highQuality: Boolean = false,
    small: Boolean = false,
) {
    val context = LocalContext.current
    val first = remember(song.videoId, song.artwork, highQuality) {
        preferredArtwork(song.artwork, song.videoId, highQuality)
    }
    var imageUrl by remember(first, song.artwork) { mutableStateOf(first) }
    val pixelLimit = if (highQuality) 640 else if (small) 112 else 260
    val request = remember(imageUrl, pixelLimit) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .size(pixelLimit, pixelLimit)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(false) // cheap scrolling and quick transitions
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier,
        onError = {
            if (imageUrl != song.artwork) imageUrl = song.artwork
        },
    )
}
