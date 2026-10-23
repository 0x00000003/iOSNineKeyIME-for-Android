// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.editor

import android.os.Build
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import dev.local.iosninekeyime.privacy.EditorPolicy
import dev.local.iosninekeyime.state.Delivery
import dev.local.iosninekeyime.state.EditorAction
import dev.local.iosninekeyime.state.Effect
import dev.local.iosninekeyime.state.HostCommitGate

enum class EditResult { ACCEPTED, DELEGATED, UNAVAILABLE, STALE, CONTEXT_LIMIT }

/** Owner-thread adapter; the service must pass only its current field's connection. No logging/IO. */
class HostEditorAdapter {
    private var session = 0L
    private var active = false
    private var connection: InputConnection? = null
    private var sensitive = true
    private val commits = HostCommitGate(::commitToConnection)

    fun begin(value: Long, editor: InputConnection?, policy: EditorPolicy) {
        require(value > session)
        session = value
        active = true
        connection = editor
        sensitive = policy.sensitive
        commits.startSession(value)
    }

    fun reconnect(expectedSession: Long, editor: InputConnection?): Boolean {
        if (!active || expectedSession != session) return false
        connection = editor
        return true // Deliberately does not replay pending text automatically.
    }

    fun commit(effect: Effect.Commit): Delivery = commits.deliver(effect)
    fun retry(expectedSession: Long) = commits.retry(expectedSession)
    val pendingCount get() = commits.pendingCount

    private fun commitToConnection(text: String): Boolean {
        val editor = connection ?: return false
        editor.beginBatchEdit()
        return try { editor.commitText(text, 1) } finally { editor.endBatchEdit() }
    }

    fun delete(expectedSession: Long): EditResult {
        if (!active || expectedSession != session) return EditResult.STALE
        val editor = connection ?: return EditResult.UNAVAILABLE
        // Sensitive fields: delegate deletion to the host; never request selected/surrounding text.
        if (sensitive || Build.VERSION.SDK_INT < 24) return deleteKey(editor)
        editor.beginBatchEdit()
        return try {
            if (!editor.getSelectedText(0).isNullOrEmpty()) return if (commitToConnection("")) EditResult.ACCEPTED else EditResult.UNAVAILABLE
            val before = editor.getTextBeforeCursor(4096, 0)?.toString() ?: return EditResult.UNAVAILABLE
            if (before.isEmpty()) return EditResult.ACCEPTED
            val iterator = android.icu.text.BreakIterator.getCharacterInstance(java.util.Locale.ROOT)
            iterator.setText(before)
            iterator.last()
            val boundary = iterator.previous()
            // If a giant cluster fills the bounded context, its start may be truncated. Fail closed.
            if (boundary < 0 || (boundary == 0 && before.length == 4096)) return EditResult.CONTEXT_LIMIT
            if (editor.deleteSurroundingText(before.length - boundary, 0)) EditResult.ACCEPTED else EditResult.UNAVAILABLE
        } finally { editor.endBatchEdit() }
    }

    private fun deleteKey(editor: InputConnection): EditResult {
        // AOSP BaseInputConnection dispatches the event but deliberately returns false.
        // This is a request delegated to the host, never a synchronous success assertion.
        editor.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
        editor.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
        return EditResult.DELEGATED
    }

    fun action(expectedSession: Long, action: EditorAction): Boolean {
        if (!active || expectedSession != session) return false
        if (action == EditorAction.NEWLINE) return commitToConnection("\n")
        val value = when (action) {
            EditorAction.DONE -> EditorInfo.IME_ACTION_DONE
            EditorAction.SEND -> EditorInfo.IME_ACTION_SEND
            EditorAction.SEARCH -> EditorInfo.IME_ACTION_SEARCH
            EditorAction.NEXT -> EditorInfo.IME_ACTION_NEXT
            EditorAction.GO -> EditorInfo.IME_ACTION_GO
            EditorAction.NEWLINE -> error("E_ACTION")
        }
        return connection?.performEditorAction(value) ?: false
    }

    fun end(expectedSession: Long) {
        if (expectedSession != session) return
        commits.endSession(expectedSession)
        active = false
        connection = null
    }
}
