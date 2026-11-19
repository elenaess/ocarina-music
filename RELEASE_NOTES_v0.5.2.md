# Ocarina v0.5.2 — Android themed icon

- Updated adaptive launcher icon with Android 13+ monochrome silhouette.
- Launcher applies the wallpaper/OS-themed palette only when themed icons are supported and enabled.
- Added adaptive roundIcon for compatible launchers.
- Monochrome star has additional safe-zone padding for circular masks.
- Preserved the normal graphite/pink full-color icon, Kotlin UI, Google Sign-In, Firebase and Media3 player.
- Same applicationId `com.ocarina.app` and GitHub Actions signing key configuration.

## Installation

Send the full ZIP's `ocarina-music` directory to GitHub using `push-to-github.sh`.
After a successful Actions build, install `ocarina-v0.5.2-themed-icon-debug` APK.
To test themed icon: Android Settings > Wallpaper & style > Themed icons (if available on the launcher). Android 13+ is required.

## Tests

All XML and references checked locally; ZIP integrity passed.
Android SDK unavailable in this environment, so `:app:assembleDebug` and device behavior have not been tested.
