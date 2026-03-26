package com.savings.tracker.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.savings.tracker.domain.usecase.ApplyMonthlyFeeUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Duration

@HiltWorker
class MonthlyFeeWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val applyMonthlyFeeUseCase: ApplyMonthlyFeeUseCase
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            applyMonthlyFeeUseCase()
            scheduleNext(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "monthly_fee_worker"

        fun scheduleNext(context: Context) {
            val now = LocalDateTime.now()
            val firstOfNextMonth = LocalDate.of(now.year, now.month, 1)
                .plusMonths(1)
                .atTime(LocalTime.of(0, 1))
            val delay = Duration.between(now, firstOfNextMonth)

            val request = OneTimeWorkRequestBuilder<MonthlyFeeWorker>()
                .setInitialDelay(delay)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    request
                )
        }

        fun enqueueInitial(context: Context) {
            val request = OneTimeWorkRequestBuilder<MonthlyFeeWorker>()
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    WORK_NAME,
                    ExistingWorkPolicy.KEEP,
                    request
                )
        }
    }
}
