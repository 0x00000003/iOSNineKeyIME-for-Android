// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.state

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain

class KeyboardReducerTest : StringSpec({
    fun idle() = KeyboardState(stamp = Stamp(1, 0), ready = true)
    fun pending() = KeyboardReducer.reduce(idle(), Event.Digit(6)).state
    fun composing() = KeyboardReducer.reduce(pending(), Event.Snapshot(Stamp(1, 1), "64426", "synthetic", listOf("target", "other"))).state
    fun native(e: Effect) = e is Effect.Key || e is Effect.Select || e is Effect.Clear

    "U01 all letters map exactly once and nihao encodes numerically" {
        KeyboardReducer.mapping.values.joinToString("").toSet().size shouldBe 26
        KeyboardReducer.encode("nihao") shouldBe "64426"
        KeyboardReducer.mapping.forEach { (digit, letters) -> letters.forEach { KeyboardReducer.encode(it.toString()) shouldBe digit.toString() } }
    }
    "U01 first digit never commits its digit to the host" {
        val r = KeyboardReducer.reduce(idle(), Event.Digit(6))
        r.state.bar shouldBe Bar.CANDIDATES
        r.effects.filterIsInstance<Effect.Commit>().size shouldBe 0
        (r.effects.single() as Effect.Key).key shouldBe '6'.code
    }
    "U02 Chinese idle always presents punctuation" { idle().bar shouldBe Bar.PUNCTUATION }
    "U02 nonempty composition with no menu does not become punctuation" {
        val r = KeyboardReducer.reduce(pending(), Event.Snapshot(Stamp(1, 1), "6", "synthetic", emptyList()))
        r.state.bar shouldBe Bar.NO_CANDIDATES
        KeyboardReducer.reduce(r.state, Event.Space).effects shouldBe emptyList()
        KeyboardReducer.reduce(r.state, Event.Space).state.input shouldBe "6"
    }
    "U02 full commit returns to punctuation and ignores next-word candidates" {
        val pick = KeyboardReducer.reduce(composing(), Event.Choose(composing().candidates[0].id))
        val r = KeyboardReducer.reduce(pick.state, Event.Snapshot(pick.state.stamp, "", "", listOf("prediction"), "target"))
        r.state.bar shouldBe Bar.PUNCTUATION
        r.state.candidates shouldBe emptyList()
        r.effects.filterIsInstance<Effect.Commit>().map { it.text } shouldBe listOf("target")
    }
    "U02 partial commit keeps composition and candidate bar" {
        val pick = KeyboardReducer.reduce(composing(), Event.Choose(composing().candidates[0].id))
        val r = KeyboardReducer.reduce(pick.state, Event.Snapshot(pick.state.stamp, "426", "remaining", listOf("rest"), "part"))
        r.state.bar shouldBe Bar.CANDIDATES
        r.state.composing shouldBe true
        r.effects.filterIsInstance<Effect.Commit>().single().text shouldBe "part"
    }
    "U02 native composition length keeps candidates when raw input is empty" {
        val r = KeyboardReducer.reduce(pending(), Event.Snapshot(Stamp(1, 1), "", "remaining", listOf("rest"), compositionLength = 2))
        r.state.composing shouldBe true
        r.state.bar shouldBe Bar.CANDIDATES
        KeyboardReducer.reduce(r.state, Event.Backspace).effects.filterIsInstance<Effect.Key>().size shouldBe 1
    }
    "U03 cancellation before native readiness never calls native" {
        val s = KeyboardState(stamp = Stamp(2, 0))
        val cancelled = KeyboardReducer.reduce(s, Event.Cancel)
        cancelled.effects shouldBe emptyList()
        cancelled.state.awaiting shouldBe false
        cancelled.state.stamp shouldBe Stamp(2, 1)
    }
    "U03 duplicate native response commits only once" {
        val pick = KeyboardReducer.reduce(composing(), Event.Space)
        val response = Event.Snapshot(pick.state.stamp, "", "", emptyList(), "target")
        val first = KeyboardReducer.reduce(pick.state, response)
        KeyboardReducer.reduce(first.state, response).effects shouldBe emptyList()
    }
    "U03 responses from a previous field cannot commit into a new field" {
        val old = pending()
        val fresh = KeyboardReducer.reduce(old, Event.Start(2, true, Layout.CHINESE, EditorAction.DONE)).state
        val r = KeyboardReducer.reduce(fresh, Event.Snapshot(old.stamp, "", "", emptyList(), "old"))
        r.effects shouldBe emptyList()
        r.state shouldBe fresh
    }
    "U03 wrong revision in the same field is rejected" {
        KeyboardReducer.reduce(pending(), Event.Snapshot(Stamp(1, 0), "", "", emptyList(), "old")).effects shouldBe emptyList()
    }
    "U03 rapid key waits behind response barrier without changing stamp" {
        val s = pending()
        val r = KeyboardReducer.reduce(s, Event.Digit(4))
        r.state.stamp shouldBe s.stamp
        r.effects shouldBe listOf(Effect.Defer(1, Event.Digit(4)))
    }
    "U03 stale candidate identifier cannot select a different menu" {
        val s = composing()
        val stale = CandidateId(Stamp(1, 0), 0)
        KeyboardReducer.reduce(s, Event.Choose(stale)).effects shouldBe emptyList()
    }
    "U03 composing Enter confirms without issuing Send in the same event" {
        val r = KeyboardReducer.reduce(composing().copy(action = EditorAction.SEND), Event.Enter)
        r.effects.filterIsInstance<Effect.Select>().size shouldBe 1
        r.effects.filterIsInstance<Effect.Action>().size shouldBe 0
    }
    "U03 idle Enter preserves explicit editor action" {
        KeyboardReducer.reduce(idle().copy(action = EditorAction.SEARCH), Event.Enter).effects shouldBe listOf(Effect.Action(1, EditorAction.SEARCH))
    }
    "U03 punctuation follows successful confirmation exactly once" {
        val pick = KeyboardReducer.reduce(composing(), Event.Punctuation("……"))
        val response = Event.Snapshot(pick.state.stamp, "", "", emptyList(), "target")
        val first = KeyboardReducer.reduce(pick.state, response)
        first.effects.filterIsInstance<Effect.Commit>().map { it.text } shouldBe listOf("target", "……")
        KeyboardReducer.reduce(first.state, response).effects shouldBe emptyList()
    }
    "U03 partial confirmation before punctuation drains remaining segments" {
        val pick = KeyboardReducer.reduce(composing(), Event.Punctuation("，"))
        val partial = KeyboardReducer.reduce(pick.state, Event.Snapshot(pick.state.stamp, "426", "remaining", listOf("rest"), "part"))
        partial.effects.filterIsInstance<Effect.Select>().size shouldBe 1
        partial.effects.filterIsInstance<Effect.Commit>().map { it.text } shouldBe listOf("part")
        val last = KeyboardReducer.reduce(partial.state, Event.Snapshot(partial.state.stamp, "", "", emptyList(), "rest"))
        last.effects.filterIsInstance<Effect.Commit>().map { it.text } shouldBe listOf("rest", "，")
    }
    "U02 no candidate prevents layout switch without losing composition" {
        val s = composing().copy(candidates = emptyList())
        val r = KeyboardReducer.reduce(s, Event.ChangeLayout(Layout.NUMBER))
        r.state.layout shouldBe Layout.CHINESE
        r.state.input shouldBe s.input
        r.effects shouldBe emptyList()
    }
    "U03 successful layout switch waits for native commit" {
        val pick = KeyboardReducer.reduce(composing(), Event.ChangeLayout(Layout.NUMBER))
        pick.state.layout shouldBe Layout.CHINESE
        KeyboardReducer.reduce(pick.state, Event.Snapshot(pick.state.stamp, "", "", emptyList(), "target")).state.layout shouldBe Layout.NUMBER
    }
    "U04 composition backspace uses engine and never deletes host" {
        val r = KeyboardReducer.reduce(composing(), Event.Backspace)
        r.effects.filterIsInstance<Effect.Delete>().size shouldBe 0
        (r.effects.single() as Effect.Key).key shouldBe 0xff08
        KeyboardReducer.reduce(r.state, Event.Snapshot(r.state.stamp, "", "", emptyList())).state.bar shouldBe Bar.PUNCTUATION
    }
    "U04 idle backspace delegates to host with sensitivity policy" {
        KeyboardReducer.reduce(idle(), Event.Backspace).effects shouldBe listOf(Effect.Delete(1, false))
    }
    "U02 expand requires real candidates and resets after commit" {
        KeyboardReducer.reduce(idle(), Event.Expand).state.expanded shouldBe false
        KeyboardReducer.reduce(composing(), Event.Expand).state.expanded shouldBe true
        val pick = KeyboardReducer.reduce(composing().copy(expanded = true), Event.Space)
        KeyboardReducer.reduce(pick.state, Event.Snapshot(pick.state.stamp, "", "", emptyList(), "target")).state.expanded shouldBe false
    }
    "U05 sensitive field forces direct layout and all events avoid native input" {
        val s = KeyboardReducer.reduce(composing(), Event.Start(2, true, Layout.CHINESE, EditorAction.DONE)).state
        s.layout shouldBe Layout.LATIN
        s.bar shouldBe Bar.PRIVATE
        listOf<Event>(Event.Digit(6), Event.Text("x"), Event.Space, Event.Enter, Event.Backspace, Event.Cancel, Event.ChangeLayout(Layout.CHINESE)).forEach { event ->
            KeyboardReducer.reduce(s, event).effects.any(::native) shouldBe false
        }
    }
    "U03 cancellation invalidates an outstanding key response" {
        val old = pending()
        val cancelled = KeyboardReducer.reduce(old, Event.Cancel)
        KeyboardReducer.reduce(cancelled.state, Event.Snapshot(old.stamp, "6", "old", listOf("old"))).effects shouldBe emptyList()
        KeyboardReducer.reduce(cancelled.state, Event.Snapshot(cancelled.state.stamp, "", "", emptyList())).state.bar shouldBe Bar.PUNCTUATION
    }
    "privacy diagnostic strings exclude payloads" {
        val s = composing().copy(input = "private-canary", preedit = "private-canary")
        s.toString() shouldNotContain "private-canary"
        Candidate(CandidateId(Stamp(1, 1), 0), "private-canary").toString() shouldNotContain "private-canary"
        Effect.Commit(Stamp(1, 1), 0, "private-canary").toString() shouldNotContain "private-canary"
    }
    "invalid nine-key digit is rejected instead of sent to host" {
        KeyboardReducer.reduce(idle(), Event.Digit(1)).effects shouldBe emptyList()
    }
})
