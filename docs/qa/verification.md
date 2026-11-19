# Sigha — build, test and runtime verification

Date: 2026-11-19 (Asia/Baghdad); the 0.2.1 UI/UX pass was recorded on 2026-11-18. Workspace:
`E:\android_apps\digital_converter`. Everything below was executed in this tree on those dates.
Anything not executed is named as not executed — there are no inherited or assumed results here.

The facts below were re-recorded on the **0.2.1** tree (versionCode 3) after the UI/UX fix pass and
again after the critique re-run's regression-closure commit; the earlier records this file replaced
are superseded. On 2026-11-19 the **upload-tree** commit added batch-position legibility, the saved
destination in the completion summary, target-named actions, thrown OCR/PDF failure kinds, the
Android 15 foreground-service timeout, removal of the unavailable upscaling card, and a hardened
gate (EN↔AR parity, release-candidate placeholder gate, recorded release signer). That pass changed
app code, resources, scripts and docs only — no library, format or routing change — and its runtime
check is recorded in §7.2.

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

This run: `docs/qa/evidence/verify-20261119-004710.json` (full gate after the upload-tree commit:
clean build, 137 tests, release APKs, AAB, lint, claim drift, EN↔AR resource parity, placeholder
scan, release signer). The 0.2.1 runs remain as `verify-20261118-193810.json` and the
`-SkipBuild` re-check `verify-20261118-195341.json`; the 0.2.1 device-smoke evidence is recorded
in §7.

## 3. Build results

```
:app:clean :app:testDebugUnitTest :app:assembleDebug   -> BUILD SUCCESSFUL
:app:assembleRelease                                  -> BUILD SUCCESSFUL
:app:bundleRelease                                    -> BUILD SUCCESSFUL
:app:lintDebug                                        -> BUILD SUCCESSFUL
```

Each invocation's exit code is recorded in the evidence JSON (`steps`). Wall-clock time depends on
host load and is not a claim; the full gate ran in about 12 minutes on this host.

Release and bundle are **separate Gradle invocations on purpose**: AGP 8.9+ fails
`bundleRelease` while ABI splits are enabled (issuetracker.google.com/issues/402800800), so
`app/build.gradle.kts` enables splits only when the requested task list does not contain
`bundle`. Play splits the AAB itself, so the bundle does not need splits.

Lint: **0 errors, 63 warnings** (`app/build/reports/lint-results-debug.txt`). The gate
fails on any error; warnings are accepted for now. The five new warnings are the unused
`model_packs_upscale_*` strings left behind when the unavailable upscaling card was removed; they
are dead resources, not behaviour.

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
| Size | **115,120,790 bytes** (109.8 MiB) |
| SHA-256 | `C346BBAE99FD7C3B1C5B1721FBE262D7200EEE6222E231816A180F03683BCC86` |
| ABI splits | `app-arm64-v8a-debug.apk` 68,798,114 B · `app-x86_64-debug.apk` 75,912,658 B |
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
| `app-arm64-v8a-release.apk` | 49,817,303 B | `8E2B7D6CED9745CF1BDB49CF8BF262CEAD8BBF106A419FAEF47B86AA7A172406` | `apksigner verify` pass (CN=Sigha Local Verification) |
| `app-x86_64-release.apk` | 56,931,847 B | `0F58728B8354FEC645625E5C4DAAE07DE0304764E0D3CA0D04C03D9315763BCA` | `apksigner verify` pass |
| `app-universal-release.apk` | 96,139,979 B | `0832E6214BB4FDBA612FA89970301B6907506CD7E9F4F95C793A1B82154CDB03` | `apksigner verify` pass |
| `app-release.aab` | 51,159,496 B | `CAC83C515489F75B108CCA02786539765BC56D0C6B0FB8741A247AD612E6CEA8` | built by AGP with the same signing config |

Release builds run R8 (`minifyRelease`, `shrinkReleaseResources`) and the release manifest does
not opt into legacy native-lib extraction, so native libraries are page-aligned and stored
uncompressed.

## 7. Runtime smoke test (emulator-5554, Android 16 / API 36, x86_64 ATD image)

`scripts/device-smoke.ps1` result on the 0.2.1 build (evidence
`docs/qa/evidence/device-smoke-20261118-195244.json`):

| Step | Result |
|---|---|
| install `app-universal-debug.apk` | `Success` |
| cold start `am start -W` | `Status: ok`, `LaunchState: COLD`, `TotalTime: 6763 ms` |
| process alive | pid 4950 |
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

### 7.1 Visual and Arabic RTL pass (emulator-5556, 0.2.1)

The headless ATD image cannot capture frames, so the visual pass ran on a second, windowed AVD
(`review36`, Android 16 / API 36, x86_64, 1080×2400 @ 420 dpi) with the debug x86_64 APK. Arabic
was applied per app (`cmd locale set-app-locales com.sigha.converter --locales ar`), rotation with
`wm user-rotation lock`, font scale with `settings put system font_scale`, dark with
`cmd uimode night`. Screenshots are archived in [`docs/qa/evidence/ui-021/`](evidence/ui-021/):

| Screenshot | State | Verified |
|---|---|---|
| `home-empty-ar-portrait.png` | Arabic, light, portrait | mirrored chrome and tab order; icon, title, body, CTA and privacy footnote all present; one primary action |
| `home-empty-en.png` | English, light, portrait | LTR mirror of the same empty state: English copy, "Choose files", privacy footnote and Convert→History→Settings tab order |
| `home-empty-ar-landscape.png` | Arabic, light, 2400×1080 | the 0.2.0 landscape P0 is gone: every empty-state element fits above the navigation bar |
| `home-empty-ar-landscape-recheck.png` | Arabic, light, 2400×1080, fresh install, 3-button nav | re-captured from the final tree for the critique re-run: the content is centred inside the real safe area (128 px left display cutout + 126 px right nav strip), not at the raw screen centre |
| `home-empty-ar-landscape-font200-scrolled.png` | landscape, font scale 2.0 | the empty state scrolls instead of clipping; CTA and footnote reachable |
| `home-empty-ar-dark.png` | Arabic, forced dark on a light system | dark surfaces `#0B100E`, light text, mint primary, legible system bars |
| `settings-ar.png` | Arabic settings | theme chips, output-mode radios, ≥48 dp choose-folder, OCR and model-pack rows |
| `settings-full-ar.png` | Arabic settings, scrolled | system theme chip selected, output-mode radios, folder row, OCR language and model-pack rows with their labels |
| `model-packs-ar.png` | model packs, OCR pack installed (re-captured after the regression closure) | installed pack card with size and licence; the upscaling row's "not available yet" pill is neutral, not the sand selection colour |
| `workbench-file-ar.png` | one TXT selected | file card (name/type/size), target groups, disabled Convert with the target hint |
| `workbench-target-ar.png` | PDF target selected | selected-chip state, enabled Convert, hint removed |
| `ime-pages-ar-recheck.png` | keyboard open on the PDF pages field | re-captured from the final tree: the navigation bar sits directly above the IME, no stacking gap |
| `convert-done-ar.png` | conversion finished | progress section, done row with localized status pill and open action |
| `history-ar.png` | history after a TXT→PDF run (re-captured after the regression closure) | one outlined `SighaCard` row with a single open target (the row) plus share; localized route announcement; date and size on separate atomic lines |
| `queue-empty-ar.png` | Arabic, empty queue | back arrow, localized title and body, centred empty state; nothing clipped at font scale 1.0 |
| `privacy-policy-ar.png` | Arabic privacy screen | `privacy-policy.ar.md` rendered RTL: nine sections and the localized effective date |
| `ocr-pack-card-ar.png` | image selected, OCR pack missing | inline OCR install card with size; OCR-only targets hidden |
| `ocr-pack-dialog-ar.png` | install confirmation | the pack dialog states the size, contents and one-time download |
| `ocr-pack-download-ar.png` | download running | live progress bar and cancel action on the workbench card |
| `ocr-unlocked-ar.png` | download finished | the hint is gone and the "النصوص / TXT" target appears without touching the selection — the 0.2.0 OCR dead end, end to end |

The OCR sequence was exercised end to end on this AVD: with the pack absent the card offered the
install, the 28,004,325-byte download ran to completion on the card, and the TXT target then
appeared live — no navigation, no change to the selection.

Sixteen of these captures were taken on the fix-pass commit; `history-ar.png`, `model-packs-ar.png`
and the two `*-recheck.png` captures were re-taken on 2026-11-18 after the regression-closure commit,
so the card grammar, the neutral "not available yet" pill and the window measurements shown are the
shipped ones.

`uiautomator dump` of the history row confirms the accessibility work: the source→target pair is one
node with content description "TXT إلى PDF", the row itself is the open target, and the share
action keeps its own label.

One emulator artefact, recorded for honesty: while the host was also building the release
artefacts, the windowed AVD raised one "Sigha isn't responding" dialog during a swipe. The ANR
trace shows the main thread blocked in the hardware renderer's draw sync
(`DrawFrameTask::drawFrame`), not in app code; the idle device smoke (120/120 monkey, no crash) and
every subsequent capture passed. It is treated as an emulator-under-load artefact, not an app
finding, and it is not covered by the smoke evidence.

Not covered by this pass: TalkBack speech output, a native Arabic copy review, OEM skins, and real
hardware. Those remain on the device QA checklist.

### 7.2 Upload-tree legibility pass (2026-11-19, emulator-5560, Android 16)

The upload-tree commit was exercised on a second boot of the same `review36` AVD (Android 16 /
API 36, x86_64, 1080×2400 @ 420 dpi) with the universal debug APK built from this tree. Files were
selected through the **real DocumentsUI picker** (three PNGs, then the 539 kB `iraq-law-3-1971-en.pdf`),
not through synthetic shares; Arabic was applied per app (`cmd locale set-app-locales
com.sigha.converter --locales ar`). Screenshots are archived next to the gate evidence:

| Screenshot | State | Verified |
|---|---|---|
| `upload-tree-en-cta-no-target.png` | English, 3 PNGs, no target | the action reads **"Convert 3 files"** and is disabled — no dangling "to" while no target exists |
| `upload-tree-en-cta-target.png` | English, PDF target selected | the action reads **"Convert 3 files to PDF"**; the selected chip carries the check mark |
| `upload-tree-en-bar.png` | English, PDF→PNG running | the pinned bar shows **"Converting 1 of 1 to PNG"**, the current file name and Cancel; the action row is replaced, not overlaid |
| `upload-tree-en-done.png` | English, batch finished | the summary reads **"All files converted · saved to Download/Sigha"** — the destination comes from the queue's own path |
| `upload-tree-en-history.png` | English history | the three PDF outputs are listed with date and size |
| `upload-tree-ar-cta-no-target.png` | Arabic, 3 PNGs, no target | **"حوّل 3 ملفات"** — the Arabic plural without the dangling preposition |
| `upload-tree-ar-cta-target.png` | Arabic, PDF target selected | **"حوّل 3 ملفات إلى PDF"** |
| `upload-tree-ar-bar.png` | Arabic, PDF→PNG running | **"جارٍ تحويل 1 من 1 إلى PNG"**, bidi-isolated target name, file name and **إلغاء**, full RTL bar |
| `upload-tree-ar-done.png` | Arabic, batch finished | **"اكتمل تحويل جميع الملفات · حُفظت في Download/Sigha"** |
| `upload-tree-en-cta-pdf-to-png.png` | English, one PDF, PNG target | the single-file action names its target ("Convert to PNG") |

Two things this pass caught and this tree fixes:

1. The first capture showed **"Convert 3 files to"** / **"حوّل ٣ ملفات إلى"** when no target was
   selected: the new target-named action had no empty-target fallback. The action now falls back to
   the generic `workbench_convert` / `workbench_convert_files` strings, and the table above is the
   re-capture from the fixed build.
2. The Arabic plural for the action has to agree with the counted noun; the six-category
   `workbench_convert_files_to` and `workbench_convert_files` plurals are what the captures show,
   and the gate now fails if any Arabic category is missing.

Not verified by this pass: a **multi-file** running bar (the PNG→PDF batch completed before a frame
could be captured; the position counter is the same composable, and the string gate covers the
"1 of N" placeholders, but no screenshot shows "2 of 3"), physical arm64 hardware, a native Arabic
copy review, TalkBack, OEM skins, MediaStore publishing on API 26–28, and the Android 15
foreground-service timeout (compile-time override only; the emulator is API 36 but the service was
not driven to its 6-hour budget).

Host note, recorded for honesty: this machine was shared with another agent session that started and
killed emulators on ports 5554/5556 while the gate was building; one of those boots raised a
"System UI isn't responding" dialog. The captures above were taken on a private port (5560) with a
quiet host, and the gate run is `verify-20261119-004710.json`. The emulator is not a substitute for
the physical-device checklist.

### 7.3 Release-artefact install and run (2026-11-19, emulator-5554/5562)

Until this pass the release artefacts had only been inspected and signed; **no release build had
ever been installed and started**. Three builds were installed and run through
`scripts/device-smoke.ps1` (which gained `-Serial` and an API-26-compatible resumed-activity check
in this pass):

| Evidence | Artefact | Device | Result |
|---|---|---|---|
| `device-smoke-20261119-183558.json` | `app-universal-release.apk` | API 36, x86_64 | **FAIL** — "activity resumed" only: the POST_NOTIFICATIONS dialog took the foreground. The app had not crashed; the script now grants the permission for the run. |
| `device-smoke-20261119-183748.json` | `app-universal-release.apk` | API 36, x86_64 | **PASS** — install Success, cold start 3910 ms, process alive (pid 7972), activity resumed, 60/60 monkey events, empty crash buffer. |
| `device-smoke-20261119-184015.json` | universal APK **extracted from `app-release.aab`** with bundletool 1.18.0, signed by the QA key | API 36, x86_64 | **PASS** — install Success, cold start 3962 ms, resumed, 60/60 monkey events, no crash. This is the artefact chain Play actually ingests: the AAB the store would receive yields an installable, runnable APK. |
| `device-smoke-20261119-184317.json` | `app-universal-release.apk` | **API 26**, x86_64 | **FAIL** — script bug only: API 26 prints `mResumedActivity`, not `topResumedActivity`; the app was resumed. Fixed in the script. |
| `device-smoke-20261119-184528.json` | `app-universal-release.apk` | **API 26**, x86_64 | **PASS** — install Success, cold start 5959 ms, activity resumed (`mResumedActivity`), 60/60 monkey events, no crash. |

Static facts of the AAB-derived APK (`aapt2 dump badging/xmltree`): `com.sigha.converter`,
versionCode 3, versionName 0.2.1, minSdk 26, targetSdk 36, ABIs `arm64-v8a`, `x86_64`, 32 native
libraries stored uncompressed in the APK and loaded from there (not extracted at install time),
`ConversionService` with
`android:foregroundServiceType="dataSync"`. The extracted universal APK is 96,144,613 B (the
`.apks` container is 96,144,917 B).

What this pass found and did **not** fix: while exercising a video→MP3 conversion on the AAB-derived
build, the queue failed with the generic "Conversion failed." The sample clip
(`/sdcard/Movies/clip-sage.mp4`, generated in an earlier visual pass) carries **no audio track** —
verified by pulling the file and finding `vide`/`avc1` but no `soun`/`mp4a` atom. The failure is
therefore correct behaviour for a silent video, but two product issues remain: (a) the video still
offers every audio target, and (b) the message does not say why. Both are candidates for the next
app-side pass; they are not a release-build regression (R8 did not change this path).

### 7.4 Release conversion matrix and OCR runtime proof (2026-11-19, emulator-5554)

Nine conversions were then run **through the real UI** on the same AAB-derived universal APK — the
R8-processed build the store chain produces. Each case went: cold start → DocumentsUI picker
(Show roots → Downloads) → source row → target chip → target-named Convert action. Every output was
checked twice: MediaStore `content://media/external/downloads` (`relative_path = Download/Sigha/`)
and a host-side pull with magic-byte and size checks. Evidence:
`docs/qa/evidence/conversion-matrix-20261119/` (`matrix.json`, `README.md`, eight screenshots).
The installed base APK SHA-256 is `e3b6c481…f1037e`, matching the bundletool-extracted universal APK.

| Case | Source | Result |
|---|---|---|
| PNG → JPG | `uitest1.png` (120,061 B) | **PASS** — `uitest1.jpg`, 93,573 B, JFIF magic |
| PNG → PDF | `uitest1.png` | **PASS** — `uitest1.pdf`, 82,883 B, %PDF-1.4 |
| PDF → PNG | `law.pdf` (539,474 B, 21 pages) | **PASS** — `law.png` + `law (1)…(20).png` = **21 PNGs, one per page** (no page dropped, no 20-page cap) |
| PDF → TXT | `law.pdf` | **PASS** — `law.txt`, 35,346 B, 791 lines of real extracted text |
| DOCX → PDF | `notes.docx` (minimal OOXML) | **PASS** — `notes.pdf`, 18,112 B; History carries the typed fidelity note ("Images and tables were not rendered; headings and lists were kept.") |
| TXT → PDF | `notes.txt` | **PASS** — `notes (1).pdf`, 16,123 B; unique name made by the app on collision |
| MP4 → GIF | `clip-sage.mp4` (no audio track) | **PASS** — `clip-sage.gif`, 4,392,362 B, GIF89a (FFmpeg path) |
| WAV → MP3 | `test.wav`, 16,044 B | **PASS** — `test.mp3`, 10,701 B, ID3 (FFmpeg path) |
| PNG → TXT (OCR) | `uitest1.png` | **PASS** — after the in-app pack install, `uitest1.txt` 273 B; the screenshot's own UI text came back as "Sigha", "Your files, converted on this phone", "Nothing is uploaded — everything happens on your device" |

The OCR case is the one the JVM suite cannot reach: the "Download model pack?" dialog (English +
Arabic, 28 MB) was confirmed in-app, download progress was observed on the same screen, and the
Text → TXT target appeared without reselecting the file. The resulting convert drove the tesseract
JNI under R8, so the pack install, the download state and the OCR engine are now runtime-proven —
previously §8 item 1 was describing them from code inspection only.

One automation artefact, recorded rather than hidden: the driver missed the transient completion
banner for PDF → TXT and timed out after 150 s, although the file had been written at 20:28 and its
History row is present. The case is marked PASS on the output and History evidence, not on the
banner.

**Legacy save path (API 26–28) proven too.** On Android 8.0 (API 26, `emulator-5562`, x86_64) the
Gradle-built release universal APK converted PNG → JPG and wrote
`/sdcard/Download/Sigha/uitest1.jpg` (117,784 B, JPEG magic) through the direct-file branch, with no
MediaStore insert — exactly the design for 26–28. The automation's MediaStore check correctly found
nothing there; the file on disk and the app's completion state are the evidence. That closes the
"MediaStore publishing on API 26–28" caveat for the write path itself.

Not covered by §7.4: archive sources, scanned-PDF OCR fallback (blank text layer), the remaining
media/office targets, a 20+-file batch on the release build, and any physical arm64 device.

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
10. **No arm64 execution evidence.** Every runtime record in this document is an x86_64 emulator. This
    host cannot run the arm64 system image locally — QEMU2 refuses it
    (`Avd's CPU Architecture 'arm64' is not supported by the QEMU2 emulator on x86_64 host`). Prove it
    on a borrowed Android 14+ phone or Samsung Remote Test Lab before release; the Play pre-launch
    report is a startup crawl, not a flow test.
11. **Release conversion matrix is partial, not complete.** Nine conversions (images, PDF both ways,
    text, office, FFmpeg audio/video, OCR) now pass on the AAB-derived release APK, and the legacy
    save path passes on an API 26 release APK (§7.4). Still untested on a release artefact: archive
    sources, the scanned-PDF OCR fallback, the remaining 32 converters/targets (AAC, M4A, OGG, OPUS,
    FLAC, XLSX, PPTX, ODS, …) and multi-file batches. Those paths rest on the JVM suite and the debug
    build (§7.2), not on release-run evidence.
12. **Silent media still offers audio targets.** A video with no audio track shows MP3/M4A/AAC/WAV/
    FLAC/OGG/OPUS as targets and then fails with the generic "Conversion failed." (§7.3). Fix
    candidate: probe the source with `MediaExtractor` and hide audio targets, or give the failure a
    specific cause.

## 9. Reproduce

```powershell
Set-Location 'E:\android_apps\digital_converter'

# Whole gate: clean build, tests, debug facts, release APKs, AAB, lint, claim drift
pwsh -File scripts/verify.ps1

# Runtime smoke on the first attached device/emulator (installs the debug universal APK)
pwsh -File scripts/device-smoke.ps1

# Quick variants
pwsh -File scripts/verify.ps1 -SkipBuild          # facts about the artefacts that already exist
pwsh -File scripts/verify.ps1 -ReleaseCandidate   # additionally fail on shipped legal placeholders
```

`scripts/verify.ps1` finds the JDK itself if `JAVA_HOME` is not set, and stops leftover
Gradle/Kotlin daemons before `:app:clean` (Windows keeps the lint cache locked otherwise).

## 10. Verdict

**Build, packaging and JVM verification: green.** Clean build, 137 unit tests, stored and 16
KB-aligned native libraries, zipalign, signed release APKs, an AAB and zero lint errors all pass
from the commands in §9, and the claim-drift check keeps the public docs tied to measured numbers.

**Device verification: partial.** Install, cold start, resumed activity, a 120-event monkey run
and an empty crash buffer are recorded. On the emulator, real conversions through the real
DocumentsUI picker and the upload-tree bar/action/summary states are now recorded too (§7.2), in
English and Arabic, and a nine-case conversion matrix plus OCR now passes on the AAB-derived release
APK, with the legacy save path proven on API 26 (§7.4). They are still **emulator** results and the
matrix is not the whole catalogue: run `docs/qa/device-checklist.md` on a physical arm64 API 26–36
device before submitting to Play. The app must not be described as device-tested until that is done.

**Upload tree: green locally, blocked on the human lane.** The upload-tree commit passes the full
gate (`verify-20261119-004710.json`), and `verify.ps1 -ReleaseCandidate` still fails on the 21
placeholder markers in the shipped legal assets — deliberately, until the publisher supplies the
legal name, contact, policy URL and LGPL mailbox. No tag, no AAB for Play and no closed test until
then; the order is in `docs/plan/publisher-guide.md`.
