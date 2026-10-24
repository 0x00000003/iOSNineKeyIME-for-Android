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

/** Production view/controller/serial JNI with synthetic host assertions, never a system-IME claim. */
object PinyinBoundaryProbe {
    fun run(instrumentation:Instrumentation):JSONObject {
        val context=instrumentation.targetContext
        val guards=PinyinNativeGuards.run(context)
        if(!guards.getBoolean("pass"))return JSONObject().put("native_guards",guards).put("pass",false)
        val activity=instrumentation.startActivitySync(Intent(context,EditorHarnessActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))as EditorHarnessActivity
        val handler=Handler(Looper.getMainLooper())
        val engine=SerialRimeEngine(context,File(context.noBackupFilesDir,"m2/pinyin/${UUID.randomUUID()}")){task->handler.post{task()}}
        val host=HostEditorAdapter()
        lateinit var controller:KeyboardController
        lateinit var keyboard:NineKeyView
        var created=false;var viewCreated=false;var commits=0;var fetches=0;var stage="setup";var requestedOffset=0
        val selected=mutableListOf<Int>()
        fun main(block:()->Unit){var error:Throwable?=null;instrumentation.runOnMainSync{try{block()}catch(t:Throwable){error=t}};error?.let{throw it}}
        fun waitFor(test:()->Boolean){val end=System.nanoTime()+TimeUnit.SECONDS.toNanos(60);while(true){var done=false;main{done=test()};if(done)return;check(System.nanoTime()<end){"E_PINYIN_TIMEOUT"};Thread.sleep(10)}}
        fun tap(tag:String){
            val child=checkNotNull(keyboard.findViewWithTag<View>(tag)){"E_PINYIN_KEY"}
            val x=(child.left+child.right)/2f;val y=(child.top+child.bottom)/2f
            for(action in listOf(MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP)){
                val now=android.os.SystemClock.uptimeMillis();val event=MotionEvent.obtain(now,now,action,x,y,0)
                try{keyboard.dispatchTouchEvent(event)}finally{event.recycle()}
            }
        }
        fun offset(value:Int){requestedOffset=value;waitFor{!controller.state.awaiting && controller.state.candidateOffset==value}}
        val result=JSONObject().put("scope","production view/controller/serial JNI pinyin component; NOT system IME/multi-host or all-path privacy acceptance")
        result.put("native_guards",guards)
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
                    override fun deleteAsync(session:Long,sensitive:Boolean,reply:(Boolean)->Unit){host.deleteAsync(session){reply(it==dev.local.iosninekeyime.editor.EditResult.ACCEPTED)}}
                    override fun action(session:Long,action:EditorAction){host.action(session,action)}
                    override fun end(session:Long){host.end(session)}
                }
                controller=KeyboardController(native,port,onState={keyboard.render(it)},checkOwner={check(Looper.myLooper()==Looper.getMainLooper())})
                created=true;controller.start(1,false,Layout.CHINESE,EditorAction.DONE)
            }
            waitFor{controller.state.ready};stage="first-query"
            fun type(digits:String){main{for(ch in digits)tap("digit-$ch")};waitFor{!controller.state.awaiting && controller.state.input==digits}}
            fun open(){main{tap("pinyin")};waitFor{!controller.state.awaiting}}
            fun choose(syllable:String){
                main{
                    val index=controller.state.pinyinChoices.indexOfFirst{it.syllable==syllable}
                    check(index>=0){"E_PINYIN_OPTION"}
                    repeat(index/8){tap("pinyin-more")}
                    val stamp=controller.state.stamp
                    tap("pinyin-${stamp.session}-${stamp.revision}-$index")
                }
                waitFor{!controller.state.awaiting}
            }
            fun word(text:String){
                main{tap("popup-close")}
                var found=false
                for(page in 0..24){
                    main{
                        val candidate=controller.state.candidates.firstOrNull{it.text==text}
                        if(candidate!=null){
                            found=true
                            val tag="candidate-${candidate.id.stamp.session}-${candidate.id.stamp.revision}-${candidate.id.index}"
                            if(keyboard.findViewWithTag<View>(tag)==null)tap("expand")
                            tap(tag)
                        }else if(controller.state.hasMoreCandidates){
                            if(!controller.state.expanded)tap("expand")
                            tap("next")
                        }
                    }
                    waitFor{!controller.state.awaiting}
                    if(found)break
                }
                check(found){"E_PINYIN_WORD"}
            }
            type("64426");open()
            main{check(controller.state.pinyinChoices.any{it.syllable=="ni"}&&controller.state.pinyinChoices.any{it.syllable=="mi"});check(commits==0)}
            stage="first-syllable";choose("ni")
            main{check(controller.state.input=="ni'426"&&commits==0)}
            stage="second-syllable";choose("hao")
            main{check(controller.state.input=="ni'hao"&&commits==0)}
            word("你好");waitFor{controller.state.bar==Bar.PUNCTUATION}
            main{check(activity.editor.text.toString()=="你好"&&commits==1)}
            result.put("two_syllables_exact_once_host_commit",true)
            stage="alternative";type("64426");open();choose("mi")
            main{check(controller.state.input=="mi'426"&&controller.state.candidates.none{it.text=="你好"}&&commits==1);tap("pinyin-cancel")}
            waitFor{!controller.state.awaiting&&!controller.state.composing}
            main{check(controller.state.pinyinChoices.isEmpty())}
            result.put("alternative_prefix_changes_candidates_without_commit",true).put("cancel_clears_choices",true)
            stage="partial-word";type("64426744543");open();word("你好")
            waitFor{!controller.state.awaiting&&controller.state.composing}
            var beforePartial="";var beforeCalls=0
            main{beforePartial=activity.editor.text.toString();beforeCalls=commits}
            stage="partial-query";open();stage="partial-shi";choose("shi")
            main{check(controller.state.input=="64426'shi'543"&&activity.editor.text.toString()==beforePartial&&commits==beforeCalls)}
            stage="partial-jie";choose("jie")
            main{check(controller.state.input=="64426'shi'jie"&&activity.editor.text.toString()==beforePartial&&commits==beforeCalls)}
            stage="partial-final-word";word("世界");waitFor{!controller.state.awaiting&&!controller.state.composing}
            main{check(activity.editor.text.toString()=="你好你好世界"&&commits==beforeCalls+1)}
            result.put("selected_prefix_retained_without_duplicate_commit",true)
            stage="sensitive-cutover";type("64426");open()
            main{check(controller.state.pinyinChoices.isNotEmpty());controller.end(1);check(controller.state.pinyinChoices.isEmpty());controller.start(2,true,Layout.CHINESE,EditorAction.DONE)}
            waitFor{engine.quiescentSession.get()==2L}
            main{check(controller.state.layout==Layout.LATIN);controller.submit(Event.RequestPinyin);check(controller.state.pinyinChoices.isEmpty()&&!controller.state.awaiting);controller.end(2)}
            result.put("field_end_clears_choices_and_sensitive_query_disabled",true).put("pass",true)
            return result
        }catch(error:Throwable){
            result.put("failure_code",error.message?.takeIf{it.matches(Regex("E_PINYIN_[A-Z_]+"))}?:"E_PINYIN_ASSERTION")
            if(created)main{result.put("raw_length",controller.state.input.length).put("awaiting",controller.state.awaiting).put("ready",controller.state.ready).put("option_count",controller.state.pinyinChoices.size).put("error_code",controller.state.errorCode)}
            return result.put("failure_stage",stage).put("pass",false)
        }finally{
            main{if(viewCreated)keyboard.cancelTouches();if(created)controller.end(controller.state.stamp.session)}
            val closed=CountDownLatch(1);engine.close{closed.countDown()};check(closed.await(15,TimeUnit.SECONDS)){"E_ENGINE_CLOSE"}
            main{activity.finish()}
        }
    }
}
