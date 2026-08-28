package dev.brahmkshatriya.echo.utils.ui.prefs

import android.content.Context
import android.view.KeyEvent
import android.widget.EditText
import androidx.preference.Preference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.accessibility.KeybindAccessibilityService.Companion.describeKeyEvent
import dev.brahmkshatriya.echo.accessibility.KeybindAccessibilityService.Companion.isModifierOnly

class KeybindPreference(context: Context) : Preference(context) {

    fun currentValue(defaultKeybind: String): String = getPersistedString(defaultKeybind)

    fun showRecorderDialog(defaultKeybind: String) {
        var recorded: String? = null
        val input = EditText(context).apply {
            isFocusableInTouchMode = true
            isCursorVisible = false
            isLongClickable = false
            setText(getPersistedString(defaultKeybind))
            hint = context.getString(R.string.keybind_press_a_key)
            setOnKeyListener { _, _, event ->
                if (event.action == KeyEvent.ACTION_DOWN && !isModifierOnly(event.keyCode)) {
                    recorded = describeKeyEvent(event)
                    setText(recorded)
                }
                true
            }
        }
        val padding = (context.resources.displayMetrics.density * 24).toInt()
        input.setPadding(padding, padding / 2, padding, padding / 2)

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setView(input)
            .setPositiveButton(R.string.save) { _, _ ->
                (recorded ?: getPersistedString(defaultKeybind)).let {
                    persistString(it)
                    summary = it
                }
            }
            .setNeutralButton(R.string.reset) { _, _ ->
                persistString(defaultKeybind)
                summary = defaultKeybind
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
        dialog.setOnShowListener { input.requestFocus() }
        dialog.show()
    }
}
