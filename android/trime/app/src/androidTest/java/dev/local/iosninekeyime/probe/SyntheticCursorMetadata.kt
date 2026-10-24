// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.probe

import android.graphics.Matrix
import android.graphics.RectF
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.EditorBoundsInfo
import android.widget.EditText

/** Owned synthetic host bridge for the IMS callback. No per-character layout oracle. */
object SyntheticCursorMetadata {
    @androidx.annotation.RequiresApi(33)
    fun anchor(editor:EditText):CursorAnchorInfo {
        val layout=checkNotNull(editor.layout)
        val cursor=editor.selectionEnd
        val line=layout.getLineForOffset(cursor)
        val location=IntArray(2);editor.getLocationOnScreen(location)
        val transform=Matrix().apply {setTranslate(location[0].toFloat(),location[1].toFloat())}
        val top=editor.totalPaddingTop-editor.scrollY
        return CursorAnchorInfo.Builder().setSelectionRange(editor.selectionStart,cursor)
            .setMatrix(transform)
            .setInsertionMarkerLocation(editor.totalPaddingLeft+layout.getPrimaryHorizontal(cursor)-editor.scrollX,
                (top+layout.getLineTop(line)).toFloat(),(top+layout.getLineBaseline(line)).toFloat(),
                (top+layout.getLineBottom(line)).toFloat(),CursorAnchorInfo.FLAG_HAS_VISIBLE_REGION)
            .setEditorBoundsInfo(EditorBoundsInfo.Builder().setEditorBounds(RectF(0f,0f,editor.width.toFloat(),editor.height.toFloat())).build())
            .build()
    }
}
