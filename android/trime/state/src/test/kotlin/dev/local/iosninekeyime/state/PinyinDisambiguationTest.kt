// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.state

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain

class PinyinDisambiguationTest : StringSpec({
    "native tone hints produce distinct compatible syllables and precise numeric ranges" {
        val result=PinyinDisambiguation.choices("64426",0,listOf("nǐ hǎo","mí hào","nǐ gǎn","noise", "nǐ hǎo"))
        result shouldBe listOf(PinyinChoice("ni",0,2),PinyinChoice("mi",0,2))
        PinyinDisambiguation.rewrite("64426",0,result.first()) shouldBe "ni'426"
    }
    "choosing the next syllable respects the earlier explicit spelling" {
        val result=PinyinDisambiguation.choices("ni'426",0,listOf("nǐ hǎo","nǐ gǎn","mí hào"))
        result shouldBe listOf(PinyinChoice("hao",3,6),PinyinChoice("gan",3,6))
        PinyinDisambiguation.rewrite("ni'426",0,result.first()) shouldBe "ni'hao"
    }
    "confirmed numeric prefix is retained and remaining comment starts at its own syllable" {
        val result=PinyinDisambiguation.choices("64426744543",5,listOf("shì jiè"))
        result shouldBe listOf(PinyinChoice("shi",5,8))
        PinyinDisambiguation.rewrite("64426744543",5,result.single()) shouldBe "64426'shi'543"
    }
    "subsequent spelling after a selected word retains both prior boundaries" {
        val result=PinyinDisambiguation.choices("64426'shi'543",5,listOf("shì jiè"))
        result shouldBe listOf(PinyinChoice("jie",10,13))
        PinyinDisambiguation.rewrite("64426'shi'543",5,result.single()) shouldBe "64426'shi'jie"
    }
    "an explicit choice can complete a partial final syllable without dropping following input" {
        val result=PinyinDisambiguation.choices("64",0,listOf("míng", "nǐ"))
        result shouldBe listOf(PinyinChoice("ming",0,2),PinyinChoice("ni",0,2))
        PinyinDisambiguation.rewrite("64",0,result.first()) shouldBe "ming"
    }
    "umlaut spelling uses v while accents and case do not alter the numeric match" {
        val result=PinyinDisambiguation.choices("58",0,listOf("LǛ", "lǜ"))
        result shouldBe listOf(PinyinChoice("lv",0,2))
    }
    "unknown hint formats and mismatched codes never become guessed phonetic choices" {
        PinyinDisambiguation.choices("64426",0,listOf("nihao", "secret marker", "shì jiè")) shouldBe emptyList()
        PinyinDisambiguation.choices("ni'426",0,listOf("mí hào")) shouldBe emptyList()
    }
    "invalid input metadata and long input fail without truncation" {
        listOf("", "64426/", "64 426", "64A26", "6".repeat(129)).forEach {
            PinyinDisambiguation.choices(it,0,listOf("nǐ hǎo")) shouldBe emptyList()
        }
        PinyinDisambiguation.choices("64426",-1,listOf("nǐ hǎo")) shouldBe emptyList()
        PinyinDisambiguation.choices("64426",6,listOf("nǐ hǎo")) shouldBe emptyList()
    }
    "stale forged and already confirmed edit ranges are rejected" {
        PinyinDisambiguation.rewrite("74426",0,PinyinChoice("ni",0,2)) shouldBe null
        PinyinDisambiguation.rewrite("64426",0,PinyinChoice("ni",1,3)) shouldBe null
        PinyinDisambiguation.rewrite("64426",0,PinyinChoice("ni",0,5)) shouldBe null
        PinyinDisambiguation.rewrite("64426744543",5,PinyinChoice("ni",0,2)) shouldBe null
        PinyinDisambiguation.rewrite("64426",0,PinyinChoice("ni'hao",0,5)) shouldBe null
    }
    "diagnostic representation never includes the phonetic label" {
        PinyinChoice("private_marker",0,2).toString() shouldNotContain "private_marker"
    }
})
