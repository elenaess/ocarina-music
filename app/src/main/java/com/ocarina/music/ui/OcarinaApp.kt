package com.ocarina.music.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowOutward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ocarina.music.data.FavoritesStore
import com.ocarina.music.data.Song
import com.ocarina.music.data.YouTubeMusicSearch
import com.ocarina.music.playback.PlaybackConnector
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.util.Locale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private enum class Tab { DISCOVER, SEARCH, LIBRARY }

@Composable
fun OcarinaApp() = OcarinaTheme {
    val context = LocalContext.current
    val searchClient = remember { YouTubeMusicSearch() }
    val store = remember { FavoritesStore(context) }
    val favorites = remember { mutableStateListOf<Song>().apply { addAll(store.load()) } }
    var tab by remember { mutableStateOf(Tab.DISCOVER) }
    var searchText by remember { mutableStateOf("") }
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadIndex by remember { mutableIntStateOf(0) }
    val playback = remember { PlaybackConnector(context) }
    DisposableEffect(playback) { onDispose { playback.close() } }
    var showFullPlayer by remember { mutableStateOf(false) }
    var playQueue by remember { mutableStateOf<List<Song>>(emptyList()) }
    fun playSong(song: Song) {
        val source = if (tab == Tab.LIBRARY) favorites.toList() else songs
        playQueue = source.ifEmpty { listOf(song) }
        val idx = playQueue.indexOfFirst { it.videoId == song.videoId }
        playback.play(song, playQueue.getOrNull(idx + 1))
        showFullPlayer = true
    }
    fun nextSong() {
        val currentId = playback.currentSong?.videoId
        val idx = playQueue.indexOfFirst { it.videoId == currentId }
        playQueue.getOrNull(idx + 1)?.let { playback.play(it, playQueue.getOrNull(idx + 2)) }
    }
    fun previousSong() {
        val currentId = playback.currentSong?.videoId
        val idx = playQueue.indexOfFirst { it.videoId == currentId }
        playQueue.getOrNull(idx - 1)?.let { playback.play(it, playQueue.getOrNull(idx)) }
    }
    var showInfo by remember { mutableStateOf(false) }

    fun toggleFavorite(song: Song) {
        val match = favorites.indexOfFirst { it.videoId == song.videoId }
        if (match >= 0) favorites.removeAt(match) else favorites.add(0, song)
        store.save(favorites.toList())
    }

    LaunchedEffect(tab, searchText, reloadIndex) {
        if (tab == Tab.LIBRARY) return@LaunchedEffect
        val term = if (tab == Tab.DISCOVER) "música brasileira contemporânea" else searchText.trim()
        if (term.isBlank()) {
            songs = emptyList()
            error = null
            isLoading = false
            return@LaunchedEffect
        }
        if (tab == Tab.SEARCH) delay(400)
        isLoading = true
        error = null
        try {
            songs = searchClient.search(term)
            if (songs.isEmpty()) error = "Nenhuma faixa encontrada. Experimente outra busca."
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            songs = emptyList()
            error = e.message ?: "Não foi possível acessar o catálogo agora."
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        containerColor = Ink,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(start = 22.dp, end = 18.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(43.dp).clip(RoundedCornerShape(16.dp)).background(Lime),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("o", fontWeight = FontWeight.Black, fontSize = 30.sp, color = Ink, lineHeight = 30.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("ocarina", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = WarmWhite, lineHeight = 24.sp)
                    Text("o seu som, no seu ritmo.", fontSize = 11.sp, color = Muted)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { showInfo = true }) {
                    Icon(Icons.Rounded.Info, "Sobre o app", tint = Muted)
                }
            }
        },
        bottomBar = {
            Column {
                playback.currentSong?.let { playingSong ->
                    MiniPlayer(playingSong, playback, onExpand = { showFullPlayer = true }, onNext = ::nextSong)
                }
            NavigationBar(containerColor = Card, tonalElevation = 0.dp) {
                listOf(
                    Triple(Tab.DISCOVER, "Descobrir", Icons.Rounded.Explore),
                    Triple(Tab.SEARCH, "Buscar", Icons.Rounded.Search),
                    Triple(Tab.LIBRARY, "Biblioteca", Icons.Rounded.LibraryMusic),
                ).forEach { (value, label, icon) ->
                    NavigationBarItem(
                        selected = tab == value,
                        onClick = { tab = value },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, fontSize = 11.sp) },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Ink,
                            selectedTextColor = Lime,
                            indicatorColor = Lime,
                            unselectedIconColor = Muted,
                            unselectedTextColor = Muted,
                        ),
                    )
                }
            }
            }
        },
    ) { padding ->
        AnimatedContent(
            targetState = tab,
            modifier = Modifier.fillMaxSize().padding(padding),
            transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(110)) },
            label = "ocarina-navigation",
        ) { page ->
            when (page) {
                Tab.DISCOVER -> CatalogPage(
                    songs = songs,
                    favorites = favorites,
                    isLoading = isLoading,
                    error = error,
                    onRetry = { reloadIndex++ },
                    onOpen = ::playSong,
                    onToggle = ::toggleFavorite,
                    onSearchClick = { tab = Tab.SEARCH },
                )
                Tab.SEARCH -> SearchPage(
                    searchText = searchText,
                    onSearchChange = { searchText = it },
                    songs = songs,
                    favorites = favorites,
                    isLoading = isLoading,
                    error = error,
                    onRetry = { reloadIndex++ },
                    onOpen = ::playSong,
                    onToggle = ::toggleFavorite,
                )
                Tab.LIBRARY -> LibraryPage(
                    favorites = favorites,
                    onOpen = ::playSong,
                    onToggle = ::toggleFavorite,
                    onExplore = { tab = Tab.SEARCH },
                )
            }
        }
    }

    if (showFullPlayer && playback.currentSong != null) {
        NativePlayerScreen(
            playback = playback,
            isFavorite = favorites.any { it.videoId == playback.currentSong?.videoId },
            onFavorite = { playback.currentSong?.let(::toggleFavorite) },
            onClose = { showFullPlayer = false },
            onNext = ::nextSong,
            onPrevious = ::previousSong,
        )
    }
    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("Sobre o Ocarina Music") },
            text = { Text(
                "Versão 0.2.1 · Player nativo de áudio (Media3). Catálogo e fonte musical experimentais " +
                    "via YouTube Music / NewPipe. Algumas músicas podem não funcionar. " +
                    "Favoritos são armazenados no dispositivo. Ocarina não é afiliado ao YouTube ou Spotify."
            ) },
            confirmButton = { TextButton(onClick = { showInfo = false }) { Text("Entendi") } },
        )
    }

}

@Composable
private fun CatalogPage(
    songs: List<Song>, favorites: List<Song>, isLoading: Boolean, error: String?,
    onRetry: () -> Unit, onOpen: (Song) -> Unit, onToggle: (Song) -> Unit,
    onSearchClick: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(Modifier.height(12.dp))
            HeroCard(onSearchClick)
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Para descobrir", color = WarmWhite, fontWeight = FontWeight.Bold, fontSize = 25.sp)
                    Text("Uma seleção direto do catálogo musical", color = Muted, fontSize = 13.sp)
                }
                Icon(Icons.Rounded.GraphicEq, null, tint = Lime)
            }
            Spacer(Modifier.height(8.dp))
        }
        catalogItems(songs, favorites, isLoading, error, onRetry, onOpen, onToggle)
    }
}

@Composable
private fun HeroCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().height(222.dp)
            .clip(RoundedCornerShape(29.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFD5F2BA), Color(0xFF92BC9B), Color(0xFF50806E))))
            .clickable(onClick = onClick)
    ) {
        Box(Modifier.align(Alignment.TopEnd).offset(x = 36.dp, y = (-50).dp)
            .size(210.dp).clip(CircleShape).background(Color.White.copy(alpha = .12f)))
        Box(Modifier.align(Alignment.BottomEnd).offset(x = 55.dp, y = 73.dp)
            .size(180.dp).clip(CircleShape).background(Ink.copy(alpha = .15f)))
        Column(Modifier.fillMaxHeight().padding(25.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Headphones, null, tint = Ink)
                Spacer(Modifier.width(7.dp))
                Text("SEU SOM. SUAS REGRAS.", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Ink, letterSpacing = 1.1.sp)
            }
            Text("A música\né sua.", fontSize = 41.sp, lineHeight = 41.sp, fontWeight = FontWeight.Black, color = Ink)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Explore o que te move", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Rounded.ArrowOutward, null, tint = Ink, modifier = Modifier.size(17.dp))
            }
        }
    }
}

@Composable
private fun SearchPage(
    searchText: String, onSearchChange: (String) -> Unit,
    songs: List<Song>, favorites: List<Song>, isLoading: Boolean, error: String?,
    onRetry: () -> Unit, onOpen: (Song) -> Unit, onToggle: (Song) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("O que vamos ouvir?", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = WarmWhite)
            Spacer(Modifier.height(17.dp))
            OutlinedTextField(
                value = searchText,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                placeholder = { Text("Músicas, artistas, álbuns…", color = Muted) },
                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Lime) },
                trailingIcon = {
                    if (searchText.isNotEmpty()) IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Rounded.Close, "Limpar busca")
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Lime,
                    unfocusedBorderColor = Raised,
                    focusedContainerColor = Card,
                    unfocusedContainerColor = Card,
                ),
            )
            Spacer(Modifier.height(22.dp))
            Text(
                if (searchText.isBlank()) "Encontre sua próxima favorita" else "Resultados para \"${searchText.take(40)}\"",
                color = WarmWhite, fontSize = 19.sp, fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
        }
        if (searchText.isBlank()) {
            item { EmptyState("Busque por um artista ou uma música.", "Sem conta, sem complicação.") }
        } else {
            catalogItems(songs, favorites, isLoading, error, onRetry, onOpen, onToggle)
        }
    }
}

@Composable
private fun LibraryPage(
    favorites: List<Song>, onOpen: (Song) -> Unit, onToggle: (Song) -> Unit,
    onExplore: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Sua biblioteca", fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("O que você ama, num só lugar.", color = Muted, fontSize = 13.sp)
            Spacer(Modifier.height(27.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Favorite, null, tint = Lime)
                Spacer(Modifier.width(10.dp))
                Text("Curtidas", fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text("${favorites.size} faixa(s)", color = Muted, fontSize = 12.sp)
            }
        }
        if (favorites.isEmpty()) {
            item {
                EmptyState("Ainda não tem nada por aqui.", "Curta algumas músicas para encontrá-las depois.")
                Spacer(Modifier.height(12.dp))
                Button(onClick = onExplore, colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink)) {
                    Text("Explorar músicas")
                }
            }
        } else {
            items(favorites, key = { it.videoId }) { song ->
                SongRow(song, isFavorite = true, onOpen = { onOpen(song) }, onFavorite = { onToggle(song) })
            }
        }
    }
}

/** Shared result section, so all screens use the same consistent song rows. */
private fun androidx.compose.foundation.lazy.LazyListScope.catalogItems(
    songs: List<Song>, favorites: List<Song>, isLoading: Boolean, error: String?,
    onRetry: () -> Unit, onOpen: (Song) -> Unit, onToggle: (Song) -> Unit,
) {
    if (isLoading) {
        item {
            Row(Modifier.fillMaxWidth().padding(26.dp), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator(color = Lime, strokeWidth = 3.dp, modifier = Modifier.size(25.dp))
                Spacer(Modifier.width(16.dp))
                Text("Buscando faixas…", color = Muted)
            }
        }
    } else if (error != null) {
        item { ErrorState(error, onRetry) }
    } else {
        items(songs, key = { it.videoId }) { song ->
            SongRow(
                song = song,
                isFavorite = favorites.any { it.videoId == song.videoId },
                onOpen = { onOpen(song) },
                onFavorite = { onToggle(song) },
            )
        }
    }
}

@Composable
private fun SongRow(song: Song, isFavorite: Boolean, onOpen: () -> Unit, onFavorite: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onOpen).padding(vertical = 7.dp, horizontal = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(60.dp).clip(RoundedCornerShape(13.dp)).background(Raised)) {
            AsyncImage(
                model = song.artwork,
                contentDescription = "Capa de ${song.title}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = WarmWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Text(song.artist, maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = Muted, fontSize = 12.sp)
        }
        IconButton(onClick = onFavorite, modifier = Modifier.size(42.dp)) {
            Icon(
                if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = if (isFavorite) "Remover dos favoritos" else "Adicionar aos favoritos",
                tint = if (isFavorite) Lime else Muted,
                modifier = Modifier.size(21.dp),
            )
        }
        Icon(Icons.Rounded.PlayArrow, contentDescription = "Abrir player", tint = Lime, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(4.dp))
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Rounded.MusicNote, null, tint = Lime, modifier = Modifier.size(44.dp))
        Spacer(Modifier.height(15.dp))
        Text(title, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold, color = WarmWhite)
        Spacer(Modifier.height(5.dp))
        Text(subtitle, textAlign = TextAlign.Center, color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(Card).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Não conseguimos carregar agora", fontWeight = FontWeight.Bold, color = WarmWhite)
        Spacer(Modifier.height(9.dp))
        Text(message, color = Muted, textAlign = TextAlign.Center, fontSize = 12.sp)
        Spacer(Modifier.height(14.dp))
        TextButton(onClick = onRetry) {
            Icon(Icons.Rounded.Refresh, null, tint = Lime)
            Spacer(Modifier.width(5.dp))
            Text("Tentar novamente", color = Lime)
        }
    }
}


/** Barra compacta de áudio, sempre visível acima da navegação como no Spotify. */
@Composable
private fun MiniPlayer(song: Song, playback: PlaybackConnector, onExpand: () -> Unit, onNext: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(15.dp)).background(Raised)
            .clickable(onClick = onExpand).padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(model = song.artwork, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.size(45.dp).clip(RoundedCornerShape(9.dp)))
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, color = WarmWhite, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
            Text(if (playback.resolvingAudio) "Preparando áudio…" else song.artist, color = Muted, fontSize = 11.sp, maxLines = 1)
        }
        IconButton(onClick = { playback.toggle() }, enabled = !playback.resolvingAudio) {
            if (playback.resolvingAudio || playback.buffering) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Lime)
            } else Icon(if (playback.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Play/Pause", tint = WarmWhite)
        }
        IconButton(onClick = onNext) { Icon(Icons.Rounded.SkipNext, "Próxima", tint = WarmWhite) }
    }
    if (playback.error != null) {
        Text(playback.error.orEmpty(), modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp), color = Color(0xFFF6AB9D), fontSize = 11.sp, maxLines = 2)
    }
}

@Composable
private fun NativePlayerScreen(
    playback: PlaybackConnector, isFavorite: Boolean, onFavorite: () -> Unit,
    onClose: () -> Unit, onNext: () -> Unit, onPrevious: () -> Unit,
) {
    val song = playback.currentSong ?: return
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF344737), Ink, Ink)))
                .statusBarsPadding().navigationBarsPadding().padding(horizontal = 27.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Rounded.ArrowBack, "Minimizar", tint = WarmWhite) }
                Spacer(Modifier.weight(1f))
                Text("OCARINA MUSIC", color = WarmWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(48.dp))
            }
            Spacer(Modifier.weight(0.6f))
            AsyncImage(model = song.artwork, contentDescription = "Capa de ${song.title}", contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f).shadow(18.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp)).background(Raised))
            Spacer(Modifier.weight(0.7f))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(song.title, color = WarmWhite, fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Text(song.artist, color = Muted, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onFavorite) {
                    Icon(if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        "Curtir", tint = if (isFavorite) Lime else WarmWhite)
                }
            }
            Spacer(Modifier.height(17.dp))
            if (playback.error != null) {
                Text(playback.error.orEmpty(), color = Color(0xFFF6AB9D), fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
            }
            val maxMs = playback.durationMs.coerceAtLeast(1L)
            var dragging by remember(song.videoId) { mutableStateOf<Float?>(null) }
            Slider(
                value = dragging ?: (playback.positionMs.toFloat() / maxMs).coerceIn(0f, 1f),
                onValueChange = { dragging = it },
                onValueChangeFinished = {
                    dragging?.let { playback.seekTo((it * maxMs).toLong()) }
                    dragging = null
                },
                colors = SliderDefaults.colors(thumbColor = WarmWhite, activeTrackColor = Lime, inactiveTrackColor = Raised),
                enabled = playback.durationMs > 0,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(playback.positionMs), color = Muted, fontSize = 11.sp)
                Text(formatTime(playback.durationMs), color = Muted, fontSize = 11.sp)
            }
            Spacer(Modifier.height(13.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Rounded.SkipPrevious, "Anterior", tint = WarmWhite, modifier = Modifier.size(35.dp))
                }
                FilledIconButton(
                    onClick = { playback.toggle() },
                    modifier = Modifier.size(74.dp),
                    enabled = !playback.resolvingAudio,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Lime, contentColor = Ink),
                ) {
                    if (playback.resolvingAudio || playback.buffering) CircularProgressIndicator(Modifier.size(27.dp), color = Ink, strokeWidth = 3.dp)
                    else Icon(if (playback.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        if (playback.playing) "Pausar" else "Tocar", modifier = Modifier.size(38.dp))
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Rounded.SkipNext, "Próxima", tint = WarmWhite, modifier = Modifier.size(35.dp))
                }
            }
            Spacer(Modifier.weight(0.45f))
            Text(
                if (playback.sourceBitrateKbps > 0)
                    "Áudio original · ${playback.sourceBitrateKbps} kbps estimados pela fonte"
                else "Áudio original · qualidade conforme a fonte",
                color = Muted, fontSize = 11.sp,
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val seconds = (ms / 1000).coerceAtLeast(0)
    return String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60)
}
