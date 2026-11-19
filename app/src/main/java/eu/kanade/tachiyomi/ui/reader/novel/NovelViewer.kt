package eu.kanade.tachiyomi.ui.reader.novel

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.setting.NovelTextSettings
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderThemedContent
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderTransitionPage
import eu.kanade.tachiyomi.ui.reader.viewer.Viewer
import eu.kanade.tachiyomi.ui.reader.viewer.transitionDownloadState
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonItems
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonPageTracker
import eu.kanade.tachiyomi.util.system.createReaderThemeContext
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import mihon.app.di.appGraph
import kotlin.math.min

/**
 * Shows a novel chapter as a scrolling column of [TextPage]s. Each paragraph is a page to the reader, so progress,
 * marking read and moving between chapters work as in the webtoon viewer, whose items and tracker it shares.
 */
class NovelViewer(val activity: ReaderActivity) : Viewer {

    private val scope = MainScope()
    private val settingsViewModel = activity.settingsViewModel
    private val downloadManager by lazy { activity.appGraph.downloadManager }
    private val readerThemedContext = activity.createReaderThemeContext()

    private val items = WebtoonItems()
    private val tracker = WebtoonPageTracker(
        activity,
        items,
        activity.appGraph.readerPreferences.readerHideThreshold.get().threshold,
    )
    private val listState = LazyListState()
    private var hasMovedToFirstPage = false

    /** Set while a transition button is held, so its tap doesn't also toggle the menu. */
    private var isButtonPressed = false

    @Composable
    override fun Content(modifier: Modifier) {
        val text by settingsViewModel.novelText.collectAsState()

        LaunchedEffect(listState) {
            snapshotFlow { listState.layoutInfo }.collect { info ->
                lastEndVisibleIndex(info.visibleItemsInfo, info.viewportStartOffset, info.viewportEndOffset)
                    ?.let(tracker::onScrolled)
            }
        }
        val scrollConnection = remember {
            object : NestedScrollConnection {
                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                    val info = listState.layoutInfo
                    tracker.onScrolledBy(
                        dy = -consumed.y,
                        firstVisible = listState.firstVisibleItemIndex,
                        lastEndVisible = lastEndVisibleIndex(
                            info.visibleItemsInfo,
                            info.viewportStartOffset,
                            info.viewportEndOffset,
                        ) ?: -1,
                    )
                    return Offset.Zero
                }
            }
        }

        ReaderThemedContent(readerThemedContext) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
                modifier = modifier
                    .nestedScroll(scrollConnection)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { position ->
                                if (isButtonPressed) return@detectTapGestures
                                activity.onViewerTap(
                                    Offset(position.x / size.width, position.y / size.height),
                                )
                            },
                        )
                    },
            ) {
                items(items.items, key = ::keyOf) { item ->
                    when (item) {
                        is TextPage -> NovelBlockItem(item, text)
                        is ChapterTransition -> TransitionItem(item)
                    }
                }
            }
        }
    }

    @Composable
    private fun NovelBlockItem(page: TextPage, settings: NovelTextSettings) {
        val color = MaterialTheme.colorScheme.onBackground
        val style = TextStyle(
            color = color,
            fontSize = settings.fontSize.sp,
            lineHeight = (settings.lineSpacingPercent / 100f).em,
        )
        when (val block = page.block) {
            is NovelBlock.Heading -> Text(
                text = page.text!!,
                style = style.copy(fontSize = (settings.fontSize * 1.3f).sp, fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(top = 24.dp, bottom = 12.dp),
            )
            is NovelBlock.Text -> Text(
                text = page.text!!,
                style = style,
                modifier = Modifier.padding(bottom = (settings.fontSize * 0.75f).dp),
            )
            is NovelBlock.Image -> AsyncImage(
                model = block.url,
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
            )
        }
    }

    @Composable
    private fun TransitionItem(transition: ChapterTransition) {
        val downloadState = remember(transition) {
            downloadManager.transitionDownloadState(
                transition,
                activity.viewModel.manga,
                activity.viewModel.source,
            )
        }
        LaunchedEffect(downloadState) { downloadState?.checkDisk() }
        ReaderTransitionPage(
            transition = transition,
            downloadState = downloadState,
            onRetry = activity::requestPreloadChapter,
            onButtonPressChange = { isButtonPressed = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 128.dp, horizontal = 12.dp),
            statusPadding = PaddingValues(vertical = 16.dp),
        )
    }

    override fun destroy() {
        scope.cancel()
    }

    override fun setChapters(chapters: ViewerChapters) {
        val forceTransition = settingsViewModel.general.value.alwaysShowChapterTransition ||
            tracker.currentPage is ChapterTransition
        items.setChapters(chapters, forceTransition)

        if (!hasMovedToFirstPage) {
            val pages = chapters.currChapter.pages ?: return
            hasMovedToFirstPage = true
            moveToPage(pages[min(chapters.currChapter.requestedPage, pages.lastIndex)])
        }
    }

    override fun moveToPage(page: ReaderPage) {
        val index = items.items.indexOf(page)
        if (index == -1) return
        scope.launch {
            listState.scrollToItem(index)
            tracker.onScrolled(index)
        }
    }

    override fun moveForward() = scrollByScreen(1)

    override fun moveBackward() = scrollByScreen(-1)

    private fun scrollByScreen(direction: Int) {
        val distance = listState.layoutInfo.viewportSize.height * 0.85f * direction
        scope.launch {
            if (settingsViewModel.general.value.usePageTransitions) {
                listState.animateScrollBy(distance)
            } else {
                listState.scrollBy(distance)
            }
        }
    }

    /** Keys must survive chapters being added above, so the list keeps its place, and fit in a Bundle. */
    private fun keyOf(item: Any): String = when (item) {
        is ReaderPage -> "${item.chapter.chapter.id}-${item.index}"
        is ChapterTransition.Prev -> "prev-${item.from.chapter.id}"
        is ChapterTransition.Next -> "next-${item.from.chapter.id}"
        else -> error("Unknown item ${item.javaClass}")
    }
}
