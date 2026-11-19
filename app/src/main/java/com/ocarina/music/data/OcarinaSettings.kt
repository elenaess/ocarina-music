package com.ocarina.music.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Local-only preferences. A Firebase account will be wired in a later milestone. */
class OcarinaSettings(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("ocarina_preferences", Context.MODE_PRIVATE)
    fun isDark(): Boolean = preferences.getBoolean("dark_theme", false)
    fun setDark(value: Boolean) { preferences.edit().putBoolean("dark_theme", value).apply() }
    fun recentlyPlayed(): List<Song> = try {
        val items = JSONArray(preferences.getString("recent_played", "[]") ?: "[]")
        (0 until items.length()).mapNotNull { index ->
            val item = items.optJSONObject(index) ?: return@mapNotNull null
            val videoId = item.optString("id")
            if (videoId.isBlank()) return@mapNotNull null
            Song(videoId, item.optString("title"), item.optString("artist"), item.optString("artwork"))
        }
    } catch (_: Exception) { emptyList() }

    fun addRecent(song: Song) {
        val updated = listOf(song) + recentlyPlayed().filterNot { it.videoId == song.videoId }
        val array = JSONArray()
        updated.take(12).forEach { item ->
            array.put(JSONObject().apply {
                put("id", item.videoId)
                put("title", item.title)
                put("artist", item.artist)
                put("artwork", item.artwork)
            })
        }
        preferences.edit().putString("recent_played", array.toString()).apply()
    }
}
