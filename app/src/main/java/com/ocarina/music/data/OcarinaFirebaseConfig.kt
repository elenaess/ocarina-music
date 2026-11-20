package com.ocarina.music.data

import com.google.firebase.FirebaseOptions

/** Public Android Firebase configuration for the ocarinaapp project. No admin secret here. */
internal object OcarinaFirebaseConfig {
    const val WEB_CLIENT_ID = "220371673528-ik2c284j884ih1ujrpivoj87fqae5cs9.apps.googleusercontent.com"
    fun options(): FirebaseOptions = FirebaseOptions.Builder()
        .setProjectId("ocarinaapp")
        .setApplicationId("1:220371673528:android:6224cfe5c14a1971be2f4d")
        .setApiKey("AIzaSyC6jKHt9WLajTICvx4uTWyayr9rLtf8eP4")
        .build()
}
