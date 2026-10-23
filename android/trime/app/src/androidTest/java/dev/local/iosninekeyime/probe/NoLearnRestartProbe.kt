// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.probe

import android.content.Context
import com.osfans.trime.core.Rime
import dev.local.iosninekeyime.engine.NativeEngineLease
import java.io.File
import java.util.UUID
import org.json.JSONObject

/** Compatibility investigation ONLY. Does not enable private Chinese in the product. */
object NoLearnRestartProbe {
    fun run(context:Context):JSONObject=NativeEngineLease.exclusive {
        val shared=ProbeResources.deploy(context);ProbeModelResources.deploy(context,shared)
        val staticBefore=ProbeFileSnapshot.fingerprint(shared)
        val profile=File(context.noBackupFilesDir,"m4/nolearn-restart/${UUID.randomUUID()}");check(profile.mkdirs())
        var started=false
        fun start(full:Boolean) {Rime.startupRime(shared.absolutePath,profile.absolutePath,"m4-nolearn-restart",full);started=true;Rime.awaitRimeMaintenance()}
        fun stop(){if(started){Rime.exitRime();started=false}}
        fun exactCommit():Boolean {
            for(ch in "64426")check(Rime.processRimeKey(ch.code,0)){"E_SYNTHETIC_KEY"}
            val expected=Rime.getRimeCandidates(0,1).first().text
            check(Rime.selectRimeCandidate(0,true)){"E_SELECT"}
            return Rime.getRimeCommit().text==expected && Rime.getRimeCommit().text.isNullOrEmpty() && Rime.getRimeContext().composition.length==0
        }
        var exact=true
        try {
            // Explicitly outside the private-measurement baseline: public/schema first-time preparation.
            start(true);check(Rime.getCurrentRimeSchema()=="wanxiang_t9_nolearn"){"E_DEFAULT_NOLEARN"}
            repeat(3){exact=exactCommit()&&exact};stop()
            android.os.SystemClock.sleep(1100) // Make metadata time changes observable, not same-second coincidence.
            val before=ProbeFileSnapshot.fingerprint(profile)
            start(false)
            val startupUnchanged=before==ProbeFileSnapshot.fingerprint(profile)
            val defaultNoLearn=Rime.getCurrentRimeSchema()=="wanxiang_t9_nolearn"
            val historyFresh=Rime.getRimeProbeHistorySize()==0
            repeat(20){exact=exactCommit()&&exact}
            stop()
            val unchanged=before==ProbeFileSnapshot.fingerprint(profile)
            val noUserDb=profile.walkTopDown().none{it.name.contains("userdb")}
            val staticUnchanged=staticBefore==ProbeFileSnapshot.fingerprint(shared)
            JSONObject().put("scope","preprepared no-learning profile reopen/input/close investigation ONLY; initial preparation excluded and no host/IME privacy acceptance")
                .put("initial_preparation_outside_baseline",true).put("metadata_clock_separation_ms",1100)
                .put("reopen_maintenance_full_check",false).put("profile_unchanged_immediately_after_restart",startupUnchanged)
                .put("default_no_learning_schema",defaultNoLearn).put("initial_history_empty",historyFresh)
                .put("twenty_commits_exact_once",exact).put("profile_bytes_mtimes_unchanged_after_restart_input_close",unchanged)
                .put("no_userdb",noUserDb).put("shared_bytes_mtimes_unchanged",staticUnchanged)
                .put("pass",startupUnchanged&&defaultNoLearn&&historyFresh&&exact&&unchanged&&noUserDb&&staticUnchanged)
        }finally{stop()}
    }
}
