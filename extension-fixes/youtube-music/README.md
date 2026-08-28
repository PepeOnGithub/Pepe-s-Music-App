# YouTube Music extension fixes

These are the actual fixes for the "Youtube Music" extension, applied and maintained on
[PepeOnGithub/Eco-Youtube_Music](https://github.com/PepeOnGithub/Eco-Youtube_Music) (a fork of
[Abhishek890/Eco-Youtube_Music](https://github.com/Abhishek890/Eco-Youtube_Music), kept in sync
with it via a scheduled workflow). This directory just mirrors the fixed files for reference —
**it is not part of the Echo app build**; the app loads the extension from the fork above.

## `YtContinuation.kt` / `EchoPlaylistContinuationEndpoint.kt`: crash on end-of-list

```
kotlinx.serialization.MissingFieldException: Field 'onResponseReceivedActions' is required
for type with serial name 'dev.brahmkshatriya.echo.extension.endpoints.YtContinuation',
but it was missing
```

YouTube's browse-continuation response omits `onResponseReceivedActions` once there are no
more items to continue (e.g. reaching the end of a playlist). The extension's model declared
the field as required, so deserialization threw instead of treating it as "no more results" —
the same shape of bug the rest of that extension's endpoints already guard against elsewhere
with nullable fields and safe calls. Made the field (and its nested fields on the same path)
nullable, falling back to an empty list.

## `EchoEnhancedSongEndpoint.kt`: tracks always showing "Unknown Artist"

The endpoint only fell back to the legacy song endpoint when the primary (ytm-kt) response was
missing `lyricsId`/`relatedId`/`isLiked`, never when it came back with an empty artists list.
Since ytm-kt's artist extraction is more fragile against YouTube's current response shape than
the legacy endpoint's independent fallback (which keys off menu icon type instead of browse page
type), a track with all three extras present but no artists never got a second chance and fell
straight through to "Unknown Artist". An empty artists list now also triggers the legacy fetch.

## To land either fix upstream at Abhishek890/Eco-Youtube_Music

This session has no write access there (not a collaborator), so it can't be pushed or PR'd
directly from here. From the fork above, GitHub's **Contribute → Open pull request** button
sends either commit upstream with one tap — no terminal required.
