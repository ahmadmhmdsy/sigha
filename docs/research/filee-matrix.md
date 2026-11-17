# Filee to Android: takhrij al-manat — feature, engine and format matrix, Android feasibility, MVP and risks

> **Author:** filee-analyst (Expert / takhrij al-manat), teammate on shared task-2. **Date of record:** 2026-11-16 (Asia/Baghdad).
> **Status:** evidence-based research deliverable. It is **not** a final product spec and **not** a legal/fatwa opinion (that is legal-clearance).
> **Copying rule:** this document extracts **facts and ideas only**. No Filee code, assets, brand, name, or UI expression is reused in the Android app. Filee is MIT — if any expression were ever reused, the MIT notice must travel with it (copyright line quoted in section 0.3).
> **Evidence key (shorthand used in citations):**

| Key | Source file (all under docs/research/filee/, MIT, snapshot of github.com/KnifeLemon/Filee) | Verified read |
|---|---|---|
| A | docs_ARCHITECTURE.md | full (architecture, flow, donut, tools, watch folders, presets, Connect) |
| E | docs_ENGINES.md | ~90% (engine table, images/vector, office/PDF, media, archives, OCR/Whisper/Translate/Upscale) |
| F | docs_FORMAT-RULES.md | full (fields, grades, donut faces, naming rules) |
| C | docs_ADDING-A-CONVERTER.md | full (IConverter contract, registration, tests) |
| N | THIRD-PARTY-NOTICES.md | full (license table) |
| L | CHANGELOG.md | head + 2.5.0 section |
| G | LICENSE | full (MIT) |
| J | global.json | full (.NET SDK 10.0.100) |

Citation form: [A:32-81] = lines 32-81 of docs_ARCHITECTURE.md; local links are one click: [A:32-81](filee/docs_ARCHITECTURE.md#L32-L81). Android platform facts are cited by URL with **fetched 2026-11-16**; temporary saved copies are in .tmp_research/ and are disposable.
**UNVERIFIED** marks a claim I could not verify from a primary source. **HYPOTHESIS** marks causal/interpretive claims for the Qiyas Council to judge, not facts.

---

## 0. What Filee is (facts only)

### 0.1 Product, platform, version

| Fact | Evidence |
|---|---|
| Free, offline, local file converter for **desktop** Windows/macOS/Linux; Avalonia UI; .NET (SDK pinned 10.0.100) | [A:1-30](filee/docs_ARCHITECTURE.md#L1-L30), [J](filee/global.json) |
| MIT license, (c) 2026 Myeonggu Kim (KnifeLemon) and Filee contributors | [G:1-3](filee/LICENSE#L1-L3) |
| Recognizes **"about 270 formats"** | [F:3](filee/docs_FORMAT-RULES.md#L3) |
| Changelog records coverage growth 244 to 255 to 263 to 268 formats, newest section **2.5.0** | [L](filee/CHANGELOG.md) (2.5.0 section; whether 2.5.0 is the current public release on 2026-11-16 is **UNVERIFIED**) |
| No account, no cloud upload for conversion; optional engines are **downloaded** and verified by SHA-256, and can be installed offline from files | [E:100-149](filee/docs_ENGINES.md#L100-L149) |
| Also ships: CLI (filee convert/watch/formats/presets/engines, --json), watch folders, LAN "Filee Connect" (phone to PC) | [A:595-642](filee/docs_ARCHITECTURE.md#L595-L642), [A:840-847](filee/docs_ARCHITECTURE.md#L840-L847) |

### 0.2 The three user-facing ideas, one line each

1. **Ingress without a window:** a global drag gesture on a file opens a radial menu at the cursor; the drop converts immediately ([A:32-42](filee/docs_ARCHITECTURE.md#L32-L42)).
2. **Outcome without a decision:** the app picks the engine chain itself (cost-based routing, at most 3 hops); the user only picks a target format (a preset) ([A:43-48](filee/docs_ARCHITECTURE.md#L43-L48), [E:3-4](filee/docs_ENGINES.md#L3-L4)).
3. **Trust without a cloud:** every step runs locally; optional heavy engines are downloaded on demand, never a server; even the AI features run locally (Ollama on loopback, Whisper local, Tesseract local) ([E:108-110](filee/docs_ENGINES.md#L108-L110), [E:2403-2428](filee/docs_ENGINES.md#L2403-L2428), [E:2386-2402](filee/docs_ENGINES.md#L2386-L2402), [E:1735-1771](filee/docs_ENGINES.md#L1735-L1771)).

### 0.3 The license line we must carry if we ever reuse expression

> "MIT License / Copyright (c) 2026 Myeonggu Kim (KnifeLemon) and Filee contributors" — [G:1-3](filee/LICENSE#L1-L3)

Our project copies no expression, so nothing is owed today. Facts (format lists, engine names, costs) are not protected as expression, but **the curated taxonomy and preset names are Filee's work product**; we should write our own registry and our own names rather than transplant theirs (see section 7.5).

---

## 1. Feature inventory (what Filee does, grouped)

This is a **feature** inventory, not a format list (section 3). Each row states what exists and the evidence.

### 1.1 Conversion surface by family

| Family | What a user can do | Evidence |
|---|---|---|
| Images (raster) | ~40 picture formats to each other: JPG, PNG, WEBP, TIFF, BMP, GIF, ICO, AVIF, JPEG XL, JPEG 2000, PSD/PSB, TGA, PPM, QOI, DDS, PCX, XPM, XBM, WBMP, PAM, PFM, SGI, Sun Raster, MIFF, APNG (own reader/writer), HEIC (via FFmpeg), camera RAW (LibRaw), DICOM (uncompressed), FITS (own), Photo CD, OpenEXR, Radiance HDR, EMF/WMF (Windows), LIVP, KRA, PDN (own), XCF (own), VTF (own), EXE/DLL icon, ANI | [E:8](filee/docs_ENGINES.md#L8), [E:12-13](filee/docs_ENGINES.md#L12-L13) |
| Image tools | Annotate (pen/arrow/box/highlight/text/number), Add background (gradient/colour/picture, padding, corners, shadow, shape), Pixelate/blur, resize, crop, grayscale, metadata read/write via ExifTool | [A:214-249](filee/docs_ARCHITECTURE.md#L214-L249), [E:46](filee/docs_ENGINES.md#L46) |
| Vector | SVG/SVGZ to PDF (vector) and images, SVG to SVGZ, SVG to DXF, HP-GL/PLT to SVG, Tajima DST to SVG, JSON Canvas to SVG/Markdown, Gerber PCB to SVG/PDF/PNG/DXF, **raster to SVG (tracing, cost 40)** | [E:10](filee/docs_ENGINES.md#L10), [E:496-536](filee/docs_ENGINES.md#L496-L536) |
| PDF (read) | PDF to images (multi-page TIFF/GIF too), PDF to TXT/Markdown with reading order and two columns (no OCR), PDF to PPTX (page pictures + speaker notes), PDF to XLSX/ODS/CSV (tables with lines) | [E:13](filee/docs_ENGINES.md#L13), [E:21-24](filee/docs_ENGINES.md#L21-L24) |
| PDF (write/finish) | images to PDF (CMYK preserved), merge with bookmarks/links/forms, split, page ranges, page organising, lossless rotate/crop-box, **N-up**, compress, strip metadata, watermark, page numbers, AES-256 password, **PDF/A-2b** (Ghostscript) | [E:12](filee/docs_ENGINES.md#L12), [E:39](filee/docs_ENGINES.md#L39), [E:1083-1120](filee/docs_ENGINES.md#L1083-L1120) |
| Documents (OOXML and readers) | DOCM/DOTX/DOTM to DOCX; HWPX, DOCX, PDF, OFD, HTML, MHTML, EPUB, MOBI/AZW3, FB2, TXT, Markdown, XLSX/XLS/ODS/CSV, PPTX to DOCX (own writer, cost 8); **DOCX/XLSX/PPTX to PDF needs neither Office nor LibreOffice** (built-in readers + rhwp renderer) | [E:17](filee/docs_ENGINES.md#L17), [E:19-20](filee/docs_ENGINES.md#L19-L20), [E:48-50](filee/docs_ENGINES.md#L48-L50) |
| Legacy documents | HWP to HWPX, HWP/HWPX to PDF/TXT/MD/HTML (bundled rhwp + Unhwp); DOC/XLS/PPT/RTF/ODF and rare read-only formats via optional LibreOffice (~420 MB) | [E:14](filee/docs_ENGINES.md#L14), [E:35](filee/docs_ENGINES.md#L35), [E:37](filee/docs_ENGINES.md#L37) |
| Spreadsheets | XLSX, XLS, ODS, CSV, TSV to XLSX, ODS, CSV, TSV (one CSV/TSV per sheet); spreadsheets/DOCX tables/HTML tables/PDF tables to XLSX; Spreadsheet Print Studio (N-up assembly) | [E:16](filee/docs_ENGINES.md#L16), [E:23-24](filee/docs_ENGINES.md#L23-L24), [E:1068-1082](filee/docs_ENGINES.md#L1068-L1082) |
| Markup/text | Markdown to HTML/TXT; many source/code/INI formats to UTF-8 TXT without executing; markup formats to DOCX/ODT/RTF/EPUB via Pandoc (~42 MB) | [E:15](filee/docs_ENGINES.md#L15), [E:38](filee/docs_ENGINES.md#L38) |
| E-mail / signed files | EML, MSG to HTML/TXT/ZIP (attachments), MSG to EML; MHTML to HTML; P7M to the signed file inside (CMS/CAdES; signature not verified) then continue (P7M to PDF to PNG) | [E:25-26](filee/docs_ENGINES.md#L25-L26) |
| E-books | EPUB, MOBI/AZW/AZW3/PRC, FB2, HTMLZ, TXTZ to EPUB/TXT/HTML/Markdown; comics CBZ/CBR/CB7/CBT/CBC to PDF/EPUB/CBZ; PDF to CBZ; EPUB to KEPUB; rare formats via calibre (~216 MB) | [E:28](filee/docs_ENGINES.md#L28), [E:43](filee/docs_ENGINES.md#L43) |
| Archives | ZIP (incl. ANSYS WBPZ), 7Z, RAR, TAR (with GZ/BZ2/XZ/Z/7Z/LZ), CAB, ISO, DMG, split .001, Minecraft/Modrinth packs to folder or ZIP/7Z/TAR/TAR.GZ/TAR.XZ/packs; ALZ/EGG/lzip readers; compress into one archive | [E:36](filee/docs_ENGINES.md#L36), [E:1798-1903](filee/docs_ENGINES.md#L1798-L1903) |
| Media (video/audio) | All video/audio via optional FFmpeg (~100 MB): video to video (H.264/HEVC/AV1), video to GIF/APNG/frames/still, audio+subtitle extraction, audio to audio (APE, WavPack, TTA, DSD, DTS, ALAC), images to MP4/WEBM/MOV, Live/Motion Photos, clips, **merge video** (with music), split, strip metadata, slideshow | [E:42](filee/docs_ENGINES.md#L42), [E:2017-2319](filee/docs_ENGINES.md#L2017-L2319) |
| Subtitles | SRT, ASS/SSA, WebVTT, SAMI, LRC, KuGou KRC, TTML, MicroDVD, SCC to SRT/ASS/WebVTT/SAMI/LRC/TTML/MicroDVD/TXT; UTF-8, Korean CP949 read correctly | [E:31](filee/docs_ENGINES.md#L31), [E:1981-2008](filee/docs_ENGINES.md#L1981-L2008) |
| Data | JSON / JSON Lines / YAML / TOML / XML / CSV to each other; vCard/iCal to CSV; plist (XML + binary) to data formats/TXT; FreeMASTER TXT to CSV | [E:27](filee/docs_ENGINES.md#L27) |
| OCR | Tesseract (~50 MB, languages 0.4-10 MB on first use): scans/pictures to **searchable PDF** and text to DOCX/TXT/MD/HWPX; Apple Vision alternative on macOS | [E:40-41](filee/docs_ENGINES.md#L40-L41), [E:2320-2385](filee/docs_ENGINES.md#L2320-L2385) |
| Speech-to-text | Whisper (optional, Windows only, ~157 MB incl. model): video/audio to SRT/TXT in ~100 languages, local/offline; needs FFmpeg | [E:45](filee/docs_ENGINES.md#L45), [E:2386-2402](filee/docs_ENGINES.md#L2386-L2402) |
| Translation | Local **Ollama** over 127.0.0.1:11434 (never cloud models), document structure preserved, languages configurable; not an on-device model shipped with the app | [E:2403-2428](filee/docs_ENGINES.md#L2403-L2428) |
| Upscaling (AI) | upscayl-bin + NCNN/Vulkan, 2x to 4x, **9 models** (Siax, Superscale, WDN, RealESRGAN General, 4xLSDIR family, Nomos8kSC, HFA2k); 200 MP output guard | [E:2429-2441](filee/docs_ENGINES.md#L2429-L2441) |
| CAD / G-code | DWG to DXF; DWG/DXF to vector PDF/SVG/images; G-code to PDF/SVG/images (toolpath or slicer preview) | [E:29-30](filee/docs_ENGINES.md#L29-L30) |
| 3D / geo | GLB/glTF/STL/OBJ/3MF/PLY to each other, FBX to them, 3D to preview PNG/JPG; pictures to 3D relief (GLB/STL/OBJ); GeoJSON/KML/KMZ/GPX/Shapefile to each other and CSV; FIT/OSM read-only | [E:32-34](filee/docs_ENGINES.md#L32-L34) |
| Fonts | TTF, OTF, WOFF, WOFF2, EOT to each other; FON (NE FNT v2/v3) to TTF/OTF/WOFF/WOFF2/EOT; CFF outlines become TrueType | [E:18](filee/docs_ENGINES.md#L18) |
| Misc | ICNS to images; DjVu explicitly unsupported; ISO images; Windows JXR; ExifTool metadata; HDR tone-mapping on Windows | [E:9](filee/docs_ENGINES.md#L9), [E:11](filee/docs_ENGINES.md#L11), [E:46](filee/docs_ENGINES.md#L46), [E:2009-2016](filee/docs_ENGINES.md#L2009-L2016) |

### 1.2 Workflow and shell features (the part users actually feel)

| Feature | What it does | Evidence |
|---|---|---|
| **Global drag gesture to donut** | modifier + drag past a threshold opens a transparent, topmost, non-activating radial window before files are known; first DragEnter gives the file list; drop is reported as *Copy* so Explorer never deletes sources | [A:32-42](filee/docs_ARCHITECTURE.md#L32-L42) |
| **Donut ring as live toolbar** | ring drawn directly (no child controls) for speed; front/back faces; groups are one slice that fans out up to 6 actions and **cannot nest**; flip via Tab/wheel with 200 ms 3D turn; reduce-animations snaps; hit-testing math unit-tested | [A:147-213](filee/docs_ARCHITECTURE.md#L147-L213) |
| **Toolbar profiles / presets** | two faces per profile; ProfileSelector picks the first profile covering all dropped extensions (mixed-selection rules); PresetAvailability greys out slices with a reason; preset gallery (CC0 shares) in 2.5.0 | [A:34-40](filee/docs_ARCHITECTURE.md#L34-L40), [A:616-642](filee/docs_ARCHITECTURE.md#L616-L642), [L:22,31](filee/CHANGELOG.md#L22-L31) |
| **Fields and grades** | 14 everyday fields always on (image, pdf, document, spreadsheet, presentation, hwp, text, video, audio, ebook, archive, vector, subtitle, data); 12 hideable (CAD, 3D, geo, font, game, design, science, **developer on by default**, writing, video-pro, audio-pro, legacy); grade everyday/general/specialist/legacy; a grade alone hides nothing | [F:1-45](filee/docs_FORMAT-RULES.md#L1-L45) |
| **Browser drags** | pictures/links/downloads dragged out of Chrome/Edge/Firefox/Safari are resolved by a documented rule order (bytes, then picture, then DownloadURL, then extension-matching link, then pixels) | [A:83-140](filee/docs_ARCHITECTURE.md#L83-L140) |
| **Job queue** | per-engine concurrency; work folder mode 700; final outputs via OutputPathResolver (pattern, folder, conflict policy); source never overwritten; names reserved; failure/cancel deletes only its own paths | [A:49-54](filee/docs_ARCHITECTURE.md#L49-L54) |
| **Post-processing** | PDF finishing (watermark/page numbers/password) then PDF/A on every produced PDF; Replace the original off by default, then Recycle Bin/Trash with translated reasons | [A:55-65](filee/docs_ARCHITECTURE.md#L55-L65) |
| **Combine/join** | Merge into one PDF (bookmarks/links/forms kept); Compress into one archive (unconverted files); Merge videos/audio and Video from images (order picked, music under video, Loop music); Merge images (layout, gap, background, 200 MP guard) | [A:66-77](filee/docs_ARCHITECTURE.md#L66-L77) |
| **Watch folders** | rules per folder, stability checks, state file, tray app and CLI watch; browser/watch files never replace original | [A:595-614](filee/docs_ARCHITECTURE.md#L595-L614) |
| **History and errors** | Home to Recent conversions, retry, thumbnails, missing-font reporting per original source (renderers check only the source) | [A:78-81](filee/docs_ARCHITECTURE.md#L78-L81) |
| **Offline engine management** | engines pinned by SHA-256 in engines.json; staged unpack; offline download/install from folder; proxy setting; engines live outside app folder so updates keep them | [E:106-149](filee/docs_ENGINES.md#L106-L149) |
| **Localization, theming, update notices** | many languages (ko, en, zh, pl, pt-BR, Mongolian added in 2.5.0); update card levels required/recommended/optional | [L:9-16](filee/CHANGELOG.md#L9-L16) |
| **Filee Connect** | embedded Kestrel LAN server + QR pairing, phone to PC transfer, 2 GB quota | [A:840-847](filee/docs_ARCHITECTURE.md#L840-L847) |
| **Portable mode / installers** | normal, portable ZIP, ARM64 builds; transactional upgrade with staged payload | [L:32](filee/CHANGELOG.md#L32), [A:824-833](filee/docs_ARCHITECTURE.md#L824-L833) |

### 1.3 What Filee deliberately does *not* do

* **DjVu** is unsupported by design ([E:2009-2016](filee/docs_ENGINES.md#L2009-L2016)).
* **Microsoft Office / Hancom are never used**, even when installed ([E:102-104](filee/docs_ENGINES.md#L102-L104)).
* It never signs, verifies, or claims authenticity of signed files: P7M unwraps and **"the signature isn't checked"** ([E:26](filee/docs_ENGINES.md#L26)).
* It does not upload conversions to a service; Filee Connect is LAN transfer between the user's own phone and PC, not a cloud ([A:840-847](filee/docs_ARCHITECTURE.md#L840-L847)); its AI translator refuses cloud models ([E:2403-2428](filee/docs_ENGINES.md#L2403-L2428)).

---

## 2. Conversion engine architecture (why the result is possible)

### 2.1 Layers

    App (Avalonia): TriggerService(SharpHook) -> GestureDetector -> RadialController -> DonutMenu/RadialWindow
                    ConversionService -> ToastWindow ; MainWindow <-> UserDataStore(JSON)
    Core:           JobQueue -> RoutePlanner (Dijkstra over converter edges) -> IConverter steps
                    FormatRegistry . Preset . OutputPathResolver . ToolbarProfile . ProfileSelector
    Engines:        built-in libs + bundled binaries + optional downloads

Source: [A:3-30](filee/docs_ARCHITECTURE.md#L3-L30).

The **contract** that makes this extensible: every converter declares Id, DisplayName, MaxParallelism, and a set of Edges (from-format to to-format with a Cost, 10 by default); ConvertAsync receives a step with an allocated output and its own work directory, must never modify the input, runs external programs through ProcessRunner, and finds bundled binaries through EngineEnvironment.FindBundled; converters are registered in EngineRegistry.CreateAll ([C](filee/docs_ADDING-A-CONVERTER.md)).

### 2.2 Format registry and the conversion graph

* A format has an id, display name, category and **fields + grade** ([F:13-21](filee/docs_FORMAT-RULES.md#L13-L21)).
* Converters contribute directed edges; the default cost is 10, and each engine's notes tune costs so the intended route wins ([C](filee/docs_ADDING-A-CONVERTER.md)). Real cost facts: tracing 40 ([E:531](filee/docs_ENGINES.md#L531)), DOCX writer 8 ([E:764](filee/docs_ENGINES.md#L764)), EML 10 ([E:1139](filee/docs_ENGINES.md#L1139)), e-books through calibre 20 and calibre PDF edges 23 ([E:1712](filee/docs_ENGINES.md#L1712), [E:1719-1721](filee/docs_ENGINES.md#L1719-L1721)), WeasyPrint HTML to PDF 1 but **source-restricted** ([E:1747](filee/docs_ENGINES.md#L1747)), markup to format 11 ([E:1959](filee/docs_ENGINES.md#L1959)), PPTX 20 ([E:1962](filee/docs_ENGINES.md#L1962)), subtitle extraction 11 ([E:2184](filee/docs_ENGINES.md#L2184)), GIF/APNG 12, PNG to video and APNG to video 11 ([E:2302-2304](filee/docs_ENGINES.md#L2302-L2304)), drawing 15 ([E:471](filee/docs_ENGINES.md#L471)).
* **Policy flags on edges** (not costs) express real constraints: ImageSourcesOnly (tracing only from raster), DirectOnly (PDF to PPTX pictures is a route of its own, never a step), Ocr (OCR edge planned first when OCR requested), Metadata (metadata-only edges never used for ordinary routes), Unshaped (rhwp text shaping; JobQueue re-plans with LibreOffice when complex scripts are detected, the documented Arabic/Hebrew/Indic limitation), Sources (WeasyPrint only from HTML/MD/EML/MSG/MHTML), MotionPhoto/LivePhoto/FrameSequence (media specials), EnvelopeOpener + PlanThrough (P7M to inner file to onward) ([A:43-48](filee/docs_ARCHITECTURE.md#L43-L48), [E:6-46](filee/docs_ENGINES.md#L6-L46), [E:52-60](filee/docs_ENGINES.md#L52-L60)).

### 2.3 Routing (the "no decisions" part)

1. For every input file, **Dijkstra** finds the cheapest chain of edges from source format to target: **at most 3 hops** ([A:43-44](filee/docs_ARCHITECTURE.md#L43-L44)).
2. Ties are broken by **engine priority** (Settings to Engines, higher wins) ([E:3-4](filee/docs_ENGINES.md#L3-L4), [A:43-44](filee/docs_ARCHITECTURE.md#L43-L44)).
3. Costs are deliberately arranged so built-in routes beat optional heavy engines **where a built-in route exists** ("LibreOffice's and Pandoc's costs are set so the built-in route always wins where one exists", [E:50](filee/docs_ENGINES.md#L50)); a user can still re-prioritize to change outcomes ([E:1719-1721](filee/docs_ENGINES.md#L1719-L1721)).
4. A missing engine at run time is surfaced on the slice ("Needs LibreOffice ..."), not as a crash ([E:108-110](filee/docs_ENGINES.md#L108-L110)).

### 2.4 Execution, safety, output

* JobQueue runs steps with **per-engine concurrency limits** (IConverter.MaxParallelism: 0 = one per CPU core, 1 = not thread-safe) ([C](filee/docs_ADDING-A-CONVERTER.md), [A:49](filee/docs_ARCHITECTURE.md#L49)).
* Intermediate files live in a per-user work folder (%TEMP%/Filee, ~/.cache/filee, ~/Library/Caches/Filee, mode 700 because /tmp is shared) ([A:49-51](filee/docs_ARCHITECTURE.md#L49-L51)).
* Final naming: OutputPathResolver (name pattern, subfolder rule, conflict policy); **the source is never overwritten**; each name is **reserved** while held; a failed/cancelled job deletes only paths that were its own and removes an output folder it created if empty ([A:51-54](filee/docs_ARCHITECTURE.md#L51-L54), [A:72-73](filee/docs_ARCHITECTURE.md#L72-L73)).
* Post-steps: PDF finishing then PDF/A on every PDF produced; ReplaceOriginal (off by default) only for the source's own format/extension family and via Recycle Bin/Trash with a translated reason if impossible ([A:55-65](filee/docs_ARCHITECTURE.md#L55-L65)).
* "Only if smaller" copies keep the source; watch/browser files never replace ([A:64-65](filee/docs_ARCHITECTURE.md#L64-L65)).

### 2.5 Presets, profiles, discoverability

* A **Preset** = target format + options (used by both donut slices and queue jobs). A **ToolbarProfile** = front/back preset id lists; **ProfileSelector** chooses by covered extensions; **PresetAvailability** disables slices with a reason ([A:34-40](filee/docs_ARCHITECTURE.md#L34-L40)).
* The donut is a *live toolbar*: edit mode on the same control, drag to reorder, groups of at most 6, no nested groups; the ring editor has a real-size preview window ([A:147-213](filee/docs_ARCHITECTURE.md#L147-L213)).
* Fields/grades keep default exposure small without deleting capability: hideable fields exist, only Developer is on for a new install, and **only new installs get new defaults** (existing users' donuts are not changed) ([F:22-45](filee/docs_FORMAT-RULES.md#L22-L45)).

### 2.6 Design principles that produce the observed result (P1-P10)

| # | Principle | Evidence |
|---|---|---|
| P1 | **Registry over hardcoding**: formats and conversions are data/edges, so coverage grows without UI rewrites | [C](filee/docs_ADDING-A-CONVERTER.md), [A:16-19](filee/docs_ARCHITECTURE.md#L16-L19) |
| P2 | **Cost = product policy**: one routing algorithm, but costs encode which engine should win and which routes are forbidden | [A:43-46](filee/docs_ARCHITECTURE.md#L43-L46), [E:50](filee/docs_ENGINES.md#L50) |
| P3 | **Never destroy the source**: no overwrite, reserved names, trash only, own paths only | [A:49-65](filee/docs_ARCHITECTURE.md#L49-L65) |
| P4 | **Honest availability**: unavailable slice says why; missing engine says what is needed; no silent fallback that lies | [E:108-110](filee/docs_ENGINES.md#L108-L110), [A:40](filee/docs_ARCHITECTURE.md#L40) |
| P5 | **Local by construction**: engines are local binaries/libraries, downloads verified by hash, AI via loopback/offline models | [E:106-149](filee/docs_ENGINES.md#L106-L149) |
| P6 | **Heavy capability is optional**: small installer, on-demand engines (420 MB LibreOffice, 100 MB FFmpeg, 216 MB calibre) | [E:37-45](filee/docs_ENGINES.md#L37-L45) |
| P7 | **Progressive disclosure**: 14 everyday fields on by default, 12 hidden, grades; about 270 formats without a 270-item menu | [F:1-45](filee/docs_FORMAT-RULES.md#L1-L45) |
| P8 | **One gesture, zero configuration**: ingress is a drag; output is the slice under the cursor | [A:32-42](filee/docs_ARCHITECTURE.md#L32-L42) |
| P9 | **Undo-shaped side effects**: trash, not delete; replace only on explicit opt-in; own-path cleanup | [A:55-65](filee/docs_ARCHITECTURE.md#L55-L65) |
| P10 | **Reversibility of settings**: engine priority editable, fields editable, donut editor with preview; defaults change only for new installs | [E:3-4](filee/docs_ENGINES.md#L3-L4), [F:43-45](filee/docs_FORMAT-RULES.md#L43-L45) |

### 2.7 Where the architecture carries hidden costs (relevant to Android)

Evidence-based tensions, so we do not copy the shape blindly:

1. **Optional engines are desktop binaries**. LibreOffice/Pandoc/Ghostscript/calibre/FFmpeg/Tesseract are desktop builds; this is *the* mechanism that gives desktop coverage and is **not portable to Android** (section 4) ([E:37-45](filee/docs_ENGINES.md#L37-L45)).
2. **Bundled native programs need a shim for each OS**; Filee ships Windows runtime DLLs next to each binary and adapts to Windows/macOS/Linux behaviors ([E:150-160](filee/docs_ENGINES.md#L150-L160)). Android's equivalent complexity is ABI, 16 KB page size, and Play packaging.
3. **Text shaping is a known hole**: rhwp "draws one glyph per character", so PDFs from complex scripts are detected and re-planned through LibreOffice ([E:52-60](filee/docs_ENGINES.md#L52-L60)). On Android, **Arabic shaping is a first-class requirement**, not an edge case. The built-in renderer must shape (HarfBuzz or the Android text stack via StaticLayout/Skia) or Arabic output will be broken. **This is a hard requirement for this market.**
4. **Dependencies on LAN/local services** (Ollama translator) make a feature "local" only for users who already run Ollama; Android must ship models or defer the feature ([E:2403-2428](filee/docs_ENGINES.md#L2403-L2428)).
5. **Licensing of the bundled engines is mixed**: several are GPL/AGPL; a closed commercial app cannot ship them as one process ([N](filee/THIRD-PARTY-NOTICES.md)); see Appendix A.

---

## 3. Format-category matrix (Filee coverage, by cause)

Columns: category | representative inputs | representative outputs | Filee engine(s) | note / limit. Not exhaustive (Filee lists about 270 formats); this captures the families that decide Android scope.

| Category | Inputs (examples) | Outputs (examples) | Engine(s) | Note |
|---|---|---|---|---|
| Raster image | JPG, PNG, WEBP, TIFF, BMP, GIF, ICO, AVIF, JXL, JP2, PSD/PSB, TGA, PPM, QOI, DDS, PCX, XPM, XBM, WBMP, PAM, PFM, SGI, MIFF, APNG, HEIC, RAW, DICOM, FITS, EXR, HDR, LIVP, KRA, PDN, XCF, VTF | each other; plus PDF/ICNS/HEIC | ImageMagick/Magick.NET (+own APNG/PDN/XCF/VTF/FITS/DICOM readers; FFmpeg for HEIC; LibRaw for RAW) | desktop codec breadth is the differentiator ([E:8](filee/docs_ENGINES.md#L8)) |
| Windows HDR / JXR | JXR/WDP/HDP | images, PDF, ICNS, HEIC, SVG (via PNG) | WIC (Windows only) | OS-bound, not on Android ([E:9](filee/docs_ENGINES.md#L9)) |
| Vector | SVG/SVGZ, AI (PDF-compatible), PLT HP-GL/2, DST, JSON Canvas, Gerber | SVG, PDF, PNG/JPG, DXF, Markdown | Svg.Skia + SkiaSharp + ACadSharp + own parsers | tracing is the only raster-to-SVG route, cost 40 ([E:10](filee/docs_ENGINES.md#L10), [E:531](filee/docs_ENGINES.md#L531)) |
| PDF read | PDF | images, TXT, Markdown, PPTX (pictures+notes), XLSX/ODS/CSV (tables) | PDFium, PdfPig, PDFsharp | no OCR in PDF-to-text; tables need drawn lines ([E:13](filee/docs_ENGINES.md#L13), [E:21-24](filee/docs_ENGINES.md#L21-L24)) |
| PDF write/finish | images; PDFs; office docs | PDF, merged/split/N-up PDF, PDF/A-2b, protected/watermarked PDF | PDFsharp + Ghostscript (optional) | finishing pipeline after last step ([E:12](filee/docs_ENGINES.md#L12), [E:39](filee/docs_ENGINES.md#L39)) |
| Office OOXML | DOCX/DOCM/DOTX/DOTM, XLSX/XLSM/XLTX, PPTX/PPTM/POTX/POTM/PPSX, DOC/XLS/PPT (LO), ODT/ODS/ODP (LO), OFD?, HWPX | HWPX, DOCX, PDF, images, HTML, Markdown, TXT, XLSX, PPTX | built-in readers + HWPX writer + DOCX writer + rhwp renderer; LibreOffice optional for legacy | **no Office, no LibreOffice needed for DOCX/XLSX/PPTX to PDF** ([E:19-20](filee/docs_ENGINES.md#L19-L20), [E:48-50](filee/docs_ENGINES.md#L48-L50)) |
| Legacy HWP | HWP, HWPX | TXT, MD, HTML, PDF, HWPX to HWP | Unhwp + rhwp (bundled) | Korean market feature; irrelevant to Android MVP ([E:14](filee/docs_ENGINES.md#L14), [E:35](filee/docs_ENGINES.md#L35)) |
| Spreadsheets | XLSX, XLS, ODS, CSV, TSV | XLSX, ODS, CSV, TSV, PDF (print studio); tables from DOCX/HTML/PDF to XLSX | ExcelDataReader + own writers | one CSV per sheet ([E:16](filee/docs_ENGINES.md#L16)) |
| Text/markup | Markdown, TXT, source code, INI, HTML, MHTML, EML, MSG, P7M inner file | TXT, HTML, MD, PDF, DOCX, HWPX, EPUB, XLSX | Markdig + DOCX/HWPX writers; Pandoc optional | text-only conversions are cheap and safe ([E:15](filee/docs_ENGINES.md#L15), [E:25-26](filee/docs_ENGINES.md#L25-L26), [E:38](filee/docs_ENGINES.md#L38)) |
| E-books | EPUB, MOBI/AZW/AZW3/PRC, FB2, HTMLZ, TXTZ, CBZ/CBR/CB7/CBT/CBC, KEPUB | EPUB, HWPX/PDF, TXT, HTML, MD, FB2, HTMLZ/TXTZ, CBZ, PDF | built-in e-book engine + calibre optional | calibre is GPL-3.0 ([E:28](filee/docs_ENGINES.md#L28), [E:43](filee/docs_ENGINES.md#L43)) |
| Archives / ISO | ZIP, 7Z, RAR, TAR(+compression), CAB, ISO, DMG, split .001, WBPZ, MC packs, ALZ, EGG, lzip | folder; ZIP, 7Z, TAR, TAR.GZ/BZ2/XZ; ISO (UDF 2.01) | 7-Zip bundled (~2.5 MB) + built-in readers | 7-Zip is LGPL-2.1+ with **unRAR restriction** ([E:36](filee/docs_ENGINES.md#L36), [N](filee/THIRD-PARTY-NOTICES.md)) |
| Video | MP4, MKV, WEBM, MOV, AVI, WMV; images; Live/Motion photos; GIF/APNG | MP4, WEBM, MOV; GIF/APNG; frames/still; audio out; HEIC from video | FFmpeg (optional, GPL build) | GPL-3.0-or-later build in Filee ([E:42](filee/docs_ENGINES.md#L42), [N](filee/THIRD-PARTY-NOTICES.md)) |
| Audio | MP3, AAC/M4A, WAV, FLAC, OGG/Vorbis, Opus, APE, WavPack, TTA, DSD, DTS, ALAC | each other; ringtones M4R; cut clips | FFmpeg (optional) | "all audio" is FFmpeg's doing, not platform APIs ([E:42](filee/docs_ENGINES.md#L42)) |
| Subtitles | SRT, ASS/SSA, WebVTT, SAMI, LRC, KRC, TTML, MicroDVD, SCC | SRT/ASS/WebVTT/SAMI/LRC/TTML/MicroDVD/TXT | built-in | pure text work; SRT is the hub ([E:31](filee/docs_ENGINES.md#L31), [E:2184](filee/docs_ENGINES.md#L2184)) |
| Data | JSON/JSONL, YAML, TOML, XML, CSV, plist, vCard, iCal | each other; CSV for tables | System.Text.Json, YamlDotNet, Tomlyn, XML | no external dependency ([E:27](filee/docs_ENGINES.md#L27)) |
| E-mail / signed | EML, MSG, MHTML, P7M | HTML, TXT, ZIP(attachments), EML; inner file then onward | MimeKit, MsgReader, PKCS#7 | signature **not verified** ([E:25-26](filee/docs_ENGINES.md#L25-L26)) |
| OCR | scans, images, PDF pages | searchable PDF; DOCX/TXT/MD/HWPX | Tesseract optional (~50 MB + langs) | OCR edge planned first; Apple Vision on macOS ([E:40-41](filee/docs_ENGINES.md#L40-L41), [E:2320-2385](filee/docs_ENGINES.md#L2320-L2385)) |
| Speech | video/audio | SRT, TXT | Whisper (Windows only, ~157 MB) | needs FFmpeg; local ([E:45](filee/docs_ENGINES.md#L45)) |
| Translation | text/document model | text in target language | local Ollama (loopback) | user must run Ollama; refuses cloud ([E:2403-2428](filee/docs_ENGINES.md#L2403-L2428)) |
| AI upscale | PNG/JPG/WEBP/ICO/AVIF/BMP/TIFF | 2x to 4x images | upscayl-bin + NCNN/Vulkan, 9 models | upscayl-bin AGPL-3.0; models various permissive ([E:2429-2441](filee/docs_ENGINES.md#L2429-L2441), [N](filee/THIRD-PARTY-NOTICES.md)) |
| Fonts | TTF, OTF, WOFF, WOFF2, EOT, FON | each other | built-in | pure binary work, desktop-grade library ([E:18](filee/docs_ENGINES.md#L18)) |
| CAD | DWG, DXF | DWG, DXF, vector PDF/SVG/images | ACadSharp + built-in renderer | DWG needs ACadSharp; Android has no equivalent free lib ([E:29](filee/docs_ENGINES.md#L29)) |
| G-code | G-code | vector PDF/SVG/images | built-in | text/geometric renderer ([E:30](filee/docs_ENGINES.md#L30)) |
| 3D | GLB, glTF, STL, OBJ(+MTL), 3MF, PLY, FBX | each other; preview PNG/JPG; relief from pictures | SharpGLTF + built-in | FBX read; relief generator ([E:32-33](filee/docs_ENGINES.md#L32-L33)) |
| Geo | GeoJSON, KML, KMZ, GPX, Shapefile, FIT, OSM | each other + CSV | built-in | projection handling included ([E:34](filee/docs_ENGINES.md#L34)) |
| Design/legacy | PSD/PSB, XCF, PDN, KRA, VTF | PSD/PDN/XCF (layers merged or kept) | ImageMagick + own readers | niche; desktop-specific plugins ([E:8](filee/docs_ENGINES.md#L8)) |

### 3.1 The taxonomy Filee uses to hide 270 formats (copy the *concept*, not the names)

* 14 ever-present fields; 12 hideable fields; 4 grades. A format may belong to several fields via Overlaps (e.g. dxf is cad + vector; srt is subtitle + video + audio) ([F:15-21](filee/docs_FORMAT-RULES.md#L15-L21)).
* The donut has **front = convert**, **back = edit**; groups hold at most 6 non-group actions, no nesting ([A:208-212](filee/docs_ARCHITECTURE.md#L208-L212), [F:6](filee/docs_FORMAT-RULES.md#L6)).
* Naming rule: a preset is named by what the user gets, not by the engine ([F:8-9](filee/docs_FORMAT-RULES.md#L8-L9)).

---

## 4. Android feasibility: verdict per category

**Verdict scale:** HIGH = platform API or mature permissive library gives it directly; MEDIUM = doable with known gaps/effort; LOW = significant engine/port work or fidelity loss; NEVER = no lawful/practical on-device path for this product.

**Android platform facts used below (all fetched 2026-11-16):**

* Play target API: from **Aug 31 2026** new apps and updates must target **Android 16 (API 36)**; existing apps API 35+. <https://developer.android.com/google/play/requirements/target-sdk>
* 16 KB page size: apps targeting API 35+ on 64-bit must support 16 KB; **from Feb 1 2027** non-compliant updates cannot be released. <https://developer.android.com/guide/practices/page-sizes>
* PdfRenderer.Page.getTextContents() **added in API 35**. <https://developer.android.com/reference/android/graphics/pdf/PdfRenderer.Page>
* Platform media codecs/containers: <https://developer.android.com/media/platform/supported-formats>
* Media3 Transformer supported formats/muxers: <https://developer.android.com/media/media3/transformer/supported-formats>
* Foreground service types (mediaProcessing for media conversion; dataSync): <https://developer.android.com/develop/background-work/services/fg-service-types>
* FFmpegKit retirement + FFmpegKitNext continuation (source-only, LGPL-3.0 default / GPL-3.0 with GPL libs; **no Maven/CocoaPods binaries**): <https://github.com/arthenica/ffmpeg-kit/>, <https://tanersener.medium.com/saying-goodbye-to-ffmpegkit-33ae939767e1>, <https://github.com/arthenica/ffmpeg-kit-next>

| Category | Verdict | Concrete Android stack | Hard limits / why |
|---|---|---|---|
| Raster image (JPEG/PNG/WebP/BMP/GIF/HEIC/AVIF) | **HIGH** (except GIF/HEIC/AVIF encode) | BitmapFactory/ImageDecoder (API 28+), Bitmap.compress (JPEG, PNG, WEBP; WEBP_LOSSLESS API 30+), HeifWriter/MediaCodec for HEIC (encode API 28+), MediaCodec for AVIF (encode **API 34+**, decode 12+ per platform docs) | No platform TIFF, ICO, JXL, PSD, RAW, EXR, HDR, VTF, XCF, PDN, KRA. GIF encode absent (own LZW encoder feasible); HEIC/AVIF encode device-codec-dependent. |
| Vector SVG | **MEDIUM** | AndroidSVG (Apache-2.0) renders SVG to Bitmap; SVG to PDF by drawing into PdfDocument canvas (vector) | Full SVG 1.1/1.2 Tiny minus filters; no SVG-to-DXF/tracing; AI-to-PDF needs a PDF parser (PdfBox-Android can partially read AI-as-PDF). |
| PDF render / read | **HIGH** | android.graphics.pdf.PdfRenderer (API 21+) for page bitmaps; text via getTextContents() only **API 35+**, else **PdfBox-Android 2.0.27.0** (Apache-2.0) PDFTextStripper | No built-in PDF-to-DOCX/XLSX/PPTX; table extraction needs own heuristics. |
| PDF write / merge / finish | **HIGH** | PdfDocument (simple vector/text pages), **PdfBox-Android** (merge, split, encryption, watermark, page overlay, metadata; Apache-2.0) | PDF/A-2b and Ghostscript quality: **NEVER** realistically; PDF/A claim must be dropped. Password/AES via PDFBox. |
| Office OOXML (DOCX/XLSX/PPTX) | **MEDIUM** (read/extract), **LOW** (write), **NEVER** (fidelity print) | Own ZIP + XML readers (java.util.zip + XmlPullParser); XLSX sharedStrings/sheet to CSV; DOCX word/document.xml runs to TXT/HTML; PPTX slide XML to TXT. XLSX writing possible via own OOXML writer (MEDIUM). Apache POI/docx4j on Android are **UNVERIFIED** and heavy/desugaring-risk | Layout-faithful DOCX-to-PDF on-device is **NEVER** (no LibreOffice/Word engine; Collabora is a commercial product, not a bundleable free engine). "Preview-quality" export with own renderer is a different, honest feature. |
| Legacy Office (DOC/XLS/PPT/RTF/ODF) | **LOW/NEVER** for v1 | none reliable on Android | Binary formats + ODF need a real layout engine; defer. |
| Markdown/HTML/TXT | **HIGH** | own parsers (CommonMark via commonmark-java BSD-2 or Markwon Apache-2.0 for rendering); text to PDF via PdfDocument + StaticLayout (Android text stack shapes Arabic) | HTML-to-PDF: own CSS-subset renderer (MEDIUM) or offscreen WebView + PrintDocumentAdapter (**UNVERIFIED** for direct file output); no WeasyPrint-class layout. |
| E-books | **LOW** (read EPUB to TXT/HTML possible MEDIUM; MOBI/AZW3/KEPUB **NEVER** v1) | EPUB = ZIP+XHTML, parse with own code | Calibre-class conversion impossible. |
| Archives | **HIGH** (ZIP/TAR/GZ/BZ2/XZ read+write; 7z **read-only**), **MEDIUM** (RAR read) | **Apache Commons Compress** (Apache-2.0): zip, tar, gz, bz2, xz, 7z (SevenZFile read-only), arj, cpio, lz4, zstd, snappy; junrar (UnRAR license, extraction only) for RAR | 7z **write** is not supported by Commons Compress, so it needs a 7-Zip JNI (LGPL) or drop 7z output v1. RAR read-only requires UnRAR license review; never create RAR. |
| Video | **MEDIUM** | MediaExtractor/MediaCodec/MediaMuxer; **Media3 Transformer** (Apache-2.0) for transcode/trim/transmux; MP4 default muxer, WebM/AAC/Ogg/WAV muxers require wrapping in a custom Muxer.Factory | Container/codec coverage = platform + device encoders; no AVI/WMV/FLV/MKV **output** (MKV input via extractors, depth UNVERIFIED); VP9/AV1 encode only on capable devices; no GIF/APNG encode (own encoder or drop); **no FFmpeg** (FFmpegKit retired; FFmpegKitNext source-only with LGPL-3.0/GPL obligations). |
| Audio | **MEDIUM** (input broad-ish; output gaps) | MediaExtractor + MediaCodec: AAC encode/decode, FLAC encode (4.1+)/decode, Opus encode (10+)/decode (5+), Vorbis decode; WAV via own writer; Media3 WavMuxer, AacMuxer, OggMuxer | **MP3 encode absent from platform** (decode only), so mp3 output requires LAME (LGPL) or FFmpeg; AMR/APE/WavPack/DSD/DTS/ALAC unsupported. FLAC muxer in Media3 is **UNVERIFIED** (write the container ourselves or offer WAV). |
| Subtitles | **HIGH** | own text parsers (SRT/VTT/ASS) | Pure text/time math; ASS styling is partially lost when converting. |
| Data | **HIGH** | kotlinx.serialization or org.json (JSON), SnakeYAML (Apache-2.0) or snakeyaml-engine, TomlJ or own (TOML), Android XmlPullParser/javax.xml (XML), own CSV | Tabular vs tree mismatch rules matter (JSON/YAML to CSV only for tables). |
| OCR | **HIGH** (images/PDF pages to text; searchable PDF MEDIUM) | **Tesseract4Android** (Apache-2.0; Tesseract 5.5.1, Leptonica 1.85.0, tessdata_fast Apache-2.0, Arabic available) | APK size per ABI (~10-15 MB native + traineddata per language 1-4 MB); searchable PDF needs invisible-text overlay via PDFBox (MEDIUM). No Apple Vision. |
| Speech-to-text | **MEDIUM** | **whisper.cpp** (MIT) via JNI + ggml model (MIT); Android example exists (<https://github.com/ggml-org/whisper.cpp/blob/master/examples/whisper.android/README.md>); audio decode to 16 kHz mono PCM via MediaExtractor/MediaCodec | Model sizes (tiny ~75 MB, base ~148 MB, quantized smaller); CPU speed/thermal; 16 KB alignment for NDK libs; no FFmpeg dependency if we feed PCM ourselves. |
| AI upscale | **MEDIUM** | **NCNN** (BSD-3) + **Real-ESRGAN** models (BSD-3), NMKD (WTFPL), LSDIR (CC-BY-4.0 attribution); Vulkan via NCNN | Not upscayl-bin (AGPL). Model download must be user-initiated with license shown; thermal/time on mid-range phones. |
| Translation | **MEDIUM/LOW for v1; UNVERIFIED license path** | Option A: **ML Kit on-device Translation** (free, model download via Play services; commercial use per ML Kit terms, **UNVERIFIED**, legal to confirm); Option B: **llama.cpp** (MIT) + small instruct model (e.g. Qwen2.5-1.5B-Instruct Apache-2.0); Option C: OPUS-MT (CC-BY-4.0) + ONNX Runtime Mobile (MIT) or MNN (Apache-2.0) | Filee used local Ollama = desktop LLM, not on-device; NLLB-200 is **CC-BY-NC-4.0, not commercial**. Arabic quality unverified per option. |
| Fonts | **LOW v1** | WOFF2 (Google woff2 lib, MIT) via NDK, own TTF/OTF parsing | TTF to OTF (quadratic to cubic) is real engine work; EOT is dead; not worth v1. |
| CAD | **NEVER v1** | none | DWG proprietary (ODA not free); ACadSharp is .NET. |
| G-code | **LOW** | own parser + renderer | Niche; text renderer possible later. |
| 3D | **MEDIUM/LOW** | assimp (BSD-3) NDK or own glTF/STL/OBJ; Filament (Apache-2.0) for preview | FBX import heavy; conversions feasible but not MVP. |
| Geo | **LOW** | own GeoJSON/KML/GPX text handling (no Shapefile without a port) | Shapefile/projections need a JTS-class lib; defer. |
| Design/legacy (PSD/XCF/PDN/KRA/VTF) | **NEVER/LOW v1** | none | Desktop codec breadth; PSD read partially possible later. |
| RAW/HDR | **LOW** | LibRaw (LGPL-2.1/CDDL) via NDK possible | LGPL relinking compliance + size; defer. |

### 4.1 Cross-cutting Android feasibility notes

1. **The desktop 'illa cannot be cloned mechanism-for-mechanism.** Desktop coverage comes from bundling desktop binaries; Android coverage must come from platform APIs plus a few permissive native and JVM libraries. The gap is structural, not a matter of effort.
2. **FFmpeg is the big asymmetry.** Filee's media breadth = FFmpeg. On Android FFmpegKit is retired; FFmpegKitNext has **no prebuilt packages** and is **LGPL-3.0 by default / GPL-3.0 with GPL libs**; shipping it means LGPL-3.0 compliance (relinkable shared libs + notices) or GPL. Options: (a) platform MediaCodec/Media3 only (narrower, clean); (b) self-built LGPL FFmpeg shared libs with a documented relink path (legal approval needed); (c) skip media conversion in v1. **Decision required from Lead and legal.**
3. **Arabic shaping is mandatory, not optional.** Android's StaticLayout/TextPaint/Skia shapes Arabic correctly; any custom PDF text renderer must use the Android text stack or HarfBuzz. Never port Filee's rhwp-style unshaped renderer. This is a market-critical requirement (Iraq).
4. **Permissions/storage:** SAF (ACTION_OPEN_DOCUMENT, ACTION_CREATE_DOCUMENT, ACTION_OPEN_DOCUMENT_TREE, persistable URI grants) is the only general file path; READ_MEDIA_IMAGES/VIDEO/AUDIO (Android 13+) and All files access (restricted, Play approval) cover the rest. No arbitrary watch-folder semantics on shared storage.
5. **Background limits:** conversion must run in a foreground service; **mediaProcessing** FGS type exists specifically for converting media, with a **6-hour-per-24 h limit** shared across the app's mediaProcessing services, and the service must stop itself at completion/timeout (verified: developer.android.com FGS types page, fetched 2026-11-16). dataSync also lists "local file processing" but has its own limits. WorkManager is for orchestration, not for unbounded long jobs.
6. **16 KB pages / Play:** every native lib (Tesseract, whisper.cpp, NCNN, FFmpeg if used, woff2) must be built 16 KB-aligned before **Feb 1 2027**; target API 36 from **Aug 31 2026**. Starting today with abiFilters arm64-v8a and 16 KB-aligned NDK builds avoids rework.

---

## 5. Recommended Android MVP: about 40 formats

**Selection rule:** (a) the conversion is achievable with permissive on-device components; (b) it covers the everyday task list of the target user (share, convert, save, offline); (c) each format has a real in-to-out edge, not a marketing row. **Not** "as many formats as Filee".

### 5.1 The 40 formats

| # | Ext | Family | MVP direction | Android engine | Risk |
|---|---|---|---|---|---|
| 1 | jpg/jpeg | image | decode + encode | BitmapFactory/Bitmap.compress | low |
| 2 | png | image | decode + encode | same | low |
| 3 | webp | image | decode + encode (lossless API 30+) | same | low |
| 4 | bmp | image | decode (platform) + encode (own writer) | BitmapFactory + own | low |
| 5 | gif | image | decode (animated, ImageDecoder) + encode (own LZW) | ImageDecoder + own encoder | medium |
| 6 | heic | image | decode + encode (device codec) | MediaCodec/HeifWriter + ImageDecoder | medium |
| 7 | avif | image | decode (12+) + encode (14+) | MediaCodec + ImageDecoder | medium |
| 8 | svg | vector | render to png; to pdf (vector) | AndroidSVG + PdfDocument | medium |
| 9 | pdf | pdf | render to png/jpg; text to txt; merge/split; images to pdf | PdfRenderer + PdfBox-Android + PdfDocument | medium |
| 10 | txt | text | decode/encode; to pdf | own + PdfDocument/StaticLayout | low |
| 11 | md | markup | to html, txt, pdf | own/CommonMark + renderer | low |
| 12 | html | markup | to txt, md, pdf (subset) | own parser + renderer | medium |
| 13 | docx | office | read to txt, html (to pdf phase 2, honest "preview") | own ZIP+XML | medium |
| 14 | xlsx | office | read to csv, tsv, json (write phase 2) | own ZIP+XML | medium |
| 15 | csv | data/office | to tsv, json; xlsx (write phase 2); to pdf table | own | low |
| 16 | tsv | data | to csv | own | low |
| 17 | pptx | office | read to txt (phase 2) | own ZIP+XML | medium/low |
| 18 | mp3 | audio | decode only (**no encode**) | MediaCodec | medium (gap) |
| 19 | m4a/aac | audio | decode + encode | MediaCodec + Media3/AacMuxer | low |
| 20 | wav | audio | decode + encode | own writer + MediaCodec | low |
| 21 | flac | audio | decode + encode | MediaCodec (encode 4.1+); own container if no muxer | medium |
| 22 | ogg | audio | decode Vorbis; encode Opus (10+) | MediaCodec + Media3/OggMuxer | medium |
| 23 | opus | audio | decode + encode (10+) | MediaCodec + Media3/OggMuxer | medium |
| 24 | mp4 | video | read/transmux/transcode/trim/extract audio | Media3 Transformer + MediaExtractor/Muxer | medium |
| 25 | webm | video | decode; encode if VP8/VP9 encoder present | Media3/MediaCodec | medium |
| 26 | mov | video | read to mp4 | MediaExtractor/Media3 | medium |
| 27 | mkv | video | read to mp4 (extractor support to verify) | MediaExtractor/Media3 | medium/UNVERIFIED |
| 28 | zip | archive | read/write | Commons Compress | low |
| 29 | 7z | archive | read only | Commons Compress SevenZFile | medium (read-only) |
| 30 | tar | archive | read/write (plus gz/xz variants) | Commons Compress | low |
| 31 | gz | archive | read/write | Commons Compress Gzip | low |
| 32 | xz | archive | read/write | Commons Compress XZ | low |
| 33 | json | data | to yaml/toml/xml/csv (table) | kotlinx.serialization | low |
| 34 | yaml | data | to json/toml/xml/csv | SnakeYAML | low |
| 35 | xml | data | to json/yaml/csv | XmlPullParser | low |
| 36 | toml | data | to json/yaml/xml | TomlJ or own | low |
| 37 | srt | subtitle | to vtt, ass; to txt | own | low |
| 38 | vtt | subtitle | to srt, ass | own | low |
| 39 | ass | subtitle | to srt, vtt (lossy styles) | own | low |
| 40 | epub | ebook | read to txt, html (phase 2) | own ZIP+XHTML | low/medium |

Total: **40 extensions across 12 families**, plus **out-of-format operations** that do not add formats: merge PDF, split PDF, images to PDF, PDF to images, extract audio, trim video, resize/compress image, strip metadata, zip/unzip, batch queue, presets.

### 5.2 The MVP edges (what "support" means)

| Conversion edge | Family | Engine | Risk / note |
|---|---|---|---|
| jpg/png/webp/bmp/gif/heic/avif to jpg, png, webp | image | Bitmap pipeline | HEIC/AVIF encode device-dependent; animated gif gives first frame unless own encoder used |
| jpg/png/webp/bmp/gif/heic/avif to pdf | image to pdf | PdfDocument (or PDFBox for CMYK/quality) | one page per image; multi-image merge |
| pdf to png, jpg | pdf | PdfRenderer | per page; render scale/DPI choice |
| pdf to txt | pdf | getTextContents (API 35+) or PdfBox-Android | layout order best-effort |
| pdf merge / split / rotate / n-up | pdf | PdfBox-Android | no re-render |
| svg to png | vector | AndroidSVG | raster at chosen size |
| svg to pdf | vector | AndroidSVG into PdfDocument canvas | vector output (no filters) |
| txt/md/html to pdf | text | own renderer + PdfDocument + StaticLayout shaping | Arabic-safe via Android text stack |
| md to html, html to txt, md to txt | markup | own/CommonMark | subset |
| docx to txt, html, md | office | own ZIP+XML | structure flattened; no images in md v1 |
| xlsx to csv, tsv, json | office | own ZIP+XML | one file per sheet or first sheet (decide) |
| csv/tsv to xlsx | office | own OOXML writer (phase 2) | real spreadsheet cell types |
| csv, tsv, json (tabular) to each other | data | own | header/typed rules |
| json to yaml to toml to xml | data | libs | lossy where models differ |
| pptx to txt | office | own ZIP+XML | phase 2 |
| mp3/m4a/aac/wav/flac/ogg/opus to playable set | audio | MediaCodec + muxers | **no mp3 encode**; FLAC container writer if needed |
| video (mp4/mov/mkv/webm) to mp4 (H.264/AAC) | video | Media3 Transformer | transcode/trim; device encoder limits |
| video to m4a/wav | audio extract | MediaExtractor + MediaCodec | no re-encode of video |
| video to gif | image | own GIF encoder from frames | phase 2 (size/time) |
| archive to folder; folder to zip/tar/tar.gz/xz | archive | Commons Compress | streaming; zip-slip guard |
| 7z to folder | archive | Commons Compress read | read-only |
| srt to vtt to ass | subtitle | own | ASS styling lost going to srt |
| epub to txt/html | ebook | own | phase 2 |

### 5.3 Deliberately excluded from MVP (and why)

| Excluded | Reason |
|---|---|
| Office to PDF with layout fidelity (DOCX/XLSX/PPTX) | No on-device layout engine; would be a false claim. Offer "render preview" only if we build a real renderer, and label it. |
| PDF to DOCX/XLSX/PPTX/EPUB | Requires document reconstruction + layout models; desktop-class work. |
| MOBI/AZW3/KEPUB/CBZ/PDF to CBZ | E-book engine scope; no permissive Android engine at Filee's level. |
| TIFF/JXL/PSD/XCF/PDN/KRA/RAW/EXR/HDR/DICOM/FITS/VTF/ICO/ICNS/ANI | Desktop codec breadth; each is a project. TIFF/PSD/RAW are the only later candidates. |
| DWG/DXF, G-code, 3D conversions, geo/Shapefile, fonts | Niche for the target user, heavy engines; later if data justifies. |
| MP3 encode, APE/WavPack/DTS/DSD/ALAC | Platform codecs absent; needs LAME/FFmpeg (license/build decision). |
| ISO/DMG/CAB/RAR creation | No permissive writers; archive creation scope is zip/tar/gz/xz. |
| PDF/A, CMS signatures, e-mail MSG to EML | No lawful/practical on-device path (Ghostscript AGPL; PKCS#7 + MIME engine work). |
| Full Whisper/Translate/Upscale/OCR in base app | Correctly treated as **downloadable packs** post-MVP; OCR is the strongest first pack (Arabic tessdata). |

### 5.4 Packaging sketch (**ESTIMATE**, needs android-stack verification)

| Pack | Content | Estimated download |
|---|---|---|
| Base APK/AAB | Compose UI, image, PDF (PdfBox), archive (Commons Compress), data, subtitle, docx/xlsx readers, Media3 | ~25-35 MB plus per-ABI libs (**ESTIMATE**) |
| OCR pack (AR+EN) | Tesseract4Android native + tessdata_fast ara+eng | ~15-25 MB |
| Speech pack | whisper.cpp lib + quantized model (tiny/base) | ~75-150 MB |
| Upscale pack | NCNN lib + Real-ESRGAN models | ~50-100 MB |
| Translate pack | small MT model (per option) | ~100 MB-1 GB depending option |

The pack model mirrors Filee's optional-engine design (download on demand, verify hash, install/remove) without copying its implementation.

### 5.5 Android-specific capability decisions the MVP forces

1. **Where does output go?** SAF ACTION_CREATE_DOCUMENT per file, or a user-picked tree (ACTION_OPEN_DOCUMENT_TREE) for batch; app-private cache for intermediates; MediaStore insert for images/video/audio/Downloads. **No arbitrary writes outside SAF/MediaStore.**
2. **How does a file arrive?** Share sheet (ACTION_SEND, ACTION_SEND_MULTIPLE, ACTION_VIEW), plus in-app picker (Photo Picker or ACTION_OPEN_DOCUMENT). "Convert with (app)" must be registered for our formats; SEND_MULTIPLE batch limit varies by sender (**UNVERIFIED** practical cap).
3. **What runs in background?** Foreground service with mediaProcessing type for media; dataSync/WorkManager for short work; always user-visible progress; stop at completion; implement onTimeout.
4. **What happens on process death / cancel?** Atomic write (temp then rename), reserved names, delete only own temp, resume from a job record (Room/DataStore) where possible.

---

## 6. Desktop donut to Android UX translation

### 6.1 What the donut actually is (decompose before translating)

The desktop donut bundles **three mechanisms** that must be translated separately ([A:32-42](filee/docs_ARCHITECTURE.md#L32-L42), [A:147-213](filee/docs_ARCHITECTURE.md#L147-L213)):

1. **Ingress**: a gesture on a file from another app (Explorer/Finder/browser) opens the menu at the pointer, before the app knows the files.
2. **Picker**: a radial target list filtered to valid outputs for the dropped files, with presets hiding engine/quality decisions.
3. **Queue/feedback**: drop starts the job; progress and results appear without leaving the user's work.

### 6.2 Mechanism-by-mechanism translation

| Filee mechanism | Android reality | Android equivalent we should build | Verdict |
|---|---|---|---|
| Global mouse/keyboard hook (SharpHook) opens donut on modifier+drag | Android has no global pointer/drag hook for third-party apps; global gestures belong to the OS/launcher | **Share sheet target** for supported MIME types + **in-app picker**; "Convert with" chooser | **Translate via OS ingress**, not a clone |
| Transparent topmost window that does not take focus during another app's drag | A share target receives URIs in its own activity; no cross-app drag session | ACTION_SEND/ACTION_VIEW to a convert activity that shows the radial picker immediately | Same job, different host |
| Cursor-anchored radial menu; "drop on slice" converts | Touch targets are fingers; a menu at the touched point is possible; a share target has no pointer location | **Radial/fan picker in Compose** filling the lower half; single tap converts; hub/long-press for options | Same idea, feasible |
| Two faces (front convert / back edit) via Tab/wheel | No Tab/wheel; add an explicit front/back switch (segmented control or swipe on the ring) | Ring with at most 6-8 slices plus a "more" ring; back face for edit/tools | Adapt, do not copy |
| Groups of at most 6, no nesting | Same idea works, but deep nesting is bad on small screens | One level of grouping max; overflow goes to a list sheet | Adapt |
| Drop is reported as Copy so Explorer keeps the source | Android share grants read access; we must copy to app cache before processing (SAF URIs are not always seekable) | Copy in, convert, save out via SAF; never "move" the user's original unless explicit | Must copy |
| Explorer/Finder context menu ("Convert with Filee") | Android share sheet does this job; long-press in Files offers "Open with"/"Share" | Register ACTION_SEND/ACTION_VIEW and ACTION_SEND_MULTIPLE | Translate |
| Watch folders on desktop paths | Scoped storage forbids arbitrary watching; SAF tree watching is coarse/unreliable (no reliable inotify semantics) | Optional "watch a folder I picked" via DocumentFile polling + WorkManager (battery cost, unreliable) | **Do not build v1** |
| Browser drag (virtual files/downloads/links) | Android share sheet can receive links (text/plain URLs) and files | Accept URL text: download to cache (with user consent) then convert; no "drag from browser" gesture | Partial only |
| Toast window + Recent conversions | Standard Android notifications + in-app history | Foreground-service notification with progress + result notification; in-app history list | Translate |
| Presets gallery (Filee server, CC0) | A Play-distributed app can host its own catalog; no server needed in v1 | Ship built-in presets; gallery later | Defer |
| Filee Connect (LAN phone to PC, QR) | Android could be the **server** or the **client**; useful for moving files between phone and PC offline | Optional future feature: local WebSocket/HTTP over Wi-Fi + QR (the idea, our own protocol/UI) | Later, not MVP |

### 6.3 What does **not** transfer, and why (do not budget for it)

| Desktop feature | Why it cannot transfer |
|---|---|
| Global mouse/keyboard hook and cursor donut | No global pointer hook; the OS owns drag; apps cannot overlay input across apps |
| Explorer/Finder integration | No desktop shell; the share sheet is the equivalent surface |
| Watch folders on arbitrary paths | Scoped storage + SAF; no inotify on shared storage; background limits |
| Tray app, portable mode, multi-window donut placement math | Desktop window model; irrelevant |
| Bundling LibreOffice/Pandoc/Ghostscript/calibre/FFmpeg binaries | Not buildable/lawful on Android (sections 4 and Appendix A) |
| "Drag from browser" gesture | No cross-app drag on Android; URLs arrive via share |
| Filee Connect's desktop Kestrel server | Only the *idea* (LAN transfer) transfers; implementation does not |
| The exact donut visual/geometry/ring animation | Filee's UI expression; copy the *idea* of a radial picker, not the design |

### 6.4 Proposed Android flows (v1)

**Flow A, share then convert (primary):**
Files/Photos/Gmail/WhatsApp, Share, "Convert with (app)", [copy to cache, detect format], radial picker (valid outputs only), tap, progress notification, [SAF save dialog or auto-save to chosen folder], result notification and in-app history.

**Flow B, in-app:**
App, "+" (Open/Photo picker, multi-select), batch list, picker per file or "convert all to X", queue screen with per-file progress, save all to a picked folder.

**Flow C, "convert to PDF" everywhere (high value, low risk):**
Images, text, HTML to PDF; PDF to image. This is the everyday case in this market (ID copies, documents, homework) and needs no exotic codecs.

**UI requirements specific to this market:**

* **Arabic/RTL**: full RTL layout, Arabic labels, Arabic shaping in PDF output (Android text stack handles it), Arabic numerals option; OCR pack Arabic+English first.
* **Low-end devices**: lightweight ring animation, respect reduce-animations, list picker fallback (TalkBack: radial menus are hard; provide a semantic list).
* **One-hand reach**: ring/fan in the lower half; large touch targets.
* **No account, no network for base conversions**; packs download only on explicit action, with license/attribution shown.

### 6.5 Compose radial picker: implementation notes (feasible, no Filee code)

* Draw with Canvas + drawArc slices; hit-test by angle/radius (pure Kotlin, unit-testable like DonutGeometry). The center hub shows the target and a back/more control.
* Filter slices by the planner (FormatRegistry/RoutePlanner): the UI asks the planner, never hardcodes edges.
* Animate opening with a short spring; respect system animator scale and accessibility.
* Provide the same choices as a **list sheet** for accessibility and for more than 8 outputs.

---

## 7. 'Asl and 'Illa: material for the Qiyas Council

> This section is the Expert's draft takhrij al-manat: separate the **effective cause ('illa)** of Filee's success from its **incidental attributes (awsaf)** and from its **form ('arad)**. It states facts and hypotheses; the ruling belongs to the Council.

### 7.1 Al-'asl (the established case)

**Observed:** a free MIT desktop converter, offline, about 270 formats, one-gesture radial UI, no account, optional downloadable engines, local AI (Ollama/Whisper/Tesseract). **Premise assumed:** it achieved meaningful adoption among desktop users. *Evidence gap:* no download or user numbers were verified in this task, so the success premise is **UNVERIFIED**. The Council should not treat "success" as measured fact until market-analyst supplies numbers.

### 7.2 Candidate 'ilal (what actually caused the value)

| # | Candidate cause | Evidence in Filee | Strength |
|---|---|---|---|
| C1 | **Local-only processing, so trust** (files never uploaded; no account) | all engines local; downloads hash-pinned; AI via loopback/offline; Connect is LAN-only ([E:106-149](filee/docs_ENGINES.md#L106-L149), [E:2403-2428](filee/docs_ENGINES.md#L2403-L2428), [A:840-847](filee/docs_ARCHITECTURE.md#L840-L847)) | **Strongest** proximate trust cause |
| C2 | **Coverage without Office/paid tools** (one app replaces a toolbox; no MS Office needed for many office-to-PDF routes) | built-in readers + HWPX/DOCX writers + rhwp renderer ([E:19-20](filee/docs_ENGINES.md#L19-L20), [E:48-50](filee/docs_ENGINES.md#L48-L50)) | **Strong**; this is the retention cause |
| C3 | **Zero-decision conversion** (app plans route/engine; user picks only the target) | RoutePlanner + presets + engine priority ([A:43-48](filee/docs_ARCHITECTURE.md#L43-L48), [E:3-4](filee/docs_ENGINES.md#L3-L4)) | **Strong**; explains "it just works" |
| C4 | **One-gesture ingress at the point of work** (drag from Explorer/browser) | global hook + donut ([A:32-42](filee/docs_ARCHITECTURE.md#L32-L42), [A:83-140](filee/docs_ARCHITECTURE.md#L83-L140)) | Medium as *cause*; high as *differentiator/marketing* |
| C5 | **Free, no account, no ads/subscription** | MIT, no account, no cloud | Medium; removes adoption friction, not unique |
| C6 | **Safety-shaped side effects** (never overwrite; trash; reserved names; own-path cleanup) | [A:49-65](filee/docs_ARCHITECTURE.md#L49-L65) | Medium; reduces fear of using it on important files |
| C7 | **Desktop breadth via bundled binaries** (FFmpeg/LibreOffice/Pandoc/calibre/Ghostscript) | [E:37-45](filee/docs_ENGINES.md#L37-L45) | **This is the mechanism of C2 on desktop, and it does not transfer** |

**My reading (HYPOTHESIS, for the Council):** the operative 'illa is **C1 + C2 + C3 jointly**: the user's private files can be converted, comprehensively and without decisions, without leaving the device. Each alone is replaceable by existing apps (cloud converters, one-format apps, batch tools); the combination is what Filee uniquely offers. **C4 (the donut) is the form that makes the combination discoverable and delightful, not the cause itself.** C7 is the desktop-specific means (sabab) by which C2 was obtained, and that is exactly where Android fails the analogy.

### 7.3 Essential vs incidental (what may and may not be carried)

| Element | Status for qiyas | Transfer to Android? |
|---|---|---|
| Local-only, zero-upload | **'illa element (C1)** | **Yes, mandatory** (it is the trust claim) |
| Broad everyday coverage (with honest limits) | **'illa element (C2)** | **Yes, but a smaller set; honesty required** |
| Automatic engine/route selection | **'illa element (C3)** | **Yes: planner in app; user picks target only** |
| Free, no account, no ads | supporting (C5) | Yes (business model decision is Lead's) |
| Never overwrite / trash semantics | supporting (C6) | Yes (SAF + copy workflow) |
| Radial menu *idea* | form ('arad) | Yes, as an original Compose implementation |
| Filee's exact ring geometry, colors, icons, animations, preset names | Filee's expression/brand | **No** |
| Global gesture hook, Explorer integration, watch folders, Connect server, portable mode | desktop-only mechanism | **No** |
| Bundled GPL/AGPL engines | license-incompatible with a closed app | **No** (Appendix A) |
| Filee's field names/grades taxonomy verbatim | curated work product | **No: write our own** |

### 7.4 Conditions (shurut) for a valid qiyas to Android

1. **Same need:** the target Android user must have the same friction (private files + many formats + no trustworthy local tool). Evidence needed for Iraq: market-analyst's dossier.
2. **Same trust premise:** the app must be **verifiably offline** for base conversions: no upload of file content, no hidden network on convert, minimal permissions; packs downloaded only on demand. If telemetry touches filenames, C1 is broken.
3. **Coverage threshold:** the app must cover the user's everyday tasks so they do not keep another converter. **Proposed measurable condition:** at least 80% of the top-20 conversion tasks from the market dossier, offline, with honest "not supported" messaging for the rest.
4. **Fidelity honesty:** where output is approximate (docx to pdf preview, ass to srt styling, heic encode device-dependent), the app must say so before conversion. Otherwise C2 becomes deception (khilaf al-sidq).
5. **Lawful means:** no GPL/AGPL bundling in the closed app; commercial-friendly model licenses; UnRAR/7-Zip restrictions reviewed; Play policy compliance (permissions, data safety, target API).
6. **Arabic-first quality:** shaping, RTL, Arabic OCR first; otherwise the app excludes its own market and the analogy to Filee's Korean-first polish fails.

### 7.5 What must NOT be copied (for legal-clearance to confirm)

* **Name/brand/logo/UI identity:** avoid confusion with Filee; MIT covers code, not trademarks or visual identity.
* **Source code, comments, docs text, preset data files, taxonomy verbatim, icons, sounds:** even where MIT allows reuse with notice, our project rule is original implementation; copying the donut's exact look would also be a market-confusion risk.
* **Its bundled engines:** they are third-party works under their own licenses (GPL/AGPL/MPL among them, [N](filee/THIRD-PARTY-NOTICES.md)); we choose Android-appropriate permissive components instead.
* **Its installation/updater/connect mechanisms:** unnecessary and OS-bound.

### 7.6 Hypotheses to test cheaply (before building the whole app)

| # | Hypothesis | Cheap test |
|---|---|---|
| H1 | Users adopt primarily for **privacy/offline**, not format count | landing page or share-target A/B copy; 10 interviews |
| H2 | Users adopt because **one app replaces several tools** | task-list survey; measure which formats are actually used |
| H3 | The **radial picker** improves speed/joy enough to matter on touch | prototype Compose ring vs list, timed task test with 10 users |
| H4 | **Share-sheet ingress** is enough; no need for a "watch folder" | track share vs in-app ratio in beta |
| H5 | **Arabic OCR + Arabic documents** are the wedge use case | beta cohort tasks (ID/PDF/photo to PDF/text) |
| H6 | Users will download packs (OCR/speech) if explained | pack download conversion in beta |

### 7.7 Council-ready summary

* **Al-'asl:** Filee = local, comprehensive, zero-decision conversion on desktop, delivered through a one-gesture radial menu.
* **Al-'illa (draft):** removing the trust barrier and the coverage gap at the moment of need, while asking nothing from the user (C1+C2+C3).
* **Al-'arad:** the donut/gesture, the desktop shell integrations, the bundled engine set.
* **Al-fariq (the difference that may break the qiyas):** Android cannot bundle the desktop engines, so coverage (C2) must be rebuilt by different means and will be narrower; no global gesture, so ingress must be the OS share sheet; SAF replaces the desktop file path. **If the narrower coverage falls below the user's everyday threshold, the 'illa is not instantiated** and the app becomes "one more partial converter". The testable threshold is section 7.4(3).
* **Hukm shape (for the Council, not mine to issue):** the *activity* (local conversion software, original code, permissive licenses, no deception) is permissible in principle; the *means* must be lawful (licenses, no brand copying, honest claims). The *market* qiyas (that it will succeed because Filee succeeded) is not established by the analogy alone; it needs the threshold tests above.

---

## 8. Top 10 risks of doing this on Android

| # | Risk | Why it bites here | Evidence | Mitigation / MVP stance | Severity |
|---|---|---|---|---|---|
| 1 | **Coverage gap vs user expectation** ("a converter app converts everything") | Android cannot reproduce Filee's desktop binaries; a 40-format app can feel "broken" next to ad-filled 200-format apps | Section 4; [E:37-45](filee/docs_ENGINES.md#L37-L45) | Honest scope + share-target positioning ("convert this file now", not "all formats"); roadmap packs; never advertise 270 | **High** |
| 2 | **Media codec/license wall** (no MP3 encode, no GIF encode, HEIC/AVIF device-dependent, no FFmpeg) | The most-requested conversions are audio/video; Filee's answer (FFmpeg) is GPL/retired on Android | Section 4.1(2); platform docs; FFmpegKitNext (LGPL-3.0 default, source-only) | MediaCodec/Media3 only in MVP; explicit "output support depends on device"; decide on LGPL FFmpeg with legal before promising mp3/avi | **High** |
| 3 | **Native lib compliance and packaging** (16 KB pages by 2027-02-01, target API 36 by 2026-08-31, ABIs) | Tesseract/whisper/NCNN/FFmpeg are native; a missed alignment blocks releases | <https://developer.android.com/guide/practices/page-sizes>, <https://developer.android.com/google/play/requirements/target-sdk> | Build NDK libs 16 KB-aligned, arm64-v8a (plus optionally armeabi-v7a) from day one; CI check | **High** |
| 4 | **Storage/SAF friction** (no arbitrary writes; URI permissions expire; copy-in cost for large files) | Users expect "save next to the original"; per-file SAF dialogs annoy; large videos need cache space | SAF docs (developer.android.com); ACTION_OPEN_DOCUMENT_TREE | Batch save to one tree; persistable grants; stream + temp file; free-space check before video jobs | **Medium-High** |
| 5 | **Background execution limits** (mediaProcessing FGS 6 h per 24 h; process death; OEM killers) | Long video conversions and big PDFs exceed limits, especially on mid-range phones | FGS types page (fetched 2026-11-16) | mediaProcessing FGS with progress and stop at end; resumable job records; cap per-job size; warn on low battery/thermal | **Medium-High** |
| 6 | **Memory/thermal on mid-range devices** (large PDFs, 4K video, OCR on big scans) | OOM kills the FGS and loses work; heat throttles | platform behavior; device matrix **UNVERIFIED** | Stream page-by-page, downscale, tile rendering, limit OCR resolution, per-job timeouts, test on 2-4 GB RAM devices | **Medium-High** |
| 7 | **False office fidelity expectations** (DOCX/XLSX/PPTX to PDF) | Users will compare with Word; a preview differs; reviews punish | Section 4 (NEVER fidelity print) | Do not ship office-to-PDF until a real renderer; label "text extraction" vs "render"; collect requests | **Medium** |
| 8 | **Legal/licensing mistakes** (GPL/AGPL in a closed app; UnRAR/7-Zip restrictions; model licenses; Play Data Safety) | One GPL/AGPL lib or a false offline claim can sink release and trust | [N](filee/THIRD-PARTY-NOTICES.md); unrar license; Play Data safety | License gate in build (allowlist Apache/BSD/MIT/LGPL-with-approval); review data-safety and offline claims; attribution screen | **Medium-High** |
| 9 | **Pack UX and size** (OCR/speech/upscale downloads, storage, license display) | Packs are the Android answer to optional engines but users on metered data dislike big downloads; Play size limits | Filee's optional-engine pattern [E:106-149](filee/docs_ENGINES.md#L106-L149) | Download on Wi-Fi by default, show size+license before download, allow removal, hash-verify, offline import from file | **Medium** |
| 10 | **Market/trust trap** (Play converter category is ad-ridden and distrusted; "offline" claims must be provable) | If the app shows any ad/upload/telemetry surprise, C1 collapses and the product loses its only moat | C1 reading; market data **UNVERIFIED** | No ads v1, no account, no analytics on file content, transparent permissions; let market-analyst validate | **Medium** |

*(Risks 1-3 can kill the project; 7 and 10 can kill trust; 4-6 generate 1-star reviews if ignored.)*

---

## Appendix A. License constraints that decide the Android stack

Filee's notices show which licenses are in play on desktop ([N](filee/THIRD-PARTY-NOTICES.md)); the table records the ones that shape our Android choices.

| Component (Filee use) | License | Can a closed commercial Android app bundle it? | Android alternative |
|---|---|---|---|
| Filee itself | MIT | n/a (we copy nothing) | original Kotlin |
| FFmpeg 9.0.2 full build (gyan.dev) | **GPL-3.0-or-later** (--enable-gpl --enable-version3); FFmpeg core LGPL-2.1+ | **No** for the GPL build; LGPL FFmpeg only with relinkable dynamic-link compliance | MediaCodec/Media3 (clean); or self-built LGPL FFmpeg .so with legal sign-off |
| FFmpegKit (retired) / FFmpegKitNext | LGPL-3.0 default, GPL-3.0 with GPL libs; Next is source-only with no Maven packages | Only with LGPL-3.0 compliance review | Media3 first |
| LibreOffice + H2Orestart + Java | MPL-2.0 (LO); H2Orestart GPL-3.0 | Desktop-only; **no Android port** | none |
| Pandoc | GPL-2.0+ | No | own markdown/HTML subset |
| Ghostscript | **AGPL-3.0** | **No** | drop PDF/A and PS/EPS |
| calibre | GPL-3.0 | No | EPUB read-only own code |
| 7-Zip | LGPL-2.1+ **with unRAR restriction** | Review; restrictions on RAR | Commons Compress (7z read-only) |
| UnRAR / junrar | UnRAR license (extract only; cannot build a RAR archiver) | Needs clearance | read-only RAR extraction, or skip |
| Tesseract 5.4 + tessdata_fast | Apache-2.0 | **Yes** (Tesseract4Android, tessdata_fast) | ship as OCR pack |
| whisper.cpp + ggml-base model | MIT | **Yes** | speech pack |
| upscayl-bin | **AGPL-3.0** | No | NCNN (BSD-3) + Real-ESRGAN (BSD-3) / NMKD (WTFPL) / LSDIR (CC-BY-4.0) |
| WeasyPrint | BSD-3 | n/a (desktop Python) | own HTML subset renderer |
| SharpHook / libuiohook | LGPL-3.0 | n/a (global hook impossible) | share sheet |
| PDFsharp (Filee) | MIT (PDFsharp 6); exact bundled version **UNVERIFIED** | n/a | PdfBox-Android (Apache-2.0) / PdfDocument |
| ImageMagick/Magick.NET (Filee) | ImageMagick license (Apache-2.0-compatible); exact **UNVERIFIED** | n/a | BitmapFactory/ImageDecoder + own codecs |

**Rule of thumb for the Android app:** ship only MIT/Apache-2.0/BSD-2/BSD-3/WTFPL/CC-BY (with attribution) components; treat every LGPL and UnRAR dependency as a legal decision, and every GPL/AGPL as excluded.

---

## Appendix B. Claims ledger (fact vs estimate)

| Claim | Status |
|---|---|
| Filee is MIT, about 270 formats, desktop Avalonia/.NET, 2.5.0 newest changelog section | **Fact** (sources above); "2.5.0 is latest release today" **UNVERIFIED** |
| Filee adoption/success metrics | **UNVERIFIED** (no numbers in corpus) |
| Filee desktop coverage is enabled by bundled desktop binaries and optional engines | **Fact** ([E:37-45](filee/docs_ENGINES.md#L37-L45)) |
| Filee routing is Dijkstra, at most 3 hops, cost+priority, edge flags | **Fact** ([A:43-48](filee/docs_ARCHITECTURE.md#L43-L48)) |
| Android platform facts (target API 36 by 2026-08-31; 16 KB by 2027-02-01; PdfRenderer text API 35; codecs; Media3 muxers; FFmpegKit retirement; FGS mediaProcessing 6 h per 24 h) | **Fact**, fetched 2026-11-16 from the URLs in section 4 |
| Android library licenses (PdfBox-Android Apache-2.0; Commons Compress Apache-2.0; Tesseract4Android Apache-2.0; AndroidSVG Apache-2.0; whisper.cpp MIT; NCNN BSD-3; junrar UnRAR license) | **Fact** from each project's README/LICENSE fetched 2026-11-16 where noted; individual versions **UNVERIFIED** |
| MVP package-size estimates | **ESTIMATE** |
| Media3 FLAC muxer availability; Transformer remove-video/extract-audio exact API; MKV extractor depth; POI/docx4j viability on Android; ML Kit translation terms/Arabic; HTML-to-PDF via WebView PrintDocumentAdapter | **UNVERIFIED**, android-stack must verify before they enter the spec |
| Arabic OCR quality with tessdata_fast | **UNVERIFIED** (test with real Iraqi documents) |
| "Users adopt for privacy" and similar | **HYPOTHESIS** (section 7.6) |

---

## Appendix C. How this maps onto the existing project skeleton

The repo already contains same-named stubs (app/src/main/java/com/sigha/converter/core/FormatRegistry.kt, RoutePlanner.kt, Converter.kt, ConverterRegistry.kt). The Android design in sections 2 and 5 is intended to fit that shape: formats go to the registry; engines are Converter implementations with declared edges; UI queries the planner; queue goes to WorkManager/FGS. This file does not modify them.

## Appendix D. Open questions for the Lead and teammates

1. **Legal:** LGPL FFmpeg (for mp3/avi breadth) or platform-only media? Is RAR read (UnRAR license) acceptable? Is CC-BY model attribution acceptable in-app?
2. **Model-auditor:** Arabic-capable MT option that is commercial-friendly and small enough to download (ML Kit vs OPUS-MT vs small instruct model); Tesseract Arabic vs ML Kit Text Recognition for Arabic.
3. **Android-stack:** Media3 Transformer exact capabilities for our MVP edges (trim/transcode/extract audio/mkv input/webm output); PdfBox-Android footprint; 16 KB-aligned build plan; minSdk decision (suggests 26/28).
4. **Lead:** is the MVP scope "40 formats + share-target" approved, or should v1 be narrower ("images + PDF + archives + data + subtitles", no media)?
5. **Lead:** monetization stance (free/no-ads trust positioning vs pack purchases), which affects C1/C5.
