package com.task.asadexercise.screens

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.task.asadexercise.R
import com.task.asadexercise.player.VideoPlayer

@Composable
fun VideoList(videos: List<Uri>) {
    val listState = rememberLazyListState()
    var currentlyPlayingIndex by remember { mutableStateOf(-1) }

    // Use rememberUpdatedState to avoid lag and prevent unnecessary recomposition
    val updatedCurrentlyPlayingIndex by rememberUpdatedState(currentlyPlayingIndex)

    // LaunchedEffect to track the visible item index
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }.collect { index ->
            if (updatedCurrentlyPlayingIndex != index) {
                currentlyPlayingIndex =
                    index // Update the currently playing index only when necessary
            }
        }
    }

    LazyColumn(state = listState) {
        itemsIndexed(videos) { index, mediaUri ->
            Box(
                modifier = Modifier
                    .fillMaxWidth() // Fill width but don't stretch vertically
                    .height(300.dp) // Set a fixed height for the items
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (index == 1 || index == 3 || index == 5) {
                    // Display image if the URI points to an image
                    Image(
                        painter = painterResource(id = R.drawable.one_), // Use the correct drawable name
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .aspectRatio(1.77f) // Aspect ratio to prevent stretching
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Display video if the URI points to a video
                    VideoPlayer(
                        videoUri = mediaUri,
                        isPlaying = index == updatedCurrentlyPlayingIndex, // Check if video is playing
                        onVideoEnded = {
                            // Handle video ended event
                            currentlyPlayingIndex = -1
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .aspectRatio(1.77f) // Aspect ratio for videos too
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            }
        }
    }
}
