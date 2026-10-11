# Pepe's Music

A free, account-less music player — the fifth "Pepe's …" sibling app. Streams from YouTube Music
as the primary source, with Audius, Jamendo, Deezer, Apple iTunes Search and Radio Browser as
user-toggleable, reorderable fallback sources.

## Status

This is an early, compiling scaffold, not the full feature set described in the original brief.
See `docs/DECISIONS.md` for exactly what changed mid-build (YouTube Music primary source, at the
user's request) and what is not yet implemented (playback service, Room database, the fallback
API clients, full Home/Search/Library/Radio/Now-Playing UI, widget, downloads, backup).

## Features (current)

- Four tabs: Home, Search, Library, Radio — each a real `PageScaffold` with a settings action and
  a genuine empty state (no fake data).
- Settings: Playback (crossfade, gapless, skip silence, default speed), **Sources** (YouTube Music
  as the default primary source; every fallback source can be individually enabled/disabled and
  drag-reordered by priority), Appearance (nav style, artwork colors, haptics), About, Licenses.
- `YtMusicClient`: a real client for YouTube Music's unofficial "innertube" API — search plus
  best-effort stream URL resolution (see the known cipher limitation in `docs/DECISIONS.md`).
- Settings persist as JSON in `SharedPreferences`, restored on launch.

## Sources

| Source | Role | Auth |
|---|---|---|
| YouTube Music (primary) | Full tracks, search | None (unofficial API) |
| Audius | Full tracks | None |
| Jamendo | Full tracks, Creative Commons | Free client ID |
| Deezer | 30 s previews | None |
| Apple iTunes Search | 30 s previews | None |
| Radio Browser | Live radio | None |

## Privacy

No accounts, no ads, no analytics. See the in-app About page for the full list of hosts contacted.

## Build

```bash
./gradlew compileDebugKotlin
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew installDebug   # onto a connected device/emulator
```

## Architecture

```
MainActivity → AppRoot (4 tabs) ──┬─ HomeScreen
                                    ├─ SearchScreen
                                    ├─ LibraryScreen
                                    └─ RadioScreen
             → SettingsHost (Playback / Sources / Appearance / About / Licenses)

AppContainer (manual DI)
  ├─ okHttp (User-Agent interceptor)
  ├─ settings: SettingsRepository (SharedPreferences JSON, StateFlow)
  └─ ytMusic: YtMusicClient (innertube search + stream resolution)
```

## Credits

Family theme, `Controls.kt` and `PageScaffold.kt` copied from Pepe's Clock. `EmptyState` copied
from Pepe's Notes. YouTube Music access modeled on
[SimpMusic](https://github.com/maxrave-dev/SimpMusic)'s approach to the innertube API.
