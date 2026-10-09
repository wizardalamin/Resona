package com.resona.player.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.resona.player.data.Track

/** One row per distinct folder that contains at least one scanned track. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderListScreen(
    library: List<Track>,
    onFolderClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val folders = remember(library) {
        library.groupBy { it.folderPath }
            .toSortedMap()
            .map { (path, tracks) -> FolderSummary(path, tracks.size) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Folders") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (folders.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No folders found yet.")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                items(folders, key = { it.path }) { folder ->
                    ListItem(
                        leadingContent = { Icon(Icons.Filled.Folder, contentDescription = null) },
                        headlineContent = { Text(folder.displayName) },
                        supportingContent = { Text("${folder.trackCount} tracks") },
                        modifier = Modifier.clickableRow { onFolderClick(folder.path) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

/** Tracks within a single folder, playable as their own queue. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    folderPath: String,
    library: List<Track>,
    onTrackClick: (Track, List<Track>) -> Unit,
    onBack: () -> Unit
) {
    val tracksInFolder = remember(library, folderPath) {
        library.filter { it.folderPath == folderPath }
    }
    val displayName = remember(folderPath) { folderPath.substringAfterLast('/', folderPath) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(displayName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            items(tracksInFolder, key = { it.id }) { track ->
                ListItem(
                    headlineContent = { Text(track.title) },
                    supportingContent = { Text(track.artist) },
                    modifier = Modifier.clickableRow { onTrackClick(track, tracksInFolder) }
                )
                HorizontalDivider()
            }
        }
    }
}

private data class FolderSummary(val path: String, val trackCount: Int) {
    val displayName: String get() = path.substringAfterLast('/', path).ifBlank { "/" }
}

private fun Modifier.clickableRow(onClick: () -> Unit): Modifier =
    this.then(
        androidx.compose.foundation.clickable(onClick = onClick)
    )
