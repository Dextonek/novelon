# Novelon

A novel reader for Android. Browse sources, add novels to your library and read them as text, with the font
size and line spacing you like. Chapters can be downloaded for offline reading.

> [!NOTE]
> Novelon is a fork of [Mihon](https://github.com/mihonapp/mihon), the manga reader, reworked for text novels.
> It is not affiliated with or endorsed by the Mihon project.

## Status

Early development. There is no release yet. Build it from source (see below).

*Requires Android 8.0 or higher.*

## Features

* A text reader that scrolls through a chapter paragraph by paragraph and continues into the next chapter.
* Font size and line spacing settings, and light, dark and reader themes.
* Reading progress saved per chapter, with a marker where you stopped when you reopen one.
* Chapter downloads for offline reading.
* A built-in [Project Gutenberg](https://www.gutenberg.org) source with tens of thousands of public-domain books.
* Extensions for other sources, installed separately from the app.
* From Mihon: library categories, scheduled library updates, and backups.

## Extensions

Novelon itself ships only the Project Gutenberg source. Other sources are extensions: separate APKs built
from their own repository, the way Mihon does it. Novelon asks you to trust an extension once before using it.
The extension repository is not public yet.

## Building

Needs a JDK (24 or 25 are known to work) and the Android SDK.

```sh
./gradlew assembleDebug
```

The debug APKs end up in `app/build/outputs/apk/debug/` and install next to a release build as
`io.github.dextonek.novelon.dev`. Run `./gradlew check` before committing.

## Contributing

Work happens on the `novel/main` branch; `main` follows Mihon. Read [AGENTS.md](./AGENTS.md) for the rules on
code and commits, and [docs/HANDOVER.md](./docs/HANDOVER.md) and [docs/STATUS.md](./docs/STATUS.md) for where
the project stands.

## Disclaimer

The developers of this application have no affiliation with the content providers available, and the
application hosts no content.

## License

Novelon is licensed under the Apache License 2.0, like Mihon.

<pre>
Copyright © 2015 Javier Tomás
Copyright © 2024 Mihon Open Source Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
</pre>
