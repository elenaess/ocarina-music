package com.ocarina.music.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Lightweight local preferences; listening history is isolated by account. */
class OcarinaSettings(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("ocarina_preferences", Context.MODE_PRIVATE)
    fun isDark(): Boolean {
        if (!preferences.getBoolean("dark_default_v053", false)) {
            preferences.edit().putBoolean("dark_theme", true)
                .putBoolean("dark_default_v053", true).apply()
            return true
        }
        return preferences.getBoolean("dark_theme", true)
    }
    fun setDark(value: Boolean) { preferences.edit().putBoolean("dark_theme", value).apply() }
    fun smartQueueEnabled(): Boolean = preferences.getBoolean("smart_queue_enabled", true)
    fun setSmartQueueEnabled(enabled: Boolean) {
        preferences.edit().putBoolean("smart_queue_enabled", enabled).apply()
    }
    fun repeatMode(): Int = preferences.getInt("repeat_mode", 0).coerceIn(0, 2)
    fun setRepeatMode(value: Int) { preferences.edit().putInt("repeat_mode", value.coerceIn(0, 2)).apply() }
    private fun recentKey(uid: String?): String = if (uid.isNullOrBlank()) "recent_played_guest" else "recent_played_$uid"
    fun recentlyPlayed(uid: String? = null): List<Song> = try {
        val items = JSONArray(preferences.getString(recentKey(uid), "[]") ?: "[]")
        (0 until items.length()).mapNotNull { index ->
            val item = items.optJSONObject(index) ?: return@mapNotNull null
            val videoId = item.optString("id")
            if (videoId.isBlank()) return@mapNotNull null
            Song(videoId, item.optString("title"), item.optString("artist"), item.optString("artwork"), item.optString("album"))
        }
    } catch (_: Exception) { emptyList() }

    fun addRecent(song: Song, uid: String? = null) {
        val updated = listOf(song) + recentlyPlayed(uid).filterNot { it.videoId == song.videoId }
        val array = JSONArray()
        updated.take(12).forEach { item ->
            array.put(JSONObject().apply {
                put("id", item.videoId)
                put("title", item.title)
                put("artist", item.artist)
                put("artwork", item.artwork)
                put("album", item.album)
            })
        }
        preferences.edit().putString(recentKey(uid), array.toString()).apply()
    }
}
