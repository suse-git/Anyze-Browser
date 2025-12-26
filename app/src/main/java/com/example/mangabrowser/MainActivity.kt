package com.example.mangabrowser

import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mangabrowser.ui.BrowserScreen
import com.example.mangabrowser.ui.IndexedScreen
import com.example.mangabrowser.ui.ReaderScreen
import com.example.mangabrowser.ui.theme.MangaBrowserTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel by viewModels<BrowserViewModel>()

        setContent {
            MangaBrowserTheme(darkTheme = viewModel.isDarkTheme) {
                MainApp(viewModel)
            }
        }
    }
}

@Composable
fun MainApp(viewModel: BrowserViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val context = LocalContext.current
    
    // Foreground Visible WebView
    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.supportMultipleWindows()
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.domStorageEnabled = true
            
            addJavascriptInterface(WebAppInterface(viewModel), "Android")
            
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    view?.loadUrl("javascript:window.Android.processHTML('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>', '$url');")
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val newUrl = request?.url?.toString() ?: ""
                    
                    // Prevent ERR_UNKNOWN_URL_SCHEME by blocking non-http schemes (intent:, market:, mailto:, etc.)
                    if (!newUrl.startsWith("http://") && !newUrl.startsWith("https://")) {
                        return true
                    }

                    val isUserInitiated = request?.isForMainFrame == true && request.hasGesture()
                    
                    // 1. Block known Ad/Popup keywords (Aggressive)
                    val adKeywords = listOf(
                        "adsystem", "doubleclick", "syndication", "opt-out", "popup", 
                        "exoclick", "juicyads", "popads", "propeller", "tracker", 
                        "analytics", "banner", "ads", "betting", "casino"
                    )
                    if (adKeywords.any { newUrl.contains(it, ignoreCase = true) }) {
                        return true 
                    }

                    // 2. Block Redirects
                    if (view?.url != null) {
                        val currentHost = android.net.Uri.parse(view.url).host
                        val newHost = android.net.Uri.parse(newUrl).host
                        
                        // Block Automatic redirection to DIFFERENT host
                        if (currentHost != newHost && !isUserInitiated) {
                            val safeHosts = listOf("google.com", "bing.com", "duckduckgo.com", "brave.com")
                            if (safeHosts.none { newUrl.contains(it) }) {
                                return true 
                            }
                        }
                    }
                    return false
                }
            }
            
            webChromeClient = object : android.webkit.WebChromeClient() {
                override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?): Boolean {
                    // BLOCK: All popup windows (regardless of gesture for maximum safety)
                    return false 
                }
            }
        }
    }

    // Background Invisible WebView for Smart Search
    val backgroundWebView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.supportMultipleWindows()
            settings.javaScriptCanOpenWindowsAutomatically = false
            
            addJavascriptInterface(WebAppInterface(viewModel), "Android")
            
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    view?.loadUrl("javascript:window.Android.processBackgroundHTML('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');")
                }
                
                // Heavily restrict background webview to only search engines
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val url = request?.url?.toString() ?: ""
                    return !url.contains("brave.com") && !url.contains("google.com")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            if (currentRoute != "reader") {
                MangaBrowserSearchBar(
                     viewModel = viewModel,
                     onSearchTriggered = {
                         navController.navigate("indexed") {
                             popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                             launchSingleTop = true
                             restoreState = true
                         }
                     }
                )
            }
        },
        bottomBar = {
            if (currentRoute != "reader") {
                NavigationBar(modifier = Modifier.height(56.dp)) {
                    NavigationBarItem(
                        icon = { Text("🌐") },
                        label = { Text("Browser") },
                        selected = currentRoute == "browser",
                        onClick = {
                            navController.navigate("browser") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Text("📑") },
                        label = { Text("Smart Search") },
                        selected = currentRoute == "indexed",
                        onClick = {
                            navController.navigate("indexed") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Text("📖") },
                        label = { Text("Read Mode") },
                        selected = currentRoute == "reader",
                        onClick = {
                            if (viewModel.chapterImages.isEmpty() && viewModel.parsedMangaList.isNotEmpty()) {
                                viewModel.loadChapter(viewModel.parsedMangaList.last().link)
                            }
                            navController.navigate("reader")
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Background Indexing WebView
            AndroidView(
                modifier = Modifier.size(0.dp),
                factory = { backgroundWebView },
                update = { view ->
                    if (view.url != viewModel.backgroundSearchUrl && viewModel.backgroundSearchUrl.isNotEmpty()) {
                        view.loadUrl(viewModel.backgroundSearchUrl)
                    }
                }
            )

            if (currentRoute != "reader") {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { webView },
                    update = { view ->
                        if (view.url != viewModel.url) {
                            view.loadUrl(viewModel.url)
                        }
                    }
                )
            }

            NavHost(
                navController = navController,
                startDestination = "browser",
                modifier = Modifier.fillMaxSize()
            ) {
                composable("browser") {
                    BrowserScreen(
                        viewModel = viewModel,
                        onNavigateToIndexed = {
                            navController.navigate("indexed") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
                composable("indexed") {
                    IndexedScreen(
                        viewModel = viewModel,
                        onOpenBrowser = { url ->
                            viewModel.url = url
                            navController.navigate("browser") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onOpenReader = { chapterUrl ->
                            viewModel.loadChapter(chapterUrl)
                            navController.navigate("reader")
                        }
                    )
                }
                composable("reader") {
                    ReaderScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaBrowserSearchBar(
    viewModel: BrowserViewModel,
    onSearchTriggered: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp).background(MaterialTheme.colorScheme.surface),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(onClick = { viewModel.isSearchExpanded = true }) {
            val engineName = if (viewModel.selectedEngineIndex < viewModel.searchEngines.size)
                viewModel.searchEngines[viewModel.selectedEngineIndex].first
            else
                viewModel.customSearchEngines.getOrNull(viewModel.selectedEngineIndex - viewModel.searchEngines.size)?.first ?: "Err"
            Text(engineName)
        }
        DropdownMenu(expanded = viewModel.isSearchExpanded, onDismissRequest = { viewModel.isSearchExpanded = false }) {
            viewModel.searchEngines.forEachIndexed { index, pair ->
                DropdownMenuItem(
                    text = { Text(pair.first) },
                    onClick = {
                        viewModel.selectedEngineIndex = index
                        viewModel.isSearchExpanded = false
                    }
                )
            }
            viewModel.customSearchEngines.forEachIndexed { index, pair ->
                DropdownMenuItem(
                    text = { Text(pair.first) },
                    onClick = {
                        viewModel.selectedEngineIndex = viewModel.searchEngines.size + index
                        viewModel.isSearchExpanded = false
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("+ Add Custom") },
                onClick = {
                    viewModel.addCustomEngine("DuckDuckGo", "https://duckduckgo.com/?q=")
                    viewModel.isSearchExpanded = false
                }
            )
        }

        OutlinedTextField(
            value = viewModel.searchQuery,
            onValueChange = { viewModel.searchQuery = it },
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            placeholder = { Text("Search...") },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onSearch = { 
                    viewModel.performSearch(viewModel.searchQuery)
                    onSearchTriggered()
                }
            )
        )
        IconButton(onClick = { 
            viewModel.performSearch(viewModel.searchQuery)
            onSearchTriggered()
        }) {
            Icon(Icons.Default.Search, contentDescription = "Search")
        }
        IconButton(onClick = { viewModel.addCurrentToBookmarks() }) {
            Icon(Icons.Default.Star, contentDescription = "Bookmark", tint = MaterialTheme.colorScheme.primary)
        }
        IconButton(onClick = { viewModel.isDarkTheme = !viewModel.isDarkTheme }) {
            Text(text = if (viewModel.isDarkTheme) "☀" else "🌙")
        }
        IconButton(onClick = { viewModel.isShowingHome = true }) {
            Icon(Icons.Default.Home, contentDescription = "Home")
        }
    }
}

class WebAppInterface(private val viewModel: BrowserViewModel) {
    @android.webkit.JavascriptInterface
    fun processHTML(html: String, url: String) {
        viewModel.onPageLoaded(html, url)
    }

    @android.webkit.JavascriptInterface
    fun processBackgroundHTML(html: String) {
        viewModel.onBackgroundPageLoaded(html)
    }
}
