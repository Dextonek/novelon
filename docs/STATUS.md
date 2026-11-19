# Status

Novel reader forked from Mihon. Android only. Working branch: `novel/main`
(`origin` = github.com/Dextonek/novelon, `upstream` = github.com/mihonapp/mihon).

## Plan

| Phase | What | State |
| --- | --- | --- |
| 0 | Fork, clone, build unchanged app | Done; runs on a phone |
| 1 | Rebrand | Done, pushed to `novel/main` |
| 2 | Novel source API | Done, pushed |
| 3 | One built-in source (Project Gutenberg) | Works on a phone for cached Gutendex queries; catalogue unreliable |
| 4 | Text reader | Works on a phone |
| 5 | Offline downloads as `chapter.html` — end of first usable version | Works on a phone |
| 6 | Local EPUB in `source-local` | Pending |
| 7 | Separate extension repo + GitHub Pages index | Repo started locally with one web-novel source; no hosting or index yet |
| 8 | TTS, page mode, cleanup, signed release | Pending |

Internal names (`Manga`, `SManga`, `SChapter`, DB tables, `eu.kanade.tachiyomi` namespace, `mihon.*` packages)
stay as they are; only user-facing text changes.

## Done

- Forked `mihonapp/mihon` to `penkert-adus/mihon`, renamed to `novelon`, transferred to `Dextonek/novelon`, cloned to `C:\Users\adus\Work\novel-reader`.
- Android SDK at `%LOCALAPPDATA%\Android\Sdk` (moved from D: because the D: disk was busy). `ANDROID_HOME` set
  for the user. Empty folders `D:\Android\_download\unz` are left behind; the agent could not delete them.
- Baseline `gradlew check` on the unchanged code passes.
- Phase 1 (six commits on `novel/main`, `check` passes):
  - Application id `io.github.dextonek.novelon`, app name and Gradle root name `Novelon`.
  - Update checker, onboarding guide and About link point at `Dextonek/novelon`.
  - Donation prompt, "Support us" entry, `SupportUsScreen` and its route removed.
  - About screen: privacy policy and Mihon social links removed; one GitHub link to the fork.
  - Open-book icon (launcher, splash, notifications), replaced on 2026-11-19 by an open book with a
    bookmark; debug build uses a brown background.
  - Base English strings: "manga" → "entry", "scanlator" → "translator". Translations untouched.
  - README notice that this is an unaffiliated fork of Mihon (Apache-2.0).

- Phase 2 (pushed):
  - `Source.getChapterText(chapter): String` returns the chapter body as HTML; default throws, `StubSource`
    throws `SourceNotInstalledException`.
  - `ExtensionLoader` only loads APKs with the `novelon.extension` feature and `novelon.*` metadata keys,
    lib version `1.0`. Installed Mihon/Tachiyomi extensions are ignored entirely (not listed).
    Repo listings have no lib-version filter yet; a Mihon repo would still list extensions that then
    fail to load — handle in Phase 7.

- Phase 3 (committed locally, not pushed):
  - `mihon.source.gutenberg.GutenbergBook` splits a Gutenberg HTML book at its most frequent h1–h3 heading,
    strips the licence (`#pg-header`/`#pg-footer` or the `*** START/END OF` lines) and page numbers, makes
    image URLs absolute, prefixes act/part names, and keys chapters by the heading's anchor id (`n<index>`
    if none). Unit tests on trimmed fixtures; also checked by hand on 1342, 2600, 1661, 844, 1524, 98 from
    the gutenberg.pglaf.org mirror (older `-h.htm` files).
  - The newer generated page (`/cache/epub/<id>/pg<id>-images.html`) checked by hand on 1342, 2600, 1661, 1524,
    98, 84 from the mirror: clean chapter lists, licence removed.
  - `GutenbergSource` (built in, registered next to `LocalSource` in `AndroidSourceManager`): catalogue from
    Gutendex JSON (`languages=en`, `mime_type=text/html`; popular = default sort, latest = `sort=descending`,
    search = `search=`), book details from `/books/<id>/`, book HTML and covers from the PGLAF mirror
    (`gutenberg.pglaf.org/cache/epub/<id>/`). One parsed book is kept in memory for reading chapters in a
    row. Its HTTP client allows 90 s reads because Gutendex is slow on uncached queries.
  - Manga url `/ebooks/<id>`, chapter url `/ebooks/<id>#<anchor>`, chapters listed newest first.
  - Tests use real Gutendex responses saved on 2026-11-19 (`app/src/test/resources/gutendex/`).
  - Not verified live: Gutendex stopped answering at all later on 2026-11-19 (even `/books/84/`, 100 s).

- Phase 4 (pushed; never seen running — no device, and no source returns text yet):
  - `ui/reader/novel/`: `novelBlocks()` splits chapter HTML into headings, paragraphs and images, flattening
    containers; `NovelPageLoader` turns them into `TextPage`s (one paragraph = one reader page);
    `NovelViewer` is a Compose `LazyColumn` that reuses `WebtoonItems` and `WebtoonPageTracker`, so progress
    (`last_page_read` = paragraph index), marking read, history and chapter transitions are the existing code.
  - Routing: `readsAsText(source)` (= online source) picks `NovelPageLoader` in `ChapterLoader` and
    `NovelViewer` in `ReaderActivity.updateViewer`, ahead of the reading-mode/WebGPU choice.
  - Settings: font size (12–32 sp) and line spacing (1.0–2.5) on the reader settings' first tab for text.
  - Known gaps: page slider and counter count paragraphs; no font family or sepia theme yet (reader themes
    black/gray/white apply); `HttpPageLoader` is now unused for reading (remove in Phase 8).

- Phase 5 (committed locally, not pushed; never seen running):
  - `Downloader.downloadText` saves `getChapterText()` as `chapter.html` (`Downloader.TEXT_CHAPTER_FILE`) in the
    chapter's download folder; no CBZ, no ComicInfo. Images in the text stay links to the source.
  - `ChapterLoader` gives text sources a `NovelPageLoader` that reads that file when the chapter is
    downloaded, otherwise calls the source.

- Device test (2026-11-19, OnePlus Nord 4 over adb wireless debugging):
  - Works: Gutenberg Latest list, book details, chapter list, text reader, chapter transitions, saved
    progress, marking read, add to library, font size, download, and reading the downloaded `chapter.html`
    (checked by putting a marker paragraph into the file).
  - Fixed and pushed: the reader menu ran right to left (the manga reading-mode default) and showed the
    reading mode and crop buttons; the page number covered the text. New app icon: an open book with a
    bookmark.
  - Open: Gutendex timeouts and the missing opening chapter of book 80001 (HANDOVER.md, Known gaps).
- Extension repo `C:\projects\novelon-extensions` (local, no remote): a monorepo modelled on
  keiyoushi/extensions-source, one module per site under `src/<lang>/<name>`, a `novelon.extension` convention
  plugin, and compile-only jars of Novelon's `source-api` and `core:common` in `libs/`
  (`scripts/update-api-jars.sh` refreshes them). The first extension, a web-novel source, works on the
  phone.

## Build

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-24.0.2.12-hotspot'
.\gradlew.bat check assembleDebug --max-workers=2
```

- JDK 24 works. The user-level `JAVA_HOME` points at JDK 26, which is untested with Gradle 9.9.1.
- On a second PC the SDK was installed from the command-line tools into
  `%LOCALAPPDATA%\Android\Sdk`, and JDK 25 (Adoptium) builds and passes `check`. Git there has no identity,
  so commits pass `-c user.name=... -c user.email=...`.
- RAM is tight; the first full build crashed the Gradle daemon out of memory. `--max-workers=2` got it through.
- `./gradlew check` must pass before each commit (AGENTS.md).
- Push: `git -c credential.helper= -c 'credential.helper=!gh auth git-credential' push` (git has no stored
  GitHub login; gh's active account is Dextonek).

## Not changed on purpose

- Telemetry and the updater are already off for local builds (`Distribution.LOCAL` in `gradle/build-logic`,
  which AGENTS.md says not to touch). `app/google-services.json` is Mihon's and only used when telemetry is on.
- Trackers still send "Mihon" user agents and use Mihon's OAuth client ids and the `mihon://` / `app.mihon`
  callback schemes. Needs own tracker app registrations — decide later.
- No default extension repos existed, so nothing to remove.

## Blocked

- Gutenberg catalogue: Gutendex answers only cached queries, and gutenberg.org (OPDS) returns 503. The
  user's priority moved to web novels, so this waits.
- Gutenberg's robot-access policy (gutenberg.org/policy/robot_access.html) is unread (site down). Read it
  before deciding how the app fetches books.

## Waiting on the user

- Where to host `novelon-extensions` (a private repository is recommended).

## Next step

Host the extension repo and give it an index (Phase 7), then the Gutenberg catalogue and opening-chapter
fixes, then Phase 6 (local EPUB).

## Decisions

- App name: **Novelon**. No GitHub repo uses that exact name.
- Application id: `io.github.dextonek.novelon` (debug build adds the `.dev` suffix).
- 2026-11-19: the user wants to read web novels. Website sources live in a separate extension repository, not
  in this app repository, as in Mihon.
