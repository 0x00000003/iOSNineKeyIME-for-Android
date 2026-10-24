// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.ui

import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout

/** Stable transparent IME window; only the bottom keyboard participates in insets/touch region. */
internal class ReferenceInputContainer(context:Context,keyboard:NineKeyView):FrameLayout(context) {
    init {
        importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO
        addView(keyboard,LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT,Gravity.BOTTOM))
    }
    override fun onMeasure(widthMeasureSpec:Int,heightMeasureSpec:Int){
        val height=if(MeasureSpec.getMode(heightMeasureSpec)==MeasureSpec.UNSPECIFIED)resources.displayMetrics.heightPixels else MeasureSpec.getSize(heightMeasureSpec)
        super.onMeasure(widthMeasureSpec,MeasureSpec.makeMeasureSpec(height,MeasureSpec.EXACTLY))
    }
}
