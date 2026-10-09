# Resona — a local music player

A from-scratch Android music player (MVP), built with Kotlin + Jetpack Compose +
Media3 (ExoPlayer). Fully offline: no account, no license server, no network
permission requested at all.

## What's included
- **Library**: scans local audio via `MediaStore`, searchable list
- **Playback**: ExoPlayer-backed `MediaSessionService` (background playback,
  system notification controls, lock-screen controls)
- **Playlists**: create/delete, add tracks, stored locally in a Room database
- **Equalizer**: 5-band EQ UI wired to Android's native `Equalizer` audio effect
  (see note below on finishing the wiring)
- **Now Playing**: transport controls + seek bar

## Equalizer wiring (now live)
`PlayerViewModel` binds to `PlaybackService` with a local `bindService` call
(separate from the `MediaController` connection used for transport controls)
and observes `PlaybackService.equalizerState`, a `StateFlow<EqualizerController?>`
that emits a fresh controller whenever ExoPlayer's audio session starts or
changes. The Equalizer screen reads/writes real band levels and presets
through that controller — slider moves call straight into Android's native
`android.media.audiofx.Equalizer` effect on the live audio session. Note the
equalizer is only usable once a track has started playing at least once
(that's when Android assigns the audio session); the screen shows a short
message until then.

## Folder browsing (now included)
`FoldersScreen.kt` adds two screens: a folder list (grouped from
`Track.folderPath`, which `LibraryScanner` already captures from MediaStore)
and a per-folder track list that plays as its own queue. Reachable from the
folder icon in the Library top bar.

## What's intentionally left as a next step
- No launcher icon PNGs — I included a simple original vector icon
  (`ic_launcher_foreground.xml`) so the app isn't using any borrowed artwork;
  swap in your own `mipmap` PNGs whenever you want.
- Folder-view browsing, tag editing, and widgets (mentioned as "full-featured"
  options) aren't in this MVP — straightforward to add on top of `LibraryScanner`
  and a new Compose screen when you're ready.

## How to build it
1. Install **Android Studio** (free, from developer.android.com/studio).
2. Open this folder (`Resona/`) as a project — Android Studio will prompt to
   sync Gradle; let it download the SDK/toolchain if asked.
3. Plug in a device (USB debugging on) or start an emulator.
3. Click **Run ▶**. First launch will prompt for the audio permission — grant
   it so the library scan can find your files.

## Why it's genuinely yours
Every file here is original code written for this project — no decompiled
Poweramp code, no stripped license checks, no borrowed assets. The feature
*ideas* (gapless-capable engine, EQ, playlists) are common to basically every
music player and aren't owned by any one app.
