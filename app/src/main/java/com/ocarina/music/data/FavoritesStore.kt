package com.ocarina.music.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Saves only the user's own favourites; no account or cloud backend required. */
class FavoritesStore(context: Context) {
    private val prefs = context.getSharedPreferences("ocarina_library", Context.MODE_PRIVATE)

    fun load(): List<Song> = try {
        val array = JSONArray(prefs.getString("saved_songs", "[]") ?: "[]")
        (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val videoId = item.optString("id")
            if (videoId.isBlank()) return@mapNotNull null
            Song(
                videoId,
                item.optString("title", "Faixa"),
                item.optString("artist", "Artista"),
                item.optString("artwork"),
            )
        }
    } catch (_: Exception) { emptyList() }

    fun save(songs: List<Song>) {
        val array = JSONArray()
        songs.forEach { song ->
            array.put(JSONObject().apply {
                put("id", song.videoId)
                put("title", song.title)
                put("artist", song.artist)
                put("artwork", song.artwork)
            })
        }
        prefs.edit().putString("saved_songs", array.toString()).apply()
    }
}
