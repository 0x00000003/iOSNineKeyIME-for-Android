// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.engine

import android.content.Context
import com.osfans.trime.core.Rime
import dev.local.iosninekeyime.probe.ProbeResources
import dev.local.iosninekeyime.probe.ProbeModelResources
import dev.local.iosninekeyime.state.*
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicBoolean

/** One native worker, one process lease, no InputConnection/context access and no logging. */
class SerialRimeEngine(
    context: Context,
    private val profile: File,
    private val postOwner: (() -> Unit) -> Unit,
) : KeyboardEnginePort {
    private val context = context.applicationContext
    private val worker = Executors.newSingleThreadExecutor { task -> Thread(task,"ninekey-engine") }
    private val requested = AtomicLong(0)
    private val closed = AtomicBoolean(false)
    private var leased = false
    private var started = false
    private var nativeSession = 0L
    // Metadata only. Used by synthetic integration tests to observe the native privacy barrier.
    val quiescentSession = AtomicLong(0)

    override fun begin(session: Long, sensitive: Boolean, reply: (Boolean) -> Unit) {
        if (closed.get()) { postOwner { reply(false) }; return }
        requested.set(session)
        worker.execute {
            if (closed.get() || requested.get() != session) return@execute
            var success = false
            try {
                if (!leased) { check(NativeEngineLease.acquire(this)) { "E_ENGINE_BUSY" }; leased = true }
                stopNative()
                if (!sensitive) {
                    check(profile.canonicalPath.startsWith(context.noBackupFilesDir.canonicalPath + File.separator)) { "E_PROFILE_PATH" }
                    val shared = ProbeResources.deploy(context)
                    ProbeModelResources.deploy(context,shared)
                    if (closed.get() || requested.get() != session) return@execute
                    check(profile.mkdirs() || profile.isDirectory) { "E_PROFILE" }
                    Rime.startupRime(shared.absolutePath,profile.absolutePath,"m2-component",true)
                    started = true
                    Rime.awaitRimeMaintenance()
                    check(Rime.selectRimeSchema("wanxiang_t9")) { "E_SCHEMA" }
                    nativeSession = session
                }
                success = true
                quiescentSession.set(session)
            } catch (_: Exception) { stopNative() }
            val result = success
            postOwner { reply(result) }
        }
    }

    override fun execute(effect: Effect, reply: (Event.Snapshot?) -> Unit) {
        val stamp = when(effect) {
            is Effect.Key -> effect.stamp
            is Effect.Select -> effect.stamp
            is Effect.Clear -> effect.stamp
            is Effect.FetchCandidates -> effect.stamp
            else -> error("E_NATIVE_COMMAND")
        }
        if (closed.get()) { postOwner { reply(null) }; return }
        worker.execute {
            if (closed.get() || requested.get() != stamp.session) return@execute
            var response: Event.Snapshot? = null
            try {
                check(started && nativeSession == stamp.session) { "E_NATIVE_SESSION" }
                when(effect) {
                    is Effect.Key -> check(Rime.processRimeKey(effect.key,0)) { "E_NATIVE_KEY" }
                    is Effect.Select -> check(Rime.selectRimeCandidate(effect.index,true)) { "E_NATIVE_SELECT" }
                    is Effect.Clear -> { Rime.resetRimeSession(); check(Rime.selectRimeSchema("wanxiang_t9")) { "E_SCHEMA" } }
                    is Effect.FetchCandidates -> check(effect.offset>=0 && effect.limit==KeyboardReducer.CANDIDATE_PAGE_SIZE) { "E_NATIVE_PAGE" }
                    else -> error("E_NATIVE_COMMAND")
                }
                // Read-only paging never drains a host commit or changes composition.
                val commit = if(effect is Effect.FetchCandidates)null else Rime.getRimeCommit().text
                val current = Rime.getRimeContext()
                val offset=(effect as? Effect.FetchCandidates)?.offset?:0
                val limit=KeyboardReducer.CANDIDATE_PAGE_SIZE
                val candidates=if(current.composition.length>0 || current.input.isNotEmpty())Rime.getRimeCandidates(offset,limit+1).map{it.text}else emptyList()
                response = Event.Snapshot(stamp,current.input,current.composition.preedit.orEmpty(),
                    candidates.take(limit),commit,current.composition.length,offset,candidates.size>limit)
            } catch (_: Exception) { stopNative() }
            val result = response
            postOwner { reply(result) }
        }
    }

    override fun end(session: Long) {
        requested.compareAndSet(session,0)
        if (closed.get()) return
        worker.execute {
            if (nativeSession == session) stopNative()
            quiescentSession.compareAndSet(session,0)
            if(requested.get()==0L && leased){NativeEngineLease.release(this);leased=false}
        }
    }
    fun close(onClosed: () -> Unit = {}) {
        if (!closed.compareAndSet(false,true)) return
        requested.set(0)
        worker.execute {
            try { stopNative() } finally {
                if (leased) { NativeEngineLease.release(this); leased = false }
                quiescentSession.set(0)
                postOwner(onClosed)
            }
        }
        worker.shutdown()
    }
    private fun stopNative() {
        if (started) { try { Rime.exitRime() } finally { started = false; nativeSession = 0 } }
    }
}
