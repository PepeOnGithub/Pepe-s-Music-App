package xyz.pepe.glacierclient.musicapp.ui.home

import kotlinx.coroutines.flow.MutableStateFlow
import xyz.pepe.glacierclient.ecosystem.CardSize
import xyz.pepe.glacierclient.ecosystem.CardSlot
import xyz.pepe.glacierclient.ecosystem.DashboardLayout

/** Pure description of Home's rearrangeable sections (the greeting header is pinned and not one of them). */
object HomeSections {
  const val CATEGORIES = "categories"
  const val QUICK_PICKS = "quick_picks"
  const val SPEED_DIAL = "speed_dial"
  const val BECAUSE = "because_you_listened"
  const val TRENDING = "trending"

  /** Default order and sizes. */
  val DEFAULTS: List<CardSlot> = listOf(
    CardSlot(CATEGORIES, CardSize.FULL),
    CardSlot(QUICK_PICKS, CardSize.FULL),
    CardSlot(SPEED_DIAL, CardSize.FULL),
    CardSlot(BECAUSE, CardSize.FULL),
    CardSlot(TRENDING, CardSize.FULL),
  )

  /** Only these sections look right at half width; carousels, chips and the song list stay full. */
  private val HALF_OK = setOf(QUICK_PICKS, SPEED_DIAL)

  fun halfAllowed(id: String): Boolean = id in HALF_OK

  fun title(id: String): String = when (id) {
    CATEGORIES -> "Categories"
    QUICK_PICKS -> "Quick picks"
    SPEED_DIAL -> "Speed dial"
    BECAUSE -> "Because you listened"
    TRENDING -> "Trending / category songs"
    else -> id
  }

  /** The saved string merged with the current defaults; sizes not allowed for a section are forced to FULL. */
  fun load(raw: String?): DashboardLayout {
    var layout = DashboardLayout.reconcile(DashboardLayout.decode(raw), DEFAULTS)
    for (s in layout.slots) if (s.size == CardSize.HALF && !halfAllowed(s.id)) layout = layout.resize(s.id, CardSize.FULL)
    return layout
  }

  /** The string to store; blank means "default" so the stored value never pins an old arrangement needlessly. */
  fun save(layout: DashboardLayout): String = if (layout == load(null)) "" else layout.encode()
}

/** Lets Settings ask Home to open edit mode (Home consumes and clears it). */
object HomeEditRequests {
  val requested = MutableStateFlow(false)
}
