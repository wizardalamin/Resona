package com.resona.player.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.resona.player.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Hosts the ExoPlayer instance and exposes it as a MediaSession so
 * system media controls, notifications and (optionally) Bluetooth/
 * Android Auto all work the normal Android way. Nothing here talks
 * to a server — playback is driven entirely by local content URIs.
 */
class PlaybackService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    private val _equalizer = MutableStateFlow<EqualizerController?>(null)
    /** Emits a fresh controller whenever the player's audio session (re)starts. */
    val equalizerState: StateFlow<EqualizerController?> = _equalizer.asStateFlow()

    /** Binder used for Resona's own in-app connection (separate from the
     *  MediaSessionService binding the system uses for transport controls). */
    inner class LocalBinder : Binder() {
        fun getService(): PlaybackService = this@PlaybackService
    }
    private val localBinder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder? {
        return if (intent?.action == ACTION_LOCAL_BIND) localBinder
        else super.onBind(intent)
    }

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        player = exoPlayer

        exoPlayer.addListener(object : androidx.media3.common.Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                _equalizer.value?.release()
                _equalizer.value = EqualizerController(audioSessionId)
            }
        })

        val sessionActivityIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setSessionActivity(sessionActivityIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        _equalizer.value?.release()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    companion object {
        const val ACTION_LOCAL_BIND = "com.resona.player.LOCAL_BIND"
    }
}
