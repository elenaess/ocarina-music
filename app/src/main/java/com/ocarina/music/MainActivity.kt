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
        window.statusBarColor = android.graphics.Color.rgb(18, 21, 18)
        window.navigationBarColor = android.graphics.Color.rgb(18, 21, 18)
        setContent { OcarinaApp() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        OcarinaDeepLink.offer(intent.data)
    }
}
