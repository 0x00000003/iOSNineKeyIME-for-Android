/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Offline fork: background/export sync is deliberately unavailable. Retain failure-only ABI.
 */
package com.osfans.trime.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class BackgroundSyncWork(context:Context,workerParams:WorkerParameters):CoroutineWorker(context,workerParams) {
    override suspend fun doWork():Result=Result.failure()
    companion object {
        const val supported=false
        // No scheduler, external storage, daemon, logs or success notification.
        fun start(context:Context) { }
        fun forceStart(context:Context) { }
    }
}
