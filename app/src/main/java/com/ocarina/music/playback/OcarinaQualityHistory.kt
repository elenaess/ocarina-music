package com.ocarina.music.playback

import android.content.Context
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import org.json.JSONObject

/** A private, bounded history, not a global scan or guarantee of future stream availability. */
internal object OcarinaQualityHistory {
    private var context: Context? = null
    private var account: String? = null
    private val verified = LinkedHashMap<String, Pair<String, Long>>()
    private val lastSent = mutableMapOf<String, Long>()
    private const val MAX_ENTRIES = 150
    private const val FRESH_MS = 3L * 24 * 60 * 60 * 1000
    private const val CANDIDATE_MS = 6L * 60 * 60 * 1000

    fun bind(app: Context, uid: String?) {
        context = app.applicationContext
        account = uid
        verified.clear(); lastSent.clear()
        HighQualitySourceResolver.qualityLabels.clear()
        if (uid == null) return
        val prefs = app.getSharedPreferences("ocarina_quality_history", Context.MODE_PRIVATE)
        runCatching {
            val root = JSONObject(prefs.getString("user_$uid", "{}") ?: "{}")
            val now = System.currentTimeMillis()
            val keys = root.keys()
            while (keys.hasNext()) {
                val id = keys.next()
                val item = root.optJSONObject(id) ?: continue
                val label = item.optString("label")
                val time = item.optLong("time")
                val ttl = if (label == "CANDIDATE") CANDIDATE_MS else FRESH_MS
                if (id.matches(Regex("[A-Za-z0-9_-]{11}")) &&
                    label in setOf("FLAC", "HQ", "CANDIDATE") && now - time in 0..ttl) {
                    verified[id] = label to time
                    HighQualitySourceResolver.qualityLabels[id] = if (label == "CANDIDATE") "FLAC?" else "$label?"
                }
            }
        }
        val captured = uid
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .collection("quality_history").orderBy("checkedAt", Query.Direction.DESCENDING)
            .limit(MAX_ENTRIES.toLong()).get()
            .addOnSuccessListener { snap ->
                if (account != captured) return@addOnSuccessListener
                val now = System.currentTimeMillis()
                snap.documents.forEach { doc ->
                    val label = doc.getString("label") ?: return@forEach
                    val at = doc.getTimestamp("checkedAt")?.toDate()?.time ?: return@forEach
                    val ttl = if (label == "CANDIDATE") CANDIDATE_MS else FRESH_MS
                    if (label !in setOf("FLAC", "HQ", "CANDIDATE") || now - at !in 0..ttl) return@forEach
                    val older = verified[doc.id]?.second ?: 0L
                    if (at > older) {
                        verified[doc.id] = label to at
                        if (HighQualitySourceResolver.qualityLabels[doc.id] !in setOf("FLAC", "HQ"))
                            HighQualitySourceResolver.qualityLabels[doc.id] = if (label == "CANDIDATE") "FLAC?" else "$label?"
                    }
                }
                saveLocal()
            }
        // If Firestore is unavailable, the local cache remains usable.
    }

    fun recordVerified(videoId: String, label: String) {
        val uid = account ?: return
        if (label !in setOf("FLAC", "HQ") || !videoId.matches(Regex("[A-Za-z0-9_-]{11}"))) return
        val now = System.currentTimeMillis()
        verified[videoId] = label to now
        saveLocal()
        if (now - (lastSent[videoId] ?: 0L) < 8 * 60 * 60 * 1000L) return
        lastSent[videoId] = now
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .collection("quality_history").document(videoId)
            .set(mapOf("label" to label, "checkedAt" to FieldValue.serverTimestamp()))
            .addOnFailureListener { lastSent.remove(videoId) }
    }

    /** Ranged header only: preserve a fast FLAC? hint without ever claiming confirmed FLAC. */
    fun recordCandidate(videoId: String) {
        val uid = account ?: return
        if (!videoId.matches(Regex("[A-Za-z0-9_-]{11}"))) return
        val now = System.currentTimeMillis()
        if (verified[videoId]?.first in setOf("FLAC", "HQ")) return
        verified[videoId] = "CANDIDATE" to now
        saveLocal()
        if (now - (lastSent[videoId] ?: 0L) < 12 * 60 * 60 * 1000L) return
        lastSent[videoId] = now
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .collection("quality_history").document(videoId)
            .set(mapOf("label" to "CANDIDATE", "checkedAt" to FieldValue.serverTimestamp()))
            .addOnFailureListener { lastSent.remove(videoId) }
    }

    fun invalidate(videoId: String) {
        verified.remove(videoId)
        saveLocal()
        val uid = account ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .collection("quality_history").document(videoId).delete()
    }

    private fun saveLocal() {
        val uid = account ?: return
        val prefs = context?.getSharedPreferences("ocarina_quality_history", Context.MODE_PRIVATE) ?: return
        val json = JSONObject()
        verified.entries.sortedByDescending { it.value.second }.take(MAX_ENTRIES).forEach { (id, data) ->
            json.put(id, JSONObject().put("label", data.first).put("time", data.second))
        }
        prefs.edit().putString("user_$uid", json.toString()).apply()
    }
}
