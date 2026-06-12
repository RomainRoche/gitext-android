package io.gitext.sdk.sdk

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that periodically fetches fresh translations in the background.
 *
 * Schedule it once at app startup (ideally after [Gitext.configure]):
 * ```kotlin
 * GitextSyncWorker.schedule(context, intervalHours = 12)
 * ```
 *
 * The worker requires an active network connection and retries automatically
 * on transient errors. It stops retrying on permanent auth failures.
 *
 * Requires `androidx.work:work-runtime-ktx` on the runtime classpath.
 */
class GitextSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result =
        runCatching { Gitext.prefetch() }
            .fold(
                onSuccess = { Result.success() },
                onFailure = { error ->
                    when (GitextError.from(error)) {
                        is GitextError.Unauthorized,
                        is GitextError.SubscriptionInactive -> Result.failure()
                        else -> Result.retry()
                    }
                }
            )

    companion object {
        private const val WORK_NAME = "gitext_sync"

        /**
         * Schedules (or keeps) a periodic sync with [WorkManager].
         * Safe to call multiple times; uses [ExistingPeriodicWorkPolicy.KEEP].
         *
         * @param context       Application context.
         * @param intervalHours Minimum interval between syncs. Defaults to 12 hours.
         */
        fun schedule(context: Context, intervalHours: Long = 12) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<GitextSyncWorker>(intervalHours, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /** Cancels a previously scheduled sync. */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
