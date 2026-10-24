// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.state

import kotlin.math.max
import kotlin.math.min

enum class KeyGlyph { DELETE, SHIFT, EMOJI, GLOBE, MICROPHONE, ENTER, EXPAND, CLOCK, DOG, APPLE, BALL, CAR, BULB, HEART, FLAG, SEARCH, STICKER }
enum class KeyTone { MAIN, ENTER, CLEAR }
enum class KeyCommand { SHIFT, EMOJI, EMOTICONS, SYMBOL_PAGE, NUMBER_MORE, PUNCTUATION, PINYIN, IME_SWITCH, MICROPHONE, EMOJI_SEARCH, EMOJI_CATEGORY, EMOJI_RESULT, SEARCH_LETTER, SEARCH_DELETE, SEARCH_DONE }
data class ReferenceKey(val box:KeyBox,val label:String,val event:Event?=null,val command:KeyCommand?=null,
                        val glyph:KeyGlyph?=null,val tone:KeyTone=KeyTone.MAIN,val textSize:Float=20f)
data class ReferencePanel(val keys:List<ReferenceKey>,val width:Float,val totalHeight:Float,val contentHeight:Float,
                          val headerHeight:Float,val barTop:Float,val barHeight:Float,val bodyTop:Float,
                          val bodyBottom:Float,val footerTop:Float,val scale:Float,val compact:Boolean)

/** Measurements from five user references, spec/reference/ios-dark-v1.json. No image/font assets.
 * Portrait uses width-normalized source rectangles; constrained landscape is an explicit fallback.
 * Navigation is included in total height, never added to the reference footer a second time.
 */
object ReferenceGeometry {
    fun emojiPageSize(width:Float,maxHeight:Float)=if(width>600 || 587*width/591>maxHeight)21 else 28
    fun actionLabel(action:EditorAction)=when(action){EditorAction.NEWLINE->"换行";EditorAction.DONE->"完成";EditorAction.SEND->"发送";EditorAction.SEARCH->"搜索";EditorAction.NEXT->"下一项";EditorAction.GO->"前往"}
    fun returnLabel(layout:Layout)=when(layout){Layout.CHINESE->"拼音";Layout.LATIN->"ABC";Layout.NUMBER->"123";Layout.SYMBOLS->"#@¥";Layout.EMOJI->"表情"}

    fun build(state:KeyboardState,width:Float,bottomInset:Float,maxHeight:Float=Float.POSITIVE_INFINITY,
              shift:Boolean=false,symbolPage:Boolean=false,numberMore:Boolean=false,
              emojiItems:List<String> = emptyList(),emojiSearch:Boolean=false):ReferencePanel {
        require(width>200 && bottomInset>=0)
        val s=width/591f
        val sourceTop=when(state.layout){Layout.CHINESE,Layout.NUMBER->714f;Layout.SYMBOLS->724f;Layout.LATIN->783f;Layout.EMOJI->693f}
        val normalTotal=(1280-sourceTop)*s
        val compact=normalTotal>maxHeight || width>600
        val header=if(state.layout==Layout.LATIN || state.layout==Layout.EMOJI || compact)0f else 55*s
        val bar=max(48f,if(compact)48f else if(state.layout==Layout.LATIN)78*s else 74*s)
        val mainTop=if(compact)header+bar+6 else max(header+bar,((if(state.layout in setOf(Layout.CHINESE,Layout.NUMBER))854 else 861)-sourceTop)*s)
        val step=if(compact)54f else max(54f,82*s)
        val keyH=if(compact)48f else max(48f,if(state.layout in setOf(Layout.CHINESE,Layout.NUMBER))71*s else 65*s)
        val emojiSearchTop=if(compact)0f else 14*s
        val emojiSearchHeight=if(compact)48f else max(48f,60*s)
        val emojiGridTop=max(if(compact)56f else 92*s,emojiSearchTop+emojiSearchHeight+8)
        val emojiGridStep=if(compact)52f else max(54f,84*s)
        val emojiItemHeight=if(compact)48f else max(48f,76*s)
        val emojiRows=if(compact)3 else 4
        val emojiCategoryTop=max(if(compact)emojiGridTop+emojiRows*emojiGridStep else 421*s,emojiGridTop+(emojiRows-1)*emojiGridStep+emojiItemHeight+2)
        val bodyBottom=max(mainTop+3*step+keyH,if(state.layout==Layout.EMOJI && !emojiSearch)emojiCategoryTop+48 else 0f)
        val expandedBottom=if(state.expanded)mainTop+(if(compact)3 else 4)*54+48 else bodyBottom
        val footerTop=expandedBottom+2
        val total=max(if(compact)0f else normalTotal,footerTop+48+bottomInset)
        val content=total-bottomInset
        val keys=mutableListOf<ReferenceKey>()
        fun add(id:String,label:String,left:Float,top:Float,right:Float,bottom:Float,event:Event?=null,command:KeyCommand?=null,
                glyph:KeyGlyph?=null,tone:KeyTone=KeyTone.MAIN,font:Float=30*s,repeats:Boolean=false){
            keys+=ReferenceKey(KeyBox(id,left,top,right,bottom,repeats),label,event,command,glyph,tone,if(compact)min(font,24f)else font)
        }
        fun rect(id:String,label:String,x0:Float,x1:Float,row:Int,event:Event?=null,command:KeyCommand?=null,glyph:KeyGlyph?=null,
                 tone:KeyTone=KeyTone.MAIN,font:Float=30*s,repeats:Boolean=false){
            val y=mainTop+row*step
            add(id,label,x0*s,y,x1*s,y+keyH,event,command,glyph,tone,font,repeats)
        }
        fun enter(x0:Float,x1:Float,row:Int,span:Boolean=false){
            add("enter",actionLabel(state.action),x0*s,mainTop+row*step,x1*s,if(span)bodyBottom else mainTop+row*step+keyH,
                Event.Enter,glyph=KeyGlyph.ENTER,tone=KeyTone.ENTER)
        }
        if(!state.expanded)when(state.layout){
            Layout.CHINESE,Layout.NUMBER->{
                val cols=listOf(12f to 118f,127f to 233f,243f to 348f,358f to 464f,473f to 580f)
                rect("layout-primary",if(state.layout==Layout.CHINESE)"123" else if(state.sensitive)"ABC"else"拼音",cols[0].first,cols[0].second,0,
                    Event.ChangeLayout(if(state.layout==Layout.CHINESE)Layout.NUMBER else if(state.sensitive)Layout.LATIN else Layout.CHINESE),font=28*s)
                rect("layout-symbols","#@¥",cols[0].first,cols[0].second,1,Event.ChangeLayout(Layout.SYMBOLS),font=27*s)
                rect("layout-latin","ABC",cols[0].first,cols[0].second,2,Event.ChangeLayout(Layout.LATIN),font=30*s)
                rect("emoji","表情",cols[0].first,cols[0].second,3,command=KeyCommand.EMOJI,glyph=KeyGlyph.EMOJI)
                val legends=if(state.layout==Layout.CHINESE)listOf("，。?!","ABC","DEF","GHI","JKL","MNO","PQRS","TUV","WXYZ")
                    else if(numberMore)listOf("+","−","×","÷","=","%","(",")","¥") else (1..9).map{it.toString()}
                legends.forEachIndexed {i,label->
                    val col=cols[1+i%3];val row=i/3
                    val id=if(state.layout==Layout.CHINESE && i>0)"digit-${i+1}" else "text-$i"
                    rect(id,label,col.first,col.second,row,
                        event=if(state.layout==Layout.CHINESE && i>0)Event.Digit(i+1)else if(state.layout==Layout.NUMBER)Event.Text(label)else null,
                        command=if(state.layout==Layout.CHINESE && i==0)KeyCommand.PUNCTUATION else null,font=if(state.layout==Layout.NUMBER)38*s else 30*s)
                }
                rect("delete","退格",473f,580f,0,Event.Backspace,glyph=KeyGlyph.DELETE,repeats=true)
                rect("more",if(state.layout==Layout.CHINESE)"颜文字" else if(numberMore)"数字"else"更多",473f,580f,1,
                    command=if(state.layout==Layout.CHINESE)KeyCommand.EMOTICONS else KeyCommand.NUMBER_MORE,font=28*s)
                enter(473f,580f,2,true)
                if(state.layout==Layout.CHINESE){
                    rect("pinyin","选拼音",127f,233f,3,command=KeyCommand.PINYIN,font=27*s)
                    rect("space","空格",243f,464f,3,Event.Space,font=30*s)
                }else{
                    rect("number-punctuation",".,;",127f,233f,3,command=KeyCommand.PUNCTUATION,font=32*s)
                    rect("zero","0",243f,348f,3,Event.Text("0"),font=38*s)
                    rect("space","空格",358f,464f,3,Event.Space,font=28*s)
                }
            }
            Layout.LATIN,Layout.SYMBOLS->{
                val latin=state.layout==Layout.LATIN
                val first=if(latin)"qwertyuiop".map{it.toString()} else "1234567890".map{it.toString()}
                val second=if(latin)"asdfghjkl".map{it.toString()}else if(symbolPage)listOf("[","]","{","}","#","%","^","*","+","=")else listOf("-","/",":",";","(",")","$","@","“","”")
                var index=0
                fun letter(label:String,x0:Float,x1:Float,row:Int){
                    val text=if(latin && shift)label.uppercase()else label
                    rect("text-${index++}",if(latin)label.uppercase()else label,x0,x1,row,Event.Text(text),font=if(latin)42*s else 38*s)
                }
                first.forEachIndexed {i,label->letter(label,10f+i*58,59f+i*58,0)}
                second.forEachIndexed {i,label->val x=if(latin)39f+i*58 else 10f+i*58;letter(label,x,x+49,1)}
                if(latin){
                    rect("shift",if(shift)"小写"else"大写",10f,76f,2,command=KeyCommand.SHIFT,glyph=KeyGlyph.SHIFT)
                    "zxcvbnm".forEachIndexed {i,ch->letter(ch.toString(),97f+i*58,146f+i*58,2)}
                }else{
                    rect("symbol-page",if(symbolPage)"123"else"#+=",10f,76f,2,command=KeyCommand.SYMBOL_PAGE,font=23*s)
                    val third=if(symbolPage)listOf("_","\\","|","~","<",">")else listOf("。","，","、","?","!",".")
                    third.forEachIndexed {i,label->letter(label,91f+i*70,151f+i*70,2)}
                }
                rect("delete","退格",515f,581f,2,Event.Backspace,glyph=KeyGlyph.DELETE,repeats=true)
                if(latin){
                    rect("layout-symbols","123",10f,73f,3,Event.ChangeLayout(Layout.SYMBOLS),font=29*s)
                    rect("emoji","表情",82f,146f,3,command=KeyCommand.EMOJI,glyph=KeyGlyph.EMOJI)
                }else rect("layout-return",returnLabel(state.returnLayout),10f,146f,3,Event.ChangeLayout(state.returnLayout),font=28*s)
                rect("space",if(latin)"English (US)"else"空格",155f,436f,3,Event.Space,font=27*s)
                enter(445f,582f,3)
            }
            Layout.EMOJI->{
                val searchTop=emojiSearchTop
                val searchH=emojiSearchHeight
                add("emoji-search","搜索表情",14*s,searchTop,577*s,searchTop+searchH,command=KeyCommand.EMOJI_SEARCH,glyph=KeyGlyph.SEARCH,tone=KeyTone.CLEAR,font=26*s)
                // Search input uses local keys only, never the host InputConnection.
                if(emojiSearch){
                    val rows=listOf("qwertyuiop","asdfghjkl","zxcvbnm")
                    rows.forEachIndexed {r,line->line.forEachIndexed {i,ch->
                        val x=when(r){0->10f+i*58;1->39f+i*58;else->97f+i*58}
                        rect("search-letter-$ch",ch.toString(),x,x+49,r,command=KeyCommand.SEARCH_LETTER,font=36*s)
                    }}
                    rect("search-delete","退格",515f,581f,2,command=KeyCommand.SEARCH_DELETE,glyph=KeyGlyph.DELETE,repeats=true)
                    rect("search-done","完成搜索",10f,146f,3,command=KeyCommand.SEARCH_DONE,font=24*s)
                    rect("search-space"," ",155f,436f,3,command=KeyCommand.SEARCH_LETTER,font=26*s)
                    rect("search-enter","完成搜索",445f,582f,3,command=KeyCommand.SEARCH_DONE,glyph=KeyGlyph.ENTER,tone=KeyTone.ENTER)
                }else{
                    val gridTop=emojiGridTop
                    val gridStep=emojiGridStep
                    emojiItems.take(emojiRows*7).forEachIndexed {i,label->
                        val l=(10+i%7*(571f/7))*s;val top=gridTop+(i/7)*gridStep
                        add("emoji-item-$i",label,l,top,l+571*s/7-2,top+emojiItemHeight,command=KeyCommand.EMOJI_RESULT,tone=KeyTone.CLEAR,font=if(compact)30f else if(label.length>6 && label.firstOrNull()=='(')24*s else 51*s)
                    }
                    val catTop=emojiCategoryTop
                    val catH=48f
                    add("emoji-return",returnLabel(state.emojiReturnLayout),10*s,catTop,69*s,catTop+catH,Event.ChangeLayout(state.emojiReturnLayout),tone=KeyTone.CLEAR,font=23*s)
                    val glyphs=listOf(KeyGlyph.CLOCK,KeyGlyph.EMOJI,KeyGlyph.DOG,KeyGlyph.APPLE,KeyGlyph.BALL,KeyGlyph.CAR,KeyGlyph.BULB,KeyGlyph.HEART,KeyGlyph.FLAG)
                    add("emoji-category-10","文字表情",77*s,catTop,116*s,catTop+catH,command=KeyCommand.EMOJI_CATEGORY,glyph=KeyGlyph.STICKER,tone=KeyTone.CLEAR)
                    glyphs.forEachIndexed{i,glyph->val l=(117+i*40f)*s;add("emoji-category-$i","表情分类${i+1}",l,catTop,l+39*s,catTop+catH,command=KeyCommand.EMOJI_CATEGORY,glyph=glyph,tone=KeyTone.CLEAR)}
                    add("emoji-category-9","人物表情",477*s,catTop,516*s,catTop+catH,command=KeyCommand.EMOJI_CATEGORY,glyph=KeyGlyph.EMOJI,tone=KeyTone.CLEAR)
                    add("delete","退格",539*s,catTop,581*s,catTop+catH,Event.Backspace,glyph=KeyGlyph.DELETE,tone=KeyTone.CLEAR,repeats=true)
                }
            }
        }
        val footerY=min(if(compact)footerTop+24 else (1219-sourceTop)*s,content-24).coerceAtLeast(footerTop+24)
        add("globe","切换输入法",64*s-24,footerY-24,64*s+24,footerY+24,command=KeyCommand.IME_SWITCH,glyph=KeyGlyph.GLOBE,tone=KeyTone.CLEAR)
        add("microphone","语音输入",527*s-24,footerY-24,527*s+24,footerY+24,command=KeyCommand.MICROPHONE,glyph=KeyGlyph.MICROPHONE,tone=KeyTone.CLEAR)
        HitMap(keys.map{it.box})
        return ReferencePanel(keys,width,total,content,header,header,bar,mainTop,expandedBottom,footerTop,s,compact)
    }
}
