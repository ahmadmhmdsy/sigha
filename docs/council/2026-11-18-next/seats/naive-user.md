# The Naive User — Umm Ali / الزبون الغبي

**Seat:** product-strategist · **Round:** 2026-11-18-next · **Rule:** a remaining item is work only if it changes a step she takes or a sentence she reads.

*Path shorthand: `WorkbenchScreen.kt` = `app/src/main/java/com/sigha/converter/ui/WorkbenchScreen.kt`; `MainActivity.kt` = `app/src/main/java/com/sigha/converter/MainActivity.kt`; `strings_ui.xml` = `app/src/main/res/values-ar/strings_ui.xml`.*

## 1. What remains in my dimension, grounded

- **I asked for one PDF; the app hands me twenty.** Multi-file→one-PDF is not implemented — each image becomes its own PDF (`PRODUCT.md:47`; per-file converter `engine/image/ImageToPdfConverter.kt:24-58`), and «make this photo small enough for WhatsApp» is a quality slider, not a target size (`PRODUCT.md:49`). Both are the roadmap outcomes the wedge is built on (`PRODUCT.md:40-41`), explicitly not in 0.2.x (`PRODUCT.md:79`). The moment my 20 photos of a government form come back as 20 files is the moment I delete the app: nothing crashed, it just did not do my job. — **the delete moment.**
- **The one thing I would tell a friend is real today:** «التحويل يصير على الهاتف، الملف ما ينرفع» — my Word CV becomes a PDF on the phone (`PRODUCT.md:3,63`). That is the only «what is in it for me» the build has earned.
- **Remaining order, where it touches me:** (a) while the batch runs the bar is spinner + «جارٍ التحويل…» + Cancel — no «7 of 20», no destination (`WorkbenchScreen.kt:874-883`; `council-review.md:131-135`); the done row now shows the saved path (`WorkbenchScreen.kt:671-687`) but only after the wait; (b) the CTA says «تحويل» / «تحويل N ملفات», never «إلى PDF» (`WorkbenchScreen.kt:906-916`; `council-review.md:119-123`) — I confirm nothing before a long wait; (c) the target card is below my files, so the bar asks for a decision I cannot see (`council-review.md:121`); (d) my scanned form meets an install card whose only explanation is «تظهر أهداف OCR بعد تثبيت حزمة OCR» (`strings_ui.xml:122`) — a 28 MB acronym, never «نص تنسخه» — and no hardware evidence exists that scan→TXT yields a usable Arabic page (`verification.md:155-156,187-194,212-213`).
- **One unlisted step:** a raw notification-permission prompt on cold start, before I have picked a file (`MainActivity.kt:44,91`; `council-review.md:217`). For me «notifications» means ads, so I deny; when I later leave the app mid-batch I see nothing and assume it froze.
- **Not my dimension — no step, no sentence:** identity layer, per-app language, motion, DESIGN.md, bidi beyond the already-wrapped saved path, duplication counts. Downgrade or drop them.

## 2. Recommended next sequence (max 5) — and what I would not do now

1. **Prove my money steps on one real Arabic phone before any 0.2.2 work:** CV DOCX→PDF opened and shared; a real scanned Arabic government form → TXT after the pack, read by a native Arabic speaker; a 20-file batch that survives leaving the app (`device-checklist.md` B6-B7, B18, C5-C6, E1-E3; preconditions `store-listing.md:245-252`; today only AVD evidence, `verification.md:155-156`). Fix only what breaks.
2. **Maestro's #1 plus the destination in the bar:** «جارٍ تحويل ٧ من ٢٠ — إلى PDF» while running, «حُفظت في Download/Sigha» when done — I watch the bar, not the queue (`council-review.md:226,259`).
3. **Composition, as Maestro ordered:** one contextual bottom surface, target sheet opened from the bar, CTA naming the target («تحويل إلى PDF») (`council-review.md:119-135`).
4. **OCR says what I buy before it asks 28 MB,** and asks at the moment I choose a scan→text; the notification prompt moves to my first conversion with one line of reason (`council-review.md:217,227`).
5. **Hide the AI-upscaling card in the tester build** (one line): a shipped «غير متاح بعد» invites exactly the false-claim reports the test grades (`council-review.md:208,264`; `store-listing.md:290-293`).

**Not now:** identity layer, per-app language, motion, bidi-everywhere; any 0.3 outcome (many→one PDF, target-size compress) — that is the next product increment, not 0.2.2.

## 3. Qiyas card — the bar answers the waiting question (position + destination)

- **Al-Far':** the bar reads «7 of 20, converting to PDF, lands in Download/Sigha» while I wait — not a richer results summary afterwards.
- **Al-Asl:** the app's own 0.2.1 OCR fix — the inline card put the answer where I stand and an Arabic user installed 28 MB without navigating (`verification.md:187-194`; `council-review.md:202`). Internal precedent, verifiable in this repo; no external study claimed.
- **Al-'Illa:** a waiting person asks one question («خلص؟») and can answer it only by leaving the surface where the work started; an answer behind navigation arrives after doubt, Cancel, or a swipe away.
- **Tanqih:** index, count and path already exist in the queue and rows — no engine, state or new screen; drop a second summary surface and the redundant «where» in prose.
- **Conditions:** one line at Arabic font 2.0; destination only after the file is published; no per-item TalkBack announcements.

## 4. Fatal flaw in the obvious plan — Tahqiq al-manat

The obvious plan is Maestro's 0.2.2 order, then the 12-tester / 14-day closed test as the real-user evidence. Its biggest item reports a batch, but my batch has no reward to report: 20 photos become 20 PDFs (`PRODUCT.md:47`), and the tester script never asks anyone to attempt any of the four outcomes (`store-listing.md:269-286`). **Tool: Tahqiq al-manat** — the presumed 'illa («she cannot see what happened») exists but is not sufficient; the cause that decides tomorrow is that two of my four jobs cannot end in a file I can send (`PRODUCT.md:40-49`). Executed perfectly, the test yields a crash/claim census, not retention; the wedge metric the roadmap re-order depends on needs ≥200 installs (`PRODUCT.md:121,129-131`). **Do not read 'she would come back' from it.**

## 5. Red line before the 12-tester closed test begins

- On a real Arabic phone, not the AVD: (i) a real scanned Arabic form → TXT after the pack, the text read by a native Arabic speaker and judged useful — `device-checklist.md` B6 forbids only an empty file and never measures quality; (ii) CV DOCX→PDF opened and shared from History; (iii) a 20-file batch left for two minutes, returned to, complete, position and destination readable at font 2.0 (`device-checklist.md` B18, E1-E3; `verification.md:155-156`).
- One unaided first run by a non-technical Arabic speaker (not a recruiter, not a script): «حوّل هذه السيرة إلى PDF وابعتها واتساب». If she needs instruction, the sentences are the defect, not the tester.
- The first ask is explainable: notification permission gets one line of reason at the right moment, never a raw cold-start prompt (`MainActivity.kt:44,91`).
- If any of these fails, do not start the 14-day clock — it cannot be paused, and the testers become QA for what the device checklist would have caught (`store-listing.md:245-252`).

## Round 2 — revision

**Accepted:** the 12×14-day test is a production-access gate, not a retention experiment (`store-listing.md:239-243,299-301`; `release-blockers.md:104-109`). I withdraw the round-1 objection; only the exit wording must stay honest — report tasks, crashes and claims, never «users want it» (`store-listing.md:287-295`).

**Keep:** the hardware walk as the first action (rewritten below); the referral sentence and the delete moment remain post-access hypotheses, not upload work.

**Drop/downgrade before upload:** bar n-of-m + destination sentence, OCR benefit copy, notification-rationale move, AI-card hide, native-speaker OCR quality read, unaided first-run test, 20-file batch, folder grant/revocation, theme/font/TalkBack/landscape rows — none is graded by the script (`store-listing.md:267-286` vs `device-checklist.md` A-H); all move to post-access. Composition (Maestro #2) and bidi beyond the saved path likewise; identity and per-app language stay out.

**First action changes:** walk the 12-task tester script once, end-to-end, on one physical Arabic Android 13+ phone against the upload commit; fix crashes/P1s and the one code sentence below; then upload — no design work first.

**(a) Minimum for a tester to complete the script:** on that phone, tasks 1–10 each run once — image→JPG/PDF with the result in Downloads; DOCX→PDF/TXT + note; XLSX→CSV first-sheet note; PDF→JPG page 1; scanned PDF→TXT without the pack = install instruction; pack on Wi-Fi then scanned→TXT = text + OCR note; 5-file batch survives 2 minutes backgrounded; airplane-mode image; share-in and share-out; deny-notification batch with progress still in Queue. Task 11 only if a legacy device is in the cohort (`store-listing.md:256-262`). Task 5 also needs the 28 MB pack host reachable from testers' Wi-Fi (`store-listing.md:199-202`; reachability unverified).

**(b) The one sentence:** «لا يحتوي ملف PDF على طبقة نصية (يبدو أنه صورة ممسوحة). ثبّت حزمة OCR لقراءة الملفات الممسوحة.» (`strings_ui.xml:6`) — task 4 is the only script time the expected result is an instruction, not a file. It is written but unreachable: `NO_TEXT_LAYER` and `MODEL_PACK_MISSING` are declared and never thrown (`core/Converter.kt:54-55`), and the scanned-PDF path rethrows the default kind, so the tester sees «فشل التحويل.» (`engine/pdf/PdfToTextConverter.kt:94-97`; `core/Converter.kt:70-74`; `ui/FailureUi.kt:27`). One `kind` argument puts the script sentence into the upload build. The destination sentence is not it: testers find Downloads themselves, and done rows already render the path (`WorkbenchScreen.kt:671-687`).

**(c) Dropped from my round-1 sequence:** 2 (bar n-of-m/destination), 4 (OCR benefit + permission rationale) and 5 (AI card) as pre-upload work — kept on the 0.2.2 roadmap, not the upload cut; the round-1 red line re-scopes to post-access (native OCR quality read, unaided first run, 20 photos). What survives: script walk → failure-kind fix → upload.
