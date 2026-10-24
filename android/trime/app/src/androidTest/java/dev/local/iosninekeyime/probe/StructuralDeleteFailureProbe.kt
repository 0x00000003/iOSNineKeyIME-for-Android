// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.probe

import android.app.Instrumentation
import android.content.Intent
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.inputmethod.*
import dev.local.iosninekeyime.editor.*
import dev.local.iosninekeyime.privacy.EditorPolicy
import dev.local.iosninekeyime.state.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.function.IntConsumer

/** Real synthetic EditText, production adapter, fault injection only at host/API boundaries. */
object StructuralDeleteFailureProbe {
    @androidx.annotation.RequiresApi(34)
    fun run(instrumentation:Instrumentation):JSONObject {
        val context=instrumentation.targetContext
        val activity=instrumentation.startActivitySync(Intent(context,EditorHarnessActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as EditorHarnessActivity
        val handler=Handler(Looper.getMainLooper());val rows=JSONArray();var reads=0;var stage="setup"
        val policy=EditorPolicy.from(InputType.TYPE_CLASS_TEXT,0,true)
        fun main(block:()->Unit){var failure:Throwable?=null;instrumentation.runOnMainSync {try{block()}catch(t:Throwable){failure=t}};failure?.let {throw it}}
        val plans=listOf(
            "cursor_unsupported","cursor_throw","anchor_timeout","anchor_wrong_selection",
            "bounds_unsupported","bounds_throw","bounds_timeout","field_switch_before_bounds","selection_move_before_bounds",
            "gesture_unsupported","gesture_throw_before","gesture_throw_after","gesture_timeout_before","gesture_timeout_after",
            "duplicate_bounds","duplicate_gesture","duplicate_failed_gesture","selected_range","empty_cursor","reentrant_begin","reentrant_end",
        )
        try {
            for(id in plans){
                stage=id
                val host=HostEditorAdapter();var session=1L;var result:EditResult?=null;var replies=0
                var boundsCalls=0;var gestureCalls=0;var keyCalls=0;var anchorCalls=0;var faultApplied=false
                val latch=CountDownLatch(1)
                lateinit var guarded:InputConnection
                main {
                    activity.editor.apply {setSingleLine(true);inputType=InputType.TYPE_CLASS_TEXT;setText(if(id=="duplicate_failed_gesture")"Ae\u0301"else"AB");setSelection(text.length);requestFocus()}
                    if(id=="selected_range")activity.editor.setSelection(0,2)
                    if(id=="empty_cursor")activity.editor.setSelection(0)
                    val info=EditorInfo();val actual=checkNotNull(activity.editor.onCreateInputConnection(info))
                    guarded=object:InputConnectionWrapper(actual,false){
                        private fun forbidden():Nothing{reads++;error("E_STRUCTURAL_BODY_READ")}
                        override fun getTextBeforeCursor(n:Int,flags:Int):CharSequence?=forbidden()
                        override fun getTextAfterCursor(n:Int,flags:Int):CharSequence?=forbidden()
                        override fun getSelectedText(flags:Int):CharSequence?=forbidden()
                        override fun getSurroundingText(before:Int,after:Int,flags:Int):SurroundingText?=forbidden()
                        override fun getExtractedText(request:ExtractedTextRequest?,flags:Int):ExtractedText?=forbidden()
                        override fun getCursorCapsMode(modes:Int):Int=forbidden()
                        override fun sendKeyEvent(event:android.view.KeyEvent):Boolean {keyCalls++;return super.sendKeyEvent(event)}
                        override fun requestCursorUpdates(mode:Int,filter:Int):Boolean {
                            anchorCalls++
                            check(mode==InputConnection.CURSOR_UPDATE_IMMEDIATE && filter==(InputConnection.CURSOR_UPDATE_FILTER_INSERTION_MARKER or InputConnection.CURSOR_UPDATE_FILTER_EDITOR_BOUNDS)){"E_STRUCTURAL_FILTER"}
                            if(id=="cursor_unsupported")return false
                            if(id=="cursor_throw")throw IllegalStateException()
                            if(id!="anchor_timeout")handler.post {
                                if(id=="anchor_wrong_selection"){
                                    val original=SyntheticCursorMetadata.anchor(activity.editor)
                                    host.updateCursorAnchor(session,CursorAnchorInfo.Builder().setSelectionRange(0,0).setMatrix(original.matrix).build())
                                }else host.updateCursorAnchor(session,SyntheticCursorMetadata.anchor(activity.editor))
                            }
                            return true
                        }
                        override fun requestTextBoundsInfo(area:RectF,executor:Executor,consumer:Consumer<TextBoundsInfoResult>){
                            boundsCalls++
                            when(id){
                                "bounds_throw" -> throw IllegalStateException()
                                "bounds_timeout" -> return
                                "bounds_unsupported" -> {executor.execute {consumer.accept(TextBoundsInfoResult(TextBoundsInfoResult.CODE_UNSUPPORTED))};return}
                            }
                            super.requestTextBoundsInfo(area,executor,Consumer { value ->
                                if(id=="field_switch_before_bounds"){
                                    session++;host.begin(session,guarded,policy,info);faultApplied=true
                                }
                                if(id=="selection_move_before_bounds"){
                                    activity.editor.setSelection(0);host.updateSelection(session,0,0);faultApplied=true
                                }
                                consumer.accept(value)
                                if(id=="duplicate_bounds")consumer.accept(value)
                            })
                        }
                        override fun performHandwritingGesture(gesture:HandwritingGesture,delivery:Executor?,callback:IntConsumer?){
                            val executor=checkNotNull(delivery);val consumer=checkNotNull(callback)
                            gestureCalls++
                            when(id){
                                "gesture_unsupported" -> {executor.execute {consumer.accept(InputConnection.HANDWRITING_GESTURE_RESULT_UNSUPPORTED)};return}
                                "gesture_throw_before" -> throw IllegalStateException()
                                "gesture_timeout_before" -> return
                                "gesture_throw_after" -> {super.performHandwritingGesture(gesture,executor,IntConsumer { });faultApplied=true;throw IllegalStateException()}
                                "gesture_timeout_after" -> {super.performHandwritingGesture(gesture,executor,IntConsumer { });faultApplied=true;return}
                            }
                            super.performHandwritingGesture(gesture,executor,IntConsumer { code ->
                                consumer.accept(code)
                                if(id=="duplicate_gesture" || id=="duplicate_failed_gesture")consumer.accept(code)
                            })
                        }
                    }
                    host.begin(session,guarded,policy,info)
                    host.deleteAsync(session){
                        result=it;replies++
                        if(id=="reentrant_begin" || id=="reentrant_end"){
                            session=if(id=="reentrant_begin")3L else 2L
                            host.begin(session,guarded,policy,info)
                        }
                        latch.countDown()
                    }
                    if(id=="reentrant_begin" || id=="reentrant_end"){
                        if(id=="reentrant_begin")host.begin(2,guarded,policy,info)else host.end(1)
                        faultApplied=host.commit(Effect.Commit(Stamp(session,1),0,"Q"))==Delivery.ACCEPTED
                    }
                }
                check(latch.await(4,TimeUnit.SECONDS)){"E_STRUCTURAL_TEST_TIMEOUT"}
                instrumentation.waitForIdleSync()
                main {
                    val expected=when(id){
                        "field_switch_before_bounds","selection_move_before_bounds","anchor_wrong_selection","reentrant_begin","reentrant_end" -> EditResult.STALE
                        "gesture_throw_before","gesture_throw_after","gesture_timeout_before","gesture_timeout_after" -> EditResult.AMBIGUOUS
                        "duplicate_bounds","duplicate_gesture","duplicate_failed_gesture","selected_range","empty_cursor" -> EditResult.ACCEPTED
                        else -> EditResult.UNAVAILABLE
                    }
                    val expectedBody=when(id){
                        "duplicate_bounds","duplicate_gesture","duplicate_failed_gesture","gesture_throw_after","gesture_timeout_after" -> "A"
                        "reentrant_begin","reentrant_end" -> "ABQ"
                        "selected_range" -> ""
                        else -> "AB"
                    }
                    val exact=activity.editor.text.toString()==expectedBody
                    val faultRequired=id in setOf("field_switch_before_bounds","selection_move_before_bounds","gesture_throw_after","gesture_timeout_after","reentrant_begin","reentrant_end")
                    val callsCorrect=when(id){
                        "duplicate_bounds","duplicate_gesture" -> boundsCalls==1 && gestureCalls==1
                        "duplicate_failed_gesture" -> boundsCalls==1 && gestureCalls==2
                        "selected_range" -> anchorCalls==0 && boundsCalls==0 && gestureCalls==0
                        "empty_cursor" -> anchorCalls==1 && boundsCalls==0 && gestureCalls==0
                        "field_switch_before_bounds","selection_move_before_bounds" -> gestureCalls==0
                        else -> true
                    }
                    rows.put(JSONObject().put("id",id).put("result_matches",result==expected).put("exact_synthetic_remaining",exact)
                        .put("reply_count",replies).put("bounds_requests",boundsCalls).put("gesture_requests",gestureCalls)
                        .put("no_key_fallback",keyCalls==0).put("fault_injected",faultApplied).put("pass",result==expected && replies==1 && exact && callsCorrect && keyCalls==0 && (!faultRequired || faultApplied)))
                    host.end(session)
                }
            }
        }catch(_:Throwable){return JSONObject().put("scope","production structural adapter faults with synthetic cursor bridge; NOT system IMS lifecycle").put("stage",stage).put("cases",rows).put("pass",false)}
        finally {main {activity.finish()}}
        val passed=(0 until rows.length()).count {rows.getJSONObject(it).getBoolean("pass")}
        return JSONObject().put("scope","production structural adapter faults with synthetic cursor bridge; NOT system IMS lifecycle")
            .put("planned_cases",plans.size).put("passed_cases",passed).put("ic_body_getters",reads).put("cases",rows)
            .put("pass",passed==plans.size && rows.length()==plans.size && reads==0)
    }
}
