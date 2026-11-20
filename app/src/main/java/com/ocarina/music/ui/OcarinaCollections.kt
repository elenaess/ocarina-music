package com.ocarina.music.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.composables.icons.lucide.R as LucideR
import com.ocarina.music.data.CatalogEntry
import com.ocarina.music.data.CatalogKind
import com.ocarina.music.data.OcarinaPlaylist
import com.ocarina.music.data.Song

@Composable
private fun CIcon(@DrawableRes res: Int, description: String?, modifier: Modifier = Modifier) {
    Icon(painterResource(res), description, modifier, tint = ocarinaColors.text)
}

@Composable
fun OcarinaPlaylistCover(list: OcarinaPlaylist, size: androidx.compose.ui.unit.Dp = 65.dp) {
    val c = ocarinaColors
    val image = list.cover.ifBlank { list.songs.firstOrNull()?.artwork.orEmpty() }
    Box(Modifier.size(size).clip(RoundedCornerShape(17.dp)).background(c.softAccent),
        contentAlignment = Alignment.Center) {
        if (image.isNotBlank()) AsyncImage(model = image, contentDescription = "Capa de ${list.name}",
            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else Icon(painterResource(LucideR.drawable.lucide_ic_list_music), "Playlist",
            Modifier.size(size / 2), tint = c.accent)
    }
}

@Composable
fun OcarinaPlaylistsPage(
    playlists: List<OcarinaPlaylist>, onCreate: () -> Unit,
    onOpen: (OcarinaPlaylist) -> Unit, syncLabel: String = "",
) {
    val c = ocarinaColors
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)) {
        item {
            Text("Suas playlists", fontSize = 29.sp, fontWeight = FontWeight.SemiBold, color = c.text)
            Spacer(Modifier.height(5.dp))
            Text("Organize suas músicas e escolha a capa de cada lista.", color = c.muted, fontSize = 12.sp)
            if (syncLabel.isNotBlank()) Text(syncLabel, color = c.accent, fontSize = 11.sp)
            Spacer(Modifier.height(15.dp))
            Button(onClick = onCreate, shape = RoundedCornerShape(15.dp)) {
                Icon(painterResource(LucideR.drawable.lucide_ic_plus), null, Modifier.size(18.dp))
                Spacer(Modifier.width(9.dp))
                Text("Criar playlist")
            }
        }
        if (playlists.isEmpty()) item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                .background(c.surface).padding(25.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                CIcon(LucideR.drawable.lucide_ic_list_music, null, Modifier.size(34.dp))
                Spacer(Modifier.height(8.dp))
                Text("Sua primeira playlist começa aqui", color = c.text)
            }
        }
        items(playlists, key = { it.id }) { list ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).clickable { onOpen(list) }
                .padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                OcarinaPlaylistCover(list, 74.dp)
                Spacer(Modifier.width(15.dp))
                Column(Modifier.weight(1f)) {
                    Text(list.name, color = c.text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${list.songs.size} músicas", color = c.muted, fontSize = 12.sp)
                }
                CIcon(LucideR.drawable.lucide_ic_chevron_right, "Abrir", Modifier.size(19.dp))
            }
        }
    }
}

@Composable
fun OcarinaPlaylistDetailPage(
    list: OcarinaPlaylist,
    onCoverSelected: (android.net.Uri) -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onPlay: (Song) -> Unit,
    onRemove: (Song) -> Unit,
    onAdd: () -> Unit,
) {
    val c = ocarinaColors
    var rename by remember { mutableStateOf(false) }
    var remove by remember { mutableStateOf(false) }
    var title by remember(list.id, list.name) { mutableStateOf(list.name) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onCoverSelected(uri)
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OcarinaPlaylistCover(list, 133.dp)
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Text(list.name, color = c.text, fontSize = 24.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Text("${list.songs.size} músicas", fontSize = 12.sp, color = c.muted)
                    TextButton(onClick = { picker.launch("image/*") }) {
                        Icon(painterResource(LucideR.drawable.lucide_ic_image), null, Modifier.size(17.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Trocar capa")
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { rename = true }) { CIcon(LucideR.drawable.lucide_ic_pencil, "Renomear") }
                IconButton(onClick = onShare) { CIcon(LucideR.drawable.lucide_ic_share_2, "Compartilhar") }
                IconButton(onClick = { remove = true }) { CIcon(LucideR.drawable.lucide_ic_trash_2, "Excluir") }
                Spacer(Modifier.weight(1f))
                FilledIconButton(onClick = { list.songs.firstOrNull()?.let(onPlay) },
                    enabled = list.songs.isNotEmpty(), modifier = Modifier.size(53.dp)) {
                    Icon(painterResource(LucideR.drawable.lucide_ic_play), "Reproduzir", Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)) {
                Icon(painterResource(LucideR.drawable.lucide_ic_plus), null, Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text("Adicionar músicas")
            }
        }
        if (list.songs.isEmpty()) item {
            Text("Playlist vazia. Adicione músicas pela Busca ou pela biblioteca.",
                color = c.muted, fontSize = 13.sp)
        }
        items(list.songs, key = { it.videoId }) { song ->
            OcarinaCollectionSong(song, onClick = { onPlay(song) },
                trailing = { IconButton(onClick = { onRemove(song) }) {
                    CIcon(LucideR.drawable.lucide_ic_x, "Remover da lista", Modifier.size(18.dp))
                } })
        }
    }
    if (rename) AlertDialog(onDismissRequest = { rename = false },
        title = { Text("Renomear playlist") },
        text = { OutlinedTextField(title, { title = it.take(80) }, singleLine = true,
            label = { Text("Nome") }) },
        confirmButton = { TextButton(onClick = { onRename(title); rename = false }, enabled = title.isNotBlank()) {
            Text("Salvar")
        } },
        dismissButton = { TextButton(onClick = { rename = false }) { Text("Cancelar") } })
    if (remove) AlertDialog(onDismissRequest = { remove = false },
        title = { Text("Excluir playlist?") },
        text = { Text("As músicas originais não serão apagadas.") },
        confirmButton = { TextButton(onClick = { onDelete(); remove = false }) { Text("Excluir") } },
        dismissButton = { TextButton(onClick = { remove = false }) { Text("Cancelar") } })
}

@Composable
fun OcarinaCollectionSong(song: Song, onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null) {
    val c = ocarinaColors
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).clickable(onClick = onClick)
        .padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)).background(c.artworkSurface),
            contentAlignment = Alignment.Center) {
            if (song.artwork.isNotBlank()) AlbumArtwork(song, song.title, small = true,
                modifier = Modifier.fillMaxSize())
            else Icon(painterResource(LucideR.drawable.lucide_ic_music_2), null,
                Modifier.size(25.dp), tint = c.accent)
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, color = c.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artist, color = c.muted, fontSize = 12.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (trailing != null) trailing()
        else CIcon(LucideR.drawable.lucide_ic_play, "Ouvir", Modifier.size(20.dp))
    }
}

@Composable
fun OcarinaCatalogBrowsePage(
    kind: CatalogKind, query: String, onQuery: (String) -> Unit,
    entries: List<CatalogEntry>, loading: Boolean, error: String?,
    local: List<CatalogEntry>, onOpen: (CatalogEntry) -> Unit,
) {
    val c = ocarinaColors
    val title = if (kind == CatalogKind.ARTIST) "Artistas" else "Álbuns"
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(title, fontSize = 29.sp, color = c.text, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(5.dp))
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(query, onValueChange = onQuery, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar ${title.lowercase()}") }, singleLine = true,
                leadingIcon = { CIcon(LucideR.drawable.lucide_ic_search, null, Modifier.size(20.dp)) },
                shape = RoundedCornerShape(16.dp), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search))
            Spacer(Modifier.height(10.dp))
            Text(if (query.isBlank()) "Da sua biblioteca" else "Resultados do catálogo",
                color = c.text, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        }
        if (loading) item { CircularProgressIndicator(Modifier.padding(20.dp)) }
        else if (error != null) item { Text(error, color = c.muted, fontSize = 12.sp) }
        val shown = if (query.isBlank()) local else entries
        if (shown.isEmpty() && !loading && error == null) item {
            Text(if (query.isBlank()) "Suas músicas aparecerão aqui quando você ouvir ou salvar artistas e álbuns."
                else "Nenhum resultado encontrado.", color = c.muted, fontSize = 13.sp)
        }
        items(shown, key = { it.browseId }) { entry ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).clickable { onOpen(entry) }
                .padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(71.dp).clip(if (kind == CatalogKind.ARTIST) CircleShape
                    else RoundedCornerShape(16.dp)).background(c.softAccent), contentAlignment = Alignment.Center) {
                    if (entry.image.isNotBlank()) AsyncImage(model = entry.image,
                        contentDescription = entry.name, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize())
                    else CIcon(if (kind == CatalogKind.ARTIST) LucideR.drawable.lucide_ic_user_round
                        else LucideR.drawable.lucide_ic_disc_3, null, Modifier.size(29.dp))
                }
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(entry.name, color = c.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(entry.subtitle, color = c.muted, fontSize = 12.sp)
                }
                CIcon(LucideR.drawable.lucide_ic_chevron_right, "Abrir", Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun OcarinaCatalogDetailPage(
    entry: CatalogEntry, songs: List<Song>, loading: Boolean, error: String?,
    onPlay: (Song) -> Unit, onShare: () -> Unit,
    onQueue: (Song) -> Unit, onSave: (Song) -> Unit,
) {
    val c = ocarinaColors
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(133.dp).clip(if (entry.kind == CatalogKind.ARTIST) CircleShape
                    else RoundedCornerShape(22.dp)).background(c.artworkSurface), contentAlignment = Alignment.Center) {
                    if (entry.image.isNotBlank()) AsyncImage(model = entry.image, contentDescription = entry.name,
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else CIcon(LucideR.drawable.lucide_ic_music_2, null, Modifier.size(45.dp))
                }
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Text(entry.name, fontSize = 24.sp, color = c.text, fontWeight = FontWeight.SemiBold)
                    Text(entry.subtitle, fontSize = 12.sp, color = c.muted)
                    IconButton(onClick = onShare) { CIcon(LucideR.drawable.lucide_ic_share_2, "Compartilhar") }
                }
            }
            Spacer(Modifier.height(11.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Faixas", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = c.text,
                    modifier = Modifier.weight(1f))
                FilledIconButton(onClick = { songs.firstOrNull()?.let(onPlay) },
                    enabled = songs.isNotEmpty(), modifier = Modifier.size(49.dp)) {
                    Icon(painterResource(LucideR.drawable.lucide_ic_play), "Ouvir", Modifier.size(22.dp))
                }
            }
        }
        if (loading) item { CircularProgressIndicator(Modifier.padding(18.dp)) }
        else if (error != null) item { Text(error, color = c.muted, fontSize = 12.sp) }
        else if (songs.isEmpty()) item { Text("Nenhuma faixa disponível para essa seleção.",
            color = c.muted, fontSize = 13.sp) }
        items(songs, key = { it.videoId }) { song ->
            OcarinaCollectionSong(song, onClick = { onPlay(song) }, trailing = {
                Row {
                    IconButton(onClick = { onQueue(song) }) {
                        CIcon(LucideR.drawable.lucide_ic_list_plus, "Adicionar à fila", Modifier.size(19.dp))
                    }
                    IconButton(onClick = { onSave(song) }) {
                        CIcon(LucideR.drawable.lucide_ic_plus, "Playlist", Modifier.size(19.dp))
                    }
                }
            })
        }
    }
}

@Composable
fun OcarinaOfflinePage(tracks: List<Song>, busy: Boolean, onImport: () -> Unit,
    onPlay: (Song) -> Unit, onRemove: (Song) -> Unit,
    onDownloadLink: (String) -> Unit, message: String? = null) {
    val c = ocarinaColors
    var search by remember { mutableStateOf("") }
    var directLink by remember { mutableStateOf("") }
    val filtered = remember(tracks, search) { tracks.filter {
        it.title.contains(search, true) || it.artist.contains(search, true)
    } }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(
        start = 18.dp, end = 18.dp, top = 10.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Text("Sua música, offline", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = c.text)
            Spacer(Modifier.height(5.dp))
            Text("Organize os arquivos salvos neste aparelho.", color = c.muted, fontSize = 13.sp)
        }
        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(25.dp))
                .background(c.elevated).padding(19.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(52.dp).clip(RoundedCornerShape(17.dp))
                        .background(c.softAccent), contentAlignment = Alignment.Center) {
                        CIcon(LucideR.drawable.lucide_ic_headphones, "Biblioteca offline", Modifier.size(26.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Sempre com você", color = c.text,
                            fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                        Text("${tracks.size} ${if (tracks.size == 1) "faixa salva" else "faixas salvas"}",
                            color = c.muted, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(17.dp))
                Button(onClick = onImport, enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(49.dp),
                    shape = RoundedCornerShape(16.dp)) {
                    CIcon(LucideR.drawable.lucide_ic_plus, "Importar", Modifier.size(19.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (busy) "Processando..." else "Adicionar arquivo do celular")
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(23.dp))
                .background(c.surface).padding(17.dp)) {
                Text("Baixar de um link direto", color = c.text,
                    fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(5.dp))
                Text("Para áudios que você tenha autorização para salvar. Links de páginas do YouTube não são arquivos diretos.",
                    color = c.muted, fontSize = 12.sp)
                Spacer(Modifier.height(13.dp))
                OutlinedTextField(value = directLink, onValueChange = { directLink = it.take(2000) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("https://exemplo.com/audio.flac", fontSize = 12.sp) },
                    shape = RoundedCornerShape(15.dp))
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { onDownloadLink(directLink.trim()) },
                    enabled = !busy && directLink.startsWith("https://"),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Text(if (busy) "Baixando..." else "Salvar áudio autorizado")
                }
            }
        }
        if (!message.isNullOrBlank()) item {
            Text(message, color = c.accent, fontSize = 12.sp)
        }
        item {
            Text("Biblioteca neste aparelho", color = c.text,
                fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
            if (tracks.isNotEmpty()) {
                Spacer(Modifier.height(9.dp))
                OutlinedTextField(value = search, onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    leadingIcon = { CIcon(LucideR.drawable.lucide_ic_search, "Buscar", Modifier.size(19.dp)) },
                    placeholder = { Text("Buscar arquivos salvos", fontSize = 13.sp) },
                    shape = RoundedCornerShape(15.dp))
            }
        }
        if (tracks.isEmpty()) item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                .background(c.surface).padding(vertical = 37.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(72.dp).clip(CircleShape).background(c.softAccent),
                    contentAlignment = Alignment.Center) {
                    CIcon(LucideR.drawable.lucide_ic_music_2, "Nenhuma música", Modifier.size(32.dp))
                }
                Spacer(Modifier.height(13.dp))
                Text("Ainda não tem nada por aqui", color = c.text,
                    fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(5.dp))
                Text("Importe músicas do celular ou salve um arquivo permitido por link.",
                    color = c.muted, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        if (tracks.isNotEmpty() && filtered.isEmpty()) item {
            Text("Nenhum arquivo corresponde à busca.", color = c.muted, fontSize = 13.sp)
        }
        items(filtered, key = { it.videoId }) { song ->
            OcarinaCollectionSong(song, onClick = { onPlay(song) }, trailing = {
                IconButton(onClick = { onRemove(song) }) {
                    CIcon(LucideR.drawable.lucide_ic_trash_2, "Remover arquivo", Modifier.size(19.dp))
                }
            })
        }
    }
}

@Composable
fun OcarinaPickPlaylistDialog(playlists: List<OcarinaPlaylist>, song: Song,
    onPick: (OcarinaPlaylist) -> Unit, onCreate: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Adicionar à playlist") },
        text = { Column {
            Text(song.title, fontSize = 12.sp, color = ocarinaColors.muted, maxLines = 1)
            Spacer(Modifier.height(13.dp))
            OutlinedButton(onClick = onCreate) { Text("+ Criar nova playlist") }
            Spacer(Modifier.height(7.dp))
            LazyColumn(Modifier.heightIn(max = 305.dp)) {
                items(playlists, key = { it.id }) { list ->
                    Row(Modifier.fillMaxWidth().clickable { onPick(list) }.padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        OcarinaPlaylistCover(list, 38.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(list.name, maxLines = 1, modifier = Modifier.weight(1f))
                    }
                }
            }
        } },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } })
}

@Composable
fun OcarinaQueueDialog(queue: List<Song>, currentId: String?, repeat: Int,
    onSelect: (Song) -> Unit, onRemove: (Song) -> Unit, onDismiss: () -> Unit) {
    val c = ocarinaColors
    AlertDialog(onDismissRequest = onDismiss, shape = RoundedCornerShape(24.dp),
        containerColor = c.surface,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Próximas músicas", color = c.text, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text("${queue.size} faixas · ${when(repeat) { 1 -> "repetir uma"; 2 -> "repetir todas"; else -> "sem repetição" }}",
                    color = c.muted, fontSize = 12.sp)
            }
        },
        text = {
            if (queue.isEmpty()) Text("A fila está vazia.", color = c.muted)
            else LazyColumn(Modifier.heightIn(max = 385.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(queue, key = { it.videoId }) { song ->
                    val current = song.videoId == currentId
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
                        .background(if (current) c.softAccent else c.elevated.copy(alpha = 0.50f))
                        .clickable { onSelect(song) }.padding(start = 8.dp, top = 6.dp,
                            end = 2.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(45.dp).clip(RoundedCornerShape(10.dp))
                            .background(c.artworkSurface), contentAlignment = Alignment.Center) {
                            AlbumArtwork(song, song.title, Modifier.fillMaxSize(), small = true)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(song.title, color = if (current) c.accent else c.text,
                                fontWeight = FontWeight.SemiBold, maxLines = 1,
                                overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
                            Text(song.artist, color = c.muted, maxLines = 1,
                                overflow = TextOverflow.Ellipsis, fontSize = 11.sp)
                        }
                        IconButton(onClick = { onRemove(song) }, modifier = Modifier.size(37.dp)) {
                            CIcon(LucideR.drawable.lucide_ic_x, "Remover faixa", Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Concluir", color = c.accent) } },
    )
}
