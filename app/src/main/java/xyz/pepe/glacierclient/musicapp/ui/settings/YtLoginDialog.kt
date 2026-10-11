package xyz.pepe.glacierclient.musicapp.ui.settings

import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import xyz.pepe.glacierclient.musicapp.di.AppContainer

/**
 * Signs in to YouTube Music, shown as a modal over the rest of the app. Uses the same WebView
 * recipe as brahmkshatriya/echo's YouTube extension (`echo-youtube-extension-old`) — verified by
 * reading its actual source, not guessed at:
 *  - the specific `accounts.google.com/v3/signin/identifier` sign-in URL (with `ltmpl=music`),
 *    not the generic `ServiceLogin` redirect tried earlier, which is what Google's real "This
 *    browser or app may not be secure" wall was blocking;
 *  - clearing all cookies and WebStorage before every attempt;
 *  - an intentionally old, generic mobile Chrome user-agent — a brand-new top-of-the-line desktop
 *    Chrome UA (tried earlier) is itself an unusual thing for an Android WebView to claim to be.
 * A manual cookie-paste fallback (the previous approach, which does independently work — it's how
 * ytmusicapi's own docs set up "browser auth") stays one tap away in case a Google-side change
 * breaks the WebView path again in the future.
 */
@Composable
fun YtLoginDialog(container: AppContainer, onDismiss: () -> Unit) {
  var showManualPaste by remember { mutableStateOf(false) }

  Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
    Surface(
      modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f).padding(16.dp),
      shape = MaterialTheme.shapes.extraLarge,
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
      Column(Modifier.fillMaxWidth().fillMaxHeight()) {
        Row(
          Modifier.fillMaxWidth().padding(start = 20.dp, end = 4.dp, top = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text("Sign in to YouTube Music", style = MaterialTheme.typography.titleMedium)
          IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, contentDescription = "Close") }
        }
        if (showManualPaste) {
          ManualCookiePaste(container, onDone = onDismiss, onBack = { showManualPaste = false }, modifier = Modifier.weight(1f))
        } else {
          Column(Modifier.weight(1f)) {
            AndroidView(modifier = Modifier.fillMaxWidth().weight(1f), factory = { ctx -> buildLoginWebView(ctx, container, onDismiss) })
            TextButton(onClick = { showManualPaste = true }, modifier = Modifier.padding(8.dp)) {
              Text("Having trouble? Paste a cookie manually instead")
            }
          }
        }
      }
    }
  }
}

private fun buildLoginWebView(context: android.content.Context, container: AppContainer, onDismiss: () -> Unit): WebView {
  // Wipe any leftover session before every attempt, same as echo's WebViewUtils.configure — this
  // is also what makes "add another account" show Google's account picker fresh each time.
  WebStorage.getInstance().deleteAllData()
  val cookieManager = CookieManager.getInstance()
  cookieManager.removeAllCookies(null)
  cookieManager.flush()

  return WebView(context).apply {
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.databaseEnabled = true
    // WebView tags every request with "X-Requested-With: <app package>" — Google's primary signal
    // for "embedded WebView", independent of user-agent. An empty allow-list suppresses it.
    if (androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
      androidx.webkit.WebSettingsCompat.setRequestedWithHeaderOriginAllowList(settings, emptySet())
    }
    cookieManager.setAcceptCookie(true)
    cookieManager.setAcceptThirdPartyCookies(this, true)
    // Deliberately NOT a fresh top-of-the-line desktop Chrome string — a WebView claiming to be
    // exactly that is itself an anomaly Google's sign-in page can flag. This exact UA is what
    // echo-youtube-extension-old uses.
    settings.userAgentString = "Mozilla/5.0 (Linux; Android 2; Jeff Bezos) AppleWebKit/537.36 " +
      "(KHTML, like Gecko) Chrome/66.0.3359.158 Mobile Safari/537.36"
    webViewClient = object : WebViewClient() {
      private var done = false
      // Google finishes sign-in on music.youtube.com, but YT Music is a single-page app, so the
      // final hop may only show up as a history update (not onPageFinished) — check on both, and
      // read cookies from both youtube hosts since SAPISID lives on .youtube.com.
      private fun tryCapture(url: String?) {
        if (done || url == null || !url.contains("youtube.com") || url.contains("accounts.google")) return
        cookieManager.flush()
        val cookie = listOf("https://music.youtube.com", "https://www.youtube.com")
          .mapNotNull { cookieManager.getCookie(it) }
          .firstOrNull { it.contains("SAPISID") }
        if (cookie != null) {
          done = true
          container.ytAuth.addAccount(cookie)
          onDismiss()
        }
      }
      override fun onPageFinished(view: WebView, url: String?) = tryCapture(url)
      override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) = tryCapture(url)
    }
    // Same specific sign-in flow URL echo-youtube-extension-old uses (v3/signin/identifier with
    // ltmpl=music), not the generic ServiceLogin redirect tried before, which Google was
    // specifically blocking with "This browser or app may not be secure".
    loadUrl(
      "https://accounts.google.com/v3/signin/identifier?dsh=S1527412391%3A1678373417598386" +
        "&continue=https%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26app%3Ddesktop" +
        "%26hl%3Den-GB%26next%3Dhttps%253A%252F%252Fmusic.youtube.com%252F%253Fcbrd%253D1%26feature%3D__FEATURE__" +
        "&hl=en-GB&ifkv=AWnogHfK4OXI8X1zVlVjzzjybvICXS4ojnbvzpE4Gn_Pfddw7fs3ERdfk-q3tRimJuoXjfofz6wuzg" +
        "&ltmpl=music&passive=true&service=youtube&uilel=3&flowName=GlifWebSignIn&flowEntry=ServiceLogin",
    )
  }
}

@Composable
private fun ManualCookiePaste(container: AppContainer, onDone: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
  var cookie by remember { mutableStateOf("") }
  var error by remember { mutableStateOf<String?>(null) }
  Column(modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
    Text(
      "1. On any device, open music.youtube.com in a browser and log in.\n" +
        "2. Open DevTools (F12 on desktop) → Network tab → reload the page.\n" +
        "3. Click any request to music.youtube.com → find the \"cookie\" request header → copy its full value.\n" +
        "4. Paste it below.",
      style = MaterialTheme.typography.bodyMedium,
    )
    OutlinedTextField(
      value = cookie,
      onValueChange = { cookie = it; error = null },
      label = { Text("Cookie header value") },
      isError = error != null,
      supportingText = error?.let { { Text(it) } },
      singleLine = false,
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
      modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(120.dp),
    )
    Row(
      Modifier.fillMaxWidth().padding(top = 12.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
    ) {
      TextButton(onClick = onBack) { Text("Back") }
      TextButton(onClick = {
        val trimmed = cookie.trim()
        if (!trimmed.contains("SAPISID") && !trimmed.contains("__Secure-3PAPISID")) {
          error = "Doesn't look like a full cookie header — make sure you copied the whole value"
        } else {
          container.ytAuth.addAccount(trimmed)
          onDone()
        }
      }) { Text("Add account") }
    }
  }
}
