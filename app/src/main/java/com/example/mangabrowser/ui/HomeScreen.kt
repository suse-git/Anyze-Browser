package com.example.mangabrowser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mangabrowser.BrowserViewModel

@Composable
fun HomeScreen(
    viewModel: BrowserViewModel,
    onNavigate: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var bookmarkTitle by remember { mutableStateOf("") }
    var bookmarkUrl by remember { mutableStateOf("") }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Add Bookmark") },
            text = {
                Column {
                    OutlinedTextField(value = bookmarkTitle, onValueChange = { bookmarkTitle = it }, label = { Text("Title") })
                    OutlinedTextField(value = bookmarkUrl, onValueChange = { bookmarkUrl = it }, label = { Text("URL") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (bookmarkTitle.isNotEmpty() && bookmarkUrl.isNotEmpty()) {
                        viewModel.addBookmark(bookmarkTitle, bookmarkUrl)
                        showDialog = false
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Welcome Back!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            // Bookmarks Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Bookmarks (Top 4)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { 
                    bookmarkTitle = ""
                    bookmarkUrl = "https://"
                    showDialog = true 
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Bookmark")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Bookmarks List (Limited to 4)
        val topBookmarks = viewModel.bookmarks.take(4)
        items(topBookmarks) { bookmark ->
            Card(
                onClick = {
                    viewModel.isShowingHome = false
                    onNavigate(bookmark.url)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = bookmark.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(text = bookmark.url, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))

            // Recent History Header
            Text(
                text = "Recent History (Last 6)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            if (viewModel.history.isEmpty()) {
                 Text("No recent history", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column {
                        // History List (Limited to 6)
                        val lastHistory = viewModel.history.take(6)
                        lastHistory.forEach { item ->
                            HistoryRow(item) {
                                viewModel.isShowingHome = false
                                onNavigate(item.url)
                            }
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryRow(item: BrowserViewModel.HistoryItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("🕒", fontSize = 16.sp) 
        Spacer(modifier = Modifier.width(12.dp))
        Column {
             Text(text = item.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
             Text(text = item.url, style = MaterialTheme.typography.labelSmall, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
