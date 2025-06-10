package com.task.asadexercise.ui.theme

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.task.asadexercise.base.CarProgressNotificationHelper
import com.task.asadexercise.base.NavigationGraph
import com.task.asadexercise.player.VideoCache
import com.task.asadexercise.screens.MainScreen
import com.task.asadexercise.viewmodel.SharedVM
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.random.Random

// provide the number of screens to be displayed in the navigation graph.
enum class Destinations { SPLASH, MAIN, DETAIL }

@AndroidEntryPoint
class MainActivity : NavigationGraph<Destinations>() {


    // Define the launcher at class level
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startNotificationSequence()
        } else {
            Toast.makeText(
                this,
                "Notification permission denied",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun startNotificationSequence() {

                notificationHelper = CarProgressNotificationHelper(this)

                lifecycleScope.launch {
                    // Generate a stable notification ID based on tag or random
                    val notificationId = "ts".hashCode().absoluteValue ?: Random.nextInt().absoluteValue

                    for (progress in 1..10) {
                        notificationHelper.showProgressNotification(
                            progress,
                            notificationId = notificationId
                        )
                        delay(2000) // 1 second delay
                    }
                }

                lifecycleScope.launch {
                    // Generate a stable notification ID based on tag or random
                    val notificationId = "tss".hashCode().absoluteValue ?: Random.nextInt().absoluteValue

                    for (progress in 1..5) {
                        notificationHelper.showProgressNotification(
                            progress,
                            notificationId = notificationId
                        )
                        delay(5000) // 1 second delay
                    }
                }

       // showWebViewDialog(this@MainActivity)
    }

    private lateinit var notificationHelper: CarProgressNotificationHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkNotificationPermission()
    }

    override val sharedViewModel: SharedVM by viewModels() // Using SharedViewModel to share data between screens and maintain a consistent state across the app. This allows the ViewModel to persist data and handle communication screens , keep in mind this is only communication purppose every screen should have different viewmodel
    override val startDestination = Destinations.MAIN

    // Defines the mapping of destinations to their corresponding Composable screens
    override val destinations: Map<Destinations, @Composable () -> Unit> =
        mapOf(
            Destinations.SPLASH to {}, // Splash Screen will come here },
            Destinations.MAIN to { MainScreen() },
            Destinations.DETAIL to { }// the detail screen will be implemented here. Generic navigation is already handled, so no need to pass anything in the Composable screen constructor to keep it clean."
        )

    override fun onDestroy() {
        super.onDestroy()
        VideoCache.releaseCache() // Release the cache
    }


    // Then modify your permission check:
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    startNotificationSequence()
                }

                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    //  showPermissionExplanationDialog()
                }

                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            startNotificationSequence()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun showWebViewDialog(context: Context) {
        val webView = WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                javaScriptCanOpenWindowsAutomatically = true
                mediaPlaybackRequiresUserGesture = false
                cacheMode = WebSettings.LOAD_DEFAULT
            }

            webChromeClient = object : WebChromeClient() {
                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    // Handle fullscreen video
                }

                override fun onHideCustomView() {
                    // Exit fullscreen video
                }

            }
            addJavascriptInterface(object {
                @JavascriptInterface
                fun onMediaEvent(type: String, state: String, src: String = "") {
                    showPlayPauseNotification(
                        context,
                        isPlaying = state == "play",
                        message = "$type $state\n$src"
                    )
                }

                @JavascriptInterface
                fun controlMedia(action: String) {
                    val js = when (action) {
                        "play" -> "document.querySelector('video,audio').play();"
                        "pause" -> "document.querySelector('video,audio').pause();"
                        "toggle" -> """
                        var media = document.querySelector('video,audio');
                        if(media.paused) media.play(); else media.pause();
                    """.trimIndent()

                        else -> ""
                    }
                    post { evaluateJavascript(js, null) }
                }
            }, "MediaController")

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    return false
                }


                override fun onLoadResource(view: WebView?, url: String?) {
                    super.onLoadResource(view, url)
                    // Your custom logic here
                    Log.d("WebView", "Resource loaded: $url")

                    // For example, block loading a specific resource
                    // (though blocking is better done in shouldInterceptRequest)
                }

                override fun onReceivedSslError(
                    view: WebView?,
                    handler: SslErrorHandler?,
                    error: SslError?
                ) {
                    handler?.proceed() // Ignore SSL errors (for testing only)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    injectMediaDetectionScript(view)
                }
            }

            loadUrl("https://webtest.dewa.gov.ae/en/about-us/media-publications/latest-news/2023/02/dewas-innovation-centre")
        }

        val dialog = AlertDialog.Builder(context)
            .setView(webView)
            .setPositiveButton("Close") { d, _ -> d.dismiss() }
            .setNeutralButton("Play All") { _, _ ->
                webView.evaluateJavascript(
                    """
                document.querySelectorAll('video,audio').forEach(media => {
                    media.play();
                });
            """.trimIndent(), null
                )
            }
            .setNegativeButton("Pause All") { _, _ ->
                webView.evaluateJavascript(
                    """
                document.querySelectorAll('video,audio').forEach(media => {
                    media.pause();
                });
            """.trimIndent(), null
                )
            }
            .create()

        dialog.setOnDismissListener {
            webView.loadUrl("about:blank")
            webView.destroy()
        }

        dialog.show()

        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            (context.resources.displayMetrics.heightPixels * 0.6).toInt()
        )
    }

    private fun injectMediaDetectionScript(webView: WebView?) {
        val js = """
        (function() {
            // Function to report media events
            const reportMediaEvent = (media, eventType) => {
                MediaController.onMediaEvent(
                    media.tagName.toLowerCase(),
                    eventType,
                    media.currentSrc || media.src
                );
            };

            // Setup media element listeners
            const setupMediaListeners = (media) => {
                ['play', 'pause', 'ended', 'volumechange', 'timeupdate'].forEach(event => {
                    media.addEventListener(event, () => reportMediaEvent(media, event));
                });
            };

            // Observe DOM changes for dynamically added media
            const observer = new MutationObserver(mutations => {
                mutations.forEach(mutation => {
                    mutation.addedNodes.forEach(node => {
                        if (node.nodeType === 1) { // Element node
                            if (node.tagName === 'VIDEO' || node.tagName === 'AUDIO') {
                                setupMediaListeners(node);
                            }
                            node.querySelectorAll('video, audio').forEach(setupMediaListeners);
                        }
                    });
                });
            });

            // Start observing the document
            observer.observe(document, {
                childList: true,
                subtree: true
            });

            // Setup existing media elements
            document.querySelectorAll('video, audio').forEach(setupMediaListeners);

            // Setup iframe media detection
            const setupIframeMedia = (iframe) => {
                try {
                    const iframeDoc = iframe.contentDocument || iframe.contentWindow.document;
                    iframeDoc.querySelectorAll('video, audio').forEach(setupMediaListeners);
                    
                    // Observe iframe DOM changes
                    new MutationObserver(mutations => {
                        mutations.forEach(mutation => {
                            mutation.addedNodes.forEach(node => {
                                if (node.nodeType === 1) {
                                    if (node.tagName === 'VIDEO' || node.tagName === 'AUDIO') {
                                        setupMediaListeners(node);
                                    }
                                    node.querySelectorAll('video, audio').forEach(setupMediaListeners);
                                }
                            });
                        });
                    }).observe(iframeDoc, {
                        childList: true,
                        subtree: true
                    });
                } catch (e) {
                    // Cross-origin iframe
                    iframe.addEventListener('load', () => {
                        MediaController.onMediaEvent('iframe', 'loaded', iframe.src);
                    });
                }
            };

            // Setup existing iframes
            document.querySelectorAll('iframe').forEach(setupIframeMedia);
        })();
    """.trimIndent()

        webView?.evaluateJavascript(js, null)
    }

    fun showPlayPauseNotification(
        context: Context,
        isPlaying: Boolean,
        message: String
    ) {
        val channelId = "media_channel"
        val channelName = "Media Playback"
        val notificationId = 101

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for media playback status"
                setSound(null, null) // Disable sound for these notifications
            }
            manager.createNotificationChannel(channel)
        }

        // Create broadcast intents for media control
        val playIntent = Intent(context, MediaActionReceiver::class.java).apply {
            action = "ACTION_PLAY"
        }
        val pauseIntent = Intent(context, MediaActionReceiver::class.java).apply {
            action = "ACTION_PAUSE"
        }
        val toggleIntent = Intent(context, MediaActionReceiver::class.java).apply {
            action = "ACTION_TOGGLE"
        }

        val playPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            playIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pausePendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val togglePendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Media Playback")
            .setContentText(message)
            .setSmallIcon(if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_media_play,
                "Play",
                playPendingIntent
            )
            .addAction(
                android.R.drawable.ic_media_pause,
                "Pause",
                pausePendingIntent
            )
            .addAction(
                android.R.drawable.ic_media_previous,
                "Toggle",
                togglePendingIntent
            )

        manager.notify(notificationId, builder.build())
    }

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
}
