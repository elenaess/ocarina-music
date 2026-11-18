package com.ocarina.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ocarina.music.ui.OcarinaApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(18, 21, 18)
        window.navigationBarColor = android.graphics.Color.rgb(18, 21, 18)
        setContent { OcarinaApp() }
    }
}
