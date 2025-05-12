package com.task.asadexercise.base

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.task.asadexercise.R
import com.task.asadexercise.ui.theme.MainActivity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.content.ContextCompat

class CarProgressNotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "CAR_PROGRESS_CHANNEL"
        const val NOTIFICATION_ID = 1002
        private const val PROGRESS_BAR_WIDTH = 300
        private const val PROGRESS_BAR_HEIGHT = 30
        private const val CAR_ICON_SIZE = 40
        const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }

    fun showProgressNotification(status: Int, totalStages: Int = 10) {
        // Check notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Request permission if in an Activity
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

        createNotificationChannel()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Create progress bar with moving car
        val progressBitmap = createProgressBarWithCar(status, totalStages)

        // Resolve the BigPictureStyle ambiguity by explicitly setting null Bitmap
        val style = NotificationCompat.BigPictureStyle()
            .bigPicture(progressBitmap)
            .bigLargeIcon(null as Bitmap?) // Explicit cast to Bitmap?

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Order Progress")
            .setContentText("Status: $status/$totalStages")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setStyle(style)
            .setOnlyAlertOnce(true) // Only sound/vibrate on first show

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(NOTIFICATION_ID, builder.build())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createProgressBarWithCar(currentProgress: Int, totalProgress: Int): Bitmap {
        val progressPercentage = (currentProgress.toFloat() / totalProgress.toFloat()).coerceIn(0f, 1f)
        val totalWidth = PROGRESS_BAR_WIDTH + CAR_ICON_SIZE
        val bitmap = Bitmap.createBitmap(totalWidth, PROGRESS_BAR_HEIGHT + CAR_ICON_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        // Draw track background
        paint.color = Color.LTGRAY

        val reducedHeight = PROGRESS_BAR_HEIGHT / 7f // Same reduction as before

        canvas.drawRoundRect(
            RectF(
                0f,
                CAR_ICON_SIZE / 2f,
                PROGRESS_BAR_WIDTH.toFloat(),
                CAR_ICON_SIZE / 2f + reducedHeight
            ),
            reducedHeight / 2f,
            reducedHeight / 2f,
            paint
        )


        // Draw progress
        paint.color = ContextCompat.getColor(context, R.color.teal_200)

        val progressWidth = PROGRESS_BAR_WIDTH * progressPercentage

        canvas.drawRoundRect(
            RectF(
                0f,
                CAR_ICON_SIZE / 2f,
                progressWidth,
                (CAR_ICON_SIZE / 2f + reducedHeight)
            ),
            reducedHeight / 2f,
            reducedHeight / 2f,
            paint
        )

        // Draw car at current progress position
        val carIcon = BitmapFactory.decodeResource(context.resources, R.drawable.cea)
        val scaledCar = Bitmap.createScaledBitmap(carIcon, CAR_ICON_SIZE, CAR_ICON_SIZE, true)

        // Position car at progress point (centered vertically)
        val carX = progressWidth - (CAR_ICON_SIZE/2f)
        val carY = 0f

        canvas.drawBitmap(scaledCar, carX.coerceIn(0f, (PROGRESS_BAR_WIDTH - CAR_ICON_SIZE/2f)), carY, paint)

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
}