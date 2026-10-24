// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.editor

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.inputmethod.CursorAnchorInfo
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import dev.local.iosninekeyime.privacy.EditorPolicy
import dev.local.iosninekeyime.state.Delivery
import dev.local.iosninekeyime.state.EditorAction
import dev.local.iosninekeyime.state.Effect
import dev.local.iosninekeyime.state.HostCommitGate

enum class EditResult { ACCEPTED, DELEGATED, UNAVAILABLE, STALE, CONTEXT_LIMIT, AMBIGUOUS }

/** Owner-thread adapter; the service must pass only its current field's connection. No logging/IO. */
class HostEditorAdapter(private val onCommitAttempt: (Int) -> ((Boolean) -> Unit) = { { } },
                        private val onDeleteSelectionSettled:(Long,Int)->Unit={_,_->}) {
    private var session = 0L
    private var active = false
    private var connection: InputConnection? = null
    private var sensitive = true
    private val commits = HostCommitGate(::commitToConnection)
    private var selectionStart=-1
    private var selectionEnd=-1
    private var selectionVersion=0L
    private var structural:StructuralDelete?=null
    private var deleting=false

    fun begin(value: Long, editor: InputConnection?, policy: EditorPolicy, info:EditorInfo?=null) {
        require(value > session)
        session = value; active = false; connection = null; deleting=false
        selectionStart=info?.initialSelStart?:-1;selectionEnd=info?.initialSelEnd?:-1;selectionVersion++
        if(Build.VERSION.SDK_INT>=34)structural?.cancel()
        // Cancellation may synchronously begin a newer field through its callback.
        if(session!=value)return
        active = true
        connection = editor
        sensitive = policy.sensitive
        commits.startSession(value)
    }

    fun reconnect(expectedSession: Long, editor: InputConnection?): Boolean {
        if (!active || expectedSession != session) return false
        connection = editor; deleting=false
        selectionVersion++;selectionStart=-1;selectionEnd=-1
        if(Build.VERSION.SDK_INT>=34)structural?.cancel()
        return active && session==expectedSession && connection===editor // Never replay pending text automatically.
    }

    fun commit(effect: Effect.Commit): Delivery = commits.deliver(effect)
    fun retry(expectedSession: Long) = commits.retry(expectedSession)
    val pendingCount get() = commits.pendingCount

    private fun commitToConnection(text: String): Boolean {
        if(deleting)return false
        val editor = connection ?: return false
        val expectedSession = session
        var batchStarted = false
        var finishAttempt: ((Boolean) -> Unit)? = null
        return try {
            editor.beginBatchEdit()
            batchStarted = true
            // A local host may synchronously restart input from beginBatchEdit.
            if (!current(expectedSession, editor)) return false
            val finish = onCommitAttempt(text.length)
            finishAttempt = finish
            editor.commitText(text, 1).also {
                if(it && current(expectedSession,editor) && sensitive){
                    if(selectionStart>=0 && selectionEnd>=0){selectionStart=minOf(selectionStart,selectionEnd)+text.length;selectionEnd=selectionStart}
                    selectionVersion++
                }
                finish(it)
            }
        } catch (_:RuntimeException) {
            finishAttempt?.invoke(false)
            false
        } finally { if (batchStarted) endBatchQuietly(editor) }
    }

    fun delete(expectedSession: Long): EditResult {
        if (!active || expectedSession != session) return EditResult.STALE
        val editor = connection ?: return EditResult.UNAVAILABLE
        // Sensitive deletion is asynchronous and must use the structural path below.
        if (sensitive) return EditResult.UNAVAILABLE
        if (Build.VERSION.SDK_INT < 24) return deleteKey(editor)
        var batchStarted = false
        return try {
            editor.beginBatchEdit()
            batchStarted = true
            if (!current(expectedSession, editor)) return EditResult.STALE
            val selected = editor.getSelectedText(0)
            if (!current(expectedSession, editor)) return EditResult.STALE
            if (!selected.isNullOrEmpty()) return if (commitToConnection("")) EditResult.ACCEPTED else EditResult.UNAVAILABLE
            val before = editor.getTextBeforeCursor(4096, 0)?.toString() ?: return EditResult.UNAVAILABLE
            if (!current(expectedSession, editor)) return EditResult.STALE
            if (before.isEmpty()) return EditResult.ACCEPTED
            val iterator = android.icu.text.BreakIterator.getCharacterInstance(java.util.Locale.ROOT)
            iterator.setText(before)
            iterator.last()
            val boundary = iterator.previous()
            // If a giant cluster fills the bounded context, its start may be truncated. Fail closed.
            if (boundary < 0 || (boundary == 0 && before.length == 4096)) return EditResult.CONTEXT_LIMIT
            if (editor.deleteSurroundingText(before.length - boundary, 0)) EditResult.ACCEPTED else EditResult.UNAVAILABLE
        } catch (_:RuntimeException) { EditResult.UNAVAILABLE }
        finally { if (batchStarted) endBatchQuietly(editor) }
    }

    fun updateSelection(expectedSession:Long,start:Int,end:Int) {
        if(!active || session!=expectedSession)return
        if(start!=selectionStart || end!=selectionEnd){
            selectionVersion++;selectionStart=start;selectionEnd=end
            if(Build.VERSION.SDK_INT>=34)structural?.selection(start,end)
        }
    }

    fun ownsSelectionUpdate(expectedSession:Long,start:Int,end:Int):Boolean =
        active && session==expectedSession && Build.VERSION.SDK_INT>=34 && structural?.ownsSelection(start,end)==true

    fun updateCursorAnchor(expectedSession:Long,info:CursorAnchorInfo) {
        if(active && session==expectedSession && sensitive && Build.VERSION.SDK_INT>=34)structural?.anchor(info)
    }

    fun deleteAsync(expectedSession:Long,reply:(EditResult)->Unit) {
        if(!active || session!=expectedSession){reply(EditResult.STALE);return}
        val editor=connection
        if(editor==null || deleting){reply(EditResult.UNAVAILABLE);return}
        if(!sensitive){reply(delete(expectedSession));return}
        if(selectionStart>=0 && selectionEnd>=0 && selectionStart!=selectionEnd){
            reply(if(commitToConnection(""))EditResult.ACCEPTED else EditResult.UNAVAILABLE);return
        }
        if(Build.VERSION.SDK_INT<34){reply(EditResult.UNAVAILABLE);return}
        val version=selectionVersion
        val worker=structural?:StructuralDelete(Handler(Looper.getMainLooper())).also {structural=it}
        deleting=true
        worker.start(editor,selectionStart,selectionEnd,
            valid={current(expectedSession,editor)},selectionCurrent={selectionVersion==version},settled={caret->
                if(current(expectedSession,editor)){
                    selectionStart=caret;selectionEnd=caret;selectionVersion++
                    onDeleteSelectionSettled(expectedSession,caret)
                }
            }) { result ->
            if(current(expectedSession,editor))deleting=false
            reply(result)
        }
    }

    private fun deleteKey(editor: InputConnection): EditResult {
        // AOSP BaseInputConnection dispatches the event but deliberately returns false.
        // This is a request delegated to the host, never a synchronous success assertion.
        return try {
            editor.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
            editor.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
            EditResult.DELEGATED
        } catch (_:RuntimeException) { EditResult.UNAVAILABLE }
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
        return try { connection?.performEditorAction(value) ?: false } catch (_:RuntimeException) { false }
    }

    private fun current(expectedSession:Long, editor:InputConnection) = active && session==expectedSession && connection===editor

    private fun endBatchQuietly(editor:InputConnection) {
        // Batch cleanup failure must not turn an accepted commit into an ambiguous retry.
        try { editor.endBatchEdit() } catch (_:RuntimeException) { }
    }

    fun end(expectedSession: Long) {
        if (expectedSession != session) return
        active = false; connection = null; deleting=false
        selectionStart=-1;selectionEnd=-1;selectionVersion++
        commits.endSession(expectedSession)
        if(Build.VERSION.SDK_INT>=34)structural?.cancel() // All old-field cleanup precedes callbacks.
    }
}
