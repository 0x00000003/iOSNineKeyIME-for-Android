// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.probe

import android.app.Instrumentation
import android.content.Intent
import android.graphics.RectF
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Matrix
import android.os.Parcel
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.inputmethod.*
import dev.local.iosninekeyime.editor.EditorHarnessActivity
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.IntConsumer
import java.util.function.Consumer

/** Research ONLY: geometry is supplied by the synthetic host, never a production context query. */
object HostGestureResearchProbe {
    @androidx.annotation.RequiresApi(34)
    fun run(instrumentation:Instrumentation,boundsMetadata:Boolean=false):JSONObject {
        val context=instrumentation.targetContext
        val activity=instrumentation.startActivitySync(Intent(context,EditorHarnessActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as EditorHarnessActivity
        val handler=Handler(Looper.getMainLooper());val executor=Executor {handler.post(it)}
        val rows=JSONArray();var bodyReads=0;var boundsRequests=0;var stage="setup"
        val planned=if(boundsMetadata)18 else 36
        val start=System.nanoTime()
        val result=JSONObject().put("scope",if(boundsMetadata)"synthetic STRUCTURAL METADATA queries and host gestures; NOT production privacy policy or full R04"else"synthetic host character-gesture capability with TEST-ONLY layout geometry; NOT production deletion or full R04")
            .put("protocol_frozen_commit",if(boundsMetadata)"5cfed97"else"bc342b0").put("production_host_path_changed",false)
            .put("geometry_provider",if(boundsMetadata)"parcelled IC character bounds; test provides only visible editor region"else"owned synthetic Layout oracle; no production metadata query implemented")
        fun main(block:()->Unit){var error:Throwable?=null;instrumentation.runOnMainSync{try{block()}catch(t:Throwable){error=t}};error?.let{throw it}}
        val clusters=listOf("\uD83D\uDE80","e\u0301","\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66","\uD83C\uDDE8\uD83C\uDDF3","\uD83D\uDC4D\uD83C\uDFFD","a\u0301\u0327","\u05D0\u05B7","\u79B0\uDB40\uDD00","q")
        val prefix="xy"
        try {
            for(masked in listOf(false,true))for(strategy in if(boundsMetadata)listOf("parcelled-character-bounds-scan")else listOf("whole-cluster-oracle","unit-range-scan"))for((index,cluster)in clusters.withIndex()) {
                val id="g%03d".format(index+1);stage="$id/$strategy/${if(masked)"password"else"plain"}"
                check(System.nanoTime()-start<TimeUnit.SECONDS.toNanos(90)){"E_GESTURE_TOTAL_BUDGET"}
                val expected=prefix+cluster
                lateinit var connection:InputConnection
                var cursor=0;var advertised=false
                main {
                    activity.editor.apply {
                        setSingleLine(true)
                        inputType=InputType.TYPE_CLASS_TEXT or if(masked)InputType.TYPE_TEXT_VARIATION_PASSWORD else InputType.TYPE_TEXT_VARIATION_NORMAL
                        check((transformationMethod is android.text.method.PasswordTransformationMethod)==masked){"E_GESTURE_MASK_FIXTURE"}
                        gravity=Gravity.TOP or Gravity.START;showSoftInputOnFocus=false
                        setText(expected);setSelection(expected.length);requestFocus()
                    }
                    val info=EditorInfo();val actual=checkNotNull(activity.editor.onCreateInputConnection(info))
                    cursor=info.initialSelEnd
                    check(cursor==expected.length && activity.editor.text.toString()==expected){"E_GESTURE_FIXTURE"}
                    advertised=info.supportedHandwritingGestures.contains(DeleteGesture::class.java)
                    connection=object:InputConnectionWrapper(actual,false){
                        private fun forbidden():Nothing{bodyReads++;error("E_RESEARCH_BODY_READ")}
                        override fun getTextBeforeCursor(length:Int,flags:Int):CharSequence?=forbidden()
                        override fun getTextAfterCursor(length:Int,flags:Int):CharSequence?=forbidden()
                        override fun getSelectedText(flags:Int):CharSequence?=forbidden()
                        override fun getSurroundingText(beforeLength:Int,afterLength:Int,flags:Int):SurroundingText?=forbidden()
                        override fun getExtractedText(request:ExtractedTextRequest?,flags:Int):ExtractedText?=forbidden()
                        override fun getCursorCapsMode(reqModes:Int):Int=forbidden()
                        override fun requestTextBoundsInfo(bounds:RectF,executor:Executor,consumer:Consumer<TextBoundsInfoResult>){boundsRequests++;super.requestTextBoundsInfo(bounds,executor,consumer)}
                    }
                }
                instrumentation.waitForIdleSync()
                var structural:TextBoundsInfo?=null;var markerAbsent=true
                if(boundsMetadata){
                    val received=CountDownLatch(1);val status=AtomicInteger(Int.MIN_VALUE);var callbackFailed=false
                    main{
                        val region=Rect();check(activity.editor.getGlobalVisibleRect(region)){"E_RESEARCH_REGION"}
                        connection.requestTextBoundsInfo(RectF(region),executor,Consumer {value->
                            try{
                                status.set(value.resultCode)
                                if(value.resultCode==TextBoundsInfoResult.CODE_SUCCESS){
                                    val incoming=checkNotNull(value.textBoundsInfo)
                                    val parcel=Parcel.obtain()
                                    try{
                                        incoming.writeToParcel(parcel,0);val bytes=parcel.marshall();check(bytes.size<=2*1024*1024){"E_RESEARCH_PARCEL_BUDGET"}
                                        markerAbsent=!contains(bytes,expected.toByteArray(Charsets.UTF_8)) && !contains(bytes,expected.toByteArray(Charsets.UTF_16LE))
                                        bytes.fill(0);parcel.setDataPosition(0);structural=TextBoundsInfo.CREATOR.createFromParcel(parcel)
                                    }finally{parcel.recycle()}
                                }
                            }catch(_:Throwable){callbackFailed=true}finally{received.countDown()}
                        })
                    }
                    check(received.await(2,TimeUnit.SECONDS)){"E_RESEARCH_BOUNDS_TIMEOUT"}
                    result.put("last_bounds_result_code",status.get())
                    check(!callbackFailed && status.get()==TextBoundsInfoResult.CODE_SUCCESS){"E_RESEARCH_BOUNDS"}
                    val snapshot=checkNotNull(structural);check(snapshot.startIndex<cursor && snapshot.endIndex>=cursor){"E_RESEARCH_BOUNDS_RANGE"}
                }
                val available=cursor-(structural?.startIndex?:0)
                val attempts=if(strategy=="whole-cluster-oracle")listOf(cluster.length)else(1..minOf(32,available)).toList()
                var code=InputConnection.HANDWRITING_GESTURE_RESULT_UNKNOWN;var calls=0;var unchangedOnFailure=true;var exact=false
                for(units in attempts) {
                    check(System.nanoTime()-start<TimeUnit.SECONDS.toNanos(90)){"E_GESTURE_TOTAL_BUDGET"}
                    val latch=CountDownLatch(1);val callback=AtomicInteger(Int.MIN_VALUE)
                    main {
                        val snapshot=structural
                        val area=if(snapshot!=null){
                            val bounds=RectF();snapshot.getCharacterBounds(cursor-units,bounds)
                            for(i in cursor-units+1 until cursor){val next=RectF();snapshot.getCharacterBounds(i,next);bounds.union(next)}
                            val matrix=Matrix();snapshot.getMatrix(matrix);matrix.mapRect(bounds)
                            RectF(bounds.left-0.1f,bounds.top,bounds.right+0.1f,bounds.bottom)
                        }else{
                            val editor=activity.editor;val layout=checkNotNull(editor.layout)
                            val location=IntArray(2);editor.getLocationOnScreen(location)
                            val line=layout.getLineForOffset(cursor)
                            val x=location[0]+editor.totalPaddingLeft-editor.scrollX
                            val y=location[1]+editor.baseline-layout.getLineBaseline(0)-editor.scrollY
                            // Selection paths preserve logical-range geometry at bidi boundaries.
                            val path=Path();layout.getSelectionPath(cursor-units,cursor,path)
                            val bounds=RectF();path.computeBounds(bounds,true)
                            if(bounds.isEmpty){val caret=layout.getPrimaryHorizontal(cursor);bounds.set(caret-0.1f,layout.getLineTop(line).toFloat(),caret+0.1f,layout.getLineBottom(line).toFloat())}
                            RectF(x+bounds.left-0.1f,y+bounds.top,x+bounds.right+0.1f,y+bounds.bottom)
                        }
                        val gesture=DeleteGesture.Builder().setDeletionArea(area).setGranularity(HandwritingGesture.GRANULARITY_CHARACTER).build()
                        calls++
                        connection.performHandwritingGesture(gesture,executor,IntConsumer {callback.set(it);latch.countDown()})
                    }
                    check(latch.await(2,TimeUnit.SECONDS)){"E_GESTURE_TIMEOUT"}
                    code=callback.get()
                    main {
                        if(code==InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS)exact=activity.editor.text.toString()==prefix && activity.editor.selectionStart==prefix.length && activity.editor.selectionEnd==prefix.length
                        else unchangedOnFailure=unchangedOnFailure && activity.editor.text.toString()==expected && activity.editor.selectionStart==cursor && activity.editor.selectionEnd==cursor
                    }
                    if(code==InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS || code!=InputConnection.HANDWRITING_GESTURE_RESULT_FAILED || !unchangedOnFailure)break
                }
                rows.put(JSONObject().put("id",id).put("mode",if(masked)"text-password"else"plain").put("strategy",strategy)
                    .put("actual_password_transform_verified",true)
                    .put("delete_gesture_advertised",advertised).put("gesture_calls",calls).put("result_code",code)
                    .put("failed_calls_left_host_unchanged",unchangedOnFailure).put("exact_prefix_remaining",exact)
                    .put("parcel_known_fixture_marker_absent",markerAbsent)
                    .put("pass",exact && unchangedOnFailure && markerAbsent))
            }
            val passed=(0 until rows.length()).count{rows.getJSONObject(it).getBoolean("pass")}
            return result.put("cases",rows).put("planned_cases",planned).put("passed_cases",passed).put("ic_body_or_context_getter_calls",bodyReads)
                .put("rime_native_calls_requested_by_probe",0).put("research_structural_bounds_requests",boundsRequests).put("production_metadata_queries",0).put("pass",passed==planned && rows.length()==planned && bodyReads==0)
        }catch(_:Throwable){return result.put("cases",rows).put("planned_cases",planned).put("failure_stage",stage).put("ic_body_or_context_getter_calls",bodyReads).put("research_structural_bounds_requests",boundsRequests).put("pass",false)}
        finally{main{activity.finish()}}
    }
    private fun contains(data:ByteArray,needle:ByteArray):Boolean{
        if(needle.isEmpty() || data.size<needle.size)return false
        outer@for(offset in 0..data.size-needle.size){for(i in needle.indices)if(data[offset+i]!=needle[i])continue@outer;return true}
        return false
    }
}
