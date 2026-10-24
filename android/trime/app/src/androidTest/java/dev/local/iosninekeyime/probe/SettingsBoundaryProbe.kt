// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.probe

import android.app.Instrumentation
import android.app.AlertDialog
import android.content.Intent
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import com.osfans.trime.BuildConfig
import dev.local.iosninekeyime.settings.OfflineNoticesActivity
import java.security.MessageDigest
import org.json.JSONObject

/** Only opens own offline UI and CANCELS the clear dialog. Never enables an IME or clears normal data. */
object SettingsBoundaryProbe {
    fun run(instrumentation:Instrumentation):JSONObject {
        var notice:OfflineNoticesActivity?=null
        var probe:ProbeActivity?=null
        var cancelledDialog:AlertDialog?=null
        val result=JSONObject().put("scope","own offline notice UI and cancel-only clear confirmation; NOT full license audit or production learning erase")
        fun main(block:()->Unit){var failure:Throwable?=null;instrumentation.runOnMainSync {try{block()}catch(e:Throwable){failure=e}};failure?.let{throw it}}
        try {
            val raw=instrumentation.targetContext.assets.open("offline-notices.json").use {it.readBytes()}
            val sha=MessageDigest.getInstance("SHA-256").digest(raw).joinToString(""){"%02x".format(it)}
            val entries=JSONObject(String(raw,Charsets.UTF_8)).getJSONArray("entries")
            val originals= (0 until entries.length()).all {i->
                val row=entries.getJSONObject(i)
                val digest=MessageDigest.getInstance("SHA-256").digest(row.getString("text").toByteArray(Charsets.UTF_8)).joinToString(""){"%02x".format(it)}
                digest==row.getString("sha256")
            }
            result.put("bundle_sha_matches_pin",sha==BuildConfig.OFFLINE_NOTICES_SHA256).put("all_notice_text_hashes_match",originals)
            val context=instrumentation.targetContext
            notice=instrumentation.startActivitySync(Intent(context,OfflineNoticesActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)) as OfflineNoticesActivity
            main {
                val activity=checkNotNull(notice)
                result.put("notice_count",activity.noticeCount).put("dependency_count",activity.dependencyCount)
                val list=checkNotNull(activity.window.decorView.findViewWithTag<ListView>("offline-notice-list"))
                list.performItemClick(list.getChildAt(0),0,0)
                val dialog=checkNotNull(activity.sourceNoticeDialog)
                val text=checkNotNull(dialog.window?.decorView?.findViewWithTag<TextView>("offline-notice-body"))
                result.put("plain_text_dialog_visible",dialog.isShowing && text.text.length>500)
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                val ndkIndex=(0 until entries.length()).single {entries.getJSONObject(it).getString("source_path")=="docs/reference/upstream/ndk-r28/LLVM-NOTICE"}
                list.performItemClick(list.getChildAt(0),ndkIndex,ndkIndex.toLong())
                val ndkDialog=checkNotNull(activity.sourceNoticeDialog)
                val ndkText=checkNotNull(ndkDialog.window?.decorView?.findViewWithTag<TextView>("offline-notice-body"))
                result.put("ndk_runtime_notice_visible",ndkDialog.isShowing && ndkText.text.toString().endsWith(entries.getJSONObject(ndkIndex).getString("text")))
                ndkDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                activity.finish();notice=null
            }
            probe=instrumentation.startActivitySync(Intent(context,ProbeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)) as ProbeActivity
            main {
                val activity=checkNotNull(probe)
                checkNotNull(activity.window.decorView.findViewWithTag<Button>("clear-learning")).performClick()
                val dialog=checkNotNull(activity.clearConfirmation)
                val present=dialog.isShowing && dialog.getButton(AlertDialog.BUTTON_POSITIVE).text.toString()=="清空学习"
                cancelledDialog=dialog
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
                result.put("explicit_clear_confirmation_present",present)
            }
            // AlertDialog posts dismissal to its Handler after performClick returns.
            instrumentation.waitForIdleSync()
            main {result.put("clear_dialog_cancelled",cancelledDialog?.isShowing==false);probe?.finish();probe=null}
            result.put("production_clear_not_executed",true).put("system_ime_settings_not_opened",true)
            val controls=listOf("bundle_sha_matches_pin","all_notice_text_hashes_match","plain_text_dialog_visible","ndk_runtime_notice_visible","explicit_clear_confirmation_present","clear_dialog_cancelled")
            result.put("pass",controls.all {result.getBoolean(it)} && result.getInt("notice_count")==44 && result.getInt("dependency_count")>100)
            return result
        }finally{main{notice?.finish();probe?.finish()}}
    }
}
