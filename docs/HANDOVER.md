# Novelon handover

Novelon is an Android novel reader forked from [Mihon](https://github.com/mihonapp/mihon) (Apache-2.0). It keeps
Mihon's library, browsing, updates, history, downloads, backups and trackers, and replaces the image reader with
a text reader. This document is for whoever picks the work up next. The running log of decisions and state is
[STATUS.md](STATUS.md).

## The state in one paragraph

Phases 0 to 5 of the plan are written: rebrand, novel source API, a built-in Project Gutenberg source, the text
reader and offline downloads. Everything builds and `./gradlew check` passes. On 2026-11-19 the app ran on a
phone (OnePlus Nord 4) for the first time: library, browsing, the text reader, chapter transitions, progress,
font size and offline reading work. The user wants web novels rather than Gutenberg's classics, so the first
extension, a FreeWebNovel source, lives in a separate repository (see below) and works on the phone. Gutendex,
the Gutenberg catalogue API, only answers queries it has cached, so browsing Gutenberg is unreliable.

## Repository

| What | Where |
| --- | --- |
| Repository | `github.com/Dextonek/novelon` (public, a GitHub fork of `mihonapp/mihon`) |
| Working branch | `novel/main`. `main` is still Mihon's `main` at the fork point. |
| Upstream | `upstream` remote = `mihonapp/mihon`. The fork is permanent: upstream changes are not merged. |
| Rules for code and commits | [AGENTS.md](../AGENTS.md) (Mihon's, still in force) |
| Extensions | `C:\projects\novelon-extensions` on the user's PC, local only, no remote yet. Sources for sites that host unlicensed translations stay out of this public repo. |

## Building

Needs the Android SDK (platform 37.2, build-tools 37.0.0, NDK 29.0.14206865) and a JDK. JDK 24 and JDK 25 are
known to work with the project's Gradle 9.9.1; JDK 26 is untested.

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

- Gutendex only answers queries it has cached (0.1 s). Any other query (Popular with `page=1`, page 2, a new
  search, one filter alone) takes over 100 s or never answers, and the app shows "timeout". gutenberg.org
  answered 503 to everything on 2026-11-19, so its OPDS feed and robot-access policy are still unread. The
  PGLAF mirror serves the weekly catalogue `cache/epub/feeds/pg_catalog.csv.gz` (5.6 MB, 79,769 rows, 62,161
  English texts, no download counts). Filter it on `Type == Text`: #20000, for example, is an audiobook without
  HTML.
- `GutenbergBook` drops the text before the first chapter heading. Book 80001 ("A bird in the hand") has no
  "Chapter I" heading, so its first chapter is missing. A fix must keep title pages and contents out.
- Reader: the page slider and counter count paragraphs; no font family or sepia theme; long-press does nothing.
- Downloads: images inside a chapter are not downloaded.
- `HttpPageLoader` (online image pages) is no longer used for reading but still referenced elsewhere.
- Trackers still identify as Mihon (user agents, OAuth client ids, `mihon://` and `app.mihon` callbacks). Using
  them needs Novelon's own tracker app registrations.
- Extension repo listings don't filter by lib version, so a Mihon repo would list extensions that then fail to
  load.

## Next steps

1. Decide where `novelon-extensions` is hosted (a private repository is the safe choice for FreeWebNovel) and
   give it an index so Novelon can install and update extensions itself (Phase 7); filter repo listings by lib
   version. Until then extensions are installed with `adb install` and trusted once in Browse > Extensions.
2. Gutenberg: move the catalogue off Gutendex (mirror catalogue CSV, or gutenberg.org's OPDS feed once the site
   answers) and fix the missing opening chapter.
3. Phase 6: local EPUB files in `source-local`, read with the novel viewer.
4. Phase 8: text-to-speech, page mode, removal of the unused image paths, a signed release.
