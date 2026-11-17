# Third-party notices

Sigha includes or links the following third-party components. Each remains under its own
license; this file is informational, not a replacement for the license texts.

## Runtime libraries (shipped in the APK)

| Component | License | Source |
|---|---|---|
| FFmpegKit (maintained fork) | LGPL-3.0 | https://github.com/ffmpegkit-maintained/ffmpeg |
| FFmpeg | LGPL-2.1-or-later (build without x264/x265) | https://ffmpeg.org |
| PDFBox-Android | Apache-2.0 | https://github.com/TomRoush/PdfBox-Android |
| Apache Commons Compress | Apache-2.0 | https://commons.apache.org/proper/commons-compress/ |
| XZ for Java | Public domain | https://tukaani.org/xz/java.html |
| Tesseract4Android | Apache-2.0 | https://github.com/adaptech-cz/Tesseract4Android |
| Tesseract (native) | Apache-2.0 | https://github.com/tesseract-ocr/tesseract |
| ONNX Runtime (Android) | MIT | https://github.com/microsoft/onnxruntime |
| AndroidX / Jetpack Compose / Material 3 | Apache-2.0 | https://developer.android.com/jetpack |
| Kotlin / kotlinx | Apache-2.0 | https://github.com/JetBrains/kotlin |
| Coil | Apache-2.0 | https://github.com/coil-kt/coil |
| DocumentFile / ExifInterface | Apache-2.0 | https://developer.android.com/jetpack |

## Optional model packs (downloaded by explicit user action)

| Pack | Component | License | Source |
|---|---|---|---|
| OCR eng/ara | Tesseract traineddata (tessdata_best) | Apache-2.0 | https://github.com/tesseract-ocr/tessdata_best |
| Upscale (if published) | Real-ESRGAN weights | BSD-3-Clause | https://github.com/xinntao/Real-ESRGAN |

Model packs are stored on the device only. They are never uploaded, and their license text
is shown in the app before download.

## Deliberately excluded (license or feasibility)

LibreOffice (MPL-2.0 but far too heavy), Pandoc (GPL-2.0+), Ghostscript (AGPL-3.0),
calibre (GPL-3.0), unRAR (restricted), unrar-based tools. If ever added they must be
separate, user-initiated downloads with their own notices — never inside the APK.

## Inspiration

Sigha's product idea (drag/choose files → pick a format → convert locally) is inspired by
Filee (https://github.com/KnifeLemon/Filee, MIT). No Filee source code, assets, documentation
text, brand name or trade dress is included in this project.
