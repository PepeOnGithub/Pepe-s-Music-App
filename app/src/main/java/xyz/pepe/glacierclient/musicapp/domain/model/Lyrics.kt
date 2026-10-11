package xyz.pepe.glacierclient.musicapp.domain.model

data class LyricLine(val timeMs: Long, val text: String)

data class Lyrics(val synced: List<LyricLine>?, val plain: String?, val source: String)
