// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.state

import java.text.Normalizer
import java.util.Locale

data class PinyinChoice(val syllable: String, val start: Int, val end: Int) {
    override fun toString() = "PinyinChoice(start=$start, end=$end)"
}

/** Only the IME's unconfirmed composition and native phonetic hints enter this helper. */
object PinyinDisambiguation {
    private const val MAX_INPUT = 128
    private val rawPattern = Regex("[a-z2-9']{1,$MAX_INPUT}")
    private val hintPattern = Regex("[a-z]+(?:[ '\\t]+[a-z]+)*")
    private val separators = Regex("[ '\\t]+")
    private val marks = Regex("\\p{M}+")
    // A conservative shape check: an unsegmented multi-syllable hint is never guessed.
    private val syllablePattern = Regex(
        "(?:(?:zh|ch|sh|[bpmfdtnlgkhjqxrzcsyw])?" +
            "(?:a|o|e|ai|ei|ao|ou|an|en|ang|eng|ong|er|i|ia|ie|iao|iu|ian|in|iang|ing|iong|u|ua|uo|uai|ui|uan|un|uang|ueng|v|ve|van|vn)|m|n|ng|hm|hng)"
    )

    fun choices(input: String, confirmed: Int, comments: List<String>): List<PinyinChoice> {
        val start = digitStart(input, confirmed) ?: return emptyList()
        val prefix = input.substring(confirmed, start).split('\'').filter { it.isNotEmpty() }
        if (prefix.any { !syllablePattern.matches(it) }) return emptyList()
        val digits = input.substring(start)
        val result = linkedSetOf<PinyinChoice>()
        for (comment in comments.take(100)) {
            if (comment.length > 512) continue
            val normalized = normalize(comment)
            if (!hintPattern.matches(normalized)) continue
            val syllables = normalized.split(separators)
            if (syllables.any { !syllablePattern.matches(it) } ||
                syllables.take(prefix.size) != prefix || syllables.size <= prefix.size) continue
            val remaining = syllables.drop(prefix.size)
            if (!remaining.joinToString("") { code(it) }.startsWith(digits)) continue
            val syllable = remaining.first()
            result.add(PinyinChoice(syllable, start, start + minOf(syllable.length, digits.length)))
            if (result.size == 32) break
        }
        return result.toList()
    }

    fun rewrite(input: String, confirmed: Int, choice: PinyinChoice): String? {
        val start = digitStart(input, confirmed) ?: return null
        if (!syllablePattern.matches(choice.syllable) || choice.start != start) return null
        val length = minOf(choice.syllable.length, input.length - start)
        if (choice.end != start + length || !code(choice.syllable).startsWith(input.substring(start, choice.end))) return null
        val prefix = input.substring(0, start)
        val tail = input.substring(choice.end)
        val rewritten = prefix + (if (prefix.isNotEmpty() && !prefix.endsWith('\'')) "'" else "") +
            choice.syllable + (if (tail.isNotEmpty()) "'" else "") + tail
        return rewritten.takeIf { it.length <= MAX_INPUT }
    }

    private fun digitStart(input: String, confirmed: Int): Int? {
        if (!rawPattern.matches(input) || confirmed !in 0..input.length) return null
        val start = (confirmed until input.length).firstOrNull { input[it] in '2'..'9' } ?: return null
        if (input.substring(start).any { it !in '2'..'9' }) return null
        return start
    }

    private fun normalize(comment: String): String {
        val umlauts = comment.lowercase(Locale.ROOT).replace(Regex("[üǖǘǚǜ]"), "v")
        return Normalizer.normalize(umlauts, Normalizer.Form.NFD).replace(marks, "").trim()
    }

    private fun code(syllable: String): String = syllable.map {
        when (it) {
            in 'a'..'c' -> '2'
            in 'd'..'f' -> '3'
            in 'g'..'i' -> '4'
            in 'j'..'l' -> '5'
            in 'm'..'o' -> '6'
            in 'p'..'s' -> '7'
            in 't'..'v' -> '8'
            else -> '9'
        }
    }.joinToString("")
}
