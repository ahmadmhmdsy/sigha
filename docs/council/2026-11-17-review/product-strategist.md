# Seat report — The Expert (product / market / regulator) · Naive User proxy · The Accountant (unit economics)

**Project:** Sigha (صيغة) — privacy-first offline Android file converter, `E:\android_apps\digital_converter`
**Council:** qiyas-council, merged mode, 2026-11-17 · **Seat:** product-strategist
**Method:** repository reads and greps (read-only), PowerShell/GitHub API fetches, Google Play listing pulls, live web pages; nothing in app source was modified. **Not run by this seat:** full Gradle build, unit tests, device QA, Play submission, DEX-level network audit.

---

## Verdict (one paragraph)

The engineering is real and unusually disciplined for a first build — a clean converter graph, honest QA document, no ad/analytics SDKs, 193/193 Arabic strings, SAF-only file access — but the *product* is not yet the product the market will pay attention to, and the *business* has no legal path to take money today. The build is a **format converter**; the demand sitting in front of an Iraqi/MENA user in 2026 is **compress-for-WhatsApp, images→one PDF, PDF merge/split/compress, and scan-to-PDF** — none of which exist in this build, while CamScanner alone shows 500M+ installs for just the scanning slice. At the same time, an Iraq-based publisher **cannot register a Play merchant account** (Google's own table: Iraq ✔ developer registration, ✘ merchant registration), so free-with-IAP/paid — the only model that has ever sustained a no-ads offline utility (Musicolet: 10M+, no ads, one-time unlock) — is blocked until an entity exists abroad. The fix is not more formats: it is **four document operations + shippable artifacts (repo, licence, release signing) + a monetization/entity decision**, before any store upload. As built today the app cannot even be listed (debug-signed, no repo, no privacy policy), and its two headline claims — "48 formats, every one with a working edge" and the APK byte hash — are both false or unreproducible.

---

## Findings

### F1 — Critical — No shippable artifact: debug-signed APK only, no repo, no release key, no CI
**Evidence:** `app/build.gradle.kts:22-27` — release block has no `signingConfig`, `isMinifyEnabled=false`; no `bundle`/ABI-split config (`:12-20` puts arm64-v8a **and** x86_64 in one APK). PowerShell: `Get-ChildItem -Recurse -Include *.jks,*.keystore` → **0**; `.git` absent (`Test-Path '.git'` → False); `.github` workflow files → **0**. APK on disk is `app-debug.apk` (debug keystore, `android:debuggable` inherited) — Play rejects debug builds. No privacy-policy file exists anywhere in the repo (grep of docs/README/NOTICE: none).
**Why it matters:** every market plan is downstream of this. A debug APK is not distributable to Play; a source-less "verifiable offline" app is not verifiable; without git there is no build history to audit or roll back.
**Fix (1–2 days):** `git init` + public repo, `LICENSE` (see F9), release `signingConfig` in `~/.gradle` properties (never in repo), `bundleRelease` + per-ABI splits, GitHub Actions running `:app:testDebugUnitTest :app:assembleRelease`, and publish a privacy policy URL. Only then start the 12-testers × 14-days Play closed-test clock.

### F2 — Critical — Monetization is structurally blocked for an Iraq-based publisher
**Evidence:** Google Play Console Help, *Supported locations for developer and merchant registration* (fetched 2026-11-17): row **Iraq ✔ ✘ USD** — developer registration supported, **merchant registration not supported**; page text: "If your location supports merchant registration, Google Play customers must be in a supported location to purchase apps." A merchant account is required for paid apps and in-app purchases. The same page lists Türkiye ✔ ✔, UAE ✔ ✔, Jordan ✔ ✔ — i.e., an entity in one of those countries would unblock it.
**Why it matters:** the two sane monetization models for this product (one-time "Pro"/OCR-pack purchase, as Musicolet proves at 10M+ installs) cannot be collected by an Iraqi developer under Play billing; ads are excluded by the product's own trust thesis; donations need a payout rail (Stripe/PayPal/Ko-fi/GitHub Sponsors do not pay to Iraq). This is not a pricing question — it is a legal-entity question, and the prior council left it as an open item rather than a launch blocker.
**Fix:** decide before pricing: (a) register an entity in a merchant-supported country (Türkiye/UAE/Jordan) if revenue is intended; or (b) commit to **free core + optional donation link that works from Iraq** and treat revenue as deferred. Do not put price text in the store listing until (a) or (b) is chosen.

### F3 — High — The wedge does not match the highest-frequency demand: no merge, no compress, no scan
**Evidence:** greps over `app/src/main/java/.../engine/`: no `merge`/`split` in `engine/pdf` (only `PdfPageSelection.kt:24`, which parses a page-range string); no camera/scan code or `CAMERA` permission (manifest has only INTERNET, ACCESS_NETWORK_STATE, POST_NOTIFICATIONS, WRITE_EXTERNAL_STORAGE≤28 — `AndroidManifest.xml:4-7`); `ImageToPdfConverter.kt:20-24,69-105` converts **one** image to **one** A4 PDF; `ConversionQueue.kt:72-101,127-129` converts each input file independently, so 10 images → 10 PDFs, never one. Video: `FfmpegArgs.kt:82-122` exposes bitrate only (no resolution control in the UI; `WorkbenchViewModel.kt:139-149` sets only quality/bitrate/pages), so there is no "compress to under X MB for WhatsApp".
**Why it matters:** these are the repeat jobs in the target market. CamScanner (scan→PDF) shows **500M+ installs** on Play (fetched 2026-11-17, listing header), and the market dossier's 10M+ converter incumbents are *compress* tools (Inverse AI, fwit, Bdroid). A user who wants "10 receipts → one PDF" cannot do it; the app's core gesture (pick files → pick target format) is a *conversion* gesture, not a *document outcome* gesture. This is the single finding most likely to cause low retention even if installs happen.
**Fix (highest ROI):** add outcomes, not formats: (1) multi-select images → one PDF (ordered, page size, DPI); (2) PDF merge/split/compress (PDFBox can rewrite/compress streams); (3) image compress-to-target-size (iterate quality/dimension until ≤ N KB); (4) "Compress for WhatsApp/Share" video preset with target-size search (bitrate + maxDimension loop) and a share-sheet entry; (5) camera capture → auto-crop → PDF (later than 1–4). Each is small relative to the 5,696-line engine already built.

### F4 — High — Scope claims are misleading: "48 formats, every one with a working edge" is false
**Evidence:** `README.md:20,32-33` and `docs/product/PRODUCT.md:59,66` ("48 formats … every format here has a working edge; no catalogue padding"). `core/FormatRegistry.kt:97,108,146,147` registers `tiff`, `rtf`, `mobi`, `fb2`; grep across `engine/` for those ids → **no matches** (no converter references them). The project's own QA doc admits it: `docs/qa/verification.md:127` — "MOBI / FB2 have no converters… RTF/TIFF/DOC are catalogue entries without edges." Worse, the user-facing message when such a file is picked is `workbench_no_target` ("These files have no target in common. Try files of the same kind", `WorkbenchScreen.kt:357`); the dedicated `workbench_unsupported` string (`strings.xml:65`) is **never used** (grep of `workbench_unsupported` → 0 call sites).
**Why it matters:** Play's Metadata and Deceptive Behaviour policies require the listing to "precisely reflect the app's functionality"; a "48 formats" listing and a "try files of the same kind" dead end is exactly the mismatch reviewers and 1-star reviewers punish. It also contradicts the project's own "honesty" positioning.
**Fix:** hide catalogue entries with no edges from the picker/format UI (or add edges); say "44 formats" (recount) or "formats that work on this phone"; use the unused string with the file name; and add a pre-submission claim audit to `docs/qa`.

### F5 — High — "Arabic-first" is real in count, weak in quality — and no longer unique at idea level
**Evidence:** 193 EN / 193 AR strings (line counts identical) — a genuine strength. But Arabic quality defects: `values-ar/strings.xml:141` `category_subtitle` = **"الترجمة"** ("translation"), not subtitles; `:110` `output_mode_downloads` = "التنزيلات/Sigha" while the real folder is `Download/Sigha`; all counts are plain `<string>` with `%1$d` (`:43,60,68,93`) and there are **zero `<plurals>`** in either language, so Arabic's zero/one/two/few/many categories produce ungrammatical output ("%1$d محددة", "تحويل %1$d ملفات"). Meanwhile Filee itself now ships an **Arabic README and a 20-language website** (`README.ar.md` in KnifeLemon/Filee; filee.sh language list fetched 2026-11-17), so "the market leader set ignores Arabic" is true of *Android apps*, not of the idea's source.
**Why it matters:** Arabic-first is the chosen differentiator; a native speaker noticing "الترجمة" for subtitles and broken plural agreement reads it as machine translation, which damages trust faster than an English-only app would. Trust is the product.
**Fix:** native-review the AR string file; convert counts to `<plurals>` with `zero/one/two/few/many/other`; fix the subtitle term and the Downloads path label; add an RTL screenshot set and an Arabic reviewer to the closed test.

### F6 — Medium — OCR pack download UX is not designed for metered, low-bandwidth users
**Evidence:** `ModelPacksScreen.kt:74-91` starts a **28,004,325 B** download immediately on tap with no confirmation dialog, no "mobile data vs Wi-Fi" question, no size/space warning; `PackCatalog.kt:16-28` (15.4 MB eng + 12.6 MB ara) fetched from `raw.githubusercontent.com` pinned to a commit; `PackInstaller.kt:66-95` allows 3 whole-file attempts and **no resume** — a dropped 28 MB transfer restarts from byte 0; `ModelPackStore.kt` exposes no download-count telemetry (by design), so pack completion rate is invisible.
**Why it matters:** in the target market a 28 MB download on prepaid mobile data is a real spend; an unexplained, restartable-only download is the kind of thing users abandon and then rate 1★. GitHub raw is not a contracted CDN; at scale GitHub may rate-limit/abuse-block, and there is no way to observe it.
**Fix:** show the size and "download once, then works offline" in a confirm dialog (size/licence already rendered in the row, so reuse it); respect `ConnectivityManager.isActiveNetworkMetered` (ask once, remember); implement HTTP Range resume for the `.part` file; move packs to a CDN with a stable path (Cloudflare R2 has free egress); keep the SHA-256 pin.

### F7 — Medium — APK size and ABI strategy are unmeasured for release; the "16 KB" packaging claim is contradicted by the build itself
**Evidence:** debug APK is 69,569,713 B today (measured), containing arm64-v8a **and** x86_64 and 16 compressed `.so` per ABI (`zipalign-claimed.txt:15-36` shows every native lib as "OK - compressed"); `verification.md:46` claims "uncompressed, aligned native libs", but `build-final.log` warns: *"PackagingOptions.jniLibs.useLegacyPackaging should be set to true because android:extractNativeLibs is set to 'true' in AndroidManifest.xml"* — because `AndroidManifest.xml:12` sets `extractNativeLibs="true"`. The release-size estimate "~45–55 MB" (`verification.md:130`) has not been built, and no ABI split is configured.
**Why it matters:** install conversion in low-storage markets depends on the download size, and the 16 KB page-size mandate (Play: update-blocking from 2027-02-01) must be demonstrated with ELF-level checks (`readelf -l`), not on a zip whose libs are compressed. This is the Engineer seat's area; I record it because the QA document currently overstates it.
**Fix:** build a release AAB with per-ABI splits + R8 + trimmed icons and record its real size; remove `android:extractNativeLibs` from the manifest and let AGP honour `useLegacyPackaging=false`; verify 16 KB at ELF level on every shipped `.so`; put both numbers in `verification.md`.

### F8 — Medium — The "asl" (Filee) is a 7-week-old high-velocity startup, not an established precedent — and it is already multilingual with a web build
**Evidence (GitHub API, fetched 2026-11-17):** `KnifeLemon/Filee` — **created_at 2026-09-29**, 28,159 stars, 1,611 forks, 173 open issues, releases v2.4.0 (11/09) → v2.4.1 (11/14) → **v2.5.0 (11/16)**, MIT, not archived. Author repos include **Filee-Web** (JavaScript, pushed 11/16) and the README links a LinkedIn company page and `filee.sh` with 20 languages including **العربية**; the website offers an in-browser "Try it here".
**Why it matters (takhrij al-manat):** the prior council treated Filee as a settled desktop case proving "local + breadth + zero-decision wins". The stars do prove demand, but the *cause* of the 28k-star spike is a 2026 product launch + trend aggregation, not years of retention; the same author is one commit away from a web/mobile surface, and the brand already localises to Arabic. Sigha's 48 formats vs Filee's 263 also means breadth parity is unreachable; the differentiator must be *the mobile job* (share-sheet compression/merge/scan), not format count.
**Fix:** drop any "Filee for Android" positioning; position on outcomes + Arabic + verifiability. Track Filee-Web weekly; if a PWA/mobile build appears, the window is closing.

### F9 — Medium — Trust is asserted, not made verifiable: no project licence, no public source, no transparency page
**Evidence:** no `LICENSE` file in the repo root (file listing; only `NOTICE.md`); README says "Sigha is original code" and describes third-party notices, but never states the licence Sigha itself is offered under; there is no git repo (F1) and no published artifact. `NOTICE.md`/Licenses screen cover third parties, which is good.
**Why it matters:** the SMT precedent is exactly this failure mode — a beloved no-ads Android utility (Simple Mobile Tools) was sold to an ad company, users revolted, and trust migrated to a community fork (Fossify). The lesson is causal: **when users cannot audit or relicense the thing they trust, ownership change destroys the trust premium.** Sigha's entire pitch is a trust premium.
**Fix:** choose a licence now (AGPL-3.0 fits "if you fork it, stay open"; Apache-2.0 if you want maximum reuse), publish the repo and reproducible build instructions, add an in-app "Transparency" page listing every permission, every network call (one: pack download), and the SHA-256s. That converts the claim into evidence — exactly the competitor gap the dossier identified.

### F10 — Medium — "Offline" copy is qualified in the app but not in the most visible string
**Evidence:** `values/strings.xml:6` `app_tagline` = "Convert offline. Nothing leaves your phone." — but the same app downloads a 28 MB OCR pack over the network (`ModelPackStore.kt:20-27`, `PackInstaller.kt:97-125`), and the manifest declares `INTERNET` (`AndroidManifest.xml:4`). The About and Model Packs screens do qualify it (`strings.xml:148,171`). Store listing wording is not written yet.
**Why it matters:** Play's Metadata/Deceptive Behaviour policy requires precise listing claims; "nothing leaves your phone" is literally false at pack-download time, and privacy-conscious reviewers read the permission list before the description. The *accurate* claim is stronger anyway: "converts offline; optional OCR pack downloads once".
**Fix:** rewrite tagline/store copy as "Converts on your phone. Optional OCR pack downloads once." Keep the airplane-mode test as the proof and publish it.

### F11 — Low — The 48-format breadth is invisible until a file is chosen; there is no discoverability surface
**Evidence:** target chips are computed only from selected files (`WorkbenchViewModel.kt:244-260`); the empty state lists categories, not capabilities (`strings.xml:40-42`, `WorkbenchScreen.kt` empty branch). There is no "what can it do" screen, no sample file, no per-route limitations text (e.g., "DOCX→PDF is text-only").
**Why it matters:** breadth is the claimed differentiator and the user cannot see it; worse, the first DOCX→PDF attempt silently produces a text-only PDF (`verification.md:124-126`), which reads as "broken" rather than "scoped".
**Fix:** add a compact capability matrix screen (source → targets, with "text-only"/"no layout" badges) linked from the empty state; state limits before conversion where relevant.

### F12 — Low — Output/history semantics are safe but thin: public `Download/Sigha`, last-50 history, no re-export or batch summary export
**Evidence:** `strings.xml:110-113` (Downloads/Sigha or a chosen folder), `strings.xml:93` ("last 50 conversions"), `HistoryStore` cap; `ConversionQueue.kt:164-178` records one history row per output. No "open folder", no "share all", no per-batch summary.
**Why it matters:** none of this blocks a first conversion, but "where did my file go?" is a top support question; and because the app has no analytics, support burden is the only feedback loop.
**Fix:** a post-batch result sheet with "Open folder / Share all / Done" and a "History keeps the last 50; files stay in Downloads" line already present; add a one-tap "Open Downloads/Sigha".

### F13 — Medium — QA evidence is stale/non-reproducible as of this review (claim integrity)
**Evidence:** `docs/qa/verification.md:37-38` claims APK **72,522,602 B**, SHA-256 `8922614FD9F06FD1C3E30F0D957D533E7CBCC8EFAA26064C744EF5A8BD9E8266`. Measured now: `Get-Item app/build/outputs/apk/debug/app-debug.apk` → **69,569,713 B**; `Get-FileHash` → **75C0FC7BCDABF445EE3982533013D0339187C8CB89F6115587AC35438D29108F**; artifact mtime **2026-11-17 07:28:14**, while a clean rebuild ran in this workspace at 07:28:15 (`build-verify-clean.log`). So the documented artifact is either stale or was replaced during the review — the byte-for-byte claim does not reproduce.
**Why it matters:** a hash claim is only worth something if it reproduces; a QA document that cannot be reproduced is worse than no document because it manufactures false confidence, and the same number would go into release notes/store evidence.
**Fix:** rebuild clean, recompute size/hash, update `verification.md` with the command **and** its output, and gate the doc on a script (`scripts/verify-apk.ps1`) that fails when the artifact and the recorded hash diverge. Do not cite either hash in public copy until it reproduces.

---

## Claims checked vs actual

| Claim (where) | Check performed | Actual | Verdict |
|---|---|---|---|
| "48 formats, every one with a working edge" — `README.md:20,32-33`; `PRODUCT.md:59,66` | grep catalogue ids in `engine/` | `tiff`, `rtf`, `mobi`, `fb2` registered (`FormatRegistry.kt:97,108,146,147`) but **no converter references them**; `verification.md:127` admits it | **FALSE (≥4 dead formats)** |
| Debug APK 72,522,602 B / SHA-256 8922… — `verification.md:37-38` | `Get-Item` + `Get-FileHash` | 69,569,713 B / 75C0FC7B… (mtime 07:28:14 today) | **NOT REPRODUCIBLE / STALE** |
| "16 KB-aligned" — `README.md:20`; `verification.md:45` | `zipalign-claimed.txt`; `build-final.log` warning; manifest | zipalign passes but every `.so` is **"OK - compressed"**; build warns `extractNativeLibs="true"` contradicts `useLegacyPackaging=false`; no ELF check shown | **UNVERIFIED / misleading packaging note** |
| "Images … resize" — `PRODUCT.md:22` | UI setters + strings | engine has `maxDimension` (`Converter.kt:10`) but UI only exposes quality/bitrate/pages (`WorkbenchViewModel.kt:139-149`); no resize control | **FALSE for the shipped UI** |
| targetSdk/compileSdk 36; minSdk 26; ABIs arm64-v8a + x86_64 — `README.md:16`; `build.gradle.kts` | read config | `compileSdk=36`, `targetSdk=36`, `minSdk=26`, both ABIs | **TRUE** |
| 131 JVM tests green — `README.md:20` | not run by this seat (Engineer seat has the build) | documented table in `verification.md:58-76` | **NOT VERIFIED HERE** |
| 32 converter edges / 12 families — `README.md:32` | not re-counted by this seat | per `verification.md:84-85` | **NOT VERIFIED HERE** |
| "No ads, no analytics" — `README.md:6`; `PRODUCT.md:41` | dependency list `build.gradle.kts:56-99` | no ad/analytics SDK; only FFmpegKit, PDFBox, Commons Compress, xz, Tesseract4Android, Coil, AndroidX, serialization | **TRUE at compile-level** (network audit not run) |
| Arabic: full strings + RTL — `PRODUCT.md:40`; `README.md:17` | string file comparison | 193 EN / 193 AR lines; `supportsRtl="true"` (`AndroidManifest.xml:16`) | **TRUE in count, quality gaps (F5)** |
| Device QA not done — `README.md:20`; `verification.md:152` | read | stated honestly; only emulator smoke test (§6) | **TRUE and correctly disclosed** |
| Filee: 28k-star MIT desktop converter — `README.md:9`; prior council §2 | GitHub API 2026-11-17 | 28,159★, 1,611 forks, MIT, created **2026-09-29**, v2.5.0 shipped 11/16 | **TRUE but framing overstates maturity (F8)** |
| "New personal Play accounts: closed test 12 testers × 14 days" — `docs/research/market.md:173` | cited to official page (not re-fetched by me) | consistent with Play policy as researched 2026-11-16 | **ACCEPTED (secondary source)** |

---

## Competitor table (offline converter / document-demand landscape, late 2026)

| # | Name | Model | Offline | Arabic | Weakness that matters to Sigha | Source | Confidence |
|---|---|---|---|---|---|---|---|
| 1 | **Filee** (desktop, MIT) | FOSS, free, no ads; 263 formats; v2.5.0 | Yes (local engines; optional model downloads) | Yes — Arabic README + website in 20 languages | Desktop only (Win/Linux/macOS; Win-on-ARM), no Android; 7-week-old project; 173 open issues; unclear revenue | github.com/KnifeLemon/Filee (API), filee.sh | High (fetched 2026-11-17) |
| 2 | **CamScanner** | freemium: ads + IAP | Partly; scan is local, cloud/OCR features online | Not verified | 500M+ installs but 2019 malware-in-ad-SDK incident (Google removed, then restored); ads/premium prompts | Play listing (500M+, 4.6, 5.07M reviews) + Wikipedia | High (installs fetched 2026-11-17; Arabic unverified) |
| 3 | **MobiSystems OfficeSuite** | ads + IAP | Partly; PDF tools local | Not verified | 100M+ installs, heavy suite; PDF-centric competitor for merge/compress/convert | Play listing (100M+, 4.6, 1.51M reviews) | High (installs fetched) |
| 4 | **Musicolet** | free, **no ads**, IAP (one-time Pro) | Yes (offline music player) | Not verified (UI languages unknown here) | Precedent, not competitor: 10M+ with no-ads + one-time unlock; proves offline utilities can be sustained without ads | Play listing (10M+, 4.6, 245K reviews) | High (installs/rating fetched) |
| 5 | **Organic Maps** | free, no ads, donations + Estonian OÜ | Yes | Yes (its site/localisation list; UI Arabic not separately checked) | Precedent: 12M+ installs (official news 2026-11-03) shows trust-first utility can scale, but donations + legal entity, 5M+ on Play | organicmaps.app news page | High (official page fetched) |
| 6 | **ZenConverter** | free, no ads, AGPL | Yes (INTERNET only for models/updates) | No (EN/zh/fr) | Closest functional rival; young (~1K+ installs), arm64-only | market dossier (Play/AppBrain, 2026-11-16) | Medium (third-party install counts) |
| 7 | **fwit Video Converter & Compressor** | free, no ads | Yes (claims no INTERNET permission) | No | Video-only but 10K+ installs prove the compress job; would outcompete Sigha on the single biggest MENA use case | market dossier (Play, 2026-11-16) | Medium |
| 8 | **Bdroid Audio Converter** | ads + IAP | Claimed | No | 10M+ but Play data-safety admits sharing Location/App activity → the trust gap Sigha can exploit | market dossier (Play, 2026-11-16) | Medium |
| 9 | **Inverse AI Video Converter** | ads + IAP | Not explicit | No | 10M+ downloads, 4.5; collects app activity/device IDs | market dossier | Medium |
| 10 | **Convertly: File Converter** | ads + IAP | "100% private & offline" claim | No | 211 installs; **Convertly is a registered US trademark** (MediaNews Group, Reg. 5616718) | market dossier | Medium |
| 11 | **MorphDrop** | free/no ads, Apache-2.0 | Yes | No (OCR EN + Indic) | 34-tool doc suite but ~150 downloads, no Play listing → proves features ≠ distribution | market dossier | Medium |
| 12 | **Convertio** | cloud upload; free tier + paid | **No** (cloud) | Site localised? (not verified) | Upload model: retains files server-side; 1 GB max; "in the cloud" | convertio.co (fetched: "1 GB maximum file size", "In the cloud") | High for upload model |
| 13 | **CloudConvert** | cloud; credits/subscription | **No** | Not verified | Credit pricing (1 credit ≈ 1 min; Office→PDF 2, PDF→Office 4); no offline | cloudconvert.com/pricing (fetched) | High |
| 14 | **MConverter** | cloud; ads + premium; "convert on-device if possible" toggle | Partial | Not verified | Ads gate downloads; large files locked ("Watch an ad" / premium) | mconverter.eu (fetched) | High for model |

**Precedent causes (tahqiq al-manat — why they won/lost):**

| Precedent | Outcome | Effective cause ('illa) | Condition for transfer |
|---|---|---|---|
| Simple Mobile Tools → ZipoApps (2023) → Fossify forks | Beloved no-ads utility sold; users migrated to a fork | Users could not control/audit ownership; when the owner changed, the *promise* changed with no technical barrier | Trust needs a licence + public source + a stated owner; otherwise the exit is the risk |
| Tachiyomi → Kakao cease-and-desist (Jan 2024) → Mihon | Project killed by legal exposure to third-party content | The app existed to consume unlicensed content; no amount of OSS purity protected it | Sigha's exposure is codec/model licences (already controlled) — but trademark and pack provenance must stay clean |
| Organic Maps | 12M+ installs (2026-11-03), no ads, donations | Offline maps have recurring server cost, solved by donations + a legal entity (OÜ); trust-first positioning buys press/community | Requires a donation rail + entity + patience; not a fast-growth model |
| Musicolet | 10M+, no ads, 245K reviews, IAP present | 100% offline utility, no server cost; a one-time unlock can fund a solo developer | Needs Play merchant registration (Iraq ✘ — F2) |
| Filee | 28k★ in 7 weeks; active releases; MIT | Breadth + local-only + zero-friction desktop gesture, amplified by trend aggregation | Desktop mechanism does not transfer; the *demand* does; the author can extend to web/mobile |
| fwit / Inverse AI / Bdroid | 10K–10M installs with ads/IAP | They sell *outcomes* (compress video, convert audio) in one step, not formats | Compression/quality presets must beat WhatsApp's own compression to be worth keeping |

---

## Qiyas cards (for the top strategic proposals)

### Qiyas 1 — Make compression/merge the first screen, not the last
- **Far' (proposal):** Sigha leads with outcome actions — "Compress video for WhatsApp", "Images → one PDF", "Compress PDF", "Merge PDFs" — reachable from the share sheet and the first screen; format conversion stays as the advanced surface.
- **Asl (precedent):** fwit Video Converter & Compressor (10K+, free, no-ads, offline, no INTERNET permission) and Inverse AI (10M+, 4.5) both sell *compression* as the product; CamScanner (500M+) sells *scan→PDF* as the product.
- **'Illa (effective cause):** users do not want "MP4" or "PDF" — they want *a file that will send/print*. Compression and merge are outcome jobs that recur (every video, every set of receipts), while format conversion is occasional and one-off. The share sheet is where the intent appears; an app that must be opened loses the moment.
- **Shurut (conditions):** (a) output must be as good as the incumbent's — a compression preset that produces larger/uglier files than WhatsApp's own has no cause and will be uninstalled; (b) target-size search must respect device thermals/time (cap 1080p, loop bitrate); (c) Arabic labels and honest size estimates shown before running; (d) tested on 3+ physical devices, including a 3 GB-RAM phone.
- **Ruling:** **ADOPT** — this is the only proposal that changes the app's market category from "another converter" to "the tool for the job the user has now".

### Qiyas 2 — One-time paid unlock, never ads, with the core free forever
- **Far':** free core (all conversions), paid one-time "OCR + document tools pack" (~$2–3) or "Pro" (batch auto-naming, no size limits); no ads, no analytics, no account.
- **Asl:** Musicolet (10M+, no ads, IAP present, 4.6/245K) — an offline utility with a one-time unlock; fwit (free, no ads) shows the zero-monetization version can reach 10K+ but gives no revenue path.
- **'Illa:** for a serverless offline utility, one-time unlock is the only price that matches the cost structure (no recurring cost ⇒ no subscription justification); ads would contradict the trust claim that is the product.
- **Shurut:** (a) Play merchant registration must exist — **fails today for Iraq** (`support.google.com/googleplay/android-developer/answer/9306917`: Iraq developer ✔, merchant ✘); (b) the free tier must be complete enough that the paid pack is an *extra* (OCR), not a hostage feature; (c) if external payment is used, Play policy forbids steering to it from inside the Play-distributed app.
- **Ruling:** **ADOPT WITH CONDITION (condition currently unmet)** — decide the entity first (F2). Until then: free + donation link outside the app listing; no price text.

### Qiyas 3 — Publish source + licence as the trust proof
- **Far':** public git repo with AGPL/Apache licence, reproducible release builds, transparency page (permissions, one network call, SHA-256s), and an "audit this app" section in the listing.
- **Asl:** Organic Maps (open source, 12M+ installs, donations) and the Fossify forks that replaced Simple Mobile Tools after its sale; F-Droid's whole model.
- **'Illa:** privacy claims are cheap; verifiability is expensive and therefore rare — the incumbents (10M+ ad/IAP apps) cannot copy it without changing their business. The SMT/Fossify case proves users *do* move to auditable alternatives when trust is broken.
- **Shurut:** (a) the licence must permit the promised freedom (AGPL is the strongest match); (b) reproducible build instructions must actually reproduce the APK hash (F13 must be fixed first); (c) no third-party component whose licence forbids redistribution (already handled for FFmpegKit/tessdata).
- **Ruling:** **ADOPT** — it is cheap, it is the only durable moat in the dossier, and it converts the "no data collected" claim into evidence.

---

## Accountant's view — unit economics (order-of-magnitude, assumptions flagged)

| Item | Figure | Basis |
|---|---|---|
| Play registration | $25 one-time | standard Play fee (not re-fetched here) |
| Revenue per paid user if unlocked | ~$2–3 one-time, developer keeps 70–85% | Play's 15/30% cut (policy, not re-fetched); **blocked for Iraq entity (F2)** |
| OCR pack hosting | 28.0 MB per download; 10k downloads ≈ 280 GB egress | `PackCatalog.kt:16-28`; Cloudflare R2 egress $0 → storage ~$0.4/month for 28 GB; GitHub raw = $0 but no SLA/CDN and abuse risk |
| Support | one solo maintainer, no crash analytics in app; Play vitals only | `build.gradle.kts:56-99` (no SDKs) |
| Distribution gate | 12 testers × 14 days + production review ~7 days | Play policy (dossier §2.11, official page) |
| Failure cost | 0 revenue until entity + review; debug APK unbillable | F1/F2 |

**Reading:** hosting is *not* the financial risk (packs are trivially cheap at realistic scale). The financial risk is **time + no collection rail**: 2–3 months of build/QA with zero revenue and no legal way to charge Iraqi users, and no analytics to measure whether the effort is working. Budget the first 90 days as a distribution/trust experiment, not a revenue plan; if revenue is intended, the entity decision is due before the Play listing, not after.

---

## 90-day roadmap with kill metrics

**Days 1–14 — make it shippable, not better.**
- Public repo + `LICENSE` (Qiyas 3) + `git init`; release signing config + AAB + per-ABI split; CI on `testDebugUnitTest` + `assembleRelease`; recompute and record APK hash (F13); remove dead catalogue formats or label them (F4); fix AR strings (F5); device QA on 3 physical devices (one API 26–28, one 29–33, one 34–36; one ≤3 GB RAM) with a scripted 20-conversion matrix; record FGS demo video for Play declaration.
- **Gate:** no device QA = no closed test. 200 conversions, ≥95% success on claimed routes, 0 fatal crashes.

**Days 15–45 — build the four outcome features (F3) and only those.**
- Multi-image → one PDF; PDF merge/split/compress; image compress-to-target-size; "Compress for WhatsApp" video preset with target-size search + share-sheet entry; camera scan-to-PDF if time allows (else v1.1).
- Add a capability/limits screen (F11) and the "what this route cannot do" notes (office text-only).
- **Gate:** each feature tested on 3 devices; compressed video must be smaller than the source and accepted by WhatsApp send; PDF merge preserves pages; no output over the user's chosen size target +2%.

**Days 46–75 — closed test and store readiness.**
- Recruit 12–20 Arabic-speaking testers (Play closed test 14 continuous days — the clock cannot be shortened); ship 3–4 weekly builds; fix top crash/confusion reports.
- Arabic + English listing: title ≤30 chars, screenshots in AR/EN, honest "offline" wording (F10), privacy policy URL, Data safety "No data collected", content rating, trademark/name clearance (human; "Sigha" is a generic Arabic word and hard to protect — decide before listing).
- **Gate to apply for production:** crash-free sessions ≥99.5%, testers complete the 4 outcome tasks unaided, rating-in-test ≥4/5, zero P0 bugs.

**Days 76–90 — launch and measure.**
- Production submission; AppGallery/direct APK as secondary channels; publish transparency page.
- **Kill metrics (decide continuation):**
  - **Install:** <500 organic installs in 30 days after an Arabic launch ⇒ distribution hypothesis wrong; stop feature work and reconsider channel (partnership with print shops/phone shops, share-sheet utility positioning).
  - **Retention:** D1 <25% or D7 <10% ⇒ the product is a one-shot; pivot to the outcome jobs only (compress/merge/scan) and cut conversion surface.
  - **Wedge proof:** <15% of 30-day actives use a document/compression outcome ⇒ the conversion-only thesis is dead; ≥35% ⇒ double down.
  - **Quality:** crash-free <99.5% or conversion failure >5% on claimed routes ⇒ no paid plan, fix first.
  - **Cost:** pack hosting >$20/month at 10k MAU or OCR completion <60% ⇒ move to R2/PAD or bundle a small fast OCR pack.
  - **Trust:** rating <4.0 with ≥50 ratings, or any "offline" complaint in reviews ⇒ rewrite claims (F4/F10) before any growth spend.

---

## Naive User verdict (Arabic-speaking, mid-range phone, 4 GB data/month)

**Would they install it?** Only if it is on Play and visible when they share a video or open a PDF — today it is a debug APK in a folder, so the honest answer is no, they cannot. If shipped as described in F1/F2 with an Arabic listing, yes: the empty state is clear ("Your files, converted on this phone", one obvious choose-files action), the Arabic UI is real, and "no upload, no ads" is a promise this audience actively wants.

**Would they keep it?** Only if the first job works. Their first jobs are not "convert DOCX to PDF" — they are *"make this video small enough to send"*, *"turn these 10 photos into one PDF for the print shop"*, *"merge these two PDFs"*. None exist. The second job after that is a TIFF/RTF/MOBI file that the app lists as supported and then answers with "These files have no target in common. Try files of the same kind" — that reads as a broken app, not a scoped one.

**Would they recommend it?** Yes, enthusiastically, *if* the four outcomes exist and the DOCX→PDF limitation is stated before conversion instead of discovered after; the trust story is the kind of thing this user repeats to family. No, if the first result is a plain-text PDF where a Word layout was expected — that is the "it ruined my document" story that ends word-of-mouth.

**The one thing that would make them uninstall:** tapping Convert on a Word file and getting a PDF that looks nothing like the document (no formatting, tables flattened, images missing) — `verification.md:124-126` says the office engines are text-only, but nothing in the UI warns before the conversion. The app must say "text-only PDF — layout, tables and images are not preserved" **before** the button, or this user never trusts it with anything private again.

---

## Top-5 prioritized actions

1. **Stop adding formats; add the four outcomes** (multi-image→one PDF, PDF merge/split/compress, image compress-to-size, WhatsApp video compress preset + share-sheet entry). This is the difference between a demo and a product (F3, Qiyas 1).
2. **Make the app shippable this sprint:** git repo + public licence + release signing + AAB/ABI split + CI + reproducible APK hash + privacy policy (F1, F9, F13). Nothing else can launch without it.
3. **Decide the entity/monetization now:** Iraq has no Play merchant registration (F2); choose foreign entity (Türkiye/UAE/Jordan) or free+donations, and keep "no ads, no analytics" absolute.
4. **Fix the honesty layer before any listing:** remove "every format has a working edge", hide or implement dead formats, warn before text-only office conversions, qualify the offline tagline, fix Arabic plurals and "الترجمة" (F4, F5, F10).
5. **Run the distribution plan with kill metrics:** device QA → 12-testers×14-days closed test → Arabic listing + AppGallery/direct APK → measure installs/retention/wedge-use at 30 days and be willing to kill or narrow (roadmap above).

---

## Limits of this report

- I did not run the Gradle build or the 131 tests; that is the Engineer seat's evidence. I did not run device QA or any Play submission (human-only).
- The APK hash/size above were measured at 07:34 local; the artifact was rebuilt during this review (07:28:14), so numbers may move again.
- Competitor install/rating figures are Play-listing headers fetched on 2026-11-17 (first-party); the "ads/IAP" badges for third parties come from the market dossier's 2026-11-16 reading (third-party trackers for some counts) and are marked Medium where so.
- A 28 MB "OCR pack download" is a network call; therefore "fully offline" should always be written as "conversion is offline; optional pack downloads once".
