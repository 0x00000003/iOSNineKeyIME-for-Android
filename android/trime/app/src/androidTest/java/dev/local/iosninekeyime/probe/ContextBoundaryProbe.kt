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

/** True prefix candidate commit, four-cell controls. No host reads or user dictionary learning. */
object ContextBoundaryProbe {
    fun run(context:Context,fixtures:AssetManager):JSONObject=NativeEngineLease.exclusive {
        val bytes=fixtures.open("context-v1.json").use {it.readBytes()}
        val sha=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}
        check(sha=="b8928e45bf8d25a9cf653ef64002601a53b85680430a3348c1cd73d74a5591bd"){"E_CORPUS_CHANGED"}
        val corpus=JSONObject(String(bytes,Charsets.UTF_8));val cases=corpus.getJSONArray("cases")
        check(cases.length()==10 && corpus.getInt("minimum_improved")==8){"E_CORPUS_COUNT"}
        val shared=ProbeResources.deploy(context);ProbeModelResources.deploy(context,shared)
        val sharedBefore=ProbeFileSnapshot.fingerprint(shared)
        val results=Array(10){JSONObject().put("id",cases.getJSONObject(it).getString("id"))}
        val modeControls=JSONArray();val start=System.nanoTime()
        var allControls=true
        var improved=0
        for(modelEnabled in listOf(false,true)) {
            val label=if(modelEnabled)"model_on" else "model_off"
            val profile=File(context.noBackupFilesDir,"m3/context/${UUID.randomUUID()}");check(profile.mkdirs())
            // Test-only profile overlay. Shipping shared schemas/data remain byte-identical.
            val patch="patch:\n  translator/enable_user_dict: false\n  translator/contextual_suggestions: true\n  grammar/language: '${if(modelEnabled)"zh-hans-t-essay-bgw-compact" else ""}'\n"
            for(schema in listOf("wanxiang_t9","wanxiang_t9_nolearn"))File(profile,"$schema.custom.yaml").writeText(patch)
            var started=false
            try {
                Rime.startupRime(shared.absolutePath,profile.absolutePath,"m3-context",true);started=true
                Rime.awaitRimeMaintenance();check(Rime.selectRimeSchema("wanxiang_t9")){"E_SCHEMA"}
                val loadedBefore=Rime.getRimeProbeCompactGrammarLoaded()
                val queryBefore=Rime.getRimeProbeGrammarQueryCount()
                val profileBefore=ProbeFileSnapshot.fingerprint(profile)
                var histories=true;var prefixes=true;var noAutoCommit=true;var offUnchanged=true
                fun fresh(){Rime.resetRimeSession();check(Rime.getCurrentRimeSchema()=="wanxiang_t9"){"E_DEFAULT_SCHEMA"};histories=histories && Rime.getRimeProbeHistorySize()==0}
                fun type(pinyin:String){Rime.clearRimeComposition();for(ch in KeyboardReducer.encode(pinyin))check(Rime.processRimeKey(ch.code,0)){"E_KEY"}}
                fun rank(target:String)=Rime.getRimeCandidates(0,500).indexOfFirst {it.text==target}.let {if(it<0)-1 else it+1}
                for(i in 0 until cases.length()) {
                    val row=cases.getJSONObject(i)
                    fresh();type(row.getString("target_pinyin"));val before=rank(row.getString("target"))
                    noAutoCommit=noAutoCommit && Rime.getRimeCommit().text.isNullOrEmpty()
                    fresh();type(row.getString("prefix_pinyin"));val prefixRank=rank(row.getString("prefix"))
                    val prefixSelected=prefixRank>0 && Rime.selectRimeCandidate(prefixRank-1,true)
                    val prefixExact=Rime.getRimeCommit().text==row.getString("prefix")
                    prefixes=prefixes && prefixSelected && prefixExact && Rime.getRimeProbeHistorySize()>0
                    type(row.getString("target_pinyin"));val after=rank(row.getString("target"))
                    noAutoCommit=noAutoCommit && Rime.getRimeCommit().text.isNullOrEmpty()
                    if(modelEnabled && before>0 && after>0 && after<before)improved++
                    if(!modelEnabled && before!=after)offUnchanged=false
                    results[i].put(label,JSONObject().put("before",before).put("after_context",after).put("prefix_committed",prefixSelected && prefixExact))
                }
                val loadedAfter=Rime.getRimeProbeCompactGrammarLoaded()
                val queryAfter=Rime.getRimeProbeGrammarQueryCount()
                Rime.exitRime();started=false
                val unchanged=profileBefore==ProbeFileSnapshot.fingerprint(profile)
                val noUserdb=profile.walkTopDown().none {it.name.contains(".userdb")}
                val control=histories && prefixes && noAutoCommit && unchanged && noUserdb && loadedBefore==modelEnabled && loadedAfter==modelEnabled && if(modelEnabled)queryAfter>queryBefore else queryAfter==0L && offUnchanged
                modeControls.put(JSONObject().put("mode",label).put("grammar_loaded_before",loadedBefore).put("grammar_loaded_after",loadedAfter)
                    .put("grammar_queries_before",queryBefore).put("grammar_queries_after",queryAfter).put("fresh_history",histories)
                    .put("all_prefix_commits_exact",prefixes).put("no_automatic_commit",noAutoCommit).put("profile_unchanged",unchanged)
                    .put("no_userdb",noUserdb).put("model_off_rank_unchanged",offUnchanged).put("pass",control))
                allControls=allControls && control
            }finally{if(started)Rime.exitRime()}
        }
        val staticUnchanged=sharedBefore==ProbeFileSnapshot.fingerprint(shared)
        JSONObject().put("scope","R10 frozen synthetic minimal pairs, model/history four-cell control; NOT enabled IME or quality equivalence")
            .put("corpus_sha256",sha).put("corpus_frozen_commit","41d6795").put("candidate_limit",500).put("case_count",10)
            .put("improved_with_model",improved).put("required_improved",8).put("controls",modeControls).put("ranks",JSONArray(results.toList()))
            .put("static_resources_unchanged",staticUnchanged).put("controls_pass",allControls).put("pass",improved>=8 && allControls && staticUnchanged)
            .put("elapsed_ms",(System.nanoTime()-start)/1_000_000)
    }
}
