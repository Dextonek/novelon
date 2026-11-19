package mihon.source.gutenberg

import eu.kanade.tachiyomi.source.model.SManga
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.jsoup.Jsoup
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.CONCURRENT)
class GutendexTest {

    private fun fixture(name: String) = javaClass.getResource("/gutendex/$name")!!.readText()

    @Test
    fun `maps a popular page of the real API`() {
        val page = GutendexPage.parse(fixture("popular.json")).toMangasPage()

        page.mangas shouldHaveSize 32
        page.hasNextPage shouldBe true
        val first = page.mangas.first()
        first.url shouldBe "/ebooks/1342"
        first.title shouldBe "Pride and Prejudice"
        first.author shouldBe "Jane Austen"
        first.status shouldBe SManga.COMPLETED
        first.thumbnail_url shouldBe "https://gutenberg.pglaf.org/cache/epub/1342/pg1342.cover.medium.jpg"
        first.genre!!.split(", ") shouldContainExactly listOf(
            "Best Books Ever Listings",
            "British Literature",
            "Classics of Literature",
            "Novels",
            "Romance",
            "Harvard Classics",
        )
    }

    @Test
    fun `knows the last search page`() {
        val page = GutendexPage.parse(fixture("search.json")).toMangasPage()

        page.mangas shouldHaveSize 6
        page.hasNextPage shouldBe false
    }

    @Test
    fun `parses a single book`() {
        val book = GutendexBook.parse("""{"id":84,"title":"Frankenstein","authors":[{"name":"Shelley, Mary"}]}""")

        book.toSManga().author shouldBe "Mary Shelley"
        book.toSManga().thumbnail_url shouldBe null
    }

    @Test
    fun `keeps names without a comma`() {
        GutendexBook.displayName("Homer") shouldBe "Homer"
    }

    @Test
    fun `lists chapters newest first and finds their book`() {
        val book = GutenbergBook(
            Jsoup.parse("<h2 id='a'>One</h2><p>1</p><h2 id='b'>Two</h2><p>2</p>"),
        )

        val chapters = GutenbergSource.chaptersOf(84, book)

        chapters.map { it.url } shouldContainExactly listOf("/ebooks/84#b", "/ebooks/84#a")
        chapters.map { it.chapter_number } shouldContainExactly listOf(2f, 1f)
        bookId(chapters.first().url) shouldBe 84
    }
}
