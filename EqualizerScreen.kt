package com.resona.player.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.resona.player.playback.EqualizerController

/**
 * Reads/writes the real Android Equalizer effect through the controller
 * exposed by PlaybackService (via PlayerViewModel). The controller is only
 * non-null once ExoPlayer has an active audio session, i.e. after a track
 * has started playing at least once.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    equalizer: EqualizerController?,
    onBandLevelChange: (band: Int, levelMb: Int) -> Unit,
    onApplyPreset: (Int) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Equalizer") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (equalizer == null || equalizer.bandCount == 0) {
            Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Play a track first to enable the equalizer.")
            }
            return@Scaffold
        }

        val bandCount = equalizer.bandCount
        val (minMb, maxMb) = equalizer.levelRangeMb
        val presets = equalizer.presetNames()
        var selectedPreset by remember { mutableStateOf(-1) }

        // Local mirror of band levels so sliders redraw instantly; source of
        // truth is still the Equalizer effect itself (read on screen entry).
        val bandLevels = remember(equalizer) {
            mutableStateListOf<Int>().apply {
                repeat(bandCount) { add(equalizer.getBandLevel(it)) }
            }
        }

        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {

            if (presets.isNotEmpty()) {
                Text("Presets", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.forEachIndexed { index, name ->
                        FilterChip(
                            selected = selectedPreset == index,
                            onClick = {
                                selectedPreset = index
                                onApplyPreset(index)
                                repeat(bandCount) { bandLevels[it] = equalizer.getBandLevel(it) }
                            },
                            label = { Text(name) }
                        )
                    }
                }
                Spacer(Modifier.height(32.dp))
            }

            Text("Bands", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
                for (band in 0 until bandCount) {
                    val freqHz = equalizer.bandFrequencyHz(band)
                    val label = if (freqHz >= 1000) "${freqHz / 1000}kHz" else "${freqHz}Hz"

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(label, modifier = Modifier.width(64.dp), style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = bandLevels[band].toFloat(),
                            onValueChange = { newValue ->
                                bandLevels[band] = newValue.toInt()
                                onBandLevelChange(band, newValue.toInt())
                            },
                            valueRange = minMb.toFloat()..maxMb.toFloat(),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${bandLevels[band] / 100}dB",
                            modifier = Modifier.width(48.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
