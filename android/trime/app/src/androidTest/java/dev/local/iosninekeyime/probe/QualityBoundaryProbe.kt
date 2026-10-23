// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.probe

import android.content.Context
import android.content.res.AssetManager
import com.osfans.trime.core.Rime
import dev.local.iosninekeyime.engine.NativeEngineLease
import dev.local.iosninekeyime.state.KeyboardReducer
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/** Fixed public synthetic corpus, frozen in commit13208fe BEFORE measuring. No candidate dumps. */
object QualityBoundaryProbe {
    fun run(context:Context,fixtures:AssetManager):JSONObject=NativeEngineLease.exclusive {
        val bytes=fixtures.open("quality-v1.json").use {it.readBytes()}
        val sha=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}
        check(sha=="6d44fe6d9e9c1e8f9c40ffe535ccefd71c0943845bd1b45cde1d4f6452512566"){"E_CORPUS_CHANGED"}
        val corpus=JSONObject(String(bytes,Charsets.UTF_8))
        check(corpus.getJSONArray("words").length()==100 && corpus.getJSONArray("sentences").length()==20){"E_CORPUS_COUNT"}
        val shared=ProbeResources.deploy(context);ProbeModelResources.deploy(context,shared)
        val sharedBefore=ProbeFileSnapshot.fingerprint(shared)
        val profile=File(context.noBackupFilesDir,"m3/quality/${UUID.randomUUID()}")
        check(profile.mkdirs())
        val start=System.nanoTime()
        val result=JSONObject().put("scope","R08 fixed synthetic corpus, clean no-personalization JNI session per fixture; NOT full IME or release performance")
            .put("corpus_sha256",sha).put("corpus_frozen_commit","13208fe").put("candidate_limit",100)
        var started=false
        var historyZero=true
        var noAutoCommit=true
        var handled=true
        val groups=mapOf("words" to 95,"sentences" to 16)
        val counts=mutableMapOf<String,Int>()
        try {
            Rime.startupRime(shared.absolutePath,profile.absolutePath,"m3-quality",true);started=true
            Rime.awaitRimeMaintenance()
            check(Rime.selectRimeSchema("wanxiang_t9_nolearn")){"E_SCHEMA"}
            result.put("compact_grammar_loaded",Rime.getRimeProbeCompactGrammarLoaded())
            // Baseline after initial setup; subsequent same-schema sessions must not reselect/save user.yaml.
            val baseline=ProbeFileSnapshot.fingerprint(profile)
            for((group,_) in groups) {
                val cases=corpus.getJSONArray(group);val ranks=JSONArray();var top1=0;var top5=0
                for(i in 0 until cases.length()) {
                    val row=cases.getJSONObject(i)
                    Rime.resetRimeSession();check(Rime.getCurrentRimeSchema()=="wanxiang_t9_nolearn"){"E_DEFAULT_SCHEMA"}
                    historyZero=historyZero && Rime.getRimeProbeHistorySize()==0
                    val digits=KeyboardReducer.encode(row.getString("pinyin"))
                    for(ch in digits)handled=Rime.processRimeKey(ch.code,0) && handled
                    val rank=Rime.getRimeCandidates(0,100).indexOfFirst {it.text==row.getString("expected")}.let {if(it<0)-1 else it+1}
                    noAutoCommit=noAutoCommit && Rime.getRimeCommit().text.isNullOrEmpty()
                    if(rank==1)top1++
                    if(rank in 1..5)top5++
                    ranks.put(JSONObject().put("id",row.getString("id")).put("rank",rank))
                }
                counts[group]=top5
                result.put(group,JSONObject().put("count",cases.length()).put("top1",top1).put("top5",top5).put("ranks",ranks))
            }
            Rime.exitRime();started=false
            result.put("no_learning_profile_bytes_and_timestamps_unchanged",baseline==ProbeFileSnapshot.fingerprint(profile))
                .put("no_userdb",profile.walkTopDown().none {it.name.contains(".userdb")})
        } finally {if(started)Rime.exitRime()}
        result.put("history_zero_each_fixture",historyZero).put("no_automatic_host_commit",noAutoCommit).put("all_digit_keys_handled",handled)
            .put("static_resources_unchanged",sharedBefore==ProbeFileSnapshot.fingerprint(shared))
            .put("elapsed_ms",(System.nanoTime()-start)/1_000_000)
        val controls=listOf("compact_grammar_loaded","no_learning_profile_bytes_and_timestamps_unchanged","no_userdb","history_zero_each_fixture","no_automatic_host_commit","all_digit_keys_handled","static_resources_unchanged")
        result.put("controls_pass",controls.all {result.getBoolean(it)})
        result.put("pass",groups.all {(group,min)->counts.getValue(group)>=min} && result.getBoolean("controls_pass"))
        result
    }
}
