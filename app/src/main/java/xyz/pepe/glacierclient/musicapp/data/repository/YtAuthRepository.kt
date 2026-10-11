package xyz.pepe.glacierclient.musicapp.data.repository

import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.json.JSONArray
import org.json.JSONObject

data class YtAccount(val id: String, val label: String, val cookie: String, val photoUrl: String? = null)

/**
 * Holds one or more YouTube Music session cookies, each captured after the user logs in through
 * Google's own login page (see `YtLoginPage`) — the app never sees a password, only the resulting
 * session cookie, the same thing a browser holds after sign-in. Supports multiple accounts (e.g.
 * a personal and a family/premium account) with one marked active at a time; every
 * [xyz.pepe.glacierclient.musicapp.data.remote.YtMusicClient] request uses whichever is active.
 */
class YtAuthRepository(private val prefs: SharedPreferences) {
  private val scope = CoroutineScope(Dispatchers.Main.immediate)

  private val _accounts = MutableStateFlow(load())
  val accounts: StateFlow<List<YtAccount>> = _accounts

  private val _activeId = MutableStateFlow(prefs.getString(KEY_ACTIVE, null) ?: _accounts.value.firstOrNull()?.id)

  val activeAccount: StateFlow<YtAccount?> = combine(_accounts, _activeId) { accounts, activeId ->
    accounts.firstOrNull { it.id == activeId } ?: accounts.firstOrNull()
  }.stateIn(scope, SharingStarted.Eagerly, _accounts.value.firstOrNull { it.id == _activeId.value } ?: _accounts.value.firstOrNull())

  /** The active account's cookie — what [xyz.pepe.glacierclient.musicapp.data.remote.YtMusicClient] actually sends. */
  val cookie: StateFlow<String?> = activeAccount
    .map { it?.cookie }
    .stateIn(scope, SharingStarted.Eagerly, activeAccount.value?.cookie)

  /** Adds a newly logged-in account and makes it active. [label] defaults to "Account N". */
  fun addAccount(rawCookie: String, label: String? = null): YtAccount {
    val id = System.currentTimeMillis().toString()
    val account = YtAccount(id, label ?: "Account ${_accounts.value.size + 1}", rawCookie)
    val next = _accounts.value + account
    _accounts.value = next
    _activeId.value = id
    save(next, id)
    return account
  }

  /** Stores the real name/avatar fetched from YouTube Music for [id]. */
  fun updateProfile(id: String, name: String, photoUrl: String?) {
    val next = _accounts.value.map { if (it.id == id) it.copy(label = name, photoUrl = photoUrl) else it }
    _accounts.value = next
    save(next, _activeId.value)
  }

  fun switchTo(id: String) {
    if (_accounts.value.none { it.id == id }) return
    _activeId.value = id
    save(_accounts.value, id)
  }

  fun removeAccount(id: String) {
    val next = _accounts.value.filterNot { it.id == id }
    _accounts.value = next
    val newActive = if (_activeId.value == id) next.firstOrNull()?.id else _activeId.value
    _activeId.value = newActive
    save(next, newActive)
  }

  private fun load(): List<YtAccount> {
    val json = prefs.getString(KEY_ACCOUNTS, null) ?: return emptyList()
    return runCatching {
      val arr = JSONArray(json)
      (0 until arr.length()).mapNotNull { i ->
        val o = arr.optJSONObject(i) ?: return@mapNotNull null
        YtAccount(o.optString("id"), o.optString("label"), o.optString("cookie"), o.optString("photo").ifEmpty { null })
      }
    }.getOrDefault(emptyList())
  }

  private fun save(accounts: List<YtAccount>, activeId: String?) {
    val arr = JSONArray()
    accounts.forEach { a -> arr.put(JSONObject().put("id", a.id).put("label", a.label).put("cookie", a.cookie).put("photo", a.photoUrl ?: "")) }
    prefs.edit().putString(KEY_ACCOUNTS, arr.toString()).putString(KEY_ACTIVE, activeId).apply()
  }

  private companion object {
    const val KEY_ACCOUNTS = "yt_accounts_json"
    const val KEY_ACTIVE = "yt_active_account_id"
  }
}
