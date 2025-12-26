package com.example.mangabrowser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.mangabrowser.BrowserViewModel

@Composable
fun IndexedScreen(
    viewModel: BrowserViewModel,
    onOpenBrowser: (String) -> Unit,
    onOpenReader: (String) -> Unit
) {
    Column(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
    ) {
        // Optional: Filter/Search bar matching screenshot could go here, but omitted to keep scope focused on list
        
        if (viewModel.parsedMangaList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No chapters found. \nBrowse web to find manga.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(viewModel.parsedMangaList) { entry ->
                MangaItemView(
                    entry = entry, 
                    onBrowserClick = { onOpenBrowser(entry.link) },
                    onReaderClick = { onOpenReader(entry.link) }
                )
            }
        }
    }
}

@Composable
fun MangaItemView(
    entry: com.example.mangabrowser.data.MangaParser.MangaEntry,
    onBrowserClick: () -> Unit,
    onReaderClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onBrowserClick) // Clicking the row opens Web
            .background(MaterialTheme.colorScheme.surface), 
        verticalAlignment = Alignment.Top
    ) {
        // Cover Image
        Card(
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.size(width = 80.dp, height = 110.dp)
        ) {
            if (entry.imageUrl != null) {
                AsyncImage(
                    model = entry.imageUrl,
                    contentDescription = "Cover",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Placeholder
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.DarkGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text("IMG", color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Info Column
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            val chapterText = entry.chapterNumber?.let { "Chapter $it" } ?: "Unknown Chapter"
            
            // Chapter Link (Reader Mode)
            androidx.compose.material3.Surface(
                onClick = onReaderClick,
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                 Text(
                    text = "Read $chapterText",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(2.dp))
            
            val timeText = entry.updateTime ?: "Unknown"
            Text(
                text = "Update Time: $timeText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF333333), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Manga",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = entry.source ?: "Web",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
