package com.ocarina.music.ui

import android.app.Activity
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import com.ocarina.music.R
import com.composables.icons.lucide.R as LucideR
import com.ocarina.music.data.FavoritesStore
import com.ocarina.music.data.OcarinaSettings
import com.ocarina.music.data.Song
import com.ocarina.music.data.YouTubeMusicSearch
import com.ocarina.music.playback.PlaybackConnector
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.util.Locale

private enum class Screen { HOME, SEARCH, LIBRARY, LIKED, PROFILE }

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
        val favorites = remember { mutableStateListOf<Song>().apply { addAll(store.load()) } }
        val recent = remember { mutableStateListOf<Song>().apply { addAll(settings.recentlyPlayed()) } }
        val playback = remember(context) { PlaybackConnector(context) }
        DisposableEffect(playback) { onDispose { playback.close() } }
        var screen by remember { mutableStateOf(Screen.HOME) }
        var previousScreen by remember { mutableStateOf(Screen.HOME) }
        var searchText by remember { mutableStateOf("") }
        var discoverSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
        var searchSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
        var discoveryLoading by remember { mutableStateOf(true) }
        var searchLoading by remember { mutableStateOf(false) }
        var discoverError by remember { mutableStateOf<String?>(null) }
        var searchError by remember { mutableStateOf<String?>(null) }
        var searchRefresh by remember { mutableIntStateOf(0) }
        var discoverRefresh by remember { mutableIntStateOf(0) }
        var showFullPlayer by remember { mutableStateOf(false) }
        var showInfo by remember { mutableStateOf(false) }
        var queue by remember { mutableStateOf<List<Song>>(emptyList()) }

        fun openProfile() {
            if (screen != Screen.PROFILE) previousScreen = screen
            screen = Screen.PROFILE
        }
        fun toggleFavorite(song: Song) {
            val index = favorites.indexOfFirst { it.videoId == song.videoId }
            if (index < 0) favorites.add(0, song) else favorites.removeAt(index)
            store.save(favorites.toList())
        }
        fun playSong(song: Song, source: List<Song>) {
            queue = source.ifEmpty { listOf(song) }
            val index = queue.indexOfFirst { it.videoId == song.videoId }
            playback.play(song, queue.getOrNull(index + 1))
            recent.removeAll { it.videoId == song.videoId }
            recent.add(0, song)
            if (recent.size > 12) recent.removeAt(recent.lastIndex)
            settings.addRecent(song)
            showFullPlayer = true
        }
        fun next() {
            val i = queue.indexOfFirst { it.videoId == playback.currentSong?.videoId }
            queue.getOrNull(i + 1)?.let { playback.play(it, queue.getOrNull(i + 2)) }
        }
        fun previous() {
            val i = queue.indexOfFirst { it.videoId == playback.currentSong?.videoId }
            queue.getOrNull(i - 1)?.let { playback.play(it, queue.getOrNull(i)) }
        }

        LaunchedEffect(discoverRefresh) {
            discoveryLoading = true
            discoverError = null
            try { discoverSongs = searchClient.search("música brasileira contemporânea") }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { discoverError = e.message ?: "Não foi possível consultar o catálogo." }
            finally { discoveryLoading = false }
        }
        LaunchedEffect(screen, searchText, searchRefresh) {
            if (screen != Screen.SEARCH) return@LaunchedEffect
            val query = searchText.trim()
            if (query.isBlank()) { searchSongs = emptyList(); searchError = null; searchLoading = false; return@LaunchedEffect }
            delay(350)
            searchLoading = true
            searchError = null
            try { searchSongs = searchClient.search(query) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { searchSongs = emptyList(); searchError = e.message ?: "Não foi possível buscar." }
            finally { searchLoading = false }
        }

        Scaffold(
            containerColor = c.background,
            topBar = {
                AppHeader(
                    screen = screen,
                    onProfile = ::openProfile,
                    onBack = { screen = previousScreen },
                    onSearch = { screen = Screen.SEARCH },
                )
            },
            bottomBar = {
                Column(Modifier.background(c.background)) {
                    playback.currentSong?.let { song ->
                        MiniPlayer(song, playback, onExpand = { showFullPlayer = true })
                    }
                    NavigationBar(containerColor = c.background, tonalElevation = 0.dp, modifier = Modifier.height(76.dp)) {
                        listOf(
                            Triple(Screen.HOME, "Início", LucideR.drawable.lucide_ic_house),
                            Triple(Screen.SEARCH, "Buscar", LucideR.drawable.lucide_ic_search),
                            Triple(Screen.LIBRARY, "Biblioteca", LucideR.drawable.lucide_ic_library),
                            Triple(Screen.LIKED, "Curtidas", LucideR.drawable.lucide_ic_heart),
                        ).forEach { (tab, label, icon) ->
                            val active = screen == tab
                            NavigationBarItem(
                                selected = active,
                                onClick = { screen = tab },
                                icon = { LIcon(icon, label, Modifier.size(23.dp), if (active) c.accent else c.muted) },
                                label = { Text(label, fontSize = 10.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal) },
                                alwaysShowLabel = true,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = c.accent,
                                    selectedTextColor = c.accent,
                                    indicatorColor = c.softAccent,
                                    unselectedIconColor = c.muted,
                                    unselectedTextColor = c.muted,
                                ),
                            )
                        }
                    }
                }
            },
        ) { insets ->
            AnimatedContent(
                targetState = screen,
                modifier = Modifier.fillMaxSize().padding(insets),
                transitionSpec = { fadeIn(tween(170)) togetherWith fadeOut(tween(100)) },
                label = "ocarina-screen",
            ) { page ->
                when (page) {
                    Screen.HOME -> HomePage(
                        songs = discoverSongs, recent = recent, favorites = favorites,
                        loading = discoveryLoading, error = discoverError,
                        onRetry = { discoverRefresh++ },
                        onPlay = { song, source -> playSong(song, source) },
                        onFavorite = ::toggleFavorite,
                        onSearch = { screen = Screen.SEARCH },
                        onLibrary = { screen = Screen.LIBRARY },
                    )
                    Screen.SEARCH -> SearchPage(
                        text = searchText, onText = { searchText = it }, songs = searchSongs,
                        favorites = favorites, loading = searchLoading, error = searchError,
                        onRetry = { searchRefresh++ },
                        onPlay = { song -> playSong(song, searchSongs) }, onFavorite = ::toggleFavorite,
                    )
                    Screen.LIBRARY -> LibraryPage(
                        favorites = favorites, recent = recent,
                        onLiked = { screen = Screen.LIKED }, onPlay = { song -> playSong(song, recent) },
                        onFavorite = ::toggleFavorite, onSearch = { screen = Screen.SEARCH }, onInfo = { showInfo = true },
                    )
                    Screen.LIKED -> LikedPage(
                        favorites = favorites, onPlay = { song -> playSong(song, favorites.toList()) },
                        onFavorite = ::toggleFavorite, onExplore = { screen = Screen.SEARCH },
                    )
                    Screen.PROFILE -> ProfilePage(
                        isDark = dark, favorites = favorites.size, recent = recent.size,
                        onTheme = { value -> dark = value; settings.setDark(value) },
                        onInfo = { showInfo = true }, onLiked = { screen = Screen.LIKED },
                    )
                }
            }
        }

        if (showFullPlayer && playback.currentSong != null) {
            NativePlayerScreen(
                playback = playback,
                isFavorite = favorites.any { it.videoId == playback.currentSong?.videoId },
                onFavorite = { playback.currentSong?.let(::toggleFavorite) },
                onClose = { showFullPlayer = false }, onNext = ::next, onPrevious = ::previous,
            )
        }
        if (showInfo) {
            AlertDialog(
                onDismissRequest = { showInfo = false },
                title = { Text("Ocarina") },
                text = { Text("Versão 0.3.0. Seu perfil é local; login Firebase e integração com Psiu ainda não estão ativos. Reprodução experimental via fontes não oficiais do YouTube Music. Recursos de playlist e download serão adicionados futuramente.") },
                confirmButton = { TextButton(onClick = { showInfo = false }) { Text("Entendi") } },
            )
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
        if (screen == Screen.PROFILE) {
            IconButton(onClick = onBack) { LIcon(LucideR.drawable.lucide_ic_arrow_left, "Voltar", Modifier.size(23.dp)) }
        } else {
            OcarinaMark(Modifier.size(48.dp))
        }
        Spacer(Modifier.width(6.dp))
        Text(if (screen == Screen.PROFILE) "Perfil" else "Ocarina", color = c.text,
            fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.8).sp)
        Spacer(Modifier.weight(1f))
        if (screen != Screen.SEARCH && screen != Screen.PROFILE) {
            IconButton(onClick = onSearch) { LIcon(LucideR.drawable.lucide_ic_search, "Buscar", Modifier.size(22.dp)) }
        }
        if (screen != Screen.PROFILE) {
            IconButton(onClick = onProfile) {
                Box(Modifier.size(37.dp).clip(CircleShape).background(c.elevated), contentAlignment = Alignment.Center) {
                    LIcon(LucideR.drawable.lucide_ic_user_round, "Abrir perfil", Modifier.size(19.dp))
                }
            }
        }
    }
}

@Composable
private fun Greeting() {
    val time = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when (time) { in 5..11 -> "Bom dia"; in 12..17 -> "Boa tarde"; else -> "Boa noite" }
    Text(greeting, color = ocarinaColors.text, fontSize = 33.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1.3).sp)
}

@Composable
private fun HomePage(
    songs: List<Song>, recent: List<Song>, favorites: List<Song>,
    loading: Boolean, error: String?, onRetry: () -> Unit,
    onPlay: (Song, List<Song>) -> Unit, onFavorite: (Song) -> Unit,
    onSearch: () -> Unit, onLibrary: () -> Unit,
) {
    val c = ocarinaColors
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 21.dp, end = 21.dp, top = 12.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { Greeting() }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                items(listOf("Início", "Músicas", "Playlists", "Rádios")) { title ->
                    val active = title == "Início"
                    Surface(
                        onClick = { if (title == "Playlists") onLibrary() else if (!active) onSearch() },
                        shape = RoundedCornerShape(50),
                        color = if (active) c.accent else c.surface,
                        border = if (active) null else BorderStroke(1.dp, c.outline),
                    ) {
                        Text(title, modifier = Modifier.padding(horizontal = 17.dp, vertical = 10.dp),
                            color = if (active) c.onAccent else c.text,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                            fontSize = 13.sp)
                    }
                }
            }
        }
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
                    items(songs.take(10), key = { it.videoId }) { song ->
                        ArtworkCard(song, onClick = { onPlay(song, songs) }, wide = true)
                    }
                }
            }
        }
        item {
            Column {
                SectionHeader("Músicas para explorar", onClick = onSearch)
                Spacer(Modifier.height(12.dp))
                songs.take(5).forEach { song ->
                    SongRow(song, favorites.any { it.videoId == song.videoId },
                        onOpen = { onPlay(song, songs) }, onFavorite = { onFavorite(song) })
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, onClick: (() -> Unit)? = null) {
    val c = ocarinaColors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = c.text, fontSize = 21.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
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
            AsyncImage(model = song.artwork, contentDescription = song.title,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
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
    text: String, onText: (String) -> Unit, songs: List<Song>, favorites: List<Song>,
    loading: Boolean, error: String?, onRetry: () -> Unit,
    onPlay: (Song) -> Unit, onFavorite: (Song) -> Unit,
) {
    val c = ocarinaColors
    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("O que vamos ouvir?", color = c.text, fontSize = 28.sp, fontWeight = FontWeight.Bold)
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
                fontSize = 19.sp, fontWeight = FontWeight.Bold)
        }
        if (text.isBlank()) item { EmptyPanel("Uma nova descoberta começa na busca", "Pesquise sua próxima música favorita.", LucideR.drawable.lucide_ic_search) }
        else if (loading) item { LoadingRow() }
        else if (error != null) item { ErrorPanel(error, onRetry) }
        else if (songs.isEmpty()) item { EmptyPanel("Não encontramos resultados", "Tente outra música ou artista.", LucideR.drawable.lucide_ic_music_2) }
        else items(songs, key = { it.videoId }) { song ->
            SongRow(song, favorites.any { it.videoId == song.videoId },
                onOpen = { onPlay(song) }, onFavorite = { onFavorite(song) })
        }
    }
}

@Composable
private fun LibraryPage(
    favorites: List<Song>, recent: List<Song>,
    onLiked: () -> Unit, onPlay: (Song) -> Unit, onFavorite: (Song) -> Unit,
    onSearch: () -> Unit, onInfo: () -> Unit,
) {
    val c = ocarinaColors
    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        item {
            Text("Sua Biblioteca", fontSize = 31.sp, fontWeight = FontWeight.Bold, color = c.text)
            Spacer(Modifier.height(17.dp))
            LibraryEntry(LucideR.drawable.lucide_ic_heart, "Músicas Curtidas", "${favorites.size} músicas", onLiked)
            LibraryEntry(LucideR.drawable.lucide_ic_download, "Downloads", "Em desenvolvimento", onInfo)
            LibraryEntry(LucideR.drawable.lucide_ic_list_music, "Playlists", "Em desenvolvimento", onInfo)
            LibraryEntry(LucideR.drawable.lucide_ic_disc_3, "Álbuns", "Em desenvolvimento", onInfo)
            LibraryEntry(LucideR.drawable.lucide_ic_user_round, "Artistas", "Em desenvolvimento", onInfo)
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
                onOpen = { onPlay(song) }, onFavorite = { onFavorite(song) })
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
private fun LikedPage(favorites: List<Song>, onPlay: (Song) -> Unit, onFavorite: (Song) -> Unit, onExplore: () -> Unit) {
    val c = ocarinaColors
    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
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
            Text("Músicas Curtidas", color = c.text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
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
            SongRow(song, true, onOpen = { onPlay(song) }, onFavorite = { onFavorite(song) })
        }
    }
}

@Composable
private fun ProfilePage(isDark: Boolean, favorites: Int, recent: Int, onTheme: (Boolean) -> Unit, onInfo: () -> Unit, onLiked: () -> Unit) {
    val c = ocarinaColors
    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 21.dp, end = 21.dp, top = 14.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Column {
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(29.dp)).background(c.surface)
                        .padding(horizontal = 22.dp, vertical = 26.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(95.dp).clip(CircleShape).background(c.softAccent),
                            contentAlignment = Alignment.Center) {
                            LIcon(LucideR.drawable.lucide_ic_user_round, "Avatar", Modifier.size(48.dp), c.accent)
                        }
                        Spacer(Modifier.height(15.dp))
                        Text("Seu espaço", fontSize = 26.sp, color = c.text, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(5.dp))
                        Text("Perfil local, só seu por enquanto", color = c.muted, fontSize = 13.sp)
                        Spacer(Modifier.height(22.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            ProfileStat(favorites.toString(), "Curtidas")
                            Box(Modifier.width(1.dp).height(37.dp).background(c.outline))
                            ProfileStat(recent.toString(), "Recentes")
                            Box(Modifier.width(1.dp).height(37.dp).background(c.outline))
                            ProfileStat("0", "Playlists")
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Surface(
                    onClick = onInfo, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(17.dp), color = c.softAccent,
                ) {
                    Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                        LIcon(LucideR.drawable.lucide_ic_log_in, null, Modifier.size(22.dp), c.accent)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Entrar ou criar conta", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = c.text)
                            Text("Firebase será conectado na próxima etapa", fontSize = 11.sp, color = c.muted)
                        }
                        LIcon(LucideR.drawable.lucide_ic_chevron_right, null, Modifier.size(18.dp), c.accent)
                    }
                }
            }
        }
        item {
            Column {
                SectionHeader("Preferências")
                Spacer(Modifier.height(7.dp))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.surface).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    LIcon(if (isDark) LucideR.drawable.lucide_ic_moon else LucideR.drawable.lucide_ic_sun,
                        null, Modifier.size(24.dp), c.accent)
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Modo escuro", fontSize = 15.sp, color = c.text, fontWeight = FontWeight.SemiBold)
                        Text("Altere o tema instantaneamente", fontSize = 11.sp, color = c.muted)
                    }
                    Switch(checked = isDark, onCheckedChange = onTheme)
                }
                Spacer(Modifier.height(10.dp))
                Surface(onClick = onLiked, shape = RoundedCornerShape(18.dp), color = c.surface) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        LIcon(LucideR.drawable.lucide_ic_heart, null, Modifier.size(23.dp), c.accent)
                        Spacer(Modifier.width(13.dp))
                        Text("Minhas curtidas", modifier = Modifier.weight(1f), color = c.text, fontSize = 15.sp)
                        LIcon(LucideR.drawable.lucide_ic_chevron_right, null, Modifier.size(19.dp), c.muted)
                    }
                }
            }
        }
        item {
            Column {
                SectionHeader("Conexões")
                Spacer(Modifier.height(7.dp))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.surface).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    LIcon(LucideR.drawable.lucide_ic_cloud, null, Modifier.size(22.dp), c.accent)
                    Spacer(Modifier.width(13.dp))
                    Column {
                        Text("Integração com Psiu", fontWeight = FontWeight.SemiBold, color = c.text)
                        Text("Planejada para uma versão futura", color = c.muted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileStat(value: String, caption: String) {
    val c = ocarinaColors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = c.text)
        Text(caption, fontSize = 11.sp, color = c.muted)
    }
}

@Composable
private fun SongRow(song: Song, isFavorite: Boolean, onOpen: () -> Unit, onFavorite: () -> Unit) {
    val c = ocarinaColors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpen).padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(model = song.artwork, contentDescription = song.title, contentScale = ContentScale.Crop,
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
        LIcon(LucideR.drawable.lucide_ic_play, "Tocar", Modifier.size(20.dp), c.accent)
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
        Text(title, color = c.text, fontWeight = FontWeight.Bold, fontSize = 15.sp, textAlign = TextAlign.Center)
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
        Text("Não conseguimos carregar", color = c.text, fontWeight = FontWeight.Bold)
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
            AsyncImage(model = song.artwork, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(9.dp)))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, color = c.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
) {
    val song = playback.currentSong ?: return
    val c = ocarinaColors
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxSize().background(c.background).statusBarsPadding().navigationBarsPadding()
                .padding(horizontal = 26.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { LIcon(LucideR.drawable.lucide_ic_chevron_down, "Minimizar", Modifier.size(28.dp)) }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TOCANDO AGORA", color = c.muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("Ocarina", color = c.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onClose) { LIcon(LucideR.drawable.lucide_ic_ellipsis, "Minimizar", Modifier.size(22.dp)) }
            }
            Spacer(Modifier.weight(0.5f))
            BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val coverSize = minOf(maxWidth, 365.dp)
                AsyncImage(model = song.artwork, contentDescription = "Capa de ${song.title}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(coverSize).shadow(15.dp, RoundedCornerShape(23.dp))
                        .clip(RoundedCornerShape(23.dp)).background(c.artworkSurface))
            }
            Spacer(Modifier.weight(0.65f))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(song.title, fontSize = 25.sp, lineHeight = 28.sp, color = c.text,
                        fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(5.dp))
                    Text(song.artist, color = c.muted, fontSize = 15.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onFavorite) {
                    LIcon(LucideR.drawable.lucide_ic_heart, "Curtir música", Modifier.size(27.dp),
                        if (isFavorite) c.accent else c.text)
                }
            }
            if (playback.error != null) {
                Spacer(Modifier.height(8.dp))
                Text(playback.error.orEmpty(), color = c.accent, fontSize = 12.sp)
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
                LIcon(LucideR.drawable.lucide_ic_audio_lines, null, Modifier.size(21.dp), c.muted)
                Text(if (playback.sourceBitrateKbps > 0) "Áudio original: ${playback.sourceBitrateKbps} kbps (estimado)"
                    else "Áudio original da fonte", color = c.muted, fontSize = 11.sp)
                LIcon(LucideR.drawable.lucide_ic_list_music, null, Modifier.size(21.dp), c.muted)
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val seconds = (ms / 1000).coerceAtLeast(0)
    return String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60)
}
