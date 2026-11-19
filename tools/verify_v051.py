#!/usr/bin/env python3
"""Ocarina 0.5.1 offline checks. This cannot verify an Android APK build."""
from pathlib import Path
from xml.etree import ElementTree
import re, sys, zipfile
root=Path(__file__).resolve().parent.parent
files={p.name:p.read_text(encoding='utf-8') for p in (root/'app/src/main/java').rglob('*.kt')}
a=files['OcarinaApp.kt']; s=files['OcarinaV5Screens.kt']; c=files['OcarinaCollections.kt']; auth=files['OcarinaAccount.kt']
checks=[]
def check(n,v):
    assert v, n
    checks.append(n)
    print('PASS',n)
app_gradle=(root/'app/build.gradle.kts').read_text()
check('Release v0.5.1 package and version', all(x in app_gradle for x in ['versionCode = 14','versionName = "0.5.1"','applicationId = "com.ocarina.app"']))
check('Android manifest XML', ElementTree.parse(root/'app/src/main/AndroidManifest.xml') is not None)
check('Adaptive launcher icon XML', ElementTree.parse(root/'app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml') is not None)
check('Google login remains available', 'signInWithGoogle' in s and 'resetPassword' in s and 'sendPasswordResetEmail' in auth)
check('Login email form staged behind button', 'var emailMode by remember' in s and 'if (emailMode)' in s)
check('No redundant main screen headings', all(x not in s for x in ['"SEARCH" -> "Procure seu som"','"LIBRARY" -> "Seu espaço"','"PROFILE" -> "Perfil"']))
check('No redundant library/search hints', all(x not in s for x in ['Sem playlists? Use o +','Digite um nome para encontrar músicas e perfis públicos.']))
check('Profile header avatar not duplicated', 'if (screen != "PROFILE" && screen != "PROFILE_EDIT")' in s)
check('Navbar ripple disabled and tabs retained', 'indication = null' in s and '"HOME", "SEARCH", "LIBRARY", "PROFILE"' in s)
check('CD spin, tap-to-flip with play state', all(x in s for x in ['fun OcarinaV5Disc(song: Song, size: Dp, playing: Boolean)','Animatable(0f)','rotationZ = turn.value','rotationY = if (flip <= 90f)','flipped = !flipped']))
check('Player wired to playback state', 'OcarinaV5Disc(song, discSize, playback.playing)' in a)
check('Create playlist dialog and queue redesigned', all(x in a+c for x in ['"Criar playlist"','"Nome da playlist"','"Próximas músicas"','"Concluir"']))
check('Playlists and Firebase intact', all(x in a+auth for x in ['signInWithGoogle','setFavorite','OcarinaPlaylistStore','OcarinaDirectory','OcarinaV5ProfileView']))
check('Search categories and opted-in profiles still available', all(x in s for x in ['"Música", "Vídeos", "Podcast", "Ao vivo", "Perfis"','"Pessoas no Ocarina"']))
check('Existing icon resource names reused',set(re.findall(r'LucideR\.drawable\.(lucide_ic_\w+)',s)).issubset(set(re.findall(r'LucideR\.drawable\.(lucide_ic_\w+)',a+c))))
check('No obsolete Lucide file_audio', 'lucide_ic_file_audio' not in '\n'.join(files.values()))
check('Same signed APK workflow', 'OCARINA_SIGNING_KEYSTORE_B64' in (root/'.github/workflows/android.yml').read_text())
check('Offline source and repeat preserved', all(x in '\n'.join(files.values()) for x in ['class OcarinaOfflineStore','fun cycleRepeat','OcarinaQueueDialog']))
check('Termux push script included', (root/'push-to-github.sh').is_file() and 'git push origin main' in (root/'push-to-github.sh').read_text())
print(f'{len(checks)} checks passed. Android Gradle build and runtime testing are not available in this environment.')
