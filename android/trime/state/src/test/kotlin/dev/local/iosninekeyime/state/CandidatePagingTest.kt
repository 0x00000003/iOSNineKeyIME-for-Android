// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.state

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class CandidatePagingTest:StringSpec({
    fun composing():KeyboardState {
        val pending=KeyboardReducer.reduce(KeyboardState(Stamp(1,0),ready=true),Event.Digit(6)).state
        return KeyboardReducer.reduce(pending,Event.Snapshot(pending.stamp,"6","synthetic",List(20){"synthetic-$it"},hasMoreCandidates=true)).state.copy(expanded=true)
    }
    fun response(request:KeyboardState,offset:Int,more:Boolean=true)=Event.Snapshot(request.stamp,"6","synthetic",List(20){"synthetic-${offset+it}"},candidateOffset=offset,hasMoreCandidates=more)
    fun second():KeyboardState {
        val request=KeyboardReducer.reduce(composing(),Event.CandidatePage(1))
        return KeyboardReducer.reduce(request.state,response(request.state,20)).state
    }
    "page fetch crosses 100 with global stable ids and one bounded window" {
        var state=composing()
        repeat(7){
            val request=KeyboardReducer.reduce(state,Event.CandidatePage(1))
            val fetch=request.effects.single()as Effect.FetchCandidates
            fetch.offset shouldBe (it+1)*20;fetch.limit shouldBe 20
            state=KeyboardReducer.reduce(request.state,response(request.state,fetch.offset)).state
            state.candidates.size shouldBe 20;state.candidates.first().id.index shouldBe fetch.offset
            state.candidates.first().id.stamp shouldBe state.stamp
        }
        state.candidateOffset shouldBe 140
    }
    "previous page asks engine and collapse returns first global page" {
        val previous=KeyboardReducer.reduce(second(),Event.CandidatePage(-1))
        (previous.effects.single()as Effect.FetchCandidates).offset shouldBe 0
        val collapse=KeyboardReducer.reduce(second(),Event.Expand)
        collapse.state.expanded shouldBe false
        val restored=KeyboardReducer.reduce(collapse.state,response(collapse.state,0)).state
        restored.candidateOffset shouldBe 0;restored.expanded shouldBe false
    }
    "space and punctuation on a later page still confirm global primary" {
        (KeyboardReducer.reduce(second(),Event.Space).effects.single()as Effect.Select).index shouldBe 0
        val punct=KeyboardReducer.reduce(second(),Event.Punctuation("，"))
        (punct.effects.single()as Effect.Select).index shouldBe 0
        punct.state.tail shouldBe Tail.Punctuation("，")
    }
    "choosing a later page uses its actual global index" {
        val state=second();val selected=KeyboardReducer.reduce(state,Event.Choose(state.candidates[4].id))
        (selected.effects.single()as Effect.Select).index shouldBe 24
    }
    "candidate from an older page revision cannot commit" {
        val old=composing().candidates.first().id
        KeyboardReducer.reduce(second(),Event.Choose(old)).effects shouldBe emptyList()
    }
    "end and first boundaries do not query outside range" {
        KeyboardReducer.reduce(composing(),Event.CandidatePage(-1)).effects shouldBe emptyList()
        KeyboardReducer.reduce(second().copy(hasMoreCandidates=false),Event.CandidatePage(1)).effects shouldBe emptyList()
    }
    "paging a private field never starts native" {
        val private=KeyboardReducer.reduce(KeyboardState(),Event.Start(1,true,Layout.CHINESE,EditorAction.DONE)).state
        KeyboardReducer.reduce(private,Event.CandidatePage(1)).effects shouldBe emptyList()
    }
    "wrong page or injected commit is rejected without a host effect" {
        val request=KeyboardReducer.reduce(composing(),Event.CandidatePage(1)).state
        for(event in listOf(response(request,0),response(request,20).copy(commit="injected"),response(request,20).copy(input="changed"))) {
            val rejected=KeyboardReducer.reduce(request,event)
            rejected.effects shouldBe emptyList();rejected.state.ready shouldBe false
            rejected.state.input shouldBe "6";rejected.state.errorCode shouldBe "E_CANDIDATE_PAGE"
        }
    }
    "cancel invalidates an outstanding page response" {
        val request=KeyboardReducer.reduce(composing(),Event.CandidatePage(1)).state
        val cancelled=KeyboardReducer.reduce(request,Event.Cancel).state
        KeyboardReducer.reduce(cancelled,response(request,20)).state shouldBe cancelled
    }
    "digit during page fetch waits for the same ordering barrier" {
        val request=KeyboardReducer.reduce(composing(),Event.CandidatePage(1)).state
        KeyboardReducer.reduce(request,Event.Digit(4)).effects shouldBe listOf(Effect.Defer(1,Event.Digit(4)))
    }
})
