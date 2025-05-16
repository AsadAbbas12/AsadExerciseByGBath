package com.task.asadexercise.base

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.task.asadexercise.R
import com.task.asadexercise.ui.theme.MainActivity

class CarProgressNotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "CAR_PROGRESS_CHANNEL"
        private const val MAX_ACTIVE_NOTIFICATIONS = 2
        private val activeNotificationIds = mutableListOf<Int>()
        private const val PROGRESS_BAR_WIDTH = 440
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
            !activeNotificationIds.contains(notificationId)
        ) {
            val oldestId = activeNotificationIds.first()
            NotificationManagerCompat.from(context).cancel(oldestId)
            activeNotificationIds.remove(oldestId)
        }

        // Add this notification to active list if not already there
        if (!activeNotificationIds.contains(notificationId)) {
            activeNotificationIds.add(notificationId)
        }

        // Determine status text and color based on refined progress steps
        val (statusText, statusColor) = when {
            status == 0 -> Pair("Preparing", "#FFA500")
            status < totalStages * 1 / 5 -> Pair("On the Way", "#FFA500")
            status < totalStages * 2 / 5 -> Pair("Approaching", "#FFC107")
            status < totalStages * 3 / 5 -> Pair("Arriving Soon", "#8BC34A")
            status < totalStages -> Pair("Team Arrived", "#4CAF50")
            else -> Pair("Done", "#4CAF50")
        }


        // Estimated time remaining with 3-minute gaps
        val estimatedMinutesLeft = when {
            status == 0 -> 30
            status < totalStages / 5 -> 27
            status < totalStages * 2 / 5 -> 24
            status < totalStages * 3 / 5 -> 21
            status < totalStages * 4 / 5 -> 18
            status < totalStages -> 15
            else -> 0
        }

        // Set ETA if team has not arrived
        val etaText =
            if (status < totalStages) "ETA ${formatMinutes(estimatedMinutesLeft)}" else "Arrived"

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
        val collapsedView =
            RemoteViews(context.packageName, R.layout.custom_notification_collapse).apply {
                setImageViewBitmap(R.id.imageViewProgress, progressBitmap)
                setTextViewText(R.id.tvTitle, "Smart Response - $statusText")
                setTextColor(R.id.tvTitle, Color.parseColor(statusColor))
            }


        val expandedView =
            RemoteViews(context.packageName, R.layout.custom_notification_expand).apply {
                setImageViewBitmap(R.id.imageViewProgress, progressBitmap)
                setTextViewText(R.id.tvTitle, "Smart Response")
                setTextViewText(R.id.tvStatus, statusText)
                setTextColor(R.id.tvStatus, Color.parseColor(statusColor))
                setTextViewText(R.id.tvEstimate, etaText)

                if (status >= totalStages) {
                    setViewVisibility(R.id.btnComplete, View.VISIBLE)
                    setViewVisibility(R.id.btnCancel, View.GONE)

                    val completeIntent =
                        Intent(context, NotificationActionReceiver::class.java).apply {
                            action = "ACTION_COMPLETE"
                            putExtra("notificationId", notificationId)
                        }
                    val completePendingIntent = PendingIntent.getBroadcast(
                        context,
                        notificationId,
                        completeIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    setOnClickPendingIntent(R.id.btnComplete, completePendingIntent)
                } else {
                    setViewVisibility(R.id.btnComplete, View.GONE)
                    setViewVisibility(R.id.btnCancel, View.VISIBLE)
                    val completeIntent =
                        Intent(context, NotificationActionReceiver::class.java).apply {
                            action = "ACTION_COMPLETE"
                            putExtra("notificationId", notificationId)
                        }
                    val completePendingIntent = PendingIntent.getBroadcast(
                        context,
                        notificationId,
                        completeIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    setOnClickPendingIntent(R.id.btnCancel, completePendingIntent)
                }

            }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_dewa_pie_chart)
            .setContentTitle("Smart Response")
            .setContentText(statusText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setCustomContentView(collapsedView)
            .setCustomBigContentView(expandedView)
             builder.setOngoing(true) // Prevents user from swiping it away

        builder.setProgress(totalStages, status, false)

        // Notify the system
        NotificationManagerCompat.from(context).notify(notificationId, builder.build())

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

        // Define all segment colors
        val segmentColors = listOf(
            Color.parseColor("#FF8C00"),  // Brighter Orange
            Color.parseColor("#FFD700"),  // Brighter Amber (Gold)
            Color.parseColor("#AEEA00"),  // Brighter Light Green (Lime)
            Color.parseColor("#00C853")   // Brighter Green (Emerald)
        )


        // Determine current segment
        val currentSegment = when {
            currentProgress == 0 -> 0
            currentProgress < totalProgress / 3 -> 0
            currentProgress < totalProgress * 2 / 3 -> 1
            currentProgress < totalProgress -> 2
            else -> 3
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

        val progressWidth = PROGRESS_BAR_WIDTH * progressPercentage

        // Handle gradient creation differently based on segment
        if (currentSegment == 0) {
            // For first segment, just use solid color
            paint.color = segmentColors[0]
            paint.shader = null
        } else {
            // For other segments, create gradient with all colors up to current segment
            val gradientColors = segmentColors.take(currentSegment + 1).toIntArray()
            val gradientPositions = FloatArray(gradientColors.size).apply {
                for (i in indices) {
                    this[i] = i / (gradientColors.size - 1).toFloat()
                }
            }

            val gradient = LinearGradient(
                0f, 0f,
                progressWidth, 0f,
                gradientColors,
                gradientPositions,
                Shader.TileMode.CLAMP
            )
            paint.shader = gradient
        }

        canvas.drawRoundRect(
            RectF(0f, 100 / 2f, progressWidth, (100 / 2f + reducedHeight)),
            reducedHeight / 2f,
            reducedHeight / 2f,
            paint
        )

        // Reset shader for other elements
        paint.shader = null

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

    fun formatMinutes(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return when {
            hours > 0 && mins > 0 -> "${hours} hr ${mins} min"
            hours > 0 -> "${hours} hr"
            else -> "${mins} min"
        }
    }

}


class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val id = intent?.getIntExtra("notificationId", -1) ?: return
        if (intent.action == "ACTION_COMPLETE") {
            // Example: dismiss the notification or do something else
            NotificationManagerCompat.from(context!!).cancel(id)
            Toast.makeText(context, "Marked as Complete", Toast.LENGTH_SHORT).show()
        }
    }
}