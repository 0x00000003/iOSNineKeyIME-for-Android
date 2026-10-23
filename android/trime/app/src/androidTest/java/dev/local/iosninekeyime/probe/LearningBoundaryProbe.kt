// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.probe

import android.content.Context
import android.content.res.AssetManager
import com.osfans.trime.core.Rime
import dev.local.iosninekeyime.engine.NativeEngineLease
import dev.local.iosninekeyime.privacy.LearningStore
import dev.local.iosninekeyime.state.KeyboardReducer
import java.io.File
import java.util.UUID
import java.security.MessageDigest
import org.json.JSONArray
import org.json.JSONObject

/** Frozen ten-query protocol. Candidate targets remain only in RAM; reports contain ID/rank. */
object LearningBoundaryProbe {
    fun run(context:Context,fixtures:AssetManager):JSONObject {
        val bytes=fixtures.open("learning-v1.json").use {it.readBytes()}
        val sha=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}
        check(sha=="3c43d871220a5e8952ca77f862f6526b63bfb88da66eef3fbd14799727d88a6b"){"E_CORPUS_CHANGED"}
        val protocol=JSONObject(String(bytes,Charsets.UTF_8));val rows=protocol.getJSONArray("pinyin")
        check(rows.length()==10 && protocol.getInt("repetitions")==20 && protocol.getInt("minimum_improved")==8){"E_PROTOCOL"}
        val profile=File(context.noBackupFilesDir,"m3/learning/${UUID.randomUUID()}")
        val targets=mutableListOf<String>()
        val before=mutableListOf<Int>();val after=mutableListOf<Int>();val restarted=mutableListOf<Int>();val cleared=mutableListOf<Int>()
        val start=System.nanoTime()
        val result=JSONObject().put("scope","R09 deterministic ten-query JNI/erase component; NOT system IME or complete privacy/security acceptance")
            .put("protocol_sha256",sha).put("protocol_frozen_commit","28fabd5").put("repetitions_per_target",20)
        var commitsExact=true
        var historyZero=true
        var started=false
        lateinit var shared:File
        fun initialize() {
            check(profile.mkdirs() || profile.isDirectory){"E_PROFILE"}
            Rime.startupRime(shared.absolutePath,profile.absolutePath,"m3-learning",true);started=true
            Rime.awaitRimeMaintenance();check(Rime.selectRimeSchema("wanxiang_t9")){"E_SCHEMA"}
        }
        fun stop(){if(started){Rime.exitRime();started=false}}
        fun query(index:Int):List<String> {
            Rime.resetRimeSession();check(Rime.selectRimeSchema("wanxiang_t9")){"E_SCHEMA"}
            historyZero=historyZero && Rime.getRimeProbeHistorySize()==0
            for(ch in KeyboardReducer.encode(rows.getString(index)))check(Rime.processRimeKey(ch.code,0)){"E_KEY"}
            return Rime.getRimeCandidates(0,100).map {it.text}
        }
        fun rank(index:Int)=query(index).indexOf(targets[index]).let {if(it<0)-1 else it+1}
        NativeEngineLease.exclusive {
            shared=ProbeResources.deploy(context);ProbeModelResources.deploy(context,shared)
            try {
                initialize()
                result.put("clear_rejected_while_native_reserved",try{LearningStore.clearSynthetic(context,profile);false}catch(e:IllegalStateException){e.message=="E_ENGINE_BUSY"})
                for(i in 0 until rows.length()) {
                    targets+=query(i).getOrNull(1)?:error("E_NONFIRST_FIXTURE")
                    before+=rank(i)
                }
                check(targets.distinct().size==10){"E_DUPLICATE_FIXTURE"}
                for(i in 0 until rows.length())repeat(20) {
                    val position=rank(i)
                    check(position>0 && Rime.selectRimeCandidate(position-1,true)){"E_SELECT"}
                    val commit=Rime.getRimeCommit().text
                    val duplicate=Rime.getRimeCommit().text
                    commitsExact=commitsExact && commit==targets[i] && duplicate.isNullOrEmpty()
                    Rime.processRimeKey(0xffe1,0)
                }
                for(i in 0 until rows.length())after+=rank(i)
                stop();initialize()
                for(i in 0 until rows.length())restarted+=rank(i)
                result.put("positive_normal_userdb",profile.walkTopDown().any {it.name.contains(".userdb")})
            } finally {stop()}
        }
        // A private sibling sentinel tests no symlink traversal. Neither file contains user data.
        val sentinel=File(context.noBackupFilesDir,"m3/erase-sentinel/${UUID.randomUUID()}")
        check(sentinel.mkdirs())
        val marker=File(sentinel,"marker");marker.writeText("synthetic")
        android.system.Os.symlink(sentinel.absolutePath,File(profile,"synthetic-sibling-link").absolutePath)
        try {
            val erased=LearningStore.clearSynthetic(context,profile)
            result.put("old_profile_absent_after_clear",erased && !profile.exists())
                .put("sibling_sentinel_unchanged",marker.readText()=="synthetic")
        } finally {check(marker.delete());check(sentinel.delete())}
        NativeEngineLease.exclusive {
            try {initialize();for(i in 0 until rows.length())cleared+=rank(i)} finally {stop()}
        }
        var improved=0;var persisted=0;var reset=0;val ranks=JSONArray()
        for(i in 0 until rows.length()) {
            if(after[i]>0 && (after[i]<before[i] || after[i]==1))improved++
            if(restarted[i]==after[i])persisted++
            if(cleared[i]==before[i])reset++
            ranks.put(JSONObject().put("id","l%03d".format(i+1)).put("before",before[i]).put("after",after[i]).put("restart",restarted[i]).put("clear",cleared[i]))
        }
        result.put("target_count",10).put("nonfirst_baselines",before.count {it>1}).put("improved",improved)
            .put("persisted_exact_rank",persisted).put("clear_restored_baseline",reset).put("ranks",ranks)
            .put("all_commits_exact_once",commitsExact).put("fresh_history_each_measurement",historyZero)
            .put("elapsed_ms",(System.nanoTime()-start)/1_000_000)
        val controls=listOf("clear_rejected_while_native_reserved","positive_normal_userdb","old_profile_absent_after_clear","sibling_sentinel_unchanged","all_commits_exact_once","fresh_history_each_measurement")
        result.put("pass",improved>=8 && persisted==10 && reset==10 && before.all {it>1} && controls.all {result.getBoolean(it)})
        return result
    }
}
