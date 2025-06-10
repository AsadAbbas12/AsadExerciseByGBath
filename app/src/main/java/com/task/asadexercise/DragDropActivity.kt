package com.task.asadexercise

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import kotlinx.coroutines.launch

class DragDropListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PinImageToLazyColumnItem()
                }
            }
        }
    }
}

@Composable
fun PinImageToLazyColumnItem() {
    val imageBitmap = ImageBitmap.imageResource(id = R.drawable.one_)
    val items = remember { List(50) { "Item #$it" } }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var pinnedItemIndex by remember { mutableStateOf(0) }
    var offsetWithinItem by remember { mutableStateOf(Offset(10f, 10f)) }

    val imageOffset = remember {
        androidx.compose.animation.core.Animatable(Offset.Zero, Offset.VectorConverter)
    }

    var isDragging by remember { mutableStateOf(false) }
    var dragOffsetFromImageTopLeft by remember { mutableStateOf(Offset.Zero) }

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
                        .height(800.dp)
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
                                val tappedItem =
                                    lazyListState.layoutInfo.visibleItemsInfo.firstOrNull {
                                        val top = it.offset
                                        val bottom = top + it.size
                                        tapPosition.y in top.toFloat()..bottom.toFloat()
                                    }
                                if (tappedItem != null) {
                                    isDragging = false
                                    pinnedItemIndex = tappedItem.index
                                    offsetWithinItem = tapPosition
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
                                dragOffsetFromImageTopLeft = touchPoint - imageOffset.value

                                coroutineScope.launch {
                                    imageOffset.snapTo(touchPoint - dragOffsetFromImageTopLeft)
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
                                    imageOffset.snapTo(newOffset)
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
                                }
                                offsetWithinItem = imageOffset.value

                                coroutineScope.launch {
                                    imageOffset.animateTo(
                                        imageOffset.value,
                                        animationSpec = tween(
                                            durationMillis = 300,
                                            easing = FastOutSlowInEasing
                                        )
                                    )
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
@Composable
fun PinImageToLazyColumnItem() {
    val imageBitmap = ImageBitmap.imageResource(id = android.R.drawable.ic_menu_camera)
    val items = remember { List(50) { "Item #$it" } }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var pinnedItemIndex by remember { mutableStateOf(0) }
    var offsetWithinItem by remember { mutableStateOf(Offset(10f, 10f)) }

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
                        .height(800.dp)
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

        fun clampOffsetPinned(offset: Offset): Offset {
            // Allow full-screen movement in both directions
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

//                                val threshold = 5
//                                val viewportHeight = lazyListState.layoutInfo.viewportEndOffset
//                                val canScrollUp = lazyListState.firstVisibleItemIndex > 0 ||
//                                        lazyListState.firstVisibleItemScrollOffset > 0
//                                val canScrollDown = (lazyListState.layoutInfo.visibleItemsInfo
//                                    .lastOrNull()?.index ?: 0) < items.lastIndex
//
//                                coroutineScope.launch {
//                                    when {
//                                        imageOffset.value.y < threshold && canScrollUp -> {
//                                            lazyListState.scrollBy(-35f)
//                                        }
//
//                                        imageOffset.value.y > viewportHeight - threshold && canScrollDown -> {
//                                            lazyListState.scrollBy(35f)
//                                        }
//                                    }
//                                }
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