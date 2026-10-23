// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.probe

import android.content.Context
import com.osfans.trime.BuildConfig
import dev.local.iosninekeyime.engine.NativeEngineLease
import java.io.File
import java.io.RandomAccessFile
import org.json.JSONObject

/** Faults only owned public copies; a native lease protects every mutation and repair. */
object ResourceBoundaryProbe {
    fun run(context:Context):JSONObject=synchronized(ProbeRunner) {
        val owner=Any()
        check(NativeEngineLease.acquire(owner)){"E_ENGINE_BUSY"}
        val profiles=File(context.noBackupFilesDir,"m1/profiles")
        val existing=mutableMapOf<File,String>()
        var publicRejected=false;var unknownRejected=false;var symlinkRejected=false;var sentinelPreserved=false
        var modelRejected:Boolean?=null;var preserved=false
        try {
            var shared=ProbeResources.deploy(context);ProbeModelResources.deploy(context,shared)
            check(profiles.walkTopDown().any {it.name.endsWith(".userdb")}){"E_POSITIVE_LEARNING_CONTROL_REQUIRED"}
            for(profile in profiles.listFiles().orEmpty().filter {it.isDirectory})existing[profile]=ProbeFileSnapshot.fingerprint(profile)
            val before=ProbeFileSnapshot.fingerprint(profiles)
            fun publicRejects()=try{ProbeResources.deploy(context);false}catch(error:Exception){ProbeFailureCode.from(error)=="E_RESOURCE_TAMPER"}
            fun repair(){ProbeRunner.rebuildReservedResources(context,owner);shared=ProbeResources.deploy(context)}
            File(shared,"default.yaml").appendText("\n# synthetic public-resource fault\n")
            publicRejected=publicRejects();repair()
            File(shared,"lua/untrusted.lua").writeText("return function() end\n")
            unknownRejected=publicRejects();repair()
            val script=File(shared,"lua/ime_probe.lua")
            val publicBytes=script.readBytes()
            val sentinel=File.createTempFile("m4-public-", ".lua",context.cacheDir)
            sentinel.writeBytes(publicBytes)
            try {
                check(script.delete())
                android.system.Os.symlink(sentinel.absolutePath,script.absolutePath)
                symlinkRejected=publicRejects();repair()
                sentinelPreserved=sentinel.readBytes().contentEquals(publicBytes)
            }finally{check(sentinel.delete())}
            if(BuildConfig.PROBE_MODEL_ENABLED) {
                RandomAccessFile(File(shared,ProbeModelResources.NAME),"rw").use {file->
                    file.seek(64);val original=file.read();check(original>=0);file.seek(64);file.write(original xor 1)
                }
                modelRejected=try{ProbeModelResources.deploy(context,shared);false}catch(error:Exception){ProbeFailureCode.from(error)=="E_MODEL_INTEGRITY"}
                repair()
            }
            ProbeModelResources.deploy(context,shared)
            preserved=before==ProbeFileSnapshot.fingerprint(profiles)
        }finally{NativeEngineLease.release(owner)}
        val reopened=ProbeRunner.run(context).getBoolean("pass")
        val existingPreserved=existing.all {(directory,fingerprint)->fingerprint==ProbeFileSnapshot.fingerprint(directory)}
        JSONObject().put("scope","owned public-resource/model faults and restoration with native lease; NOT general privacy/backup acceptance")
            .put("public_corruption_rejected",publicRejected).put("unknown_public_lua_rejected",unknownRejected)
            .put("public_symlink_rejected",symlinkRejected).put("public_sentinel_preserved",sentinelPreserved)
            .put("model_corruption_rejected",modelRejected?:JSONObject.NULL).put("model_bundled",BuildConfig.PROBE_MODEL_ENABLED)
            .put("recovered_public_resources",true).put("profiles_bytes_and_timestamps_unchanged",preserved)
            .put("preservation_scope","all existing synthetic profiles immediately after repair and after reopening with fresh synthetic profiles")
            .put("existing_profiles_unchanged_after_engine_reopen",existingPreserved).put("engine_reopened_with_repaired_resources",reopened)
            .put("positive_existing_profile_count",existing.size)
            .put("pass",publicRejected && unknownRejected && symlinkRejected && sentinelPreserved && (!BuildConfig.PROBE_MODEL_ENABLED || modelRejected==true) && preserved && existingPreserved && reopened)
    }
}
