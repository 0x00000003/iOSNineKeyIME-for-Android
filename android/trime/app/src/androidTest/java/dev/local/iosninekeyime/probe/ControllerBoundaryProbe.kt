// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.probe

import android.app.Instrumentation
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.LinearLayout
import dev.local.iosninekeyime.ui.NineKeyView
import android.view.inputmethod.*
import dev.local.iosninekeyime.editor.EditorHarnessActivity
import dev.local.iosninekeyime.editor.HostEditorAdapter
import dev.local.iosninekeyime.engine.SerialRimeEngine
import dev.local.iosninekeyime.privacy.EditorPolicy
import dev.local.iosninekeyime.state.*
import java.io.File
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.json.JSONObject

/** Synthetic JNI -> reducer/controller -> actual EditText integration; never enables an IME. */
object ControllerBoundaryProbe {
    @androidx.annotation.RequiresApi(31)
    fun run(instrumentation: Instrumentation): JSONObject {
        val context=instrumentation.targetContext
        val activity=instrumentation.startActivitySync(Intent(context,EditorHarnessActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as EditorHarnessActivity
        val handler=Handler(Looper.getMainLooper())
        val profile=File(context.noBackupFilesDir,"m2/profiles/${UUID.randomUUID()}")
        val engine=SerialRimeEngine(context,profile) { task -> handler.post { task() } }
        val host=HostEditorAdapter()
        lateinit var connection:InputConnection
        lateinit var controller:KeyboardController
        lateinit var keyboard:NineKeyView
        var reads=0
        var privateField=false
        var commits=0
        var stage="setup"
        fun main(block:()->Unit) {
            var failure:Throwable?=null
            instrumentation.runOnMainSync { try {block()} catch(t:Throwable){failure=t} }

            failure?.let {throw it}
        }
        fun waitFor(condition:()->Boolean) {
            val deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(60)
            while(true) {
                var done=false;main {done=condition()}
                if(done)return
                check(System.nanoTime()<deadline){"E_CONTROLLER_TIMEOUT"}
                Thread.sleep(20)
            }
        }
        fun touch(tag:String,action:Int) {
            val v=checkNotNull(keyboard.findViewWithTag<android.view.View>(tag)){"E_VIEW_KEY"}
            val x=(v.left+v.right)/2f;val y=(v.top+v.bottom)/2f
            val now=android.os.SystemClock.uptimeMillis()
            val event=MotionEvent.obtain(now,now,action,x,y,0)
            try {keyboard.dispatchTouchEvent(event)} finally {event.recycle()}
        }
        fun tap(tag:String) {touch(tag,MotionEvent.ACTION_DOWN);touch(tag,MotionEvent.ACTION_UP)}
        fun type(digits:String) = main { digits.forEach {tap("digit-$it")} }
        val result=JSONObject().put("scope","serial JNI/controller/real EditText component; NOT enabled IME, system lifecycle or all-path privacy acceptance")
        try {
            main {
                val root=LinearLayout(context).apply {orientation=LinearLayout.VERTICAL}
                (activity.editor.parent as ViewGroup).removeView(activity.editor)
                root.addView(activity.editor,LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f))
                keyboard=NineKeyView(context,send={event,repeated->if(repeated)controller.submitRepeat(event)else controller.submit(event)})
                root.addView(keyboard,LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT))
                activity.setContentView(root)
                activity.editor.requestFocus()
                connection=checkNotNull(activity.editor.onCreateInputConnection(EditorInfo()))
                val guarded=object:InputConnectionWrapper(connection,false) {
                    override fun getSelectedText(flags:Int):CharSequence? {if(privateField)reads++;return super.getSelectedText(flags)}
                    override fun getTextBeforeCursor(n:Int,flags:Int):CharSequence? {if(privateField)reads++;return super.getTextBeforeCursor(n,flags)}
                    override fun getTextAfterCursor(n:Int,flags:Int):CharSequence? {if(privateField)reads++;return super.getTextAfterCursor(n,flags)}
                    override fun getExtractedText(request:ExtractedTextRequest?,flags:Int):ExtractedText? {if(privateField)reads++;return super.getExtractedText(request,flags)}
                    override fun getSurroundingText(beforeLength:Int,afterLength:Int,flags:Int):SurroundingText? {if(privateField)reads++;return super.getSurroundingText(beforeLength,afterLength,flags)}
                    override fun getCursorCapsMode(reqModes:Int):Int {if(privateField)reads++;return super.getCursorCapsMode(reqModes)}
                    override fun commitText(text:CharSequence?,newCursorPosition:Int):Boolean {commits++;return super.commitText(text,newCursorPosition)}
                }
                val port=object:KeyboardHostPort {
                    override val pendingCount get()=host.pendingCount
                    override fun begin(session:Long,sensitive:Boolean) {
                        privateField=sensitive
                        host.begin(session,guarded,EditorPolicy.from(InputType.TYPE_CLASS_TEXT,if(sensitive)EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING else 0,false))
                    }
                    override fun commit(effect:Effect.Commit)=host.commit(effect)
                    override fun retry(session:Long)=host.retry(session)
                    override fun delete(session:Long,sensitive:Boolean) {host.delete(session)}
                    override fun action(session:Long,action:EditorAction) {host.action(session,action)}
                    override fun end(session:Long) {host.end(session)}
                }
                controller=KeyboardController(engine,port,onState={keyboard.render(it)},checkOwner={check(Looper.myLooper()==Looper.getMainLooper()){"E_OWNER_THREAD"}})
                controller.start(1,false,Layout.CHINESE,EditorAction.DONE)
            }
            waitFor {keyboard.width>0 && keyboard.findViewWithTag<android.view.View>("digit-6")?.width?.let {it>0}==true}
            // Send all five keys before waiting for native startup, preserving semantic order.
            stage="first-fast-touch"
            type("64426")
            waitFor {controller.state.input=="64426" && !controller.state.awaiting}
            main {
                result.put("fast_keys_preserved",activity.editor.text.isEmpty() && controller.state.candidates.any {it.text=="你好"})
                val candidate=controller.state.candidates.first {it.text=="你好"}
                tap("candidate-${candidate.id.stamp.session}-${candidate.id.stamp.revision}-${candidate.id.index}")
            }
            waitFor {controller.state.bar==Bar.PUNCTUATION}
            main {result.put("single_commit_and_idle_punctuation",activity.editor.text.toString()=="你好" && commits==1);tap("punct-1")}
            main {
                val image=android.graphics.Bitmap.createBitmap(keyboard.width,keyboard.height,android.graphics.Bitmap.Config.ARGB_8888)
                keyboard.draw(android.graphics.Canvas(image))
                File(context.cacheDir,"m2-provisional-view.png").outputStream().use {image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
                image.recycle()
                result.put("view_touches_drive_jni",true)
                val stamp=controller.state.stamp
                touch("digit-6",MotionEvent.ACTION_DOWN)
                val now=android.os.SystemClock.uptimeMillis()
                for(action in listOf(MotionEvent.ACTION_MOVE,MotionEvent.ACTION_UP)) {
                    val event=MotionEvent.obtain(now,now,action,-10f,-10f,0)
                    try{keyboard.dispatchTouchEvent(event)}finally{event.recycle()}
                }
                result.put("view_slide_out_cancel",controller.state.stamp==stamp && !controller.state.composing)
            }
            stage="confirm-punctuation"
            type("64426744543")
            waitFor {controller.state.input=="64426744543" && !controller.state.awaiting}
            main {controller.submit(Event.Punctuation("。"))}
            waitFor {controller.state.bar==Bar.PUNCTUATION}
            main {result.put("confirm_then_single_punctuation",activity.editor.text.toString()=="你好。你好世界。")}
            type("64426")
            main {controller.submit(Event.Cancel)}
            waitFor {controller.state.bar==Bar.PUNCTUATION}
            main {result.put("cancel_drops_queued_input",activity.editor.text.toString()=="你好。你好世界。")}
            type("64426")
            waitFor {controller.state.input=="64426" && !controller.state.awaiting}
            main {
                val old=controller.state.candidates.first().id
                controller.start(2,true,Layout.LATIN,EditorAction.DONE)
                controller.submit(Event.Choose(old))
            }
            waitFor {engine.quiescentSession.get()==2L}
            stage="private-barrier"
            val before=ProbeFileSnapshot.fingerprint(profile)
            val busy=try { ProbeRunner.run(context);false } catch(t:IllegalStateException){t.message=="E_ENGINE_BUSY"}
            main {controller.submit(Event.Text("a"));controller.submit(Event.Digit(2));controller.submit(Event.Backspace)}
            main {
                result.put("new_private_field_rejects_old_candidate",activity.editor.text.toString()=="你好。你好世界。a")
                result.put("sensitive_context_reads",reads)
                result.put("probe_excluded_during_private_session",busy)
            }
            result.put("normal_profile_positive_userdb",profile.walkTopDown().any {it.name.contains(".userdb")})
                .put("normal_profile_unchanged_after_privacy_barrier",before==ProbeFileSnapshot.fingerprint(profile))
            main {controller.end(2);controller.submit(Event.Text("ignored"));result.put("ended_field_ignores_input",activity.editor.text.toString()=="你好。你好世界。a")}
            stage="held-delete-field-change"
            main {
                controller.start(3,true,Layout.LATIN,EditorAction.DONE)
                activity.editor.setText("AB");activity.editor.setSelection(2)
                touch("delete",MotionEvent.ACTION_DOWN)
            }
            waitFor {activity.editor.text.toString()=="A"}
            main {
                controller.start(4,true,Layout.LATIN,EditorAction.DONE)
                activity.editor.setText("12");activity.editor.setSelection(2)
            }
            Thread.sleep(600)
            main {touch("delete",MotionEvent.ACTION_UP);result.put("view_hold_cancelled_on_new_field",activity.editor.text.toString()=="12");controller.end(4)}
            val bools=listOf("view_touches_drive_jni","view_slide_out_cancel","view_hold_cancelled_on_new_field","fast_keys_preserved","single_commit_and_idle_punctuation","confirm_then_single_punctuation","cancel_drops_queued_input","new_private_field_rejects_old_candidate","probe_excluded_during_private_session","normal_profile_positive_userdb","normal_profile_unchanged_after_privacy_barrier","ended_field_ignores_input")
            result.put("pass",reads==0 && bools.all {result.getBoolean(it)})
            return result
        } catch (_:Throwable) {
            return result.put("pass",false).put("failed_stage",stage)
        } finally {
            val closed=CountDownLatch(1)
            main {engine.close {closed.countDown()};activity.finish()}
            check(closed.await(30,TimeUnit.SECONDS)){"E_ENGINE_CLOSE_TIMEOUT"}
        }
    }
}
