package com.ocarina.music

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** An incoming App Link is held until the Firebase session is ready. */
object OcarinaDeepLink {
    var pending by mutableStateOf<Uri?>(null)
        private set

    fun offer(uri: Uri?) {
        if (uri?.scheme == "https" && uri.host == "elena.services" &&
            uri.path.orEmpty().startsWith("/ocarina/")) pending = uri
    }

    fun consumed(uri: Uri) { if (pending == uri) pending = null }
}
