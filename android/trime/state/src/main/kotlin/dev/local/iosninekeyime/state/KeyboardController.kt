// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.state

/** Implementations serialize native calls and marshal every reply back to the controller owner. */
interface KeyboardEnginePort {
    fun begin(session: Long, sensitive: Boolean, reply: (Boolean) -> Unit)
    fun execute(effect: Effect, reply: (Event.Snapshot?) -> Unit)
    fun end(session: Long)
}
interface KeyboardHostPort {
    val pendingCount: Int
    fun begin(session: Long, sensitive: Boolean)
    fun commit(effect: Effect.Commit): Delivery
    fun retry(session: Long): Delivery
    fun deleteAsync(session: Long, sensitive: Boolean, reply: (Boolean) -> Unit)
    fun action(session: Long, action: EditorAction)
    fun end(session: Long)
}

/** Owner-thread controller. Queues contain only in-memory data, never log/serialize input. */
class KeyboardController(
    private val engine: KeyboardEnginePort,
    private val host: KeyboardHostPort,
    private val capacity: Int = 128,
    private val onState: (KeyboardState) -> Unit = {},
    private val checkOwner: () -> Unit = {},
) {
    var state = KeyboardState()
        private set
    private var active = false
    private var epoch = 0L
    private var pumping = false
    private var failed = false
    private var readyReceived = false
    private var hostBlocked = false
    private var deleteWaiting = false
    private val events = ArrayDeque<Event>()
    private val responses = ArrayDeque<Pair<Long, () -> Unit>>()
    private val remaining = ArrayDeque<Effect>()

    init { require(capacity > 0) }

    fun start(session: Long, sensitive: Boolean, layout: Layout, action: EditorAction) {
        checkOwner()
        if (session <= state.stamp.session) return
        val previous = if (active) state.stamp.session else null
        active = false; epoch++
        val generation = epoch
        // Invalidate before ending ports: cancellation may reply synchronously.
        if (previous != null) {
            host.end(previous)
            engine.end(previous)
        }
        if (generation != epoch) return
        active = true; failed = false; readyReceived = false; hostBlocked = false; deleteWaiting = false
        events.clear(); responses.clear(); remaining.clear()
        state = KeyboardReducer.reduce(state, Event.Start(session,sensitive,layout,action)).state
        host.begin(session,sensitive)
        if (generation != epoch || !active) return
        onState(state)
        if (generation != epoch || !active) return
        engine.begin(session,sensitive) { success -> receive(generation) {
            if (!readyReceived) {
                readyReceived = true
                if (!sensitive) {
                    failed = !success
                    if (!success) events.clear()
                    transition(Event.Ready(session,success))
                }
            }
        } }
    }

    fun end(session: Long) {
        checkOwner()
        if (!active || session != state.stamp.session) return
        active = false; epoch++
        events.clear(); responses.clear(); remaining.clear(); hostBlocked = false; deleteWaiting = false
        host.end(session); engine.end(session)
        state = state.copy(ready=false,input="",preedit="",compositionLength=0,candidates=emptyList(),awaiting=false,tail=null,expanded=false,
            candidateOffset=0,hasMoreCandidates=false,primaryCandidateText=null,requestedCandidateOffset=null,pinyinChoices=emptyList())
        onState(state)
    }

    fun submit(event: Event) {
        checkOwner()
        require(event !is Event.Start && event !is Event.Ready && event !is Event.Snapshot)
        if (!active || failed) return
        if (event == Event.Cancel && !hostBlocked) {
            events.clear()
            transition(event)
            pump()
            return
        }
        if (events.size >= capacity) { fault("E_QUEUE_CAPACITY"); return }
        events += event
        pump()
    }

    /** A held delete never accumulates deferred repeats behind an engine/host barrier. */
    fun submitRepeat(event: Event) {
        checkOwner()
        require(event == Event.Backspace)
        if (active && !state.awaiting && !hostBlocked && !deleteWaiting && events.isEmpty() && !failed && (state.ready || state.sensitive || state.layout != Layout.CHINESE)) submit(event)
    }

    /** Reconnect alone never calls this; the host must explicitly authorize same-session retry. */
    fun retryHost(session: Long) {
        checkOwner()
        if (!active || session != state.stamp.session || !hostBlocked) return
        val generation = epoch
        val delivery = host.retry(session)
        if (!active || generation != epoch) return
        if (delivery == Delivery.ACCEPTED || delivery == Delivery.DUPLICATE) {
            hostBlocked = false
            state = state.copy(errorCode=null)
            val rest = remaining.toList(); remaining.clear()
            perform(rest,generation)
            if (generation == epoch && active) { onState(state); pump() }
        } else if (delivery != Delivery.BLOCKED) fault("E_HOST_DELIVERY")
    }

    private fun receive(generation: Long, response: () -> Unit) {
        checkOwner()
        if (!active || generation != epoch) return
        // A port may reply synchronously. This barrier prevents nested response processing.
        if (responses.size >= capacity) { fault("E_RESPONSE_CAPACITY"); return }
        responses += generation to response
        pump()
    }

    private fun pump() {
        if (pumping) return
        pumping = true
        try {
            while (active) {
                if (responses.isNotEmpty()) {
                    val (generation,response) = responses.removeFirst()
                    if (generation == epoch) response()
                    continue
                }
                if (hostBlocked || deleteWaiting || state.awaiting || failed || events.isEmpty()) break
                // All events retain arrival order while deployment/first key is outstanding.
                if (!state.sensitive && state.layout == Layout.CHINESE && !state.ready) break
                transition(events.removeFirst())
            }
        } finally { pumping = false }
    }

    private fun transition(event: Event) {
        val generation = epoch
        val next = KeyboardReducer.reduce(state,event)
        state = next.state
        onState(state)
        if (generation == epoch && active) perform(next.effects,generation)
    }

    private fun perform(effects: List<Effect>, generation: Long) {
        for ((index,effect) in effects.withIndex()) {
            if (!active || generation != epoch) return
            when (effect) {
                is Effect.Key, is Effect.Select, is Effect.Clear, is Effect.FetchCandidates, is Effect.QueryPinyin, is Effect.SelectPinyin -> {
                    val stamp = when(effect) { is Effect.Key -> effect.stamp; is Effect.Select -> effect.stamp; is Effect.Clear -> effect.stamp; is Effect.FetchCandidates -> effect.stamp; is Effect.QueryPinyin -> effect.stamp; is Effect.SelectPinyin -> effect.stamp; else -> error("E_COMMAND") }
                    engine.execute(effect) { snapshot -> receive(generation) {
                        if (state.awaiting && stamp == state.stamp) {
                            if (snapshot == null || snapshot.stamp != stamp) {
                                failed = true; events.clear()
                                state = state.copy(ready=false,awaiting=false,tail=null,pinyinChoices=emptyList(),errorCode="E_ENGINE")
                                onState(state)
                            } else transition(snapshot)
                        }
                    } }
                }
                is Effect.Commit -> {
                    val delivery = host.commit(effect)
                    if (!active || generation != epoch) return
                    if (delivery == Delivery.BLOCKED) {
                        hostBlocked = true
                        remaining.addAll(effects.drop(index+1))
                        fault("E_HOST_PENDING")
                        return
                    }
                    if (delivery != Delivery.ACCEPTED && delivery != Delivery.DUPLICATE) {
                        failed = true; events.clear(); fault("E_HOST_DELIVERY"); return
                    }
                }
                is Effect.Delete -> {
                    deleteWaiting = true
                    var received = false
                    host.deleteAsync(effect.session,effect.sensitive) { success -> receive(generation) {
                        if (!received) {
                            received = true; deleteWaiting = false
                            if (success) perform(effects.drop(index+1),generation)
                            else { failed = true; events.clear(); remaining.clear(); fault("E_HOST_DELETE") }
                        }
                    } }
                    return
                }
                is Effect.Action -> host.action(effect.session,effect.action)
                is Effect.Defer -> {
                    if (events.size < capacity) events.addFirst(effect.event) else fault("E_QUEUE_CAPACITY")
                }
                is Effect.Begin -> Unit // start() owns connection and native lifecycle ordering.
            }
        }
    }

    private fun fault(code: String) { state = state.copy(errorCode=code); onState(state) }
}
