// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.probe

import android.app.Instrumentation
import android.content.Intent
import android.text.InputType
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import dev.local.iosninekeyime.editor.EditorHarnessActivity
import dev.local.iosninekeyime.editor.HostEditorAdapter
import dev.local.iosninekeyime.editor.EditResult
import dev.local.iosninekeyime.privacy.EditorPolicy
import dev.local.iosninekeyime.state.*
import org.json.JSONArray
import org.json.JSONObject

/** Real synthetic EditText behind throwing/reentrant connections; never persist exception payloads/text. */
object HostConnectionFailureProbe {
    fun run(instrumentation:Instrumentation):JSONObject {
        val activity=instrumentation.startActivitySync(Intent(instrumentation.targetContext,EditorHarnessActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as EditorHarnessActivity
        val results=JSONArray()
        var privacyReads=0
        fun main(block:()->Unit) {
            var failure:Throwable?=null
            instrumentation.runOnMainSync {try{block()}catch(error:Throwable){failure=error}}
            instrumentation.waitForIdleSync()
            failure?.let {throw it}
        }
        try {
            val points=listOf("begin_commit","commit","end_commit","selected","before","delete","private_key","action","reentrant_begin","begin_delete","end_delete")
            for((index,point)in points.withIndex()) {
                var passed=false
                main {
                    activity.editor.inputType=InputType.TYPE_CLASS_TEXT
                    activity.editor.setText("AB");activity.editor.setSelection(2)
                    val normal=EditorPolicy.from(InputType.TYPE_CLASS_TEXT,0,false)
                    val host=HostEditorAdapter()
                    val actual=checkNotNull(activity.editor.onCreateInputConnection(EditorInfo()))
                    var commitCalls=0
                    val wrapped=object:InputConnectionWrapper(actual,false) {
                        fun fail():Nothing=throw IllegalStateException("E_SYNTHETIC_HOST_CONNECTION")
                        override fun beginBatchEdit():Boolean {
                            if(point in listOf("begin_commit","begin_delete"))fail()
                            if(point=="reentrant_begin")host.begin(2,actual,normal)
                            return super.beginBatchEdit()
                        }
                        override fun endBatchEdit():Boolean {if(point in listOf("end_commit","end_delete")){super.endBatchEdit();fail()};return super.endBatchEdit()}
                        override fun commitText(text:CharSequence?,newCursorPosition:Int):Boolean {commitCalls++;if(point=="commit")fail();return super.commitText(text,newCursorPosition)}
                        override fun getSelectedText(flags:Int):CharSequence? {if(point=="private_key")privacyReads++;if(point=="selected")fail();return super.getSelectedText(flags)}
                        override fun getTextBeforeCursor(n:Int,flags:Int):CharSequence? {if(point=="private_key")privacyReads++;if(point=="before")fail();return super.getTextBeforeCursor(n,flags)}
                        override fun deleteSurroundingText(beforeLength:Int,afterLength:Int):Boolean {if(point=="delete")fail();return super.deleteSurroundingText(beforeLength,afterLength)}
                        override fun sendKeyEvent(event:KeyEvent?):Boolean {if(point=="private_key")fail();return super.sendKeyEvent(event)}
                        override fun performEditorAction(actionCode:Int):Boolean {if(point=="action")fail();return super.performEditorAction(actionCode)}
                    }
                    host.begin(1,wrapped,if(point=="private_key")EditorPolicy.from(InputType.TYPE_CLASS_TEXT,0,true)else normal)
                    try {
                        val effect=Effect.Commit(Stamp(1,1),0,"C")
                        passed=when(point) {
                            "begin_commit","commit" -> host.commit(effect)==Delivery.BLOCKED && host.pendingCount==1 && activity.editor.text.toString()=="AB"
                            "end_commit" -> host.commit(effect)==Delivery.ACCEPTED && host.commit(effect)==Delivery.DUPLICATE && activity.editor.text.toString()=="ABC" && commitCalls==1
                            "reentrant_begin" -> host.commit(effect)==Delivery.STALE && activity.editor.text.toString()=="AB" && commitCalls==0
                            "action" -> !host.action(1,EditorAction.DONE) && activity.editor.text.toString()=="AB"
                            "end_delete" -> host.delete(1)==EditResult.ACCEPTED && activity.editor.text.toString()=="A"
                            else -> host.delete(1)==EditResult.UNAVAILABLE && activity.editor.text.toString()=="AB"
                        }
                    }catch(_:RuntimeException){passed=false}
                    host.end(if(point=="reentrant_begin")2 else 1)
                }
                results.put(JSONObject().put("case_id","h%03d".format(index+1)).put("fault_point",point).put("pass",passed))
            }
            val all=(0 until results.length()).all {results.getJSONObject(it).getBoolean("pass")}
            return JSONObject().put("scope","synthetic real-host exception/reentrant batch boundaries only; no after-apply ambiguous commit recovery guarantee or R04 waiver")
                .put("cases",results).put("private_context_reads",privacyReads).put("pass",all&&privacyReads==0)
        }finally{main{activity.finish()}}
    }
}
