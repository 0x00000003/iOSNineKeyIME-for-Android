// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.ime

import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.CursorAnchorInfo
import com.osfans.trime.ime.core.LifecycleInputMethodService
import dev.local.iosninekeyime.editor.HostEditorAdapter
import dev.local.iosninekeyime.engine.SerialRimeEngine
import dev.local.iosninekeyime.privacy.EditorPolicy
import dev.local.iosninekeyime.privacy.DirectLayout
import dev.local.iosninekeyime.state.*
import dev.local.iosninekeyime.ui.NineKeyView
import java.io.File

/** Thin offline front end on Trime's lifecycle base and pinned JNI. No legacy daemon/preferences. */
class NineKeyInputMethodService : LifecycleInputMethodService() {
    private val handler=Handler(Looper.getMainLooper())
    private val host=HostEditorAdapter(onCommitAttempt={length ->
        val expectedSession=session
        val oldStart=selectionStart;val oldEnd=selectionEnd;val oldQueue=expectedSelections.toList()
        val position=if(oldStart>=0 && oldEnd>=0)minOf(oldStart,oldEnd)+length else -1
        if(position>=0){expectedSelections.addLast(position);while(expectedSelections.size>64)expectedSelections.removeFirst();selectionStart=position;selectionEnd=position}
        val finish:(Boolean)->Unit={accepted -> if(!accepted && expectedSession==session){expectedSelections.clear();expectedSelections.addAll(oldQueue);selectionStart=oldStart;selectionEnd=oldEnd}}
        finish
    })
    private lateinit var engine:SerialRimeEngine
    private lateinit var controller:KeyboardController
    private var keyboard:NineKeyView?=null
    private var session=0L
    private var fieldActive=false
    private var userPrivate=false
    private var info:EditorInfo?=null
    private var selectionStart=-1
    private var selectionEnd=-1
    private val expectedSelections=ArrayDeque<Int>()

    override fun onCreate() {
        super.onCreate()
        engine=SerialRimeEngine(this,File(noBackupFilesDir,"ime/normal")){task->handler.post {task()}}
        val port=object:KeyboardHostPort {
            override val pendingCount get()=host.pendingCount
            override fun begin(session:Long,sensitive:Boolean) {
                val current=checkNotNull(info)
                host.begin(session,currentInputConnection,EditorPolicy.from(current.inputType,current.imeOptions,userPrivate),current)
            }
            override fun commit(effect:Effect.Commit)=host.commit(effect)
            override fun retry(session:Long)=host.retry(session)
            override fun deleteAsync(session:Long,sensitive:Boolean,reply:(Boolean)->Unit){host.deleteAsync(session){reply(it==dev.local.iosninekeyime.editor.EditResult.ACCEPTED)}}
            override fun action(session:Long,action:EditorAction){host.action(session,action)}
            override fun end(session:Long){host.end(session)}
        }
        controller=KeyboardController(engine,port,onState={keyboard?.render(it)},checkOwner={check(Looper.myLooper()==Looper.getMainLooper()){"E_OWNER_THREAD"}})
    }
    override fun onCreateInputView():View {
        return NineKeyView(this,
            send={event,repeated->if(repeated)controller.submitRepeat(event)else controller.submit(event)},
            hide={keyboard?.cancelTouches();requestHideSelf(0)},
            togglePrivacy={userPrivate=!userPrivate;info?.let {startField(it)}},
            retry={
                if(host.pendingCount>0){host.reconnect(session,currentInputConnection);controller.retryHost(session)}
                else info?.let {startField(it)}
            },
            switchIme={keyboard?.cancelTouches();if(android.os.Build.VERSION.SDK_INT>=28)switchToNextInputMethod(false)else(getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager).showInputMethodPicker()},
        ).also {keyboard=it;it.render(controller.state)}
    }
    override fun onStartInput(attribute:EditorInfo?,restarting:Boolean) {
        super.onStartInput(attribute,restarting)
        if(attribute!=null)startField(attribute)
    }
    override fun onStartInputView(attribute:EditorInfo?,restarting:Boolean) {
        super.onStartInputView(attribute,restarting)
        if(!fieldActive && attribute!=null)startField(attribute)
        keyboard?.render(controller.state)
    }
    private fun startField(attribute:EditorInfo) {
        keyboard?.cancelTouches()
        info=attribute;fieldActive=true;session++
        selectionStart=attribute.initialSelStart;selectionEnd=attribute.initialSelEnd;expectedSelections.clear()
        val policy=EditorPolicy.from(attribute.inputType,attribute.imeOptions,userPrivate)
        val variation=attribute.inputType and InputType.TYPE_MASK_VARIATION
        val latin=policy.sensitive || variation in setOf(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,InputType.TYPE_TEXT_VARIATION_URI)
        val layout=if(policy.directLayout==DirectLayout.NUMBER)Layout.NUMBER else if(latin)Layout.LATIN else Layout.CHINESE
        val action=if(attribute.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION!=0)EditorAction.NEWLINE else when(attribute.imeOptions and EditorInfo.IME_MASK_ACTION){
            EditorInfo.IME_ACTION_DONE->EditorAction.DONE
            EditorInfo.IME_ACTION_SEND->EditorAction.SEND
            EditorInfo.IME_ACTION_SEARCH->EditorAction.SEARCH
            EditorInfo.IME_ACTION_NEXT->EditorAction.NEXT
            EditorInfo.IME_ACTION_GO->EditorAction.GO
            else->EditorAction.NEWLINE
        }
        controller.start(session,policy.sensitive,layout,action)
    }
    override fun onUpdateSelection(oldSelStart:Int,oldSelEnd:Int,newSelStart:Int,newSelEnd:Int,candidatesStart:Int,candidatesEnd:Int) {
        super.onUpdateSelection(oldSelStart,oldSelEnd,newSelStart,newSelEnd,candidatesStart,candidatesEnd)
        if(!fieldActive)return
        val own=if(newSelStart==newSelEnd)expectedSelections.indexOf(newSelStart)else -1
        if(own>=0){repeat(own+1){expectedSelections.removeFirst()}}
        else if(newSelStart!=selectionStart || newSelEnd!=selectionEnd){expectedSelections.clear();keyboard?.cancelTouches();controller.submit(Event.Cancel)}
        selectionStart=newSelStart;selectionEnd=newSelEnd
        host.updateSelection(session,newSelStart,newSelEnd)
    }
    override fun onUpdateCursorAnchorInfo(cursorAnchorInfo:CursorAnchorInfo?) {
        super.onUpdateCursorAnchorInfo(cursorAnchorInfo)
        if(fieldActive && cursorAnchorInfo!=null)host.updateCursorAnchor(session,cursorAnchorInfo)
    }
    private fun endField() {
        keyboard?.cancelTouches()
        if(fieldActive){fieldActive=false;controller.end(session)}
        expectedSelections.clear()
    }
    override fun onFinishInputView(finishingInput:Boolean){endField();super.onFinishInputView(finishingInput)}
    override fun onFinishInput(){endField();info=null;super.onFinishInput()}
    override fun onEvaluateFullscreenMode()=false
    override fun onDestroy(){endField();keyboard=null;engine.close();super.onDestroy()}
}
