package com.task.asadexercise

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class MediaActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "ACTION_PLAY" -> {
                // You might want to store a reference to your WebView or use EventBus
                // For simplicity, we'll just show a toast
                Toast.makeText(context, "Play command sent", Toast.LENGTH_SHORT).show()
            }
            "ACTION_PAUSE" -> {
                Toast.makeText(context, "Pause command sent", Toast.LENGTH_SHORT).show()
            }
            "ACTION_TOGGLE" -> {
                Toast.makeText(context, "Toggle command sent", Toast.LENGTH_SHORT).show()
            }
        }
    }
}