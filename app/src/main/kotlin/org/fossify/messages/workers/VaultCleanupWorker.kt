package org.fossify.messages.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.fossify.messages.helpers.VaultCleanupHelper
import org.fossify.messages.helpers.VaultConfig

class VaultCleanupWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Update last execution time
            VaultConfig.getInstance(appContext).lastSweeperExecution = System.currentTimeMillis()

            // Perform the unified cleanup
            VaultCleanupHelper.runCleanup(appContext)

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
