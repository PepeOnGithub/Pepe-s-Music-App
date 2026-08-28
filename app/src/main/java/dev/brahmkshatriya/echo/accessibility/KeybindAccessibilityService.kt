package dev.brahmkshatriya.echo.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import dev.brahmkshatriya.echo.di.App
import dev.brahmkshatriya.echo.utils.ContextUtils.getSettings
import dev.brahmkshatriya.echo.widget.ControllerHelper
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Lets a physical keyboard skip/play-pause the current track while another app has
 * input focus (e.g. while gaming). Android only delivers key events like this to a
 * background app via an AccessibilityService with FLAG_REQUEST_FILTER_KEY_EVENTS -
 * there's no lighter-weight way to intercept keys system-wide. Must be enabled
 * manually in Settings > Accessibility; some OEM skins (MIUI in particular) can
 * silently disable it again after an update or their own battery-optimization sweep.
 */
class KeybindAccessibilityService : AccessibilityService(), KoinComponent {

    private val app: App by inject()
    private val consumedKeyCodes = mutableSetOf<Int>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo?.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
        ControllerHelper.register(app, SERVICE_KEY) {}
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_UP) {
            return consumedKeyCodes.remove(event.keyCode)
        }
        if (event.action != KeyEvent.ACTION_DOWN || event.repeatCount != 0) return false

        val controller = ControllerHelper.controller ?: return false
        val prefs = applicationContext.getSettings()
        val pressed = describeKeyEvent(event)
        when (pressed) {
            prefs.getString(SKIP_KEYBIND_KEY, DEFAULT_SKIP_KEYBIND) -> controller.seekToNext()
            prefs.getString(PLAY_PAUSE_KEYBIND_KEY, DEFAULT_PLAY_PAUSE_KEYBIND) -> controller.run {
                prepare()
                playWhenReady = !playWhenReady
            }

            else -> return false
        }
        consumedKeyCodes.add(event.keyCode)
        return true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    override fun onDestroy() {
        super.onDestroy()
        ControllerHelper.unregister(SERVICE_KEY)
    }

    companion object {
        private const val SERVICE_KEY = "keybind_accessibility_service"
        const val SKIP_KEYBIND_KEY = "keybind_skip"
        const val PLAY_PAUSE_KEYBIND_KEY = "keybind_play_pause"
        const val DEFAULT_SKIP_KEYBIND = "CTRL+S"
        const val DEFAULT_PLAY_PAUSE_KEYBIND = "CTRL+P"

        private val MODIFIER_KEY_CODES = setOf(
            KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT,
            KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT,
            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT,
            KeyEvent.KEYCODE_META_LEFT, KeyEvent.KEYCODE_META_RIGHT
        )

        fun isModifierOnly(keyCode: Int) = keyCode in MODIFIER_KEY_CODES

        fun describeKeyEvent(event: KeyEvent): String = buildString {
            if (event.isCtrlPressed) append("CTRL+")
            if (event.isAltPressed) append("ALT+")
            if (event.isShiftPressed) append("SHIFT+")
            if (event.isMetaPressed) append("META+")
            append(KeyEvent.keyCodeToString(event.keyCode).removePrefix("KEYCODE_"))
        }
    }
}
