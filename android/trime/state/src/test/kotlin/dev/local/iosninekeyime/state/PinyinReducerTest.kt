// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.state

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain

class PinyinReducerTest : StringSpec({
    fun composing()=KeyboardState(stamp=Stamp(1,3),ready=true,input="64426",preedit="synthetic",compositionLength=5,
        candidates=listOf(Candidate(CandidateId(Stamp(1,3),0),"synthetic")))
    fun query()=KeyboardReducer.reduce(composing(),Event.RequestPinyin)
    fun choices():KeyboardState {
        val q=query()
        return KeyboardReducer.reduce(q.state,Event.Snapshot(q.state.stamp,"64426","synthetic",listOf("synthetic"),
            pinyinChoices=listOf(PinyinChoice("ni",0,2),PinyinChoice("mi",0,2)))).state
    }
    "query preserves composition and binds raw and revision without host effects" {
        val q=query()
        q.state.input shouldBe composing().input
        q.state.awaiting shouldBe true
        q.state.stamp shouldBe Stamp(1,4)
        q.effects shouldBe listOf(Effect.QueryPinyin(Stamp(1,4),"64426",0))
    }
    "read only query cannot modify composition or inject a commit" {
        val q=query()
        listOf(Event.Snapshot(q.state.stamp,"64426","synthetic",emptyList(),commit="synthetic"),
            Event.Snapshot(q.state.stamp,"6442","synthetic",emptyList())).forEach {
            val rejected=KeyboardReducer.reduce(q.state,it)
            rejected.state.errorCode shouldBe "E_CANDIDATE_PAGE"
            rejected.state.ready shouldBe false
            rejected.effects shouldBe emptyList()
        }
    }
    "current syllable choice carries exact raw and cannot commit by itself" {
        val s=choices()
        val picked=KeyboardReducer.reduce(s,Event.ChoosePinyin(s.stamp,0))
        picked.effects shouldBe listOf(Effect.SelectPinyin(Stamp(1,5),"64426",PinyinChoice("ni",0,2)))
        picked.state.pinyinChoices shouldBe emptyList()
        picked.state.awaiting shouldBe true
    }
    "stale revision session and invalid indexes cannot select" {
        val s=choices()
        listOf(Event.ChoosePinyin(Stamp(1,3),0),Event.ChoosePinyin(Stamp(2,4),0),
            Event.ChoosePinyin(s.stamp,-1),Event.ChoosePinyin(s.stamp,2)).forEach {
            KeyboardReducer.reduce(s,it).effects shouldBe emptyList()
        }
    }
    "private idle not ready or nonChinese states cannot query or choose" {
        val s=choices()
        listOf(s.copy(sensitive=true),s.copy(ready=false),s.copy(input="",compositionLength=0),s.copy(layout=Layout.LATIN)).forEach {
            KeyboardReducer.reduce(it,Event.RequestPinyin).effects shouldBe emptyList()
        }
        listOf(s.copy(sensitive=true),s.copy(ready=false),s.copy(layout=Layout.NUMBER)).forEach {
            KeyboardReducer.reduce(it,Event.ChoosePinyin(it.stamp,0)).effects shouldBe emptyList()
        }
    }
    "new key cancel and field change invalidate cached choices" {
        val s=choices()
        listOf(Event.Digit(4),Event.Cancel,Event.Start(2,false,Layout.CHINESE,EditorAction.DONE)).forEach {
            KeyboardReducer.reduce(s,it).state.pinyinChoices shouldBe emptyList()
        }
    }
    "old response after cancellation cannot repopulate choices" {
        val q=query();val cancelled=KeyboardReducer.reduce(q.state,Event.Cancel).state
        KeyboardReducer.reduce(cancelled,Event.Snapshot(q.state.stamp,"64426","synthetic",emptyList(),
            pinyinChoices=listOf(PinyinChoice("ni",0,2)))).state.pinyinChoices shouldBe emptyList()
    }
    "native effect diagnostic representations exclude composition" {
        Effect.QueryPinyin(Stamp(1,1),"private_marker",0).toString() shouldNotContain "private_marker"
        Effect.SelectPinyin(Stamp(1,1),"private_marker",PinyinChoice("private_marker",0,1)).toString() shouldNotContain "private_marker"
    }
})
