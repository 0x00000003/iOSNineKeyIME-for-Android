// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.probe

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ProbeFailureCodeTest : StringSpec({
    "known resource failure has a safe repair code" {
        ProbeFailureCode.from(IllegalStateException("E_RESOURCE_TAMPER")) shouldBe "E_RESOURCE_TAMPER"
        ProbeFailureCode.repairable("E_RESOURCE_TAMPER") shouldBe true
    }
    "arbitrary host or native error payload is never shown" {
        ProbeFailureCode.from(IllegalStateException("synthetic-private-canary")) shouldBe "E_PROBE_EXECUTION"
    }
    "a code prefix cannot smuggle an exception payload into the UI" {
        ProbeFailureCode.from(IllegalStateException("E_MODEL_INTEGRITY: synthetic-private-canary")) shouldBe "E_PROBE_EXECUTION"
    }
    "unknown resource-looking operations cannot activate repair" {
        ProbeFailureCode.repairable("E_RESOURCE_DELETE_LEARNING") shouldBe false
        ProbeFailureCode.repairable("E_PROBE_EXECUTION") shouldBe false
    }

    "native busy cannot enable resource repair or expose arbitrary operational payload" {
        ProbeFailureCode.from(IllegalStateException("E_ENGINE_BUSY")) shouldBe "E_ENGINE_BUSY"
        ProbeFailureCode.repairable("E_ENGINE_BUSY") shouldBe false
        ProbeFailureCode.from(IllegalStateException("E_LEARNING_CLEAR: synthetic-private-canary")) shouldBe "E_PROBE_EXECUTION"
    }
})
