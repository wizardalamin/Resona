package com.resona.player.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.resona.player.PlayerUiState
import com.resona.player.data.PlaylistEntity
import com.resona.player.data.Track

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    uiState: PlayerUiState,
    onTrackClick: (Track) -> Unit,
    onOpenNowPlaying: () -> Unit,
    onOpenPlaylists: () -> Unit,
    onOpenFolders: () -> Unit,
    onAddToPlaylist: (Long, Track) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var trackForMenu by remember { mutableStateOf<Track?>(null) }

    val filtered = remember(uiState.library, query) {
        if (query.isBlank()) uiState.library
        else uiState.library.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resona", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onOpenFolders) {
                        Icon(Icons.Filled.Folder, contentDescription = "Folders")
                    }
                    IconButton(onClick = onOpenPlaylists) {
                        Icon(Icons.Filled.PlaylistPlay, contentDescription = "Playlists")
                    }
                }
            )
        },
        bottomBar = {
            uiState.currentTrack?.let { track ->
                MiniPlayerBar(
                    track = track,
                    isPlaying = uiState.isPlaying,
                    onClick = onOpenNowPlaying
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search your library") },
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )

            if (uiState.library.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.LibraryMusic,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("No local audio found yet.")
                    }
                }
            } else {
                LazyColumn {
                    items(filtered, key = { it.id }) { track ->
                        TrackRow(
                            track = track,
                            onClick = { onTrackClick(track) },
                            onLongClick = { trackForMenu = track }
                        )
                    }
                }
            }
        }
    }

    trackForMenu?.let { track ->
        AddToPlaylistSheet(
            playlists = uiState.playlists,
            onDismiss = { trackForMenu = null },
            onPick = { playlistId ->
                onAddToPlaylist(playlistId, track)
                trackForMenu = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrackRow(track: Track, onClick: () -> Unit, onLongClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(track.title) },
        supportingContent = { Text("${track.artist} · ${track.album}") },
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
    )
    HorizontalDivider()
}

@Composable
private fun MiniPlayerBar(track: Track, isPlaying: Boolean, onClick: () -> Unit) {
    Surface(tonalElevation = 4.dp, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(track.title, fontWeight = FontWeight.Medium, maxLines = 1)
                Text(track.artist, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            Text(if (isPlaying) "⏸" else "▶", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun AddToPlaylistSheet(
    playlists: List<PlaylistEntity>,
    onDismiss: () -> Unit,
    onPick: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to playlist") },
        text = {
            if (playlists.isEmpty()) {
                Text("Create a playlist first from the Playlists tab.")
            } else {
                Column {
                    playlists.forEach { pl ->
                        TextButton(onClick = { onPick(pl.id) }) { Text(pl.name) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
