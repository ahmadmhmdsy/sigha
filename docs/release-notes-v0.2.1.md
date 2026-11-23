# Sigha 0.2.1 — صيغة

**This is not a Google Play Store release.** The APKs attached to this release are side-load QA
builds signed with the standard Android debug key; there is no Play listing, and the shipped legal
texts still contain the 21 placeholders that block a Play upload. Install with "Install unknown
apps", expect a Play Protect warning about an unknown developer, and treat this as a test build.

Sigha (صيغة) is an offline, privacy-first file converter for Android: pick files with the system
picker (or share them in), choose a target format, and the conversion runs on the phone. There is no
account, no ads, no analytics and no upload — the only network request the app can make is the
optional OCR language pack, and only after an explicit confirmation. Version 0.2.1 is the project's
first GitHub release and completes the 0.2.x capability catalogue (48 formats recognised, 39
convertible sources, 25 producible targets, 32 converters over 172 directed edges in 11 families)
together with the UI/UX, Arabic, accessibility and reliability passes that made it usable. The
changelog section for 0.2.1 dates from 2026-11-17; the APKs here were rebuilt from the final 0.2.1
hardening tree on 2026-11-22 and passed the full verification gate.

## Highlights

- **Conversion never leaves the phone.** Documents, images, PDF, audio, video, archives, data,
  subtitles and e-books are converted locally with bundled engines (PDFBox-Android,
  commons-compress/xz, Tesseract4Android, FFmpegKit) and first-party readers for Office, data and
  e-book formats. No account, no ads, no telemetry.
- **English and Arabic, properly.** Full Arabic UI with RTL layout, six-category Arabic plurals,
  Arabic-Indic page-range input, a dedicated Arabic type scale, and a localised privacy policy asset.
- **A conversion graph, not a format list.** The workbench offers only targets reachable from the
  selection, so it never promises a conversion the build cannot perform. Nine recognised formats
  (TIFF, RTF, TAR, BZ2, XZ, YAML, XML, MOBI, FB2) have no converter yet and are never offered as
  targets.
- **Results you can find.** Output lands in the collection the gallery/music app looks at —
  `Pictures/Sigha`, `Movies/Sigha`, `Music/Sigha` or `Download/Sigha` — or in a folder you grant
  through the system picker.
- **A verified build.** 155 JVM unit tests (0 failures), lint 0 errors, and 37/37 end-to-end
  emulator smoke steps, all recorded as machine-readable evidence in the repository.

## What's new in 0.2.1

### Workbench and queue

- The OCR pack is no longer a dead end: install it from an inline card on the same screen, watch the
  download progress, cancel it, and the OCR targets appear live without reselecting files.
- Convert is never disabled without a reason — the bar says what is missing.
- The action names its target ("Convert 3 files to PDF") and never ends in a dangling "to".
- While a batch runs, the pinned bar shows the batch position and the current file name.
- The target is remembered per category (images, documents, audio, video).
- "Retry failed (N)" re-runs failed rows with the same target and options; a source that can no
  longer be opened is reported as such and the dead retry action is not offered.
- Folder mode names the destination folder instead of a bare file name.
- Files shared in from other apps open the workbench and announce "1 file added".

### Results, history and sharing

- Outputs are routed to gallery-visible MediaStore collections, with a fallback to Downloads when a
  collection refuses the insert.
- The workbench result card, the queue and history all offer Share; "Share all N files" opens the
  system sheet once with the whole batch.
- The batch completion notification appears when a conversion finishes while the app is in the
  background, and its tap target brings the app back.
- Empty History and Queue screens now lead back to Convert.

### Arabic, accessibility and theming

- Typed, serialisable conversion notes (`core.Note`) replaced free-text English engine messages and are
  localised in Arabic and English.
- Counted strings use Android plurals with all six Arabic categories; pages validation accepts
  Arabic-Indic and Extended Arabic-Indic digits and separators and reports invalid input inline.
- Arabic type scale, Arabic OCR language defaulting from the device locale, and an Arabic privacy
  policy asset selected when the app locale is Arabic.
- State is announced, not only painted: status pills expose state descriptions, section headers are
  headings, the quality slider is named, progress rows are merged for TalkBack, and a source→target
  pair is announced as one phrase.
- Contrast and touch targets fixed (format badges, status pills, "Choose folder", one-line top bar at
  200% font scale).
- A single chrome owner for the app Scaffold and insets; system bars follow the resolved theme;
  the Material 3 role ramp, shapes and a night window background are authored in the Sigha palette.

### Reliability

- Silent videos no longer offer audio targets; media failures are typed (`NO_AUDIO_TRACK`,
  `NO_VIDEO_TRACK`) instead of a generic message.
- Publishing is per output: a page that cannot be saved no longer discards the pages that were, and
  partial results are reported.
- MediaStore inserts are transactional, legacy (API 26–28) saves resolve the scanned URI, and SAF
  copies are cancellation-aware and read the destination name back.
- A removed or revoked output folder falls back to Downloads instead of failing a finished
  conversion.
- History decoding is per row: a row written by another version is dropped, not the whole list.
- Every converter family is registered defensively, so a wiring bug in one family cannot crash app
  start.
- Resource bounds: JSON nesting is checked before parsing, text→PDF is capped at 400 pages,
  PDF→images at 500 pages and PDF→text at 2,000 pages with an actionable page-range message.
- Pack downloads verify a full-length `.part` before resuming and restart from zero if it is corrupt.
- Android 15+ foreground-service timeouts are handled, and the start/stop race behind
  `ForegroundServiceDidNotStartInTimeException` is fixed.

## Fixes

- Fixed the app being killed a few seconds after a fast-failing retry (foreground-service
  start/stop race).
- Fixed the Android 15+ `dataSync` foreground-service timeout crash.
- Fixed images and audio landing where galleries and music apps could not see them.
- Fixed OCR and scanned-PDF failures showing a generic headline instead of the typed install
  instruction (`NO_TEXT_LAYER`, `MODEL_PACK_MISSING`).
- Fixed the completion action bar producing "Convert 3 files to " with no target.
- Fixed a start-up crash caused by a catalogue id/extension drift.
- Fixed page ranges typed with Arabic-Indic digits not being parsed.
- Fixed Arabic plural quantity for page-limited notes.
- Fixed history rows with dates/sizes breaking mid-word in Arabic.
- Fixed media probing for video files that have no audio track.
- Fixed landscape dead ends on small viewports and at 200% font scale.
- Fixed stale history rows from older versions taking the list down with them.
- Fixed a corrupt partial pack download resuming into a broken install.

## Verification

| Check | Result |
|---|---|
| JVM unit tests | **155 tests in 19 classes, 0 failures / 0 errors / 0 skipped** |
| Lint | **0 errors**, 66 warnings (dead resources and deprecation notices, not behaviour) |
| Full build gate | clean build, debug + release APKs, AAB, signing recorded, 32/32 native libraries stored, 16 KB ELF alignment, `zipalign -c -P 16` — **all checks passed** |
| Emulator feature smoke | **37/37 steps passed**, 0 crashes, on an API 36 x86_64 emulator |
| EN↔AR resource parity | pass |
| Release-candidate gate | **fails by design** — 21 placeholder markers remain in the shipped legal text |

Evidence in the repository:

- `docs/qa/evidence/verify-20261122-215316.json` — the authoritative gate run quoted above.
- `docs/qa/evidence/features-20261122-214849/` — feature smoke run with screenshots.
- `docs/qa/verification.md` — the human-readable record, including what was **not** verified.

Not verified for this release: a physical arm64 device, the arm64 release artefact on hardware, and
the Arabic privacy-policy text by a native reviewer. Those gaps are documented rather than hidden.

## Downloads

| File | Size | SHA-256 |
|---|---|---|
| `Sigha-0.2.1-universal-debug.apk` (recommended) | 115,181,014 B | `FAED0769EE8D2B7F80083808EF860E6BE5A0F60F150FE43205AD891F2A691B4F` |
| `Sigha-0.2.1-arm64-debug.apk` (arm64 phones only, smaller) | 68,858,338 B | `00A7A41A98903730DE6D412234B82FAD1DFE0DAB44698E2A5326923EA060B8C5` |

`SHA256SUMS.txt` contains the same digests. Both files carry the same code, resources and features;
the arm64 file is smaller because it contains one native ABI instead of two. Android 8.0+ (API 26),
ABIs arm64-v8a and x86_64.

## Install

1. Copy the APK to the phone (USB, Drive, Telegram — any transfer works), or install over adb with
   `adb install -r Sigha-0.2.1-universal-debug.apk`.
2. Open the file on the phone. Android asks to allow installs from that app ("Install unknown apps") —
   allow it for that one app only, then tap **Install**. Play Protect may warn about an unknown
   developer: this is expected for a debug-key build; choose "Install anyway".
3. The first OCR run downloads the English + Arabic language pack (~28 MB) after an explicit
   confirmation; after that, OCR works offline. Every other conversion works with no network at all.

## Known limitations

- **Not a Play Store build.** Debug-key signature, no Play listing, and 21 placeholders (privacy
  policy URL and contacts, LGPL corresponding-source URL and mailbox) still block a Play release; see
  `docs/legal/release-blockers.md`.
- **HEIC is not supported on Android 8.0–8.1** (API 26–27). It works on later versions.
- **AI upscaling is not available** in this build; the family is registered empty, and the UI does not
  advertise it.
- **Device QA scope:** runtime verification happened on API 36 x86_64 emulators only. No physical
  phone, and no arm64 hardware, has been tested yet; on-device conversions on arm64 are unverified.
- **Nine recognised formats have no converter:** TIFF, RTF, TAR, BZ2, XZ, YAML, XML, MOBI, FB2. They
  are catalogued so the app can explain why it cannot help, not advertised as supported.
- **Office conversion is extraction, not layout:** DOCX/XLSX/PPTX are read for text, values and
  images, not pixel-perfect layout.
- **The Arabic privacy-policy text has not had a native Arabic review** — it is a documented release
  gate, not a claim of approval.
- The app is a phone-first 0.2.x build: tablets, foldables and desktop layouts are out of scope.

## Links

- Repository: <https://github.com/ahmadmhmdsy/sigha>
- Website: <https://ahmadmhmdsy.github.io/sigha/>
- Privacy policy (repository copy, pending the publisher's legal values):
  `docs/legal/privacy-policy.md`
- Changelog: `CHANGELOG.md`
- Verification record: `docs/qa/verification.md`
- Release blockers: `docs/legal/release-blockers.md`
- All releases: <https://github.com/ahmadmhmdsy/sigha/releases>
