// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.probe

import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle

/** Does not enable/select an IME, modify device settings or read host text. */
class ProbeInstrumentation : Instrumentation() {
    private var editorOnly = false
    private var engineOnly = false
    private var resourceOnly = false
    private var controllerOnly = false
    private var qualityOnly = false
    private var learningOnly = false
    private var contextOnly = false
    private var settingsOnly = false
    private var faultsOnly = false
    private var noLearnRestartOnly = false
    private var pagingOnly = false
    private var gestureResearchOnly = false
    private var boundsResearchOnly = false
    private var runId: String? = null
    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        editorOnly = arguments?.getString("probe_scope") == "editor"
        engineOnly = arguments?.getString("probe_scope") == "engine"
        resourceOnly = arguments?.getString("probe_scope") == "resources"
        controllerOnly = arguments?.getString("probe_scope") == "controller"
        qualityOnly = arguments?.getString("probe_scope") == "quality"
        learningOnly = arguments?.getString("probe_scope") == "learning"
        contextOnly = arguments?.getString("probe_scope") == "context"
        boundsResearchOnly = arguments?.getString("probe_scope") == "gesture-metadata-research"
        gestureResearchOnly = arguments?.getString("probe_scope") == "gesture-research"
        pagingOnly = arguments?.getString("probe_scope") == "paging"
        noLearnRestartOnly = arguments?.getString("probe_scope") == "nolearn-restart"
        faultsOnly = arguments?.getString("probe_scope") == "connection-faults"
        settingsOnly = arguments?.getString("probe_scope") == "settings"
        runId = arguments?.getString("probe_run_id")?.takeIf { it.matches(Regex("[a-f0-9]{32}")) }
        start()
    }
    override fun onStart() {
        val output = Bundle()
        try {
            val result = when {
                boundsResearchOnly -> {check(android.os.Build.VERSION.SDK_INT>=34){"E_GESTURE_API"};HostGestureResearchProbe.run(this,true)}
                gestureResearchOnly -> {check(android.os.Build.VERSION.SDK_INT>=34){"E_GESTURE_API"};HostGestureResearchProbe.run(this)}
                pagingOnly -> CandidatePagingBoundaryProbe.run(this)
                noLearnRestartOnly -> NoLearnRestartProbe.run(targetContext)
                faultsOnly -> HostConnectionFailureProbe.run(this)
                settingsOnly -> SettingsBoundaryProbe.run(this)
                contextOnly -> ContextBoundaryProbe.run(targetContext,context.assets)
                learningOnly -> LearningBoundaryProbe.run(targetContext,context.assets)
                qualityOnly -> QualityBoundaryProbe.run(targetContext,context.assets)
                controllerOnly -> ControllerBoundaryProbe.run(this)
                resourceOnly -> ResourceBoundaryProbe.run(targetContext)
                editorOnly -> org.json.JSONObject().put("scope", "real host editor component ONLY; native engine not rerun").put("pass", true)
                else -> ProbeRunner.run(targetContext)
            }
            check(android.os.Build.VERSION.SDK_INT >= 31) { "E_PROBE_TEST_API" }
            if (!editorOnly && !resourceOnly && !controllerOnly && !qualityOnly && !learningOnly && !contextOnly && !settingsOnly && !faultsOnly && !noLearnRestartOnly && !pagingOnly && !gestureResearchOnly && !boundsResearchOnly) result.put("native_engine_probe_pass", result.getBoolean("pass"))
            if (!engineOnly && !resourceOnly && !controllerOnly && !qualityOnly && !learningOnly && !contextOnly && !settingsOnly && !faultsOnly && !noLearnRestartOnly && !pagingOnly && !gestureResearchOnly && !boundsResearchOnly) {
                val editor = EditorBoundaryProbe.run(this)
                val faults = HostConnectionFailureProbe.run(this)
                val structuralFaults = StructuralDeleteFailureProbe.run(this)
                result.put("host_editor_component", editor)
                result.put("host_connection_fault_component", faults)
                result.put("structural_delete_fault_component",structuralFaults)
                result.put("pass", result.getBoolean("pass") && editor.getBoolean("pass") && faults.getBoolean("pass") && structuralFaults.getBoolean("pass"))
                if(!editorOnly){
                    val paging=CandidatePagingBoundaryProbe.run(this)
                    result.put("native_candidate_paging_component",paging)
                    result.put("pass",result.getBoolean("pass") && paging.getBoolean("pass"))
                }
            } else result.put("host_editor_component", org.json.JSONObject().put("status", "NOT_RUN").put("reason", "explicit separate component scope; R04 remains open"))
            result.put("probe_run_id", runId)
            java.io.File(targetContext.noBackupFilesDir, "m1/probe-result.json").writeText(result.toString(2))
            output.putString("stream", result.toString(2) + "\n")
            output.putBoolean("m1_probe_pass", result.getBoolean("pass"))
            finish(if (result.getBoolean("pass")) Activity.RESULT_OK else Activity.RESULT_CANCELED, output)
        } catch (_: Throwable) {
            // Never serialize exception payloads or input into instrumentation/logcat.
            output.putString("stream", "E_PROBE_EXECUTION\n")
            output.putBoolean("m1_probe_pass", false)
            finish(Activity.RESULT_CANCELED, output)
        }
    }
}
