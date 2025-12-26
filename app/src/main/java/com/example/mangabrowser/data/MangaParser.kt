package com.example.mangabrowser.data

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.net.URI

object MangaParser {

    data class MangaEntry(
        val title: String,
        val link: String,
        val chapterNumber: String? = null,
        val imageUrl: String? = null,
        val source: String? = null,
        val updateTime: String? = null,
        val chapterVal: Float = 0f
    )

    fun parseSearchResults(html: String): List<MangaEntry> {
        val entries = mutableListOf<MangaEntry>()
        try {
            val doc = Jsoup.parse(html)
            val links = doc.select("a[href]")
            for (link in links) {
                val rawHref = link.attr("href")
                val text = link.text()

                if (text.length < 3) continue 
                if (rawHref.startsWith("javascript") || rawHref.startsWith("#")) continue

                if (text.contains("Chapter", ignoreCase = true) || 
                    text.contains("Vol", ignoreCase = true) || 
                    text.contains("Manga", ignoreCase = true) ||
                    rawHref.contains("chapter", ignoreCase = true)
                ) {
                    val chapterRegex = Regex("(?i)Chapter\\s+(\\d+(\\.\\d+)?)")
                    val match = chapterRegex.find(text)
                    val chapNumStr = match?.groupValues?.get(1)
                    val chapNumVal = chapNumStr?.toFloatOrNull() ?: 0f

                    var imgUrl: String? = null
                    val parent = link.parent()
                    val grandParent = parent?.parent()
                    
                    val img = parent?.selectFirst("img") ?: grandParent?.selectFirst("img")
                    if (img != null) {
                        imgUrl = getImageUrl(img, null)
                    }
                   
                    val source = try {
                        URI(rawHref).host ?: "Unknown"
                    } catch (e: Exception) { "Web" }

                    entries.add(MangaEntry(
                        title = text,
                        link = rawHref,
                        chapterNumber = chapNumStr,
                        imageUrl = imgUrl,
                        source = source,
                        updateTime = "Recently",
                        chapterVal = chapNumVal
                    ))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return entries.distinctBy { it.link }.sortedByDescending { it.chapterVal }
    }

    fun parseChapterImages(html: String, baseUrl: String? = null): List<String> {
        val imageUrls = mutableListOf<String>()
        try {
            val doc = Jsoup.parse(html)
            
            // 1. Selector-based (High Confidence)
            val containerSelectors = listOf(
                "#comic_page", "#img", ".reader-main img", "#manga-page", 
                "#image", ".prw a > img", "#viewer img", ".img-link > img",
                "img.scan", "#scanmr", "#divImage img", "#mainimage", 
                "#center_box > img", "#hq-page", ".comic_wraCon > img",
                "#page1", ".image img", "#eatmanga_image", "#eatmanga_image_big",
                ".img", ".page_chapter-2 img", "#current_page", "img.chapter-img",
                "img.CurImage", "#imgPage", "#mangaImg", ".picture", "#imageWrapper img",
                "#image_frame img", "#page-img", "#qTcms_pic", "img.open", "#thePicLink img",
                "#showchaptercontainer img", "#page > img", ".coverIssue img", "#mainImg",
                "#viewimg", "img.real", "#main_img", "tr td a img", "#mangaFile",
                "#images > img", "img.jsNext", "#TheImg", ".page-img", ".manga-image",
                ".reader-content img", ".reading-content img", "#reader-area img"
            )

            for (selector in containerSelectors) {
                val imgs = doc.select(selector)
                if (imgs.size > 0) { 
                     imgs.forEach { img -> getImageUrl(img, baseUrl)?.let { imageUrls.add(it) } }
                     if (imageUrls.size > 2) return imageUrls.distinct() // Quick return if good match
                }
            }

            // 2. Generic Heuristic (Dominant Container)
            fun isBadImage(img: Element, src: String): Boolean {
                 val badKeywords = listOf("logo", "banner", "icon", "avatar", "thumb", "cover", "social", "share", "comment", "footer", "header", "ad", "promo", "rec", "related", "prev", "next", "button", "pixel", "loader", "spinner", "analytics", "tracker")
                 if (badKeywords.any { src.contains(it, true) }) return true
                 
                 val alt = img.attr("alt")
                 if (badKeywords.any { alt.contains(it, true) }) return true
                 
                 val cls = img.className()
                 if (badKeywords.any { cls.contains(it, true) }) return true
                 
                 // Size check
                 val w = img.attr("width").toIntOrNull()
                 val h = img.attr("height").toIntOrNull()
                 if (w != null && w < 100) return true
                 if (h != null && h < 100) return true
                 
                 return false
            }

            // Find the container with the MOST large/valid images
            val containers = doc.select("div, section, article, main, p")
            var bestContainer: Element? = null
            var maxValidImages = 0
            
            for (container in containers) {
                // Heuristic: Don't check massive root containers like 'body' repeatedly, prefer deeper nodes
                if (container.tagName() == "div" || container.tagName() == "p") {
                    val imgs = container.select("> img") // Direct children first
                    if (imgs.size < 2) continue
                    
                    val validImgs = imgs.filter { 
                         val src = getImageUrl(it, baseUrl)
                         src != null && !isBadImage(it, src)
                    }
                    
                    if (validImgs.size > maxValidImages) {
                        maxValidImages = validImgs.size
                        bestContainer = container
                    }
                }
            }
            
            if (bestContainer != null && maxValidImages >= 3) {
                 bestContainer.select("> img").forEach { img ->
                     val src = getImageUrl(img, baseUrl)
                     if (src != null && !isBadImage(img, src)) {
                         imageUrls.add(src)
                     }
                 }
            } 
            
            // 3. Last Resort: All valid images on page
            if (imageUrls.isEmpty()) {
                doc.select("img").forEach { img ->
                     val src = getImageUrl(img, baseUrl)
                     if (src != null && !isBadImage(img, src)) {
                         imageUrls.add(src)
                     }
                }
            }
            
            // 4. Script Fallback
             if (imageUrls.size < 2) {
                val scriptTags = doc.select("script")
                for (script in scriptTags) {
                    val content = script.html()
                    val arrayMatch = Regex("""["'](https?://[^"']+\.(?:jpg|jpeg|png|webp|gif)[^"']*)["']""").findAll(content)
                    for (match in arrayMatch) {
                        val url = match.groupValues[1]
                        if (!url.contains("logo") && !url.contains("icon") && !url.contains("thumb")) {
                            imageUrls.add(url)
                        }
                    }
                }
             }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return imageUrls.distinct()
    }

    fun parseNextChapterUrl(html: String, baseUrl: String): String? {
        try {
            val doc = Jsoup.parse(html)
            val baseUri = URI(baseUrl)
            
            // Common selectors for "Next" links
            val nextSelectors = listOf(
                ".next a", "a.next", "a:contains(Next)", "a:contains(Next Chapter)",
                ".pager-list-left > span > a:last-child", ".nxt", "#next_chapter",
                ".next > a", ".next_chapter", ".lastSlider_nextButton",
                "a[title*='Next']", "a[rel='next']"
            )

            for (selector in nextSelectors) {
                val element = doc.selectFirst(selector)
                if (element != null) {
                    val href = element.attr("href")
                    if (href.isNotEmpty() && !href.startsWith("javascript") && href != "#") {
                        return baseUri.resolve(href).toString()
                    }
                }
            }

            // Heuristic: look for links containing "chapter" that might be the next one
            // This is harder without knowing the current chapter number, but we can try
            // to find a chapter list and pick the one after the current one.
            // For now, we'll stick to explicit "Next" buttons as found in the .js
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun getImageUrl(img: Element, baseUrl: String?): String? {
        val attributes = listOf(
            "data-src", "data-original", "data-lazy-src", "data-lazy", 
            "data-src-optimized", "data-actual-src", "data-srcset", "src"
        )
        
        var src = ""
        for (attr in attributes) {
            val value = img.attr(attr).trim()
            if (value.isNotEmpty() && !value.startsWith("data:image")) {
                src = value
                break
            }
        }
        
        if (src.isEmpty()) return null
        
        if (src.contains(" ")) {
            src = src.split(" ").first()
        }

        if (src.startsWith("//")) {
            src = "https:$src"
        } 
        else if (!src.startsWith("http") && baseUrl != null) {
            try {
                val uri = URI(baseUrl)
                if (src.startsWith("/")) {
                    val root = "${uri.scheme}://${uri.host}"
                    src = root + src
                } else {
                    src = uri.resolve(src).toString()
                }
            } catch (e: Exception) {
                // ignore
            }
        }
        
        return src
    }
}
