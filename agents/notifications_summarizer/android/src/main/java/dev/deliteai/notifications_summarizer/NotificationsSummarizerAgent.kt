/*
 * SPDX-FileCopyrightText: (C) 2025 DeliteAI Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.deliteai.notifications_summarizer

import dev.deliteai.NimbleNet
import dev.deliteai.datamodels.NimbleNetError
import dev.deliteai.datamodels.NimbleNetResult
import dev.deliteai.notifications_summarizer.dataModels.NotificationSummarizerConfig
import dev.deliteai.notifications_summarizer.dataModels.NotificationSummary
import dev.deliteai.notifications_summarizer.impl.Controller
import dev.deliteai.notifications_summarizer.impl.DependencyContainer
import dev.deliteai.notifications_summarizer.impl.common.Constants.TAG
import android.app.Application
import android.util.Log
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicBoolean

object NotificationsSummarizerAgent {
    private val isAgentInitialized = AtomicBoolean(false)
    private lateinit var controller: Controller

    suspend fun initialize(
        application: Application,
        config: NotificationSummarizerConfig
    ): NimbleNetResult<Unit> =
        runCatchingResult(initRequired = false) {
            if (!isAgentInitialized.get()) {
                val dependencyContainer =
                    DependencyContainer.getInstance(application, config)

                controller = dependencyContainer.getController()
                isAgentInitialized.set(true)
            }
        }

    suspend fun scheduleNotificationSummaryJob(timeInMillis: Long): NimbleNetResult<Unit> =
        runCatchingResult {
            controller.schedule(timeInMillis)
        }

    suspend fun getSummaryOfCurrentNotification(): NimbleNetResult<NotificationSummary> =
        runCatchingResult {
            controller.getSummaryForCurrentNotifications()
        }

    suspend fun getSummary(id: String): NimbleNetResult<NotificationSummary?> =
        runCatchingResult {
            controller.get(id)
        }

    suspend fun getSummary(date: LocalDate): NimbleNetResult<List<NotificationSummary>?> =
        runCatchingResult {
            controller.get(date)
        }

    suspend fun getSummary(
        startDate: LocalDate,
        endDate: LocalDate
    ): NimbleNetResult<List<NotificationSummary>?> =
        runCatchingResult {
            controller.get(startDate, endDate)
        }


    //helpers
    private suspend fun <T> runCatchingResult(
        initRequired: Boolean = true,
        action: suspend () -> T,
    ): NimbleNetResult<T> {

        if (initRequired) {
            if (!isAgentInitialized.get())
                throw IllegalStateException("Please initialize NotificationsSummarizerAgent first.")

            waitForNimbleNetReady()
        }

        return try {
            val result = action()
            NimbleNetResult(status = true, payload = result)
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
            NimbleNetResult(
                status = false,
                payload = null,
                error = NimbleNetError(123, e.message ?: "Something went wrong")
            )
        }
    }

    private suspend fun waitForNimbleNetReady() {
        while (true) {
            val res = NimbleNet.isReady()
            if (res.status) break
            Log.i(TAG, "NN is not ready. ${res.error?.message}")

            delay(1000)
        }
    }
}
