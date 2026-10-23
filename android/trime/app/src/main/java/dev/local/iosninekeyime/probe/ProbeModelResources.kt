// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.probe

import android.content.Context
import com.osfans.trime.BuildConfig
import java.io.File
import java.security.MessageDigest

/** One pinned built-in model only. No external path, downloader, runtime selection or learned data. */
object ProbeModelResources {
    const val NAME = "zh-hans-t-essay-bgw-compact.gram"
    fun deploy(context: Context, shared: File) {
        if (!BuildConfig.PROBE_MODEL_ENABLED) return
        val expectedBytes = BuildConfig.PROBE_MODEL_BYTES
        check(expectedBytes in 1..(48L * 1024 * 1024)) { "E_MODEL_BUDGET" }
        fun valid(file: File): Boolean {
            if (!file.isFile || file.length() != expectedBytes) return false
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(65536)
                while (true) { val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) }
            }
            return digest.digest().joinToString("") { "%02x".format(it) } == BuildConfig.PROBE_MODEL_SHA256
        }
        val installed = File(shared, NAME)
        if (installed.exists()) { check(valid(installed)) { "E_MODEL_INTEGRITY" }; return }
        val staging = File.createTempFile("model-", ".staging", shared)
        try {
            context.assets.open(NAME).use { input -> staging.outputStream().use { output ->
                val buffer = ByteArray(65536)
                var total = 0L
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    check(total <= expectedBytes) { "E_MODEL_BUDGET" }
                    output.write(buffer, 0, count)
                }
            } }
            check(valid(staging)) { "E_MODEL_INTEGRITY" }
            check(staging.renameTo(installed)) { "E_MODEL_ACTIVATE" }
        } finally { staging.delete() } // Only this call's unique staging file.
    }
}
