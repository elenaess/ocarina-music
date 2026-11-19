#!/usr/bin/env python3
"""Structural, offline Ocarina checks; does not replace Gradle/Android tests."""
import json
import re
import sys
from pathlib import Path
from xml.etree import ElementTree
root=Path(__file__).resolve().parent.parent
files={str(p.relative_to(root)):p.read_text(encoding='utf-8') for p in root.rglob('*.kt')}
assert files, 'Sources not found'
all_code='\n'.join(files.values())
manifest=root/'app/src/main/AndroidManifest.xml'
ElementTree.parse(manifest)
assert 'com.ocarina.app' in (root/'app/build.gradle.kts').read_text()
assert 'versionName = "0.4.3"' in (root/'app/build.gradle.kts').read_text()
assert 'versionCode = 12' in (root/'app/build.gradle.kts').read_text()
checks={
 'Firebase unchanged': ('class OcarinaAccount', 'signInWithGoogle', 'uploadAvatar'),
 'Playlists and cover': ('class OcarinaPlaylistStore', 'saveCover', 'fun save(uid'),
 'Artist and album catalog': ('enum class CatalogKind', 'findCollections', 'browseSongs'),
 'Private audio imports': ('class OcarinaOfflineStore', 'OpenMultipleDocuments', 'import(uris'),
 'Media3 local source': ('DefaultDataSource.Factory(this, httpFactory)', 'offline.uriFor'),
 'Repeat and queue': ('fun cycleRepeat', 'REPEAT_MODE_ONE', 'onTrackEnded', 'OcarinaQueueDialog'),
 'Android share intent': ('Intent.ACTION_SEND', 'Compartilhar perfil', 'Compartilhar playlist'),
 'Profile/playlists': ('OcarinaPlaylistCover', 'Minhas playlists', 'playlistCount'),
 'Back/miniplayer': ('BackHandler(', 'MiniPlayer(song, playback'),
 'Lucide icons': ('import com.composables.icons.lucide.R as LucideR',),
}
for name, needles in checks.items():
 missing=[n for n in needles if n not in all_code]
 assert not missing, f'{name}: missing {missing}'
 print('PASS',name)
assert 'fun downloadFromYouTube' not in all_code, 'Unlicensed download flow'
assert len(re.findall(r'private enum class Screen',all_code))==1
assert (root/'firestore.rules').exists()
print(f'PASS AndroidManifest XML, {len(files)} Kotlin files, release metadata')
print('NOTE: Android SDK/Gradle compilation and device tests are still required.')
