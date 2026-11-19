package com.ocarina.music.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import coil.compose.AsyncImage
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat
import com.ocarina.music.R
import com.composables.icons.lucide.R as LucideR
import com.ocarina.music.data.CatalogEntry
import com.ocarina.music.data.CatalogKind
import com.ocarina.music.data.OcarinaDirectory
import com.ocarina.music.data.OcarinaPublicProfile
import com.ocarina.music.data.OcarinaOfflineStore
import com.ocarina.music.data.OcarinaPlaylist
import com.ocarina.music.data.OcarinaPlaylistStore
import com.ocarina.music.data.FavoritesStore
import com.ocarina.music.data.OcarinaSettings
import com.ocarina.music.data.OcarinaAccount
import com.ocarina.music.data.Song
import com.ocarina.music.data.YouTubeMusicSearch
import com.ocarina.music.playback.PlaybackConnector
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private enum class Screen {
    HOME, SEARCH, LIBRARY, LIKED, PROFILE, PLAYLISTS, PLAYLIST_DETAIL,
    ARTISTS, ALBUMS, CATALOG_DETAIL, DOWNLOADS, PROFILE_EDIT, PUBLIC_PROFILE
}

/** Every interface icon is a Lucide vector resource. The brand mark is its own vector. */
@Composable
private fun LIcon(@DrawableRes icon: Int, label: String?, modifier: Modifier = Modifier, tint: Color = ocarinaColors.text) {
    Icon(painter = painterResource(icon), contentDescription = label, modifier = modifier, tint = tint)
}

@Composable
private fun OcarinaMark(modifier: Modifier = Modifier, tint: Color = ocarinaColors.text) {
    Icon(painterResource(R.drawable.ocarina_logo), contentDescription = "Ocarina", modifier = modifier, tint = tint)
}

@Composable
fun OcarinaApp() {
    val context = LocalContext.current
    val settings = remember(context) { OcarinaSettings(context) }
    var dark by remember { mutableStateOf(settings.isDark()) }
    OcarinaTheme(dark = dark) {
        val c = ocarinaColors
        SideEffect {
            (context as? Activity)?.window?.let { window ->
                window.statusBarColor = if (dark) android.graphics.Color.rgb(23, 20, 25)
                    else android.graphics.Color.rgb(251, 247, 247)
                window.navigationBarColor = if (dark) android.graphics.Color.rgb(23, 20, 25)
                    else android.graphics.Color.rgb(251, 247, 247)
                val controller = WindowInsetsControllerCompat(window, window.decorView)
                controller.isAppearanceLightStatusBars = !dark
                controller.isAppearanceLightNavigationBars = !dark
            }
        }
        val searchClient = remember { YouTubeMusicSearch() }
        val store = remember(context) { FavoritesStore(context) }
        val account = remember(context) { OcarinaAccount(context) }
        val directory = remember(account) { OcarinaDirectory() }
        DisposableEffect(account) { onDispose { account.close() } }
        val favorites = remember { mutableStateListOf<Song>().apply { addAll(store.load()) } }
        val recent = remember { mutableStateListOf<Song>() }
        val playback = remember(context) { PlaybackConnector(context) }
        DisposableEffect(playback) { onDispose { playback.close() } }
        var screen by remember { mutableStateOf(Screen.HOME) }
        var previousScreen by remember { mutableStateOf(Screen.HOME) }
        var searchText by remember { mutableStateOf("") }
        var discoverSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
        var searchSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
        var publicProfiles by remember { mutableStateOf<List<OcarinaPublicProfile>>(emptyList()) }
        var profileSearchLoading by remember { mutableStateOf(false) }
        var selectedPublicProfile by remember { mutableStateOf<OcarinaPublicProfile?>(null) }
        var discoveryLoading by remember { mutableStateOf(true) }
        var searchLoading by remember { mutableStateOf(false) }
        var discoverError by remember { mutableStateOf<String?>(null) }
        var searchError by remember { mutableStateOf<String?>(null) }
        var searchRefresh by remember { mutableIntStateOf(0) }
        var discoverRefresh by remember { mutableIntStateOf(0) }
        var showFullPlayer by remember { mutableStateOf(false) }
        var showInfo by remember { mutableStateOf(false) }
        val homeScroll = rememberLazyListState()
        val searchScroll = rememberLazyListState()
        val libraryScroll = rememberLazyListState()
        val likedScroll = rememberLazyListState()
        var queue by remember { mutableStateOf<List<Song>>(emptyList()) }
        var switchingTabs by remember { mutableStateOf(false) }
        var syncedAccount by remember { mutableStateOf<String?>(null) }
        val playlistStore = remember(context) { OcarinaPlaylistStore(context) }
        val offlineStore = remember(context) { OcarinaOfflineStore(context) }
        val coroutineScope = rememberCoroutineScope()
        var playlists by remember { mutableStateOf(playlistStore.load(account.user?.uid)) }
        var offlineSongs by remember { mutableStateOf(offlineStore.load()) }
        var importing by remember { mutableStateOf(false) }
        var importMessage by remember { mutableStateOf<String?>(null) }
        var selectedPlaylistId by remember { mutableStateOf<String?>(null) }
        var playlistReturnScreen by remember { mutableStateOf(Screen.PLAYLISTS) }
        var addToPlaylist by remember { mutableStateOf<Song?>(null) }
        var creatingPlaylist by remember { mutableStateOf(false) }
        var newPlaylistName by remember { mutableStateOf("") }
        var songForNewPlaylist by remember { mutableStateOf<Song?>(null) }
        var showQueue by remember { mutableStateOf(false) }
        var selectedCatalog by remember { mutableStateOf<CatalogEntry?>(null) }
        var catalogQuery by remember { mutableStateOf("") }
        var catalogResults by remember { mutableStateOf<List<CatalogEntry>>(emptyList()) }
        var catalogLoading by remember { mutableStateOf(false) }
        var catalogError by remember { mutableStateOf<String?>(null) }
        var detailSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
        var detailLoading by remember { mutableStateOf(false) }
        var detailError by remember { mutableStateOf<String?>(null) }
        val offlinePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris.isNotEmpty() && !importing) {
                coroutineScope.launch {
                    importing = true
                    try {
                        val imported = withContext(Dispatchers.IO) { offlineStore.import(uris) }
                        offlineSongs = offlineStore.load()
                        importMessage = if (imported.isEmpty()) "Nenhum arquivo compatível foi importado."
                            else "${imported.size} arquivo(s) salvos offline."
                    } catch (e: Exception) { importMessage = e.localizedMessage ?: "Falha na importação." }
                    finally { importing = false }
                }
            }
        }
        fun savePlaylists(items: List<OcarinaPlaylist>) {
            playlists = items
            playlistStore.save(account.user?.uid, items)
        }
        fun updatePlaylist(list: OcarinaPlaylist) {
            savePlaylists(playlists.map { if (it.id == list.id) list else it })
        }
        fun putInPlaylist(list: OcarinaPlaylist, song: Song) {
            updatePlaylist(list.copy(songs = (list.songs + song).distinctBy { it.videoId }))
        }
        fun addToQueue(song: Song) {
            if (queue.none { it.videoId == song.videoId }) queue = queue + song
            Toast.makeText(context, "Adicionada à fila", Toast.LENGTH_SHORT).show()
        }
        fun shareText(title: String, body: String) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, body)
            }
            runCatching { context.startActivity(Intent.createChooser(intent, title)) }
        }
        fun songLink(song: Song): String = if (song.videoId.matches(Regex("[A-Za-z0-9_-]{11}")))
            "https://music.youtube.com/watch?v=${song.videoId}" else "Áudio pessoal no Ocarina"
        fun shareSong(song: Song) {
            shareText("Compartilhar música", "${song.title} — ${song.artist}\n${songLink(song)}")
        }
        fun sharePlaylist(list: OcarinaPlaylist) {
            val songsList = list.songs.take(50).mapIndexed { i, song ->
                "${i + 1}. ${song.title} — ${song.artist}\n${songLink(song)}"
            }.joinToString("\n")
            shareText("Compartilhar playlist", "Playlist ${list.name} • Ocarina\n\n$songsList")
        }
        fun openCatalog(kind: CatalogKind) {
            catalogQuery = ""
            catalogResults = emptyList()
            catalogError = null
            screen = if (kind == CatalogKind.ARTIST) Screen.ARTISTS else Screen.ALBUMS
        }
        fun openCatalogDetail(item: CatalogEntry) {
            selectedCatalog = item
            detailSongs = emptyList()
            detailError = null
            screen = Screen.CATALOG_DETAIL
        }
        fun goBack() {
            when {
                showInfo -> showInfo = false
                showQueue -> showQueue = false
                creatingPlaylist -> { creatingPlaylist = false; songForNewPlaylist = null }
                addToPlaylist != null -> addToPlaylist = null
                showFullPlayer -> showFullPlayer = false
                screen == Screen.PLAYLIST_DETAIL -> screen = playlistReturnScreen
                screen == Screen.CATALOG_DETAIL -> screen = if (selectedCatalog?.kind == CatalogKind.ARTIST)
                    Screen.ARTISTS else Screen.ALBUMS
                screen == Screen.PLAYLISTS || screen == Screen.ARTISTS ||
                    screen == Screen.ALBUMS || screen == Screen.DOWNLOADS -> screen = Screen.LIBRARY
                screen == Screen.PROFILE_EDIT -> screen = Screen.PROFILE
                screen == Screen.PUBLIC_PROFILE -> screen = Screen.SEARCH
                screen == Screen.SEARCH && searchText.isNotBlank() -> searchText = ""
                screen == Screen.PROFILE -> screen = previousScreen
                screen != Screen.HOME -> screen = Screen.HOME
            }
        }
        BackHandler(enabled = showFullPlayer || showInfo || showQueue || creatingPlaylist ||
            addToPlaylist != null || screen != Screen.HOME) { goBack() }
        // Keep guest favourites local; cloud favourites are scoped to each signed-in user.
        LaunchedEffect(account.user?.uid) {
            if (account.user == null) {
                playback.resetForLogout()
                showFullPlayer = false
                queue = emptyList()
                screen = Screen.HOME
            }
            syncedAccount = null
            favorites.clear()
            favorites.addAll(store.load(account.user?.uid))
            playlists = playlistStore.load(account.user?.uid)
            recent.clear()
            recent.addAll(settings.recentlyPlayed(account.user?.uid))
        }
        LaunchedEffect(account.user?.uid, account.favoritesReady, account.cloudFavorites) {
            val uid = account.user?.uid ?: return@LaunchedEffect
            if (!account.favoritesReady) return@LaunchedEffect
            val cloud = account.cloudFavorites
            if (syncedAccount != uid) {
                val local = store.load(uid)
                val onlyLocal = local.filterNot { entry -> cloud.any { it.videoId == entry.videoId } }
                onlyLocal.forEach { account.setFavorite(it, true) }
                val combined = (cloud + onlyLocal).distinctBy { it.videoId }
                favorites.clear(); favorites.addAll(combined)
                store.save(combined, uid)
                syncedAccount = uid
            } else {
                // Subsequent snapshots are authoritative, including deletions from another device.
                favorites.clear(); favorites.addAll(cloud)
                store.save(cloud, uid)
            }
        }
        LaunchedEffect(screen) {
            switchingTabs = true
            delay(90)
            switchingTabs = false
        }

        fun openProfile() {
            if (screen != Screen.PROFILE) previousScreen = screen
            screen = Screen.PROFILE
        }
        fun toggleFavorite(song: Song) {
            val index = favorites.indexOfFirst { it.videoId == song.videoId }
            val adding = index < 0
            if (adding) favorites.add(0, song) else favorites.removeAt(index)
            store.save(favorites.toList(), account.user?.uid)
            account.setFavorite(song, adding)
        }
        fun playSong(song: Song, source: List<Song>) {
            queue = source.ifEmpty { listOf(song) }
            val index = queue.indexOfFirst { it.videoId == song.videoId }
            playback.play(song, queue.getOrNull(index + 1))
            recent.removeAll { it.videoId == song.videoId }
            recent.add(0, song)
            if (recent.size > 12) recent.removeAt(recent.lastIndex)
            settings.addRecent(song, account.user?.uid)
            showFullPlayer = true
        }
        fun next() {
            val i = queue.indexOfFirst { it.videoId == playback.currentSong?.videoId }
            val nextSong = queue.getOrNull(i + 1)
                ?: if (playback.repeatMode == 2) queue.firstOrNull() else null
            nextSong?.let { playback.play(it) }
        }
        fun previous() {
            val i = queue.indexOfFirst { it.videoId == playback.currentSong?.videoId }
            queue.getOrNull(i - 1)?.let { playback.play(it, queue.getOrNull(i)) }
        }

        LaunchedEffect(discoverRefresh, account.user?.uid) {
            if (account.user == null) return@LaunchedEffect
            discoveryLoading = true
            discoverError = null
            try { discoverSongs = searchClient.search("música brasileira contemporânea") }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { discoverError = e.message ?: "Não foi possível consultar o catálogo." }
            finally { discoveryLoading = false }
        }
        LaunchedEffect(searchText, searchRefresh, account.user?.uid) {
            val query = searchText.trim()
            if (query.isBlank() || account.user == null) {
                searchSongs = emptyList(); searchError = null; searchLoading = false
                return@LaunchedEffect
            }
            searchLoading = true
            searchError = null
            searchSongs = emptyList()
            delay(360)
            try { searchSongs = searchClient.search(query) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { searchSongs = emptyList(); searchError = e.message ?: "Não foi possível buscar." }
            finally { searchLoading = false }
        }

        // Authenticated public-profile search; only opt-in profiles are discoverable.
        LaunchedEffect(searchText, account.user?.uid) {
            val query = searchText.trim()
            if (query.length < 2 || account.user == null) {
                publicProfiles = emptyList()
                profileSearchLoading = false
                return@LaunchedEffect
            }
            delay(380)
            profileSearchLoading = true
            try { publicProfiles = directory.search(query) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { publicProfiles = emptyList() }
            finally { profileSearchLoading = false }
        }
        LaunchedEffect(screen, catalogQuery) {
            if (screen != Screen.ARTISTS && screen != Screen.ALBUMS) return@LaunchedEffect
            val query = catalogQuery.trim()
            if (query.isBlank()) { catalogResults = emptyList(); catalogError = null; catalogLoading = false; return@LaunchedEffect }
            delay(270)
            catalogLoading = true; catalogError = null
            try {
                catalogResults = searchClient.findCollections(query,
                    if (screen == Screen.ARTISTS) CatalogKind.ARTIST else CatalogKind.ALBUM)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { catalogResults = emptyList(); catalogError = e.localizedMessage ?: "Erro do catálogo." }
            finally { catalogLoading = false }
        }
        LaunchedEffect(selectedCatalog?.browseId, screen) {
            if (screen != Screen.CATALOG_DETAIL) return@LaunchedEffect
            val item = selectedCatalog ?: return@LaunchedEffect
            if (item.browseId.startsWith("local:")) {
                detailSongs = (favorites + recent + searchSongs + offlineSongs).distinctBy { it.videoId }
                    .filter { song -> if (item.kind == CatalogKind.ARTIST) song.artist.equals(item.name, true)
                        else song.album.equals(item.name, true) }
                detailLoading = false
                detailError = null
                return@LaunchedEffect
            }
            detailLoading = true; detailSongs = emptyList(); detailError = null
            try {
                detailSongs = searchClient.browseSongs(item.browseId).map { song ->
                    if (item.kind == CatalogKind.ALBUM) song.copy(album = item.name) else song
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { detailError = e.localizedMessage ?: "Não foi possível abrir." }
            finally { detailLoading = false }
        }
        SideEffect {
            playback.onTrackEnded = { ended ->
                val position = queue.indexOfFirst { it.videoId == ended.videoId }
                val candidate = if (position < 0) null else queue.getOrNull(position + 1)
                    ?: if (playback.repeatMode == 2) queue.firstOrNull() else null
                if (candidate != null) playback.play(candidate)
            }
        }
        if (!account.authReady) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                OcarinaMark(Modifier.size(86.dp), c.accent)
                Spacer(Modifier.height(22.dp))
                CircularProgressIndicator(Modifier.size(23.dp), color = c.accent, strokeWidth = 2.dp)
            }
        } else if (account.user == null) {
            OcarinaV5Login(account)
        } else {
        Box(Modifier.fillMaxSize()) {
        if (!showFullPlayer || playback.currentSong == null) {
        Scaffold(
            containerColor = c.background,
            topBar = {
                OcarinaV5Header(
                    screen = screen.name, account = account,
                    onProfile = ::openProfile,
                    onBack = ::goBack,
                    onSearch = { screen = Screen.SEARCH },
                    onCreate = { creatingPlaylist = true },
                )
            },
            bottomBar = {
                Column(Modifier.background(c.background)) {
                    playback.currentSong?.let { song ->
                        MiniPlayer(song, playback, onExpand = { showFullPlayer = true })
                    }
                    OcarinaV5Nav(selected = screen.name, account = account,
                        onSelect = { tab ->
                            if (tab == "PROFILE" && screen != Screen.PROFILE) previousScreen = screen
                            screen = when (tab) {
                                "SEARCH" -> Screen.SEARCH
                                "LIBRARY" -> Screen.LIBRARY
                                "PROFILE" -> Screen.PROFILE
                                else -> Screen.HOME
                            }
                        })
                }
            },
        ) { insets ->
            Box(Modifier.fillMaxSize().padding(insets)) {
                when (screen) {
                    Screen.HOME -> OcarinaV5Home(
                        listState = homeScroll, playlists = playlists,
                        discoveries = discoverSongs, recent = recent, favorites = favorites,
                        loading = discoveryLoading, onRetry = { discoverRefresh++ },
                        onOpenPlaylist = {
                            selectedPlaylistId = it.id
                            playlistReturnScreen = Screen.HOME
                            screen = Screen.PLAYLIST_DETAIL
                        },
                        onOpenLibrary = { screen = Screen.LIBRARY }, onSearch = { screen = Screen.SEARCH },
                        onPlay = ::playSong, onLike = ::toggleFavorite,
                        onQueue = ::addToQueue, onPlaylist = { addToPlaylist = it },
                        onShare = ::shareSong,
                    )
                    Screen.SEARCH -> OcarinaV5Search(
                        listState = searchScroll,
                        query = searchText, onQuery = { searchText = it },
                        songs = searchSongs, recommendations = discoverSongs,
                        profiles = publicProfiles, favorites = favorites,
                        loading = searchLoading, profilesLoading = profileSearchLoading,
                        error = searchError, onRetry = { searchRefresh++ },
                        onPlay = { song -> playSong(song, searchSongs) }, onLike = ::toggleFavorite,
                        onQueue = ::addToQueue, onPlaylist = { addToPlaylist = it },
                        onShare = ::shareSong,
                        onProfile = { profile -> selectedPublicProfile = profile; screen = Screen.PUBLIC_PROFILE },
                    )
                    Screen.LIBRARY -> OcarinaV5Library(
                        listState = libraryScroll, playlists = playlists, favorites = favorites,
                        recent = recent, onOpenPlaylist = {
                            selectedPlaylistId = it.id
                            playlistReturnScreen = Screen.LIBRARY
                            screen = Screen.PLAYLIST_DETAIL
                        },
                        onPlaylists = { screen = Screen.PLAYLISTS },
                        onLiked = { screen = Screen.LIKED },
                        onArtists = { openCatalog(CatalogKind.ARTIST) },
                        onAlbums = { openCatalog(CatalogKind.ALBUM) },
                        onDownloads = { screen = Screen.DOWNLOADS },
                        onPlay = { song -> playSong(song, recent) },
                        onLike = ::toggleFavorite, onQueue = ::addToQueue,
                        onPlaylist = { addToPlaylist = it }, onShare = ::shareSong,
                    )
                    Screen.LIKED -> LikedPage(
                        listState = likedScroll,
                        favorites = favorites, onPlay = { song -> playSong(song, favorites.toList()) },
                        onFavorite = ::toggleFavorite, onExplore = { screen = Screen.SEARCH },
                        onQueue = ::addToQueue, onPlaylist = { addToPlaylist = it }, onShare = ::shareSong,
                    )
                    Screen.PROFILE -> OcarinaV5ProfileView(
                        account = account, favorites = favorites.size, recent = recent.size,
                        playlists = playlists,
                        onEdit = { screen = Screen.PROFILE_EDIT },
                        onLiked = { screen = Screen.LIKED },
                        onOpenPlaylist = {
                            selectedPlaylistId = it.id
                            playlistReturnScreen = Screen.PROFILE
                            screen = Screen.PLAYLIST_DETAIL
                        },
                        onShare = {
                            shareText("Compartilhar perfil", "Perfil Ocarina: " +
                                account.displayName.ifBlank { account.user?.displayName ?: "Usuário" })
                        },
                    )
                    Screen.PROFILE_EDIT -> OcarinaV5ProfileEdit(
                        account, dark,
                        onDark = { dark = it; settings.setDark(it) },
                        onBack = { screen = Screen.PROFILE },
                    )
                    Screen.PUBLIC_PROFILE -> selectedPublicProfile?.let { profile ->
                        OcarinaV5PublicProfile(profile,
                            onShare = { shareText("Compartilhar perfil", "Ocarina: ${profile.displayName}") })
                    }
                    Screen.PLAYLISTS -> OcarinaPlaylistsPage(playlists,
                        onCreate = { creatingPlaylist = true },
                        onOpen = {
                            selectedPlaylistId = it.id
                            playlistReturnScreen = Screen.PLAYLISTS
                            screen = Screen.PLAYLIST_DETAIL
                        })
                    Screen.PLAYLIST_DETAIL -> {
                        val list = playlists.firstOrNull { it.id == selectedPlaylistId }
                        if (list != null) OcarinaPlaylistDetailPage(list,
                            onCoverSelected = { uri ->
                                coroutineScope.launch {
                                    try {
                                        val cover = withContext(Dispatchers.IO) { playlistStore.saveCover(list.id, uri) }
                                        playlists.firstOrNull { it.id == list.id }?.let { current ->
                                            updatePlaylist(current.copy(cover = cover))
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, e.localizedMessage ?: "Erro na capa", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            onRename = { updatePlaylist(list.copy(name = it.trim().take(80))) },
                            onDelete = { savePlaylists(playlists.filterNot { it.id == list.id }); screen = Screen.PLAYLISTS },
                            onShare = { sharePlaylist(list) },
                            onPlay = { song -> playSong(song, list.songs) },
                            onRemove = { song -> updatePlaylist(list.copy(songs = list.songs.filterNot { it.videoId == song.videoId })) },
                            onAdd = {
                                screen = Screen.SEARCH
                                Toast.makeText(context, "Na Busca, abra os três pontos da faixa e escolha Playlist.",
                                    Toast.LENGTH_LONG).show()
                            },
                        )
                    }
                    Screen.ARTISTS, Screen.ALBUMS -> {
                        val kind = if (screen == Screen.ARTISTS) CatalogKind.ARTIST else CatalogKind.ALBUM
                        val local = (favorites + recent + offlineSongs).distinctBy { it.videoId }
                        val grouped = if (kind == CatalogKind.ARTIST) local.groupBy { it.artist }
                            else local.filter { it.album.isNotBlank() }.groupBy { it.album }
                        val entries = grouped.map { (name, songs) ->
                            CatalogEntry("local:${kind.name}:${name}", name,
                                if (kind == CatalogKind.ARTIST) "Artista salvo" else "Álbum salvo",
                                songs.firstOrNull()?.artwork.orEmpty(), kind)
                        }
                        OcarinaCatalogBrowsePage(kind, catalogQuery, { catalogQuery = it },
                            catalogResults, catalogLoading, catalogError, entries, ::openCatalogDetail)
                    }
                    Screen.CATALOG_DETAIL -> selectedCatalog?.let { entry ->
                        OcarinaCatalogDetailPage(entry, detailSongs, detailLoading, detailError,
                            onPlay = { song -> playSong(song, detailSongs) },
                            onShare = {
                                shareText("Compartilhar ${entry.kind.name.lowercase()}",
                                    "${entry.name} • Ocarina" +
                                        if (entry.browseId.startsWith("local:")) ""
                                        else "\nhttps://music.youtube.com/browse/${entry.browseId}")
                            },
                            onQueue = ::addToQueue,
                            onSave = { addToPlaylist = it })
                    }
                    Screen.DOWNLOADS -> OcarinaOfflinePage(offlineSongs, importing,
                        onImport = { offlinePicker.launch(arrayOf("audio/*")) },
                        onPlay = { song -> playSong(song, offlineSongs) },
                        onRemove = { offlineStore.remove(it); offlineSongs = offlineStore.load() },
                    )
                }
                if (switchingTabs) Box(Modifier.matchParentSize().background(c.accent.copy(alpha = 0.035f)))
                AnimatedVisibility(
                    visible = switchingTabs, modifier = Modifier.align(Alignment.Center),
                    enter = fadeIn(tween(20)), exit = fadeOut(tween(20)),
                ) { ThreeDots() }
            }
        }

        } // Descarta as listas pesadas enquanto o player está aberto.
        if (showFullPlayer && playback.currentSong != null) {
            NativePlayerScreen(
                playback = playback,
                isFavorite = favorites.any { it.videoId == playback.currentSong?.videoId },
                onFavorite = { playback.currentSong?.let(::toggleFavorite) },
                onClose = { showFullPlayer = false }, onNext = ::next, onPrevious = ::previous,
                onQueue = { showQueue = true },
                onRepeat = { playback.cycleRepeat() },
                onShare = { playback.currentSong?.let(::shareSong) },
            )
        }
        }
        if (showQueue) OcarinaQueueDialog(queue, playback.currentSong?.videoId, playback.repeatMode,
            onSelect = { song -> showQueue = false; playSong(song, queue) },
            onRemove = { song -> queue = queue.filterNot { it.videoId == song.videoId } },
            onDismiss = { showQueue = false })
        addToPlaylist?.let { song ->
            OcarinaPickPlaylistDialog(playlists, song,
                onPick = { list -> putInPlaylist(list, song); addToPlaylist = null },
                onCreate = { songForNewPlaylist = song; creatingPlaylist = true; addToPlaylist = null },
                onDismiss = { addToPlaylist = null })
        }
        if (creatingPlaylist) AlertDialog(onDismissRequest = { creatingPlaylist = false; songForNewPlaylist = null },
            title = { Text("Nova playlist") },
            text = { OutlinedTextField(value = newPlaylistName,
                onValueChange = { newPlaylistName = it.take(80) },
                singleLine = true, label = { Text("Nome") }) },
            confirmButton = { TextButton(onClick = {
                val list = playlistStore.create(newPlaylistName).copy(
                    songs = songForNewPlaylist?.let(::listOf).orEmpty())
                songForNewPlaylist = null
                savePlaylists(listOf(list) + playlists)
                selectedPlaylistId = list.id
                playlistReturnScreen = Screen.PLAYLISTS
                creatingPlaylist = false
                newPlaylistName = ""
                screen = Screen.PLAYLIST_DETAIL
            }, enabled = newPlaylistName.isNotBlank()) { Text("Criar") } },
            dismissButton = { TextButton(onClick = { creatingPlaylist = false; songForNewPlaylist = null }) { Text("Cancelar") } })
        if (importMessage != null) AlertDialog(onDismissRequest = { importMessage = null },
            title = { Text("Downloads") }, text = { Text(importMessage.orEmpty()) },
            confirmButton = { TextButton(onClick = { importMessage = null }) { Text("Entendi") } })
        if (showInfo) {
            AlertDialog(
                onDismissRequest = { showInfo = false },
                title = { Text("Ocarina") },
                text = { Text("Versão 0.5.0. Contas Google e e-mail pelo Firebase, foto e favoritos no Firestore. Psiu ainda não está integrado. Reprodução experimental via fontes externas.") },
                confirmButton = { TextButton(onClick = { showInfo = false }) { Text("Entendi") } },
            )
        }
        } // authenticated content
    }
}


/** Compact, icon-only navigation. Insets keep it clear of Android's gesture bar. */
@Composable
private fun FastNav(screen: Screen, onSelect: (Screen) -> Unit) {
    val c = ocarinaColors
    val tabs = remember {
        listOf(
            Triple(Screen.HOME, "Início", LucideR.drawable.lucide_ic_house),
            Triple(Screen.SEARCH, "Buscar", LucideR.drawable.lucide_ic_search),
            Triple(Screen.LIBRARY, "Biblioteca", LucideR.drawable.lucide_ic_library),
            Triple(Screen.LIKED, "Curtidas", LucideR.drawable.lucide_ic_heart),
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth().background(c.background)
            .navigationBarsPadding().height(52.dp).padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { (tab, label, drawable) ->
            val selected = tab == screen
            val tint by animateColorAsState(
                if (selected) c.accent else c.muted,
                animationSpec = tween(90), label = "navigation-tint",
            )
            val interaction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight()
                    .clickable(interactionSource = interaction, indication = null) {
                        if (screen != tab) onSelect(tab)
                    },
                contentAlignment = Alignment.Center,
            ) {
                LIcon(drawable, label, Modifier.size(if (selected) 25.dp else 24.dp), tint)
            }
        }
    }
}

/** Tiny three-dot 90ms navigation indicator. Shapes, not Unicode glyphs. */
@Composable
private fun ThreeDots() {
    val c = ocarinaColors
    var active by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        repeat(3) { step -> active = step; delay(30) }
    }
    Row(
        modifier = Modifier.clip(RoundedCornerShape(16.dp))
            .background(c.surface).padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { index ->
            Box(Modifier.size(if (index == 1) 6.dp else 5.dp).clip(CircleShape)
                .background(c.accent.copy(alpha = if (index == active) 1f else 0.30f)))
        }
    }
}

@Composable
private fun AppHeader(screen: Screen, onProfile: () -> Unit, onBack: () -> Unit, onSearch: () -> Unit) {
    val c = ocarinaColors
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(start = 18.dp, end = 18.dp, top = 13.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val nested = screen in setOf(Screen.PROFILE, Screen.PLAYLISTS, Screen.PLAYLIST_DETAIL,
            Screen.ARTISTS, Screen.ALBUMS, Screen.CATALOG_DETAIL, Screen.DOWNLOADS)
        if (nested) {
            IconButton(onClick = onBack) { LIcon(LucideR.drawable.lucide_ic_arrow_left, "Voltar", Modifier.size(23.dp)) }
        } else OcarinaMark(Modifier.size(48.dp))
        Spacer(Modifier.width(6.dp))
        if (nested) {
            val header = when (screen) {
                Screen.PROFILE -> "Perfil"
                Screen.PLAYLISTS, Screen.PLAYLIST_DETAIL -> "Playlists"
                Screen.ARTISTS -> "Artistas"
                Screen.ALBUMS -> "Álbuns"
                Screen.CATALOG_DETAIL -> "Explorar"
                Screen.DOWNLOADS -> "Downloads"
                else -> "Ocarina"
            }
            Text(header, color = c.text, fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold, letterSpacing = (-0.25).sp)
        }
        Spacer(Modifier.weight(1f))
        if (!nested && screen != Screen.SEARCH) {
            IconButton(onClick = onSearch) { LIcon(LucideR.drawable.lucide_ic_search, "Buscar", Modifier.size(22.dp)) }
        }
        if (!nested) {
            IconButton(onClick = onProfile) {
                Box(Modifier.size(37.dp).clip(CircleShape).background(c.elevated), contentAlignment = Alignment.Center) {
                    LIcon(LucideR.drawable.lucide_ic_user_round, "Abrir perfil", Modifier.size(19.dp))
                }
            }
        }
    }
}

@Composable
private fun HomePage(
    listState: LazyListState,
    songs: List<Song>, recent: List<Song>, favorites: List<Song>,
    playlists: List<OcarinaPlaylist>, onOpenPlaylist: (OcarinaPlaylist) -> Unit,
    loading: Boolean, error: String?, onRetry: () -> Unit,
    onPlay: (Song, List<Song>) -> Unit, onFavorite: (Song) -> Unit,
    onSearch: () -> Unit, onLibrary: () -> Unit,
    onQueue: (Song) -> Unit, onPlaylist: (Song) -> Unit, onShare: (Song) -> Unit,
) {
    val c = ocarinaColors
    LazyColumn(
        modifier = Modifier.fillMaxSize(), state = listState,
        contentPadding = PaddingValues(start = 17.dp, end = 17.dp, top = 8.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column {
                SectionHeader("Continue ouvindo", onClick = if (recent.isNotEmpty()) onLibrary else null)
                Spacer(Modifier.height(14.dp))
                if (recent.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(recent.take(6), key = { it.videoId }) { song ->
                            ArtworkCard(song, onClick = { onPlay(song, recent) })
                        }
                    }
                } else {
                    EmptyPanel("Seu histórico começa aqui", "Escolha uma música para encontrar suas últimas faixas neste espaço.",
                        LucideR.drawable.lucide_ic_headphones, onSearch, "Encontrar músicas")
                }
            }
        }
        item {
            Column {
                SectionHeader("Descobertas da semana", onClick = onSearch)
                Spacer(Modifier.height(5.dp))
                Text("Explore novas faixas do catálogo", color = c.muted, fontSize = 12.sp)
                Spacer(Modifier.height(13.dp))
                if (loading) LoadingRow()
                else if (error != null) ErrorPanel(error, onRetry)
                else if (songs.isEmpty()) EmptyPanel("Sem sugestões no momento", "Experimente pesquisar uma música.",
                    LucideR.drawable.lucide_ic_search, onSearch, "Buscar")
                else LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(songs.take(8), key = { it.videoId }) { song ->
                        ArtworkCard(song, onClick = { onPlay(song, songs) }, wide = true)
                    }
                }
            }
        }
        if (playlists.isNotEmpty()) item {
            Column {
                SectionHeader("Suas playlists", onClick = onLibrary)
                Spacer(Modifier.height(13.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                    items(playlists.take(6), key = { it.id }) { list ->
                        Column(Modifier.width(125.dp).clickable { onOpenPlaylist(list) }) {
                            OcarinaPlaylistCover(list, 125.dp)
                            Spacer(Modifier.height(8.dp))
                            Text(list.name, color = c.text, fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold, maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
                            Text("${list.songs.size} músicas", color = c.muted, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
        item {
            Column {
                SectionHeader("Músicas para explorar", onClick = onSearch)
                Spacer(Modifier.height(12.dp))
                songs.take(4).forEach { song ->
                    SongRow(song, favorites.any { it.videoId == song.videoId },
                        onOpen = { onPlay(song, songs) }, onFavorite = { onFavorite(song) },
                        onQueue = { onQueue(song) }, onPlaylist = { onPlaylist(song) },
                        onShare = { onShare(song) })
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, onClick: (() -> Unit)? = null) {
    val c = ocarinaColors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = c.text, fontSize = 21.sp, letterSpacing = (-0.35).sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        if (onClick != null) IconButton(onClick = onClick, modifier = Modifier.size(34.dp)) {
            LIcon(LucideR.drawable.lucide_ic_chevron_right, "Ver mais", Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ArtworkCard(song: Song, onClick: () -> Unit, wide: Boolean = false) {
    val c = ocarinaColors
    val size = if (wide) 178.dp else 142.dp
    Column(Modifier.width(size).clickable(onClick = onClick)) {
        Box(Modifier.size(size).clip(RoundedCornerShape(19.dp)).background(c.artworkSurface)) {
            AlbumArtwork(song, song.title, modifier = Modifier.fillMaxSize())
            Box(Modifier.align(Alignment.BottomEnd).padding(10.dp).size(36.dp)
                .clip(CircleShape).background(c.surface), contentAlignment = Alignment.Center) {
                LIcon(LucideR.drawable.lucide_ic_play, "Tocar ${song.title}", Modifier.size(19.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(song.title, color = c.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(song.artist, color = c.muted, fontSize = 11.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SearchPage(
    listState: LazyListState,
    text: String, onText: (String) -> Unit, songs: List<Song>, favorites: List<Song>,
    loading: Boolean, error: String?, onRetry: () -> Unit,
    onPlay: (Song) -> Unit, onFavorite: (Song) -> Unit,
    onQueue: (Song) -> Unit, onPlaylist: (Song) -> Unit, onShare: (Song) -> Unit,
) {
    val c = ocarinaColors
    LazyColumn(
        modifier = Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("O que vamos ouvir?", color = c.text, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = text, onValueChange = onText, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Músicas, artistas e álbuns", color = c.muted) },
                leadingIcon = { LIcon(LucideR.drawable.lucide_ic_search, null, Modifier.size(21.dp), c.accent) },
                trailingIcon = { if (text.isNotEmpty()) IconButton(onClick = { onText("") }) {
                    LIcon(LucideR.drawable.lucide_ic_x, "Limpar busca", Modifier.size(19.dp))
                } },
                singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                shape = RoundedCornerShape(17.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = c.accent, unfocusedBorderColor = c.outline,
                    focusedContainerColor = c.surface, unfocusedContainerColor = c.surface,
                ),
            )
            Spacer(Modifier.height(17.dp))
            Text(if (text.isBlank()) "Explore por aqui" else "Resultados", color = c.text,
                fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        }
        if (text.isBlank()) item { EmptyPanel("Uma nova descoberta começa na busca", "Pesquise sua próxima música favorita.", LucideR.drawable.lucide_ic_search) }
        else if (loading) item { LoadingRow() }
        else if (error != null) item { ErrorPanel(error, onRetry) }
        else if (songs.isEmpty()) item { EmptyPanel("Não encontramos resultados", "Tente outra música ou artista.", LucideR.drawable.lucide_ic_music_2) }
        else items(songs, key = { it.videoId }) { song ->
            SongRow(song, favorites.any { it.videoId == song.videoId },
                onOpen = { onPlay(song) }, onFavorite = { onFavorite(song) },
                onQueue = { onQueue(song) }, onPlaylist = { onPlaylist(song) },
                onShare = { onShare(song) })
        }
    }
}

@Composable
private fun LibraryPage(
    listState: LazyListState,
    favorites: List<Song>, recent: List<Song>,
    onLiked: () -> Unit, onPlay: (Song) -> Unit, onFavorite: (Song) -> Unit,
    onSearch: () -> Unit, onPlaylists: () -> Unit, onDownloads: () -> Unit,
    onArtists: () -> Unit, onAlbums: () -> Unit,
    onQueue: (Song) -> Unit, onPlaylist: (Song) -> Unit, onShare: (Song) -> Unit,
) {
    val c = ocarinaColors
    LazyColumn(
        modifier = Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        item {
            Text("Sua Biblioteca", fontSize = 31.sp, fontWeight = FontWeight.SemiBold, color = c.text)
            Spacer(Modifier.height(17.dp))
            LibraryEntry(LucideR.drawable.lucide_ic_heart, "Músicas Curtidas", "${favorites.size} músicas", onLiked)
            LibraryEntry(LucideR.drawable.lucide_ic_download, "Downloads", "Áudio offline do celular", onDownloads)
            LibraryEntry(LucideR.drawable.lucide_ic_list_music, "Playlists", "Suas listas de escuta", onPlaylists)
            LibraryEntry(LucideR.drawable.lucide_ic_disc_3, "Álbuns", "Capas e faixas", onAlbums)
            LibraryEntry(LucideR.drawable.lucide_ic_user_round, "Artistas", "Explore o catálogo", onArtists)
        }
        item {
            Spacer(Modifier.height(9.dp))
            SectionHeader("Tocadas recentemente")
        }
        if (recent.isEmpty()) item {
            EmptyPanel("Nenhuma música recente", "Quando você ouvir uma faixa, ela aparecerá aqui.",
                LucideR.drawable.lucide_ic_music_2, onSearch, "Explorar músicas")
        } else items(recent, key = { it.videoId }) { song ->
            SongRow(song, favorites.any { it.videoId == song.videoId },
                onOpen = { onPlay(song) }, onFavorite = { onFavorite(song) },
                onQueue = { onQueue(song) }, onPlaylist = { onPlaylist(song) },
                onShare = { onShare(song) })
        }
    }
}

@Composable
private fun LibraryEntry(@DrawableRes icon: Int, title: String, subtitle: String, onClick: () -> Unit) {
    val c = ocarinaColors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).clickable(onClick = onClick)
            .padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(15.dp)).background(c.elevated), contentAlignment = Alignment.Center) {
            LIcon(icon, null, Modifier.size(23.dp), c.text)
        }
        Spacer(Modifier.width(15.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = c.text)
            Text(subtitle, fontSize = 12.sp, color = c.muted)
        }
        LIcon(LucideR.drawable.lucide_ic_chevron_right, "Abrir", Modifier.size(19.dp), c.muted)
    }
}

@Composable
private fun LikedPage(listState: LazyListState, favorites: List<Song>, onPlay: (Song) -> Unit,
    onFavorite: (Song) -> Unit, onExplore: () -> Unit,
    onQueue: (Song) -> Unit, onPlaylist: (Song) -> Unit, onShare: (Song) -> Unit) {
    val c = ocarinaColors
    LazyColumn(
        modifier = Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Box(
                Modifier.fillMaxWidth().height(175.dp).clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(c.softAccent, c.artworkSurface))),
                contentAlignment = Alignment.Center,
            ) {
                LIcon(LucideR.drawable.lucide_ic_heart, null, Modifier.size(67.dp), c.accent)
            }
            Spacer(Modifier.height(17.dp))
            Text("Músicas Curtidas", color = c.text, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
            Text("${favorites.size} músicas salvas neste dispositivo", color = c.muted, fontSize = 12.sp)
            Spacer(Modifier.height(17.dp))
            if (favorites.isNotEmpty()) FilledIconButton(
                onClick = { onPlay(favorites.first()) },
                modifier = Modifier.size(54.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = c.accent, contentColor = c.onAccent),
            ) { LIcon(LucideR.drawable.lucide_ic_play, "Tocar todas", Modifier.size(23.dp), c.onAccent) }
        }
        if (favorites.isEmpty()) item {
            EmptyPanel("Suas favoritas vão morar aqui", "Toque no coração de uma música para salvá-la.",
                LucideR.drawable.lucide_ic_heart, onExplore, "Encontrar músicas")
        }
        else items(favorites, key = { it.videoId }) { song ->
            SongRow(song, true, onOpen = { onPlay(song) }, onFavorite = { onFavorite(song) },
                onQueue = { onQueue(song) }, onPlaylist = { onPlaylist(song) },
                onShare = { onShare(song) })
        }
    }
}

@Composable
private fun SongRow(song: Song, isFavorite: Boolean, onOpen: () -> Unit, onFavorite: () -> Unit,
    onQueue: (() -> Unit)? = null, onPlaylist: (() -> Unit)? = null, onShare: (() -> Unit)? = null) {
    val c = ocarinaColors
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpen).padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArtwork(song, song.title, small = true,
            modifier = Modifier.size(57.dp).clip(RoundedCornerShape(13.dp)).background(c.artworkSurface))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = c.text,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(song.artist, color = c.muted, fontSize = 12.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onFavorite, modifier = Modifier.size(41.dp)) {
            LIcon(LucideR.drawable.lucide_ic_heart,
                if (isFavorite) "Remover dos favoritos" else "Adicionar aos favoritos",
                Modifier.size(20.dp), if (isFavorite) c.accent else c.muted)
        }
        if (onQueue != null || onPlaylist != null || onShare != null) {
            Box {
                IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(37.dp)) {
                    LIcon(LucideR.drawable.lucide_ic_ellipsis, "Mais opções", Modifier.size(19.dp), c.muted)
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    if (onQueue != null) DropdownMenuItem(text = { Text("Adicionar à fila") },
                        onClick = { menuExpanded = false; onQueue() })
                    if (onPlaylist != null) DropdownMenuItem(text = { Text("Adicionar à playlist") },
                        onClick = { menuExpanded = false; onPlaylist() })
                    if (onShare != null) DropdownMenuItem(text = { Text("Compartilhar música") },
                        onClick = { menuExpanded = false; onShare() })
                }
            }
        } else LIcon(LucideR.drawable.lucide_ic_play, "Tocar", Modifier.size(20.dp), c.accent)
        Spacer(Modifier.width(4.dp))
    }
}

@Composable
private fun EmptyPanel(title: String, message: String, @DrawableRes icon: Int, onClick: (() -> Unit)? = null, label: String = "") {
    val c = ocarinaColors
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c.surface).padding(25.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LIcon(icon, null, Modifier.size(32.dp), c.accent)
        Spacer(Modifier.height(11.dp))
        Text(title, color = c.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(5.dp))
        Text(message, color = c.muted, fontSize = 12.sp, textAlign = TextAlign.Center)
        if (onClick != null) {
            Spacer(Modifier.height(13.dp))
            TextButton(onClick = onClick) { Text(label, color = c.accent) }
        }
    }
}

@Composable
private fun ErrorPanel(message: String, onRetry: () -> Unit) {
    val c = ocarinaColors
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.surface).padding(20.dp)) {
        Text("Não conseguimos carregar", color = c.text, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(5.dp))
        Text(message, color = c.muted, fontSize = 12.sp)
        TextButton(onClick = onRetry) {
            LIcon(LucideR.drawable.lucide_ic_refresh_cw, null, Modifier.size(16.dp), c.accent)
            Spacer(Modifier.width(8.dp))
            Text("Tentar novamente", color = c.accent)
        }
    }
}

@Composable
private fun LoadingRow() {
    val c = ocarinaColors
    Row(Modifier.fillMaxWidth().padding(19.dp), horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.size(23.dp), color = c.accent, strokeWidth = 2.dp)
        Spacer(Modifier.width(12.dp))
        Text("Carregando músicas...", color = c.muted, fontSize = 12.sp)
    }
}

@Composable
private fun MiniPlayer(song: Song, playback: PlaybackConnector, onExpand: () -> Unit) {
    val c = ocarinaColors
    Column(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 3.dp)
        .clip(RoundedCornerShape(16.dp)).background(c.elevated)) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onExpand).padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically) {
            AlbumArtwork(song, small = true, modifier = Modifier.size(44.dp).clip(RoundedCornerShape(9.dp)))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, color = c.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(if (playback.resolvingAudio) "Preparando áudio..." else song.artist,
                    color = c.muted, fontSize = 11.sp, maxLines = 1)
            }
            IconButton(onClick = { playback.toggle() }, enabled = !playback.resolvingAudio) {
                if (playback.resolvingAudio || playback.buffering) {
                    CircularProgressIndicator(Modifier.size(19.dp), color = c.accent, strokeWidth = 2.dp)
                } else LIcon(if (playback.playing) LucideR.drawable.lucide_ic_pause else LucideR.drawable.lucide_ic_play,
                    "Tocar ou pausar", Modifier.size(23.dp))
            }
        }
        if (playback.durationMs > 0) {
            LinearProgressIndicator(
                progress = { (playback.positionMs.toFloat() / playback.durationMs).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = c.accent, trackColor = c.outline,
            )
        }
        if (playback.error != null) Text(playback.error.orEmpty(), color = c.accent,
            fontSize = 10.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), maxLines = 2)
    }
}

@Composable
private fun NativePlayerScreen(
    playback: PlaybackConnector, isFavorite: Boolean, onFavorite: () -> Unit,
    onClose: () -> Unit, onNext: () -> Unit, onPrevious: () -> Unit,
    onQueue: () -> Unit, onRepeat: () -> Unit, onShare: () -> Unit,
) {
    val song = playback.currentSong ?: return
    val c = ocarinaColors
    Box(Modifier.fillMaxSize().background(c.background)) {
        Column(
            Modifier.fillMaxSize().background(c.background).statusBarsPadding().navigationBarsPadding()
                .padding(horizontal = 26.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { LIcon(LucideR.drawable.lucide_ic_chevron_down, "Minimizar", Modifier.size(28.dp)) }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TOCANDO AGORA", color = c.muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                    OcarinaMark(Modifier.size(19.dp), c.text)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onShare) { LIcon(LucideR.drawable.lucide_ic_share_2, "Compartilhar música", Modifier.size(22.dp)) }
            }
            Spacer(Modifier.weight(0.5f))
            BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val discSize = minOf(maxWidth * 0.91f, 350.dp)
                OcarinaV5Disc(song, discSize)
            }
            Spacer(Modifier.weight(0.65f))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(song.title, fontSize = 25.sp, lineHeight = 28.sp, color = c.text,
                        fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(5.dp))
                    Text(song.artist, color = c.muted, fontSize = 15.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onFavorite, enabled = !song.videoId.startsWith("local:")) {
                    LIcon(LucideR.drawable.lucide_ic_heart, "Curtir música", Modifier.size(27.dp),
                        if (isFavorite) c.accent else c.text)
                }
            }
            if (playback.error != null) {
                Spacer(Modifier.height(8.dp))
                Text(playback.error.orEmpty(), color = c.accent, fontSize = 12.sp)
                TextButton(onClick = { playback.retryCurrent() }) {
                    LIcon(LucideR.drawable.lucide_ic_refresh_cw, null, Modifier.size(16.dp), c.accent)
                    Spacer(Modifier.width(6.dp))
                    Text("Tentar novamente", color = c.accent)
                }
            }
            Spacer(Modifier.height(14.dp))
            val duration = playback.durationMs.coerceAtLeast(1L)
            var dragging by remember(song.videoId) { mutableStateOf<Float?>(null) }
            Slider(
                value = dragging ?: (playback.positionMs.toFloat() / duration).coerceIn(0f, 1f),
                onValueChange = { dragging = it },
                onValueChangeFinished = { dragging?.let { playback.seekTo((it * duration).toLong()) }; dragging = null },
                enabled = playback.durationMs > 0,
                colors = SliderDefaults.colors(thumbColor = c.accent, activeTrackColor = c.accent,
                    inactiveTrackColor = c.outline),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(playback.positionMs), color = c.muted, fontSize = 11.sp)
                Text(formatTime(playback.durationMs), color = c.muted, fontSize = 11.sp)
            }
            Spacer(Modifier.height(17.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevious) { LIcon(LucideR.drawable.lucide_ic_skip_back, "Anterior", Modifier.size(28.dp)) }
                FilledIconButton(onClick = { playback.toggle() }, modifier = Modifier.size(73.dp),
                    enabled = !playback.resolvingAudio,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = c.accent, contentColor = c.onAccent)) {
                    if (playback.resolvingAudio || playback.buffering) CircularProgressIndicator(
                        Modifier.size(26.dp), strokeWidth = 2.dp, color = c.onAccent)
                    else LIcon(if (playback.playing) LucideR.drawable.lucide_ic_pause else LucideR.drawable.lucide_ic_play,
                        "Tocar ou pausar", Modifier.size(32.dp), c.onAccent)
                }
                IconButton(onClick = onNext) { LIcon(LucideR.drawable.lucide_ic_skip_forward, "Próxima", Modifier.size(28.dp)) }
            }
            Spacer(Modifier.weight(0.35f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onRepeat) {
                    LIcon(if (playback.repeatMode == 1) LucideR.drawable.lucide_ic_repeat_1
                        else LucideR.drawable.lucide_ic_repeat,
                        "Repetição: ${when(playback.repeatMode) { 1 -> "faixa"; 2 -> "lista"; else -> "desligada" }}",
                        Modifier.size(23.dp), if (playback.repeatMode > 0) c.accent else c.muted)
                }
                Text(if (playback.sourceBitrateKbps > 0) "~${playback.sourceBitrateKbps} kbps"
                    else "Áudio original", color = c.muted, fontSize = 11.sp)
                IconButton(onClick = onQueue) {
                    LIcon(LucideR.drawable.lucide_ic_list_music, "Abrir fila de escuta", Modifier.size(23.dp), c.accent)
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val seconds = (ms / 1000).coerceAtLeast(0)
    return String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60)
}
