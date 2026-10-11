package xyz.pepe.glacierclient.musicapp.data.repository

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

data class AppNotification(val id: Long, val title: String, val body: String, val atMs: Long, val read: Boolean)

/**
 * In-app notification inbox (the bell on Home). Only real events the app itself produces are
 * posted here — playback failures, account sign-ins, playlist changes — never placeholder content.
 */
class NotificationsRepository(private val prefs: SharedPreferences) {
  private val _items = MutableStateFlow(load())
  val items: StateFlow<List<AppNotification>> = _items

  fun post(title: String, body: String) {
    val now = System.currentTimeMillis()
    val next = (listOf(AppNotification(now, title, body, now, read = false)) + _items.value).take(MAX)
    _items.value = next
    save(next)
  }

  fun markAllRead() {
    if (_items.value.none { !it.read }) return
    val next = _items.value.map { it.copy(read = true) }
    _items.value = next
    save(next)
  }

  fun clear() { _items.value = emptyList(); save(emptyList()) }

  private fun load(): List<AppNotification> = runCatching {
    val arr = JSONArray(prefs.getString(KEY, "[]"))
    List(arr.length()) { i ->
      val o = arr.getJSONObject(i)
      AppNotification(o.getLong("id"), o.getString("title"), o.getString("body"), o.getLong("at"), o.optBoolean("read"))
    }
  }.getOrDefault(emptyList())

  private fun save(list: List<AppNotification>) {
    val arr = JSONArray()
    list.forEach { arr.put(JSONObject().put("id", it.id).put("title", it.title).put("body", it.body).put("at", it.atMs).put("read", it.read)) }
    prefs.edit().putString(KEY, arr.toString()).apply()
  }

  private companion object { const val KEY = "items"; const val MAX = 50 }
}
