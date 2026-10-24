// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.widget.TextView
import android.widget.FrameLayout
import dev.local.iosninekeyime.state.KeyGlyph

/** Original geometric vector drawings. No Apple glyph files, fonts or bitmap resources. */
internal class ReferenceKeyView(context:Context):FrameLayout(context) {
    val caption=TextView(context)
    var glyph:KeyGlyph?=null
        set(value){field=value;updateCaption()}
    var leadingGlyph=false
        set(value){field=value;updateCaption()}
    var selectionDisc=false
    var iconColor=0xfff4f4f4.toInt()
    private val ink=Paint(Paint.ANTI_ALIAS_FLAG).apply {strokeWidth=1.6f;strokeCap=Paint.Cap.ROUND;strokeJoin=Paint.Join.ROUND}
    init {
        setWillNotDraw(false)
        caption.importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO
        addView(caption,LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT))
    }
    private fun updateCaption(){caption.visibility=if(glyph==null || leadingGlyph)VISIBLE else INVISIBLE}
    override fun onDraw(canvas:Canvas) {
        val g=glyph
        if(g==null){super.onDraw(canvas);return}
        val size=minOf(width,height)*if(g in setOf(KeyGlyph.GLOBE,KeyGlyph.MICROPHONE))0.72f else if(leadingGlyph)0.45f else 0.60f
        val saved=canvas.save()
        try {
            canvas.translate(if(leadingGlyph)12*resources.displayMetrics.density else (width-size)/2,(height-size)/2);canvas.scale(size/24,size/24)
            if(selectionDisc){ink.color=0xff55555b.toInt();ink.style=Paint.Style.FILL;canvas.drawCircle(12f,12f,18f,ink)}
            ink.color=iconColor;ink.style=Paint.Style.STROKE;ink.strokeWidth=1.5f
            fun line(x:Float,y:Float,x2:Float,y2:Float)=canvas.drawLine(x,y,x2,y2,ink)
            fun circle(x:Float,y:Float,r:Float)=canvas.drawCircle(x,y,r,ink)
            fun path(vararg points:Float){val p=Path();p.moveTo(points[0],points[1]);for(i in 2 until points.size step 2)p.lineTo(points[i],points[i+1]);canvas.drawPath(p,ink)}
            fun dot(x:Float,y:Float,r:Float=1f){ink.style=Paint.Style.FILL;circle(x,y,r);ink.style=Paint.Style.STROKE}
            when(g){
                KeyGlyph.DELETE->{path(9f,4f,22f,4f,22f,20f,9f,20f,2f,12f,9f,4f);line(12f,8f,18f,16f);line(18f,8f,12f,16f)}
                KeyGlyph.SHIFT->{val p=Path();p.moveTo(12f,2f);p.lineTo(2f,12f);p.lineTo(7f,12f);p.lineTo(7f,21f);p.lineTo(17f,21f);p.lineTo(17f,12f);p.lineTo(22f,12f);p.close();ink.style=Paint.Style.FILL;canvas.drawPath(p,ink)}
                KeyGlyph.EMOJI->{circle(12f,12f,10f);dot(8f,9f);dot(16f,9f);canvas.drawArc(RectF(6f,7f,18f,18f),20f,140f,false,ink)}
                KeyGlyph.GLOBE->{circle(12f,12f,10f);canvas.drawOval(RectF(7f,2f,17f,22f),ink);line(2f,12f,22f,12f);canvas.drawArc(RectF(2f,5f,22f,12f),0f,180f,false,ink);canvas.drawArc(RectF(2f,12f,22f,19f),180f,180f,false,ink)}
                KeyGlyph.MICROPHONE->{canvas.drawRoundRect(RectF(9f,2f,15f,16f),3f,3f,ink);canvas.drawArc(RectF(5f,4f,19f,20f),0f,180f,false,ink);line(5f,11f,5f,13f);line(19f,11f,19f,13f);line(12f,20f,12f,23f);line(8f,23f,16f,23f)}
                KeyGlyph.ENTER->when(contentDescription?.toString()){
                    "搜索"->{circle(10f,10f,7f);line(15f,15f,22f,22f)}
                    "完成"->path(3f,12f,9f,18f,21f,5f)
                    "下一项","前往"->{line(3f,12f,21f,12f);path(14f,5f,21f,12f,14f,19f)}
                    "换行"->{path(20f,4f,20f,14f,4f,14f,10f,8f);path(4f,14f,10f,20f)}
                    else->{line(12f,21f,12f,3f);path(5f,10f,12f,3f,19f,10f)}
                }
                KeyGlyph.EXPAND->if(contentDescription=="收起候选")path(3f,16f,12f,8f,21f,16f)else path(3f,8f,12f,16f,21f,8f)
                KeyGlyph.CLOCK->{circle(12f,12f,10f);path(12f,5f,12f,12f,7f,12f)}
                KeyGlyph.DOG->{path(5f,9f,2f,5f,2f,16f,5f,18f);path(19f,9f,22f,5f,22f,16f,19f,18f);canvas.drawRoundRect(RectF(5f,5f,19f,22f),6f,6f,ink);dot(8f,12f);dot(16f,12f);dot(12f,17f,1.5f)}
                KeyGlyph.APPLE->{val p=Path();p.moveTo(12f,7f);p.cubicTo(1f,0f,0f,15f,6f,21f);p.cubicTo(9f,24f,11f,20f,12f,21f);p.cubicTo(19f,26f,24f,14f,21f,8f);p.cubicTo(18f,3f,14f,6f,12f,7f);canvas.drawPath(p,ink);path(12f,7f,13f,2f,17f,1f)}
                KeyGlyph.BALL->{circle(12f,12f,10f);path(12f,7f,17f,10f,15f,16f,9f,16f,7f,10f,12f,7f);line(12f,2f,12f,7f);line(17f,10f,22f,8f);line(15f,16f,18f,21f);line(9f,16f,6f,21f);line(7f,10f,2f,8f)}
                KeyGlyph.CAR->{path(3f,10f,6f,4f,18f,4f,21f,10f);canvas.drawRoundRect(RectF(2f,10f,22f,19f),2f,2f,ink);circle(6f,20f,2f);circle(18f,20f,2f);line(6f,14f,8f,14f);line(16f,14f,18f,14f)}
                KeyGlyph.BULB->{canvas.drawArc(RectF(5f,1f,19f,17f),155f,230f,false,ink);path(6f,13f,9f,18f,15f,18f,18f,13f);line(9f,21f,15f,21f);line(10f,23f,14f,23f)}
                KeyGlyph.HEART->{val p=Path();p.moveTo(12f,22f);p.cubicTo(3f,16f,-1f,10f,3f,5f);p.cubicTo(6f,1f,10f,3f,12f,6f);p.cubicTo(16f,-1f,24f,3f,22f,10f);p.cubicTo(21f,15f,17f,18f,12f,22f);canvas.drawPath(p,ink)}
                KeyGlyph.FLAG->{line(4f,2f,4f,23f);path(4f,3f,20f,3f,17f,8f,20f,13f,4f,13f)}
                KeyGlyph.SEARCH->{circle(10f,10f,7f);line(15f,15f,22f,22f)}
                KeyGlyph.STICKER->{val p=Path();p.moveTo(14f,2f);p.cubicTo(-2f,4f,1f,24f,17f,21f);p.cubicTo(7f,18f,6f,9f,14f,2f);canvas.drawPath(p,ink)}
            }
        }finally{canvas.restoreToCount(saved)}
    }
}
