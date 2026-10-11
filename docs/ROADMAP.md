# Roadmap (user-supplied "ultimate music player" spec)

The user asked for a player combining Spotify + Apple Music + Poweramp + Samsung Music + VLC +
YouTube Music + Winamp + foobar2000: ~150 features and ~150 settings, all optional/toggleable.
That is a realistic multi-person, multi-month product, not something any single session builds.
This file is the honest backlog so the ask isn't lost — it's worked through incrementally, in
real, verified slices, each documented in `docs/DECISIONS.md`/`CHANGELOG.md` as it lands rather
than claimed up front.

## Full requested list (verbatim, for reference)

Playback: Play/Pause, Next/Previous, Shuffle, Repeat One/All, Gapless, Crossfade, Smart Crossfade,
ReplayGain, Normalize Volume, Fade In/Out, Sleep Timer, Skip Silence, Speed Control, Pitch
Control, A–B Loop, Bookmark Position, Resume Playback, Queue Management.

Library: auto/manual scan, folder/artist/album/genre/composer/year views, Recently
Added/Played, Most Played, Favorites, Hidden Songs, Duplicate Finder, Missing Metadata Finder,
custom tags, multi-select editing, batch rename, album/disc grouping.

Playlists: create, smart, collaborative, folders, M3U import/export, auto-generated, mood/
workout/sleep playlists, party mode, queue-as-temp-playlist, stats, cover generator, drag reorder.

Audio quality: 10/32-band EQ, parametric EQ, bass/treble/vocal boost, surround, Dolby, Spatial
Audio, 3D audio, balance, mono, channel swap, Hi-Res, DSD.

Lyrics & content: synced/static lyrics, karaoke, translation, sharing, bios, reviews, credits,
song meanings, related artists.

Discovery: AI recommendations, similar songs/artists, Discover Weekly, new releases, trending,
concert suggestions, radio, mood/BPM/key detection, genre AI, song recognition, service import,
personalized mixes.

Social: friend activity, live listening, share timestamp, QR sharing, streaks, stats, monthly/
yearly recap, comments, reactions.

Offline & sync: downloads, SD card, Wi-Fi-only downloads, smart downloads, backup, cloud sync,
multi-device sync, Chromecast, AirPlay, Bluetooth LE Audio, Android Auto, Wear OS, car mode,
offline lyrics/artwork.

Advanced: waveform/spectrum/Winamp visualizers, folder lock, hidden mode, gestures, shake-to-skip,
double-tap headphones, voice control, automation, plugin system, theme marketplace, Material You,
custom fonts, animated backgrounds, OLED black, battery saver, developer mode, audio diagnostics.

Settings: ~150 toggles across Appearance, Playback, Audio, Library, Lyrics, Downloads, Privacy,
Notifications, Gestures, Developer/Experimental (full list as given by the user, unabridged,
kept in the original message — not re-typed here to avoid drift; treat the user's own message as
the source of truth for exact wording).

"Insane premium" extras: AI DJ, Dynamic Island animations, 3D vinyl mode, lock-screen waveform,
discovery-by-country map, "one year ago today", mood timeline, concert mode with setlists, stem
separation, 100+-language live lyric translation, Winamp shader visualizers, plugin SDK.

## Phased backlog — 20 items per phase, priority order

Priority tiers: **P0** core, ship-blocking · **P1** high-value, buildable with current stack ·
**P2** nice-to-have, needs more plumbing (Room, downloads, notifications) · **P3** differentiator,
needs real engineering effort (DSP, EQ, visualizers) · **P4** moonshot — needs infra this app
doesn't have (backend, licensed codecs, ML models, social graph).

### Feature phases

**Phase F1 — Core playback (P0).** Play/Pause *(done)*, Next/Previous *(done)*, Shuffle *(done)*,
Repeat One *(done)*, Repeat All *(done)*, Gapless Playback, Crossfade, Smart Crossfade, ReplayGain,
Normalize Volume, Fade In, Fade Out, Sleep Timer *(done)*, Skip Silence, Speed Control, Pitch
Control, A–B Loop, Bookmark Position, Resume Playback *(done)*, Queue Management *(done: reorder,
remove, play-next/add-to-queue via the new Now Playing screen)*.

**Phase F2 — Library views (P1).** Automatic scan, manual scan, folder browsing, artist view,
album view, genre view, composer view, year view, Recently Added, Most Played, Recently Played,
Favorites *(done)*, Hidden Songs, Duplicate Finder, Missing Metadata Finder, custom tags,
multi-select editing, batch rename, album grouping, disc grouping.

**Phase F3 — Playlists + basic EQ (P1/P2).** Create playlists, smart playlists, collaborative
playlists, playlist folders, import M3U, export M3U, auto-generated playlists, mood playlists,
workout playlists, sleep playlists, party mode, queue-as-temp-playlist, playlist stats, cover
generator, drag-and-drop ordering, 10-band EQ, 32-band EQ, parametric EQ, bass boost, treble
boost.

**Phase F4 — Advanced audio + lyrics (P2/P3).** Vocal boost, surround sound, Dolby support,
Spatial Audio, 3D audio, balance control, mono mode, L/R channel swap, Hi-Res playback, DSD
support, synchronized lyrics, static lyrics, karaoke mode, lyrics translation, lyrics sharing,
artist biographies, album reviews, credits, song meanings, related artists.

**Phase F5 — Discovery + early social (P2/P4).** AI recommendations, similar songs, similar
artists, Discover Weekly, New Releases, trending tracks, local concert suggestions, radio
stations, mood detection, BPM finder, key detection, genre AI detection, song recognition, import
from other services, personalized mixes, friend activity, live listening, share timestamp, QR
code sharing, listening streaks.

**Phase F6 — Social stats + offline/sync core (P2/P4).** Listening stats, monthly recap, yearly
recap, comments, reactions, offline downloads, SD card storage, Wi-Fi-only downloads, Smart
Downloads, backup library, cloud sync, multi-device sync, Chromecast, AirPlay, Bluetooth LE
Audio, Android Auto, Wear OS support, car mode, offline lyrics, offline artwork.

**Phase F7 — Advanced/platform (P3/P4).** Waveform view, spectrum visualizer, Winamp
visualizations, folder lock, hidden mode, gesture controls, shake to skip, double-tap headphones,
voice control, automation routines, plugin system, theme marketplace, Material You, dynamic
colors, custom fonts, animated backgrounds, OLED black mode, battery saver mode, developer mode,
audio diagnostics.

### Settings phases

**Phase S1 — Appearance (P1).** Light, Dark, AMOLED, System theme, Material You colors, accent
color, album art blur, album art opacity, rounded corners, font size, font family, bold text,
compact mode, tablet layout, landscape layout, mini player style *(done: classic/floating)*,
player animation speed, icon style, navigation style *(done)*, wallpaper sync.

**Phase S2 — Playback (P0/P1).** Crossfade duration *(done)*, gapless toggle *(done)*, ReplayGain
mode, normalize volume, fade duration, default playback speed *(done)*, pitch lock, skip-silence
sensitivity *(done: on/off)*, auto resume, remember queue, default repeat, shuffle algorithm,
headphone auto-play, Bluetooth auto-play, default sleep timer, skip-button seconds, double-tap
rewind time, long-press behavior, queue persistence, end-of-playlist behavior.

**Phase S3 — Audio (P2/P3).** EQ preset, custom EQ, bass level, treble level, balance, mono
audio, spatial audio, surround mode, Hi-Res toggle, sample rate, bit depth, audio output
selection, Bluetooth codec preference, loudness, limiter, compressor, channel swap, DSD mode, USB
DAC priority, exclusive mode.

**Phase S4 — Library (P1/P2).** Auto scan, scan frequency, include hidden folders, SD card
priority, ignore short files, show podcasts, show audiobooks, show recordings, album sort, artist
sort, year sort, group discs, show singles separately, metadata source, artwork source, artwork
quality, folder hierarchy, duplicate detection, Recently Added period, hidden songs list.

**Phase S5 — Lyrics + downloads (P2).** Auto-fetch lyrics, sync lyrics *(done, model exists)*,
translate lyrics, font size *(done)*, background blur, karaoke highlight, offline lyrics, lyrics
provider, auto-scroll speed, explicit-lyrics filter, download quality, Wi-Fi only *(done)*,
cellular downloads, Smart Downloads, storage location, cache size, auto-delete cache, offline
expiry, download notifications, battery limit for downloads.

**Phase S6 — Privacy + notifications (P1/P2).** Private session, hide listening activity, disable
analytics *(moot — none exist)*, crash reports, personalized recommendations, clear history,
clear cache, lock app, fingerprint unlock, PIN unlock, lock-screen controls, notification style,
album art in notification, progress bar, headphone notifications, download notifications,
new-release notifications, friend-activity alerts, daily-recap reminder, sleep-timer reminder.

**Phase S7 — Gestures + controls (P2).** Swipe-left action, swipe-right action, double-tap
action, triple-tap action, volume-button skip, shake to skip, headset-button mapping, Wear OS
controls, car-mode auto-launch, voice-assistant integration, lock-screen gestures, Quick Settings
tile, widget style, widget transparency, widget size, beta features, debug logging, performance
overlay, animation FPS, GPU rendering.

**Phase S8 — Developer/experimental (P3/P4).** Audio latency display, plugin manager, custom CSS
theme, import settings, export settings, backup preferences, restore preferences, API access,
developer shortcuts, reset all settings.

**Phase X — Moonshot extras (P4, needs infra this app doesn't have).** AI DJ, Dynamic Island/Now
Playing animations, 3D rotating vinyl mode, lock-screen waveform, country discovery map, "one
year ago today," mood timeline, concert mode with setlists, stem separation, 100+-language live
lyric translation, Winamp shader visualizers, plugin SDK.

## What's realistically buildable incrementally, and in what order

1. **Favorites/Likes** (Library, Playback) — done.
2. **Now Playing full screen + Queue sheet + Sleep Timer** — done.
3. **Resume playback across app restarts** — done.
4. **Radio Browser client for the Radio tab** — done. All four tabs (Home, Search, Library,
   Radio) now do real work; none are placeholders.
5. **Basic EQ** — done.
6. **Room database + Playlists** (create/rename/delete/reorder, add-from-anywhere) — done (see
   below). Favorites still uses SharedPreferences — migrating it to Room too is a reasonable
   future cleanup, not required for playlists to work.

Everything past that (AI DJ, stem separation, social features, cloud sync, Android
Auto/Wear/Chromecast, plugin SDK, theme marketplace, 32-band EQ, Hi-Res/DSD, Dolby/Spatial Audio)
requires either licensed SDKs, backend infrastructure this app doesn't have, or scope well beyond
an incremental session. Flagged here rather than silently dropped.

## This round's addition: Now Playing, Sleep Timer, Queue, Resume Playback

- New `ui/player/NowPlayingScreen.kt`: full player screen (artwork, seek bar, shuffle/repeat/
  prev/play/next), reachable by tapping either mini player (previously a no-op).
- `PlayerController` gained `setSleepTimer(minutes)`/`cancelSleepTimer()` — fades volume out over
  the last 10s (via the already-tested `SleepTimer` util) then pauses; exposed as a menu on the
  Now Playing screen.
- Queue management is real: drag-to-reorder (`moveQueueItem`, using the tested `QueueOps.move`),
  remove (`removeFromQueue`), and `playNext`/`addToQueue` for inserting tracks — the "Up next"
  list on Now Playing is a live, reorderable view of the actual playback queue, not a mock.
- `data/repository/PlaybackStateRepository.kt` (new): persists the queue/index/position to disk;
  `PlayerController` restores it on cold start (gated by the new "Resume playback on launch"
  toggle in Settings → Playback, default on) so the mini player shows the last track immediately
  and resumes from where it left off on the next tap of Play.

## Previous addition: Favorites

- `data/repository/FavoritesRepository.kt`: SharedPreferences-backed like/unlike, persisted as a
  JSON array of serialized tracks (not just keys, so Library can render them without a re-fetch).
- `TrackRow` now takes an optional trailing heart toggle; Home and Search wire it in.
- Library's "Liked" section (previously a static empty state) now reads real favorited tracks and
  can play them.

## This round's addition: real Basic EQ

Implemented item #6 from the buildable-order list: a working equalizer, not a placeholder toggle.

- `android.media.audiofx.Equalizer` attached to `ExoPlayer`'s own audio session inside
  `PlaybackService` — Android's standard audio-effect API, not anything hand-rolled.
- Because the effect has to live in the playback process while the UI talks to it only through a
  `MediaSession`, control is wired through session custom commands (`eq.describe`,
  `eq.setEnabled`, `eq.setPreset`) — `PlayerController.eq: StateFlow<EqState>` mirrors the real
  device state (supported/not, available presets, enabled, current preset) rather than assuming a
  fixed band count, since that's genuinely device-dependent.
- Settings → Playback → Equalizer: on/off toggle plus a preset picker built from
  `Equalizer.getPresetName()` for whatever presets the device actually reports.
- `eqEnabled`/`eqPreset` persist in `AppSettings` and are re-applied by `PlaybackService` on
  create, so the setting survives the service being killed and recreated.
- Per-band manual sliders (not just presets) are a reasonable next step but weren't built this
  round — the preset picker alone is the honestly-scoped "Basic EQ" this phase called for.

## This round's addition: Room database + real Playlists

Implemented items #6 and #7 from the buildable-order list — the app's first real relational
storage, not more SharedPreferences+JSON:

- `data/local/AppDatabase.kt`, `PlaylistEntities.kt`, `PlaylistDao.kt`: a real Room database
  (`playlists` + `playlist_tracks` tables) with proper queries — track count via SQL `COUNT`/
  `JOIN`, not loading everything into memory, membership checks (`containsTrack`) before
  inserting, and reordering via a transactional batch position update.
- `PlaylistRepository` wraps the DAO and maps rows to/from the existing `Track`/`Playlist` domain
  models — the rest of the app never touches Room types directly.
- Library tab: a real Playlists section above Liked — create (dialog), tap to open, and inside a
  playlist: reorder (drag handle, same `sh.calvin.reorderable` pattern as Sources/queue), remove,
  rename, delete, and play any track (queues the whole playlist from that point).
- Search and Home track rows gained a queue-plus icon next to the heart — "add to playlist" from
  wherever you find a song, not only from inside a playlist screen. Picks an existing playlist or
  creates a new one in the same dialog.
- Not built this round: drag-reorder of playlists themselves (only tracks within a playlist),
  M3U import/export, and collaborative/smart playlists — all explicitly out of scope for "Room
  database + basic playlists," not silently dropped.
