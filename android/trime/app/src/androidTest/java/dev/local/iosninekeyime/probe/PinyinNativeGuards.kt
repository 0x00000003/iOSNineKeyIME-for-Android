// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.probe

import android.content.Context
import com.osfans.trime.core.Rime
import dev.local.iosninekeyime.engine.NativeEngineLease
import java.io.File
import java.util.UUID
import org.json.JSONObject

/** One reserved JNI thread, fixed synthetic data; only bools/counts leave this probe. */
object PinyinNativeGuards {
    fun run(context:Context):JSONObject=NativeEngineLease.exclusive {
        val result=JSONObject().put("scope","Android JNI stale raw/range/confirmed-prefix/session guards")
        var started=false;var stage="no-session"
        try {
            check(Rime.getRimeConfirmedInputPosition()==-1)
            check(!Rime.rewriteRimePinyin("64426",0,2,"ni"))
            result.put("no_implicit_session_creation",true)
            val shared=ProbeResources.deploy(context);ProbeModelResources.deploy(context,shared)
            val profile=File(context.noBackupFilesDir,"m2/pinyin-native/${UUID.randomUUID()}")
            check(profile.mkdirs())
            Rime.startupRime(shared.absolutePath,profile.absolutePath,"pinyin-guards",true);started=true
            Rime.awaitRimeMaintenance();check(Rime.selectRimeSchema("wanxiang_t9"))
            fun type(raw:String){Rime.clearRimeComposition();for(ch in raw)check(Rime.processRimeKey(ch.code,0))}
            fun select(word:String){val i=Rime.getRimeCandidates(0,500).indexOfFirst{it.text==word};check(i>=0&&Rime.selectRimeCandidate(i,true))}
            stage="stale-range";type("64426")
            check(!Rime.rewriteRimePinyin("74426",0,2,"ni"))
            check(!Rime.rewriteRimePinyin("64426",1,3,"ni"))
            check(!Rime.rewriteRimePinyin("64426",0,5,"ni"))
            check(!Rime.rewriteRimePinyin("64426",0,2,"shi"))
            check(!Rime.rewriteRimePinyin("64426",-1,2,"ni"))
            check(!Rime.rewriteRimePinyin("64426",0,2,"ni'hao"))
            check(Rime.getRimeRawInput()=="64426"&&Rime.getRimeCommit().text.isNullOrEmpty())
            result.put("invalid_native_rewrites_leave_input_unchanged",true)
            stage="partial-prefix";type("64426744543");select("你好")
            val prefixCommit=Rime.getRimeCommit().text.orEmpty()
            check(Rime.getRimeConfirmedInputPosition()==5)
            check(!Rime.rewriteRimePinyin("64426744543",0,2,"ni"))
            val history=Rime.getRimeProbeHistorySize()
            check(Rime.rewriteRimePinyin("64426744543",5,8,"shi"))
            check(Rime.getRimeRawInput()=="64426'shi'543"&&Rime.getRimeConfirmedInputPosition()==5)
            check(Rime.getRimeCommit().text.isNullOrEmpty()&&Rime.getRimeProbeHistorySize()==history)
            check(Rime.rewriteRimePinyin("64426'shi'543",10,13,"jie"))
            check(Rime.getRimeConfirmedInputPosition()==5&&Rime.getRimeCommit().text.isNullOrEmpty())
            select("世界")
            check(prefixCommit+Rime.getRimeCommit().text.orEmpty()=="你好世界"&&Rime.getRimeCommit().text.isNullOrEmpty())
            result.put("confirmed_prefix_history_retained_and_final_commit_exact_once",true)
            stage="end-session";Rime.resetRimeSession()
            check(Rime.getRimeConfirmedInputPosition()==-1&&!Rime.rewriteRimePinyin("64426",0,2,"ni"))
            result.put("ended_session_cannot_be_recreated_by_pinyin",true).put("pass",true)
        }catch(_:Throwable){result.put("failure_stage",stage).put("pass",false)}
        finally{if(started)Rime.exitRime()}
        result
    }
}
