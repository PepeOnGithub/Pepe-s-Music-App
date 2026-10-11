# APIs

## Primary source (added mid-build at the user's request)

### YouTube Music (unofficial "innertube" API)
- **Base**: `https://music.youtube.com/youtubei/v1/{search,player}`
- **Auth**: none — a public web-client API key embedded in `YtMusicClient` (the same key
  `music.youtube.com` itself uses in its browser JS; not a secret credential).
- **Status**: NOT a documented/public API. Reverse-engineered, same approach as SimpMusic,
  NewPipe and the various `youtube-music-api` OSS libraries. Can change or rate-limit without
  notice; likely against YouTube's Terms of Service for this kind of client access.
- **Used for**: search (`search` endpoint, `musicResponsiveListItemRenderer` parsing) and stream
  resolution (`player` endpoint, `streamingData.adaptiveFormats`/`formats`).
- **Known gap**: only formats with an already-unciphered `url` are used; signature-ciphered
  formats are skipped (see docs/DECISIONS.md).
- This is why every source below remains available as a user-toggleable, reorderable fallback in
  Settings → Sources, per the user's "make YouTube Music main, others fallback/choosable, very
  customisable" instruction.

## Fallback sources (from the original free-apis.github.io brief)

| Role | API | Base URL | Auth | Notes |
|---|---|---|---|---|
| Full-length streams | Audius | `https://api.audius.co` for host discovery, then `{host}/v1/...` | None | Host must be re-resolved periodically; rotate on 5xx. |
| Full-length streams (CC) | Jamendo | `https://api.jamendo.com/v3.0/` | Free `client_id` | CC-licensed catalog; license shown per track. |
| Live radio | Radio Browser | `https://all.api.radio-browser.info/json/` | None | Must call `/json/url/{uuid}` on every play per their rules. |
| Previews + search | Deezer | `https://api.deezer.com/` | None | 30 s previews only. |
| Previews + search | iTunes Search | `https://itunes.apple.com/search` | None | 30 s previews only. |
| Metadata | MusicBrainz | `https://musicbrainz.org/ws/2/` | None, but 1 req/s + required `User-Agent` | Not yet wired into a repository this session. |
| Cover art | Cover Art Archive | `https://coverartarchive.org/` | None | Not yet wired in. |
| Lyrics | LRCLIB | `https://lrclib.net/api/` | None | Not yet wired in. |
| Lyrics fallback | Lyrics.ovh | `https://api.lyrics.ovh/v1/` | None | Not yet wired in. |

None of these were exercised with live `curl` calls in this session (time was spent on the
YouTube Music pivot and the settings/customization work the user asked for instead). Retrofit
interfaces, DTOs, mappers and fixture-based `MappersTest`s for these sources are listed as
follow-up work in `docs/DECISIONS.md` under "Not yet implemented."

## Rejected

None formally rejected yet — the fallback sources above were carried over from the original brief
without re-verification this session; re-verify each with `curl` before wiring it into a
repository, per the original PART 3.1 procedure.
