plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.ocarina.music"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ocarina.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 13
        versionName = "0.5.0"
    }
    val ciKeyStoreFile = System.getenv("OCARINA_SIGNING_STORE_FILE")?.takeIf { it.isNotBlank() && file(it).isFile }
    signingConfigs {
        if (ciKeyStoreFile != null) {
            create("ocarinaPreview") {
                storeFile = file(ciKeyStoreFile)
                storePassword = System.getenv("OCARINA_SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("OCARINA_SIGNING_KEY_ALIAS") ?: "ocarina"
                keyPassword = System.getenv("OCARINA_SIGNING_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        getByName("debug") {
            if (signingConfigs.findByName("ocarinaPreview") != null) {
                signingConfig = signingConfigs.getByName("ocarinaPreview")
            }
        }
        release { isMinifyEnabled = false }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:35.2.0"))
    implementation("com.google.firebase:firebase-auth") {
        version { strictly("24.0.1") }
    }
    implementation("com.google.firebase:firebase-firestore")
    implementation("androidx.credentials:credentials:1.6.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.6.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.1.5")
    implementation("com.github.teamnewpipe:NewPipeExtractor:v0.27.1")
    implementation("androidx.media3:media3-exoplayer:1.9.4")
    implementation("androidx.media3:media3-session:1.9.4")
    implementation("androidx.media3:media3-datasource:1.9.4")
    implementation("androidx.media3:media3-database:1.9.4")

    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("com.composables:icons-lucide-android:2.2.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.2")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
