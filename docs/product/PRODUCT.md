# Sigha — Product specification

## Problem

Android users who need to convert a file (a DOCX to PDF, a HEIC to JPG, a video to MP3) are pushed
to upload it to an ad-funded website. That leaks private files and fails offline. Desktop tools
like Filee proved a different shape works: many formats, local engines, one gesture.

## Product idea

A local-file workbench:

1. Bring files in: pick with the system picker, or share/"Open with" from any app.
2. The app offers **only formats reachable from the selected files** (computed from the converter graph).
3. Convert on-device, in the background, with a queue.
4. Result lands in `Download/Sigha`; history keeps the last 50.

## MVP scope (this build)

| Family | Inputs → Outputs |
|---|---|
| Images | JPG/PNG/WebP/BMP/GIF/HEIC/AVIF → JPG/PNG/WebP; resize, quality, EXIF fix; image → PDF |
| PDF | PDF → PNG/JPG (page range, DPI); PDF → TXT; images → PDF |
| Office-simple | DOCX → TXT/HTML/PDF; XLSX → CSV/TSV/PDF; PPTX → TXT; CSV → XLSX; ODT/ODS (stretch) |
| Text | TXT/MD → PDF; HTML → TXT |
| Media | audio ↔ MP3/M4A/AAC/WAV/FLAC/OGG/OPUS; video → MP4/WebM/MKV; video → GIF; extract audio; trim; bitrate |
| Archives | ZIP ↔ TAR.GZ repack; 7Z → ZIP |
| Data | CSV ↔ TSV; CSV → JSON; JSON → CSV |
| Subtitles | SRT ↔ VTT |
| E-books | EPUB → TXT/HTML |

## Non-goals (v1)

Builds that need a desktop office suite: precise DOCX/XLSX layout fidelity, HWP/HWPX, CAD/DWG,
3D models, geo data, fonts, RAR (license restricted), ISO images. All are possible later as
optional engines, not core.

## Differentiators

1. **Arabic-first** UI (RTL, full Arabic strings) — the market leader set ignores it.
2. **No upload, no ads, no account** — a verifiable offline promise.
3. **Graph-based format matrix** — new formats are adapters, not rewrites; only useful targets appear.
4. **Optional AI packs** (OCR, upscaling) that are commercially-licensed and downloaded on demand.

## Metrics that decide continuation

- Cold start < 2s; image conversion < 1.5s for a 12MP JPG.
- 100% of conversions complete with no network access.
- Crash-free sessions > 99.5%; failed conversions explain themselves.
- Store listing: "offline" claim verified by reviewers.

## Open questions for the human

- Final app name + trademark clearance (Sigha is provisional).
- Play Console account, signing key, upload (human-only steps).
- Whether to publish OCR packs on our host or use Play Asset Delivery.
## Implementation status (2026-11-17)

Built and compiling from this workspace: 48 formats in the catalogue, 32 converter edges across 12
engine families, Compose UI in English and Arabic, SAF-only file access, MediaStore output, a
sequential queue, history, settings, downloadable OCR pack, and a green 131-test JVM suite.

Evidence: `docs/qa/verification.md` (build log, APK metadata, hashes, test counts, runtime smoke
test and its limits). Decisions that changed versus this spec:

- Format count is 48, not ~270: every format here has a working edge; no catalogue padding.
- OCR ships as a downloadable pack only (the 5.5 MB bundled `tessdata_fast` fallback was removed).
- Upscaling is deferred: ONNX Runtime is not in the APK; the verified Real-ESRGAN candidate is
  documented in `docs/models/integration.md`.
- Scanned PDFs still route through the text extractor; the OCR fallback is the next fix.

Device QA, release signing and Play Console steps remain human tasks.
