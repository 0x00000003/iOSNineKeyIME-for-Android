// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.probe;

import android.content.Context;
import android.os.Debug;
import android.os.PowerManager;
import android.os.Process;
import android.net.TrafficStats;
import com.osfans.trime.core.Rime;
import dev.local.iosninekeyime.engine.NativeEngineLease;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;

/** Fixed synthetic, no-learning JNI workload. No host editor/text/context or device configuration. */
public final class NativePerformanceProbe {
    private static final String[] DIGITS = {"64426", "94664486", "744543"};
    private static void require(boolean condition) {
        if (!condition) throw new IllegalStateException("E_PERF_ASSERTION");
    }
    private static JSONObject memory(long start, int keys, PowerManager power) throws Exception {
        Debug.MemoryInfo info = new Debug.MemoryInfo();
        Debug.getMemoryInfo(info);
        JSONObject result = new JSONObject().put("elapsed_ms", (System.nanoTime()-start)/1_000_000)
            .put("keys", keys).put("total_pss_kib",info.getTotalPss())
            .put("total_private_dirty_kib",info.getTotalPrivateDirty())
            .put("thermal_status",power.getCurrentThermalStatus());
        String gc=Debug.getRuntimeStat("art.gc.gc-count");
        result.put("gc_count",gc == null ? JSONObject.NULL : Long.parseLong(gc));
        // Read own mapping ranges in RAM only; never emit paths or profile names.
        long modelMapping=0;
        for(String line:Files.readAllLines(new File("/proc/self/maps").toPath(),StandardCharsets.UTF_8)) {
            if(!line.endsWith("/"+ProbeModelResources.NAME)) continue;
            String[] range=line.substring(0,line.indexOf(' ')).split("-");
            modelMapping+=Long.parseUnsignedLong(range[1],16)-Long.parseUnsignedLong(range[0],16);
        }
        return result.put("model_mapped_address_bytes_not_rss",modelMapping);
    }
    public static JSONObject run(Context context, boolean modelEnabled, boolean longRun) throws Exception {
        Object owner=new Object();require(NativeEngineLease.INSTANCE.acquire(owner));
        boolean started=false;
        try {
            File shared=ProbeResources.INSTANCE.deploy(context);
            ProbeModelResources.INSTANCE.deploy(context,shared);
            String sharedBefore=ProbeFileSnapshot.INSTANCE.fingerprint(shared);
            File profile=new File(context.getNoBackupFilesDir(),"m5/native-perf/"+UUID.randomUUID());require(profile.mkdirs());
            String patch="patch:\n  translator/enable_user_dict: false\n  translator/contextual_suggestions: true\n  grammar/language: '"+(modelEnabled?"zh-hans-t-essay-bgw-compact":"")+"'\n";
            for(String schema:new String[]{"wanxiang_t9","wanxiang_t9_nolearn"})
                Files.write(new File(profile,schema+".custom.yaml").toPath(),patch.getBytes(StandardCharsets.UTF_8));
            Rime.startupRime(shared.getAbsolutePath(),profile.getAbsolutePath(),"m5-native-perf",true);started=true;
            Rime.awaitRimeMaintenance();require(Rime.selectRimeSchema("wanxiang_t9"));
            require(Rime.getRimeProbeCompactGrammarLoaded()==modelEnabled);
            String profileBefore=ProbeFileSnapshot.INSTANCE.fingerprint(profile);
            // Fixed 120 warm-up keys, commits checked exactly once. No timing budget inferred for UI.
            int word=0,cursor=0,commits=0;
            for(int i=0;i<120;i++) {
                String digits=DIGITS[word%DIGITS.length];
                require(Rime.processRimeKey(digits.charAt(cursor++),0));
                if(cursor==digits.length()) {commit();commits++;cursor=0;word++;}
            }
            Rime.clearRimeComposition();cursor=0;
            long[] latency=new long[16000], keyLatency=new long[16000], contextLatency=new long[16000], candidatesLatency=new long[16000];
            PowerManager power=(PowerManager)context.getSystemService(Context.POWER_SERVICE);
            long start=System.nanoTime(), nextSample=start;
            long duration=longRun?1_800_000_000_000L:10_000_000_000L;
            long txBefore=TrafficStats.getUidTxBytes(Process.myUid()),rxBefore=TrafficStats.getUidRxBytes(Process.myUid());
            int keys=0,measuredCommits=0;
            JSONArray samples=new JSONArray();
            while(System.nanoTime()-start<duration) {
                long tick=System.nanoTime();
                if(tick>=nextSample) {samples.put(memory(start,keys,power));nextSample=tick+60_000_000_000L;}
                String digits=DIGITS[word%DIGITS.length];
                long keyStart=System.nanoTime();
                require(Rime.processRimeKey(digits.charAt(cursor++),0));
                long keyEnd=System.nanoTime();
                require(Rime.getRimeContext().getComposition().getLength()>0);
                long contextEnd=System.nanoTime();
                require(Rime.getRimeCandidates(0,100).length>0);
                long candidatesEnd=System.nanoTime();
                require(keys<latency.length);
                keyLatency[keys]=keyEnd-keyStart;contextLatency[keys]=contextEnd-keyEnd;candidatesLatency[keys]=candidatesEnd-contextEnd;
                latency[keys++]=candidatesEnd-keyStart;
                if(cursor==digits.length()) {commit();measuredCommits++;cursor=0;word++;}
                // Eight keys per second, no catch-up bursts when a call stalls.
                long rest=125_000_000L-(System.nanoTime()-tick);
                if(rest>0)Thread.sleep(rest/1_000_000L,(int)(rest%1_000_000L));
            }
            samples.put(memory(start,keys,power));
            long queryCount=Rime.getRimeProbeGrammarQueryCount();
            boolean profilesUnchanged=profileBefore.equals(ProbeFileSnapshot.INSTANCE.fingerprint(profile));
            Rime.exitRime();started=false;
            boolean staticUnchanged=sharedBefore.equals(ProbeFileSnapshot.INSTANCE.fingerprint(shared));
            long txAfter=TrafficStats.getUidTxBytes(Process.myUid()),rxAfter=TrafficStats.getUidRxBytes(Process.myUid());
            long[] ordered=Arrays.copyOf(latency,keys);Arrays.sort(ordered);
            double p95=ordered[(int)Math.ceil(keys*.95)-1]/1e6;
            double p99=ordered[(int)Math.ceil(keys*.99)-1]/1e6;
            JSONObject result=new JSONObject().put("scope","optimized JNI/process component ONLY; instrumentation overhead included; NOT P01 touch/frame, P02 keyboard startup, P03 full IME/model-on-off long-run acceptance or P04 host/rotation oracle")
                .put("candidate_limit",100).put("protocol_revision",3).put("session_reset_per_commit",false)
                .put("component_candidate_budget_p95_80_p99_150_met",p95<=80&&p99<=150)
                .put("full_product_performance_acceptance","NOT_RUN")
                .put("model_enabled",modelEnabled).put("grammar_queries",queryCount).put("requested_duration_seconds",longRun?1800:10)
                .put("elapsed_ms",(System.nanoTime()-start)/1_000_000).put("keys",keys).put("paced_keys_per_second",8)
                .put("warmup_keys",120).put("warmup_commits",commits).put("measured_exact_once_commits",measuredCommits)
                .put("jni_key_context_candidates_p50_ms",ordered[(int)Math.ceil(keys*.50)-1]/1e6)
                .put("jni_key_context_candidates_p95_ms",ordered[(int)Math.ceil(keys*.95)-1]/1e6)
                .put("jni_key_context_candidates_p99_ms",ordered[(int)Math.ceil(keys*.99)-1]/1e6)
                .put("phase_p95_ms",new JSONObject().put("process_key",percentile95(keyLatency,keys))
                    .put("get_context",percentile95(contextLatency,keys)).put("get_candidates",percentile95(candidatesLatency,keys)))
                .put("samples",samples).put("profile_unchanged_before_native_close",profilesUnchanged)
                .put("shared_bytes_and_timestamps_unchanged",staticUnchanged)
                .put("own_uid_traffic_supported",txBefore>=0&&rxBefore>=0&&txAfter>=0&&rxAfter>=0)
                .put("own_uid_tx_bytes_delta",txAfter>=0&&txBefore>=0?txAfter-txBefore:JSONObject.NULL)
                .put("own_uid_rx_bytes_delta",rxAfter>=0&&rxBefore>=0?rxAfter-rxBefore:JSONObject.NULL)
                .put("pass",keys>0&&profilesUnchanged&&staticUnchanged&&(modelEnabled?queryCount>0:queryCount==0));
            return result;
        } finally {
            if(started)Rime.exitRime();
            NativeEngineLease.INSTANCE.release(owner);
        }
    }
    private static double percentile95(long[] values,int count) {
        long[] copy=Arrays.copyOf(values,count);Arrays.sort(copy);
        return copy[(int)Math.ceil(count*.95)-1]/1e6;
    }
    private static void commit() {
        String expected=Rime.getRimeCandidates(0,1)[0].getText();
        require(Rime.selectRimeCandidate(0,true));
        require(expected.equals(Rime.getRimeCommit().getText()));
        String duplicate=Rime.getRimeCommit().getText();
        require(duplicate==null||duplicate.isEmpty());
        require(Rime.getRimeContext().getComposition().getLength()==0);
    }
}
