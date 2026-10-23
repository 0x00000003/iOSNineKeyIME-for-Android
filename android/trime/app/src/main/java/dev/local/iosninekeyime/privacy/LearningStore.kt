// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.privacy

import android.content.Context
import android.system.Os
import android.system.OsConstants
import dev.local.iosninekeyime.engine.NativeEngineLease
import java.io.File

/** Worker-only, process-native lease required before touching a stopped profile. No backup/export. */
object LearningStore {
    fun clearNormal(context:Context):Boolean=NativeEngineLease.exclusive {
        erase(context,File(context.noBackupFilesDir,"ime/normal"))
    }
    internal fun clearSynthetic(context:Context,profile:File):Boolean=NativeEngineLease.exclusive {
        val parent=File(context.noBackupFilesDir,"m3/learning").canonicalFile
        check(profile.parentFile?.canonicalFile==parent && profile.name.matches(Regex("[a-f0-9-]{36}"))){"E_LEARNING_PATH"}
        erase(context,profile)
    }
    private fun erase(context:Context,profile:File):Boolean {
        val base=context.noBackupFilesDir.canonicalFile
        val target=profile.canonicalFile
        check(target.path.startsWith(base.path+File.separator) && target!=base){"E_LEARNING_PATH"}
        if(!profile.exists())return true
        fun remove(node:File) {
            // lstat before traversal: unlink a symlink itself, never follow it into a sibling tree.
            val mode=Os.lstat(node.path).st_mode
            if(OsConstants.S_ISDIR(mode) && !OsConstants.S_ISLNK(mode)) {
                check(node.canonicalPath.startsWith(target.path+File.separator) || node.canonicalFile==target){"E_LEARNING_PATH"}
                val children=checkNotNull(node.listFiles()){"E_LEARNING_CLEAR"}
                for(child in children)remove(child)
            }
            check(node.delete()){"E_LEARNING_CLEAR"}
        }
        remove(profile)
        check(!profile.exists()){"E_LEARNING_CLEAR"}
        return true
    }
}
