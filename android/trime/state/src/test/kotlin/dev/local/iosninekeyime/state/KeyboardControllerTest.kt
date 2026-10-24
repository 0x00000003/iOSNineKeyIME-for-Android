// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.state

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class KeyboardControllerTest : StringSpec({
    class Engine : KeyboardEnginePort {
        val commands = mutableListOf<Effect>()
        val starts = mutableListOf<Pair<Long, Boolean>>()
        val ready = mutableListOf<(Boolean) -> Unit>()
        val replies = mutableListOf<(Event.Snapshot?) -> Unit>()
        val ends = mutableListOf<Long>()
        override fun begin(session: Long, sensitive: Boolean, reply: (Boolean) -> Unit) { starts += session to sensitive; ready += reply }
        override fun execute(effect: Effect, reply: (Event.Snapshot?) -> Unit) { commands += effect; replies += reply }
        override fun end(session: Long) { ends += session }
    }
    class Host : KeyboardHostPort {
        var available = true
        var deferredDelete = false
        var onEnd: (() -> Unit)? = null
        val deletes = mutableListOf<(Boolean) -> Unit>()
        val text = mutableListOf<String>()
        var reenter: (() -> Unit)? = null
        val gate = HostCommitGate(commit = { value ->
            if (!available) false else { text += value; reenter?.also { reenter = null }?.invoke(); true }
        })
        override val pendingCount get() = gate.pendingCount
        override fun begin(session: Long, sensitive: Boolean) { gate.startSession(session) }
        override fun commit(effect: Effect.Commit) = gate.deliver(effect)
        override fun retry(session: Long) = gate.retry(session)
        override fun deleteAsync(session: Long, sensitive: Boolean, reply: (Boolean) -> Unit) {
            deletes += reply
            if (!deferredDelete) reply(true)
        }
        override fun action(session: Long, action: EditorAction) {}
        override fun end(session: Long) { gate.endSession(session); onEnd?.invoke() }
    }
    fun Engine.answer(index: Int, input: String, commit: String? = null, candidates: List<String> = if (input.isEmpty()) emptyList() else listOf("synthetic"), offset:Int=0, more:Boolean=false) {
        val stamp = when (val c = commands[index]) { is Effect.Key -> c.stamp; is Effect.Select -> c.stamp; is Effect.Clear -> c.stamp; is Effect.FetchCandidates -> c.stamp; else -> error("fixture") }
        replies[index](Event.Snapshot(stamp, input, input, candidates, commit,candidateOffset=offset,hasMoreCandidates=more))
    }
    "pinyin query serializes a following digit and invalidates old option selections" {
        val e=Engine();val h=Host();val c=KeyboardController(e,h)
        c.start(1,false,Layout.CHINESE,EditorAction.DONE);e.ready[0](true)
        c.submit(Event.Digit(6));e.answer(0,"6")
        c.submit(Event.RequestPinyin)
        val q=e.commands[1]as Effect.QueryPinyin
        c.submit(Event.Digit(4));c.submit(Event.ChoosePinyin(q.stamp,0))
        e.commands.size shouldBe 2
        e.replies[1](Event.Snapshot(q.stamp,"6","6",listOf("synthetic"),pinyinChoices=listOf(PinyinChoice("ni",0,1))))
        e.commands.size shouldBe 3
        (e.commands[2]as Effect.Key).key shouldBe '4'.code
        e.answer(2,"64")
        e.commands.size shouldBe 3;c.state.pinyinChoices shouldBe emptyList();h.text shouldBe emptyList()
    }
    "pinyin query callback after field cutover cannot commit or repopulate options" {
        val e=Engine();val h=Host();val c=KeyboardController(e,h)
        c.start(1,false,Layout.CHINESE,EditorAction.DONE);e.ready[0](true)
        c.submit(Event.Digit(6));e.answer(0,"6");c.submit(Event.RequestPinyin)
        val q=e.commands[1]as Effect.QueryPinyin
        c.start(2,true,Layout.LATIN,EditorAction.DONE)
        e.replies[1](Event.Snapshot(q.stamp,"6","6",listOf("synthetic"),commit="synthetic",pinyinChoices=listOf(PinyinChoice("ni",0,1))))
        c.state.pinyinChoices shouldBe emptyList();h.text shouldBe emptyList();c.state.stamp.session shouldBe 2L
    }
    "failed pinyin operation clears options and drops queued continuation" {
        val e=Engine();val h=Host();val c=KeyboardController(e,h)
        c.start(1,false,Layout.CHINESE,EditorAction.DONE);e.ready[0](true)
        c.submit(Event.Digit(6));e.answer(0,"6");c.submit(Event.RequestPinyin)
        val q=e.commands[1]as Effect.QueryPinyin
        e.replies[1](Event.Snapshot(q.stamp,"6","6",listOf("synthetic"),pinyinChoices=listOf(PinyinChoice("ni",0,1))))
        c.submit(Event.ChoosePinyin(c.state.stamp,0));c.submit(Event.Digit(4));e.replies[2](null)
        c.state.errorCode shouldBe "E_ENGINE";c.state.pinyinChoices shouldBe emptyList();e.commands.size shouldBe 3;h.text shouldBe emptyList()
    }
    "host deletion blocks following input and held repeats until one reply" {
        val e=Engine();val h=Host();h.deferredDelete=true;val c=KeyboardController(e,h)
        c.start(1,true,Layout.LATIN,EditorAction.DONE)
        c.submit(Event.Backspace);repeat(30){c.submitRepeat(Event.Backspace)};c.submit(Event.Text("a"))
        h.deletes.size shouldBe 1;h.text shouldBe emptyList()
        h.deletes[0](true);h.text shouldBe listOf("a")
        h.deletes[0](false);c.state.errorCode shouldBe null
    }
    "failed or ambiguous host deletion drops deferred input and cannot be retried as a commit" {
        val e=Engine();val h=Host();h.deferredDelete=true;val c=KeyboardController(e,h)
        c.start(1,true,Layout.LATIN,EditorAction.DONE)
        c.submit(Event.Backspace);c.submit(Event.Text("a"));h.deletes[0](false)
        c.state.errorCode shouldBe "E_HOST_DELETE";h.text shouldBe emptyList()
        c.retryHost(1);c.submit(Event.Cancel);c.state.errorCode shouldBe "E_HOST_DELETE";c.submit(Event.Text("b"));h.text shouldBe emptyList()
        c.start(2,true,Layout.LATIN,EditorAction.DONE);c.submit(Event.Text("c"));h.text shouldBe listOf("c")
        h.deletes[0](true);h.text shouldBe listOf("c")
    }
    "synchronous delete completion from end cannot drain old field input" {
        val e=Engine();val h=Host();h.deferredDelete=true;val c=KeyboardController(e,h)
        c.start(1,true,Layout.LATIN,EditorAction.DONE);c.submit(Event.Backspace);c.submit(Event.Text("old"))
        h.onEnd={h.deletes[0](true)}
        c.start(2,true,Layout.LATIN,EditorAction.DONE);c.submit(Event.Text("new"))
        h.text shouldBe listOf("new");c.state.stamp.session shouldBe 2L
    }
    "late deletion response cannot unblock a different field deletion" {
        val e=Engine();val h=Host();h.deferredDelete=true;val c=KeyboardController(e,h)
        c.start(1,true,Layout.LATIN,EditorAction.DONE);c.submit(Event.Backspace)
        c.start(2,true,Layout.LATIN,EditorAction.DONE);c.submit(Event.Backspace);c.submit(Event.Text("a"))
        h.deletes[0](true);h.text shouldBe emptyList();h.deletes[1](true);h.text shouldBe listOf("a")
    }
    "native page is an ordering barrier before the next digit" {
        val e=Engine();val h=Host();val c=KeyboardController(e,h)
        c.start(1,false,Layout.CHINESE,EditorAction.DONE);e.ready[0](true)
        c.submit(Event.Digit(6));e.answer(0,"6",candidates=List(20){"synthetic-$it"},more=true)
        c.submit(Event.Expand);c.submit(Event.CandidatePage(1));c.submit(Event.Digit(4))
        e.commands.size shouldBe 2
        (e.commands[1]as Effect.FetchCandidates).offset shouldBe 20
        e.answer(1,"6",candidates=List(20){"synthetic-${20+it}"},offset=20,more=true)
        e.commands.size shouldBe 3
        (e.commands[2]as Effect.Key).key shouldBe '4'.code
        e.answer(2,"64");c.state.candidateOffset shouldBe 0;h.text shouldBe emptyList()
        c.end(1);c.state.primaryCandidateText shouldBe null;c.state.hasMoreCandidates shouldBe false
    }
    "field switch invalidates delayed native page including an injected commit" {
        val e=Engine();val h=Host();val c=KeyboardController(e,h)
        c.start(1,false,Layout.CHINESE,EditorAction.DONE);e.ready[0](true)
        c.submit(Event.Digit(6));e.answer(0,"6",more=true)
        c.submit(Event.Expand);c.submit(Event.CandidatePage(1))
        c.start(2,true,Layout.LATIN,EditorAction.DONE)
        e.answer(1,"6",commit="stale",offset=20)
        c.state.stamp.session shouldBe 2L;c.state.primaryCandidateText shouldBe null
        c.submit(Event.CandidatePage(1));e.commands.size shouldBe 2;h.text shouldBe emptyList()
    }
    "fast keys wait for readiness and then one native response at a time" {
        val e = Engine(); val h = Host(); val c = KeyboardController(e,h)
        c.start(1,false,Layout.CHINESE,EditorAction.DONE)
        c.submit(Event.Digit(6)); c.submit(Event.Digit(4))
        e.commands.size shouldBe 0
        e.ready[0](true); e.commands.size shouldBe 1
        c.state.bar shouldBe Bar.CANDIDATES
        e.answer(0,"6"); e.commands.size shouldBe 2
        e.answer(1,"64"); c.state.input shouldBe "64"
        h.text shouldBe emptyList()
    }
    "field change and end reject late native commits and duplicate readiness" {
        val e=Engine();val h=Host();val c=KeyboardController(e,h)
        c.start(1,false,Layout.CHINESE,EditorAction.DONE);e.ready[0](true);c.submit(Event.Digit(6))
        c.start(2,true,Layout.LATIN,EditorAction.DONE)
        e.answer(0,"", "old");e.ready[0](false)
        c.state.stamp.session shouldBe 2L;h.text shouldBe emptyList()
        c.end(2);c.submit(Event.Text("ignored"));h.text shouldBe emptyList()
        e.ends shouldBe listOf(1L,2L)
    }
    "partial commit and follow-up selection preserve effect order across revision" {
        val e=Engine();val h=Host();val c=KeyboardController(e,h)
        c.start(1,false,Layout.CHINESE,EditorAction.DONE);e.ready[0](true);c.submit(Event.Digit(6));e.answer(0,"64426744543")
        c.submit(Event.Punctuation("。"));e.answer(1,"744543","prefix")
        e.commands.size shouldBe 3;h.text shouldBe listOf("prefix")
        e.answer(2,"","suffix");h.text shouldBe listOf("prefix","suffix","。")
        c.state.bar shouldBe Bar.PUNCTUATION
        e.answer(2,"","suffix");h.text.size shouldBe 3
    }
    "blocked host pauses remaining punctuation and later input until explicit same-field retry" {
        val e=Engine();val h=Host();val c=KeyboardController(e,h)
        c.start(1,false,Layout.CHINESE,EditorAction.DONE);e.ready[0](true);c.submit(Event.Digit(6));e.answer(0,"6")
        c.submit(Event.Punctuation("。"));h.available=false;e.answer(1,"","word")
        c.submit(Event.Digit(4));h.text shouldBe emptyList();e.commands.size shouldBe 2
        h.available=true;c.retryHost(1)
        h.text shouldBe listOf("word","。");e.commands.size shouldBe 3
    }
    "blocked partial commit resumes its selection without losing or duplicating it" {
        val e=Engine();val h=Host();val c=KeyboardController(e,h)
        c.start(1,false,Layout.CHINESE,EditorAction.DONE);e.ready[0](true);c.submit(Event.Digit(6));e.answer(0,"64426744543")
        c.submit(Event.Punctuation("。"));h.available=false;e.answer(1,"744543","prefix")
        e.commands.size shouldBe 2
        h.available=true;c.retryHost(1);e.commands.size shouldBe 3
        e.answer(2,"","suffix");h.text shouldBe listOf("prefix","suffix","。")
    }
    "synchronous host field switch aborts punctuation tail from old field" {
        val e=Engine();val h=Host();val c=KeyboardController(e,h)
        c.start(1,false,Layout.CHINESE,EditorAction.DONE);e.ready[0](true);c.submit(Event.Digit(6));e.answer(0,"6")
        c.submit(Event.Punctuation("。"));h.reenter={c.start(2,true,Layout.LATIN,EditorAction.DONE)}
        e.answer(1,"","word");h.text shouldBe listOf("word");c.state.stamp.session shouldBe 2L
    }
    "cancel invalidates queued digits and outstanding snapshot but waits for its own reset" {
        val e=Engine();val h=Host();val c=KeyboardController(e,h)
        c.start(1,false,Layout.CHINESE,EditorAction.DONE);e.ready[0](true)
        c.submit(Event.Digit(6));c.submit(Event.Digit(4));c.submit(Event.Cancel)
        e.commands.size shouldBe 2;e.answer(0,"6","stale");h.text shouldBe emptyList()
        e.answer(1,"");c.state.bar shouldBe Bar.PUNCTUATION;e.commands.size shouldBe 2
    }
    "native failure is explicit and clears deferred input without replay" {
        val e=Engine();val h=Host();val c=KeyboardController(e,h)
        c.start(1,false,Layout.CHINESE,EditorAction.DONE);e.ready[0](true);c.submit(Event.Digit(6));c.submit(Event.Digit(4))
        e.replies[0](null);c.state.errorCode shouldBe "E_ENGINE";c.state.ready shouldBe false
        e.commands.size shouldBe 1;h.text shouldBe emptyList()
    }
    "bounded queue reports overflow and sensitive typing never calls native execute" {
        val e=Engine();val h=Host();val c=KeyboardController(e,h,capacity=2)
        c.start(1,false,Layout.CHINESE,EditorAction.DONE);repeat(3){c.submit(Event.Digit(6))}
        c.state.errorCode shouldBe "E_QUEUE_CAPACITY"
        c.start(2,true,Layout.LATIN,EditorAction.DONE);c.submit(Event.Text("a"));c.submit(Event.Digit(2))
        h.text shouldBe listOf("a","2");e.commands shouldBe emptyList()
    }
    "render callback can start new field without stale commands or replay" {
        val e=Engine();val h=Host();var switch=false
        lateinit var c:KeyboardController
        c=KeyboardController(e,h,onState={if(switch && it.awaiting){switch=false;c.start(2,true,Layout.LATIN,EditorAction.DONE)}})
        c.start(1,false,Layout.CHINESE,EditorAction.DONE);e.ready[0](true);switch=true;c.submit(Event.Digit(6))
        c.state.stamp.session shouldBe 2L;e.commands shouldBe emptyList()
    }

    "held deletion never queues a burst behind an outstanding native response" {
        val e=Engine();val h=Host();val c=KeyboardController(e,h)
        c.start(1,false,Layout.CHINESE,EditorAction.DONE);e.ready[0](true);c.submit(Event.Digit(6));e.answer(0,"64426")
        c.submitRepeat(Event.Backspace);repeat(30){c.submitRepeat(Event.Backspace)}
        e.commands.size shouldBe 2;e.answer(1,"6442");e.commands.size shouldBe 2
        c.end(1);c.submitRepeat(Event.Backspace);e.commands.size shouldBe 2
    }
})
