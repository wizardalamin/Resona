package com.resona.player

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.resona.player.data.LibraryScanner
import com.resona.player.data.PlaylistDao
import com.resona.player.data.PlaylistEntity
import com.resona.player.data.PlaylistTrackEntity
import com.resona.player.data.ResonaDatabase
import com.resona.player.data.Track
import com.resona.player.playback.EqualizerController
import com.resona.player.playback.PlaybackService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class PlayerUiState(
    val library: List<Track> = emptyList(),
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playlists: List<PlaylistEntity> = emptyList()
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var controller: MediaController? = null
    private val dao: PlaylistDao = ResonaDatabase.get(application).playlistDao()

    // --- Equalizer wiring ---
    private val _equalizer = MutableStateFlow<EqualizerController?>(null)
    val equalizer: StateFlow<EqualizerController?> = _equalizer.asStateFlow()

    private var playbackService: PlaybackService? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = (binder as? PlaybackService.LocalBinder)?.getService() ?: return
            playbackService = service
            service.equalizerState
                .onEach { _equalizer.value = it }
                .launchIn(viewModelScope)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playbackService = null
            _equalizer.value = null
        }
    }

    init {
        val context = application
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        future.addListener({
            controller = future.get()
            controller?.addListener(playerListener)
        }, MoreExecutors.directExecutor())

        // Separate local bind (distinct action) so we get a direct reference
        // to PlaybackService for the equalizer, alongside the MediaController
        // above which handles transport commands.
        val bindIntent = Intent(context, PlaybackService::class.java).apply {
            action = PlaybackService.ACTION_LOCAL_BIND
        }
        context.bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        viewModelScope.launch {
            dao.observePlaylists().collect { lists ->
                _uiState.value = _uiState.value.copy(playlists = lists)
            }
        }
    }

    // --- Equalizer controls (proxy straight through to the live controller) ---

    fun eqBandCount(): Int = _equalizer.value?.bandCount ?: 0
    fun eqLevelRangeMb(): Pair<Int, Int> = _equalizer.value?.levelRangeMb ?: (-1500 to 1500)
    fun eqBandFrequencyHz(band: Int): Int = _equalizer.value?.bandFrequencyHz(band) ?: 0
    fun eqGetBandLevel(band: Int): Int = _equalizer.value?.getBandLevel(band) ?: 0
    fun eqSetBandLevel(band: Int, levelMb: Int) { _equalizer.value?.setBandLevel(band, levelMb) }
    fun eqPresetNames(): List<String> = _equalizer.value?.presetNames() ?: emptyList()
    fun eqApplyPreset(index: Int) { _equalizer.value?.applyPreset(index) }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val track = _uiState.value.library.find { it.uri == mediaItem?.mediaId }
            _uiState.value = _uiState.value.copy(
                currentTrack = track,
                durationMs = controller?.duration?.coerceAtLeast(0) ?: 0
            )
        }
    }

    fun loadLibrary() {
        viewModelScope.launch {
            val tracks = LibraryScanner.scan(getApplication())
            _uiState.value = _uiState.value.copy(library = tracks)
        }
    }

    fun playTrack(track: Track, queue: List<Track> = _uiState.value.library) {
        val items = queue.map { toMediaItem(it) }
        val startIndex = queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        controller?.setMediaItems(items, startIndex, 0L)
        controller?.prepare()
        controller?.play()
    }

    private fun toMediaItem(track: Track): MediaItem =
        MediaItem.Builder()
            .setMediaId(track.uri)
            .setUri(track.uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)
                    .build()
            )
            .build()

    fun togglePlayPause() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun next() = controller?.seekToNextMediaItem()
    fun previous() = controller?.seekToPreviousMediaItem()
    fun seekTo(positionMs: Long) = controller?.seekTo(positionMs)

    fun currentPositionMs(): Long = controller?.currentPosition ?: 0L

    // --- Playlists ---

    fun createPlaylist(name: String) = viewModelScope.launch {
        dao.insertPlaylist(PlaylistEntity(name = name))
    }

    fun deletePlaylist(id: Long) = viewModelScope.launch {
        dao.deletePlaylist(id)
    }

    fun addToPlaylist(playlistId: Long, track: Track) = viewModelScope.launch {
        val position = dao.trackCount(playlistId)
        dao.insertTrack(
            PlaylistTrackEntity(
                playlistId = playlistId,
                trackId = track.id,
                title = track.title,
                artist = track.artist,
                uri = track.uri,
                albumArtUri = track.albumArtUri,
                durationMs = track.durationMs,
                position = position
            )
        )
    }

    fun observePlaylistTracks(playlistId: Long) = dao.observeTracks(playlistId)

    override fun onCleared() {
        controller?.release()
        controller = null
        getApplication<Application>().unbindService(serviceConnection)
        super.onCleared()
    }
}
