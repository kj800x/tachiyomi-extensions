package eu.kanade.tachiyomi.extension.en.smbc

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import rx.Observable
import java.text.SimpleDateFormat

/**
 *  @author Kevin Johnson <kevin@kj800x.com>
 */
class Smbc : ParsedHttpSource() {
    override val name = "SMBC"
    override val baseUrl = "https://www.smbc-comics.com"
    override val lang = "en"
    override val supportsLatest = false

    override fun fetchPopularManga(page: Int): Observable<MangasPage> {
        val manga = SManga.create()
        manga.setUrlWithoutDomain("/comic/archive/")
        manga.title = "SMBC"
        manga.artist = "Zach Weinersmith"
        manga.author = "Zach Weinersmith"
        manga.status = SManga.ONGOING
        manga.description = "Saturday Morning Breakfast Cereal is a webcomic by Zach Weinersmith. It features few recurring characters or storylines, and has no set format; some strips may be a single panel, while others may go on for ten panels or more. Recurring themes in SMBC include atheism, God, superheroes, romance, dating, science, research, parenting and the meaning of life."
        manga.thumbnail_url = thumbnailUrl

        return Observable.just(MangasPage(arrayListOf(manga), false))
    }

    override fun fetchSearchManga(page: Int, query: String, filters: FilterList) = Observable.just(MangasPage(arrayListOf(), false))

    override fun fetchMangaDetails(manga: SManga): Observable<SManga> = fetchPopularManga(1)
        .map { it.mangas.first().apply { initialized = true } }

    override fun chapterListSelector() = "select[name='comic'] option"

    // Must be overriden, but not used since we overrode chapterListParse
    override fun chapterFromElement(element: Element): SChapter = throw Exception("Not used")

    /**
     * Override the chapter list parsing to enable certain elements to not match
     * (using a flatMap instead of a regular map)
     */
    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        return document.select(chapterListSelector()).reversed().flatMap { chapterListFromElement(it) }
    }

    /**
     * Parses the response from the site and returns a list of chapters.
     *
     * @param response the response from the site.
     */
    private fun chapterListFromElement(element: Element): List<SChapter> {
        try {
            val chapter = SChapter.create()
            chapter.url = "/" + element.attr("value")
            chapter.name = element.text().split(" - ")[1]
            val date = element.text().split(" - ")[0]
            val month = date.split(" ")[0]
            val day = date.split(", ")[0].split(" ")[1]
            val year = date.split(", ")[1]
            chapter.date_upload = SimpleDateFormat("yyyy-MMMMM-dd").parse("$year-$month-$day").time
            return listOf(chapter)
        } catch (e: Exception) {
            return listOf()
        }
    }

    private fun buildAltTextUrl(altText: String): String {
        val titleWords = altText.splitToSequence(" ")

        val builder = StringBuilder()
        var count = 0

        for (i in titleWords) {
            if (count != 0 && count.rem(7) == 0) {
                builder.append("%0A")
            }
            builder.append(i).append("+")
            count++
        }
        builder.append("%0A%0A")

        return baseAltTextUrl + builder.toString()
    }

    private fun isTextADate(alttext: String): Boolean {
        return dateAltTextRegex.matches(alttext)
    }

    override fun pageListParse(document: Document): List<Page> {
        val pages = mutableListOf<Page>()

        val src = document.select("#cc-comicbody img").first().attr("src")
        pages.add(Page(0, "", src))

        // Try to add the votey if it exists
        try {
            val afterComicSrc = document.select("#aftercomic img").first().attr("src")
            pages.add(Page(1, "", afterComicSrc))
        } catch (e: Exception) {}

        // Try to add the alt-text if it's not just a date
        try {
            val altText = document.select("#cc-comicbody img").first().attr("title")
            if (!isTextADate(altText)) {
                pages.add(Page(2, "", buildAltTextUrl(altText)))
            }
        } catch (e: Exception) {}

        return pages
    }

    override fun popularMangaSelector(): String = throw Exception("Not used")

    override fun searchMangaFromElement(element: Element): SManga = throw Exception("Not used")

    override fun searchMangaNextPageSelector(): String? = throw Exception("Not used")

    override fun searchMangaSelector(): String = throw Exception("Not used")

    override fun popularMangaRequest(page: Int): Request = throw Exception("Not used")

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request = throw Exception("Not used")

    override fun popularMangaNextPageSelector(): String? = throw Exception("Not used")

    override fun popularMangaFromElement(element: Element): SManga = throw Exception("Not used")

    override fun mangaDetailsParse(document: Document): SManga = throw Exception("Not used")

    override fun latestUpdatesNextPageSelector(): String? = throw Exception("Not used")

    override fun latestUpdatesFromElement(element: Element): SManga = throw Exception("Not used")

    override fun latestUpdatesRequest(page: Int): Request = throw Exception("Not used")

    override fun latestUpdatesSelector(): String = throw Exception("Not used")

    override fun imageUrlParse(document: Document) = throw Exception("Not used")

    companion object {
        const val thumbnailUrl = "https://s3.amazonaws.com/tachiyomi.kj800x.com/smbc/cover.png"
        const val baseAltTextUrl = "https://fakeimg.pl/1500x2126/ffffff/000000/?font_size=42&font=museo&text="
        val dateAltTextRegex = Regex("\\d\\d\\d\\d-\\d\\d-\\d\\d")
    }
}
