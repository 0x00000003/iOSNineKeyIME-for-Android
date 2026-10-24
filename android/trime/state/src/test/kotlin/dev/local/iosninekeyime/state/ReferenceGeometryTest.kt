// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.state

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ReferenceGeometryTest:StringSpec({
    "portrait and constrained landscape panels keep every physical key inside content and above navigation" {
        for(width in listOf(320f,360f,390f,426.67f,800f))for(inset in listOf(0f,24f,48f))for(layout in Layout.entries){
            val p=ReferenceGeometry.build(KeyboardState(Stamp(1,0),layout,ready=true),width,inset,if(width>600)400f else 1000f,emojiItems=List(28){"😀"})
            val invalid=p.keys.filter{val k=it.box;k.left<0 || k.right>width+0.01f || k.top<0 || k.bottom>p.contentHeight+0.01f || k.bottom-k.top<47.99f}
            check(invalid.isEmpty()){"E_GEOMETRY_${layout.name}_${width}_${inset}_${invalid.map{it.box.id}}"}
            (p.totalHeight<=if(width>600)400f else 1000f) shouldBe true
            HitMap(p.keys.map{it.box}).keys.size shouldBe p.keys.size
        }
    }
    "search and auxiliary pages never overlap their query field or navigation" {
        for(width in listOf(320f,426.67f,800f))for(layout in Layout.entries){
            val p=ReferenceGeometry.build(KeyboardState(Stamp(1,0),layout,ready=true),width,24f,if(width>600)400f else 1000f,
                symbolPage=true,numberMore=true,emojiItems=List(28){"😀"},emojiSearch=true)
            p.keys.all{it.box.bottom<=p.contentHeight+0.01f} shouldBe true
            HitMap(p.keys.map{it.box}).keys.size shouldBe p.keys.size
            if(layout==Layout.EMOJI)p.keys.filter{it.box.id.startsWith("search-")}.all{it.event==null} shouldBe true
        }
    }
    "emoji and symbol returns retain the originating keyboard without reading a host field" {
        var s=KeyboardState(Stamp(1,0),Layout.LATIN,ready=true)
        s=KeyboardReducer.reduce(s,Event.ChangeLayout(Layout.SYMBOLS)).state
        s.returnLayout shouldBe Layout.LATIN
        s=KeyboardReducer.reduce(s,Event.ChangeLayout(Layout.EMOJI)).state
        s.emojiReturnLayout shouldBe Layout.SYMBOLS
        s=KeyboardReducer.reduce(s,Event.ChangeLayout(s.emojiReturnLayout)).state
        s.returnLayout shouldBe Layout.LATIN
        KeyboardReducer.reduce(s,Event.ChangeLayout(s.returnLayout)).state.layout shouldBe Layout.LATIN
    }
    "entering emoji during composition first confirms once and retains the return destination" {
        var s=KeyboardState(Stamp(1,1),Layout.CHINESE,ready=true,input="64",candidates=listOf(Candidate(CandidateId(Stamp(1,1),0),"synthetic")))
        val switch=KeyboardReducer.reduce(s,Event.ChangeLayout(Layout.EMOJI))
        switch.state.layout shouldBe Layout.CHINESE
        switch.effects.filterIsInstance<Effect.Select>().size shouldBe 1
        s=KeyboardReducer.reduce(switch.state,Event.Snapshot(switch.state.stamp,"","",emptyList(),"synthetic")).state
        s.layout shouldBe Layout.EMOJI
        s.emojiReturnLayout shouldBe Layout.CHINESE
    }
})
