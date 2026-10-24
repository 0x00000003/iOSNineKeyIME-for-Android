// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.editor

import android.graphics.Matrix
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.DeleteGesture
import android.view.inputmethod.HandwritingGesture
import android.view.inputmethod.InputConnection
import android.view.inputmethod.TextBoundsInfoResult
import androidx.annotation.RequiresApi
import java.util.concurrent.Executor
import java.util.function.Consumer
import java.util.function.IntConsumer

/** Owner-thread, bounded structural metadata only. Never reads or retains text/segment finders. */
@RequiresApi(34)
internal class StructuralDelete(private val handler:Handler, private val timeoutMs:Long=2000) {
    private class Job(
        val editor:InputConnection,
        val valid:()->Boolean,
        val selectionCurrent:()->Boolean,
        val expectedStart:Int,
        val expectedEnd:Int,
        val reply:(EditResult)->Unit,
    ) {
        var phase=0 // anchor, bounds, gesture; no body data in this state.
        var attempt=0
        var call=0
        var areas:List<RectF> = emptyList()
        lateinit var timeout:Runnable
    }
    private var pending:Job?=null
    private val executor=Executor { handler.post(it) }

    fun start(editor:InputConnection, start:Int, end:Int, valid:()->Boolean, selectionCurrent:()->Boolean, reply:(EditResult)->Unit) {
        check(Looper.myLooper()==handler.looper){"E_OWNER_THREAD"}
        if(pending!=null){reply(EditResult.UNAVAILABLE);return}
        val job=Job(editor,valid,selectionCurrent,start,end,reply)
        pending=job
        job.timeout=Runnable { if(pending===job)finish(job,if(job.phase==2)EditResult.AMBIGUOUS else EditResult.UNAVAILABLE) }
        handler.postDelayed(job.timeout,timeoutMs)
        try {
            // Only insertion marker/editor bounds. Never request composing or character text.
            val supported=editor.requestCursorUpdates(InputConnection.CURSOR_UPDATE_IMMEDIATE,
                InputConnection.CURSOR_UPDATE_FILTER_INSERTION_MARKER or InputConnection.CURSOR_UPDATE_FILTER_EDITOR_BOUNDS)
            if(!supported && pending===job && job.phase==0)finish(job,EditResult.UNAVAILABLE)
        }catch(_:RuntimeException){if(pending===job)finish(job,EditResult.UNAVAILABLE)}
    }

    fun anchor(info:CursorAnchorInfo) {
        val job=pending?:return
        if(job.phase!=0)return
        if(!job.valid() || !job.selectionCurrent()){finish(job,EditResult.STALE);return}
        val cursor=info.selectionEnd
        if(cursor<0 || cursor!=info.selectionStart || (job.expectedStart>=0 && (info.selectionStart!=job.expectedStart || cursor!=job.expectedEnd))){finish(job,EditResult.STALE);return}
        if(cursor==0){finish(job,EditResult.ACCEPTED);return}
        // Copy just the current caret's line band inside editor bounds; do not retain CursorAnchorInfo.
        val editorBounds=info.editorBoundsInfo?.editorBounds
        if(editorBounds==null){finish(job,EditResult.UNAVAILABLE);return}
        val region=RectF(editorBounds)
        region.top=maxOf(region.top,info.insertionMarkerTop)
        region.bottom=minOf(region.bottom,info.insertionMarkerBottom)
        val matrix=info.matrix
        matrix.mapRect(region)
        if(!usable(region)){finish(job,EditResult.UNAVAILABLE);return}
        job.phase=1
        try { job.editor.requestTextBoundsInfo(region,executor,Consumer { result ->
            if(pending!==job || job.phase!=1)return@Consumer
            if(!job.valid() || !job.selectionCurrent()){finish(job,EditResult.STALE);return@Consumer}
            if(result.resultCode!=TextBoundsInfoResult.CODE_SUCCESS){finish(job,EditResult.UNAVAILABLE);return@Consumer}
            try {
                val bounds=result.textBoundsInfo
                if(bounds==null || bounds.startIndex>=cursor || bounds.endIndex<cursor){finish(job,EditResult.UNAVAILABLE);return@Consumer}
                val transform=Matrix();bounds.getMatrix(transform)
                // At most 32 trailing UTF-16 positions on this line. Host decides original graphemes,
                // including password transforms; masked segment finders are deliberately never read.
                val count=minOf(32,cursor-bounds.startIndex)
                val areas=ArrayList<RectF>(count)
                var union:RectF?=null
                for(units in 1..count){
                    val next=RectF();bounds.getCharacterBounds(cursor-units,next)
                    if(!finite(next)){finish(job,EditResult.UNAVAILABLE);return@Consumer}
                    val previous=union
                    if(previous==null)union=RectF(next)else previous.set(minOf(previous.left,next.left),minOf(previous.top,next.top),maxOf(previous.right,next.right),maxOf(previous.bottom,next.bottom))
                    val area=RectF(checkNotNull(union));transform.mapRect(area)
                    area.inset(-0.1f,0f)
                    if(!usable(area)){finish(job,EditResult.UNAVAILABLE);return@Consumer}
                    areas.add(area)
                }
                job.areas=areas
                gesture(job)
            }catch(_:RuntimeException){if(pending===job)finish(job,if(job.phase==2)EditResult.AMBIGUOUS else EditResult.UNAVAILABLE)}
        }) }catch(_:RuntimeException){if(pending===job)finish(job,EditResult.UNAVAILABLE)}
    }

    private fun gesture(job:Job) {
        if(!job.valid() || !job.selectionCurrent()){finish(job,EditResult.STALE);return}
        if(job.attempt>=job.areas.size){finish(job,EditResult.CONTEXT_LIMIT);return}
        val gesture=DeleteGesture.Builder().setDeletionArea(job.areas[job.attempt++])
            .setGranularity(HandwritingGesture.GRANULARITY_CHARACTER).build()
        job.phase=2
        val call=++job.call
        try { job.editor.performHandwritingGesture(gesture,executor,IntConsumer { code ->
            if(pending!==job || job.phase!=2 || job.call!=call)return@IntConsumer
            job.call++ // A duplicate response cannot retry or finish a later attempt.
            if(!job.valid()){finish(job,EditResult.STALE);return@IntConsumer}
            when(code){
                InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS -> finish(job,EditResult.ACCEPTED)
                InputConnection.HANDWRITING_GESTURE_RESULT_FAILED -> gesture(job)
                InputConnection.HANDWRITING_GESTURE_RESULT_UNSUPPORTED,
                InputConnection.HANDWRITING_GESTURE_RESULT_CANCELLED -> finish(job,EditResult.UNAVAILABLE)
                else -> finish(job,EditResult.AMBIGUOUS) // No key fallback or automatic replay.
            }
        }) }catch(_:RuntimeException){if(pending===job)finish(job,EditResult.AMBIGUOUS)}
    }
    fun cancel(){pending?.let {finish(it,EditResult.STALE)}}
    private fun finish(job:Job,result:EditResult){
        if(pending!==job)return
        pending=null;handler.removeCallbacks(job.timeout);job.areas=emptyList()
        job.reply(result)
    }
    private fun finite(rect:RectF)=listOf(rect.left,rect.top,rect.right,rect.bottom).all {it.isFinite() && kotlin.math.abs(it)<10_000_000f}
    private fun usable(rect:RectF)=finite(rect) && !rect.isEmpty
}
