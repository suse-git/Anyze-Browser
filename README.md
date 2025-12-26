# Anyze Manga Browser

Developed by **Anyze Studio**.

A custom Android Web Browser specialized for reading Manga. It features a triple-tab interface:
1.  **Browser Tab**: A standard web browser where you can surf to your favorite manga sites. It automatically parses the current page.
2.  **Smart Search Tab**: Uses a background Brave Search indexer to find and list manga chapters related to your search query.
3.  **Read Mode**: A distraction-free "long strip" reader similar to popular web userscripts.

## Features
- **Search Engines**: Built-in Brave (default), Google, Bing, and Custom support.
- **Smart Indexing**: Background processing of search results to find readable content.
- **Read Mode**: Fullscreen vertical scrolling reader with automatic next-chapter loading.
- **Privacy**: Built-in popup and redirection blocker to silence aggressive advertisements.
- **Personalization**: Support for bookmarks and browsing history with automatic sorting.

## Setup & Run
1.  **Prerequisites**: Android Studio Koala or newer, JDK 17+.
2.  **Open**: Open this directory (`d:/AI/BrowserApp`) in Android Studio.
3.  **Sync**: Allow Gradle to sync dependencies.
4.  **Run**: Specify a Target Device (Emulator or Physical) and press Run.

## Architecture
- **Language**: Kotlin
- **UI**: Jetpack Compose (Material3)
- **Architecture**: MVVM
- **Key Dependencies**:
    - `jsoup`: For robust HTML parsing.
    - `coil`: For high-performance image loading with header support.
    - `navigation-compose`: For seamless tab and screen transitions.

## Usage
1.  Launch the App.
2.  **Search**: Enter a manga name in the search bar.
3.  **Browser**: View the search results in the native browser.
4.  **Smart Search**: Switch to this tab to see a clean, indexed list of chapters found in the background.
5.  **Read Mode**: Click any indexed chapter to enter the fullscreen reader.
