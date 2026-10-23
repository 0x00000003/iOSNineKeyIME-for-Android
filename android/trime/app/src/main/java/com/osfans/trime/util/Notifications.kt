/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Project modification: platform notifications disabled in the offline fork.
 */
package com.osfans.trime.util

import android.app.PendingIntent
import com.osfans.trime.data.sync.SyncStats

/**
 * The offline frontend owns visible progress/error state. It never posts a
 * background notification or requests notification permission. These legacy
 * signatures remain for source compatibility until the old frontend is removed.
 * They do not report successful deployment; the real engine result drives UI.
 */
fun createNotificationChannel(id: String, name: String) = Unit

object DeployNotification {
    const val CHANNEL_ID = "rime-daemon"
    const val MESSAGE_ID = 2331
    fun ensureChannel() = Unit
    fun showProgress() = Unit
    fun showSuccess() = Unit
    fun showFailure(contentIntent: PendingIntent) = Unit
    fun cancel() = Unit
    fun notifyPartialCopyIfNeeded(stats: SyncStats, operation: String): SyncStats = stats
    fun showExternalSyncFallback() = Unit
}
