<div align="center">

# Loadz

### A modular Android download engine for M3U8 / HLS streams and regular files

**Build it. Integrate it. Make downloading part of your Android app.**

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat&logo=android&logoColor=white)](https://developer.android.com/)
[![Language](https://img.shields.io/badge/Language-Java%20%2F%20Kotlin-7F52FF?style=flat&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![HTTP](https://img.shields.io/badge/Networking-OkHttp-3E4348?style=flat)](https://square.github.io/okhttp/)
[![Media processing](https://img.shields.io/badge/Media-FFmpeg-007808?style=flat)](https://ffmpeg.org/)
[![License](https://img.shields.io/badge/License-Apache--2.0-blue.svg?style=flat)](LICENSE)
[![Status](https://img.shields.io/badge/Status-Under%20Development-orange?style=flat)](https://github.com/yogesh-hacker/Loadz)

[Repository](https://github.com/yogesh-hacker/Loadz) ·
[Issues](https://github.com/yogesh-hacker/Loadz/issues) ·
[Discussions](https://github.com/yogesh-hacker/Loadz/discussions) ·
[Contributing](#-contributing) ·
[Support the project](#-support-loadz)

</div>

---

> **Project status: active development.** Loadz is public so developers can inspect the source, experiment with the library module, report bugs, and contribute. APIs and behaviour may change. It is not yet presented as a stable, production-ready release, and a published Maven/JitPack artifact is not currently documented here.

## 📚 Table of contents

- [What is Loadz?](#-what-is-loadz)
- [Why Loadz?](#-why-loadz)
- [Current capabilities](#-current-capabilities)
- [Project status and roadmap](#-project-status-and-roadmap)
- [Repository layout](#-repository-layout)
- [Technology stack](#-technology-stack)
- [Requirements](#-requirements)
- [Getting started](#-getting-started)
- [Using Loadz in an Android project](#-using-loadz-in-an-android-project)
- [How the download pipeline works](#-how-the-download-pipeline-works)
- [M3U8 and HLS notes](#-m3u8-and-hls-notes)
- [FFmpeg and media processing](#-ffmpeg-and-media-processing)
- [Networking, headers, and security](#-networking-headers-and-security)
- [Troubleshooting](#-troubleshooting)
- [Testing checklist](#-testing-checklist)
- [Contributing](#-contributing)
- [Reporting bugs](#-reporting-bugs)
- [Responsible use](#-responsible-use)
- [Privacy](#-privacy)
- [Support Loadz](#-support-loadz)
- [Acknowledgements](#-acknowledgements)
- [License](#-license)
- [About the developer](#-about-the-developer)

## 🚀 What is Loadz?

**Loadz** is an Android download-manager project with a dedicated Android library module. It is being developed to make media downloading and download management easier to integrate into Android applications.

The project currently includes a library module named `loadz` and a tester application that depends on it. The library uses OkHttp for networking and includes FFmpeg-related media-processing dependencies. The tester app serves as a development playground for trying the library while the project evolves.

Loadz focuses on two broad use cases:

- **HLS / M3U8 media:** working with media playlists and their associated media resources.
- **Regular file downloads:** building toward a reusable download workflow for ordinary remote files.

The long-term goal is to offer app developers a maintainable component they can integrate into their own applications instead of implementing every download-related responsibility from scratch.

### Design goals

- **Reusable:** keep download functionality in a library module rather than tying it entirely to one screen.
- **Android-focused:** fit into Android application projects and their lifecycle.
- **Media-aware:** support the distinct steps involved in processing HLS playlists and media segments.
- **Practical:** make testing, debugging, and diagnosing failures straightforward.
- **Incremental:** document what works today and develop advanced behaviour without claiming unfinished features are complete.

## 💡 Why Loadz?

Implementing a robust downloader involves more than sending an HTTP request. Media playlists can reference many separate resources, servers may require request headers, networks can fail halfway through a transfer, and downloaded segments may need to be processed into a playable output.

Loadz is intended to centralise this work behind a reusable Android module. Its tester application provides a place to exercise the implementation before relying on it in another app.

## ✨ Current capabilities

The repository is still under development. The following table separates the project scope from features that should not yet be assumed to be production-ready.

| Area | Status / notes |
|---|---|
| Android library module | Present as `:loadz` |
| Tester application | Present as `:app`, depending on `:loadz` |
| M3U8 / HLS download workflow | Core project focus; test against your actual playlist types |
| Standard file downloads | Listed in the project scope; verify current implementation and edge cases |
| HTTP networking | OkHttp dependency included |
| FFmpeg-based media processing | FFmpeg-related dependency included; supported operations depend on the current implementation |
| Pause / resume | Part of the intended download-manager experience; validate behaviour for each supported download type |
| Custom request headers | A desired integration capability; inspect the current API before relying on it |
| Adaptive quality selection | Planned/under development; do not assume all master-playlist variants are handled |
| Retry and recovery | Needs validation across network failures and process death |
| Public Maven/JitPack release | No release coordinates are documented in this README yet |
| Stable API / production readiness | Not declared |

**Please open an issue** if you find a discrepancy between the documentation and the current source. Accurate documentation matters more than an impressive feature list.

## 🗺️ Project status and roadmap

The roadmap is a guide, not a promise that every item is already implemented. Contributions and test reports are welcome.

### Foundation

- [x] Publish the source repository publicly.
- [x] Separate the library module from the tester application.
- [x] Include an HTTP client dependency.
- [x] Include FFmpeg-related media-processing support in the build.
- [ ] Document the public API and its stability.
- [ ] Add reproducible examples for the supported download paths.

### Download engine

- [ ] Validate direct-file download behaviour across different response types.
- [ ] Improve progress reporting and lifecycle-safe cancellation.
- [ ] Test pause/resume semantics and interrupted downloads.
- [ ] Add robust retry policies where appropriate.
- [ ] Improve temporary-file handling and final-file integrity checks.
- [ ] Improve error reporting so applications can explain failures to users.

### HLS / M3U8

- [ ] Test master playlists and media playlists independently.
- [ ] Validate relative and absolute segment URLs.
- [ ] Test playlists with query parameters and expiring URLs.
- [ ] Validate header propagation to playlist, key, and segment requests where applicable.
- [ ] Test varying segment durations, missing segments, and transient HTTP failures.
- [ ] Improve quality selection and playlist edge-case handling.
- [ ] Define explicit behaviour for live playlists versus finite VOD playlists.

### Media output and storage

- [ ] Validate FFmpeg command construction and process completion.
- [ ] Test output integrity before marking a job complete.
- [ ] Improve storage destination handling on modern Android versions.
- [ ] Ensure failed and cancelled jobs clean up temporary files safely.
- [ ] Document supported input and output container formats.

### Developer experience

- [ ] Publish stable usage examples.
- [ ] Add tests for parser, networking, and download-state behaviour.
- [ ] Add CI checks for builds and tests.
- [ ] Establish versioning and release notes.
- [ ] Publish an installable artifact once distribution is configured and verified.

Want to help? Pick a roadmap item, open an issue to discuss the approach, or submit a focused pull request.

## 📂 Repository layout

The main project modules are:

```text
Loadz/
├── app/                         # Tester / sample Android application
├── loadz/                       # Reusable Android library module
├── bundle-android-aar-24-maven/ # Bundled Maven repository for a media dependency
├── gradle/                      # Gradle wrapper and version catalog
├── build.gradle.kts             # Root Gradle build configuration
├── settings.gradle.kts          # Project modules and repositories
├── gradle.properties
├── gradlew
├── gradlew.bat
├── LICENSE
└── README.md
```

The precise implementation may change as the project develops. Check the source tree for the current class names, public methods, and internal package structure.

## 🧰 Technology stack

| Technology | Role |
|---|---|
| Android Gradle Plugin | Android application and library builds |
| Java 11 source/target compatibility | Configured Java compatibility for the modules |
| Android SDK | Android platform APIs |
| OkHttp 4.12.0 | HTTP networking dependency |
| FFmpeg-related Android dependency | Media processing / stream output workflows |
| AndroidX and Material libraries | Supporting Android UI and core components in the tester app |
| JUnit / AndroidX test libraries | Test dependencies configured in the project |

The library module currently declares namespace `com.loadz` and `minSdk 24`. The tester app uses application ID `com.loadz.testapp`, `minSdk 24`, and `targetSdk 37`. Check the Gradle files for the current authoritative configuration before integrating or building.

## 🛠️ Requirements

Before building, install:

1. **Android Studio** with a compatible Android SDK.
2. **A compatible JDK** for the Android Gradle Plugin version configured in this repository. Use the JDK recommended by your installed Android Studio / Gradle setup.
3. **Git** to clone the source.
4. An Android emulator or physical device for runtime testing.

The project currently configures Android SDK compilation at API 37 and Java source/target compatibility 11. The Android Gradle Plugin version is managed through `gradle/libs.versions.toml`; use the checked-in wrapper and configuration rather than arbitrarily upgrading Gradle or the plugin.

## 🏁 Getting started

### 1. Clone the repository

```bash
git clone https://github.com/yogesh-hacker/Loadz.git
cd Loadz
```

### 2. Open the project

Open the repository root in Android Studio and allow Gradle sync to finish. Let Android Studio install any missing SDK components required by the build.

### 3. Build the project

On Linux or macOS:

```bash
./gradlew build
```

On Windows Command Prompt:

```bat
gradlew.bat build
```

To build the tester app specifically:

```bash
./gradlew :app:assembleDebug
```

On Windows, use `gradlew.bat` in place of `./gradlew`.

### 4. Run the tester app

Connect an Android device with developer options and USB debugging enabled, or start an emulator. Select the tester app's run configuration in Android Studio.

> **Build note:** these are the standard Gradle tasks for the checked-in project. If dependency resolution or compilation fails, please report the exact error, Android Studio version, JDK version, and operating system.

## 📦 Using Loadz in an Android project

### Current recommended approach: include the source module

Until a versioned public artifact and stable API are documented, the most reliable integration route is to include the library module in your own Gradle project and adapt the example to the actual public API in the source.

1. Copy or add the `loadz` module to your project.
2. Include the module in your `settings.gradle.kts`:

   ```kotlin
   include(":loadz")
   ```

3. Add the module dependency in your app's `build.gradle.kts`:

   ```kotlin
   dependencies {
       implementation(project(":loadz"))
   }
   ```

4. Make sure your project's SDK and JDK configuration is compatible with the library's Gradle configuration.
5. Inspect the current source for the available classes, constructors, methods, callback types, and required manifest declarations.
6. Test the integration on a physical device and with media URLs you are authorised to use.

This is the same module-based approach used by the repository's tester application. It does **not** imply that Loadz has already been published to Maven Central or JitPack.

### Why isn't there a copy-paste download example yet?

The correct code depends on the exact public API currently implemented in the library. An invented class name or method signature would create a misleading integration guide. Once the public API is settled, this section should include a complete, compiling example showing initialisation, request configuration, starting a download, progress events, cancellation, and output handling.

### Distribution plans

A versioned Maven/JitPack distribution may be added later. Before using remote dependency coordinates, look for an official release and installation instructions in the repository. Do not assume an unpublished version such as `1.0.0` or copy coordinates from another project.

## ⚙️ How the download pipeline works

A robust media downloader commonly involves the following stages. This describes the problem domain and intended architecture, not a guarantee that every stage is complete in the current code.

1. **Input validation** — check the URL and requested download configuration.
2. **Request setup** — prepare the HTTP request, including any legitimate headers required by the source.
3. **Playlist or file inspection** — determine whether the input is a direct file or an HLS playlist.
4. **Resource resolution** — for HLS, resolve playlist and segment references correctly.
5. **Transfer** — retrieve the required resources and track progress.
6. **Failure handling** — distinguish network errors, HTTP failures, invalid playlists, storage errors, and media-processing errors.
7. **Media processing** — when required and supported, use the configured media-processing layer to produce the intended output.
8. **Output verification** — confirm that the result is valid before reporting success.
9. **Cleanup and reporting** — remove incomplete temporary data safely and expose a useful result to the host application.

A good integration should never report a completed download merely because the HTTP request ended. The output must be usable, and any media-processing stage must finish successfully.

## 🎞️ M3U8 and HLS notes

M3U8 is a playlist format used by HTTP Live Streaming (HLS). Depending on the playlist, a stream can involve a master playlist, one or more variant playlists, media segments, initialization sections, encryption metadata, and other referenced resources.

### Playlist types

- **Master playlist:** may describe multiple variants or renditions, often at different resolutions and bitrates.
- **Media playlist:** describes media resources for a particular rendition.
- **VOD playlist:** usually describes a finite presentation.
- **Live playlist:** may change over time and require different scheduling and completion rules.

### Things to test

- Relative segment paths versus absolute URLs.
- Redirects and expiring query parameters.
- Servers that require a `Referer`, `User-Agent`, or other permitted request headers.
- Different segment extensions and container formats.
- Network interruptions and HTTP status errors.
- Master playlists with multiple quality variants.
- Empty, malformed, or changing playlists.
- Output compatibility with the selected player and container.

### Important limitations

Not every `.m3u8` URL is downloadable as a simple list of files. Some streams require authentication, use short-lived tokens, depend on session state, or are protected by DRM. A downloader must respect the source's access controls and the applicable permissions.

**Do not assume** that every HLS stream, encrypted stream, live stream, or playlist layout is supported. Check the current implementation and test your intended source.

## 🎬 FFmpeg and media processing

The project includes an FFmpeg-related Android dependency. Media processing can be useful when downloaded stream resources need to be remuxed or combined into a playable output.

Typical engineering considerations include:

- Constructing arguments safely rather than concatenating untrusted input into shell commands.
- Handling FFmpeg success and failure results.
- Checking that the output exists and is non-empty.
- Avoiding premature success callbacks.
- Cleaning up temporary files after failures or cancellation.
- Accounting for storage space and long-running work.
- Understanding the licensing and redistribution terms of the exact FFmpeg build being packaged.

The presence of an FFmpeg dependency does **not** mean every codec, container, or command is supported. Confirm the actual bundled binary, available codecs, package provenance, and licences before distributing an application that incorporates it.

## 🌐 Networking, headers, and security

Loadz uses OkHttp as a declared networking dependency. When building on this foundation:

- Use HTTPS whenever the source supports it.
- Validate URLs and handle redirects intentionally.
- Avoid logging full URLs when they contain authentication tokens or signed query parameters.
- Never commit credentials, API keys, cookies, private test URLs, or personal data.
- Treat remote playlists and response headers as untrusted input.
- Apply sensible connection/read timeouts and retry limits.
- Do not retry permanent failures indefinitely.
- Avoid retaining sensitive headers in persistent download history.
- Ensure cancellation stops relevant network and media-processing work where possible.
- Never bypass DRM or access controls.

If a server requires custom headers, use only headers you are authorised to send. A header such as `X-Forwarded-For` does not make a client legitimately assume another IP address; it is normally a proxy-related header and should not be used to impersonate network identity.

## 🧯 Troubleshooting

### Gradle sync or build fails

- Check that Android Studio has installed the SDK required by the project.
- Confirm the JDK used by Gradle is compatible with the configured Android Gradle Plugin.
- Run `./gradlew --version` (or `gradlew.bat --version` on Windows) to inspect the Gradle runtime.
- Retry with `./gradlew build --stacktrace` and include the relevant error in an issue.
- Avoid deleting project configuration or changing dependency versions without understanding the failure.

### Dependency cannot be resolved

- Check your internet connection and Gradle repository configuration.
- Confirm that the dependency's repository and artifact version are available.
- Check the bundled local Maven repository under `bundle-android-aar-24-maven`.
- Include the exact unresolved dependency and Gradle error when reporting the problem.

### Playlist opens but download fails

- Check whether the URL is a master playlist or media playlist.
- Confirm the URL has not expired and that the source is accessible from the device.
- Check HTTP status codes, redirects, and any legitimate required headers.
- Try a known, authorised test playlist to determine whether the problem is source-specific.

### Downloaded output will not play

- Check whether all expected resources were retrieved.
- Verify that the media-processing stage completed successfully.
- Confirm the output container and codecs are compatible with the player.
- Check available storage and inspect logs for processing errors.

### Works on Wi-Fi but not mobile data

- Check Android network permissions, connectivity, source restrictions, DNS, and timeouts.
- Avoid assuming the problem is caused by the playlist alone.
- Compare the HTTP status and error logs across both networks, redacting credentials and tokens.

### App crashes or a download continues after leaving the screen

- Inspect lifecycle handling, cancellation, and any background-work/service requirements.
- Include the Android version, device model, reproduction steps, and stack trace in a bug report.
- Do not share private stream URLs or authentication headers in public issues.

## 🧪 Testing checklist

Before relying on Loadz in an application, test on real devices and with sources you are allowed to download.

| Test case | What to verify |
|---|---|
| Valid direct file | Correct output and final status |
| Invalid URL | Clear validation or network error |
| HTTP error response | Failure is surfaced accurately |
| Slow connection | Progress remains sensible; no UI freeze |
| Connection loss | Defined retry or failure behaviour |
| User cancellation | Work stops and temporary files are handled |
| Low storage | Safe failure without corrupting unrelated files |
| HLS VOD playlist | All required media resources are accounted for |
| Master playlist | Variant selection behaves as documented |
| Relative URLs | References resolve against the correct base URL |
| Required headers | Headers are sent only where appropriate |
| FFmpeg failure | Job is not reported as successfully processed |
| Process recreation | State is recovered only where persistence is implemented |
| Android API differences | Storage and background-work behaviour is correct |

Please report which cases you tested when submitting a bug fix. Reproducible test results help the project improve much faster.

## 🤝 Contributing

Contributions are welcome — bug reports, code, documentation, test cases, architecture discussions, and small reproducible examples are all valuable.

### Suggested workflow

1. Check the [existing issues](https://github.com/yogesh-hacker/Loadz/issues) to avoid duplicating work.
2. Open an issue first for major changes or new public APIs.
3. Fork the repository.
4. Create a focused branch:

   ```bash
   git checkout -b fix/short-description
   ```

5. Make the smallest change that solves the problem.
6. Build and test the affected module.
7. Review your diff for secrets, generated files, unrelated changes, and formatting problems.
8. Submit a pull request describing the problem, solution, testing performed, and any compatibility impact.

### Contribution guidelines

- Keep the library reusable; avoid coupling core download logic to tester-app UI.
- Prefer clear error handling over silently swallowing exceptions.
- Avoid logging tokens, cookies, or private URLs.
- Document changes to public APIs.
- Include reproduction steps or tests for bug fixes where practical.
- Do not add dependencies without explaining why they are necessary.
- Be transparent about untested behaviour.
- Be respectful and constructive in code reviews.

### Good first contributions

- Add safe, reproducible test cases.
- Improve error messages and troubleshooting documentation.
- Test playlist URL resolution.
- Improve cleanup after failed downloads.
- Document the current public API.
- Test behaviour on different Android versions.
- Help verify dependency licensing and release packaging.

## 🐛 Reporting bugs

Please use [GitHub Issues](https://github.com/yogesh-hacker/Loadz/issues).

A useful report includes:

```text
Title:
Environment:
- Device / emulator:
- Android version:
- Android Studio / JDK version (for build issues):

Expected behaviour:
Actual behaviour:
Steps to reproduce:
Relevant logs:
Tested URL type: (direct file / HLS VOD / master playlist / other)
Reproducible every time?: yes / no
```

**Privacy first:** redact access tokens, cookies, session IDs, signed query parameters, personal file paths, and other secrets. If the stream URL is private, do not post it publicly. Provide a safe reproduction case instead.

For feature requests, describe the use case, the expected API or behaviour, and any compatibility concerns.

## ⚖️ Responsible use

Loadz is a general-purpose development project. You are responsible for how you use it and for complying with applicable law, copyright rules, platform terms, and the source provider's conditions.

Use Loadz only for content you own, content that is licensed for downloading, or content you otherwise have explicit permission to save. Do not use it to bypass DRM, defeat access controls, redistribute copyrighted content without permission, or access private material unlawfully.

The project does not grant permission to download any particular website's content. A technically accessible URL is not necessarily a legally authorised download.

## 🔐 Privacy

Loadz is source-available on GitHub under the repository's stated licence. Review the source and dependencies before incorporating them into an application.

Do not assume that a test URL, cookie, header, or download history is safe to share. If you build an application around Loadz, explain your own application's data handling to your users and avoid collecting information that is not necessary.

This README does not make claims about telemetry, analytics, or data collection beyond what can be verified in the source code. Review the current implementation and any bundled dependencies to determine actual behaviour.

## 💖 Support Loadz

Loadz is an independent open-source project built to make Android download functionality easier for developers to explore and reuse. Maintaining a library takes time: implementation, debugging, testing across Android versions, dependency maintenance, documentation, and helping contributors all add up.

**If Loadz is useful to you, you can support its development.** Financial support is optional; using the project, reporting a bug, improving the documentation, or sharing it with another developer is also a meaningful contribution.

### Ways to support the project

#### ⭐ 1. Star the repository

If you find Loadz useful, consider giving the project a star:

**[⭐ Star Loadz on GitHub](https://github.com/yogesh-hacker/Loadz)**

Stars help other developers discover the project and give the maintainer encouragement to keep improving it.

#### 🔀 2. Share it with developers

Share the repository with Android developers, open-source communities, and people building media applications. If you use Loadz in a project, consider linking back to this repository.

#### 🧑‍💻 3. Contribute code or documentation

You can help by fixing bugs, testing edge cases, improving examples, reviewing dependencies, or documenting APIs. Start with the [contribution guide](#-contributing).

#### 🐞 4. Report useful bugs

A clear report with reproducible steps can save hours of debugging. Please use the [issue tracker](https://github.com/yogesh-hacker/Loadz/issues) and remove private information from logs.

#### 💸 5. Make a voluntary donation

If you want to support ongoing development financially, use **only an official payment link published by the maintainer**.

> **Donation links are intentionally not fabricated here.** Add your verified payment methods below before publishing this section as an active donation destination. Remove any method you do not personally control or cannot reliably receive payments through.

### 💸 Donation methods

If this API saves you time or helps your project, consider supporting its development. Every contribution is voluntary and helps support ongoing maintenance, testing, documentation, and future improvements.

#### Binance Pay

```text
992511662
```

#### USDT — TRON / TRC20

```text
TWUnCAxq8sGRpTfaP4XKQSL7ifAc6CxrTt
```

#### USDC — Solana

```text
HuSSSG4gwmAuamir45VteBiHYKpZwBjpV6Nr1VT3nDjz
```

#### Another payment method

If you'd like to donate using another payment method, feel free to contact me on Discord.

**Discord:** `peerless_x`

> **Please double-check the payment network before sending crypto.** Send USDT only on the TRON (TRC20) network to the USDT address above, and send USDC only on Solana to the Solana address above. Crypto transfers are generally irreversible. Confirm the address and network in your wallet before approving a transaction.


### How contributions help

With community support, development time can be directed toward:

- More reliable download and cancellation behaviour.
- Better handling of HLS playlist edge cases.
- Safer temporary-file management and recovery.
- Improved compatibility across Android versions.
- Test coverage and reproducible bug reports.
- Clear API documentation and integration examples.
- Dependency maintenance and release preparation.

### Transparency and expectations

- Donations are **voluntary** and are not required to use the project.
- A donation does not purchase priority support, guaranteed fixes, or a delivery date.
- Donations do not grant ownership of Loadz or special rights over the project.
- There is no promise that a particular feature will be implemented by a particular date.
- Please verify the recipient and payment details before sending money.
- If donation methods change, the repository's official README should be treated as the source of truth.

Thank you for supporting independent development — whether by donating, contributing, testing, starring, or simply giving Loadz a try. Every bit of support is appreciated. ❤️

## 🙏 Acknowledgements

Loadz is developed independently, with architectural learning and inspiration from existing Android download libraries such as:

- [Fetch2](https://github.com/tonyofrancis/Fetch) — a reference for download-manager design ideas.
- [PRDownloader](https://github.com/MindorksOpenSource/PRDownloader) — another reference for download workflows and developer experience.
- [OkHttp](https://square.github.io/okhttp/) — HTTP client library.
- [FFmpeg](https://ffmpeg.org/) and the Android FFmpeg dependency used by the project — media processing.

These projects and tools retain their own names, licences, and copyrights. Mentioning them here is an acknowledgement of inspiration or dependency, not a claim of ownership or endorsement. Consult their respective repositories and licence files for exact terms.

## 📄 License

Loadz is distributed under the **Apache License 2.0**. See [`LICENSE`](LICENSE) for the complete terms.

In general, the Apache 2.0 licence permits use, modification, and redistribution subject to its conditions, including retaining applicable notices and providing a copy of the licence. It also includes an express patent licence from contributors under the terms described in the licence.

Third-party dependencies and bundled binaries may have separate licences and notices. Check those terms before redistributing an application that includes them.

## 👨‍💻 About the developer

**Loadz is created and maintained by [Yogesh Kumar Jamre](https://github.com/yogesh-hacker).**

- GitHub: [@yogesh-hacker](https://github.com/yogesh-hacker)
- Repository: [yogesh-hacker/Loadz](https://github.com/yogesh-hacker/Loadz)
- Bug reports and feature requests: [GitHub Issues](https://github.com/yogesh-hacker/Loadz/issues)

If you build something with Loadz, feedback and contributions are welcome. If you publish an app that uses the library, please document your own app's behaviour and comply with the applicable licences.

---

<div align="center">

**Loadz — download functionality, built to be integrated.**

If this project helps you, consider ⭐ starring the repository, reporting a bug, contributing a fix, or supporting future development.

Made with ❤️ by [Yogesh Kumar Jamre](https://github.com/yogesh-hacker)

</div>
