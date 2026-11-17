# Model packs, OCR and upscaling

Status: **downloadable OCR pack and OCR conversion implemented; AI upscaling intentionally not
enabled in this build.**

Everything here is data-only: a model pack contains traineddata files, never executable code.
Nothing is fetched at app start - only an explicit Download tap opens a connection.

## What ships in the APK and what is downloaded

| Piece | In the APK | Downloaded on demand |
| --- | --- | --- |
| Tesseract engine (Tesseract4Android 4.9.0) | yes | no |
| OCR English + Arabic (tessdata_best) | no | **yes**, 28,004,325 B |
| AI upscaler (Real-ESRGAN) | no | no (feature disabled, see below) |

The app has no internet permission requirement beyond what the manifest already declares; the
downloader uses HTTPS and never sends file contents, file names or usage data - only the GET for
the model URL.

## The OCR pack

### Catalogue entry

| Field | Value |
| --- | --- |
| id | `ocr` (`ModelPacks.OCR_PACK_ID`) |
| title | OCR: English + Arabic |
| sizeBytes | 28,004,325 (26.7 MiB) |
| license | Apache-2.0 |
| licenseUrl | https://github.com/tesseract-ocr/tessdata_best/blob/main/LICENSE |
| source pin | tessdata_best commit `e12c65a915945e4c28e237a9b52bc4a8f39a0cec` |
| install path | `<filesDir>/models/ocr/tessdata/` |

### Files

| File | Bytes | SHA-256 | URL |
| --- | --- | --- | --- |
| `eng.traineddata` | 15,400,601 | `8280aed0782fe27257a68ea10fe7ef324ca0f8d85bd2fd145d1c2b560bcb66ba` | https://raw.githubusercontent.com/tesseract-ocr/tessdata_best/e12c65a915945e4c28e237a9b52bc4a8f39a0cec/eng.traineddata |
| `ara.traineddata` | 12,603,724 | `ab9d157d8e38ca00e7e39c7d5363a5239e053f5b0dbdb3167dde9d8124335896` | https://raw.githubusercontent.com/tesseract-ocr/tessdata_best/e12c65a915945e4c28e237a9b52bc4a8f39a0cec/ara.traineddata |

The byte counts and digests above are of the exact responses those pinned URLs serve. **Re-run the
verification before changing any of them**; a moved branch head must never change what the app
expects.

### Install layout

```
<filesDir>/models/ocr/                     <- installDir(context, "ocr"); Tesseract data path
<filesDir>/models/ocr/tessdata/eng.traineddata
<filesDir>/models/ocr/tessdata/ara.traineddata
<filesDir>/models/ocr.part                 <- temporary file during one download
```

Tesseract's Java `init(datapath, language)` appends `/tessdata` itself, which is why the pack root
- not the `tessdata` folder - is handed to it.

### Download pipeline

`ModelPacks.download` runs `PackInstaller` on `Dispatchers.IO`:

1. Reuse: files already present at the correct size and digest are skipped, so a retry after a
   partial install only fetches what is missing.
2. Free-space check (payload + 4 MiB margin) before opening a connection.
3. One file at a time, into `models/ocr.part`; progress is 0f..1f across the whole pack.
4. Manual redirect handling (max 5), `User-Agent: Sigha/1.0 (Android) model pack downloader`,
   identity encoding, 20 s connect / 30 s read timeouts.
5. Content-Length is cross-checked against the catalogue; a larger body and a short body are
   both rejected.
6. Size and SHA-256 are verified **before** anything is moved into place.
7. Atomic `renameTo` into `tessdata/` (copy + delete fallback on filesystems that refuse a
   rename), then a final size check.
8. `finally` deletes the `.part` file: a failure, a hash mismatch or a cancellation cannot leave a
   half file behind. A process kill can leave a `.part`; the next attempt truncates it.

Retries: up to 3 attempts per file for transport errors (`IOException`), with a short backoff. A
SHA-256 mismatch is final - the pack is rejected with "The downloaded file failed its security
check. Please try again." and is not retried automatically.

Cancellation: `onProgress` throwing `kotlinx.coroutines.CancellationException` aborts the loop,
deletes the temporary file and rethrows, which the conversion queue already treats as a cancelled
run. `ModelPacks.download` also rethrows `CancellationException` instead of wrapping it in a
`Result`, and cancelling the calling coroutine stops the blocking read at the next progress tick.

### Public API (frozen contract)

```kotlin
ModelPacks.attach(application)                                  // AppGraph.init, once
ModelPacks.catalog(): List<ModelPacks.PackInfo>                 // safe before attach
ModelPacks.isInstalled("ocr"): Boolean                          // cheap size check, no network
ModelPacks.installDir(context, "ocr"): File?                    // null until at least one file is installed
ModelPacks.download(context, "ocr") { p -> /* 0f..1f */ }: Result<File>
ModelPacks.installedOcrLanguages(context): List<String>         // ["eng", "ara"]
```

`installedOcrLanguages` reflects the traineddata actually present, in catalogue order. A partial
install (English present, Arabic missing) reports `["eng"]`; English OCR already works, while
`isInstalled("ocr")` stays false so the card keeps offering Download to finish Arabic.

### How the UI calls it

`ModelPackStore` (data layer) already wraps the UI-facing calls; `ModelPacksScreen` renders
`PackInfo.title/description/sizeBytes/license/licenseUrl` and drives
`download(packId) { progress -> ... }`. Sample size is shown before the tap; the license button
opens the URL in a browser.

### OCR runtime

`engine/ocr/OcrConverters.kt` exposes:

```kotlin
fun ocrConverters(app: Application): List<Converter>
```

Two converters:

| id | inputs | outputs |
| --- | --- | --- |
| `ocr.image-to-txt` | jpg, png, webp, bmp, gif, heic | txt |
| `ocr.pdf-to-txt` | pdf | txt |

**Language.** `request.options.ocrLanguage` is parsed leniently (`+`, comma, semicolon, space,
case-insensitive) and filtered to `eng`/`ara`, canonical order. Default is `"eng"`. The request
must be covered by the installed languages or the converter fails with exactly:

> Install the OCR pack (eng/ara) first

A blank/unknown request fails with "Unsupported OCR language. Choose English, Arabic, or both."

**Images.** API 28+ uses `ImageDecoder` with `ALLOCATOR_SOFTWARE` (hardware bitmaps are unreadable
by Tesseract) and platform EXIF orientation; API 26-27 uses `BitmapFactory` (bounds pass, power-of-
two `inSampleSize`, exact scale-down, EXIF rotation via androidx ExifInterface). The longest side is
capped at the request's `maxDimension` (default 3000, hard cap 8192) so a large photo cannot OOM
the OCR step; OOM becomes "This image is too large to process". HEIC on Android 7.x is refused with
"HEIC images need Android 9 or newer" instead of a generic decoder failure.

**PDFs.** The URI is copied to a temporary cache file, then `PdfRenderer` renders selected pages
at 200 dpi (`scale = 200/72`), `RENDER_MODE_FOR_PRINT`, white background, longest side capped at
4096. Pages come from `ConversionOptions.pages` (`"1-5,8"`); without it, the first 50 pages are
read and the result note says "Only the first 50 of N pages were read." An explicit range is
capped at 500 pages. A range that exists but lies past the last page fails with "None of the
selected pages exist in this PDF."; unparseable text falls back to the default window with a
note. Password-protected PDFs fail with "This PDF is password-protected".

**Output.** UTF-8 text, line endings normalised, trailing spaces removed, one final newline. An
empty recognition fails with a short hint (sharper photo / visible text) and no empty file is
reported to the runner.

**Threading and cancellation.** Both converters run on `Dispatchers.IO`; the runner's
`isCancelled()` is checked before decoding, before OCR, before writing and between PDF pages.
A single native recognition call is not interruptible; cancellation lands at the next checkpoint.
The Tesseract instance is closed (`recycle()`) in a `use` block; bitmaps are recycled in
`finally`. Missing native libraries or unreadable pack data become
"OCR pack unavailable on this device" instead of a crash.

### Known routing gap: scanned PDFs

`pdfConverters(app)` registers PDFBox's `pdf-to-text` edge before the OCR edge, and the planner
keeps the first edge for an equal-cost route. The OCR `pdf-to-txt` converter is therefore
registered and ready but **not reachable through the normal picker**: a scanned PDF goes to
PDFBox, which writes an empty file, and the run ends as a failed conversion.

Recommended fix, in Lead-owned files: in `PdfToTextConverter`, when the extracted text is empty,
fall back to `PdfTextConverter` (or decide by whether the PDF has a text layer). Do not simply
move OCR before PDFBox: digital PDFs would pay OCR time and lose layout. Until then, a scanned
PDF can be handled as two steps in the UI: PDF -> PNG, then PNG -> TXT.

### Limitations

- No resume and no delta updates: if the download dies, the next attempt restarts the missing
  file from byte 0. A failure after English finished keeps English installed, so only Arabic is
  fetched again.
- No metered-network policy in this layer; it downloads exactly when the UI asks. If the product
  wants "Wi-Fi only", that check belongs in `ModelPackStore`/`ModelPacks.download` before the
  first byte.
- No Play Asset Delivery; the pack is a normal HTTPS file, so it needs a connection once and then
  works fully offline.
- Installed files are app-private. Only their size is checked at rest; the digest is verified at
  install time (re-hashing 28 MB on every screen build would be wasteful).
- OCR speed is dominated by Tesseract init (roughly 0.5-1.5 s) plus per-page recognition; one
  process reuses a single `TessBaseAPI` for all pages of a PDF.

## AI upscaling

`engine/upscale/UpscaleConverters.kt` exists with the agreed signature but returns `emptyList()`:

```kotlin
fun upscaleConverters(app: Application): List<Converter> = emptyList()
```

This is deliberate. The model packs screen says "Coming soon / Not part of this build yet", and
wiring an image->PNG upscaler into the route graph now would either hijack every ordinary
image->PNG conversion (the planner takes the first registered edge) or fail at the end of a route.
Upscaling also needs an explicit user choice ("2x/4x"), which the current screen model does not
carry (the option key is reserved as `options.extra["scale"] = "2x" / "4x"`).

### Verified candidate (do not ship until the checklist below is done)

| Item | Value |
| --- | --- |
| Model | Qualcomm AI Hub **Real-ESRGAN General x4v3** |
| Licence | BSD-3-Clause (per the model card); upstream Real-ESRGAN by Xintao Wang et al. |
| Source | https://github.com/qualcomm/Real-ESRGAN-General-x4v3 |
| Archive | https://qaihub-public-assets.s3.us-west-2.amazonaws.com/qai-hub-models/models/real_esrgan_general_x4v3/releases/v0.66.0/real_esrgan_general_x4v3-onnx-float.zip |
| Archive size | 4,540,895 B |
| `real_esrgan_general_x4v3.onnx` | 161,087 B, SHA-256 `09e6b675a0a18a97e1057c8dd7e984ef878a0a02ce104430da525b6241c858e7` |
| `real_esrgan_general_x4v3.data` | 4,836,096 B, SHA-256 `512d0ec9940c2e9d85d27f2952f12a0b77b7841dc22df4ce9f3ea458bc98f37f` |
| Graph | input `float32[1,3,128,128]` RGB 0..1, output `float32[1,3,512,512]` 0..1 |
| Size | ~1.21 M parameters, 4x fixed scale |

The `.data` file is external weights and must sit next to the `.onnx` file; ONNX Runtime resolves
it relative to the model path. The model was exported with ONNX Runtime 1.27.1 while this app
ships 1.32.0 - loading it is very likely fine but must be proven on a device.

### Enablement checklist

1. Add a `ModelPack(id = "upscale", ...)` with both files, no `subdirectory`, and a new verified
   digest check.
2. Build a `TileUpscalerConverter` (id `upscale.image-to-png`) that: decodes with
   `BitmapLoader`, pads each 128-pixel-aligned tile (256 px tiles, 16 px overlap), runs ORT with
   `OrtEnvironment`/`OrtSession`, blends seams, writes PNG, reports per-tile progress, checks
   `isCancelled()` between tiles.
3. Make the route opt-in: an explicit "AI upscale" action or a non-default output format, so
   ordinary image->PNG stays on the built-in converter.
4. Cap the output longest side (4x of a 3000 px image is 12,000 px) and fail with a short message
   when the result would exceed the memory budget.
5. Verify on arm64 hardware that the exported graph loads under ORT 1.32.0 and that a 128x128 tile
   matches the reference output.
6. Ship the BSD-3-Clause notice text (copyright line included) in NOTICE.md and the license URL on
   the pack card.

## Repository notes

- `app/src/main/assets/tessdata/` currently contains `eng.traineddata` (4,113,088 B) and
  `ara.traineddata` (1,432,056 B) - the tessdata_fast files. **No code reads them in this build**:
  the pack manager installs tessdata_best under `files/models/ocr` and OCR refuses to run without
  it. Decision needed: delete the assets (saves ~5.5 MB in the APK) or add them as an explicit
  bundled fallback. If they are kept as a fallback, `installedOcrLanguages` would always report
  English and Arabic and the "Install the OCR pack" prompt would become unreachable.
- `docs/legal/clearance-memo.md` (T11, B16) already allows the Apache-2.0 OCR data, and
  `NOTICE.md` lists tessdata_best - which matches the downloaded pack. The fast assets are not
  mentioned there.
- Never commit `.traineddata` files for the downloadable pack; only the catalogue metadata and
  digests belong in the repository.

## Tests

Pure-JVM tests, JUnit 4 only (no Android classes):

| Test | Covers |
| --- | --- |
| `model/PackCatalogTest.kt` | unique ids, OCR file set, licence, pinned HTTPS URLs, lowercase digests, size sums, `info()` |
| `model/OcrLanguagesTest.kt` | default, ordering, unknown-language dropping, Tesseract code, installed-file mapping |
| `model/OcrPageRangeTest.kt` | default window, explicit ranges, clamping, past-the-end, unparseable fallback, 500-page ceiling |
| `model/DigestTest.kt` | known SHA-256 vectors, lowercase zero-padded hex, file/stream agreement |

Run with `gradlew testDebugUnitTest`.

## Verification log (2026-11-16)

- Downloaded both pinned raw URLs with `curl.exe -sSL --fail`; recorded sizes with `Get-Item` and
  SHA-256 with `Get-FileHash -Algorithm SHA256` - values in the tables above.
- Fetched the tessdata_best `LICENSE` over HTTPS (HTTP 200) and confirmed the Apache-2.0 text.
- Downloaded the Qualcomm ONNX archive; recorded archive size, member sizes and both SHA-256
  values.
- Confirmed Tesseract4Android 4.9.0 API (`init(String, String)`, `setImage(Bitmap)`,
  `getUTF8Text()`, `recycle()`, `PageSegMode.PSM_AUTO`) and the ExifInterface 1.3.7 orientation
  constants against the AARs resolved by Gradle.
