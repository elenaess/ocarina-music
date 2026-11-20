package com.ocarina.music.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Private per-account playlist synchronization. Firestore rules must be deployed separately. */
class OcarinaCloudLibrary {
    private val database = FirebaseFirestore.getInstance()
    private val writeMutex = Mutex()
    private val videoId = Regex("[A-Za-z0-9_-]{11}")
    private val playlistId = Regex("[a-f0-9-]{36}")
    private fun collection(uid: String) = database.collection("users").document(uid).collection("playlists")

    suspend fun load(uid: String): List<OcarinaPlaylist> {
        val snapshots = collection(uid).get(Source.SERVER).await()
        return snapshots.documents.mapNotNull { doc ->
            if (!playlistId.matches(doc.id)) return@mapNotNull null
            val tracks = (doc.get("songs") as? List<*>)?.mapNotNull { raw ->
                val item = raw as? Map<*, *> ?: return@mapNotNull null
                val id = item["id"] as? String ?: return@mapNotNull null
                if (!videoId.matches(id)) return@mapNotNull null
                Song(id,
                    (item["title"] as? String).orEmpty().take(200),
                    (item["artist"] as? String).orEmpty().take(200),
                    (item["artwork"] as? String).orEmpty().take(800),
                    (item["album"] as? String).orEmpty().take(200))
            }.orEmpty()
            OcarinaPlaylist(doc.id, doc.getString("name").orEmpty().ifBlank { "Playlist" },
                doc.getString("cover").orEmpty(), tracks)
        }.sortedBy { it.name.lowercase() }
    }

    suspend fun syncDiff(uid: String, previous: List<OcarinaPlaylist>, next: List<OcarinaPlaylist>) = writeMutex.withLock {
        // Bounded batches: a malicious/oversized playlist never goes to the cloud.
        require(next.size <= 150) { "Limite de 150 playlists por conta." }
        val before = previous.associateBy { it.id }
        val after = next.associateBy { it.id }
        val col = collection(uid)
        val batch = database.batch()
        var changes = 0
        for (id in before.keys - after.keys) {
            if (!playlistId.matches(id)) continue
            batch.delete(col.document(id)); changes++
        }
        for (item in next) {
            if (!playlistId.matches(item.id) || before[item.id] == item) continue
            val tracks = item.songs.filter { videoId.matches(it.videoId) }.take(200).map { track ->
                mapOf("id" to track.videoId, "title" to track.title.take(200),
                    "artist" to track.artist.take(200), "artwork" to track.artwork.take(800),
                    "album" to track.album.take(200))
            }
            batch.set(col.document(item.id), mapOf(
                "name" to item.name.trim().ifBlank { "Minha playlist" }.take(80),
                // File URIs are private to one install and cannot restore on a new device.
                "cover" to item.cover.takeIf { it.startsWith("https://") }?.take(800).orEmpty(),
                "songs" to tracks,
                "updatedAt" to FieldValue.serverTimestamp()))
            changes++
        }
        if (changes > 0) batch.commit().await()
    }
}
