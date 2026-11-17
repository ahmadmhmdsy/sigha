# Sigha (صيغة) — Qiyas Council Review

**Merged mode C · adversarial council + code review · one language (English)**
**Date:** 2026-11-17 · **Project:** digital_converter workspace · **Build reviewed:** debug APK, versionCode 1 / 0.1.0, targetSdk 36
**Seats:** 7 (Lead + 5 teammates + Maestro) · **Evidence rule:** no finding without a file:line, a command output, or an explicit "UNVERIFIED".

---

## 0. One-page verdict

**Engineering core: real. Release discipline: absent. Claims: partly false. Product wedge: unproven.**

Sigha is a genuinely well-layered Kotlin/Compose converter with a correct uniform-cost router, real archive/XXE/OOM guards, a pinned-and-hashed model-pack downloader, and 131 unit tests that actually run. A cache-disabled clean build succeeds (44/44 tasks, 3 m 45 s) and produces a working debug APK. That is a strong base and it deserves to be said first.

But the artefact **cannot be shipped today**, and several published statements are **false or unreproducible**: the documented APK hash belongs to a stale artifact; "uncompressed, aligned native libs" is false (all 32 .so are compressed); "every format has a working edge" is false (9 of 48 have no edge at all); "32 edges from 12 engine families" is wrong on both numbers (32 converters, 172/171 edges, 11 families); "notices and relinking information are published" is false; ONNX Runtime is listed as shipped and is not in the build; the flagship scanned-PDF→TXT path fails 100 % of the time; the queue dies with the process; and no device QA has ever been run.

| Severity | Count | Meaning for the release |
|---|---|---|
| **Critical** | 5 | Blocks any public release or store listing |
| **High** | 14 | Must be fixed before open beta; most before closed test |
| **Medium** | 16 | Fix in the 90-day plan; some are user-visible defects |
| **Low** | 9 | Cleanup and hygiene |

**The single sentence:** *stop shipping claims and start shipping evidence — then finish the three outcomes (scan, merge/compress PDF, compress media) that this market actually opens the app for.*

---

## 1. Al-mas'ala — the question put to the council

Is Sigha, as it stands in this workspace, (a) a truthful and safe product, (b) a shippable Android artefact, (c) a defensible business for an Iraq-based, Arabic-first publisher? The council was asked to find what is right, what is wrong, what is missing, and how to improve — from engineering, security, policy, product, market and naive-user perspectives.

## 2. Al-asl — the standard we judged against

The ruling is only as good as its standard. Seven invariants were fixed before any evidence was read:

1. **Truth** — every published number, capability and licence statement is reproducible from this tree, or is marked unverified.
2. **Amanah over user files** — files never leave the device; no metadata leakage; temp data destroyed; no content logging.
3. **Safety** — hostile archives, OOXML/EPUB, PDFs and media must not crash, hang or exhaust the process.
4. **Durability** — a conversion the user started survives backgrounding, screen-off and process death, or its loss is explicit.
5. **Licence and policy lawfulness** — every shipped byte is attributable under its licence; Play metadata matches behaviour (the "deceptive behaviour" category is a policy risk, not rhetoric).
6. **Arabic-first** — not just RTL strings, but error paths, plural grammar, and PDF/Arabic rendering.
7. **Outcome value** — the formats exist to deliver jobs users actually open the app for (scan → PDF, images → one PDF, compress, merge, convert).

Anything that failed ≥2 of these was treated as a systemic issue, not a bug.

## 3. Al-manat — the verification ledger (tanqih)

The council's first duty was to verify the facts its own case rests on. Seat disagreements are adjudicated here.

| # | Claim (source) | Claimed | What was found | Ruling |
|---|---|---|---|---|
| 1 | Formats (README, PRODUCT) | 48 | 48 in core/FormatRegistry.kt — **but only 39 convertible, 25 producible, 9 dead** (tiff, rtf, tar, bz2, xz, yaml, xml, mobi, fb2) | **True number, false capability** |
| 2 | "32 converter edges" (README:32) | 32 | 32 **Converter implementations**; registry builds **172 directed edges / 171 distinct pairs** (same-format pairs skipped, core/ConverterRegistry.kt:14-19; pdf→txt duplicated) | **Wrong term and number** |
| 3 | "12 engine families" (verification.md:84) | 12 | **11** addAll calls (engine/BuiltInConverters.kt:24-36), 10 productive (upscale returns empty) | **False** |
| 4 | "Every format has a working edge" (PRODUCT.md:66) | all | 9 dead formats; a TIFF/RTF user gets "These files have no target in common" (strings.xml:65 workbench_unsupported exists but is unused) | **False** |
| 5 | 131 JVM tests green | 131 | Static count 131 @Test in 15 classes (Lead); forced execution: **131 pass, 0 fail, 0 skip** (build-verifier). No androidTest source set | **True, but shallow coverage** |
| 6 | APK "72,522,602 B / SHA-256 8922…8266" (README:20, verification.md:37-38) | current build | Matches only the stale saved copy .tmp_verify/app-debug-claimed.apk (with **2,952,891 B of internal zip dead space**); clean rebuild is **69,569,713 B / 75C0FC7B…108F** | **Stale / not reproducible** |
| 7 | "uncompressed, aligned native libs" (verification.md:46) | uncompressed | **0 stored / 32 deflated .so** in both APKs; android:extractNativeLibs="true" (AndroidManifest.xml:12) overrides useLegacyPackaging=false (app/build.gradle.kts:47) | **False** |
| 8 | "16 KB page aligned" (zipalign proof, verification.md:45) | proven | Conclusion **true**: ELF64 PT_LOAD p_align = 16384 for all 32 libs (verified independently by build-verifier and Lead). Cited zipalign -P 16 proof is **vacuous** on compressed entries | **Right conclusion, invalid evidence** |
| 9 | "Clean invocation" (verification.md:150) | clean | Documented command: **43 up-to-date, 41 s, test task UP-TO-DATE** — a no-op. Forced clean build: 44/44 executed, 3 m 45 s | **False as evidence** |
| 10 | Permissions (verification.md:48) | 4 | Merged manifest: **8** (+WAKE_LOCK, RECEIVE_BOOT_COMPLETED, FOREGROUND_SERVICE, DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION) | **Incomplete** |
| 11 | "Full EN/AR" | full | **165 EN / 165 AR keys, zero mismatch, supportsRtl** (Lead); but **≥162 hard-coded English error sites / 124 distinct messages** reach the UI, **0 plurals** in both locales, Arabic copy defects | **UI chrome true, end-to-end false** |
| 12 | "Nothing leaves your phone" (strings.xml:6, README:3) | nothing | Conversion is local; **one user-initiated HTTPS GET** to raw.githubusercontent.com for the **28,004,325 B** OCR pack (hash-pinned) | **True only with the in-app disclosure; store copy risks rejection** |
| 13 | "Notices and relinking information are published" (README:59) | published | NOTICE.md is a bare table; ui/LicensesScreen.kt:29-43 has no URLs; the only shipped res/raw/source.txt comes from the AAR and points to the **retired Arthenica wiki**; no LGPL text, no Corresponding Source, no written offer, no relink instructions | **False** |
| 14 | ONNX Runtime shipped (NOTICE.md:17, LicensesScreen.kt:35) | included | 0 grep hits, no libonnxruntime* in the APK | **False** |
| 15 | DOC is a catalogue entry (verification.md:127) | included | DOC is not in FormatRegistry; the MIME alias application/msword -> byExtension["doc"] is dead (core/FormatRegistry.kt:45-46) | **False** |
| 16 | Device QA | — | adb devices empty; an API-36 AVD **exists**; the QA doc itself says "not done" | **Honest, but the biggest residual risk** |
| 17 | Clean-room / brand (memo C1-C3) | respected | Filee appears only in **2 code comments**, no assets, no strings, no screenshots in APK | **True** |
| 18 | No ads/analytics/crash SDK | none | Dependency list clean; 0 Log/println in app/src | **True** |
| 19 | Legal memo C5/C8/C9/C10 (own release blockers) | met | C5 (LGPL duties) **unmet**; C8 (privacy policy/Data safety) **unmet**; C9 (offline test) **not run**; C10 partly met (metadata leakage) | **3 of 4 unmet** |

**Adjudicated seat disagreements**

- *16 KB:* app-auditor said "not established", policy-guardian said "zipalign passes". Both are right about their evidence; the property is true (ELF p_align), the documented proof is invalid, and the "uncompressed" half of the claim is false. Fix = remove extractNativeLibs, assert stored entries + p_align, correct the doc.
- *APK hash:* policy-guardian "matches exactly" (it does, for the saved file) vs app-auditor/build-verifier "not reproducible" (it is not, from this tree). Both true; the docs must distinguish "measured artifact" from "current build".
- *String count:* product-strategist reported 193/193; Lead counted **165/165** string entries in each file — 165 stands.
- *Hard-coded errors:* app-auditor reported 139; Lead's grep found **162 call sites / 124 distinct messages** — use "≈160".
- *Edges:* Lead's first graph pass computed 183; engine-auditor's registry reading (same-format dedupe) gives **172/171** — engine-auditor stands.

---

## 4. The seated council

| Seat | Arabic role | Mandate | Filled by |
|---|---|---|---|
| The Expert | الخبير | Product/format/fidelity domain truth; what a converter must do | product-strategist |
| The Engineer | المهندس | Correctness, data flow, safety, performance of engines and app | engine-auditor + app-auditor |
| The Objector | المعترض | Attack every number and every "verified" claim | build-verifier |
| The Policy Guardian | حارس السياسة | Licences, Play policy, privacy, trademark, halal red lines | policy-guardian |
| The Market | السوق | Demand, competitors, monetisation, distribution reality | product-strategist |
| The Naive User | المستخدم | First-run experience, trust, Arabic usability | Lead |
| The Maestro | المايسترو | Silent until the stop condition; rules on what survives | Lead |

Global invariants the council refused to trade: no uploads; no GPL/AGPL in the APK; no false store claim; Arabic-first means error paths too; no release without device QA; every number carries a command.

---

## 5. What we got right (verified, not flattering)

| Area | What is genuinely good | Evidence |
|---|---|---|
| Architecture | Clean direction core → engine → data → work → ui, one Converter interface, one wiring point per family; no God-object | engine/BuiltInConverters.kt, core/Converter.kt, package layout |
| Routing | Correct uniform-cost Dijkstra, hop cap, from == to short-circuit, unreachable → null; runner and picker share MAX_HOPS=3 | core/RoutePlanner.kt, data/ConversionRunner.kt |
| Archive safety | Archive conversion **never extracts**; zip-slip structurally impossible; entry/expansion caps; 7z memory cap; name normalisation rejects .., absolute, drive letter, NUL | engine/archive/ArchiveGuards.kt, ArchiveConverters.kt |
| XML safety | FEATURE_PROCESS_DOCDECL=false, so DOCTYPE is refused rather than expanded; lazy part guards and per-part byte caps | engine/office/OoxmlPackage.kt:194-204, SafePackage |
| Memory discipline | OOM mapped in image/PDF paths; bitmaps recycled in finally; PDFBox temp-file-only; partial outputs deleted on failure | engine/image/*, engine/pdf/* |
| Media hygiene | FFmpeg args are Array<String> (no shell); -y; input-side -ss/-to; return code + output existence checked; temp input deleted; stale cache pruned; no GPL encoders | engine/media/FfmpegArgs.kt, FfmpegRunner.kt |
| Model packs | HTTPS + commit-pinned URL + **SHA-256 before rename** + size check + disk-space check + redirect cap + atomic-ish install + no traversal | model/PackCatalog.kt, PackInstaller.kt:157-164 |
| Storage privacy | SAF-only, no MANAGE_EXTERNAL_STORAGE, no All-files, allowBackup=false, no analytics/ads/crash SDK, 0 Log calls, MediaStore IS_PENDING + unique names on API 29+ | manifest, data/OutputPublisher.kt |
| i18n basics | 165/165 string keys, supportsRtl, no hardcoded Text() literals in ui/, Arabic app label صيغة | strings.xml, values-ar/strings.xml |
| Tests | 131 real JVM tests (arg builders, parsers, guards, codecs) — they run and pass | app/src/test, build-verifier execution |
| Honesty where it counts | The QA doc itself admits device QA missing, scanned-PDF gap, OCR download-only, no RAR write, upscale disabled | docs/qa/verification.md §7 |
| Documentation | Product, legal, models, QA and council dossiers exist and are unusually thorough | docs/** |

**The council's note:** these are not consolation prizes. The archive/XXE/pack-download discipline is above the quality of most indie Android projects; it is the reason the fix list is 90 days and not a rewrite.

---

## 6. What is wrong — defect register

### 6.1 Critical (block any public release)

| ID | Defect | Evidence | Fix |
|---|---|---|---|
| **C1** | **No shippable artefact and no release process**: no git repo, no keystore/signingConfig, isMinifyEnabled=false, no ABI split/AAB, lint.abortOnError=false with a real lint **Error**, no CI, no project LICENSE | app/build.gradle.kts:22-27,50-53; git log → not a repository; .github absent | git init + commit; signing from keystore.properties; R8; AAB + per-ABI; lint gate on; one verification script |
| **C2** | **Licence compliance is asserted, not met**: LGPL-3.0 §4 duties (source offer, relink/installation info, licence texts) are absent; README says they are "published"; shipped attribution points at a retired wiki; ONNX phantom row; missing components | README:59, NOTICE.md:6-21, LicensesScreen.kt:29-43, AAR res/raw/source.txt | Ship assets/licenses/ (LGPL-3.0 + GPL-3.0 texts, AAR licences, fork LICENSE, written offer naming 8.1.9, "you may replace the .so" paragraph), render in-app, correct README; counsel sign-off (memo C5) or move to Media3 |
| **C3** | **Play policy exposure**: "Nothing leaves your phone" + declared INTERNET + a 28 MB download, with **no privacy policy anywhere** and no Data-safety answers; the memo's own C8/C9/C10 are release blockers and unmet | strings.xml:6, README:3, manifest, glob "privacy" = 0, memo §6.1 | Precise copy ("conversion runs on your device; one optional download"), host a privacy policy + link it in Settings/About, Data-safety = no data collected, disclose download host; run the network-off test |
| **C4** | **The flagship OCR path for PDFs always fails**: OCR pdf→txt is shadowed by PDFBox pdf→txt (registration order + strict < tie-break); a scanned PDF goes to PDFBox, which writes nothing, and the run ends as generic "converter returned no file" | engine/pdf/PdfConverters.kt:7-10 before engine/ocr/OcrConverters.kt:25-28; core/RoutePlanner.kt:30; data/ConversionRunner.kt:89,100-105; strings.xml:186 | Blank-text fallback inside PdfToTextConverter to ocr/PdfTextConverter (one file); then OCR target gating |
| **C5** | **Never run on a device; nothing device-dependent is verified**: SAF grants, MediaStore publishing, FFmpeg sessions, Tesseract init, backgrounding, API 26–28 legacy publish, RTL layout on hardware | docs/qa/verification.md:151-154; adb devices empty; no androidTest | Boot the existing API-36 AVD + one physical arm64 device; run the checklist; record evidence; do not claim "device-tested" before |

### 6.2 High (fix before open beta)

| ID | Defect | Evidence | Fix |
|---|---|---|---|
| **H1** | **Execution is not durable**: conversions run in an app-scoped coroutine; swipe/process death loses the batch silently; WorkManager is declared but has **0 usages**, yet ships WAKE_LOCK/RECEIVE_BOOT_COMPLETED/FOREGROUND_SERVICE + 3 services; POST_NOTIFICATIONS declared, no notification code | data/AppCoroutines.kt:11, work/ConversionQueue.kt:83, app/build.gradle.kts:74, merged manifest | Pick one model: CoroutineWorker + persisted job spec + progress notification, or FGS; align permissions; delete dead dependency |
| **H2** | **Silent data loss in route chains**: ConversionResult.note has 12 producers and **0 consumers**; intermediate multi-output steps keep outputs.first() (PDF→WebP converts page 1 only); PDF→TXT ignores options.pages while PDF→PNG honours it | core/Converter.kt:34, data/ConversionRunner.kt:88-108, engine/pdf/PdfToTextConverter.kt:91-116, PdfToImageConverter.kt:82-130 | Carry note to queue/history/UI; fan out suffix routes per output or forbid multi-output intermediates; honour page spans |
| **H3** | **Privacy leak in media conversion**: no -map_metadata -1 / -map_chapters -1, so GPS, device, author and creation metadata survive video→mp4/webm/mkv and video→audio | engine/media/FfmpegArgs.kt (0 grep hits) | Add both flags to every builder + assert in FfmpegArgsTest |
| **H4** | **Two reliability failures**: FFmpeg completion.await() has **no timeout** (a stuck native session freezes the sequential queue forever); the runner/queue catch Exception only, so an OOM in JSON/EPUB/OOXML/archive paths kills the process | engine/media/FfmpegRunner.kt:113, data/ConversionRunner.kt:130-145, work/ConversionQueue.kt:84-98 | withTimeout + FFmpegKit.cancel; catch Throwable at the runner boundary, rethrow CancellationException, map OOM to a failed item |
| **H5** | **Localisation fails on the paths users remember**: ≥162 hard-coded English messages reach Queue/History; 0 plurals (broken Arabic counts); Arabic copy defects (subtitle category says "الترجمة" = translation; download folder label mismatch) | engine/** messages → data/ConversionRunner.kt:92-98 → ui/QueueScreen.kt:234-240; strings.xml/values-ar | Typed failure codes resolved with R.string at the UI boundary; plurals; native Arabic review |
| **H6** | **9 dead formats presented as capabilities**: tiff, rtf, tar, bz2, xz, yaml, xml, mobi, fb2 have no edge; a single TIFF gets the misleading "no target in common" message; 17 strings are dead including the correct workbench_unsupported | core/FormatRegistry.kt, graph analysis, WorkbenchScreen.kt:354-357, strings.xml:65 | Hide or implement dead formats; state "48 recognised, 39 convertible"; use the unsupported message |
| **H7** | **OCR edges offered without the pack**: converters are registered unconditionally by design and the picker never consults ModelPacks, so a fresh install offers image/PDF→TXT and the first run fails with "Install the OCR pack" | engine/ocr/OcrConverters.kt:11-13, OcrSupport.kt:14, ui/WorkbenchViewModel.kt:244-260 | Grey out OCR targets with an install hint; keep registration |
| **H8** | **Packaging contradiction**: manifest extracts native libs while Gradle says "uncompressed"; every .so is deflated, so the 16 KB proof is vacuous (property itself true) | AndroidManifest.xml:12, app/build.gradle.kts:47, APK entries, build-final.log:50 | Remove extractNativeLibs, rebuild, assert **32 stored** entries + zipalign + ELF p_align; correct verification.md:45-46 |
| **H9** | **Evidence files are stale and self-contradictory**: build-final.log contradicts the current build file; verification.md:150 claims a clean run that was a no-op; APK hash/size stale; "12 families"; permission row 4 vs 8 | see ledger §3 | One evidence command (:app:clean … --no-build-cache), regenerate numbers from a clean build, delete stale logs/artifacts from the tree |
| **H10** | **Lint gate disabled hides a real error**: 58 issues including 1 **Error** (WrongConstant, engine/text/TextPdfWriter.kt:67), 3 TrustAllX509TrustManager inside transitive BouncyCastle via PDFBox, 3 plural candidates, 18 unused resources | build-verifier lint run | abortOnError=true, baseline, burn down; confirm the bcpkix finding is transitive-only |
| **H11** | **Product wedge mismatch**: no PDF merge/split/compress, no camera scan, image→PDF produces one PDF per image (10 photos = 10 PDFs), no WhatsApp target-size video preset; PRODUCT.md:22 claims "resize" that no UI sets; OOM messages tell users to use controls that do not exist | PRODUCT.md:22, ui/WorkbenchViewModel.kt:139-149, ImageToPdfConverter.kt, strings.xml OOM messages | Ship 4 outcome features before store push; either wire max-dimension/DPI or delete the claim and fix the messages |
| **H12** | **Monetisation is structurally blocked**: Play developer registration for Iraq is supported, **merchant registration is not**, so paid apps/IAP are unavailable to an Iraq-based publisher; no ads decided (correctly) | Google support table fetched 2026-11-17 (product-strategist) | Decide entity (foreign) or free + donations; keep no-ads invariant; do not price before this |
| **H13** | **Attribution is wrong and incomplete**: missing libleptonica (BSD-2), libjpeg (IJG), libpng, BouncyCastle, okhttp/okio, commons-io/codec/lang3, smart-exception-java (licence URL 404s), and 32 ffmpeg-kit externals (incl. libsndfile LGPL-2.1, openh264); META-INF/NOTICE.txt stripped for all deps | NOTICE.md, app/build.gradle.kts:42, AAR contents | Generate NOTICE from the resolved dependency graph; stop stripping NOTICE files; add a test that fails on an unlisted shipped dep |
| **H14** | **Supply chain is young and unverified**: dev.ffmpegkit-maintained:ffmpeg-kit-full:8.1.9 is a single-maintainer republish (POM: inceptionYear 2026) of the retired Arthenica line; no Gradle verification metadata; JitPack added globally though nothing needs it | POM, settings.gradle.kts:19 | Dependency verification + lockfile; remove JitPack; provenance note; migration plan to Media3 |

### 6.3 Medium (fix within 90 days)

| ID | Defect | Evidence | Fix |
|---|---|---|---|
| M1 | SAF persistable grants are never released — hits the 128-grant cap and old history entries stop reopening | IncomingFilesBus.kt:69-76, WorkbenchViewModel.kt:96-101 | Release on eviction/clear; re-pick on open failure |
| M2 | DataStore has no corruption handler; a corrupt history file fails the batch after files were published | HistoryStore.kt:16, SettingsStore.kt:39 | ReplaceFileCorruptionHandler; history writes non-fatal |
| M3 | Model-pack download is composition-scoped: back cancels 28 MB with no resume; specific errors swallowed by a generic snackbar; Compose state mutated off-main | ModelPacksScreen.kt:74-91, ModelPacks.kt:84-92 | App-scoped/Worker download; surface reason; StateFlow to main; confirm dialog with size + licence + metered warning; HTTP Range resume |
| M4 | Queue cap 60 silently drops rows: a 70-file selection converts 60 with no row for the rest | work/ConversionQueue.kt:82, WorkbenchViewModel.kt:161 | Cap the selection with a message, or grow rows |
| M5 | Output-folder mode has no validation/fallback; a revoked folder fails every file | ConversionQueue.kt:212-218, OutputPublisher.kt:186-191 | Validate + fall back to Downloads with a notice |
| M6 | No disk preflight for media: the whole input is copied into cache while a second copy is written | FfmpegRunner.kt:158-218 | Free-space check + streaming where possible |
| M7 | Legacy API 26–28 publish reports FAILED after the file was written, on a 20 s MediaScanner timeout | OutputPublisher.kt:163-164 | Treat scan timeout as success-with-warning |
| M8 | Archive: zip entries are materialised (toList()) before the 50 000 entry guard; unknown-size spill excluded from the 2 GB sum | ArchiveConverters.kt:61-63 | Count incrementally; include spill bytes |
| M9 | EPUB: no DRM/encryption.xml detection, unbounded text accumulation, silent spine skips | engine/ebook/EpubReader.kt | Detect encryption and say so; cap text; report skipped spine items via note |
| M10 | Text encoding limited to UTF-8/UTF-16/CP1256/ISO-8859-1 → GBK/Shift-JIS/CP1251 mojibake | engine/text/TextIo | Charset detection + explicit failure instead of garbage |
| M11 | RoutePlanner tie-break is registration order, contradicting the "order does not matter" comment; the next added converter can silently hijack a route | RoutePlanner.kt:30, BuiltInConverters.kt:21-22 | Edge priorities/weights or documented deterministic tie-break + test |
| M12 | "Cheapest chain" is only hop count — no quality/priority model | RoutePlanner.kt | Rename or add weights |
| M13 | FFmpeg progress formula statistics.time * 1000 / totalMs assumes seconds while totalMs is ms; if getTime() is ms the bar saturates at 99 % instantly — **UNVERIFIED unit** | FfmpegRunner.kt:299,224-225 | One device log settles it; then a pure tested function |
| M14 | Dead MIME aliases (application/vnd.ms-excel → xls, application/msword → doc) create a ghost format with no route | core/FormatRegistry.kt:45-46 | Explicit unsupported handling or delete |
| M15 | Test coverage is pure-utility only: no tests for the registry graph, archive/EPUB converters, PDF page handling, runner notes/multi-output, media timeout, SAF/MediaStore | app/src/test | Topology test + integration tests; androidTest for SAF/MediaStore |
| M16 | Release/ops hygiene: .tmp_research (667 KB) and stale build-*.log/zipalign-*.txt/.tmp_verify in the tree; stray Gradle 8.9 distro in tools/; no ADR directory | tree listing | Clean, .gitignore, docs/adr/0001-… |

### 6.4 Low

POST_NOTIFICATIONS/ACCESS_NETWORK_STATE unused · enableOnBackInvokedCallback absent with targetSdk 36 (back behaviour unverified) · broad */* SEND/VIEW surface (no exploit found; consider narrowing) · no LICENSE file · versionCode 1 / 0.1.0 for anything public · 18 unused resources · 3 unused action_* strings · no mention of the download host in the About screen · RTL screenshot evidence missing.

---

## 7. What is missing (gaps, not bugs)

1. **Release pipeline**: git history, signed AAB, CI, versioning, rollback, crash-reporting policy (deliberately none — document it), Play closed-test plan.
2. **Device verification**: zero instrumented tests, zero device screenshots/recordings, zero API-matrix runs.
3. **Privacy artefacts**: policy, Data-safety form, download disclosure, retention statement.
4. **Licence artefacts**: source offer, licence texts, relink instructions, dependency manifest generated from the graph.
5. **Outcome features**: scan → PDF, multi-image → one PDF, PDF merge/split/compress, compress-to-size, WhatsApp video preset, share-sheet "compress" entry.
6. **Error model**: typed failure codes, plural-aware messages, a note channel to the user.
7. **Durability**: Worker/FGS, resume, batch persistence, notification progress.
8. **Accessibility**: no verified TalkBack pass, no contrast audit of the custom theme, no font-scale check.
9. **Performance evidence**: no cold-start measurement, no 100-photo batch timing, no memory ceiling test on a 2 GB device.
10. **Business**: entity, merchant rail, pricing, support, refunds, trademark registration.

---

## 8. Qiyas cards (precedent → ʿilla → conditions → ruling)

**Card 1 — The retired engine.**
*Precedent:* arthenica/ffmpeg-kit retired (April 2025), archived; many Android apps kept shipping the last LGPL build.
*ʿilla:* the app depends for its most-used codecs on an unmaintained native engine whose licence imposes continuing duties.
*Conditions:* verified fork provenance; complete LGPL-3.0 §4 pack (texts + Corresponding Source + written offer + relink/installation info); counsel sign-off; a migration path.
*Ruling:* **conditionally permitted for a closed beta, not for a public release until the pack exists.** If the deadline forces a choice, a Media3-only build with fewer video outputs is lawful by default; FFmpegKit must earn its place.

**Card 2 — Clean-room reimplementation.**
*Precedent:* KnifeLemon/Filee (MIT, created 2026-09-29, 28 159 stars at review, v2.5.0 on 2026-11-16).
*ʿilla:* copyright protects expression, not the idea of a converter; MIT grants no trademark rights; the project reads the public repo for facts.
*Conditions:* no code, prose, assets, strings or tables copied; own name, icon, screenshots; attribution where due.
*Ruling:* **sound.** Verified: Filee appears only in two code comments, no asset or string in the APK. Keep a signed no-copy attestation per contributor (memo C1) so this stays provable.

**Card 3 — The acquired privacy app.**
*Precedent:* Simple Mobile Tools → ZipoApps → Fossify.
*ʿilla:* a privacy promise carried only by the publisher's word collapses when ownership changes; users cannot audit what they cannot see.
*Conditions:* public source or at least a published permission/network manifest; project licence; transparency page; a "no telemetry" statement that is checkable.
*Ruling:* **conditionally unmet.** The code is clean (no ads/analytics, no logging, SAF-only) but there is no repo, no LICENSE and no public evidence. Adopt LICENSE + permission manifest + transparency page; decide on open source deliberately.

**Card 4 — The demand signal.**
*Precedent:* CamScanner (500 M+ installs), Image-to-PDF apps, WhatsApp "compress video" share flows.
*ʿilla:* in this market the phone is a scanner and a compressor; format conversion is a secondary utility.
*Conditions:* the app must deliver scan → PDF, many images → one PDF, PDF merge/split/compress, and target-size media compression within the first session.
*Ruling:* **the current wedge is wrong.** 48-format breadth is an architecture achievement, not a value proposition. Ship three outcomes before the store push; keep breadth as a silent differentiator.

**Card 5 — The honest price.**
*Precedent:* Musicolet (no ads, one-time unlock, 10 M+ installs).
*ʿilla:* the no-server, no-ads model needs a merchant rail; Iraq is not a supported merchant country for Play.
*Conditions:* a foreign entity for paid distribution, or free + donations; never ads/analytics.
*Ruling:* **monetisation suspended** until the entity question is answered. Ship free; keep the no-ads/no-analytics invariant absolute; decide the rail before spending on branding.

**Card 6 — The written offer.**
*Precedent:* LGPL enforcement actions treat "we linked dynamically" as insufficient without the offer and relink info.
*ʿilla:* a signed APK is a distribution of the library; the duties attach to the binary, not to the repository.
*Conditions:* licence texts in the APK, Corresponding Source for the exact 8.1.9 build, written offer, "you may replace the library" instructions.
*Ruling:* **currently violated.** This is the one Critical that is cheap to fix (a folder, a screen, a URL) and expensive to ignore.

**Card 7 — The platform deadline.**
*Precedent:* Play's API-level mandates (36 since 31 Aug 2026) and Android 16's default predictive-back animation.
*ʿilla:* platform behaviour changes on a schedule the publisher does not control; "it works on my build" is not evidence.
*Conditions:* official requirement read, configuration verified in the artifact, behaviour tested on the target OS.
*Ruling:* **API 36 verified; predictive back and edge-to-edge unverified on device.** Treat as a device-QA item, not a code change until evidence arrives.

**Card 8 — The number as promise.**
*Precedent:* converter apps advertising "270 formats" (Filee's own README) while the useful set is far smaller.
*ʿilla:* a format count is a proxy users decode as "my file will work"; a recognised-but-dead format breaks that promise at the worst moment.
*Conditions:* the advertised number equals formats with at least one working edge, and unsupported formats are refused honestly before selection.
*Ruling:* **drop "48 formats, every one working".** State "48 recognised, 39 convertible" or add the nine edges; use the existing workbench_unsupported message.

---

## 9. Adversarial transcript (condensed to the turns that changed the verdict)

**Expert (opening).** The asl is an offline Arabic-first converter that is honest, safe and shippable. On the evidence, the engine layer meets a professional bar: archives are never extracted, DOCTYPE is refused, OOM is mapped, packs are hash-pinned. The product question is not "does it convert" but "will anyone open it for the jobs they have".

**Objector.** I do not accept "verified" as an adjective. I ran the documented command: 43 tasks up-to-date, 41 s, tests UP-TO-DATE. That is a no-op, not a clean build. I forced :app:clean --no-build-cache: 44/44 executed, 3 m 45 s, BUILD SUCCESSFUL — so the code is real, but verification.md:150 is false as written. Then the APK: the claimed 72,522,602 B / 8922…8266 matches only the saved pre-existing file, which contains **2,952,891 B of internal zip dead space** (a hole before AndroidManifest.xml). The clean rebuild is 69,569,713 B / 75C0FC7B…108F. The doc describes an artefact this tree no longer produces.

**Engineer (engines).** Accept. And the same failure mode exists inside the code: ConversionResult.note has twelve producers and zero consumers — every fidelity limit the engines carefully compute is thrown away. Worse: OCR pdf→txt is registered after PDFBox pdf→txt, and the planner only relaxes on strict improvement, so the OCR converter is fully built, tested, and **unreachable**. Scanned PDF → TXT fails every time, and the user sees "the converter returned no file".

**Objector.** Did you run it? No device. Then mark it "code-path-proven, device-unverified". I will not let the council hang a Critical on an unexecuted inference — but the shadowing is structural, not probabilistic, so it stands as proven-by-construction.

**Engineer (app).** Then two more structural ones. The queue runs in AppScope; WorkManager is declared and unused, and ships WAKE_LOCK/RECEIVE_BOOT_COMPLETED/FOREGROUND_SERVICE into a privacy-first manifest. And FFmpeg has no withTimeout; a stuck session freezes the sequential queue forever. Also, every media conversion preserves GPS/device metadata — no -map_metadata -1 anywhere.

**Policy Guardian.** I concede the privacy engineering: no ads, no analytics, no logs, SAF-only, hash-pinned packs. I do not concede the paperwork. README says relinking information is published; it is not. NOTICE.md lists ONNX Runtime, which is not in the APK, and omits leptonica, libjpeg, libpng, BouncyCastle and 32 ffmpeg-kit externals. There is **no privacy policy anywhere** — not a file, not a screen, an unused string. Play requires it on the listing and in the app even when no data is collected. And "Nothing leaves your phone" next to a declared INTERNET permission and a 28 MB download is a deceptive-behaviour complaint waiting to be written.

**Market.** The wedge is the deeper problem. The demand proof in this region is scan/merge/compress, and the app cannot merge two PDFs, cannot scan, and turns ten photos into ten PDFs. Meanwhile the monetisation rail does not exist: Play shows Iraq as developer-supported and **merchant-unsupported**. There is no price we can charge from Baghdad today.

**Naive User.** I install it. The Arabic UI looks right. I pick a scanned PDF, choose TXT — it fails with an English message about an OCR pack I never saw offered, and a second time with "no file". I pick a TIFF — "these files have no target in common", which blames my file mix, not the app. I convert a video and the progress bar jumps to 99 % and sits there. I close the app mid-batch and everything is gone. I do not come back.

**Forced revisions (what the council changed under its own evidence):**

1. **The 48-format headline dies.** It was the planned store hook; the graph shows 9 dead formats and 39 convertible. Revised claim: "48 recognised, 39 convertible" — or add the edges.
2. **"16 KB compliant" is re-established on better evidence, not the original.** The cited zipalign proof is vacuous on compressed libs; the ELF p_align=16384 parse is real. The action changes from "prove it" to "fix packaging so the claim is true as written".
3. **"Durable via WorkManager" is deleted from the design's self-description.** The dependency exists; the execution model does not. Either implement a Worker or remove the dependency and the permissions.
4. **"Cheapest chain" is downgraded to "fewest hops".** A uniform-cost router cannot prefer a text layer over OCR or a better encoder; the wording overclaims.
5. **The plan order is inverted.** The first draft started with features. Under the Objector's and Guardian's evidence, the first 30 days are evidence, licence, privacy and device QA; features start after the closed test is feasible.

**Fallen ideas (recorded, not hidden):**

- *"Breadth is the moat."* Fallen: the market opens for outcomes; breadth is architecture, not demand.
- *"The verification doc proves the build."* Fallen as evidence; the build is real, the document is stale and one command was a no-op.
- *"FFmpegKit is fine because the POM says no GPL."* Fallen: a POM is a claim; LGPL duties attach to the distributed binary.
- *"The app is fully offline."* Fallen as copy: it is offline for conversion and one tap away from a network call. Precision, not deletion, is the fix.

---

## 10. Maestro verdict

The Maestro speaks once, at the stop condition.

| # | Proposition put to the council | Verdict | Ruling |
|---|---|---|---|
| 1 | The engine/core architecture (Converter graph, Dijkstra, family wiring) | **Survived** | Keep. It is the project's real asset; do not rewrite it for features. |
| 2 | Archive/XXE/OOM/pack-download safety discipline | **Survived** | Keep and extend to the remaining surfaces (metadata, timeouts, Throwable boundary). |
| 3 | "48 formats, every one with a working edge" | **Dropped** | False. Replace with "48 recognised / 39 convertible" or implement the nine edges. |
| 4 | "32 edges from 12 families" | **Dropped** | 32 converters, 172/171 edges, 11 families (10 productive). |
| 5 | Documented APK size/hash and "clean invocation" | **Dropped** | Regenerate from a forced clean build; publish command + output + hash together or publish nothing. |
| 6 | "16 KB aligned" as documented | **Downgraded** | Property true (ELF p_align), evidence invalid, "uncompressed" false. Fix packaging, restate proof. |
| 7 | WorkManager as the durable execution model | **Suspended** | It is not used. Either implement Worker + progress notification or remove the dependency and its permissions. |
| 8 | AppScope-only execution | **Dropped** | Replace with Worker/FGS before open beta; process death is not an acceptable data-loss mode. |
| 9 | ConversionResult.note | **Downgraded** | Keep the field, wire it end-to-end; a computed-then-discarded note is worse than no note. |
| 10 | PDF→TXT via OCR | **Survived, blocked** | The converter is good; the route is shadowed. Mandatory fix: blank-text fallback. |
| 11 | "Fully offline / nothing leaves your phone" | **Downgraded** | Conversion offline: true. Copy must disclose the one optional download; host a privacy policy; run the offline test. |
| 12 | FFmpegKit choice | **Suspended** | Lawful only with the LGPL pack and counsel sign-off; a Media3-only fallback decision point at day 45. |
| 13 | Current attribution/licence screen | **Dropped** | Ink on a promise. Rebuild from the resolved dependency graph; phantom ONNX out, missing components in. |
| 14 | Release readiness (signing, R8, AAB, lint gate, CI, repo) | **Dropped** | None of it exists; the debug APK is not a release. |
| 15 | Device QA as "pending" | **Downgraded to mandatory** | Not a footnote; the first action. AVD exists, hardware not touched. |
| 16 | Current product wedge (format matrix as store hook) | **Dropped** | Replace with four outcomes: scan→PDF, multi-image→one PDF, PDF merge/split/compress, compress media to size. |
| 17 | Paid app / IAP as the 2026 plan | **Suspended** | Iraq has no merchant rail; decide the entity before pricing. Ads and analytics remain forbidden. |
| 18 | "Sigha" as the final brand | **Suspended** | No registry search; a common Arabic word; keep as working name until searched and registered. |
| 19 | Upscaling ("Coming soon") | **Suspended** | Keep it off. Never advertise it; no model in the APK. |
| 20 | The Arabic-first claim | **Downgraded** | 165/165 keys is necessary, not sufficient: error paths are English, plural grammar is absent, copy defects exist. |

**Stop condition:** the council stops when the evidence stops changing the ruling. It did at proposition 20.

---

## 11. Step Zero (today, one working session)

Goal: make the repository truthful and internally consistent. Nothing user-visible changes.

1. git init, first commit (.gitignore already present); decide LICENSE (proprietary is acceptable — file it anyway; the *decision* is what must exist).
2. Delete .tmp_verify/, .tmp_research/, build-*.log, zipalign-*.txt, tools/ distro. Stale evidence is worse than none.
3. Remove android:extractNativeLibs="true" from AndroidManifest.xml; :app:clean :app:assembleDebug; assert **32 stored** .so, zipalign -P 16 pass, ELF p_align = 16384; record the new size + SHA-256.
4. Correct claims in one pass: README (APK numbers, "32 edges/12 families", relinking statement), PRODUCT.md:22,59,66 ("resize", "every format", numbers), verification.md (clean-command, APK facts, "uncompressed", "12 families", permissions row).
5. Fix NOTICE.md + LicensesScreen: remove ONNX; state fork licence correctly (POM: LGPL-3.0) and FFmpeg build licence (LGPL-2.1-or-later) consistently; list the missing shipped components; add the LGPL source-offer + relink paragraph (even a placeholder URL is honest; "published" is not).
6. Write docs/legal/privacy-policy.md + in-app screen + Settings link; disclosure: files never leave the device; the only network call is a user-initiated HTTPS GET for the OCR pack (size, host, IP).
7. Remove the unused work-runtime dependency (it is not the execution model yet) and the four permissions it injects; keep POST_NOTIFICATIONS only when notifications exist.
8. Add scripts/verify.ps1: clean build → tests → hash → stored-lib assertion → ELF check → claim extraction, so the next report cannot drift.

**Definition of done:** every number in README.md, PRODUCT.md and verification.md is produced by that script in this tree, or is labelled historical.

---

## 12. The 90-day plan (evidence first, features second)

### Phase 1 — Weeks 1–2: truth and safety (P0)
- Step Zero complete.
- Scanned PDF→TXT fallback; note surfaced to queue/history; PDF→TXT honours page ranges; multi-output intermediates fixed or forbidden.
- FFmpeg: -map_metadata -1 -map_chapters -1, withTimeout, cancel-on-timeout; runner catches Throwable and maps OOM.
- OCR target gating (grey out until pack installed); dead formats hidden or implemented; workbench_unsupported used.
- Error localisation: typed failure codes + R.string; plurals for counts; one Arabic copy review pass.
- DataStore corruption handlers; SAF grant release; output-folder validation/fallback; queue cap message.
- **Exit:** clean build + tests green under the new script; no English error can reach the UI; APK packaging assertions pass.

### Phase 2 — Weeks 3–4: release mechanics and device proof (P0/P1)
- Signing config, R8, AAB + per-ABI, lint gate, CI (even a local scheduled script), versioning.
- Boot the API-36 AVD + one physical arm64 device; run the verification checklist (SAF, MediaStore, 20 conversions across families, backgrounding, process death, RTL, TalkBack pass, font scale, API 26–28 legacy publish if a device is available); record evidence.
- androidTest for SAF + MediaStore + queue + one engine family; registry topology test; notes test.
- Privacy policy live + in-app link; Play Data-safety answers drafted; network-off conversion test.
- **Exit:** a signed AAB built from a clean tree; a dated device-evidence folder; offline test passes with the radio off.

### Phase 3 — Weeks 5–8: licence closure and outcomes (P1)
- LGPL compliance pack (texts, Corresponding Source for 8.1.9, written offer, relink instructions) signed off by counsel; otherwise make the Media3-only call by day 45.
- Outcome features: many images → one PDF; PDF merge/split/compress; image compress-to-size; video compress preset + share-sheet entry; camera scan only if time allows.
- OCR pack UX: confirm dialog (size, licence, metered warning), HTTP Range resume, specific error surfacing, background-capable download.
- Trademark search for the working name (Iraq + Play), then either register or rename before any public artefact.
- **Exit:** counsel sign-off or engine migration decision recorded; four outcomes work on device; OCR first-run is a two-tap, informed flow.

### Phase 4 — Weeks 9–12: closed test and the go/no-go (P1)
- 12 testers × 14 days closed test on Play (requires the release rail + privacy policy + Data safety).
- Instrument crash-free rate, D1/D7, conversion mix, OCR adoption, failure taxonomy from typed codes.
- Fix the top failure clusters; re-run device matrix; publish an honest changelog and verification report.
- Decide monetisation/entity; ship free until the rail exists.
- **Exit:** kill metrics below pass; then and only then open the listing.

---

## 13. Kill metrics and decision gates

| Gate | Metric | Threshold | If missed |
|---|---|---|---|
| G1 Packaging | Clean build + 131 tests + 32 stored libs + ELF 16 KB | all green | Stop; no store work until green |
| G2 Device | Smoke matrix pass rate on 3 API levels + 1 physical device | ≥95 %, crash-free ≥99 % | Freeze features; fix reliability |
| G3 Licence | LGPL pack + counsel sign-off, or Media3 decision | complete by day 45 | Do not publish; ship the narrower lawful build |
| G4 Policy | Privacy policy live, Data safety submitted, offline copy precise, network-off test | complete before closed test | No closed test |
| G5 Closed test | 12 testers × 14 days | Play requirement | Postpone launch; never bypass |
| G6 Retention | D1 ≥ 20 %, D7 ≥ 8 % at ≥200 installs | per cohort | Rethink wedge before spending on growth |
| G7 Wedge fit | ≥35 % of conversions are document/compress outcomes | per telemetry (local-only or to be decided) | Reposition or narrow scope |
| G8 Trust | Zero false claims in the listing; zero unresolved licence findings | absolute | Revert the listing wording immediately |
| G9 OCR | Pack install rate ≥ 20 % of document users; first-run OCR failure < 5 % | per cohort | Demote OCR from the pitch |

**Absolute stop rule:** if any shipped claim is found false after listing, treat it as a P0 incident — correct the copy the same day and record it in the verification doc.

---

## 14. Risk register (ranked)

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| LGPL/attribution defect found after release | Medium | High (takedown, trust) | Phase 3 pack; counsel; source offer in-app |
| Play rejection for "offline" copy / missing privacy policy | High | High | Step Zero + Phase 2 policy and Data safety |
| FFmpegKit fork abandoned mid-flight | Medium | High | Dependency pin + verification metadata + Media3 fallback decision at day 45 |
| Device-only failures (SAF/MediaStore/FFmpeg/Tesseract) | High | High | Phase 2 device matrix before any feature work |
| No git → lost work / no rollback | High | High | git init today |
| Trademark objection to the working name | Medium | Medium | Search before any public asset; rename early |
| Iraq merchant block on monetisation | High | Medium | Free launch; entity decision; donations |
| OCR 28 MB on metered data | Medium | Medium | Confirm dialog, metered check, resume, CDN |
| Silent data loss (notes, page 1 only, ignored page ranges) | High | Medium | H2 fixes + tests |
| Metadata leak from media conversions | Medium | High (privacy promise) | H3 flags + test |
| Competing Filee-derived apps close the window | Medium | Medium | Ship outcomes, not breadth |

---

## 15. Decisions only the human can make

1. **Markets** (Iraq only / MENA / global) — drives counsel, entity and trademark scope.
2. **Monetisation and entity** — Play merchant registration is unavailable for Iraq; a foreign entity or free+donations are the only lawful paths seen today.
3. **FFmpegKit vs Media3-only** — the licence burden against video-format coverage.
4. **Name** — keep "Sigha" (search + register) or rename before any public artefact.
5. **Open source or not** — either is defensible; silence is not (LICENSE + transparency page either way).
6. **OCR hosting** — GitHub Releases vs contracted CDN; affects privacy wording and metered behaviour.
7. **Model packs for v1** — OCR only, or OCR + upscaling (currently "Coming soon").
8. **RAR** — already effectively dropped; record it formally to end the unRAR question.

---

## 16. Code-review annex (project-wide)

| Category | Findings | Highest severity | Note |
|---|---|---|---|
| Security | 6 | High | XXE/zip-slip/pack verification are strong; metadata leak, redirect host check, transitive TrustAllX509TrustManager (PDFBox→bcpkix), no dependency verification |
| Performance | 4 | Medium | Whole-input media copy, no disk preflight, unbounded PDFBox memory on huge PDFs (unmeasured), no baseline metrics |
| Architecture | 7 | Medium | Router tie-break by registration order; "cheapest" is hop count; note channel dropped; multi-output intermediates; execution model undeclared |
| Quality | 9 | High | Dead formats, English error paths, plurals, stale docs, unused strings, lint gate off |
| Testing | 5 | High | 131 pure-utility tests, zero instrumentation, no device run, no graph/runner/engine integration tests |
| Documentation | 8 | Critical | Claim drift is the single largest defect class; the legal and QA dossiers are otherwise unusually good |

**ADR gate (code-review skill):** docs/adr/ does not exist. Three decisions are currently recorded only in prose and should be ADRs: **ADR-0001** engine choice (FFmpegKit vs Media3) and its licence obligations; **ADR-0002** execution model (AppScope vs Worker/FGS); **ADR-0003** catalogue-as-product vs outcome-first product.

**Not done / unverified (stated, not hidden):** no device or emulator run; no assembleRelease/bundleRelease; ELF alignment verified for arm64 libs only (x86_64 assumed, same build); Statistics.getTime() unit unresolved (needs one device log); OpenH264 presence in the FFmpeg build unverified; smart-exception-java licence unknown (URL 404); Play 16 KB packaging nuance to be re-read at release time; competitor ad/IAP badges medium confidence.

---

## 17. Evidence paths

- Seat reports: docs/council/2026-11-17-review/{engine-auditor,app-auditor,build-verifier,policy-guardian,product-strategist}.md
- Build evidence: build-final.log, docs/qa/verification.md, zipalign-out.txt
- Legal: docs/legal/clearance-memo.md (§6.1 C1-C12, §6.2 Q1-Q8, §7), NOTICE.md
- Product: docs/product/PRODUCT.md, docs/models/integration.md, docs/research/*
- Reproduction (Phase 1 script): :app:clean :app:testDebugUnitTest :app:assembleDebug --no-build-cache --no-daemon --console=plain → parse app/build/test-results, Get-FileHash, zip entry compression check, ELF PT_LOAD parse.

---

## 18. The first action (one line)

**Today: git init and commit the tree as it is — then run the clean-build + packaging-assertion script and rewrite every claim that script does not reproduce.** Everything else in this report is downstream of being able to prove what we say.
