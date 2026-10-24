// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.probe

import android.app.Instrumentation
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnectionWrapper
import android.widget.LinearLayout
import dev.local.iosninekeyime.editor.EditorHarnessActivity
import dev.local.iosninekeyime.editor.HostEditorAdapter
import dev.local.iosninekeyime.engine.SerialRimeEngine
import dev.local.iosninekeyime.privacy.EditorPolicy
import dev.local.iosninekeyime.state.*
import dev.local.iosninekeyime.ui.NineKeyView
import java.io.File
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.json.JSONObject

/** Actual MotionEvent -> serial JNI pages beyond 100 -> real synthetic host oracle. */
object CandidatePagingBoundaryProbe {
    fun run(instrumentation:Instrumentation):JSONObject {
        val context=instrumentation.targetContext
        val activity=instrumentation.startActivitySync(Intent(context,EditorHarnessActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))as EditorHarnessActivity
        val handler=Handler(Looper.getMainLooper())
        val engine=SerialRimeEngine(context,File(context.noBackupFilesDir,"m2/paging/${UUID.randomUUID()}")){task->handler.post{task()}}
        val host=HostEditorAdapter()
        lateinit var controller:KeyboardController
        lateinit var keyboard:NineKeyView
        var created=false;var viewCreated=false;var commits=0;var fetches=0;var stage="setup";var requestedOffset=0
        val selected=mutableListOf<Int>()
        fun main(block:()->Unit){var error:Throwable?=null;instrumentation.runOnMainSync{try{block()}catch(t:Throwable){error=t}};error?.let{throw it}}
        fun waitFor(test:()->Boolean){val end=System.nanoTime()+TimeUnit.SECONDS.toNanos(60);while(true){var done=false;main{done=test()};if(done)return;check(System.nanoTime()<end){"E_PAGING_TIMEOUT"};Thread.sleep(10)}}
        fun tap(tag:String){
            val child=checkNotNull(keyboard.findViewWithTag<View>(tag)){"E_PAGING_KEY"}
            val x=(child.left+child.right)/2f;val y=(child.top+child.bottom)/2f
            for(action in listOf(MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP)){
                val now=android.os.SystemClock.uptimeMillis();val event=MotionEvent.obtain(now,now,action,x,y,0)
                try{keyboard.dispatchTouchEvent(event)}finally{event.recycle()}
            }
        }
        fun offset(value:Int){requestedOffset=value;waitFor{!controller.state.awaiting && controller.state.candidateOffset==value}}
        val result=JSONObject().put("scope","actual serial JNI bounded native pages and synthetic real-host oracle; NOT enabled IME/system lifecycle, visual or all-path privacy acceptance")
        try {
            main {
                val root=LinearLayout(context).apply{orientation=LinearLayout.VERTICAL}
                (activity.editor.parent as ViewGroup).removeView(activity.editor)
                root.addView(activity.editor,LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f))
                keyboard=NineKeyView(context,send={event,repeated->if(repeated)controller.submitRepeat(event)else controller.submit(event)})
                viewCreated=true
                root.addView(keyboard,LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));activity.setContentView(root)
                activity.editor.requestFocus()
                val info=EditorInfo();val actual=checkNotNull(activity.editor.onCreateInputConnection(info))
                val connection=object:InputConnectionWrapper(actual,false){override fun commitText(text:CharSequence?,newCursorPosition:Int):Boolean{commits++;return super.commitText(text,newCursorPosition)}}
                val native=object:KeyboardEnginePort{
                    override fun begin(session:Long,sensitive:Boolean,reply:(Boolean)->Unit)=engine.begin(session,sensitive,reply)
                    override fun end(session:Long)=engine.end(session)
                    override fun execute(effect:Effect,reply:(Event.Snapshot?)->Unit){if(effect is Effect.FetchCandidates)fetches++;if(effect is Effect.Select)selected+=effect.index;engine.execute(effect,reply)}
                }
                val port=object:KeyboardHostPort{
                    override val pendingCount get()=host.pendingCount
                    override fun begin(session:Long,sensitive:Boolean){host.begin(session,connection,EditorPolicy.from(info.inputType,info.imeOptions,false))}
                    override fun commit(effect:Effect.Commit)=host.commit(effect)
                    override fun retry(session:Long)=host.retry(session)
                    override fun delete(session:Long,sensitive:Boolean){host.delete(session)}
                    override fun action(session:Long,action:EditorAction){host.action(session,action)}
                    override fun end(session:Long){host.end(session)}
                }
                controller=KeyboardController(native,port,onState={keyboard.render(it)},checkOwner={check(Looper.myLooper()==Looper.getMainLooper())})
                created=true;controller.start(1,false,Layout.CHINESE,EditorAction.DONE)
            }
            waitFor{controller.state.ready};stage="first-page"
            main{tap("digit-6");tap("digit-4")};waitFor{controller.state.input=="64"&&!controller.state.awaiting}
            var old:CandidateId?=null
            main{old=controller.state.candidates.first().id;tap("expand");check(!keyboard.findViewWithTag<View>("previous").isEnabled)}
            stage="disabled-page-geometry"
            main{
                keyboard.measure(View.MeasureSpec.makeMeasureSpec(keyboard.width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED))
                keyboard.layout(keyboard.left,keyboard.top,keyboard.left+keyboard.measuredWidth,keyboard.top+keyboard.measuredHeight)
                val previous=keyboard.findViewWithTag<View>("previous")
                check(previous.top>=200*context.resources.displayMetrics.density)
                tap("previous");check(fetches==0 && controller.state.candidateOffset==0)
            }
            result.put("disabled_previous_keeps_geometry_and_has_no_hit_target",true)
            stage="forward"
            for(next in 20..120 step 20){main{tap("next")};offset(next);main{check(controller.state.candidates.size<=20 && controller.state.candidates.first().id.index==next)}}
            main{check(commits==0);controller.submit(Event.Choose(checkNotNull(old)));check(commits==0&&selected.isEmpty())}
            result.put("late_old_page_candidate_rejected",true)
            stage="backward";main{tap("previous")};offset(100);main{tap("next")};offset(120)
            var expected=""
            main {
                val candidate=controller.state.candidates.first();expected=candidate.text
                check(candidate.id.index==120 && commits==0)
                tap("candidate-${candidate.id.stamp.session}-${candidate.id.stamp.revision}-${candidate.id.index}")
            }
            waitFor{controller.state.bar==Bar.PUNCTUATION&&!controller.state.awaiting}
            main{check(activity.editor.text.toString()==expected&&commits==1&&selected.single()==120)}
            result.put("global_index_120_exact_once_host_commit",true).put("native_forward_backward_pages",true)
            stage="collapse";main{tap("digit-6");tap("digit-4")};waitFor{controller.state.input=="64"&&!controller.state.awaiting}
            main{tap("expand");tap("next")};offset(20);main{tap("collapse")};offset(0)
            main{check(!controller.state.expanded && controller.state.candidates.first().id.index==0 && commits==1)}
            result.put("collapse_reloads_global_first_page",true)
            stage="global-primary"
            var primary=""
            main{primary=checkNotNull(controller.state.primaryCandidateText);tap("expand");tap("next")};offset(20)
            main{controller.submit(Event.Space)};waitFor{controller.state.bar==Bar.PUNCTUATION&&!controller.state.awaiting}
            main{check(activity.editor.text.toString()==expected+primary&&commits==2&&selected==listOf(120,0))}
            result.put("later_page_space_confirms_global_primary",true).put("native_page_fetches",fetches)
                .put("host_commit_calls",commits).put("max_resident_candidate_window",20).put("pass",true)
            return result
        }catch(error:Throwable){
            result.put("failure_stage",stage).put("requested_offset",requestedOffset).put("native_page_fetches",fetches)
                .put("failure_code",error.message?.takeIf{it.matches(Regex("E_PAGING_[A-Z_]+"))}?:"E_PAGING_ASSERTION")
            if(created)main{result.put("actual_offset",controller.state.candidateOffset).put("candidate_count",controller.state.candidates.size)
                .put("has_more",controller.state.hasMoreCandidates).put("awaiting",controller.state.awaiting)
                .put("expanded",controller.state.expanded).put("ready",controller.state.ready)}
            return result.put("pass",false)
        }
        finally{
            main{if(viewCreated)keyboard.cancelTouches();if(created)controller.end(1)}
            val closed=CountDownLatch(1);engine.close{closed.countDown()};check(closed.await(15,TimeUnit.SECONDS)){"E_ENGINE_CLOSE"}
            main{activity.finish()}
        }
    }
}
