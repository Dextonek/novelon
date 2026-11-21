package eu.kanade.tachiyomi.ui.reader.novel

import androidx.compose.foundation.lazy.LazyListItemInfo

/** The index of the last visible item whose end is on screen, or of an item reaching past the top. */
fun lastEndVisibleIndex(visibleItems: List<LazyListItemInfo>, viewportStart: Int, viewportEnd: Int): Int? {
    return visibleItems.lastOrNull { it.offset + it.size <= viewportEnd || it.offset < viewportStart }?.index
}

/**
 * The index of the item the novel viewer reports as the current page: the one at the top of the screen, so a reopened
 * chapter starts where the reader left it. Once the end of a chapter's last item ([isChapterEnd]) is on screen, that
 * item is reported instead, since a short last paragraph never reaches the top and the chapter would stay unread.
 */
fun topVisibleIndex(
    visibleItems: List<LazyListItemInfo>,
    viewportEnd: Int,
    isChapterEnd: (Int) -> Boolean,
): Int? {
    val chapterEnd = visibleItems.firstOrNull { isChapterEnd(it.index) && it.offset + it.size in 1..viewportEnd }
    // Offset 0 is the top of the content, below the list's top padding.
    return (chapterEnd ?: visibleItems.firstOrNull { it.offset + it.size > 0 })?.index
}
