# Reliability

Modeled on Clock's `docs/RELIABILITY.md`. Current state reflects what actually exists after this
session — see `docs/DECISIONS.md` for the full "not yet implemented" list.

## Audio focus, noisy intent, process death
Not yet implemented — no `PlaybackService`/`ExoPlayer` exists yet. When built it must follow
Clock's precedent: `setAudioAttributes(..., handleAudioFocus = true)`,
`setHandleAudioBecomingNoisy(true)`, `setWakeMode(C.WAKE_MODE_NETWORK)`, and queue/position
persisted to survive process death (restored without auto-play).

## Network loss / dead sources
`YtMusicClient` throws `YtMusicException` on a non-2xx or empty body; callers should treat that as
a signal to fall back to the next enabled source in `AppSettings.sourcePriority`, mirroring
Weather's cache-then-network approach. That fallback chain in a `CatalogRepository` is not yet
built.

## YouTube Music specifically
- The innertube endpoints are unofficial. Expect occasional schema drift (renderer key names
  change) and possible IP-based rate limiting. `YtMusicClient`'s renderer walk
  (`findRenderers`/`findVideoId`) is deliberately tolerant of missing/extra fields rather than
  strict-schema parsing, but a full outage of the endpoint would need a fallback to the other
  sources — see above.
- Signature-ciphered stream formats are skipped rather than deciphered (documented limitation).

## Rate limits
- MusicBrainz's 1 request/second + required `User-Agent` rule is documented in `docs/APIS.md` but
  no `RateLimiter` exists yet since MusicBrainz isn't wired in.
- Radio Browser's "call `/json/url/{uuid}` on every play" rule is documented but not yet
  implemented (no radio playback exists yet).

## Doze / battery optimization, notification permission
Not yet applicable — no foreground service or notification channel exists yet.

## What's tested vs. manual
- **Tested** (JUnit4 + Robolectric, `./gradlew testDebugUnitTest`): `Lrc` parsing/seek-index,
  `QueueOps` shuffle/next/previous/move/remove, `Durations` formatting, `SleepTimer` fade math,
  and `PrefsSettingsRepository` defaults/round-trip/unknown-enum-fallback.
- **Manual**: everything else. No emulator or device was available in this environment (see the
  final report), so no UI, playback, or live-network behavior was exercised by hand — only
  `compileDebugKotlin`, `testDebugUnitTest` and `assembleDebug` were run.
