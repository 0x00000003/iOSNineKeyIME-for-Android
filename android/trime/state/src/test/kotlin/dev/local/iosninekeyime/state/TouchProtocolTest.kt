// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.state

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class TouchProtocolTest : StringSpec({
    val map = HitMap(listOf(KeyBox("2", 0f, 0f, 50f, 50f), KeyBox("delete", 50f, 0f, 100f, 50f, true)))
    fun touch() = TouchProtocol(map).also { it.startSession(1) }
    "U06 shared boundary belongs to only its right-hand key" {
        map.hit(50f, 20f)?.id shouldBe "delete"
        map.keys.count { it.contains(50f, 20f) } shouldBe 1
        map.hit(100f, 20f) shouldBe null
    }
    "U06 overlapping geometry is rejected at construction" {
        shouldThrow<IllegalArgumentException> { HitMap(listOf(KeyBox("a", 0f, 0f, 50f, 50f), KeyBox("b", 49f, 0f, 90f, 50f))) }
    }
    "U06 nine-key centers retain mapping and honor insets" {
        val grid = HitMap.nineKey(400f, 12f, 18f)
        grid.keys.map { grid.hit((it.left + it.right) / 2, (it.top + it.bottom) / 2)?.id } shouldBe listOf("punctuation", "2", "3", "4", "5", "6", "7", "8", "9")
        grid.keys.all { it.bottom - it.top >= 48 && it.right - it.left >= 48 } shouldBe true
        grid.hit(12f, 6f) shouldBe null
        grid.hit(390f, 20f) shouldBe null
    }
    "U06 too narrow a display must select an alternate layout explicitly" {
        shouldThrow<IllegalArgumentException> { HitMap.nineKey(100f) }
    }
    "U06 ordinary key emits one activation on release" {
        val t = touch(); t.down(0, 20f, 20f, 0)
        t.tick(500) shouldBe null
        t.up(0, 20f, 20f) shouldBe KeyActivation(1, "2", false)
        t.up(0, 20f, 20f) shouldBe null
    }
    "U06 sliding out permanently cancels even after sliding back" {
        val t = touch(); t.down(0, 20f, 20f, 0)
        t.move(0, 60f, 20f); t.move(0, 20f, 20f)
        t.up(0, 20f, 20f) shouldBe null
    }
    "U06 second pointer cannot steal the primary key" {
        val t = touch(); t.down(0, 20f, 20f, 0); t.down(1, 70f, 20f, 1)
        t.up(1, 70f, 20f) shouldBe null
        t.up(0, 20f, 20f) shouldBe KeyActivation(1, "2", false)
    }
    "U04 long deletion begins at 400ms repeats every 60ms and has no short release" {
        val t = touch(); t.down(0, 70f, 20f, 0)
        t.tick(399) shouldBe null
        t.tick(400) shouldBe KeyActivation(1, "delete", true)
        t.tick(459) shouldBe null
        t.tick(460) shouldBe KeyActivation(1, "delete", true)
        t.up(0, 70f, 20f) shouldBe null
    }
    "U04 changing field terminates a running deletion" {
        val t = touch(); t.down(0, 70f, 20f, 0); t.tick(400)
        t.startSession(2)
        t.tick(500) shouldBe null
        t.up(0, 70f, 20f) shouldBe null
    }
    "U06 system cancellation and moving out stop repeat" {
        val t = touch(); t.down(0, 70f, 20f, 0); t.move(0, 200f, 20f)
        t.tick(400) shouldBe null
        t.cancel(); t.up(0, 70f, 20f) shouldBe null
    }
    "U06 pause does not replay missed deletion ticks in a burst" {
        val t = touch(); t.down(0, 70f, 20f, 0)
        t.tick(10_000) shouldBe KeyActivation(1, "delete", true)
        t.tick(10_000) shouldBe null
    }
    "U06 accessibility action uses the same key IDs and current session" {
        val t = touch()
        t.accessibilityActivate("2", 1) shouldBe KeyActivation(1, "2", false)
        t.accessibilityActivate("unknown", 1) shouldBe null
        t.startSession(2)
        t.accessibilityActivate("2", 1) shouldBe null
    }

    "dynamic candidate update cancels its old touch but retains static delete repeat" {
        val delete = KeyBox("delete",50f,0f,100f,50f,true)
        val old = KeyBox("candidate-1",0f,0f,50f,50f)
        val next = KeyBox("candidate-2",0f,0f,50f,50f)
        val touch = TouchProtocol(HitMap(listOf(old,delete)))
        touch.startSession(1);touch.down(0,20f,20f,0)
        touch.updateMap(HitMap(listOf(next,delete)))
        touch.up(0,20f,20f) shouldBe null
        touch.down(0,70f,20f,0);touch.updateMap(HitMap(listOf(old,delete)))
        touch.tick(400)?.key shouldBe "delete"
        touch.up(0,70f,20f) shouldBe null
    }
})
