// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.probe

import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle

/** Does not enable/select an IME, modify device settings or read host text. */
class ProbeInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        val output = Bundle()
        try {
            val result = ProbeRunner.run(targetContext)
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
