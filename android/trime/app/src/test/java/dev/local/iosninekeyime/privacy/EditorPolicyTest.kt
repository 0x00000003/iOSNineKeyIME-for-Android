// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.privacy

import android.text.InputType as T
import android.view.inputmethod.EditorInfo as I
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class EditorPolicyTest : StringSpec({
    "U05 every Android password variation forbids context and learning despite extra flags" {
        listOf(T.TYPE_TEXT_VARIATION_PASSWORD, T.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD, T.TYPE_TEXT_VARIATION_WEB_PASSWORD).forEach { variation ->
            val p = EditorPolicy.from(T.TYPE_CLASS_TEXT or variation or T.TYPE_TEXT_FLAG_MULTI_LINE or T.TYPE_TEXT_FLAG_CAP_SENTENCES, I.IME_ACTION_DONE, false)
            p.reason shouldBe PrivacyReason.PASSWORD
            p.permitContextRead shouldBe false
            p.permitLearning shouldBe false
            p.directLayout shouldBe DirectLayout.LATIN
        }
    }
    "U05 numeric password keeps numeric direct keys and denies learning" {
        val p = EditorPolicy.from(T.TYPE_CLASS_NUMBER or T.TYPE_NUMBER_VARIATION_PASSWORD or T.TYPE_NUMBER_FLAG_SIGNED, I.IME_ACTION_DONE, false)
        p.reason shouldBe PrivacyReason.PASSWORD
        p.directLayout shouldBe DirectLayout.NUMBER
        p.permitLearning shouldBe false
    }
    "U05 NO_PERSONALIZED_LEARNING applies to all editor classes" {
        listOf(T.TYPE_CLASS_TEXT, T.TYPE_CLASS_NUMBER, T.TYPE_CLASS_PHONE, T.TYPE_CLASS_DATETIME, T.TYPE_NULL).forEach { type ->
            val p = EditorPolicy.from(type, I.IME_ACTION_SEARCH or I.IME_FLAG_NO_PERSONALIZED_LEARNING or I.IME_FLAG_NO_EXTRACT_UI, false)
            p.reason shouldBe PrivacyReason.NO_PERSONALIZED_LEARNING
            p.permitLearning shouldBe false
            p.permitContextRead shouldBe false
        }
    }
    "U05 user privacy takes effect without password or editor flags" {
        EditorPolicy.from(T.TYPE_CLASS_TEXT, 0, true).reason shouldBe PrivacyReason.USER_PRIVATE
    }
    "U05 ordinary web edit text does not become password from overlapping class bits" {
        val p = EditorPolicy.from(T.TYPE_CLASS_TEXT or T.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT, I.IME_ACTION_SEND, false)
        p.reason shouldBe PrivacyReason.NORMAL
        p.permitLearning shouldBe true
    }
    "U05 text variation bits cannot classify a phone field as a password" {
        EditorPolicy.from(T.TYPE_CLASS_PHONE or T.TYPE_TEXT_VARIATION_PASSWORD, 0, false).reason shouldBe PrivacyReason.NORMAL
    }
})
