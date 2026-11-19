package com.ocarina.music.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.ocarina.music.data.OcarinaPlaylist
import com.ocarina.music.data.Song

@Composable
fun OcarinaSharedPlaylistView(playlist: OcarinaPlaylist, onPlay: (Song) -> Unit) {
    val c = ocarinaColors
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                OcarinaPlaylistCover(playlist, 190.dp)
            }
            Spacer(Modifier.height(16.dp))
            Text(playlist.name, color = c.text, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Text("Playlist compartilhada • ${playlist.songs.size} músicas", color = c.muted)
        }
        items(playlist.songs, key = { it.videoId }) { song ->
            TextButton(onClick = { onPlay(song) }, modifier = Modifier.fillMaxWidth()) {
                AlbumArtwork(song, song.title, Modifier.size(48.dp), small = true)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(song.title, color = c.text, maxLines = 1)
                    Text(song.artist, color = c.muted, fontSize = 12.sp, maxLines = 1)
                }
            }
        }
    }
}
