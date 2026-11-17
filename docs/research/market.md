# Market dossier + Google Play constraints (2026) — offline Android file converter

**Author:** market-analyst (teammate)
**Date of research:** 2026-11-16 (all live fetches below were made on this date unless another date is shown)
**Scope:** competitor/precedent dossier, Google Play policy & technical constraints for a 2026 offline file-converter app, candidate-name screening, and differentiation recommendation.
**Method:** tools.web_search for discovery; exact pages fetched with PowerShell Invoke-WebRequest / Invoke-RestMethod (tools.web_fetch is broken in this environment — do not use it). GitHub facts come from the GitHub REST API. Third-party trackers (AppBrain, androidrank, chrome-stats, apppage) are marked as third-party and are not first-party Play numbers.
**Evidence rule:** facts only. Anything not directly observed is marked **UNVERIFIED** or **not visible**. No download counts, ratings, prices or licences below are invented. Short quotes are <=3 lines.

---

## 0. Executive summary (what the Lead needs)

1. **There is no Android port/fork of Filee (KnifeLemon/Filee) visible as of 2026-11-16.** GitHub repo search for "filee android" returns the MIT C# desktop app (28,026 stars, "263 formats"), a mirror, a macOS wrapper and a Docker web service — no Android app. The idea space is being attacked by independent apps, not by a Filee port.
2. **The Android market is split by silo, not by breadth.** The big installs sit in single-domain apps: Inverse AI video converter (10M+), Bdroid Audio Converter (10M+), Image-to-PDF (10M+). The all-in-one offline converters that approximate Filee's "breadth" idea are all small: ZenConverter (~1K+ installs, open source, 1,933 GitHub stars), MorphDrop (150+ downloads claimed, 34 tools), Convertly (~211 installs), fwit Video Converter & Compressor (10K+). **Breadth + quality + Arabic is an open lane, but distribution is the moat the incumbents have.**
3. **What clean apps compete on is trust, verbatim:** "no ads", "no watermark", "no upload", "no account", "no data collected". fwit's Play listing claims *no Internet permission*; ZenConverter's Play data-safety card says *No data collected / No data shared*; MorphDrop's site says *0 ads & trackers*. The incumbents monetise with ads + IAP and some collect/share location and app activity (Bdroid). **A no-ads, no-analytics, SAF-only app is a credible differentiator, not a marketing gimmick.**
4. **Google Play in Nov 2026 requires targetSdk 36.** Official page: "Starting August 31 2026: New apps and app updates must target Android 16 (API level 36) or higher". The extension window (to 2026-11-01) has already passed. Our sibling project targets 35 — **this must be raised to 36 before any upload.** (Verified 2026-11-16.)
5. **16 KB page size is now a compatibility requirement** for apps targeting API 35+ on 64-bit devices; the current page says update-blocking starts **2027-02-01**. Because we will ship native FFmpeg libs, 16 KB-aligned .so builds are a day-one build requirement, not a nice-to-have.
6. **Do not request MANAGE_EXTERNAL_STORAGE ("All files access").** Play permits it for file managers, backup/restore, anti-virus and document-management apps; a converter is not on the permitted list and would need a Permissions Declaration Form and review. Use SAF; it needs no storage permission at all.
7. **Do not request READ_MEDIA_IMAGES/READ_MEDIA_VIDEO.** Play's Photo and Video Permissions policy says apps with one-time/infrequent photo access must use the system photo picker; broad access is for gallery-type core use cases. SAF + picker is the compliant path, and it also removes a Data-safety headache.
8. **Foreground services:** the correct type for conversion is **mediaProcessing** (Android 15+): it is literally described as "operations on media assets, like converting media to different formats", 6 h/24 h limit. Play Console requires an FGS declaration per type **including a demo video link** when targeting Android 14+.
9. **Model packs are feasible under Play rules if they are data, not code.** Play prohibits downloading executable code (dex/JAR/.so) outside Play; Play Asset Delivery asset packs "compose assets … but no executable code". Own-host downloads of weight files are therefore possible, but the listing/offline claim must be accurate, and Play size limits apply only to Play-delivered content: base module 500 MB, single asset pack 1.5 GB, 4 GB install-time cumulative, 30 GB on-demand/fast-follow cumulative, 34 GB total.
10. **Name screening kills two candidates outright:** "Convertly" is a **registered US trademark** (MediaNews Group, Reg. 5616718, class 009) *and* an existing Play app "Convertly: File Converter"; "Formatly" is already an online converter (formatly.app) and an AI formatting product (formatlyapp.com). "Tahweel" collides with a live Arabic fintech app (tahweel.io, 4.5-star, 1.51K reviews). Direction recommendation in section 3; **final trademark clearance remains a human legal step — I do not pick the name.**
11. **New personal Play accounts are gated:** closed test with **12 testers opted-in for 14 continuous days** before production access (accounts created after 2023-11-13), then production review (usually <=7 days). Plan this into the launch schedule.

---

## 1. Competitive dossier (15 apps/projects)

Star/download/rating numbers are snapshots; "not visible" means the number was not shown by the source we could read. Third-party = AppBrain/androidrank/chrome-stats/apppage, explicitly not first-party Play data.

### 1.1 Summary table

| # | App / project | Type | Licence | Formats / scope | Offline claim | Monetisation | Rating / downloads (source) | Key weakness |
|---|---|---|---|---|---|---|---|---|
| 1 | **Filee** (KnifeLemon/Filee) | Windows desktop (C#), the inspiration | MIT | "Convert 263 formats offline by drag & drop — images, PDF, Word, Excel, PowerPoint, HWP/HWPX, video, audio, e-books and archives" | offline, "No upload, no Office needed" | open source/free | **28,026 GitHub stars** (GitHub API, 2026-11-16) | Desktop only; no Android app; no mobile UX precedent |
| 2 | **ZenConverter** (Jasonzhu1207/ZenConverter) | Android, Kotlin/Compose, OSS | **AGPL-3.0** | video (MP4/MKV/MOV/WebM), video→GIF, contact sheet, audio (MP3/M4A/WAV/FLAC/WMA/OPUS), images (JPG/PNG/WEBP/GIF/HEIC/ICO→PDF), PDF compress/merge/text, DOCX/PPTX/XLSX→PDF/TXT/MD, fonts, subtitles, EXIF strip; AI upscale (Real-ESRGAN) + frame interpolation (RIFE) via NCNN/Vulkan | "no network transfer for conversion work"; INTERNET only for update checks and on-demand model/font downloads; SAF + foreground service | free, no ads | 1,933 GitHub stars; Play 0.2.4, dev DogeNet LLC; **1K+ installs / 1.6k** (AppBrain, third-party); **no ratings yet** (AppBrain); Play data safety: **No data collected / No data shared** | arm64-v8a only; AI routes experimental; very young (first push 2026-07); small install base |
| 3 | **MorphDrop** (Rajendra0309/morphdrop-android) | Android, Kotlin, OSS | **Apache-2.0** | 34 document/image tools: PDF↔TXT/DOCX/XLSX/CSV, image→PDF, Markdown→PDF, batch OCR (incl. Devanagari), sign PDF, compress to target KB, watermark, QR; no video/audio | "100% offline … no uploads, no accounts, no paywalls"; "Works in airplane mode" | free, no ads, open source | 17 GitHub stars (API; site says 16); **150+ downloads claimed** on morphdrop.vercel.app; no Play listing found (searches 2026-11-16) | doc/image only; tiny distribution; no media conversion |
| 4 | **Video Converter & Compressor** (fwit, com.fwit.videoconverter) | Android, published OSS source | source on Codeberg (fabianwimberger/video-converter-android); **licence not declared in repo metadata — UNVERIFIED** | H.264/H.265/AV1, up to 4K, device or FFmpeg encoder, bitrate, batch + background, history | listing: "Everything runs 100% offline … no account, no uploads, no ads, no watermarks"; claims **no Internet permission** (vendor claim) | free, no ads | **10K+ downloads** (Play page, 2026-11-16); updated 2026-11-13; rating not visible; data safety: **No data collected / No data shared** | video-only; small; no docs/images; source licence unclear |
| 5 | **Morph** (RizkyFauzi06/Morph) | Android, Kotlin, OSS | **GPL-3.0** | "Convert videos, audio, images, and documents locally"; background queue; system Share-menu integration; "100% offline" | explicit "100% offline" | free/OSS | 19 GitHub stars; pushed 2026-11-15 | no visible store presence; unproven UX/QA |
| 6 | **Video Transcoder** (brarcher/video-transcoder) | Android, OSS (FFmpeg) | **GPL-3.0** | AVI, FLV, GIF, Matroska, MP3, MP4, Ogg, Opus, WebM; H.264, MPEG-1/2/4, VP8, VP9, Xvid | "requires very few permissions, and never attempts to access the Internet" | free/OSS | **587 GitHub stars**, 72 forks, **71 open issues**, created 2018-01 | effectively unmaintained (secondary source says it is seeking a volunteer maintainer — UNVERIFIED from repo itself); dated UI; media only |
| 7 | **Simple Audio Converter** (F-Droid) | Android, Flutter/Dart, OSS | **MPL-2.0** | audio conversion only | F-Droid app; no network permission declared beyond a custom receiver permission | free | 32 GitHub stars; F-Droid v1.0.5 (2026-10-06), Android 7.0+, APK 39–53 MB per ABI | audio-only; very large APK for one job; F-Droid-only distribution |
| 8 | **FFShare** (F-Droid) | Android, OSS | **GPL-3.0-only** | compress image/video/audio through ffmpeg before sharing | local processing | free | F-Droid listing (fetched 2026-11-16); install count not visible | share-sheet utility, not a converter UI; not a Filee-like hub |
| 9 | **Audio Converter (MP3 AAC OPUS)** (Bdroid, com.bdroid.audiomediaconverter) | Android, closed source | proprietary | MP3, AAC, M4A, OGG, OPUS, WAV, FLAC, WMA, AMR (NB/WB), AC3; batch; cut; ringtone; extract from video; 8–320 kb/s; CBR/VBR/ABR | "Works fully offline"; media access only for file selection | **avoidance-heavy:** "Contains ads", "In-app purchases" | **10M+ downloads, 4.69/5 from 32,307 ratings** (Play page, 2026-11-16); updated 2026-11-12; data safety: **may share Location, App activity and 2 others with third parties**, collects same, can't be deleted | ads + IAP; shares location/app-activity with third parties; audio-only |
| 10 | **Video Converter, Compressor** (Inverse AI, com.inverseai.video_converter) | Android, closed source | proprietary | MP4, MKV, AVI, 3GP, MOV, MTS, MPEG, MPG, WMV, M4V, VOB, FLV; → MP3, AAC, AC3, OGG, M4A, WAV; compress, resolution/FPS/bitrate | listing presents conversion/compression; on-device claim not explicit in the snippet we read | **Contains ads, In-app purchases** | **10M+ downloads, 4.51/5 from 216,374 ratings** (Play, 2026-11-16); 216,006 ratings / 4.52 (androidrank, 2026-11-01); updated 2026-11-01; data safety: no third-party sharing; **may collect App activity, App info and performance, Device or other IDs** | ads + IAP; collects device/app data; video+audio only; no documents |
| 11 | **Image to PDF: PDF Converter** (TrustedOffice, com.readpdf.pdfreader.pdfviewer) | Android, closed source | proprietary | image→PDF, scan, PDF reader/editor, merge/split, PDF→Word claims | claims offline access for reading/conversion | **Contains ads, In-app purchases** | **10M+ downloads, 3.9/5 from 187K reviews** (Play, 2026-11-16) | low 3.9 rating; ads/IAP; narrow (image→PDF-centric) |
| 12 | **Video Converter & Compressor** (Androho, com.androho.simplevideoconverter) | Android, OSS | **GPL** (repo: Androho24/SimpleVideoConverter) | MOV/MKV/MP4/TS/AVI; MP3/AC3/Opus; trim, compress, size estimate; "no watermarks" | local FFmpeg conversion | **ads + IAP "Remove Ads Permanently"** | Play 1K+ (secondary article, 2026-10-28); 4,564 downloads estimate (chrome-stats, data 2026-10-19); v2.2.0 updated 2026-09-27; permissions seen: READ_MEDIA_AUDIO, FOREGROUND_SERVICE_MEDIA_PROCESSING, READ_BASIC_PHONE_STATE, AD_ID, ACCESS_ADSERVICES_AD_ID, INTERNET | ads; tiny install base; asks phone-state + ad-ID permissions |
| 13 | **Convertly: File Converter** (com.app.convertlyy, bigame soft) | Android, closed source | proprietary | video MP4/MKV/MOV/AVI/WEBM/FLV/GIF/3GP/TS; audio 11 formats; images JPG/PNG/WEBP/GIF/BMP/TIFF/HEIC; docs DOCX/PPTX/XLSX/TXT/EPUB/RTF/CSV/ODT/ODS/ODP→PDF, PDF→TXT/images; OCR, QR, archive extract | "100% PRIVATE & OFFLINE … no internet required for core conversions" | **Contains ads, In-app purchases** | **211 installs** (apppage, third-party); released 2026-07-25, last update 2026-09-27, Android 7.0+ | tiny install base; ads/IAP; **name is a registered US trademark** (see section 3) |
| 14 | **HEIC to JPG Converter & PDF** (ARK, com.ark.imageconverterproapp) | Android, closed source | proprietary | JPG, PNG, WebP, HEIC, AVIF, TIFF, BMP, ICO, SVG; batch; PDF; on-device AI background removal/upscale/OCR claims | "operate entirely on your phone … nothing ever leaves your device" | **Contains ads, In-app purchases** (lifetime or subscription) | **100+ downloads** (Play, 2026-11-16); updated 2026-07-27; data safety: **may collect Photos and videos and Device or other IDs** | tiny; privacy claim contradicted by its own data-safety disclosure |
| 15 | **Filee mirror/derivatives** (WDHUS4G/Filee mirror 137★; yavuzmeteafsar/molt-mac macOS wrapper; scanek/filee-converter Docker web API) | non-Android | MIT (mirror), others mixed | inherits Filee formats | inherits | free | GitHub search 2026-11-16 | none of these is an Android app |

Additional OSS apps seen but not dossiers (too small / not enough evidence): LinuxKernel44/OpenMultiConverter (GPL-3.0, offline audio), nasahackerhq/ConvertIt (Apache-2.0, ad-free audio converter), as-repo1/Transmute (MIT, documents+images), gueboba/conv-android (MIT, 138 KB offline converter), File-Flip (MIT).

### 1.2 Notes that matter for our product decisions

* **Filee itself (the idea source).** GitHub API description: "Free, open-source file converter. Convert 263 formats offline by drag & drop — images, PDF, Word, Excel, PowerPoint, HWP/HWPX, video, audio, e-books and archives. No upload, no Office needed." Licence MIT; 28,026 stars; C#. *(Discrepancy to log: our brief says ~270 formats; the repo description at fetch time says 263.)* MIT permits reuse, but per the project rules we are writing original Kotlin and will not copy Filee's code; any reused expression/format table needs MIT attribution (checked by legal-clearance).
* **No Android Filee port found.** Searches (GitHub repo search "filee android", "filee file converter"; Play search for "Filee file converter") did not surface an Android app. Separately, **fileee.com is an existing document-management product with Android and iOS apps** (Play rating 4.1 / 533 reviews per its own download page) — a name-collision risk, not a code risk.
* **ZenConverter is the closest thing to a "Filee for Android".** It is the only app we found that combines broad media + documents + PDF + on-demand AI models, and it explicitly markets the exact trust posture (SAF, no uploads, no ads, no account, INTERNET only for update/model download). It is also AGPL-3.0 — useful as a *feature benchmark*, dangerous as a *code source* unless the whole app is AGPL (decision for legal-clearance, not mine). Its Play data-safety card says "No data collected" — a useful precedent for our form.
* **Its model packaging is directly relevant to our "downloadable model packs" requirement:** models are downloaded on demand and **SHA-256 checked**, per README. It does get its AI models from a non-Play host, which is allowed for data files (see section 2.7).
* **MorphDrop** is the best evidence that a **34-tool document suite** can be built Apache-2.0 with PDFBox and ship at ~28 MB, with no ads. It also shows the ceiling: 150+ downloads and no Play listing. Great feature reference, weak market proof.
* **The incumbents' weakness is our opening:** Bdroid (10M+) shares *Location and App activity with third parties*; Inverse AI (10M+) collects *App activity, performance, Device IDs*; TrustedOffice (10M+) sits at a **3.9** rating; Androho asks for phone-state + ad-ID. Meanwhile the clean apps are tiny. That is the classic "trust gap" to exploit, but also proof that clean apps do not automatically win distribution.
* **Second-order names already occupied in the same space:** "Convertly: File Converter" (all-in-one offline converter!) and "Formatly" (online converter) shows the naming lane is crowded with "-ly" converter brands.

### 1.3 Precedents: what the market rewards

(These are signals from store listings, data-safety cards and third-party trackers — **not** a user survey; treat as directional.)

1. **Explicit no-ads / no-watermark / no-upload / no-account copy is used as a headline by every clean competitor** (fwit, ZenConverter, MorphDrop, Androho "No watermarks", Convertly "100% PRIVATE & OFFLINE"). Clean-app marketing converges on the same promise list; users can compare apps on it.
2. **Batch + background processing is table stakes** in every >=10K-install converter we read (fwit, Bdroid, Inverse AI, Convertly). MorphDrop and ZenConverter both added batch queues. Our brief's drag/drop-and-queue model matches market expectations.
3. **On-demand model downloads are acceptable to users *if* the base app stays offline** — ZenConverter proves the pattern (SAF + on-demand SHA-256-checked models + INTERNET only for those/updates). fwit goes further and ships without Internet permission, which is the strongest verifiable trust signal we saw in the Play listings.
4. **Format breadth is a desktop demand, not yet an Android reality.** The only high-install Play apps are single-domain; breadth lives in tiny apps. Interpreted conservatively: breadth is a real gap but unproven on Android at scale — do not assume 263 formats buys installs by itself.
5. **Data-safety "No data collected" is achievable and visible.** Both fwit and ZenConverter publish it. That gives us a concrete store-listing differentiator against 10M+ apps whose cards admit collection/sharing.

### 1.4 Precedents: what the market punishes

1. **Ads/IAP at scale correlates with poor sentiment in the numbers we can see** — e.g. Image to PDF: 10M+ installs but **3.9/5**; Inverse AI: 10M+ but **20,811 one-star ratings of 216,006** (androidrank, 2026-11-01, third-party). Causation is not proven, but the pattern is consistent with ad/paywall complaints.
2. **Data collection contradicts the offline promise.** ARK's listing says "nothing ever leaves your device" while its Play data-safety card says it **may collect Photos and videos and Device or other IDs** — exactly the contradiction Play's metadata/deceptive-behaviour policies exist to punish. We must not repeat it.
3. **Unmaintained OSS loses.** brarcher/video-transcoder: 587 stars, 71 open issues, no recent release cadence — a common "was good in 2019" converter. Secondary article says it is looking for a maintainer (UNVERIFIED).
4. **Large APKs for narrow jobs hurt.** Simple Audio Converter: 39–53 MB per ABI for audio-only. Model packs may be large, but the base app must stay small and the download must be explicit/optional.
5. **The "offline" claim is scrutinised.** A 2026-10-28 secondary review article explicitly notes that offline claims are "vendor claims, not independent audits" and calls out apps that still talk to the network. Our listing must be literally true (see section 2.6).

---

## 2. Google Play constraints for a 2026 offline converter

All pages below were fetched **2026-11-16** unless noted. "Official" = Google-owned domains.

### 2.1 Target API level — **must be 36 now**

Source: https://developer.android.com/google/play/requirements/target-sdk (official, fetched 2026-11-16).

> "Starting August 31 2026: New apps and app updates must target Android 16 (API level 36) or higher to be submitted to Google Play"

Also on the page: an extension to **2026-11-01** was offered (already in the past as of today); exceptions are permanently-private/internal apps; out-of-date apps lose availability to new users on newer Android versions; Wear OS / Android TV / Automotive / XR have their own (lower) requirements.

**Consequences for us:** targetSdk = 36 and compileSdk = 36. The sibling reference project currently builds AGP 8.7.3 / Kotlin 2.0.21 / compileSdk 35. Whether AGP 8.7.3 can cleanly compileSdk 36 is **UNVERIFIED** in this research (the AGP release-notes page did not expose its compatibility table to our text extraction; latest AGP listed there is 9.5.0, Oct 2026). Action: android-stack must verify/adjust the build stack; assume an AGP bump is needed. Also note API 36 brings Android 16 behaviour changes into scope for targetSdk 36 (edge-to-edge, predictive back, etc.) — covered by the stack task, not detailed here.

### 2.2 16 KB memory page sizes

Source: https://developer.android.com/guide/practices/page-sizes (official, fetched 2026-11-16).

> "all apps targeting Android 15 (API level 35) and higher must support 16 KB memory page sizes on 64-bit devices on Google Play. Starting February 1, 2027, if your app updates don't support 16 KB memory page sizes, you won't be able to release these updates."

Note: the current page text no longer states the 2025-11-01 "new apps" date that older summaries cite; the authoritative current wording is the quote above. **Treat 16 KB support as required now**; the hard update block is 2027-02-01. Any app with native code must rebuild for 16 KB; Kotlin/Java-only apps are already compliant.

**Consequences for us:** every native .so we ship (FFmpeg/FFmpegKit alternative, codec libs, OCR/upscale native kernels) must be 16 KB-aligned. This is a real risk for prebuilt FFmpeg binaries — many are not aligned. Put it in the acceptance criteria for android-stack's library choice.

### 2.3 Storage & media permissions — use SAF, avoid All-files access

**All files access (MANAGE_EXTERNAL_STORAGE)** — source: https://support.google.com/googleplay/android-developer/answer/10467955 (official, fetched 2026-11-16). Permitted uses are "file managers, backup and restore apps, anti-virus apps, and document management apps"; it requires the Permissions Declaration Form and Google Play approval; a converter is not listed. It notes non-compliance requires removing the permission from the manifest.

**Photo and Video Permissions policy** — source: https://support.google.com/googleplay/android-developer/answer/14115180 (official, fetched 2026-11-16). READ_MEDIA_IMAGES/READ_MEDIA_VIDEO are restricted: "Apps with a one-time or limited use of photo and video files are requested to use a system picker such as the Android photo picker"; broad access is for "photo/video managers and gallery apps" and needs an access review. Since Aug 2024 the request has been to remove the permissions where access is not core; converters that only need the files the user selects do not qualify.

**READ_MEDIA_AUDIO** does not appear in that policy page (the page covers images/video only), but the same logic applies and SAF avoids it anyway. **Decision: SAF only** — ACTION_OPEN_DOCUMENT (with CATEGORY_OPENABLE and MIME/EXTRA_MIME_TYPES), ACTION_OPEN_DOCUMENT_TREE only if a folder workflow is essential, ACTION_CREATE_DOCUMENT for outputs, takePersistableUriPermission for long jobs. No storage permission in the manifest. For images from the gallery, the Android photo picker is the policy-preferred entry point.

**POST_NOTIFICATIONS** — source: https://developer.android.com/develop/ui/views/notifications/notification-permission (official, fetched 2026-11-16). Android 13 (API 33)+ requires the runtime notification permission for non-exempt notifications, **including foreground-service notifications**. We need it for conversion progress; ask contextually when the user starts a long conversion, not at first launch. Declare POST_NOTIFICATIONS and handle denial (FGS can still start but its notification is suppressed/affected — test on API 33+).

### 2.4 Foreground service type — mediaProcessing is the designed fit

Sources: https://developer.android.com/develop/background-work/services/fg-service-types (official, fetched 2026-11-16) and https://support.google.com/googleplay/android-developer/answer/13392821 (official, fetched 2026-11-16).

* mediaProcessing: "Service for performing time-consuming operations on media assets, like converting media to different formats." Permission: FOREGROUND_SERVICE_MEDIA_PROCESSING; constant: FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING; runtime prerequisites: none; **time limit 6 hours out of every 24** (shared across the app's mediaProcessing services); must call stopForeground()/stopSelf() when done or the system calls onTimeout() and can ANR. Android 15+ (API 35) is where this type exists; for API 26–34 use dataSync (declared "Local file processing" is explicitly listed under dataSync) or shortService for short jobs.
* Play Console declaration: "When your apps target Android 14 and above, you'll need to declare any foreground service types that you use in a new declaration on the App content page." For each type you must provide a description, user impact if deferred/interrupted, **a link to a video demonstrating the feature**, and a use case. **Budget time to record this video before submission.**

### 2.5 Data safety form — "No data collected" is achievable but must be true

Sources: https://support.google.com/googleplay/android-developer/answer/10787469 and https://developer.android.com/privacy-and-security/declare-data-use (official, fetched 2026-11-16).

* Every app must complete the form, **even apps that collect no user data**, and must provide a privacy-policy link: "Even developers with apps that do not collect any user data must complete this form and provide a link to their privacy policy. In this case, the completed form and privacy policy can indicate that no user data is collected or shared."
* Third-party SDKs count: if an SDK collects or shares data, it must be declared. This is why our no-analytics stance is not just ethics — it simplifies compliance.
* Precedent: fwit and ZenConverter both publish "No data collected / No data shared" (Play pages, 2026-11-16).
* **Nuance (UNVERIFIED, needs decision):** if model packs are downloaded from our host, the server sees an IP address in standard logs. Play's Data-safety guidance is about data the app/SDK collects and shares; a plain file download with no identifiers is normally treated as not collecting user data, but this should be confirmed against Play guidance and covered in the privacy policy. If we add crash reporting/analytics later, the form must change (Crash logs / Diagnostics / Device or other IDs).
* Avoid the ARK failure mode: never write "nothing leaves your device" in the listing while the data-safety form declares collection. That is exactly the metadata/deceptive-behaviour mismatch Play enforces.

### 2.6 "Offline" claims, metadata accuracy and deceptive behaviour

Sources: https://support.google.com/googleplay/android-developer/answer/9898842 (Metadata policy) and https://support.google.com/googleplay/android-developer/answer/9888077 (Deceptive Behavior), fetched 2026-11-16.

* Metadata: "You must ensure your app's metadata accurately reflects its functionality"; misleading/inaccurate descriptions are prohibited. App **title max 30 characters**; no emojis/repeated special chars/ALL CAPS (brand names excepted); no text in icon that implies store performance/price.
* Deceptive Behavior: "All metadata, including the store listing, screenshots, and title, must precisely reflect the app's functionality." So "100% offline" is only defensible if the shipped app performs no mandatory network operation. If the base app can convert everything without network and model packs are optional downloads, the accurate phrasing is something like: "converts on-device; optional model packs download once" — the Lead/legal should approve exact wording. Do not say "no Internet permission" unless the manifest truly has none.

### 2.7 Model downloads, app size and the executable-code line

Sources (official, fetched 2026-11-16): https://support.google.com/googleplay/android-developer/answer/9888379 (Device and Network Abuse), https://developer.android.com/guide/playcore/asset-delivery (Play Asset Delivery), https://developer.android.com/guide/app-bundle (Android App Bundle), https://support.google.com/googleplay/android-developer/answer/9859372 (size limits, via search snippet).

* **Hard line:** "an app may not download executable code (such as dex, JAR, .so files) from a source other than Google Play." Interpreted-language code loaded at runtime (JS/Python/Lua) also must not enable policy violations. Therefore: model **weights/config data** may be downloaded; **native libs/plugins/dex must ship in the app** (or via Play), never fetched at runtime. If we ever add downloadable OCR/upscale engines, they must be data-driven interpreters, not .so downloads.
* **PAD:** asset packs are "composed of assets (such as textures, shaders, and sounds), but no executable code", with install-time, fast-follow, on-demand modes; Play hosts and serves them (no CDN needed). PAD is documented as a games mechanism, but it is the Play-sanctioned way to ship big optional assets. If we use PAD, verify current Console eligibility for non-game apps (UNVERIFIED — docs frame it for games).
* **Size limits (Play-delivered):** base module 500 MB; individual feature module 500 MB; individual asset pack 1.5 GB; cumulative all modules + install-time packs 4 GB; cumulative on-demand/fast-follow packs 30 GB; total max 34 GB. Apps >1 GB must have minSdk >=21; >200 MB triggers a mobile-data install warning dialog. AAB is mandatory for new apps since Aug 2021.
* **Own-host downloads:** no Play size limit applies because Play is not delivering the bytes; the constraints are the executable-code rule, accurate listing/metadata, privacy policy/data safety, and (practically) user consent, resumability, SHA-256 verification, storage checks. **UNVERIFIED:** there is no documented per-download cap in the policies reviewed; do not promise users unlimited sizes without a storage/UX design.
* **Practical recommendation:** keep base AAB small (SAF-only, no bundled models) and deliver models as (a) PAD asset packs if eligible, or (b) first-run/on-demand download of data files from our host with SHA-256 and clear disclosure, or (c) import of a model pack file the user already has (SAF). Option (c) is the strongest trust story and needs no network permission at all for those users.

### 2.8 Ads (only if we monetise with ads)

Source: https://support.google.com/googleplay/android-developer/answer/9857753 (official, fetched 2026-11-16).

* Ads are treated as part of the app and must follow all policies; they must match the app's content rating; "Disruptive ads" are prohibited — no full-screen ad without a clear dismiss, no false dismiss buttons, no ads outside the app, no forcing clicks/data entry to use the app; the page's examples require a skip for a full-screen ad within 15 seconds and forbid repeated interstitials after each user action ("Made for Ads").
* If ads are added: ad SDK data collection must appear in Data safety; AD_ID/ACCESS_ADSERVICES_AD_ID permissions and consent (UMP) come with it; families/children rules apply if the target audience includes children. Cleaning this up later is painful — decide monetisation before building.
* **Our default recommendation (for the Lead, not my decision):** no ads, no third-party SDKs, so Data safety can say "No data collected" and the offline claim is clean; monetise (if at all) via optional paid model packs or donations without tracking. This matches the strongest competitor trust signals.

### 2.9 Content rating

Source: https://support.google.com/googleplay/android-developer/answer/9898843 (official, fetched 2026-11-16). All apps must have an IARC rating: "All apps must have a content rating from the IARC to be on Google Play"; the questionnaire must be completed accurately and re-done if content changes; ads must not be significantly more mature than the app's content. For a converter with no user-generated content and no ads, the questionnaire is short and typically yields a low (Everyone/3+) rating — the exact rating depends on our answers, so do not promise a rating before filling it in.

### 2.10 AI-generated content policy — likely out of scope for OCR/upscale, but watch it

Source: https://support.google.com/googleplay/android-developer/answer/14094294 (official, fetched 2026-11-16).

* The policy covers "generative AI apps": text-to-text chatbots, text/voice-to-image and image-to-image apps that use AI to generate images, voice/video recordings of real people.
* It is explicitly **not intended to cover** some limited-scope AI apps, including "Productivity apps that use AI to improve an existing feature" and summarisation of non-AI content.
* **Our reading (not legal advice):** on-device OCR (text extraction), upscaling/denoising and format conversion are deterministic transformations of the user's own file, not prompt-driven generation — so this policy is likely not applicable. If we add generative features (e.g. AI background generation, generative fill, AI rewriting of documents), the policy applies and needs a separate review. Confirm during Play Console declaration.

### 2.11 Launch gates (often missed)

* **Closed testing for new personal developer accounts** — source: https://support.google.com/googleplay/android-developer/answer/14151465 (official, fetched 2026-11-16). Personal accounts created after 2023-11-13 must run a closed test with **at least 12 testers opted in continuously for at least 14 days** before applying for production access; production review "usually takes seven days or less". If the Lead's Play account is personal and new, the calendar has ~3 weeks of gate before public launch. (Organisation accounts are not covered by this article.)
* **Android developer verification** — source: https://developer.android.com/developer-verification (official, fetched 2026-11-16): "Effective September 30, 2026 Protections begin for users installing apps from participating stores in select regions on certified Android devices. We'll continue rolling out user protections globally in 2027." Play "automatically registers 99% of apps" per the page; this is mostly a sideload/distribution concern, but register early if we distribute outside Play.
* **No high-risk permission declaration needed** if we ship with zero storage/media permissions (SAF only) — that is a submission-time advantage.

### 2.12 Permission / declaration checklist for our app (proposed)

| Item | Decision | Manifest / Console action | Policy source |
|---|---|---|---|
| Storage | SAF only; no MANAGE_EXTERNAL_STORAGE | open/create document intents only | 2.3 |
| Photos/videos | photo picker / SAF; no READ_MEDIA_IMAGES or READ_MEDIA_VIDEO | none | 2.3 |
| Audio | SAF; no READ_MEDIA_AUDIO | none | 2.3 |
| Notifications | request on first long job | POST_NOTIFICATIONS runtime | 2.3 |
| Conversion in background | mediaProcessing FGS (API 35+), dataSync/shortService fallback | FOREGROUND_SERVICE_MEDIA_PROCESSING, service type, Play Console FGS declaration + demo video | 2.4 |
| Network | only optional model-pack download/update check | INTERNET only if we ship download; else none | 2.6/2.7 |
| Models | data files only; SHA-256; explicit consent; storage check | never .so/dex downloads | 2.7 |
| Data safety | "No data collected/shared" if no analytics | privacy policy URL; keep truthful if SDKs change | 2.5 |
| Ads | none by default | App content: "No ads"; content rating unaffected | 2.8 |
| Content rating | IARC questionnaire | complete before submission | 2.9 |
| AI features | OCR/upscale = transformation → likely outside AI content policy | confirm in Console declaration | 2.10 |
| Launch | personal account? plan 12 testers x 14 days | closed test + production application | 2.11 |

---

## 3. Candidate-name screening (spot check, **not** trademark clearance)

Method: Play/App Store/web searches plus a quick Justia (USPTO-mirror) check per candidate, 2026-11-16. **This is not a legal search.** A proper clearance requires a trademark attorney searching USPTO, EUIPO, WIPO and the relevant national registries (Iraq: Ministry of Trade / trademark registrar), plus domain and store-name checks. **Final legal check is a human step; I do not pick the name.**

| Candidate | Meaning | Existing apps / products with the name (evidence) | Trademark signals (quick check) | Risk assessment |
|---|---|---|---|---|
| **Sigha** (صيغة, "format") | descriptive Arabic noun for "format" | No exact app found. Closest: Grupo Sigha, a Brazilian IT/ERP company (sigha.com.br, LinkedIn, founded 1983); "SIGA Mobile" airport-ops app on Play; "SISGHA" is a registered US mark for appliances (class 011, Reg. 6538305, multiple classes) | No exact "SIGHA" software mark surfaced in the quick check; the generic Arabic word is weak as a distinctive mark | **Moderate-low** conflict risk, but the word is generic/descriptive in Arabic and may be hard to protect; check for Arabic-script marks and app-store similarity |
| **Tabdeel** (تبديل, "change/swap") | generic Arabic noun for "exchange" | "Tabdeel - Exchange Goods" Android app (Alphatech Solutions, 10+ installs, Play; also on Apple App Store id6464123721) | No mark surfaced; generic word | **Moderate**: same name already used for an Android app, different category (barter); descriptive in Arabic |
| **Tahweel** (تحويل, "transfer/conversion") | generic Arabic noun; also strongly associated with money transfer | **"Tahweel" financial app** (package tahweel.io), Arabic listing, **4.5 stars, 1.51K reviews**, Tahweel Financial Services LLC (Sheridan, WY) | Established live brand in Arabic fintech; high collision with an app name in a different but high-trust category | **High** — avoid as the primary brand; the word is also generic, so it is hard to own |
| **Formatly** | English "-ly" converter-style brand | **formatly.app — "Free Online File Converter"** (same problem domain); **formatlyapp.com — AI academic document formatting** product; the Play search surfaced no exact app, but web brands exist | No US mark surfaced in quick check; "-ly" converter names are crowded | **High** in the same product category (direct online-converter collision) |
| **Convertly** | English "-ly" converter-style brand | **"Convertly: File Converter"** on Google Play (com.app.convertlyy, bigame soft, 211 installs, ads+IAP, all-in-one offline converter — a direct competitor) | **CONVERTLY is a registered US trademark**: MediaNews Group, Inc., Reg. 5616718, Serial 87876131, class 009, registered 2018-11-27 (Justia) | **Very high — reject**: registered mark + same-category app |
| **Qalib** (قالب, "template/mold") | Arabic noun for template; less tied to conversion | **qalib.qa** — Qatar-based software agency doing Arabic RTL mobile apps (web) | No software mark surfaced in quick check | **Moderate-low** direct conflict, but semantically "template", not conversion; also an existing Arabic tech brand |

**Two directions to consider (not a decision):**
* **Direction A — Arabic-first name, not yet used as an app brand.** A short Arabic word/coinage that means "format/shape" or a novel compound, presented bilingually (Arabic + Latin transliteration), e.g. build around the "Sigha" family (صيغة) but only after a full Arabic-script + app-store + domain clearance; or an invented Arabic-coined word that is easy to type in Latin. Advantage: fits the Arabic-first UI edge; risk: weak distinctiveness if the word is generic.
* **Direction B — invented/coined Latin name + Arabic descriptor subtitle in the listing.** Avoid the crowded "-ly" converter family and any name already in file-conversion. Advantage: defensible mark, clean store title; risk: less immediately meaningful to Arabic speakers — mitigate with the Arabic subtitle and icon.
* **Constraints either way:** Play title max 30 characters; no keyword stuffing; avoid names that imply "Filee" or "fileee" (existing commercial app fileee.com); check the Arabic transliteration for unfortunate readings and for existing Arabic apps before committing.

---

## 4. Differentiation recommendation (3 concrete edges)

These are grounded in what the 15 dossiers show; each edge names the competitor gap it exploits.

### Edge 1 — Arabic-first, RTL-native UX (the clearest, least contested gap)
None of the apps whose listings/READMEs we inspected advertises an Arabic interface: ZenConverter offers EN / Simplified & Traditional Chinese / French; fwit is English; Bdroid and Inverse AI listings are English; MorphDrop's OCR supports English + Hindi/Marathi; Convertly is English. For an Iraq/MENA launch, a fully RTL, Arabic-first app with Arabic filenames/metadata handled correctly is a visible differentiator against every major converter we found. (Caveat: "not visible in the listings we read" is not proof no Arabic UI exists — spot-check the store in Arabic before claiming "first".)

### Edge 2 — One offline hub for documents + media + images (Filee's breadth idea, currently unowned on Android)
The Android market is siloed by domain: 10M+ video converters do video, 10M+ audio converters do audio, 10M+ image-to-PDF apps do image→PDF. The only all-in-one offline apps are tiny (ZenConverter ~1K+, fwit 10K+ but video-only, MorphDrop 150+ docs-only, Convertly 211). Filee's 28k stars show desktop demand for breadth. A small, fast, offline hub that covers **office-simple documents + PDF + images + audio + common video containers** — with clear per-route limits instead of vague "263 formats" claims — is a real gap. Do not promise Filee's 263 formats on mobile; promise verified routes with documented limits (ZenConverter's public support-matrix approach is the right precedent: "supported routes are verified on a physical Android device, and their known limits are written down instead of hidden").

### Edge 3 — Verifiable trust: no ads, no analytics, SAF-only, optional model packs with checksums
Incumbents monetise with ads + IAP and some admit data collection/sharing; the clean apps are small but are winning exactly the trust-minded user (fwit and ZenConverter both publish "No data collected"). The differentiator is to make trust **verifiable, not just claimed**: SAF-only permission set (no storage/media permissions), zero third-party SDKs, Data safety "No data collected", no ads, and model packs that are (a) data-only, (b) SHA-256 checked, (c) optionally importable from local storage so the base app can stay network-free, and (d) free for commercial use as the brief requires. Publish a transparency page (permissions, network calls, model licences, checksums) — MorphDrop and fwit market the idea, but none of the big apps can match the verifiability.

**Optional Edge 4 (if capacity allows):** target the Iraqi/low-bandwidth, mid/low-end-device reality with resumable batch jobs, strict storage-safety checks, and a small base APK + optional model packs (base app without models is tiny; models only when the user wants OCR/upscale). Precedents show the failure mode to avoid: Simple Audio Converter's 39–53 MB APK for an audio-only job, and apps that make the offline promise while shipping ad SDKs.

---

## 5. Risks and open decisions for the Lead

**Risks**
1. **Target API 36 is already mandatory** (extension to 2026-11-01 has passed). Existing sibling stack targets 35; upgrade path (AGP/Kotlin/compileSdk) needs verification before any build work is finalised. **UNVERIFIED**: AGP 8.7.3 + compileSdk 36 compatibility.
2. **16 KB alignment of native media libs** is a launch blocker if prebuilt FFmpeg binaries are not aligned.
3. **Play FGS declaration needs a demo video** — schedule it; missing it blocks release.
4. **Name collisions are real**: Convertly is registered + occupied; Formatly and Tahweel are live in adjacent spaces. A rushed name creates impersonation/opposition risk.
5. **Distribution risk**: clean apps in this space are tiny (100–10K installs) while ad-funded incumbents have 10M+. Even a better product needs an explicit acquisition plan (store optimisation, Arabic listing, community, partnerships).
6. **Model licensing** (commercial-use) is model-auditor's deliverable; any "free for commercial use" claim in the listing must be backed by that audit, per-model, with attribution.
7. **AGPL contamination risk**: ZenConverter (AGPL-3.0) is the closest feature benchmark; do not copy its code or assets into a non-AGPL app. Same caution for GPL apps (Video Transcoder, Morph, Androho, FFShare).
8. **Data-safety honesty**: if we add crash reporting, analytics, ads or an account later, the form and privacy policy must change; the listing's offline/privacy claims must track reality.

**Open decisions (Lead)**
* Monetisation: no-ads/no-tracking by default (recommended, cleanest for Data safety + halal/legal clearance) vs ads/IAP (needs section 2.8 work + data-safety changes).
* Model delivery: PAD asset packs (Play-hosted, needs eligibility check) vs own-host download (data-only, needs consent/SHA-256) vs user-imported model packs (strongest trust story, weakest convenience) — or a mix.
* Whether the base manifest ships INTERNET (needed for downloads; fwit's no-Internet-permission stance is the strongest trust signal but rules out built-in download).
* Name: choose a direction and commission a real trademark clearance (USPTO/EUIPO/WIPO + Iraq + app-store names + domains); this document only flags conflicts.
* Iraq/MENA go-to-market: Arabic listing, RTL screenshots, local pricing/payment constraints (if any paid packs).

---

## 6. Verification log (what was actually fetched)

| Source | URL | Status / note |
|---|---|---|
| Play target API policy | https://developer.android.com/google/play/requirements/target-sdk | fetched 2026-11-16 (HTTP 200) |
| 16 KB page sizes | https://developer.android.com/guide/practices/page-sizes | fetched 2026-11-16 |
| FGS types | https://developer.android.com/develop/background-work/services/fg-service-types | fetched 2026-11-16 |
| Play FGS requirements | https://support.google.com/googleplay/android-developer/answer/13392821 | fetched 2026-11-16 |
| All files access policy | https://support.google.com/googleplay/android-developer/answer/10467955 | fetched 2026-11-16 |
| Photo & video permissions | https://support.google.com/googleplay/android-developer/answer/14115180 | fetched 2026-11-16 (web_search snippet = full page) |
| Notification permission | https://developer.android.com/develop/ui/views/notifications/notification-permission | fetched 2026-11-16 |
| Data safety form | https://support.google.com/googleplay/android-developer/answer/10787469 | fetched 2026-11-16 |
| Data use declaration | https://developer.android.com/privacy-and-security/declare-data-use | fetched 2026-11-16 (snippet) |
| Metadata policy | https://support.google.com/googleplay/android-developer/answer/9898842 | fetched 2026-11-16 |
| Deceptive behaviour | https://support.google.com/googleplay/android-developer/answer/9888077 | fetched 2026-11-16 |
| Device & Network Abuse | https://support.google.com/googleplay/android-developer/answer/9888379 | fetched 2026-11-16 |
| App size limits | https://support.google.com/googleplay/android-developer/answer/9859372 | fetched 2026-11-16 (snippet) |
| Play Asset Delivery | https://developer.android.com/guide/playcore/asset-delivery | fetched 2026-11-16 (snippet) |
| Android App Bundle | https://developer.android.com/guide/app-bundle | fetched 2026-11-16 (snippet) |
| Ads policy | https://support.google.com/googleplay/android-developer/answer/9857753 | fetched 2026-11-16 |
| Content ratings | https://support.google.com/googleplay/android-developer/answer/9898843 | fetched 2026-11-16 (snippet) |
| AI-generated content | https://support.google.com/googleplay/android-developer/answer/14094294 | fetched 2026-11-16 (snippet) |
| Closed testing gate | https://support.google.com/googleplay/android-developer/answer/14151465 | fetched 2026-11-16 (snippet) |
| Developer verification | https://developer.android.com/developer-verification | fetched 2026-11-16 |
| Filee repo | https://github.com/KnifeLemon/Filee | GitHub API 2026-11-16: 28,026 stars, MIT, C# |
| ZenConverter repo+README | https://github.com/Jasonzhu1207/ZenConverter | GitHub API + README 2026-11-16 |
| ZenConverter Play | https://play.google.com/store/apps/details?id=org.zenconverter.app | Play HTML 2026-11-16; AppBrain third-party |
| MorphDrop repo+site | https://github.com/Rajendra0309/morphdrop-android , https://morphdrop.vercel.app/ | GitHub API + site 2026-11-16 |
| fwit app + source | https://play.google.com/store/apps/details?id=com.fwit.videoconverter , https://codeberg.org/fabianwimberger/video-converter-android | Play HTML 2026-11-16; Codeberg API licence null |
| RizkyFauzi06/Morph | https://github.com/RizkyFauzi06/Morph | GitHub API 2026-11-16 |
| brarcher/video-transcoder | https://github.com/brarcher/video-transcoder | GitHub + search snippet 2026-11-16 |
| F-Droid Simple Audio Converter | https://f-droid.org/en/packages/com.technicjelle.simple_audio_converter/ | fetched 2026-11-16 |
| F-Droid FFShare | https://f-droid.org/en/packages/com.caydey.ffshare/ | search result 2026-11-16 |
| Bdroid Audio Converter | https://play.google.com/store/apps/details?id=com.bdroid.audiomediaconverter | Play HTML 2026-11-16 |
| Inverse AI Video Converter | https://play.google.com/store/apps/details?id=com.inverseai.video_converter | Play HTML 2026-11-16; androidrank 2026-11-01 |
| Image to PDF (TrustedOffice) | https://play.google.com/store/apps/details?id=com.readpdf.pdfreader.pdfviewer | web_search snippet 2026-11-16 |
| Androho | https://play.google.com/store/apps/details?id=com.androho.simplevideoconverter , https://github.com/Androho24/SimpleVideoConverter , https://chrome-stats.com/d/com.androho.simplevideoconverter | third-party data 2026-10-19 |
| Convertly app | https://apppage.net/preview/com.app.convertlyy | third-party 2026-11-16 |
| ARK HEIC app | https://play.google.com/store/apps/details?id=com.ark.imageconverterproapp | Play HTML 2026-11-16 |
| Secondary review (video converters) | https://www.thegeeksclub.com/best-video-converters-for-android-in-2026/ | published 2026-10-28; secondary, claims marked in text |
| Name evidence: Tahweel | https://play.google.com/store/apps/details?hl=ar&id=tahweel.io | snippet 2026-11-16 |
| Name evidence: Tabdeel | https://apppage.net/preview/alphatech.app.tabdeel ; https://apps.apple.com/sg/app/tabdeel-exchange-goods/id6464123721 | 2026-11-16 |
| Name evidence: Formatly | https://formatly.app/ ; https://formatlyapp.com/ | 2026-11-16 |
| Name evidence: Convertly mark | https://trademark.justia.com/878/76/convertly-87876131.html | Justia (USPTO mirror) 2026-11-16 |
| Name evidence: Qalib | https://qalib.qa/solutions/mobile-app-development | 2026-11-16 |
| Name evidence: Sigha | https://linkedin.com/company/sigha-sistemas ; https://trademarks.justia.com/904/35/n-90435163.html | 2026-11-16 |

**Limitations of this dossier:** Play Store pages do not expose install/rating numbers for every app and their HTML was partially JS-rendered; numbers marked third-party are not first-party. We did not run a user survey or scrape reviews systematically; "what users punish" is inferred from store cards, ratings, descriptions and third-party trackers, and is labelled accordingly. No trademark clearance was performed. No code from any listed project was copied.
