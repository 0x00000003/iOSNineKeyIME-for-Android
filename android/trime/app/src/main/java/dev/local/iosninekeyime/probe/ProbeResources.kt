// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.probe

import android.content.Context
import com.osfans.trime.BuildConfig
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import org.json.JSONObject

/** Built-in immutable M1 pack only. No picker, external path, Intent or online source. */
object ProbeResources {
    /** Called only after the serial probe worker has stopped native use; never touches learning profiles. */
    fun quarantineCurrent(context: Context): Boolean {
        val root = File(context.noBackupFilesDir, "m1/resources")
        val installed = File(root, BuildConfig.PROBE_RESOURCE_SHA256)
        if (!installed.exists()) return false
        val quarantine = File(root, "quarantine-${BuildConfig.PROBE_RESOURCE_SHA256}-${java.util.UUID.randomUUID()}")
        check(installed.renameTo(quarantine)) { "E_RESOURCE_QUARANTINE" }
        return true
    }

    private fun digest(file: File): String = file.inputStream().use { input ->
        val md = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(65536)
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            md.update(buffer, 0, n)
        }
        md.digest().joinToString("") { "%02x".format(it) }
    }

    fun deploy(context: Context): File {
        val root = File(context.noBackupFilesDir, "m1/resources")
        check(root.mkdirs() || root.isDirectory)
        val expected = JSONObject(context.assets.open("ime-probe-manifest.json").bufferedReader().use { it.readText() })
        val rows = expected.getJSONArray("files")
        val files = (0 until rows.length()).associate { i ->
            val row = rows.getJSONObject(i)
            row.getString("path") to row
        }
        val allowedFiles = files.keys + if(BuildConfig.PROBE_MODEL_ENABLED)setOf(ProbeModelResources.NAME) else emptySet()
        val allowedDirs = mutableSetOf("")
        for(name in allowedFiles) {
            var parent=File(name).parent
            while(parent!=null){allowedDirs+=parent;parent=File(parent).parent}
        }
        fun validate(dir: File): Boolean {
            fun walk(node:File,name:String):Boolean {
                val mode=android.system.Os.lstat(node.path).st_mode
                if(android.system.OsConstants.S_ISLNK(mode))return false
                if(android.system.OsConstants.S_ISDIR(mode)) {
                    if(name !in allowedDirs)return false
                    return node.listFiles()?.all {walk(it,if(name.isEmpty())it.name else "$name/${it.name}")}==true
                }
                return name in allowedFiles && android.system.OsConstants.S_ISREG(mode)
            }
            return walk(dir,"") && files.all { (name, row) ->
                val f = File(dir, name)
                f.isFile && f.length() == row.getLong("bytes") && digest(f) == row.getString("sha256")
            }
        }
        val installed = File(root, BuildConfig.PROBE_RESOURCE_SHA256)
        if (installed.exists()) {
            check(validate(installed)) { "E_RESOURCE_TAMPER" }
            return installed
        }
        val archive = File.createTempFile("pack-", ".zip", root)
        val staging = File(root, "staging-${java.util.UUID.randomUUID()}")
        try {
            context.assets.open("ime-probe.zip").use { input -> archive.outputStream().use { input.copyTo(it) } }
            check(archive.length() <= 32L * 1024 * 1024 && digest(archive) == BuildConfig.PROBE_RESOURCE_SHA256) { "E_RESOURCE_HASH" }
            check(staging.mkdir())
            val seen = mutableSetOf<String>()
            var total = 0L
            ZipInputStream(archive.inputStream()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val name = entry.name
                    check(name in files && seen.add(name) && !entry.isDirectory) { "E_RESOURCE_ENTRY" }
                    val f = File(staging, name)
                    check(f.canonicalPath.startsWith(staging.canonicalPath + File.separator))
                    check(f.parentFile!!.mkdirs() || f.parentFile!!.isDirectory)
                    f.outputStream().use { out ->
                        val buffer = ByteArray(65536)
                        var bytes = 0L
                        while (true) {
                            val n = zip.read(buffer)
                            if (n < 0) break
                            bytes += n
                            total += n
                            check(bytes <= files.getValue(name).getLong("bytes") && total <= 128L * 1024 * 1024) { "E_RESOURCE_BUDGET" }
                            out.write(buffer, 0, n)
                        }
                    }
                }
            }
            check(seen == files.keys && validate(staging)) { "E_RESOURCE_CONTENT" }
            check(staging.renameTo(installed)) { "E_RESOURCE_ACTIVATE" }
            return installed
        } finally {
            archive.delete()
            // Only the unique staging tree created by this call; never the profile.
            if (staging.exists()) staging.deleteRecursively()
        }
    }
}
