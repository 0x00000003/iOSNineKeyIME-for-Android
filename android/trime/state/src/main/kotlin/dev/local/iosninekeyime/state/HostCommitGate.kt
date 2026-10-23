// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.state

enum class Delivery { ACCEPTED, DUPLICATE, STALE, BLOCKED, CONFLICT, CAPACITY }

/** Single owner-thread, in-memory commit boundary. Never retries into a different field. */
class HostCommitGate(private val commit: (String) -> Boolean, private val capacity: Int = 64) {
    private data class Token(val revision: Long, val part: Int) : Comparable<Token> {
        override fun compareTo(other: Token): Int = compareValuesBy(this, other, Token::revision, Token::part)
    }
    private data class Pending(val token: Token, val text: String) {
        override fun toString() = "Pending(token=$token)"
    }
    private var session = 0L
    private var active = false
    private var flushing = false
    private var consumed = Token(-1, -1)
    private val pending = ArrayDeque<Pending>()
    val pendingCount get() = pending.size

    init { require(capacity > 0) }

    fun startSession(value: Long) {
        require(value > session)
        session = value
        active = true
        consumed = Token(-1, -1)
        pending.clear()
    }

    fun deliver(effect: Effect.Commit): Delivery {
        if (effect.stamp.session != session || !active) return Delivery.STALE
        val token = Token(effect.stamp.revision, effect.part)
        require(token.revision >= 0 && token.part >= 0)
        if (token <= consumed) return Delivery.DUPLICATE
        pending.firstOrNull { it.token == token }?.let {
            return if (it.text == effect.text) Delivery.BLOCKED else Delivery.CONFLICT
        }
        if (pending.lastOrNull()?.let { token < it.token } == true) return Delivery.STALE
        if (pending.size >= capacity) return Delivery.CAPACITY
        pending += Pending(token, effect.text)
        return flush()
    }

    /** Explicit retry after restoring the same connection/session, never an automatic cross-field replay. */
    fun retry(expectedSession: Long): Delivery =
        if (expectedSession != session || !active) Delivery.STALE else flush()

    private fun flush(): Delivery {
        if (flushing) return Delivery.BLOCKED
        val expectedSession = session
        flushing = true
        try {
            while (pending.isNotEmpty()) {
                val item = pending.first()
                val accepted = item.text.isEmpty() || commit(item.text)
                // A local InputConnection can synchronously trigger a field/lifecycle callback.
                if (!active || session != expectedSession) return Delivery.STALE
                if (!accepted) return Delivery.BLOCKED
                consumed = item.token
                pending.removeFirst()
            }
            return Delivery.ACCEPTED
        } finally {
            flushing = false
        }
    }

    fun endSession(expectedSession: Long) {
        if (session != expectedSession) return
        pending.clear()
        // Keep the high-water session ID to reject an old Start.
        active = false
    }
}
