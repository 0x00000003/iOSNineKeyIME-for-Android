// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.testhost;

import android.content.Context;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.view.inputmethod.*;
import android.widget.EditText;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/** Synthetic IPC fault fixture. Real TextView geometry/gestures; no coordinate/anchor bridge. */
public final class GuardedEditText extends EditText {
    public boolean rejectBody;
    public int boundsDelayMs;
    public int commitCalls,bodyReads,cursorRequests,boundsRequests,boundsDelivered,gestureRequests;
    private final Handler owner=new Handler(Looper.getMainLooper());
    public GuardedEditText(Context context){super(context);}
    private void body(){bodyReads++;if(rejectBody)throw new IllegalStateException("E_SYNTHETIC_BODY_REQUEST");}
    @Override public InputConnection onCreateInputConnection(EditorInfo info){
        InputConnection base=super.onCreateInputConnection(info);if(base==null)return null;
        return new InputConnectionWrapper(base,false){
            @Override public boolean commitText(CharSequence text,int cursor){commitCalls++;return super.commitText(text,cursor);}
            @Override public boolean commitText(CharSequence text,int cursor,TextAttribute attribute){commitCalls++;return super.commitText(text,cursor,attribute);}
            @Override public CharSequence getTextBeforeCursor(int n,int f){body();return super.getTextBeforeCursor(n,f);}
            @Override public CharSequence getTextAfterCursor(int n,int f){body();return super.getTextAfterCursor(n,f);}
            @Override public CharSequence getSelectedText(int f){body();return super.getSelectedText(f);}
            @Override public ExtractedText getExtractedText(ExtractedTextRequest r,int f){body();return super.getExtractedText(r,f);}
            @Override public SurroundingText getSurroundingText(int before,int after,int f){body();return super.getSurroundingText(before,after,f);}
            @Override public int getCursorCapsMode(int modes){body();return super.getCursorCapsMode(modes);}
            @Override public TextSnapshot takeSnapshot(){body();return super.takeSnapshot();}
            @Override public boolean requestCursorUpdates(int mode){cursorRequests++;return super.requestCursorUpdates(mode);}
            @Override public boolean requestCursorUpdates(int mode,int filter){cursorRequests++;return super.requestCursorUpdates(mode,filter);}
            @Override public void requestTextBoundsInfo(RectF area,Executor executor,Consumer<TextBoundsInfoResult> consumer){
                boundsRequests++;
                super.requestTextBoundsInfo(area,executor,result->{
                    if(boundsDelayMs>0)owner.postDelayed(()->executor.execute(()->{boundsDelivered++;consumer.accept(result);}),boundsDelayMs);
                    else {boundsDelivered++;consumer.accept(result);}
                });
            }
            @Override public void performHandwritingGesture(HandwritingGesture gesture,Executor executor,IntConsumer consumer){gestureRequests++;super.performHandwritingGesture(gesture,executor,consumer);}
        };
    }
}
