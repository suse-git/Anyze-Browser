package com.example.mangabrowser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.mangabrowser.BrowserViewModel
import com.example.mangabrowser.data.MangaParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun ReaderScreen(viewModel: BrowserViewModel) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    LaunchedEffect(viewModel.url) {
        if (viewModel.url.isEmpty() || viewModel.url == "about:blank") return@LaunchedEffect
        
        if (viewModel.currentChapterUrl == viewModel.url && viewModel.chapterImages.isNotEmpty()) {
            listState.scrollToItem(0)
            return@LaunchedEffect
        }
        
        listState.scrollToItem(0)
        isLoading = true
        
        try {
            val html = withContext(Dispatchers.IO) {
                val connection = URL(viewModel.url).openConnection() as HttpURLConnection
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36")
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.inputStream.bufferedReader().use { it.readText() }
            }
            val images = MangaParser.parseChapterImages(html, viewModel.url)
            if (images.isNotEmpty()) {
                viewModel.chapterImages.clear()
                viewModel.chapterImages.addAll(images)
                viewModel.currentChapterUrl = viewModel.url
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(listState) {
        androidx.compose.runtime.snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null && lastVisibleItem.index == totalItems - 1 && totalItems > 0
        }.collect { isAtBottom ->
            if (isAtBottom && !isLoading && viewModel.chapterImages.isNotEmpty()) {
                viewModel.loadNextChapter()
            }
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)) {
        
        Column(modifier = Modifier.fillMaxSize()) {
            if (viewModel.chapterImages.isNotEmpty()) {
                ReaderStatsBar(
                    loadedCount = viewModel.chapterImages.size,
                    totalCount = viewModel.chapterImages.size,
                    onPrevChapter = { viewModel.loadNextChapter() },
                    onNextChapter = { /* Next logic */ }
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                itemsIndexed(viewModel.chapterImages) { index, imageUrl ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imageUrl)
                                .setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36")
                                .setHeader("Referer", viewModel.url)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Page ${index + 1}",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.FillWidth
                        )
                        // Page counter (Manga Loader style)
                        Box(
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .background(Color(0xFF222222), shape = MaterialTheme.shapes.small)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${index + 1}",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
                
                if (viewModel.chapterImages.isNotEmpty() && !isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(
                                "End of Chapter. Loading Next...",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        } else if (viewModel.chapterImages.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No images found.\nEnsure the chapter is fully loaded in the browser tab first.",
                    modifier = Modifier.padding(16.dp),
                    color = Color.White,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun ReaderStatsBar(
    loadedCount: Int,
    totalCount: Int,
    onPrevChapter: () -> Unit,
    onNextChapter: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF111111))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "PAGE: $loadedCount/$totalCount",
            color = Color.White,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onPrevChapter, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Prev", tint = Color.White)
        }
        IconButton(onClick = onNextChapter, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next", tint = Color.White)
        }
    }
}
