// CANONICAL COPY — edit only in pepe-ecosystem/, then run pepe-ecosystem/sync.sh.
// Every Pepe app carries a byte-identical copy of this file (sync.sh --check verifies it).
package xyz.pepe.glacierclient.ecosystem

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * The shared contract that makes the six Pepe apps one ecosystem.
 *
 * - Every cross-app request is a typed [Action] with one canonical `pepe://<app>/<verb>?…` URI.
 * - [parse] is the single inbound validator: unknown hosts/verbs, oversize payloads and
 *   out-of-range numbers are rejected here, once, instead of in six hand-rolled handlers.
 * - [send] is the single outbound router: explicit package (no other app can intercept it), and
 *   it reports "not installed" instead of throwing, so callers can fall back instead of breaking.
 * - Each app implements [Agent] for the actions addressed to it; nothing else is app-specific.
 *
 * Notes and Clock are reached through the standard Android intents they already handle
 * (`ACTION_SEND`, `AlarmClock.ACTION_SET_TIMER`/`SET_ALARM`), so they need no new code to receive.
 */
object PepeEcosystem {
  const val SCHEME = "pepe"
  const val MAX_TEXT = 10_000
  const val MAX_QUERY = 200

  enum class App(val host: String, val packageName: String, val label: String) {
    CALENDAR("calendar", "xyz.pepe.glacierclient.calendarapp", "Pepe's Calendar"),
    CALCULATOR("calculator", "xyz.pepe.glacierclient.calculatorapp", "Pepe's Calculator"),
    CLOCK("clock", "xyz.pepe.glacierclient.clockapp", "Pepe's Clock"),
    MUSIC("music", "xyz.pepe.glacierclient.musicapp", "Pepe's Music"),
    NOTES("notes", "xyz.pepe.glacierclient.notesapp", "Pepe's Notes"),
    TRANSLATOR("translator", "xyz.pepe.glacierclient.translatorapp", "Pepe's Translator"),
    WEATHER("weather", "xyz.pepe.glacierclient.weatherapp", "Pepe's Weather");

    companion object {
      fun ofHost(host: String?) = entries.firstOrNull { it.host == host }
      fun ofPackage(pkg: String?) = entries.firstOrNull { it.packageName == pkg }
    }
  }

  sealed interface Action {
    val app: App
    val verb: String
    val params: Map<String, String>
    fun toUri(): String = buildString {
      append("$SCHEME://${app.host}/$verb")
      val p = params.filterValues { it.isNotEmpty() }
      if (p.isNotEmpty()) append(p.entries.joinToString("&", "?") { (k, v) -> "$k=${enc(v)}" })
    }
  }

  /** Opens an app at its home screen. */
  data class Open(override val app: App) : Action {
    override val verb get() = "open"
    override val params get() = emptyMap<String, String>()
  }
  /** Search every enabled music source and play the results. */
  data class PlayMusic(val query: String) : Action {
    override val app get() = App.MUSIC
    override val verb get() = "play"
    override val params get() = mapOf("q" to query)
  }
  /** Prefill the translator; [to] is an ISO-639 code or empty for the user's default. */
  data class Translate(val text: String, val to: String = "") : Action {
    override val app get() = App.TRANSLATOR
    override val verb get() = "translate"
    override val params get() = mapOf("text" to text, "to" to to)
  }
  /** Open a new note pre-filled with [text] (the user still decides whether to keep it). */
  data class CreateNote(val text: String, val title: String = "") : Action {
    override val app get() = App.NOTES
    override val verb get() = "create"
    override val params get() = mapOf("text" to text, "title" to title)
  }
  /** Show the weather; [place] empty = current/selected location. */
  data class ShowWeather(val place: String = "") : Action {
    override val app get() = App.WEATHER
    override val verb get() = "show"
    override val params get() = mapOf("place" to place)
  }
  /** Prefill the calculator with an expression. */
  data class Calculate(val expression: String) : Action {
    override val app get() = App.CALCULATOR
    override val verb get() = "calc"
    override val params get() = mapOf("expr" to expression)
  }
  /** Open Calendar's editor prefilled. [startMillis] 0 = let the user pick; [title] may be empty. */
  data class AddEvent(val title: String, val startMillis: Long = 0, val location: String = "") : Action {
    override val app get() = App.CALENDAR
    override val verb get() = "add"
    override val params get() = mapOf("title" to title, "start" to if (startMillis > 0) startMillis.toString() else "", "location" to location)
  }
  /** Start a countdown in Clock. */
  data class StartTimer(val seconds: Int, val label: String = "") : Action {
    override val app get() = App.CLOCK
    override val verb get() = "timer"
    override val params get() = mapOf("seconds" to seconds.toString(), "label" to label)
  }

  /** Implemented once per app: handle the actions addressed to it, return false to ignore. */
  fun interface Agent {
    fun handle(action: Action): Boolean
  }

  private val EXPR_CHARS = Regex("""^[0-9a-zA-Z+\-*/^().,%!πe√ ×÷−]*$""")

  /**
   * Validates an inbound `pepe://` URI. Pure JVM (no android.net.Uri) so it is unit-testable.
   * Returns null for anything malformed, unknown, empty or out of range — never throws.
   */
  fun parse(uri: String?): Action? = runCatching {
    if (uri == null || uri.length > MAX_TEXT * 4) return null
    val rest = uri.removePrefix("$SCHEME://").takeIf { it != uri } ?: return null
    val pathEnd = rest.indexOf('?').let { if (it < 0) rest.length else it }
    val segments = rest.substring(0, pathEnd).split('/').filter { it.isNotEmpty() }
    val app = App.ofHost(segments.getOrNull(0)?.lowercase()) ?: return null
    val verb = segments.getOrNull(1)?.lowercase() ?: "open"
    val q = query(rest.substring(pathEnd).removePrefix("?"))
    fun text(k: String, max: Int) = q[k]?.trim()?.takeIf { it.isNotEmpty() && it.length <= max }
    when (app to verb) {
      App.MUSIC to "play" -> text("q", MAX_QUERY)?.let(::PlayMusic)
      App.TRANSLATOR to "translate" -> text("text", MAX_TEXT)?.let { t ->
        Translate(t, q["to"]?.lowercase()?.takeIf { it.matches(Regex("[a-z]{2,3}(-[a-z]{2,4})?")) }.orEmpty())
      }
      App.NOTES to "create" -> text("text", MAX_TEXT)?.let { CreateNote(it, text("title", MAX_QUERY).orEmpty()) }
      App.WEATHER to "show" -> ShowWeather(text("place", MAX_QUERY).orEmpty())
      App.CALCULATOR to "calc" -> text("expr", 500)?.takeIf { EXPR_CHARS.matches(it) }?.let(::Calculate)
      App.CALENDAR to "add" -> {
        val title = text("title", MAX_QUERY).orEmpty()
        val start = q["start"]?.toLongOrNull()?.takeIf { it in 1..4_102_444_800_000L } ?: 0L // before year 2100
        if (title.isEmpty() && start == 0L) null else AddEvent(title, start, text("location", MAX_QUERY).orEmpty())
      }
      App.CLOCK to "timer" -> q["seconds"]?.toIntOrNull()?.takeIf { it in 1..86_400 }
        ?.let { StartTimer(it, text("label", MAX_QUERY).orEmpty()) }
      else -> if (verb == "open") Open(app) else null
    }
  }.getOrNull()

  private fun query(raw: String): Map<String, String> = raw.split('&').mapNotNull { pair ->
    val i = pair.indexOf('=')
    if (i <= 0) null else pair.substring(0, i) to URLDecoder.decode(pair.substring(i + 1), "UTF-8")
  }.toMap()

  private fun enc(v: String): String = URLEncoder.encode(v, "UTF-8").replace("+", "%20")

  // ---- Android side ----

  fun isInstalled(context: Context, app: App): Boolean =
    context.packageManager.getLaunchIntentForPackage(app.packageName) != null

  /** Sibling apps installed on this device, excluding the caller. */
  fun installedSiblings(context: Context): List<App> =
    App.entries.filter { it.packageName != context.packageName && isInstalled(context, it) }

  /** The explicit intent for [action], or null if the target app isn't installed. */
  fun intentFor(context: Context, action: Action): Intent? {
    if (!isInstalled(context, action.app)) return null
    val pkg = action.app.packageName
    val intent = when (action) {
      is Open -> context.packageManager.getLaunchIntentForPackage(pkg)
      // Notes and Clock already handle these platform intents; no Notes/Clock code changes needed.
      is CreateNote -> Intent(Intent.ACTION_SEND).setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, action.text)
        .apply { if (action.title.isNotEmpty()) putExtra(Intent.EXTRA_SUBJECT, action.title) }
      is StartTimer -> Intent(AlarmClock.ACTION_SET_TIMER)
        .putExtra(AlarmClock.EXTRA_LENGTH, action.seconds)
        .putExtra(AlarmClock.EXTRA_SKIP_UI, false)
        .apply { if (action.label.isNotEmpty()) putExtra(AlarmClock.EXTRA_MESSAGE, action.label) }
      else -> Intent(Intent.ACTION_VIEW, Uri.parse(action.toUri()))
    } ?: return null
    return intent.setPackage(pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
  }

  /** Routes [action] to its app. Returns false (never throws) if it isn't installed or can't open. */
  fun send(context: Context, action: Action): Boolean {
    val intent = intentFor(context, action) ?: return false
    return try { context.startActivity(intent); true } catch (_: ActivityNotFoundException) { false } catch (_: SecurityException) { false }
  }

  // ---- Clock alarms (background channel, no UI pop-up) ----
  // Calendar asks Clock to keep one alarm per event. The receiver in Clock is exported but guarded by
  // a signature-level permission, so only apps signed with the same key (the Pepe family) can use it.

  const val CLOCK_PERMISSION = "xyz.pepe.glacierclient.permission.CLOCK_COMMANDS"
  const val ACTION_CLOCK_SET_ALARM = "xyz.pepe.glacierclient.clock.SET_ALARM"
  const val ACTION_CLOCK_CANCEL_ALARM = "xyz.pepe.glacierclient.clock.CANCEL_ALARM"
  /** Long: exact instant (epoch millis) the alarm should ring. */
  const val EXTRA_ALARM_AT = "at"
  const val EXTRA_ALARM_LABEL = "label"
  /** String: caller-chosen id; Clock keeps at most one alarm per key (setting again replaces it). */
  const val EXTRA_ALARM_KEY = "key"

  /** True if Pepe's Clock is installed and has a receiver for [ACTION_CLOCK_SET_ALARM] (i.e. a build that supports it). */
  fun clockAlarmsAvailable(context: Context): Boolean {
    if (!isInstalled(context, App.CLOCK)) return false
    val probe = Intent(ACTION_CLOCK_SET_ALARM).setPackage(App.CLOCK.packageName)
    return context.packageManager.queryBroadcastReceivers(probe, 0).isNotEmpty()
  }

  /** Asks Clock to ring at [atMillis]. Returns false (caller should keep its own alarm) if Clock can't take it. */
  fun setClockAlarm(context: Context, key: String, atMillis: Long, label: String): Boolean {
    if (atMillis <= 0 || key.isBlank() || !clockAlarmsAvailable(context)) return false
    return try {
      context.sendBroadcast(
        Intent(ACTION_CLOCK_SET_ALARM).setPackage(App.CLOCK.packageName)
          .putExtra(EXTRA_ALARM_AT, atMillis).putExtra(EXTRA_ALARM_LABEL, label.take(MAX_QUERY)).putExtra(EXTRA_ALARM_KEY, key.take(MAX_QUERY)),
      )
      true
    } catch (_: SecurityException) { false }
  }

  /** Removes the alarm stored under [key] in Clock, if any. Safe to call when Clock isn't installed. */
  fun cancelClockAlarm(context: Context, key: String): Boolean {
    if (key.isBlank() || !clockAlarmsAvailable(context)) return false
    return try {
      context.sendBroadcast(Intent(ACTION_CLOCK_CANCEL_ALARM).setPackage(App.CLOCK.packageName).putExtra(EXTRA_ALARM_KEY, key.take(MAX_QUERY)))
      true
    } catch (_: SecurityException) { false }
  }

  /** Inbound helper for an Activity: the validated action carried by [intent], if any. */
  fun actionFrom(intent: Intent?): Action? =
    intent?.takeIf { it.action == Intent.ACTION_VIEW && it.data?.scheme == SCHEME }?.let { parse(it.dataString) }

}
