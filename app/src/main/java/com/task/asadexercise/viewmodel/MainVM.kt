package com.task.asadexercise.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.task.asadexercise.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.core.net.toUri
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainVM @Inject constructor() : ViewModel() {

    private val _videos = MutableStateFlow<List<Uri>>(emptyList())
    val videos: StateFlow<List<Uri>> = _videos

    init {
        loadVideos()
    }

    private fun loadVideos() {
        _videos.value = listOf(
            "android.resource://com.task.asadexercise/${R.raw.wetex_new}".toUri(),
            "android.resource://com.task.asadexercise/${R.drawable.one_}".toUri(),
            "android.resource://com.task.asadexercise/${R.raw.wetex_new}".toUri(),
            "android.resource://com.task.asadexercise/${R.drawable.one_}".toUri(),
            "android.resource://com.task.asadexercise/${R.raw.wetex_new}".toUri(),
            "android.resource://com.task.asadexercise/${R.drawable.one_}".toUri(),
            "android.resource:/com.task.asadexercise/${R.raw.wetex_new}".toUri(),
            "android.resource://com.task.asadexercise/${R.drawable.one_}".toUri()
        )
    }
}
