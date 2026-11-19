# Sigha — store listing (Play Console draft)

> **Status:** draft for the closed test. Do not paste anything here into Play Console until the
> gate in §9 passes. Every claim below was checked against the source tree and
> [`PRODUCT.md`](PRODUCT.md); if a maximum length is exceeded, Play truncates or rejects it.
> This file owns the listing; the app's own strings are owned by the UI workstream.
> The keyword evidence, the localized title and the shot order are ruled in
> [`aso-plan-2026-11-19.md`](aso-plan-2026-11-19.md) (council 2026-11-19: title/description frozen
> until B5 answers).

## 1. Listing metadata

| Field | Proposed value | Limit | Status |
|---|---|---|---|
| App name | `Sigha: File Converter` | 30 chars | fits (21) |
| App name AR (localized) | `صيغة: محوّل ملفات على هاتفك` | 30 chars | fits (27); confirm the console exposes a localized title field |
| Short description EN | see §2 | 80 chars | 71 |
| Short description AR | see §2 | 80 chars | 79 |
| Full description EN | see §2 | 4,000 chars | 2,138 usable (1,862 spare) |
| Full description AR | see §2 | 4,000 chars | 2,045 usable |
| Release notes EN | see §3 | 500 chars | 327 |
| Release notes AR | see §3 | 500 chars | 318 |
| Category | Tools | — | proposed; Product category must not imply office-suite fidelity |
| Contact email | publisher-to-fill | — | human task |
| Privacy policy URL | publisher-to-fill | — | human task (B2) |
| Icon | 512×512 PNG, 32-bit | — | human task; current launcher icon is the source |
| Feature graphic | 1024×500 PNG | — | human task |
| Phone screenshots | 8 per language, 16:9 or 9:16 | 2–8 | §4 |
| Content rating | Everyone; no ads, no user-generated content, no data sharing | — | answer from §6 |
| Target audience | 13+ (utility) | — | confirm with policy owner |
| Ads declaration | No ads | — | code has no ad SDK |

Name note: keep "Offline" out of the app title. The title is already a claim channel; "Sigha: File
Converter" is verifiable, "Offline" invites a policy argument about the one network request.

## 2. Store copy (AR + EN)

### Short description — English (71/80)

```
Image to PDF, PDF to text, DOCX to PDF, video to MP3 — free, no upload.
```

### Short description — Arabic (79/80)

```
تحويل صورة إلى PDF، وPDF إلى نص، وDOCX إلى PDF، وفيديو إلى MP3. مجاني، دون رفع.
```

### Full description — English (2,138/4,000)

```
Sigha converts files on your phone. Pick a file with the system picker, or share it from any app, choose a target format, and Sigha converts it without uploading it anywhere.

Sigha converts PDFs and to PDFs, but it does not edit, merge, split or compress them; extracting text from a scanned PDF needs the optional OCR pack.

WHAT YOU CAN DO
• Documents: DOCX, ODT and PPTX to text; DOCX and XLSX to PDF; XLSX and ODS to CSV; CSV to XLSX; TXT and Markdown to PDF; HTML to text.
• PDF: export pages as PNG or JPG, or extract the text. If a PDF is a scan with no text layer, Sigha can read it with OCR after you install the free OCR pack.
• Images: JPG, PNG, WebP, BMP, GIF, HEIC and AVIF to JPG, PNG or WebP; any image to PDF; quality control.
• Audio and video: convert between MP3, M4A, AAC, WAV, FLAC, OGG and OPUS; video to MP4, WebM, MKV or GIF; extract audio; choose the bitrate.
• Data: CSV, TSV and JSON. Subtitles: SRT and VTT. E-books: EPUB to text or HTML. Archives: ZIP and TAR.GZ repacking, 7Z to ZIP.

BUILT FOR PRIVACY
• Conversions run on your device. Your files are not uploaded.
• No account, no ads, no analytics, no tracking SDKs.
• The only network request is the optional OCR pack download, which you start yourself (28 MB, English and Arabic).
• Results are saved to Download/Sigha, or a folder you choose.

BUILT FOR ARABIC
• Full Arabic and English interface, including right-to-left layout.

HONEST LIMITS
• Office documents are converted as text and tables; complex layout and images are not preserved.
• Scanned PDFs need the OCR pack to produce text; OCR works in English and Arabic only.
• RAR archives are not supported, and 7Z can only be converted to ZIP.
• Photos and videos cannot be compressed to a size you choose; you can re-encode at a chosen quality or bitrate.
• HEIC needs Android 9 or newer; AVIF needs Android 12 or newer.

Version 0.2.1 is a closed-test build. The four things we build next — many images into one PDF, scanned pages into one PDF, PDF merge/split/compress, and compressing photos and videos to a shareable size — are not in this build yet.

Sigha requires Android 8.0 or newer.
```

**Before the production listing:** delete the "Version 0.2.1 is a closed-test build…" paragraph.
It is true for the closed test and false algorithmically once production users install the app.
Keep the "HONEST LIMITS" block in every version.

### Full description — Arabic (2,045/4,000)

```
يحوّل Sigha ملفاتك على هاتفك. اختر ملفًا من مدير الملفات أو شاركه من أي تطبيق، ثم اختر الصيغة المطلوبة، ويحوّله Sigha دون رفعه إلى أي مكان.

يحوّل Sigha ملفات PDF وإلى PDF، لكنه لا يحرّرها ولا يدمجها ولا يقسّمها ولا يضغطها؛ واستخراج النص من ملف PDF ممسوح ضوئيًا يحتاج حزمة OCR الاختيارية.

ما يمكنك فعله
• المستندات: تحويل DOCX وODT وPPTX إلى نص؛ وDOCX وXLSX إلى PDF؛ وXLSX وODS إلى CSV؛ وCSV إلى XLSX؛ وTXT وMarkdown إلى PDF؛ وHTML إلى نص.
• PDF: تصدير الصفحات صورًا PNG أو JPG، أو استخراج النص. وإذا كان الملف ممسوحًا ضوئيًا بلا طبقة نصية، يمكن لـ Sigha قراءته بالتعرّف الضوئي بعد تثبيت حزمة OCR المجانية.
• الصور: تحويل JPG وPNG وWebP وBMP وGIF وHEIC وAVIF إلى JPG أو PNG أو WebP؛ وأي صورة إلى PDF؛ مع التحكم في الجودة.
• الصوت والفيديو: التحويل بين MP3 وM4A وAAC وWAV وFLAC وOGG وOPUS؛ وتحويل الفيديو إلى MP4 أو WebM أو MKV أو GIF؛ واستخراج الصوت؛ واختيار معدل البت.
• البيانات: CSV وTSV وJSON. والترجمات: SRT وVTT. والكتب: EPUB إلى نص أو HTML. والأرشيفات: إعادة تجميع ZIP وTAR.GZ، وتحويل 7Z إلى ZIP.

مصمَّم للخصوصية
• تجري عمليات التحويل على جهازك، ولا تُرفع ملفاتك.
• لا حساب، ولا إعلانات، ولا تحليلات، ولا أدوات تتبّع.
• الطلب الوحيد عبر الشبكة هو تنزيل حزمة OCR الاختيارية الذي تبدأه بنفسك (28 ميغابايت، الإنجليزية والعربية).
• تُحفظ النتائج في Download/Sigha أو في مجلد تختاره.

مصمَّم للعربية
• واجهة كاملة بالعربية والإنجليزية، مع تخطيط من اليمين إلى اليسار.

حدود صريحة
• تُحوَّل مستندات المكتب كنصوص وجداول؛ ولا يُحتفظ بالتنسيق المعقّد ولا بالصور.
• تحتاج ملفات PDF الممسوحة ضوئيًا إلى حزمة OCR لإخراج النص، والتعرّف الضوئي متاح بالإنجليزية والعربية فقط.
• لا يدعم التطبيق أرشيفات RAR، ويمكن تحويل 7Z إلى ZIP فقط.
• لا يمكن ضغط الصور والفيديو إلى حجم تختاره؛ يمكنك إعادة الترميز بجودة أو معدل بت محدد.
• يتطلب HEIC نظام أندرويد 9 أو أحدث، ويتطلب AVIF نظام أندرويد 12 أو أحدث.

الإصدار 0.2.1 إصدار اختبار مغلق. الأمور الأربعة التي نبنيها تاليًا — دمج عدة صور في PDF واحد، وتحويل الصفحات الممسوحة إلى PDF واحد، ودمج/تقسيم/ضغط PDF، وضغط الصور والفيديو إلى حجم قابل للمشاركة — ليست في هذا الإصدار بعد.

يتطلب Sigha أندرويد 8.0 أو أحدث.
```

### Translation parity rules

1. Both descriptions must list the same formats, the same limits and the same privacy statement.
   A feature present in one language and absent in the other is a policy defect.
2. The Arabic must not be a machine translation: "OCR", "bitrate", "metadata" and format names
   may stay in Latin script, everything else is Arabic.
3. "offline" is translated as "على جهازك" / "دون إنترنت للتحويل", never as an absolute promise.
4. RTL: the Arabic listing must be reviewed on a device; mixed-direction lines (format names,
   "Download/Sigha") must render correctly.

## 3. What's new — 0.2.1 (closed test, first upload)

Release notes must fit 500 characters per language. The 0.2.0 build was never uploaded, so these
notes describe the first closed-test upload; they are user-visible changes only.

### English (399/500)

```
First closed-test build.
• Convert documents, images, PDFs, audio, video, archives and data files on your phone.
• Arabic and English interface with right-to-left layout.
• Arabic privacy policy, and every status note now speaks Arabic.
• Install the OCR pack from the convert screen for scanned PDFs and photos.
• Batch queue, history, and a choice of output folder.
No uploads, no ads, no account.
```

### Arabic (366/500)

```
أول إصدار للاختبار المغلق.
• حوّل المستندات والصور وملفات PDF والصوت والفيديو والأرشيفات والبيانات على هاتفك.
• واجهة عربية وإنجليزية مع تخطيط من اليمين إلى اليسار.
• سياسة خصوصية عربية، وكل ملاحظات الحالة أصبحت بالعربية.
• ثبّت حزمة OCR من شاشة التحويل لملفات PDF الممسوحة ضوئيًا والصور.
• قائمة تحويل دفعة وسجل وخيار مجلد الإخراج.
دون رفع، دون إعلانات، دون حساب.
```

Rules for future release notes: describe user-visible changes only; never publish format counts,
APK sizes, hashes, "bug fixes" bowls of soup, or a number that today's build cannot reproduce.
The engineering changelog is [`CHANGELOG.md`](../../CHANGELOG.md); it is not store copy.

## 4. Screenshot shot list

Eight phone screenshots per language (English set, Arabic set). Capture from the same build that
will be uploaded, on a device or emulator with a realistic screen (16:9 or 9:16, no debug banners,
no test ad placeholders, no developer overlays). Seed real-looking but harmless files; never show
real personal data. Arabic shots require an Arabic system locale and RTL layout.

Order is outcome-first (council 2026-11-19): the first two shots prove a selection and a saved
result — the old set opened on an empty picker and never showed a result. Shot 1 uses **one** image,
not three: three images with a PDF target imply many-images-to-one-PDF, which 0.2.1 does not ship.

| # | Screen | State to capture | Caption EN | Caption AR |
|---|---|---|---|---|
| 1 | Convert | One image selected, PDF target visible, convert CTA on screen | One photo becomes one PDF | صورة واحدة تصبح PDF واحدًا |
| 2 | Queue/history | Completed rows with the saved path `Download/Sigha` | Saved to Download/Sigha | تُحفظ في Download/Sigha |
| 3 | PDF | PDF selected, page-range field and page-export targets | PDF pages to JPG, or the text out | صفحات PDF إلى صور أو النص |
| 4 | Result | Extracted text, selectable, with the source PDF name | The text out of a PDF, selectable | النص من PDF، قابل للتحديد |
| 5 | Model packs | OCR pack row: name, 28 MB, Apache-2.0, Download button | OCR pack for scanned PDFs — English and Arabic | حزمة OCR للملفات الممسوحة — الإنجليزية والعربية |
| 6 | Queue | Batch in progress with per-file progress | Convert a whole batch, one by one | حوّل دفعة كاملة، ملفًا بعد ملف |
| 7 | Options sheet | Image quality slider at 85 | Choose quality or bitrate | اختر الجودة أو معدل البت |
| 8 | About | Version, privacy statement, licence link | No upload. No account. No ads. | دون رفع. دون حساب. دون إعلانات. |

The Arabic set mirrors the eight screens **entirely in Arabic UI** — shots 1 and 2 must be Arabic,
not shot 6 of an English set (Expert round 2). Capture both sets from the upload-key build after the
tag; record the device model, Android version and build SHA.

Do **not** capture: upscaling (not shipped), camera scanning (not shipped), merge/compress
screens (do not exist), a notification if the notification flow is not final, or a mock result
that the app cannot produce.

## 5. Data safety answers

Play Console → App content → Data safety. Answer from the shipped build, not from this plan; the
answer must match the privacy policy word for word in substance.

| Question | Answer | Why |
|---|---|---|
| Does the app collect or share required user data types? | **No** | No account, no telemetry, no ad SDK, no crash SDK, no server. File processing is local. |
| Is any data processed off the device? | **No**, except the pack download | The OCR pack download is an app-content download, not user data; no identifier is sent. |
| Data types: location, personal info, financial, health, messages, photos/videos, files/docs, contacts, calendar, app activity, web browsing, app info/performance, device IDs | **None collected, none shared** | Local file handling is explicitly not "collection" under Play's definition; verify with policy owner before submitting. |
| Is all data encrypted in transit? | N/A | No user data is transmitted. The pack download is HTTPS. |
| Do you provide a way to request data deletion? | N/A | No data held off-device; the in-app history can be cleared by the user and files are the user's own. |
| Ads or third-party advertising SDKs | No | No ad dependency in the build. |
| Account creation | No | No login of any kind. |
| Third-party SDKs that collect data | None | Engines (FFmpeg fork, PDFBox, Commons Compress, XZ, Tesseract4Android), UI (Compose, Coil for local thumbnails) and storage (DataStore) do not transmit. |
| App access | All functionality available without restrictions | No login wall. |
| Permissions | see [`permissions-rationale.md`](permissions-rationale.md) | Play asks about sensitive permissions; the rationale file is the source. |

Note for the form: the pack host sees the device IP as any HTTP request does; Play's Data safety
questionnaire does not treat an app-content download as user-data collection, but the **privacy
policy must still disclose** that the optional download contacts `raw.githubusercontent.com`
(see WS4 / `docs/legal/`). Do not write "no network requests at all" anywhere.

## 6. Store listing answers that are not the Data safety form

| Declaration | Answer | Evidence |
|---|---|---|
| Ads | No ads | no ad dependency; grep the dependency graph before release |
| In-app purchases | No | no billing dependency |
| News app | No | — |
| COVID-19 apps | No | — |
| Government app | No | — |
| Financial features | No | — |
| Health | No | — |
| Data collection for account creation | No account | — |
| Content rating questionnaire | Utility: no violence, sexual content, language, controlled substances, gambling, user interaction, location sharing, purchases, ads | — |
| Target API level | 36 (Android 16) | `app/build.gradle.kts`; re-check with `scripts/verify.ps1` |
| App bundle format | AAB with all ABIs; per-ABI APKs only for direct distribution | `app/build.gradle.kts` |

## 7. Permissions summary (for listing review, not user copy)

Full detail: [`permissions-rationale.md`](permissions-rationale.md).

| Permission | Protection | Why | If denied |
|---|---|---|---|
| `INTERNET` | normal | Optional OCR pack download only | Pack download fails with a message; conversion works |
| `ACCESS_NETWORK_STATE` | normal | Metered-connection warning before the 28 MB download | Warning may not appear; download still offered |
| `POST_NOTIFICATIONS` | runtime (Android 13+) | Progress notification for a running batch | Batch still runs; progress stays in the app's Queue screen |
| `FOREGROUND_SERVICE` | normal | Keep a user-started batch alive while backgrounded | Batch pauses when the app leaves the foreground |
| `FOREGROUND_SERVICE_DATA_SYNC` | normal | Service type for the same batch | Service cannot start; same fallback |
| `WRITE_EXTERNAL_STORAGE` | runtime, maxSdk 28 | Legacy save to Downloads on Android 8–9 only | Save fails with a message; user can pick another folder |

Deliberately **not** requested: `READ_EXTERNAL_STORAGE`, `READ_MEDIA_IMAGES/VIDEO`, `CAMERA`,
`RECORD_AUDIO`, `ACCESS_FINE_LOCATION`, `QUERY_ALL_PACKAGES`, `MANAGE_EXTERNAL_STORAGE`,
`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, `SYSTEM_ALERT_WINDOW`. Files arrive through SAF or a share
intent; nothing else needs broad access. A build that adds one of these must update this table and
the policy notes first.

## 8. Closed test plan (12 testers × 14 days)

This is the Play requirement for **new personal developer accounts**; confirm the account type in
Play Console with the publisher (organizations may differ). Plan for 15 recruited testers so
dropouts do not break the 12-tester floor.

### Preconditions

1. Signed release AAB built from the exact commit being tested, device QA checklist recorded
   (currently not run — do not start the clock before it is).
2. Privacy policy URL live and linked from the app and the listing (human task B2).
3. Data safety form completed and matching the privacy policy; content rating done.
4. Store listing text (this file), screenshots (EN + AR), icon and feature graphic ready.
5. `scripts/verify.ps1` green: JVM tests, lint, release build, AAB, packaging assertions.

### Testers

| Cohort | Count | Why |
|---|---|---|
| Arabic-first, Android 13+ | 6 | Primary audience, notification permission path, RTL |
| English, Android 13+ | 3 | Secondary locale |
| Android 8–9 devices | 2 | Legacy storage permission and legacy save path |
| Android 12 and below without notification permission | 2 | Pre-13 behaviour |
| Low-storage or slow device | 2 | Failure messages, timeouts, free-space guard |

Testers install through the Play opt-in link and keep the app installed for 14 **consecutive**
days. The clock only counts opted-in days.

### Tester script (send as a checklist; ask for one line per task)

1. Convert a photo to JPG and to PDF; check the result in the Download/Sigha folder.
2. Convert a DOCX to PDF and to TXT; check the text output and the note about layout.
3. Convert an XLSX with two sheets to CSV; confirm the "first sheet only" note.
4. Convert a PDF to JPG with page range `1`; then a scanned PDF (no text layer) to TXT **without**
   the OCR pack — expect a clear install instruction, not a crash.
5. Install the OCR pack on Wi-Fi, then repeat task 4 — expect text and the OCR note.
6. Start a 5-file batch, switch apps for 2 minutes — expect the notification/progress to continue,
   and the final rows to be complete on return.
7. Turn on airplane mode and convert an image — expect success (no network needed).
8. Deny the notification permission (Android 13+) and start a batch — expect conversion to
   continue, no crash, progress still visible in Queue.
9. Share a file from Files/Photos into Sigha; convert it; share the result back out.
10. Switch the app to Arabic and repeat one conversion; report any untranslated or broken RTL text.
11. Deny legacy storage permission on Android 8–9 (if applicable) and save — expect a message and
    an alternative folder path.
12. Fill the report form with: device model, Android version, task number, result, screenshot if
    broken.

### Pass criteria

- No open P0/P1 defect; no crash in a core flow.
- Crash-free sessions ≥ 99.5% in Play vitals for the test cohort.
- ≥ 95% conversion success across the script's expected-success tasks.
- Zero false claims found by testers (any claim that a feature exists but is absent is a P0 for
  the listing, not a code bug).
- Notification and denial behaviour verified on at least one Android 13+ device.
- Every tester completes ≥ 8 of 12 tasks; written exit decision recorded before production.

### Exit

Promote the same build to production (staged rollout 20% → 50% → 100%, watching vitals), keep the
closed track for future builds. If a criterion fails, fix and restart the 14-day clock only if the
tester cohort or opt-in list changes materially.

## 9. Gate before anything here is submitted

- [ ] `scripts/verify.ps1` green on the release commit **with real B2/B6 values** — never greened by
      deleting placeholder lines
- [ ] Device QA checklist recorded (not just emulator smoke)
- [ ] Privacy policy URL live; Data safety submitted; content rating done
- [ ] Foreground-service declaration completed with the demo video recorded on the tagged build
- [ ] The **upload-key-signed** AAB has been installed once on an arm64 Android 14+ phone and the
      Step Zero script walked on it, before the closed-test clock starts
- [ ] Title, short and full descriptions match `aso-plan-2026-11-19.md` §4–§6 (EN default +
      Arabic localized title); the no-edit/merge/split/compress line is before the read-more fold;
      both locales ≤ limits and both true **today**
- [ ] Listing parity: the EN and AR title/short carry the same claim set (no locale-only promise)
- [ ] 8 screenshots per language, outcome-first order, captured from the upload build, no
      unimplemented feature; the Arabic set is entirely Arabic UI
- [ ] Permissions table above matches the merged manifest (`aapt2 dump permissions`)
- [ ] Human blockers B1–B7 from `docs/plan/production-readiness.md` cleared
- [ ] Legal counsel sign-off on "privacy-safe" wording and the licence/relink statement

## 10. Human-only tasks (cannot be done in this repository)

Upload key and Play Console; developer/merchant account status; privacy-policy hosting; Data
safety and content-rating forms; tester recruitment; screenshots, icon and feature graphic upload;
trademark clearance for "Sigha"; LGPL Corresponding Source hosting for the FFmpeg fork; legal
counsel review. See `docs/plan/production-readiness.md` B1–B7.
