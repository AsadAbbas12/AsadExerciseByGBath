package com.task.asadexercise

import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.session.PlaybackState
import android.net.Uri
import android.os.*
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media.session.MediaButtonReceiver

class WebViewActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var mediaNotificationManager: MediaNotificationManager
    private lateinit var audioManager: AudioManager
    private lateinit var focusRequest: AudioFocusRequest
    private val customTabLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // Handle result if needed
        }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        webView = WebView(this).apply {
            settings.apply {
                javaScriptEnabled = true
                mediaPlaybackRequiresUserGesture = false
                domStorageEnabled = true
            }
            webChromeClient = WebChromeClient()
            addJavascriptInterface(JSBridge(), "AndroidBridge")

            webViewClient = object : WebViewClient() {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
//                    audioManager.requestAudioFocus(focusRequest)
                  //  injectJavaScriptInterface(view)
                }
            }


            loadUrl(intent.getStringExtra(EXTRA_URL) ?: DEFAULT_URL)
        }

        setContentView(webView)
       // setupAudioFocus()

        val serviceIntent = Intent(this, AudioDetectionService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun injectJavaScriptInterface(view: WebView?) {
        val js = """
        (function() {
            function notifyAndroid(eventType, info) {
                if (window.AndroidBridge && typeof window.AndroidBridge.onMediaEvent === 'function') {
                    window.AndroidBridge.onMediaEvent(eventType + (info ? ': ' + info : ''));
                }
            }

            const iframes = document.getElementsByTagName('iframe');
            for (let i = 0; i < iframes.length; i++) {
                let src = iframes[i].getAttribute('src') || '';
                if (src.toLowerCase().includes('rss')) {
                    notifyAndroid('rss_iframe_detected', src);
                }
            }

            function attachAudioListeners(audio) {
                audio.addEventListener('play', () => notifyAndroid('play'));
                audio.addEventListener('pause', () => notifyAndroid('pause'));
                audio.addEventListener('ended', () => notifyAndroid('ended'));
            }

            const audios = document.getElementsByTagName('audio');
            for (let i = 0; i < audios.length; i++) {
                attachAudioListeners(audios[i]);
            }

            const observer = new MutationObserver(mutations => {
                mutations.forEach(mutation => {
                    mutation.addedNodes.forEach(node => {
                        if (node.tagName && node.tagName.toLowerCase() === 'audio') {
                            attachAudioListeners(node);
                        }
                    });
                });
            });

            observer.observe(document.body, { childList: true, subtree: true });

            document.addEventListener('click', function(event) {
                let targetTag = event.target.tagName;
                notifyAndroid('click', targetTag);
            }, true);

            for (let i = 0; i < iframes.length; i++) {
                try {
                    iframes[i].contentWindow.document.addEventListener('click', function(event) {
                        let targetTag = event.target.tagName;
                        notifyAndroid('iframe_click', targetTag);
                    }, true);
                } catch (e) {
                    notifyAndroid('iframe_click', 'cross-origin - no access');
                }
            }
            
            // --- ADD THIS: Listen for postMessage events from RSS.com iframe ---
            window.addEventListener('message', function(event) {
                if (event.origin === 'https://player.rss.com') {
                    // event.data is expected to be an object like { event: "play" } or similar
                    try {
                        let data = event.data;
                        if (typeof data === 'string') {
                            // Sometimes data might be stringified JSON
                            data = JSON.parse(data);
                        }
                        if (data && data.event) {
                            notifyAndroid('rss_player_event', data.event);
                        } else {
                            notifyAndroid('rss_player_message', JSON.stringify(data));
                        }
                    } catch (e) {
                        notifyAndroid('rss_player_message_error', e.message);
                    }
                }
            });
        })();
    """.trimIndent()

        view?.evaluateJavascript(js, null)
    }

    private fun openChromeTab(url: String) {
        val customTabsIntent = CustomTabsIntent.Builder()
            .setInitialActivityHeightPx(
                150.dpToPx(this),
                CustomTabsIntent.ACTIVITY_HEIGHT_ADJUSTABLE
            )
            .setShareState(CustomTabsIntent.SHARE_STATE_ON)
            .build()

        customTabLauncher.launch(customTabsIntent.intent.apply {
            data = Uri.parse(url)
        })
    }

    fun Int.dpToPx(context: Context): Int =
        (this * context.resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        mediaNotificationManager.stopSession()
        webView.apply {
            stopLoading()
            destroy()
        }
        super.onDestroy()
    }

    inner class JSBridge {
        @JavascriptInterface
        fun onMediaEvent(event: String) {
            Log.d("WebViewMedia", "Event received: $event")
            runOnUiThread {
                when {
                    event.startsWith("play") -> {
                        mediaNotificationManager.updatePlaybackState(true)
                    }

                    event.startsWith("pause") || event.startsWith("ended") -> {
                        mediaNotificationManager.updatePlaybackState(false)
                    }

                    event.startsWith("rss_iframe_detected") -> {
                        val iframeUrl = event.substringAfter(": ").trim()
                        if (iframeUrl.startsWith("https://player.rss.com/dewa-news/")) {
                            openChromeTab(iframeUrl)
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_URL = "extra_url"
        const val DEFAULT_URL =
            "https://www.dewa.gov.ae/en/about-us/media-publications/latest-news/2025/05/mohammed-bin-rashid-al-maktoum-solar-park-sets-a-world"

        fun newIntent(context: Context, url: String): Intent {
            return Intent(context, WebViewActivity::class.java).apply {
                putExtra(EXTRA_URL, url)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupAudioFocus() {
        mediaNotificationManager = MediaNotificationManager(this@WebViewActivity)
        audioManager = ContextCompat.getSystemService(this, AudioManager::class.java)!!

        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setOnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        mediaNotificationManager.startSession(true)
                    }

                    AudioManager.AUDIOFOCUS_LOSS,
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        mediaNotificationManager.updatePlaybackState(false)
                    }
                }
            }
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .build()
    }
}

class MediaNotificationManager(private val context: Context) {
    private val mediaSession = MediaSessionCompat(context, "WebViewPodcast").apply {
        setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                super.onPlay()
                updatePlaybackState(true)
                // Notify WebView to play if needed
            }

            override fun onPause() {
                super.onPause()
                updatePlaybackState(false)
                // Notify WebView to pause if needed
            }
        })
    }

    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        createNotificationChannel()
        mediaSession.isActive = true
    }

    fun startSession(isPlaying: Boolean) {
        updatePlaybackState(isPlaying)
        showNotification(isPlaying)
    }

    fun updatePlaybackState(isPlaying: Boolean) {
        val state =
            if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED

        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE
            )
            .setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1.0f)
            .build()

        mediaSession.setPlaybackState(playbackState)
        showNotification(isPlaying)
    }

    fun stopSession() {
        mediaSession.release()
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun showNotification(isPlaying: Boolean) {
        val playPauseAction = if (isPlaying) {
            NotificationCompat.Action(
                R.drawable.ic_launcher_background,
                "Pause",
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    context,
                    PlaybackStateCompat.ACTION_PAUSE
                )
            )
        } else {
            NotificationCompat.Action(
                R.drawable.ic_launcher_foreground,
                "Play",
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    context,
                    PlaybackStateCompat.ACTION_PLAY
                )
            )
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("DEWA podcast")
            .setContentText("Tap to manage playback")
            .setSmallIcon(R.drawable.ic_dewa_pie_chart)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    context.resources,
                    R.drawable.ic_dewa_pie_chart
                )
            )
            .addAction(playPauseAction)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0)
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setOngoing(isPlaying)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Podcast Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows podcast playback controls"
            }

            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "media_channel"
        const val NOTIFICATION_ID = 1001
    }
}

class BottomSheetWebViewFragment : BottomSheetDialogFragment() {
    companion object {
        private const val ARG_URL = "arg_url"

        fun newInstance(url: String): BottomSheetWebViewFragment {
            return BottomSheetWebViewFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_URL, url)
                }
            }
        }
    }

    private lateinit var webView: WebView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return WebView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        webView = view as WebView
        webView.settings.apply {
            javaScriptEnabled = true
            mediaPlaybackRequiresUserGesture = false
            domStorageEnabled = true
        }
        webView.webChromeClient = WebChromeClient()
        webView.loadUrl(requireArguments().getString(ARG_URL) ?: "")
    }

    override fun onStart() {
        super.onStart()
        dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let {
            it.layoutParams.height = (resources.displayMetrics.heightPixels * 0.4).toInt()
        }
    }

    override fun onDestroyView() {
        webView.apply {
            stopLoading()
            destroy()
        }
        super.onDestroyView()
    }
}