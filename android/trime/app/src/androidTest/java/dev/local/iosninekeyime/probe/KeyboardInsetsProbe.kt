// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.probe

import android.app.Instrumentation
import android.graphics.Insets
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import dev.local.iosninekeyime.state.*
import dev.local.iosninekeyime.ui.NineKeyView
import org.json.JSONArray
import org.json.JSONObject

/** Actual view measure/layout and MotionEvent hit testing; NOT reference/TalkBack/system IME. */
object KeyboardInsetsProbe {
    @androidx.annotation.RequiresApi(30)
    fun run(instrumentation:Instrumentation):JSONObject {
        val rows=JSONArray();var stage="setup"
        fun main(task:()->Unit){var error:Throwable?=null;instrumentation.runOnMainSync {try{task()}catch(t:Throwable){error=t}};error?.let {throw it}}
        try {
            for((index,padding)in listOf(Insets.NONE,Insets.of(0,0,0,54),Insets.of(54,0,0,54),Insets.of(0,0,72,54)).withIndex()){
                for(layout in Layout.entries){
                    stage="${layout.name.lowercase()}-$index"
                    main {
                        val sent=mutableListOf<Event>();val view=NineKeyView(instrumentation.targetContext,send={event,_->sent+=event})
                        view.render(KeyboardState(stamp=Stamp(1,0),layout=layout,ready=true))
                        view.dispatchApplyWindowInsets(WindowInsets.Builder().setInsets(WindowInsets.Type.systemBars(),padding).build())
                        view.measure(View.MeasureSpec.makeMeasureSpec(960,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1600,View.MeasureSpec.AT_MOST))
                        view.layout(0,0,view.measuredWidth,view.measuredHeight)
                        val density=view.resources.displayMetrics.density
                        val expected=kotlin.math.ceil(ReferenceGeometry.build(KeyboardState(stamp=Stamp(1,0),layout=layout,ready=true),
                            (960-padding.left-padding.right)/density,padding.bottom/density,1600/density,emojiItems=List(28){"😀"}).totalHeight*density).toInt()
                        val children=(0 until view.childCount).map {view.getChildAt(it)}
                        val contained=children.all {it.left>=padding.left && it.right<=view.width-padding.right && it.bottom<=view.height-padding.bottom}
                        val key=checkNotNull(view.findViewWithTag<View>(when(layout){Layout.CHINESE->"digit-2";Layout.EMOJI->"emoji-item-0";else->"text-0"}))
                        fun tap(x:Float,y:Float){val now=android.os.SystemClock.uptimeMillis();for(action in listOf(MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP)){val e=MotionEvent.obtain(now,now,action,x,y,0);try{view.dispatchTouchEvent(e)}finally{e.recycle()}}}
                        if(padding.left>0)tap(padding.left/2f,(key.top+key.bottom)/2f)
                        if(padding.right>0)tap(view.width-padding.right/2f,(key.top+key.bottom)/2f)
                        if(padding.bottom>0)tap((key.left+key.right)/2f,view.height-padding.bottom/2f)
                        val safeAreaIgnored=sent.isEmpty()
                        tap((key.left+key.right)/2f,(key.top+key.bottom)/2f)
                        val expectedEvent=when(layout){Layout.CHINESE->Event.Digit(2);Layout.LATIN->Event.Text("q");Layout.NUMBER->Event.Text("1");Layout.SYMBOLS->Event.Text("1");Layout.EMOJI->Event.Text("😀")}
                        val hit=sent==listOf(expectedEvent)
                        rows.put(JSONObject().put("id",stage).put("height_includes_bottom_inset",view.measuredHeight==expected).put("children_inside_safe_area",contained)
                            .put("safe_inset_does_not_activate",safeAreaIgnored).put("physical_hit_matches_view",hit).put("pass",view.measuredHeight==expected && contained && safeAreaIgnored && hit))
                        view.cancelTouches()
                    }
                }
            }
        }catch(_:Throwable){return JSONObject().put("scope","real view inset geometry/hit component; NOT system/reference acceptance").put("failure_stage",stage).put("cases",rows).put("pass",false)}
        val passed=(0 until rows.length()).count {rows.getJSONObject(it).getBoolean("pass")}
        return JSONObject().put("scope","real view inset geometry/hit component; NOT system/reference acceptance").put("cases",rows).put("planned_cases",20).put("passed_cases",passed).put("pass",passed==20)
    }
}
