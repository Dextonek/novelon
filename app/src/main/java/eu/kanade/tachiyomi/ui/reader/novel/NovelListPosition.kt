package eu.kanade.tachiyomi.ui.reader.novel

import androidx.compose.foundation.lazy.LazyListItemInfo

/**
 * The index of the last visible item whose end is on screen, or of an item reaching past the top, as the webtoon
 * viewer reports pages. A short last paragraph is never first on screen, so reporting the first visible item would
 * never mark a chapter read.
 */
fun lastEndVisibleIndex(visibleItems: List<LazyListItemInfo>, viewportStart: Int, viewportEnd: Int): Int? {
    return visibleItems.lastOrNull { it.offset + it.size <= viewportEnd || it.offset < viewportStart }?.index
}
