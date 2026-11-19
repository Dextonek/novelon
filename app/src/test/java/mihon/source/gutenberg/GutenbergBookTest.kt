package mihon.source.gutenberg

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.jsoup.Jsoup
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.CONCURRENT)
class GutenbergBookTest {

    private fun book(body: String) = GutenbergBook(
        Jsoup.parse("<html><body>$body</body></html>", "https://www.gutenberg.org/files/1/1-h/1-h.htm"),
    )

    @Test
    fun `splits older files at headings between the licence markers`() {
        val book = book(
            """
            <pre>Licence header</pre>
            <div>*** START OF THE PROJECT GUTENBERG EBOOK 1 ***</div>
            <h1>A Novel</h1>
            <h2>Contents</h2>
            <p class="toc"><a href="#c1">I</a> <a href="#c2">II</a></p>
            <h2><a id="c1"></a>CHAPTER I.<span class="pagenum"><a id="page_1">{1}</a></span></h2>
            <p>First text.</p>
            <h2><a id="c2"></a>CHAPTER II.</h2>
            <p>Second text.</p>
            <div>*** END OF THE PROJECT GUTENBERG EBOOK 1 ***</div>
            <pre>Licence footer</pre>
            """,
        )

        book.chapters shouldContainExactly listOf(
            GutenbergBook.Chapter("c1", "CHAPTER I."),
            GutenbergBook.Chapter("c2", "CHAPTER II."),
        )
        val first = book.chapterHtml("c1")!!
        first shouldContain "First text."
        first shouldNotContain "Second text."
        first shouldNotContain "{1}"
        book.chapterHtml("c2")!! shouldNotContain "Licence footer"
    }

    @Test
    fun `takes whole chapter blocks from newer files`() {
        val book = book(
            """
            <section id="pg-header"><h2>Licence</h2><p>Licence header</p></section>
            <div class="chapter"><h2 id="one">One</h2><p>First text.</p></div>
            <div class="chapter"><h2 id="two">Two</h2><p>Second text.</p></div>
            <section id="pg-footer"><h2>Licence</h2><p>Licence footer</p></section>
            """,
        )

        book.chapters.map { it.key } shouldContainExactly listOf("one", "two")
        book.chapterHtml("one")!! shouldContain "<div class=\"chapter\">"
        book.chapterHtml("two")!! shouldNotContain "Licence footer"
    }

    @Test
    fun `names scenes after their act`() {
        val book = book(
            """
            <h2>ACT I</h2>
            <h3 id="s11">SCENE I. A platform.</h3><p>Text.</p>
            <h3 id="s12">SCENE II. A room.</h3><p>Text.</p>
            <h2>ACT II</h2>
            <h3 id="s21">SCENE I. A street.</h3><p>Text.</p>
            """,
        )

        book.chapters.map { it.name } shouldContainExactly listOf(
            "ACT I · SCENE I. A platform.",
            "ACT I · SCENE II. A room.",
            "ACT II · SCENE I. A street.",
        )
    }

    @Test
    fun `uses empty part headings as group names`() {
        val book = book(
            """
            <h2>BOOK ONE</h2>
            <h2 id="b1c1">CHAPTER I</h2><p>Text.</p>
            <h2>BOOK TWO</h2>
            <h2 id="b2c1">CHAPTER I</h2><p>Text.</p>
            """,
        )

        book.chapters shouldContainExactly listOf(
            GutenbergBook.Chapter("b1c1", "BOOK ONE · CHAPTER I"),
            GutenbergBook.Chapter("b2c1", "BOOK TWO · CHAPTER I"),
        )
    }

    @Test
    fun `names image-only headings after their anchor`() {
        val book = book(
            """
            <h2><a id="LIST_OF_ILLUSTRATIONS"></a><img src="images/list.jpg" alt=""></h2><p>Plates.</p>
            <h2><a id="c1"></a>CHAPTER I.</h2><p>Text.</p>
            """,
        )

        book.chapters.first().name shouldBe "List of illustrations"
        book.chapterHtml("LIST_OF_ILLUSTRATIONS")!! shouldContain
            "https://www.gutenberg.org/files/1/1-h/images/list.jpg"
    }

    @Test
    fun `keeps a book without chapter headings whole`() {
        val book = book("<h1>A Poem</h1><p>Only text.</p>")

        book.chapters shouldContainExactly listOf(GutenbergBook.Chapter(GutenbergBook.WHOLE_BOOK_KEY, "Full text"))
        book.chapterHtml(GutenbergBook.WHOLE_BOOK_KEY)!! shouldContain "Only text."
    }
}
