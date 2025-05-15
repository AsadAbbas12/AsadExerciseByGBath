package com.task.asadexercise.base

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.task.asadexercise.R
import com.task.asadexercise.ui.theme.MainActivity
import kotlin.random.Random

class CarProgressNotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "CAR_PROGRESS_CHANNEL"
        private const val MAX_ACTIVE_NOTIFICATIONS = 2
        private val activeNotificationIds = mutableListOf<Int>()
        private const val PROGRESS_BAR_WIDTH = 430
        private const val PROGRESS_BAR_HEIGHT = 30
        private const val CAR_ICON_SIZE = 30
        const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }

    fun showProgressNotification(status: Int, totalStages: Int = 10, notificationId: Int) {

        // Check notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (context is android.app.Activity) {
                    ActivityCompat.requestPermissions(
                        context,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                    )
                }
                return
            }
        }

        // If we have max notifications and this is a new one, remove the oldest
        if (activeNotificationIds.size >= MAX_ACTIVE_NOTIFICATIONS &&
            !activeNotificationIds.contains(notificationId)) {
            val oldestId = activeNotificationIds.first()
            NotificationManagerCompat.from(context).cancel(oldestId)
            activeNotificationIds.remove(oldestId)
        }

        // Add this notification to active list if not already there
        if (!activeNotificationIds.contains(notificationId)) {
            activeNotificationIds.add(notificationId)
        }

        // Determine status text based on progress
        val (statusText, statusColor) = when {
            status == 0 -> Pair("Preparing", "#FFA500")
            status < totalStages / 3 -> Pair("On the Way", "#FFA500")
            status < totalStages * 2 / 3 -> Pair("Approaching", "#4CAF50")
            status < totalStages -> Pair("Arriving Soon", "#4CAF50")
            else -> Pair("Arrived", "#4CAF50")
        }

        createNotificationChannel()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notificationId", notificationId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Create progress bar with moving car
        val progressBitmap = createProgressBarWithCar(status, totalStages)
        val collapsedView = RemoteViews(context.packageName, R.layout.custom_notification).apply {
            setImageViewBitmap(R.id.imageViewProgress, progressBitmap)
            setTextViewText(R.id.textSmartResponse, "Live Tracking: $statusText")
            setTextColor(R.id.textSmartResponse, Color.parseColor(statusColor))
        }

        val expandedView = RemoteViews(context.packageName, R.layout.custom_notification_expand).apply {
            setImageViewBitmap(R.id.imageViewProgress, progressBitmap)
            setTextViewText(R.id.textSmartResponse, "Service Progress Details")
            setTextColor(R.id.textSmartResponse, Color.parseColor(statusColor))
        }

        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.test)
            .setContentTitle("Service Progress")
            .setContentText(statusText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setCustomContentView(collapsedView)
            .setCustomBigContentView(expandedView)
            .setProgress(totalStages, status, false)
            .also {
                NotificationManagerCompat.from(context).notify(notificationId, it.build())
            }
    }

    fun cancelNotification(notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
        activeNotificationIds.remove(notificationId)
    }

    fun cancelAllNotifications() {
        NotificationManagerCompat.from(context).cancelAll()
        activeNotificationIds.clear()
    }

    private fun createProgressBarWithCar(currentProgress: Int, totalProgress: Int): Bitmap {
        val progressPercentage = (currentProgress.toFloat() / totalProgress.toFloat()).coerceIn(0f, 1f)
        val totalWidth = PROGRESS_BAR_WIDTH + CAR_ICON_SIZE
        val bitmap = Bitmap.createBitmap(
            totalWidth,
            PROGRESS_BAR_HEIGHT + CAR_ICON_SIZE,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        // Determine progress color based on current progress
        val progressColor = when {
            currentProgress == 0 -> Color.parseColor("#FFA500") // Orange for Preparing
            currentProgress < totalProgress / 3 -> Color.parseColor("#FFA500") // Orange for On the Way
            currentProgress < totalProgress * 2 / 3 -> Color.parseColor("#4CAF50") // Green for Approaching
            currentProgress < totalProgress -> Color.parseColor("#4CAF50") // Green for Arriving Soon
            else -> Color.parseColor("#4CAF50") // Green for Arrived
        }

        // Draw track background
        paint.color = Color.LTGRAY
        val reducedHeight = PROGRESS_BAR_HEIGHT / 7f

        canvas.drawRoundRect(
            RectF(0f, 100 / 2f, PROGRESS_BAR_WIDTH.toFloat(), 100 / 2f + reducedHeight),
            reducedHeight / 2f,
            reducedHeight / 2f,
            paint
        )

        // Draw progress with appropriate color
        paint.color = progressColor
        val progressWidth = PROGRESS_BAR_WIDTH * progressPercentage

        canvas.drawRoundRect(
            RectF(0f, 100 / 2f, progressWidth, (100 / 2f + reducedHeight)),
            reducedHeight / 2f,
            reducedHeight / 2f,
            paint
        )

        // Draw car at current progress position
        val carIcon = BitmapFactory.decodeResource(context.resources, R.drawable.cea)
        val scaledCar = Bitmap.createScaledBitmap(carIcon, 100, 100, true)
        val carX = progressWidth - (100 / 2f)
        val carY = 0f

        canvas.drawBitmap(
            scaledCar,
            carX.coerceIn(0f, (PROGRESS_BAR_WIDTH - 100 / 2f)),
            carY,
            paint
        )

        return bitmap
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Progress Tracking"
            val description = "Shows progress with moving car"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                this.description = description
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private val Int.absoluteValue: Int
        get() = if (this < 0) -this else this
}