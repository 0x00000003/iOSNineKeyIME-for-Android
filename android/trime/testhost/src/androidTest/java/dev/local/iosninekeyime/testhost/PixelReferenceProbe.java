// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.testhost;

import android.app.Instrumentation;
import android.app.UiAutomation;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.WindowInsets;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.view.inputmethod.InputMethodManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.util.LinkedHashMap;
import java.util.function.BooleanSupplier;

/** Independent external pixel/physical-touch oracle. Never replaces accessibility/reshow gates. */
final class PixelReferenceProbe {
    private final Instrumentation instrumentation;
    private final UiAutomation automation;
    private final HostActivity activity;
    private final String ime,nonce;
    private final JSONArray cases=new JSONArray(),captures=new JSONArray();
    private String stage="setup",panel="chinese",detail="setup";
    private int taps;
    PixelReferenceProbe(Instrumentation i,UiAutomation u,HostActivity a,String p,String n){instrumentation=i;automation=u;activity=a;ime=p;nonce=n;}
    private void check(boolean valid){if(!valid)throw new IllegalStateException("E_PIXEL_ASSERTION");}
    private void checkAt(boolean valid,String point){if(!valid){detail=point;check(false);}}
    private void main(Runnable task){instrumentation.runOnMainSync(task);}
    private boolean read(BooleanSupplier task){boolean[] result={false};main(()->result[0]=task.getAsBoolean());return result[0];}
    private boolean await(BooleanSupplier task,long timeout){long until=SystemClock.uptimeMillis()+timeout;while(SystemClock.uptimeMillis()<until){if(task.getAsBoolean())return true;SystemClock.sleep(40);}return task.getAsBoolean();}
    private boolean ownIme(){for(AccessibilityWindowInfo w:automation.getWindows()){if(w.getType()!=AccessibilityWindowInfo.TYPE_INPUT_METHOD)continue;AccessibilityNodeInfo r=w.getRoot();if(r==null)continue;boolean own=ime.equals(String.valueOf(r.getPackageName()));r.recycle();if(own)return true;}return false;}
    private int top(){switch(panel){case "number":case "chinese":return 714;case "symbols":return 724;case "latin":return 783;default:return 693;}}
    private final class Shot implements AutoCloseable {
        final Bitmap bitmap;final int[] pixels;final float scale;final int screenTop;
        Shot(Bitmap b,int t){bitmap=b;screenTop=t;scale=b.getWidth()/591f;pixels=new int[b.getWidth()*b.getHeight()];b.getPixels(pixels,0,b.getWidth(),0,0,b.getWidth(),b.getHeight());}
        int color(int x,int y){return pixels[y*bitmap.getWidth()+x];}
        boolean neutral(int c,int fill){int r=(c>>16)&255,g=(c>>8)&255,b=c&255;return Math.abs(r-fill)<=3 && Math.abs(g-fill)<=3 && Math.abs(b-fill)<=3;}
        Rect scaled(float[] source){return new Rect(Math.round(source[0]*scale),Math.round((source[1]-top())*scale),Math.round(source[2]*scale),Math.round((source[3]-top())*scale));}
        Rect locate(String id,int fill){
            float[] source=boxes().get(id);checkAt(source!=null,"locate-key-absent");Rect expected=scaled(source);int margin=Math.max(1,Math.round(2*scale));
            Rect scan=new Rect(expected);scan.inset(-margin,-margin);check(scan.intersect(0,0,bitmap.getWidth(),bitmap.getHeight()));
            int l=scan.right,t=scan.bottom,r=-1,b=-1,count=0;
            for(int y=scan.top;y<scan.bottom;y++)for(int x=scan.left;x<scan.right;x++)if(neutral(color(x,y),fill)){l=Math.min(l,x);t=Math.min(t,y);r=Math.max(r,x);b=Math.max(b,y);count++;}
            checkAt(count>expected.width()*expected.height()/3,"locate-fill-count");Rect actual=new Rect(l,t,r+1,b+1);
            checkAt(Math.abs(actual.left-expected.left)<=margin+1 && Math.abs(actual.top-expected.top)<=margin+1 && Math.abs(actual.right-expected.right)<=margin+1 && Math.abs(actual.bottom-expected.bottom)<=margin+1,"locate-fill-geometry");
            return actual;
        }
        boolean emptyHeader(){int white=0;for(int y=Math.round(5*scale);y<Math.round(48*scale);y++)for(int x=Math.round(30*scale);x<Math.round(550*scale);x++){int c=color(x,y);if(((c>>16)&255)>200 && ((c>>8)&255)>200 && (c&255)>200)white++;}return white<10;}
        long headerHash(){long h=17;for(int y=0;y<Math.min(bitmap.getHeight(),Math.round(53*scale));y++)for(int x=0;x<bitmap.getWidth();x+=2)h=h*31+color(x,y);return h;}
        Rect emojiCell(int index){float l=10+(index%7)*(571f/7),t=693+92+(index/7)*84;Rect r=scaled(new float[]{l,t,l+571f/7-2,t+76});int colored=0;for(int y=r.top;y<r.bottom;y++)for(int x=r.left;x<r.right;x++){int c=color(x,y),red=(c>>16)&255,g=(c>>8)&255,b=c&255;if(Math.max(red,Math.max(g,b))-Math.min(red,Math.min(g,b))>35 || Math.max(red,Math.max(g,b))>80)colored++;}check(colored>20);return r;}
        public void close(){bitmap.recycle();}
    }
    private Shot shot(){
        final int[] h={0};checkAt(read(()->{WindowInsets insets=activity.getWindow().getDecorView().getRootWindowInsets();if(insets==null)return false;h[0]=insets.getInsets(WindowInsets.Type.ime()).bottom;return activity.hasWindowFocus() && !activity.imeAnimationRunning && insets.isVisible(WindowInsets.Type.ime());}),"shot-focus-animation-inset");
        checkAt(ownIme(),"shot-own-ime");Bitmap full=automation.takeScreenshot();check(full!=null);checkAt(h[0]>200 && h[0]<full.getHeight()-200,"shot-inset-height");int y=full.getHeight()-h[0];Bitmap crop=Bitmap.createBitmap(full,0,y,full.getWidth(),h[0]);if(crop!=full)full.recycle();Shot s=new Shot(crop,y);
        if(!s.neutral(s.color(s.bitmap.getWidth()/2,Math.max(1,Math.round(5*s.scale))),34)){s.close();detail="shot-panel-fill";check(false);}return s;
    }
    private Shot stableShot(){
        long until=SystemClock.uptimeMillis()+3000,last=0;int repeats=0,lastHeight=-1;
        while(SystemClock.uptimeMillis()<until){
            Shot current=null;
            try{
                current=shot();float sourceHeight=current.bitmap.getHeight()/current.scale;
                checkAt(Math.abs(sourceHeight-(1280-top()))<=2,"shot-reference-height");
                long signature=17;int y0=Math.round((panel.equals("emoji")?92:861-top())*current.scale),y1=Math.min(current.bitmap.getHeight(),Math.round((1172-top())*current.scale));
                for(int y=Math.max(0,y0);y<y1;y+=3)for(int x=0;x<current.bitmap.getWidth();x+=3)signature=31*signature+current.color(x,y);
                if(current.bitmap.getHeight()==lastHeight && signature==last)repeats++;else repeats=1;
                lastHeight=current.bitmap.getHeight();last=signature;
                if(repeats>=3)return current;
            }catch(IllegalStateException ignored){repeats=0;}
            if(current!=null)current.close();SystemClock.sleep(40);
        }
        checkAt(false,"stable-frame-timeout-"+detail);throw new IllegalStateException("E_UNREACHABLE");
    }
    private LinkedHashMap<String,float[]> boxes(){
        LinkedHashMap<String,float[]> m=new LinkedHashMap<>();
        if(panel.equals("chinese") || panel.equals("number")){
            boolean chinese=panel.equals("chinese");float[][] cols={{12,118},{127,233},{243,348},{358,464},{473,580}};
            String[] side={"layout-primary","layout-symbols","layout-latin","emoji"};for(int row=0;row<4;row++)m.put(side[row],new float[]{12,854+82*row,118,925+82*row});
            for(int i=0;i<9;i++){float[] c=cols[1+i%3];int row=i/3;m.put(chinese && i>0?"digit-"+(i+1):"text-"+i,new float[]{c[0],854+82*row,c[1],925+82*row});}
            m.put("delete",new float[]{473,854,580,925});m.put("more",new float[]{473,936,580,1007});m.put("enter",new float[]{473,1017,580,1171});
            if(chinese){m.put("pinyin",new float[]{127,1100,233,1171});m.put("space",new float[]{243,1100,464,1171});}
            else{m.put("number-punctuation",new float[]{127,1100,233,1171});m.put("zero",new float[]{243,1100,348,1171});m.put("space",new float[]{358,1100,464,1171});}
        }else if(panel.equals("latin") || panel.equals("symbols")){
            boolean latin=panel.equals("latin");int index=0;
            for(int i=0;i<10;i++)m.put("text-"+index++,new float[]{10+i*58,861,59+i*58,926});
            for(int i=0;i<(latin?9:10);i++){float x=(latin?39:10)+i*58;m.put("text-"+index++,new float[]{x,943,x+49,1008});}
            if(latin){m.put("shift",new float[]{10,1025,76,1090});for(int i=0;i<7;i++)m.put("text-"+index++,new float[]{97+i*58,1025,146+i*58,1090});m.put("layout-symbols",new float[]{10,1107,73,1172});m.put("emoji",new float[]{82,1107,146,1172});}
            else{m.put("symbol-page",new float[]{10,1025,76,1090});for(int i=0;i<6;i++)m.put("text-"+index++,new float[]{91+i*70,1025,151+i*70,1090});m.put("layout-return",new float[]{10,1107,146,1172});}
            m.put("delete",new float[]{515,1025,581,1090});m.put("space",new float[]{155,1107,436,1172});m.put("enter",new float[]{445,1107,582,1172});
        }else m.put("emoji-search",new float[]{14,707,577,767});
        return m;
    }
    private int fill(String id){return id.equals("enter")?(panel.equals("chinese") || panel.equals("number")?144:122):id.equals("emoji-search")?59:70;}
    private void physical(Rect local,Shot s,int action,long downTime){MotionEvent e=MotionEvent.obtain(downTime,SystemClock.uptimeMillis(),action,local.centerX(),s.screenTop+local.centerY(),0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);try{check(automation.injectInputEvent(e,true));}finally{e.recycle();}}
    private void tap(String id){detail="tap-"+id;try(Shot s=stableShot()){Rect r=s.locate(id,fill(id));long now=SystemClock.uptimeMillis();physical(r,s,MotionEvent.ACTION_DOWN,now);SystemClock.sleep(20);physical(r,s,MotionEvent.ACTION_UP,now);taps++;}SystemClock.sleep(80);}
    private void tapPoint(float x,float absoluteY){try(Shot s=stableShot()){Rect r=new Rect(Math.round(x*s.scale)-1,Math.round((absoluteY-top())*s.scale)-1,Math.round(x*s.scale)+1,Math.round((absoluteY-top())*s.scale)+1);long now=SystemClock.uptimeMillis();physical(r,s,MotionEvent.ACTION_DOWN,now);SystemClock.sleep(20);physical(r,s,MotionEvent.ACTION_UP,now);taps++;}SystemClock.sleep(80);}
    private boolean body(String expected){return read(()->expected.contentEquals(activity.editor.getText()));}
    private void ready(){check(await(()->{try(Shot s=shot()){return s.emptyHeader();}catch(IllegalStateException e){return false;}},15000));}
    private void choose(){main(()->activity.selectCase("send","",0,true));check(await(()->read(()->activity.getSystemService(InputMethodManager.class).isActive(activity.editor) && activity.getWindow().getDecorView().getRootWindowInsets().isVisible(WindowInsets.Type.ime()) && !activity.imeAnimationRunning),5000));panel="chinese";ready();}
    private void capture(String id,String pressed)throws Exception{
        try(Shot s=stableShot()){
            JSONObject keys=new JSONObject();for(String key:boxes().keySet()){Rect r=s.locate(key,key.equals(pressed)?133:fill(key));keys.put(key,new JSONArray().put(r.left).put(r.top).put(r.right).put(r.bottom));}
            if(panel.equals("emoji"))for(int i=0;i<28;i++){Rect r=s.emojiCell(i);keys.put("emoji-item-"+i,new JSONArray().put(r.left).put(r.top).put(r.right).put(r.bottom));}
            try(FileOutputStream stream=new FileOutputStream(new File(instrumentation.getTargetContext().getCacheDir(),"host-"+nonce+"-"+id+".png"))){check(s.bitmap.compress(Bitmap.CompressFormat.PNG,100,stream));}
            JSONObject c=new JSONObject().put("id",id).put("width",s.bitmap.getWidth()).put("height",s.bitmap.getHeight()).put("keys",keys).put("location_method","observed screenshot neutral fill around independent reference boxes; Emoji observed foreground cells");
            if(panel.equals("chinese")){Rect r=s.locate("digit-6",pressed==null?70:133);int color=s.color(r.left+r.width()/5,r.top+r.height()/5);c.put("synthetic_key_fill_rgb",new JSONArray().put((color>>16)&255).put((color>>8)&255).put(color&255));}
            captures.put(c);
        }
    }
    private void result(String id,boolean valid)throws Exception{cases.put(new JSONObject().put("id",id).put("pass",valid));check(valid);}
    private void restoreChoose(String kind,String fixture,int cursor,boolean guard){
        main(()->{activity.guardConnections=guard;activity.selectCase(kind,fixture,cursor,true);});
        check(await(()->read(()->activity.getSystemService(InputMethodManager.class).isActive(activity.editor) && activity.getWindow().getDecorView().getRootWindowInsets().isVisible(WindowInsets.Type.ime()) && !activity.imeAnimationRunning),5000));
        detail="restore-ui-idle";try{automation.waitForIdle(100,3000);}catch(java.util.concurrent.TimeoutException e){throw new IllegalStateException("E_RESTORE_UI_BUSY");}
        panel=kind.equals("number-password")?"number":"latin";
    }
    private void hideSameField(){
        detail="hide-same-field";
        main(()->activity.getSystemService(InputMethodManager.class).hideSoftInputFromWindow(activity.editor.getWindowToken(),0));
        check(await(()->read(()->!activity.getWindow().getDecorView().getRootWindowInsets().isVisible(WindowInsets.Type.ime()) && !activity.imeAnimationRunning),5000));
    }
    private void showSameField(){
        detail="show-same-field";
        main(()->{activity.editor.requestFocus();activity.getSystemService(InputMethodManager.class).showSoftInput(activity.editor,InputMethodManager.SHOW_IMPLICIT);});
        check(await(()->read(()->activity.getWindow().getDecorView().getRootWindowInsets().isVisible(WindowInsets.Type.ime()) && !activity.imeAnimationRunning),5000));
        try{automation.waitForIdle(100,3000);}catch(java.util.concurrent.TimeoutException e){throw new IllegalStateException("E_RESTORE_UI_BUSY");}
    }
    private JSONObject restoreResult(String id,boolean exact,boolean sensitive)throws Exception{
        final int[] counts=new int[4];main(()->{GuardedEditText v=(GuardedEditText)activity.editor;counts[0]=v.bodyReads;counts[1]=v.cursorRequests;counts[2]=v.boundsRequests;counts[3]=v.gestureRequests;});
        boolean valid=exact && (!sensitive || counts[0]==0);
        JSONObject row=new JSONObject().put("id",id).put("exact_synthetic_result",exact).put("sensitive",sensitive).put("body_getter_calls",counts[0]).put("cursor_requests",counts[1]).put("bounds_requests",counts[2]).put("gesture_requests",counts[3]).put("pass",valid);cases.put(row);return row;
    }
    private JSONObject queueResult(String id,boolean exact,GuardedEditText view)throws Exception{
        final int[] counts=new int[5];main(()->{counts[0]=view.bodyReads;counts[1]=view.cursorRequests;counts[2]=view.boundsRequests;counts[3]=view.gestureRequests;counts[4]=view.boundsDelivered;});
        JSONObject row=new JSONObject().put("id",id).put("exact_synthetic_result",exact).put("body_getter_calls",counts[0]).put("cursor_requests",counts[1]).put("bounds_requests",counts[2]).put("gesture_requests",counts[3]).put("bounds_delivered",counts[4]).put("pass",exact && counts[0]==0);cases.put(row);return row;
    }
    private void cachedTap(String id,Shot frame,GuardedEditText view){
        detail="cached-tap-"+id;
        check(read(()->{WindowInsets i=activity.getWindow().getDecorView().getRootWindowInsets();return activity.editor==view && activity.hasWindowFocus() && !activity.imeAnimationRunning && i.isVisible(WindowInsets.Type.ime()) && i.getInsets(WindowInsets.Type.ime()).bottom==frame.bitmap.getHeight();}));
        Rect box=frame.locate(id,fill(id));long now=SystemClock.uptimeMillis();physical(box,frame,MotionEvent.ACTION_DOWN,now);SystemClock.sleep(20);physical(box,frame,MotionEvent.ACTION_UP,now);taps++;
    }
    JSONObject runQueue()throws Exception{
        JSONObject output=new JSONObject().put("transport","observed stable screen pixels + actual system touchscreen/structure; no IC or cursor bridge");
        String[] kinds={"password","visible-password","web-password","number-password","no-learn"};
        String[] clusters={"e\u0301","\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66"};
        try{
            for(String kind:kinds)for(int index=0;index<clusters.length;index++){
                stage="guard-"+kind+"-g"+(index+1);String cluster=clusters[index];restoreChoose(kind,"A"+cluster+"B",1+cluster.length(),true);GuardedEditText view=(GuardedEditText)activity.editor;tap("delete");
                queueResult(stage,await(()->read(()->"AB".contentEquals(view.getText()) && view.getSelectionStart()==1 && view.getSelectionEnd()==1),3000),view);
            }
            for(String kind:kinds){
                stage="selection-"+kind;restoreChoose(kind,"ABCD",3,true);GuardedEditText view=(GuardedEditText)activity.editor;main(()->view.setSelection(1,3));automation.waitForIdle(100,3000);tap("delete");
                queueResult(stage,await(()->read(()->"AD".contentEquals(view.getText()) && view.getSelectionStart()==1 && view.getSelectionEnd()==1),3000),view);
            }
            for(String kind:new String[]{"password","no-learn"}){
                stage="queued-"+kind;main(()->activity.nextBoundsDelayMs=300);restoreChoose(kind,"AB",2,true);GuardedEditText view=(GuardedEditText)activity.editor;
                boolean pending,secondPending;
                try(Shot frame=stableShot()){
                    frame.locate("delete",fill("delete"));frame.locate("text-10",fill("text-10"));frame.locate("text-23",fill("text-23"));
                    cachedTap("delete",frame,view);pending=await(()->read(()->view.boundsRequests==1 && view.boundsDelivered==0),1000);cachedTap("text-10",frame,view);
                    secondPending=read(()->view.boundsRequests==1 && view.boundsDelivered==0);cachedTap("text-23",frame,view);
                }
                boolean exact=await(()->read(()->"Aab".contentEquals(view.getText()) && view.getSelectionStart()==3 && view.getSelectionEnd()==3),3000);
                queueResult(stage,pending && secondPending && exact,view).put("observed_query_pending_before_typing",pending).put("observed_pending_before_second_key",secondPending);
            }
            stage="normal-delete-typing";main(()->activity.nextBoundsDelayMs=0);restoreChoose("email","AB",2,false);tap("delete");tap("text-10");tap("text-23");result(stage,await(()->body("Aab") && read(()->activity.editor.getSelectionStart()==3),3000));
            stage="held-private-delete";restoreChoose("password","ABCDEFGH",8,true);GuardedEditText held=(GuardedEditText)activity.editor;boolean empty;
            try(Shot frame=stableShot()){
                Rect box=frame.locate("delete",fill("delete"));long now=SystemClock.uptimeMillis();physical(box,frame,MotionEvent.ACTION_DOWN,now);
                try{empty=await(()->read(()->held.length()==0),2500);}finally{physical(box,frame,MotionEvent.ACTION_UP,now);}taps++;
            }
            queueResult(stage,empty,held);
            stage="pending-cross-field";main(()->activity.nextBoundsDelayMs=300);restoreChoose("password","AB",2,true);GuardedEditText previous=(GuardedEditText)activity.editor;
            try(Shot frame=stableShot()){cachedTap("delete",frame,previous);}boolean pending=await(()->read(()->previous.boundsRequests==1 && previous.boundsDelivered==0),1000);
            main(()->activity.nextBoundsDelayMs=0);restoreChoose("password","D",1,true);GuardedEditText next=(GuardedEditText)activity.editor;tap("text-21");
            boolean delivered=await(()->read(()->previous.boundsDelivered==1),1500),unchanged=read(()->"AB".contentEquals(previous.getText()));
            boolean exact=read(()->unchanged && "Dc".contentEquals(next.getText()) && next.getSelectionStart()==2 && previous.bodyReads==0);
            queueResult(stage,pending && delivered && exact,next).put("observed_old_query_pending",pending).put("old_result_delivered",delivered).put("old_field_unchanged",unchanged);
            boolean valid=cases.length()==20;for(int i=0;i<cases.length();i++)valid&=cases.getJSONObject(i).getBoolean("pass");output.put("pass",valid);
        }catch(Throwable failure){output.put("pass",false).put("failure_stage",stage).put("failure_detail",detail).put("error_code","E_PIXEL_QUEUE_ASSERTION");}
        finally{main(()->activity.nextBoundsDelayMs=0);}
        return output.put("cases",cases).put("reference_captures",captures).put("actual_pixel_targeted_taps",taps).put("planned_cases",20);
    }
    JSONObject runPrivate()throws Exception{
        JSONObject output=new JSONObject().put("transport","stable observed pixel frames + touchscreen; actual system structural deletion; all sensitive body getters rejected");
        String[] clusters={"\uD83D\uDE80","e\u0301","\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66","\uD83C\uDDE8\uD83C\uDDF3","\uD83D\uDC4D\uD83C\uDFFD","a\u0301\u0327","\u05D0\u05B7","\u79B0\uDB40\uDD00","q"};
        try{
            for(String kind:new String[]{"password","visible-password","web-password","no-learn"})for(int i=0;i<clusters.length;i++){
                stage=kind+"-g"+(i+1);String cluster=clusters[i];restoreChoose(kind,"A"+cluster+"B",1+cluster.length(),true);tap("delete");
                boolean exact=await(()->body("AB") && read(()->activity.editor.getSelectionStart()==1 && activity.editor.getSelectionEnd()==1),3000);restoreResult(stage,exact,true);
            }
            boolean valid=cases.length()==36;for(int i=0;i<cases.length();i++)valid&=cases.getJSONObject(i).getBoolean("pass");output.put("pass",valid);
        }catch(Throwable e){output.put("pass",false).put("failure_stage",stage).put("failure_detail",detail).put("error_code","E_PIXEL_PRIVATE_ASSERTION");}
        return output.put("cases",cases).put("reference_captures",captures).put("actual_pixel_targeted_taps",taps).put("planned_cases",36);
    }
    JSONObject runRestore()throws Exception{
        JSONObject output=new JSONObject().put("transport","observed pixels + touchscreen + public host IMM hide/show; same EditText identity retained");
        try{
            for(String kind:new String[]{"email","password","no-learn","number-password"}){
                stage="same-field-"+kind;boolean sensitive=!kind.equals("email"),number=kind.equals("number-password");
                restoreChoose(kind,number?"12":"AB",2,sensitive);android.widget.EditText same=activity.editor;
                tap(number?"text-2":"text-10");check(await(()->body(number?"123":"ABa"),3000));
                hideSameField();showSameField();check(read(()->activity.editor==same));tap("delete");
                boolean deleted=await(()->body(number?"12":"AB"),3000);tap(number?"text-3":"text-23");
                boolean exact=await(()->body(number?"124":"ABb") && read(()->activity.editor.getSelectionStart()==3 && activity.editor.getSelectionEnd()==3),3000);
                restoreResult(stage,deleted && exact,sensitive);
            }
            stage="hidden-private-selection";restoreChoose("password","ABCD",4,true);android.widget.EditText same=activity.editor;tap("text-10");check(await(()->body("ABCDa"),3000));
            hideSameField();main(()->activity.editor.setSelection(1));SystemClock.sleep(150);showSameField();check(read(()->activity.editor==same));tap("delete");
            boolean deleted=await(()->body("BCDa") && read(()->activity.editor.getSelectionStart()==0),3000);tap("text-23");
            boolean exact=await(()->body("bBCDa") && read(()->activity.editor.getSelectionStart()==1 && activity.editor.getSelectionEnd()==1),3000);restoreResult(stage,deleted && exact,true);
            boolean valid=cases.length()==5;for(int i=0;i<cases.length();i++)valid&=cases.getJSONObject(i).getBoolean("pass");output.put("pass",valid);
        }catch(Throwable e){output.put("pass",false).put("failure_stage",stage).put("failure_detail",detail).put("error_code","E_PIXEL_RESTORE_ASSERTION");}
        return output.put("cases",cases).put("reference_captures",captures).put("actual_pixel_targeted_taps",taps).put("planned_cases",5);
    }
    JSONObject run()throws Exception{
        JSONObject output=new JSONObject().put("transport","observed screen pixels + actual touchscreen; no accessibility-coordinate or service-state bridge");
        try{
            choose();stage="upper-host-touch";final Rect upper=new Rect();final int[] clicks={0};main(()->{activity.upperPlainButton().getGlobalVisibleRect(upper);clicks[0]=activity.upperButtonClicks;});
            try(Shot s=shot()){check(upper.bottom<s.screenTop);Rect local=new Rect(upper);local.offset(0,-s.screenTop);long now=SystemClock.uptimeMillis();physical(local,s,MotionEvent.ACTION_DOWN,now);physical(local,s,MotionEvent.ACTION_UP,now);}
            boolean routed=await(()->read(()->activity.upperButtonClicks==clicks[0]+1),3000);output.put("physical_upper_host_button_received",routed);check(routed);choose();
            stage="chinese";capture("r01-chinese",null);result(stage,boxes().size()==18 && body(""));
            stage="press-cancel";try(Shot s=shot()){Rect r=s.locate("digit-6",70);long now=SystemClock.uptimeMillis();physical(r,s,MotionEvent.ACTION_DOWN,now);try{SystemClock.sleep(100);capture("r01-pressed","digit-6");}finally{physical(r,s,MotionEvent.ACTION_CANCEL,now);}}result(stage,body(""));
            stage="number";tap("layout-primary");panel="number";capture("r02-number",null);tap("text-0");tap("text-1");tap("text-2");result(stage,boxes().size()==19 && await(()->body("123"),3000));
            stage="symbols";tap("layout-symbols");panel="symbols";capture("r03-symbols",null);tap("text-11");result(stage,boxes().size()==31 && await(()->body("123/"),3000));
            stage="return-chinese";tap("layout-return");panel="chinese";ready();for(String id:new String[]{"digit-6","digit-4","digit-4","digit-2","digit-6"}){long before;try(Shot s=shot()){before=s.headerHash();}tap(id);check(await(()->{try(Shot s=shot()){return s.headerHash()!=before;}},3000));}tapPoint(35,806);result(stage,await(()->body("123/你好"),3000));
            stage="latin";tap("layout-latin");panel="latin";capture("r04-latin",null);tap("text-10");tap("text-23");tap("text-21");result(stage,boxes().size()==32 && await(()->body("123/你好abc"),3000));
            stage="emoji";choose();tap("emoji");panel="emoji";capture("r05-emoji",null);result(stage,body(""));
            stage="emoji-commit-delete";try(Shot s=shot()){Rect r=s.emojiCell(0);long now=SystemClock.uptimeMillis();physical(r,s,MotionEvent.ACTION_DOWN,now);physical(r,s,MotionEvent.ACTION_UP,now);}check(await(()->body("😀"),3000));tapPoint(560,1138);result(stage,await(()->body(""),3000));
            stage="emoji-return";tapPoint(40,1138);panel="chinese";ready();try(Shot s=shot()){s.locate("digit-6",70);}result(stage,body(""));
            output.put("pass",true);
        }catch(Throwable e){output.put("pass",false).put("failure_stage",stage).put("failure_detail",detail).put("error_code","E_PIXEL_REFERENCE_ASSERTION");}
        return output.put("cases",cases).put("reference_captures",captures).put("actual_pixel_targeted_taps",taps).put("planned_cases",9);
    }
}
