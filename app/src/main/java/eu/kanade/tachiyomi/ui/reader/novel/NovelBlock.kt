package eu.kanade.tachiyomi.ui.reader.novel

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/** One readable block of a chapter. */
sealed interface NovelBlock {
    /** A heading, kept apart so it can be drawn larger. */
    data class Heading(val html: String) : NovelBlock

    /** A paragraph, list, table or poem, as HTML. */
    data class Text(val html: String) : NovelBlock

    data class Image(val url: String) : NovelBlock
}

/**
 * Splits chapter [html] into the blocks the reader shows one by one. Containers holding other blocks, like a
 * Gutenberg `div.chapter`, are flattened so that no block grows to a whole chapter.
 */
fun novelBlocks(html: String, baseUri: String = ""): List<NovelBlock> {
    val body = Jsoup.parseBodyFragment(html, baseUri).body()
    val blocks = mutableListOf<NovelBlock>()
    collect(body, blocks)
    return blocks
}

private fun collect(parent: Element, blocks: MutableList<NovelBlock>) {
    val loose = StringBuilder()
    fun flushLoose() {
        if (loose.isNotBlank()) blocks += NovelBlock.Text("<p>${loose.toString().trim()}</p>")
        loose.clear()
    }

    for (node in parent.childNodes()) {
        when {
            node is Element && node.tagName() in SKIPPED_TAGS -> Unit
            node is Element && node.isBlock() -> {
                flushLoose()
                block(node, blocks)
            }
            node is Element && node.tagName() == "img" -> {
                flushLoose()
                node.imageUrl()?.let { blocks += NovelBlock.Image(it) }
            }
            node is TextNode -> loose.append(node.outerHtml())
            node is Element -> loose.append(node.outerHtml())
            else -> Unit
        }
    }
    flushLoose()
}

private fun block(element: Element, blocks: MutableList<NovelBlock>) {
    when {
        element.tagName() in HEADING_TAGS -> {
            element.select("img").forEach { img -> img.imageUrl()?.let { blocks += NovelBlock.Image(it) } }
            element.select("img").remove()
            if (element.hasText()) blocks += NovelBlock.Heading(element.html())
        }
        element.tagName() in CONTAINER_TAGS && element.childNodes().any { it.isBlockOrImage() } ->
            collect(element, blocks)
        element.tagName() == "hr" -> Unit
        !element.hasText() -> element.select("img").forEach { img ->
            img.imageUrl()?.let { blocks += NovelBlock.Image(it) }
        }
        else -> blocks += NovelBlock.Text(element.outerHtml())
    }
}

private fun Node.isBlockOrImage(): Boolean = this is Element && (isBlock() || tagName() == "img")

private fun Element.isBlock(): Boolean = tagName() in BLOCK_TAGS

private fun Element.imageUrl(): String? = absUrl("src").ifEmpty { attr("src") }.takeIf { it.isNotEmpty() }

private val HEADING_TAGS = setOf("h1", "h2", "h3", "h4", "h5", "h6")

/** Blocks that are split further when they hold other blocks. */
private val CONTAINER_TAGS = setOf("div", "section", "article", "main", "blockquote", "figure", "center")

private val BLOCK_TAGS = HEADING_TAGS + CONTAINER_TAGS + setOf(
    "p", "pre", "ul", "ol", "dl", "table", "hr", "address", "figcaption", "header", "footer", "aside", "nav",
)

private val SKIPPED_TAGS = setOf("script", "style", "noscript", "template")
