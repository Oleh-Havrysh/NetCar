package com.hakito.netcar.work

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hakito.netcar.ControlPreferences
import com.hakito.netcar.R
import com.hakito.netcar.sender.CarSenderImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CheckCarEnabledWorker(private val context: Context, workerParameters: WorkerParameters) :
    CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val pingResult = CarSenderImpl(ControlPreferences(context)).ping()
        if (pingResult) {
            withContext(Dispatchers.Main) {
                showCarEnabledNotification()
            }
        }
        return Result.success()
    }

    private fun showCarEnabledNotification() {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(
            0,
            Notification.Builder(context)
                .setContentTitle("Car is enabled")
                .setContentText("Did your forget to turn off the car?")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build()
        )
    }
}