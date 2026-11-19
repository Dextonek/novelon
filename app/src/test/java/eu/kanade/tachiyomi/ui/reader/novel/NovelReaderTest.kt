package eu.kanade.tachiyomi.ui.reader.novel

import androidx.compose.foundation.lazy.LazyListItemInfo
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.CONCURRENT)
class NovelReaderTest {

    @Test
    fun `flattens chapter containers into paragraphs`() {
        val blocks = novelBlocks(
            """
            <div class="chapter">
              <h2><a id="c1"></a>CHAPTER I.</h2>
              <p>First.</p>
              <div class="poem"><div class="stanza"><span>Line one</span><br><span>Line two</span></div></div>
              <p>Second.</p>
            </div>
            """,
        )

        blocks.map { it::class.simpleName } shouldContainExactly listOf("Heading", "Text", "Text", "Text")
        (blocks[1] as NovelBlock.Text).html shouldBe "<p>First.</p>"
    }

    @Test
    fun `pulls images out of headings and paragraphs`() {
        val blocks = novelBlocks(
            """<h2><img src="images/a.jpg">CHAPTER IX.</h2><p><img src="images/b.png"></p>""",
            baseUri = "https://example.org/book/",
        )

        blocks shouldContainExactly listOf(
            NovelBlock.Image("https://example.org/book/images/a.jpg"),
            NovelBlock.Heading("CHAPTER IX."),
            NovelBlock.Image("https://example.org/book/images/b.png"),
        )
    }

    @Test
    fun `wraps loose text into paragraphs`() {
        val blocks = novelBlocks("Loose <i>text</i><p>Para</p>tail")

        blocks shouldContainExactly listOf(
            NovelBlock.Text("<p>Loose <i>text</i></p>"),
            NovelBlock.Text("<p>Para</p>"),
            NovelBlock.Text("<p>tail</p>"),
        )
    }

    @Test
    fun `reports the last paragraph whose end is visible`() {
        val items = listOf(item(0, offset = -50, size = 100), item(1, offset = 50, size = 400), item(2, 450, 200))

        lastEndVisibleIndex(items, viewportStart = 0, viewportEnd = 500) shouldBe 1
        lastEndVisibleIndex(items.take(2) + item(2, 450, 40), viewportStart = 0, viewportEnd = 500) shouldBe 2
    }

    @Test
    fun `reports a paragraph taller than the screen`() {
        val items = listOf(item(3, offset = -200, size = 1000))

        lastEndVisibleIndex(items, viewportStart = 0, viewportEnd = 500) shouldBe 3
    }

    private fun item(index: Int, offset: Int, size: Int) = object : LazyListItemInfo {
        override val index = index
        override val key: Any = index
        override val offset = offset
        override val size = size
    }
}
