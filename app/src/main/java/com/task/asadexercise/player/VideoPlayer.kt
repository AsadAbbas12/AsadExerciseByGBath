package com.task.asadexercise.player


import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import java.io.File

object VideoCache {
    private var cache: SimpleCache? = null

    fun getCache(context: Context): SimpleCache {
        return cache ?: synchronized(this) {
            cache ?: createCache(context).also { cache = it }
        }
    }

    private fun createCache(context: Context): SimpleCache {
        val cacheDir = File(context.cacheDir, "media")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        return SimpleCache(cacheDir, LeastRecentlyUsedCacheEvictor(90 * 1024 * 1024)) // 90MB cache
    }

    fun releaseCache() {
        cache?.release()
        cache = null
    }
}

@Composable
fun VideoPlayer(videoUri: Uri, isPlaying: Boolean, onVideoEnded: () -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val cache = VideoCache.getCache(context)

    val exoPlayer = remember {
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(DefaultDataSource.Factory(context))

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(videoUri))
                prepare()
            }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) exoPlayer.play() else exoPlayer.pause()
    }

    LaunchedEffect(exoPlayer) {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    onVideoEnded()
                }
            }
        })
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false  // Removes ExoPlayer UI buttons
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

