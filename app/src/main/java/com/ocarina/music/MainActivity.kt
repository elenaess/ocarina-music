package com.ocarina.music

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ocarina.music.ui.OcarinaApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OcarinaDeepLink.offer(intent?.data)
        // Antes de o Compose desenhar, evita o branco da janela padrão do Android.
        val prefs = getSharedPreferences("ocarina_preferences", MODE_PRIVATE)
        val dark = !prefs.getBoolean("dark_default_v053", false) || prefs.getBoolean("dark_theme", true)
        val background = if (dark) android.graphics.Color.rgb(23, 20, 25)
            else android.graphics.Color.rgb(251, 247, 247)
        window.decorView.setBackgroundColor(background)
        window.statusBarColor = background
        window.navigationBarColor = background
        setContent { OcarinaApp() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        OcarinaDeepLink.offer(intent.data)
    }
}
