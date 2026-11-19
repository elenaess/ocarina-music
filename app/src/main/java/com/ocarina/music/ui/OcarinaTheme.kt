package com.ocarina.music.ui

import com.ocarina.music.R
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.isAvailableOnDevice
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Dual palettes derived from the supplied Ocarina moodboards. */
data class OcarinaPalette(
    val dark: Boolean,
    val background: Color,
    val surface: Color,
    val elevated: Color,
    val text: Color,
    val muted: Color,
    val accent: Color,
    val onAccent: Color,
    val outline: Color,
    val softAccent: Color,
    val artworkSurface: Color,
)

val LightPalette = OcarinaPalette(
    dark = false,
    background = Color(0xFFFBF7F7),
    surface = Color(0xFFFFFDFD),
    elevated = Color(0xFFF2EAEA),
    text = Color(0xFF1E191E),
    muted = Color(0xFF82767D),
    accent = Color(0xFF9B536A),
    onAccent = Color.White,
    outline = Color(0xFFE8DDDF),
    softAccent = Color(0xFFF1DEE4),
    artworkSurface = Color(0xFFE8D6E2),
)
val DarkPalette = OcarinaPalette(
    dark = true,
    background = Color(0xFF171419),
    surface = Color(0xFF211C23),
    elevated = Color(0xFF302931),
    text = Color(0xFFF9F4F7),
    muted = Color(0xFFB4A8B3),
    accent = Color(0xFFF0ACC0),
    onAccent = Color(0xFF25151F),
    outline = Color(0xFF413842),
    softAccent = Color(0xFF4A303D),
    artworkSurface = Color(0xFF674B60),
)

private val LocalOcarinaPalette = compositionLocalOf { DarkPalette }
val ocarinaColors: OcarinaPalette
    @Composable get() = LocalOcarinaPalette.current

@Composable
fun OcarinaTheme(dark: Boolean, content: @Composable () -> Unit) {
    val palette = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) darkColorScheme(
        primary = palette.accent,
        onPrimary = palette.onAccent,
        background = palette.background,
        onBackground = palette.text,
        surface = palette.surface,
        onSurface = palette.text,
        surfaceVariant = palette.elevated,
        onSurfaceVariant = palette.muted,
        outline = palette.outline,
    ) else lightColorScheme(
        primary = palette.accent,
        onPrimary = palette.onAccent,
        background = palette.background,
        onBackground = palette.text,
        surface = palette.surface,
        onSurface = palette.text,
        surfaceVariant = palette.elevated,
        onSurfaceVariant = palette.muted,
        outline = palette.outline,
    )
    // Inter is distributed by Android's Google Fonts provider, not bundled in the APK.
    // Display is consistent with the neutral neo-grotesque visual direction.
    val provider = remember { GoogleFont.Provider(
        providerAuthority = "com.google.android.gms.fonts",
        providerPackage = "com.google.android.gms",
        certificates = R.array.com_google_android_gms_fonts_certs
    ) }
    val context = LocalContext.current
    val inter = remember {
        FontFamily(
        Font(googleFont = GoogleFont("Inter"), fontProvider = provider, weight = FontWeight.Normal),
        Font(googleFont = GoogleFont("Inter"), fontProvider = provider, weight = FontWeight.SemiBold),
        Font(googleFont = GoogleFont("Inter"), fontProvider = provider, weight = FontWeight.Bold),
        )
    }
    val fontFamily = if (remember(context) { provider.isAvailableOnDevice(context) }) inter else FontFamily.SansSerif
    val typography = Typography(
        bodyLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, letterSpacing = (-0.25).sp),
        bodyMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, letterSpacing = (-0.15).sp),
        bodySmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal),
        titleLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.45).sp),
        titleMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold),
        titleSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold),
        headlineSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold),
        headlineMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold),
        headlineLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold),
        labelLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold),
        labelMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold),
        labelSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold),
    )
    CompositionLocalProvider(LocalOcarinaPalette provides palette) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}
