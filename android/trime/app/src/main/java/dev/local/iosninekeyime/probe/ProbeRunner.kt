// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.probe

import android.content.Context
import com.osfans.trime.core.Rime
import com.osfans.trime.BuildConfig
import java.io.File
import java.util.UUID
import org.json.JSONObject

/** All calls are on one worker. Uses only baked-in synthetic input, never a host editor. */
object ProbeRunner {
    @Synchronized
    fun rebuildPublicResources(context: Context) = dev.local.iosninekeyime.engine.NativeEngineLease.exclusive {
        ProbeResources.quarantineCurrent(context)
        val shared = ProbeResources.deploy(context)
        ProbeModelResources.deploy(context, shared)
    }

    @Synchronized
    fun run(context: Context): JSONObject = dev.local.iosninekeyime.engine.NativeEngineLease.exclusive { runExclusive(context) }

    private fun runExclusive(context: Context): JSONObject {
        val result = JSONObject().put("scope", "M1 pinned-resource JNI compatibility; not IME/visual/privacy/quality acceptance")
            .put("model_bundled", BuildConfig.PROBE_MODEL_ENABLED).put("model_sha256", BuildConfig.PROBE_MODEL_SHA256)
        val start = System.nanoTime()
        val shared = ProbeResources.deploy(context)
        ProbeModelResources.deploy(context, shared)
        result.put("resource_deploy_ms", (System.nanoTime() - start) / 1_000_000)
        val sharedBefore = ProbeFileSnapshot.fingerprint(shared)
        val profile = File(context.noBackupFilesDir, "m1/profiles/${UUID.randomUUID()}")
        check(profile.mkdirs())
        var started = false
        try {
            val engineStart = System.nanoTime()
            Rime.startupRime(shared.absolutePath, profile.absolutePath, "m1-probe-1", true)
            started = true
            Rime.awaitRimeMaintenance()
            check(Rime.selectRimeSchema("wanxiang_t9")) { "E_SCHEMA" }
            val grammarLoaded = BuildConfig.PROBE_MODEL_ENABLED && Rime.getRimeProbeCompactGrammarLoaded()
            result.put("compact_grammar_loaded", grammarLoaded)
            result.put("first_profile_engine_ready_ms", (System.nanoTime() - engineStart) / 1_000_000)
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
            val historyBeforeClear = Rime.getRimeProbeHistorySize()
            Rime.clearRimeComposition()
            result.put("clear_composition_retains_history", historyBeforeClear > 0 && Rime.getRimeProbeHistorySize() == historyBeforeClear)
            Rime.resetRimeSession()
            result.put("session_reset_clears_history", Rime.getRimeProbeHistorySize() == 0)
            result.put("session_reset_clears_property", Rime.getRimeProbeProperty("ime_probe_lua").isEmpty())
            result.put("session_reset_empty_raw_and_commit", Rime.getRimeRawInput().isEmpty() && Rime.getRimeCommit().text.isNullOrEmpty())
            check(Rime.selectRimeSchema("wanxiang_t9")) { "E_RESET_SCHEMA" }
            fun type(digits: String) {
                Rime.clearRimeComposition()
                for (ch in digits) check(Rime.processRimeKey(ch.code, 0)) { "E_FIXTURE_KEY" }
            }
            fun rank(text: String) = Rime.getRimeCandidates(0, 100).indexOfFirst { it.text == text } + 1
            var multi = 0
            for ((digits, expected) in listOf("64426" to "你好", "94664486" to "中国", "744543" to "世界")) {
                type(digits)
                val position = rank(expected)
                if (position > 0 && Rime.selectRimeCandidate(position - 1, true) &&
                    Rime.getRimeCommit().text == expected && Rime.getRimeCommit().text.isNullOrEmpty() &&
                    Rime.getRimeContext().composition.length == 0) multi++
            }
            result.put("multi_syllable_fixtures_passed", multi)
            type("64426744543")
            val prefix = Rime.getRimeCandidates(0, 500).indexOfFirst { it.text == "你好" }
            val partial = prefix >= 0 && Rime.selectRimeCandidate(prefix, true) &&
                Rime.getRimeContext().composition.length > 0 && Rime.getRimeCandidates(0, 20).isNotEmpty()
            val partialCommit = Rime.getRimeCommit().text.orEmpty()
            val drained = partial && Rime.commitRimeComposition()
            val lastCommit = Rime.getRimeCommit().text.orEmpty()
            result.put("partial_selection_preserves_composition", partial)
            result.put("partial_drain_single_exact_commit", drained && partialCommit + lastCommit == "你好世界" &&
                Rime.getRimeContext().composition.length == 0 && Rime.getRimeCommit().text.isNullOrEmpty())
            // Compare learning under the same empty field-local history, independently of grammar.
            Rime.resetRimeSession()
            check(Rime.selectRimeSchema("wanxiang_t9")) { "E_LEARNING_BASELINE_SCHEMA" }
            val baselineHistory = Rime.getRimeProbeHistorySize()
            type("64426")
            val learned = Rime.getRimeCandidates(0, 20).getOrNull(1)?.text ?: error("E_LEARNING_FIXTURE")
            val before = rank(learned)
            repeat(20) {
                type("64426")
                val current = rank(learned)
                check(current > 0 && Rime.selectRimeCandidate(current - 1, true)) { "E_LEARNING_SELECT" }
                check(Rime.getRimeCommit().text == learned && Rime.getRimeCommit().text.isNullOrEmpty()) { "E_LEARNING_COMMIT" }
                Rime.processRimeKey(0xffe1, 0) // End the pending normal learning transaction.
            }
            type("64426")
            val contextualAfter = rank(learned)
            Rime.resetRimeSession()
            check(Rime.selectRimeSchema("wanxiang_t9")) { "E_LEARNING_AFTER_SCHEMA" }
            val afterHistory = Rime.getRimeProbeHistorySize()
            type("64426")
            val after = rank(learned)
            Rime.clearRimeComposition()
            Rime.exitRime()
            started = false
            Rime.startupRime(shared.absolutePath, profile.absolutePath, "m1-probe-1", true)
            started = true
            Rime.awaitRimeMaintenance()
            check(Rime.selectRimeSchema("wanxiang_t9")) { "E_RESTART_SCHEMA" }
            type("64426")
            val restarted = rank(learned)
            val database = profile.listFiles().orEmpty().any { it.name.endsWith(".userdb") }
            result.put("learning_repetitions", 20).put("learning_before_rank", before)
                .put("learning_rank_control", "fresh native session before/after, no prior field text")
                .put("learning_baseline_history_count", baselineHistory).put("learning_after_history_count", afterHistory)
                .put("learning_contextual_after_rank", contextualAfter)
                .put("learning_after_rank", after).put("restart_rank", restarted).put("user_database_created", database)
            result.put("normal_learning_probe", database && before > 1 && after > 0 && after < before && restarted == after && baselineHistory == 0 && afterHistory == 0)
            Rime.clearRimeComposition()
            Rime.exitRime()
            started = false
            val noLearningProfile = File(context.noBackupFilesDir, "m1/profiles/${UUID.randomUUID()}")
            check(noLearningProfile.mkdirs())
            Rime.startupRime(shared.absolutePath, noLearningProfile.absolutePath, "m1-probe-2", true)
            started = true
            Rime.awaitRimeMaintenance()
            check(Rime.selectRimeSchema("wanxiang_t9_nolearn")) { "E_NO_LEARNING_SCHEMA" }
            val noUserDatabaseAtStart = noLearningProfile.walkTopDown().none { it.name.contains(".userdb") }
            val filesBefore = ProbeFileSnapshot.fingerprint(noLearningProfile)
            type("64426")
            val noLearningTarget = Rime.getRimeCandidates(0, 20).getOrNull(1)?.text ?: error("E_NO_LEARNING_FIXTURE")
            val noLearningBefore = rank(noLearningTarget)
            repeat(20) {
                type("64426")
                val current = rank(noLearningTarget)
                check(current > 0 && Rime.selectRimeCandidate(current - 1, true)) { "E_NO_LEARNING_SELECT" }
                check(Rime.getRimeCommit().text == noLearningTarget && Rime.getRimeCommit().text.isNullOrEmpty()) { "E_NO_LEARNING_COMMIT" }
                Rime.processRimeKey(0xffe1, 0)
            }
            type("64426")
            val noLearningAfter = rank(noLearningTarget)
            Rime.clearRimeComposition()
            Rime.exitRime()
            started = false
            val filesUnchanged = filesBefore == ProbeFileSnapshot.fingerprint(noLearningProfile)
            val noUserDatabase = noLearningProfile.walkTopDown().none { it.name.contains(".userdb") }
            result.put("no_learning_before_rank", noLearningBefore).put("no_learning_after_rank", noLearningAfter)
                .put("no_learning_userdb_absent_at_start", noUserDatabaseAtStart)
                .put("no_learning_profile_bytes_and_timestamps_unchanged", filesUnchanged).put("no_learning_userdb_absent", noUserDatabase)
                .put("no_learning_probe", noLearningBefore > 1 && noLearningBefore == noLearningAfter && filesUnchanged && noUserDatabase && noUserDatabaseAtStart)
            val prebuiltShared = File(shared, "build/wanxiang_lite.table.bin").isFile &&
                !File(profile, "build/wanxiang_lite.table.bin").exists() && !File(noLearningProfile, "build/wanxiang_lite.table.bin").exists()
            val sharedUnchanged = sharedBefore == ProbeFileSnapshot.fingerprint(shared)
            result.put("prebuilt_main_table_shared", prebuiltShared).put("static_resources_bytes_and_timestamps_unchanged", sharedUnchanged)
            val all = multi == 3 && (!BuildConfig.PROBE_MODEL_ENABLED || grammarLoaded) && listOf("digits_handled", "lua_executed", "single_commit", "full_commit_empty", "delete_to_empty",
                "partial_selection_preserves_composition", "partial_drain_single_exact_commit", "normal_learning_probe", "no_learning_probe",
                "prebuilt_main_table_shared", "static_resources_bytes_and_timestamps_unchanged", "clear_composition_retains_history",
                "session_reset_clears_history", "session_reset_clears_property", "session_reset_empty_raw_and_commit").all { result.getBoolean(it) }
            result.put("pass", all)
        } finally {
            if (started) Rime.exitRime()
        }
        result.put("elapsed_ms", (System.nanoTime() - start) / 1_000_000)
        File(context.noBackupFilesDir, "m1/probe-result.json").writeText(result.toString(2))
        return result
    }
}
