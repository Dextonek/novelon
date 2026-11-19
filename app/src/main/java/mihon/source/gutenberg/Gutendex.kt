package mihon.source.gutenberg

import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** A page of books from the Gutendex catalogue API (gutendex.com). */
@Serializable
internal data class GutendexPage(
    val next: String? = null,
    val results: List<GutendexBook>,
) {
    fun toMangasPage() = MangasPage(results.map { it.toSManga() }, hasNextPage = next != null)

    companion object {
        fun parse(text: String): GutendexPage = json.decodeFromString(text)
    }
}

@Serializable
internal data class GutendexBook(
    val id: Int,
    val title: String,
    val authors: List<Person> = emptyList(),
    val summaries: List<String> = emptyList(),
    val bookshelves: List<String> = emptyList(),
    val formats: Map<String, String> = emptyMap(),
) {
    @Serializable
    data class Person(val name: String)

    fun toSManga(): SManga = SManga.create().also {
        it.url = bookUrl(id)
        it.title = title
        it.author = authors.joinToString { person -> displayName(person.name) }.ifEmpty { null }
        it.description = summaries.firstOrNull()
        it.genre = bookshelves.joinToString { shelf -> shelf.removePrefix("Category: ") }.ifEmpty { null }
        // The mirror, as gutenberg.org can be down while its files are mirrored.
        it.thumbnail_url = if ("image/jpeg" in formats) "$MIRROR_URL/cache/epub/$id/pg$id.cover.medium.jpg" else null
        it.status = SManga.COMPLETED
        it.update_strategy = UpdateStrategy.ONLY_FETCH_ONCE
        it.initialized = true
    }

    companion object {
        fun parse(text: String): GutendexBook = json.decodeFromString(text)

        /** "Austen, Jane" as "Jane Austen". */
        fun displayName(name: String): String {
            val parts = name.split(", ", limit = 2)
            return if (parts.size == 2) "${parts[1]} ${parts[0]}" else name
        }
    }
}

private val json = Json { ignoreUnknownKeys = true }

internal const val MIRROR_URL = "https://gutenberg.pglaf.org"

internal fun bookUrl(id: Int) = "/ebooks/$id"

/** The Gutenberg book id in a manga or chapter url made by [bookUrl]. */
internal fun bookId(url: String): Int = url.removePrefix("/ebooks/").substringBefore('#').toInt()
