// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.testhost;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.*;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.Bitmap;
import java.io.File;
import java.io.FileOutputStream;
import android.os.*;
import android.provider.Settings;
import android.text.InputType;
import android.view.*;
import android.view.accessibility.*;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.text.method.PasswordTransformationMethod;
import org.json.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

/** Real system IME/host boundary. No coordinate bridge, text logs or in-app IME selection. */
@SuppressWarnings("deprecation")
public final class HostInstrumentation extends Instrumentation {
    private String IME="dev.local.iosninekeyime.probe.debug";

    private String scope="readiness",nonce,stage="setup";
    private String step="setup";
    private int taps=0,injected=0,imeWindows=0;
    private boolean ownImeRoot=false;
    private int measuredGap=-1,expectedGap=-1;
    private int navigationGap=-1,lastGap=-1,stableGapSamples=0;
    private final JSONArray ownFrameGeometry=new JSONArray();
    private final JSONArray captures=new JSONArray();
    private HostActivity activity;
    private UiAutomation automation;
    @Override public void onCreate(Bundle args){
        super.onCreate(args);
        if(args!=null){scope=args.getString("host_scope","readiness");nonce=args.getString("host_run_id");IME=args.getString("ime_package",IME);}
        start();
    }
    private void main(Runnable task){
        AtomicReference<Throwable> failure=new AtomicReference<>();
        runOnMainSync(()->{try{task.run();}catch(Throwable error){failure.set(error);}});
        if(failure.get()!=null)throw new IllegalStateException("E_HOST_MAIN");
    }
    private boolean read(BooleanSupplier test){final boolean[] value={false};main(()->value[0]=test.getAsBoolean());return value[0];}
    private boolean await(BooleanSupplier test,long milliseconds){
        long until=SystemClock.uptimeMillis()+milliseconds;
        while(SystemClock.uptimeMillis()<until){if(test.getAsBoolean())return true;SystemClock.sleep(40);}
        return test.getAsBoolean();
    }
    private void check(boolean value){if(!value)throw new IllegalStateException("E_HOST_ASSERTION");}
    private void choose(String kind,String fixture,int cursor){
        step="focus";main(()->activity.selectCase(kind,fixture,cursor,true));
        step="new-editor-served";check(await(()->read(()->activity.getSystemService(InputMethodManager.class).isActive(activity.editor)),3000));
        step="ime-insets-visible";check(await(()->read(()->activity.getWindow().getDecorView().getRootWindowInsets()!=null &&
            activity.getWindow().getDecorView().getRootWindowInsets().isVisible(WindowInsets.Type.ime())),5000));
        step="ime-animation-end";check(await(()->read(()->!activity.imeAnimationRunning),3000));
        // Accessibility exposes new keys before the parent window resize/translation commits.
        // Require the actual bottom-gravity window to reach the display edge, with safe keys.
        step="ime-frame-coherent";check(await(this::frameCoherent,3000));
    }
    private Rect key(String label,boolean bottom){
        Rect found=null;imeWindows=0;ownImeRoot=false;
        for(AccessibilityWindowInfo window:automation.getWindows()){
            if(window.getType()!=AccessibilityWindowInfo.TYPE_INPUT_METHOD)continue;
            imeWindows++;
            AccessibilityNodeInfo root=window.getRoot();
            if(root==null)continue;
            try{
                if(!IME.equals(String.valueOf(root.getPackageName())))continue;
                ownImeRoot=true;
                if(ownFrameGeometry.length()<2){Rect r=new Rect();window.getBoundsInScreen(r);ownFrameGeometry.put(new JSONArray().put(r.left).put(r.top).put(r.right).put(r.bottom));root.getBoundsInScreen(r);ownFrameGeometry.put(new JSONArray().put(r.left).put(r.top).put(r.right).put(r.bottom));}
                ArrayDeque<AccessibilityNodeInfo> queue=new ArrayDeque<>();queue.add(AccessibilityNodeInfo.obtain(root));
                while(!queue.isEmpty()){
                    AccessibilityNodeInfo node=queue.removeFirst();
                    try{
                        if(label.equals("切换输入法") && "globe".equals(node.getExtras().getString("dev.local.iosninekeyime.key_id")) && ownFrameGeometry.length()<8){Rect r=new Rect();node.getBoundsInScreen(r);ownFrameGeometry.put(new JSONArray().put("globe").put(node.isVisibleToUser()).put(node.isEnabled()).put(node.isClickable()).put(r.left).put(r.top).put(r.right).put(r.bottom));}
                        boolean match=label.startsWith("key-id:")?label.substring(7).equals(node.getExtras().getString("dev.local.iosninekeyime.key_id")):label.contentEquals(node.getContentDescription());
                        if(IME.contentEquals(node.getPackageName()) && node.isVisibleToUser() && node.isEnabled() && node.isClickable() && match){
                            Rect bounds=new Rect();node.getBoundsInScreen(bounds);
                            if(!bounds.isEmpty() && (found==null || (bottom && bounds.centerY()>found.centerY())))found=bounds;
                            if(label.equals("收起") && ownFrameGeometry.length()<8){
                                AccessibilityNodeInfo parent=node.getParent();
                                while(parent!=null && ownFrameGeometry.length()<8){Rect r=new Rect();parent.getBoundsInScreen(r);ownFrameGeometry.put(new JSONArray().put(r.left).put(r.top).put(r.right).put(r.bottom));AccessibilityNodeInfo next=parent.getParent();parent.recycle();parent=next;}
                                if(parent!=null)parent.recycle();
                            }
                        }
                        for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo child=node.getChild(i);if(child!=null)queue.addLast(child);}
                    }finally{node.recycle();}
                }
            }finally{root.recycle();}
        }
        return found;
    }
    private boolean frameCoherent(){
        Rect last=key("切换输入法",true);if(last==null)return false;
        final int[] bottom={0},displayBottom={0};main(()->{bottom[0]=activity.getWindow().getDecorView().getRootWindowInsets().getInsets(WindowInsets.Type.systemBars()).bottom;displayBottom[0]=activity.getSystemService(WindowManager.class).getCurrentWindowMetrics().getBounds().bottom;});
        expectedGap=bottom[0];
        for(AccessibilityWindowInfo window:automation.getWindows()){
            if(window.getType()!=AccessibilityWindowInfo.TYPE_INPUT_METHOD)continue;
            AccessibilityNodeInfo root=window.getRoot();if(root==null)continue;
            boolean own=IME.equals(String.valueOf(root.getPackageName()));root.recycle();if(!own)continue;
            Rect frame=new Rect();window.getBoundsInScreen(frame);measuredGap=frame.bottom-last.bottom;
            if(ownFrameGeometry.length()==0){
                // Geometry only: never enumerate text, candidate labels or host nodes.
                ownFrameGeometry.put(new JSONArray().put(frame.left).put(frame.top).put(frame.right).put(frame.bottom));
                AccessibilityNodeInfo chain=window.getRoot();
                if(chain!=null){Rect r=new Rect();chain.getBoundsInScreen(r);ownFrameGeometry.put(new JSONArray().put(r.left).put(r.top).put(r.right).put(r.bottom));chain.recycle();}
            }
            // IME navigation controls may require more space than the host gesture pill.
            // The host inset is a minimum safety bound, never an equality assumption.
            boolean safe=frame.bottom==displayBottom[0] && frame.contains(last) && measuredGap>=expectedGap-2;
            if(!safe)return false;
            // This scope keeps orientation/navigation mode fixed. Learn its actual IME margin
            // after entry animation, then require it to remain unchanged across layout heights.
            // A shrinking keyboard can otherwise leave new keys above an old window bottom.
            if(navigationGap<0){stableGapSamples=lastGap==measuredGap?stableGapSamples+1:1;lastGap=measuredGap;if(stableGapSamples<3)return false;navigationGap=measuredGap;}
            return Math.abs(measuredGap-navigationGap)<=2;
        }
        return false;
    }
    private void tap(String label){tap(label,false);}
    private void tap(String label,boolean bottom){
        taps++;step="find-key-node";final Rect[] bounds={null};
        check(await(()->{bounds[0]=key(label,bottom);return bounds[0]!=null;},6000));
        step="key-frame-coherent";check(await(this::frameCoherent,3000));
        bounds[0]=key(label,bottom);check(bounds[0]!=null);
        long now=SystemClock.uptimeMillis();
        MotionEvent down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,bounds[0].centerX(),bounds[0].centerY(),0);
        MotionEvent up=MotionEvent.obtain(now,now+20,MotionEvent.ACTION_UP,bounds[0].centerX(),bounds[0].centerY(),0);
        down.setSource(InputDevice.SOURCE_TOUCHSCREEN);up.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        try{step="inject-down";check(automation.injectInputEvent(down,true));step="inject-up";check(automation.injectInputEvent(up,true));injected++;step="settle";}finally{down.recycle();up.recycle();}
        SystemClock.sleep(55);
    }
    private void word(){for(String label:new String[]{"MNO","GHI","GHI","ABC","MNO"})tap(label);}
    private JSONObject capture(String id)throws Exception{
        check(await(this::frameCoherent,3000));
        check(id.matches("r0[1-5]-(chinese|number|symbols|latin|emoji|pressed)"));
        for(AccessibilityWindowInfo window:automation.getWindows()){
            if(window.getType()!=AccessibilityWindowInfo.TYPE_INPUT_METHOD)continue;
            AccessibilityNodeInfo root=window.getRoot();if(root==null)continue;
            if(!IME.equals(String.valueOf(root.getPackageName()))){root.recycle();continue;}
            Rect region=new Rect();window.getBoundsInScreen(region);Bitmap screen=automation.takeScreenshot();check(screen!=null);
            region.intersect(0,0,screen.getWidth(),screen.getHeight());check(!region.isEmpty());
            Bitmap crop=Bitmap.createBitmap(screen,region.left,region.top,region.width(),region.height());
            JSONObject keys=new JSONObject();ArrayDeque<AccessibilityNodeInfo> queue=new ArrayDeque<>();queue.add(root);
            while(!queue.isEmpty()){
                AccessibilityNodeInfo node=queue.removeFirst();
                try{
                    String keyId=node.getExtras().getString("dev.local.iosninekeyime.key_id");
                    if(IME.equals(String.valueOf(node.getPackageName())) && node.isVisibleToUser() && node.isClickable() && keyId!=null && keyId.matches("[a-z0-9-]{1,80}") && !keyId.startsWith("candidate-")){
                        Rect r=new Rect();node.getBoundsInScreen(r);r.offset(-region.left,-region.top);
                        keys.put(keyId,new JSONArray().put(r.left).put(r.top).put(r.right).put(r.bottom));
                    }
                    for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo child=node.getChild(i);if(child!=null)queue.add(child);}
                }finally{node.recycle();}
            }
            String file="host-"+nonce+"-"+id+".png";
            try(FileOutputStream stream=new FileOutputStream(new File(getTargetContext().getCacheDir(),file))){check(crop.compress(Bitmap.CompressFormat.PNG,100,stream));}
            JSONObject record=new JSONObject().put("id",id).put("width",crop.getWidth()).put("height",crop.getHeight()).put("keys",keys);
            if(keys.has("digit-6")){
                JSONArray box=keys.getJSONArray("digit-6");int x=box.getInt(0)+(box.getInt(2)-box.getInt(0))/5,y=box.getInt(1)+(box.getInt(3)-box.getInt(1))/5;
                int color=crop.getPixel(x,y);record.put("synthetic_key_fill_rgb",new JSONArray().put(android.graphics.Color.red(color)).put(android.graphics.Color.green(color)).put(android.graphics.Color.blue(color)));
            }
            if(crop!=screen)crop.recycle();screen.recycle();captures.put(record);return record;
        }
        throw new IllegalStateException("E_OWN_IME_CAPTURE");
    }
    private GuardedEditText guarded(){final GuardedEditText[] view={null};main(()->view[0]=(GuardedEditText)activity.editor);return view[0];}
    private JSONObject privacyCase(String id,boolean exact,GuardedEditText view)throws JSONException{
        final int[] n=new int[5];main(()->{n[0]=view.bodyReads;n[1]=view.cursorRequests;n[2]=view.boundsRequests;n[3]=view.boundsDelivered;n[4]=view.gestureRequests;});
        return new JSONObject().put("id",id).put("exact_synthetic_result",exact).put("body_getter_calls",n[0]).put("cursor_requests",n[1]).put("bounds_requests",n[2]).put("bounds_delivered",n[3]).put("gesture_requests",n[4]).put("pass",exact && n[0]==0);
    }
    @Override public void onStart(){
        JSONObject output=new JSONObject();JSONArray cases=new JSONArray();boolean passed=false;
        try{
            check(nonce!=null && nonce.matches("[a-f0-9]{32}"));
            check(IME.equals("dev.local.iosninekeyime.probe.debug") || IME.equals("dev.local.iosninekeyime.probe"));
            String component=IME+"/dev.local.iosninekeyime.ime.NineKeyInputMethodService";
            boolean selected=component.equals(Settings.Secure.getString(getTargetContext().getContentResolver(),Settings.Secure.DEFAULT_INPUT_METHOD));
            output.put("scope",scope).put("system_ime_selected",selected).put("cursor_metadata_bridge",false);
            if(!scope.equals("readiness") && !selected){output.put("status","BLOCKED").put("reason","Own debug IME must be enabled and selected before system tests");return;}
            Intent intent=new Intent(getTargetContext(),HostActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity=(HostActivity)startActivitySync(intent);
            check(await(()->read(()->activity.hasWindowFocus()),3000));
            output.put("separate_host_uid",getTargetContext().getApplicationInfo().uid!=getTargetContext().getPackageManager().getApplicationInfo(IME,0).uid);
            if(scope.equals("readiness")){
                // API37 TextView adds this documented accessibility/candidate-selection flag.
                // Preserve every requested bit; permit only the runtime's named public flag.
                final int platformAddedFlag=Build.VERSION.SDK_INT>=37?InputType.class.getField("TYPE_TEXT_FLAG_ENABLE_TEXT_SUGGESTION_SELECTED").getInt(null):0;
                for(String kind:new String[]{"plain","password","visible-password","web-password","number-password","no-learn","number","phone","email","url","multiline","send","search","next","go"}){
                    stage="metadata-"+kind;final boolean[] valid={false},present={false};final int[] metadata=new int[4];
                    main(()->{
                        activity.selectCase(kind,"",0,false);EditorInfo info=new EditorInfo();
                        present[0]=activity.editor.onCreateInputConnection(info)!=null;
                        metadata[0]=info.inputType;metadata[1]=activity.editor.getInputType();metadata[2]=info.imeOptions & EditorInfo.IME_MASK_ACTION;metadata[3]=activity.editor.getImeOptions() & EditorInfo.IME_MASK_ACTION;
                        valid[0]=present[0] && (metadata[0] & ~platformAddedFlag)==metadata[1] && metadata[2]==metadata[3];
                        if(kind.equals("password") || kind.equals("web-password") || kind.equals("number-password"))valid[0]&=activity.editor.getTransformationMethod()instanceof PasswordTransformationMethod;
                        if(kind.equals("no-learn"))valid[0]&=(info.imeOptions & EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING)!=0;
                    });
                    cases.put(new JSONObject().put("id",stage).put("connection_present",present[0]).put("actual_input_type",metadata[0]).put("requested_input_type",metadata[1]).put("permitted_platform_added_flag",platformAddedFlag).put("actual_action",metadata[2]).put("requested_action",metadata[3]).put("pass",valid[0]));
                }
                stage="local-webview";main(()->activity.showWeb());
                boolean loaded=await(()->read(()->"Synthetic offline host".equals(activity.web.getTitle())),5000);
                boolean isolated=read(()->!activity.web.getSettings().getJavaScriptEnabled() && activity.web.getSettings().getBlockNetworkLoads() && !activity.web.getSettings().getAllowFileAccess() && !activity.web.getSettings().getAllowContentAccess());
                cases.put(new JSONObject().put("id",stage).put("local_page_loaded",loaded).put("isolated",isolated).put("pass",loaded&&isolated));
                output.put("system_ime_path_exercised",false);
            }else{
                automation=getUiAutomation();AccessibilityServiceInfo service=automation.getServiceInfo();service.flags|=AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;automation.setServiceInfo(service);
                if(scope.equals("core")){
                    stage="native-nine-key";choose("plain","",0);word();tap("你好");
                    boolean committed=await(()->read(()->activity.editor.getText().toString().equals("你好")),3000);
                    tap("，");boolean punctuation=await(()->read(()->activity.editor.getText().toString().equals("你好，")),3000);
                    cases.put(new JSONObject().put("id",stage).put("single_commit",committed).put("idle_punctuation",punctuation).put("pass",committed&&punctuation));
                    stage="latin";choose("email","",0);tap("a");tap("b");tap("c");boolean latin=await(()->read(()->activity.editor.getText().toString().equals("abc")),3000);
                    cases.put(new JSONObject().put("id",stage).put("pass",latin));
                    for(String kind:new String[]{"number","phone"}){
                        stage="direct-"+kind;choose(kind,"",0);tap("1");tap("2");tap("3");
                        cases.put(new JSONObject().put("id",stage).put("pass",await(()->read(()->activity.editor.getText().toString().equals("123")),3000)));
                    }
                    for(String[] plan:new String[][]{{"send","发送","4"},{"search","搜索","3"},{"next","下一项","5"},{"go","前往","2"},{"plain","完成","6"}}){
                        stage="action-"+plan[0];choose(plan[0],"",0);tap(plan[1]);
                        boolean action=await(()->read(()->activity.actionCount==1 && activity.lastAction==Integer.parseInt(plan[2]) && activity.editor.getText().length()==0),3000);
                        final int[] counts=new int[3];main(()->{counts[0]=activity.actionCount;counts[1]=activity.lastAction;counts[2]=activity.editor.length();});
                        cases.put(new JSONObject().put("id",stage).put("action_count",counts[0]).put("last_action",counts[1]).put("remaining_length",counts[2]).put("pass",action));
                    }
                    stage="composition-enter";choose("send","",0);word();check(await(()->key("你好",false)!=null,6000));tap("发送");
                    boolean confirm=await(()->read(()->activity.editor.getText().toString().equals("你好") && activity.actionCount==0),3000);
                    tap("发送");boolean sent=await(()->read(()->activity.actionCount==1 && activity.lastAction==EditorInfo.IME_ACTION_SEND),3000);
                    cases.put(new JSONObject().put("id",stage).put("confirm_only_first_enter",confirm).put("action_only_second_enter",sent).put("pass",confirm&&sent));
                }else if(scope.equals("private-delete")){
                    String[] clusters={"\uD83D\uDE80","e\u0301","\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66","\uD83C\uDDE8\uD83C\uDDF3","\uD83D\uDC4D\uD83C\uDFFD","a\u0301\u0327","\u05D0\u05B7","\u79B0\uDB40\uDD00","q"};
                    for(String kind:new String[]{"password","visible-password","web-password","no-learn"})for(int index=0;index<clusters.length;index++){
                        stage=kind+"-g"+(index+1);String cluster=clusters[index];choose(kind,"A"+cluster+"B",1+cluster.length());
                        tap("退格");boolean exact=await(()->read(()->activity.editor.getText().toString().equals("AB") && activity.editor.getSelectionStart()==1 && activity.editor.getSelectionEnd()==1),3000);
                        cases.put(new JSONObject().put("id",stage).put("exact_synthetic_remaining",exact).put("pass",exact));
                    }
                }else if(scope.equals("lifecycle")){
                    for(int i=1;i<=5;i++){
                        stage="hide-show-"+i;choose("send","",0);
                        main(()->activity.getSystemService(InputMethodManager.class).hideSoftInputFromWindow(activity.editor.getWindowToken(),0));
                        boolean hidden=await(()->read(()->!activity.getWindow().getDecorView().getRootWindowInsets().isVisible(WindowInsets.Type.ime()) && !activity.imeAnimationRunning),5000);
                        check(hidden);choose("send","",0);word();tap("你好");tap("，");
                        boolean exact=await(()->read(()->activity.editor.getText().toString().equals("你好，")),3000);
                        cases.put(new JSONObject().put("id",stage).put("hidden_confirmed",hidden).put("shown_own_ime_frame_safe",frameCoherent()).put("pass",exact && frameCoherent()));
                    }
                }else if(scope.equals("reference-ui")){
                    stage="reference-chinese";choose("send","",0);check(await(()->key("，",false)!=null,6000));JSONObject normal=capture("r01-chinese");
                    cases.put(new JSONObject().put("id",stage).put("pass",key("MNO",false)!=null && key("#@¥",false)!=null && key("选拼音",false)!=null));
                    stage="reference-highlight";Rect pressed=key("MNO",false);check(pressed!=null);long now=SystemClock.uptimeMillis();
                    MotionEvent down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,pressed.centerX(),pressed.centerY(),0);down.setSource(InputDevice.SOURCE_TOUCHSCREEN);JSONObject highlighted;
                    try{check(automation.injectInputEvent(down,true));SystemClock.sleep(100);highlighted=capture("r01-pressed");}
                    finally{MotionEvent cancel=MotionEvent.obtain(now,SystemClock.uptimeMillis(),MotionEvent.ACTION_CANCEL,pressed.centerX(),pressed.centerY(),0);cancel.setSource(InputDevice.SOURCE_TOUCHSCREEN);try{check(automation.injectInputEvent(cancel,true));}finally{cancel.recycle();down.recycle();}}
                    boolean colorChanged=!normal.getJSONArray("synthetic_key_fill_rgb").toString().equals(highlighted.getJSONArray("synthetic_key_fill_rgb").toString());
                    cases.put(new JSONObject().put("id",stage).put("physical_press_changes_fill",colorChanged).put("pass",colorChanged && read(()->activity.editor.length()==0) && key("，",false)!=null));
                    stage="reference-number";tap("123");check(await(()->key("0",false)!=null,3000));capture("r02-number");cases.put(new JSONObject().put("id",stage).put("pass",key("1",false)!=null && key("拼音",false)!=null && key("更多",false)!=null));
                    stage="reference-symbols";tap("#@¥");check(await(()->key("#+=",false)!=null,3000));capture("r03-symbols");cases.put(new JSONObject().put("id",stage).put("pass",key("9",false)!=null && key("/",false)!=null && key("拼音",false)!=null));
                    stage="reference-return-chinese";tap("拼音");boolean back=await(()->key("MNO",false)!=null,3000);cases.put(new JSONObject().put("id",stage).put("pass",back));
                    stage="reference-latin";tap("key-id:layout-latin");check(await(()->key("q",false)!=null,3000));capture("r04-latin");cases.put(new JSONObject().put("id",stage).put("pass",key("a",false)!=null && key("z",false)!=null && key("大写",false)!=null));
                    // Emoji reference was opened from Chinese, so its return key must be 拼音.
                    stage="reference-emoji";tap("123");check(await(()->key("#+=",false)!=null,3000));tap("ABC");check(await(()->key("q",false)!=null,3000));
                    // Go through a fresh Chinese field without manufacturing reducer events.
                    choose("send","",0);check(await(()->key("，",false)!=null,6000));tap("表情");check(await(()->key("搜索表情",false)!=null,3000));capture("r05-emoji");
                    cases.put(new JSONObject().put("id",stage).put("pass",key("😀",false)!=null && key("拼音",false)!=null));
                    stage="reference-emoji-commit-delete";tap("😀");boolean once=await(()->read(()->activity.editor.getText().toString().equals("😀")),3000);tap("退格");boolean deleted=await(()->read(()->activity.editor.length()==0),3000);
                    cases.put(new JSONObject().put("id",stage).put("pass",once && deleted));
                    stage="reference-emoji-return";tap("拼音");cases.put(new JSONObject().put("id",stage).put("pass",await(()->key("MNO",false)!=null,3000)));
                }else if(scope.equals("privacy-queue")){
                    main(()->activity.guardConnections=true);
                    String[] kinds={"password","visible-password","web-password","number-password","no-learn"};
                    String[] clusters={"e\u0301","\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66"};
                    for(String kind:kinds)for(int i=0;i<clusters.length;i++){
                        stage="guard-"+kind+"-g"+(i+1);String cluster=clusters[i];choose(kind,"A"+cluster+"B",1+cluster.length());GuardedEditText view=guarded();tap("退格");
                        boolean exact=await(()->read(()->view.getText().toString().equals("AB") && view.getSelectionStart()==1 && view.getSelectionEnd()==1),3000);
                        cases.put(privacyCase(stage,exact,view));
                    }
                    for(String kind:kinds){
                        stage="selection-"+kind;choose(kind,"ABCD",3);GuardedEditText view=guarded();main(()->view.setSelection(1,3));
                        check(await(()->read(()->view.getSelectionStart()==1 && view.getSelectionEnd()==3),1000));
                        // Let the actual selection callback be dispatched before the physical key.
                        automation.waitForIdle(100,3000);tap("退格");
                        cases.put(privacyCase(stage,await(()->read(()->view.getText().toString().equals("AD") && view.getSelectionStart()==1 && view.getSelectionEnd()==1),3000),view));
                    }
                    for(String kind:new String[]{"password","no-learn"}){
                        stage="queued-"+kind;main(()->activity.nextBoundsDelayMs=300);choose(kind,"AB",2);GuardedEditText view=guarded();tap("退格");
                        boolean pending=await(()->read(()->view.boundsRequests==1 && view.boundsDelivered==0),1000);tap("a");tap("b");
                        boolean exact=await(()->read(()->view.getText().toString().equals("Aab") && view.getSelectionStart()==3 && view.getSelectionEnd()==3),3000);
                        JSONObject result=privacyCase(stage,pending && exact,view);result.put("observed_query_pending_before_typing",pending);cases.put(result);
                    }
                    stage="normal-delete-typing";main(()->activity.nextBoundsDelayMs=0);choose("email","AB",2);GuardedEditText normal=guarded();tap("退格");tap("a");tap("b");
                    cases.put(new JSONObject().put("id",stage).put("normal_body_reads_permitted",true).put("pass",await(()->read(()->normal.getText().toString().equals("Aab") && normal.getSelectionStart()==3),3000)));
                    stage="held-private-delete";choose("password","ABCDEFGH",8);GuardedEditText held=guarded();Rect box=key("退格",false);check(box!=null);long now=SystemClock.uptimeMillis();
                    MotionEvent down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,box.centerX(),box.centerY(),0);down.setSource(InputDevice.SOURCE_TOUCHSCREEN);
                    boolean empty=false;
                    try{check(automation.injectInputEvent(down,true));empty=await(()->read(()->held.length()==0),2500);}
                    finally{MotionEvent up=MotionEvent.obtain(now,SystemClock.uptimeMillis(),MotionEvent.ACTION_UP,box.centerX(),box.centerY(),0);up.setSource(InputDevice.SOURCE_TOUCHSCREEN);try{automation.injectInputEvent(up,true);}finally{up.recycle();down.recycle();}}
                    cases.put(privacyCase(stage,empty,held));
                    stage="pending-cross-field";main(()->activity.nextBoundsDelayMs=300);choose("password","AB",2);GuardedEditText previous=guarded();tap("退格");
                    boolean pending=await(()->read(()->previous.boundsRequests==1 && previous.boundsDelivered==0),1000);
                    main(()->activity.nextBoundsDelayMs=0);choose("password","D",1);GuardedEditText next=guarded();tap("c");
                    boolean delivered=await(()->read(()->previous.boundsDelivered==1),1500);
                    boolean isolated=read(()->previous.getText().toString().equals("AB") && next.getText().toString().equals("Dc") && next.getSelectionStart()==2);
                    JSONObject result=privacyCase(stage,pending && delivered && isolated && previous.bodyReads==0,next);result.put("observed_old_query_pending",pending).put("old_result_delivered",delivered).put("old_field_unchanged",read(()->previous.getText().toString().equals("AB")));cases.put(result);
                }else throw new IllegalStateException("E_HOST_SCOPE");
                output.put("system_ime_path_exercised",true);
            output.put("actual_ime_bottom_margin",navigationGap).put("host_minimum_bottom_margin",expectedGap);
            }
            passed=cases.length()>0;
            int success=0;for(int i=0;i<cases.length();i++){boolean ok=cases.getJSONObject(i).getBoolean("pass");passed&=ok;if(ok)success++;}
            output.put("passed_cases",success).put("planned_cases",scope.equals("readiness")?16:scope.equals("core")?10:scope.equals("reference-ui")?9:scope.equals("lifecycle")?5:scope.equals("privacy-queue")?20:36);
            passed&=cases.length()==output.getInt("planned_cases");
            output.put("status",passed?"PASS":"FAIL");
        }catch(Throwable failure){try{output.put("status","FAIL").put("failure_stage",stage).put("failure_step",step).put("tap_attempts",taps).put("injected_taps",injected).put("measured_bottom_gap",measuredGap).put("expected_bottom_gap",expectedGap).put("ime_window_root_geometry",ownFrameGeometry).put("ime_windows",imeWindows).put("own_ime_root",ownImeRoot).put("error_code","E_SYSTEM_HOST_ASSERTION");}catch(JSONException ignored){}}
        finally{
            try{if(captures.length()>0)output.put("reference_captures",captures);}catch(JSONException ignored){}
            if(!passed && automation!=null){
                // Own service overrides dump to expose keyboard window geometry ONLY, no super dump.
                try{
                    android.content.pm.ApplicationInfo info=getTargetContext().getPackageManager().getApplicationInfo(IME,android.content.pm.PackageManager.GET_META_DATA);
                    check(info.metaData!=null && info.metaData.getInt("dev.local.iosninekeyime.layout_metadata_version",0)==1);
                    try(android.os.ParcelFileDescriptor fd=automation.executeShellCommand("dumpsys -t 3 activity service "+IME+"/dev.local.iosninekeyime.ime.NineKeyInputMethodService");
                    java.io.BufferedReader reader=new java.io.BufferedReader(new java.io.InputStreamReader(new android.os.ParcelFileDescriptor.AutoCloseInputStream(fd)))){
                    String line;while((line=reader.readLine())!=null){int at=line.indexOf("NINEKEY_LAYOUT_META ");if(at>=0){output.put("window_layout_metadata",new JSONObject(line.substring(at+20)));break;}}
                    }
                }catch(Throwable ignored){}
            }
            if(!passed && automation!=null){
                // Crop the synthetic IME window only, excluding host/status-bar pixels. Never Git.
                try{
                    if(output.has("window_layout_metadata") && read(()->activity.hasWindowFocus())){
                        JSONObject meta=output.getJSONObject("window_layout_metadata");JSONObject keyboard=meta.optJSONObject("keyboard");
                        if(keyboard!=null){
                            int expected=keyboard.getInt("height");Bitmap full=automation.takeScreenshot();
                            if(full!=null){
                                if(expected>100 && expected<full.getHeight()-200){
                                    Bitmap bottom=Bitmap.createBitmap(full,0,full.getHeight()-expected,full.getWidth(),expected);
                                    try(FileOutputStream stream=new FileOutputStream(new File(getTargetContext().getCacheDir(),"host-"+nonce+"-bottom-diagnostic.png"))){bottom.compress(Bitmap.CompressFormat.PNG,100,stream);}
                                    output.put("debug_owned_bottom_crop_saved",true).put("debug_owned_bottom_crop_basis","requested keyboard height; may include owned synthetic host; NOT reference acceptance");
                                    if(bottom!=full)bottom.recycle();
                                }
                                full.recycle();
                            }
                        }
                    }
                    for(AccessibilityWindowInfo window:automation.getWindows()){
                        if(window.getType()!=AccessibilityWindowInfo.TYPE_INPUT_METHOD)continue;
                        AccessibilityNodeInfo node=window.getRoot();
                        if(node==null)continue;
                        boolean own=IME.equals(String.valueOf(node.getPackageName()));node.recycle();if(!own)continue;
                        Rect region=new Rect();window.getBoundsInScreen(region);
                        Bitmap screen=automation.takeScreenshot();
                        if(screen!=null){
                            region.intersect(0,0,screen.getWidth(),screen.getHeight());
                            if(!region.isEmpty()){
                                Bitmap crop=Bitmap.createBitmap(screen,region.left,region.top,region.width(),region.height());
                                try(FileOutputStream stream=new FileOutputStream(new File(getTargetContext().getCacheDir(),"synthetic-ime-failure.png"))){crop.compress(Bitmap.CompressFormat.PNG,100,stream);}
                                if(crop!=screen)crop.recycle();output.put("debug_ime_crop_saved",true);
                            }
                            screen.recycle();
                        }
                        break;
                    }
                }catch(Throwable ignored){}
            }
            if(activity!=null){
                main(()->{try{output.put("ime_animation_starts",activity.imeAnimationStarts).put("ime_animation_ends",activity.imeAnimationEnds).put("ime_animation_running_at_finish",activity.imeAnimationRunning);}catch(JSONException ignored){}});
                main(()->activity.finish());
            }
            try{output.put("cases",cases).put("pass",passed).put("host_run_id",nonce).put("fixture_source","fixed synthetic values only; no body/candidate dumps");}catch(JSONException ignored){}
            Bundle result=new Bundle();result.putString("stream",output.toString()+"\n");result.putBoolean("host_pass",passed);
            finish(passed?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);
        }
    }
}
