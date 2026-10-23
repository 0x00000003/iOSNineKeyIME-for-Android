// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.probe

import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle
import com.osfans.trime.BuildConfig
import org.json.JSONObject

/** Optimized, non-debuggable target. Explicit engine scopes only; never marks IME/editor gates passed. */
class ReleaseEngineInstrumentation : Instrumentation() {
    private var scope = ""
    private var runId: String? = null
    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        scope = arguments?.getString("probe_scope").orEmpty()
        runId = arguments?.getString("probe_run_id")?.takeIf { it.matches(Regex("[a-f0-9]{32}")) }
        start()
    }
    override fun onStart() {
        val output = Bundle()
        try {
            check(!BuildConfig.DEBUG && runId != null) { "E_RELEASE_TARGET" }
            val result = when (scope) {
                "perf-smoke" -> NativePerformanceProbe.run(targetContext, true, false)
                "perf-model-off-smoke" -> NativePerformanceProbe.run(targetContext, false, false)
                "perf-model-on-long" -> NativePerformanceProbe.run(targetContext, true, true)
                "perf-model-off-long" -> NativePerformanceProbe.run(targetContext, false, true)
                "engine" -> ProbeRunner.run(targetContext)
                "resources" -> ResourceBoundaryProbe.run(targetContext)
                "quality" -> QualityBoundaryProbe.run(targetContext, context.assets)
                "learning" -> LearningBoundaryProbe.run(targetContext, context.assets)
                "context" -> ContextBoundaryProbe.run(targetContext, context.assets)
                else -> error("E_UNSUPPORTED_RELEASE_SCOPE")
            }
            result.put("probe_run_id", runId).put("target_debuggable", BuildConfig.DEBUG)
                .put("host_editor_component", JSONObject().put("status", "NOT_RUN").put("reason", "release engine-only runner; R04 remains FAIL in debug component tests"))
            output.putString("stream", result.toString(2) + "\n")
            output.putBoolean("m1_probe_pass", result.getBoolean("pass"))
            finish(if (result.getBoolean("pass")) Activity.RESULT_OK else Activity.RESULT_CANCELED, output)
        } catch (_: Throwable) {
            output.putString("stream", "E_RELEASE_PROBE_EXECUTION\n")
            output.putBoolean("m1_probe_pass", false)
            finish(Activity.RESULT_CANCELED, output)
        }
    }
}
