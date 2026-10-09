package com.resona.player.playback

import android.media.audiofx.Equalizer

/**
 * Thin wrapper over Android's built-in Equalizer audio effect.
 * This runs entirely on-device against the audio session; no
 * external DSP library or licensed code is involved.
 */
class EqualizerController(audioSessionId: Int) {

    private var equalizer: Equalizer? = try {
        Equalizer(0, audioSessionId).apply { enabled = true }
    } catch (e: Exception) {
        null
    }

    val bandCount: Int get() = equalizer?.numberOfBands?.toInt() ?: 0

    val levelRangeMb: Pair<Int, Int>
        get() {
            val range = equalizer?.bandLevelRange
            return Pair(range?.get(0)?.toInt() ?: -1500, range?.get(1)?.toInt() ?: 1500)
        }

    fun bandFrequencyHz(band: Int): Int =
        (equalizer?.getCenterFreq(band.toShort()) ?: 0) / 1000

    fun getBandLevel(band: Int): Int =
        equalizer?.getBandLevel(band.toShort())?.toInt() ?: 0

    fun setBandLevel(band: Int, levelMb: Int) {
        equalizer?.setBandLevel(band.toShort(), levelMb.toShort())
    }

    fun applyPreset(presetIndex: Int) {
        equalizer?.usePreset(presetIndex.toShort())
    }

    fun presetNames(): List<String> {
        val eq = equalizer ?: return emptyList()
        return (0 until eq.numberOfPresets).map { eq.getPresetName(it.toShort()) }
    }

    fun release() {
        equalizer?.release()
        equalizer = null
    }
}
