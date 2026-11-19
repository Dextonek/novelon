package mihon.source.gutenberg

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import java.util.IdentityHashMap

/**
 * Splits a Project Gutenberg HTML book into chapters at its headings.
 *
 * Tables of contents aren't used because they also link to page numbers and illustrations.
 */
internal class GutenbergBook(document: Document) {

    data class Chapter(val key: String, val name: String)

    private class Section(val key: String, val name: String, val nodes: List<Node>)

    private val body: Element = document.body()
    private val sections: List<Section>

    init {
        removeBoilerplate()
        body.select("img[src]").forEach { it.attr("src", it.absUrl("src")) }
        sections = buildSections()
    }

    val chapters: List<Chapter>
        get() = sections.map { Chapter(it.key, it.name) }

    /** The HTML of the chapter with [key], or null if the book has no such chapter. */
    fun chapterHtml(key: String): String? {
        val section = sections.find { it.key == key } ?: return null
        return section.nodes.joinToString("") { it.outerHtml() }
    }

    private fun removeBoilerplate() {
        body.select("#pg-header, #pg-footer, .pagenum").remove()
        // Older files mark the licence with these lines instead of sections.
        markerElement("*** START OF")?.let { start ->
            start.siblingNodes().filter { it.siblingIndex() < start.siblingIndex() }.forEach { it.remove() }
            start.remove()
        }
        markerElement("*** END OF")?.let { end ->
            end.siblingNodes().filter { it.siblingIndex() > end.siblingIndex() }.forEach { it.remove() }
            end.remove()
        }
    }

    private fun markerElement(marker: String): Element? {
        val element = body.getElementsContainingOwnText(marker).firstOrNull() ?: return null
        return generateSequence(element) { it.parent() }.first { it.parent() == body || it.parent() == null }
    }

    private fun buildSections(): List<Section> {
        val level = chapterLevel() ?: return listOf(wholeBook())
        // h1 is the book title, so only levels between it and the chapter level name groups.
        val groupLevels = HEADING_TAGS.takeWhile { it != level }.drop(1)

        val headings = mutableListOf<Element>()
        val groupBefore = IdentityHashMap<Element, String>()
        var lastGroup: String? = null
        body.select(HEADING_TAGS.joinToString()).forEach { heading ->
            val text = headingText(heading)
            if (CONTENTS_REGEX.matches(text)) return@forEach
            when (heading.tagName()) {
                in groupLevels -> lastGroup = text.ifEmpty { null }
                level -> {
                    headings.add(heading)
                    lastGroup?.let { groupBefore[heading] = it }
                }
            }
        }
        val blocks = headings.map { blockOf(it, headings) }

        var group: String? = null
        val sections = mutableListOf<Section>()
        headings.forEachIndexed { index, heading ->
            val nodes = nodesBetween(blocks[index], blocks.getOrNull(index + 1))
            val name = headingName(heading, index)
            // An empty heading such as "BOOK ONE" names the chapters after it instead.
            if (nodes.none { it.hasTextBesides(heading) }) {
                group = name
                return@forEachIndexed
            }
            groupBefore[heading]?.let { group = it }
            val fullName = group?.let { "$it · $name" } ?: name
            sections += Section(keyOf(heading, index), fullName, nodes)
        }
        return sections.ifEmpty { listOf(wholeBook()) }
    }

    private fun wholeBook(): Section = Section(WHOLE_BOOK_KEY, "Full text", body.childNodes().toList())

    /** The heading tag used for chapters: the most frequent of h1 to h3, if it occurs at least twice. */
    private fun chapterLevel(): String? {
        return HEADING_TAGS
            .map { it to body.getElementsByTag(it).size }
            .filter { it.second >= 2 }
            .maxByOrNull { it.second }
            ?.first
    }

    /** The outermost ancestor of [heading] below the body that holds no other chapter heading. */
    private fun blockOf(heading: Element, headings: List<Element>): Element {
        var block = heading
        while (true) {
            val parent = block.parent() ?: break
            if (parent == body || headings.any { it != heading && parent.isAncestorOf(it) }) break
            block = parent
        }
        return block
    }

    /** [start] and every node after it in document order, up to the node holding [end]. */
    private fun nodesBetween(start: Element, end: Element?): List<Node> {
        val nodes = mutableListOf<Node>(start)
        var current: Node = start
        while (current != body) {
            var sibling = current.nextSibling()
            while (sibling != null) {
                if (end != null && (sibling == end || (sibling is Element && sibling.isAncestorOf(end)))) {
                    return nodes
                }
                nodes += sibling
                sibling = sibling.nextSibling()
            }
            current = current.parent() ?: break
        }
        return nodes
    }

    private fun headingName(heading: Element, index: Int): String {
        headingText(heading).takeIf { it.isNotEmpty() }?.let { return it }
        anchorId(heading)?.let { id -> return id.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() } }
        return "Section ${index + 1}"
    }

    private fun keyOf(heading: Element, index: Int): String = anchorId(heading) ?: "n$index"

    companion object {
        const val WHOLE_BOOK_KEY = "all"

        private val HEADING_TAGS = listOf("h1", "h2", "h3")
        private val CONTENTS_REGEX = Regex("(?i)(table of )?contents\\.?")

        private fun headingText(heading: Element): String {
            val copy = heading.clone()
            copy.select(".caption, img").remove()
            return copy.text().trim()
        }

        private fun anchorId(heading: Element): String? {
            heading.id().takeIf { it.isNotEmpty() }?.let { return it }
            val anchor = heading.selectFirst("[id], a[name]")
                ?: heading.previousElementSibling()?.takeIf { it.tagName() == "a" && it.children().isEmpty() }
            return anchor?.let { it.id().ifEmpty { it.attr("name") } }?.takeIf { it.isNotEmpty() }
        }

        private fun Element.isAncestorOf(other: Element): Boolean = other.parents().any { it == this }

        private fun Node.hasTextBesides(heading: Element): Boolean {
            if (this == heading) return false
            if (this !is Element) return this is TextNode && !isBlank
            val copy = clone()
            copy.select("${heading.tagName()}, img").remove()
            return copy.text().isNotBlank()
        }
    }
}
