package com.ocarina.music.data

data class Song(
    val videoId: String,
    val title: String,
    val artist: String,
    val artwork: String,
    val album: String = "",
    val category: String = "music", // music, video, podcast, live
)
