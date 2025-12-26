package com.example.mangabrowser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mangabrowser.data.MangaParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BrowserViewModel(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application) {
    var url by mutableStateOf("https://www.brave.com")
    var isDarkTheme by mutableStateOf(false)
    
    // Search Engines
    val searchEngines = listOf(
        "Brave" to "https://search.brave.com/search?q=",
        "Bing" to "https://www.bing.com/search?q=",
        "Google" to "https://www.google.com/search?q="
    )
    
    var customSearchEngines = mutableStateListOf<Pair<String, String>>()
    var selectedEngineIndex by mutableStateOf(0)

    // Navigation State
    var isShowingHome by mutableStateOf(true)
    
    // User Data
    var history = mutableStateListOf<HistoryItem>()
    var bookmarks = mutableStateListOf<BookmarkItem>()

    data class HistoryItem(val title: String, val url: String)
    data class BookmarkItem(val title: String, val url: String)

    // Data
    var pageSource by mutableStateOf("")
    var parsedMangaList = mutableStateListOf<MangaParser.MangaEntry>()
    var chapterImages = mutableStateListOf<String>()
    var currentChapterUrl by mutableStateOf("")
    var nextChapterUrl by mutableStateOf<String?>(null)

    // UI State for SearchBar (Hoisted)
    var searchQuery by mutableStateOf("")
    var isSearchExpanded by mutableStateOf(false)

    // Background Indexing
    var backgroundSearchUrl by mutableStateOf("")

    init {
        loadData()
        
        // Default Bookmarks (only if empty)
        if (bookmarks.isEmpty()) {
            addBookmark("MangaRead", "https://mangaread.org")
            addBookmark("Asura Comics", "https://asuracomic.net")
        }
    }
    
    private fun loadData() {
        try {
            val context = getApplication<android.app.Application>().applicationContext
            val historyFile = java.io.File(context.filesDir, "history.json")
            if (historyFile.exists()) {
                val jsonStr = historyFile.readText()
                val jsonArray = org.json.JSONArray(jsonStr)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    history.add(HistoryItem(obj.getString("title"), obj.getString("url")))
                }
            }
            
            val bookmarksFile = java.io.File(context.filesDir, "bookmarks.json")
            if (bookmarksFile.exists()) {
                val jsonStr = bookmarksFile.readText()
                val jsonArray = org.json.JSONArray(jsonStr)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    bookmarks.add(BookmarkItem(obj.getString("title"), obj.getString("url")))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun saveData() {
        try {
            val context = getApplication<android.app.Application>().applicationContext
            
            val historyArray = org.json.JSONArray()
            history.forEach { 
                val obj = org.json.JSONObject()
                obj.put("title", it.title)
                obj.put("url", it.url)
                historyArray.put(obj)
            }
            java.io.File(context.filesDir, "history.json").writeText(historyArray.toString())
            
            val bookmarksArray = org.json.JSONArray()
            bookmarks.forEach {
                val obj = org.json.JSONObject()
                obj.put("title", it.title)
                obj.put("url", it.url)
                bookmarksArray.put(obj)
            }
            java.io.File(context.filesDir, "bookmarks.json").writeText(bookmarksArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun onPageLoaded(html: String, currentUrl: String) {
        pageSource = html
        url = currentUrl // Update current url if changed by browser nav
        if (currentUrl.isNotEmpty() && currentUrl != "about:blank") {
             // Add to history (avoid duplicates at top)
             if (history.isEmpty() || history.first().url != currentUrl) {
                 // Remove if exists elsewhere to move to top (replace with new)
                 history.removeAll { it.url == currentUrl }
                 history.add(0, HistoryItem("Visited Site", currentUrl))
                 if (history.size > 20) history.removeLast()
                 saveData()
             }
        }
        
        // Automatically parse images from the current page with baseUrl
        val images = MangaParser.parseChapterImages(html, currentUrl)
        // If we found a significant number of images, assume it's a chapter and update
        if (images.size > 2) {
            chapterImages.clear()
            chapterImages.addAll(images)
            currentChapterUrl = currentUrl
            // Also look for next chapter URL on the page
            nextChapterUrl = MangaParser.parseNextChapterUrl(html, currentUrl)
        }
    }

    fun onBackgroundPageLoaded(html: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val entries = MangaParser.parseSearchResults(html)
            val validEntries = entries.filter { it.chapterNumber != null || it.title.contains("Chapter", true) }
            
            if (validEntries.isNotEmpty()) {
                launch(Dispatchers.Main) {
                    parsedMangaList.clear()
                    parsedMangaList.addAll(validEntries)
                }
            }
        }
    }

    fun loadChapter(chapterUrl: String) {
        url = chapterUrl
        // Reset chapter images if loading a new one, so ReaderScreen knows to fetch
        if (currentChapterUrl != chapterUrl) {
            chapterImages.clear()
            currentChapterUrl = ""
            nextChapterUrl = null
        }
    }

    fun loadNextChapter() {
        // Priority 1: Use the 'Next Chapter' link found on the current page
        nextChapterUrl?.let {
            loadChapter(it)
            return
        }

        // Priority 2: Use the indexed list (search results)
        val currentIndex = parsedMangaList.indexOfFirst { it.link == url || it.link == currentChapterUrl }
        if (currentIndex > 0) {
            val nextEntry = parsedMangaList[currentIndex - 1]
            loadChapter(nextEntry.link)
        }
    }
    
    fun performSearch(query: String) {
        isShowingHome = false
        
        // Background search for Smart Search
        backgroundSearchUrl = "https://search.brave.com/search?q=" + java.net.URLEncoder.encode("$query manga chapters free", "UTF-8")

        val engine = if (selectedEngineIndex < searchEngines.size) {
            searchEngines[selectedEngineIndex]
        } else {
            customSearchEngines[selectedEngineIndex - searchEngines.size]
        }
        val modifiedQuery = "$query"
        url = engine.second + java.net.URLEncoder.encode(modifiedQuery, "UTF-8")
    }

    fun addCustomEngine(name: String, baseUrl: String) {
        customSearchEngines.add(name to baseUrl)
    }

    fun addBookmark(title: String, url: String) {
        // If already bookmarked, move to top (replace with new entry)
        bookmarks.removeAll { it.url == url }
        bookmarks.add(0, BookmarkItem(title, url))
        saveData()
    }
    
    fun addCurrentToBookmarks() {
        if (url.isNotEmpty() && url != "about:blank") {
            addBookmark("Bookmarked Site", url)
        }
    }
}
