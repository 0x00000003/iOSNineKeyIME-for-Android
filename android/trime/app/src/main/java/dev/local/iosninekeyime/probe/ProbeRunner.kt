// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.probe

import android.content.Context
import com.osfans.trime.core.Rime
import java.io.File
import java.util.UUID
import org.json.JSONObject

/** All calls are on one worker. Uses only baked-in synthetic input, never a host editor. */
object ProbeRunner {
    @Synchronized
    fun run(context: Context): JSONObject {
        val result = JSONObject().put("scope", "M1 no-model JNI compatibility; not IME/visual/privacy acceptance")
        val start = System.nanoTime()
        val shared = ProbeResources.deploy(context)
        val profile = File(context.noBackupFilesDir, "m1/profiles/${UUID.randomUUID()}")
        check(profile.mkdirs())
        var started = false
        try {
            Rime.startupRime(shared.absolutePath, profile.absolutePath, "m1-probe-1", true)
            started = true
            Rime.awaitRimeMaintenance()
            check(Rime.selectRimeSchema("wanxiang_t9")) { "E_SCHEMA" }
            var handled = true
            for (ch in "64426") handled = Rime.processRimeKey(ch.code, 0) && handled
            val candidates = Rime.getRimeCandidates(0, 20)
            val rank = candidates.indexOfFirst { it.text == "你好" } + 1
            result.put("digits_handled", handled).put("target_rank", rank)
            val luaExecuted = (0 until 100).all { Rime.getRimeProbeProperty("ime_probe_lua") == "executed" }
            result.put("lua_executed", luaExecuted).put("jni_string_regression_calls", 100)
            check(handled && rank > 0) { "E_CANDIDATE" }
            check(Rime.selectRimeCandidate(rank - 1, true)) { "E_SELECT" }
            val commit = Rime.getRimeCommit().text
            val duplicate = Rime.getRimeCommit().text
            result.put("single_commit", commit == "你好" && duplicate.isNullOrEmpty())
            result.put("full_commit_empty", Rime.getRimeContext().composition.length == 0)
            Rime.processRimeKey('6'.code, 0)
            Rime.processRimeKey(0xff08, 0)
            result.put("delete_to_empty", Rime.getRimeContext().composition.length == 0)
            Rime.clearRimeComposition()
            val all = listOf("digits_handled", "lua_executed", "single_commit", "full_commit_empty", "delete_to_empty").all { result.getBoolean(it) }
            result.put("pass", all)
        } finally {
            if (started) Rime.exitRime()
        }
        result.put("elapsed_ms", (System.nanoTime() - start) / 1_000_000)
        File(context.noBackupFilesDir, "m1/probe-result.json").writeText(result.toString(2))
        return result
    }
}
