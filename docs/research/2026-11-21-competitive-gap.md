# Competitive gap and build-vs-buy research — 2026-11-21

Scope: Sigha's conversion scope (48 recognised formats, 39 convertible sources, 25 producible
targets, 32 converters) compared with Android PDF suites and converter apps. Store pages, pricing
pages and first-party review samples were read on 2026-11-21. Prices change; re-check before any
purchase. Estimates are marked (E); third-party numbers (T); not evidenced (n/e).

## 1. The market is two businesses

- **PDF suites** — Acrobat, iLovePDF, Smallpdf, Xodo, WPS — are subscription products with few or
  no ads and ratings of 4.2–4.7. Consumer prices are **$4–13/month, $48–108/year**.
- **Converter apps** — Ice Cold File Converter, FreeConvert, HEIC converters — run ads and sit at
  3.6–4.3. Their reviews are dominated by ads, paywall and "where did my file go" complaints.

Sigha's trust posture belongs to the first group; its feature set currently belongs to the second.

Going rates: Acrobat Premium **$12.99/mo or $69.99/yr** (App Store IAP list); Xodo **$11.99/mo or
$107.99/yr**; iLovePDF **$4/mo billed annually ($48) or $7 monthly**; Smallpdf ~**$12/mo or $108/yr**
(T); WPS **$29.99/yr** (T).

## 2. Where Sigha stands

Sigha already has: batch queue with per-item status, share-to-app intake, SAF output folders, PDF
page ranges, OCR with English/Arabic/both and a 28 MB model pack, on-device-only processing,
no account/ads/analytics. That is a genuinely better foundation than most converter apps.

The gap is not format count. It is the **PDF workshop**: merge, split/extract, organise pages with
thumbnails, compress to a target size, password protect/unlock, watermark/page numbers,
scan-to-PDF. Every PDF app ships these; Sigha ships none.

## 3. Feature gap matrix (Sigha vs best-in-class)

Legend: effort S ≤3 days, M 4–10, L 11–25, XL >25 (for this codebase).

| # | Capability | Sigha | Best-in-class | Value | Effort | Call |
|---|---|---|---|---|---|---|
| 1 | Merge PDFs | No | iLovePDF, Smallpdf, Acrobat | 5 | S | **Now** |
| 2 | Split / extract pages | No (input page ranges only) | all | 5 | S | **Now** |
| 3 | Page organiser with thumbnails | No | all | 5 | M | **Now** |
| 4 | Compress PDF to a target size | No | Smallpdf, iLovePDF | 5 | M | **Now** |
| 5 | Image compress/resize presets | Partial (convert only) | Image Resizer | 5 | S | **Now** |
| 6 | HEIC/HEIF on API 26–27 | **No — rejects below 28** | HEIC apps | 4 | S | **Now** |
| 7 | Save-location and naming clarity | Partial | nobody does it well | 5 | S | **Now** |
| 8 | Scan-to-PDF | No | Adobe Scan, CamScanner | 4 | S–M | **Next** (ML Kit) |
| 9 | Password protect / unlock PDF | No | iLovePDF, Smallpdf | 4 | S–M | **Next** |
| 10 | Arabic searchable PDF (RTL-correct) | No | **nobody** | 5 | L | **Next** (wedge) |
| 11 | Video trim + chat presets | No | VidSoftLab | 4 | M | Next |
| 12 | "Fit under X MB" presets | Partial (bitrate) | Inverse AI | 4 | M | Next |
| 13 | Batch OCR + verification view | Partial | CamScanner | 4 | M | Next |
| 14 | Watermark / page numbers | No | iLovePDF, WPS | 3 | M | Later |
| 15 | Folder conversion (SAF tree) | No | desktop tools | 3 | M–L | Later |
| 16 | Sign / fill forms | No | Acrobat, Xodo | 3 | L | Later |
| 17 | PDF→DOCX | No | Acrobat, WPS | 3 | XL | **Do not build** |
| 18 | Cloud sync / account | No | — | 1 | — | **Do not build** |

## 4. What competitors do that Sigha should copy (free)

- Batch is Pro-gated at Smallpdf; several converter apps are one-file-at-a-time. Sigha's batch is
  already better — surface it (multi-select, queue, per-item status) rather than hiding it.
- Share on every result row (Sigha now has it on Queue, History and the workbench row).
- "Open folder" / copy path next to the saved destination.
- Scan-to-PDF is table stakes; it is free to adopt (below).
- Target-size compression ("make this PDF 2 MB") is more useful than a quality slider; almost
  nobody exposes it.
- Remembering the last target and offering "Convert again" is cheap and matches how utility apps
  build habit.

## 5. What competitors do not fix — Sigha's wedge

1. **Arabic-correct OCR and searchable PDFs.** Tesseract still reverses Arabic words
   ([#4544](https://github.com/tesseract-ocr/tesseract/issues/4544)) and writes Arabic into
   searchable PDFs in reverse logical order ([#4428](https://github.com/tesseract-ocr/tesseract/issues/4428)).
   Arabic PDF copy-paste breaks ligatures. Offline Arabic OCR exists as a Windows product
   ([BixoPDF](https://www.bixopdf.com/arabic-ocr-pdf.html)) and a GitHub project
   ([Waraqi](https://github.com/odytr/waraqi)); **no Android app owns it.**
2. **RTL-safe office→PDF.** Arabic DOCX→PDF must shape and order correctly; Arabic shaping is the
   moat and the risk.
3. **Sensitive documents that must not be uploaded** (IDs, court papers, medical reports, salary
   letters). CamScanner had malware at 100M+ scale
   ([Kaspersky](https://www.kaspersky.com/blog/camscanner-malicious-android-app/28156/)); online
   converters upload and delete after a window. Sigha's verifiable claim is "it never leaves the
   phone".
4. **Batch iPhone-photo conversion on cheap Android phones** — HEIC is rejected below API 28 today;
   FFmpeg is already in the app.
5. **"Make it fit"** — compress to a portal/email/WhatsApp limit without destroying legibility.
6. **Official-document assembly** — scan pages, rotate, merge, page numbers, password, one PDF.
7. **Low-RAM / low-storage reality.** Iraq had 48.1M mobile connections in early 2025
   ([DataReportal](https://datareportal.com/reports/digital-2025-iraq)); the market rewards a small
   base install plus optional packs.

**Positioning that survives scrutiny:** "Arabic, actually" (correct output, not just translated UI)
+ "nothing leaves the phone" + "it fits".

## 6. Build vs buy

### 6.1 Adopt now (free)

**ML Kit Document Scanner** — free, on-device, ~300 KB app-side, API 21+, no camera permission
([docs](https://developers.google.com/ml-kit/vision/doc-scanner/android)). GMS required; verify the
Data safety form and test on both emulator images. This closes scan-to-PDF without a camera pipeline.

### 6.2 Buy only if a benchmark fails

| Option | Gap it closes | Price ballpark | Offline terms that matter | Verdict |
|---|---|---|---|---|
| Apryse (PDFTron) | Full PDF stack | from **$1,500/yr** ([pricing](https://apryse.com/pricing)) | Quote per platform/app; native `.so`; check telemetry and 16 KB alignment | Only if PDFBox fidelity/compression blocks |
| ComPDFKit | PDF view/edit/convert/security | from **$1,300** ([pricing](https://www.compdf.com/pricing)) | Quote by product/feature; native libs | Cheapest credible fallback |
| Nutrient (PSPDFKit) | Full PDF stack | no public price; Capterra (T) lists **€5,000/user/yr** | Annual, per component, sales-quoted | No — cost far above value |
| Aspose.PDF / Aspose.Total | Office→PDF fidelity, PDF | **$1,199** / **$3,999** dev (OEM higher) ([PDF](https://purchase.aspose.com/pricing/pdf/android-java/), [Total](https://purchase.aspose.com/pricing/total/android-java/)) | Per developer + deployment; large Java footprint | No now; Office fidelity option |
| ABBYY FineReader Engine | Best-in-class OCR incl. Arabic | no published price; developer + runtime, page counters | **Online licence needs port 443** ([licensing](https://docs.abbyy.com/fine-reader/engine/licensing)) — negotiate offline licence | Gated on a 100-page Arabic benchmark |
| Scanbot SDK | Scanning UX | from ~**$2,500/yr** ([page](https://scanbot.io/developer/kotlin-multiplatform-document-scanner/)) | Annual per app | No — ML Kit is free |
| Nokia HEIF | HEIC codec | commercial; free only for non-commercial evaluation, patents not granted ([repo](https://github.com/nokiatech/heif/)) | Patent licence separate | No — platform decode + FFmpeg fallback |

### 6.3 Copyleft and patent traps to keep avoiding

MuPDF (AGPL-3.0), Ghostscript (AGPL/commercial), iText (AGPL/commercial), x265 encode (GPL),
Nokia HEIF, app-side HEVC software encode (patent-pool question; device decode is covered by the
OEM licence). LGPL components already used (FFmpeg via FFmpegKit, DSS if adopted later) need the
usual dynamic-linking + relink-offer compliance, which the licence docs already cover.

### 6.4 Worth-it rule at zero revenue

Buy only if all are true: closes a top-3 user job; runs without licence phone-home; licence allows
closed-source Play distribution across arm64-v8a and x86_64 with 16 KB-aligned `.so`; APK delta
acceptable (debug APK is already 115 MB with 32 `.so`); cost is below the build alternative; no
copyleft contamination. **At launch: buy nothing; adopt ML Kit Scanner; build on PDFBox.**

## 7. Ranked next steps (value ÷ effort)

1. Merge PDFs (S) — PDFBox already present.
2. Split/extract pages (S) — reuse the page-selection parser.
3. Page organiser with thumbnails (M) — the most "PDF app" screen.
4. Target-size compression for PDF/image/video (M).
5. HEIC on API 26–27 via FFmpeg fallback (S).
6. Save-location UX: exact folder, "Open folder", filename pattern (S).
7. Scan-to-PDF via ML Kit (S–M, free).
8. Password protect/unlock PDF (S–M, PDFBox standard security).
9. Arabic searchable PDF MVP (L) — the wedge.
10. Video trim + WhatsApp/Telegram presets (M).

**Do not build now:** PDF→DOCX, form filling/e-signature, full office editing, cloud sync, unit
converters.

## 8. Sources

All URLs above were read on 2026-11-21. Highlighted: Tesseract issues
[#4544](https://github.com/tesseract-ocr/tesseract/issues/4544) and
[#4428](https://github.com/tesseract-ocr/tesseract/issues/4428); Abu Dhabi/arelion Arabic PDF analysis
https://arelion.dev/guides/arabic-pdf-to-text/; ML Kit Document Scanner
https://developers.google.com/ml-kit/vision/doc-scanner/android; FFmpeg legal
https://ffmpeg.org/legal.html; FFmpegKit community fork
https://github.com/ffmpegkit-maintained/ffmpeg.
