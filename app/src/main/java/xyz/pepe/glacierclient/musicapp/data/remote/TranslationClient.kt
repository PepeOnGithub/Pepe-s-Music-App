package xyz.pepe.glacierclient.musicapp.data.remote

import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * On-device lyric translation via Google ML Kit — no API key, no account, no server call for the
 * actual translation: the language model downloads once (over whatever network policy the device
 * has) and every translation after that runs locally on the phone. This is the "phone as its own
 * backend" approach for translation, since there's no free hosted translation API without a key.
 */
class TranslationClient {
  private val translators = mutableMapOf<String, Translator>()

  private fun translatorFor(targetLanguageTag: String): Translator = translators.getOrPut(targetLanguageTag) {
    val target = TranslateLanguage.fromLanguageTag(targetLanguageTag) ?: targetLanguageTag
    val options = TranslatorOptions.Builder()
      .setSourceLanguage(TranslateLanguage.ENGLISH) // lrclib lyrics are predominantly English-transliterated; best-effort source
      .setTargetLanguage(target)
      .build()
    Translation.getClient(options)
  }

  suspend fun ensureModelDownloaded(targetLanguageTag: String, wifiOnly: Boolean): Boolean = suspendCancellableCoroutine { cont ->
    val translator = translatorFor(targetLanguageTag)
    val conditions = com.google.mlkit.common.model.DownloadConditions.Builder().apply {
      if (wifiOnly) requireWifi()
    }.build()
    translator.downloadModelIfNeeded(conditions)
      .addOnSuccessListener { if (cont.isActive) cont.resume(true) }
      .addOnFailureListener { if (cont.isActive) cont.resume(false) }
  }

  suspend fun translate(text: String, targetLanguageTag: String): String? = suspendCancellableCoroutine { cont ->
    val translator = translatorFor(targetLanguageTag)
    translator.translate(text)
      .addOnSuccessListener { if (cont.isActive) cont.resume(it) }
      .addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
  }

  companion object {
    // A short, real, well-supported subset of ML Kit's on-device language list.
    val LANGUAGES = listOf(
      "es" to "Spanish", "fr" to "French", "de" to "German", "hi" to "Hindi", "pt" to "Portuguese",
      "ar" to "Arabic", "ja" to "Japanese", "ko" to "Korean", "zh" to "Chinese", "ru" to "Russian",
    )
  }
}
