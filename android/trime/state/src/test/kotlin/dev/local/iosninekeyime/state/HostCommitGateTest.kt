// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.state

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class HostCommitGateTest : StringSpec({
    fun effect(revision: Long = 1, part: Int = 0, text: String = "synthetic", session: Long = 1) = Effect.Commit(Stamp(session, revision), part, text)
    "U03 repeated token commits into the host once" {
        val host = mutableListOf<String>(); val gate = HostCommitGate({ host += it; true }).also { it.startSession(1) }
        gate.deliver(effect()) shouldBe Delivery.ACCEPTED
        gate.deliver(effect()) shouldBe Delivery.DUPLICATE
        host shouldBe listOf("synthetic")
    }
    "U03 punctuation and word parts of one response retain order" {
        val host = mutableListOf<String>(); val gate = HostCommitGate({ host += it; true }).also { it.startSession(1) }
        gate.deliver(effect(text = "word")) shouldBe Delivery.ACCEPTED
        gate.deliver(effect(part = 1, text = "，")) shouldBe Delivery.ACCEPTED
        host shouldBe listOf("word", "，")
    }
    "U03 rejected connection preserves an unconsumed commit for explicit same-session retry" {
        var available = false; val host = mutableListOf<String>()
        val gate = HostCommitGate({ if (available) { host += it; true } else false }).also { it.startSession(1) }
        gate.deliver(effect()) shouldBe Delivery.BLOCKED
        gate.pendingCount shouldBe 1
        gate.deliver(effect()) shouldBe Delivery.BLOCKED
        host shouldBe emptyList()
        available = true
        gate.retry(1) shouldBe Delivery.ACCEPTED
        gate.retry(1) shouldBe Delivery.ACCEPTED
        host shouldBe listOf("synthetic")
    }
    "U03 a new field discards pending old-field text without replaying it" {
        var available = false; val host = mutableListOf<String>()
        val gate = HostCommitGate({ if (available) { host += it; true } else false }).also { it.startSession(1) }
        gate.deliver(effect()) shouldBe Delivery.BLOCKED
        gate.startSession(2); available = true
        gate.retry(1) shouldBe Delivery.STALE
        gate.deliver(effect()) shouldBe Delivery.STALE
        gate.deliver(effect(session = 2, text = "new")) shouldBe Delivery.ACCEPTED
        host shouldBe listOf("new")
    }
    "U03 an out-of-order consumed revision cannot write into the host" {
        var calls = 0; val gate = HostCommitGate({ calls++; true }).also { it.startSession(1) }
        gate.deliver(effect(2)) shouldBe Delivery.ACCEPTED
        gate.deliver(effect(1)) shouldBe Delivery.DUPLICATE
        calls shouldBe 1
    }
    "U03 blocked word and punctuation are retained and retried in order" {
        var available = false; val host = mutableListOf<String>()
        val gate = HostCommitGate({ if (available) { host += it; true } else false }).also { it.startSession(1) }
        gate.deliver(effect(text = "word")) shouldBe Delivery.BLOCKED
        gate.deliver(effect(part = 1, text = "，")) shouldBe Delivery.BLOCKED
        available = true; gate.retry(1) shouldBe Delivery.ACCEPTED
        host shouldBe listOf("word", "，")
    }
    "U03 conflicting payload for a pending token is rejected" {
        val gate = HostCommitGate({ false }).also { it.startSession(1) }
        gate.deliver(effect()) shouldBe Delivery.BLOCKED
        gate.deliver(effect(text = "different")) shouldBe Delivery.CONFLICT
        gate.pendingCount shouldBe 1
    }
    "U03 finite pending capacity reports failure without discarding the first item" {
        val gate = HostCommitGate({ false }, 1).also { it.startSession(1) }
        gate.deliver(effect()) shouldBe Delivery.BLOCKED
        gate.deliver(effect(2)) shouldBe Delivery.CAPACITY
        gate.pendingCount shouldBe 1
    }
    "U03 ending a field prevents pending and new callbacks from writing" {
        var calls = 0; val gate = HostCommitGate({ calls++; true }).also { it.startSession(1) }
        gate.endSession(1)
        gate.deliver(effect()) shouldBe Delivery.STALE
        gate.retry(1) shouldBe Delivery.STALE
        calls shouldBe 0
    }
    "U03 empty native commit never calls InputConnection" {
        var calls = 0; val gate = HostCommitGate({ calls++; true }).also { it.startSession(1) }
        gate.deliver(effect(text = "")) shouldBe Delivery.ACCEPTED
        calls shouldBe 0
    }
    "U03 synchronous field switch during host callback cannot consume the new field queue" {
        val host = mutableListOf<String>()
        lateinit var gate: HostCommitGate
        gate = HostCommitGate({ text ->
            host += text
            if (text == "old") {
                gate.startSession(2)
                gate.deliver(effect(session = 2, text = "new")) shouldBe Delivery.BLOCKED
            }
            true
        }).also { it.startSession(1) }
        gate.deliver(effect(text = "old")) shouldBe Delivery.STALE
        gate.pendingCount shouldBe 1
        gate.retry(2) shouldBe Delivery.ACCEPTED
        host shouldBe listOf("old", "new")
    }
})
