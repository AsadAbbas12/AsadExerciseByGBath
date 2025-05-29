package com.task.asadexercise

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat

class AudioDetectionService : Service() {

    companion object {
        const val CHANNEL_ID = "media_playback_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_PLAY = "action_play"
        const val ACTION_PAUSE = "action_pause"
        const val ACTION_STOP = "action_stop"
    }

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        mediaSession = MediaSessionCompat(this, "MyAudioSession").apply {
            isActive = true
        }

        notificationManager = getSystemService(NotificationManager::class.java)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                // TODO: Start actual audio playback here
                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                showNotification()
            }
            ACTION_PAUSE -> {
                // TODO: Pause actual audio playback here
                updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
                showNotification()
            }
            ACTION_STOP -> {
                stopForeground(true)
                mediaSession.isActive = false
                stopSelf()
            }
            else -> {
                startAudioDetection()
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Media Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startAudioDetection() {
        // Set metadata for the session
        val albumArt = BitmapFactory.decodeResource(resources, R.drawable.ic_dewa_pie_chart) // Replace with your album art drawable
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "Track Title")
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "Artist Name")
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
            .build()

        mediaSession.setMetadata(metadata)

        // Start with paused state
        updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)

        showNotification()

        mediaSession.setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                // TODO: Start your audio playback
                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                showNotification()
            }

            override fun onPause() {
                // TODO: Pause your audio playback
                updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
                showNotification()
            }

            override fun onStop() {
                stopForeground(true)
                mediaSession.isActive = false
                stopSelf()
            }
        })
    }

    private fun updatePlaybackState(state: Int) {
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_STOP
            )
            .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1.0f)
            .build()
        mediaSession.setPlaybackState(playbackState)
    }

    private fun showNotification() {
        // Intents for notification actions
        val playIntent = PendingIntent.getService(
            this, 0, Intent(this, AudioDetectionService::class.java).apply { action = ACTION_PLAY },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val pauseIntent = PendingIntent.getService(
            this, 0, Intent(this, AudioDetectionService::class.java).apply { action = ACTION_PAUSE },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            this, 0, Intent(this, AudioDetectionService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Choose actions depending on playback state
        val playbackState = mediaSession.controller.playbackState?.state ?: PlaybackStateCompat.STATE_PAUSED

        val playPauseAction = if (playbackState == PlaybackStateCompat.STATE_PLAYING) {
            NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_pause, "Pause", pauseIntent
            ).build()
        } else {
            NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_play, "Play", playIntent
            ).build()
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(mediaSession.controller.metadata?.getString(MediaMetadataCompat.METADATA_KEY_TITLE) ?: "Unknown Title")
            .setContentText(mediaSession.controller.metadata?.getString(MediaMetadataCompat.METADATA_KEY_ARTIST) ?: "Unknown Artist")
            .setLargeIcon(mediaSession.controller.metadata?.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART))
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(playPauseAction)
            .addAction(
                NotificationCompat.Action.Builder(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "Stop", stopIntent
                ).build()
            )
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1)
            )
            .setOnlyAlertOnce(true)
            .setOngoing(playbackState == PlaybackStateCompat.STATE_PLAYING)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }
}
