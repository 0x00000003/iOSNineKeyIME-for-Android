// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.state

/** Provisional geometry, in dp. Rendering and hit testing consume the same boxes. */
data class KeyBox(val id: String, val left: Float, val top: Float, val right: Float, val bottom: Float, val repeats: Boolean = false) {
    init {
        require(listOf(left, top, right, bottom).all { it.isFinite() })
        require(right > left && bottom > top)
    }
    fun contains(x: Float, y: Float) = x >= left && x < right && y >= top && y < bottom
}
class HitMap(val keys: List<KeyBox>) {
    init {
        require(keys.map { it.id }.distinct().size == keys.size)
        keys.forEachIndexed { i, a -> keys.drop(i + 1).forEach { b ->
            require(a.right <= b.left || b.right <= a.left || a.bottom <= b.top || b.bottom <= a.top) { "E_OVERLAPPING_KEYS" }
        } }
    }
    fun hit(x: Float, y: Float) = keys.firstOrNull { it.contains(x, y) }
    companion object {
        fun nineKey(width: Float, insetLeft: Float = 0f, insetRight: Float = 0f): HitMap {
            require(width.isFinite() && insetLeft >= 0 && insetRight >= 0)
            val cell = (width - insetLeft - insetRight - 24f) / 3
            require(cell >= 48f) { "E_KEYBOARD_WIDTH" }
            val labels = listOf("punctuation", "2", "3", "4", "5", "6", "7", "8", "9")
            return HitMap(labels.mapIndexed { i, label ->
                val left = insetLeft + 6 + (i % 3) * (cell + 6)
                val top = 6f + (i / 3) * 58
                KeyBox(label, left, top, left + cell, top + 52)
            })
        }
    }
}

data class KeyActivation(val session: Long, val key: String, val repeated: Boolean)

/** One pointer owns a gesture. Leaving a key or changing a field permanently cancels it. */
class TouchProtocol(private var map: HitMap) {
    private data class Press(val session: Long, val pointer: Int, val key: KeyBox, var due: Long,
                             var repeated: Boolean = false, var cancelled: Boolean = false)
    private var session = 0L
    private var press: Press? = null
    fun startSession(value: Long) {
        require(value > session)
        session = value
        press = null
    }
    fun down(pointer: Int, x: Float, y: Float, timeMs: Long) {
        if (session == 0L || press != null) return
        map.hit(x, y)?.let { press = Press(session, pointer, it, timeMs + 400) }
    }
    fun move(pointer: Int, x: Float, y: Float) {
        press?.takeIf { it.pointer == pointer }?.let { if (!it.key.contains(x, y)) it.cancelled = true }
    }
    fun up(pointer: Int, x: Float, y: Float): KeyActivation? {
        val p = press?.takeIf { it.pointer == pointer } ?: return null
        press = null
        return if (p.cancelled || p.repeated || p.session != session || !p.key.contains(x, y)) null
               else KeyActivation(session, p.key.id, false)
    }
    fun tick(timeMs: Long): KeyActivation? {
        val p = press ?: return null
        if (p.cancelled || p.session != session || !p.key.repeats || timeMs < p.due) return null
        p.repeated = true
        p.due = timeMs + 60 // No catch-up burst after a paused UI thread.
        return KeyActivation(session, p.key.id, true)
    }
    /** Static keys may keep their long press; changed/removed candidate IDs cancel it. */
    fun updateMap(next: HitMap) {
        press?.let { p -> if (next.keys.none { it == p.key }) p.cancelled = true }
        map = next
    }
    fun cancel() { press = null }
    fun accessibilityActivate(key: String, expectedSession: Long): KeyActivation? =
        if (session > 0 && expectedSession == session && map.keys.any { it.id == key }) KeyActivation(session, key, false) else null
}
