// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.probe

/** Never show arbitrary exceptions: native/host messages might carry text. */
object ProbeFailureCode {
    private val resource = setOf("E_RESOURCE_TAMPER", "E_RESOURCE_HASH", "E_RESOURCE_ENTRY", "E_RESOURCE_BUDGET",
        "E_RESOURCE_CONTENT", "E_RESOURCE_ACTIVATE", "E_RESOURCE_QUARANTINE", "E_MODEL_BUDGET", "E_MODEL_INTEGRITY", "E_MODEL_ACTIVATE")
    private val operational=setOf("E_ENGINE_BUSY","E_LEARNING_CLEAR","E_LEARNING_PATH")
    fun from(error: Throwable): String = error.message?.takeIf { it in resource || it in operational } ?: "E_PROBE_EXECUTION"
    fun repairable(code: String) = code in resource
}
