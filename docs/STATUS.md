# Status

Novel reader forked from Mihon. Android only. Working branch: `novel/main`
(`origin` = github.com/Dextonek/novelon, `upstream` = github.com/mihonapp/mihon).

## Plan

| Phase | What | State |
| --- | --- | --- |
| 0 | Fork, clone, build unchanged app | Built; not yet run on a device |
| 1 | Rebrand | Done, pushed to `novel/main` |
| 2 | Novel source API | Done, pushed |
| 3 | One built-in source (Project Gutenberg) | In progress: chapter splitter done; catalogue blocked |
| 4 | Text reader | Written, `check` passes; never run on a device |
| 5 | Offline downloads as `chapter.html` — end of first usable version | Written, `check` passes; never run on a device |
| 6 | Local EPUB in `source-local` | Pending |
| 7 | Separate extension repo + GitHub Pages index | Pending |
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
  - Placeholder open-book icon (launcher, splash, notifications); debug build uses a brown background.
  - Base English strings: "manga" → "entry", "scanlator" → "translator". Translations untouched.
  - README notice that this is an unaffiliated fork of Mihon (Apache-2.0).

- Phase 2 (pushed):
  - `Source.getChapterText(chapter): String` returns the chapter body as HTML; default throws, `StubSource`
    throws `SourceNotInstalledException`.
  - `ExtensionLoader` only loads APKs with the `novelon.extension` feature and `novelon.*` metadata keys,
    lib version `1.0`. Installed Mihon/Tachiyomi extensions are ignored entirely (not listed).
    Repo listings have no lib-version filter yet; a Mihon repo would still list extensions that then
    fail to load — handle in Phase 7.

- Phase 3 so far (committed locally, not pushed):
  - `mihon.source.gutenberg.GutenbergBook` splits a Gutenberg HTML book at its most frequent h1–h3 heading,
    strips the licence (`#pg-header`/`#pg-footer` or the `*** START/END OF` lines) and page numbers, makes
    image URLs absolute, prefixes act/part names, and keys chapters by the heading's anchor id (`n<index>`
    if none). Unit tests on trimmed fixtures; also checked by hand on 1342, 2600, 1661, 844, 1524, 98 from
    the gutenberg.pglaf.org mirror (older `-h.htm` files).
  - Not yet verified: the newer generated page on the main site (`/ebooks/<id>.html.images`); gutenberg.org
    was down for maintenance (503) on 2026-11-19.

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

## Build

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-24.0.2.12-hotspot'
.\gradlew.bat check assembleDebug --max-workers=2
```

- JDK 24 works. The user-level `JAVA_HOME` points at JDK 26, which is untested with Gradle 9.9.1.
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

- Phase 3 catalogue. Gutendex (gutendex.com, JSON) answers the popular list in 0.2 s, but search and the
  newest-first sort timed out after 60 s on 2026-11-19; the app's read timeout is 30 s. gutenberg.org's own
  catalogue feed (OPDS) couldn't be fetched because the site was down. Re-time Gutendex and fetch a real OPDS
  response before writing the catalogue code. User decided (2026-11-19): wait for gutenberg.org and use its
  OPDS feed if the robot-access policy allows it.
- Gutenberg's robot-access policy (gutenberg.org/policy/robot_access.html) is unread (site down). Read it
  before deciding how the app fetches books.

## Waiting on the user

- Install the debug APK on a phone or emulator and confirm it starts.

## Next step

Phase 3 is still open: catalogue (see Blocked), then `GutenbergSource` registered next to `LocalSource` in
`AndroidSourceManager`, then one run on a phone.

## Decisions

- App name: **Novelon**. No GitHub repo uses that exact name.
- Application id: `io.github.dextonek.novelon` (debug build adds the `.dev` suffix).
