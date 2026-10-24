// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.ui

import android.content.Context
import android.content.res.Configuration
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.TextUtils
import android.view.*
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import dev.local.iosninekeyime.state.*
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/** Reference-derived panels with real reducer events and shared drawing/hit geometry.
 * Original pixels stay private. Uses system fonts and original geometric vector drawings.
 */
class NineKeyView(context:Context,private val send:(Event,Boolean)->Unit,
                  private val hide:()->Unit={},private val togglePrivacy:()->Unit={},private val retry:()->Unit={},
                  private val switchIme:()->Unit={},private val openSettings:()->Unit={},
                  private val availableHeightPx:(()->Int)?=null) : ViewGroup(context) {
    private enum class Popup { NONE, OPTIONS, PUNCTUATION, PINYIN, VOICE }
    private val density=resources.displayMetrics.density
    private var state=KeyboardState()
    private var shift=false
    private var symbolPage=false
    private var numberMore=false
    private var emojiCategory=0
    private var pendingEmojiCategory:Int?=null
    private var emojiPage=0
    private var emojiQuery=""
    private var emojiSearch=false
    private var popup=Popup.NONE
    private var pinyinPage=0
    private var lastStamp=Stamp(0,0)
    private var map=HitMap(emptyList())
    private val touches=TouchProtocol(map)
    private val actions=mutableMapOf<String,()->Unit>()
    private val geometry=mutableMapOf<String,KeyBox>()
    private val handler=Handler(Looper.getMainLooper())
    private var pointerDown=false
    private var pointerId=-1
    private var downX=0f
    private var downY=0f
    private var pressedId:String?=null
    private var currentWidth=0f
    private var maximumHeight=Float.POSITIVE_INFINITY
    private var publishedHeight=-1
    private val measureHistory=org.json.JSONArray()
    private var pendingPanelAccessibility=true
    private var lastWidthSpec=0
    private var lastHeightSpec=0
    private var panel:ReferencePanel?=null
    private var safeLeft=0
    private var safeRight=0
    private var safeBottom=0
    private val dark get()=resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK==Configuration.UI_MODE_NIGHT_YES
    private val backgroundColor get()=if(dark)0xff222222.toInt()else 0xffd1d3d9.toInt()
    private val keyForeground get()=if(dark)0xfff4f4f4.toInt()else Color.BLACK
    private val repaint=Paint(Paint.ANTI_ALIAS_FLAG)
    private val optionsHold=Runnable {if(pointerDown && pressedId=="globe"){cancelTouches();popup=Popup.OPTIONS;rebuild()}}
    private val repeater=object:Runnable {override fun run(){if(!pointerDown)return;touches.tick(SystemClock.uptimeMillis())?.let(::activate);handler.postDelayed(this,16)}}
    init {importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_YES;setWillNotDraw(false);clipChildren=true;clipToPadding=false}

    fun render(value:KeyboardState){
        if(value.stamp.session!=state.stamp.session || value.layout!=state.layout || value.expanded!=state.expanded)pendingPanelAccessibility=true
        if(value.stamp.session>lastStamp.session){
            touches.startSession(value.stamp.session);stopRepeating();clearPressed();shift=false;symbolPage=false;numberMore=false
            emojiCategory=0;emojiPage=0;emojiQuery="";emojiSearch=false;pendingEmojiCategory=null;popup=Popup.NONE;pinyinPage=0
        }else if(value.layout!=state.layout || value.expanded!=state.expanded){cancelTouches();popup=Popup.NONE}
        if(value.layout==Layout.EMOJI && state.layout!=Layout.EMOJI){pendingEmojiCategory?.let{emojiCategory=it;pendingEmojiCategory=null};emojiPage=0}
        if(value.layout!=Layout.EMOJI){emojiQuery="";emojiSearch=false}
        lastStamp=value.stamp;state=value;rebuild()
    }
    override fun onInitializeAccessibilityNodeInfo(info:AccessibilityNodeInfo){
        super.onInitializeAccessibilityNodeInfo(info)
        info.extras.putBoolean("dev.local.iosninekeyime.keyboard_root",true)
    }
    fun clearSessionUi(){cancelTouches();emojiQuery="";emojiSearch=false;pendingEmojiCategory=null;popup=Popup.NONE;pinyinPage=0;rebuild()}
    fun cancelTouches(){touches.cancel();stopRepeating();clearPressed()}
    fun layoutMetadata():org.json.JSONObject {
        val ancestors=org.json.JSONArray()
        var ancestor:View?=this
        while(ancestor!=null && ancestors.length()<8){
            val loc=IntArray(2);ancestor.getLocationInWindow(loc)
            val rect=Rect();val visible=ancestor.getGlobalVisibleRect(rect)
            ancestors.put(org.json.JSONObject().put("class",ancestor.javaClass.simpleName).put("id",ancestor.id)
                .put("height",ancestor.height).put("width",ancestor.width).put("y",loc[1])
                .put("global_visible",visible).put("visible_rect",org.json.JSONArray(listOf(rect.left,rect.top,rect.right,rect.bottom))))
            ancestor=ancestor.parent as? View
        }
        val screen=IntArray(2);getLocationOnScreen(screen)
        val location=IntArray(2);getLocationInWindow(location)
        return org.json.JSONObject()
        .put("initial_measure_specs",measureHistory).put("width",width).put("height",height).put("measured_width",measuredWidth).put("measured_height",measuredHeight)
        .put("width_spec_size",MeasureSpec.getSize(lastWidthSpec)).put("height_spec_size",MeasureSpec.getSize(lastHeightSpec)).put("height_spec_mode",MeasureSpec.getMode(lastHeightSpec))
        .put("desired_height",publishedHeight).put("child_count",childCount).put("root_height",rootView.height)
        .put("parent_height",(parent as? View)?.height?:-1).put("is_shown",isShown).put("visibility",visibility)
        .put("safe_left",safeLeft).put("safe_right",safeRight).put("safe_bottom",safeBottom)
        .put("ancestors",ancestors).put("screen_x",screen[0]).put("screen_y",screen[1]).put("window_x",location[0]).put("window_y",location[1])
    }
    private fun stopRepeating(){pointerDown=false;pointerId=-1;handler.removeCallbacks(repeater);handler.removeCallbacks(optionsHold)}
    private fun clearPressed(){pressedId?.let{findViewWithTag<View>(it)?.isPressed=false};pressedId=null}
    private fun activate(a:KeyActivation){if(a.session==state.stamp.session){if(a.key=="delete")send(Event.Backspace,a.repeated)else if(a.key=="search-delete")searchDelete()else actions[a.key]?.invoke()}}
    private fun rebuild(){if(currentWidth>200)buildKeys();requestLayout();invalidate()}
    private fun local(action:()->Unit){cancelTouches();action();rebuild()}
    private fun emit(event:Event){popup=Popup.NONE;send(event,false)}

    private fun keyBackground(tone:KeyTone,selected:Boolean=false,search:Boolean=false):StateListDrawable {
        val normal=when{
            search->if(dark)0xff3b3b3b.toInt()else 0xffbfc1c7.toInt()
            selected->Color.WHITE
            tone==KeyTone.CLEAR->Color.TRANSPARENT
            tone==KeyTone.ENTER->if(dark){if(state.layout in setOf(Layout.CHINESE,Layout.NUMBER))0xff909090.toInt()else 0xff7a7a7a.toInt()}else Color.WHITE
            else->if(dark)0xff464646.toInt()else Color.WHITE
        }
        val radius=(if(search)30f else 15f)*(panel?.scale?:1f)*density
        fun fill(color:Int)=GradientDrawable().apply{setColor(color);cornerRadius=radius}
        return StateListDrawable().apply{
            addState(intArrayOf(android.R.attr.state_pressed),fill(if(dark)0xff858585.toInt()else 0xffaeb1b8.toInt()))
            addState(intArrayOf(android.R.attr.state_focused),fill(if(dark)0xff606066.toInt()else 0xffb6bbc5.toInt()))
            addState(intArrayOf(),fill(normal))
        }
    }
    private fun addKey(key:ReferenceKey,enabled:Boolean=true,action:(()->Unit)?=null){
        val box=key.box;geometry[box.id]=box
        val view=ReferenceKeyView(context).apply{
            tag=box.id;caption.text=key.label;caption.textSize=key.textSize;caption.includeFontPadding=false;caption.gravity=Gravity.CENTER
            key.visualHeight?.let{visual->visualHeightPx=(visual*density).toInt();caption.layoutParams=android.widget.FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,checkNotNull(visualHeightPx))}
            caption.maxLines=1;caption.ellipsize=TextUtils.TruncateAt.END;setPadding(0,0,0,0);glyph=key.glyph
            iconColor=when{box.id=="shift" && shift->Color.BLACK;box.id=="enter" && !state.composing->if(state.layout in setOf(Layout.CHINESE,Layout.NUMBER))0xffa0a0a0.toInt()else 0xff8a8a8a.toInt();else->keyForeground}
            caption.setTextColor(ColorStateList.valueOf(iconColor))
            val fill=keyBackground(key.tone,box.id=="shift" && shift,box.id=="emoji-search")
            val visual=key.visualHeight
            background=if(visual!=null)android.graphics.drawable.InsetDrawable(fill,0,0,0,((box.bottom-box.top-visual)*density).toInt().coerceAtLeast(0))else fill
            if(box.id in setOf("globe","microphone")){panel?.takeIf{!it.compact}?.let{p->glyphOffsetY=(((1219-p.sourceTop)*p.scale-(box.top+box.bottom)/2)*density).coerceIn(-4.8f*density,4.8f*density)}}
            if(box.id=="space" && state.layout==Layout.SYMBOLS){caption.text="拼";caption.textSize=20*(panel?.scale?:1f);caption.gravity=Gravity.RIGHT or Gravity.BOTTOM;caption.setTextColor(0xff777777.toInt());caption.setPadding(0,0,(4*density).toInt(),(2*density).toInt())}
            if(box.id=="space" && state.layout==Layout.LATIN){caption.textScaleX=1.035f;caption.scaleY=0.885f}
            if(box.id=="space" && state.layout in setOf(Layout.CHINESE,Layout.NUMBER))caption.scaleY=0.94f
            if((box.id=="layout-primary" && state.layout==Layout.NUMBER) || box.id=="layout-return")caption.scaleY=0.925f
            if(box.id=="more" && state.layout==Layout.NUMBER){caption.textScaleX=0.98f;caption.scaleY=0.947f}
            if(box.id=="symbol-page" && key.label=="#+=")caption.scaleY=0.74f
            if(box.id=="number-punctuation"){caption.letterSpacing=0.22f;caption.scaleY=0.89f}
            if(box.id=="emoji-return")caption.scaleY=0.90f
            if(box.id=="pinyin"){caption.textScaleX=0.97f;caption.scaleY=0.85f}
            if(state.layout in setOf(Layout.CHINESE,Layout.NUMBER) && box.id=="layout-symbols")caption.scaleY=0.83f
            if(state.layout==Layout.CHINESE && box.id=="text-0")caption.textScaleX=0.84f
            if(state.layout==Layout.CHINESE && box.id=="more")caption.scaleY=0.88f
            if(state.layout==Layout.CHINESE && box.id=="more"){caption.text="^^";caption.paintFlags=caption.paintFlags or Paint.UNDERLINE_TEXT_FLAG}
            if(state.layout==Layout.CHINESE && box.id.startsWith("digit-"))caption.letterSpacing=0.175f
            if(box.id=="emoji-search"){
                caption.text=if(emojiQuery.isEmpty())"Search Emoji"else emojiQuery;glyph=KeyGlyph.SEARCH;leadingGlyph=true
                caption.gravity=Gravity.CENTER_VERTICAL;setPadding((56*(panel?.scale?:1f)*density).toInt(),0,(12*density).toInt(),0)
                caption.setTextColor(if(dark)0xffa0a0a0.toInt()else 0xff555555.toInt())
            }
            if(box.id.startsWith("emoji-category-")){
                val index=box.id.substringAfterLast('-').toInt();isSelected=index==emojiCategory;selectionDisc=false
            }
            isEnabled=enabled;isFocusable=enabled;isClickable=action!=null && enabled
            contentDescription=when{
                box.id.startsWith("emoji-category-")->EmojiCatalog.categoryNames[box.id.substringAfterLast('-').toInt()]
                key.event is Event.Text && state.layout==Layout.LATIN->(key.event as Event.Text).value
                box.id=="emoji-search"->"搜索表情"
                else->key.label
            }
            accessibilityDelegate=object:View.AccessibilityDelegate(){override fun onInitializeAccessibilityNodeInfo(host:View,info:AccessibilityNodeInfo){super.onInitializeAccessibilityNodeInfo(host,info);if(isClickable)info.className=Button::class.java.name;info.extras.putString("dev.local.iosninekeyime.key_id",box.id)}}
            if(action!=null && enabled)setOnClickListener{touches.accessibilityActivate(box.id,state.stamp.session)?.let(::activate)}
            if(box.id=="globe")setOnLongClickListener{local{popup=Popup.OPTIONS};true}
        }
        val w=((box.right-box.left)*density).toInt();val h=((box.bottom-box.top)*density).toInt()
        addView(view,LayoutParams(w,h));view.measure(MeasureSpec.makeMeasureSpec(w,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(h,MeasureSpec.EXACTLY))
        view.layout(safeLeft+(box.left*density).toInt(),(box.top*density).toInt(),safeLeft+(box.right*density).toInt(),(box.bottom*density).toInt())
        if(action!=null && enabled)actions[box.id]=action
    }
    private fun label(id:String,text:String,l:Float,t:Float,r:Float,b:Float,size:Float=14f){if(b>t && r>l)addKey(ReferenceKey(KeyBox(id,l,t,r,b),text,tone=KeyTone.CLEAR,textSize=size))}
    private fun button(id:String,text:String,l:Float,t:Float,r:Float,b:Float,enabled:Boolean=true,action:()->Unit){addKey(ReferenceKey(KeyBox(id,l,t,r,b),text,textSize=17f),enabled,action)}
    private fun buildKeys(){
        val previous=pressedId?.let{map.keys.firstOrNull{box->box.id==it}}
        removeAllViews();actions.clear();geometry.clear()
        val allEmoji=EmojiCatalog.select(emojiCategory,emojiQuery)
        val emojiPageSize=ReferenceGeometry.emojiPageSize(currentWidth,maximumHeight)
        emojiPage=emojiPage.coerceIn(0,max(0,(allEmoji.size-1)/emojiPageSize))
        val visible=allEmoji.drop(emojiPage*emojiPageSize).take(emojiPageSize)
        val p=ReferenceGeometry.build(state,currentWidth,safeBottom/density,maximumHeight,shift,symbolPage,numberMore,visible,emojiSearch)
        panel=p
        val desired=ceil(p.totalHeight*density).toInt()
        if(desired!=publishedHeight)publishedHeight=desired
        fun bind(key:ReferenceKey){
            val event=key.event
            val action:()->Unit=when{key.command!=null->{ {command(key)} };event!=null->{ {emit(event)} };else->{ {} }}
            addKey(key,action=action)
        }
        val header=when{
            state.errorCode!=null->when(state.errorCode){"E_HOST_PENDING"->"宿主未接收，请恢复连接后重试";"E_NO_CANDIDATE"->"无可用候选";else->"输入暂不可用（${state.errorCode}）"}
            state.composing->state.preedit.ifEmpty{state.input}
            !state.ready && state.layout==Layout.CHINESE->"正在准备离线资源…"
            else->""
        }
        if(p.headerHeight>0 && header.isNotEmpty())label("status",header,8f,0f,currentWidth-8,p.headerHeight,13f)
        if(state.layout!=Layout.EMOJI){
            val barRight=if(state.layout==Layout.LATIN)currentWidth else currentWidth-48;val t=p.barTop;val b=t+p.barHeight
            when{
                state.errorCode!=null || state.bar==Bar.UNAVAILABLE->{
                    val k=ReferenceKey(KeyBox("bar-info",8f,t,barRight,b),if(state.sensitive)"编辑失败 · 点此重新连接"else if(state.errorCode!=null)"点此重试"else"正在准备离线资源…",tone=KeyTone.CLEAR,textSize=14f)
                    addKey(k,action={retry()})
                }
                state.sensitive->label("bar-private","私密 · 不学习",8f,t,barRight,b,14f)
                state.composing || state.awaiting->{
                    if(state.candidates.isEmpty())label("bar-empty",if(state.awaiting)"正在生成候选…"else"无候选 · 修改或取消",8f,t,barRight,b,14f)
                    else if(!state.expanded){
                        val textPaint=Paint().apply{textSize=31*p.scale*density}
                        var left=10f
                        for(c in state.candidates){
                            val cell=max(44f,textPaint.measureText(c.text)/density+18)
                            if(left+cell>barRight)break
                            val key=ReferenceKey(KeyBox("candidate-${c.id.stamp.session}-${c.id.stamp.revision}-${c.id.index}",left,t,left+cell,b),c.text,tone=KeyTone.CLEAR,textSize=31*p.scale)
                            addKey(key,!state.awaiting){emit(Event.Choose(c.id))};left+=cell
                        }
                    }
                }
                state.layout==Layout.LATIN->{
                    listOf("I","The","I'm").forEachIndexed{i,text->val cell=barRight/3;addKey(ReferenceKey(KeyBox("english-quick-$i",i*cell,t,(i+1)*cell,b),text,Event.Text(text),tone=KeyTone.CLEAR,textSize=27*p.scale),action={emit(Event.Text(text))})}
                }
                else->{
                    val values=if(state.layout==Layout.NUMBER)listOf(".",",","-","+","/",":",";","=")else KeyboardReducer.punctuation
                    val cell=(barRight-8)/values.size
                    values.forEachIndexed{i,text->val event=if(state.layout==Layout.NUMBER)Event.Text(text)else Event.Punctuation(text);addKey(ReferenceKey(KeyBox("punct-$i",8+i*cell,t,8+(i+1)*cell,b),text,event,tone=KeyTone.CLEAR,textSize=31*p.scale),action={emit(event)})}
                }
            }
            if(state.layout!=Layout.LATIN){
                val expand=ReferenceKey(KeyBox("expand",currentWidth-44,t,currentWidth-4,b),if(state.expanded)"收起候选"else"展开",glyph=KeyGlyph.EXPAND,tone=KeyTone.CLEAR)
                addKey(expand,action={if(state.composing)emit(Event.Expand)else local{popup=Popup.PUNCTUATION}})
            }
        }
        if(popup!=Popup.NONE){p.keys.filter{it.box.id in setOf("globe","microphone")}.forEach(::bind);buildPopup(p)}
        else if(state.expanded){
            p.keys.forEach(::bind)
            val cols=if(p.compact)7 else 5;val cell=(currentWidth-6*(cols+1))/cols
            state.candidates.take(20).forEachIndexed{i,c->val l=6+(i%cols)*(cell+6);val t=p.bodyTop+(i/cols)*54;addKey(ReferenceKey(KeyBox("candidate-${c.id.stamp.session}-${c.id.stamp.revision}-${c.id.index}",l,t,l+cell,t+48),c.text,textSize=17f),!state.awaiting){emit(Event.Choose(c.id))}}
            val top=p.bodyTop+(if(p.compact)3 else 4)*54;val w=(currentWidth-30)/4
            listOf("上一页","下一页","收起","取消").forEachIndexed{i,text->val l=6+i*(w+6);button(listOf("previous","next","collapse","cancel")[i],text,l,top,l+w,top+48,
                enabled=!state.awaiting && when(i){0->state.candidateOffset>0;1->state.hasMoreCandidates;else->true}){emit(when(i){0->Event.CandidatePage(-1);1->Event.CandidatePage(1);2->Event.Expand;else->Event.Cancel})}}
        }else{
            p.keys.forEach(::bind)
            if(state.layout==Layout.EMOJI && !emojiSearch && visible.isEmpty())label("emoji-empty","没有匹配的表情",8f,p.bodyTop,currentWidth-8,p.bodyTop+48,16f)
        }
        val next=HitMap(actions.keys.map{checkNotNull(geometry[it])})
        touches.updateMap(next);map=next
        if(previous!=null && next.keys.any{it==previous})findViewWithTag<View>(previous.id)?.isPressed=true else clearPressed()
    }
    private fun command(key:ReferenceKey){when(key.command){
        KeyCommand.SHIFT->local{shift=!shift}
        KeyCommand.EMOJI->{pendingEmojiCategory=0;emit(Event.ChangeLayout(Layout.EMOJI))}
        KeyCommand.EMOTICONS->{pendingEmojiCategory=10;emit(Event.ChangeLayout(Layout.EMOJI))}
        KeyCommand.SYMBOL_PAGE->local{symbolPage=!symbolPage}
        KeyCommand.NUMBER_MORE->local{numberMore=!numberMore}
        KeyCommand.PUNCTUATION->local{popup=Popup.PUNCTUATION}
        KeyCommand.PINYIN->{local{popup=Popup.PINYIN;pinyinPage=0};send(Event.RequestPinyin,false)}
        KeyCommand.IME_SWITCH->switchIme()
        KeyCommand.MICROPHONE->local{popup=Popup.VOICE}
        KeyCommand.EMOJI_SEARCH->local{emojiSearch=true}
        KeyCommand.EMOJI_CATEGORY->local{emojiCategory=key.box.id.substringAfterLast('-').toInt();emojiPage=0;emojiQuery=""}
        KeyCommand.EMOJI_RESULT->emit(Event.Text(key.label))
        KeyCommand.SEARCH_LETTER->local{if(emojiQuery.length<32){emojiQuery+=key.label;emojiPage=0}}
        KeyCommand.SEARCH_DELETE->searchDelete()
        KeyCommand.SEARCH_DONE->local{emojiSearch=false;emojiPage=0}
        null->Unit
    }}
    private fun searchDelete(){emojiQuery=emojiQuery.dropLast(1);emojiPage=0;rebuild()}
    private fun buildPopup(p:ReferencePanel){
        val top=p.bodyTop;val w=currentWidth;val full=w-12
        when(popup){
            Popup.OPTIONS->{
                val rows=listOf("切换私密模式","打开离线设置","切换输入法","收起键盘")
                rows.forEachIndexed{i,text->val x=6+(i%2)*(full/2+3);val y=top+(i/2)*54;button("option-$i",text,x,y,min(w-6,x+full/2-3),y+48){local{popup=Popup.NONE};when(i){0->togglePrivacy();1->openSettings();2->switchIme();3->hide()}}}
            }
            Popup.PUNCTUATION->{
                val values=if(state.layout==Layout.NUMBER)listOf(".",",",";",":","+","-","/","(",")","%","=","¥")else KeyboardReducer.punctuation
                val cell=(w-30)/4
                values.forEachIndexed{i,text->val x=6+(i%4)*(cell+6);val y=top+(i/4)*54;button("popup-punct-$i",text,x,y,x+cell,y+48){local{popup=Popup.NONE};emit(if(state.layout==Layout.NUMBER)Event.Text(text)else Event.Punctuation(text))}}
            }
            Popup.PINYIN->{
                val pages=max(1,(state.pinyinChoices.size+7)/8)
                pinyinPage=pinyinPage.coerceIn(0,pages-1)
                val info=when{state.awaiting->"正在读取音节…";!state.composing->"请先输入拼音";state.pinyinChoices.isEmpty()->"没有可选音节 · 返回选词";else->"选择下一音节"}
                label("pinyin-info",info,8f,top,w-88,top+48,15f)
                button("pinyin-more","更多",w-84,top,w-6,top+48,!state.awaiting && pages>1){local{pinyinPage=(pinyinPage+1)%pages}}
                val cell=(w-30)/4
                val stamp=state.stamp
                state.pinyinChoices.drop(pinyinPage*8).take(8).forEachIndexed{i,choice->
                    val index=pinyinPage*8+i;val x=6+(i%4)*(cell+6);val y=top+54+(i/4)*54
                    button("pinyin-${stamp.session}-${stamp.revision}-$index",choice.syllable,x,y,x+cell,y+48,!state.awaiting){
                        cancelTouches();pinyinPage=0;send(Event.ChoosePinyin(stamp,index),false)
                    }
                }
                val bottom=max(top+162,p.bodyBottom-48)
                button("pinyin-cancel","取消组合",6f,bottom,w/2-3,p.bodyBottom,state.composing){emit(Event.Cancel)}
                button("popup-close","返回键盘",w/2+3,bottom,w-6,p.bodyBottom){local{popup=Popup.NONE}}
                return
            }
            Popup.VOICE->label("voice-info","当前离线版本未提供语音输入",8f,top,w-8,top+96,16f)
            Popup.NONE->Unit
        }
        button("popup-close","返回键盘",6f,max(top+162,p.bodyBottom-48),w-6,p.bodyBottom){local{popup=Popup.NONE}}
    }
    override fun onApplyWindowInsets(insets:WindowInsets):WindowInsets {
        val left:Int;val right:Int;val bottom:Int
        if(Build.VERSION.SDK_INT>=30){val bars=insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout());left=bars.left;right=bars.right;bottom=bars.bottom}
        else{
            @Suppress("DEPRECATION")
            val legacyLeft=insets.systemWindowInsetLeft
            @Suppress("DEPRECATION")
            val legacyRight=insets.systemWindowInsetRight
            @Suppress("DEPRECATION")
            val legacyBottom=insets.systemWindowInsetBottom
            left=legacyLeft;right=legacyRight;bottom=legacyBottom
        }
        if(left!=safeLeft || right!=safeRight || bottom!=safeBottom){cancelTouches();safeLeft=left;safeRight=right;safeBottom=bottom;rebuild()}
        return super.onApplyWindowInsets(insets)
    }
    override fun onMeasure(widthMeasureSpec:Int,heightMeasureSpec:Int){
        lastWidthSpec=widthMeasureSpec;lastHeightSpec=heightMeasureSpec
        val w=MeasureSpec.getSize(widthMeasureSpec);val dp=(w-safeLeft-safeRight).coerceAtLeast(0)/density
        val available=availableHeightPx?.invoke()?.div(density)?:if(MeasureSpec.getMode(heightMeasureSpec)==MeasureSpec.UNSPECIFIED)Float.POSITIVE_INFINITY else MeasureSpec.getSize(heightMeasureSpec)/density
        if(dp>200 && (dp!=currentWidth || available!=maximumHeight || panel==null)){currentWidth=dp;maximumHeight=available;cancelTouches();buildKeys()}
        for(i in 0 until childCount){val v=getChildAt(i);v.measure(MeasureSpec.makeMeasureSpec(v.layoutParams.width,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(v.layoutParams.height,MeasureSpec.EXACTLY))}
        val intrinsic=panel?.totalHeight?:0f
        if(measureHistory.length()<12)measureHistory.put(org.json.JSONArray(listOf(w,MeasureSpec.getSize(heightMeasureSpec),MeasureSpec.getMode(heightMeasureSpec),ceil(intrinsic*density).toInt())))
        setMeasuredDimension(w,resolveSize(ceil(intrinsic*density).toInt(),heightMeasureSpec))
    }
    override fun onAttachedToWindow(){super.onAttachedToWindow();pendingPanelAccessibility=true}
    override fun dispatchPopulateAccessibilityEvent(event:AccessibilityEvent):Boolean {
        if(event.eventType==AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED){
            event.text.add(if(state.sensitive)"私密键盘"else when(state.layout){Layout.CHINESE->"中文九键";Layout.LATIN->"English";Layout.NUMBER->"数字键盘";Layout.SYMBOLS->"符号键盘";Layout.EMOJI->"表情键盘"})
            return true // Panel events contain static mode labels, never candidates or host text.
        }
        return super.dispatchPopulateAccessibilityEvent(event)
    }
    override fun onLayout(changed:Boolean,l:Int,t:Int,r:Int,b:Int){
        for(i in 0 until childCount){val v=getChildAt(i);val box=geometry[v.tag]?:continue;v.layout(safeLeft+(box.left*density).toInt(),(box.top*density).toInt(),safeLeft+(box.right*density).toInt(),(box.bottom*density).toInt())}
        if(pendingPanelAccessibility && isAttachedToWindow){pendingPanelAccessibility=false;post{if(isAttachedToWindow)sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)}}
    }
    override fun onDraw(canvas:Canvas){
        val radius=40*(panel?.scale?:1f)*density;repaint.color=backgroundColor;repaint.style=Paint.Style.FILL
        // The five dark references share a larger lower curve. Keep it inside this
        // keyboard's bounds; the transparent upper IME window still belongs to the host.
        val lowerRadius=min(80*(panel?.scale?:1f)*density,min(width/2f,height/2f))
        if(dark)canvas.drawColor(0xff1e1e1e.toInt())
        val path=Path().apply{
            moveTo(0f,radius);quadTo(0f,0f,radius,0f);lineTo(width-radius,0f)
            quadTo(width.toFloat(),0f,width.toFloat(),radius)
            lineTo(width.toFloat(),height-lowerRadius);quadTo(width.toFloat(),height.toFloat(),width-lowerRadius,height.toFloat())
            lineTo(lowerRadius,height.toFloat());quadTo(0f,height.toFloat(),0f,height-lowerRadius);close()
        };canvas.drawPath(path,repaint)
        if(state.layout==Layout.EMOJI && !emojiSearch && popup==Popup.NONE){panel?.let{p->geometry["emoji-category-$emojiCategory"]?.let{box->repaint.color=0xff55555b.toInt();canvas.drawCircle(safeLeft+(box.left+box.right)/2*density,(box.top+24*p.scale)*density,21.5f*p.scale*density,repaint)}}}
        panel?.takeIf{it.headerHeight>0}?.let{repaint.color=if(dark)0xff171717.toInt()else 0xffbec1c7.toInt();canvas.drawRect(0f,it.barTop*density,width.toFloat(),it.barTop*density+1,repaint)}
        if(state.layout==Layout.LATIN && !state.sensitive){panel?.let{p->repaint.color=if(dark)0xff363638.toInt()else 0xffb8bcc5.toInt();for(i in 1..2){val x=(safeLeft+currentWidth*density*i/3);canvas.drawRect(x,(p.barTop+12)*density,x+1,(p.barTop+p.barHeight-12)*density,repaint)}}}
    }
    override fun onInterceptTouchEvent(event:MotionEvent)=true
    override fun onTouchEvent(event:MotionEvent):Boolean {
        val index=event.actionIndex;val pointer=event.getPointerId(index);val x=(event.getX(index)-safeLeft)/density;val y=event.getY(index)/density
        when(event.actionMasked){
            MotionEvent.ACTION_DOWN->{touches.down(pointer,x,y,event.eventTime);pointerDown=true;pointerId=pointer;downX=x;downY=y;pressedId=map.hit(x,y)?.id;pressedId?.let{findViewWithTag<View>(it)?.isPressed=true};handler.post(repeater);if(pressedId=="globe")handler.postDelayed(optionsHold,500)}
            MotionEvent.ACTION_MOVE->{for(i in 0 until event.pointerCount){val px=(event.getX(i)-safeLeft)/density;val py=event.getY(i)/density;touches.move(event.getPointerId(i),px,py);if(event.getPointerId(i)==pointerId && pressedId?.let{id->map.keys.firstOrNull{it.id==id}?.contains(px,py)}!=true){clearPressed();handler.removeCallbacks(optionsHold)}}}
            MotionEvent.ACTION_UP->{
                val swipe=state.layout==Layout.EMOJI && !emojiSearch && popup==Popup.NONE && downY>(panel?.barHeight?:0f) && downY<(panel?.bodyBottom?:0f)-48 && abs(x-downX)>48 && abs(y-downY)<40
                stopRepeating();clearPressed()
                if(swipe){touches.cancel();local{emojiPage=(emojiPage+if(x<downX)1 else -1).coerceAtLeast(0)}}else touches.up(pointer,x,y)?.let(::activate)
                performClick()
            }
            MotionEvent.ACTION_POINTER_UP->touches.up(pointer,x,y)?.let(::activate)
            MotionEvent.ACTION_CANCEL->cancelTouches()
        };return true
    }
    override fun performClick():Boolean {super.performClick();return true}
    override fun onDetachedFromWindow(){cancelTouches();super.onDetachedFromWindow()}
}
