package com.resona.player.data

/**
 * A single audio file read from the device's MediaStore.
 * Nothing here is persisted unless the user adds it to a Playlist.
 */
data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: String,
    val albumArtUri: String?,
    val folderPath: String
)
