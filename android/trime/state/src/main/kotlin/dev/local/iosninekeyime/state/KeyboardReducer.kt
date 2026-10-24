// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.state

enum class Layout { CHINESE, LATIN, NUMBER, SYMBOLS, EMOJI }
enum class Bar { UNAVAILABLE, PRIVATE, PUNCTUATION, CANDIDATES, NO_CANDIDATES, TOOLS }
enum class EditorAction { NEWLINE, DONE, SEND, SEARCH, NEXT, GO }
data class Stamp(val session: Long, val revision: Long)
data class CandidateId(val stamp: Stamp, val index: Int)
data class Candidate(val id: CandidateId, val text: String) {
    override fun toString() = "Candidate(id=$id)"
}
sealed interface Tail {
    data class Punctuation(val text: String) : Tail
    data class ChangeLayout(val layout: Layout) : Tail
}
data class KeyboardState(
    val stamp: Stamp = Stamp(0, 0),
    val layout: Layout = Layout.CHINESE,
    val sensitive: Boolean = false,
    val action: EditorAction = EditorAction.NEWLINE,
    val ready: Boolean = false,
    val input: String = "",
    val preedit: String = "",
    val candidates: List<Candidate> = emptyList(),
    val awaiting: Boolean = false,
    val expanded: Boolean = false,
    val tail: Tail? = null,
    val errorCode: String? = null,
    val compositionLength: Int = 0,
    val candidateOffset: Int = 0,
    val hasMoreCandidates: Boolean = false,
    val primaryCandidateText: String? = null,
    val requestedCandidateOffset: Int? = null,
    val returnLayout: Layout = Layout.CHINESE,
    val emojiReturnLayout: Layout = Layout.CHINESE,
) {
    val composing get() = input.isNotEmpty() || compositionLength > 0
    val bar: Bar get() = when {
        sensitive -> Bar.PRIVATE
        layout == Layout.CHINESE && !ready -> Bar.UNAVAILABLE
        composing || awaiting -> if (!awaiting && candidates.isEmpty()) Bar.NO_CANDIDATES else Bar.CANDIDATES
        layout == Layout.CHINESE -> Bar.PUNCTUATION
        else -> Bar.TOOLS
    }
    override fun toString() = "KeyboardState(stamp=$stamp, layout=$layout, sensitive=$sensitive, inputLength=${input.length}, candidates=${candidates.size}, awaiting=$awaiting)"
}
sealed interface Event {
    data class Start(val session: Long, val sensitive: Boolean, val layout: Layout, val action: EditorAction) : Event
    data class Ready(val session: Long, val success: Boolean) : Event
    data class Digit(val value: Int) : Event
    data class Text(val value: String) : Event { override fun toString() = "Text(length=${value.length})" }
    data class Choose(val id: CandidateId) : Event
    data class Punctuation(val value: String) : Event
    data class ChangeLayout(val layout: Layout) : Event
    data class Snapshot(val stamp: Stamp, val input: String, val preedit: String, val candidates: List<String>, val commit: String? = null, val compositionLength: Int = input.length,
                        val candidateOffset: Int = 0, val hasMoreCandidates: Boolean = false) : Event {
        override fun toString() = "Snapshot(stamp=$stamp, inputLength=${input.length}, candidates=${candidates.size})"
    }
    data object Space : Event
    data object Enter : Event
    data object Backspace : Event
    data object Cancel : Event
    data object Expand : Event
    data class CandidatePage(val direction: Int) : Event
}
sealed interface Effect {
    data class Begin(val session: Long, val sensitive: Boolean) : Effect
    data class Key(val stamp: Stamp, val key: Int) : Effect
    data class Select(val stamp: Stamp, val index: Int) : Effect
    data class Clear(val stamp: Stamp) : Effect
    data class FetchCandidates(val stamp: Stamp, val offset: Int, val limit: Int) : Effect
    data class Commit(val stamp: Stamp, val part: Int, val text: String) : Effect {
        override fun toString() = "Commit(stamp=$stamp, part=$part, length=${text.length})"
    }
    data class Delete(val session: Long, val sensitive: Boolean) : Effect
    data class Action(val session: Long, val action: EditorAction) : Effect
    /** Controller queues this semantic event, with the current session, until its response barrier. */
    data class Defer(val session: Long, val event: Event) : Effect
}
data class Transition(val state: KeyboardState, val effects: List<Effect> = emptyList())

object KeyboardReducer {
    const val CANDIDATE_PAGE_SIZE = 20
    val punctuation = listOf("，", "。", "？", "！", "、", "：", "；", "……")
    val mapping = linkedMapOf(2 to "ABC", 3 to "DEF", 4 to "GHI", 5 to "JKL", 6 to "MNO", 7 to "PQRS", 8 to "TUV", 9 to "WXYZ")
    private fun changeLayout(s:KeyboardState,layout:Layout):KeyboardState = s.copy(
        layout=layout,expanded=false,
        returnLayout=if(layout in setOf(Layout.NUMBER,Layout.SYMBOLS) && s.layout in setOf(Layout.CHINESE,Layout.LATIN))s.layout else s.returnLayout,
        emojiReturnLayout=if(layout==Layout.EMOJI && s.layout!=Layout.EMOJI)s.layout else s.emojiReturnLayout,
    )
    fun encode(pinyin: String): String = pinyin.lowercase().map { ch ->
        mapping.entries.firstOrNull { ch.uppercaseChar() in it.value }?.key ?: error("E_NON_PINYIN")
    }.joinToString("")

    private fun issue(s: KeyboardState, tail: Tail? = null, command: (Stamp) -> Effect): Transition {
        val stamp = s.stamp.copy(revision = s.stamp.revision + 1)
        return Transition(s.copy(stamp = stamp, awaiting = true, candidates = emptyList(), expanded = false, tail = tail, errorCode = null,
            candidateOffset=0,hasMoreCandidates=false,primaryCandidateText=null,requestedCandidateOffset=null), listOf(command(stamp)))
    }
    private fun commit(s: KeyboardState, text: String): Transition {
        val next = s.copy(stamp = s.stamp.copy(revision = s.stamp.revision + 1), errorCode = null)
        return Transition(next, listOf(Effect.Commit(next.stamp, 0, text)))
    }
    private fun confirm(s: KeyboardState, tail: Tail? = null): Transition =
        if (s.candidates.isEmpty() || (s.candidateOffset>0 && s.primaryCandidateText==null)) Transition(s.copy(errorCode = "E_NO_CANDIDATE"))
        else issue(s, tail) { Effect.Select(it, if(s.candidateOffset>0)0 else s.candidates.first().id.index) }

    private fun fetchPage(s:KeyboardState,offset:Int,expanded:Boolean):Transition {
        val stamp=s.stamp.copy(revision=s.stamp.revision+1)
        return Transition(s.copy(stamp=stamp,awaiting=true,expanded=expanded,requestedCandidateOffset=offset,errorCode=null),
            listOf(Effect.FetchCandidates(stamp,offset,CANDIDATE_PAGE_SIZE)))
    }

    fun reduce(s: KeyboardState, e: Event): Transition {
        if (e is Event.Start) {
            if (e.session <= s.stamp.session) return Transition(s)
            val layout = if (e.sensitive && e.layout == Layout.CHINESE) Layout.LATIN else e.layout
            return Transition(KeyboardState(Stamp(e.session, 0), layout, e.sensitive, e.action, returnLayout=if(e.sensitive)Layout.LATIN else Layout.CHINESE), listOf(Effect.Begin(e.session, e.sensitive)))
        }
        if (e is Event.Ready) return if (e.session == s.stamp.session && !s.sensitive)
            Transition(s.copy(ready = e.success, errorCode = if (e.success) null else "E_ENGINE")) else Transition(s)
        if (e is Event.Snapshot) {
            if (s.sensitive || !s.awaiting || e.stamp != s.stamp) return Transition(s)
            val page=s.requestedCandidateOffset
            if(e.candidateOffset!=(page?:0) || e.candidateOffset<0 || e.candidateOffset>Int.MAX_VALUE-e.candidates.size ||
                (page!=null && (e.input!=s.input || e.preedit!=s.preedit || e.compositionLength!=s.compositionLength || !e.commit.isNullOrEmpty() || e.candidates.size>CANDIDATE_PAGE_SIZE))) {
                return Transition(s.copy(ready=false,awaiting=false,candidates=emptyList(),hasMoreCandidates=false,
                    primaryCandidateText=null,requestedCandidateOffset=null,errorCode="E_CANDIDATE_PAGE"))
            }
            val active = e.input.isNotEmpty() || e.compositionLength > 0
            val choices = if (active) e.candidates.mapIndexed { i, text -> Candidate(CandidateId(e.stamp, e.candidateOffset+i), text) } else emptyList()
            val primary=if(!active)null else if(e.candidateOffset==0)e.candidates.firstOrNull() else s.primaryCandidateText
            var next = s.copy(input = e.input, compositionLength = e.compositionLength, preedit = if (active) e.preedit else "", candidates = choices, awaiting = false, expanded = s.expanded && active,
                candidateOffset=if(active)e.candidateOffset else 0,hasMoreCandidates=active&&e.hasMoreCandidates,
                primaryCandidateText=primary,requestedCandidateOffset=null)
            val effects = mutableListOf<Effect>()
            if (!e.commit.isNullOrEmpty()) effects += Effect.Commit(e.stamp, 0, e.commit)
            if (active && s.tail != null) {
                if (choices.isNotEmpty()) {
                    val follow = confirm(next, s.tail)
                    return Transition(follow.state, effects + follow.effects)
                }
                next = next.copy(tail = null, errorCode = "E_NO_CANDIDATE")
            }
            if (!active) {
                when (val tail = s.tail) {
                    is Tail.Punctuation -> effects += Effect.Commit(e.stamp, 1, tail.text)
                    is Tail.ChangeLayout -> next = changeLayout(next,tail.layout)
                    null -> Unit
                }
                next = next.copy(tail = null)
            }
            return Transition(next, effects)
        }
        if (e == Event.Cancel) {
            if (s.sensitive || !s.ready) return Transition(s.copy(
                stamp = s.stamp.copy(revision = s.stamp.revision + 1),
                input = "", preedit = "", compositionLength = 0,
                candidates = emptyList(), awaiting = false, expanded = false, tail = null,
                candidateOffset=0,hasMoreCandidates=false,primaryCandidateText=null,requestedCandidateOffset=null,
            ))
            return issue(s.copy(tail = null)) { Effect.Clear(it) }
        }
        if (s.awaiting) return Transition(s, listOf(Effect.Defer(s.stamp.session, e)))
        return when (e) {
            is Event.Digit -> when {
                e.value !in 2..9 -> Transition(s.copy(errorCode = "E_DIGIT"))
                s.sensitive || s.layout != Layout.CHINESE -> commit(s, e.value.toString())
                !s.ready -> Transition(s, listOf(Effect.Defer(s.stamp.session, e)))
                else -> issue(s) { Effect.Key(it, '0'.code + e.value) }
            }
            is Event.Text -> if (s.composing) Transition(s.copy(errorCode = "E_COMPOSING")) else commit(s, e.value)
            is Event.Choose -> if (s.composing && e.id.stamp == s.stamp && s.candidates.any { it.id == e.id }) issue(s) { Effect.Select(it, e.id.index) } else Transition(s)
            Event.Space -> if (s.composing) confirm(s) else commit(s, " ")
            Event.Enter -> if (s.composing) confirm(s) else Transition(s, listOf(Effect.Action(s.stamp.session, s.action)))
            is Event.Punctuation -> if (e.value !in punctuation) Transition(s.copy(errorCode = "E_PUNCTUATION")) else if (s.composing) confirm(s, Tail.Punctuation(e.value)) else commit(s, e.value)
            is Event.ChangeLayout -> when {
                s.sensitive && e.layout == Layout.CHINESE -> Transition(s.copy(errorCode = "E_PRIVATE_DIRECT_ONLY"))
                s.composing -> confirm(s, Tail.ChangeLayout(e.layout))
                else -> Transition(changeLayout(s,e.layout))
            }
            Event.Backspace -> if (s.composing) issue(s) { Effect.Key(it, 0xff08) } else Transition(s, listOf(Effect.Delete(s.stamp.session, s.sensitive)))
            Event.Expand -> if(s.expanded && s.candidateOffset>0)fetchPage(s,0,false)
                else Transition(s.copy(expanded = s.composing && s.candidates.isNotEmpty() && !s.expanded))
            is Event.CandidatePage -> when {
                s.sensitive || !s.ready || !s.expanded || !s.composing -> Transition(s)
                e.direction !in listOf(-1,1) -> Transition(s.copy(errorCode="E_CANDIDATE_PAGE"))
                e.direction<0 && s.candidateOffset==0 -> Transition(s)
                e.direction>0 && !s.hasMoreCandidates -> Transition(s)
                e.direction>0 && s.candidateOffset>Int.MAX_VALUE-CANDIDATE_PAGE_SIZE -> Transition(s.copy(errorCode="E_CANDIDATE_PAGE"))
                else -> fetchPage(s,(s.candidateOffset+e.direction*CANDIDATE_PAGE_SIZE).coerceAtLeast(0),true)
            }
            else -> Transition(s)
        }
    }
}
