package com.hakito.netcar.work

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class CarEnabledChecker(context: Context) {

    private val workManager = WorkManager.getInstance(context)

    fun onAppStart() {
        workManager.cancelUniqueWork(CHECK_CAR_ENABLED_WORK_NAME)
    }

    fun onAppStop() {
        workManager.beginUniqueWork(
            CHECK_CAR_ENABLED_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequest.Builder(CheckCarEnabledWorker::class.java)
                .setInitialDelay(15, TimeUnit.SECONDS)
                .build()
        ).enqueue()
    }

    companion object {

        private val CHECK_CAR_ENABLED_WORK_NAME = "CHECK_CAR_ENABLED"
    }
}