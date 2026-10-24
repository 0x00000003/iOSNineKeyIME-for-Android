// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.probe

import android.app.Instrumentation
import android.content.Intent
import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import dev.local.iosninekeyime.editor.EditorHarnessActivity
import dev.local.iosninekeyime.editor.HostEditorAdapter
import dev.local.iosninekeyime.editor.EditResult
import dev.local.iosninekeyime.privacy.EditorPolicy
import dev.local.iosninekeyime.state.Delivery
import dev.local.iosninekeyime.state.Effect
import dev.local.iosninekeyime.state.Stamp
import java.io.File
import java.security.MessageDigest
import org.json.JSONObject

/** Real Android EditText/InputConnection, isolated debug host, fixed synthetic text only. */
object EditorBoundaryProbe {
    @androidx.annotation.RequiresApi(34)
    fun run(instrumentation: Instrumentation): JSONObject {
        val context = instrumentation.targetContext
        val activity = instrumentation.startActivitySync(Intent(context, EditorHarnessActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as EditorHarnessActivity
        val host = HostEditorAdapter()
        lateinit var connection: InputConnection
        val normal = EditorPolicy.from(InputType.TYPE_CLASS_TEXT, 0, false)
        var session = 1L
        var unique = false
        var composingReplacement = false
        var restored = false
        var stale = false
        var deletionFixtures = 0
        var privacyReads = 0
        var structuralRequests=0
        val handler=android.os.Handler(android.os.Looper.getMainLooper())
        var sensitiveCases = 0
        var sensitiveDeletionFixtures = 0
        val metadataResults = org.json.JSONArray()
        val privateDeletionResults = org.json.JSONArray()
        fun main(block: () -> Unit) {
            var failure: Throwable? = null
            instrumentation.runOnMainSync { try { block() } catch (error: Throwable) { failure = error } }
            instrumentation.waitForIdleSync()
            failure?.let { throw it } // Fail on the instrumentation worker, not by crashing the UI thread.
        }
        fun fixture(value: String, cursor: Int = value.length) = main { activity.editor.setText(value); activity.editor.setSelection(cursor);host.updateSelection(session,cursor,cursor) }
        fun privateDelete():EditResult {
            val latch=java.util.concurrent.CountDownLatch(1);var result=EditResult.UNAVAILABLE
            main {host.deleteAsync(session){result=it;latch.countDown()}}
            check(latch.await(3,java.util.concurrent.TimeUnit.SECONDS)){"E_PRIVATE_DELETE_TIMEOUT"}
            return result
        }
        try {
            main {
                connection = checkNotNull(activity.editor.onCreateInputConnection(EditorInfo()))
                host.begin(session, connection, normal)
                val commit = Effect.Commit(Stamp(session, 1), 0, "A")
                unique = host.commit(commit) == Delivery.ACCEPTED && host.commit(commit) == Delivery.DUPLICATE && activity.editor.text.toString() == "A"
                connection.setComposingText("draft", 1)
                composingReplacement = host.commit(Effect.Commit(Stamp(session, 2), 0, "B")) == Delivery.ACCEPTED && activity.editor.text.toString() == "AB"
                host.reconnect(session, null)
                val pending = host.commit(Effect.Commit(Stamp(session, 3), 0, "C"))
                host.reconnect(session, connection)
                restored = pending == Delivery.BLOCKED && activity.editor.text.toString() == "AB" && host.retry(session) == Delivery.ACCEPTED && activity.editor.text.toString() == "ABC"
                host.reconnect(session, null)
                host.commit(Effect.Commit(Stamp(session, 4), 0, "old"))
                session++
                host.begin(session, connection, normal)
                stale = host.retry(session - 1) == Delivery.STALE && activity.editor.text.toString() == "ABC"
            }
            val clusters = listOf("\uD83D\uDE80", "e\u0301", "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66", "\uD83C\uDDE8\uD83C\uDDF3", "\uD83D\uDC4D\uD83C\uDFFD")
            for (cluster in clusters) {
                fixture("A${cluster}B", 1 + cluster.length)
                main { if (host.delete(session) == EditResult.ACCEPTED && activity.editor.text.toString() == "AB") deletionFixtures++ }
            }
            fixture("ABCDEF")
            main { activity.editor.setSelection(2, 4); if (host.delete(session) == EditResult.ACCEPTED && activity.editor.text.toString() == "ABEF") deletionFixtures++ }

            // Flush native engine before the baseline; this test never calls it with sensitive data.
            val treeBefore = treeHash(File(context.noBackupFilesDir, "m1/profiles"))
            val sensitive = listOf(
                Triple(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD, 0, false),
                Triple(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD, 0, false),
                Triple(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD, 0, false),
                Triple(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD, 0, false),
                Triple(InputType.TYPE_CLASS_TEXT, EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING, false),
                Triple(InputType.TYPE_CLASS_TEXT, 0, true),
            )
            for ((index, metadata) in sensitive.withIndex()) {
                val (type, options, userPrivate) = metadata
                main {
                    activity.editor.inputType = type
                    activity.editor.imeOptions = options
                    activity.editor.setText("12") // Valid for numeric as well as text password fields.
                    activity.editor.setSelection(2)
                    val actualInfo = EditorInfo()
                    connection = checkNotNull(activity.editor.onCreateInputConnection(actualInfo))
                    val guarded = object : InputConnectionWrapper(connection, false) {
                        override fun requestCursorUpdates(mode:Int,filter:Int):Boolean {
                            check(filter==(InputConnection.CURSOR_UPDATE_FILTER_INSERTION_MARKER or InputConnection.CURSOR_UPDATE_FILTER_EDITOR_BOUNDS)){"E_STRUCTURAL_FILTER"}
                            handler.post {host.updateCursorAnchor(session,SyntheticCursorMetadata.anchor(activity.editor))};return true
                        }
                        override fun requestTextBoundsInfo(bounds:android.graphics.RectF,executor:java.util.concurrent.Executor,consumer:java.util.function.Consumer<android.view.inputmethod.TextBoundsInfoResult>){structuralRequests++;super.requestTextBoundsInfo(bounds,executor,consumer)}
                        override fun getSelectedText(flags: Int): CharSequence? { privacyReads++; return super.getSelectedText(flags) }
                        override fun getTextBeforeCursor(n: Int, flags: Int): CharSequence? { privacyReads++; return super.getTextBeforeCursor(n, flags) }
                        override fun getTextAfterCursor(n: Int, flags: Int): CharSequence? { privacyReads++; return super.getTextAfterCursor(n, flags) }
                        override fun getExtractedText(request: android.view.inputmethod.ExtractedTextRequest?, flags: Int): android.view.inputmethod.ExtractedText? { privacyReads++; return super.getExtractedText(request, flags) }
                        override fun getSurroundingText(beforeLength: Int, afterLength: Int, flags: Int): android.view.inputmethod.SurroundingText? { privacyReads++; return super.getSurroundingText(beforeLength, afterLength, flags) }
                        override fun getCursorCapsMode(reqModes: Int): Int { privacyReads++; return super.getCursorCapsMode(reqModes) }
                    }
                    session++
                    host.begin(session, guarded, EditorPolicy.from(actualInfo.inputType, actualInfo.imeOptions, userPrivate),actualInfo)
                }
                check(privateDelete()==EditResult.ACCEPTED){"E_PRIVATE_DELETE"}
                main {
                    val passed = activity.editor.text.toString() == "1" && privacyReads == 0
                    if (passed) sensitiveCases++
                    metadataResults.put(JSONObject().put("case", listOf("text_password", "visible_password", "web_password", "number_password", "no_personalized_learning", "user_private")[index]).put("pass", passed))
                }
            }
            for ((index, cluster) in clusters.withIndex()) {
                fixture("A${cluster}B", 1 + cluster.length)
                check(privateDelete()==EditResult.ACCEPTED){"E_PRIVATE_CLUSTER_DELETE"}
                main {
                    val passed = activity.editor.text.toString() == "AB"
                    if (passed) sensitiveDeletionFixtures++
                    privateDeletionResults.put(JSONObject().put("case", listOf("astral", "combining_accent", "zwj_family", "regional_flag", "skin_modifier")[index]).put("pass", passed))
                }
            }
            val treeUnchanged = treeBefore == treeHash(File(context.noBackupFilesDir, "m1/profiles"))
            return JSONObject().put("scope", "real EditText/InputConnection component test; NOT full IME/multi-host or I05 all-path acceptance")
                .put("single_host_commit", unique).put("composing_replacement", composingReplacement)
                .put("same_session_restore", restored).put("old_field_not_replayed", stale)
                .put("grapheme_and_selection_deletion_passed", deletionFixtures).put("deletion_fixture_count", 6)
                .put("sensitive_metadata_cases_passed", sensitiveCases).put("sensitive_context_reads", privacyReads)
                .put("sensitive_structural_bounds_requests",structuralRequests).put("cursor_anchor_delivery","owned synthetic host bridge; system IMS callback NOT tested")
                .put("sensitive_metadata_results", metadataResults).put("sensitive_deletion_results", privateDeletionResults)
                .put("sensitive_grapheme_deletion_passed", sensitiveDeletionFixtures).put("sensitive_deletion_fixture_count", 5)
                .put("normal_learning_tree_unchanged", treeUnchanged)
                .put("pass", unique && composingReplacement && restored && stale && deletionFixtures == 6 && sensitiveCases == 6 && sensitiveDeletionFixtures == 5 && privacyReads == 0 && treeUnchanged)
        } finally {
            main { host.end(session); activity.finish() }
        }
    }

    private fun treeHash(root: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(65536)
        root.walkTopDown().filter { it.isFile }.sortedBy { it.relativeTo(root).path }.forEach { file ->
            digest.update(file.relativeTo(root).path.toByteArray())
            file.inputStream().use { input -> while (true) { val size = input.read(buffer); if (size < 0) break; digest.update(buffer, 0, size) } }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
