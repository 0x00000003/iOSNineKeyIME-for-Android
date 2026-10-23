// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.engine

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow

class NativeEngineLeaseTest : StringSpec({
    "lease admits only its owner and rejects an independent native client" {
        val first=Any();val second=Any()
        NativeEngineLease.acquire(first) shouldBe true
        try {
            NativeEngineLease.acquire(first) shouldBe true
            NativeEngineLease.acquire(second) shouldBe false
            shouldThrow<IllegalStateException>{NativeEngineLease.release(second)}
        } finally {NativeEngineLease.release(first)}
        NativeEngineLease.acquire(second) shouldBe true
        NativeEngineLease.release(second)
    }
    "exclusive probe releases its lease even when an assertion fails" {
        shouldThrow<IllegalArgumentException>{NativeEngineLease.exclusive {throw IllegalArgumentException("synthetic")}}
        NativeEngineLease.exclusive {true} shouldBe true
    }
    "a reserved private session prevents probes from entering their body" {
        val owner=Any();var entered=false
        NativeEngineLease.acquire(owner) shouldBe true
        try {
            shouldThrow<IllegalStateException>{NativeEngineLease.exclusive {entered=true}}
            entered shouldBe false
        } finally {NativeEngineLease.release(owner)}
    }
})
