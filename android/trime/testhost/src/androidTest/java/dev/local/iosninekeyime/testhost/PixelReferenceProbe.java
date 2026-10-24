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
    private String stage="setup",panel="chinese";
    private int taps;
    PixelReferenceProbe(Instrumentation i,UiAutomation u,HostActivity a,String p,String n){instrumentation=i;automation=u;activity=a;ime=p;nonce=n;}
    private void check(boolean valid){if(!valid)throw new IllegalStateException("E_PIXEL_ASSERTION");}
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
            float[] source=boxes().get(id);check(source!=null);Rect expected=scaled(source);int margin=Math.max(1,Math.round(2*scale));
            Rect scan=new Rect(expected);scan.inset(-margin,-margin);check(scan.intersect(0,0,bitmap.getWidth(),bitmap.getHeight()));
            int l=scan.right,t=scan.bottom,r=-1,b=-1,count=0;
            for(int y=scan.top;y<scan.bottom;y++)for(int x=scan.left;x<scan.right;x++)if(neutral(color(x,y),fill)){l=Math.min(l,x);t=Math.min(t,y);r=Math.max(r,x);b=Math.max(b,y);count++;}
            check(count>expected.width()*expected.height()/3);Rect actual=new Rect(l,t,r+1,b+1);
            check(Math.abs(actual.left-expected.left)<=margin+1 && Math.abs(actual.top-expected.top)<=margin+1 && Math.abs(actual.right-expected.right)<=margin+1 && Math.abs(actual.bottom-expected.bottom)<=margin+1);
            return actual;
        }
        boolean emptyHeader(){int white=0;for(int y=Math.round(5*scale);y<Math.round(48*scale);y++)for(int x=Math.round(30*scale);x<Math.round(550*scale);x++){int c=color(x,y);if(((c>>16)&255)>200 && ((c>>8)&255)>200 && (c&255)>200)white++;}return white<10;}
        long headerHash(){long h=17;for(int y=0;y<Math.min(bitmap.getHeight(),Math.round(53*scale));y++)for(int x=0;x<bitmap.getWidth();x+=2)h=h*31+color(x,y);return h;}
        Rect emojiCell(int index){float l=10+(index%7)*(571f/7),t=693+92+(index/7)*84;Rect r=scaled(new float[]{l,t,l+571f/7-2,t+76});int colored=0;for(int y=r.top;y<r.bottom;y++)for(int x=r.left;x<r.right;x++){int c=color(x,y),red=(c>>16)&255,g=(c>>8)&255,b=c&255;if(Math.max(red,Math.max(g,b))-Math.min(red,Math.min(g,b))>35 || Math.max(red,Math.max(g,b))>80)colored++;}check(colored>20);return r;}
        public void close(){bitmap.recycle();}
    }
    private Shot shot(){
        final int[] h={0};check(read(()->{WindowInsets insets=activity.getWindow().getDecorView().getRootWindowInsets();if(insets==null)return false;h[0]=insets.getInsets(WindowInsets.Type.ime()).bottom;return activity.hasWindowFocus() && !activity.imeAnimationRunning && insets.isVisible(WindowInsets.Type.ime());}));
        check(ownIme());Bitmap full=automation.takeScreenshot();check(full!=null);check(h[0]>200 && h[0]<full.getHeight()-200);int y=full.getHeight()-h[0];Bitmap crop=Bitmap.createBitmap(full,0,y,full.getWidth(),h[0]);if(crop!=full)full.recycle();Shot s=new Shot(crop,y);
        if(!s.neutral(s.color(s.bitmap.getWidth()/2,Math.max(1,Math.round(5*s.scale))),34)){s.close();check(false);}return s;
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
    private void tap(String id){try(Shot s=shot()){Rect r=s.locate(id,fill(id));long now=SystemClock.uptimeMillis();physical(r,s,MotionEvent.ACTION_DOWN,now);SystemClock.sleep(20);physical(r,s,MotionEvent.ACTION_UP,now);taps++;}SystemClock.sleep(80);}
    private void tapPoint(float x,float absoluteY){try(Shot s=shot()){Rect r=new Rect(Math.round(x*s.scale)-1,Math.round((absoluteY-top())*s.scale)-1,Math.round(x*s.scale)+1,Math.round((absoluteY-top())*s.scale)+1);long now=SystemClock.uptimeMillis();physical(r,s,MotionEvent.ACTION_DOWN,now);SystemClock.sleep(20);physical(r,s,MotionEvent.ACTION_UP,now);taps++;}SystemClock.sleep(80);}
    private boolean body(String expected){return read(()->expected.contentEquals(activity.editor.getText()));}
    private void ready(){check(await(()->{try(Shot s=shot()){return s.emptyHeader();}catch(IllegalStateException e){return false;}},15000));}
    private void choose(){main(()->activity.selectCase("send","",0,true));check(await(()->read(()->activity.getSystemService(InputMethodManager.class).isActive(activity.editor) && activity.getWindow().getDecorView().getRootWindowInsets().isVisible(WindowInsets.Type.ime()) && !activity.imeAnimationRunning),5000));panel="chinese";ready();}
    private void capture(String id,String pressed)throws Exception{
        try(Shot s=shot()){
            JSONObject keys=new JSONObject();for(String key:boxes().keySet()){Rect r=s.locate(key,key.equals(pressed)?133:fill(key));keys.put(key,new JSONArray().put(r.left).put(r.top).put(r.right).put(r.bottom));}
            if(panel.equals("emoji"))for(int i=0;i<28;i++){Rect r=s.emojiCell(i);keys.put("emoji-item-"+i,new JSONArray().put(r.left).put(r.top).put(r.right).put(r.bottom));}
            try(FileOutputStream stream=new FileOutputStream(new File(instrumentation.getTargetContext().getCacheDir(),"host-"+nonce+"-"+id+".png"))){check(s.bitmap.compress(Bitmap.CompressFormat.PNG,100,stream));}
            JSONObject c=new JSONObject().put("id",id).put("width",s.bitmap.getWidth()).put("height",s.bitmap.getHeight()).put("keys",keys).put("location_method","observed screenshot neutral fill around independent reference boxes; Emoji observed foreground cells");
            if(panel.equals("chinese")){Rect r=s.locate("digit-6",pressed==null?70:133);int color=s.color(r.left+r.width()/5,r.top+r.height()/5);c.put("synthetic_key_fill_rgb",new JSONArray().put((color>>16)&255).put((color>>8)&255).put(color&255));}
            captures.put(c);
        }
    }
    private void result(String id,boolean valid)throws Exception{cases.put(new JSONObject().put("id",id).put("pass",valid));check(valid);}
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
        }catch(Throwable e){output.put("pass",false).put("failure_stage",stage).put("error_code","E_PIXEL_REFERENCE_ASSERTION");}
        return output.put("cases",cases).put("reference_captures",captures).put("actual_pixel_targeted_taps",taps).put("planned_cases",9);
    }
}
