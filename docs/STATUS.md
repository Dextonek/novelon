# Status

Novel reader forked from Mihon. Android only. Working branch: `novel/main`
(`origin` = github.com/penkert-adus/novelon, transfer to `Dextonek` pending; `upstream` = github.com/mihonapp/mihon).

## Plan

| Phase | What | State |
| --- | --- | --- |
| 0 | Fork, clone, build unchanged app | Built; not yet run on a device |
| 1 | Rebrand | Done, committed on `novel/main` (not pushed) |
| 2 | Novel source API (`getChapterText`, own extension feature tag and lib version in `ExtensionLoader.kt`) | Next |
| 3 | One built-in source (Project Gutenberg) | Pending |
| 4 | Text reader (Compose, paragraphs, progress in `chapter.last_page_read`, reader settings) | Pending |
| 5 | Offline downloads as `chapter.html` — end of first usable version | Pending |
| 6 | Local EPUB in `source-local` | Pending |
| 7 | Separate extension repo + GitHub Pages index | Pending |
| 8 | TTS, page mode, cleanup, signed release | Pending |

Internal names (`Manga`, `SManga`, `SChapter`, DB tables, `eu.kanade.tachiyomi` namespace, `mihon.*` packages)
stay as they are; only user-facing text changes.

## Done

- Forked `mihonapp/mihon` to `penkert-adus/mihon`, renamed to `penkert-adus/novelon`, cloned to `C:\Users\adus\Work\novel-reader`.
- Android SDK at `%LOCALAPPDATA%\Android\Sdk` (moved from D: because the D: disk was busy). `ANDROID_HOME` set
  for the user. Empty folders `D:\Android\_download\unz` are left behind; the agent could not delete them.
- Baseline `gradlew check` on the unchanged code passes.
- Phase 1 (six commits on `novel/main`, `check` passes):
  - Application id `io.github.penkertadus.novelon`, app name and Gradle root name `Novelon`.
  - Update checker, onboarding guide and About link point at `penkert-adus/novelon`.
  - Donation prompt, "Support us" entry, `SupportUsScreen` and its route removed.
  - About screen: privacy policy and Mihon social links removed; one GitHub link to the fork.
  - Placeholder open-book icon (launcher, splash, notifications); debug build uses a brown background.
  - Base English strings: "manga" → "entry", "scanlator" → "translator". Translations untouched.
  - README notice that this is an unaffiliated fork of Mihon (Apache-2.0).

## Build

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-24.0.2.12-hotspot'
.\gradlew.bat check assembleDebug --max-workers=2
```

- JDK 24 works. The user-level `JAVA_HOME` points at JDK 26, which is untested with Gradle 9.9.1.
- RAM is tight; the first full build crashed the Gradle daemon out of memory. `--max-workers=2` got it through.
- `./gradlew check` must pass before each commit (AGENTS.md).

## Not changed on purpose

- Telemetry and the updater are already off for local builds (`Distribution.LOCAL` in `gradle/build-logic`,
  which AGENTS.md says not to touch). `app/google-services.json` is Mihon's and only used when telemetry is on.
- Trackers still send "Mihon" user agents and use Mihon's OAuth client ids and the `mihon://` / `app.mihon`
  callback schemes. Needs own tracker app registrations — decide later.
- No default extension repos existed, so nothing to remove.

## Waiting on the user

- Finish `gh auth login` as Dextonek (Terminal tab "gh login Dextonek").
- Accept the repo transfer as Dextonek (GitHub e-mail or https://github.com/penkert-adus/novelon/settings).
- Install the debug APK on a phone or emulator and confirm it starts.

## Next step

After the transfer: change `penkert-adus/novelon` to `Dextonek/novelon` in `AppUpdateChecker.kt`,
`GuidesStep.kt` and `AboutScreen.kt`, set `origin` to the new URL, push `novel/main`. Then Phase 2.

## Decisions

- App name: **Novelon**. No GitHub repo uses that exact name.
- Application id: `io.github.penkertadus.novelon` (debug build adds the `.dev` suffix).
