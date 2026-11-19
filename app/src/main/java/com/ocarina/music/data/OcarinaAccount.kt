package com.ocarina.music.data

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Ocarina accounts: only a display name, a small avatar and the user's own favourites.
 * Avatar is a tiny JPEG stored as an encoded string in the user's private Firestore document.
 * No Firebase Storage bucket or paid plan is needed for this personal experiment.
 */
class OcarinaAccount(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val auth: FirebaseAuth
    private val db: FirebaseFirestore
    private val onAuth: FirebaseAuth.AuthStateListener
    private var profileListener: ListenerRegistration? = null
    private var favoritesListener: ListenerRegistration? = null

    var user by mutableStateOf<FirebaseUser?>(null)
        private set
    var displayName by mutableStateOf("")
        private set
    var avatarBase64 by mutableStateOf("")
        private set
    var cloudFavorites by mutableStateOf<List<Song>>(emptyList())
        private set
    var favoritesReady by mutableStateOf(false)
        private set
    var busy by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set

    init {
        // FirebaseOptions uses public identifiers from the supplied Android Firebase configuration.
        // Mobile Firebase config values are project identifiers, not administrative credentials.
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context, OcarinaFirebaseConfig.options())
        }
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        onAuth = FirebaseAuth.AuthStateListener { firebaseAuth ->
            bindUser(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(onAuth)
    }

    private fun bindUser(next: FirebaseUser?) {
        if (user?.uid == next?.uid) return
        profileListener?.remove()
        favoritesListener?.remove()
        profileListener = null
        favoritesListener = null
        user = next
        displayName = next?.displayName.orEmpty()
        avatarBase64 = ""
        cloudFavorites = emptyList()
        favoritesReady = false
        val uid = next?.uid ?: return
        profileListener = db.collection("users").document(uid).addSnapshotListener { snapshot, err ->
            if (user?.uid != uid) return@addSnapshotListener
            if (err != null) { message = "Perfil indisponível: ${err.localizedMessage}"; return@addSnapshotListener }
            displayName = snapshot?.getString("displayName") ?: next.displayName.orEmpty()
            avatarBase64 = snapshot?.getString("avatarBase64").orEmpty()
        }
        favoritesListener = db.collection("users").document(uid).collection("favorites")
            .addSnapshotListener { snapshot, err ->
                if (user?.uid != uid) return@addSnapshotListener
                if (err != null) { message = "Não foi possível sincronizar curtidas: ${err.localizedMessage}"; return@addSnapshotListener }
                cloudFavorites = snapshot?.documents?.mapNotNull { doc ->
                    val id = doc.getString("id") ?: doc.id
                    if (!id.matches(Regex("[A-Za-z0-9_-]{11}"))) null else Song(
                        id, doc.getString("title").orEmpty(), doc.getString("artist").orEmpty(),
                        doc.getString("artwork").orEmpty()
                    )
                }.orEmpty()
                favoritesReady = true
            }
    }

    /** Official Google Credential Manager flow; token is exchanged for a Firebase session. */
    fun signInWithGoogle(activity: Activity) {
        if (busy) return
        busy = true
        message = null
        scope.launch {
            try {
                val googleOption = GetGoogleIdOption.Builder()
                    .setServerClientId(OcarinaFirebaseConfig.WEB_CLIENT_ID)
                    .setFilterByAuthorizedAccounts(false)
                    .setAutoSelectEnabled(false)
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleOption)
                    .build()
                val credential = CredentialManager.create(activity)
                    .getCredential(context = activity, request = request).credential
                if (credential !is CustomCredential ||
                    credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) throw IllegalStateException("O Google não retornou uma credencial válida.")
                val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
            } catch (_: GetCredentialCancellationException) {
                // The account picker was dismissed; not an authentication failure.
            } catch (e: Exception) {
                message = "Login com Google indisponível: ${e.localizedMessage ?: "verifique SHA-1 e Google no Firebase."}"
            } finally {
                busy = false
            }
        }
    }

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) { message = "Digite e-mail e senha."; return }
        busy = true; message = null
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { result ->
                busy = false
                if (!result.isSuccessful) message = result.exception?.localizedMessage ?: "Não foi possível entrar."
            }
    }

    fun register(email: String, password: String) {
        if (email.isBlank() || password.length < 6) { message = "Use um e-mail e senha com pelo menos 6 caracteres."; return }
        busy = true; message = null
        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { result ->
                busy = false
                if (result.isSuccessful) {
                    result.result?.user?.sendEmailVerification()
                } else message = result.exception?.localizedMessage ?: "Não foi possível criar a conta."
            }
    }

    fun logout() { auth.signOut(); message = null }
    fun clearMessage() { message = null }

    fun updateName(name: String) {
        val uid = user?.uid ?: return
        val clean = name.trim().take(50)
        if (clean.isBlank()) { message = "Escolha um nome de perfil."; return }
        busy = true
        db.collection("users").document(uid)
            .set(mapOf("displayName" to clean, "updatedAt" to FieldValue.serverTimestamp()), SetOptions.merge())
            .addOnCompleteListener { task ->
                busy = false
                if (!task.isSuccessful) message = task.exception?.localizedMessage ?: "Não foi possível salvar o nome."
            }
    }

    fun uploadAvatar(uri: Uri) {
        val uid = user?.uid ?: return
        busy = true; message = null
        scope.launch {
            try {
                val encoded = withContext(Dispatchers.IO) { encodeAvatar(uri) }
                if (user?.uid != uid) { busy = false; return@launch }
                db.collection("users").document(uid)
                    .set(mapOf("avatarBase64" to encoded, "updatedAt" to FieldValue.serverTimestamp()), SetOptions.merge())
                    .addOnCompleteListener { task ->
                        busy = false
                        if (!task.isSuccessful) message = task.exception?.localizedMessage ?: "Não foi possível salvar a foto."
                    }
            } catch (e: Exception) {
                busy = false
                message = "Não foi possível processar a imagem: ${e.localizedMessage}"
            }
        }
    }

    /** Downsample before decoding, then compress to a compact, bounded JPEG. */
    private fun encodeAvatar(uri: Uri): String {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val opened = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Imagem não encontrada")
        opened.use { stream -> BitmapFactory.decodeStream(stream, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IllegalArgumentException("Arquivo de imagem inválido")
        val sample = BitmapFactory.Options().apply {
            inSampleSize = generateSequence(1) { it * 2 }
                .first { bounds.outWidth / it <= 512 && bounds.outHeight / it <= 512 }
        }
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, sample)
        } ?: throw IllegalArgumentException("Não foi possível ler a imagem")
        val maxSide = maxOf(decoded.width, decoded.height)
        val scale = 192f / maxSide.coerceAtLeast(1)
        val resized = if (scale < 1f) Bitmap.createScaledBitmap(decoded,
            (decoded.width * scale).toInt().coerceAtLeast(1),
            (decoded.height * scale).toInt().coerceAtLeast(1), true) else decoded
        var encoded = ""
        for (quality in listOf(75, 60, 45)) {
            val out = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, quality, out)
            encoded = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            if (encoded.length <= 85_000) break
        }
        if (decoded !== resized) decoded.recycle()
        resized.recycle()
        require(encoded.length <= 85_000) { "Imagem muito grande para o perfil." }
        return encoded
    }

    fun setFavorite(song: Song, liked: Boolean) {
        val uid = user?.uid ?: return
        val doc = db.collection("users").document(uid).collection("favorites").document(song.videoId)
        val task = if (liked) doc.set(mapOf(
            "id" to song.videoId, "title" to song.title.take(200),
            "artist" to song.artist.take(200), "artwork" to song.artwork.take(800)
        )) else doc.delete()
        task.addOnFailureListener { message = "Não foi possível sincronizar esta curtida: ${it.localizedMessage}" }
    }

    fun close() {
        auth.removeAuthStateListener(onAuth)
        profileListener?.remove(); favoritesListener?.remove()
        scope.cancel()
    }
}
