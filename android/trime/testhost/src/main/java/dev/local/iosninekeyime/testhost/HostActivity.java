// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.testhost;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsAnimation;
import java.util.List;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;
import java.io.ByteArrayInputStream;

/** Cross-UID synthetic host. No input/file/network logging, persistence, clipboard or IME selection. */
public final class HostActivity extends Activity {
    public EditText editor;
    public WebView web;
    public int actionCount;
    public int lastAction;
    public boolean imeAnimationRunning;
    public int imeAnimationStarts,imeAnimationEnds;
    private LinearLayout panel;
    private TextView status;
    private String currentCase = "plain";
    public boolean guardConnections;
    public int nextBoundsDelayMs;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(12), dp(12), dp(12), dp(12));
        panel.setOnApplyWindowInsetsListener((view,insets)->{
            android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
            panel.setPadding(bars.left+dp(12),bars.top+dp(12),bars.right+dp(12),bars.bottom+dp(12));return insets;
        });
        panel.setWindowInsetsAnimationCallback(new WindowInsetsAnimation.Callback(WindowInsetsAnimation.Callback.DISPATCH_MODE_CONTINUE_ON_SUBTREE){
            @Override public void onPrepare(WindowInsetsAnimation animation){if((animation.getTypeMask() & WindowInsets.Type.ime())!=0){imeAnimationRunning=true;imeAnimationStarts++;}}
            @Override public WindowInsets onProgress(WindowInsets insets,List<WindowInsetsAnimation> animations){return insets;}
            @Override public void onEnd(WindowInsetsAnimation animation){if((animation.getTypeMask() & WindowInsets.Type.ime())!=0){imeAnimationRunning=false;imeAnimationEnds++;}}
        });
        status = new TextView(this);
        status.setText("仅合成数据；不输入真实密码。先在系统设置由你启用/选中离线九键。");
        status.setTextSize(15);panel.addView(status);
        LinearLayout controls = new LinearLayout(this);
        for(String kind:new String[]{"plain","password","no-learn","web"}){
            Button button=new Button(this);button.setText(kind);
            button.setOnClickListener(v->{if(kind.equals("web"))showWeb();else selectCase(kind,"",0,true);});
            controls.addView(button,new LinearLayout.LayoutParams(0,dp(52),1));
        }
        panel.addView(controls);
        editor=new EditText(this);editor.setTextSize(20);
        editor.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
        editor.setOnEditorActionListener((v,id,event)->{actionCount++;lastAction=id;return true;});
        panel.addView(editor,new LinearLayout.LayoutParams(-1,dp(100)));
        panel.addView(new TextView(this){{setText("测试夹具在内存中；切字段会重置。结果只记录计数/布尔值。WebView仅加载固定内页。");}});
        setContentView(panel);selectCase("plain","",0,false);
    }
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    public void selectCase(String kind,String fixture,int caret,boolean show){
        if(web!=null)web.setVisibility(View.GONE);
        currentCase=kind;
        // Prepare a new field before it is attached/served. Mutating the already served view's
        // type several times schedules multiple legitimate restartInput calls during typing.
        EditText previous=editor;
        GuardedEditText guarded=new GuardedEditText(this);guarded.rejectBody=guardConnections && !kind.equals("email");guarded.boundsDelayMs=nextBoundsDelayMs;
        editor=guarded;editor.setId(View.generateViewId());editor.setTextSize(20);
        editor.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
        editor.setOnEditorActionListener((v,id,event)->{actionCount++;lastAction=id;return true;});
        editor.setSingleLine(true);
        int type=InputType.TYPE_CLASS_TEXT, action=EditorInfo.IME_ACTION_DONE;
        switch(kind){
            case "password": type|=InputType.TYPE_TEXT_VARIATION_PASSWORD;break;
            case "visible-password":type|=InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;break;
            case "web-password":type|=InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD;break;
            case "number-password":type=InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD;break;
            case "number":type=InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED;break;
            case "phone":type=InputType.TYPE_CLASS_PHONE;break;
            case "email":type|=InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;break;
            case "url":type|=InputType.TYPE_TEXT_VARIATION_URI;break;
            case "multiline":editor.setSingleLine(false);type|=InputType.TYPE_TEXT_FLAG_MULTI_LINE;action=EditorInfo.IME_ACTION_NONE;break;
            case "send":action=EditorInfo.IME_ACTION_SEND;break;
            case "search":action=EditorInfo.IME_ACTION_SEARCH;break;
            case "next":action=EditorInfo.IME_ACTION_NEXT;break;
            case "go":action=EditorInfo.IME_ACTION_GO;break;
        }
        if(kind.equals("no-learn"))action|=EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING;
        editor.setInputType(type);editor.setImeOptions(action);editor.setText(fixture);editor.setSelection(caret);
        actionCount=0;lastAction=-1;status.setText("合成字段："+kind);
        int position=panel.indexOfChild(previous);previous.clearFocus();panel.removeView(previous);
        panel.addView(editor,position,new LinearLayout.LayoutParams(-1,dp(100)));editor.requestFocus();
        InputMethodManager manager=getSystemService(InputMethodManager.class);
        if(show)manager.showSoftInput(editor,InputMethodManager.SHOW_IMPLICIT);
    }
    public void showWeb(){
        currentCase="web";editor.clearFocus();editor.setVisibility(View.GONE);
        if(web==null){
            web=new WebView(this);
            web.getSettings().setJavaScriptEnabled(false);
            web.getSettings().setAllowFileAccess(false);web.getSettings().setAllowContentAccess(false);
            web.getSettings().setBlockNetworkLoads(true);web.getSettings().setSaveFormData(false);
            web.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
            web.setWebViewClient(new WebViewClient(){
                @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){return true;}
                @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest request){return new WebResourceResponse("text/plain","utf-8",new ByteArrayInputStream(new byte[0]));}
            });
            panel.addView(web,new LinearLayout.LayoutParams(-1,dp(180)));
            web.loadDataWithBaseURL(null,"<!doctype html><title>Synthetic offline host</title><meta name='viewport' content='width=device-width'><meta http-equiv='Content-Security-Policy' content=\"default-src 'none'; style-src 'unsafe-inline'; form-action 'none'\"><style>input{font-size:20px;width:85%;margin:8px}</style><label>synthetic plain<input aria-label='synthetic web plain'></label><label>synthetic password<input type='password' aria-label='synthetic web password' autocomplete='off'></label>","text/html","UTF-8",null);
        }
        web.setVisibility(View.VISIBLE);status.setText("合成本地WebView；无脚本、无网络、无文件访问");
    }
    public String caseId(){return currentCase;}
    @Override public void onDestroy(){if(web!=null){web.destroy();web=null;}super.onDestroy();}
}
