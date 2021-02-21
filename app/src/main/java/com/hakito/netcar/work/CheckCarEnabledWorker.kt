package com.hakito.netcar.work

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.media.RingtoneManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hakito.netcar.R
import com.hakito.netcar.sender.CarSender
import com.hakito.netcar.wifi.WifiHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.context.GlobalContext.get

class CheckCarEnabledWorker(private val context: Context, workerParameters: WorkerParameters) :
    CoroutineWorker(context, workerParameters) {

    private val carSender: CarSender by get().koin.inject()

    override suspend fun doWork(): Result {
        val pingResult = carSender.ping()
        //val carWifiEnabled = WifiHelper(context).getWifiNetworks().any { it.contains("car", true) }
        return if (pingResult /*|| carWifiEnabled*/) {
            withContext(Dispatchers.Main) {
                showCarEnabledNotification()
            }
            Result.retry()
        } else {
            cancelNotification()
            Result.success()
        }
    }

    private fun showCarEnabledNotification() {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(
            NOTIFICATION_ID,
            Notification.Builder(context)
                .setContentTitle("Car is enabled")
                .setContentText("Did your forget to turn off the car?")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .build()
        )
    }

    private fun cancelNotification() {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

    companion object{

        private const val NOTIFICATION_ID = 0
    }
}