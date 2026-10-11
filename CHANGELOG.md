# Changelog

## 1.0.0 — 2026-09-27 (scaffold; verified compiling, tested, packaged)
- Project scaffold: Gradle/module setup, manifest, theme and component copies from Pepe's Clock,
  `EmptyState` from Pepe's Notes.
- Domain models and pure utility functions with unit tests: `Lrc`, `QueueOps`, `Durations`,
  `SleepTimer`.
- `AppSettings` persistence (`PrefsSettingsRepository`) with round-trip and fallback tests.
- Four navigable tabs (Home, Search, Library, Radio) with empty states.
- Full Settings flow: Playback, Sources (YouTube Music primary, Audius/Jamendo/Deezer/
  iTunes/Radio Browser as reorderable, individually toggleable fallbacks), Appearance, About,
  Licenses.
- `YtMusicClient`: real search + best-effort stream resolution against YouTube Music's unofficial
  innertube API.
- See `docs/DECISIONS.md` for what is not yet built (playback service, Room database, the other
  API clients, full UI, widget, downloads, backup).
