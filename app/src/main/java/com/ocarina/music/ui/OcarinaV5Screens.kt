package com.ocarina.music.ui

import android.app.Activity
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import kotlinx.coroutines.isActive
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ocarina.music.R
import com.composables.icons.lucide.R as LucideR
import com.ocarina.music.data.OcarinaAccount
import com.ocarina.music.data.OcarinaPlaylist
import com.ocarina.music.data.OcarinaPublicProfile
import com.ocarina.music.data.Song

@Composable
private fun VI(@DrawableRes res: Int, label: String? = null,
               modifier: Modifier = Modifier, tint: Color = ocarinaColors.text) {
    Icon(painterResource(res), label, modifier, tint = tint)
}

@Composable
fun OcarinaV5Avatar(base64: String, photoUrl: String?, size: Dp = 40.dp) {
    val c = ocarinaColors
    val image = remember(base64) {
        runCatching {
            if (base64.isBlank()) null
            else Base64.decode(base64, Base64.DEFAULT).let { bytes ->
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }
        }.getOrNull()
    }
    Box(Modifier.size(size).clip(CircleShape).background(c.softAccent),
        contentAlignment = Alignment.Center) {
        when {
            image != null -> Image(image, "Foto do perfil", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            !photoUrl.isNullOrBlank() -> AsyncImage(photoUrl, "Foto do perfil", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else -> VI(LucideR.drawable.lucide_ic_user_round, "Perfil", Modifier.size(size * 0.53f), c.accent)
        }
    }
}

@Composable
fun OcarinaV5Login(account: OcarinaAccount) {
    val c = ocarinaColors
    val activity = LocalContext.current as? Activity
    var emailMode by remember { mutableStateOf(false) }
    var register by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    androidx.activity.compose.BackHandler(emailMode) { emailMode = false; account.clearMessage() }
    Column(Modifier.fillMaxSize().background(c.background).statusBarsPadding()
        .navigationBarsPadding().padding(horizontal = 27.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        if (!emailMode) {
            // Intentional simplicity: no icon tile, headline, caption or input box on the entry view.
            VI(R.drawable.ocarina_logo, "Ocarina", Modifier.size(104.dp), c.accent)
            Spacer(Modifier.height(52.dp))
            Button(onClick = { activity?.let(account::signInWithGoogle) },
                enabled = activity != null && !account.busy,
                modifier = Modifier.fillMaxWidth().height(55.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent)) {
                Image(painterResource(R.drawable.google_g), null, Modifier.size(23.dp))
                Spacer(Modifier.width(12.dp))
                Text("Continuar com Google", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { emailMode = true; account.clearMessage() },
                modifier = Modifier.fillMaxWidth().height(55.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = c.text),
                border = androidx.compose.foundation.BorderStroke(1.dp, c.outline)) {
                VI(R.drawable.ocarina_mail, null, Modifier.size(20.dp), c.text)
                Spacer(Modifier.width(12.dp))
                Text("Continuar com e-mail", fontWeight = FontWeight.SemiBold)
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { emailMode = false; account.clearMessage() }) {
                    VI(LucideR.drawable.lucide_ic_arrow_left, "Voltar", Modifier.size(24.dp))
                }
                Spacer(Modifier.width(9.dp))
                Text(if (register) "Criar conta" else "Entrar", color = c.text,
                    fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(23.dp))
            OutlinedTextField(email, { email = it.take(200) },
                modifier = Modifier.fillMaxWidth(), label = { Text("E-mail") },
                singleLine = true, shape = RoundedCornerShape(14.dp))
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(password, { password = it },
                modifier = Modifier.fillMaxWidth(), label = { Text("Senha") },
                singleLine = true, shape = RoundedCornerShape(14.dp),
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { showPassword = !showPassword }) {
                        Text(if (showPassword) "Ocultar" else "Mostrar", fontSize = 11.sp)
                    }
                })
            Spacer(Modifier.height(20.dp))
            Button(onClick = { if (register) account.register(email, password) else account.signIn(email, password) },
                enabled = !account.busy && email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(26.dp)) {
                Text(if (register) "Criar conta" else "Entrar", fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = { register = !register; account.clearMessage() }) {
                Text(if (register) "Já tenho uma conta" else "Criar conta com e-mail", color = c.accent)
            }
            if (!register) TextButton(onClick = { account.resetPassword(email) },
                enabled = !account.busy && email.isNotBlank()) {
                Text("Esqueci minha senha", color = c.muted)
            }
        }
        if (account.busy) {
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(Modifier.fillMaxWidth(), color = c.accent)
        }
        account.message?.let {
            Spacer(Modifier.height(11.dp))
            Text(it, color = c.accent, fontSize = 12.sp)
        }
    }
}

@Composable
fun OcarinaV5Header(screen: String, account: OcarinaAccount,
    onProfile: () -> Unit, onSearch: () -> Unit,
    onCreate: () -> Unit, onBack: () -> Unit) {
    val c = ocarinaColors
    val main = screen in setOf("HOME", "SEARCH", "LIBRARY", "PROFILE")
    Row(Modifier.fillMaxWidth().background(c.background).statusBarsPadding()
        .height(60.dp).padding(horizontal = 17.dp),
        verticalAlignment = Alignment.CenterVertically) {
        if (!main) {
            // Secondary screens only need a back arrow. No logo, avatar or header label.
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                VI(LucideR.drawable.lucide_ic_arrow_left, "Voltar", Modifier.size(23.dp))
            }
        } else {
            VI(R.drawable.ocarina_logo, "Ocarina", Modifier.size(36.dp), c.accent)
            if (screen != "PROFILE") {
                Spacer(Modifier.width(11.dp))
                Box(Modifier.clip(CircleShape).clickable(onClick = onProfile)) {
                    OcarinaV5Avatar(account.avatarBase64, account.user?.photoUrl?.toString(), 35.dp)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        when (screen) {
            "LIBRARY" -> {
                IconButton(onClick = onSearch) {
                    VI(LucideR.drawable.lucide_ic_search, "Buscar", Modifier.size(22.dp))
                }
                IconButton(onClick = onCreate) {
                    VI(LucideR.drawable.lucide_ic_plus, "Criar playlist", Modifier.size(24.dp))
                }
            }
            "SEARCH" -> {
                // Icon is displayed for visual consistency; camera search is not enabled yet.
                VI(R.drawable.ocarina_camera, null, Modifier.size(24.dp), c.text)
                Spacer(Modifier.width(11.dp))
            }
            "HOME", "PROFILE" -> IconButton(onClick = onSearch) {
                VI(LucideR.drawable.lucide_ic_search, "Buscar", Modifier.size(24.dp))
            }
        }
    }
}

/** One moving indicator, not four independent highlighted backgrounds. */
@Composable
fun OcarinaV5Nav(selected: String, account: OcarinaAccount, onSelect: (String) -> Unit) {
    val c = ocarinaColors
    val tabs = remember { listOf("HOME", "SEARCH", "LIBRARY", "PROFILE") }
    val icons = remember { listOf(LucideR.drawable.lucide_ic_house, LucideR.drawable.lucide_ic_search,
        LucideR.drawable.lucide_ic_library, LucideR.drawable.lucide_ic_user_round) }
    val activeTab = when (selected) {
        "SEARCH" -> "SEARCH"
        "LIBRARY", "LIKED", "PLAYLISTS", "PLAYLIST_DETAIL", "ARTISTS", "ALBUMS",
        "CATALOG_DETAIL", "DOWNLOADS", "PUBLIC_PLAYLIST" -> "LIBRARY"
        "PROFILE", "PROFILE_EDIT", "PUBLIC_PROFILE" -> "PROFILE"
        else -> "HOME"
    }
    val currentIndex = tabs.indexOf(activeTab).coerceAtLeast(0)
    BoxWithConstraints(
        Modifier.fillMaxWidth().background(c.background).navigationBarsPadding()
            .height(57.dp).padding(horizontal = 14.dp)
    ) {
        val slot = maxWidth / tabs.size
        val pillWidth = 56.dp
        val targetX = slot * currentIndex + (slot - pillWidth) / 2
        val pillX by animateDpAsState(
            targetValue = targetX,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "ocarina-navigation-slide",
        )
        // This pill persists while the icons change, so it visibly glides between tabs.
        Box(Modifier.offset(x = pillX, y = 8.dp).size(pillWidth, 41.dp)
            .clip(RoundedCornerShape(22.dp)).background(c.softAccent))
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            tabs.forEachIndexed { idx, tab ->
                val active = currentIndex == idx
                val tint by animateColorAsState(
                    targetValue = if (active) c.accent else c.muted,
                    animationSpec = tween(155), label = "ocarina-nav-tint-$tab",
                )
                val interaction = remember(tab) { MutableInteractionSource() }
                Box(Modifier.weight(1f).fillMaxHeight()
                    .clickable(interactionSource = interaction, indication = null) {
                        if (!active) onSelect(tab)
                    }, contentAlignment = Alignment.Center) {
                    if (tab == "PROFILE") Box(Modifier.size(29.dp).clip(CircleShape)
                        .border(if (active) 2.dp else 0.dp, tint, CircleShape),
                        contentAlignment = Alignment.Center) {
                        OcarinaV5Avatar(account.avatarBase64, account.user?.photoUrl?.toString(), 26.dp)
                    } else VI(icons[idx], tab, Modifier.size(24.dp), tint)
                }
            }
        }
    }
}

@Composable
private fun V5Chip(text: String, selected: Boolean = false, onClick: () -> Unit) {
    val c = ocarinaColors
    Surface(onClick = onClick, color = if (selected) c.accent else c.elevated,
        shape = RoundedCornerShape(25.dp)) {
        Text(text, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            color = if (selected) c.onAccent else c.text)
    }
}

@Composable
private fun V5Section(name: String, onOpen: (() -> Unit)? = null) {
    val c = ocarinaColors
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(name, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = c.text,
            modifier = Modifier.weight(1f), letterSpacing = (-0.5).sp)
        if (onOpen != null) IconButton(onClick = onOpen) {
            VI(LucideR.drawable.lucide_ic_chevron_right, "Ver mais", Modifier.size(22.dp), c.accent)
        }
    }
}

@Composable
private fun V5Cover(song: Song, onClick: () -> Unit, width: Dp = 150.dp) {
    val c = ocarinaColors
    Column(Modifier.width(width).clickable(onClick = onClick)) {
        Box(Modifier.size(width).clip(RoundedCornerShape(22.dp)).background(c.artworkSurface)) {
            AlbumArtwork(song, song.title, Modifier.fillMaxSize(), small = width <= 125.dp)
        }
        Spacer(Modifier.height(9.dp))
        Text(song.title, color = c.text, fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
        Text(song.artist, color = c.muted, maxLines = 1,
            overflow = TextOverflow.Ellipsis, fontSize = 11.sp)
    }
}

@Composable
private fun V5SongRow(song: Song, liked: Boolean, onPlay: () -> Unit,
    onLike: () -> Unit, onQueue: () -> Unit, onPlaylist: () -> Unit, onShare: () -> Unit) {
    val c = ocarinaColors
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().height(67.dp).clickable(onClick = onPlay),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(53.dp).clip(RoundedCornerShape(12.dp)).background(c.artworkSurface),
            contentAlignment = Alignment.Center) {
            if (song.artwork.isNotBlank()) AlbumArtwork(song, song.title, Modifier.fillMaxSize(), small = true)
            else VI(LucideR.drawable.lucide_ic_music_2, null, Modifier.size(23.dp), c.accent)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, color = c.text, fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artist + if (song.category == "video") " • Vídeo" else "",
                color = c.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onLike, modifier = Modifier.size(41.dp)) {
            OcarinaHeart(liked = liked, size = 20.dp)
        }
        Box {
            IconButton(onClick = { menu = true }, modifier = Modifier.size(39.dp)) {
                VI(LucideR.drawable.lucide_ic_ellipsis, "Opções", Modifier.size(22.dp), c.muted)
            }
            DropdownMenu(menu, { menu = false }) {
                DropdownMenuItem(text = { Text("Adicionar à fila") }, onClick = { menu = false; onQueue() })
                DropdownMenuItem(text = { Text("Adicionar à playlist") }, onClick = { menu = false; onPlaylist() })
                DropdownMenuItem(text = { Text("Compartilhar") }, onClick = { menu = false; onShare() })
            }
        }
    }
}

@Composable
fun OcarinaV5Home(listState: LazyListState, playlists: List<OcarinaPlaylist>,
    discoveries: List<Song>, recent: List<Song>, favorites: List<Song>,
    loading: Boolean, onRetry: () -> Unit, onOpenPlaylist: (OcarinaPlaylist) -> Unit,
    onOpenLibrary: () -> Unit, onSearch: () -> Unit,
    onPlay: (Song, List<Song>) -> Unit, onLike: (Song) -> Unit,
    onQueue: (Song) -> Unit, onPlaylist: (Song) -> Unit, onShare: (Song) -> Unit) {
    val c = ocarinaColors
    var filter by remember { mutableStateOf("Tudo") }
    val likedIds = remember(favorites) { derivedStateOf { favorites.map { it.videoId }.toHashSet() } }
    LazyColumn(Modifier.fillMaxSize(), state = listState,
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(17.dp)) {
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                listOf("Tudo", "Música", "Playlists").forEach { chip ->
                    V5Chip(chip, filter == chip) { filter = chip }
                }
            }
        }
        if (playlists.isNotEmpty() && filter != "Música") {
            item { V5Section("Suas playlists", onOpenLibrary) }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(playlists.take(12), key = { it.id }) { list ->
                        Column(Modifier.width(166.dp).clickable { onOpenPlaylist(list) }) {
                            OcarinaPlaylistCover(list, 166.dp)
                            Spacer(Modifier.height(8.dp))
                            Text(list.name, color = c.text, fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${list.songs.size} faixas", color = c.muted, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
        
        if (filter != "Playlists" || playlists.isEmpty()) {
            item { V5Section("Feito pra sua escuta", onSearch) }
            if (loading) item { CircularProgressIndicator(Modifier.size(28.dp), color = c.accent) }
            else if (discoveries.isEmpty()) item {
                Text("Busque músicas para conhecer novas faixas.", color = c.muted, fontSize = 13.sp)
                TextButton(onClick = onRetry) { Text("Atualizar recomendações") }
            }
            else item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                    items(discoveries.take(10), key = { it.videoId }) { song ->
                        V5Cover(song, { onPlay(song, discoveries) }, 166.dp)
                    }
                }
            }
            if (recent.isNotEmpty()) {
                item { V5Section("Continue ouvindo", onOpenLibrary) }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                        items(recent.take(8), key = { it.videoId }) { song ->
                            V5Cover(song, { onPlay(song, recent) }, 125.dp)
                        }
                    }
                }
            }
            if (discoveries.isNotEmpty()) {
                item { V5Section("Para descobrir agora") }
                items(discoveries.take(8), key = { "home:${it.videoId}" }, contentType = { "song" }) { song ->
                    V5SongRow(song, likedIds.value.contains(song.videoId),
                        onPlay = { onPlay(song, discoveries) }, onLike = { onLike(song) },
                        onQueue = { onQueue(song) }, onPlaylist = { onPlaylist(song) }, onShare = { onShare(song) })
                }
            }
        }
    }
}

@Composable
private fun V5Category(label: String, color: Color, icon: Int, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(24.dp))
        .background(Brush.linearGradient(listOf(color, color.copy(alpha = 0.76f))))
        .clickable(onClick = onClick)) {
        // Soft overlapping shape creates a material tile, not a flat generic square.
        Box(Modifier.align(Alignment.BottomEnd).offset(x = 29.dp, y = 33.dp)
            .size(116.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.13f)))
        Text(label, modifier = Modifier.align(Alignment.TopStart).padding(17.dp),
            color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        VI(icon, null, Modifier.align(Alignment.BottomEnd).padding(15.dp).size(32.dp), Color.White)
    }
}

@Composable
fun OcarinaV5Search(listState: LazyListState, query: String, onQuery: (String) -> Unit,
    songs: List<Song>, recommendations: List<Song>, profiles: List<OcarinaPublicProfile>,
    favorites: List<Song>, loading: Boolean, profilesLoading: Boolean,
    error: String?, onRetry: () -> Unit, onPlay: (Song) -> Unit, onLike: (Song) -> Unit,
    onQueue: (Song) -> Unit, onPlaylist: (Song) -> Unit, onShare: (Song) -> Unit,
    onProfile: (OcarinaPublicProfile) -> Unit) {
    val c = ocarinaColors
    var filter by remember { mutableStateOf("Tudo") }
    val likedIds = remember(favorites) { derivedStateOf { favorites.map { it.videoId }.toHashSet() } }
    val hasQuery = query.trim().isNotEmpty()
    val filtered = remember(songs, filter) {
        when (filter) {
            "Música" -> songs.filter { it.category == "music" }
            "Vídeos" -> songs.filter { it.category == "video" }
            "Podcast" -> songs.filter { it.category == "podcast" }
            "Ao vivo" -> songs.filter { it.category == "live" }
            "Perfis" -> emptyList()
            else -> songs.sortedBy { when(it.category) { "music" -> 0; "video" -> 1; "podcast" -> 2; "live" -> 3; else -> 4 } }
        }
    }
    LazyColumn(Modifier.fillMaxSize(), state = listState,
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 35.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            OutlinedTextField(query, onValueChange = onQuery,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                placeholder = { Text("O que você quer ouvir?", color = c.muted) },
                leadingIcon = { VI(LucideR.drawable.lucide_ic_search, null, Modifier.size(21.dp), c.accent) },
                trailingIcon = {
                    if (query.isNotBlank()) IconButton(onClick = { onQuery("") }) {
                        VI(LucideR.drawable.lucide_ic_x, "Limpar busca", Modifier.size(20.dp))
                    }
                }, singleLine = true, shape = RoundedCornerShape(17.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = c.surface, unfocusedContainerColor = c.surface,
                    focusedBorderColor = c.accent, unfocusedBorderColor = c.outline))
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("Tudo", "Música", "Vídeos", "Podcast", "Ao vivo", "Perfis").forEach { type ->
                    V5Chip(type, filter == type) { filter = type }
                }
            }
        }
        if (!hasQuery) {
            if (filter == "Tudo") item { V5Section("Explore alguma coisa nova") }
            if (filter == "Tudo") item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.weight(1f)) { V5Category("Música", Color(0xFFB64D80),
                            LucideR.drawable.lucide_ic_music_2) { filter = "Música" } }
                        Box(Modifier.weight(1f)) { V5Category("Vídeos", Color(0xFF73559D),
                            LucideR.drawable.lucide_ic_play) { filter = "Vídeos" } }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.weight(1f)) { V5Category("Podcast", Color(0xFF3E777A),
                            LucideR.drawable.lucide_ic_headphones) { filter = "Podcast" } }
                        Box(Modifier.weight(1f)) { V5Category("Ao vivo", Color(0xFFA45B64),
                            LucideR.drawable.lucide_ic_headphones) { filter = "Ao vivo" } }
                    }
                }
            }
            val discovery = when (filter) {
                "Música" -> recommendations.filter { it.category == "music" }
                "Vídeos" -> recommendations.filter { it.category == "video" }
                "Podcast" -> recommendations.filter { it.category == "podcast" }
                "Ao vivo" -> recommendations.filter { it.category == "live" }
                "Perfis" -> emptyList()
                else -> recommendations
            }
            if (discovery.isNotEmpty()) {
                item { V5Section(if (filter == "Tudo") "Descobertas" else "Sugestões de $filter") }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(discovery.take(8), key = { it.videoId }) { song ->
                            V5Cover(song, { onPlay(song) }, 152.dp)
                        }
                    }
                }
            } else if (filter != "Tudo") item {
                Text("Pesquise para encontrar ${filter.lowercase()} no catálogo.", color = c.muted, fontSize = 13.sp)
            }

        } else {
            if (filter != "Perfis") {
                if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = c.accent) }
                else if (error != null) item { Text(error, color = c.accent, fontSize = 12.sp) }
                else if (filtered.isEmpty()) item {
                    Text("Nenhum resultado de ${filter.lowercase()} encontrado.", color = c.muted)
                }
                else items(filtered, key = { "search:${it.videoId}" }, contentType = { "song" }) { song ->
                    V5SongRow(song, likedIds.value.contains(song.videoId),
                        onPlay = { onPlay(song) }, onLike = { onLike(song) },
                        onQueue = { onQueue(song) }, onPlaylist = { onPlaylist(song) }, onShare = { onShare(song) })
                }
            }
            if (filter == "Perfis" || filter == "Tudo") {
                item { V5Section("Pessoas no Ocarina") }
                if (profilesLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = c.accent) }
                if (!profilesLoading && profiles.isEmpty()) item {
                    Text("Nenhum perfil público encontrado. A busca só mostra quem escolheu aparecer.",
                        color = c.muted, fontSize = 12.sp)
                }
                items(profiles, key = { "profile:${it.uid}" }, contentType = { "person" }) { profile ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .clickable { onProfile(profile) }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        OcarinaV5Avatar(profile.avatarBase64, profile.photoUrl, 48.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(profile.displayName, color = c.text, fontWeight = FontWeight.SemiBold)
                            Text("Perfil público · ${profile.bio}", color = c.muted,
                                fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        VI(LucideR.drawable.lucide_ic_chevron_right, null, Modifier.size(20.dp), c.muted)
                    }
                }
            }
        }
    }
}

@Composable
fun OcarinaV5Library(listState: LazyListState, playlists: List<OcarinaPlaylist>,
    favorites: List<Song>, recent: List<Song>,
    onOpenPlaylist: (OcarinaPlaylist) -> Unit, onPlaylists: () -> Unit,
    onLiked: () -> Unit, onArtists: () -> Unit, onAlbums: () -> Unit, onDownloads: () -> Unit,
    onPlay: (Song) -> Unit, onLike: (Song) -> Unit,
    onQueue: (Song) -> Unit, onPlaylist: (Song) -> Unit, onShare: (Song) -> Unit) {
    val c = ocarinaColors
    val likedIds = remember(favorites) { derivedStateOf { favorites.map { it.videoId }.toHashSet() } }
    LazyColumn(Modifier.fillMaxSize(), state = listState,
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                V5Chip("Playlists", onClick = onPlaylists)
                V5Chip("Álbuns", onClick = onAlbums)
                V5Chip("Artistas", onClick = onArtists)
                V5Chip("Downloads", onClick = onDownloads)
            }
        }
        item { Spacer(Modifier.height(6.dp)) }
        item {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(23.dp))
                .background(Brush.horizontalGradient(listOf(c.elevated, c.surface)))
                .clickable(onClick = onLiked).padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(68.dp).clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(c.accent, c.artworkSurface))),
                    contentAlignment = Alignment.Center) {
                    VI(LucideR.drawable.lucide_ic_heart, "Curtidas", Modifier.size(30.dp), c.onAccent)
                }
                Spacer(Modifier.width(15.dp))
                Column(Modifier.weight(1f)) {
                    Text("Músicas curtidas", color = c.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text("${favorites.size} músicas • Sua coleção", color = c.muted, fontSize = 12.sp)
                }
                VI(LucideR.drawable.lucide_ic_chevron_right, null, Modifier.size(19.dp), c.muted)
            }
        }
        if (playlists.isNotEmpty()) {
            item { V5Section("Suas playlists") }
            items(playlists, key = { "list:${it.id}" }, contentType = { "playlist" }) { list ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { onOpenPlaylist(list) }
                    .padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    OcarinaPlaylistCover(list, 68.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(list.name, color = c.text, fontWeight = FontWeight.SemiBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Playlist • ${list.songs.size} músicas", color = c.muted, fontSize = 12.sp)
                    }
                    VI(LucideR.drawable.lucide_ic_chevron_right, "Abrir", Modifier.size(19.dp), c.muted)
                }
            }
        }
        if (recent.isNotEmpty()) {
            item { V5Section("Ouvidas recentemente") }
            items(recent.take(15), key = { "recent:${it.videoId}" }, contentType = { "song" }) { song ->
                V5SongRow(song, likedIds.value.contains(song.videoId),
                    onPlay = { onPlay(song) }, onLike = { onLike(song) },
                    onQueue = { onQueue(song) }, onPlaylist = { onPlaylist(song) }, onShare = { onShare(song) })
            }
        }
    }
}

@Composable
fun OcarinaV5ProfileView(account: OcarinaAccount, favorites: Int, recent: Int,
    playlists: List<OcarinaPlaylist>, onEdit: () -> Unit, onLiked: () -> Unit,
    onOpenPlaylist: (OcarinaPlaylist) -> Unit, onShare: () -> Unit) {
    val c = ocarinaColors
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
                .background(Brush.verticalGradient(listOf(c.softAccent, c.surface, c.surface)))
                .padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                OcarinaV5Avatar(account.avatarBase64, account.user?.photoUrl?.toString(), 114.dp)
                Spacer(Modifier.height(14.dp))
                Text(account.displayName.ifBlank { account.user?.displayName ?: "Seu perfil" },
                    color = c.text, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                if (account.bio.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(account.bio, color = c.muted, fontSize = 13.sp)
                }
                Text(if (account.isPublic) "Perfil público" else "Perfil privado",
                    color = c.accent, fontSize = 11.sp)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onEdit, shape = RoundedCornerShape(14.dp)) {
                        VI(LucideR.drawable.lucide_ic_pencil, null, Modifier.size(17.dp), c.accent)
                        Spacer(Modifier.width(6.dp)); Text("Editar")
                    }
                    OutlinedButton(onClick = onShare, shape = RoundedCornerShape(14.dp)) {
                        VI(LucideR.drawable.lucide_ic_share_2, null, Modifier.size(17.dp), c.accent)
                        Spacer(Modifier.width(6.dp)); Text("Compartilhar")
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    V5ProfileNumber("${playlists.size}", "Playlists")
                    V5ProfileNumber("$favorites", "Curtidas")
                    V5ProfileNumber("$recent", "Recentes")
                }
            }
        }
        item { V5Section("Suas playlists") }
        if (playlists.isEmpty()) item {
            Text("As suas playlists vão aparecer por aqui.", color = c.muted)
        }
        items(playlists, key = { "profile-list:${it.id}" }) { list ->
            Row(Modifier.fillMaxWidth().clickable { onOpenPlaylist(list) }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                OcarinaPlaylistCover(list, 64.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(list.name, color = c.text, fontWeight = FontWeight.SemiBold)
                    Text("${list.songs.size} faixas", color = c.muted, fontSize = 12.sp)
                }
                VI(LucideR.drawable.lucide_ic_chevron_right, null, Modifier.size(18.dp), c.muted)
            }
        }
        item {
            OutlinedButton(onClick = onLiked, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp)) {
                VI(LucideR.drawable.lucide_ic_heart, null, Modifier.size(18.dp), c.accent)
                Spacer(Modifier.width(8.dp)); Text("Ver minhas curtidas")
            }
        }
    }
}

@Composable
private fun V5ProfileNumber(value: String, label: String) {
    val c = ocarinaColors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = c.text, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text(label, color = c.muted, fontSize = 11.sp)
    }
}

@Composable
fun OcarinaV5ProfileEdit(account: OcarinaAccount, dark: Boolean,
    onDark: (Boolean) -> Unit, onBack: () -> Unit) {
    val c = ocarinaColors
    var name by remember(account.user?.uid, account.displayName) { mutableStateOf(account.displayName) }
    var bio by remember(account.user?.uid, account.bio) { mutableStateOf(account.bio) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) account.uploadAvatar(uri)
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(22.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.clickable { picker.launch("image/*") }) {
                        OcarinaV5Avatar(account.avatarBase64, account.user?.photoUrl?.toString(), 105.dp)
                    }
                    TextButton(onClick = { picker.launch("image/*") }) { Text("Trocar foto") }
                }
            }
        }
        item {
            OutlinedTextField(name, { name = it.take(50) }, modifier = Modifier.fillMaxWidth(),
                label = { Text("Nome do perfil") }, singleLine = true, shape = RoundedCornerShape(15.dp))
        }
        item {
            OutlinedTextField(bio, { bio = it.take(140) }, modifier = Modifier.fillMaxWidth(),
                label = { Text("Sobre você • até 140 caracteres") }, minLines = 2,
                maxLines = 3, shape = RoundedCornerShape(15.dp))
        }
        item {
            Button(onClick = {
                account.updateName(name)
                account.updateBio(bio)
                if (account.isPublic) account.setPublic(true, name, bio)
                onBack()
            }, enabled = !account.busy && name.isNotBlank(), modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(15.dp)) { Text("Salvar alterações") }
        }
        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.surface).padding(17.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Aparecer na busca", fontWeight = FontWeight.SemiBold, color = c.text)
                        Text("Outras pessoas verão seu nome, foto e bio. E-mail e curtidas ficam privados.",
                            color = c.muted, fontSize = 11.sp, lineHeight = 17.sp)
                    }
                    Switch(account.isPublic, onCheckedChange = { account.setPublic(it, name, bio) }, enabled = !account.busy)
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Modo escuro", color = c.text, modifier = Modifier.weight(1f))
                    Switch(dark, onCheckedChange = onDark)
                }
            }
        }
        item {
            OutlinedButton(onClick = { account.logout() }, modifier = Modifier.fillMaxWidth()) {
                Text("Sair da conta")
            }
        }
        account.message?.let { msg -> item { Text(msg, color = c.accent, fontSize = 12.sp) } }
    }
}

@Composable
fun OcarinaV5PublicProfile(profile: OcarinaPublicProfile, onShare: () -> Unit) {
    val c = ocarinaColors
    Column(Modifier.fillMaxSize().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(30.dp))
        OcarinaV5Avatar(profile.avatarBase64, profile.photoUrl, 124.dp)
        Spacer(Modifier.height(18.dp))
        Text(profile.displayName, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = c.text)
        Text(profile.bio.ifBlank { "Ouvindo seu próprio ritmo no Ocarina." },
            color = c.muted, fontSize = 14.sp)
        Spacer(Modifier.height(14.dp))
        OutlinedButton(onClick = onShare) {
            VI(LucideR.drawable.lucide_ic_share_2, null, Modifier.size(17.dp), c.accent)
            Spacer(Modifier.width(8.dp)); Text("Compartilhar perfil")
        }
        Spacer(Modifier.height(20.dp))
        Text("Informações privadas e favoritos não ficam visíveis.", color = c.muted, fontSize = 12.sp)
    }
}

/** Only the full player animates: list thumbnails and the mini-player remain static. */
@Composable
fun OcarinaV5Disc(song: Song, size: Dp, playing: Boolean) {
    val c = ocarinaColors
    var flipped by remember(song.videoId) { mutableStateOf(false) }
    val flip by animateFloatAsState(if (flipped) 180f else 0f,
        animationSpec = tween(durationMillis = 420), label = "Virada do CD")
    val turn = remember(song.videoId) { Animatable(0f) }
    LaunchedEffect(song.videoId, playing, flipped) {
        if (playing && !flipped) {
            while (isActive) {
                val start = turn.value % 360f
                turn.snapTo(start)
                turn.animateTo(start + 360f, animationSpec = tween(19_000, easing = LinearEasing))
            }
        }
    }
    Box(Modifier.size(size)
        .graphicsLayer {
            rotationY = if (flip <= 90f) flip else flip - 180f
            cameraDistance = 18f * density
        }
        .clickable { flipped = !flipped }, contentAlignment = Alignment.Center) {
        if (flip < 90f) {
            Box(Modifier.fillMaxSize().graphicsLayer { rotationZ = turn.value }
                .clip(CircleShape).background(c.elevated)
                .border(2.dp, c.outline.copy(alpha = 0.78f), CircleShape),
                contentAlignment = Alignment.Center) {
                if (song.artwork.isNotBlank()) AlbumArtwork(song, "CD de ${song.title}",
                    Modifier.fillMaxSize(), highQuality = true)
                else VI(LucideR.drawable.lucide_ic_disc_3, "Disco", Modifier.size(size * 0.66f), c.accent)
                Box(Modifier.fillMaxSize().clip(CircleShape)
                    .background(Brush.sweepGradient(listOf(Color.White.copy(alpha = 0.13f),
                        Color.Transparent, c.softAccent.copy(alpha = 0.15f), Color.Transparent,
                        Color.White.copy(alpha = 0.13f)))))
                Box(Modifier.size(size * 0.27f).clip(CircleShape)
                    .background(c.surface.copy(alpha = 0.92f))
                    .border(2.dp, c.accent.copy(alpha = 0.65f), CircleShape),
                    contentAlignment = Alignment.Center) {
                    Box(Modifier.size(size * 0.09f).clip(CircleShape)
                        .background(c.background).border(1.dp, c.outline, CircleShape))
                }
            }
        } else {
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(22.dp))
                .background(c.artworkSurface), contentAlignment = Alignment.Center) {
                if (song.artwork.isNotBlank()) AlbumArtwork(song, "Capa de ${song.title}",
                    Modifier.fillMaxSize(), highQuality = true)
                else VI(LucideR.drawable.lucide_ic_music_2, "Álbum", Modifier.size(size * 0.5f), c.accent)
            }
        }
    }
}
