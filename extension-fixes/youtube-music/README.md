# YouTube Music extension: YtContinuation crash fix

Fix for the stack trace reported against the "Youtube Music" extension (source:
[Abhishek890/Eco-Youtube_Music](https://github.com/Abhishek890/Eco-Youtube_Music)):

```
kotlinx.serialization.MissingFieldException: Field 'onResponseReceivedActions' is required
for type with serial name 'dev.brahmkshatriya.echo.extension.endpoints.YtContinuation',
but it was missing
```

YouTube's browse-continuation response omits `onResponseReceivedActions` once there are no
more items to continue (e.g. reaching the end of a playlist). The extension's model declared
the field as required, so deserialization threw instead of treating it as "no more results" —
the same shape of bug the rest of that extension's endpoints already guard against elsewhere
with nullable fields and safe calls.

This directory is **not part of the Echo app build** — these are drop-in replacements for the
two files of the same name in the extension's own repository:

- `endpoints/YtContinuation.kt` → `ext/src/main/java/dev/brahmkshatriya/echo/extension/endpoints/YtContinuation.kt`
- `endpoints/EchoPlaylistContinuationEndpoint.kt` → `ext/src/main/java/dev/brahmkshatriya/echo/extension/endpoints/EchoPlaylistContinuationEndpoint.kt`

## To land the fix upstream

This session has no write access to `Abhishek890/Eco-Youtube_Music` (not a collaborator), so
the fix can't be pushed or PR'd there directly from here. To get it merged:

1. Fork `Abhishek890/Eco-Youtube_Music` on GitHub (one tap on mobile: open the repo → **Fork**).
2. On the fork, open each of the two files above via GitHub's web editor (pencil icon) and
   paste in the contents from this folder, replacing the original.
3. Commit each change, then use GitHub's **Contribute → Open pull request** button on the
   fork's page to send it upstream.

No terminal or `git apply` required — steps 2 and 3 work entirely through the GitHub mobile
web UI.
