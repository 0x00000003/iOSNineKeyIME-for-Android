// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.probe

import android.content.Context
import com.osfans.trime.BuildConfig
import java.io.File
import java.io.RandomAccessFile
import org.json.JSONObject

/** Fault injection ONLY into this test APK's owned public resource copy, never learned/user files. */
object ResourceBoundaryProbe {
    fun run(context: Context): JSONObject = synchronized(ProbeRunner) {
        val shared = ProbeResources.deploy(context)
        ProbeModelResources.deploy(context, shared)
        val profiles = File(context.noBackupFilesDir, "m1/profiles")
        check(profiles.walkTopDown().any { it.name.endsWith(".userdb") }) { "E_POSITIVE_LEARNING_CONTROL_REQUIRED" }
        val existing = profiles.listFiles().orEmpty().filter { it.isDirectory }.associateWith { ProbeFileSnapshot.fingerprint(it) }
        val before = ProbeFileSnapshot.fingerprint(profiles)
        File(shared, "default.yaml").appendText("\n# synthetic public-resource fault\n")
        val publicRejected = try { ProbeResources.deploy(context); false } catch (error: Exception) {
            ProbeFailureCode.from(error) == "E_RESOURCE_TAMPER"
        }
        ProbeRunner.rebuildPublicResources(context)
        var modelRejected: Boolean? = null
        if (BuildConfig.PROBE_MODEL_ENABLED) {
            val current = ProbeResources.deploy(context)
            RandomAccessFile(File(current, ProbeModelResources.NAME), "rw").use { file ->
                file.seek(64); val original = file.read(); check(original >= 0); file.seek(64); file.write(original xor 1)
            }
            modelRejected = try { ProbeModelResources.deploy(context, current); false } catch (error: Exception) {
                ProbeFailureCode.from(error) == "E_MODEL_INTEGRITY"
            }
            ProbeRunner.rebuildPublicResources(context)
        }
        val restored = ProbeResources.deploy(context)
        ProbeModelResources.deploy(context, restored)
        val preserved = before == ProbeFileSnapshot.fingerprint(profiles)
        val reopened = ProbeRunner.run(context).getBoolean("pass")
        val existingPreserved = existing.all { (directory, fingerprint) -> fingerprint == ProbeFileSnapshot.fingerprint(directory) }
        JSONObject().put("scope", "debug-owned public-resource/model corruption and recovery; NOT general privacy/backup acceptance")
            .put("public_corruption_rejected", publicRejected).put("model_corruption_rejected", modelRejected ?: JSONObject.NULL)
            .put("model_bundled", BuildConfig.PROBE_MODEL_ENABLED).put("recovered_public_resources", true)
            .put("profiles_bytes_and_timestamps_unchanged", preserved)
            .put("preservation_scope", "whole profile tree immediately after repair; then separately reopen engine with fresh synthetic profiles")
            .put("existing_profiles_unchanged_after_engine_reopen", existingPreserved).put("engine_reopened_with_repaired_resources", reopened)
            .put("positive_existing_profile_count", existing.size)
            .put("pass", publicRejected && (!BuildConfig.PROBE_MODEL_ENABLED || modelRejected == true) && preserved && existingPreserved && reopened)
    }
}
