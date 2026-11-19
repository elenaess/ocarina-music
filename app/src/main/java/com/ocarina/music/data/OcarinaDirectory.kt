package com.ocarina.music.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Locale

data class OcarinaPublicProfile(
    val uid: String,
    val displayName: String,
    val bio: String = "",
    val photoUrl: String = "",
    val avatarBase64: String = "",
)

/** Explicit opt-in only. Queries never include email, library or private favorites. */
class OcarinaDirectory {
    private val profiles = FirebaseFirestore.getInstance().collection("public_profiles")

    suspend fun search(query: String): List<OcarinaPublicProfile> {
        val key = query.trim().lowercase(Locale.ROOT).take(50)
        if (key.length < 2) return emptyList()
        val snapshot = profiles.orderBy("searchName")
            .startAt(key).endAt(key + "\uf8ff").limit(12).get().await()
        return snapshot.documents.mapNotNull { doc ->
            val name = doc.getString("displayName").orEmpty()
            if (name.isBlank()) null else OcarinaPublicProfile(
                uid = doc.id, displayName = name,
                bio = doc.getString("bio").orEmpty(),
                photoUrl = doc.getString("photoUrl").orEmpty(),
                avatarBase64 = doc.getString("avatarBase64").orEmpty(),
            )
        }
    }
}
