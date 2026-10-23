// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.privacy

import android.text.InputType
import android.view.inputmethod.EditorInfo

enum class DirectLayout { LATIN, NUMBER }
enum class PrivacyReason { PASSWORD, NO_PERSONALIZED_LEARNING, USER_PRIVATE, NORMAL }
data class EditorPolicy(val reason: PrivacyReason, val directLayout: DirectLayout) {
    val sensitive get() = reason != PrivacyReason.NORMAL
    val permitContextRead get() = !sensitive
    val permitLearning get() = !sensitive

    companion object {
        /** Only metadata enters this decision; no InputConnection/text access. */
        fun from(inputType: Int, imeOptions: Int, userPrivate: Boolean): EditorPolicy {
            val type = inputType and InputType.TYPE_MASK_CLASS
            val variation = inputType and InputType.TYPE_MASK_VARIATION
            val password = (type == InputType.TYPE_CLASS_TEXT && variation in setOf(
                InputType.TYPE_TEXT_VARIATION_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
            )) || (type == InputType.TYPE_CLASS_NUMBER && variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD)
            val reason = when {
                password -> PrivacyReason.PASSWORD
                imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING != 0 -> PrivacyReason.NO_PERSONALIZED_LEARNING
                userPrivate -> PrivacyReason.USER_PRIVATE
                else -> PrivacyReason.NORMAL
            }
            return EditorPolicy(reason, if (type == InputType.TYPE_CLASS_NUMBER || type == InputType.TYPE_CLASS_PHONE) DirectLayout.NUMBER else DirectLayout.LATIN)
        }
    }
}
