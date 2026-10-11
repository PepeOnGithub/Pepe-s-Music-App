package xyz.pepe.glacierclient.ecosystem

/** Width of a dashboard card: FULL spans the row, HALF shares a row with a neighbour (like a 2x1 vs 1x1 tile). */
enum class CardSize { FULL, HALF }

/** One card in a [DashboardLayout]. [id] is the app's stable name for the card (letters, digits, `_ . -` only). */
data class CardSlot(val id: String, val size: CardSize = CardSize.FULL, val hidden: Boolean = false)

/**
 * The user-editable arrangement of an app's dashboard cards (order, width, hidden). Pure Kotlin so it is
 * unit-testable and behaves the same in every Pepe app. Instances are immutable: every edit returns a new layout.
 *
 * Typical use: `DashboardLayout.reconcile(DashboardLayout.decode(saved), defaults)` on load, `encode()` on save,
 * `rows()` to render, and `moveBy` / `resize` / `setHidden` from the edit-mode UI.
 */
class DashboardLayout private constructor(val slots: List<CardSlot>) {

  val visible: List<CardSlot> get() = slots.filter { !it.hidden }
  val hiddenSlots: List<CardSlot> get() = slots.filter { it.hidden }

  fun slot(id: String): CardSlot? = slots.firstOrNull { it.id == id }

  /** Moves a visible card by [delta] positions among the visible cards (hidden ones keep their place). */
  fun moveBy(id: String, delta: Int): DashboardLayout {
    val vis = visible
    val from = vis.indexOfFirst { it.id == id }
    if (from < 0 || delta == 0) return this
    val to = (from + delta).coerceIn(0, vis.lastIndex)
    if (to == from) return this
    val target = vis[to].id
    val list = slots.toMutableList()
    val moving = list.removeAt(list.indexOfFirst { it.id == id })
    val at = list.indexOfFirst { it.id == target }
    list.add(if (delta > 0) at + 1 else at, moving)
    return DashboardLayout(list)
  }

  /** Moves a visible card to [visibleIndex] (clamped) among the visible cards. */
  fun moveTo(id: String, visibleIndex: Int): DashboardLayout {
    val from = visible.indexOfFirst { it.id == id }
    return if (from < 0) this else moveBy(id, visibleIndex.coerceIn(0, visible.lastIndex) - from)
  }

  fun resize(id: String, size: CardSize): DashboardLayout = update(id) { it.copy(size = size) }

  fun toggleSize(id: String): DashboardLayout =
    update(id) { it.copy(size = if (it.size == CardSize.FULL) CardSize.HALF else CardSize.FULL) }

  fun setHidden(id: String, hidden: Boolean): DashboardLayout = update(id) { it.copy(hidden = hidden) }

  private fun update(id: String, f: (CardSlot) -> CardSlot): DashboardLayout {
    val i = slots.indexOfFirst { it.id == id }
    if (i < 0) return this
    val next = f(slots[i])
    if (next == slots[i]) return this
    return DashboardLayout(slots.toMutableList().also { it[i] = next })
  }

  /**
   * Packs the visible cards into rows. Two consecutive HALF cards share a row; a FULL card always has its own
   * row; a HALF card with no HALF neighbour sits alone in its row (the UI may render it half-width or full-width).
   */
  fun rows(): List<List<CardSlot>> {
    val out = ArrayList<List<CardSlot>>()
    var pending: CardSlot? = null
    for (s in visible) {
      if (s.size == CardSize.HALF) {
        val p = pending
        if (p == null) pending = s else { out += listOf(p, s); pending = null }
      } else {
        pending?.let { out += listOf(it) }
        pending = null
        out += listOf(s)
      }
    }
    pending?.let { out += listOf(it) }
    return out
  }

  /** Compact text form for SharedPreferences: `id:F:0,id2:H:1` (size F/H, hidden 0/1). */
  fun encode(): String = slots.joinToString(",") { "${it.id}:${if (it.size == CardSize.FULL) 'F' else 'H'}:${if (it.hidden) 1 else 0}" }

  override fun equals(other: Any?) = other is DashboardLayout && other.slots == slots
  override fun hashCode() = slots.hashCode()
  override fun toString() = "DashboardLayout(${encode()})"

  companion object {
    private const val MAX_RAW = 4096
    private val ID = Regex("[A-Za-z0-9_.-]{1,64}")

    /** The layout where every card is visible in the app's default order, with the app's default sizes. */
    fun of(defaults: List<CardSlot>): DashboardLayout = DashboardLayout(defaults.distinctBy { it.id }.filter { ID.matches(it.id) })

    /** Parses [encode]'s output. Returns null for null/blank/oversized/malformed input; never throws. */
    fun decode(raw: String?): DashboardLayout? {
      if (raw.isNullOrBlank() || raw.length > MAX_RAW) return null
      val seen = HashSet<String>()
      val list = ArrayList<CardSlot>()
      for (part in raw.split(',')) {
        val f = part.split(':')
        if (f.size != 3 || !ID.matches(f[0]) || f[1] !in setOf("F", "H") || f[2] !in setOf("0", "1")) return null
        if (seen.add(f[0])) list += CardSlot(f[0], if (f[1] == "F") CardSize.FULL else CardSize.HALF, f[2] == "1")
      }
      return DashboardLayout(list)
    }

    /**
     * Merges a saved layout with the cards the app has NOW: saved cards the app no longer has are dropped, the
     * saved order/size/hidden of the rest is kept, and cards that are new since the layout was saved are appended
     * with their default size (so an app update that adds a card never loses the user's arrangement).
     */
    fun reconcile(saved: DashboardLayout?, defaults: List<CardSlot>): DashboardLayout {
      val base = of(defaults)
      if (saved == null) return base
      val known = base.slots.associateBy { it.id }
      val kept = saved.slots.filter { it.id in known }
      val keptIds = kept.map { it.id }.toSet()
      return DashboardLayout(kept + base.slots.filter { it.id !in keptIds })
    }
  }
}
