// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.probe

import java.io.File
import java.security.MessageDigest

/** Synthetic test profiles only. The digest is compared in RAM, never emitted or persisted. */
object ProbeFileSnapshot {
    fun fingerprint(root: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(65536)
        root.walkTopDown().sortedBy { it.relativeTo(root).path }.forEach { file ->
            digest.update((file.relativeTo(root).path + "\u0000" + file.length() + "\u0000" + file.lastModified() + "\u0000").toByteArray())
            if (file.isFile) file.inputStream().use { input ->
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
