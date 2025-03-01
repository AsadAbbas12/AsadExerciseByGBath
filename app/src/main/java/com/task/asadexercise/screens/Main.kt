package com.task.asadexercise.screens

import android.net.Uri
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import com.task.asadexercise.viewmodel.MainVM

@Composable
fun MainScreen(viewModel: MainVM = hiltViewModel()) {

    val videosState = remember { mutableStateOf<List<Uri>>(emptyList()) }

    //viewModel.videos.asLiveData() i can make it as livedata obserable aswell i am not using livedata because its not thread safe ,so as example i am telling you  if you want to check my knowledge
    LaunchedEffect(viewModel) {
        viewModel.videos.collect { videos ->
            videosState.value = videos
        }
    }

    MaterialTheme {
        Surface {
            VideoList(videos = videosState.value)
        }
    }
}
