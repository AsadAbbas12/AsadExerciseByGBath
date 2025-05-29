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
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.task.asadexercise.R
import com.task.asadexercise.base.CarProgressNotificationHelper
import com.task.asadexercise.base.NavigationGraph
import com.task.asadexercise.player.VideoCache
import com.task.asadexercise.screens.MainScreen
import com.task.asadexercise.viewmodel.SharedVM
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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
        /*
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

                    for (progress in 1..10) {
                        notificationHelper.showProgressNotification(
                            progress,
                            notificationId = notificationId
                        )
                        delay(5000) // 1 second delay
                    }
                }
        */
        showWebViewDialog(this@MainActivity)
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

class DragDropListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PinImageAndTextToLazyColumnItem()
                }
            }
        }
    }
}


@Composable
fun PinImageAndTextToLazyColumnItem() {
    val imageBitmap = ImageBitmap.imageResource(id = android.R.drawable.ic_menu_camera)
    val items = remember { List(50) { "Item #$it" } }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Image drag state
    var pinnedImageIndex by remember { mutableStateOf(0) }
    var imageOffsetWithinItem by remember { mutableStateOf(Offset(20f, 20f)) }
    val imageSizeDp = 80.dp
    val imageSizePx = with(LocalDensity.current) { imageSizeDp.toPx() }
    val imageOffset = remember { androidx.compose.animation.core.Animatable(Offset.Zero, Offset.VectorConverter) }
    var isDraggingImage by remember { mutableStateOf(false) }
    var dragOffsetFromImageTopLeft by remember { mutableStateOf(Offset.Zero) }

    // Text drag state
    var pinnedTextIndex by remember { mutableStateOf(0) }
    var textOffsetWithinItem by remember { mutableStateOf(Offset(120f, 20f)) }
    var textContent by remember { mutableStateOf("Drag me!") }
    val textSizeDp = 150.dp
    val textHeightDp = 80.dp
    val textSizePx = with(LocalDensity.current) { textSizeDp.toPx() }
    val textOffset = remember { androidx.compose.animation.core.Animatable(Offset.Zero, Offset.VectorConverter) }
    var isDraggingText by remember { mutableStateOf(false) }
    var dragOffsetFromTextTopLeft by remember { mutableStateOf(Offset.Zero) }

    val boxSize = remember { mutableStateOf(IntSize.Zero) }

    // Helper to clamp offsets inside the Box container horizontally
    fun clampHorizontalOffset(offset: Offset, widthPx: Float): Offset {
        val maxX = boxSize.value.width - widthPx
        return Offset(x = offset.x.coerceIn(0f, maxX.coerceAtLeast(0f)), y = offset.y)
    }

    // Helpers to find visible item info for pinned indices
    val pinnedImageItemInfo by remember {
        derivedStateOf { lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == pinnedImageIndex } }
    }
    val pinnedTextItemInfo by remember {
        derivedStateOf { lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == pinnedTextIndex } }
    }

    // Compute actual offsets depending on drag state or pinned item position
    val actualImageOffset = if (isDraggingImage) {
        clampHorizontalOffset(imageOffset.value, imageSizePx)
    } else {
        pinnedImageItemInfo?.let {
            clampHorizontalOffset(
                Offset(
                    x = imageOffsetWithinItem.x,
                    y = it.offset.toFloat() + imageOffsetWithinItem.y
                ), imageSizePx
            )
        } ?: Offset.Zero
    }

    val actualTextOffset = if (isDraggingText) {
        clampHorizontalOffset(textOffset.value, textSizePx)
    } else {
        pinnedTextItemInfo?.let {
            clampHorizontalOffset(
                Offset(
                    x = textOffsetWithinItem.x,
                    y = it.offset.toFloat() + textOffsetWithinItem.y
                ), textSizePx
            )
        } ?: Offset.Zero
    }

    // Update animated offsets when pinning changes or drag ends
    LaunchedEffect(!isDraggingImage, pinnedImageItemInfo, imageOffsetWithinItem, boxSize.value) {
        if (!isDraggingImage && pinnedImageItemInfo != null && boxSize.value.width > 0) {
            val target = clampHorizontalOffset(
                Offset(
                    imageOffsetWithinItem.x,
                    pinnedImageItemInfo!!.offset.toFloat() + imageOffsetWithinItem.y
                ), imageSizePx
            )
            imageOffset.snapTo(target)
        }
    }
    LaunchedEffect(!isDraggingText, pinnedTextItemInfo, textOffsetWithinItem, boxSize.value) {
        if (!isDraggingText && pinnedTextItemInfo != null && boxSize.value.width > 0) {
            val target = clampHorizontalOffset(
                Offset(
                    textOffsetWithinItem.x,
                    pinnedTextItemInfo!!.offset.toFloat() + textOffsetWithinItem.y
                ), textSizePx
            )
            textOffset.snapTo(target)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { boxSize.value = it }
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(items) { index, item ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(if (index % 2 == 0) Color.LightGray else Color.White)
                        .padding(16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(text = item, fontSize = 20.sp)
                }
            }
        }

        // IMAGE composable - draggable and pinned
        if (isDraggingImage || pinnedImageItemInfo != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                modifier = Modifier
                    .size(imageSizeDp)
                    .background(Color.Red)
                    .zIndex(1f)
                    .graphicsLayer {
                        translationX = actualImageOffset.x
                        translationY = actualImageOffset.y
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { tapPos ->
                                // On tap, pin to item under tap
                                lazyListState.layoutInfo.visibleItemsInfo.firstOrNull {
                                    val top = it.offset
                                    val bottom = top + it.size
                                    tapPos.y in top.toFloat()..bottom.toFloat()
                                }?.let { tappedItem ->
                                    isDraggingImage = false
                                    pinnedImageIndex = tappedItem.index
                                    imageOffsetWithinItem = clampHorizontalOffset(
                                        Offset(tapPos.x, tapPos.y - tappedItem.offset), imageSizePx
                                    )
                                    coroutineScope.launch {
                                        imageOffset.snapTo(clampHorizontalOffset(tapPos, imageSizePx))
                                    }
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { touchPoint ->
                                isDraggingImage = true
                                dragOffsetFromImageTopLeft = touchPoint - imageOffset.value
                                coroutineScope.launch {
                                    imageOffset.snapTo(clampHorizontalOffset(touchPoint - dragOffsetFromImageTopLeft, imageSizePx))
                                }
                                // Update pinned index on drag start
                                lazyListState.layoutInfo.visibleItemsInfo.firstOrNull {
                                    val top = it.offset
                                    val bottom = top + it.size
                                    touchPoint.y in top.toFloat()..bottom.toFloat()
                                }?.let {
                                    pinnedImageIndex = it.index
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                coroutineScope.launch {
                                    val newOffset = imageOffset.value + dragAmount
                                    imageOffset.snapTo(clampHorizontalOffset(newOffset, imageSizePx))
                                }
                                // Optionally: add auto scroll logic here if needed
                            },
                            onDragEnd = {
                                isDraggingImage = false
                                // Save pinned offset relative to item top after drag ends
                                pinnedImageItemInfo?.let {
                                    val offsetInItem = Offset(
                                        x = imageOffset.value.x,
                                        y = imageOffset.value.y - it.offset
                                    )
                                    imageOffsetWithinItem = clampHorizontalOffset(offsetInItem, imageSizePx)
                                }
                            },
                            onDragCancel = {
                                isDraggingImage = false
                            }
                        )
                    }
            )
        }

        // TEXT composable - draggable and pinned
        if (isDraggingText || pinnedTextItemInfo != null) {
            Box(
                modifier = Modifier
                    .width(textSizeDp)
                    .height(textHeightDp)
                    .background(Color.Blue.copy(alpha = 0.7f))
                    .zIndex(1f)
                    .graphicsLayer {
                        translationX = actualTextOffset.x
                        translationY = actualTextOffset.y
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { tapPos ->
                                lazyListState.layoutInfo.visibleItemsInfo.firstOrNull {
                                    val top = it.offset
                                    val bottom = top + it.size
                                    tapPos.y in top.toFloat()..bottom.toFloat()
                                }?.let { tappedItem ->
                                    isDraggingText = false
                                    pinnedTextIndex = tappedItem.index
                                    textOffsetWithinItem = clampHorizontalOffset(
                                        Offset(tapPos.x, tapPos.y - tappedItem.offset), textSizePx
                                    )
                                    coroutineScope.launch {
                                        textOffset.snapTo(clampHorizontalOffset(tapPos, textSizePx))
                                    }
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { touchPoint ->
                                isDraggingText = true
                                dragOffsetFromTextTopLeft = touchPoint - textOffset.value
                                coroutineScope.launch {
                                    textOffset.snapTo(clampHorizontalOffset(touchPoint - dragOffsetFromTextTopLeft, textSizePx))
                                }
                                lazyListState.layoutInfo.visibleItemsInfo.firstOrNull {
                                    val top = it.offset
                                    val bottom = top + it.size
                                    touchPoint.y in top.toFloat()..bottom.toFloat()
                                }?.let {
                                    pinnedTextIndex = it.index
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                coroutineScope.launch {
                                    val newOffset = textOffset.value + dragAmount
                                    textOffset.snapTo(clampHorizontalOffset(newOffset, textSizePx))
                                }
                                // Optionally: add auto scroll logic here if needed
                            },
                            onDragEnd = {
                                isDraggingText = false
                                pinnedTextItemInfo?.let {
                                    val offsetInItem = Offset(
                                        x = textOffset.value.x,
                                        y = textOffset.value.y - it.offset
                                    )
                                    textOffsetWithinItem = clampHorizontalOffset(offsetInItem, textSizePx)
                                }
                            },
                            onDragCancel = {
                                isDraggingText = false
                            }
                        )
                    }
            ) {
                Text(
                    text = textContent,
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        // Debug info showing pinned indices and offsets
        Text(
            text = "Image: Index $pinnedImageIndex (${actualImageOffset.x.toInt()}, ${actualImageOffset.y.toInt()})\n" +
                    "Text: Index $pinnedTextIndex (${actualTextOffset.x.toInt()}, ${actualTextOffset.y.toInt()})",
            color = Color.Black,
            fontSize = 16.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .background(Color.White.copy(alpha = 0.7f))
                .padding(8.dp)
        )
    }
}

private fun CoroutineScope.autoScrollWhenDragging(
    lazyListState: LazyListState,
    currentY: Float,
    itemCount: Int
) {
    val threshold = 100
    val viewportHeight = lazyListState.layoutInfo.viewportEndOffset
    val canScrollUp = lazyListState.firstVisibleItemIndex > 0 ||
            lazyListState.firstVisibleItemScrollOffset > 0
    val canScrollDown = (lazyListState.layoutInfo.visibleItemsInfo
        .lastOrNull()?.index ?: 0) < itemCount - 1

    launch {
        when {
            currentY < threshold && canScrollUp -> {
                lazyListState.scrollBy(-35f)
            }
            currentY > viewportHeight - threshold && canScrollDown -> {
                lazyListState.scrollBy(35f)
            }
        }
    }
}

private fun handleDragEnd(
    lazyListState: LazyListState,
    currentOffset: Offset,
    sizePx: Float,
    onPinned: (Int, Offset) -> Unit,
    animateTo: (Offset) -> Unit
) {
    val dropTarget =
        lazyListState.layoutInfo.visibleItemsInfo.firstOrNull {
            val top = it.offset
            val bottom = top + it.size
            currentOffset.y in top.toFloat()..bottom.toFloat()
        }

    if (dropTarget != null) {
        val index = dropTarget.index
        val offsetWithinItem = Offset(
            x = currentOffset.x,
            y = currentOffset.y - dropTarget.offset
        )
        onPinned(index, offsetWithinItem)

        val targetOffset = Offset(
            x = offsetWithinItem.x,
            y = dropTarget.offset.toFloat() + offsetWithinItem.y
        )
        animateTo(targetOffset)
    }
}

/*
@Composable
fun PinImageToLazyColumnItem() {
    val imageBitmap = ImageBitmap.imageResource(id = android.R.drawable.ic_menu_camera)
    val items = remember { List(50) { "Item #$it" } }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var pinnedItemIndex by remember { mutableStateOf(0) }
    var offsetWithinItem by remember { mutableStateOf(Offset(20f, 20f)) }

    val imageSizePx = with(LocalDensity.current) { 80.dp.toPx() }

    val imageOffset = remember {
        androidx.compose.animation.core.Animatable(Offset.Zero, Offset.VectorConverter)
    }

    var isDragging by remember { mutableStateOf(false) }
    var dragOffsetFromImageTopLeft by remember { mutableStateOf(Offset.Zero) }

    val scrollOffsetTrigger = remember { mutableStateOf(0) }
    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemScrollOffset }
            .collect {
                scrollOffsetTrigger.value = it
            }
    }

    val pinnedItemInfo by remember {
        derivedStateOf {
            pinnedItemIndex.let { index ->
                lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(items) { index, item ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(if (index % 2 == 0) Color.LightGray else Color.White)
                        .padding(16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(text = item, fontSize = 20.sp)
                }
            }
        }

        val boxSize = remember { mutableStateOf(IntSize.Zero) }
        Box(modifier = Modifier
            .matchParentSize()
            .onSizeChanged { boxSize.value = it }
        )

        fun clampOffsetOnDrag(offset: Offset): Offset {
            val maxX = boxSize.value.width - imageSizePx
            return Offset(
                x = offset.x.coerceIn(0f, maxX.coerceAtLeast(0f)),
                y = offset.y
            )
        }

        fun clampOffsetPinned(offset: Offset): Offset {
            val maxX = boxSize.value.width - imageSizePx
            return Offset(
                x = offset.x.coerceIn(0f, maxX.coerceAtLeast(0f)),
                y = offset.y
            )
        }

        val showImage = isDragging || (pinnedItemInfo != null)

        val actualImageOffset = if (isDragging) {
            clampOffsetOnDrag(imageOffset.value)
        } else {
            pinnedItemInfo?.let {
                clampOffsetPinned(
                    Offset(
                        x = offsetWithinItem.x,
                        y = it.offset.toFloat() + offsetWithinItem.y
                    )
                )
            } ?: Offset.Zero
        }

        LaunchedEffect(showImage, pinnedItemInfo, offsetWithinItem, isDragging, boxSize.value) {
            if (!isDragging && pinnedItemInfo != null && boxSize.value.width > 0 && boxSize.value.height > 0) {
                val target = clampOffsetPinned(
                    Offset(
                        x = offsetWithinItem.x,
                        y = pinnedItemInfo!!.offset.toFloat() + offsetWithinItem.y
                    )
                )
                imageOffset.snapTo(target)
            }
        }

        if (showImage) {
            // The debug text showing index and offset
            Text(
                text = "Index: $pinnedItemIndex\nX: ${actualImageOffset.x.toInt()}, Y: ${actualImageOffset.y.toInt()}",
                color = Color.Black,
                fontSize = 16.sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
                    .background(Color.White.copy(alpha = 0.7f))
                    .padding(8.dp)
            )

            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .background(Color.Red)
                    .zIndex(1f)
                    .graphicsLayer {
                        translationX = actualImageOffset.x
                        translationY = actualImageOffset.y
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { tapPosition ->
                                val tappedItem =
                                    lazyListState.layoutInfo.visibleItemsInfo.firstOrNull {
                                        val top = it.offset
                                        val bottom = top + it.size
                                        tapPosition.y in top.toFloat()..bottom.toFloat()
                                    }
                                if (tappedItem != null) {
                                    isDragging = false
                                    pinnedItemIndex = tappedItem.index
                                    offsetWithinItem = clampOffsetPinned(
                                        Offset(
                                            tapPosition.x,
                                            tapPosition.y - tappedItem.offset
                                        )
                                    )
                                    coroutineScope.launch {
                                        imageOffset.snapTo(clampOffsetPinned(tapPosition))
                                    }
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { touchPoint ->
                                isDragging = true
                                dragOffsetFromImageTopLeft = touchPoint - imageOffset.value

                                coroutineScope.launch {
                                    imageOffset.snapTo(clampOffsetOnDrag(touchPoint - dragOffsetFromImageTopLeft))
                                }

                                val dropTarget =
                                    lazyListState.layoutInfo.visibleItemsInfo.firstOrNull {
                                        val top = it.offset
                                        val bottom = top + it.size
                                        touchPoint.y in top.toFloat()..bottom.toFloat()
                                    }
                                pinnedItemIndex = dropTarget?.index ?: 0
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                coroutineScope.launch {
                                    val newOffset = imageOffset.value + dragAmount
                                    imageOffset.snapTo(clampOffsetOnDrag(newOffset))
                                }

                                val threshold = 100
                                val viewportHeight = lazyListState.layoutInfo.viewportEndOffset
                                val canScrollUp = lazyListState.firstVisibleItemIndex > 0 ||
                                        lazyListState.firstVisibleItemScrollOffset > 0
                                val canScrollDown = (lazyListState.layoutInfo.visibleItemsInfo
                                    .lastOrNull()?.index ?: 0) < items.lastIndex

                                coroutineScope.launch {
                                    when {
                                        imageOffset.value.y < threshold && canScrollUp -> {
                                            lazyListState.scrollBy(-35f)
                                        }

                                        imageOffset.value.y > viewportHeight - threshold && canScrollDown -> {
                                            lazyListState.scrollBy(35f)
                                        }
                                    }
                                }
                            },
                            onDragEnd = {
                                isDragging = false
                                val dropTarget =
                                    lazyListState.layoutInfo.visibleItemsInfo.firstOrNull {
                                        val top = it.offset
                                        val bottom = top + it.size
                                        imageOffset.value.y in top.toFloat()..bottom.toFloat()
                                    }

                                if (dropTarget != null) {
                                    pinnedItemIndex = dropTarget.index
                                    offsetWithinItem = clampOffsetPinned(
                                        Offset(
                                            x = imageOffset.value.x,
                                            y = imageOffset.value.y - dropTarget.offset
                                        )
                                    )
                                    val targetOffset = Offset(
                                        x = offsetWithinItem.x,
                                        y = dropTarget.offset.toFloat() + offsetWithinItem.y
                                    )
                                    coroutineScope.launch {
                                        imageOffset.animateTo(
                                            clampOffsetPinned(targetOffset),
                                            animationSpec = tween(
                                                durationMillis = 300,
                                                easing = FastOutSlowInEasing
                                            )
                                        )
                                    }
                                }
                            },
                            onDragCancel = {
                                isDragging = false
                            }
                        )
                    }
            )
        }
    }
}

/*

// First Priority Code
/*
@Composable
fun PinImageToLazyColumnItem() {
    val imageBitmap = ImageBitmap.imageResource(id = android.R.drawable.ic_menu_camera)
    val items = remember { List(50) { "Item #$it" } }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var pinnedItemIndex by remember { mutableStateOf(0) }
    var offsetWithinItem by remember { mutableStateOf(Offset(20f, 20f)) }

    val imageSizePx = with(LocalDensity.current) { 80.dp.toPx() }

    val imageOffset = remember {
        androidx.compose.animation.core.Animatable(Offset.Zero, Offset.VectorConverter)
    }

    var isDragging by remember { mutableStateOf(false) }
    var dragOffsetFromImageTopLeft by remember { mutableStateOf(Offset.Zero) }

    val scrollOffsetTrigger = remember { mutableStateOf(0) }
    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemScrollOffset }
            .collect {
                scrollOffsetTrigger.value = it
            }
    }

    val pinnedItemInfo by remember {
        derivedStateOf {
            pinnedItemIndex.let { index ->
                lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(items) { index, item ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(if (index % 2 == 0) Color.LightGray else Color.White)
                        .padding(16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(text = item, fontSize = 20.sp)
                }
            }
        }

        val boxSize = remember { mutableStateOf(IntSize.Zero) }
        Box(modifier = Modifier
            .matchParentSize()
            .onSizeChanged { boxSize.value = it }
        )

        // Clamp only X during drag (keep horizontal inside screen)
        fun clampOffsetOnDrag(offset: Offset): Offset {
            val maxX = boxSize.value.width - imageSizePx
            return Offset(
                x = offset.x.coerceIn(0f, maxX.coerceAtLeast(0f)),
                y = offset.y // vertical free movement during drag
            )
        }

        // Clamp only X when pinned (no vertical clamp)
        fun clampOffsetPinned(offset: Offset): Offset {
            val maxX = boxSize.value.width - imageSizePx
            return Offset(
                x = offset.x.coerceIn(0f, maxX.coerceAtLeast(0f)),
                y = offset.y // no vertical clamping, keep pinned vertical offset as is
            )
        }

        val showImage = isDragging || (pinnedItemInfo != null)

        val actualImageOffset = if (isDragging) {
            clampOffsetOnDrag(imageOffset.value)
        } else {
            pinnedItemInfo?.let {
                clampOffsetPinned(
                    Offset(
                        x = offsetWithinItem.x,
                        y = it.offset.toFloat() + offsetWithinItem.y
                    )
                )
            } ?: Offset.Zero
        }

        LaunchedEffect(showImage, pinnedItemInfo, offsetWithinItem, isDragging, boxSize.value) {
            if (!isDragging && pinnedItemInfo != null && boxSize.value.width > 0 && boxSize.value.height > 0) {
                val target = clampOffsetPinned(
                    Offset(
                        x = offsetWithinItem.x,
                        y = pinnedItemInfo!!.offset.toFloat() + offsetWithinItem.y
                    )
                )
                imageOffset.snapTo(target)
            }
        }

        if (showImage) {
            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .background(Color.Red)
                    .zIndex(1f)
                    .graphicsLayer {
                        translationX = actualImageOffset.x
                        translationY = actualImageOffset.y
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { tapPosition ->
                                val tappedItem =
                                    lazyListState.layoutInfo.visibleItemsInfo.firstOrNull {
                                        val top = it.offset
                                        val bottom = top + it.size
                                        tapPosition.y in top.toFloat()..bottom.toFloat()
                                    }
                                if (tappedItem != null) {
                                    isDragging = false
                                    pinnedItemIndex = tappedItem.index
                                    offsetWithinItem = clampOffsetPinned(
                                        Offset(
                                            tapPosition.x,
                                            tapPosition.y - tappedItem.offset
                                        )
                                    )
                                    coroutineScope.launch {
                                        imageOffset.snapTo(clampOffsetPinned(tapPosition))
                                    }
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { touchPoint ->
                                isDragging = true
                                dragOffsetFromImageTopLeft = touchPoint - imageOffset.value

                                coroutineScope.launch {
                                    imageOffset.snapTo(clampOffsetOnDrag(touchPoint - dragOffsetFromImageTopLeft))
                                }

                                val dropTarget =
                                    lazyListState.layoutInfo.visibleItemsInfo.firstOrNull {
                                        val top = it.offset
                                        val bottom = top + it.size
                                        touchPoint.y in top.toFloat()..bottom.toFloat()
                                    }
                                pinnedItemIndex = dropTarget?.index ?: 0
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                coroutineScope.launch {
                                    val newOffset = imageOffset.value + dragAmount
                                    imageOffset.snapTo(clampOffsetOnDrag(newOffset))
                                }

                                val threshold = 100
                                val viewportHeight = lazyListState.layoutInfo.viewportEndOffset
                                val canScrollUp = lazyListState.firstVisibleItemIndex > 0 ||
                                        lazyListState.firstVisibleItemScrollOffset > 0
                                val canScrollDown = (lazyListState.layoutInfo.visibleItemsInfo
                                    .lastOrNull()?.index ?: 0) < items.lastIndex

                                coroutineScope.launch {
                                    when {
                                        imageOffset.value.y < threshold && canScrollUp -> {
                                            lazyListState.scrollBy(-35f)
                                        }

                                        imageOffset.value.y > viewportHeight - threshold && canScrollDown -> {
                                            lazyListState.scrollBy(35f)
                                        }
                                    }
                                }
                            },
                            onDragEnd = {
                                isDragging = false
                                val dropTarget =
                                    lazyListState.layoutInfo.visibleItemsInfo.firstOrNull {
                                        val top = it.offset
                                        val bottom = top + it.size
                                        imageOffset.value.y in top.toFloat()..bottom.toFloat()
                                    }

                                if (dropTarget != null) {
                                    pinnedItemIndex = dropTarget.index
                                    offsetWithinItem = clampOffsetPinned(
                                        Offset(
                                            x = imageOffset.value.x,
                                            y = imageOffset.value.y - dropTarget.offset
                                        )
                                    )
                                    val targetOffset = Offset(
                                        x = offsetWithinItem.x,
                                        y = dropTarget.offset.toFloat() + offsetWithinItem.y
                                    )
                                    coroutineScope.launch {
                                        imageOffset.animateTo(
                                            clampOffsetPinned(targetOffset),
                                            animationSpec = tween(
                                                durationMillis = 300,
                                                easing = FastOutSlowInEasing
                                            )
                                        )
                                    }
                                }
                            },
                            onDragCancel = {
                                isDragging = false
                            }
                        )
                    }
            )
        }
    }
}
*/
// finally achieved
// region Achieved
/*
@Composable
fun PinImageToLazyColumnItem() {
    val imageBitmap = ImageBitmap.imageResource(id = android.R.drawable.ic_menu_camera)
    val items = remember { List(50) { "Item #$it" } }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var pinnedItemIndex by remember { mutableStateOf(0) }
    var offsetWithinItem by remember { mutableStateOf(Offset(20f, 20f)) }

    val imageSizePx = with(LocalDensity.current) { 80.dp.toPx() }

    val imageOffset = remember {
        androidx.compose.animation.core.Animatable(Offset.Zero, Offset.VectorConverter)
    }

    var isDragging by remember { mutableStateOf(false) }
    var dragOffsetFromImageTopLeft by remember { mutableStateOf(Offset.Zero) }

    val scrollOffsetTrigger = remember { mutableStateOf(0) }
    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemScrollOffset }
            .collect {
                scrollOffsetTrigger.value = it
            }
    }

    val pinnedItemInfo by remember {
        derivedStateOf {
            pinnedItemIndex?.let { index ->
                lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(items) { index, item ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(if (index % 2 == 0) Color.LightGray else Color.White)
                        .padding(16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(text = item, fontSize = 20.sp)
                }
            }
        }

        val boxSize = remember { mutableStateOf(IntSize.Zero) }
        Box(modifier = Modifier
            .matchParentSize()
            .onSizeChanged { boxSize.value = it }
        )

        // Clamp only X during drag (to keep image horizontally inside screen)
        fun clampOffsetOnDrag(offset: Offset): Offset {
            val maxX = boxSize.value.width - imageSizePx
            return Offset(
                x = offset.x.coerceIn(0f, maxX.coerceAtLeast(0f)),
                y = offset.y // allow vertical free movement beyond bounds
            )
        }

        // Clamp both X and Y when pinned (to keep inside visible area)
        fun clampOffsetPinned(offset: Offset): Offset {
            val maxX = boxSize.value.width - imageSizePx
            val maxY = boxSize.value.height - imageSizePx
            return Offset(
                x = offset.x.coerceIn(0f, maxX.coerceAtLeast(0f)),
                y = offset.y.coerceIn(0f, maxY.coerceAtLeast(0f))
            )
        }

        val showImage = isDragging || (pinnedItemInfo != null)

        val actualImageOffset = if (isDragging) {
            clampOffsetOnDrag(imageOffset.value)
        } else {
            pinnedItemInfo?.let {
                clampOffsetPinned(
                    Offset(
                        x = offsetWithinItem.x,
                        y = it.offset.toFloat() + offsetWithinItem.y
                    )
                )
            } ?: Offset.Zero
        }

        LaunchedEffect(showImage, pinnedItemInfo, offsetWithinItem, isDragging, boxSize.value) {
            if (!isDragging && pinnedItemInfo != null && boxSize.value.width > 0 && boxSize.value.height > 0) {
                val target = clampOffsetPinned(
                    Offset(
                        x = offsetWithinItem.x,
                        y = pinnedItemInfo!!.offset.toFloat() + offsetWithinItem.y
                    )
                )
                imageOffset.snapTo(target)
            }
        }

        if (showImage) {
            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .background(Color.Red)
                    .zIndex(1f)
                    .graphicsLayer {
                        translationX = actualImageOffset.x
                        translationY = actualImageOffset.y
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { tapPosition ->
                                val tappedItem =
                                    lazyListState.layoutInfo.visibleItemsInfo.firstOrNull {
                                        val top = it.offset
                                        val bottom = top + it.size
                                        tapPosition.y in top.toFloat()..bottom.toFloat()
                                    }
                                if (tappedItem != null) {
                                    isDragging = false
                                    pinnedItemIndex = tappedItem.index
                                    offsetWithinItem = clampOffsetPinned(
                                        Offset(
                                            tapPosition.x,
                                            tapPosition.y - tappedItem.offset
                                        )
                                    )
                                    coroutineScope.launch {
                                        imageOffset.snapTo(clampOffsetPinned(tapPosition))
                                    }
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { touchPoint ->
                                isDragging = true
                                dragOffsetFromImageTopLeft = touchPoint - imageOffset.value

                                coroutineScope.launch {
                                    imageOffset.snapTo(clampOffsetOnDrag(touchPoint - dragOffsetFromImageTopLeft))
                                }

                                val dropTarget =
                                    lazyListState.layoutInfo.visibleItemsInfo.firstOrNull {
                                        val top = it.offset
                                        val bottom = top + it.size
                                        touchPoint.y in top.toFloat()..bottom.toFloat()
                                    }
                                pinnedItemIndex = dropTarget?.index ?: 0
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                coroutineScope.launch {
                                    val newOffset = imageOffset.value + dragAmount
                                    imageOffset.snapTo(clampOffsetOnDrag(newOffset))
                                }

                                val threshold = 100
                                val viewportHeight = lazyListState.layoutInfo.viewportEndOffset
                                val canScrollUp = lazyListState.firstVisibleItemIndex > 0 ||
                                        lazyListState.firstVisibleItemScrollOffset > 0
                                val canScrollDown = (lazyListState.layoutInfo.visibleItemsInfo
                                    .lastOrNull()?.index ?: 0) < items.lastIndex

                                coroutineScope.launch {
                                    when {
                                        imageOffset.value.y < threshold && canScrollUp -> {
                                            lazyListState.scrollBy(-35f)
                                        }

                                        imageOffset.value.y > viewportHeight - threshold && canScrollDown -> {
                                            lazyListState.scrollBy(35f)
                                        }
                                    }
                                }
                            },
                            onDragEnd = {
                                isDragging = false
                                val dropTarget =
                                    lazyListState.layoutInfo.visibleItemsInfo.firstOrNull {
                                        val top = it.offset
                                        val bottom = top + it.size
                                        imageOffset.value.y in top.toFloat()..bottom.toFloat()
                                    }

                                if (dropTarget != null) {
                                    pinnedItemIndex = dropTarget.index
                                    offsetWithinItem = clampOffsetPinned(
                                        Offset(
                                            x = imageOffset.value.x,
                                            y = imageOffset.value.y - dropTarget.offset
                                        )
                                    )
                                    val targetOffset = Offset(
                                        x = offsetWithinItem.x,
                                        y = dropTarget.offset.toFloat() + offsetWithinItem.y
                                    )
                                    coroutineScope.launch {
                                        imageOffset.animateTo(
                                            clampOffsetPinned(targetOffset),
                                            animationSpec = tween(
                                                durationMillis = 300,
                                                easing = FastOutSlowInEasing
                                            )
                                        )
                                    }
                                }
                            },
                            onDragCancel = {
                                isDragging = false
                            }
                        )
                    }
            )
        }
    }
}
*/
// endregion

// region Locked Code - its good to go code
// Perfect achieved what i want to achieve.
/*
@Composable
fun PinImageToLazyColumnItem() {
    val imageBitmap = ImageBitmap.imageResource(id = R.drawable.one_)
    val items = remember { List(50) { "Item #$it" } }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var pinnedItemIndex by remember { mutableStateOf(0) }
    var offsetWithinItem by remember { mutableStateOf(Offset(20f, 20f)) }

    val imageOffset = remember {
        androidx.compose.animation.core.Animatable(Offset.Zero, Offset.VectorConverter)
    }

    var isDragging by remember { mutableStateOf(false) }
    var dragOffsetFromImageTopLeft by remember { mutableStateOf(Offset.Zero) }

    // Trigger recomposition on scroll for smooth image position updates
    val scrollOffsetTrigger = remember { mutableStateOf(0) }
    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemScrollOffset }
            .collect {
                scrollOffsetTrigger.value = it
            }
    }

    val pinnedItemInfo by remember {
        derivedStateOf {
            pinnedItemIndex?.let { index ->
                lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(items) { index, item ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(if (index % 2 == 0) Color.LightGray else Color.White)
                        .padding(16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(text = item, fontSize = 20.sp)
                }
            }
        }

        val showImage = isDragging || (pinnedItemInfo != null)

        val actualImageOffset = if (isDragging) {
            imageOffset.value
        } else {
            pinnedItemInfo?.let {
                // Use both x and y offset relative to item + item offset on screen
                Offset(
                    x = offsetWithinItem.x,
                    y = it.offset.toFloat() + offsetWithinItem.y
                )
            } ?: Offset.Zero
        }

        // Sync imageOffset when not dragging (for smooth scrolling updates)
        LaunchedEffect(showImage, pinnedItemInfo, offsetWithinItem, isDragging) {
            if (!isDragging && pinnedItemInfo != null) {
                val target = Offset(
                    x = offsetWithinItem.x,
                    y = pinnedItemInfo!!.offset.toFloat() + offsetWithinItem.y
                )
                imageOffset.snapTo(target)
            }
        }

        if (showImage) {
            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .background(Color.Red)
                    .zIndex(1f)
                    .graphicsLayer {
                        translationX = actualImageOffset.x
                        translationY = actualImageOffset.y
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { tapPosition ->
                                val tappedItem = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull {
                                    val top = it.offset
                                    val bottom = top + it.size
                                    tapPosition.y in top.toFloat()..bottom.toFloat()
                                }
                                if (tappedItem != null) {
                                    isDragging = false
                                    pinnedItemIndex = tappedItem.index
                                    offsetWithinItem = Offset(
                                        x = tapPosition.x,
                                        y = tapPosition.y - tappedItem.offset
                                    )
                                    coroutineScope.launch {
                                        imageOffset.snapTo(tapPosition)
                                    }
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { touchPoint ->
                                isDragging = true

                                // Calculate finger offset relative to image top-left
                                dragOffsetFromImageTopLeft = touchPoint - imageOffset.value

                                coroutineScope.launch {
                                    imageOffset.snapTo(touchPoint - dragOffsetFromImageTopLeft)
                                }

                                // Update pinned index based on vertical touch position
                                val dropTarget = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull {
                                    val top = it.offset
                                    val bottom = top + it.size
                                    touchPoint.y in top.toFloat()..bottom.toFloat()
                                }
                                pinnedItemIndex = dropTarget?.index ?: 0
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                coroutineScope.launch {
                                    // Move image by dragAmount smoothly, respecting finger offset
                                    imageOffset.snapTo(imageOffset.value + dragAmount)
                                }

                                val threshold = 100
                                val viewportHeight = lazyListState.layoutInfo.viewportEndOffset
                                val canScrollUp = lazyListState.firstVisibleItemIndex > 0 ||
                                        lazyListState.firstVisibleItemScrollOffset > 0
                                val canScrollDown = (lazyListState.layoutInfo.visibleItemsInfo
                                    .lastOrNull()?.index ?: 0) < items.lastIndex

                                coroutineScope.launch {
                                    when {
                                        imageOffset.value.y < threshold && canScrollUp -> {
                                            lazyListState.scrollBy(-35f)
                                        }
                                        imageOffset.value.y > viewportHeight - threshold && canScrollDown -> {
                                            lazyListState.scrollBy(35f)
                                        }
                                    }
                                }
                            },
                            onDragEnd = {
                                isDragging = false
                                val dropTarget = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull {
                                    val top = it.offset
                                    val bottom = top + it.size
                                    imageOffset.value.y in top.toFloat()..bottom.toFloat()
                                }

                                if (dropTarget != null) {
                                    pinnedItemIndex = dropTarget.index
                                    // Calculate offset relative to item for both X and Y
                                    offsetWithinItem = Offset(
                                        x = imageOffset.value.x,
                                        y = imageOffset.value.y - dropTarget.offset
                                    )
                                    val targetOffset = Offset(
                                        x = offsetWithinItem.x,
                                        y = dropTarget.offset.toFloat() + offsetWithinItem.y
                                    )
                                    coroutineScope.launch {
                                        imageOffset.animateTo(
                                            targetOffset,
                                            animationSpec = tween(
                                                durationMillis = 300,
                                                easing = FastOutSlowInEasing
                                            )
                                        )
                                    }
                                }
                            },
                            onDragCancel = {
                                isDragging = false
                            }
                        )
                    }
            )
        }

        Text(
            text = "Image offset: x=${actualImageOffset.x.format(1)}, y=${actualImageOffset.y.format(1)}",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .background(Color.White.copy(alpha = 0.7f))
                .padding(8.dp)
        )
    }
}
*/
private fun Float.format(digits: Int) = "%.${digits}f".format(this)
// endregion

// improved code
/*
@Composable
fun PinImageToLazyColumnItem() {
    val imageBitmap = ImageBitmap.imageResource(id = android.R.drawable.ic_menu_camera)
    val items = remember { List(50) { "Item #$it" } }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var pinnedItemIndex by remember { mutableStateOf(0) }
    var offsetWithinItem by remember { mutableStateOf(Offset(20f, 20f)) }

    // Animatable to smoothly animate image position
    val imageOffset = remember {
        androidx.compose.animation.core.Animatable(
            Offset(offsetWithinItem.x, 0f),
            Offset.VectorConverter
        )
    }

    var isDragging by remember { mutableStateOf(false) }
    var dragOffsetFromImageTopLeft by remember { mutableStateOf(Offset.Zero) }

    val scrollOffsetTrigger = remember { mutableStateOf(0) }
    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemScrollOffset }
            .collect {
                scrollOffsetTrigger.value = it
            }
    }

    val pinnedItemInfo by remember {
        derivedStateOf {
            pinnedItemIndex?.let { index ->
                lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(items) { index, item ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(if (index % 2 == 0) Color.LightGray else Color.White)
                        .padding(16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(text = item, fontSize = 20.sp)
                }
            }
        }

        val showImage = isDragging || (pinnedItemInfo != null)

        val actualImageOffset = if (isDragging) {
            imageOffset.value
        } else {
            pinnedItemInfo?.let {
                Offset(
                    x = offsetWithinItem.x,
                    y = it.offset.toFloat() + offsetWithinItem.y
                )
            } ?: Offset.Zero
        }

        if (showImage) {
            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .background(Color.Red)
                    .zIndex(1f)
                    .graphicsLayer {
                        translationX = actualImageOffset.x
                        translationY = actualImageOffset.y
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { tapPosition ->
                                val tappedItem = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull {
                                    val top = it.offset
                                    val bottom = top + it.size
                                    tapPosition.y in top.toFloat()..bottom.toFloat()
                                }
                                if (tappedItem != null) {
                                    isDragging = false
                                    pinnedItemIndex = tappedItem.index
                                    offsetWithinItem = Offset(
                                        x = tapPosition.x,
                                        y = tapPosition.y - tappedItem.offset
                                    )
                                    coroutineScope.launch {
                                        imageOffset.snapTo(tapPosition)
                                    }
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { touchPoint ->
                                isDragging = true

                                // Calculate finger offset relative to image top-left
                                dragOffsetFromImageTopLeft = touchPoint - imageOffset.value

                                // Snap imageOffset so drag movement is smooth (finger stays at dragOffset)
                                coroutineScope.launch {
                                    imageOffset.snapTo(touchPoint - dragOffsetFromImageTopLeft)
                                }

                                // Update pinned index based on touch vertical location
                                val dropTarget = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull {
                                    val top = it.offset
                                    val bottom = top + it.size
                                    touchPoint.y in top.toFloat()..bottom.toFloat()
                                }
                                pinnedItemIndex = dropTarget?.index ?:0
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                coroutineScope.launch {
                                    // Move image by finger movement maintaining offset from finger
                                    imageOffset.snapTo(imageOffset.value + dragAmount)
                                }

                                val threshold = 100
                                val viewportHeight = lazyListState.layoutInfo.viewportEndOffset
                                val canScrollUp = lazyListState.firstVisibleItemIndex > 0 ||
                                        lazyListState.firstVisibleItemScrollOffset > 0
                                val canScrollDown = (lazyListState.layoutInfo.visibleItemsInfo
                                    .lastOrNull()?.index ?: 0) < items.lastIndex

                                coroutineScope.launch {
                                    when {
                                        imageOffset.value.y < threshold && canScrollUp -> {
                                            lazyListState.scrollBy(-35f)
                                        }
                                        imageOffset.value.y > viewportHeight - threshold && canScrollDown -> {
                                            lazyListState.scrollBy(35f)
                                        }
                                    }
                                }
                            },
                            onDragEnd = {
                                isDragging = false

                                val dropTarget = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull {
                                    val top = it.offset
                                    val bottom = top + it.size
                                    imageOffset.value.y in top.toFloat()..bottom.toFloat()
                                }

                                if (dropTarget != null) {
                                    pinnedItemIndex = dropTarget.index
                                    // Update offsetWithinItem with current imageOffset relative to item
                                    offsetWithinItem = Offset(
                                        x = imageOffset.value.x,
                                        y = imageOffset.value.y - dropTarget.offset
                                    )
                                    val targetOffset = Offset(
                                        x = offsetWithinItem.x,
                                        y = dropTarget.offset.toFloat() + offsetWithinItem.y
                                    )
                                    coroutineScope.launch {
                                        imageOffset.animateTo(
                                            targetOffset,
                                            animationSpec = tween(
                                                durationMillis = 300,
                                                easing = FastOutSlowInEasing
                                            )
                                        )
                                    }
                                }
                            },
                            onDragCancel = {
                                isDragging = false
                            }
                        )
                    }
            )
        }

        Text(
            text = "Image offset: x=${actualImageOffset.x.format(1)}, y=${actualImageOffset.y.format(1)}",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .background(Color.White.copy(alpha = 0.7f))
                .padding(8.dp)
        )
    }
}
*/

// so far so nice code
/*
@Composable
fun PinImageToLazyColumnItem() {
    val imageBitmap = ImageBitmap.imageResource(id = R.drawable.one_)
    val items = remember { List(50) { "Item #$it" } }
    val lazyListState = rememberLazyListState()

    val imageOffset = remember {
        androidx.compose.animation.core.Animatable(Offset(100f, 100f), Offset.VectorConverter)
    }
    var isDragging by remember { mutableStateOf(false) }
    var pinnedItemIndex by remember { mutableStateOf<Int?>(0) }
    var offsetWithinItem by remember { mutableStateOf(Offset.Zero) }
    val coroutineScope = rememberCoroutineScope()
    var dragOffsetFromImageTopLeft by remember { mutableStateOf(Offset.Zero) }

    // Dummy trigger to force recomposition during scroll
    val scrollOffsetTrigger = remember { mutableStateOf(0) }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemScrollOffset }
            .collect {
                scrollOffsetTrigger.value = it // force recomposition
            }
    }

    // Reactive pinned item info
    val pinnedItemInfo by remember {
        derivedStateOf {
            pinnedItemIndex?.let { index ->
                lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(items) { index, item ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(if (index % 2 == 0) Color.LightGray else Color.White)
                        .padding(16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(text = item, fontSize = 20.sp)
                }
            }
        }

        val showImage = isDragging || (pinnedItemInfo != null)
        val actualImageOffset = if (isDragging) {
            imageOffset.value
        } else {
            pinnedItemInfo?.let {
                Offset(
                    x = offsetWithinItem.x,
                    y = it.offset.toFloat() + offsetWithinItem.y
                )
            } ?: Offset(0f, 0f)
        }

        if (showImage) {
            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .zIndex(1f)
                    .graphicsLayer {
                        translationX = actualImageOffset.x
                        translationY = actualImageOffset.y
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { touchPoint ->
                                if (!isDragging) {
                                    val dropTarget = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull {
                                        val top = it.offset
                                        val bottom = top + it.size
                                        imageOffset.value.y in top.toFloat()..bottom.toFloat()
                                    }

                                    isDragging = true
                                    pinnedItemIndex = dropTarget?.index
                                    dragOffsetFromImageTopLeft = touchPoint - imageOffset.value
                                    coroutineScope.launch {
                                        imageOffset.snapTo(touchPoint - dragOffsetFromImageTopLeft)
                                    }
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                coroutineScope.launch {
                                    imageOffset.snapTo(imageOffset.value + dragAmount)
                                }

                                val threshold = 100
                                val viewportHeight = lazyListState.layoutInfo.viewportEndOffset
                                val canScrollUp = lazyListState.firstVisibleItemIndex > 0 ||
                                        lazyListState.firstVisibleItemScrollOffset > 0
                                val canScrollDown = (lazyListState.layoutInfo.visibleItemsInfo
                                    .lastOrNull()?.index ?: 0) < items.lastIndex

                                coroutineScope.launch {
                                    when {
                                        imageOffset.value.y < threshold && canScrollUp -> {
                                            lazyListState.scrollBy(-35f)
                                        }
                                        imageOffset.value.y > viewportHeight - threshold && canScrollDown -> {
                                            lazyListState.scrollBy(35f)
                                        }
                                    }
                                }
                            },
                            onDragEnd = {
                                isDragging = false
                                val dropTarget = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull {
                                    val top = it.offset
                                    val bottom = top + it.size
                                    imageOffset.value.y in top.toFloat()..bottom.toFloat()
                                }

                                if (dropTarget != null) {
                                    pinnedItemIndex = dropTarget.index
                                    offsetWithinItem = Offset(
                                        x = imageOffset.value.x,
                                        y = imageOffset.value.y - dropTarget.offset
                                    )

                                    val targetOffset = Offset(
                                        x = offsetWithinItem.x,
                                        y = dropTarget.offset.toFloat() + offsetWithinItem.y
                                    )

                                    coroutineScope.launch {
                                        imageOffset.animateTo(
                                            targetOffset,
                                            animationSpec = tween(
                                                durationMillis = 300,
                                                easing = FastOutSlowInEasing
                                            )
                                        )
                                    }
                                }
                            },
                            onDragCancel = {
                                isDragging = false
                            }
                        )
                    }
            )
        }
    }
}
*/

/*
@Composable
fun PinImageToLazyColumnItem() {
    val imageBitmap = ImageBitmap.imageResource(id = R.drawable.one_)
    val items = remember { List(50) { "Item #$it" } }
    val lazyListState = rememberLazyListState()

    val imageOffset = remember {
        androidx.compose.animation.core.Animatable(Offset(100f, 100f), Offset.VectorConverter)
    }
    var isDragging by remember { mutableStateOf(false) }
    var pinnedItemIndex by remember { mutableStateOf<Int?>(0) }
    var offsetWithinItem by remember { mutableStateOf(Offset.Zero) }
    val coroutineScope = rememberCoroutineScope()
    var dragOffsetFromImageTopLeft by remember { mutableStateOf(Offset.Zero) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(items) { index, item ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(if (index % 2 == 0) Color.LightGray else Color.White)
                        .padding(16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(text = item, fontSize = 20.sp)
                }
            }
        }

        val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
        val pinnedItemInfo = pinnedItemIndex?.let { index ->
            visibleItems.firstOrNull { it.index == index }
        }

        val showImage = isDragging || (pinnedItemInfo != null)
        val actualImageOffset = if (isDragging) {
            imageOffset.value
        } else {
            pinnedItemInfo?.let {
                Offset(
                    x = offsetWithinItem.x,
                    y = it.offset.toFloat() + offsetWithinItem.y
                )
            } ?: Offset(0f, 0f)
        }

        if (showImage) {
            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .zIndex(1f) // Ensure image stays above list items
                    .graphicsLayer {
                        translationX = actualImageOffset.x
                        translationY = actualImageOffset.y
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { touchPoint ->
                                if (!isDragging) {
                                    val dropTarget = visibleItems.firstOrNull {
                                        val top = it.offset
                                        val bottom = top + it.size
                                        imageOffset.value.y in top.toFloat()..bottom.toFloat()
                                    }

                                    isDragging = true
                                    pinnedItemIndex = dropTarget?.index
                                    dragOffsetFromImageTopLeft = touchPoint - imageOffset.value
                                    coroutineScope.launch {
                                        imageOffset.snapTo(touchPoint - dragOffsetFromImageTopLeft)
                                    }
                                }
                            },

                            onDrag = { change, dragAmount ->
                                change.consume()
                                coroutineScope.launch {
                                    imageOffset.snapTo(imageOffset.value + dragAmount)
                                }

                                val threshold = 100
                                val viewportHeight = lazyListState.layoutInfo.viewportEndOffset
                                val canScrollUp = lazyListState.firstVisibleItemIndex > 0 ||
                                        lazyListState.firstVisibleItemScrollOffset > 0
                                val canScrollDown = (lazyListState.layoutInfo.visibleItemsInfo
                                    .lastOrNull()?.index ?: 0) < items.lastIndex

                                coroutineScope.launch {
                                    when {
                                        imageOffset.value.y < threshold && canScrollUp -> {
                                            lazyListState.scrollBy(-35f)
                                        }

                                        imageOffset.value.y > viewportHeight - threshold && canScrollDown -> {
                                            lazyListState.scrollBy(35f)
                                        }
                                    }
                                }
                            },
                            onDragEnd = {
                                isDragging = false
                                val dropTarget = visibleItems.firstOrNull {
                                    val top = it.offset
                                    val bottom = top + it.size
                                    imageOffset.value.y in top.toFloat()..bottom.toFloat()
                                }

                                if (dropTarget != null) {
                                    pinnedItemIndex = dropTarget.index
                                    offsetWithinItem = Offset(
                                        x = imageOffset.value.x,
                                        y = imageOffset.value.y - dropTarget.offset
                                    )

                                    val targetOffset = Offset(
                                        x = offsetWithinItem.x,
                                        y = dropTarget.offset.toFloat() + offsetWithinItem.y
                                    )

                                    coroutineScope.launch {
                                        imageOffset.animateTo(
                                            targetOffset,
                                            animationSpec = tween(
                                                durationMillis = 300,
                                                easing = FastOutSlowInEasing
                                            )
                                        )
                                    }
                                }
                            },
                            onDragCancel = {
                                isDragging = false
                            }
                        )
                    }
            )
        }
    }
}
*/

/*
@Composable
fun PinImageToLazyColumnItem() {
    val imageBitmap = ImageBitmap.imageResource(id = R.drawable.one_)
    val items = remember { List(50) { "Item #$it" } }
    val lazyListState = rememberLazyListState()

    val imageOffset = remember {
        androidx.compose.animation.core.Animatable(
            Offset(100f, 100f),
            Offset.VectorConverter
        )
    }
    var isDragging by remember { mutableStateOf(false) }
    var pinnedItemIndex by remember { mutableStateOf<Int?>(0) }
    var offsetWithinItem by remember { mutableStateOf(Offset.Zero) }
    val coroutineScope = rememberCoroutineScope()
    var dragOffsetFromImageTopLeft by remember { mutableStateOf(Offset.Zero) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(items) { index, item ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(if (index % 2 == 0) Color.LightGray else Color.White)
                        .padding(16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(text = item, fontSize = 20.sp)
                }
            }
        }

        val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
        val pinnedItemInfo = pinnedItemIndex?.let { index ->
            visibleItems.firstOrNull { it.index == index }
        }

        val showImage = isDragging || (pinnedItemInfo != null)
        val actualImageOffset = if (isDragging) {
            imageOffset.value
        } else {
            pinnedItemInfo?.let {
                Offset(
                    x = offsetWithinItem.x,
                    y = it.offset.toFloat() + offsetWithinItem.y
                )
            } ?: Offset(0f, 0f)
        }

        if (showImage) {
            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .graphicsLayer {
                        translationX = actualImageOffset.x
                        translationY = actualImageOffset.y
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { touchPoint ->
                                isDragging = true
                                pinnedItemIndex = null
                                dragOffsetFromImageTopLeft = touchPoint - imageOffset.value
                                coroutineScope.launch {
                                    imageOffset.snapTo(touchPoint - dragOffsetFromImageTopLeft)
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                coroutineScope.launch {
                                    imageOffset.snapTo(imageOffset.value + dragAmount)
                                }

                                coroutineScope.launch {
                                    val threshold = 100
                                    val viewportHeight = lazyListState.layoutInfo.viewportEndOffset
                                    if (imageOffset.value.y < threshold) {
                                        lazyListState.scrollBy(-35f)
                                    } else if (imageOffset.value.y > viewportHeight - threshold) {
                                        lazyListState.scrollBy(35f)
                                    }
                                }
                            },
                            onDragEnd = {
                                isDragging = false
                                val dropTarget = visibleItems.firstOrNull {
                                    val top = it.offset
                                    val bottom = top + it.size
                                    imageOffset.value.y in top.toFloat()..bottom.toFloat()
                                }
                                if (dropTarget != null) {
                                    pinnedItemIndex = dropTarget.index
                                    offsetWithinItem = Offset(
                                        x = imageOffset.value.x,
                                        y = imageOffset.value.y - dropTarget.offset
                                    )

                                    // Animate smoothly into pinned position
                                    val targetOffset = Offset(
                                        x = offsetWithinItem.x,
                                        y = dropTarget.offset.toFloat() + offsetWithinItem.y
                                    )

                                    coroutineScope.launch {
                                        imageOffset.animateTo(targetOffset, tween(300))
                                    }
                                }
                            },
                            onDragCancel = {
                                isDragging = false
                            }
                        )
                    }
            )
        }
    }
}
*/

*/