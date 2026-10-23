// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.probe

import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle

/** Does not enable/select an IME, modify device settings or read host text. */
class ProbeInstrumentation : Instrumentation() {
    private var editorOnly = false
    private var engineOnly = false
    private var runId: String? = null
    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        editorOnly = arguments?.getString("probe_scope") == "editor"
        engineOnly = arguments?.getString("probe_scope") == "engine"
        runId = arguments?.getString("probe_run_id")?.takeIf { it.matches(Regex("[a-f0-9]{32}")) }
        start()
    }
    override fun onStart() {
        val output = Bundle()
        try {
            val result = if (editorOnly) org.json.JSONObject().put("scope", "real host editor component ONLY; native engine not rerun").put("pass", true)
                         else ProbeRunner.run(targetContext)
            check(android.os.Build.VERSION.SDK_INT >= 31) { "E_PROBE_TEST_API" }
            if (!editorOnly) result.put("native_engine_probe_pass", result.getBoolean("pass"))
            if (!engineOnly) {
                val editor = EditorBoundaryProbe.run(this)
                result.put("host_editor_component", editor)
                result.put("pass", result.getBoolean("pass") && editor.getBoolean("pass"))
            } else result.put("host_editor_component", org.json.JSONObject().put("status", "NOT_RUN").put("reason", "explicit native-only scope; editor failures remain open"))
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
