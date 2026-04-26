package com.example.myweather.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.myweather.R
import com.example.myweather.data.AppDatabase
import java.util.Calendar
import java.util.concurrent.TimeUnit

class WeatherNotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val mainCity = database.cityDao().getMainCitySnapshot() ?: return Result.success()

        sendNotification(applicationContext, mainCity.name, mainCity.temperature, mainCity.weatherState)

        if (!tags.contains(MANUAL_TAG)) {
            scheduleNext(applicationContext)
        }

        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "weather_notifications"
        const val NOTIFICATION_ID = 1001
        private const val WORK_NAME = "WeatherNotificationWork"
        private const val MANUAL_TAG = "MANUAL_UPDATE"

        fun sendNotification(context: Context, cityNameRaw: String, temp: Double, stateRaw: String) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                notificationManager.createNotificationChannel(channel)
            }

            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val messageRes = when {
                hour < 12 -> R.string.notification_good_morning
                hour < 16 -> R.string.notification_good_day
                hour < 20 -> R.string.notification_good_evening
                else -> R.string.notification_good_night
            }

            // Localize city name
            val cityResId = context.resources.getIdentifier(
                "city_${cityNameRaw.lowercase()}",
                "string",
                context.packageName
            )
            val cityName = if (cityResId != 0) context.getString(cityResId) else cityNameRaw

            // Localize weather state
            val stateKey = stateRaw.lowercase().replace(" ", "_")
            val stateResId = context.resources.getIdentifier(
                "weather_$stateKey",
                "string",
                context.packageName
            )
            val weatherState = if (stateResId != 0) context.getString(stateResId) else stateRaw

            val message = context.getString(messageRes, cityName, temp, weatherState)

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.myweather_logo)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
        }

        fun triggerNow(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<WeatherNotificationWorker>()
                .addTag(MANUAL_TAG)
                .build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }

        fun scheduleNext(context: Context) {
            val targetHours = listOf(10, 14, 18, 22)
            val now = Calendar.getInstance()
            val currentHour = now.get(Calendar.HOUR_OF_DAY)
            val currentMinute = now.get(Calendar.MINUTE)

            var nextHour = targetHours.firstOrNull { it > currentHour || (it == currentHour && currentMinute < 1) }
            val delayInMinutes: Long

            if (nextHour == null) {
                nextHour = 10
                val nextRun = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, nextHour)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }
                delayInMinutes = (nextRun.timeInMillis - now.timeInMillis) / (1000 * 60)
            } else {
                val nextRun = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, nextHour)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }
                delayInMinutes = (nextRun.timeInMillis - now.timeInMillis) / (1000 * 60)
            }

            val workRequest = OneTimeWorkRequestBuilder<WeatherNotificationWorker>()
                .setInitialDelay(delayInMinutes, TimeUnit.MINUTES)
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }
    }
}