# Sigha — صيغة

**A privacy-first, fully offline file converter for Android.**

Sigha converts documents, images, PDFs, audio, video, archives and data files **on the device**.
Nothing is uploaded. No account, no ads, no analytics. Optional on-device AI model packs (OCR,
upscaling) are downloaded only when the user taps Download.

> Inspired by the *idea* of [Filee](https://github.com/KnifeLemon/Filee) (MIT, desktop).
> Sigha is an original Kotlin/Jetpack Compose implementation for Android; no Filee code is included.

## Status

| Area | State |
|---|---|
| Build | `gradlew :app:assembleDebug` — AGP 8.10.1, Gradle 8.11.1, JDK 21, compile/target SDK 36 |
| UI | Compose Material 3, English + Arabic (RTL), light/dark |
| Engines | Images, PDF, office-simple, text, data, subtitles, e-books, archives, FFmpeg media |
| AI packs | OCR (Tesseract, Apache-2.0) downloadable pack (28 MB, SHA-256 pinned); upscaling is "Coming soon" |
| Verification | 131 JVM tests green; debug APK 72,522,602 B, 16 KB-aligned, `targetSdk 36` — see `docs/qa/verification.md`; device QA still pending |

## Build

Requirements: JDK 17+ (JDK 21 used here), Android SDK platform 36 + build-tools 36.

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot"
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon --console=plain
# APK: app\build\outputs\apk\debug\app-debug.apk
```

Current build: 32 converter edges over a 48-format catalogue, 131 JVM tests passing. The APK ships
arm64-v8a and x86_64 and needs a release key (and an ABI split) before Play submission.

## Architecture

```
ui/            Compose screens + ViewModels (workbench, history, settings, model packs)
data/          conversion pipeline, output publishing (MediaStore), settings, history
core/          FileFormat, FormatRegistry, Converter, ConverterRegistry, RoutePlanner
engine/        one package per family; each exposes its own list of Converter edges
model/         downloadable model packs (catalog, SHA-256 verified install)
```

The router is a graph: converters declare `inputs` and `outputs`; the planner finds the
cheapest chain (≤ 3 hops) for any source→target pair. Adding a format means adding edges,
not editing a giant `when` statement.

## Licensing

Sigha is original code. Third-party components are permissive or LGPL (see `NOTICE.md`).
GPL/AGPL tools (LibreOffice, Pandoc, Ghostscript, calibre) are deliberately **not** bundled.
Model packs carry their own licenses and are downloaded only on explicit user action.

## Legal / halal clearance

Before building on Filee's material, a clearance memo was produced: `docs/legal/clearance-memo.md`.
Short version: facts and ideas are free; expression is not. No Filee code, brand, logo, screenshots
or trade dress is used. FFmpegKit is LGPL-3.0 — notices and relinking information are published.

## Docs

- `docs/council/qiyas-council.md` — council analysis and verdict
- `docs/legal/clearance-memo.md` — halal & legal clearance
- `docs/product/PRODUCT.md` — scope and specification
- `docs/research/` — facts gathered from Filee, Android stack, model licenses, market
- `docs/qa/verification.md` — what was actually built, tested and observed (and what was not)
- `NOTICE.md` — third-party licenses
