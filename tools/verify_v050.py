#!/usr/bin/env python3
"""Offline structural checks for the Ocarina v0.5.0 Android source project."""
import re
from pathlib import Path
from xml.etree import ElementTree
import struct
root=Path(__file__).resolve().parent.parent
code={p.name:p.read_text() for p in (root/'app/src/main/java').rglob('*.kt')}
all_code='\n'.join(code.values())
checks=[]
def check(name,condition):
    if not condition: raise AssertionError(name)
    checks.append(name)
    print('PASS',name)

gradle=(root/'app/build.gradle.kts').read_text()
check('0.5.0 app metadata and Firebase package', 'versionName = "0.5.0"' in gradle and 'versionCode = 13' in gradle and 'applicationId = "com.ocarina.app"' in gradle)
check('Android manifest valid',bool(ElementTree.parse(root/'app/src/main/AndroidManifest.xml')))
check('Adaptive icon XML valid',bool(ElementTree.parse(root/'app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml')))
icon=root/'app/src/main/res/drawable-nodpi/ocarina_launcher_art.png'
raw = icon.read_bytes()
check('Graphite pink adaptive icon embedded', raw[:8] == bytes.fromhex('89504e470d0a1a0a') and struct.unpack('>II', raw[16:24]) == (432,432))
app=code['OcarinaApp.kt']; v5=code['OcarinaV5Screens.kt']; account=code['OcarinaAccount.kt']; rules=(root/'firestore.rules').read_text()
check('Authentication gate outside main UI', all(s in app for s in ['if (!account.authReady)', 'else if (account.user == null)', 'OcarinaV5Login(account)', 'OcarinaV5Header(', 'OcarinaV5Nav(']))
check('Google login and separate profile editing',all(s in v5 for s in ['signInWithGoogle','fun OcarinaV5ProfileView', 'fun OcarinaV5ProfileEdit', 'lucide_ic_pencil']))
check('Four navigation destinations with avatar',all(s in v5 for s in ['"HOME", "SEARCH", "LIBRARY", "PROFILE"','OcarinaV5Avatar(account.avatarBase64','"Seu espaço"','"Procure seu som"']))
check('Home playlists, filters, recommendations',all(s in v5 for s in ['V5Chip(chip, filter == chip)', 'Suas playlists','Feito pra sua escuta', 'if (playlists.isNotEmpty()']))
check('Search: song/video separation and discoverability',all(s in v5 for s in ['"Música", "Vídeos", "Podcast", "Ao vivo", "Perfis"', 'songs.filter { it.category == "video" }','"Pessoas no Ocarina"', 'onQuery("")']))
check('Safe opt-in public directory',all(s in account+rules for s in ['public_profiles','bio','avatarBase64']) and 'account.isPublic' in v5 and 'Aparecer na busca' in v5 and 'request.auth != null' in rules)
check('No email/favorites in directory data', all(s not in code['OcarinaDirectory.kt'] for s in ['getString("email")', 'getString("favorites")', 'get("email")']))
check('Private favorites still protected', 'match /favorites/{videoId}' in rules and 'allow read: if owns(uid)' in rules)
check('Media3 and queue preserved', all(s in all_code for s in ['class PlaybackConnector', 'onTrackEnded','cycleRepeat','OcarinaQueueDialog','class OcarinaPlaybackService']))
check('CD-style player', 'fun OcarinaV5Disc' in v5 and 'OcarinaV5Disc(song, discSize)' in app)
check('Per-account listening history', 'recentKey(uid: String?)' in code['OcarinaSettings.kt'] and 'settings.addRecent(song, account.user?.uid)' in app)
check('No missing historical Lucide icon', 'lucide_ic_file_audio' not in all_code)
check('Original lib icons used in new UI',set(re.findall(r'LucideR\.drawable\.(lucide_ic_\w+)',v5)).issubset(set(re.findall(r'LucideR\.drawable\.(lucide_ic_\w+)',app+code['OcarinaCollections.kt']))))
check('No YouTube download implementation', 'fun downloadFromYouTube' not in all_code)
check('Termux push script included', (root/'push-to-github.sh').exists() and 'git push origin main' in (root/'push-to-github.sh').read_text())
print(f'{len(checks)} offline structural checks passed. Gradle and device tests NOT RUN.')
