# Sigha — product definition

> One file, one target format, converted on the phone. No upload. No account. No ads.

**Status (2026-11-17):** pre-launch. Version `0.2.1` is a **closed-test build**; nothing has been
published to a store yet. The production-readiness pass and the 0.2.1 UI/UX fix pass are landed in
this tree, no device QA has been recorded, and the Arabic privacy policy awaits native review. This file is the one-page definition; the exhaustive capability list is
[`docs/product/PRODUCT.md`](docs/product/PRODUCT.md).

**Verification gate:** no sentence in this repository is a release claim until
`scripts/verify.ps1`, the JVM test suite and the device QA checklist pass on the exact commit
being shipped. Formats, counts and behaviour below were read from source on 2026-11-17; they are
re-checked by that script.

## 1. The problem

Android users who need a file in another format — a DOCX in a job application, a HEIC photo on a
non-Apple device, a lecture PDF as text, a video as audio — are pushed to upload it to an
ad-funded website. That means a private file leaves the phone, a large upload on a metered
connection, and a result that is unusable when the network is bad or absent. Phone-first markets
feel this hardest: the phone is the office, the connection is not guaranteed, and Arabic support
in mainstream converter apps stops at a machine-translated menu.

The repository already documents the demand this is aimed at qualitatively
(`docs/legal/clearance-memo.md`, `docs/plan/production-readiness.md`); **we have no install or
retention data yet**, so the market claims in this file are hypotheses with observable
predictions, not results.

## 2. Who it is for

| Priority | User | Job to be done |
|---|---|---|
| Primary | Arabic-speaking Android user in Iraq / MENA who treats the phone as their office | "Make this file usable now, without an account, on a weak connection." |
| Secondary | Privacy-conscious Android user anywhere | "Convert this without handing it to a website." |
| Secondary | Anyone who receives a file they cannot open (HEIC, DOCX, PDF on a cheap phone) | "Turn it into something this phone can show or share." |
| Explicitly not first | Office power users | Layout-faithful DOCX/XLSX round-trips are out of scope (see non-goals). |

## 3. The wedge: four outcomes

The product is not "many converters". The wedge is four **outcomes** a phone user actually wants.
None of the four ships in `0.2.0` or `0.2.1`; those releases ship the capability catalogue they will be built on.
See [ADR-0003](docs/adr/0003-product-wedge.md).

| # | Outcome | Why it is the job | Ships in 0.2.x? | Nearest capability today |
|---|---|---|---|---|
| 1 | Scanned pages → one clean PDF | Receipts, IDs, forms, study notes: the scan is the deliverable | **No (roadmap)** | Image → PDF, one image per PDF. No camera capture, no page assembly, no OCR into the PDF. |
| 2 | Many images → one PDF | A folder of photos that must be a single document | **No (roadmap)** | Each image becomes its own PDF; multi-file→one-PDF is not implemented. |
| 3 | PDF merge / split / compress | The daily PDF chores | **No (roadmap)** | PDF → PNG/JPG with a page range (split-by-export); PDF → TXT. No merge, no size compression. |
| 4 | Compress a photo / video to a shareable size | "It won't send on WhatsApp" | **No (roadmap)** | Re-encode with a quality slider (images) or bitrate choice (audio/video). No target-size mode. |

The order is a hypothesis: outcome 2 and 4 are the cheapest to make real and should be validated
first (see §7). Building a converter catalogue without an outcome is exactly the failure mode this
document exists to prevent.

## 4. What ships in 0.2.1

A local-file workbench: pick files with the system picker or share them in from another app; the
app offers only targets reachable from the selection; conversion runs on-device in a queue; results
land in `Download/Sigha` (or a folder the user picks); history keeps the last 50 entries.

- **48 file formats are recognised, 39 are convertible, 25 can be produced.** These are different
  numbers on purpose — see §5.
- Documents: DOCX, ODT, PPTX to text; DOCX and XLSX to PDF; XLSX to CSV/TSV, ODS to CSV; CSV to XLSX;
  TXT and Markdown to PDF; HTML to text.
- PDF: to PNG/JPG with a page range; to text, with OCR fallback for scans when the OCR pack is
  installed.
- Images: JPG, PNG, WebP, BMP, GIF, HEIC (Android 9+), AVIF (Android 12+) to JPG/PNG/WebP; any of
  them to PDF; quality 20–100.
- Media: audio formats to each other; video to MP4/WebM/MKV/GIF; audio extraction from video;
  bitrate choice.
- Data, subtitles, e-books, archives: CSV/TSV/JSON interchange, SRT/VTT, EPUB to text/HTML, ZIP and
  TAR.GZ repacking, 7Z to ZIP.
- OCR: optional English + Arabic pack, a 28,004,325-byte download the user starts explicitly.
  OCR-only targets stay hidden until the pack is installed; the workbench offers the install inline
  with the same size and metered-connection confirmation, and the targets appear when it finishes.
- Arabic and English UI (RTL), light/dark/system theme, no account, no ads, no analytics, no
  tracking SDK.

**Not in 0.2.x:** the four outcomes above, camera scanning, PDF merge/split/compress, image resize
or DPI control, target-size compression, RAR or 7Z creation, office layout fidelity, upscaling
(the ONNX model was removed; the upscale family is registered empty), cloud sync, in-app purchases.

## 5. The catalogue, honestly

- **Recognised (48):** the app knows the extension and MIME type. This is a claim about parsing,
  not about conversion.
- **Convertible (39):** at least one converter accepts the format as input.
- **Producible (25):** at least one converter can emit it as output.
- **Nine recognised formats have no working path today:** TIFF, RTF, TAR, BZ2, XZ, YAML, XML, MOBI,
  FB2. They are identified so the app can say *why* nothing can be done, not advertised as
  supported. They must never appear in store copy as supported formats, and "every format has a
  working edge" is a false claim that must not reappear in any document.

The catalogue is a conversion graph: 32 converters across 11 engine families (10 productive on their
own; the upscale family is empty), 172 directed edges over 171 distinct format pairs. Some pairs
have more than one converter (both PDF text extractors accept PDF → TXT); reachability, not the raw
edge count, is what the UI offers. The full matrix with per-family limits is in
[`docs/product/PRODUCT.md`](docs/product/PRODUCT.md).

## 6. Non-goals (v1)

Not for lack of ambition; each would change the product's shape or trust story:

- Office layout fidelity (DOCX/XLSX round-trips, HWP/HWPX, fonts, embedded images). Office support
  here is text-and-tables extraction, and the UI must say so.
- RAR and 7Z creation (licence-restricted or no permissive writer), ISO images.
- PDF editing, annotation, signing, forms, password removal.
- Cloud storage sync, accounts, subscriptions, in-app purchases in 0.2.x.
- Tablet, foldable and desktop layouts. 0.2.x targets phones: landscape is supported and must stay
  usable at 2400×1080 with 200% font scale, but multi-pane/rail layouts are out of scope.
- AI upscaling and image enhancement until a model ships and is verified on device.
- Background "watch a folder" automation.

## 7. Success metrics

Targets only; **all baselines are unmeasured** (no release, no installs). Sample-size caveats are
part of the metric.

| Area | Metric | Target | Instrumentation |
|---|---|---|---|
| Acquisition | Installs × day-1 retention | D1 ≥ 20% at ≥ 200 installs | Play Console + a consented, opt-out analytics decision (none in 0.2.x) |
| Habit | Day-7 retention | ≥ 8% at ≥ 200 installs | same |
| Wedge proof | Share of successful conversions that are document or compress outcomes | ≥ 35% (G7) | Local counters only, exported by the user; no automatic telemetry |
| OCR | Document users who install the pack; first-run failure rate | ≥ 20%; < 5% (G9) | Local counters + store reviews |
| Product quality | Crash-free sessions; conversion success on the device matrix | ≥ 99.5%; ≥ 95% | Play vitals; device QA checklist |
| Trust | Data collected/shared; verified false claims in listing | 0; 0 | Data safety form; claim audit before every release |
| Speed | Cold start; 12 MP JPG conversion | < 2 s; < 1.5 s | Device QA (not yet measured) |

If outcome 2 or 4 validates (a measurable share of users repeat the outcome), the roadmap is
re-ordered around it; if G7 stays flat with catalogue growth, the wedge is wrong and the product
should shrink, not add formats.

## 8. Claims discipline

- **What "offline" may say:** conversion runs on the device. The **only** network request is the
  OCR pack download the user starts by tapping Download; it sends no file and no user data.
- **What it may not say:** "fully offline", "nothing leaves your phone", "no permissions", "works on
  every device", "48 formats supported" (say 48 recognised / 39 convertible, or list examples), and
  any claim about a feature that does not ship today.
- Every store string, App Store/Play listing and in-app tagline must survive
  `scripts/verify.ps1` plus the manual audit in
  [`docs/product/store-listing.md`](docs/product/store-listing.md).
- The four permissions story is written down per permission in
  [`docs/product/permissions-rationale.md`](docs/product/permissions-rationale.md).

## 9. Roadmap (no dates; sequence only)

1. **0.2.0 — production-readiness**: honest docs and listing, foreground-service execution,
   metadata stripping, typed localized errors, release build rail, closed test.
   **0.2.1 — UI/UX fix pass** (landed in this tree, not store-published): OCR pack dead end,
   landscape empty state, Arabic completeness (localized notes, plural forms, Arabic privacy
   policy pending native review), single window/theme owner, accessibility semantics and contrast.
2. **0.3 — first two outcomes:** many images → one PDF; compress photo/video to a target size.
   These are small, testable, and don't need camera permission.
3. **0.4 — PDF chores:** merge, split, compress PDF.
4. **1.0 — scanning:** camera capture → page assembly → PDF, with the Arabic OCR pack integrated.
5. Then: upscaling only if a model is verified on-device; more formats only when an outcome needs
   them.

Features that do not serve an outcome are rejected by default.

## 10. Human-only blockers (not resolvable in code)

Recorded in `docs/plan/production-readiness.md` (B1–B7): Play Console upload key; Play developer
account and merchant/entity status for the publisher's country; privacy-policy hosting URL; Data
safety form and the 12-tester / 14-day closed test; trademark clearance for the name "Sigha";
LGPL Corresponding Source hosting for the FFmpeg fork; legal counsel sign-off on the clearance
memo. Device QA on physical phones is also human-dependent.

## 11. Document map

| Document | What it owns |
|---|---|
| [`docs/product/PRODUCT.md`](docs/product/PRODUCT.md) | Capability specification: per-family matrix, options, limits |
| [`docs/product/store-listing.md`](docs/product/store-listing.md) | AR/EN store copy, screenshots, Data safety answers, claims blocklist |
| [`docs/product/permissions-rationale.md`](docs/product/permissions-rationale.md) | Every permission, why, and denial behaviour |
| [`CHANGELOG.md`](CHANGELOG.md) | What changed in 0.2.0 and 0.2.1 |
| `docs/plan/production-readiness.md` | Workstreams, definition of done, human blockers |
| `docs/adr/` | Decisions: media engine, execution model, product wedge |
| `docs/qa/verification.md` | Measured build/test evidence (owned elsewhere; not duplicated here) |
| `README.md` | How to build and run |
