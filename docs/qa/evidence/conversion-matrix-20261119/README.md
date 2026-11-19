# Release conversion matrix - 2026-11-19

Nine real conversions through the real UI on the **release artefact chain Play would ingest**: a
universal APK extracted from `app-release.aab` with bundletool 1.18.0, signed by the local QA key.
Until this pass, R8-processed code had only been smoke-tested (install, start, monkey); it had never
converted a file.

- Device: `emulator-5554`, Android 16 (API 36), x86_64, no physical arm64 device on this host.
- Installed base APK SHA-256: `e3b6c481f4d3c04439f22225dae431fdac5663ab49c4568d519dd8c873f1037e`
  (this matches the bundletool-extracted universal APK, not the Gradle-built one).
- versionCode 3 / versionName 0.2.1.
- Method: cold start, DocumentsUI picker (Show roots -> Downloads), source row, target chip,
  target-named Convert action. Outputs verified twice: MediaStore
  `content://media/external/downloads` query (`relative_path = Download/Sigha/`) and a host-side
  byte check of the pulled file (magic bytes + size).

| # | Case | Result | Output |
|---|------|--------|--------|
| 1 | PNG -> JPG | PASS | `uitest1.jpg`, 93,573 B |
| 2 | PNG -> PDF | PASS | `uitest1.pdf`, 82,883 B, %PDF-1.4 |
| 3 | PDF -> PNG (21 pages) | PASS | `law.png` + `law (1)..(20).png` = 21 PNGs, one per page |
| 4 | PDF -> TXT | PASS | `law.txt`, 35,346 B, 791 lines of real text |
| 5 | DOCX -> PDF | PASS | `notes.pdf`, 18,112 B, with the typed fidelity note in History |
| 6 | TXT -> PDF | PASS | `notes (1).pdf`, 16,123 B, unique name made by the app |
| 7 | MP4 -> GIF | PASS | `clip-sage.gif`, 4,392,362 B, GIF89a |
| 8 | WAV -> MP3 | PASS | `test.mp3`, 10,701 B, ID3 header |
| 9 | PNG -> TXT (OCR) | PASS | `uitest1.txt`, 273 B; 28 MB eng+ara pack installed through the in-app dialog, then tesseract JNI ran under R8 |

`matrix.json` is the machine-ish record; the numbered `.png` files are the post-conversion
screenshots. Case 4's completion banner was missed by the automation driver, so its evidence is the
written file plus the History row (recorded in `matrix.json` rather than hidden).

A tenth case covers the **legacy save path**: on Android 8.0 (API 26, emulator-5562) the
Gradle-built release universal APK converted PNG -> JPG and wrote `/sdcard/Download/Sigha/uitest1.jpg`
(117,784 B, JPEG magic) directly, without a MediaStore insert — exactly what API 26-28 are supposed
to do. The automation's MediaStore check correctly found nothing there; the file on disk is the
evidence.

Not covered here: archive sources, scanned-PDF OCR fallback, the other media/office targets, a
multi-file batch on release, and any physical arm64 device. See `docs/qa/verification.md` section
7.4 and section 8.
