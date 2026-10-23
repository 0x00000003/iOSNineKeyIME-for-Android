// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.editor

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.widget.EditText

/** Debug-only synthetic host. Never opens the user's current keyboard. */
class EditorHarnessActivity : Activity() {
    lateinit var editor: EditText
        private set
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        editor = EditText(this).apply {
            showSoftInputOnFocus = false
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                importantForAutofill = android.view.View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            }
        }
        setContentView(editor)
        editor.requestFocus()
    }
}
