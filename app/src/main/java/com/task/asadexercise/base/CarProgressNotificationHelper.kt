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

class CarProgressNotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "CAR_PROGRESS_CHANNEL"
        const val NOTIFICATION_ID = 1002
        private const val PROGRESS_BAR_WIDTH = 430
        private const val PROGRESS_BAR_HEIGHT = 30
        private const val CAR_ICON_SIZE = 30
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


        // Determine status text based on progress
        val (statusText, statusColor) = when {
            status == 0 -> Pair("Preparing", "#FFA500") // Orange
            status < totalStages / 3 -> Pair("Preparing", "#FFA500") // Orange
            status < totalStages * 2/3 -> Pair("On the Way", "#4CAF50") // Green
            status < totalStages -> Pair("Arriving Soon", "#4CAF50") // Green
            else -> Pair("Arrived", "#4CAF50") // Green
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
        val collapsedView = RemoteViews(context.packageName, R.layout.custom_notification)
        val expandedView = RemoteViews(context.packageName, R.layout.custom_notification_expand)

// Set your dynamic content
        collapsedView.setImageViewBitmap(R.id.imageViewProgress, progressBitmap)
        collapsedView.setTextViewText(R.id.textSmartResponse, "Smart Response Track : " + statusText) // if needed

        expandedView.setImageViewBitmap(R.id.imageViewProgress, progressBitmap)
        expandedView.setTextViewText(R.id.textSmartResponse, "Expanded Smart Response") // if needed

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Order Progress") // Optional, may be replaced by custom layout
            .setContentText("Status: $status/$totalStages") // Optional
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setCustomContentView(collapsedView)        // Collapsed layout
            .setCustomBigContentView(expandedView)      // Expanded layout

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())


    }

    private fun createProgressBarWithCar(currentProgress: Int, totalProgress: Int): Bitmap {
        val progressPercentage =
            (currentProgress.toFloat() / totalProgress.toFloat()).coerceIn(0f, 1f)
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

        // Draw track background
        paint.color = Color.LTGRAY

        val reducedHeight = PROGRESS_BAR_HEIGHT / 7f // Same reduction as before

        canvas.drawRoundRect(
            RectF(
                0f,
                100 / 2f,
                PROGRESS_BAR_WIDTH.toFloat(),
                100 / 2f + reducedHeight
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
                100 / 2f,
                progressWidth,
                (100 / 2f + reducedHeight)
            ),
            reducedHeight / 2f,
            reducedHeight / 2f,
            paint
        )

        // Draw car at current progress position
        val carIcon = BitmapFactory.decodeResource(context.resources, R.drawable.cea)
        val scaledCar = Bitmap.createScaledBitmap(carIcon, 100, 100, true)

        // Position car at progress point (centered vertically)
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
}