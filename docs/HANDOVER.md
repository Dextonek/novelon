# Novelon handover

Novelon is an Android novel reader forked from [Mihon](https://github.com/mihonapp/mihon) (Apache-2.0). It keeps
Mihon's library, browsing, updates, history, downloads, backups and trackers, and replaces the image reader with
a text reader. This document is for whoever picks the work up next. The running log of decisions and state is
[STATUS.md](STATUS.md).

## The state in one paragraph

Phases 0 to 5 of the plan are written: rebrand, novel source API, a built-in Project Gutenberg source, the text
reader and offline downloads. Everything builds and `./gradlew check` passes. **The app has never been started
on a phone or emulator**, so none of the screens has been seen working, and the Gutenberg catalogue API
(Gutendex) was unreachable when the source was finished, so the source is tested only against saved responses.
The first job is a run on a device.

## Repository

| What | Where |
| --- | --- |
| Repository | `github.com/Dextonek/novelon` (public, a GitHub fork of `mihonapp/mihon`) |
| Working branch | `novel/main`. `main` is still Mihon's `main` at the fork point. |
| Upstream | `upstream` remote = `mihonapp/mihon`. The fork is permanent: upstream changes are not merged. |
| Rules for code and commits | [AGENTS.md](../AGENTS.md) (Mihon's, still in force) |

## Building

Needs the Android SDK (platform 37.2, build-tools 37.0.0) and a JDK. JDK 24 is known to work with the
project's Gradle 9.9.1; JDK 26 is untested.

```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:JAVA_HOME = '<path to a JDK 24>'
.\gradlew.bat check assembleDebug --max-workers=2
```

- APKs land in `app\build\outputs\apk\debug\`; `app-arm64-v8a-debug.apk` fits most phones.
- `--max-workers=2` because a full build ran the Gradle daemon out of memory on a machine with little free RAM.
- `check` (tests, lint, ktlint) must pass before every commit.
- Debug builds install as `io.github.dextonek.novelon.dev`, release builds as `io.github.dextonek.novelon`.

## What changed from Mihon

### Phase 1: rebrand
- App name Novelon, application id `io.github.dextonek.novelon`, Gradle root project `Novelon`.
- Update checker, onboarding guide link and About link point at `Dextonek/novelon`.
- Donation prompt, "Support us" screen and Mihon's social links removed.
- Placeholder open-book icon (launcher, splash, notifications). It is a placeholder, not a design.
- English UI text says "entry" and "translator" instead of "manga" and "scanlator". Translations are untouched.

### Phase 2: source API
- `Source.getChapterText(chapter): String` in `source-api` returns a chapter's body as HTML. It throws by
  default; `StubSource` throws "source not installed".
- `ExtensionLoader` only loads APKs declaring the `novelon.extension` feature with `novelon.*` metadata and lib
  version `1.0`. Mihon/Tachiyomi extensions are ignored.

### Phase 3: Project Gutenberg source (`app/src/main/java/mihon/source/gutenberg/`)
- `GutenbergBook` splits a Gutenberg HTML book into chapters at its most frequent h1–h3 heading. It removes
  the licence and page numbers, makes image URLs absolute, prefixes act/part names ("ACT I · SCENE II") and
  keys chapters by the heading's anchor id. Tables of contents are not used: they also link to page numbers.
- `Gutendex.kt` maps Gutendex JSON (`gutendex.com`) to entries.
- `GutenbergSource` is built in, registered next to the local source in `AndroidSourceManager`. Catalogue and
  details come from Gutendex; book HTML and covers from the PGLAF mirror `gutenberg.pglaf.org/cache/epub/<id>/`,
  because gutenberg.org was down for maintenance. One parsed book is kept in memory. The source's HTTP client
  allows 90 s reads because Gutendex is slow on uncached queries.
- URLs: entry `/ebooks/<id>`, chapter `/ebooks/<id>#<anchor>`.

### Phase 4: text reader (`app/src/main/java/eu/kanade/tachiyomi/ui/reader/novel/`)
- Mihon's reader works with pages. Here **one paragraph is one page**, so Mihon's own code handles progress
  (`last_page_read` holds the paragraph index), marking read, history and chapter transitions.
- `novelBlocks()` splits chapter HTML into headings, paragraphs and images. `NovelPageLoader` turns them into
  `TextPage`s. `NovelViewer` shows them in a Compose `LazyColumn` and reuses `WebtoonItems` and
  `WebtoonPageTracker` from the webtoon viewer.
- `readsAsText(source)` (true for every online source) picks the novel loader in `ChapterLoader` and the novel
  viewer in `ReaderActivity.updateViewer`. The local source still uses the image reader.
- Settings: font size and line spacing on the first tab of the reader settings when a text chapter is open.

### Phase 5: offline downloads
- `Downloader` saves a text chapter as `chapter.html` in its download folder; `ChapterLoader` reads that file
  when the chapter is downloaded.

## Tests

| Test | Covers |
| --- | --- |
| `GutenbergBookTest` | chapter splitting on trimmed old- and new-format books, plays, parts, image headings |
| `GutendexTest` | real Gutendex responses saved on 2026-11-19 (`app/src/test/resources/gutendex/`) |
| `NovelReaderTest` | paragraph splitting and which paragraph counts as current while scrolling |

The splitters were also run by hand on six real books in each page format from the mirror; those runs are not
in the test suite because the books are large.

## Known gaps

- Never run on a device (see above).
- Gutendex reliability: it answered in 0.2 s for cached queries, took over 60 s for searches, and stopped
  answering entirely later the same day. Gutenberg's own OPDS feed is the fallback, but its format and
  Gutenberg's robot-access policy have not been read (the site was down).
- Reader: the page slider and counter count paragraphs; no font family or sepia theme; long-press does nothing.
- Downloads: images inside a chapter are not downloaded.
- `HttpPageLoader` (online image pages) is no longer used for reading but still referenced elsewhere.
- Trackers still identify as Mihon (user agents, OAuth client ids, `mihon://` and `app.mihon` callbacks). Using
  them needs Novelon's own tracker app registrations.
- Extension repo listings don't filter by lib version, so a Mihon repo would list extensions that then fail to
  load.

## Next steps

1. Install the debug APK on a phone or emulator. Browse Project Gutenberg, add a book, read a few chapters,
   change font size, download a chapter and read it offline. Fix what breaks.
2. Re-check Gutendex. If it stays unreliable, read gutenberg.org's robot-access policy and move the catalogue to
   its OPDS feed.
3. Phase 6: local EPUB files in `source-local`, read with the novel viewer.
4. Phase 7: a separate extension repository with a Gradle template for novel sources and a GitHub Pages index;
   move the Gutenberg source there and filter repo listings by lib version.
5. Phase 8: text-to-speech, page mode, removal of the unused image paths, a real icon, a signed release.
