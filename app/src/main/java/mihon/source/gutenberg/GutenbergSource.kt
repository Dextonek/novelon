package mihon.source.gutenberg

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.jsoup.Jsoup
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Public-domain books from Project Gutenberg: the catalogue from Gutendex, the books from the PGLAF mirror, split
 * into chapters by [GutenbergBook].
 */
class GutenbergSource : HttpSource() {

    override val name = "Project Gutenberg"

    override val lang = "en"

    override val baseUrl = "https://www.gutenberg.org"

    override val supportsLatest = true

    // Gutendex answers uncached queries such as searches slowly, often past the app-wide 30 s read timeout.
    override val client: OkHttpClient by lazy {
        network.client.newBuilder()
            .readTimeout(90.seconds)
            .callTimeout(2.minutes)
            .build()
    }

    private val bookLock = Mutex()
    private var lastBook: Pair<Int, GutenbergBook>? = null

    override suspend fun getPopularManga(page: Int): MangasPage = catalogue(page)

    override suspend fun getLatestUpdates(page: Int): MangasPage = catalogue(page, "sort" to "descending")

    override suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage =
        catalogue(page, "search" to query)

    override suspend fun getMangaUpdate(
        manga: SManga,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): SMangaUpdate {
        val id = bookId(manga.url)
        val details = if (fetchDetails) {
            val text = client.newCall(GET("$API_URL/books/$id/", headers)).awaitSuccess().body.string()
            GutendexBook.parse(text).toSManga()
        } else {
            manga
        }
        val newChapters = if (fetchChapters) chaptersOf(id, book(id)) else chapters
        return SMangaUpdate(details, newChapters)
    }

    override suspend fun getChapterText(chapter: SChapter): String {
        val key = chapter.url.substringAfter('#')
        return book(bookId(chapter.url)).chapterHtml(key)
            ?: error("Chapter \"${chapter.name}\" is no longer in this book")
    }

    private suspend fun catalogue(page: Int, vararg query: Pair<String, String>): MangasPage {
        val url = "$API_URL/books/".toHttpUrl().newBuilder()
            .addQueryParameter("languages", "en")
            .addQueryParameter("mime_type", "text/html")
            .addQueryParameter("page", page.toString())
            .apply { query.forEach { (name, value) -> addQueryParameter(name, value) } }
            .build()
        val text = client.newCall(GET(url, headers)).awaitSuccess().body.string()
        return GutendexPage.parse(text).toMangasPage()
    }

    /** The book with [id], kept while its chapters are read one after another. */
    private suspend fun book(id: Int): GutenbergBook = bookLock.withLock {
        lastBook?.takeIf { it.first == id }?.let { return@withLock it.second }
        val url = "$MIRROR_URL/cache/epub/$id/pg$id-images.html"
        val response = client.newCall(GET(url, headers)).awaitSuccess()
        val book = response.body.byteStream().use { GutenbergBook(Jsoup.parse(it, null, url)) }
        lastBook = id to book
        book
    }

    companion object {
        private const val API_URL = "https://gutendex.com"

        /** Chapters newest first, as sources list them. */
        internal fun chaptersOf(id: Int, book: GutenbergBook): List<SChapter> {
            return book.chapters.mapIndexed { index, chapter ->
                SChapter.create().also {
                    it.url = "${bookUrl(id)}#${chapter.key}"
                    it.name = chapter.name
                    it.chapter_number = (index + 1).toFloat()
                }
            }.reversed()
        }
    }
}
