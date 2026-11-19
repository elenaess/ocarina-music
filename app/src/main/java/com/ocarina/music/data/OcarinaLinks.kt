package com.ocarina.music.data

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** Public share URLs; they resolve to native screens, never to a third-party website. */
object OcarinaLinks {
    private const val BASE = "https://elena.services/ocarina"

    fun track(song: Song): String? =
        song.videoId.takeIf { it.matches(Regex("[A-Za-z0-9_-]{11}")) }?.let { id ->
            Uri.parse("$BASE/track/$id").buildUpon()
                .appendQueryParameter("title", song.title.take(150))
                .appendQueryParameter("artist", song.artist.take(100))
                .appendQueryParameter("artwork", song.artwork.take(800))
                .build().toString()
        }

    fun profile(uid: String): String = "$BASE/profile/${Uri.encode(uid)}"
    fun playlist(uid: String, id: String): String = "$BASE/playlist/${Uri.encode(uid)}/${Uri.encode(id)}"

    suspend fun publishPlaylist(ownerUid: String, playlist: OcarinaPlaylist) {
        require(FirebaseAuth.getInstance().currentUser?.uid == ownerUid)
        require(playlist.id.matches(Regex("[a-f0-9-]{36}")))
        val songs = playlist.songs.filter {
            it.videoId.matches(Regex("[A-Za-z0-9_-]{11}"))
        }.take(60)
        val snapshot = mapOf(
            "ownerUid" to ownerUid,
            "name" to playlist.name.take(80),
            "cover" to (playlist.cover.takeIf { it.startsWith("https://") }
                ?: songs.firstOrNull()?.artwork.orEmpty()).take(800),
            "songs" to songs.map { song -> mapOf(
                "id" to song.videoId, "title" to song.title.take(150),
                "artist" to song.artist.take(100), "artwork" to song.artwork.take(800),
                "album" to song.album.take(100)
            ) }
        )
        FirebaseFirestore.getInstance().collection("public_playlists")
            .document(ownerUid).collection("items").document(playlist.id)
            .set(snapshot).await()
    }

    suspend fun removePublishedPlaylist(ownerUid: String, id: String) {
        require(FirebaseAuth.getInstance().currentUser?.uid == ownerUid)
        if (!id.matches(Regex("[a-f0-9-]{36}"))) return
        FirebaseFirestore.getInstance().collection("public_playlists")
            .document(ownerUid).collection("items").document(id).delete().await()
    }

    suspend fun readPlaylist(ownerUid: String, id: String): OcarinaPlaylist? {
        if (!id.matches(Regex("[a-f0-9-]{36}")) || ownerUid.isBlank()) return null
        val doc = FirebaseFirestore.getInstance().collection("public_playlists")
            .document(ownerUid).collection("items").document(id).get().await()
        if (!doc.exists() || doc.getString("ownerUid") != ownerUid) return null
        val songs = (doc.get("songs") as? List<*>)?.mapNotNull { value ->
            val item = value as? Map<*, *> ?: return@mapNotNull null
            val videoId = (item["id"] as? String).orEmpty()
            if (!videoId.matches(Regex("[A-Za-z0-9_-]{11}"))) return@mapNotNull null
            Song(videoId, (item["title"] as? String).orEmpty(),
                (item["artist"] as? String).orEmpty(),
                (item["artwork"] as? String).orEmpty(),
                (item["album"] as? String).orEmpty())
        }.orEmpty()
        return OcarinaPlaylist(id, doc.getString("name") ?: "Playlist",
            doc.getString("cover").orEmpty(), songs)
    }
}
