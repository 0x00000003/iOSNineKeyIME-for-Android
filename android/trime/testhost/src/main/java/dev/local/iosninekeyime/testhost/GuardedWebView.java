// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.testhost;

import android.content.Context;
import android.text.InputType;
import android.graphics.RectF;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import android.view.inputmethod.*;
import android.webkit.WebView;

/** Actual local WebView connection. Reject sensitive body getters, preserve real editing APIs. */
public final class GuardedWebView extends WebView {
    public int sensitiveBodyReads,normalBodyReads,commitCalls,boundsRequests,cursorRequests,gestureRequests,lastBoundsCode=-1,lastGestureCode=-1;
    public boolean lastCursorSupported;
    public GuardedWebView(Context context){super(context);}
    @Override public InputConnection onCreateInputConnection(EditorInfo info){
        InputConnection base=super.onCreateInputConnection(info);if(base==null)return null;
        int variation=info.inputType & InputType.TYPE_MASK_VARIATION;
        final boolean sensitive=variation==InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD || variation==InputType.TYPE_TEXT_VARIATION_PASSWORD || variation==InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
        return new InputConnectionWrapper(base,false){
            private void body(){if(sensitive){sensitiveBodyReads++;throw new IllegalStateException("E_SYNTHETIC_WEB_BODY");}normalBodyReads++;}
            @Override public boolean commitText(CharSequence text,int cursor){commitCalls++;return super.commitText(text,cursor);}
            @Override public boolean commitText(CharSequence text,int cursor,TextAttribute attr){commitCalls++;return super.commitText(text,cursor,attr);}
            @Override public CharSequence getTextBeforeCursor(int n,int f){body();return super.getTextBeforeCursor(n,f);}
            @Override public CharSequence getTextAfterCursor(int n,int f){body();return super.getTextAfterCursor(n,f);}
            @Override public CharSequence getSelectedText(int f){body();return super.getSelectedText(f);}
            @Override public ExtractedText getExtractedText(ExtractedTextRequest r,int f){body();return super.getExtractedText(r,f);}
            @Override public SurroundingText getSurroundingText(int before,int after,int f){body();return super.getSurroundingText(before,after,f);}
            @Override public int getCursorCapsMode(int modes){body();return super.getCursorCapsMode(modes);}
            @Override public TextSnapshot takeSnapshot(){body();return super.takeSnapshot();}
            @Override public boolean requestCursorUpdates(int mode){cursorRequests++;lastCursorSupported=super.requestCursorUpdates(mode);return lastCursorSupported;}
            @Override public boolean requestCursorUpdates(int mode,int filter){cursorRequests++;lastCursorSupported=super.requestCursorUpdates(mode,filter);return lastCursorSupported;}
            @Override public void requestTextBoundsInfo(RectF area,Executor executor,Consumer<TextBoundsInfoResult> consumer){boundsRequests++;super.requestTextBoundsInfo(area,executor,result->{lastBoundsCode=result==null?-2:result.getResultCode();consumer.accept(result);});}
            @Override public void performHandwritingGesture(HandwritingGesture gesture,Executor executor,IntConsumer consumer){gestureRequests++;super.performHandwritingGesture(gesture,executor,result->{lastGestureCode=result;consumer.accept(result);});}
        };
    }
}
