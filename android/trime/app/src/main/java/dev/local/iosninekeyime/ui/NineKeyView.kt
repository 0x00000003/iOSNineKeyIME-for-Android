// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.ui

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.TextView
import dev.local.iosninekeyime.state.*

/** Provisional project layout. No iOS geometry/font/assets or pixel equivalence is claimed. */
class NineKeyView(context:Context, private val send:(Event,Boolean)->Unit,
                  private val hide:()->Unit={}, private val togglePrivacy:()->Unit={}, private val retry:()->Unit={}, private val switchIme:()->Unit={}) : ViewGroup(context) {
    private val density=resources.displayMetrics.density
    private var state=KeyboardState()
    private var shift=false
    private var lastStamp=Stamp(0,0)
    private var map=HitMap(emptyList())
    private val touches=TouchProtocol(map)
    private val actions=mutableMapOf<String,()->Unit>()
    private val geometry=mutableMapOf<String,KeyBox>()
    private val handler=Handler(Looper.getMainLooper())
    private var pointerDown=false
    private var contentHeight=310f
    private var currentWidth=0f
    private var safeLeft=0
    private var safeRight=0
    private var safeBottom=0
    private val dark get()=resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK==Configuration.UI_MODE_NIGHT_YES
    private val repeater=object:Runnable {
        override fun run() {
            if(!pointerDown)return
            touches.tick(SystemClock.uptimeMillis())?.let {activate(it)}
            handler.postDelayed(this,16)
        }
    }
    init { importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_YES;setWillNotDraw(false) }
    fun render(value:KeyboardState) {
        if(value.stamp.session>lastStamp.session) {touches.startSession(value.stamp.session);stopRepeating();shift=false}
        lastStamp=value.stamp;state=value
        if(currentWidth>0)buildKeys(currentWidth)
        requestLayout()
    }
    fun cancelTouches() {touches.cancel();stopRepeating()}
    private fun stopRepeating(){pointerDown=false;handler.removeCallbacks(repeater)}
    private fun activate(a:KeyActivation) {
        if(a.session!=state.stamp.session)return
        if(a.key=="delete")send(Event.Backspace,a.repeated) else actions[a.key]?.invoke()
    }
    private fun addLabel(id:String,label:String,box:KeyBox,action:(()->Unit)?=null,small:Boolean=false,enabled:Boolean=true) {
        val key=TextView(context).apply {
            tag=id;text=label;textSize=if(small)14f else 18f
            gravity=android.view.Gravity.CENTER;maxLines=1;ellipsize=TextUtils.TruncateAt.END
            setTextColor(if(dark)Color.WHITE else Color.BLACK)
            if(action!=null) {
                isEnabled=enabled;isFocusable=enabled;isClickable=enabled;alpha=if(enabled)1f else 0.45f
                contentDescription=label
                accessibilityDelegate=object:View.AccessibilityDelegate(){override fun onInitializeAccessibilityNodeInfo(host:View,info:AccessibilityNodeInfo){super.onInitializeAccessibilityNodeInfo(host,info);info.className=Button::class.java.name}}
                if(enabled)setOnClickListener {touches.accessibilityActivate(id,state.stamp.session)?.let {activate(it)}}
            }
            background=GradientDrawable().apply {setColor(if(dark)0xff515155.toInt() else Color.WHITE);cornerRadius=6*density}
        }
        val width=((box.right-box.left)*density).toInt();val height=((box.bottom-box.top)*density).toInt()
        addView(key,LayoutParams(width,height))
        // A second touch can arrive before the next layout frame after a state update.
        key.measure(MeasureSpec.makeMeasureSpec(width,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(height,MeasureSpec.EXACTLY))
        key.layout(safeLeft+(box.left*density).toInt(),(box.top*density).toInt(),safeLeft+(box.right*density).toInt(),(box.bottom*density).toInt())
        if(action!=null && enabled)actions[id]=action
    }
    private fun buildKeys(width:Float) {
        removeAllViews();actions.clear();geometry.clear()
        setBackgroundColor(if(dark)0xff2c2c2e.toInt() else 0xffd2d5db.toInt())
        val keys=mutableListOf<KeyBox>()
        fun key(id:String,label:String,l:Float,t:Float,r:Float,b:Float,small:Boolean=false,repeats:Boolean=false,enabled:Boolean=true,action:()->Unit) {
            val box=KeyBox(id,l,t,r,b,repeats);geometry[id]=box;if(enabled)keys+=box;addLabel(id,label,box,action,small,enabled)
        }
        val status=when {
            state.errorCode!=null -> when(state.errorCode){"E_HOST_PENDING"->"宿主未接收，请恢复连接后重试";"E_NO_CANDIDATE"->"无可用候选，可修改或取消";else->"输入暂不可用（${state.errorCode}）"}
            state.sensitive->"私密 · 直接输入，不学习"
            state.awaiting->"正在处理…"
            !state.ready && state.layout==Layout.CHINESE->"正在准备固定离线资源…"
            state.composing->state.preedit.ifEmpty {state.input}
            else->"九键 · 临时布局"
        }
        val info=TextView(context).apply {tag="status";text=status;textSize=13f;maxLines=1;ellipsize=TextUtils.TruncateAt.END;gravity=android.view.Gravity.CENTER;setTextColor(if(dark)Color.WHITE else Color.DKGRAY)}
        addView(info,LayoutParams((width*density).toInt(),(24*density).toInt()))
        if(state.bar==Bar.PUNCTUATION) {
            val cell=(width-12)/KeyboardReducer.punctuation.size
            KeyboardReducer.punctuation.forEachIndexed {i,p->key("punct-$i",p,6+i*cell,27f,6+(i+1)*cell-2,75f){send(Event.Punctuation(p),false)}}
        } else if(state.bar==Bar.CANDIDATES && state.candidates.isNotEmpty()) {
            val count=if(state.expanded)KeyboardReducer.CANDIDATE_PAGE_SIZE else 3
            val candidates=state.candidates.take(count)
            val cols=if(state.expanded)4 else 3
            val usable=if(state.expanded)width-12 else width-66
            val cell=usable/cols
            candidates.forEachIndexed {i,c->
                key("candidate-${c.id.stamp.session}-${c.id.stamp.revision}-${c.id.index}",c.text,6+(i%cols)*cell,27f+(i/cols)*50,6+(i%cols+1)*cell-4,75f+(i/cols)*50,true,enabled=!state.awaiting){send(Event.Choose(c.id),false)}
            }
            if(!state.expanded)key("expand","展开",width-54,27f,width-6,75f,true){send(Event.Expand,false)}
        } else {
            key("bar-info",when(state.bar){Bar.NO_CANDIDATES->"无候选 · 修改或取消";Bar.PRIVATE->"不读取周围文字";Bar.UNAVAILABLE->if(state.errorCode!=null)"引擎未就绪 · 点此重试" else "正在部署离线数据";else->"英文 / 数字 / 符号"},6f,27f,width-6,75f,true){if(state.errorCode!=null)retry()}
        }
        if(state.expanded) {
            val top=282f;val cell=(width-30)/4
            key("previous","上一页",6f,top,6+cell,top+48,true,enabled=!state.awaiting&&state.candidateOffset>0){send(Event.CandidatePage(-1),false)}
            key("next","下一页",12+cell,top,12+2*cell,top+48,true,enabled=!state.awaiting&&state.hasMoreCandidates){send(Event.CandidatePage(1),false)}
            key("collapse","收起",18+2*cell,top,18+3*cell,top+48,true,enabled=!state.awaiting){send(Event.Expand,false)}
            key("cancel","取消",24+3*cell,top,width-6,top+48,true){send(Event.Cancel,false)}
            contentHeight=336f
        } else {
            val top=81f
            val labels=when(state.layout){
                Layout.CHINESE->listOf("，","ABC","DEF","GHI","JKL","MNO","PQRS","TUV","WXYZ")
                Layout.NUMBER->listOf("1","2","3","4","5","6","7","8","9",".","0","-")
                Layout.SYMBOLS->listOf("，","。","？","！","、","：","；","（","）","@","#","/")
                Layout.LATIN->"qwertyuiopasdfghjklzxcvbnm".map {if(shift)it.uppercaseChar().toString() else it.toString()}
            }
            val cols=if(state.layout==Layout.LATIN)7 else 3
            val rows=(labels.size+cols-1)/cols
            val cell=(width-72-(cols-1)*6)/cols
            labels.forEachIndexed {i,label->
                val l=6+(i%cols)*(cell+6);val t=top+(i/cols)*58
                val id=if(state.layout==Layout.CHINESE && i>0)"digit-${i+1}" else "text-$i"
                key(id,label,l,t,l+cell,t+52){
                    if(state.layout==Layout.CHINESE) {if(i==0)send(Event.Punctuation("，"),false)else send(Event.Digit(i+1),false)}
                    else send(Event.Text(label),false)
                }
            }
            key("delete","退格",width-54,top,width-6,top+52,true,true){send(Event.Backspace,false)}
            key("cancel","取消",width-54,top+58,width-6,top+110,true){send(Event.Cancel,false)}
            key("privacy",if(state.sensitive)"私密开" else "私密关",width-54,top+116,width-6,top+168,true){togglePrivacy()}
            if(state.layout==Layout.LATIN)key("shift",if(shift)"小写" else "大写",width-54,top+174,width-6,top+226,true){shift=!shift;buildKeys(width);requestLayout()}
            val bottom=top+rows*58
            val labels2=listOf("符号","123",if(state.layout==Layout.LATIN)"中文" else "ABC","空格",when(state.action){EditorAction.NEWLINE->"换行";EditorAction.DONE->"完成";EditorAction.SEND->"发送";EditorAction.SEARCH->"搜索";EditorAction.NEXT->"下一项";EditorAction.GO->"前往"},"切换","收起")
            val cell2=(width-48)/7
            labels2.forEachIndexed {i,label->val l=6+i*(cell2+6)
                key("bottom-$i",label,l,bottom,l+cell2,bottom+52,true){when(i){0->send(Event.ChangeLayout(Layout.SYMBOLS),false);1->send(Event.ChangeLayout(Layout.NUMBER),false);2->send(Event.ChangeLayout(if(state.layout==Layout.LATIN)Layout.CHINESE else Layout.LATIN),false);3->send(Event.Space,false);4->send(Event.Enter,false);5->switchIme();6->hide()}}
            }
            contentHeight=bottom+58
        }
        val next=HitMap(keys)
        touches.updateMap(next);map=next
    }
    override fun onApplyWindowInsets(insets:WindowInsets):WindowInsets {
        val left:Int;val right:Int;val bottom:Int
        if(Build.VERSION.SDK_INT>=30){
            val bars=insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            left=bars.left;right=bars.right;bottom=bars.bottom
        }else{
            @Suppress("DEPRECATION")
            left=insets.systemWindowInsetLeft
            @Suppress("DEPRECATION")
            right=insets.systemWindowInsetRight
            @Suppress("DEPRECATION")
            bottom=insets.systemWindowInsetBottom
        }
        if(left!=safeLeft || right!=safeRight || bottom!=safeBottom){
            cancelTouches();safeLeft=left;safeRight=right;safeBottom=bottom;requestLayout()
        }
        return super.onApplyWindowInsets(insets)
    }
    override fun onMeasure(widthMeasureSpec:Int,heightMeasureSpec:Int) {
        val w=MeasureSpec.getSize(widthMeasureSpec)
        val dp=(w-safeLeft-safeRight).coerceAtLeast(0)/density
        if(dp!=currentWidth && dp>200){currentWidth=dp;cancelTouches();buildKeys(dp)}
        for(i in 0 until childCount){val v=getChildAt(i);v.measure(MeasureSpec.makeMeasureSpec(v.layoutParams.width,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(v.layoutParams.height,MeasureSpec.EXACTLY))}
        setMeasuredDimension(w,resolveSize((contentHeight*density).toInt()+safeBottom,heightMeasureSpec))
    }
    override fun onLayout(changed:Boolean,l:Int,t:Int,r:Int,b:Int) {
        for(i in 0 until childCount){val v=getChildAt(i);val box=geometry[v.tag]
            if(box!=null)v.layout(safeLeft+(box.left*density).toInt(),(box.top*density).toInt(),safeLeft+(box.right*density).toInt(),(box.bottom*density).toInt())
            else v.layout(safeLeft,0,width-safeRight,(24*density).toInt())
        }
    }
    override fun onInterceptTouchEvent(event:MotionEvent)=true
    override fun onTouchEvent(event:MotionEvent):Boolean {
        val index=event.actionIndex;val pointer=event.getPointerId(index);val x=(event.getX(index)-safeLeft)/density;val y=event.getY(index)/density
        when(event.actionMasked) {
            MotionEvent.ACTION_DOWN->{touches.down(pointer,x,y,event.eventTime);pointerDown=true;handler.post(repeater)}
            MotionEvent.ACTION_MOVE->{for(i in 0 until event.pointerCount)touches.move(event.getPointerId(i),(event.getX(i)-safeLeft)/density,event.getY(i)/density)}
            MotionEvent.ACTION_UP->{stopRepeating();touches.up(pointer,x,y)?.let {activate(it)};performClick()}
            MotionEvent.ACTION_POINTER_UP->touches.up(pointer,x,y)?.let {activate(it)}
            MotionEvent.ACTION_CANCEL->cancelTouches()
        }
        return true
    }
    override fun performClick():Boolean {super.performClick();return true}
    override fun onDetachedFromWindow(){cancelTouches();super.onDetachedFromWindow()}
}
