# Qiyas Council — Sigha: an offline Android converter after Filee

**Status:** final. **Language:** English (Mask C merged — one voice, roles are lenses, not personas).
**Question put to the council:** Filee is a 28k-star MIT desktop converter with ~270 formats. Can the same idea be rebuilt as a working Android app that is fully offline (or uses commercially-licensed downloadable models), without copying Filee, and without lying to users about it?

## 0. Method

1. **Tahqiq al-manat** — establish the facts before judging: Filee's published MIT documentation was read as *facts* (architecture, format matrix, engine inventory); no source code, assets, prose or trade dress was reused.
2. **Qiyas** — carry the ruling from an established case (al-asl = Filee on desktop) to a new case (al-far' = Android), by identifying the effective cause (al-'illa), separating essential from incidental attributes (tanqih), and checking conditions (shurut).
3. **Adversarial council** — seven seats attack the analogy from different angles; at least one proposed idea must visibly fall, otherwise the council is theatre.
4. **Maestro verdict** — the Lead closes with a verdict table, Step Zero, solution, metrics, risks and a first action.

**Seats:** Expert (Filee analyst) · Engineer (Android stack) · Devil's Advocate · Naive User · Policy Guardian (halal/legal) · Model Auditor · Market & Regulator · Maestro (Lead).

## 1. Al-Far' (the new case)

A private Android app that takes a file the user already owns, converts it on the device to a format they choose, and hands it back — with no upload, no account, no ads, no analytics. Optional AI model packs (OCR, upscaling) are downloaded only when the user asks.

Constraints from the human:

- Offline, or with models that can be downloaded and that are free for commercial use.
- Do not clone Filee's brand or assets; rebuild the idea.
- Must be an Android app that actually builds and runs.

## 2. Al-Asl (the established case)

Filee (MIT, C#/.NET Avalonia, desktop) is the precedent. Its published facts:

- ~270 formats, but **without** an office suite: its own DOCX/XLSX/PPTX/PDF readers plus permissive engines.
- A format registry plus a Dijkstra route planner that finds chains of up to three converter hops.
- A per-engine job queue, output naming, presets.
- Heavy engines are **optional downloads** (Tesseract OCR, FFmpeg, whisper.cpp, Ollama), and that is exactly how an MIT app can stay MIT while using GPL/AGPL tools.

The council accepts the asl as evidence of *what users reward*: 28k stars for local-only, broad, zero-decision conversion.

## 3. Al-'Illa (the effective cause)

**Draft 'illa:** users pay with trust and patience for *local-only processing* + *broad format coverage* + *zero-decision routing* (the app decides the chain, the user only picks a target).

Tanqih al-manat — what is essential and transfers:

| Attribute | Status | Why |
|---|---|---|
| Local-only processing | Essential | The trust claim; also the offline promise |
| Broad coverage through a converter graph | Essential | What makes it an all-in-one hub |
| Zero-decision routing (choose target, not engine) | Essential | The UX cause of 'it just works' |
| Optional heavy engine downloads | Essential | The only way to keep a permissive closed APK |
| Desktop donut / drag gesture | Incidental | A form factor, not the cause |
| Bundled LibreOffice/Pandoc/Ghostscript | Incidental (and forbidden) | Desktop mechanism that cannot transfer lawfully or practically |
| 270 formats exactly | Incidental | A number, not a cause; 40 well-chosen formats carry the same value |
| Filee name, logo, screenshots, prose | Never | Expression and trademark, not idea |

## 4. Conditions (shurut) for a valid qiyas

1. The new app must be original Kotlin; no Filee code, prose, assets, trade dress, or 'Filee-compatible' wording.
2. The APK ships only permissive (MIT/Apache-2.0/BSD) or clearly-compliant LGPL components, with notices and relink information.
3. GPL/AGPL engines never enter the APK; if ever used they must be separate, user-initiated downloads.
4. Model weights must be commercially licensed; non-commercial (CC-BY-NC, AGPL, proprietary) weights are excluded.
5. 'Offline' must be literally true: no silent network, downloads only on tap, with size and licence shown first.
6. Storage access only through the system picker (SAF); no MANAGE_EXTERNAL_STORAGE.
7. targetSdk 36 and 16 KB-aligned native libraries, or the app cannot ship in 2026.

## 5. Qiyas cards

### Qiyas 1 — the converter graph
**Far':** Android app with many formats. **Asl':** Filee registry + route planner. **'Illa:** decoupling formats from engines through a graph lets coverage grow without rewriting the UI. **Ruling:** adopt the *idea*, implement originally: FileFormat + FormatRegistry + Converter edges + Dijkstra planner (max 3 hops) + reachable-target chips. Evidence it worked: the build's own router passes unit tests for direct, 2-hop, unreachable and hop-limit cases.

### Qiyas 2 — optional engines as downloads
**Far':** Android app needs OCR and media codecs without importing GPL. **Asl:** Filee's optional engine download. **'Illa:** keeping copyleft/oversized components outside the distributed APK preserves licensing freedom and install size. **Ruling:** adopt for *data* (OCR traineddata, upscale weights) via SHA-256-verified packs; keep FFmpeg as a compliant LGPL-3.0 runtime library (dynamic link, notices, source offer), no GPL codecs.

### Qiyas 3 — breadth
**Far':** ship 270 formats. **Asl:** Filee's breadth. **'Illa (claimed):** breadth is what users reward. **Naqd (objection):** breadth on desktop came from bundled desktop binaries; on Android the same breadth would require GPL engines or months of format parsers. **Ruling:** reject 270; adopt ~40 formats across 12 families, chosen where Android's own APIs make quality achievable (images, PDF, simple OOXML, text/data, subtitles, e-books, archives, media).

### Qiyas 4 — trust as the product
**Far':** a converter users will trust with private files. **Asl:** Filee's local-only promise. **'Illa:** privacy is the differentiator against ad-funded upload sites. **Ruling:** adopt and make it verifiable: no INTERNET for conversion paths, no SDKs, Data safety 'no data collected', SAF-only, licence screen.

## 6. Objections and answers

**Naqd 1 (Engineer).** 'Android has no LibreOffice; office→PDF fidelity is impossible.' **Answer:** accept; scope office to *readable text/HTML/CSV/PDF-text* conversions and label them simple. Fidelity is a non-goal, honestly stated.

**Naqd 2 (Devil's Advocate).** 'You are cloning a competitor product; the MIT licence does not give you the market or the name.' **Answer:** the licence gives the freedom to reimplement ideas; the market is entered with differentiators (Arabic-first, all-in-one, verifiable trust). The name Sigha is provisional and requires trademark clearance — a human step, not an assumption.

**Naqd 3 (Policy Guardian).** 'FFmpegKit is LGPL-3.0 and its upstream is archived; shipping it is a legal and maintenance risk.' **Answer:** conditionally accepted: maintained fork, dynamic linking, notices, source offer and relink information, no GPL codecs; the legal memo records it as a conditional item with a Media3-only fallback if counsel rejects it.

**Naqd 4 (Model Auditor).** 'Users will download non-commercial model weights.' **Answer:** pack catalogue is limited to Apache-2.0 (Tesseract), BSD-3 (Real-ESRGAN), MIT (ONNX Runtime, Whisper roadmap); forbidden list documented; verification by SHA-256 before install.

**Naqd 5 (Market).** 'Distribution is the incumbents' moat; a new app with 1k installs changes nothing.' **Answer:** accepted as a real risk, not a refutation of the design. The plan targets a niche incumbents ignore (Arabic-first offline hub) and keeps the base app small and trustworthy; success metrics are defined before launch.

**Fariq (distinction).** 'Desktop drag-and-drop donut is not transferable.' **Answer:** agreed; it is incidental. Android's equivalent is the share sheet plus a target chooser; implemented as ACTION_SEND/SEND_MULTIPLE/VIEW ingress.

**Fasad i'tibar (invalidating assumption).** '270 formats is the product.' **Answer:** invalidated by evidence; breadth is a consequence of the desktop mechanism, not the cause of love. The product is the promise: *your file, converted, never leaves the phone*.

**Tahqiq manat (re-check the facts).** Two facts changed the plan mid-flight: (a) Play now requires targetSdk 36 — the project was upgraded to AGP 8.10.1 / Gradle 8.11.1 / SDK 36; (b) the market survey found zero Android Filee ports and no Arabic-first offline converter — the lane is open but unproven.

**Second-order objection (Maestro).** 'If you succeed, the app becomes a target for malware-like cloning and licence trolls.' **Answer:** keep the trademark searchable, publish notices, and avoid bundling anything of unclear provenance.

## 7. Ideas that fell in this council

| Idea | Seat that killed it | Reason |
|---|---|---|
| Bundle 270 formats via LibreOffice/Pandoc/Ghostscript | Policy Guardian + Engineer | GPL/AGPL incompatible with a closed APK; hundreds of MB; no Android port |
| Port the desktop drag-and-drop donut | Engineer | No global pointer hook on Android; wrong form factor |
| Ship Whisper transcription in v1 | Model Auditor + Engineer | Whisper.cpp build + model size + no clear v1 user need; roadmap only |
| Deliver 'offline' while downloading models silently | Policy Guardian | Deceptive; violates trust 'illa, Play policy and halal conditions |
| Write 7z archives | Engineer | Commons Compress is read-only for 7z; write support would be a separate project |
| RAR support | Policy Guardian | unRAR restriction; drop unless a user need is proven |
| Media3-only rewrite after FFmpegKit was chosen | Engineer | Media3 has no MP3 encoder; the LGPL FFmpegKit is the compliant way to keep audio breadth |
| Monetise with ads/analytics | Market + Policy Guardian | Destroys the trust claim and Data safety 'no data collected' |

## 8. Verdict table

| Decision | Verdict |
|---|---|
| Rebuild the idea as an original Android app | **ADOPT** |
| Graph router + reachable target chips | **ADOPT** (original implementation) |
| Share-sheet ingress instead of desktop drag | **ADOPT** |
| Optional SHA-256 model packs for OCR/upscale | **ADOPT** |
| FFmpegKit LGPL-3.0 with compliance file, no GPL codecs | **ADOPT WITH CONDITIONS** |
| Office fidelity (layout-perfect DOCX→PDF) | **REJECT for v1** |
| 270 formats | **REJECT**; ~40 formats across 12 families |
| RAR / 7z write | **REJECT** |
| Ads, analytics, accounts | **REJECT** |
| Arabic-first UI | **ADOPT as differentiator** |
| Using the Filee name or trade dress | **NEVER** |

## 9. Step Zero

Before any store listing, answer one question: **can a first-time user convert a private file on a mid-range Android phone, offline, in under a minute, and understand exactly what happened?** If the answer is no, nothing else in the plan matters.

## 10. Solution

- Kotlin + Jetpack Compose, Material 3, English + Arabic (RTL), light/dark.
- Core: FormatRegistry (data), Converter edges, ConverterRegistry, RoutePlanner (Dijkstra, max 3 hops).
- Engines: images/PDF (platform Bitmap/PdfRenderer + PDFBox-Android), office-simple (zip + XmlPullParser), text/data/subtitles/e-books/archives (Commons Compress), media (FFmpegKit-maintained 8.1.9, LGPL-3.0), OCR (Tesseract4Android + Apache-2.0 tessdata).
- Pipeline: SAF in, cache temp files, converter chain, publish to Download/Sigha (MediaStore on API 29+, public Downloads on 26–28), history of last 50.
- Model packs: catalogue + SHA-256 verified download on tap only, licence text shown first.

## 11. Metrics (decide continuation)

- Cold start under 2 s; 12 MP image conversion under 1.5 s.
- 100% of core conversions succeed with airplane mode on.
- Crash-free sessions above 99.5%; every failure produces a human message.
- Store: 'offline' claim survives review; Data safety shows no data collected.
- Arabic UI completeness: 100% of visible strings, RTL layout tested.

## 12. Risks (ranked)

1. **Distribution** (incumbents' moat) — mitigate with Arabic-first niche + trust positioning.
2. **License/maintenance** of FFmpegKit LGPL — mitigated by notices, source offer, Media3 fallback documented.
3. **Native ABI / 16 KB pages / APK size** — mitigated by arm64-v8a focus, verified 16 KB alignment, per-ABI splits if needed.
4. **Office expectations** — mitigated by honest labels and scope statement.
5. **Model pack hosting** — mitigated by SHA-256 verification and user-imported packs as fallback.
6. **Name/trademark** — unresolved; human legal clearance required before any store listing.

## 13. First action

Ship a debug APK that proves the promise end-to-end on one device: pick a DOCX, a HEIC and an MP4, convert them offline, find them in Downloads. Then run the aircraft-mode test and the 16 KB/API 36 checklist, and only then discuss the store.

## 14. Open questions for the human

1. Distribution markets and publishing entity (Iraq-only vs MENA vs global).
2. Final name + trademark clearance (Sigha is provisional; no direct Play match, but not a trademark search).
3. Play Console account, signing key, closed-test plan (12 testers x 14 days).
4. Model pack hosting: GitHub Releases vs own server vs user-imported.
5. Whether to publish a paid tier — the council recommends free/no-ads for v1.

## 15. Dissent register

- **Engineer:** wanted Media3-only for v1 to cut APK size; accepted LGPL FFmpegKit because MP3 encoding is a real user need and the build already integrates it.
- **Market:** wanted a narrower v1 (no media) to reduce QA surface; overruled by the user's explicit 'same idea' scope and the working media engine.
- **Devil's Advocate:** notes the app is a legitimate reimplementation, not a clone, but warns that the *name* could still create confusion; unresolved by design.
## 16. Implementation outcome (2026-11-17)

The verdict was executed. What is actually built differs from the plan only where evidence forced it:

| Council position | Outcome |
|---|---|
| Original Kotlin/Compose app, no Filee code or identity | Built: `com.sigha.converter`, 48-format catalogue, 32 converter edges, 12 engine families |
| Do not bundle GPL/AGPL engines | Kept out; FFmpegKit LGPL-3.0 shipped with notices and source offer |
| Models must be commercially licensed and downloaded on tap only | One OCR pack (tessdata_best, Apache-2.0, SHA-256 pinned) — download on explicit tap, then offline |
| Prefer permissive stack | Dropped ONNX Runtime for v1 (no }commercially clean, size-acceptable model shipped); upscaling deferred |
| 270 formats is not a v1 goal | 48 formats, every one with a working edge; 32 edges over 12 families |
| RAR/7z-write rejected on licence grounds | RAR has no edges; 7z is read-only (7z → ZIP) |
| Offline promise and Arabic-first UI are the differentiators | Both implemented: no network except an explicit model-pack tap; full EN/AR strings and RTL |
| Ship a debug APK proving the promise end-to-end | Built: 72.5 MB debug, 16 KB-aligned, `targetSdk 36`; 131 JVM tests green (`docs/qa/verification.md`) |

Deviations forced by evidence, recorded honestly:

1. Bundled `tessdata_fast` (5.5 MB) was removed — no code path used it; OCR is a one-tap download.
2. Scanned PDFs still hit the text extractor before OCR; the blank-text OCR fallback is the next fix.
3. The emulator available on this host could not render or expose Compose semantics, so device
   QA (the council's "first action") is still open — the debug APK is ready for it.

Evidence: `docs/qa/verification.md`, `docs/models/integration.md`, `NOTICE.md`.
