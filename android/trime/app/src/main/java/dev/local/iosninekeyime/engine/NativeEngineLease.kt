// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.engine

/** The pinned JNI owns process-global state. A private field also reserves it without using it. */
object NativeEngineLease {
    private var holder: Any? = null
    @Synchronized fun acquire(owner: Any): Boolean {
        if (holder != null && holder !== owner) return false
        holder = owner
        return true
    }
    @Synchronized internal fun requireOwner(owner:Any) { check(holder===owner) { "E_ENGINE_OWNER" } }
    @Synchronized fun release(owner: Any) {
        check(holder === owner) { "E_ENGINE_OWNER" }
        holder = null
    }
    fun <T> exclusive(block: () -> T): T {
        val owner = Any()
        check(acquire(owner)) { "E_ENGINE_BUSY" }
        return try { block() } finally { release(owner) }
    }
}
