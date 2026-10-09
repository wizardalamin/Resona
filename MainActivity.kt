package com.resona.player

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.resona.player.ui.EqualizerScreen
import com.resona.player.ui.FolderDetailScreen
import com.resona.player.ui.FolderListScreen
import com.resona.player.ui.LibraryScreen
import com.resona.player.ui.NowPlayingScreen
import com.resona.player.ui.PlaylistsScreen
import com.resona.player.ui.ResonaTheme
import java.net.URLDecoder
import java.net.URLEncoder

class MainActivity : ComponentActivity() {

    private val viewModel: PlayerViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) viewModel.loadLibrary() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permission = if (Build.VERSION.SDK_INT >= 33)
            Manifest.permission.READ_MEDIA_AUDIO
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        permissionLauncher.launch(permission)

        setContent {
            ResonaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val uiState by viewModel.uiState.collectAsState()
                    val equalizer by viewModel.equalizer.collectAsState()

                    NavHost(navController = navController, startDestination = "library") {
                        composable("library") {
                            LibraryScreen(
                                uiState = uiState,
                                onTrackClick = { viewModel.playTrack(it) },
                                onOpenNowPlaying = { navController.navigate("nowplaying") },
                                onOpenPlaylists = { navController.navigate("playlists") },
                                onOpenFolders = { navController.navigate("folders") },
                                onAddToPlaylist = { playlistId, track ->
                                    viewModel.addToPlaylist(playlistId, track)
                                }
                            )
                        }
                        composable("nowplaying") {
                            NowPlayingScreen(
                                uiState = uiState,
                                onTogglePlay = { viewModel.togglePlayPause() },
                                onNext = { viewModel.next() },
                                onPrevious = { viewModel.previous() },
                                onSeek = { viewModel.seekTo(it) },
                                onOpenEqualizer = { navController.navigate("equalizer") },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("equalizer") {
                            EqualizerScreen(
                                equalizer = equalizer,
                                onBandLevelChange = { band, level -> viewModel.eqSetBandLevel(band, level) },
                                onApplyPreset = { index -> viewModel.eqApplyPreset(index) },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("playlists") {
                            PlaylistsScreen(
                                uiState = uiState,
                                onCreatePlaylist = { viewModel.createPlaylist(it) },
                                onDeletePlaylist = { viewModel.deletePlaylist(it) },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("folders") {
                            FolderListScreen(
                                library = uiState.library,
                                onFolderClick = { path ->
                                    val encoded = URLEncoder.encode(path, "UTF-8")
                                    navController.navigate("folder/$encoded")
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            route = "folder/{path}",
                            arguments = listOf(navArgument("path") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val encoded = backStackEntry.arguments?.getString("path") ?: ""
                            val decodedPath = URLDecoder.decode(encoded, "UTF-8")
                            FolderDetailScreen(
                                folderPath = decodedPath,
                                library = uiState.library,
                                onTrackClick = { track, queue -> viewModel.playTrack(track, queue) },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
