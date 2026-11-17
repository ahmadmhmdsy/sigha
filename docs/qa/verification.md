# Sigha — build, test and runtime verification

Date: 2026-11-17 (Asia/Baghdad). Workspace: `E:\android_apps\digital_converter`.
Everything below was executed in this tree on this date. Anything not executed is named as not
executed — there are no inherited or assumed results here.

The facts below were re-recorded on the **0.2.1** tree (versionCode 3) after the UI/UX fix pass; the
0.2.0 record this file replaced is superseded. The 0.2.1 pass changed app code, resources and one
app asset (the Arabic privacy policy) only — no library, format or routing change.

## 1. Toolchain actually used

| Component | Value | Evidence |
|---|---|---|
| JDK | Eclipse Temurin 21.0.13.9 (JVM target 17) | `scripts/verify.ps1` auto-detects it; `gradlew.bat` logs |
| Gradle | 8.11.1 (wrapper) | `gradle/wrapper/gradle-wrapper.properties` |
| Android Gradle Plugin | 8.10.1 | `gradle/libs.versions.toml` / build logs |
| Kotlin | 2.0.21 | `settings.gradle.kts` plugin versions |
| compileSdk / targetSdk / minSdk | 36 / 36 / 26 | `app/build.gradle.kts`, merged manifest |
| Android SDK / build-tools | `C:\Android\Sdk`, build-tools 36.0.0 | `scripts/verify.ps1` |
| Emulator | AVD `api36`, x86_64 ATD, API 36 (headless) | `scripts/device-smoke.ps1` evidence |

## 2. How this tree is verified

Two scripts are the only accepted evidence, and both write machine-readable JSON:

1. **`scripts/verify.ps1`** — stops stale build daemons, clean build + JVM tests + debug APK,
   then debug facts (size, SHA-256, ABI splits, stored native libs, ELF `PT_LOAD` alignment,
   zipalign), release APKs + `apksigner` verification, AAB, lint, and stale-claim drift over
   `README.md`, `PRODUCT.md` and this file. Evidence: `docs/qa/evidence/verify-<stamp>.json`.
2. **`scripts/device-smoke.ps1`** — installs the universal debug APK on the first attached
   device, cold-starts it, checks the activity is resumed, runs a constrained monkey and scans the
   crash buffer. Evidence: `docs/qa/evidence/device-smoke-<stamp>.json`.

This run: `docs/qa/evidence/verify-20261117-231022.json` (full gate: clean build, tests,
release APKs, AAB, lint, claim drift) and the 0.2.1 device-smoke evidence (path recorded in §7).

## 3. Build results

```
:app:clean :app:testDebugUnitTest :app:assembleDebug
BUILD SUCCESSFUL in 2m 23s
```

```
:app:assembleRelease   -> BUILD SUCCESSFUL (see log below)
:app:bundleRelease    -> BUILD SUCCESSFUL (see log below)
:app:lintDebug        -> BUILD SUCCESSFUL
```

Release and bundle are **separate Gradle invocations on purpose**: AGP 8.9+ fails
`bundleRelease` while ABI splits are enabled (issuetracker.google.com/issues/402800800), so
`app/build.gradle.kts` enables splits only when the requested task list does not contain
`bundle`. Play splits the AAB itself, so the bundle does not need splits.

Lint: **0 errors, 58 warnings** (`app/build/reports/lint-results-debug.txt`). The gate
fails on any error; warnings are accepted for now.

## 4. Unit tests (`:app:testDebugUnitTest`)

**16 test classes, 137 tests, 0 failures, 0 errors, 0 skipped.**

| Test class | Tests |
|---|---|
| core.RoutePlannerTest | 4 |
| data.HistoryCodecTest | 8 |
| data.UniqueNamesTest | 7 |
| engine.archive.ArchiveGuardsTest | 7 |
| engine.data.CsvTest | 11 |
| engine.data.JsonCsvTest | 12 |
| engine.media.FfmpegArgsTest | 26 |
| engine.media.FfmpegProgressTest | 3 |
| engine.PdfPageSelectionTest | 8 |
| engine.subtitle.SubtitleFormatTest | 10 |
| engine.text.HtmlTextExtractorTest | 7 |
| engine.text.MarkdownParserTest | 10 |
| model.DigestTest | 3 |
| model.OcrLanguagesTest | 6 |
| model.OcrPageRangeTest | 7 |
| model.PackCatalogTest | 6 |

Coverage is logic-level only: routing, argument builders, progress parsing, parsers, digests,
pack catalogue. No Android framework, SAF, MediaStore, FFmpegKit or Tesseract code is exercised
by JVM tests.

## 5. Debug APK facts (universal, the one the smoke test installs)

| Fact | Value |
|---|---|
| Path | `app/build/outputs/apk/debug/app-universal-debug.apk` |
| Size | **115,117,750 bytes** (109.8 MiB) |
| SHA-256 | `C2E7C5762F41516A645BEA14A7E3CC2AA10567FAAE4139FD5F0CD856A1C793E1` |
| ABI splits | `app-arm64-v8a-debug.apk` 68,795,074 B · `app-x86_64-debug.apk` 75,909,618 B |
| Version | `versionCode 3`, `versionName 0.2.1` |
| Package / label | `com.sigha.converter` · Sigha / صيغة |
| minSdk / targetSdk | 26 / 36 |
| Native ABIs | `arm64-v8a`, `x86_64` — 16 `.so` each, 32 total |
| Native packaging | **32 of 32 stored (uncompressed)**; `jniLibs.useLegacyPackaging = false` |
| ELF 16 KB page alignment | 16 arm64 `.so` checked, **min `PT_LOAD p_align` = 16384** |
| zipalign | `zipalign -c -P 16 -v 4` → **exit 0** |
| Signing | debug keystore (not a release key) |
| Permissions | `INTERNET`, `ACCESS_NETWORK_STATE`, `POST_NOTIFICATIONS`, `WRITE_EXTERNAL_STORAGE` (maxSdk 28), `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC` |
| Components | `MainActivity` (MAIN/LAUNCHER + SEND/SEND_MULTIPLE/VIEW `*/*`), `ConversionService` (`dataSync`, not exported) |

The APK carries FFmpegKit (LGPL-3.0, `libav*`), Tesseract4Android, PDFBox-Android and the app's
own Kotlin/Compose code. ONNX Runtime is not in the dependency set; upscaling is not shipped.

## 6. Release artefacts

Built with the git-ignored local verification keystore (`keystore/`, documented in
`keystore/README.md`) — **not a Play upload key**.

| Artefact | Size | SHA-256 | Signature |
|---|---|---|---|
| `app-arm64-v8a-release.apk` | 49,815,043 B | `D9B357D28E03CF45AB782D98230C2801D23E13321521E57333F708A148545963` | `apksigner verify` pass (CN=Sigha Local Verification) |
| `app-x86_64-release.apk` | 56,929,587 B | `19E7A56B18DC4F94EF53D492848D6C26E2D89EBABF266634890B7EAB28D1625B` | `apksigner verify` pass |
| `app-universal-release.apk` | 96,137,719 B | `5F4A04D4AAEECC65122771AFD86B5C5D4C5222E3A3DACD403C4A55AB65BCF87A` | `apksigner verify` pass |
| `app-release.aab` | 51,154,859 B | `44FBDBA358ED03615CB659053FDDAB63104C3095EE0C4FC052EE17F6B8978EAC` | built by AGP with the same signing config |

Release builds run R8 (`minifyRelease`, `shrinkReleaseResources`) and the release manifest does
not opt into legacy native-lib extraction, so native libraries are page-aligned and stored
uncompressed.

## 7. Runtime smoke test (emulator-5554, Android 16 / API 36, x86_64 ATD image)

`scripts/device-smoke.ps1` result on the 0.2.1 build (evidence
`docs/qa/evidence/device-smoke-20261117-232531.json`):

| Step | Result |
|---|---|
| install `app-universal-debug.apk` | `Success` |
| cold start `am start -W` | `Status: ok`, `LaunchState: COLD`, `TotalTime: 5305 ms` |
| process alive | pid 4653 |
| activity resumed | `topResumedActivity=com.sigha.converter/.MainActivity` |
| monkey | 120/120 events injected into `com.sigha.converter` |
| crash buffer | no entry for `com.sigha.converter`; process still alive after the run |

The 0.2.1 build was also installed and exercised on the windowed AVD described in §7.1 (real
DocumentsUI picker, a TXT→PDF conversion, history row and Arabic privacy screen).

What the smoke run already caught and this tree fixes: on this stripped ATD image the first run
tapped “Choose files”, and the system has **no documents provider**, so
`startActivityForResult(ACTION_OPEN_DOCUMENT)` threw `ActivityNotFoundException` and killed the
app. `ui/SafLaunchers.kt` now wraps every SAF launcher (`launchSaf`) and shows a localized
message instead; the re-run above passes. A real device with DocumentsUI takes the normal path.

Not verifiable on this host, and therefore **not claimed**:

- Visual rendering **on the headless ATD image** (`emulator-5554`). `screencap` returns the same
  15,580-byte all-black PNG for Sigha and for pre-installed apps; that image does not produce usable
  frames, and `uiautomator dump` is unreliable there (it once reported another app's hierarchy while
  Sigha was resumed). The 0.2.1 visual pass therefore ran on a second, windowed AVD — see §7.1.
- Any real conversion (image/PDF/office/media/OCR/archive), SAF folder grants, MediaStore
  publishing, notification/queue behaviour, or API 26–28 paths. Those need a physical device.
- The emulator is shared with another app (`com.sabeel.steps`) that auto-starts and steals the
  foreground; the smoke script stops it for the duration of the run so the monkey cannot inject
  into the wrong package.

### 7.1 Visual and Arabic RTL pass (emulator-5566, 0.2.1)

The headless ATD image cannot capture frames, so the visual pass ran on a second, windowed AVD
(`review36`, Android 16 / API 36, x86_64, 1080×2400 @ 420 dpi) with the debug x86_64 APK. Arabic
was applied per app (`cmd locale set-app-locales com.sigha.converter --locales ar`), rotation with
`wm user-rotation lock`, font scale with `settings put system font_scale`, dark with
`cmd uimode night`. Screenshots are archived in [`docs/qa/evidence/ui-021/`](evidence/ui-021/):

| Screenshot | State | Verified |
|---|---|---|
| `home-empty-ar-portrait.png` | Arabic, light, portrait | mirrored chrome and tab order; icon, title, body, CTA and privacy footnote all present; one primary action |
| `home-empty-ar-landscape.png` | Arabic, light, 2400×1080 | the 0.2.0 landscape P0 is gone: every empty-state element fits above the navigation bar |
| `home-empty-ar-landscape-font200-scrolled.png` | landscape, font scale 2.0 | the empty state scrolls instead of clipping; CTA and footnote reachable |
| `home-empty-ar-dark.png` | Arabic, forced dark on a light system | dark surfaces `#0B100E`, light text, mint primary, legible system bars |
| `settings-ar.png` | Arabic settings | theme chips, output-mode radios, ≥48 dp choose-folder, OCR and model-pack rows |
| `workbench-file-ar.png` | one TXT selected | file card (name/type/size), target groups, disabled Convert with the target hint |
| `workbench-target-ar.png` | PDF target selected | selected-chip state, enabled Convert, hint removed |
| `convert-done-ar.png` | conversion finished | progress section, done row with localized status pill and open action |
| `history-ar.png` | history after a TXT→PDF run | localized route announcement; date and size on separate atomic lines |
| `privacy-policy-ar.png` | Arabic privacy screen | `privacy-policy.ar.md` rendered RTL: nine sections and the localized effective date |

`uiautomator dump` of the history row confirms the accessibility work: the source→target pair is one
node with content description "TXT إلى PDF", and the open/share actions carry their own labels.

Not covered by this pass: TalkBack speech output, a native Arabic copy review, OEM skins, and real
hardware. Those remain on the device QA checklist.

## 8. Known gaps and limitations (as shipped)

1. **OCR pack is download-only** (28,004,325 B, tessdata_best eng+ara, Apache-2.0, SHA-256
   pinned, resumable). OCR needs one confirmed download before first use; afterwards it works
   without a network. Scanned-PDF → TXT falls back to OCR when the text layer is blank.
2. **9 of 48 formats are catalogue-only** (TIFF, RTF, TAR, BZ2, XZ, YAML, XML, MOBI, FB2): no
   converter edge, so the UI never offers them as reachable targets. 39 formats can be a source
   and 25 a target, through 32 converters (172 directed edges, 11 families, 10 productive).
3. **No RAR** (unrar licence is not redistributable) and **no 7z write** (7z → ZIP only).
4. **Office fidelity**: DOCX/XLSX/PPTX/ODT/ODS are read as OOXML/ODF structures — headings,
   paragraphs, flattened tables, first worksheet only. No layout, formulas, headers or footnotes.
5. **Media**: FFmpegKit output depends on the bundled codecs; H.264/HEVC/AAC/MP3/VP8/VP9 work,
   exotic codecs may not. A 20-minute timeout, cancellation and a free-space preflight are in place.
6. **Upscaling is not available yet** — it is labelled that way in the UI, and ONNX Runtime is not
   in the APK.
7. **Engine failure diagnostics are English** by design: the UI shows a localized headline from
   `ConversionFailureKind` and keeps the raw English engine message behind an expandable details
   control. Lossy-conversion **notes** are no longer English: they are typed (`core.Note`) and
   rendered from resources by `NoteUi` in the queue, history and workbench.
8. **x86_64 ELF alignment is not asserted** (the 16 KB requirement applies to arm64 devices);
   only the arm64 `PT_LOAD` alignment is checked by the gate.
9. **Debug/release keystore**: the release artefacts here are signed with a throwaway local key.
   Play needs the publisher's upload key (blocker B1 in `docs/legal/release-blockers.md`).

## 9. Reproduce

```powershell
Set-Location 'E:\android_apps\digital_converter'

# Whole gate: clean build, tests, debug facts, release APKs, AAB, lint, claim drift
pwsh -File scripts/verify.ps1

# Runtime smoke on the first attached device/emulator (installs the debug universal APK)
pwsh -File scripts/device-smoke.ps1

# Quick variants
pwsh -File scripts/verify.ps1 -SkipBuild   # facts about the artefacts that already exist
```

`scripts/verify.ps1` finds the JDK itself if `JAVA_HOME` is not set, and stops leftover
Gradle/Kotlin daemons before `:app:clean` (Windows keeps the lint cache locked otherwise).

## 10. Verdict

**Build, packaging and JVM verification: green.** Clean build, 135 unit tests, stored and 16
KB-aligned native libraries, zipalign, signed release APKs, an AAB and zero lint errors all pass
from the commands in §9, and the claim-drift check keeps the public docs tied to measured numbers.

**Device verification: partial.** Install, cold start, resumed activity, a 120-event monkey run
and an empty crash buffer are recorded. Real conversions, SAF flows, MediaStore output and visual
rendering are **not** verified yet: run `docs/qa/device-checklist.md` on a physical API 26–36
device before submitting to Play. The app must not be described as device-tested until that is
done.
