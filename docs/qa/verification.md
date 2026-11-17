# Sigha — build, test and runtime verification

Date: 2026-11-17 (Asia/Baghdad). Workspace: `E:\android_apps\digital_converter`.

This report records only what was actually executed and observed. Anything not run is named as
not run — there are no inherited or assumed results here.

## 1. Toolchain actually used

| Component | Value | Evidence |
|---|---|---|
| JDK | Eclipse Temurin 21.0.13.9 (JVM target 17) | `$env:JAVA_HOME` set per command |
| Gradle | 8.11.1 (wrapper, `C:\gradle\gradle-8.11.1`) | `gradlew.bat --version` during setup |
| Android Gradle Plugin | 8.10.1 | `build.gradle.kts`; build logs |
| Kotlin | 2.0.21 | `settings.gradle.kts` plugin versions |
| compileSdk / targetSdk | 36 / 36 | `app/build.gradle.kts`; `aapt2 dump badging` |
| minSdk | 26 | same |
| Android SDK / build-tools | `C:\Android\Sdk`, platforms + build-tools 36.0.0 | `local.properties`, verification commands |
| Emulator | AVD `api36`, ATD x86_64, API 36 (headless) | `adb shell getprop` |

## 2. Build results

```
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon --console=plain
BUILD SUCCESSFUL in 49s
```

Full log: `build-final.log` (workspace root). `:app:compileDebugKotlin`, `:app:compileDebugUnitTestKotlin`,
`:app:testDebugUnitTest`, `:app:packageDebug`, `:app:assembleDebug` all completed. No errors, no
warnings that block the build.

## 3. APK facts (verified with aapt2, zipalign and a zip reader)

| Fact | Value |
|---|---|
| Path | `app/build/outputs/apk/debug/app-debug.apk` |
| Size | **72,522,602 bytes** (69.2 MiB) |
| SHA-256 | `8922614FD9F06FD1C3E30F0D957D533E7CBCC8EFAA26064C744EF5A8BD9E8266` |
| Package | `com.sigha.converter` |
| Version | `versionCode 1`, `versionName 0.1.0` |
| Label | `Sigha` (English), `صيغة` (Arabic) |
| minSdk / targetSdk | 26 / 36 |
| Native ABIs | `arm64-v8a`, `x86_64` (16 `.so` per ABI, 32 total) |
| Largest native libs | `libavcodec.so` 18.2 MB arm64 / 23.3 MB x86_64, `libavfilter.so` 5.0/5.7 MB, `libtesseract.so` 4.0/4.1 MB |
| 16 KB page alignment | `zipalign -c -P 16 4 app-debug.apk` → **exit 0 (pass)** |
| Packaging | `jniLibs.useLegacyPackaging = false` (uncompressed, aligned native libs) |
| Signing | debug keystore (not a release key) |
| Permissions | `INTERNET`, `ACCESS_NETWORK_STATE`, `POST_NOTIFICATIONS`, `WRITE_EXTERNAL_STORAGE` (maxSdk 28) |
| Launch activity | `com.sigha.converter.MainActivity` (MAIN/LAUNCHER + SEND/SEND_MULTIPLE/VIEW `*/*`) |

The APK carries FFmpegKit (LGPL-3.0, `libav*`), Tesseract4Android, PDFBox-Android and the app's
own Kotlin/Compose code. ONNX Runtime was removed from the dependency set; no ONNX code remains
(`grep ai.onnxruntime` → 0 hits). `material-icons-extended` is still present and is the main
remaining size lever for a release ABI split.

## 4. Unit tests (JVM, `:app:testDebugUnitTest`)

**15 test classes, 131 tests, 0 failures, 0 errors, 0 skipped.**

| Test class | Tests |
|---|---|
| core.RoutePlannerTest | 4 |
| data.HistoryCodecTest | 8 |
| data.UniqueNamesTest | 7 |
| engine.archive.ArchiveGuardsTest | 7 |
| engine.data.CsvTest | 11 |
| engine.data.JsonCsvTest | 12 |
| engine.media.FfmpegArgsTest | 25 |
| engine.PdfPageSelectionTest | 8 |
| engine.subtitle.SubtitleFormatTest | 10 |
| engine.text.HtmlTextExtractorTest | 7 |
| engine.text.MarkdownParserTest | 10 |
| model.DigestTest | 3 |
| model.OcrLanguagesTest | 6 |
| model.OcrPageRangeTest | 7 |
| model.PackCatalogTest | 6 |

Coverage is logic-level only (routing, argument builders, parsers, digests, pack catalogue).
No Android framework, SAF, MediaStore, FFmpegKit or Tesseract code is exercised by JVM tests.

## 5. Static matrix checks

- Format catalogue: **48 formats** in `core/FormatRegistry.kt` (`jpg png webp bmp gif tiff heic avif pdf txt md html docx odt rtf xlsx ods csv tsv pptx mp3 m4a aac wav flac ogg opus amr mp4 mkv mov avi webm flv zip 7z tar gz bz2 xz json yaml xml srt vtt epub mobi fb2`).
- Converter edges: **32 converter ids** registered from 12 engine families (`image`, `pdf`, `office`, `text`, `data`, `subtitle`, `ebook`, `media`, `ocr`, `archive`, `upscale`).
- Every family is wired exactly once in `engine/BuiltInConverters.kt`; the file is the single integration point.
- Registry entries with no converter are hidden by the planner (targets are computed from reachable edges), so an unsupported source cannot silently produce a dead end.
- `upscaleConverters()` returns `emptyList()` deliberately: upscaling is shown as "Coming soon" and the Real-ESRGAN candidate is documented in `docs/models/integration.md` but not shipped.

## 6. Runtime smoke test (emulator-5554, Android 16 / API 36, x86_64 ATD image)

Executed:

```
adb install -r app-debug.apk            → Success
adb shell am start -W -n com.sigha.converter/.MainActivity → Status: ok, Complete, TotalTime 3928 ms
adb shell pidof com.sigha.converter     → process alive (pid observed)
adb shell dumpsys activity activities   → topResumedActivity=com.sigha.converter/.MainActivity
adb logcat -b crash                     → no crash for com.sigha.converter
```

So: the APK installs, the process starts, the activity reaches resumed state, and the crash buffer
stays empty. That is the end of the runtime evidence that this emulator can provide.

Not verifiable on this host, and therefore **not claimed**:

- Visual rendering. `adb shell screencap` returned the same 15,580-byte all-black PNG for Sigha
  and for a pre-installed third-party app, so the headless ATD image does not produce usable frames
  (`docs/qa/` artifacts were removed after the check).
- Interaction/Compose semantics. A `uiautomator dump` of the third-party app listed its 10 texts,
  of Sigha listed none, then the emulator's system server began failing (`Bad file descriptor`, and
  `cmd: Can't find service: activity`), after which dumps could not be produced at all. The emulator
  was unstable under load; this is an environment failure, not an app diagnosis.
- Any real conversion (image/PDF/office/media/OCR/archive), SAF folder grants, notification/queue
  behaviour, or API 26–28 publishing paths. These need a physical device or a healthy emulator.

## 7. Known gaps and limitations (as shipped)

1. **Scanned PDFs**: the PDFBox `pdf-to-text` edge is registered before the OCR `pdf-to-txt` edge and
   wins on equal cost, so a scanned PDF converted to TXT yields little/no text instead of invoking
   OCR. Image → TXT OCR works. Fix: blank-text fallback in `PdfToTextConverter` (next iteration).
2. **OCR pack is download-only** (28,004,325 B, tessdata_best eng+ara, Apache-2.0, SHA-256 pinned).
   The 5.5 MB bundled `tessdata_fast` assets were removed from the APK because no code path used
   them; OCR therefore needs one download before first use, then works offline.
3. **No RAR** (unrar licence is not redistributable) and **no 7z write** (7z → ZIP only).
4. **Office fidelity**: DOCX/XLSX/PPTX/ODT/ODS are read as OOXML/ODF text structures — headings,
   paragraphs, tables (flattened), first worksheet only. No layout, formulas, images, headers or footnotes.
5. **MOBI / FB2** have no converters (EPUB only). RTF/TIFF/DOC are catalogue entries without edges.
6. **Upscaling** is "Coming soon"; ONNX Runtime is not in the APK.
7. Debug build only: no release signing, no ABI split (arm64 + x86_64 in one APK), no R8 shrinking.
   Estimate for a release arm64 split with icons trimmed: ~45–55 MB (see `docs/research/android-stack.md` §8).
8. A real emulator error appeared only at system level (`Bad file descriptor` in system_server); it is
   not reproduced by the app and cannot be attributed to Sigha from the available evidence.

## 8. Reproduce

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot'
Set-Location 'E:\android_apps\digital_converter'
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon --console=plain

# APK facts
& 'C:\Android\Sdk\build-tools\36.0.0\aapt2.exe' dump badging app\build\outputs\apk\debug\app-debug.apk
& 'C:\Android\Sdk\build-tools\36.0.0\zipalign.exe' -c -P 16 4 app\build\outputs\apk\debug\app-debug.apk
Get-FileHash app\build\outputs\apk\debug\app-debug.apk -Algorithm SHA256
```

## 9. Verdict

**Build and JVM verification: green.** Compile, packaging, 16 KB alignment and 131 unit tests pass
from a clean invocation of the commands above.

**Device verification: not done.** No screen/interaction/conversion evidence exists yet, so the app
must not be described as device-tested. Before any Play submission, run the manual checklist on a
physical API 26–36 device (the first action in the final report).