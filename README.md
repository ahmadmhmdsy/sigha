# Sigha — صيغة

**A privacy-first file converter for Android that runs on your device.**

Sigha converts documents, images, PDFs, audio, video, archives and data files on the phone.
Files are not uploaded: the app has no account, no ads and no analytics. The only network
request it can make is the optional OCR model pack, and only after the user confirms it
(the dialog shows the size and warns on metered connections).

> Inspired by the *idea* of [Filee](https://github.com/KnifeLemon/Filee) (MIT, desktop).
> Sigha is an original Kotlin/Jetpack Compose implementation for Android; no Filee code is included.

## Status

| Area | State |
|---|---|
| Build | AGP 8.10.1, Gradle 8.11.1, JDK 21, compile/target SDK 36, min SDK 26 |
| UI | Compose Material 3, English + Arabic (RTL), light/dark |
| Engines | Images, PDF, office-simple, text, data, subtitles, e-books, archives, FFmpeg media |
| Catalogue | 48 formats recognised · 39 convertible from · 25 producible · 32 converters (172 directed edges, 11 families) |
| AI packs | OCR (Tesseract, Apache-2.0) — 28,004,325 B, SHA-256 pinned, resumable, metered warning. Upscaling is **not available yet** |
| Verification | 135 JVM tests green; debug APK 115,043,779 B, all 32 `.so` stored, 16 KB-aligned, zipalign pass — run `scripts/verify.ps1` |
| Release | Signed release APKs (arm64-v8a, x86_64, universal) and AAB built locally with a throwaway keystore — see `docs/qa/verification.md` |
| Device QA | Install + cold start + 120-event monkey on an API 36 emulator: no crash. On-device conversions not yet verified (headless image renders black) |

## Build

Requirements: JDK 17+ (JDK 21 used here), Android SDK platform 36 + build-tools 36.

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot"
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon --console=plain
# APKs: app\build\outputs\apk\debug\app-universal-debug.apk (+ per-ABI splits)
```

Release builds need `keystore/keystore.properties` (git-ignored; see `keystore/README.md`):

```powershell
.\gradlew.bat :app:assembleRelease --no-daemon --console=plain   # ABI splits enabled
.\gradlew.bat :app:bundleRelease   --no-daemon --console=plain   # splits disabled for the bundle
```

Run the whole gate — clean build, unit tests, APK facts, stored/ELF alignment, zipalign,
release APKs + signatures, AAB, lint, and stale-claim drift — with:

```powershell
pwsh -File scripts/verify.ps1
```

It writes `docs/qa/evidence/verify-<timestamp>.json`; that JSON, not prose, is the source of
truth for the numbers quoted above.

## What the build does and does not do

- **Formats:** 48 are recognised in the picker. 39 can be a conversion source and 25 a target;
  the other 9 (TIFF, RTF, TAR, BZ2, XZ, YAML, XML, MOBI, FB2) are catalogued but have no
  converter yet, so the UI never offers an unreachable target for them.
- **Routes:** the planner uses uniform-cost search over converter edges, at most 3 hops, and
  skips converters that emit multiple files when they would sit mid-route.
- **Office/text fidelity:** `.docx/.xlsx/.pptx` are read with Apache POI — text, values and
  images, not pixel-perfect layout. `.epub` becomes text/markdown (DRM-protected books are
  refused). `.pdf` text extraction falls back to OCR for scanned pages.
- **Media:** FFmpegKit with a 20-minute timeout, cancellation, free-space preflight and
  metadata/chapter stripping.
- **Output:** every result goes through MediaStore (Downloads) or a user-granted SAF folder;
  no broad storage permission is requested on Android 10+.
- **Privacy:** see `docs/legal/privacy-policy.md` and the in-app Privacy screen. Data safety
  answers: no data collected, no data shared, no analytics.

## Architecture

```
ui/            Compose screens + ViewModels (workbench, queue, history, settings, packs, licences, privacy)
data/          conversion runner, output publishing (MediaStore), settings, history, pack downloads
work/          foreground conversion service + queue
core/          FileFormat, FormatRegistry, Converter, ConverterRegistry, RoutePlanner
engine/        one package per family; each exposes its own list of Converter edges
model/         downloadable model packs (catalog, SHA-256 verified resumable install)
```

The router is a graph: converters declare `inputs` and `outputs`; the planner finds the
cheapest chain for any source→target pair. Adding a format means adding edges, not editing a
giant `when` statement.

## Licensing

Sigha is original code. Bundled third-party components are Apache-2.0, MIT, BSD, 0BSD, IJG,
SIL OFL, or LGPL-3.0 (FFmpegKit); the full texts ship in-app under Settings → Licences.
GPL/AGPL tools (LibreOffice, Pandoc, Ghostscript, calibre) are deliberately **not** bundled.
Model packs carry their own licences and are downloaded only on explicit user action.

FFmpegKit is LGPL-3.0: `NOTICE.md`, `app/src/main/assets/legal/source-offer.txt` and
`relink-instructions.txt` carry the source offer and relinking information; hosting the
corresponding source is a release blocker tracked in `docs/legal/release-blockers.md`.

## Legal / halal clearance

Before building on Filee's material, a clearance memo was produced:
`docs/legal/clearance-memo.md`. Short version: facts and ideas are free; expression is not.
No Filee code, brand, logo, screenshots or trade dress is used.

## Docs

- `PRODUCT.md` — one-page product summary
- `docs/product/PRODUCT.md` — scope and specification
- `docs/product/store-listing.md` — store copy and screenshot plan
- `docs/product/permissions-rationale.md` — why each permission exists
- `docs/qa/verification.md` — what was actually built, tested and observed (and what was not)
- `docs/qa/evidence/` — machine-readable verification runs
- `docs/legal/` — privacy policy, licence compliance, release blockers, clearance memo
- `docs/adr/` — architecture decision records
- `docs/council/2026-11-17-review/` — the review that drove this production pass
- `NOTICE.md` — third-party notices
