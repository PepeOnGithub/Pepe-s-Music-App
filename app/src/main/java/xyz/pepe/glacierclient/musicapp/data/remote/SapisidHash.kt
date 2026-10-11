package xyz.pepe.glacierclient.musicapp.data.remote

import java.security.MessageDigest

/**
 * Google's SAPISIDHASH scheme: authorizes a request using a session cookie's SAPISID value
 * instead of full OAuth. This is the same mechanism ytmusicapi's "browser auth" mode and browser
 * extensions that call Google APIs use — it only requires the cookie, not a client secret.
 */
object SapisidHash {
  private const val ORIGIN = "https://music.youtube.com"

  /** Builds the `Authorization` header value, or null if [cookie] has no SAPISID. */
  fun authorizationHeader(cookie: String): String? {
    val sapisid = Regex("(?:^|;\\s*)(?:__Secure-3PAPISID|SAPISID)=([^;]+)").find(cookie)?.groupValues?.get(1) ?: return null
    val timestamp = System.currentTimeMillis() / 1000
    val raw = "$timestamp $sapisid $ORIGIN"
    val digest = MessageDigest.getInstance("SHA-1").digest(raw.toByteArray(Charsets.UTF_8))
    val hex = digest.joinToString("") { "%02x".format(it) }
    return "SAPISIDHASH ${timestamp}_$hex"
  }
}
