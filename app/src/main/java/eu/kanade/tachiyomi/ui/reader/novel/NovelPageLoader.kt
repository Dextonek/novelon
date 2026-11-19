package eu.kanade.tachiyomi.ui.reader.novel

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.reader.loader.PageLoader
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage

/**
 * Whether chapters of [source] are read as text. Every online source is a novel source since only Novelon
 * extensions load; the local source still serves image archives.
 */
fun readsAsText(source: Source?): Boolean = source is HttpSource

/** A paragraph, heading or image of a novel chapter, shown by the novel viewer as one page. */
class TextPage(index: Int, val block: NovelBlock, val text: AnnotatedString?) : ReaderPage(index) {
    init {
        status = Page.State.Ready
    }
}

/**
 * Splits the chapter text from [loadHtml] into [TextPage]s, resolving links against [baseUri]. [isLocal] when the
 * text comes from a download.
 */
internal class NovelPageLoader(
    private val baseUri: String,
    override var isLocal: Boolean,
    private val loadHtml: suspend () -> String,
) : PageLoader() {

    override suspend fun getPages(): List<ReaderPage> {
        return novelBlocks(loadHtml(), baseUri)
            .mapIndexed { index, block ->
                val text = when (block) {
                    is NovelBlock.Heading -> AnnotatedString.fromHtml(block.html)
                    is NovelBlock.Text -> AnnotatedString.fromHtml(block.html)
                    is NovelBlock.Image -> null
                }
                TextPage(index, block, text)
            }
    }
}
